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
 *
 */

package ru.avicomp.ontapi.jena.impl;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.enhanced.EnhNode;
import org.apache.jena.graph.FrontsNode;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.impl.RDFListImpl;
import org.apache.jena.util.iterator.ExtendedIterator;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.impl.configuration.*;
import ru.avicomp.ontapi.jena.model.OntCE;
import ru.avicomp.ontapi.jena.model.OntIndividual;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.utils.Iter;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

import java.util.Collection;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * both for anon and named individuals.
 * <p>
 * Created by szuev on 09.11.2016.
 */
public class OntIndividualImpl extends OntObjectImpl implements OntIndividual {

    public static Configurable<OntObjectFactory> anonymousIndividualFactory = mode -> new CommonOntObjectFactory(
            new OntMaker.Default(AnonymousImpl.class), OntFinder.ANY_SUBJECT_AND_OBJECT, AnonymousImpl.FILTER.get(mode));

    public static Configurable<MultiOntObjectFactory> abstractIndividualFactory = createMultiFactory(OntFinder.ANY_SUBJECT_AND_OBJECT,
            Entities.INDIVIDUAL, anonymousIndividualFactory);


    public OntIndividualImpl(Node n, EnhGraph m) {
        super(n, m);
    }

    @Override
    public OntStatement attachClass(OntCE clazz) {
        return addType(clazz);
    }

    @Override
    public void detachClass(OntCE clazz) {
        removeType(clazz);
    }

    public static class NamedImpl extends OntIndividualImpl implements OntIndividual.Named {
        public NamedImpl(Node n, EnhGraph m) {
            super(OntObjectImpl.checkNamed(n), m);
        }

        @Override
        public boolean isBuiltIn() {
            return false;
        }

        @Override
        public OntStatement getRoot() {
            // todo: it seems this logic is wrong - it should return the "this rdf:type OWLClass" statement.
            return getRoot(RDF.type, OWL.NamedIndividual);
        }

        @Override
        public Stream<OntStatement> content() {
            // todo: what about not local statements and individuals with attached to several classes?
            return statements(RDF.type);
        }
    }

    /**
     * See description to the interface {@link OntIndividual.Anonymous}.
     * The current implementation allows treating b-node as anonymous individual
     * in any case with exception of the following:
     * <ul>
     * <li>it is a subject in statement "_:x rdf:type s", where "s" is not a class expression ("C").</li>
     * <li>it is a subject in statement "_:x @predicate @any", where @predicate is from reserved vocabulary
     * but not object, data or annotation built-in property
     * and not owl:sameAs and owl:differentFrom.</li>
     * <li>it is an object in statement "@any @predicate _:x", where @predicate is from reserved vocabulary
     * but not object, data or annotation built-in property
     * and not owl:sameAs, owl:differentFrom, owl:hasValue, owl:sourceIndividual and rdf:first.</li>
     * </ul>
     *
     * for notations and self-education see our main <a href='https://www.w3.org/TR/owl2-quick-reference/'>OWL2 Quick Refs</a>
     */
    @SuppressWarnings("WeakerAccess")
    public static class AnonymousImpl extends OntIndividualImpl implements OntIndividual.Anonymous {

        // old way:
        public static final Configurable<OntFilter> _FILTER = mode -> (OntFilter) (node, graph) -> node.isBlank() &&
                (!getDeclarations(node, graph, mode).mapWith(Triple::getObject).toSet().isEmpty() ||
                        positiveAssertionAnonIndividuals(graph, mode).anyMatch(node::equals) ||
                        negativeAssertionAnonIndividuals(graph).anyMatch(node::equals) ||
                        hasValueOPEAnonIndividuals(graph).anyMatch(node::equals) ||
                        sameAnonIndividuals(graph).anyMatch(node::equals) ||
                        differentAnonIndividuals(graph).anyMatch(node::equals) ||
                        oneOfAnonIndividuals(graph).anyMatch(node::equals) ||
                        disjointAnonIndividuals(graph).anyMatch(node::equals));

        public static final Configurable<OntFilter> FILTER = mode -> (OntFilter) (node, graph) -> testAnonymousIndividual(node, graph, mode);

        public AnonymousImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        public static final Set<Node> ALLOWED_IN_SUBJECT_PREDICATES =
                Stream.concat(Entities.BUILTIN.properties().stream(),
                        Stream.of(OWL.sameAs, OWL.differentFrom))
                        .map(FrontsNode::asNode).collect(Iter.toUnmodifiableSet());
        public static final Set<Node> ALLOWED_IN_OBJECT_PREDICATES =
                Stream.concat(Entities.BUILTIN.properties().stream(),
                        Stream.of(OWL.sameAs, OWL.differentFrom, OWL.sourceIndividual, OWL.hasValue, RDF.first))
                        .map(FrontsNode::asNode).collect(Iter.toUnmodifiableSet());

        public static final Set<Node> BUILT_IN_SUBJECT_PREDICATE_SET = Entities.BUILTIN.reservedProperties().stream()
                .map(FrontsNode::asNode)
                .filter(n -> !ALLOWED_IN_SUBJECT_PREDICATES.contains(n))
                .collect(Iter.toUnmodifiableSet());
        public static final Set<Node> BUILT_IN_OBJECT_PREDICATE_SET = Entities.BUILTIN.reservedProperties().stream()
                .map(FrontsNode::asNode)
                .filter(n -> !ALLOWED_IN_OBJECT_PREDICATES.contains(n))
                .collect(Iter.toUnmodifiableSet());

        public static boolean testAnonymousIndividual(Node node, EnhGraph eg, Configurable.Mode mode) {
            if (!node.isBlank()) {
                return false;
            }
            Set<Node> types = Iter.asStream(eg.asGraph().find(node, RDF.type.asNode(), Node.ANY)).map(Triple::getObject).collect(Collectors.toSet());
            if (types.stream().anyMatch(o -> OntCEImpl.abstractCEFactory.get(mode).canWrap(o, eg))) { // class assertion:
                return true;
            }
            if (!types.isEmpty()) { // any other typed statement,
                return false;
            }
            // _:x @built-in-predicate @any
            try (Stream<Triple> triples = Iter.asStream(eg.asGraph().find(node, Node.ANY, Node.ANY))) {
                if (triples.map(Triple::getPredicate).anyMatch(BUILT_IN_SUBJECT_PREDICATE_SET::contains)) {
                    return false;
                }
            }
            // @any @built-in-predicate _:x
            try (Stream<Triple> triples = Iter.asStream(eg.asGraph().find(Node.ANY, Node.ANY, node))) {
                if (triples.map(Triple::getPredicate).anyMatch(BUILT_IN_OBJECT_PREDICATE_SET::contains)) {
                    return false;
                }
            }
            // any other blank node could be treated as anonymous individual.
            return true;
        }

        @Override
        public void detachClass(OntCE clazz) {
            if (classes().allMatch(clazz::equals)) {
                // otherwise the anonymous individual could be lost.
                // use another way for removing the single class-assertion.
                throw new OntJenaException("Can't detach class " + clazz + ": it is a single for individual " + this);
            }
            super.detachClass(clazz);
        }

        protected static ExtendedIterator<Triple> getDeclarations(Node node, EnhGraph eg, Configurable.Mode mode) {
            return eg.asGraph().find(node, RDF.type.asNode(), Node.ANY).
                    filterKeep(t -> OntCEImpl.abstractCEFactory.get(mode).canWrap(t.getObject(), eg));
        }

        protected static Stream<Node> negativeAssertionAnonIndividuals(EnhGraph eg) {
            return Stream.of(OWL.sourceIndividual, OWL.targetIndividual)
                    .map(FrontsNode::asNode)
                    .map(predicate -> Iter.asStream(eg.asGraph().find(Node.ANY, predicate, Node.ANY)).map(Triple::getObject))
                    .flatMap(Function.identity()).filter(Node::isBlank);
            // it seems we don't need full validation:
            /*return Models.asStream(eg.asGraph().find(Node.ANY, RDF.type.asNode(), OWL.NegativePropertyAssertion.asNode()))
                    .map(Triple::getSubject).map(subject ->
                            Stream.of(OWL.sourceIndividual, OWL.targetIndividual)
                                    .map(FrontsNode::asNode)
                                    .map(predicate -> Models.asStream(eg.asGraph().find(subject, predicate, Node.ANY)).map(Triple::getObject))
                                    .flatMap(Function.identity()))
                    .flatMap(Function.identity())
                    .filter(Node::isBlank);*/
        }

        protected static Stream<Node> hasValueOPEAnonIndividuals(EnhGraph eg) {
            return Iter.asStream(eg.asGraph().find(Node.ANY, OWL.hasValue.asNode(), Node.ANY)).map(Triple::getObject).filter(Node::isBlank);
        }

        protected static Stream<Node> sameAnonIndividuals(EnhGraph eg) {
            return anonsForPredicate(eg.asGraph(), OWL.sameAs.asNode());
        }

        protected static Stream<Node> differentAnonIndividuals(EnhGraph eg) {
            return anonsForPredicate(eg.asGraph(), OWL.differentFrom.asNode());
        }

        private static Stream<Node> anonsForPredicate(Graph graph, Node predicate) {
            return Iter.asStream(graph.find(Node.ANY, predicate, Node.ANY))
                    .map(triple -> Stream.of(triple.getSubject(), triple.getObject()))
                    .flatMap(Function.identity()).filter(Node::isBlank);
        }

        /**
         * returns stream of blank nodes ("_:a"), where blank node is an object in a triple
         * which corresponds object property assertion "_:a1 PN _:a2" or annotation property assertion "s A t"
         * TODO: "a1 PN a2", "a R v", "s A t"
         * t = 	IRI, anonymous individual, or literal
         * s = IRI or anonymous individual
         *
         * @param eg {@link OntGraphModelImpl}
         * @param m {@link Configurable.Mode}
         * @return Stream of {@link Node}
         */
        protected static Stream<Node> positiveAssertionAnonIndividuals(EnhGraph eg, Configurable.Mode m) {
            return positiveAssertionProperties(eg, m)
                    .map(EnhNode::asNode)
                    .map(node -> anonAssertionObjects(eg.asGraph(), node))
                    .flatMap(Function.identity());
        }

        private static Stream<EnhNode> positiveAssertionProperties(EnhGraph eg, Configurable.Mode mode) {
            return Stream.of(Entities.ANNOTATION_PROPERTY, Entities.OBJECT_PROPERTY)
                    .map(c -> c.get(mode))
                    .map(f -> f.find(eg))
                    .flatMap(Function.identity());
        }

        private static Stream<Node> anonAssertionObjects(Graph graph, Node predicate) {
            return Iter.asStream(graph.find(Node.ANY, predicate, Node.ANY))
                    .map(Triple::getObject)
                    .filter(Node::isBlank);
            //.filter(node -> !graph.contains(node, Node.ANY, Node.ANY));
        }

        protected static Stream<Node> disjointAnonIndividuals(EnhGraph eg) {
            return blankNodesFromList(eg, OWL.AllDifferent.asNode(), OWL.distinctMembers.asNode(), OWL.members.asNode());
        }

        protected static Stream<Node> oneOfAnonIndividuals(EnhGraph eg) {
            return blankNodesFromList(eg, OWL.Class.asNode(), OWL.oneOf.asNode());
        }

        private static Stream<Node> blankNodesFromList(EnhGraph eg, Node type, Node... predicates) {
            Stream<Node> roots = Iter.asStream(eg.asGraph().find(Node.ANY, RDF.type.asNode(), type))
                    .map(Triple::getSubject)
                    .filter(Node::isBlank).distinct();
            return objects(eg.asGraph(), roots, predicates)
                    .filter(node -> RDFListImpl.factory.canWrap(node, eg))
                    .map(node -> RDFListImpl.factory.wrap(node, eg))
                    .map(enhNode -> enhNode.as(RDFList.class))
                    .map(RDFList::asJavaList)
                    .map(Collection::stream)
                    .flatMap(Function.identity())
                    .map(FrontsNode::asNode)
                    .filter(Node::isBlank);

        }

        private static Stream<Node> objects(Graph graph, Node subject, Node predicate) {
            return Iter.asStream(graph.find(subject, predicate, Node.ANY).mapWith(Triple::getObject));
        }

        private static Stream<Node> objects(Graph graph, Node subject, Node... predicates) {
            return Stream.of(predicates).map(p -> objects(graph, subject, p)).flatMap(Function.identity());
        }

        private static Stream<Node> objects(Graph graph, Stream<Node> subjects, Node... predicates) {
            return subjects.map(r -> objects(graph, r, predicates)).flatMap(Function.identity());
        }
    }
}
