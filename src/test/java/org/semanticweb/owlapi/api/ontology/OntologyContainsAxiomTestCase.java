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
package org.semanticweb.owlapi.api.ontology;

import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.api.baseclasses.TestBase;
import org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat;
import org.semanticweb.owlapi.formats.OWLXMLDocumentFormat;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.io.StreamDocumentTarget;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.parameters.AxiomAnnotations;
import org.semanticweb.owlapi.model.parameters.Imports;

import java.io.File;
import java.io.FileOutputStream;

import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.*;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.Class;

/**
 * @author Matthew Horridge, The University of Manchester, Information
 *         Management Group
 * @since 3.0.0
 */

@SuppressWarnings("javadoc")
public class OntologyContainsAxiomTestCase extends TestBase {

    @Test
    public void testOntologyContainsPlainAxiom() {
        OWLAxiom axiom = SubClassOf(Class(iri("A")), Class(iri("B")));
        OWLOntology ont = getOWLOntology();
        ont.getOWLOntologyManager().addAxiom(ont, axiom);
        Assert.assertTrue(ont.containsAxiom(axiom));
        Assert.assertTrue(ont.containsAxiom(axiom, Imports.EXCLUDED, AxiomAnnotations.IGNORE_AXIOM_ANNOTATIONS));
    }

    @Test
    public void testOntologyContainsAnnotatedAxiom() {
        OWLLiteral annoLiteral = Literal("value");
        OWLAnnotationProperty annoProp = AnnotationProperty(iri("annoProp"));
        OWLAnnotation anno = Annotation(annoProp, annoLiteral);
        OWLAxiom axiom = SubClassOf(Class(iri("A")), Class(iri("B")), singleton(anno));
        OWLOntology ont = getOWLOntology();
        ont.getOWLOntologyManager().addAxiom(ont, axiom);
        Assert.assertTrue(ont.containsAxiom(axiom));
        Assert.assertTrue(ont.containsAxiom(axiom, Imports.EXCLUDED, AxiomAnnotations.IGNORE_AXIOM_ANNOTATIONS));
        Assert.assertFalse(ont.containsAxiom(axiom.getAxiomWithoutAnnotations()));
        Assert.assertTrue(ont.containsAxiom(axiom.getAxiomWithoutAnnotations(), Imports.EXCLUDED, AxiomAnnotations.IGNORE_AXIOM_ANNOTATIONS));
    }

    @Test
    public void testOntologyContainsAxiomsForRDFXML1() throws Exception {
        RDFXMLDocumentFormat format = createRDFXMLFormat();
        runTestOntologyContainsAxioms1(format);
    }

    private static RDFXMLDocumentFormat createRDFXMLFormat() {
        RDFXMLDocumentFormat format = new RDFXMLDocumentFormat();
        // This test case relies on certain declarations being in certain
        // ontologies. The default
        // behaviour is to add missing declarations. Therefore, this needs to be
        // turned off.
        format.setAddMissingTypes(false);
        return format;
    }

    @Test
    public void testOntologyContainsAxiomsForOWLXML1() throws Exception {
        runTestOntologyContainsAxioms1(new OWLXMLDocumentFormat());
    }

    @Test
    public void testOntologyContainsAxiomsForOWLFunctionalSyntax1() throws Exception {
        runTestOntologyContainsAxioms1(new FunctionalSyntaxDocumentFormat());
    }

    @Test
    public void testOntologyContainsAxiomsForTurtleSyntax1() throws Exception {
        TurtleDocumentFormat format = createTurtleOntologyFormat();
        runTestOntologyContainsAxioms1(format);
    }

    private static TurtleDocumentFormat createTurtleOntologyFormat() {
        TurtleDocumentFormat format = new TurtleDocumentFormat();
        format.setAddMissingTypes(false);
        return format;
    }

    /**
     * Modified for ONT-API:
     * After ontology reloading with default config settings all annotations from declarations become annotation assertions.
     * So NOTE: there is a replacement: {@link AxiomAnnotations#CONSIDER_AXIOM_ANNOTATIONS} -> {@link AxiomAnnotations#IGNORE_AXIOM_ANNOTATIONS}
     * in the final checks.
     *
     * @param format
     * @throws Exception
     */
    private void runTestOntologyContainsAxioms1(OWLDocumentFormat format) throws Exception {
        OWLOntology ont1 = getOWLOntology();

        IRI ont1iri = get(ont1.getOntologyID().getOntologyIRI());
        OWLOntology ont2 = getOWLOntology();

        IRI ont2iri = get(ont2.getOntologyID().getOntologyIRI());
        OWLImportsDeclaration ont2import = ImportsDeclaration(ont1iri);
        ont1.getOWLOntologyManager().applyChange(new AddImport(ont2, ont2import));
        OWLAnnotationProperty annoProp = AnnotationProperty(iri("annoProp"));
        OWLAxiom axannoPropdecl = Declaration(annoProp);
        ont1.getOWLOntologyManager().addAxiom(ont1, axannoPropdecl);
        OWLAnnotation inont1anno = Annotation(annoProp, ont1iri);
        OWLAnnotation inont2anno = Annotation(annoProp, ont2iri);
        OWLClass a = Class(iri("A"));
        OWLAxiom axAdecl = Declaration(a, singleton(inont1anno));
        ont1.getOWLOntologyManager().addAxiom(ont1, axAdecl);
        OWLClass b = Class(iri("B"));
        OWLAxiom axBdecl = Declaration(b, singleton(inont2anno));
        ont2.getOWLOntologyManager().addAxiom(ont2, axBdecl);
        OWLAxiom axAsubB = SubClassOf(Class(iri("A")), Class(iri("B")), singleton(inont2anno));
        ont2.getOWLOntologyManager().addAxiom(ont2, axAsubB);

        // annoProp is in ont1 and in the import closure of ont2
        Assert.assertTrue(containsConsiderEx(ont1, axannoPropdecl));
        Assert.assertFalse(containsConsiderEx(ont2, axannoPropdecl));
        Assert.assertTrue(containsConsider(ont2, axannoPropdecl));
        // A is in ont1 and in the import closure of ont2
        Assert.assertTrue(containsConsiderEx(ont1, axAdecl));
        Assert.assertFalse(containsConsiderEx(ont2, axAdecl));
        Assert.assertTrue(containsConsider(ont2, axAdecl));
        // B is in only in ont2
        Assert.assertFalse(containsConsider(ont1, axBdecl));
        Assert.assertTrue(containsConsiderEx(ont2, axBdecl));
        Assert.assertTrue(containsConsider(ont2, axBdecl));
        // A is a subclass of B is in only in ont2
        Assert.assertFalse(containsConsider(ont1, axAsubB));
        Assert.assertTrue(containsConsiderEx(ont2, axAsubB));
        Assert.assertTrue(containsConsider(ont2, axAsubB));

        File savedLocation1 = folder.newFile("testont1A.owl");
        FileOutputStream out1 = new FileOutputStream(savedLocation1);
        StreamDocumentTarget writer1 = new StreamDocumentTarget(out1);
        ont1.getOWLOntologyManager().saveOntology(ont1, format, writer1);

        File savedLocation2 = folder.newFile("testont2A.owl");
        FileOutputStream out2 = new FileOutputStream(savedLocation2);
        StreamDocumentTarget writer2 = new StreamDocumentTarget(out2);
        ont2.getOWLOntologyManager().saveOntology(ont2, format, writer2);

        // ONT-API: AxiomAnnotations.CONSIDER_AXIOM_ANNOTATIONS -> AxiomAnnotations.IGNORE_AXIOM_ANNOTATIONS
        OWLOntologyManager man = setupManager();
        OWLOntology ont1L = man.loadOntologyFromOntologyDocument(savedLocation1);
        OWLOntology ont2L = man.loadOntologyFromOntologyDocument(savedLocation2);
        // annoProp is in ont1 and in the import closure of ont2
        Assert.assertTrue(containsConsiderEx(ont1L, axannoPropdecl));
        Assert.assertFalse(containsConsiderEx(ont2L, axannoPropdecl));
        Assert.assertTrue(containsConsider(ont2L, axannoPropdecl));
        // A is in ont1 and in the import closure of ont2
        Assert.assertTrue(ont1L.containsAxiom(axAdecl, Imports.INCLUDED, AxiomAnnotations.IGNORE_AXIOM_ANNOTATIONS));
        Assert.assertFalse(ont2L.containsAxiom(axAdecl, Imports.EXCLUDED, AxiomAnnotations.IGNORE_AXIOM_ANNOTATIONS));
        Assert.assertTrue(ont2L.containsAxiom(axAdecl, Imports.INCLUDED, AxiomAnnotations.IGNORE_AXIOM_ANNOTATIONS));
        // B is in only in ont2
        Assert.assertFalse(ont1L.containsAxiom(axBdecl, Imports.INCLUDED, AxiomAnnotations.IGNORE_AXIOM_ANNOTATIONS));
        Assert.assertTrue(ont2L.containsAxiom(axBdecl, Imports.EXCLUDED, AxiomAnnotations.IGNORE_AXIOM_ANNOTATIONS));
        Assert.assertTrue(ont2L.containsAxiom(axBdecl, Imports.INCLUDED, AxiomAnnotations.IGNORE_AXIOM_ANNOTATIONS));
        // A is a subclass of B is in only in ont2
        Assert.assertFalse(ont1L.containsAxiom(axAsubB, Imports.INCLUDED, AxiomAnnotations.IGNORE_AXIOM_ANNOTATIONS));
        Assert.assertTrue(ont2L.containsAxiom(axAsubB, Imports.EXCLUDED, AxiomAnnotations.IGNORE_AXIOM_ANNOTATIONS));
        Assert.assertTrue(ont2L.containsAxiom(axAsubB, Imports.INCLUDED, AxiomAnnotations.IGNORE_AXIOM_ANNOTATIONS));
    }

    private boolean containsConsider(OWLOntology o, OWLAxiom ax) {
        return o.containsAxiom(ax, Imports.INCLUDED, AxiomAnnotations.CONSIDER_AXIOM_ANNOTATIONS);
    }

    private boolean containsConsiderEx(OWLOntology o, OWLAxiom ax) {
        return o.containsAxiom(ax, Imports.EXCLUDED, AxiomAnnotations.CONSIDER_AXIOM_ANNOTATIONS);
    }

    @Test
    public void testOntologyContainsAxiomsForRDFXML2() throws Exception {
        runTestOntologyContainsAxioms2(createRDFXMLFormat());
    }

    @Test
    public void testOntologyContainsAxiomsForOWLXML2() throws Exception {
        runTestOntologyContainsAxioms2(new OWLXMLDocumentFormat());
    }

    @Test
    public void testOntologyContainsAxiomsForOWLFunctionalSyntax2() throws Exception {
        runTestOntologyContainsAxioms2(new FunctionalSyntaxDocumentFormat());
    }

    @Test
    public void testOntologyContainsAxiomsForTurtleSyntax2() throws Exception {
        runTestOntologyContainsAxioms2(createTurtleOntologyFormat());
    }

    @SuppressWarnings("resource")
    private void runTestOntologyContainsAxioms2(OWLDocumentFormat format) throws Exception {
        OWLOntology ont1 = getOWLOntology();
        IRI ont1iri = get(ont1.getOntologyID().getOntologyIRI());
        OWLOntology ont2 = getOWLOntology();
        IRI ont2iri = get(ont2.getOntologyID().getOntologyIRI());
        OWLImportsDeclaration ont2import = ImportsDeclaration(ont1iri);
        ont2.getOWLOntologyManager().applyChange(new AddImport(ont2, ont2import));
        OWLAnnotationProperty annoProp = AnnotationProperty(iri("annoProp"));
        OWLAxiom axAnnoPropDecl = Declaration(annoProp);
        ont1.getOWLOntologyManager().addAxiom(ont1, axAnnoPropDecl);
        OWLAnnotation inOnt1Anno = Annotation(annoProp, ont1iri);
        OWLAnnotation inOnt2Anno = Annotation(annoProp, ont2iri);
        OWLClass a = Class(iri("A"));
        OWLAxiom axADecl = Declaration(a, singleton(inOnt1Anno));
        ont1.getOWLOntologyManager().addAxiom(ont1, axADecl);
        OWLClass b = Class(iri("B"));
        OWLAxiom axBDecl = Declaration(b, singleton(inOnt2Anno));
        ont2.getOWLOntologyManager().addAxiom(ont2, axBDecl);
        OWLAxiom axAsubB = SubClassOf(Class(iri("A")), Class(iri("B")), singleton(inOnt2Anno));
        ont2.getOWLOntologyManager().addAxiom(ont2, axAsubB);
        // annoProp is in ont1 and in the import closure of ont2
        Assert.assertTrue(containsConsiderEx(ont1, axAnnoPropDecl));
        Assert.assertFalse(containsConsiderEx(ont2, axAnnoPropDecl));
        Assert.assertTrue(containsConsider(ont2, axAnnoPropDecl));
        // A is in ont1 and in the import closure of ont2
        Assert.assertTrue(containsConsiderEx(ont1, axADecl));
        Assert.assertFalse(containsConsiderEx(ont2, axADecl));
        Assert.assertTrue(containsConsider(ont2, axADecl));
        // B is in only in ont2
        Assert.assertFalse(containsConsider(ont1, axBDecl));
        Assert.assertTrue(containsConsiderEx(ont2, axBDecl));
        Assert.assertTrue(containsConsider(ont2, axBDecl));
        // A is a subclass of B is in only in ont2
        Assert.assertFalse(containsConsider(ont1, axAsubB));
        Assert.assertTrue(containsConsiderEx(ont2, axAsubB));
        Assert.assertTrue(containsConsider(ont2, axAsubB));

        File savedLocation1 = folder.newFile("testont1B.owl");
        FileOutputStream out1 = new FileOutputStream(savedLocation1);
        StreamDocumentTarget writer1 = new StreamDocumentTarget(out1);
        ont1.getOWLOntologyManager().saveOntology(ont1, format, writer1);

        File savedLocation2 = folder.newFile("testont2B.owl");
        FileOutputStream out2 = new FileOutputStream(savedLocation2);
        StreamDocumentTarget writer2 = new StreamDocumentTarget(out2);
        ont2.getOWLOntologyManager().saveOntology(ont2, format, writer2);
        OWLOntologyManager man = setupManager();
        @SuppressWarnings("unused")
        OWLOntology ont1L = man.loadOntologyFromOntologyDocument(savedLocation1);
        OWLOntology ont2L = man.loadOntologyFromOntologyDocument(savedLocation2);
        ont2L.imports().forEach(o -> o.axioms().forEach(ax -> {
            Assert.assertTrue(containsConsiderEx(o, ax));
            Assert.assertFalse(containsConsiderEx(ont2L, ax));
        }));
    }
}
