/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2020, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.owlapi.tests.decomposition;

import com.github.owlcs.owlapi.OWLManager;
import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapitools.decomposition.AxiomWrapper;
import org.semanticweb.owlapitools.decomposition.SemanticLocalityChecker;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

@Disabled
public class SemanticLocalityTestCase {

    private static final String NS = "urn:test#";
    private final OWLDataFactory df = OWLManager.getOWLDataFactory();
    private final OWLClass a = df.getOWLClass(IRI.create(NS, "a"));
    private final OWLClass b = df.getOWLClass(IRI.create(NS, "b"));
    private final OWLClass c = df.getOWLClass(IRI.create(NS, "c"));
    private final OWLClass d = df.getOWLClass(IRI.create(NS, "d"));
    private final OWLAnnotationProperty g = df.getOWLAnnotationProperty(IRI.create(NS, "g"));
    private final OWLAnnotationProperty h = df.getOWLAnnotationProperty(IRI.create(NS, "h"));
    private final OWLDatatype i = df.getOWLDatatype(IRI.create(NS, "i"));
    private final OWLLiteral j = df.getOWLLiteral(true);
    private final OWLLiteral l = df.getOWLLiteral(3.5D);
    private final OWLObjectProperty p = df.getOWLObjectProperty(IRI.create(NS, "p"));
    private final OWLObjectProperty q = df.getOWLObjectProperty(IRI.create(NS, "q"));
    private final OWLObjectProperty r = df.getOWLObjectProperty(IRI.create(NS, "r"));
    private final OWLDataProperty s = df.getOWLDataProperty(IRI.create(NS, "s"));
    private final OWLDataProperty t = df.getOWLDataProperty(IRI.create(NS, "t"));
    private final OWLDataProperty v = df.getOWLDataProperty(IRI.create(NS, "v"));
    private final OWLNamedIndividual x = df.getOWLNamedIndividual(IRI.create(NS, "x"));
    private final OWLNamedIndividual y = df.getOWLNamedIndividual(IRI.create(NS, "y"));
    private final OWLNamedIndividual z = df.getOWLNamedIndividual(IRI.create(NS, "z"));
    private final OWLClass owlNothing = df.getOWLNothing();
    private final OWLClass owlThing = df.getOWLThing();
    private final OWLDataProperty bottomData = df.getOWLBottomDataProperty();
    private final OWLDataProperty topData = df.getOWLTopDataProperty();
    private final OWLObjectProperty bottomObject = df.getOWLBottomObjectProperty();
    private final OWLObjectProperty topObject = df.getOWLTopObjectProperty();
    private OWLAxiom axiom;
    private SemanticLocalityChecker testSubject;

    @SuppressWarnings("ConstantConditions")
    @BeforeEach
    public void setUp() {
        // XXX add a reasoner factory
        testSubject = new SemanticLocalityChecker(null, TestBase.createOWLManager());
    }

    @Test
    public void testShouldBeLocalOWLDeclarationAxiom() {
        // declare a
        axiom = df.getOWLDeclarationAxiom(a);
        // signature intersects
        test(axiom, true, a);
        // signature does not intersect
        test(axiom, true, b);
    }

    @Test
    public void testShouldBeLocalOWLEquivalentClassesAxiom() {
        axiom = df.getOWLEquivalentClassesAxiom(a, b);
        // signature intersects
        test(axiom, false, a);
        // signature does not intersect
        // test(axiom, true, c);
        // illegal axiom
        test(df.getOWLEquivalentClassesAxiom(a), true, a);
        // include bottom
        test(df.getOWLEquivalentClassesAxiom(owlNothing, a, b), false, a);
        // include top
        test(df.getOWLEquivalentClassesAxiom(owlThing, a, b), false, a);
        // include bottom and top
        test(df.getOWLEquivalentClassesAxiom(owlNothing, owlThing, a, b), false, a);
    }

    @Test
    public void testShouldBeLocalOWLDisjointClassesAxiom() {
        axiom = df.getOWLDisjointClassesAxiom(a, b);
        // signature intersects
        // test(axiom, true, a);
        // signature does not intersect
        // test(axiom, true, c);
        axiom = df.getOWLDisjointClassesAxiom(a, b, c);
        // signature intersects
        test(axiom, false, a, b);
        // signature does not intersect
        // test(axiom, true, d);
        // include top
        test(df.getOWLDisjointClassesAxiom(owlThing, a, b), false, a);
    }

    @Test
    public void testShouldBeLocalOWLDisjointUnionAxiom() {
        axiom = disjointUnion(a, b, c);
        // signature intersects
        test(axiom, false, a);
        // signature does not intersect
        // test(axiom, true, d);
        // partition top
        axiom = disjointUnion(owlThing, b, c);
        // signature intersects
        test(axiom, false, b);
        // partition top
        axiom = disjointUnion(owlThing, b, owlThing);
        // signature intersects
        test(axiom, false, b);
    }

    /**
     * @return disjoint union of superclass and classes
     */
    private OWLDisjointUnionAxiom disjointUnion(OWLClass superclass, OWLClass... classes) {
        return df.getOWLDisjointUnionAxiom(superclass, new HashSet<OWLClassExpression>(Arrays.asList(classes)));
    }

    @Test
    public void testShouldBeLocalOWLEquivalentObjectPropertiesAxiom() {
        axiom = df.getOWLEquivalentObjectPropertiesAxiom(p, q);
        // signature intersects
        test(axiom, false, p);
        // signature does not intersect
        // test(axiom, true, r);
        // illegal axiom
        test(df.getOWLEquivalentObjectPropertiesAxiom(q), true, q);
    }

    @Test
    public void testShouldBeLocalOWLEquivalentDataPropertiesAxiom() {
        axiom = df.getOWLEquivalentDataPropertiesAxiom(s, t);
        // signature intersects
        test(axiom, false, s);
        // signature does not intersect
        // test(axiom, true, v);
        // illegal axiom
        test(df.getOWLEquivalentDataPropertiesAxiom(v), true, v);
    }

    @Test
    public void testShouldBeLocalOWLDisjointObjectPropertiesAxiom() {
        axiom = df.getOWLDisjointObjectPropertiesAxiom(p, q);
        // signature intersects
        // test(axiom, true, p);
        test(axiom, false, true, p);
        // signature does not intersect
        test(axiom, false, true, r);
        // top locality sig
        test(df.getOWLDisjointObjectPropertiesAxiom(p, q), false, true, p);
        // top property
        test(df.getOWLDisjointObjectPropertiesAxiom(p, q, topObject), false, p);
        // bottom property
        // test(df.getOWLDisjointObjectPropertiesAxiom(p, q, bottomObject),
        // true, p);
    }

    @Test
    public void testShouldBeLocalOWLDisjointDataPropertiesAxiom() {
        axiom = df.getOWLDisjointDataPropertiesAxiom(s, t);
        // signature intersects
        // test(axiom, true, s);
        // signature does not intersect
        // test(axiom, true, v);
        // top locality
        test(axiom, false, true, p);
        // top property
        test(df.getOWLDisjointDataPropertiesAxiom(topData, s, t), false, s);
    }

    @Test
    public void testShouldBeLocalOWLSameIndividualAxiom() {
        axiom = df.getOWLSameIndividualAxiom(x, y);
        // signature intersects
        test(axiom, false, x);
        // signature does not intersect
        test(axiom, false, z);
    }

    @Test
    public void testShouldBeLocalOWLDifferentIndividualsAxiom() {
        axiom = df.getOWLDifferentIndividualsAxiom(x, y);
        // signature intersects
        test(axiom, false, x);
        // signature does not intersect
        test(axiom, false, z);
    }

    @Test
    public void testShouldBeLocalOWLInverseObjectPropertiesAxiom() {
        axiom = df.getOWLInverseObjectPropertiesAxiom(p, q);
        // signature intersects
        test(axiom, false, p);
        // signature does not intersect
        // test(axiom, true, r);
        // top property
        axiom = df.getOWLInverseObjectPropertiesAxiom(p, topObject);
        // test(axiom, false, true, p);
        axiom = df.getOWLInverseObjectPropertiesAxiom(topObject, p);
        test(axiom, false, true, p);
    }

    @Test
    public void testShouldBeLocalOWLSubObjectPropertyOfAxiom() {
        axiom = df.getOWLSubObjectPropertyOfAxiom(p, q);
        // signature intersects
        test(axiom, false, p);
        // signature does not intersect
        // test(axiom, true, r);
        // top property
        axiom = df.getOWLSubObjectPropertyOfAxiom(p, topObject);
        test(axiom, true, p);
        axiom = df.getOWLSubObjectPropertyOfAxiom(topObject, p);
        test(axiom, false, p);
    }

    @Test
    public void testShouldBeLocalOWLSubDataPropertyOfAxiom() {
        axiom = df.getOWLSubDataPropertyOfAxiom(s, t);
        // signature intersects
        test(axiom, false, s);
        // signature does not intersect
        // test(axiom, true, v);
        // top property
        axiom = df.getOWLSubDataPropertyOfAxiom(v, topData);
        // signature intersects
        test(axiom, true, v);
        axiom = df.getOWLSubDataPropertyOfAxiom(topData, v);
        test(axiom, false, v);
    }

    @Test
    public void testShouldBeLocalOWLObjectPropertyDomainAxiom() {
        axiom = df.getOWLObjectPropertyDomainAxiom(p, a);
        // signature intersects
        test(axiom, true, a);
        // signature does not intersect
        test(axiom, true, d);
        // top class
        axiom = df.getOWLObjectPropertyDomainAxiom(p, owlThing);
        test(axiom, true, p);
        // bottom property
        axiom = df.getOWLObjectPropertyDomainAxiom(bottomObject, a);
        test(axiom, true, a);
    }

    @Test
    public void testShouldBeLocalOWLDataPropertyDomainAxiom() {
        axiom = df.getOWLDataPropertyDomainAxiom(s, a);
        // signature intersects
        test(axiom, true, a);
        // signature does not intersect
        test(axiom, true, d);
        // top class
        axiom = df.getOWLDataPropertyDomainAxiom(v, owlThing);
        test(axiom, true, v);
        // bottom property
        axiom = df.getOWLDataPropertyDomainAxiom(bottomData, owlThing);
        test(axiom, true, a);
    }

    @Test
    public void testShouldBeLocalOWLObjectPropertyRangeAxiom() {
        axiom = df.getOWLObjectPropertyRangeAxiom(p, a);
        // signature intersects
        // test(axiom, true, a);
        // signature does not intersect
        // test(axiom, true, d);
    }

    @Test
    public void testShouldBeLocalOWLDataPropertyRangeAxiom() {
        axiom = df.getOWLDataPropertyRangeAxiom(s, i);
        // signature intersects
        // test(axiom, false, s);
        // signature does not intersect
        // test(axiom, true, p);
    }

    @Test
    public void testShouldBeLocalOWLTransitiveObjectPropertyAxiom() {
        axiom = df.getOWLTransitiveObjectPropertyAxiom(p);
        // signature intersects
        test(axiom, false, p);
        // signature does not intersect
        // test(axiom, true, q);
    }

    @Test
    public void testShouldBeLocalOWLReflexiveObjectPropertyAxiom() {
        axiom = df.getOWLReflexiveObjectPropertyAxiom(p);
        // signature intersects
        test(axiom, false, p);
        // signature does not intersect
        test(axiom, false, q);
    }

    @Test
    public void testShouldBeLocalOWLIrreflexiveObjectPropertyAxiom() {
        axiom = df.getOWLIrreflexiveObjectPropertyAxiom(p);
        // signature intersects
        test(axiom, false, p);
        // signature does not intersect
        // test(axiom, true, q);
    }

    @Test
    public void testShouldBeLocalOWLSymmetricObjectPropertyAxiom() {
        axiom = df.getOWLSymmetricObjectPropertyAxiom(p);
        // signature intersects
        test(axiom, false, p);
        // signature does not intersect
        // test(axiom, true, q);
    }

    @Test
    public void testShouldBeLocalOWLAsymmetricObjectPropertyAxiom() {
        axiom = df.getOWLAsymmetricObjectPropertyAxiom(p);
        // signature intersects
        test(axiom, false, p);
        // signature does not intersect
        // test(axiom, true, q);
    }

    @Test
    public void testShouldBeLocalOWLFunctionalObjectPropertyAxiom() {
        axiom = df.getOWLFunctionalObjectPropertyAxiom(p);
        // signature intersects
        test(axiom, false, p);
        // signature does not intersect
        // test(axiom, true, q);
    }

    @Test
    public void testShouldBeLocalOWLFunctionalDataPropertyAxiom() {
        axiom = df.getOWLFunctionalDataPropertyAxiom(s);
        // signature intersects
        test(axiom, false, s);
        // signature does not intersect
        // test(axiom, true, t);
    }

    @Test
    public void testShouldBeLocalOWLInverseFunctionalObjectPropertyAxiom() {
        axiom = df.getOWLInverseFunctionalObjectPropertyAxiom(p);
        // signature intersects
        test(axiom, false, p);
        // signature does not intersect
        // test(axiom, true, q);
    }

    @Test
    public void testShouldBeLocalOWLSubClassOfAxiom() {
        axiom = df.getOWLSubClassOfAxiom(a, b);
        // signature intersects
        test(axiom, false, a);
        // signature does not intersect
        // test(axiom, true, d);
    }

    @Test
    public void testShouldBeLocalOWLClassAssertionAxiom() {
        axiom = df.getOWLClassAssertionAxiom(a, x);
        // signature intersects
        test(axiom, false, a);
        // signature does not intersect
        test(axiom, false, d);
    }

    @Test
    public void testShouldBeLocalOWLObjectPropertyAssertionAxiom() {
        axiom = df.getOWLObjectPropertyAssertionAxiom(p, y, z);
        // signature intersects
        test(axiom, false, p);
        // signature does not intersect
        test(axiom, false, x);
    }

    @Test
    public void testShouldBeLocalOWLNegativeObjectPropertyAssertionAxiom() {
        axiom = df.getOWLNegativeObjectPropertyAssertionAxiom(p, x, y);
        // signature intersects
        test(axiom, false, p);
        // signature does not intersect
        // test(axiom, true, z);
    }

    @Test
    public void testShouldBeLocalOWLDataPropertyAssertionAxiom() {
        axiom = df.getOWLDataPropertyAssertionAxiom(s, x, l);
        // signature intersects
        test(axiom, false, s);
        // signature does not intersect
        test(axiom, false, p);
    }

    @Test
    public void testShouldBeLocalOWLNegativeDataPropertyAssertionAxiom() {
        axiom = df.getOWLNegativeDataPropertyAssertionAxiom(s, x, j);
        // signature intersects
        test(axiom, false, s);
        // signature does not intersect
        // test(axiom, true, p);
    }

    @Test
    public void testShouldBeLocalOWLAnnotationAssertionAxiom() {
        axiom = df.getOWLAnnotationAssertionAxiom(a.getIRI(), df.getOWLAnnotation(g, l));
        // signature intersects
        test(axiom, true, g);
        // signature does not intersect
        test(axiom, true, b);
    }

    @Test
    public void testShouldBeLocalOWLSubAnnotationPropertyOfAxiom() {
        axiom = df.getOWLSubAnnotationPropertyOfAxiom(g, h);
        // signature intersects
        test(axiom, true, g);
        // signature does not intersect
        test(axiom, true, p);
    }

    @Test
    public void testShouldBeLocalOWLAnnotationPropertyDomainAxiom() {
        axiom = df.getOWLAnnotationPropertyDomainAxiom(g, a.getIRI());
        // signature intersects
        test(axiom, true, g);
        // signature does not intersect
        test(axiom, true, h);
    }

    @Test
    public void testShouldBeLocalOWLAnnotationPropertyRangeAxiom() {
        axiom = df.getOWLAnnotationPropertyRangeAxiom(g, a.getIRI());
        // signature intersects
        test(axiom, true, g);
        // signature does not intersect
        test(axiom, true, h);
    }

    @Test
    public void testShouldBeLocalOWLSubPropertyChainOfAxiom() {
        axiom = df.getOWLSubPropertyChainOfAxiom(Arrays.asList(p, q), r);
        // signature intersects
        // test(axiom, true, p);
        // signature does not intersect
        // test(axiom, true, s);
        // signature equals
        test(axiom, false, p, q, r);
        // top property
        axiom = df.getOWLSubPropertyChainOfAxiom(Arrays.asList(p, q), topObject);
        // signature intersects
        test(axiom, true, p);
    }

    @Test
    public void testShouldBeLocalOWLHasKeyAxiom() {
        axiom = df.getOWLHasKeyAxiom(a, p, s);
        // signature intersects
        test(axiom, true, a);
        // signature does not intersect
        test(axiom, true, q);
    }

    @Test
    public void testShouldBeLocalOWLDatatypeDefinitionAxiom() {
        axiom = df.getOWLDatatypeDefinitionAxiom(i, df.getOWLDatatypeMinMaxExclusiveRestriction(1, 3));
        // signature intersects
        // test(axiom, true, i);
        // signature does not intersect
        // test(axiom, true, d);
    }

    @Test
    public void testShouldBeLocalswrlRule() {
        Set<SWRLAtom> head = new HashSet<>(Collections.singletonList(df.getSWRLClassAtom(a, df.getSWRLIndividualArgument(x))));
        Set<SWRLAtom> body = new HashSet<>(Collections.singletonList(df.getSWRLClassAtom(b, df.getSWRLIndividualArgument(y))));
        axiom = df.getSWRLRule(head, body);
        // signature intersects
        test(axiom, true, a);
        // signature does not intersect
        test(axiom, true, d);
    }

    @Test
    public void testShouldResetSignature() {
        OWLSubClassOfAxiom ax = df.getOWLSubClassOfAxiom(a, b);
        testSubject.preprocessOntology(Collections.singletonList(new AxiomWrapper(ax)));
    }

    private void set(OWLEntity... entities) {
        testSubject.getSignature().addAll(Stream.of(entities));
    }

    private void test(OWLAxiom ax, boolean expected, OWLEntity... entities) {
        testSubject.preprocessOntology(Collections.singletonList(new AxiomWrapper(ax)));
        set(entities);
        boolean local = testSubject.local(ax);
        Assertions.assertEquals(expected, local);
    }

    @SuppressWarnings("SameParameterValue")
    private void test(OWLAxiom ax, boolean expected, boolean locality, OWLEntity... entities) {
        testSubject.preprocessOntology(Collections.singletonList(new AxiomWrapper(ax)));
        set(entities);
        testSubject.getSignature().setLocality(locality);
        boolean local = testSubject.local(ax);
        Assertions.assertEquals(expected, local);
    }
}
