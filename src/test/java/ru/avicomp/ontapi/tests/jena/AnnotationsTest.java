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

package ru.avicomp.ontapi.tests.jena;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.util.iterator.UniqueFilter;
import org.apache.jena.vocabulary.RDFS;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avicomp.ontapi.jena.OntModelFactory;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.utils.Models;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

import java.util.Arrays;

/**
 * To test annotated statements ({@link OntStatement}) and annotations within ont objects ({@link OntObject}).
 * Created by @szuev on 28.07.2018.
 */
public class AnnotationsTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationsTest.class);

    @Test
    public void testCreatePlainAnnotations() {
        String uri = "http://test.com/graph/1";
        String ns = uri + "#";

        OntGraphModel m = OntModelFactory.createModel();
        m.setNsPrefix("test", ns);
        m.setNsPrefixes(OntModelFactory.STANDARD);

        LOGGER.debug("1) Assign version-iri and ontology comment.");
        m.setID(uri).setVersionIRI(ns + "1.0.1");
        m.getID().addComment("Some comment", "fr");
        Assert.assertEquals("Should be one header annotation", 1, m.getID().annotations()
                .peek(a -> LOGGER.debug("Annotation: '{}'", a)).count());

        LOGGER.debug("2) Create class with two labels.");
        OntClass cl = m.createOntEntity(OntClass.class, ns + "ClassN1");
        cl.addLabel("some label");
        OntStatement label2 = cl.addLabel("another label", "de");
        ReadWriteUtils.print(m);
        cl.annotations().map(String::valueOf).forEach(LOGGER::debug);
        Assert.assertEquals("Incorrect count of labels.", 2, m.listObjectsOfProperty(cl, RDFS.label).toList().size());

        LOGGER.debug("3) Annotate annotation {}", label2);
        OntStatement seeAlsoForLabel2 = label2.addAnnotation(m.getAnnotationProperty(RDFS.seeAlso),
                ResourceFactory.createResource("http://see.also/1"));
        OntStatement labelForLabel2 = label2.addAnnotation(m.getRDFSLabel(), ResourceFactory.createPlainLiteral("label"));
        ReadWriteUtils.print(m);
        cl.annotations().map(String::valueOf).forEach(LOGGER::debug);
        Assert.assertTrue("Can't find owl:Axiom section.", m.contains(null, RDF.type, OWL.Axiom));
        Assert.assertFalse("There is owl:Annotation section.", m.contains(null, RDF.type, OWL.Annotation));

        LOGGER.debug("4) Create annotation property and annotate {} and {}", seeAlsoForLabel2, labelForLabel2);
        OntNAP nap1 = m.createOntEntity(OntNAP.class, ns + "annotation-prop-1");
        seeAlsoForLabel2.addAnnotation(nap1, ResourceFactory.createPlainLiteral("comment to see also"));
        OntStatement annotationForLabelForLabel2 = labelForLabel2.addAnnotation(nap1,
                ResourceFactory.createPlainLiteral("comment to see label"));
        ReadWriteUtils.print(m);
        Assert.assertEquals("Expected two roots with owl:Annotation.", 2, m.listStatements(null, RDF.type, OWL.Annotation)
                .filterKeep(s -> !m.contains(null, null, s.getSubject())).filterKeep(new UniqueFilter<>()).toList().size());
        Assert.assertEquals("Expected two owl:Annotation.", 2, m.listStatements(null, RDF.type, OWL.Annotation).toList().size());
        Assert.assertEquals("Expected single owl:Axiom.", 1, m.listStatements(null, RDF.type, OWL.Axiom).toList().size());

        LOGGER.debug("5) Delete annotations for {}", labelForLabel2);
        labelForLabel2.deleteAnnotation(annotationForLabelForLabel2.getPredicate().as(OntNAP.class), annotationForLabelForLabel2.getObject());
        ReadWriteUtils.print(m);
        Assert.assertEquals("Expected one root with owl:Annotation.", 1, m.listStatements(null, RDF.type, OWL.Annotation)
                .filterKeep(s -> !m.contains(null, null, s.getSubject())).filterKeep(new UniqueFilter<>()).toList().size());
        Assert.assertEquals("Expected single owl:Annotation.", 1, m.listStatements(null, RDF.type, OWL.Annotation).toList().size());
        Assert.assertEquals("Expected single owl:Axiom.", 1, m.listStatements(null, RDF.type, OWL.Axiom).toList().size());


        LOGGER.debug("6) Delete all annotations for {}", label2);
        label2.clearAnnotations();
        ReadWriteUtils.print(m);
        Assert.assertEquals("Incorrect count of labels.", 2, m.listObjectsOfProperty(cl, RDFS.label).toList().size());
        Assert.assertFalse("There is owl:Axiom", m.contains(null, RDF.type, OWL.Axiom));
        Assert.assertFalse("There is owl:Annotation", m.contains(null, RDF.type, OWL.Annotation));

        LOGGER.debug("7) Annotate sub-class-of");
        OntStatement subClassOf = cl.addSubClassOf(m.getOWLThing());
        OntStatement subClassOfAnnotation = subClassOf
                .addAnnotation(nap1, ResourceFactory.createPlainLiteral("test"));
        subClassOfAnnotation.addAnnotation(m.getRDFSLabel(), ResourceFactory.createPlainLiteral("test2"))
                .addAnnotation(m.getRDFSComment(), ResourceFactory.createPlainLiteral("test3"));

        ReadWriteUtils.print(m);
        Assert.assertEquals("Expected two owl:Annotation.", 2, m.listStatements(null, RDF.type, OWL.Annotation).toList().size());
        Assert.assertEquals("Expected single owl:Axiom.", 1, m.listStatements(null, RDF.type, OWL.Axiom).toList().size());
        Assert.assertEquals("Expected 3 root annotations for class " + cl, 2, cl.annotations()
                .peek(a -> LOGGER.debug("Annotation: '{}'", a)).count());

        LOGGER.debug("8) Deleter all annotations for class {}", cl);
        Assert.assertEquals(2, cl.clearAnnotations().statements().count());
        ReadWriteUtils.print(m);
        Assert.assertEquals("Found annotations for class " + cl, 0, cl.annotations().peek(a -> LOGGER.warn("Annotation: '{}'", a)).count());
        Assert.assertFalse("There is owl:Axiom", m.contains(null, RDF.type, OWL.Axiom));
        Assert.assertFalse("There is owl:Annotation", m.contains(null, RDF.type, OWL.Annotation));
    }

    @Test
    public void testCreateAnonAnnotations() {
        String uri = "http://test.com/graph/2";
        String ns = uri + "#";

        OntGraphModel m = OntModelFactory.createModel();
        m.setNsPrefix("test", ns);
        m.setNsPrefixes(OntModelFactory.STANDARD);
        m.setID(uri);

        OntClass cl1 = m.createOntEntity(OntClass.class, ns + "Class1");
        OntClass cl2 = m.createOntEntity(OntClass.class, ns + "Class2");
        OntClass cl3 = m.createOntEntity(OntClass.class, ns + "Class3");
        OntNAP nap1 = m.createOntEntity(OntNAP.class, ns + "AnnotationProperty1");

        OntDisjoint.Classes disjointClasses = m.createDisjointClasses(Arrays.asList(cl1, cl2, cl3));
        Assert.assertEquals("Incorrect owl:AllDisjointClasses number", 1, m.ontObjects(OntDisjoint.Classes.class).count());

        disjointClasses.addLabel("label1", "en");
        disjointClasses.addLabel("comment", "kjpopo").addAnnotation(nap1, ResourceFactory.createTypedLiteral("some txt"));
        ReadWriteUtils.print(m);
        Assert.assertEquals("Expected two assertions", 2, disjointClasses.as(OntAnnotation.class).assertions().count());
        Assert.assertEquals("Expected two annotations", 2, disjointClasses.as(OntAnnotation.class)
                .annotations().peek(a -> LOGGER.debug("{}", Models.toString(a))).count());

        Assert.assertFalse("There is owl:Axiom", m.contains(null, RDF.type, OWL.Axiom));
        Assert.assertEquals("Should be single owl:Annotation", 1, m.listStatements(null, RDF.type, OWL.Annotation).toList().size());

        OntNOP nop1 = m.createOntEntity(OntNOP.class, ns + "ObjectProperty1");
        OntIndividual.Named ind1 = cl1.createIndividual(ns + "Individual1");
        OntIndividual.Anonymous ind2 = cl2.createIndividual();
        ind2.addComment("anonymous individual", "ru");
        OntNPA.ObjectAssertion nopa = nop1.addNegativeAssertion(ind1, ind2);
        Assert.assertEquals("Incorrect owl:NegativePropertyAssertion number", 1, nop1.negativeAssertions().count());
        nopa.addLabel("label1")
                .addAnnotation(m.getRDFSLabel(), ResourceFactory.createTypedLiteral("label2"))
                .addAnnotation(m.getRDFSLabel(), ResourceFactory.createPlainLiteral("label3"));
        Assert.assertEquals("Should be 3 owl:Annotation", 3, m.listStatements(null, RDF.type, OWL.Annotation).toList().size());

        ReadWriteUtils.print(m);

        Assert.assertEquals("Should only be two roots", 2, m.ontObjects(OntAnnotation.class).count());
        OntStatement disjointWith = cl1.addDisjointWith(cl3);
        Assert.assertFalse("No annotation resource is expected.", disjointWith.asAnnotationResource().isPresent());
        disjointWith.addAnnotation(m.getAnnotationProperty(OWL.deprecated), "disjoint with comment N1", null)
                .addAnnotation(m.getAnnotationProperty(OWL.incompatibleWith), "disjoint with comment N2", "rur");
        ReadWriteUtils.print(m);
        Assert.assertTrue("Should be annotation resource", disjointWith.asAnnotationResource().isPresent());
        Assert.assertEquals("Should only be three roots", 3, m.ontObjects(OntAnnotation.class).count());
    }

    @Test
    public void testRemoveAnnotations() {
        LOGGER.debug("Create a model");
        OntGraphModel m = OntModelFactory.createModel();
        m.setNsPrefixes(OntModelFactory.STANDARD);
        m.getID().addAnnotation(m.getAnnotationProperty(org.apache.jena.vocabulary.OWL.versionInfo), "anonymous ontology", "en");

        OntNDP p = m.createOntEntity(OntNDP.class, "x");
        OntClass c = m.createOntEntity(OntClass.class, "c");
        OntDT dt = m.getOntEntity(OntDT.class, RDFS.Literal);
        p.addRange(dt).addAnnotation(m.getRDFSComment(), "This is a range", null);
        p.addDomain(c).addAnnotation(m.getRDFSLabel(), "This is a domain", null).addAnnotation(m.getRDFSLabel(), "label", "hg");

        OntIndividual i = c.createIndividual("i");
        p.addNegativeAssertion(i, ResourceFactory.createPlainLiteral("test"))
                .addAnnotation(m.getRDFSComment(), "This is a negative data property assertion", null).addAnnotation(m.getRDFSLabel(), "Label", "lk");
        ReadWriteUtils.print(m);

        LOGGER.debug("Remove annotated components");
        OntNPA.DataAssertion assertion = p.negativeAssertions(i).findFirst().orElseThrow(AssertionError::new);
        OntStatement domain = m.statements(null, RDFS.domain, null).findFirst().orElseThrow(AssertionError::new);
        OntStatement range = m.statements(null, RDFS.range, null).findFirst().orElseThrow(AssertionError::new);

        m.removeOntObject(assertion).removeOntStatement(domain).removeOntStatement(range);
        ReadWriteUtils.print(m);
        Assert.assertEquals("Some unexpected garbage are found", 6, m.statements().count());
    }

    @Test
    public void testBuiltInsAnnotations() {
        OntGraphModel m = OntModelFactory.createModel();
        m.getOWLThing().addComment("This is the Thing");
        ReadWriteUtils.print(m);
        Assert.assertEquals(1, m.size());
        Assert.assertEquals(1, m.statements().count());
        Assert.assertEquals(1, m.getOWLThing().statements().count());
        Assert.assertEquals(1, m.getOWLThing().annotations().count());
        m.getOWLThing().annotations().forEach(s -> Assert.assertFalse(s.hasAnnotations()));
        Assert.assertEquals(0, m.getOWLNothing().annotations().count());
        Assert.assertEquals(0, m.getOWLNothing().statements().count());

        m.getOWLThing().clearAnnotations();
        Assert.assertTrue(m.isEmpty());
        m.getOWLBottomDataProperty()
                .addSubPropertyOf(m.getOWLTopDataProperty()).addAnnotation(m.getRDFSComment(), "Some sub-property-of");

        m.getOWLBottomDataProperty().addComment("x");
        ReadWriteUtils.print(m);
        Assert.assertEquals(1, m.getOWLBottomDataProperty().annotations().count());
        Assert.assertEquals(2, m.getOWLBottomDataProperty().statements().count());
        Assert.assertEquals(7, m.statements().count());
        m.localStatements(OWL.bottomDataProperty, RDFS.subPropertyOf, OWL.topDataProperty)
                .findFirst().orElseThrow(AssertionError::new).clearAnnotations();
        Assert.assertEquals(1, m.getOWLBottomDataProperty().annotations().count());
        Assert.assertEquals(2, m.getOWLBottomDataProperty().statements().count());
        Assert.assertEquals(2, m.size());
        m.getOWLBottomDataProperty().clearAnnotations();
        Assert.assertEquals(0, m.getOWLBottomDataProperty().annotations().count());
        Assert.assertEquals(1, m.size());

        m.getOWLTopObjectProperty().addComment("Top Object Property").addAnnotation(m.getRDFSLabel(), "lab");
        ReadWriteUtils.print(m);
        Assert.assertEquals(1, m.ontObjects(OntAnnotation.class).count());
        Assert.assertEquals(1, m.getOWLTopObjectProperty().annotations().count());

    }

    @Test
    public void testListObjectAnnotations() {
        OntGraphModel m = OntModelFactory.createModel();
        m.setNsPrefixes(OntModelFactory.STANDARD);
        OntClass clazz = m.createOntEntity(OntClass.class, "C");
        clazz.addComment("xxx");
        ReadWriteUtils.print(m);

        Assert.assertEquals(2, m.size());
        Assert.assertEquals(2, m.statements().count());
        Assert.assertEquals(2, clazz.statements().count());
        Assert.assertEquals(1, clazz.annotations().count());
        clazz.addComment("yyy").addAnnotation(m.getRDFSLabel(), "zzz");
        ReadWriteUtils.print(m);
        Assert.assertEquals(2, clazz.annotations().peek(a -> LOGGER.debug("{}", Models.toString(a))).count());
        m.statements(clazz, RDF.type, OWL.Class)
                .findFirst()
                .orElseThrow(AssertionError::new)
                .addAnnotation(m.getRDFSComment(), "kkk");
        ReadWriteUtils.print(m);
        Assert.assertEquals(3, clazz.annotations().peek(a -> LOGGER.debug("{}", Models.toString(a))).count());

        clazz.addSubClassOf(m.getOWLThing()).addAnnotation(m.getRDFSComment(), "mmm").addAnnotation(m.getRDFSComment(), "ggg");
        ReadWriteUtils.print(m);
        Assert.assertEquals(3, m.listClasses().findFirst().orElseThrow(AssertionError::new)
                .annotations().peek(a -> LOGGER.debug("{}", Models.toString(a))).count());

        Assert.assertEquals(24, m.size());
        Model model = ModelFactory.createModelForGraph(m.getBaseGraph());
        Assert.assertEquals(3, model.listStatements(null, RDF.type, OWL.Axiom).toList().size());
        Assert.assertEquals(1, model.listStatements(null, RDF.type, OWL.Annotation).toList().size());
    }
}
