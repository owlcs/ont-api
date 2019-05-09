/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2019, Avicomp Services, AO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package ru.avicomp.ontapi.jena.impl;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.enhanced.UnsupportedPolymorphismException;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.RDFListImpl;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.shared.PropertyNotFoundException;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.util.iterator.NullIterator;
import org.apache.jena.util.iterator.WrappedIterator;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntList;
import ru.avicomp.ontapi.jena.model.OntObject;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.utils.Iter;
import ru.avicomp.ontapi.jena.utils.Models;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Default OntList implementation.
 * <p>
 * Created by @szuev on 10.07.2018.
 */
@SuppressWarnings("WeakerAccess")
public abstract class OntListImpl<E extends RDFNode> extends ResourceImpl implements OntList<E> {
    private static final UnaryOperator<RDFList> IDENTITY = UnaryOperator.identity();

    protected final Resource subject;
    protected final Property predicate;
    protected final Class<E> elementType;
    protected final Resource listType;
    private RDFList objectRDFList;

    protected OntListImpl(Resource subject,
                          Property predicate,
                          RDFList object,
                          Resource listType,
                          OntGraphModel model,
                          Class<E> elementType) {
        super(object.asNode(), (EnhGraph) model);
        this.objectRDFList = object;
        this.subject = subject;
        this.predicate = predicate;
        this.elementType = elementType;
        this.listType = listType;
    }

    /**
     * Creates a fresh OntList based on the given {@code elementType}
     * containing all content from the specified collection preserving the original order.
     * The resulting list will be attached to the model by the given {@code subject} and {@code predicate}.
     *
     * @param model       {@link OntGraphModelImpl}
     * @param subject     {@link OntObject}
     * @param predicate   {@link Property}
     * @param elementType class-type of OntList elements
     * @param elements    {@link Iterator} of elements to be added to the new rdf-list
     * @param <N>         {@link RDFNode} subtype
     * @return a fresh {@link OntList} instance
     */
    public static <N extends RDFNode> OntListImpl<N> create(OntGraphModelImpl model,
                                                            OntObject subject,
                                                            Property predicate,
                                                            Class<N> elementType,
                                                            Iterator<N> elements) {
        return create(model, subject, predicate, null, elementType, WrappedIterator.create(elements));
    }

    /**
     * Creates a fresh OntList using {@code Iterator} and other parameters.
     *
     * @param model       {@link OntGraphModelImpl Ontology RDF Model Impl}, not {@code null}
     * @param subject     {@link OntObject} a subject for new root statement, not {@code null}
     * @param predicate   {@link Property} a predicate for new root statement, not {@code null}
     * @param listType    {@link Resource} list type, must be an URI-Resource or {@code null} for default []-list
     * @param elementType class-type of {@code OntList} elements
     * @param elements    {@link Iterator} of elements to be added to the new rdf-list
     * @param <N>         any subtype of {@link RDFNode}
     * @return a fresh {@link OntList} instance
     */
    public static <N extends RDFNode> OntListImpl<N> create(OntGraphModelImpl model,
                                                            OntObject subject,
                                                            Property predicate,
                                                            Resource listType,
                                                            Class<N> elementType,
                                                            ExtendedIterator<N> elements) {
        checkRequiredInput(model, subject, predicate, listType, elementType);
        elements = Iter.peek(elements, n -> OntJenaException.notNull(n, "OntList: null element is specified."));
        RDFList list = listType != null ? createTypedList(model, listType, elements) : model.createList(elements);
        model.add(subject, predicate, list);
        return new OntListImpl<N>(subject, predicate, list, listType, model, elementType) {
            @Override
            public boolean isValid(RDFNode n) {
                return true;
            }
        };
    }

    /**
     * Wraps the given RDFList as OntList.
     *
     * @param list        {@link RDFList} an existing rdf-list, not {@code null}
     * @param model       {@link OntGraphModelImpl Ontology RDF Model Impl}, not {@code null}
     * @param subject     {@link OntObject} a subject for existing root statement, not {@code null}
     * @param predicate   {@link Property} a predicate for existing root statement, not {@code null}
     * @param listType    {@link Resource} list type, must be an URI-Resource or {@code null} for default []-list
     * @param elementType class-type of {@code OntList} elements
     * @param <N>         any subtype of {@link RDFNode}
     * @return a fresh {@link OntList} instance which wraps an existing []-list within the model Graph
     * @see #asSafeOntList(RDFList, OntGraphModelImpl, OntObject, Property, Resource, Class)
     */
    public static <N extends RDFNode> OntListImpl<N> asOntList(RDFList list,
                                                               OntGraphModelImpl model,
                                                               OntObject subject,
                                                               Property predicate,
                                                               Resource listType,
                                                               Class<N> elementType) {
        checkRequiredInput(model, subject, predicate, listType, elementType);
        Objects.requireNonNull(list, "Null RDF-List");
        return new OntListImpl<N>(subject, predicate, list, listType, model, elementType) {
            @Override
            public boolean isValid(RDFNode n) {
                return n.canAs(elementType);
            }
        };
    }

    /**
     * Wraps the given RDFList as OntList.
     * This method creates an instance of {@code OntList} which takes care about possible graph-recursions.
     * Used for class-expressions and data-ranges,
     * because it is theoretically possible to have definition of some expression
     * which relies on the definition of another (or the same) expression.
     *
     * @param list        {@link RDFList} an existing rdf-list, not {@code null}
     * @param model       {@link OntGraphModelImpl Ontology RDF Model Impl}, not {@code null}
     * @param subject     {@link OntObject} a subject for existing root statement, not {@code null}
     * @param predicate   {@link Property} a predicate for existing root statement, not {@code null}
     * @param listType    {@link Resource} list type, must be an URI-Resource or {@code null} for default []-list
     * @param elementType class-type of {@code OntList} elements
     * @param <N>         any subtype of {@link RDFNode}
     * @return a fresh {@link OntList} instance which wraps an existing []-list within the model Graph
     * @see #asOntList(RDFList, OntGraphModelImpl, OntObject, Property, Resource, Class)
     */
    protected static <N extends RDFNode> OntListImpl<N> asSafeOntList(RDFList list,
                                                                      OntGraphModelImpl model,
                                                                      OntObject subject,
                                                                      Property predicate,
                                                                      Resource listType,
                                                                      Class<N> elementType) {
        return new OntListImpl<N>(subject, predicate, list, listType, model, elementType) {
            @Override
            public boolean isValid(RDFNode n) {
                return PersonalityModel.canAs(elementType, n.asNode(), model);
            }
        };
    }

    /**
     * Lists all rdf-lists by subject and predicate in the form of {@link OntList OntList}.
     *
     * @param model       {@link OntGraphModelImpl}
     * @param subject     {@link OntObject} a subject for existing root statement, not {@code null}
     * @param predicate   {@link Property} a predicate for existing root statement, not {@code null}
     * @param elementType class-type of OntList elements
     * @param <N>         {@link RDFNode} subtype
     * @return Stream of {@link OntList}s
     */
    public static <N extends RDFNode> Stream<OntList<N>> stream(OntGraphModelImpl model,
                                                                OntObject subject,
                                                                Property predicate,
                                                                Class<N> elementType) {
        return stream(model, subject, predicate, null, elementType);
    }

    /**
     * Lists all []-list resources in the form of {@link OntList OntList} for the given subject and predicate.
     *
     * @param model       {@link OntGraphModelImpl Ontology RDF Model Impl}, not {@code null}
     * @param subject     {@link OntObject} a subject for existing root statement, not {@code null}
     * @param predicate   {@link Property} a predicate for existing root statement, not {@code null}
     * @param listType    {@link Resource} list type, must be an URI-Resource or {@code null} for default []-list
     * @param elementType class-type of {@code OntList} elements
     * @param <N>         any subtype of {@link RDFNode}
     * @return Stream of {@link OntList}s
     */
    protected static <N extends RDFNode> Stream<OntList<N>> stream(OntGraphModelImpl model,
                                                                   OntObject subject,
                                                                   Property predicate,
                                                                   Resource listType,
                                                                   Class<N> elementType) {
        checkRequiredInput(model, subject, predicate, listType, elementType);
        return subject.objects(predicate, RDFList.class).map(list -> asOntList(list, model, subject, predicate, listType, elementType));
    }


    /**
     * Creates a typed []-list.
     * The example of a []-list with type {@code <type>} and elements {@code <A>}, {@code <B>}(turtle):
     * <pre>{@code
     * [ rdf:type   <type> ;
     *   rdf:first  <A> ;
     *   rdf:rest   [ rdf:type   <type> ;
     *                rdf:first  <B> ;
     *                rdf:rest   rdf:nil
     *              ]
     * ] .
     * }</pre>
     * or :
     * <pre>{@code
     * _:x rdf:type <type> .
     * _:x rdf:first <A> .
     * _:x rdf:rest _:y .
     * _:y rdf:type <type> .
     * _:y rdf:first <B> .
     * _:y rdf:rest rdf:nil .
     * }</pre>
     *
     * @param model   {@link EnhGraph} model, not {@code null}
     * @param type    {@link Resource} URI-Resource, not {@code null}
     * @param members {@link Iterator} of {@code RDFNode}, not {@code null}
     * @return a typed []-list in the form of {@link RDFList}
     */
    public static RDFList createTypedList(EnhGraph model, Resource type, Iterator<? extends RDFNode> members) {
        return new RDFListImpl(Node.ANY, model) {
            @Override
            public Resource listType() {
                return type;
            }

            @Override
            public RDFList copy() {
                return copy(members);
            }
        }.copy();
    }

    private static void checkRequiredInput(OntGraphModelImpl m,
                                           OntObject s,
                                           Property p,
                                           Resource listType,
                                           Class elementType) throws IllegalArgumentException, NullPointerException {
        Objects.requireNonNull(m, "Null model");
        Objects.requireNonNull(s, "Null subject");
        Objects.requireNonNull(p, "Null predicate");
        Objects.requireNonNull(elementType, "Null type");
        if (listType == null) {
            return;
        }
        if (!listType.isURIResource()) throw new IllegalArgumentException("List type must have URI");
    }

    private static ExtendedIterator<Resource> listAnnotations(OntGraphModelImpl m,
                                                              Resource subject,
                                                              Property predicate,
                                                              RDFNode obj) {
        return m.listAnnotations(OWL.Axiom, subject, predicate, obj);
    }

    /**
     * Answers {@code true} if the given {@link RDFNode RDF-Node} is nil []-list.
     *
     * @param list {@link RDFNode}, not {@code null}
     * @return boolean
     */
    public static boolean isNil(RDFNode list) {
        return RDF.nil.equals(list);
    }

    private static OntStatement createRDFFirst(OntGraphModelImpl m, List<Triple> batch) {
        return createListStatement(m, RDF.first, batch);
    }

    private static OntStatement createRDFRest(OntGraphModelImpl m, List<Triple> batch) {
        return createListStatement(m, RDF.rest, batch);
    }

    private static OntStatement createRDFType(OntGraphModelImpl m, List<Triple> batch, Resource type) {
        return createRDFType(m, batch, (n, g) -> g.getNodeAs(n, Resource.class), type);
    }

    private static OntStatement createRDFType(OntGraphModelImpl m,
                                              List<Triple> batch,
                                              BiFunction<Node, OntGraphModelImpl, Resource> subject,
                                              Resource type) {
        for (Triple t : batch) {
            if (!RDF.type.asNode().equals(t.getPredicate())) continue;
            if (!t.getObject().equals(type.asNode())) continue;
            return OntStatementImpl.createNotAnnotatedOntStatementImpl(subject.apply(t.getSubject(), m), RDF.type, type, m);
        }
        throw new OntJenaException.IllegalState("Can't find rdf:type=" + type + " triple in a batch " + batch);
    }

    private static OntStatement createListStatement(OntGraphModelImpl m, Property predicate, List<Triple> batch) {
        return createListStatement(m, (n, g) -> g.getNodeAs(n, Resource.class), predicate,
                (n, g) -> g.getNodeAs(n, RDFNode.class), batch);
    }

    private static OntStatement createListStatement(OntGraphModelImpl m,
                                                    BiFunction<Node, OntGraphModelImpl, Resource> subject,
                                                    Property predicate,
                                                    BiFunction<Node, OntGraphModelImpl, RDFNode> object,
                                                    List<Triple> batch) {
        for (Triple t : batch) {
            if (!predicate.asNode().equals(t.getPredicate())) continue;
            return OntStatementImpl.createNotAnnotatedOntStatementImpl(subject.apply(t.getSubject(), m),
                    predicate, object.apply(t.getObject(), m), m);
        }
        throw new OntJenaException.IllegalState("Can't find []-list triple with predicate " + predicate + " in a batch " + batch);
    }

    @Override
    public OntStatement getRoot() {
        return getModel().createStatement(subject, predicate, getRDFList());
    }

    @Override
    public Optional<Resource> type() {
        return Optional.ofNullable(listType);
    }

    @Override
    public OntGraphModelImpl getModel() {
        return (OntGraphModelImpl) enhGraph;
    }

    protected RDFList getRDFList() {
        return setRDFList(IDENTITY).objectRDFList;
    }

    protected OntListImpl<E> setRDFList(UnaryOperator<RDFList> operation) throws OntJenaException.IllegalState {
        OntGraphModelImpl m = getModel();
        RDFList list = Objects.requireNonNull(operation).apply(this.objectRDFList);
        Statement s = m.createStatement(subject, predicate, list);
        if (!m.contains(s)) {
            throw new OntJenaException.IllegalState(Models.toString(s) + " does not exist");
        }
        if (!objectRDFList.equals(list)) {
            listAnnotations(m, subject, predicate, objectRDFList).toSet()
                    .forEach(a -> m.remove(a, OWL.annotatedTarget, objectRDFList).add(a, OWL.annotatedTarget, list));
            m.getNodeCache().remove(objectRDFList.asNode());
        }
        this.objectRDFList = list;
        return this;
    }

    @Override
    public Node asNode() {
        return getRDFList().asNode();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends RDFNode> T as(Class<T> t) throws UnsupportedPolymorphismException {
        if (RDFList.class.equals(t))
            return (T) getRDFList();
        return super.as(t);
    }

    @Override
    public <X extends RDFNode> boolean canAs(Class<X> t) {
        return RDFList.class.equals(t) || super.canAs(t);
    }

    @Override
    public boolean isEmpty() {
        return isNil() || !Iter.findFirst(listMembers()).isPresent();
    }

    @Override
    public boolean isNil() {
        return isNil(getRDFList());
    }

    @Override
    public Stream<E> members() {
        return Iter.asStream(listMembers());
    }

    /**
     * Lists all elements of type {@link E} from this list.
     * Note: the list may contain nodes with incompatible type, in this case they will be skipped.
     *
     * @return {@link ExtendedIterator} of {@link E}-elements
     */
    public ExtendedIterator<E> listMembers() {
        Iterator<List<Triple>> it = createRDFListIterator();
        if (it == null) return NullIterator.instance();
        OntGraphModelImpl m = getModel();
        return WrappedIterator.create(it)
                .mapWith(l -> createRDFFirst(m, l))
                .mapWith(Statement::getObject)
                .filterKeep(this::isValid)
                .mapWith(this::cast);
    }

    @Override
    public final Stream<OntStatement> spec() {
        return Iter.asStream(listSpec());
    }

    public ExtendedIterator<OntStatement> listSpec() {
        RDFList list = getRDFList();
        if (isNil(list)) return NullIterator.instance();
        return Iter.flatMap(createSafeRDFListIterator(list.asNode()), this::toListStatements);
    }

    public ExtendedIterator<OntStatement> listContent() {
        return Iter.of(getRoot()).andThen(listSpec());
    }

    @Override
    public final Stream<OntStatement> content() {
        return Iter.asStream(listContent());
    }

    /**
     * Answers the {@code Iterator} of batches of triples that belong to this ONT-List.
     *
     * @return {@link Iterator} of {@link Triple} {@link List}s or {@code null} in case of empty for nil-list
     */
    public Iterator<List<Triple>> createRDFListIterator() {
        RDFList list = getRDFList();
        if (isNil(list)) {
            return null;
        }
        return createRDFListIterator(list.asNode());
    }

    protected ExtendedIterator<List<Triple>> createSafeRDFListIterator(Node list) {
        return WrappedIterator.create(new SafeRDFListIterator(getModel().getGraph(), list))
                .filterKeep(Objects::nonNull);
    }

    @SuppressWarnings("unused")
    protected Stream<List<Triple>> createRDFListStream(Node list) {
        return Iter.asStream(createRDFListIterator(list));
    }

    protected ExtendedIterator<List<Triple>> createRDFListIterator(Node list) {
        return WrappedIterator.create(new RDFListIterator(getModel().getGraph(), list));
    }

    protected ExtendedIterator<OntStatement> toListStatements(List<Triple> triples) {
        OntGraphModelImpl m = getModel();
        if (listType != null) {
            return Iter.of(createRDFType(m, triples, listType), createRDFFirst(m, triples), createRDFRest(m, triples));
        }
        return Iter.of(createRDFFirst(m, triples), createRDFRest(m, triples));
    }

    /**
     * Answers {@code true} if the given {@link RDFNode RDF-Node} is valid to be a typed element of this list.
     *
     * @param n {@link RDFNode}
     * @return boolean
     */
    public abstract boolean isValid(RDFNode n);

    /**
     * Makes an {@link E}-resource from the given {@link RDFNode RDF-Node}.
     *
     * @param n {@link RDFNode}
     * @return {@link RDFNode} of type {@link E}
     */
    public E cast(RDFNode n) {
        try {
            return getModel().getNodeAs(n.asNode(), elementType);
        } catch (OntJenaException.Conversion j) {
            throw new OntJenaException.IllegalState("Can't cast to " +
                    elementType.getSimpleName() + ". Problem node: '" + n + "'", j);
        }
    }

    @Override
    public OntList<E> addLast(E e) {
        return setRDFList(list -> {
            Statement last = getLastRestStatement();
            Statement s = last == null ? getRoot() : last;
            Model m = getModel();
            Resource r = m.createResource();
            if (listType != null) {
                m.add(r, RDF.type, listType);
            }
            m.add(s.getSubject(), s.getPredicate(), r).add(r, RDF.first, e).add(r, RDF.rest, RDF.nil).remove(s);
            return last == null ? r.as(RDFList.class) : list;
        });
    }

    @Override
    public OntList<E> addFirst(E e) throws PropertyNotFoundException {
        return setRDFList(list -> {
            Statement first = getFirstRestStatement();
            Statement root = getRoot();
            Statement s = first == null ? root : first;
            Model m = getModel();
            Resource r = m.createResource();
            if (listType != null) {
                m.add(r, RDF.type, listType);
            }
            m.add(r, RDF.first, e).add(r, RDF.rest,
                    first == null ? RDF.nil : s.getSubject())
                    .add(subject, predicate, r).remove(root);
            return r.as(RDFList.class);
        });
    }

    @Override
    public OntList<E> removeLast() {
        return setRDFList(list -> {
            List<Statement> stmps = getLastTwoRestStatements();
            if (stmps == null) return list;
            OntGraphModel m = getModel();
            Resource last = stmps.get(stmps.size() - 1).getSubject();
            Statement prev = stmps.size() == 1 ? getRoot() : stmps.get(0);
            m.add(prev.getSubject(), prev.getPredicate(), RDF.nil).removeAll(last, null, null).remove(prev);
            return stmps.size() == 1 ? RDF.nil.inModel(m).as(RDFList.class) : list;
        });
    }

    @Override
    public OntList<E> removeFirst() throws PropertyNotFoundException {
        return setRDFList(list -> {
            List<Statement> stmps = getFirstTwoRestStatements();
            if (stmps == null) return list;
            OntGraphModel m = getModel();
            Statement root = getRoot();
            Resource first = stmps.get(0).getSubject();
            Resource next = stmps.size() == 1 ? RDF.nil.inModel(m) : stmps.get(1).getSubject();
            m.add(root.getSubject(), root.getPredicate(), next).removeAll(first, null, null).remove(root);
            return next.as(RDFList.class);
        });
    }

    @Override
    public OntList<E> clear() {
        return setRDFList(list -> {
            Model m = getModel();
            RDFList res = RDF.nil.inModel(m).as(RDFList.class);
            Iterator<List<Triple>> it = createRDFListIterator();
            if (it == null) return res;
            Statement root = getRoot();
            if (!it.hasNext())
                throw new OntJenaException.IllegalState("The list " + this + " does not contain any items.");
            Graph g = m.getGraph();
            do {
                it.next().forEach(g::delete);
            } while (it.hasNext());
            m.remove(root).add(root.getSubject(), root.getPredicate(), res);
            return res;
        });
    }

    /**
     * Finds the first statement with predicate {@link RDF#rest rdf:rest} from this ONT-List.
     *
     * @return {@link Statement} or {@code null} in case of nil-list
     */
    public Statement getFirstRestStatement() {
        Iterator<List<Triple>> it = createRDFListIterator();
        if (it == null) return null;
        if (!it.hasNext()) throw new OntJenaException.IllegalState("Can't find any []-list batch in the list " + this);
        return getRestStatement(it.next());
    }

    /**
     * Finds the last statement with predicate {@link RDF#rest rdf:rest} from this ONT-List.
     *
     * @return {@link Statement} or {@code null} in case of nil-list
     */
    public Statement getLastRestStatement() {
        Iterator<List<Triple>> it = createRDFListIterator();
        if (it == null) return null;
        if (!it.hasNext()) throw new OntJenaException.IllegalState("Can't find any []-list batch in the list " + this);
        List<Triple> res;
        do {
            res = it.next();
        } while (it.hasNext());
        return getRestStatement(res);
    }

    /**
     * Finds the first two statements with predicate {@link RDF#rest rdf:rest} from this ONT-List.
     *
     * @return {@link List} that contains two or one {@link Statement}s or {@code null} in case of nil-list
     */
    public List<Statement> getFirstTwoRestStatements() {
        Iterator<List<Triple>> it = createRDFListIterator();
        if (it == null) return null;
        if (!it.hasNext()) throw new OntJenaException.IllegalState("Can't find any []-list batch in the list " + this);
        List<Statement> res = new ArrayList<>();
        res.add(getRestStatement(it.next()));
        if (it.hasNext()) {
            res.add(getRestStatement(it.next()));
        }
        return res;
    }

    /**
     * Finds the last two statements with predicate {@link RDF#rest rdf:rest} from this ONT-List.
     *
     * @return {@link List} that contains two or one {@link Statement}s or {@code null} in case of nil-list
     */
    public List<Statement> getLastTwoRestStatements() {
        Iterator<List<Triple>> it = createRDFListIterator();
        if (it == null) return null;
        List<Triple> prev = null;
        List<Triple> last = null;
        while (it.hasNext()) {
            prev = last;
            last = it.next();
        }
        if (last == null) throw new OntJenaException.IllegalState("Can't find last []-list batch in the list " + this);
        return Stream.of(prev, last).filter(Objects::nonNull).map(this::getRestStatement).collect(Collectors.toList());
    }

    private Statement getRestStatement(List<Triple> triples) {
        OntGraphModel m = getModel();
        return triples.stream().filter(s -> RDF.rest.asNode().equals(s.getPredicate()))
                .map(m::asStatement).findFirst()
                .orElseThrow(() -> new OntJenaException.IllegalState("Can't find rdf:rest in the batch " + triples));
    }

    @Override
    public OntList<E> get(int index) throws PropertyNotFoundException, OntJenaException.IllegalArgument {
        if (index < 0) throw new OntJenaException.IllegalArgument("Negative index: " + index);
        if (index == 0) return this;
        RDFList list = getRDFList();
        int i = 0;
        OntGraphModelImpl m = getModel();
        while (!isNil(list)) {
            Statement rest = list.getRequiredProperty(RDF.rest);
            list = rest.getObject().as(RDFList.class);
            if (++i != index) {
                continue;
            }
            return new OntListImpl<E>(rest.getSubject(), rest.getPredicate(), list, listType, m, elementType) {
                @Override
                public OntStatement getRoot() {
                    return OntStatementImpl.createNotAnnotatedOntStatementImpl(subject, predicate, getRDFList(), getModel());
                }

                @Override
                public boolean isValid(RDFNode n) {
                    return OntListImpl.this.isValid(n);
                }
            };
        }
        throw new OntJenaException.IllegalArgument("Index out of bounds: " + index);
    }

    /**
     * An extended {@link RDFListIterator} whose {@link Iterator#next()} method does not throw {@link NoSuchElementException}
     * in case no element found, but returns {@code null} instead.
     * It is important to have safe iterator since it can be cached somewhere by ONT-API.
     *
     * @see RDFListImpl#removeList()
     */
    public static class SafeRDFListIterator extends RDFListIterator {

        public SafeRDFListIterator(Graph graph, Node head) {
            super(graph, head);
        }

        @Override
        public List<Triple> next() {
            try {
                return super.next();
            } catch (NoSuchElementException n) {
                return null;
            }
        }
    }

    /**
     * A simplest {@link Iterator iterator} over a {@link RDF#List rdf:List},
     * whose {@link Iterator#next()} method returns
     * a batch of {@link Triple triple}s in the form of standard {@link List Java List}.
     */
    public static class RDFListIterator implements Iterator<List<Triple>> {
        public static final Node REST = RDF.rest.asNode();
        public static final Node NIL = RDF.nil.asNode();
        private final Graph graph;
        private Node head;

        public RDFListIterator(Graph graph, Node head) {
            this.graph = Objects.requireNonNull(graph);
            this.head = Objects.requireNonNull(head);
        }

        @Override
        public boolean hasNext() {
            return head != null && !NIL.equals(head);
        }

        @Override
        public List<Triple> next() throws NoSuchElementException {
            Node next = null;
            List<Triple> triples = new ArrayList<>();
            Iterator<Triple> it = graph.find(head, Node.ANY, Node.ANY);
            while (it.hasNext()) {
                Triple t = it.next();
                triples.add(t);
                if (REST.equals(t.getPredicate())) {
                    next = t.getObject();
                }
            }
            this.head = next;
            if (next == null) {
                throw new NoSuchElementException("No element found: " + triples);
            }
            return triples;
        }
    }

}