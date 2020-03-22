/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2020, The University of Manchester, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.jena.utils;

import com.github.owlcs.ontapi.jena.impl.OntListImpl;
import com.github.owlcs.ontapi.jena.vocabulary.RDF;
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
import org.apache.jena.util.iterator.NullIterator;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * A class-helper to work with {@link Model Jena Model}s and its related objects and components:
 * {@link RDFNode Jena RDF Node}, {@link Literal Jena Literal}, {@link Resource Jena Resource} and
 * {@link Statement Jena Statement}.
 * <p>
 * Created by szuev on 20.10.2016.
 */
@SuppressWarnings("WeakerAccess")
public class Models {
    public static final Comparator<RDFNode> RDF_NODE_COMPARATOR = (r1, r2) -> NodeUtils.compareRDFTerms(r1.asNode(), r2.asNode());
    public static final Comparator<Statement> STATEMENT_COMPARATOR = Comparator
            .comparing(Statement::getSubject, RDF_NODE_COMPARATOR)
            .thenComparing(Statement::getPredicate, RDF_NODE_COMPARATOR)
            .thenComparing(Statement::getObject, RDF_NODE_COMPARATOR);
    public static final RDFNode BLANK = new ResourceImpl();
    public static final Comparator<Statement> STATEMENT_COMPARATOR_IGNORE_BLANK = Comparator
            .comparing((Function<Statement, RDFNode>) s -> s.getSubject().isAnon() ? BLANK : s.getSubject(),
                    RDF_NODE_COMPARATOR)
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
     * @see com.github.owlcs.ontapi.jena.model.OntList
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
     * @see com.github.owlcs.ontapi.jena.model.OntList
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
     * Answers {@code true} iff the given statement belongs to some []-list.
     *
     * @param s {@link Statement}, not {@code null}
     * @return boolean
     */
    public static boolean isInList(Statement s) {
        return RDF.first.equals(s.getPredicate()) || RDF.rest.equals(s.getPredicate()) || RDF.nil.equals(s.getObject());
    }

    /**
     * Answers a set of all of the RDF statements whose subject is one of the cells of the given list.
     *
     * @param list []-list, not {@code null}
     * @return a {@code Set} of {@link Statement}s
     */
    public static Set<Statement> getListStatements(RDFList list) {
        return ((RDFListImpl) list).collectStatements();
    }

    /**
     * Replaces namespaces map with new one.
     *
     * @param mapping  {@link PrefixMapping Prefix Mapping} to modify
     * @param prefixes java Map of new prefixes to set
     * @return a {@code Map} of previously associated prefixes
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
     * @return {@code Stream} of {@code String}s
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
     * Note: {@code rdf:List} may content a large number of members (1000+),
     * which may imply heavy calculation.
     *
     * @param inModel Resource with associated model inside.
     * @return a {@code Set} of {@link Statement}s
     * @see Models#listDescendingStatements(RDFNode)
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
     * Recursively lists all ascending statements for the given {@link RDFNode RDF Node}.
     * <p>
     * More specifically, this function returns all statements,
     * which have either the specified node in an object position,
     * or its indirect ascendant in a graph tree, found by the same method.
     * Consider, the specified node {@code r} belongs to the following RDF:
     * <pre>{@code
     * <a>  p0 _:b0 .
     * _:b0 p1 _:b1 .
     * _:b1 p2  <x> .
     * _:b1 p3  r .
     * }</pre>
     * In this case the method will return three statements:
     * {@code _:b1 p3 r}, {@code _:b0 p1 _:b1} and {@code <a> p0 _:b0}.
     * The statement {@code _:b1 p2  <x>} is skipped since uri resource {@code <x>} is not an ascendant of {@code r}.
     * <p>
     * This is the opposite of the method {@link #listDescendingStatements(RDFNode)}.
     * <p>
     * Note: there is a danger of {@code StackOverflowError} in case graph contains a recursion.
     *
     * @param object, not {@code null} must be attached to a model
     * @return {@link ExtendedIterator} of {@link Statement}s
     */
    public static ExtendedIterator<Statement> listAscendingStatements(RDFNode object) {
        return Iter.flatMap(object.getModel().listStatements(null, null, object),
                s -> s.getSubject().isAnon() ?
                        Iter.concat(Iter.of(s), listAscendingStatements(s.getSubject())) : Iter.of(s));
    }

    /**
     * Recursively lists all descending statements for the given {@link RDFNode RDF Node}.
     * <p>
     * More specifically, this function returns all statements,
     * which have either the specified node in an subject position,
     * or its indirect descendant in a graph tree (if the node is anonymous resource), found by the same method.
     * Consider, the specified node {@code <a>} belongs to the following RDF:
     * <pre>{@code
     * <a>  p0 _:b0 .
     * _:b0 p1 _:b1 .
     * _:b1 p2  <x> .
     * <x> p3  <b> .
     * }</pre>
     * In this case the method will return three statements:
     * {@code <a>  p0 _:b0}, {@code :b0 p1 _:b1} and {@code _:b1 p2  <x>}.
     * The last statement is skipped, since {@code <x>} is uri resource.
     * <p>
     * This is the opposite of the method {@link #listAscendingStatements(RDFNode)}.
     * <p>
     * Note: there is a danger of {@code StackOverflowError} in case graph contains a recursion.
     *
     * @param subject, not {@code null} must be attached to a model
     * @return {@link ExtendedIterator} of {@link Statement}s
     * @see Models#getAssociatedStatements(Resource)
     */
    public static ExtendedIterator<Statement> listDescendingStatements(RDFNode subject) {
        if (!subject.isResource()) return NullIterator.instance();
        return Iter.flatMap(subject.asResource().listProperties(),
                s -> s.getObject().isAnon() ?
                        Iter.concat(Iter.of(s), listDescendingStatements(s.getResource())) : Iter.of(s));
    }

    /**
     * Returns a {@code Set} of root statements.
     * Any statement has one or more roots or is a root itself.
     * A statement with the predicate {@code rdf:type} is always a root.
     *
     * @param st {@link Statement}, not {@code null}
     * @return a {@code Set} of {@link Statement}s
     * @see #listAscendingStatements(RDFNode)
     */
    public static Set<Statement> getRootStatements(Statement st) {
        Resource subject = st.getSubject();
        if (subject.isURIResource()) {
            return Collections.singleton(st);
        }
        return findRoots(st.getModel(), st, new HashSet<>());
    }

    private static Set<Statement> findRoots(Model m, Statement st, Set<Statement> seen) {
        Resource subject = st.getSubject();
        if (subject.isURIResource()) {
            return Collections.singleton(st);
        }
        Set<Statement> res = new HashSet<>();
        if (RDF.type.equals(st.getPredicate())) {
            res.add(st);
        }
        m.listStatements(null, null, subject)
                .filterKeep(seen::add)
                .forEachRemaining(x -> res.addAll(findRoots(m, x, seen)));
        if (res.isEmpty()) {
            return Collections.singleton(st);
        }
        return res;
    }

    /**
     * Lists all direct subjects for the given object.
     *
     * @param object {@link RDFNode}, not {@code null}
     * @return <b>distinct</b> {@code Stream} of {@link Resource}s
     * @see Model#listResourcesWithProperty(Property, RDFNode)
     * @see org.apache.jena.graph.GraphUtil#listSubjects(Graph, Node, Node)
     */
    public static Stream<Resource> subjects(RDFNode object) {
        Model m = Objects.requireNonNull(object.getModel(), "No model for a resource " + object);
        return Iter.fromSet(() -> m.getGraph().find(Node.ANY, Node.ANY, object.asNode())
                .mapWith(t -> m.wrapAsResource(t.getSubject())).toSet());
    }

    /**
     * Returns a string representation of the given Jena statement taking into account PrefixMapping.
     *
     * @param st {@link Statement}, not {@code null}
     * @param pm {@link PrefixMapping}, not {@code null}
     * @return {@code String}
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
     * @param inModel {@link Statement}, not {@code null}
     * @return {@code String}
     */
    public static String toString(Statement inModel) {
        return toString(inModel, inModel.getModel());
    }
}
