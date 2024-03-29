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

import com.github.owlcs.ontapi.TestOntSpecifications;
import com.github.owlcs.ontapi.testutils.OWLIOUtils;
import com.github.owlcs.owlapi.OWLManager;
import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyExpression;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.util.OWLAPIStreamUtils;
import org.semanticweb.owlapi.util.OWLEntityRenamer;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.AnnotationAssertion;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.AnnotationProperty;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.AnnotationPropertyDomain;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.AnnotationPropertyRange;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.AsymmetricObjectProperty;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.Class;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.ClassAssertion;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.DataComplementOf;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.DataIntersectionOf;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.DataProperty;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.DataPropertyAssertion;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.DataPropertyDomain;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.DataPropertyRange;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.DataUnionOf;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.Datatype;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.Declaration;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.DisjointClasses;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.DisjointDataProperties;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.DisjointObjectProperties;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.EquivalentClasses;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.EquivalentDataProperties;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.EquivalentObjectProperties;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.FunctionalDataProperty;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.FunctionalObjectProperty;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.InverseFunctionalObjectProperty;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.IrreflexiveObjectProperty;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.Literal;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.NamedIndividual;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.NegativeDataPropertyAssertion;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.NegativeObjectPropertyAssertion;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.ObjectProperty;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.ObjectPropertyAssertion;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.ObjectPropertyDomain;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.ObjectPropertyRange;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.ReflexiveObjectProperty;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.SubAnnotationPropertyOf;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.SubClassOf;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.SubDataPropertyOf;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.SubObjectPropertyOf;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.SymmetricObjectProperty;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.TopDatatype;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.TransitiveObjectProperty;

/**
 * @author Matthew Horridge, The University of Manchester, Information Management Group
 */
public class RenameEntityTestCase extends TestBase {
    @BeforeEach
    public void setupManagersClean() {
        super.setupManagersClean();
        if (OWLManager.DEBUG_USE_OWL) return;
        // to allow DataProperty <-> ObjectProperty intersections
        com.github.owlcs.ontapi.config.OntLoaderConfiguration conf =
                ((com.github.owlcs.ontapi.config.OntLoaderConfiguration) m.getOntologyLoaderConfiguration())
                        .setSpecification(TestOntSpecifications.OWL2_FULL_NO_INF);
        m.setOntologyLoaderConfiguration(conf);
    }

    @Test
    public void testRenameClass() {
        OWLOntology ont = getOWLOntology();
        OWLClass clsAIRI1 = Class(iri("ClsA1"));
        OWLClass clsAIRI2 = Class(iri("ClsA2"));
        OWLClass clsB = Class(iri("ClsB"));
        OWLClass clsC = Class(iri("ClsC"));
        OWLObjectPropertyExpression propA = ObjectProperty(iri("propA"));
        OWLDataPropertyExpression propB = DataProperty(iri("propA"));
        OWLIndividual indA = NamedIndividual(iri("indA"));
        OWLAnnotationProperty annoProp = AnnotationProperty(iri("annoProp"));
        Set<OWLAxiom> axioms1 = new HashSet<>();
        axioms1.add(SubClassOf(clsAIRI1, clsB));
        axioms1.add(EquivalentClasses(clsAIRI1, clsC));
        axioms1.add(DisjointClasses(clsAIRI1, clsC));
        axioms1.add(ObjectPropertyDomain(propA, clsAIRI1));
        axioms1.add(ObjectPropertyRange(propA, clsAIRI1));
        axioms1.add(DataPropertyDomain(propB, clsAIRI1));
        axioms1.add(ClassAssertion(clsAIRI1, indA));
        axioms1.add(AnnotationAssertion(annoProp, clsAIRI1.getIRI(), Literal("X")));
        ont.add(axioms1);

        LOGGER.debug("Before renaming");
        OWLIOUtils.print(ont);
        ont.axioms().forEach(x -> LOGGER.debug(String.valueOf(x)));

        Set<OWLAxiom> axioms2 = new HashSet<>();
        axioms2.add(SubClassOf(clsAIRI2, clsB));
        axioms2.add(EquivalentClasses(clsAIRI2, clsC));
        axioms2.add(DisjointClasses(clsAIRI2, clsC));
        axioms2.add(ObjectPropertyDomain(propA, clsAIRI2));
        axioms2.add(ObjectPropertyRange(propA, clsAIRI2));
        axioms2.add(DataPropertyDomain(propB, clsAIRI2));
        axioms2.add(ClassAssertion(clsAIRI2, indA));
        axioms2.add(AnnotationAssertion(annoProp, clsAIRI2.getIRI(), Literal("X")));

        LOGGER.debug("----------------------------------------------");
        LOGGER.debug("Rename {} -> {}", clsAIRI1, clsAIRI2);
        OWLEntityRenamer entityRenamer = new OWLEntityRenamer(ont.getOWLOntologyManager(), Collections.singleton(ont));
        List<OWLOntologyChange> changes1 = entityRenamer.changeIRI(clsAIRI1, clsAIRI2.getIRI());
        changes1.forEach(x -> LOGGER.debug(String.valueOf(x)));
        ont.getOWLOntologyManager().applyChanges(changes1);

        Set<OWLAxiom> actual1 = ont.axioms().collect(Collectors.toSet());
        Assertions.assertTrue(actual1.containsAll(axioms2), String.format("Incorrect list of axioms after renaming %s -> %s", clsAIRI1, clsAIRI2));

        LOGGER.debug("----------------------------------------------");
        LOGGER.debug("Rename back {} -> {}", clsAIRI2, clsAIRI1);
        List<OWLOntologyChange> changes2 = entityRenamer.changeIRI(clsAIRI2.getIRI(), clsAIRI1.getIRI());
        Assertions.assertEquals(changes1.size(), changes2.size());
        changes2.forEach(x -> LOGGER.debug("{}", x));
        ont.getOWLOntologyManager().applyChanges(changes2);

        Set<OWLAxiom> actual2 = ont.axioms().collect(Collectors.toSet());
        Assertions.assertTrue(actual2.containsAll(axioms1),
                String.format("Incorrect list of axioms after back renaming %s -> %s", clsAIRI2, clsAIRI1));
    }

    @Test
    public void testRenameObjectProperty() {
        OWLOntology ont = getOWLOntology();
        OWLClass clsA = Class(iri("ClsA"));
        OWLObjectProperty propA = ObjectProperty(iri("propA"));
        OWLObjectProperty propA2 = ObjectProperty(iri("propA2"));
        OWLObjectPropertyExpression propB = ObjectProperty(iri("propB")).getInverseProperty();
        OWLIndividual indA = NamedIndividual(iri("indA"));
        OWLIndividual indB = NamedIndividual(iri("indB"));
        OWLAnnotationProperty annoProp = AnnotationProperty(iri("annoProp"));
        Set<OWLAxiom> axioms1 = new HashSet<>();
        axioms1.add(SubObjectPropertyOf(propA, propB));
        axioms1.add(EquivalentObjectProperties(propA, propB));
        axioms1.add(DisjointObjectProperties(propA, propB));
        axioms1.add(ObjectPropertyDomain(propA, clsA));
        axioms1.add(ObjectPropertyRange(propA, clsA));
        axioms1.add(FunctionalObjectProperty(propA));
        axioms1.add(InverseFunctionalObjectProperty(propA));
        axioms1.add(SymmetricObjectProperty(propA));
        axioms1.add(AsymmetricObjectProperty(propA));
        axioms1.add(TransitiveObjectProperty(propA));
        axioms1.add(ReflexiveObjectProperty(propA));
        axioms1.add(IrreflexiveObjectProperty(propA));
        axioms1.add(ObjectPropertyAssertion(propA, indA, indB));
        axioms1.add(NegativeObjectPropertyAssertion(propA, indA, indB));
        axioms1.add(AnnotationAssertion(annoProp, propA.getIRI(), Literal("X")));
        ont.add(axioms1);

        LOGGER.debug("Before renaming");
        OWLIOUtils.print(ont);
        ont.axioms().forEach(x -> LOGGER.debug(String.valueOf(x)));

        Set<OWLAxiom> axioms2 = new HashSet<>();
        axioms2.add(SubObjectPropertyOf(propA2, propB));
        axioms2.add(EquivalentObjectProperties(propA2, propB));
        axioms2.add(DisjointObjectProperties(propA2, propB));
        axioms2.add(ObjectPropertyDomain(propA2, clsA));
        axioms2.add(ObjectPropertyRange(propA2, clsA));
        axioms2.add(FunctionalObjectProperty(propA2));
        axioms2.add(InverseFunctionalObjectProperty(propA2));
        axioms2.add(SymmetricObjectProperty(propA2));
        axioms2.add(AsymmetricObjectProperty(propA2));
        axioms2.add(TransitiveObjectProperty(propA2));
        axioms2.add(ReflexiveObjectProperty(propA2));
        axioms2.add(IrreflexiveObjectProperty(propA2));
        axioms2.add(ObjectPropertyAssertion(propA2, indA, indB));
        axioms2.add(NegativeObjectPropertyAssertion(propA2, indA, indB));
        axioms2.add(AnnotationAssertion(annoProp, propA2.getIRI(), Literal("X")));

        LOGGER.debug("----------------------------------------------");
        LOGGER.debug("Rename {} -> {}", propA, propA2);
        OWLEntityRenamer entityRenamer = new OWLEntityRenamer(ont.getOWLOntologyManager(), Collections.singleton(ont));
        List<OWLOntologyChange> changes1 = entityRenamer.changeIRI(propA, propA2.getIRI());
        changes1.forEach(x -> LOGGER.debug(String.valueOf(x)));
        ont.getOWLOntologyManager().applyChanges(changes1);
        Assertions.assertEquals(ont.axioms().collect(Collectors.toSet()), axioms2);

        LOGGER.debug("----------------------------------------------");
        LOGGER.debug("Rename back {} -> {}", propA2, propA);
        List<OWLOntologyChange> changes2 = entityRenamer.changeIRI(propA2.getIRI(), propA.getIRI());
        Assertions.assertEquals(changes1.size(), changes2.size());
        changes2.forEach(x -> LOGGER.debug(String.valueOf(x)));
        ont.getOWLOntologyManager().applyChanges(changes2);
        Assertions.assertEquals(ont.axioms().collect(Collectors.toSet()), axioms1);
    }

    @Test
    public void testRenameDataProperty() {
        OWLOntology ont = getOWLOntology();
        OWLClass clsA = Class(iri("ClsA"));
        OWLDataProperty propA = DataProperty(iri("propA"));
        OWLDataProperty propA2 = DataProperty(iri("propA2"));
        OWLDataPropertyExpression propB = DataProperty(iri("propB"));
        OWLIndividual indA = NamedIndividual(iri("indA"));
        OWLAnnotationProperty annoProp = AnnotationProperty(iri("annoProp"));
        Set<OWLAxiom> axioms1 = new HashSet<>();
        axioms1.add(SubDataPropertyOf(propA, propB));
        axioms1.add(EquivalentDataProperties(propA, propB));
        axioms1.add(DisjointDataProperties(propA, propB));
        axioms1.add(DataPropertyDomain(propA, clsA));
        axioms1.add(DataPropertyRange(propA, TopDatatype()));
        axioms1.add(FunctionalDataProperty(propA));
        axioms1.add(DataPropertyAssertion(propA, indA, Literal(33)));
        axioms1.add(NegativeDataPropertyAssertion(propA, indA, Literal(44)));
        axioms1.add(AnnotationAssertion(annoProp, propA.getIRI(), Literal("X")));
        ont.add(axioms1);
        Set<OWLAxiom> axioms2 = new HashSet<>();
        axioms2.add(SubDataPropertyOf(propA2, propB));
        axioms2.add(EquivalentDataProperties(propA2, propB));
        axioms2.add(DisjointDataProperties(propA2, propB));
        axioms2.add(DataPropertyDomain(propA2, clsA));
        axioms2.add(DataPropertyRange(propA2, TopDatatype()));
        axioms2.add(FunctionalDataProperty(propA2));
        axioms2.add(DataPropertyAssertion(propA2, indA, Literal(33)));
        axioms2.add(NegativeDataPropertyAssertion(propA2, indA, Literal(44)));
        axioms2.add(AnnotationAssertion(annoProp, propA2.getIRI(), Literal("X")));
        OWLEntityRenamer entityRenamer = new OWLEntityRenamer(ont.getOWLOntologyManager(), Collections.singleton(ont));
        List<OWLOntologyChange> changes = entityRenamer.changeIRI(propA, propA2.getIRI());
        ont.getOWLOntologyManager().applyChanges(changes);
        Assertions.assertEquals(ont.axioms().collect(Collectors.toSet()), axioms2);
        List<OWLOntologyChange> changes2 = entityRenamer.changeIRI(propA2.getIRI(), propA.getIRI());
        ont.getOWLOntologyManager().applyChanges(changes2);
        Assertions.assertEquals(ont.axioms().collect(Collectors.toSet()), axioms1);
    }

    @Test
    public void testRenameIndividual() {
        OWLOntology ont = getOWLOntology();
        OWLClass clsA = Class(iri("ClsA"));
        OWLDataProperty propA = DataProperty(iri("propA"));
        OWLObjectProperty propB = ObjectProperty(iri("propB"));
        OWLNamedIndividual indA = NamedIndividual(iri("indA"));
        OWLNamedIndividual indB = NamedIndividual(iri("indA"));
        OWLAnnotationProperty annoProp = AnnotationProperty(iri("annoProp"));
        Set<OWLAxiom> axioms1 = Sets.newHashSet(ClassAssertion(clsA, indA),
                DataPropertyAssertion(propA, indA, Literal(33)), NegativeDataPropertyAssertion(propA, indA, Literal(44)),
                AnnotationAssertion(annoProp, propA.getIRI(), Literal("X")), ObjectPropertyAssertion(propB, indA, indB),
                NegativeObjectPropertyAssertion(propB, indA, indB));
        ont.add(axioms1);
        Set<OWLAxiom> axioms2 = Sets.newHashSet(ClassAssertion(clsA, indB),
                DataPropertyAssertion(propA, indB, Literal(33)), NegativeDataPropertyAssertion(propA, indB, Literal(44)),
                AnnotationAssertion(annoProp, propA.getIRI(), Literal("X")), ObjectPropertyAssertion(propB, indB, indB),
                NegativeObjectPropertyAssertion(propB, indB, indB));
        OWLEntityRenamer entityRenamer = new OWLEntityRenamer(ont.getOWLOntologyManager(), Collections.singleton(ont));
        List<OWLOntologyChange> changes = entityRenamer.changeIRI(indA, indB.getIRI());
        ont.getOWLOntologyManager().applyChanges(changes);
        Assertions.assertEquals(ont.axioms().collect(Collectors.toSet()), axioms2);
        List<OWLOntologyChange> changes2 = entityRenamer.changeIRI(indB.getIRI(), indA.getIRI());
        ont.getOWLOntologyManager().applyChanges(changes2);
        Assertions.assertEquals(ont.axioms().collect(Collectors.toSet()), axioms1);
    }

    @Test
    public void testRenameDatatype() {
        OWLOntology ont = getOWLOntology();
        OWLDatatype dtA = Datatype(iri("DtA"));
        OWLDatatype dtB = Datatype(iri("DtB"));
        OWLDatatype dtC = Datatype(iri("DtC"));
        OWLDataRange rng1 = DataIntersectionOf(dtA, dtB);
        OWLDataRange rng1R = DataIntersectionOf(dtC, dtB);
        OWLDataRange rng2 = DataUnionOf(dtA, dtB);
        OWLDataRange rng2R = DataUnionOf(dtC, dtB);
        OWLDataRange rng3 = DataComplementOf(dtA);
        OWLDataRange rng3R = DataComplementOf(dtC);
        OWLDataPropertyExpression propB = DataProperty(iri("propA"));
        Set<OWLAxiom> axioms1 = new HashSet<>();
        axioms1.add(DataPropertyRange(propB, rng1));
        axioms1.add(DataPropertyRange(propB, rng2));
        axioms1.add(DataPropertyRange(propB, rng3));
        ont.add(axioms1);
        Set<OWLAxiom> axioms2 = new HashSet<>();
        axioms2.add(DataPropertyRange(propB, rng1R));
        axioms2.add(DataPropertyRange(propB, rng2R));
        axioms2.add(DataPropertyRange(propB, rng3R));
        OWLEntityRenamer entityRenamer = new OWLEntityRenamer(ont.getOWLOntologyManager(), Collections.singleton(ont));
        List<OWLOntologyChange> changes = entityRenamer.changeIRI(dtA, dtC.getIRI());
        ont.getOWLOntologyManager().applyChanges(changes);
        Assertions.assertEquals(ont.axioms().collect(Collectors.toSet()), axioms2);
        List<OWLOntologyChange> changes2 = entityRenamer.changeIRI(dtC.getIRI(), dtA.getIRI());
        ont.getOWLOntologyManager().applyChanges(changes2);
        Assertions.assertTrue(OWLAPIStreamUtils.equalStreams(ont.axioms(), axioms1.stream()));
    }

    @Test
    public void testRenameAnnotationProperty() {
        OWLOntology ont = getOWLOntology();
        OWLNamedIndividual indA = NamedIndividual(iri("indA"));
        OWLNamedIndividual indB = NamedIndividual(iri("indB"));
        OWLAnnotationProperty annoProp = AnnotationProperty(iri("annoProp"));
        OWLAnnotationProperty annoPropR = AnnotationProperty(iri("annoPropR"));
        OWLAnnotationProperty annoProp2 = AnnotationProperty(iri("annoProp2"));
        Set<OWLAxiom> axioms1 = new HashSet<>();
        axioms1.add(Declaration(annoProp));
        axioms1.add(AnnotationAssertion(annoProp, indA.getIRI(), indB.getIRI()));
        axioms1.add(SubAnnotationPropertyOf(annoProp, annoProp2));
        axioms1.add(AnnotationPropertyRange(annoProp, indA.getIRI()));
        axioms1.add(AnnotationPropertyDomain(annoProp, indA.getIRI()));
        ont.add(axioms1);
        Set<OWLAxiom> axioms2 = new HashSet<>();
        axioms2.add(Declaration(annoPropR));
        axioms2.add(AnnotationAssertion(annoPropR, indA.getIRI(), indB.getIRI()));
        axioms2.add(SubAnnotationPropertyOf(annoPropR, annoProp2));
        axioms2.add(AnnotationPropertyRange(annoPropR, indA.getIRI()));
        axioms2.add(AnnotationPropertyDomain(annoPropR, indA.getIRI()));
        OWLEntityRenamer entityRenamer = new OWLEntityRenamer(ont.getOWLOntologyManager(), Collections.singleton(ont));
        List<OWLOntologyChange> changes = entityRenamer.changeIRI(annoProp, annoPropR.getIRI());
        ont.getOWLOntologyManager().applyChanges(changes);
        Assertions.assertEquals(ont.axioms().collect(Collectors.toSet()), axioms2);
        List<OWLOntologyChange> changes2 = entityRenamer.changeIRI(annoPropR.getIRI(), annoProp.getIRI());
        ont.getOWLOntologyManager().applyChanges(changes2);
        Assertions.assertEquals(ont.axioms().collect(Collectors.toSet()), axioms1);
    }
}
