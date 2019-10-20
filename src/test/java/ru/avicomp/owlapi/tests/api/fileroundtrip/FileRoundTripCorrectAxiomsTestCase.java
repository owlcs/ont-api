/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2019, The University of Manchester, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package ru.avicomp.owlapi.tests.api.fileroundtrip;

import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.AxiomAnnotations;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.BufferingMode;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasoner;
import org.semanticweb.owlapi.search.Searcher;
import org.semanticweb.owlapi.vocab.OWLFacet;
import ru.avicomp.ontapi.utils.ReadWriteUtils;
import ru.avicomp.owlapi.OWLFunctionalSyntaxFactory;
import ru.avicomp.owlapi.OWLManager;
import ru.avicomp.owlapi.tests.api.baseclasses.TestBase;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static ru.avicomp.owlapi.OWLFunctionalSyntaxFactory.Class;
import static ru.avicomp.owlapi.OWLFunctionalSyntaxFactory.Float;
import static ru.avicomp.owlapi.OWLFunctionalSyntaxFactory.Integer;
import static ru.avicomp.owlapi.OWLFunctionalSyntaxFactory.*;


@SuppressWarnings("javadoc")
public class FileRoundTripCorrectAxiomsTestCase extends TestBase {

    private OWLDataProperty dp = OWLFunctionalSyntaxFactory.DataProperty(iri("p"));
    private OWLClass clA = Class(iri("A"));
    private OWLObjectProperty or = OWLFunctionalSyntaxFactory.ObjectProperty(iri("r"));
    private OWLObjectProperty oq = OWLFunctionalSyntaxFactory.ObjectProperty(iri("q"));
    private OWLObjectProperty op = OWLFunctionalSyntaxFactory.ObjectProperty(iri("p"));
    private OWLDatatype dt = OWLFunctionalSyntaxFactory.Datatype(iri("B"));
    private OWLClass clB = OWLFunctionalSyntaxFactory.Class(iri("B"));
    private OWLClass classC = OWLFunctionalSyntaxFactory.Class(iri("C"));

    protected void assertEqualsSet(String ontology, OWLAxiom... axioms) {
        Set<OWLAxiom> expected = Sets.newHashSet(axioms);
        LOGGER.debug("Ontology file: " + ontology);
        OWLOntology o = ontologyFromClasspathFile(ontology);
        ru.avicomp.ontapi.utils.ReadWriteUtils.print(o);
        Set<OWLAxiom> actual = o.axioms().collect(Collectors.toSet());
        LOGGER.debug("Actual:");
        actual.forEach(a -> LOGGER.debug("{}", a));
        LOGGER.debug("COUNT: {}", actual.size());
        LOGGER.debug("Expected:");
        expected.forEach(a -> LOGGER.debug("{}", a));
        if (OWLManager.DEBUG_USE_OWL) {
            Assert.assertEquals("Incorrect set of axioms", expected, actual);
        } else { // all explicit declarations are included!
            Assert.assertTrue("Some axioms are absent", actual.containsAll(expected));
        }
    }

    @Test
    public void testCorrectAxiomAnnotatedPropertyAssertions() {
        OWLOntology ontology = ontologyFromClasspathFile("AnnotatedPropertyAssertions.rdf");
        ru.avicomp.ontapi.utils.ReadWriteUtils.print(ontology);
        OWLNamedIndividual subject = OWLFunctionalSyntaxFactory.NamedIndividual(IRI("http://Example.com#", "myBuilding"));
        OWLObjectProperty predicate = OWLFunctionalSyntaxFactory.ObjectProperty(IRI("http://Example.com#", "located_at"));
        OWLNamedIndividual object = OWLFunctionalSyntaxFactory.NamedIndividual(IRI("http://Example.com#", "myLocation"));
        OWLAxiom ax = ObjectPropertyAssertion(predicate, subject, object);
        Assert.assertTrue(ontology.containsAxiom(ax, Imports.EXCLUDED, AxiomAnnotations.IGNORE_AXIOM_ANNOTATIONS));
        Set<OWLAxiom> axioms = ontology.axiomsIgnoreAnnotations(ax, Imports.EXCLUDED).collect(Collectors.toSet());
        Assert.assertEquals(1, axioms.size());
        OWLAxiom theAxiom = axioms.iterator().next();
        Assert.assertTrue(theAxiom.isAnnotated());
    }

    @Test
    public void testContainsComplexSubPropertyAxiom() {
        List<OWLObjectProperty> chain = Arrays.asList(op, oq);
        assertEqualsSet("ComplexSubProperty.rdf", df.getOWLSubPropertyChainOfAxiom(chain, or));
    }

    @Test
    public void testCorrectAxiomsDataAllValuesFrom() {
        assertEqualsSet("DataAllValuesFrom.rdf", SubClassOf(clA, DataAllValuesFrom(dp, dt)), Declaration(dt),
                Declaration(dp));
    }

    @Test
    public void testCorrectAxiomsDataComplementOf() {
        OWLDataRange complement = DataComplementOf(Integer());
        OWLDataPropertyRangeAxiom ax = DataPropertyRange(dp, complement);
        assertEqualsSet("DataComplementOf.rdf", ax, Declaration(dp));
    }

    @Test
    public void testCorrectAxiomsDataHasValue() {
        assertEqualsSet("DataHasValue.rdf", SubClassOf(clA, DataHasValue(dp, Literal(3))), Declaration(dp), SubClassOf(
                clA, DataHasValue(dp, Literal("A", ""))));
    }

    @Test
    public void testCorrectAxiomsDataIntersectionOf() {
        OWLDataRange intersection = DataIntersectionOf(Integer(), Float());
        OWLDataPropertyRangeAxiom ax = DataPropertyRange(dp, intersection);
        assertEqualsSet("DataIntersectionOf.rdf", ax, Declaration(dp));
    }

    @Test
    public void testCorrectAxiomsDataMaxCardinality() {
        assertEqualsSet("DataMaxCardinality.rdf", SubClassOf(clA, DataMaxCardinality(3, dp, TopDatatype())),
                Declaration(dp));
    }

    @Test
    public void testCorrectAxiomsDataMinCardinality() {
        assertEqualsSet("DataMinCardinality.rdf", SubClassOf(clA, DataMinCardinality(3, dp, TopDatatype())),
                Declaration(dp));
    }

    @Test
    public void testCorrectAxiomsDataOneOf() {
        OWLDataRange oneOf = DataOneOf(Literal(30), Literal(31f));
        OWLDataPropertyRangeAxiom ax = DataPropertyRange(dp, oneOf);
        assertEqualsSet("DataOneOf.rdf", ax, Declaration(dp));
    }

    @Test
    public void testCorrectAxiomsDataSomeValuesFrom() {
        assertEqualsSet("DataSomeValuesFrom.rdf", SubClassOf(clA, DataSomeValuesFrom(dp, dt)), Declaration(dt),
                Declaration(dp));
    }

    @Test
    public void testCorrectAxiomsDataUnionOf() {
        OWLDataRange union = DataUnionOf(Integer(), Float());
        OWLDataPropertyRangeAxiom ax = DataPropertyRange(dp, union);
        assertEqualsSet("DataUnionOf.rdf", ax, Declaration(dp));
    }

    @Test
    public void testCorrectAxiomsDatatypeRestriction() {
        OWLDataRange dr = DatatypeRestriction(Integer(), FacetRestriction(OWLFacet.MIN_INCLUSIVE, Literal(18)),
                FacetRestriction(OWLFacet.MAX_INCLUSIVE, Literal(30)));
        OWLDataPropertyRangeAxiom ax = DataPropertyRange(dp, dr);
        assertEqualsSet("DatatypeRestriction.rdf", ax, Declaration(dp));
    }

    @Test
    public void testCorrectAxiomsDeclarations() {
        OWLClass c = Class(IRI("http://www.semanticweb.org/ontologies/declarations#", "Cls"));
        OWLObjectProperty o = ObjectProperty(IRI("http://www.semanticweb.org/ontologies/declarations#", "op"));
        OWLDataProperty d = DataProperty(IRI("http://www.semanticweb.org/ontologies/declarations#", "dp"));
        OWLNamedIndividual i = NamedIndividual(IRI("http://www.semanticweb.org/ontologies/declarations#", "ni"));
        OWLAnnotationProperty ap = AnnotationProperty(IRI("http://www.semanticweb.org/ontologies/declarations#", "ap"));
        OWLDatatype datatype = Datatype(IRI("http://www.semanticweb.org/ontologies/declarations#", "dt"));
        assertEqualsSet("TestDeclarations.rdf", Declaration(c), Declaration(o), Declaration(d), Declaration(i),
                Declaration(ap), Declaration(datatype));
    }

    @Test
    public void testDeprecatedAnnotationAssertionsPresent() {
        OWLOntology ont = ontologyFromClasspathFile("Deprecated.rdf");
        OWLClass cls = Class(IRI("http://www.semanticweb.org/owlapi/test#", "ClsA"));
        Searcher.annotationObjects(ont.annotationAssertionAxioms(cls.getIRI(), Imports.INCLUDED)).forEach(OWLAnnotation::isDeprecatedIRIAnnotation);
        OWLDataProperty prop = DataProperty(IRI("http://www.semanticweb.org/owlapi/test#", "prop"));
        Searcher.annotationObjects(ont.annotationAssertionAxioms(prop.getIRI(), Imports.INCLUDED)).forEach(a -> Assert.assertTrue(a
                .isDeprecatedIRIAnnotation()));
    }

    @Test
    public void testContainsDisjointClasses() {
        assertEqualsSet("DisjointClasses.rdf", DisjointClasses(clA, clB, classC));
    }

    @Test
    public void testCorrectAxiomsHasKey() {
        OWLClass cls = Class(IRI("http://example.com/", "Person"));
        OWLDataProperty propP = DataProperty(IRI("http://example.com/", "dataProperty"));
        OWLObjectProperty propQ = ObjectProperty(IRI("http://example.com/", "objectProperty"));
        assertEqualsSet("HasKey.rdf", HasKey(cls, propQ, propP), Declaration(cls), Declaration(propP), Declaration(
                propQ));
    }

    @Test
    public void testContainsInverseOf() {
        assertEqualsSet("InverseOf.rdf", InverseObjectProperties(op, oq));
    }

    @Test
    public void testCorrectAxiomsObjectAllValuesFrom() {
        assertEqualsSet("ObjectAllValuesFrom.rdf", SubClassOf(clA, ObjectAllValuesFrom(op, clB)), Declaration(clB),
                Declaration(op));
    }

    @Test
    public void testCorrectAxiomsObjectCardinality() {
        assertEqualsSet("ObjectCardinality.rdf", Declaration(op), SubClassOf(clA, ObjectExactCardinality(3, op,
                OWLThing())));
    }

    @Test
    public void testCorrectAxiomsObjectComplementOf() {
        assertEqualsSet("ObjectComplementOf.rdf", SubClassOf(clA, ObjectComplementOf(clB)));
    }

    @Test
    public void testCorrectAxiomsObjectHasSelf() {
        assertEqualsSet("ObjectHasSelf.rdf", SubClassOf(clA, ObjectHasSelf(op)), Declaration(op));
    }

    @Test
    public void testCorrectAxiomsObjectHasValue() {
        assertEqualsSet("ObjectHasValue.rdf", SubClassOf(clA, ObjectHasValue(op, NamedIndividual(iri("a")))),
                Declaration(op));
    }

    @Test
    public void testCorrectAxiomsObjectIntersectionOf() {
        assertEqualsSet("ObjectIntersectionOf.rdf", SubClassOf(clA, ObjectIntersectionOf(clB, classC)));
    }

    @Test
    public void testCorrectAxiomsObjectMaxCardinality() {
        assertEqualsSet("ObjectMaxCardinality.rdf", Declaration(op), SubClassOf(clA, ObjectMaxCardinality(3, op,
                OWLThing())));
    }

    @Test
    public void testCorrectAxiomsObjectMaxQualifiedCardinality() {
        assertEqualsSet("ObjectMaxQualifiedCardinality.rdf", Declaration(op), SubClassOf(clA, ObjectMaxCardinality(3,
                op, clB)));
    }

    @Test
    public void testCorrectAxiomsObjectMinCardinality() {
        assertEqualsSet("ObjectMinCardinality.rdf", Declaration(op), SubClassOf(clA, ObjectMinCardinality(3, op,
                OWLThing())));
    }

    @Test
    public void testCorrectAxiomsObjectMinQualifiedCardinality() {
        assertEqualsSet("ObjectMinQualifiedCardinality.rdf", Declaration(op), SubClassOf(clA, ObjectMinCardinality(3,
                op, clB)));
    }

    @Test
    public void testCorrectAxiomsObjectOneOf() {
        OWLNamedIndividual indA = NamedIndividual(iri("a"));
        OWLNamedIndividual indB = NamedIndividual(iri("b"));
        assertEqualsSet("ObjectOneOf.rdf", SubClassOf(clA, ObjectOneOf(indA, indB)));
    }

    @Test
    public void testCorrectAxiomsObjectQualifiedCardinality() {
        assertEqualsSet("ObjectQualifiedCardinality.rdf", Declaration(op), SubClassOf(clA, ObjectExactCardinality(3, op,
                clB)));
    }

    @Test
    public void testCorrectAxiomsObjectSomeValuesFrom() {
        assertEqualsSet("ObjectSomeValuesFrom.rdf", SubClassOf(clA, ObjectSomeValuesFrom(op, clB)), Declaration(clB),
                Declaration(op));
    }

    @Test
    public void testCorrectAxiomsObjectUnionOf() {
        assertEqualsSet("ObjectUnionOf.rdf", SubClassOf(clA, ObjectUnionOf(clB, classC)));
    }

    @Test
    public void testCorrectAxiomsRDFSClass() {
        OWLOntology ont = ontologyFromClasspathFile("RDFSClass.rdf");
        IRI clsIRI = IRI("http://owlapi.sourceforge.net/ontology#", "ClsA");
        OWLClass cls = Class(clsIRI);
        OWLDeclarationAxiom ax = Declaration(cls);
        Assert.assertTrue(ont.containsAxiom(ax));
    }

    @Test
    public void testStructuralReasonerRecusion() {
        OWLOntology ontology = ontologyFromClasspathFile("koala.owl");
        String ontName = ontology.getOntologyID().getOntologyIRI().get().toString();
        StructuralReasoner reasoner = new StructuralReasoner(ontology, new SimpleConfiguration(),
                BufferingMode.BUFFERING);
        OWLClass cls = Class(IRI(ontName + "#", "Koala"));
        reasoner.getSubClasses(cls, false);
        reasoner.getSuperClasses(cls, false);
    }

    @Test
    public void testCorrectAxiomsSubClassAxiom() {
        assertEqualsSet("SubClassOf.rdf", SubClassOf(clA, clB));
    }

    /**
     * Tests the isGCI method on OWLSubClassAxiom
     */
    @Test
    public void testIsGCIMethodSubClassAxiom() {
        OWLClassExpression desc = ObjectIntersectionOf(clA, classC);
        OWLSubClassOfAxiom ax1 = SubClassOf(clA, clB);
        Assert.assertFalse(ax1.isGCI());
        OWLSubClassOfAxiom ax2 = SubClassOf(desc, clB);
        Assert.assertTrue(ax2.isGCI());
    }

    @Test
    public void testParsedAxiomsSubClassOfUntypedOWLClass() {
        OWLOntology ontology = ontologyFromClasspathFile("SubClassOfUntypedOWLClass.rdf");
        List<OWLSubClassOfAxiom> axioms = ontology.axioms(AxiomType.SUBCLASS_OF).collect(Collectors.toList());
        Assert.assertEquals(1, axioms.size());
        OWLSubClassOfAxiom ax = axioms.iterator().next();
        OWLClass subCls = Class(IRI("http://www.semanticweb.org/owlapi/test#", "A"));
        OWLClass supCls = Class(IRI("http://www.semanticweb.org/owlapi/test#", "B"));
        Assert.assertEquals(subCls, ax.getSubClass());
        Assert.assertEquals(supCls, ax.getSuperClass());
    }

    @Test
    public void testParsedAxiomsSubClassOfUntypedSomeValuesFrom() throws Exception {
        OWLOntology ontology = ontologyFromClasspathFile("SubClassOfUntypedSomeValuesFrom.rdf");
        ReadWriteUtils.print(ontology);
        List<OWLSubClassOfAxiom> axioms = ontology.axioms(AxiomType.SUBCLASS_OF).collect(Collectors.toList());
        Assert.assertEquals(1, axioms.size());
        OWLSubClassOfAxiom ax = axioms.iterator().next();
        OWLClass subCls = Class(IRI("http://www.semanticweb.org/owlapi/test#", "A"));
        Assert.assertEquals(subCls, ax.getSubClass());
        OWLClassExpression supCls = ax.getSuperClass();
        Assert.assertTrue(supCls instanceof OWLObjectSomeValuesFrom);
        OWLObjectSomeValuesFrom someValuesFrom = (OWLObjectSomeValuesFrom) supCls;
        OWLObjectProperty property = ObjectProperty(IRI("http://www.semanticweb.org/owlapi/test#", "P"));
        OWLClass fillerCls = Class(IRI("http://www.semanticweb.org/owlapi/test#", "C"));
        Assert.assertEquals(property, someValuesFrom.getProperty());
        Assert.assertEquals(fillerCls, someValuesFrom.getFiller());
    }

    @Test
    public void testContainsAxiomsUntypedSubClassOf() {
        assertEqualsSet("UntypedSubClassOf.rdf", SubClassOf(clA, clB));
    }
}
