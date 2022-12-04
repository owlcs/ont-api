/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2022, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.github.owlcs.owlapi.tests.api.axioms;

import com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory;
import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.util.NNF;

/**
 * @author Matthew Horridge, The University of Manchester, Information Management Group
 */
public class NNFTestCase extends TestBase {

    private final OWLClass clsA = OWLFunctionalSyntaxFactory.Class(iri("A"));
    private final OWLClass clsB = OWLFunctionalSyntaxFactory.Class(iri("B"));
    private final OWLClass clsC = OWLFunctionalSyntaxFactory.Class(iri("C"));
    private final OWLClass clsD = OWLFunctionalSyntaxFactory.Class(iri("D"));
    private final OWLObjectProperty propP = OWLFunctionalSyntaxFactory.ObjectProperty(iri("p"));
    private final OWLNamedIndividual indA = OWLFunctionalSyntaxFactory.NamedIndividual(iri("a"));

    private static OWLClassExpression getNNF(OWLClassExpression classExpression) {
        NNF nnf = new NNF(df);
        return classExpression.accept(nnf.getClassVisitor());
    }

    @Test
    public void testPosOWLClass() {
        OWLClass cls = OWLFunctionalSyntaxFactory.Class(iri("A"));
        Assertions.assertEquals(cls.getNNF(), cls);
    }

    @Test
    public void testNegOWLClass() {
        OWLClassExpression cls = OWLFunctionalSyntaxFactory.ObjectComplementOf(OWLFunctionalSyntaxFactory.Class(iri("A")));
        Assertions.assertEquals(cls.getNNF(), cls);
    }

    @Test
    public void testPosAllValuesFrom() {
        OWLClassExpression cls = OWLFunctionalSyntaxFactory.ObjectAllValuesFrom(OWLFunctionalSyntaxFactory.ObjectProperty(iri("p")),
                OWLFunctionalSyntaxFactory.Class(iri("A")));
        Assertions.assertEquals(cls.getNNF(), cls);
    }

    @Test
    public void testNegAllValuesFrom() {
        OWLObjectProperty property = OWLFunctionalSyntaxFactory.ObjectProperty(iri("p"));
        OWLClass filler = OWLFunctionalSyntaxFactory.Class(iri("A"));
        OWLObjectAllValuesFrom allValuesFrom = OWLFunctionalSyntaxFactory.ObjectAllValuesFrom(property, filler);
        OWLClassExpression cls = allValuesFrom.getObjectComplementOf();
        OWLClassExpression nnf = OWLFunctionalSyntaxFactory.ObjectSomeValuesFrom(property, filler.getObjectComplementOf());
        Assertions.assertEquals(cls.getNNF(), nnf);
    }

    @Test
    public void testPosSomeValuesFrom() {
        OWLClassExpression cls = OWLFunctionalSyntaxFactory.ObjectSomeValuesFrom(OWLFunctionalSyntaxFactory.ObjectProperty(iri("p")),
                OWLFunctionalSyntaxFactory.Class(iri("A")));
        Assertions.assertEquals(cls.getNNF(), cls);
    }

    @Test
    public void testNegSomeValuesFrom() {
        OWLObjectProperty property = OWLFunctionalSyntaxFactory.ObjectProperty(iri("p"));
        OWLClass filler = OWLFunctionalSyntaxFactory.Class(iri("A"));
        OWLObjectSomeValuesFrom someValuesFrom = OWLFunctionalSyntaxFactory.ObjectSomeValuesFrom(property, filler);
        OWLClassExpression cls = OWLFunctionalSyntaxFactory.ObjectComplementOf(someValuesFrom);
        OWLClassExpression nnf = OWLFunctionalSyntaxFactory.ObjectAllValuesFrom(property,
                OWLFunctionalSyntaxFactory.ObjectComplementOf(filler));
        Assertions.assertEquals(cls.getNNF(), nnf);
    }

    @Test
    public void testPosObjectIntersectionOf() {
        OWLClassExpression cls = OWLFunctionalSyntaxFactory.ObjectIntersectionOf(OWLFunctionalSyntaxFactory.Class(iri("A")),
                OWLFunctionalSyntaxFactory.Class(iri("B")),
                OWLFunctionalSyntaxFactory.Class(iri("C")));
        Assertions.assertEquals(cls.getNNF(), cls);
    }

    @Test
    public void testNegObjectIntersectionOf() {
        OWLClassExpression cls = OWLFunctionalSyntaxFactory.ObjectComplementOf(OWLFunctionalSyntaxFactory
                .ObjectIntersectionOf(OWLFunctionalSyntaxFactory.Class(iri("A")),
                        OWLFunctionalSyntaxFactory.Class(iri("B")),
                        OWLFunctionalSyntaxFactory.Class(iri("C"))));
        OWLClassExpression nnf = OWLFunctionalSyntaxFactory.ObjectUnionOf(OWLFunctionalSyntaxFactory
                        .ObjectComplementOf(OWLFunctionalSyntaxFactory.Class(iri("A"))),
                OWLFunctionalSyntaxFactory.ObjectComplementOf(OWLFunctionalSyntaxFactory.Class(iri("B"))),
                OWLFunctionalSyntaxFactory.ObjectComplementOf(OWLFunctionalSyntaxFactory.Class(iri("C"))));
        Assertions.assertEquals(cls.getNNF(), nnf);
    }

    @Test
    public void testPosObjectUnionOf() {
        OWLClassExpression cls = OWLFunctionalSyntaxFactory.ObjectUnionOf(OWLFunctionalSyntaxFactory.Class(iri("A")),
                OWLFunctionalSyntaxFactory.Class(iri("B")), OWLFunctionalSyntaxFactory.Class(iri("C")));
        Assertions.assertEquals(cls.getNNF(), cls);
    }

    @Test
    public void testNegObjectUnionOf() {
        OWLClassExpression cls = OWLFunctionalSyntaxFactory.ObjectComplementOf(OWLFunctionalSyntaxFactory
                .ObjectUnionOf(OWLFunctionalSyntaxFactory.Class(iri("A")),
                        OWLFunctionalSyntaxFactory.Class(iri("B")),
                        OWLFunctionalSyntaxFactory.Class(iri("C"))));
        OWLClassExpression nnf = OWLFunctionalSyntaxFactory.ObjectIntersectionOf(OWLFunctionalSyntaxFactory
                        .ObjectComplementOf(OWLFunctionalSyntaxFactory.Class(iri("A"))),
                OWLFunctionalSyntaxFactory.ObjectComplementOf(OWLFunctionalSyntaxFactory.Class(iri("B"))),
                OWLFunctionalSyntaxFactory.ObjectComplementOf(OWLFunctionalSyntaxFactory.Class(iri("C"))));
        Assertions.assertEquals(cls.getNNF(), nnf);
    }

    @Test
    public void testPosObjectMinCardinality() {
        OWLObjectProperty prop = OWLFunctionalSyntaxFactory.ObjectProperty(iri("p"));
        OWLClassExpression filler = OWLFunctionalSyntaxFactory.Class(iri("A"));
        OWLClassExpression cls = OWLFunctionalSyntaxFactory.ObjectMinCardinality(3, prop, filler);
        Assertions.assertEquals(cls.getNNF(), cls);
    }

    @Test
    public void testNegObjectMinCardinality() {
        OWLObjectProperty prop = OWLFunctionalSyntaxFactory.ObjectProperty(iri("p"));
        OWLClassExpression filler = OWLFunctionalSyntaxFactory.Class(iri("A"));
        OWLClassExpression cls = OWLFunctionalSyntaxFactory.ObjectMinCardinality(3, prop, filler).getObjectComplementOf();
        OWLClassExpression nnf = OWLFunctionalSyntaxFactory.ObjectMaxCardinality(2, prop, filler);
        Assertions.assertEquals(cls.getNNF(), nnf);
    }

    @Test
    public void testPosObjectMaxCardinality() {
        OWLObjectProperty prop = OWLFunctionalSyntaxFactory.ObjectProperty(iri("p"));
        OWLClassExpression filler = OWLFunctionalSyntaxFactory.Class(iri("A"));
        OWLClassExpression cls = OWLFunctionalSyntaxFactory.ObjectMaxCardinality(3, prop, filler);
        Assertions.assertEquals(cls.getNNF(), cls);
    }

    @Test
    public void testNegObjectMaxCardinality() {
        OWLObjectProperty prop = OWLFunctionalSyntaxFactory.ObjectProperty(iri("p"));
        OWLClassExpression filler = OWLFunctionalSyntaxFactory.Class(iri("A"));
        OWLClassExpression cls = OWLFunctionalSyntaxFactory.ObjectMaxCardinality(3, prop, filler).getObjectComplementOf();
        OWLClassExpression nnf = OWLFunctionalSyntaxFactory.ObjectMinCardinality(4, prop, filler);
        Assertions.assertEquals(cls.getNNF(), nnf);
    }

    @Test
    public void testNamedClass() {
        OWLClassExpression comp = getNNF(clsA);
        Assertions.assertEquals(clsA, comp);
    }

    @Test
    public void testObjectIntersectionOf() {
        OWLClassExpression desc = OWLFunctionalSyntaxFactory.ObjectIntersectionOf(clsA, clsB);
        OWLClassExpression neg = OWLFunctionalSyntaxFactory.ObjectComplementOf(desc);
        OWLClassExpression nnf = OWLFunctionalSyntaxFactory.ObjectUnionOf(OWLFunctionalSyntaxFactory
                .ObjectComplementOf(clsA), OWLFunctionalSyntaxFactory.ObjectComplementOf(clsB));
        OWLClassExpression comp = getNNF(neg);
        Assertions.assertEquals(nnf, comp);
    }

    @Test
    public void testObjectUnionOf() {
        OWLClassExpression desc = OWLFunctionalSyntaxFactory.ObjectUnionOf(clsA, clsB);
        OWLClassExpression neg = OWLFunctionalSyntaxFactory.ObjectComplementOf(desc);
        OWLClassExpression nnf = OWLFunctionalSyntaxFactory.ObjectIntersectionOf(OWLFunctionalSyntaxFactory
                .ObjectComplementOf(clsA), OWLFunctionalSyntaxFactory.ObjectComplementOf(clsB));
        OWLClassExpression comp = getNNF(neg);
        Assertions.assertEquals(nnf, comp);
    }

    @Test
    public void testDoubleNegation() {
        OWLClassExpression desc = OWLFunctionalSyntaxFactory.ObjectComplementOf(clsA);
        OWLClassExpression neg = OWLFunctionalSyntaxFactory.ObjectComplementOf(desc);
        OWLClassExpression comp = getNNF(neg);
        Assertions.assertEquals(clsA, comp);
    }

    @Test
    public void testTripleNegation() {
        OWLClassExpression desc = OWLFunctionalSyntaxFactory.ObjectComplementOf(OWLFunctionalSyntaxFactory
                .ObjectComplementOf(clsA));
        OWLClassExpression neg = OWLFunctionalSyntaxFactory.ObjectComplementOf(desc);
        OWLClassExpression nnf = OWLFunctionalSyntaxFactory.ObjectComplementOf(clsA);
        OWLClassExpression comp = getNNF(neg);
        Assertions.assertEquals(nnf, comp);
    }

    @Test
    public void testObjectSome() {
        OWLClassExpression desc = OWLFunctionalSyntaxFactory.ObjectSomeValuesFrom(propP, clsA);
        OWLClassExpression neg = OWLFunctionalSyntaxFactory.ObjectComplementOf(desc);
        OWLClassExpression nnf = OWLFunctionalSyntaxFactory.ObjectAllValuesFrom(propP,
                OWLFunctionalSyntaxFactory.ObjectComplementOf(clsA));
        OWLClassExpression comp = getNNF(neg);
        Assertions.assertEquals(nnf, comp);
    }

    @Test
    public void testObjectAll() {
        OWLClassExpression desc = OWLFunctionalSyntaxFactory.ObjectAllValuesFrom(propP, clsA);
        OWLClassExpression neg = OWLFunctionalSyntaxFactory.ObjectComplementOf(desc);
        OWLClassExpression nnf = OWLFunctionalSyntaxFactory.ObjectSomeValuesFrom(propP,
                OWLFunctionalSyntaxFactory.ObjectComplementOf(clsA));
        OWLClassExpression comp = getNNF(neg);
        Assertions.assertEquals(nnf, comp);
    }

    @Test
    public void testObjectHasValue() {
        OWLClassExpression desc = OWLFunctionalSyntaxFactory.ObjectHasValue(propP, indA);
        OWLClassExpression neg = OWLFunctionalSyntaxFactory.ObjectComplementOf(desc);
        OWLClassExpression nnf = OWLFunctionalSyntaxFactory.ObjectAllValuesFrom(propP,
                OWLFunctionalSyntaxFactory.ObjectComplementOf(OWLFunctionalSyntaxFactory.ObjectOneOf(indA)));
        OWLClassExpression comp = getNNF(neg);
        Assertions.assertEquals(nnf, comp);
    }

    @Test
    public void testObjectMin() {
        OWLClassExpression desc = OWLFunctionalSyntaxFactory.ObjectMinCardinality(3, propP, clsA);
        OWLClassExpression neg = OWLFunctionalSyntaxFactory.ObjectComplementOf(desc);
        OWLClassExpression nnf = OWLFunctionalSyntaxFactory.ObjectMaxCardinality(2, propP, clsA);
        OWLClassExpression comp = getNNF(neg);
        Assertions.assertEquals(nnf, comp);
    }

    @Test
    public void testObjectMax() {
        OWLClassExpression desc = OWLFunctionalSyntaxFactory.ObjectMaxCardinality(3, propP, clsA);
        OWLClassExpression neg = OWLFunctionalSyntaxFactory.ObjectComplementOf(desc);
        OWLClassExpression nnf = OWLFunctionalSyntaxFactory.ObjectMinCardinality(4, propP, clsA);
        OWLClassExpression comp = getNNF(neg);
        Assertions.assertEquals(nnf, comp);
    }

    @Test
    public void testNestedA() {
        OWLClassExpression fillerA = OWLFunctionalSyntaxFactory.ObjectUnionOf(clsA, clsB);
        OWLClassExpression opA = OWLFunctionalSyntaxFactory.ObjectSomeValuesFrom(propP, fillerA);
        OWLClassExpression desc = OWLFunctionalSyntaxFactory.ObjectUnionOf(opA, clsB);
        OWLClassExpression nnf = OWLFunctionalSyntaxFactory.ObjectIntersectionOf(OWLFunctionalSyntaxFactory
                        .ObjectComplementOf(clsB),
                OWLFunctionalSyntaxFactory.ObjectAllValuesFrom(propP,
                        OWLFunctionalSyntaxFactory.ObjectIntersectionOf(OWLFunctionalSyntaxFactory.ObjectComplementOf(clsA),
                                OWLFunctionalSyntaxFactory.ObjectComplementOf(clsB))));
        OWLClassExpression neg = OWLFunctionalSyntaxFactory.ObjectComplementOf(desc);
        OWLClassExpression comp = getNNF(neg);
        Assertions.assertEquals(comp, nnf);
    }

    @Test
    public void testNestedB() {
        OWLClassExpression desc = OWLFunctionalSyntaxFactory.ObjectIntersectionOf(OWLFunctionalSyntaxFactory.ObjectIntersectionOf(clsA, clsB), OWLFunctionalSyntaxFactory.ObjectComplementOf(
                OWLFunctionalSyntaxFactory.ObjectUnionOf(clsC, clsD)));
        OWLClassExpression neg = OWLFunctionalSyntaxFactory.ObjectComplementOf(desc);
        OWLClassExpression nnf = OWLFunctionalSyntaxFactory.ObjectUnionOf(OWLFunctionalSyntaxFactory.ObjectUnionOf(OWLFunctionalSyntaxFactory.ObjectComplementOf(clsA), OWLFunctionalSyntaxFactory.ObjectComplementOf(clsB)),
                OWLFunctionalSyntaxFactory.ObjectUnionOf(clsC, clsD));
        OWLClassExpression comp = getNNF(neg);
        Assertions.assertEquals(comp, nnf);
    }
}
