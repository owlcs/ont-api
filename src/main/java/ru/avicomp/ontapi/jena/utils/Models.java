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
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.UnionGraph;
import ru.avicomp.ontapi.jena.impl.OntIndividualImpl;
import ru.avicomp.ontapi.jena.impl.OntListImpl;
import ru.avicomp.ontapi.jena.impl.OntStatementImpl;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntIndividual;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A helper to work with {@link Model Jena Model}s (mainly with {@link ru.avicomp.ontapi.jena.model.OntGraphModel})
 * and its related objects (i.e. {@link Resource Jena Resourec}, {@link Statement Jena Statement}).
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
     * Builds typed list from Stream of RDFNode's
     *
     * @param model   Model
     * @param type    type of list to create
     * @param members Stream of members
     * @return the head of created list.
     * @deprecated using stream as input parameter is a bad idea,
     */
    @Deprecated
    public static Resource createTypedList(Model model, Resource type, Stream<? extends RDFNode> members) {
        return createTypedList(model, type, members.collect(Collectors.toList()));
    }

    /**
     * Creates a typed []-list with the given type containing the resources from the given given collection.
     *
     * @param model   {@link Model model} in which the []-list is created
     * @param type    {@link Resource} the type for new []-list
     * @param members Collection of {@link RDFNode}s
     * @return anonymous resource - the header of the typed []-list
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
     */
    public static boolean isInList(Statement s) {
        return RDF.first.equals(s.getPredicate()) || RDF.rest.equals(s.getPredicate()) || RDF.nil.equals(s.getObject());
    }

    /**
     * Converts rdf-node to anonymous individual.
     * The result anonymous individual could be true (instance of some owl class) or fake (any blank node).
     *
     * @param node {@link RDFNode}
     * @return {@link OntIndividual.Anonymous}
     * @throws OntJenaException if node can be present as anonymous individual
     */
    public static OntIndividual.Anonymous asAnonymousIndividual(RDFNode node) {
        if (OntJenaException.notNull(node, "Null node.").canAs(OntIndividual.Anonymous.class))
            return node.as(OntIndividual.Anonymous.class);
        if (node.isAnon()) {
            return new OntIndividualImpl.AnonymousImpl(node.asNode(), (EnhGraph) node.getModel());
        }
        throw new OntJenaException.Conversion(node + " could not be " + OntIndividual.Anonymous.class);
    }

    /**
     * Replaces namespaces map with new one.
     *
     * @param mapping  {@link PrefixMapping} object
     * @param prefixes Map of new prefixes to set.
     * @return Map of previous prefixes.
     */
    public static Map<String, String> setNsPrefixes(PrefixMapping mapping, Map<String, String> prefixes) {
        Map<String, String> init = mapping.getNsPrefixMap();
        init.keySet().forEach(mapping::removeNsPrefix);
        prefixes.forEach((p, u) -> mapping.setNsPrefix(p.replaceAll(":$", ""), u));
        return init;
    }

    /**
     * Lists all literal string values with specified lang found by subject and predicate.
     *
     * @param subject   {@link Resource}
     * @param predicate {@link Property}
     * @param lang      String lang, maybe null or empty
     * @return Stream of Strings
     */
    public static Stream<String> langValues(Resource subject, Property predicate, String lang) {
        return Iter.asStream(subject.listProperties(predicate))
                .map(Statement::getObject)
                .filter(RDFNode::isLiteral)
                .map(RDFNode::asLiteral)
                .filter(l -> filterByLang(l, lang))
                .map(Literal::getString);
    }

    private static boolean filterByLang(Literal literal, String lang) {
        String other = literal.getLanguage();
        if (StringUtils.isEmpty(lang))
            return StringUtils.isEmpty(other);
        return lang.trim().equalsIgnoreCase(other);
    }

    /**
     * Recursively deletes all resource children.
     *
     * @param inModel Resource from a model
     */
    public static void deleteAll(Resource inModel) {
        deleteAll(inModel, new HashSet<>());
    }

    private static void deleteAll(Resource r, Set<Node> viewed) {
        if (viewed.contains(r.asNode())) {
            return;
        }
        viewed.add(r.asNode());
        Set<Statement> props = r.listProperties().toSet();
        props.forEach(s -> {
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
            RDFListImpl list = (RDFListImpl) root.as(RDFList.class);
            if (list.isEmpty()) return;
            list.collectStatements().forEach(statement -> {
                res.add(statement);
                if (!list.listFirst().equals(statement.getPredicate())) return;
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
     * Splits the statement on several equivalent ones but with disjoint annotations.
     * This method is useful in case there are several b-nodes for each annotations instead a single one.
     * It is not canonical way to add sub-annotations and should not be widely used, since it is redundant.
     * So usually the result stream contains only a single element: the specified OntStatement instance.
     * Consider an example:
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
     * Here the statement ("s A t") has two sub-annotations ("this A1 t1" and "this A2 t2"),
     * but they are spread over different resources.
     * In this case the method returns stream of two ont-statement, and each of them has only one sub-annotation.
     * For generality, here is an example of a correct and equivalent graph:
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
     * each element equals to this statement but has different related annotations.
     */
    public static Stream<OntStatement> split(OntStatement statement) {
        return ((OntStatementImpl) statement).split();
    }

    /**
     * Returns a string representation of the given Jena statement taking into account PrefixMapping.
     *
     * @param st {@link Statement}, not null
     * @param pm {@link PrefixMapping}, not null
     * @return String
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
     */
    public static String toString(Statement inModel) {
        return toString(inModel, inModel.getModel());
    }

    /**
     * Inserts the given ontology in the dependencies of each ontology from the specified collection ({@code manager}).
     * Can be used to fix missed graphs or to replace existing dependency with new one in case {@code replace = true}.
     *
     * @param manager the collection of other ontologies in form of {@link Supplier} providing Stream
     * @param ont     {@link OntGraphModel} the ontology to insert, must be named
     * @param replace if {@code true} existing graphs will be replaced with new one,
     *                otherwise the model will be inserted only if there is {@code owl:import} without a graph
     */
    public static void insert(Supplier<Stream<OntGraphModel>> manager, OntGraphModel ont, boolean replace) {
        String uri = Objects.requireNonNull(ont.getID().getURI(), "Must be named ontology");
        manager.get()
                .filter(m -> m.getID().imports().anyMatch(uri::equals))
                .peek(m -> {
                    if (!replace) return;
                    m.imports()
                            .filter(i -> uri.equals(i.getID().getURI()))
                            .findFirst()
                            .ifPresent(i -> ((UnionGraph) m.getGraph()).removeGraph(i.getGraph()));
                })
                .filter(m -> m.imports().map(OntGraphModel::getID).map(Resource::getURI).noneMatch(uri::equals))
                .forEach(m -> m.addImport(ont));
    }

    /**
     * Lists all models that are associated with the given model in the form of a flat stream.
     * Note: this is a recursive method.
     *
     * @param m {@link OntGraphModel}
     * @return Stream of models, not empty (contains at least the input model)
     * @see Graphs#flat(Graph)
     */
    public static Stream<OntGraphModel> flat(OntGraphModel m) {
        return Stream.concat(Stream.of(m), m.imports().map(Models::flat).flatMap(Function.identity()));
    }

}
