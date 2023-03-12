/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2023, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.github.owlcs.owlapi.tests.api.ontology;

import com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory;
import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAsymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLDifferentIndividualsAxiom;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalDataPropertyAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLInverseFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLIrreflexiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLNegativeDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLNegativeObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLReflexiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLSameIndividualAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubDataPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLTransitiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.semanticweb.owlapi.search.Filters;
import org.semanticweb.owlapi.search.Searcher;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Matthew Horridge, The University Of Manchester, Information Management Group
 */
public class OWLOntologyAccessorsTestCase extends TestBase {

    private static boolean contains(Stream<?> s, Object o) {
        return s.anyMatch(x -> x.equals(o));
    }

    private static void performAxiomTests(OWLOntology ont, OWLAxiom... axioms) {
        Assertions.assertEquals(ont.getAxiomCount(), axioms.length);
        for (OWLAxiom ax : axioms) {
            Assertions.assertTrue(contains(ont.axioms(), ax));
            if (ax.isLogicalAxiom()) {
                Assertions.assertTrue(contains(ont.logicalAxioms(), ax));
            }
            Assertions.assertEquals(ont.getLogicalAxiomCount(), axioms.length);
            AxiomType<?> axiomType = ax.getAxiomType();
            Assertions.assertTrue(contains(ont.axioms(axiomType), ax));
            Assertions.assertTrue(contains(ont.axioms(axiomType, Imports.INCLUDED), ax));
            Assertions.assertEquals(ont.getAxiomCount(axiomType), axioms.length);
            Assertions.assertEquals(ont.getAxiomCount(axiomType, Imports.INCLUDED), axioms.length);
            ax.signature().forEach(e -> {
                Assertions.assertTrue(contains(ont.referencingAxioms(e), ax));
                Assertions.assertTrue(contains(ont.signature(), e));
            });
        }
    }

    @Test
    public void testSubClassOfAxiomAccessors() {
        OWLOntology ont = getOWLOntology();
        OWLClass clsA = OWLFunctionalSyntaxFactory.Class(iri("A"));
        OWLClass clsB = OWLFunctionalSyntaxFactory.Class(iri("B"));
        OWLObjectProperty prop = OWLFunctionalSyntaxFactory.ObjectProperty(iri("prop"));
        OWLOntologyManager man = ont.getOWLOntologyManager();
        OWLSubClassOfAxiom ax = OWLFunctionalSyntaxFactory.SubClassOf(clsA, clsB);
        man.addAxiom(ont, ax);
        OWLSubClassOfAxiom ax2 = OWLFunctionalSyntaxFactory.SubClassOf(clsA,
                OWLFunctionalSyntaxFactory.ObjectSomeValuesFrom(prop, clsB));
        man.addAxiom(ont, ax2);
        performAxiomTests(ont, ax, ax2);
        Assertions.assertTrue(contains(ont.subClassAxiomsForSubClass(clsA), ax));
        Assertions.assertTrue(contains(ont.subClassAxiomsForSuperClass(clsB), ax));
        Assertions.assertTrue(contains(ont.axioms(clsA), ax));
        Assertions.assertTrue(contains(Searcher.sup(ont.axioms(Filters.subClassWithSub, clsA, Imports.INCLUDED)), clsB));
        Assertions.assertTrue(contains(Searcher.sub(ont.axioms(Filters.subClassWithSuper, clsB, Imports.INCLUDED)), clsA));
    }

    @Test
    public void testEquivalentClassesAxiomAccessors() {
        OWLOntology ont = getOWLOntology();
        OWLClass clsA = OWLFunctionalSyntaxFactory.Class(iri("A"));
        OWLClass clsB = OWLFunctionalSyntaxFactory.Class(iri("B"));
        OWLClass clsC = OWLFunctionalSyntaxFactory.Class(iri("C"));
        OWLClass clsD = OWLFunctionalSyntaxFactory.Class(iri("D"));
        OWLObjectProperty prop = OWLFunctionalSyntaxFactory.ObjectProperty(iri("prop"));
        OWLOntologyManager man = ont.getOWLOntologyManager();
        OWLEquivalentClassesAxiom ax = OWLFunctionalSyntaxFactory.EquivalentClasses(clsA, clsB, clsC,
                OWLFunctionalSyntaxFactory.ObjectSomeValuesFrom(prop, clsD));
        man.addAxiom(ont, ax);
        performAxiomTests(ont, ax);
        Assertions.assertTrue(contains(ont.equivalentClassesAxioms(clsA), ax));
        Assertions.assertTrue(contains(ont.equivalentClassesAxioms(clsB), ax));
        Assertions.assertTrue(contains(ont.equivalentClassesAxioms(clsC), ax));
        Assertions.assertTrue(contains(ont.axioms(clsA), ax));
        Assertions.assertTrue(contains(ont.axioms(clsB), ax));
        Assertions.assertTrue(contains(ont.axioms(clsC), ax));
    }

    @Test
    public void testDisjointClassesAxiomAccessors() {
        OWLOntology ont = getOWLOntology();
        OWLClass clsA = OWLFunctionalSyntaxFactory.Class(iri("A"));
        OWLClass clsB = OWLFunctionalSyntaxFactory.Class(iri("B"));
        OWLClass clsC = OWLFunctionalSyntaxFactory.Class(iri("C"));
        OWLClass clsD = OWLFunctionalSyntaxFactory.Class(iri("D"));
        OWLObjectProperty prop = OWLFunctionalSyntaxFactory.ObjectProperty(iri("prop"));
        OWLOntologyManager man = ont.getOWLOntologyManager();
        OWLDisjointClassesAxiom ax = OWLFunctionalSyntaxFactory.DisjointClasses(clsA, clsB, clsC,
                OWLFunctionalSyntaxFactory.ObjectSomeValuesFrom(prop, clsD));
        man.addAxiom(ont, ax);
        performAxiomTests(ont, ax);
        Assertions.assertTrue(contains(ont.disjointClassesAxioms(clsA), ax));
        Assertions.assertTrue(contains(ont.disjointClassesAxioms(clsB), ax));
        Assertions.assertTrue(contains(ont.disjointClassesAxioms(clsC), ax));
        Assertions.assertTrue(contains(ont.axioms(clsA), ax));
        Assertions.assertTrue(contains(ont.axioms(clsB), ax));
        Assertions.assertTrue(contains(ont.axioms(clsC), ax));
    }

    @Test
    public void testSubObjectPropertyOfAxiomAccessors() {
        OWLOntology ont = getOWLOntology();
        OWLObjectProperty propP = OWLFunctionalSyntaxFactory.ObjectProperty(iri("p"));
        OWLObjectProperty propQ = OWLFunctionalSyntaxFactory.ObjectProperty(iri("q"));
        OWLOntologyManager man = ont.getOWLOntologyManager();
        OWLSubObjectPropertyOfAxiom ax = OWLFunctionalSyntaxFactory.SubObjectPropertyOf(propP, propQ);
        man.addAxiom(ont, ax);
        performAxiomTests(ont, ax);
        Assertions.assertTrue(contains(ont.objectSubPropertyAxiomsForSubProperty(propP), ax));
        Assertions.assertTrue(contains(ont.objectSubPropertyAxiomsForSuperProperty(propQ), ax));
        Assertions.assertTrue(contains(ont.axioms(propP), ax));
    }

    @Test
    public void testEquivalentObjectPropertiesAxiomAccessors() {
        OWLOntology ont = getOWLOntology();
        OWLObjectProperty propP = OWLFunctionalSyntaxFactory.ObjectProperty(iri("p"));
        OWLObjectProperty propQ = OWLFunctionalSyntaxFactory.ObjectProperty(iri("q"));
        OWLObjectProperty propR = OWLFunctionalSyntaxFactory.ObjectProperty(iri("r"));
        OWLOntologyManager man = ont.getOWLOntologyManager();
        OWLEquivalentObjectPropertiesAxiom ax = OWLFunctionalSyntaxFactory.EquivalentObjectProperties(propP, propQ, propR);
        man.addAxiom(ont, ax);
        performAxiomTests(ont, ax);
        Assertions.assertTrue(contains(ont.equivalentObjectPropertiesAxioms(propP), ax));
        Assertions.assertTrue(contains(ont.equivalentObjectPropertiesAxioms(propQ), ax));
        Assertions.assertTrue(contains(ont.equivalentObjectPropertiesAxioms(propR), ax));
        Assertions.assertTrue(contains(ont.axioms(propP), ax));
        Assertions.assertTrue(contains(ont.axioms(propQ), ax));
        Assertions.assertTrue(contains(ont.axioms(propR), ax));
    }

    @Test
    public void testDisjointObjectPropertiesAxiomAccessors() {
        OWLOntology ont = getOWLOntology();
        OWLObjectProperty propP = OWLFunctionalSyntaxFactory.ObjectProperty(iri("p"));
        OWLObjectProperty propQ = OWLFunctionalSyntaxFactory.ObjectProperty(iri("q"));
        OWLObjectProperty propR = OWLFunctionalSyntaxFactory.ObjectProperty(iri("r"));
        OWLOntologyManager man = ont.getOWLOntologyManager();
        OWLDisjointObjectPropertiesAxiom ax = OWLFunctionalSyntaxFactory.DisjointObjectProperties(propP, propQ, propR);
        man.addAxiom(ont, ax);
        performAxiomTests(ont, ax);
        Assertions.assertTrue(contains(ont.disjointObjectPropertiesAxioms(propP), ax));
        Assertions.assertTrue(contains(ont.disjointObjectPropertiesAxioms(propQ), ax));
        Assertions.assertTrue(contains(ont.disjointObjectPropertiesAxioms(propR), ax));
        Assertions.assertTrue(contains(ont.axioms(propP), ax));
        Assertions.assertTrue(contains(ont.axioms(propQ), ax));
        Assertions.assertTrue(contains(ont.axioms(propR), ax));
    }

    @Test
    public void testObjectPropertyDomainAxiomAccessors() {
        OWLOntology ont = getOWLOntology();
        OWLObjectProperty propP = OWLFunctionalSyntaxFactory.ObjectProperty(iri("p"));
        OWLClass clsA = OWLFunctionalSyntaxFactory.Class(iri("ClsA"));
        OWLOntologyManager man = ont.getOWLOntologyManager();
        OWLObjectPropertyDomainAxiom ax = OWLFunctionalSyntaxFactory.ObjectPropertyDomain(propP, clsA);
        man.addAxiom(ont, ax);
        performAxiomTests(ont, ax);
        Assertions.assertTrue(contains(ont.objectPropertyDomainAxioms(propP), ax));
        Assertions.assertTrue(contains(ont.axioms(propP), ax));
        Assertions.assertTrue(contains(Searcher.domain(ont.objectPropertyDomainAxioms(propP)), clsA));
    }

    @Test
    public void testObjectPropertyRangeAxiomAccessors() {
        OWLOntology ont = getOWLOntology();
        OWLObjectProperty propP = OWLFunctionalSyntaxFactory.ObjectProperty(iri("p"));
        OWLClass clsA = OWLFunctionalSyntaxFactory.Class(iri("ClsA"));
        OWLOntologyManager man = ont.getOWLOntologyManager();
        OWLObjectPropertyRangeAxiom ax = OWLFunctionalSyntaxFactory.ObjectPropertyRange(propP, clsA);
        man.addAxiom(ont, ax);
        performAxiomTests(ont, ax);
        Assertions.assertTrue(contains(ont.objectPropertyRangeAxioms(propP), ax));
        Assertions.assertTrue(contains(ont.axioms(propP), ax));
        Assertions.assertTrue(contains(Searcher.range(ont.objectPropertyRangeAxioms(propP)), clsA));
    }

    @Test
    public void testFunctionalObjectPropertyAxiomAccessors() {
        OWLOntology ont = getOWLOntology();
        OWLObjectProperty propP = OWLFunctionalSyntaxFactory.ObjectProperty(iri("p"));
        OWLOntologyManager man = ont.getOWLOntologyManager();
        OWLFunctionalObjectPropertyAxiom ax = OWLFunctionalSyntaxFactory.FunctionalObjectProperty(propP);
        man.addAxiom(ont, ax);
        performAxiomTests(ont, ax);
        Assertions.assertTrue(contains(ont.functionalObjectPropertyAxioms(propP), ax));
        Assertions.assertTrue(contains(ont.axioms(propP), ax));
        Assertions.assertTrue(EntitySearcher.isFunctional(propP, ont));
    }

    @Test
    public void testInverseFunctionalObjectPropertyAxiomAccessors() {
        OWLOntology ont = getOWLOntology();
        OWLObjectProperty propP = OWLFunctionalSyntaxFactory.ObjectProperty(iri("p"));
        OWLOntologyManager man = ont.getOWLOntologyManager();
        OWLInverseFunctionalObjectPropertyAxiom ax = OWLFunctionalSyntaxFactory.InverseFunctionalObjectProperty(propP);
        man.addAxiom(ont, ax);
        performAxiomTests(ont, ax);
        Assertions.assertTrue(contains(ont.inverseFunctionalObjectPropertyAxioms(propP), ax));
        Assertions.assertTrue(contains(ont.axioms(propP), ax));
        Assertions.assertTrue(EntitySearcher.isInverseFunctional(propP, ont));
    }

    @Test
    public void testTransitiveObjectPropertyAxiomAccessors() {
        OWLOntology ont = getOWLOntology();
        OWLObjectProperty propP = OWLFunctionalSyntaxFactory.ObjectProperty(iri("p"));
        OWLOntologyManager man = ont.getOWLOntologyManager();
        OWLTransitiveObjectPropertyAxiom ax = OWLFunctionalSyntaxFactory.TransitiveObjectProperty(propP);
        man.addAxiom(ont, ax);
        performAxiomTests(ont, ax);
        Assertions.assertTrue(contains(ont.transitiveObjectPropertyAxioms(propP), ax));
        Assertions.assertTrue(contains(ont.axioms(propP), ax));
        Assertions.assertTrue(EntitySearcher.isTransitive(propP, ont));
    }

    @Test
    public void testSymmetricObjectPropertyAxiomAccessors() {
        OWLOntology ont = getOWLOntology();
        OWLObjectProperty propP = OWLFunctionalSyntaxFactory.ObjectProperty(iri("p"));
        OWLOntologyManager man = ont.getOWLOntologyManager();
        OWLSymmetricObjectPropertyAxiom ax = OWLFunctionalSyntaxFactory.SymmetricObjectProperty(propP);
        man.addAxiom(ont, ax);
        performAxiomTests(ont, ax);
        Assertions.assertTrue(contains(ont.symmetricObjectPropertyAxioms(propP), ax));
        Assertions.assertTrue(contains(ont.axioms(propP), ax));
        Assertions.assertTrue(EntitySearcher.isSymmetric(propP, ont));
    }

    @Test
    public void testAsymmetricObjectPropertyAxiomAccessors() {
        OWLOntology ont = getOWLOntology();
        OWLObjectProperty propP = OWLFunctionalSyntaxFactory.ObjectProperty(iri("p"));
        OWLOntologyManager man = ont.getOWLOntologyManager();
        OWLAsymmetricObjectPropertyAxiom ax = OWLFunctionalSyntaxFactory.AsymmetricObjectProperty(propP);
        man.addAxiom(ont, ax);
        performAxiomTests(ont, ax);
        Assertions.assertTrue(contains(ont.asymmetricObjectPropertyAxioms(propP), ax));
        Assertions.assertTrue(contains(ont.axioms(propP), ax));
        Assertions.assertTrue(EntitySearcher.isAsymmetric(propP, ont));
    }

    @Test
    public void testReflexiveObjectPropertyAxiomAccessors() {
        OWLOntology ont = getOWLOntology();
        OWLObjectProperty propP = OWLFunctionalSyntaxFactory.ObjectProperty(iri("p"));
        OWLOntologyManager man = ont.getOWLOntologyManager();
        OWLReflexiveObjectPropertyAxiom ax = OWLFunctionalSyntaxFactory.ReflexiveObjectProperty(propP);
        man.addAxiom(ont, ax);
        performAxiomTests(ont, ax);
        Assertions.assertTrue(contains(ont.reflexiveObjectPropertyAxioms(propP), ax));
        Assertions.assertTrue(contains(ont.axioms(propP), ax));
        Assertions.assertTrue(EntitySearcher.isReflexive(propP, ont));
    }

    @Test
    public void testIrreflexiveObjectPropertyAxiomAccessors() {
        OWLOntology ont = getOWLOntology();
        OWLObjectProperty propP = OWLFunctionalSyntaxFactory.ObjectProperty(iri("p"));
        OWLOntologyManager man = ont.getOWLOntologyManager();
        OWLIrreflexiveObjectPropertyAxiom ax = OWLFunctionalSyntaxFactory.IrreflexiveObjectProperty(propP);
        man.addAxiom(ont, ax);
        performAxiomTests(ont, ax);
        Assertions.assertTrue(contains(ont.irreflexiveObjectPropertyAxioms(propP), ax));
        Assertions.assertTrue(contains(ont.axioms(propP), ax));
        Assertions.assertTrue(EntitySearcher.isIrreflexive(propP, ont));
    }

    @Test
    public void testSubDataPropertyOfAxiomAccessors() {
        OWLOntology ont = getOWLOntology();
        OWLDataProperty propP = OWLFunctionalSyntaxFactory.DataProperty(iri("p"));
        OWLDataProperty propQ = OWLFunctionalSyntaxFactory.DataProperty(iri("q"));
        OWLOntologyManager man = ont.getOWLOntologyManager();
        OWLSubDataPropertyOfAxiom ax = OWLFunctionalSyntaxFactory.SubDataPropertyOf(propP, propQ);
        man.addAxiom(ont, ax);
        performAxiomTests(ont, ax);
        Assertions.assertTrue(contains(ont.dataSubPropertyAxiomsForSubProperty(propP), ax));
        Assertions.assertTrue(contains(ont.dataSubPropertyAxiomsForSuperProperty(propQ), ax));
        Assertions.assertTrue(contains(ont.axioms(propP), ax));
    }

    @Test
    public void testEquivalentDataPropertiesAxiomAccessors() {
        OWLOntology ont = getOWLOntology();
        OWLDataProperty propP = OWLFunctionalSyntaxFactory.DataProperty(iri("p"));
        OWLDataProperty propQ = OWLFunctionalSyntaxFactory.DataProperty(iri("q"));
        OWLDataProperty propR = OWLFunctionalSyntaxFactory.DataProperty(iri("r"));
        OWLOntologyManager man = ont.getOWLOntologyManager();
        OWLEquivalentDataPropertiesAxiom ax = OWLFunctionalSyntaxFactory.EquivalentDataProperties(propP, propQ, propR);
        man.addAxiom(ont, ax);
        performAxiomTests(ont, ax);
        Assertions.assertTrue(contains(ont.equivalentDataPropertiesAxioms(propP), ax));
        Assertions.assertTrue(contains(ont.equivalentDataPropertiesAxioms(propQ), ax));
        Assertions.assertTrue(contains(ont.equivalentDataPropertiesAxioms(propR), ax));
        Assertions.assertTrue(contains(ont.axioms(propP), ax));
        Assertions.assertTrue(contains(ont.axioms(propQ), ax));
        Assertions.assertTrue(contains(ont.axioms(propR), ax));
    }

    @Test
    public void testDisjointDataPropertiesAxiomAccessors() {
        OWLOntology ont = getOWLOntology();
        OWLDataProperty propP = OWLFunctionalSyntaxFactory.DataProperty(iri("p"));
        OWLDataProperty propQ = OWLFunctionalSyntaxFactory.DataProperty(iri("q"));
        OWLDataProperty propR = OWLFunctionalSyntaxFactory.DataProperty(iri("r"));
        OWLOntologyManager man = ont.getOWLOntologyManager();
        OWLDisjointDataPropertiesAxiom ax = OWLFunctionalSyntaxFactory.DisjointDataProperties(propP, propQ, propR);
        man.addAxiom(ont, ax);
        performAxiomTests(ont, ax);
        Assertions.assertTrue(contains(ont.disjointDataPropertiesAxioms(propP), ax));
        Assertions.assertTrue(contains(ont.disjointDataPropertiesAxioms(propQ), ax));
        Assertions.assertTrue(contains(ont.disjointDataPropertiesAxioms(propR), ax));
        Assertions.assertTrue(contains(ont.axioms(propP), ax));
        Assertions.assertTrue(contains(ont.axioms(propQ), ax));
        Assertions.assertTrue(contains(ont.axioms(propR), ax));
    }

    @Test
    public void testDataPropertyDomainAxiomAccessors() {
        OWLOntology ont = getOWLOntology();
        OWLDataProperty propP = OWLFunctionalSyntaxFactory.DataProperty(iri("p"));
        OWLClass clsA = OWLFunctionalSyntaxFactory.Class(iri("ClsA"));
        OWLOntologyManager man = ont.getOWLOntologyManager();
        OWLDataPropertyDomainAxiom ax = OWLFunctionalSyntaxFactory.DataPropertyDomain(propP, clsA);
        man.addAxiom(ont, ax);
        performAxiomTests(ont, ax);
        Assertions.assertTrue(contains(ont.dataPropertyDomainAxioms(propP), ax));
        Assertions.assertTrue(contains(ont.axioms(propP), ax));
        Assertions.assertTrue(contains(Searcher.domain(ont.dataPropertyDomainAxioms(propP)), clsA));
    }

    @Test
    public void testDataPropertyRangeAxiomAccessors() {
        OWLOntology ont = getOWLOntology();
        OWLDataProperty propP = OWLFunctionalSyntaxFactory.DataProperty(iri("p"));
        OWLDatatype dt = OWLFunctionalSyntaxFactory.Datatype(iri("dt"));
        OWLOntologyManager man = ont.getOWLOntologyManager();
        OWLDataPropertyRangeAxiom ax = OWLFunctionalSyntaxFactory.DataPropertyRange(propP, dt);
        man.addAxiom(ont, ax);
        performAxiomTests(ont, ax);
        Assertions.assertTrue(contains(ont.dataPropertyRangeAxioms(propP), ax));
        Assertions.assertTrue(contains(ont.axioms(propP), ax));
        Assertions.assertTrue(contains(Searcher.range(ont.dataPropertyRangeAxioms(propP)), dt));
    }

    @Test
    public void testFunctionalDataPropertyAxiomAccessors() {
        OWLOntology ont = getOWLOntology();
        OWLDataProperty propP = OWLFunctionalSyntaxFactory.DataProperty(iri("p"));
        OWLOntologyManager man = ont.getOWLOntologyManager();
        OWLFunctionalDataPropertyAxiom ax = OWLFunctionalSyntaxFactory.FunctionalDataProperty(propP);
        man.addAxiom(ont, ax);
        performAxiomTests(ont, ax);
        Assertions.assertTrue(contains(ont.functionalDataPropertyAxioms(propP), ax));
        Assertions.assertTrue(contains(ont.axioms(propP), ax));
        Assertions.assertTrue(EntitySearcher.isFunctional(propP, ont));
    }

    @Test
    public void testClassAssertionAxiomAccessors() {
        OWLOntology ont = getOWLOntology();
        OWLClass clsA = OWLFunctionalSyntaxFactory.Class(iri("clsA"));
        OWLNamedIndividual indA = OWLFunctionalSyntaxFactory.NamedIndividual(iri("indA"));
        OWLOntologyManager man = ont.getOWLOntologyManager();
        OWLClassAssertionAxiom ax = OWLFunctionalSyntaxFactory.ClassAssertion(clsA, indA);
        man.addAxiom(ont, ax);
        performAxiomTests(ont, ax);
        Assertions.assertTrue(contains(ont.classAssertionAxioms(indA), ax));
        Assertions.assertTrue(contains(ont.classAssertionAxioms(clsA), ax));
        Assertions.assertTrue(contains(ont.axioms(indA), ax));
        Assertions.assertTrue(contains(Searcher.instances(ont.classAssertionAxioms(indA)), indA));
        Assertions.assertTrue(contains(Searcher.types(ont.classAssertionAxioms(indA)), clsA));
    }

    @Test
    public void testObjectPropertyAxiomAccessors() {
        OWLOntology ont = getOWLOntology();
        OWLObjectProperty prop = OWLFunctionalSyntaxFactory.ObjectProperty(iri("prop"));
        OWLNamedIndividual indA = OWLFunctionalSyntaxFactory.NamedIndividual(iri("indA"));
        OWLNamedIndividual indB = OWLFunctionalSyntaxFactory.NamedIndividual(iri("indB"));
        OWLOntologyManager man = ont.getOWLOntologyManager();
        OWLObjectPropertyAssertionAxiom ax = OWLFunctionalSyntaxFactory.ObjectPropertyAssertion(prop, indA, indB);
        man.addAxiom(ont, ax);
        performAxiomTests(ont, ax);
        Assertions.assertTrue(contains(ont.objectPropertyAssertionAxioms(indA), ax));
        Assertions.assertTrue(contains(ont.axioms(indA), ax));
    }

    @Test
    public void testNegativeObjectPropertyAxiomAccessors() {
        OWLOntology ont = getOWLOntology();
        OWLObjectProperty prop = OWLFunctionalSyntaxFactory.ObjectProperty(iri("prop"));
        OWLNamedIndividual indA = OWLFunctionalSyntaxFactory.NamedIndividual(iri("indA"));
        OWLNamedIndividual indB = OWLFunctionalSyntaxFactory.NamedIndividual(iri("indB"));
        OWLOntologyManager man = ont.getOWLOntologyManager();
        OWLNegativeObjectPropertyAssertionAxiom ax = OWLFunctionalSyntaxFactory.NegativeObjectPropertyAssertion(prop, indA, indB);
        man.addAxiom(ont, ax);
        performAxiomTests(ont, ax);
        Assertions.assertTrue(contains(ont.negativeObjectPropertyAssertionAxioms(indA), ax));
        Assertions.assertTrue(contains(ont.axioms(indA), ax));
    }

    @Test
    public void testDataPropertyAxiomAccessors() {
        OWLOntology ont = getOWLOntology();
        OWLDataProperty prop = OWLFunctionalSyntaxFactory.DataProperty(iri("prop"));
        OWLNamedIndividual indA = OWLFunctionalSyntaxFactory.NamedIndividual(iri("indA"));
        OWLLiteral lit = OWLFunctionalSyntaxFactory.Literal(3);
        OWLOntologyManager man = ont.getOWLOntologyManager();
        OWLDataPropertyAssertionAxiom ax = OWLFunctionalSyntaxFactory.DataPropertyAssertion(prop, indA, lit);
        man.addAxiom(ont, ax);
        performAxiomTests(ont, ax);
        Assertions.assertTrue(contains(ont.dataPropertyAssertionAxioms(indA), ax));
        Assertions.assertTrue(contains(ont.axioms(indA), ax));
    }

    @Test
    public void testNegativeDataPropertyAxiomAccessors() {
        OWLOntology ont = getOWLOntology();
        OWLDataProperty prop = OWLFunctionalSyntaxFactory.DataProperty(iri("prop"));
        OWLNamedIndividual indA = OWLFunctionalSyntaxFactory.NamedIndividual(iri("indA"));
        OWLLiteral lit = OWLFunctionalSyntaxFactory.Literal(3);
        OWLOntologyManager man = ont.getOWLOntologyManager();
        OWLNegativeDataPropertyAssertionAxiom ax = OWLFunctionalSyntaxFactory.NegativeDataPropertyAssertion(prop, indA, lit);
        man.addAxiom(ont, ax);
        performAxiomTests(ont, ax);
        Assertions.assertTrue(contains(ont.negativeDataPropertyAssertionAxioms(indA), ax));
        Assertions.assertTrue(contains(ont.axioms(indA), ax));
    }

    @Test
    public void testSameIndividualAxiomAccessors() {
        OWLOntology ont = getOWLOntology();
        OWLNamedIndividual indA = OWLFunctionalSyntaxFactory.NamedIndividual(iri("indA"));
        OWLNamedIndividual indB = OWLFunctionalSyntaxFactory.NamedIndividual(iri("indB"));
        OWLNamedIndividual indC = OWLFunctionalSyntaxFactory.NamedIndividual(iri("indC"));
        OWLOntologyManager man = ont.getOWLOntologyManager();
        OWLSameIndividualAxiom ax = OWLFunctionalSyntaxFactory.SameIndividual(indA, indB, indC);
        man.addAxiom(ont, ax);
        performAxiomTests(ont, ax);
        Assertions.assertTrue(contains(ont.sameIndividualAxioms(indA), ax));
        Assertions.assertTrue(contains(ont.sameIndividualAxioms(indB), ax));
        Assertions.assertTrue(contains(ont.sameIndividualAxioms(indC), ax));
        Assertions.assertTrue(contains(ont.axioms(indA), ax));
        Collection<OWLObject> equivalent = Searcher.equivalent(ont.sameIndividualAxioms(indA)).collect(Collectors.toSet());
        Assertions.assertTrue(equivalent.contains(indB));
        Assertions.assertTrue(equivalent.contains(indC));
    }

    @Test
    public void testDifferentIndividualsAxiomAccessors() {
        OWLOntology ont = getOWLOntology();
        OWLNamedIndividual indA = OWLFunctionalSyntaxFactory.NamedIndividual(iri("indA"));
        OWLNamedIndividual indB = OWLFunctionalSyntaxFactory.NamedIndividual(iri("indB"));
        OWLNamedIndividual indC = OWLFunctionalSyntaxFactory.NamedIndividual(iri("indC"));
        OWLOntologyManager man = ont.getOWLOntologyManager();
        OWLDifferentIndividualsAxiom ax = OWLFunctionalSyntaxFactory.DifferentIndividuals(indA, indB, indC);
        man.addAxiom(ont, ax);
        performAxiomTests(ont, ax);
        Assertions.assertTrue(contains(ont.differentIndividualAxioms(indA), ax));
        Assertions.assertTrue(contains(ont.differentIndividualAxioms(indB), ax));
        Assertions.assertTrue(contains(ont.differentIndividualAxioms(indC), ax));
        Assertions.assertTrue(contains(ont.axioms(indA), ax));
        Collection<OWLObject> different = Searcher.different(ont.differentIndividualAxioms(indA)).collect(Collectors.toSet());
        Assertions.assertTrue(different.contains(indB));
        Assertions.assertTrue(different.contains(indC));
    }
}
