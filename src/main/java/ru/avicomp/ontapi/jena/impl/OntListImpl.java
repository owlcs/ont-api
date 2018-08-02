/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2018, Avicomp Services, AO
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
 * TODO:
 * Created by @szuev on 10.07.2018.
 */
@SuppressWarnings("WeakerAccess")
public abstract class OntListImpl<E extends RDFNode> extends ResourceImpl implements OntList<E> {
    private static final UnaryOperator<RDFList> IDENTITY = UnaryOperator.identity();

    protected final Resource subject;
    protected final Property predicate;
    protected final Class<E> elementType;
    private RDFList objectRDFList;

    protected OntListImpl(Resource subject, Property predicate, RDFList object, OntGraphModel model, Class<E> type) {
        super(object.asNode(), (EnhGraph) model);
        this.objectRDFList = object;
        this.subject = subject;
        this.predicate = predicate;
        this.elementType = type;
    }

    /**
     * Creates a fresh OntList based on the given {@code elementType}
     * containing all content from the specified collection preserving the original order.
     * The resulting list will be attached to the model by the given {@code subject} and {@code predicate}.
     *
     * @param model       {@link OntGraphModelImpl}
     * @param subject     {@link OntObject}
     * @param predicate   {@link Property}
     * @param elementType Class-type of OntList elements
     * @param elements    {@link Collection} of elements to be added to the new rdf-list
     * @param <N>         {@link RDFNode} subtype
     * @return a fresh {@link OntList} instance
     */
    public static <N extends RDFNode> OntList<N> create(OntGraphModelImpl model,
                                                        OntObject subject,
                                                        Property predicate,
                                                        Class<N> elementType,
                                                        Collection<N> elements) {
        return create(model, subject, predicate, elementType, Objects.requireNonNull(elements, "Null elements collection").iterator());
    }

    /**
     * Creates a fresh OntList using {@code Iterator} and other parameters.
     *
     * @param model     {@link OntGraphModelImpl}
     * @param subject   {@link OntObject}
     * @param predicate {@link Property}
     * @param type      class-type of OntList elements
     * @param elements  {@link Iterator} of elements to be added to the new rdf-list
     * @param <N>       {@link RDFNode} subtype
     * @return a fresh {@link OntList} instance
     */
    protected static <N extends RDFNode> OntList<N> create(OntGraphModelImpl model,
                                                           OntObject subject,
                                                           Property predicate,
                                                           Class<N> type,
                                                           Iterator<N> elements) {
        checkRequiredInput(model, subject, predicate, type);
        RDFList list = model.createList(elements);
        model.add(subject, predicate, list);
        return new OntListImpl<N>(subject, predicate, list, model, type) {
            @Override
            public boolean isValid(RDFNode n) {
                return true;
            }
        };
    }

    /**
     * Wraps the given RDFList as OntList.
     *
     * @param model     {@link OntGraphModelImpl}
     * @param subject   {@link OntObject}
     * @param predicate {@link Property}
     * @param list      {@link RDFList}
     * @param type      class-type of OntList elements
     * @param <N>       {@link RDFNode} subtype
     * @return a fresh {@link OntList} instance
     */
    protected static <N extends RDFNode> OntList<N> newOntList(OntGraphModelImpl model,
                                                               OntObject subject,
                                                               Property predicate,
                                                               RDFList list,
                                                               Class<N> type) {
        return new OntListImpl<N>(subject, predicate, list, model, type) {
            @Override
            public boolean isValid(RDFNode n) {
                return n.canAs(elementType);
            }
        };
    }

    /**
     * Lists all rdf-lists by subject and predicate in form of ont-lists.
     *
     * @param model     {@link OntGraphModelImpl}
     * @param subject   {@link OntObject}
     * @param predicate {@link Property}
     * @param type      class-type of OntList elements
     * @param <N>       {@link RDFNode} subtype
     * @return Stream of {@link OntList}s
     */
    public static <N extends RDFNode> Stream<OntList<N>> stream(OntGraphModelImpl model,
                                                                OntObject subject,
                                                                Property predicate,
                                                                Class<N> type) {
        checkRequiredInput(model, subject, predicate, type);
        return subject.objects(predicate, RDFList.class).map(list -> newOntList(model, subject, predicate, list, type));
    }

    private static void checkRequiredInput(OntGraphModelImpl model, OntObject subject, Property predicate, Class type) {
        Objects.requireNonNull(model, "Null model");
        Objects.requireNonNull(subject, "Null subject");
        Objects.requireNonNull(predicate, "Null predicate");
        Objects.requireNonNull(type, "Null type");
    }

    private static Stream<Resource> listAnnotations(Model m, Resource subject, Property predicate, RDFNode obj) {
        return OntStatementImpl.listAnnotations(m, OWL.Axiom, subject, predicate, obj);
    }

    public static boolean isEmpty(RDFNode list) {
        return RDF.nil.equals(list);
    }

    private static OntStatement createRDFFirst(OntGraphModelImpl m, List<Triple> batch) {
        return createListStatement(m, RDF.first, batch);
    }

    private static OntStatement createRDFRest(OntGraphModelImpl m, List<Triple> batch) {
        return createListStatement(m, RDF.rest, batch);
    }

    private static OntStatement createListStatement(OntGraphModelImpl m, Property predicate, List<Triple> batch) {
        return createListStatement(m, (n, g) -> g.getNodeAs(n, Resource.class), predicate, (n, g) -> g.getNodeAs(n, RDFNode.class), batch);
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
        throw new OntJenaException.IllegalState("Can't find rdf:List triple with predicate " + predicate);
    }

    @Override
    public OntStatement getRoot() {
        return getModel().createStatement(subject, predicate, getRDFList());
    }

    @Override
    public OntGraphModelImpl getModel() {
        return (OntGraphModelImpl) enhGraph;
    }

    protected RDFList getRDFList() {
        return setRDFList(IDENTITY).objectRDFList;
    }

    protected OntListImpl<E> setRDFList(UnaryOperator<RDFList> operation) throws OntJenaException.IllegalState {
        RDFList list = Objects.requireNonNull(operation).apply(this.objectRDFList);
        Model m = getModel();
        Statement s = m.createStatement(subject, predicate, list);
        if (!m.contains(s)) {
            throw new OntJenaException.IllegalState(Models.toString(s) + " does not exist");
        }
        if (!objectRDFList.equals(list)) {
            listAnnotations(m, subject, predicate, objectRDFList)
                    .collect(Collectors.toSet())
                    .forEach(a -> m.remove(a, OWL.annotatedTarget, objectRDFList).add(a, OWL.annotatedTarget, list));
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
        return isEmpty(getRDFList());
    }

    @Override
    public Stream<E> members() {
        return spec().filter(s -> Objects.equals(s.getPredicate(), RDF.first))
                .map(Statement::getObject)
                .filter(this::isValid)
                .map(this::cast);
    }

    @Override
    public Stream<OntStatement> spec() {
        RDFListImpl list = ((RDFListImpl) getRDFList());
        if (isEmpty(list)) return Stream.empty();
        OntGraphModelImpl m = getModel();
        return Iter.asStream(WrappedIterator.create(new RDFListIterator(getModel().getGraph(), list.asNode())))
                .flatMap(t -> Stream.of(createRDFFirst(m, t), createRDFRest(m, t)));
    }

    public abstract boolean isValid(RDFNode n);

    public E cast(RDFNode n) {
        try {
            return getModel().getNodeAs(n.asNode(), elementType);
        } catch (OntJenaException.Conversion j) {
            throw new OntJenaException.IllegalState("Problem node: '" + n + "'", j);
        }
    }

    @Override
    public OntList<E> add(E e) {
        return setRDFList(list -> {
            if (isEmpty(list)) {
                RDFList tmp = list.with(e);
                getModel().remove(subject, predicate, RDF.nil).add(subject, predicate, tmp);
                return tmp;
            }
            list.add(e);
            return list;
        });
    }

    @Override
    public OntList<E> remove() {
        return setRDFList(list -> {
            if (isEmpty(list)) return list;
            Model model = getModel();
            RDFList tmp = list;
            StmtIterator last = null;
            StmtIterator prev;
            do {
                prev = last;
                last = tmp.listProperties();
                tmp = tmp.getTail();
            } while (!tmp.isEmpty());
            model.remove(last);
            if (prev == null) { // rdf:nil
                model.remove(subject, predicate, list).add(subject, predicate, list = model.createList());
                return list;
            }
            Statement st = Iter.asStream(prev)
                    .filter(s -> RDF.rest.equals(s.getPredicate()))
                    .findFirst().orElseThrow(() -> new OntJenaException("Illegal state of " + this + ": can't find rdf:rest"));
            model.remove(st).add(st.getSubject(), st.getPredicate(), RDF.nil);
            return list;
        });
    }

    @Override
    public OntList<E> addFirst(E e) throws PropertyNotFoundException {
        return setRDFList(list -> {
            Model model = getModel();
            RDFList res;
            if (isEmpty(list)) {
                res = list.with(e);
                model.remove(subject, predicate, RDF.nil).add(subject, predicate, res);
                return res;
            }
            res = model.createList(new RDFNode[]{e});
            Statement rest = res.getRequiredProperty(RDF.rest);
            model.remove(rest).remove(subject, predicate, list)
                    .add(rest.getSubject(), rest.getPredicate(), list).add(subject, predicate, res);
            return res;
        });
    }

    @Override
    public OntList<E> removeFirst() throws PropertyNotFoundException {
        return setRDFList(list -> {
            if (isEmpty(list)) return list;
            Model model = getModel();
            Statement rest = list.getRequiredProperty(RDF.rest);
            Statement first = list.getRequiredProperty(RDF.first);
            model.remove(first).remove(rest).remove(subject, predicate, list).add(subject, predicate, rest.getObject());
            return rest.getObject().as(RDFList.class);
        });
    }

    @Override
    public OntList<E> clear() {
        return setRDFList(list -> {
            if (isEmpty(list)) return list;
            Model model = getModel();
            list.removeList();
            RDFList res;
            model.remove(subject, predicate, list).add(subject, predicate, res = model.createList());
            return res;
        });
    }

    @Override
    public OntList<E> get(int index) throws PropertyNotFoundException, OntJenaException.IllegalArgument {
        if (index < 0) throw new OntJenaException.IllegalArgument("Negative index: " + index);
        if (index == 0) return this;
        RDFList list = getRDFList();
        int i = 0;
        OntGraphModelImpl m = getModel();
        while (!isEmpty(list)) {
            Statement rest = list.getRequiredProperty(RDF.rest);
            list = rest.getObject().as(RDFList.class);
            if (++i != index) {
                continue;
            }
            return new OntListImpl<E>(rest.getSubject(), rest.getPredicate(), list, m, elementType) {
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
     * A simplest {@link Iterator iterator} over a {@link RDF#List rdf:List},
     * whose {@code next} method returns a batch of {@link Triple triple}s in the form of standard {@link List Java List}.
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
            return !NIL.equals(head);
        }

        @Override
        public List<Triple> next() {
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
            if (next == null) throw new NoSuchElementException();
            this.head = next;
            return triples;
        }
    }

}