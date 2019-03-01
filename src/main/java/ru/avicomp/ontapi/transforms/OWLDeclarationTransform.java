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

package ru.avicomp.ontapi.transforms;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.FrontsTriple;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.*;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDFS;
import ru.avicomp.ontapi.jena.utils.Iter;
import ru.avicomp.ontapi.jena.utils.Models;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;
import ru.avicomp.ontapi.jena.vocabulary.SWRL;
import ru.avicomp.ontapi.transforms.vocabulary.AVC;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Class to perform the final tuning of the OWL-2 ontology: mostly for fixing missed owl-declarations where it is possible.
 * It have to be running after {@link RDFSTransform} and {@link OWLCommonTransform}.
 * <p>
 * This transformer is designed to put in order any external (mainly none-OWL2) ontologies.
 * Also there are lots examples of incomplete or wrong ontologies provided by the tests from OWL-API contract pack,
 * which are not necessarily RDFS or OWL1.
 * And it seems such situations have to be relative rare in the real world, since
 * any API which meets specification would not produce ontologies, when there is some true parts of OWL2,
 * but no explicit declarations or some other components from which they consist.
 * At least one can be sure that ONT-API does not provide anything that only partially complies with the specification;
 * but for correct output the input should also be correct.
 * <p>
 * Consists of two inner transforms:
 * <ul>
 * <li>The first, {@link ManifestDeclarator}, works with the obvious cases
 * when the type of the left or the right statements part is defined by the predicate or from some other clear hints.
 * E.g. if we have triple "A rdfs:subClassOf B" then we know exactly - both "A" and "B" are owl-class expressions.
 * </li>
 * <li>The second, {@link ReasonerDeclarator}, performs iterative analyzing of whole graph to choose the correct entities type.
 * E.g. we can have owl-restriction (existential/universal quantification)
 * "_:x rdf:type owl:Restriction; owl:onProperty A; owl:allValuesFrom B",
 * where "A" and "B" could be either object property and class expressions or data property and data-range,
 * and therefore we need to find other entries of these two entities in the graph;
 * for this example the only one declaration either of "A" or "B" is enough.
 * </li>
 * </ul>
 *
 * @see <a href='https://www.w3.org/TR/owl2-quick-reference/'>OWL2 Short Guide</a>
 */
@SuppressWarnings("WeakerAccess")
public class OWLDeclarationTransform extends Transform {

    private static final List<Resource> PROPERTY_TYPES = Stream.of(OWL.DatatypeProperty, OWL.ObjectProperty, OWL.AnnotationProperty)
            .collect(Iter.toUnmodifiableList());
    private static final List<Resource> CLASS_TYPES = Stream.of(OWL.Class, RDFS.Datatype)
            .collect(Iter.toUnmodifiableList());


    protected final Transform manifestDeclarator;
    protected final Transform reasonerDeclarator;

    public OWLDeclarationTransform(Graph graph) {
        super(graph);
        this.manifestDeclarator = new ManifestDeclarator(graph);
        this.reasonerDeclarator = new ReasonerDeclarator(graph);
    }

    @Override
    public void perform() {
        try {
            manifestDeclarator.perform();
            reasonerDeclarator.perform();
        } finally {
            finalActions();
        }
    }

    @Override
    public Stream<Triple> uncertainTriples() {
        return reasonerDeclarator.uncertainTriples();
    }

    protected void finalActions() {
        getWorkModel().removeAll(null, RDF.type, AVC.AnonymousIndividual);
        // at times the ontology could contain some rdfs garbage,
        // even if other transformers (OWLTransformer, RDFSTransformer) have been used.
        listStatements(null, RDF.type, RDF.Property)
                .mapWith(Statement::getSubject)
                .filterKeep(RDFNode::isURIResource)
                .filterKeep(s -> hasAnyType(s, PROPERTY_TYPES))
                .toList()
                .forEach(p -> undeclare(p, RDF.Property));
        listStatements(null, RDF.type, RDFS.Class)
                .mapWith(Statement::getSubject)
                .filterKeep(RDFNode::isURIResource)
                .filterKeep(s -> hasAnyType(s, CLASS_TYPES))
                .toList()
                .forEach(c -> undeclare(c, RDFS.Class));
    }

    /**
     * The transformer to restore declarations in the clear cases
     * (all notations are taken from the <a href='https://www.w3.org/TR/owl2-quick-reference/'>OWL2 Short Guide</a>):
     * 1) The declaration in annotation
     * <ul>
     * <li>{@code _:x a owl:Annotation; owl:annotatedSource s; owl:annotatedProperty rdf:type; owl:annotatedTarget U.}</li>
     * </ul>
     * 2) Explicit class:
     * <ul>
     * <li>{@code C1 rdfs:subClassOf C2}</li>
     * <li>{@code C1 owl:disjointWith C2}</li>
     * <li>{@code _:x owl:complementOf C}</li>
     * <li>{@code _:x rdf:type owl:AllDisjointClasses; owl:members ( C1 ... Cn )}</li>
     * <li>{@code CN owl:disjointUnionOf ( C1 ... Cn )}</li>
     * <li>{@code C owl:hasKey ( P1 ... Pm R1 ... Rn )}</li>
     * <li>{@code _:x a owl:Restriction; _:x owl:onClass C}</li>
     * </ul>
     * 3) Explicit data-range:
     * <ul>
     * <li>{@code _:x owl:datatypeComplementOf D.}</li>
     * <li>{@code _:x owl:onDatatype DN; owl:withRestrictions ( _:x1 ... _:xn )}</li>
     * <li>{@code _x: a owl:Restriction; owl:onProperties ( R1 ... Rn ); owl:allValuesFrom Dn}</li>
     * <li>{@code _x: a owl:Restriction; owl:onProperties ( R1 ... Rn ); owl:someValuesFrom Dn}</li>
     * <li>{@code _:x a owl:Restriction; owl:onDataRange D.}</li>
     * </ul>
     * 4) Data range or class expression:
     * <ul><li>{@code _:x owl:oneOf ( a1 ... an )} or {@code _:x owl:oneOf ( v1 ... vn )}</li></ul>
     * 5) Explicit object property expression:
     * <ul>
     * <li>{@code P a owl:InverseFunctionalProperty}</li>
     * <li>{@code P rdf:type owl:ReflexiveProperty}</li>
     * <li>{@code P rdf:type owl:IrreflexiveProperty}</li>
     * <li>{@code P rdf:type owl:SymmetricProperty}</li>
     * <li>{@code P rdf:type owl:AsymmetricProperty}</li>
     * <li>{@code P rdf:type owl:TransitiveProperty}</li>
     * <li>{@code P1 owl:inverseOf P2}</li>
     * <li>{@code P owl:propertyChainAxiom ( P1 ... Pn )}</li>
     * <li>{@code _:x a owl:Restriction; owl:onProperty P; owl:hasSelf "true"^^xsd:boolean}</li>
     * </ul>
     * 6) Data property or object property expression:
     * <ul>
     * <li>{@code _:x a owl:Restriction; owl:onProperty R; owl:hasValue v} or {@code _:x a owl:Restriction; owl:onProperty P; owl:hasValue a}</li>
     * <li>{@code _:x rdf:type owl:NegativePropertyAssertion; owl:sourceIndividual a1; owl:assertionProperty P; owl:targetIndividual a2} or
     * {@code _:x rdf:type owl:NegativePropertyAssertion; owl:sourceIndividual a1; owl:assertionProperty R; owl:targetValue v}</li>
     * </ul>
     * 7) Explicit individuals:
     * <ul>
     * <li>{@code a1 owl:sameAs a2} and {@code a1 owl:differentFrom a2}</li>
     * <li>{@code _:x rdf:type owl:AllDifferent; owl:members ( a1 ... an )}</li>
     * </ul>
     * 8) Class assertions (individuals declarations):
     * <ul>
     * <li>{@code a rdf:type C}</li>
     * </ul>
     * 9) SWRL rules. see {@link SWRL}
     */
    @SuppressWarnings("WeakerAccess")
    public static class ManifestDeclarator extends BaseDeclarator {

        private static final List<Resource> SWRL_ARG1_ATOM_TYPES = Stream.of(SWRL.ClassAtom,
                SWRL.DatavaluedPropertyAtom, SWRL.IndividualPropertyAtom,
                SWRL.DifferentIndividualsAtom, SWRL.SameIndividualAtom).collect(Iter.toUnmodifiableList());
        private static final List<Resource> SWRL_ARG2_ATOM_TYPES = Stream.of(SWRL.IndividualPropertyAtom,
                SWRL.DifferentIndividualsAtom, SWRL.SameIndividualAtom).collect(Iter.toUnmodifiableList());
        protected final Set<? extends RDFNode> forbiddenClassCandidates;

        public ManifestDeclarator(Graph graph) {
            super(graph);
            this.forbiddenClassCandidates = collectForbiddenClassCandidates();
        }

        @Override
        public void perform() {
            parseAnnotations();
            parseClassExpressions();
            parseDataRangeExpressions();
            parseOneOfExpression();
            parseObjectPropertyExpressions();
            parseObjectOrDataProperties();
            parseIndividuals();
            parseClassAssertions();
            parseSWRL();
        }

        protected void parseAnnotations() {
            // "_:x a owl:Annotation; owl:annotatedSource entity;
            // owl:annotatedProperty rdf:type; owl:annotatedTarget type." => "entity rdf:type type"
            listStatements(null, OWL.annotatedProperty, RDF.type)
                    .mapWith(Statement::getSubject)
                    .filterKeep(s -> s.isAnon()
                            && (s.hasProperty(RDF.type, OWL.Annotation) || s.hasProperty(RDF.type, OWL.Axiom)))
                    .forEachRemaining(r -> {
                        Resource source = getObjectResource(r, OWL.annotatedSource);
                        Resource target = getObjectResource(r, OWL.annotatedTarget);
                        if (source == null || target == null) return;
                        declare(source, target);
                    });
        }

        protected void parseClassExpressions() {
            // "C1 rdfs:subClassOf C2" or "C1 owl:disjointWith C2"
            Iter.flatMap(Iter.flatMap(Iter.of(RDFS.subClassOf, OWL.disjointWith), p -> listStatements(null, p, null)),
                    s -> Iter.of(s.getSubject(), s.getObject()))
                    .filterKeep(RDFNode::isResource).mapWith(RDFNode::asResource)
                    .toSet()
                    .forEach(this::declareClass);
            // "_:x owl:complementOf C"
            Iter.flatMap(listStatements(null, OWL.complementOf, null), s -> Iter.of(s.getSubject(), s.getObject()))
                    .filterKeep(RDFNode::isResource).mapWith(RDFNode::asResource)
                    .toSet()
                    .forEach(this::declareClass);
            // "_:x rdf:type owl:AllDisjointClasses ; owl:members ( C1 ... Cn )"
            Iter.flatMap(listStatements(null, RDF.type, OWL.AllDisjointClasses)
                    .filterKeep(s -> s.getSubject().isAnon()), s -> members(s.getSubject(), OWL.members))
                    .toSet()
                    .forEach(this::declareClass);
            // "CN owl:disjointUnionOf ( C1 ... Cn )"
            Iter.flatMap(listStatements(null, OWL.disjointUnionOf, null)
                            .mapWith(Statement::getSubject).filterKeep(RDFNode::isURIResource),
                    x -> Iter.concat(Iter.of(x), members(x, OWL.disjointUnionOf)))
                    .toSet()
                    .forEach(this::declareClass);
            // "C owl:hasKey ( P1 ... Pm R1 ... Rn )"
            listStatements(null, OWL.hasKey, null).mapWith(Statement::getSubject).forEachRemaining(this::declareClass);
            // "_:x a owl:Restriction; _:x owl:onClass C"
            listStatements(null, OWL.onClass, null)
                    .filterKeep(s -> s.getSubject().isAnon() && s.getObject().isResource()
                            && s.getSubject().hasProperty(OWL.onProperty))
                    .forEachRemaining(s -> declare(s.getSubject(), OWL.Restriction).declareClass(s.getResource()));
        }

        protected void parseDataRangeExpressions() {
            // "_:x owl:datatypeComplementOf D."
            listStatements(null, OWL.datatypeComplementOf, null)
                    .filterKeep(s -> s.getObject().isResource())
                    .forEachRemaining(s -> declareDatatype(s.getSubject()).declareDatatype(s.getResource()));
            // "_:x owl:onDatatype DN; owl:withRestrictions ( _:x1 ... _:xn )"
            listStatements(null, OWL.onDatatype, null)
                    .filterKeep(s -> s.getObject().isURIResource() && s.getSubject().hasProperty(OWL.withRestrictions)
                            && s.getSubject().getProperty(OWL.withRestrictions).getObject().canAs(RDFList.class))
                    .forEachRemaining(s -> declareDatatype(s.getSubject()).declareDatatype(s.getResource()));

            // "_x: a owl:Restriction;  owl:onProperties ( R1 ... Rn ); owl:allValuesFrom Dn" or
            // "_x: a owl:Restriction;  owl:onProperties ( R1 ... Rn ); owl:someValuesFrom Dn"
            listStatements(null, OWL.onProperties, null)
                    .filterKeep(s -> s.getSubject().isAnon() && s.getObject().canAs(RDFList.class))
                    .mapWith(Statement::getSubject)
                    .forEachRemaining(r -> {
                        Iter.flatMap(Iter.of(OWL.allValuesFrom, OWL.someValuesFrom), p -> listStatements(r, p, null))
                                .mapWith(Statement::getObject)
                                .filterKeep(RDFNode::isAnon)
                                .forEachRemaining(n -> declareDatatype(n.asResource()));
                        declare(r, OWL.Restriction);
                    });
            // "_:x a owl:Restriction; owl:onDataRange D."
            listStatements(null, OWL.onDataRange, null)
                    .filterKeep(s -> s.getSubject().isAnon() && s.getSubject().hasProperty(OWL.onProperty)
                            && s.getObject().isResource())
                    .forEachRemaining(s -> declare(s.getSubject(), OWL.Restriction).declareDatatype(s.getResource()));
        }

        protected void parseOneOfExpression() {
            // "_:x owl:oneOf ( a1 ... an )" or "_:x owl:oneOf ( v1 ... vn )"
            listStatements(null, OWL.oneOf, null)
                    .filterKeep(s -> s.getSubject().isAnon() && s.getObject().canAs(RDFList.class))
                    .forEachRemaining(s -> {
                        List<RDFNode> values = s.getObject().as(RDFList.class).asJavaList();
                        if (values.isEmpty()) return;
                        if (values.stream().allMatch(RDFNode::isLiteral)) {
                            declareDatatype(s.getSubject());
                        } else {
                            declareClass(s.getSubject());
                            values.forEach(v -> declareIndividual(v.asResource()));
                        }
                    });
        }

        protected void parseObjectPropertyExpressions() {
            // "_:x a owl:InverseFunctionalProperty", etc
            Iter.flatMap(Iter.of(OWL.InverseFunctionalProperty,
                    OWL.ReflexiveProperty,
                    OWL.IrreflexiveProperty,
                    OWL.SymmetricProperty,
                    OWL.AsymmetricProperty,
                    OWL.TransitiveProperty), p -> listStatements(null, RDF.type, p))
                    .mapWith(Statement::getSubject)
                    .toSet()
                    .forEach(this::declareObjectProperty);
            // "P1 owl:inverseOf P2"
            Iter.flatMap(listStatements(null, OWL.inverseOf, null).filterKeep(s -> s.getObject().isURIResource()),
                    s -> Iter.of(s.getSubject(), s.getResource()))
                    .toSet()
                    .forEach(this::declareObjectProperty);
            // 	"P owl:propertyChainAxiom (P1 ... Pn)"
            Iter.flatMap(listStatements(null, OWL.propertyChainAxiom, null), this::subjectAndObjects)
                    .toSet()
                    .forEach(this::declareObjectProperty);
            // "_:x a owl:Restriction; owl:onProperty P; owl:hasSelf "true"^^xsd:boolean"
            listStatements(null, OWL.hasSelf, null)
                    .filterKeep(s -> Models.TRUE.equals(s.getObject()))
                    .mapWith(Statement::getSubject)
                    .filterKeep(s -> s.isAnon() && s.hasProperty(OWL.onProperty))
                    .forEachRemaining(s -> {
                        Resource p = getObjectResource(s, OWL.onProperty);
                        if (p == null) return;
                        declareObjectProperty(p).declare(s, OWL.Restriction);
                    });
        }

        protected void parseObjectOrDataProperties() {
            // "_:x a owl:Restriction; owl:onProperty R; owl:hasValue v" or "_:x a owl:Restriction; owl:onProperty P; owl:hasValue a"
            listStatements(null, OWL.hasValue, null).forEachRemaining(s -> {
                Resource p = getObjectResource(s.getSubject(), OWL.onProperty);
                if (p == null) return;
                declare(s.getSubject(), OWL.Restriction);
                if (s.getObject().isLiteral()) {
                    declareDataProperty(p);
                } else {
                    declareIndividual(s.getResource()).declareObjectProperty(p);
                }
            });
            // "_:x rdf:type owl:NegativePropertyAssertion" with owl:targetIndividual
            listStatements(null, RDF.type, OWL.NegativePropertyAssertion)
                    .mapWith(Statement::getSubject)
                    .filterKeep(RDFNode::isAnon)
                    .forEachRemaining(r -> {
                        Resource source = getObjectResource(r, OWL.sourceIndividual);
                        Resource prop = getObjectResource(r, OWL.assertionProperty);
                        if (source == null || prop == null) return;
                        Resource i = getObjectResource(r, OWL.targetIndividual);
                        if (i == null && getObjectLiteral(r, OWL.targetValue) == null) return;
                        declareIndividual(source);
                        if (i != null) {
                            declareObjectProperty(prop).declareIndividual(i);
                        } else {
                            declareDataProperty(prop);
                        }
                    });
        }

        protected void parseIndividuals() {
            // "a1 owl:sameAs a2" and "a1 owl:differentFrom a2"
            Iter.flatMap(Iter.flatMap(Iter.of(OWL.sameAs, OWL.differentFrom), p -> listStatements(null, p, null)),
                    s -> Iter.of(s.getSubject(), s.getObject()))
                    .filterKeep(RDFNode::isResource)
                    .mapWith(RDFNode::asResource)
                    .toSet()
                    .forEach(this::declareIndividual);
            // "_:x rdf:type owl:AllDifferent; owl:members (a1 ... an)"
            Iter.flatMap(listStatements(null, RDF.type, OWL.AllDifferent)
                    .filterKeep(s -> s.getSubject().isAnon())
                    .mapWith(Statement::getSubject), this::disjointIndividuals)
                    .toSet()
                    .forEach(this::declareIndividual);
        }

        private ExtendedIterator<Resource> disjointIndividuals(Resource s) {
            return Iter.flatMap(Iter.of(OWL.members, OWL.distinctMembers), p -> members(s, p));
        }

        protected Set<Resource> collectForbiddenClassCandidates() {
            Set<Resource> res = new HashSet<>(builtins.reservedResources());
            res.add(AVC.AnonymousIndividual);
            res.removeAll(builtins.classes());
            return res;
        }

        protected void parseClassAssertions() {
            // "a rdf:type C"
            listStatements(null, RDF.type, null)
                    .filterKeep(s -> s.getObject().isResource() && !forbiddenClassCandidates.contains(s.getObject()))
                    .toSet()
                    .forEach(s -> declareIndividual(s.getSubject()).declareClass(s.getResource()));
        }

        protected void parseSWRL() {
            // first IArg
            processSWRL(SWRL.argument1,
                    s -> s.getSubject().isAnon()
                            && SWRL_ARG1_ATOM_TYPES.stream().anyMatch(t -> hasType(s.getSubject(), t)),
                    r -> !hasType(r, SWRL.Variable),
                    this::declareIndividual);
            // second IArg
            processSWRL(SWRL.argument2,
                    s -> s.getSubject().isAnon()
                            && SWRL_ARG2_ATOM_TYPES.stream().anyMatch(t -> hasType(s.getSubject(), t)),
                    r -> !hasType(r, SWRL.Variable),
                    this::declareIndividual);
            // class
            processSWRL(SWRL.classPredicate,
                    s -> s.getSubject().isAnon() && hasType(s.getSubject(), SWRL.ClassAtom),
                    null, this::declareClass);
            // data-range
            processSWRL(SWRL.dataRange,
                    s -> s.getSubject().isAnon() && hasType(s.getSubject(), SWRL.DataRangeAtom),
                    null, this::declareDatatype);
            // object property
            processSWRL(SWRL.propertyPredicate,
                    s -> s.getSubject().isAnon() && hasType(s.getSubject(), SWRL.IndividualPropertyAtom),
                    null, this::declareObjectProperty);
            // data property
            processSWRL(SWRL.propertyPredicate,
                    s -> s.getSubject().isAnon() && hasType(s.getSubject(), SWRL.DatavaluedPropertyAtom),
                    null, this::declareDataProperty);
        }

        protected void processSWRL(Property predicateToFind,
                                   Predicate<Statement> functionToFilter,
                                   Predicate<Resource> functionToCheck,
                                   Consumer<Resource> functionToDeclare) {
            listStatements(null, predicateToFind, null)
                    .filterKeep(functionToFilter)
                    .mapWith(Statement::getObject)
                    .filterKeep(RDFNode::isResource)
                    .mapWith(RDFNode::asResource)
                    .filterKeep(r -> functionToCheck == null || functionToCheck.test(r))
                    .forEachRemaining(functionToDeclare);
        }
    }

    /**
     * The transformer to restore declarations in the implicit cases
     * (all notations are taken from the <a href='https://www.w3.org/TR/owl2-quick-reference/'>OWL2 Short Guide</a>):
     * 1) data or object universal or existential quantifications (restrictions):
     * <ul>
     * <li>{@code _:x rdf:type owl:Restriction; owl:onProperty P; owl:allValuesFrom C}</li>
     * <li>{@code _:x rdf:type owl:Restriction; owl:onProperty R; owl:someValuesFrom D}</li>
     * </ul>
     * 2) property domains:
     * <ul>
     * <li>{@code A rdfs:domain U}</li>
     * <li>{@code P rdfs:domain C}</li>
     * <li>{@code R rdfs:domain C}</li>
     * </ul>
     * 3) property ranges:
     * <ul>
     * <li>{@code A rdfs:range U}</li>
     * <li>{@code R rdfs:range D}</li>
     * <li>{@code P rdfs:range C}</li>
     * </ul>
     * 4) property assertions (during reasoning the {@code C owl:hasKey ( P1 ... Pm R1 ... Rn )}
     * and {@code U rdf:type owl:FunctionalProperty} are used):
     * <ul>
     * <li>{@code s A t}</li>
     * <li>{@code a R v}</li>
     * <li>{@code a1 PN a2}</li>
     * </ul>
     * 5) other expression and property constructions where one part could be determined from some another:
     * <ul>
     * <li>{@code C1 owl:equivalentClass C2}</li>
     * <li>{@code DN owl:equivalentClass D}</li>
     * <li>{@code P1 owl:equivalentProperty P2}</li>
     * <li>{@code R1 owl:propertyDisjointWith R2}</li>
     * <li>{@code A1 rdfs:subPropertyOf A2}</li>
     * <li>{@code P1 rdfs:subPropertyOf P2}</li>
     * <li>{@code R1 rdfs:subPropertyOf R2}</li>
     * <li>{@code _:x owl:unionOf ( D1 ... Dn )}</li>
     * <li>{@code _:x owl:intersectionOf ( C1 ... Cn )}</li>
     * <li>{@code _:x rdf:type owl:AllDisjointProperties; owl:members ( P1 ... Pn )}</li>
     * </ul>
     * <p>
     * Note: ObjectProperty &amp; ClassExpression have more priority then DataProperty &amp; DataRange
     */
    @SuppressWarnings("WeakerAccess")
    public static class ReasonerDeclarator extends BaseDeclarator {
        protected final int maxRerunCount;
        // a strategy decider
        protected Strategy decider;
        // Map with statements and functions to rerun
        protected final Map<Statement, Function<Statement, Res>> rerun;
        // result of processing
        protected Set<Statement> unparsed = new HashSet<>();

        public ReasonerDeclarator(Graph graph) {
            this(graph, DefaultStrategies.FIRST, 10);
        }

        public ReasonerDeclarator(Graph graph, Strategy decider, int count) {
            this(graph, new LinkedHashMap<>(), decider, count);
        }

        /**
         * Base constructor.
         *
         * @param graph   {@link Graph}
         * @param rerun   {@link Map}
         * @param decider {@link Strategy}
         * @param count   int
         */
        protected ReasonerDeclarator(Graph graph,
                                     Map<Statement, Function<Statement, Res>> rerun,
                                     Strategy decider, int count) {
            super(graph);
            if (count <= 0) throw new IllegalArgumentException();
            this.rerun = Objects.requireNonNull(rerun);
            this.decider = Objects.requireNonNull(decider);
            this.maxRerunCount = count;
        }

        protected void parse(Statement s, Function<Statement, Res> function) {
            Res res = function.apply(s);
            if (res != Res.UNKNOWN) {
                return;
            }
            rerun.put(s, function);
        }

        @Override
        public void perform() {
            try {
                parse();
                unparsed.addAll(parseTail());
            } finally { // possibility to rerun
                rerun.clear();
            }
        }

        protected void parse() {
            parseDataAndObjectRestrictions();
            parsePropertyDomains();
            parseRanges();
            parsePropertyAssertions();

            parseEquivalentClasses();
            parseUnionAndIntersectionClassExpressions();
            parseEquivalentAndDisjointProperties();
            parseAllDisjointProperties();
            parseSubProperties();
        }

        protected Set<Statement> parseTail() {
            Map<Statement, Function<Statement, Res>> prev = new LinkedHashMap<>(rerun);
            Map<Statement, Function<Statement, Res>> next = new LinkedHashMap<>();
            int count = 0;
            while (count++ < maxRerunCount) {
                for (Statement s : prev.keySet()) {
                    Function<Statement, Res> func = prev.get(s);
                    if (Res.UNKNOWN == func.apply(s)) {
                        next.put(s, prev.get(s));
                    }
                }
                if (next.isEmpty()) {
                    return Collections.emptySet();
                }
                if (next.size() == prev.size()) {
                    decider = decider.next();
                }
                if (decider == null) {
                    break;
                }
                prev = next;
                next = new LinkedHashMap<>();
            }
            LOGGER.warn("Ambiguous statements {}", next.keySet());
            return next.keySet();
        }

        @Override
        public Stream<Triple> uncertainTriples() {
            return unparsed.stream().map(FrontsTriple::asTriple);
        }

        protected void parseDataAndObjectRestrictions() {
            // "_:x rdf:type owl:Restriction; owl:onProperty P; owl:allValuesFrom C" and
            // "_:x rdf:type owl:Restriction; owl:onProperty R; owl:someValuesFrom D"
            Iter.flatMap(Iter.of(OWL.allValuesFrom, OWL.someValuesFrom),
                    p -> listStatements(null, p, null)) // add sorting to process punnings in restrictions
                    .forEachRemaining(s -> parse(s, this::testRestrictions));
        }

        protected Res testRestrictions(Statement statement) {
            Resource p = getObjectResource(statement.getSubject(), OWL.onProperty);
            Resource c = getObjectResource(statement.getSubject(), statement.getPredicate());
            if (p == null || c == null) {
                return Res.FALSE;
            }
            declare(statement.getSubject(), OWL.Restriction);
            if (isClassExpression(c) || isObjectPropertyExpression(p)) {
                declareObjectProperty(p);
                if (declareClass(c))
                    return Res.TRUE;
            }
            if (isDataRange(c) || isDataProperty(p)) {
                declareDataProperty(p).declareDatatype(c);
                return Res.TRUE;
            }
            if (decider.chooseClass()) {
                if (canBeClass(c)) {
                    declareObjectProperty(p).declareClass(c);
                } else if (canBeDatatype(c)) {
                    declareDataProperty(p).declareDatatype(c);
                } else {
                    return Res.FALSE;
                }
                return Res.TRUE;
            }
            return Res.UNKNOWN;
        }

        protected void parsePropertyDomains() {
            // "P rdfs:domain C" or "R rdfs:domain C" or "A rdfs:domain U"
            listStatements(null, RDFS.domain, null)
                    .filterKeep(s -> s.getObject().isResource())
                    .forEachRemaining(s -> parse(s, this::testDomains));
        }

        public Res testDomains(Statement statement) {
            Resource left = statement.getSubject();
            Resource right = statement.getResource();
            if (isAnnotationProperty(left) && right.isURIResource()) {
                return Res.TRUE;
            }
            if (isDataProperty(left) || isObjectPropertyExpression(left)) {
                if (declareClass(right))
                    return Res.TRUE;
            }
            if (right.isAnon()) {
                declareClass(right);
            }
            if (decider.chooseAnnotationProperty()) {
                declareAnnotationProperty(left);
                return Res.TRUE;
            }
            return Res.UNKNOWN;
        }

        protected void parseRanges() {
            // "P rdfs:range C" or "R rdfs:range D" or "A rdfs:range U"
            listStatements(null, RDFS.range, null)
                    .filterKeep(s -> s.getObject().isResource())
                    .forEachRemaining(s -> parse(s, this::testPropertyRanges));
        }

        protected Res testPropertyRanges(Statement statement) {
            Resource left = statement.getSubject();
            Resource right = statement.getObject().asResource();
            if (isAnnotationProperty(left) && right.isURIResource()) {
                // "A rdfs:range U"
                return Res.TRUE;
            }
            if (isClassExpression(right)) {
                // "P rdfs:range C"
                declareObjectProperty(left);
                return Res.TRUE;
            }
            if (isDataRange(right)) {
                // "R rdfs:range D"
                declareDataProperty(left);
                return Res.TRUE;
            }
            if (decider.chooseAnnotationProperty()) {
                declareAnnotationProperty(left);
                return Res.TRUE;
            }
            return Res.UNKNOWN;
        }

        protected void parsePropertyAssertions() {
            // "a1 PN a2", "a R v", "s A t"
            // to avoid ConcurrentModificationException:
            Map<Resource, Resource> add = new HashMap<>();
            Map<Resource, Resource> del = new HashMap<>();
            ReasonerDeclarator self = this;
            new ReasonerDeclarator(getGraph(), rerun, decider, maxRerunCount) {
                @Override
                protected ReasonerDeclarator declare(Resource subject, Resource type) {
                    add.put(subject, type);
                    return this;
                }

                @Override
                protected ReasonerDeclarator undeclare(Resource subject, Resource type) {
                    del.put(subject, type);
                    return this;
                }

                private void parse(Statement s) {
                    Res res = super.testPropertyAssertions(s);
                    if (res != Res.UNKNOWN) {
                        return;
                    }
                    rerun.put(s, self::testPropertyAssertions);
                }

                @Override
                protected void parsePropertyAssertions() {
                    listStatements(null, null, null)
                            .filterDrop(s -> builtins.reservedProperties().contains(s.getPredicate()))
                            .forEachRemaining(this::parse);
                }
            }.parsePropertyAssertions();
            add.forEach(this::declare);
            del.forEach(this::undeclare);
        }

        protected Res testPropertyAssertions(Statement statement) {
            Resource subject = statement.getSubject();
            RDFNode right = statement.getObject();
            Property property = statement.getPredicate();
            if (isAnnotationProperty(property)) { // annotation assertion ("s A t")
                return Res.TRUE;
            }
            if (right.isLiteral()) { // data property assertion ("a R v")
                if (isDataProperty(property)) {
                    declareIndividual(subject);
                    return Res.TRUE;
                }
                if (isIndividual(subject) && canBeDataPropertyInAssertion(property)) {
                    declareDataProperty(property);
                    return Res.TRUE;
                }
                if (mustBeDataOrObjectProperty(property)) {
                    declareDataProperty(property);
                    declareIndividual(subject);
                    return Res.TRUE;
                }
            } else {
                Resource object = right.asResource();
                if (isIndividual(object) || canBeIndividual(object)) {  // object property assertion ("a1 PN a2")
                    if (isObjectPropertyExpression(property)) {
                        declareIndividual(subject);
                        declareIndividual(object);
                        return Res.TRUE;
                    }
                    if (isIndividual(subject)) {
                        declareObjectProperty(property);
                        declareIndividual(object);
                        return Res.TRUE;
                    }
                    if (mustBeDataOrObjectProperty(property)) {
                        declareObjectProperty(property);
                        declareIndividual(subject);
                        declareIndividual(object);
                        return Res.TRUE;
                    }
                }
            }
            if (decider.chooseAnnotationProperty()) {
                declareAnnotationProperty(property);
                return Res.TRUE;
            }
            return Res.UNKNOWN;
        }

        protected boolean mustBeDataOrObjectProperty(Resource candidate) {
            // "P rdf:type owl:FunctionalProperty", "R rdf:type owl:FunctionalProperty"
            if (candidate.hasProperty(RDF.type, OWL.FunctionalProperty)) return true;
            // "C owl:hasKey (P1 ... Pm R1 ... Rn)"
            ExtendedIterator<RDFList> lists = listStatements(null, OWL.hasKey, null)
                    .mapWith(Statement::getObject)
                    .filterKeep(o -> o.canAs(RDFList.class))
                    .mapWith(o -> o.as(RDFList.class));
            return Iter.anyMatch(Iter.flatMap(lists, RDFList::iterator), candidate::equals);
        }

        protected boolean canBeIndividual(RDFNode candidate) {
            return candidate.isResource() && (candidate.isAnon() ? !candidate.canAs(RDFList.class) :
                    !builtins.reserved().contains(candidate.asResource()));
        }

        protected boolean canBeDataPropertyInAssertion(Property candidate) {
            // if the property participates in assertion where the right part is non-plain literal:
            return Iter.findFirst(listStatements(null, candidate, null)
                    .filterKeep(x -> x.getObject().isLiteral())
                    .mapWith(Statement::getLiteral)
                    .filterDrop(s -> XSDDatatype.XSDstring.equals(s.getDatatype())))
                    .isPresent();
        }

        protected boolean canBeClass(Resource resource) {
            return builtins.classes().contains(resource) || !builtins.datatypes().contains(resource);
        }

        protected boolean canBeDatatype(Resource resource) {
            return builtins.datatypes().contains(resource) || !builtins.classes().contains(resource);
        }

        protected void parseEquivalentClasses() {
            // "C1 owl:equivalentClass C2" and "DN owl:equivalentClass D"
            listStatements(null, OWL.equivalentClass, null)
                    .filterKeep(s -> s.getObject().isResource())
                    .forEachRemaining(s -> parse(s, this::testEquivalentClasses));
        }

        protected Res testEquivalentClasses(Statement statement) {
            Resource a = statement.getSubject();
            Resource b = statement.getObject().asResource();
            if (Iter.anyMatch(Iter.of(a, b), this::isClassExpression)) {
                declareClass(a);
                declareClass(b);
                return Res.TRUE;
            }
            if (a.isURIResource() && Iter.anyMatch(Iter.of(a, b), this::isDataRange)) {
                declareDatatype(a);
                declareDatatype(b);
                return Res.TRUE;
            }
            return Res.UNKNOWN;
        }

        protected void parseUnionAndIntersectionClassExpressions() {
            // "_:x owl:unionOf ( D1 ... Dn )", "_:x owl:intersectionOf ( C1 ... Cn )"
            Iter.flatMap(Iter.of(OWL.unionOf, OWL.intersectionOf), p -> listStatements(null, p, null))
                    .filterKeep(s -> s.getSubject().isAnon() && s.getObject().canAs(RDFList.class))
                    .forEachRemaining(s -> parse(s, this::testUnionAndIntersectionClassExpressions));
        }

        protected Res testUnionAndIntersectionClassExpressions(Statement statement) {
            Set<Resource> set = subjectAndObjects(statement).toSet();
            if (set.stream().anyMatch(this::isClassExpression)) {
                set.forEach(this::declareClass);
                return Res.TRUE;
            }
            if (set.stream().anyMatch(this::isDataRange)) {
                set.forEach(this::declareDatatype);
                return Res.TRUE;
            }
            if (decider.chooseClass()) {
                if (set.stream().allMatch(this::canBeClass)) {
                    set.forEach(this::declareClass);
                } else if (set.stream().allMatch(this::canBeDatatype)) {
                    set.forEach(this::declareDatatype);
                } else {
                    return Res.FALSE;
                }
                return Res.TRUE;
            }
            return Res.UNKNOWN;
        }

        protected void parseEquivalentAndDisjointProperties() {
            Iter.flatMap(Iter.of(OWL.equivalentProperty, OWL.propertyDisjointWith), p -> listStatements(null, p, null))
                    .filterKeep(s -> s.getObject().isResource())
                    .forEachRemaining(s -> parse(s, this::testEquivalentAndDisjointProperties));
        }

        protected Res testEquivalentAndDisjointProperties(Statement statement) {
            Resource a = statement.getSubject();
            Resource b = statement.getResource();
            if (Stream.of(a, b).anyMatch(this::isObjectPropertyExpression)) {
                declareObjectProperty(a, builtins.properties());
                declareObjectProperty(b, builtins.properties());
                return Res.TRUE;
            }
            if (Stream.of(a, b).anyMatch(this::isDataProperty)) {
                declareDataProperty(a, builtins.properties());
                declareDataProperty(b, builtins.properties());
                return Res.TRUE;
            }
            return Res.UNKNOWN;
        }

        protected void parseAllDisjointProperties() {
            // "_:x rdf:type owl:AllDisjointProperties; owl:members ( P1 ... Pn )"
            listStatements(null, RDF.type, OWL.AllDisjointProperties)
                    .filterKeep(s -> s.getSubject().isAnon() && s.getSubject().hasProperty(OWL.members))
                    .forEachRemaining(s -> parse(s, this::testAllDisjointProperties));
        }

        protected Res testAllDisjointProperties(Statement statement) {
            Set<Resource> set = members(statement.getSubject(), OWL.members).toSet();
            if (set.isEmpty()) {
                return Res.FALSE;
            }
            if (set.stream().anyMatch(this::isObjectPropertyExpression)) {
                set.forEach(this::declareObjectProperty);
                return Res.TRUE;
            }
            if (set.stream().anyMatch(this::isDataProperty)) {
                set.forEach(this::declareDataProperty);
                return Res.TRUE;
            }
            return Res.UNKNOWN;
        }

        protected void parseSubProperties() {
            listStatements(null, RDFS.subPropertyOf, null)
                    .filterKeep(s -> s.getObject().isResource())
                    .forEachRemaining(s -> parse(s, this::testSubProperties));
        }

        protected Res testSubProperties(Statement statement) {
            Resource a = statement.getSubject();
            Resource b = statement.getResource();
            Res res = Res.UNKNOWN;
            if (Stream.of(a, b).anyMatch(this::isObjectPropertyExpression)) {
                declareObjectProperty(a, builtins.properties());
                declareObjectProperty(b, builtins.properties());
                res = Res.TRUE;
            }
            if (Stream.of(a, b).anyMatch(this::isDataProperty)) {
                declareDataProperty(a, builtins.properties());
                declareDataProperty(b, builtins.properties());
                res = Res.TRUE;
            }
            if (Stream.of(a, b).anyMatch(this::isAnnotationProperty) ||
                    (Res.UNKNOWN.equals(res) && decider.chooseAnnotationProperty())) {
                declareAnnotationProperty(a, builtins.properties());
                declareAnnotationProperty(b, builtins.properties());
                res = Res.TRUE;
            }
            return res;
        }

        public enum Res {
            TRUE, // found ad fixed
            FALSE, // found, but can not be fixed
            UNKNOWN, // ambiguous situation
        }

        /**
         * A control interface, which determines the behaviour in ambiguous situations.
         */
        public interface Strategy {
            /**
             * Answers {@code true}
             * if a {@link OWL#AnnotationProperty owl:AnnotationProperty} must be chosen in an ambiguous situation.
             *
             * @return boolean
             */
            boolean chooseAnnotationProperty();

            /**
             * Answers {@code true} if a class or datatype (in that order) must be chosen in an ambiguous situation.
             *
             * @return boolean
             */
            boolean chooseClass();

            /**
             * Returns a different {@link Strategy} to process in next iteration.
             *
             * @return {@link Strategy}.
             */
            Strategy next();
        }

        public enum DefaultStrategies implements Strategy {
            FIRST(false, false) {
                @Override
                public Strategy next() {
                    return SECOND;
                }
            },
            SECOND(false, true) {
                @Override
                public Strategy next() {
                    return THIRD;
                }
            },
            THIRD(true, true) {
                @Override
                public Strategy next() {
                    return null;
                }
            };

            private final boolean chooseAnnotationProperty;
            private final boolean chooseClass;

            DefaultStrategies(boolean chooseAnnotationProperty, boolean chooseClass) {
                this.chooseAnnotationProperty = chooseAnnotationProperty;
                this.chooseClass = chooseClass;
            }

            @Override
            public boolean chooseAnnotationProperty() {
                return chooseAnnotationProperty;
            }

            @Override
            public boolean chooseClass() {
                return chooseClass;
            }

            @Override
            public abstract Strategy next();
        }
    }

    /**
     * The collection of base methods for {@link ManifestDeclarator} and {@link ReasonerDeclarator}
     */
    @SuppressWarnings("WeakerAccess")
    public static abstract class BaseDeclarator extends Transform {
        private static final List<Property> RESTRICTION_PROPERTY_MARKERS = Stream.of(OWL.onProperty, OWL.allValuesFrom,
                OWL.someValuesFrom, OWL.hasValue, OWL.onClass,
                OWL.onDataRange, OWL.cardinality, OWL.qualifiedCardinality,
                OWL.maxCardinality, OWL.maxQualifiedCardinality, OWL.minCardinality,
                OWL.maxQualifiedCardinality, OWL.onProperties).collect(Iter.toUnmodifiableList());

        private static final List<Property> ANONYMOUS_CLASS_MARKERS = Stream.of(OWL.intersectionOf, OWL.oneOf,
                OWL.unionOf, OWL.complementOf).collect(Iter.toUnmodifiableList());

        protected BaseDeclarator(Graph graph) {
            super(graph);
        }

        protected ExtendedIterator<Resource> subjectAndObjects(Statement s) {
            return Iter.concat(Iter.of(s.getSubject()), s.getObject().as(RDFList.class).iterator()
                    .filterKeep(RDFNode::isResource)
                    .mapWith(RDFNode::asResource));
        }

        protected ExtendedIterator<Resource> members(Resource subject, Property predicate) {
            return members(subject, predicate, Resource.class);
        }

        @SuppressWarnings("SameParameterValue")
        protected <X extends RDFNode> ExtendedIterator<X> members(Resource subject,
                                                                  Property predicate,
                                                                  Class<X> type) {
            return Iter.flatMap(subject.listProperties(predicate)
                            .mapWith(Statement::getObject).filterKeep(s -> s.canAs(RDFList.class)),
                    m -> m.as(RDFList.class).iterator())
                    .filterKeep(x -> x.canAs(type))
                    .mapWith(x -> x.as(type));
        }

        protected Resource getObjectResource(Resource subject, Property predicate) {
            Statement res = subject.getProperty(predicate);
            return res != null && res.getObject().isResource() ? res.getObject().asResource() : null;
        }

        @SuppressWarnings("SameParameterValue")
        protected Literal getObjectLiteral(Resource subject, Property predicate) {
            Statement res = subject.getProperty(predicate);
            return res != null && res.getObject().isLiteral() ? res.getObject().asLiteral() : null;
        }

        protected boolean isClassExpression(Resource candidate) {
            return builtins.classes().contains(candidate) || hasType(candidate, OWL.Class) || hasType(candidate, OWL.Restriction);
        }

        protected boolean isDataRange(Resource candidate) {
            return builtins.datatypes().contains(candidate) || hasType(candidate, RDFS.Datatype);
        }

        @SuppressWarnings("SuspiciousMethodCalls")
        protected boolean isObjectPropertyExpression(Resource candidate) {
            return builtins.objectProperties().contains(candidate)
                    || hasType(candidate, OWL.ObjectProperty)
                    || candidate.hasProperty(OWL.inverseOf);
        }

        @SuppressWarnings("SuspiciousMethodCalls")
        protected boolean isDataProperty(Resource candidate) {
            return builtins.datatypeProperties().contains(candidate) || hasType(candidate, OWL.DatatypeProperty);
        }

        @SuppressWarnings("SuspiciousMethodCalls")
        protected boolean isAnnotationProperty(Resource candidate) {
            return builtins.annotationProperties().contains(candidate) || hasType(candidate, OWL.AnnotationProperty);
        }

        protected boolean isIndividual(Resource candidate) {
            return hasType(candidate, OWL.NamedIndividual) || hasType(candidate, AVC.AnonymousIndividual);
        }

        protected BaseDeclarator declareObjectProperty(Resource resource) {
            declareObjectProperty(resource, builtins.objectProperties());
            return this;
        }

        protected BaseDeclarator declareDataProperty(Resource resource) {
            declareDataProperty(resource, builtins.datatypeProperties());
            return this;
        }

        @SuppressWarnings("UnusedReturnValue")
        protected BaseDeclarator declareAnnotationProperty(Resource resource) {
            declareAnnotationProperty(resource, builtins.annotationProperties());
            return this;
        }

        protected void declareObjectProperty(Resource resource, Set<? extends Resource> builtIn) {
            if (resource.isAnon()) {
                undeclare(resource, OWL.ObjectProperty);
                return;
            }
            declare(resource, OWL.ObjectProperty, builtIn);
        }

        protected void declareDataProperty(Resource resource, Set<? extends Resource> builtIn) {
            declare(resource, OWL.DatatypeProperty, builtIn);
        }

        protected void declareAnnotationProperty(Resource resource, Set<? extends Resource> builtIn) {
            declare(resource, OWL.AnnotationProperty, builtIn);
        }

        protected BaseDeclarator declareIndividual(Resource resource) {
            if (resource.isAnon()) {
                // test data from owl-api-contact contains such things also:
                undeclare(resource, OWL.NamedIndividual);
                // the temporary declaration:
                declare(resource, AVC.AnonymousIndividual);
            } else {
                declare(resource, OWL.NamedIndividual);
            }
            return this;
        }

        protected BaseDeclarator declareDatatype(Resource resource) {
            declare(resource, RDFS.Datatype, builtins.datatypes());
            return this;
        }

        protected boolean declareClass(Resource resource) {
            if (builtins.classes().contains(resource)) {
                return true;
            }
            if (builtins.datatypes().contains(resource)) {
                return false;
            }
            Resource type = resource.isURIResource() ? OWL.Class :
                    containsClassExpressionProperty(resource) ? OWL.Class :
                            containsRestrictionProperty(resource) ? OWL.Restriction : null;
            if (type != null) {
                declare(resource, type);
                return true;
            }
            return false;
        }

        protected boolean containsClassExpressionProperty(Resource candidate) {
            return hasAnyPredicate(candidate, ANONYMOUS_CLASS_MARKERS);
        }

        protected boolean containsRestrictionProperty(Resource candidate) {
            return hasAnyPredicate(candidate, RESTRICTION_PROPERTY_MARKERS);
        }

        protected void declare(Resource subject, Resource type, Set<? extends Resource> forbidden) {
            if (type == null || forbidden.contains(subject)) {
                return;
            }
            declare(subject, type);
        }

        @Override
        protected BaseDeclarator declare(Resource s, Resource t) {
            super.declare(s, t);
            return this;
        }

    }

}
