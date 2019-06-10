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

package ru.avicomp.ontapi.jena.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.RDFListImpl;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.util.NodeUtils;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.util.iterator.WrappedIterator;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.UnionGraph;
import ru.avicomp.ontapi.jena.impl.*;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A class-helper to work with {@link Model Jena Model}s (mainly with {@link ru.avicomp.ontapi.jena.model.OntGraphModel})
 * and its related objects: {@link Resource Jena Resource}, {@link Statement Jena Statement} and {@link OntStatement Ontology Statement}.
 * <p>
 * Created by szuev on 20.10.2016.
 */
@SuppressWarnings({"WeakerAccess", "UnusedReturnValue", "unused"})
public class Models {
    public static final Comparator<RDFNode> RDF_NODE_COMPARATOR = (r1, r2) -> NodeUtils.compareRDFTerms(r1.asNode(), r2.asNode());
    public static final Comparator<Statement> STATEMENT_COMPARATOR = Comparator
            .comparing(Statement::getSubject, RDF_NODE_COMPARATOR)
            .thenComparing(Statement::getPredicate, RDF_NODE_COMPARATOR)
            .thenComparing(Statement::getObject, RDF_NODE_COMPARATOR);
    public static final RDFNode BLANK = new ResourceImpl();
    public static final Comparator<Statement> STATEMENT_COMPARATOR_IGNORE_BLANK = Comparator
            .comparing((Function<Statement, RDFNode>) s -> s.getSubject().isAnon() ? BLANK : s.getSubject(), RDF_NODE_COMPARATOR)
            .thenComparing(s -> s.getPredicate().isAnon() ? BLANK : s.getPredicate(), RDF_NODE_COMPARATOR)
            .thenComparing(s -> s.getObject().isAnon() ? BLANK : s.getObject(), RDF_NODE_COMPARATOR);

    public static final Literal TRUE = ResourceFactory.createTypedLiteral(Boolean.TRUE);
    public static final Literal FALSE = ResourceFactory.createTypedLiteral(Boolean.FALSE);

    /**
     * Creates a typed []-list with the given type containing the resources from the given given collection.
     *
     * @param model   {@link Model model} in which the []-list is created
     * @param type    {@link Resource} the type for new []-list
     * @param members Collection of {@link RDFNode}s
     * @return anonymous resource - the header of the typed []-list
     * @see ru.avicomp.ontapi.jena.model.OntList
     */
    public static RDFList createTypedList(Model model, Resource type, Collection<? extends RDFNode> members) {
        return createTypedList(model, type, members.iterator());
    }

    /**
     * Creates a typed list with the given type containing the resources from the given given iterator.
     * A typed list is an anonymous resource that is created using the same rules as the standard {@link RDFList []-list}
     * (that is, using {@link RDF#first rdf:first}, {@link RDF#rest rdf:rest} and {@link RDF#nil rdf:nil} predicates),
     * but each item of this []-list has the specified type on predicate {@link RDF#type rdf:type}.
     *
     * @param model   {@link Model model} in which the []-list is created
     * @param type    {@link Resource} the type for new []-list
     * @param members {@link Iterator} of {@link RDFNode}s
     * @return anonymous resource - the header of the typed []-list
     * @see ru.avicomp.ontapi.jena.model.OntList
     */
    public static RDFList createTypedList(Model model, Resource type, Iterator<? extends RDFNode> members) {
        return OntListImpl.createTypedList((EnhGraph) model, type, members);
    }

    /**
     * Determines is s specified resource belongs to a list.
     *
     * @param model     Model
     * @param candidate Resource to test
     * @return true if specified resource is a member of some rdf:List
     */
    public static boolean isInList(Model model, Resource candidate) {
        return model.contains(null, RDF.first, candidate);
    }

    /**
     * Answers {@code true} if the given statement belongs to some []-list.
     *
     * @param s {@link Statement}, not null
     * @return boolean
     * @since 1.3.0
     */
    public static boolean isInList(Statement s) {
        return RDF.first.equals(s.getPredicate()) || RDF.rest.equals(s.getPredicate()) || RDF.nil.equals(s.getObject());
    }

    /**
     * Answers a set of all of the RDF statements whose subject is one of the cells of the given list.
     *
     * @param list []-list, not {@code null}
     * @return Set of {@link Statement}s
     * @since 1.4.0
     */
    public static Set<Statement> getListStatements(RDFList list) {
        return ((RDFListImpl) list).collectStatements();
    }

    /**
     * Converts rdf-node to anonymous individual.
     * The result anonymous individual could be "true" (instance of some owl class) or "fake"
     * (any blank node can be represented as it).
     *
     * @param node {@link RDFNode}
     * @return {@link OntIndividual.Anonymous}
     * @throws OntJenaException if the node cannot be present as anonymous individual
     */
    public static OntIndividual.Anonymous asAnonymousIndividual(RDFNode node) {
        return OntIndividualImpl.createAnonymousIndividual(node);
    }

    /**
     * Replaces namespaces map with new one.
     *
     * @param mapping  {@link PrefixMapping Prefix Mapping} to modify
     * @param prefixes java Map of new prefixes to set
     * @return java Map of previously associated prefixes
     */
    public static Map<String, String> setNsPrefixes(PrefixMapping mapping, Map<String, String> prefixes) {
        Map<String, String> init = mapping.getNsPrefixMap();
        init.keySet().forEach(mapping::removeNsPrefix);
        prefixes.forEach((p, u) -> mapping.setNsPrefix(p.replaceAll(":$", ""), u));
        return init;
    }

    /**
     * Lists all literal string values (lexical forms) with the given language tag
     * for the specified subject and predicate.
     *
     * @param subject   {@link Resource}, not {@code null}
     * @param predicate {@link Property}, can be {@code null}
     * @param lang      String lang, maybe {@code null} or empty
     * @return Stream of Strings
     * @since 1.3.0
     */
    public static Stream<String> langValues(Resource subject, Property predicate, String lang) {
        return Iter.asStream(subject.listProperties(predicate)
                .mapWith(s -> {
                    if (!s.getObject().isLiteral())
                        return null;
                    if (!filterByLangTag(s.getLiteral(), lang))
                        return null;
                    return s.getString();
                })
                .filterDrop(Objects::isNull));
    }

    /**
     * Answers {@code true} if the literal has the given language tag.
     * The comparison is case insensitive and ignores trailing spaces,
     * so two tags {@code  en } and {@code En} are considered as equaled.
     *
     * @param literal {@link Literal}, not {@code null}
     * @param tag     String, possible {@code null}
     * @return {@code true} if the given literal has the given tag
     * @since 1.4.1
     */
    public static boolean filterByLangTag(Literal literal, String tag) {
        String other = literal.getLanguage();
        if (StringUtils.isEmpty(tag))
            return StringUtils.isEmpty(other);
        return tag.trim().equalsIgnoreCase(other);
    }

    /**
     * Recursively deletes all resource children.
     *
     * @param inModel Resource from a model
     * @since 1.3.0
     */
    public static void deleteAll(Resource inModel) {
        deleteAll(inModel, new HashSet<>());
    }

    private static void deleteAll(Resource r, Set<Node> viewed) {
        if (!viewed.add(r.asNode())) {
            return;
        }
        r.listProperties().toSet().forEach(s -> {
            RDFNode o = s.getObject();
            if (o.isAnon()) {
                deleteAll(o.asResource(), viewed);
            }
            r.getModel().remove(s);
        });
    }

    /**
     * Recursively gets all statements related to the specified subject.
     * Note: {@code rdf:List} may content a large number of members (1000+).
     *
     * @param inModel Resource with associated model inside.
     * @return the Set of {@link Statement}
     */
    public static Set<Statement> getAssociatedStatements(Resource inModel) {
        Set<Statement> res = new HashSet<>();
        calcAssociatedStatements(inModel, res);
        return res;
    }

    private static void calcAssociatedStatements(Resource root, Set<Statement> res) {
        if (root.canAs(RDFList.class)) {
            RDFList list = root.as(RDFList.class);
            if (list.isEmpty()) return;
            getListStatements(list).forEach(statement -> {
                res.add(statement);
                if (!RDF.first.equals(statement.getPredicate())) return;
                RDFNode obj = statement.getObject();
                if (obj.isAnon())
                    calcAssociatedStatements(obj.asResource(), res);
            });
            return;
        }
        root.listProperties().forEachRemaining(statement -> {
            try {
                if (!statement.getObject().isAnon() ||
                        res.stream().anyMatch(s -> statement.getObject().equals(s.getSubject()))) // to avoid cycles
                    return;
                calcAssociatedStatements(statement.getObject().asResource(), res);
            } finally {
                res.add(statement);
            }
        });
    }

    /**
     * Recursively lists all statements for the specified subject (rdf-node).
     * Note: there is a possibility of StackOverflowError in case graph contains a recursion.
     *
     * @param subject {@link RDFNode}, nullable
     * @return Stream of {@link Statement}s
     * @throws StackOverflowError in case graph contains recursion
     * @see Models#getAssociatedStatements(Resource)
     */
    public static Stream<Statement> listProperties(RDFNode subject) {
        if (subject == null || !subject.isAnon()) return Stream.empty();
        return Iter.asStream(subject.asResource().listProperties())
                .flatMap(s -> s.getObject().isAnon() ? listProperties(s.getObject().asResource()) : Stream.of(s));
    }

    /**
     * Recursively lists all parent resources (subjects) for the specified object (rdf-node).
     * Note: a possibility of StackOverflowError in case the graph contains a recursion.
     *
     * @param object {@link RDFNode}, not null
     * @return Stream of {@link Resource}s
     * @throws StackOverflowError in case graph contains recursion
     */
    public static Stream<Resource> listSubjects(RDFNode object) {
        return subjects(object).flatMap(s -> {
            Stream<Resource> r = Stream.of(s);
            return s.isAnon() ? Stream.concat(r, listSubjects(s)) : r;
        });
    }

    /**
     * Lists all direct subjects for the given object.
     *
     * @param inModel {@link RDFNode}
     * @return Stream of {@link Resource}s.
     */
    public static Stream<Resource> subjects(RDFNode inModel) {
        return Iter.asStream(inModel.getModel().listResourcesWithProperty(null, inModel));
    }

    /**
     * Splits the statement into several equivalent ones but with disjoint annotations.
     * Each of the returned statements is equal to the given, the difference is only in the related annotations.
     * <p>
     * This method can be used in case there are several typed b-nodes for each annotation assertions instead of a single one.
     * Such situation is not canonical way and should not be widely used, since it is redundant.
     * So usually the result stream contains only a single element: the same {@code OntStatement} instance as the input.
     * <p>
     * The following code demonstrates that non-canonical way of writing annotations with two or more b-nodes:
     * <pre>{@code
     * s A t .
     * _:b0  a                     owl:Axiom .
     * _:b0  A1                    t1 .
     * _:b0  owl:annotatedSource   s .
     * _:b0  owl:annotatedProperty A .
     * _:b0  owl:annotatedTarget   t .
     * _:b1  a                     owl:Axiom .
     * _:b1  A2                    t2 .
     * _:b1  owl:annotatedSource   s .
     * _:b1  owl:annotatedProperty A .
     * _:b1  owl:annotatedTarget   t .
     * }</pre>
     * Here the statement {@code s A t} has two annotations,
     * but they are spread over different resources (statements {@code _:b0 A1 t1} and {@code _:b1 A2 t2}).
     * For this example, the method returns stream of two {@code OntStatement}s, and each of them has only one annotation.
     * For generality, below is an example of the correct and equivalent way to write these annotations,
     * which is the preferred since it is more compact:
     * <pre>{@code
     * s A t .
     * [ a                      owl:Axiom ;
     * A1                     t1 ;
     * A2                     t2 ;
     * owl:annotatedProperty  A ;
     * owl:annotatedSource    s ;
     * owl:annotatedTarget    t
     * ]  .
     * }</pre>
     *
     * @param statement {@link OntStatement} the statement to split
     * @return Stream of {@link OntStatement ont-statements}, not empty,
     * each element equals to the input statement but has different related annotations
     */
    public static Stream<OntStatement> split(OntStatement statement) {
        return Iter.asStream(listSplitStatements(statement));
    }

    /**
     * Lists split statements.
     * See description for the {@link #split(OntStatement)} method.
     *
     * @param statement {@link OntStatement}, not {@code null}
     * @return {@link ExtendedIterator} of {@link OntStatement}s
     * @since 1.3.0
     */
    public static ExtendedIterator<OntStatement> listSplitStatements(OntStatement statement) {
        return ((OntStatementImpl) statement).listSplitStatements();
    }

    /**
     * Creates a read-only wrapper for the given {@link OntStatement Ontology Statement} with in-memory caches.
     * This wrapper can be used to minimize graph access and speed-up the annotation calculations.
     * If there are many tree-like annotations in the ontology model, this speed-up may be noticeable.
     *
     * @param delegate {@link OntStatement}
     * @return {@link OntStatement}
     */
    public static OntStatement createCachedStatement(OntStatement delegate) {
        return OntStatementImpl.createCachedOntStatementImpl(delegate);
    }

    /**
     * Returns a string representation of the given Jena statement taking into account PrefixMapping.
     *
     * @param st {@link Statement}, not null
     * @param pm {@link PrefixMapping}, not null
     * @return String
     * @since 1.3.0
     */
    public static String toString(Statement st, PrefixMapping pm) {
        return String.format("[%s, %s, %s]",
                st.getSubject().asNode().toString(pm, false),
                st.getPredicate().asNode().toString(pm, false),
                st.getObject().asNode().toString(pm, true));
    }

    /**
     * Returns a string representation of the given Jena statement.
     *
     * @param inModel {@link Statement}, not null
     * @return String
     * @since 1.3.0
     */
    public static String toString(Statement inModel) {
        return toString(inModel, inModel.getModel());
    }

    /**
     * Inserts the given ontology in the dependencies of each ontology from the specified collection,
     * provided as {@code Supplier} (the {@code manager} parameter).
     * Can be used to fix missed graph links or
     * to replace existing dependency with the new one in case {@code replace} is {@code true}.
     *
     * @param manager the collection of other ontologies in form of {@link Supplier} that answers a {@code Stream}
     * @param ont     {@link OntGraphModel} the ontology to insert, must be named
     * @param replace if {@code true} then any existing graph,
     *                that is linked through the {@code owl:import} declaration,
     *                will be replaced with the given graph,
     *                otherwise the graph will be inserted only if
     *                there is a declaration {@code owl:import} without any graph associated
     * @see OntID#getImportsIRI()
     * @since 1.3.0
     */
    public static void insert(Supplier<Stream<OntGraphModel>> manager, OntGraphModel ont, boolean replace) {
        String uri = Objects.requireNonNull(ont.getID().getImportsIRI(), "Must be named ontology");
        manager.get()
                .filter(m -> {
                    // select only those, that have the uri in owl:imports:
                    try (Stream<String> uris = m.getID().imports()) {
                        return uris.anyMatch(uri::equals);
                    }
                })
                .peek(m -> {
                    if (!replace) return;
                    // remove a first found previously associated graph:
                    m.imports()
                            .filter(i -> uri.equals(i.getID().getImportsIRI()))
                            .findFirst()
                            .ifPresent(i -> ((UnionGraph) m.getGraph()).removeGraph(i.getGraph()));
                })
                .filter(m -> m.imports().map(OntGraphModel::getID).map(OntID::getImportsIRI).noneMatch(uri::equals))
                .forEach(m -> m.addImport(ont));
    }

    /**
     * Recursively lists all models that are associated with the given model in the form of a flat stream.
     * In normal situation, each of the models must have {@code owl:imports} statement in the overlying graph.
     *
     * @param m {@link OntGraphModel}
     * @return Stream of models, not empty (contains at least the input model)
     * @throws StackOverflowError in case the given model has a recursion in the hierarchy
     * @see Graphs#flat(Graph)
     * @since 1.3.0
     */
    public static Stream<OntGraphModel> flat(OntGraphModel m) {
        return Stream.concat(Stream.of(m), m.imports().flatMap(Models::flat));
    }

    /**
     * Synchronizes the import declarations with the graph hierarchy.
     * Underling graph tree may content named graphs which are not included to the {@code owl:imports} declaration.
     * This method tries to fix such situation by modifying base graph.
     *
     * @param m {@link OntGraphModel}, not {@code null}
     * @throws StackOverflowError in case the given model has a recursion in the hierarchy
     * @see Graphs#importsTreeAsString(Graph)
     * @since 1.3.2
     */
    public static void syncImports(OntGraphModel m) {
        OntID id = m.getID();
        id.removeAll(OWL.imports);
        m.imports()
                .peek(Models::syncImports)
                .map(OntGraphModel::getID)
                .filter(Resource::isURIResource)
                .map(OntID::getImportsIRI)
                .forEach(id::addImport);
    }

    /**
     * Recursively lists all annotations for the given {@link OntStatement Ontology Statement}
     * in the form of a flat stream.
     *
     * @param st {@link OntStatement}
     * @return Stream of {@link OntStatement}s, each of them is annotation property assertion
     * @since 1.3.0
     */
    public static Stream<OntStatement> flat(OntStatement st) {
        return Stream.concat(st.annotations(), st.annotations().flatMap(Models::flat));
    }

    /**
     * Determines the actual ontology object type.
     *
     * @param object instance of {@link O}
     * @param <O>    any subtype of {@link OntObject}
     * @return {@link Class}-type of {@link O}
     * @since 1.4.1
     */
    @SuppressWarnings("unchecked")
    public static <O extends OntObject> Class<O> getOntType(O object) {
        Class<O> res;
        if (object instanceof OntObjectImpl) {
            res = (Class<O>) ((OntObjectImpl) object).getActualClass();
        } else {
            res = (Class<O>) OntObjectImpl.findActualClass(object);
        }
        return OntJenaException.notNull(res, "Can't determine the type of object " + object);
    }

    /**
     * Lists all model statements, which belong to the base graph, using the given SPO.
     * <p>
     * It is placed here because there is no certainty that methods for working with {@code ExtendedIterator}
     * (like {@link OntGraphModelImpl#listLocalStatements(Resource, Property, RDFNode)})
     * should be placed in the public interfaces:
     * {@code Stream}-based analogues are almost the same but more functional.
     * But the ability to work with {@code ExtendedIterator} is sometimes needed,
     * since it is more lightweight and works a bit faster than Stream-API.
     *
     * @param model {@link OntGraphModel}, not {@code null}
     * @param s     {@link Resource}, can be {@code null} for any
     * @param p     {@link Property}, can be {@code null} for any
     * @param o     {@link RDFNode}, can be {@code null} for any
     * @return {@link ExtendedIterator} of {@link OntStatement}s local to the base model graph
     * @see OntGraphModel#localStatements(Resource, Property, RDFNode)
     * @since 1.4.1
     */
    public static ExtendedIterator<OntStatement> listLocalStatements(OntGraphModel model,
                                                                     Resource s,
                                                                     Property p,
                                                                     RDFNode o) {
        if (model instanceof OntGraphModelImpl) {
            return ((OntGraphModelImpl) model).listLocalStatements(s, p, o);
        }
        return WrappedIterator.create(model.localStatements(s, p, o).iterator());
    }

    /**
     * Lists all ontology objects with the given {@code type} that are defined in the base graph.
     * See also {@link #listLocalStatements(OntGraphModel, Resource, Property, RDFNode)} description.
     *
     * @param model {@link OntGraphModel}
     * @param type  {@link Class}-type
     * @param <O>   subclass of {@link OntObject}
     * @return {@link ExtendedIterator} of ontology objects of the type {@link O} that are local to the base graph
     * @see OntGraphModel#ontObjects(Class)
     * @since 1.4.1
     */
    public static <O extends OntObject> ExtendedIterator<O> listLocalObjects(OntGraphModel model,
                                                                             Class<? extends O> type) {
        if (model instanceof OntGraphModelImpl) {
            return ((OntGraphModelImpl) model).listLocalOntObjects(type);
        }
        Stream<O> res = model.ontObjects(type);
        return WrappedIterator.create(res.iterator()).filterKeep(OntObject::isLocal);
    }

    /**
     * Lists all OWL entities that are defined in the base graph.
     * See also {@link #listLocalStatements(OntGraphModel, Resource, Property, RDFNode)} description.
     *
     * @param model {@link OntGraphModel}
     * @return {@link ExtendedIterator} of {@link OntEntity}s that are local to the base graph
     * @see OntGraphModel#ontEntities()
     * @since 1.4.1
     */
    public static ExtendedIterator<OntEntity> listLocalEntities(OntGraphModel model) {
        if (model instanceof OntGraphModelImpl) {
            return ((OntGraphModelImpl) model).listLocalOntEntities();
        }
        return WrappedIterator.create(model.ontEntities().iterator()).filterKeep(OntObject::isLocal);
    }

    /**
     * Returns an iterator over all annotations of the given ontology statement.
     *
     * @param s {@link OntStatement}
     * @return {@link ExtendedIterator} over {@link OntStatement}s
     * @since 1.4.1
     */
    public static ExtendedIterator<OntStatement> listAnnotations(OntStatement s) {
        if (s instanceof OntStatementImpl) {
            return ((OntStatementImpl) s).listAnnotations();
        }
        return WrappedIterator.create(s.annotations().iterator());
    }

    /**
     * Lists all object's annotations.
     *
     * @param o {@link OntObject}, not {@code null}
     * @return {@link ExtendedIterator} over {@link OntStatement}s
     * @since 1.4.1
     */
    public static ExtendedIterator<OntStatement> listAnnotations(OntObject o) {
        if (o instanceof OntObjectImpl) {
            return ((OntObjectImpl) o).listAnnotations();
        }
        return WrappedIterator.create(o.annotations().iterator());
    }

    /**
     * Lists all members from {@link OntList Ontology List}.
     *
     * @param list {@link RDFNodeList}
     * @param <R>  {@link RDFNode}, a type of list members
     * @return {@link ExtendedIterator} of {@link R}
     * @since 1.3.0
     */
    public static <R extends RDFNode> ExtendedIterator<R> listMembers(RDFNodeList<R> list) {
        if (list instanceof OntListImpl) {
            return ((OntListImpl<R>) list).listMembers();
        }
        return WrappedIterator.create(list.members().iterator());
    }

}
