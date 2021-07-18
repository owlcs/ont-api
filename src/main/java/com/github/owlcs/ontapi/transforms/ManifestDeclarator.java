/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2021, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.transforms;

import com.github.owlcs.ontapi.jena.utils.Iter;
import com.github.owlcs.ontapi.jena.utils.Models;
import com.github.owlcs.ontapi.jena.vocabulary.OWL;
import com.github.owlcs.ontapi.jena.vocabulary.RDF;
import com.github.owlcs.ontapi.jena.vocabulary.SWRL;
import com.github.owlcs.ontapi.transforms.vocabulary.AVC;
import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.*;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDFS;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

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
public class ManifestDeclarator extends BaseDeclarator {

    private static final List<Resource> SWRL_ARG1_ATOM_TYPES = List.of(SWRL.ClassAtom,
            SWRL.DatavaluedPropertyAtom, SWRL.IndividualPropertyAtom,
            SWRL.DifferentIndividualsAtom, SWRL.SameIndividualAtom);
    private static final List<Resource> SWRL_ARG2_ATOM_TYPES = List.of(SWRL.IndividualPropertyAtom,
            SWRL.DifferentIndividualsAtom, SWRL.SameIndividualAtom);
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
                    if (Iter.allMatch(Iter.create(values), RDFNode::isLiteral)) {
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
        Set<Resource> res = new HashSet<>(builtins.getSystemResources());
        res.add(AVC.AnonymousIndividual);
        res.removeAll(builtins.getBuiltinClasses());
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
