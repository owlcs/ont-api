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

import org.apache.jena.rdf.model.*;
import org.apache.jena.util.iterator.UniqueFilter;
import org.apache.jena.vocabulary.RDFS;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.OntModelFactory;
import ru.avicomp.ontapi.jena.impl.OntGraphModelImpl;
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
public class OntAnnotationsTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(OntAnnotationsTest.class);

    @Test
    public void testCreatePlainAnnotations() {
        String uri = "http://test.com/graph/1";
        String ns = uri + "#";

        OntGraphModel m = OntModelFactory.createModel().setNsPrefix("test", ns).setNsPrefixes(OntModelFactory.STANDARD);

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

        Assert.assertEquals(2, cl.content()
                .peek(OntStatement::clearAnnotations)
                .peek(x -> LOGGER.debug("[{}] CONTENT: {}", cl.getLocalName(), Models.toString(x))).count());
        ReadWriteUtils.print(m);
        Assert.assertEquals("Found annotations for class " + cl, 0, cl.annotations().peek(a -> LOGGER.warn("Annotation: '{}'", a)).count());
        Assert.assertFalse("There is owl:Axiom", m.contains(null, RDF.type, OWL.Axiom));
        Assert.assertFalse("There is owl:Annotation", m.contains(null, RDF.type, OWL.Annotation));
    }

    @Test
    public void testCreateAnonAnnotations() {
        String uri = "http://test.com/graph/2";
        String ns = uri + "#";

        OntGraphModel m = OntModelFactory.createModel()
                .setNsPrefix("test", ns)
                .setNsPrefixes(OntModelFactory.STANDARD)
                .setID(uri)
                .getModel();

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
        OntGraphModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
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
        OntGraphModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
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

    @Test
    public void testClearAnnotations() {
        OntGraphModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntNAP nap = m.createOntEntity(OntNAP.class, "nap");
        nap.addComment("test1").addAnnotation(nap, "sub-test1");
        m.asStatement(nap.getRoot().asTriple()).addAnnotation(m.getRDFSComment(), "test2").addAnnotation(m.getRDFSLabel(), "sub-test2");

        OntStatement subPropertyOf = nap.addSubPropertyOf(m.getOWLBottomDataProperty().addProperty(RDF.type, OWL.AnnotationProperty).as(OntNAP.class));
        subPropertyOf.addAnnotation(m.getRDFSLabel(), "test3")
                .addAnnotation(m.getRDFSLabel(), "sub-test3")
                .addAnnotation(m.getRDFSLabel(), "sub-sub-test3");
        ReadWriteUtils.print(m);
        Assert.assertTrue(nap.getRoot().hasAnnotations());
        Assert.assertEquals(2, nap.annotations().count());
        Assert.assertEquals(2, nap.annotations().mapToLong(a -> a.annotations().count()).sum());

        nap.clearAnnotations();
        ReadWriteUtils.print(m);
        Assert.assertEquals(0, nap.annotations().count());
        Assert.assertTrue(subPropertyOf.hasAnnotations());
        Assert.assertEquals(1, subPropertyOf.annotations().count());
        Assert.assertEquals(1, subPropertyOf.annotations().mapToLong(a -> a.annotations().count()).sum());

        subPropertyOf.clearAnnotations();
        ReadWriteUtils.print(m);
        Assert.assertFalse(subPropertyOf.hasAnnotations());
        Assert.assertEquals(3, m.size());
    }


    @Test
    public void testRemoveAnnotatedObject() {
        OntGraphModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);

        OntCE class1 = m.createOntEntity(OntClass.class, "C-1");
        m.asStatement(class1.getRoot().asTriple()).addAnnotation(m.getRDFSComment(), "Class1::1")
                .getSubject(OntAnnotation.class).getBase()
                .getSubject(OntClass.class)
                .addAnnotation(m.getRDFSComment(), "Class1::2");
        long size1 = m.size();

        OntCE class2 = m.createComplementOf(class1);
        class2.addComment("Class2::1").addAnnotation(m.getRDFSComment(), "Class2::1::1").addAnnotation(m.getRDFSComment(), "Class2::1::1::1");
        class2.addComment("Class2::2").addAnnotation(m.getRDFSComment(), "Class2::2::1").addAnnotation(m.getRDFSComment(), "Class2::2::1::1");
        long size2 = m.size();

        OntCE class3 = m.createIntersectionOf(Arrays.asList(class2, m.getOWLNothing()));
        class3.addComment("Class3::1").addAnnotation(m.getRDFSComment(), "Class3::1::1");
        class3.statements().filter(OntStatement::isAnnotation).findFirst().orElseThrow(AssertionError::new)
                .addAnnotation(m.getRDFSComment(), "Class3::2").addAnnotation(m.getRDFSComment(), "Class3::2::1");
        class3.addDisjointWith(class1).addAnnotation(m.getRDFSComment(), "class2 disjoint with class1");
        class3.addDisjointWith(m.getOWLNothing()).addAnnotation(m.getRDFSComment(), "class2 disjoint with nothing");

        ReadWriteUtils.print(m);

        m.removeOntObject(class3);
        ReadWriteUtils.print(m);
        Assert.assertEquals(size2, m.size());

        m.removeOntObject(class2);
        ReadWriteUtils.print(m);
        Assert.assertEquals(size1, m.size());

        m.removeOntObject(class1);
        ReadWriteUtils.print(m);
        Assert.assertTrue(m.isEmpty());
    }

    @Test
    public void testAnnotationFunctionality() {
        OntGraphModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        Resource r = m.createResource("A").addProperty(m.createProperty("B"), "C");
        OntStatement base = m.statements(r, null, null).findFirst().orElseThrow(AssertionError::new);

        Literal literal_1 = m.createLiteral("annotation-1");
        Literal literal_2 = m.createLiteral("annotation-2");
        Literal literal_3 = m.createLiteral("annotation-3");

        OntStatement s1 = base.addAnnotation(m.getRDFSLabel(), literal_1);
        Assert.assertTrue(s1.getSubject().isAnon());
        OntStatement s3 = base.addAnnotation(m.getRDFSLabel(), literal_2).addAnnotation(m.getRDFSLabel(), literal_3);
        OntAnnotation annotation = m.ontObjects(OntAnnotation.class)
                .peek(a -> LOGGER.debug("{}::{}", a, a.getBase()))
                .findFirst()
                .orElseThrow(AssertionError::new);
        Assert.assertEquals(base, annotation.getBase());
        Assert.assertEquals(2, annotation.annotations().count());
        Assert.assertEquals(1, annotation.descendants().count());
        Assert.assertEquals(annotation, base.asAnnotationResource().orElseThrow(AbstractMethodError::new));
        long size = m.size();
        // has anonymous resources in the model cache:
        Assert.assertNotNull(((OntGraphModelImpl) m).getNodeAs(s1.getSubject().asNode(), OntAnnotation.class));
        Assert.assertNotNull(((OntGraphModelImpl) m).getNodeAs(s3.getSubject().asNode(), OntAnnotation.class));
        ReadWriteUtils.print(m);

        // attempt to delete annotation with children:
        try {
            base.deleteAnnotation(m.getRDFSLabel(), literal_2);
            Assert.fail("Expected error");
        } catch (OntJenaException j) {
            LOGGER.debug("Expected: {}", j.getMessage());
        }
        Assert.assertEquals(2, base.annotations().count());
        Assert.assertEquals(size, m.statements().count());

        // success deletion 'annotation-1':
        Assert.assertEquals("C", base.deleteAnnotation(s1.getPredicate().as(OntNAP.class), s1.getObject()).getObject().asLiteral().getLexicalForm());
        ReadWriteUtils.print(m);
        Assert.assertEquals(1, base.annotations().count());
        OntStatement s2 = annotation.annotations().findFirst().orElseThrow(AssertionError::new);
        Assert.assertEquals(literal_2, s2.getObject());
        size = m.size();

        // no deletion, no error
        base.deleteAnnotation(s3.getPredicate().as(OntNAP.class), literal_3);
        ReadWriteUtils.print(m);
        Assert.assertEquals(1, base.annotations().count());
        Assert.assertEquals(size, m.statements().count());

        // attempt to delete assertions from annotation object
        try {
            annotation.getRoot().deleteAnnotation(s3.getPredicate().as(OntNAP.class));
            Assert.fail("Expected error");
        } catch (OntJenaException j) {
            LOGGER.debug("Expected: {}", j.getMessage());
        }
        Assert.assertEquals(size, m.statements().count());

        // delete 'annotation-3' and then annotation-2
        Assert.assertEquals(0, s2.deleteAnnotation(m.getRDFSLabel(), literal_3)
                .getSubject(OntAnnotation.class).descendants().count());
        Assert.assertEquals(0, annotation.descendants().count());
        m.removeOntObject(annotation);
        ReadWriteUtils.print(m);
        Assert.assertEquals(1, m.size());
    }

    @Test
    public void testAssemblyAnnotations() {
        Model m = ModelFactory.createDefaultModel().setNsPrefixes(OntModelFactory.STANDARD);
        Property b = m.createProperty("B");
        Literal c = m.createLiteral("C");
        Resource a = m.createResource("A").addProperty(b, c);

        Resource an1 = m.createResource(OWL.Axiom)
                .addProperty(RDFS.comment, "annotation-1")
                .addProperty(OWL.annotatedProperty, b)
                .addProperty(OWL.annotatedSource, a)
                .addProperty(OWL.annotatedTarget, c);
        m.createResource(OWL.Axiom)
                .addProperty(RDFS.comment, "annotation-2")
                .addProperty(OWL.annotatedProperty, b)
                .addProperty(OWL.annotatedSource, a)
                .addProperty(OWL.annotatedTarget, c);
        m.createResource(OWL.Annotation)
                .addProperty(RDFS.comment, "annotation-3")
                .addProperty(OWL.annotatedProperty, RDFS.comment)
                .addProperty(OWL.annotatedSource, an1)
                .addProperty(OWL.annotatedTarget, m.listObjectsOfProperty(an1, RDFS.comment).toList().get(0));
        ReadWriteUtils.print(m);

        OntGraphModel model = OntModelFactory.createModel(m.getGraph());
        Assert.assertEquals(2, model.ontObjects(OntAnnotation.class).count());
        OntStatement base = model.statements(a, b, c).findFirst().orElseThrow(AssertionError::new);
        Assert.assertEquals(2, base.annotations().count());
        base.addAnnotation(model.getRDFSLabel(), "com-1");
        base.addAnnotation(model.getRDFSLabel(), "com-2");
        ReadWriteUtils.print(m);

        Assert.assertEquals(an1, base.asAnnotationResource().orElseThrow(AssertionError::new));
        Assert.assertEquals(3, an1.inModel(model).as(OntAnnotation.class).annotations()
                .peek(x -> LOGGER.debug("{}", Models.toString(x))).count());
        Assert.assertEquals(1, an1.inModel(model).as(OntAnnotation.class).descendants().count());

        Assert.assertEquals(2, model.statements(null, RDF.type, OWL.Axiom).count());
        Assert.assertEquals(1, model.statements(null, RDF.type, OWL.Annotation).count());
    }
}
