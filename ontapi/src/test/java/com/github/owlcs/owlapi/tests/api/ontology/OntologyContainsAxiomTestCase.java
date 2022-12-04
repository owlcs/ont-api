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
package com.github.owlcs.owlapi.tests.api.ontology;

import com.github.owlcs.TempDirectory;
import com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory;
import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat;
import org.semanticweb.owlapi.formats.OWLXMLDocumentFormat;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.io.StreamDocumentTarget;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.parameters.AxiomAnnotations;
import org.semanticweb.owlapi.model.parameters.Imports;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collections;

/**
 * @author Matthew Horridge, The University of Manchester, Information Management Group
 */
@ExtendWith(TempDirectory.class)
public class OntologyContainsAxiomTestCase extends TestBase {

    @Test
    public void testOntologyContainsPlainAxiom() {
        OWLAxiom axiom = OWLFunctionalSyntaxFactory.SubClassOf(OWLFunctionalSyntaxFactory.Class(iri("A")),
                OWLFunctionalSyntaxFactory.Class(iri("B")));
        OWLOntology ont = getOWLOntology();
        ont.getOWLOntologyManager().addAxiom(ont, axiom);
        Assertions.assertTrue(ont.containsAxiom(axiom));
        Assertions.assertTrue(ont.containsAxiom(axiom, Imports.EXCLUDED, AxiomAnnotations.IGNORE_AXIOM_ANNOTATIONS));
    }

    @Test
    public void testOntologyContainsAnnotatedAxiom() {
        OWLLiteral annoLiteral = OWLFunctionalSyntaxFactory.Literal("value");
        OWLAnnotationProperty annoProp = OWLFunctionalSyntaxFactory.AnnotationProperty(iri("annoProp"));
        OWLAnnotation anno = OWLFunctionalSyntaxFactory.Annotation(annoProp, annoLiteral);
        OWLAxiom axiom = OWLFunctionalSyntaxFactory.SubClassOf(OWLFunctionalSyntaxFactory.Class(iri("A")),
                OWLFunctionalSyntaxFactory.Class(iri("B")), Collections.singleton(anno));
        OWLOntology ont = getOWLOntology();
        ont.getOWLOntologyManager().addAxiom(ont, axiom);
        Assertions.assertTrue(ont.containsAxiom(axiom));
        Assertions.assertTrue(ont.containsAxiom(axiom, Imports.EXCLUDED, AxiomAnnotations.IGNORE_AXIOM_ANNOTATIONS));
        Assertions.assertFalse(ont.containsAxiom(axiom.getAxiomWithoutAnnotations()));
        Assertions.assertTrue(ont.containsAxiom(axiom.getAxiomWithoutAnnotations(),
                Imports.EXCLUDED, AxiomAnnotations.IGNORE_AXIOM_ANNOTATIONS));
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
     */
    private void runTestOntologyContainsAxioms1(OWLDocumentFormat format) throws Exception {
        OWLOntology ont1 = getOWLOntology();

        IRI ont1iri = ont1.getOntologyID().getOntologyIRI().orElseThrow(AssertionError::new);
        OWLOntology ont2 = getOWLOntology();

        IRI ont2iri = ont2.getOntologyID().getOntologyIRI().orElseThrow(AssertionError::new);
        OWLImportsDeclaration ont2import = OWLFunctionalSyntaxFactory.ImportsDeclaration(ont1iri);
        ont1.getOWLOntologyManager().applyChange(new AddImport(ont2, ont2import));
        OWLAnnotationProperty annoProp = OWLFunctionalSyntaxFactory.AnnotationProperty(iri("annoProp"));
        OWLAxiom axannoPropdecl = OWLFunctionalSyntaxFactory.Declaration(annoProp);
        ont1.getOWLOntologyManager().addAxiom(ont1, axannoPropdecl);
        OWLAnnotation inont1anno = OWLFunctionalSyntaxFactory.Annotation(annoProp, ont1iri);
        OWLAnnotation inont2anno = OWLFunctionalSyntaxFactory.Annotation(annoProp, ont2iri);
        OWLClass a = OWLFunctionalSyntaxFactory.Class(iri("A"));
        OWLAxiom axAdecl = OWLFunctionalSyntaxFactory.Declaration(a, Collections.singleton(inont1anno));
        ont1.getOWLOntologyManager().addAxiom(ont1, axAdecl);
        OWLClass b = OWLFunctionalSyntaxFactory.Class(iri("B"));
        OWLAxiom axBdecl = OWLFunctionalSyntaxFactory.Declaration(b, Collections.singleton(inont2anno));
        ont2.getOWLOntologyManager().addAxiom(ont2, axBdecl);
        OWLAxiom axAsubB = OWLFunctionalSyntaxFactory.SubClassOf(OWLFunctionalSyntaxFactory.Class(iri("A")),
                OWLFunctionalSyntaxFactory.Class(iri("B")), Collections.singleton(inont2anno));
        ont2.getOWLOntologyManager().addAxiom(ont2, axAsubB);

        // annoProp is in ont1 and in the import closure of ont2
        Assertions.assertTrue(containsConsiderEx(ont1, axannoPropdecl));
        Assertions.assertFalse(containsConsiderEx(ont2, axannoPropdecl));
        Assertions.assertTrue(containsConsider(ont2, axannoPropdecl));
        // A is in ont1 and in the import closure of ont2
        Assertions.assertTrue(containsConsiderEx(ont1, axAdecl));
        Assertions.assertFalse(containsConsiderEx(ont2, axAdecl));
        Assertions.assertTrue(containsConsider(ont2, axAdecl));
        // B is in only in ont2
        Assertions.assertFalse(containsConsider(ont1, axBdecl));
        Assertions.assertTrue(containsConsiderEx(ont2, axBdecl));
        Assertions.assertTrue(containsConsider(ont2, axBdecl));
        // A is a subclass of B is in only in ont2
        Assertions.assertFalse(containsConsider(ont1, axAsubB));
        Assertions.assertTrue(containsConsiderEx(ont2, axAsubB));
        Assertions.assertTrue(containsConsider(ont2, axAsubB));

        File savedLocation1 = TempDirectory.createFile("testont1A", ".owl").toFile();
        FileOutputStream out1 = new FileOutputStream(savedLocation1);
        StreamDocumentTarget writer1 = new StreamDocumentTarget(out1);
        ont1.getOWLOntologyManager().saveOntology(ont1, format, writer1);

        File savedLocation2 = TempDirectory.createFile("testont2A", ".owl").toFile();
        FileOutputStream out2 = new FileOutputStream(savedLocation2);
        StreamDocumentTarget writer2 = new StreamDocumentTarget(out2);
        ont2.getOWLOntologyManager().saveOntology(ont2, format, writer2);

        // ONT-API: AxiomAnnotations.CONSIDER_AXIOM_ANNOTATIONS -> AxiomAnnotations.IGNORE_AXIOM_ANNOTATIONS
        OWLOntologyManager man = setupManager();
        OWLOntology ont1L = man.loadOntologyFromOntologyDocument(savedLocation1);
        OWLOntology ont2L = man.loadOntologyFromOntologyDocument(savedLocation2);
        // annoProp is in ont1 and in the import closure of ont2
        Assertions.assertTrue(containsConsiderEx(ont1L, axannoPropdecl));
        Assertions.assertFalse(containsConsiderEx(ont2L, axannoPropdecl));
        Assertions.assertTrue(containsConsider(ont2L, axannoPropdecl));
        // A is in ont1 and in the import closure of ont2
        Assertions.assertTrue(ont1L.containsAxiom(axAdecl, Imports.INCLUDED, AxiomAnnotations.IGNORE_AXIOM_ANNOTATIONS));
        Assertions.assertFalse(ont2L.containsAxiom(axAdecl, Imports.EXCLUDED, AxiomAnnotations.IGNORE_AXIOM_ANNOTATIONS));
        Assertions.assertTrue(ont2L.containsAxiom(axAdecl, Imports.INCLUDED, AxiomAnnotations.IGNORE_AXIOM_ANNOTATIONS));
        // B is in only in ont2
        Assertions.assertFalse(ont1L.containsAxiom(axBdecl, Imports.INCLUDED, AxiomAnnotations.IGNORE_AXIOM_ANNOTATIONS));
        Assertions.assertTrue(ont2L.containsAxiom(axBdecl, Imports.EXCLUDED, AxiomAnnotations.IGNORE_AXIOM_ANNOTATIONS));
        Assertions.assertTrue(ont2L.containsAxiom(axBdecl, Imports.INCLUDED, AxiomAnnotations.IGNORE_AXIOM_ANNOTATIONS));
        // A is a subclass of B is in only in ont2
        Assertions.assertFalse(ont1L.containsAxiom(axAsubB, Imports.INCLUDED, AxiomAnnotations.IGNORE_AXIOM_ANNOTATIONS));
        Assertions.assertTrue(ont2L.containsAxiom(axAsubB, Imports.EXCLUDED, AxiomAnnotations.IGNORE_AXIOM_ANNOTATIONS));
        Assertions.assertTrue(ont2L.containsAxiom(axAsubB, Imports.INCLUDED, AxiomAnnotations.IGNORE_AXIOM_ANNOTATIONS));
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

    private void runTestOntologyContainsAxioms2(OWLDocumentFormat format) throws Exception {
        OWLOntology ont1 = getOWLOntology();
        IRI ont1iri = ont1.getOntologyID().getOntologyIRI().orElseThrow(AssertionError::new);
        OWLOntology ont2 = getOWLOntology();
        IRI ont2iri = ont2.getOntologyID().getOntologyIRI().orElseThrow(AssertionError::new);
        OWLImportsDeclaration ont2import = OWLFunctionalSyntaxFactory.ImportsDeclaration(ont1iri);
        ont2.getOWLOntologyManager().applyChange(new AddImport(ont2, ont2import));
        OWLAnnotationProperty annoProp = OWLFunctionalSyntaxFactory.AnnotationProperty(iri("annoProp"));
        OWLAxiom axAnnoPropDecl = OWLFunctionalSyntaxFactory.Declaration(annoProp);
        ont1.getOWLOntologyManager().addAxiom(ont1, axAnnoPropDecl);
        OWLAnnotation inOnt1Anno = OWLFunctionalSyntaxFactory.Annotation(annoProp, ont1iri);
        OWLAnnotation inOnt2Anno = OWLFunctionalSyntaxFactory.Annotation(annoProp, ont2iri);
        OWLClass a = OWLFunctionalSyntaxFactory.Class(iri("A"));
        OWLAxiom axADecl = OWLFunctionalSyntaxFactory.Declaration(a, Collections.singleton(inOnt1Anno));
        ont1.getOWLOntologyManager().addAxiom(ont1, axADecl);
        OWLClass b = OWLFunctionalSyntaxFactory.Class(iri("B"));
        OWLAxiom axBDecl = OWLFunctionalSyntaxFactory.Declaration(b, Collections.singleton(inOnt2Anno));
        ont2.getOWLOntologyManager().addAxiom(ont2, axBDecl);
        OWLAxiom axAsubB = OWLFunctionalSyntaxFactory.SubClassOf(OWLFunctionalSyntaxFactory.Class(iri("A")),
                OWLFunctionalSyntaxFactory.Class(iri("B")), Collections.singleton(inOnt2Anno));
        ont2.getOWLOntologyManager().addAxiom(ont2, axAsubB);
        // annoProp is in ont1 and in the import closure of ont2
        Assertions.assertTrue(containsConsiderEx(ont1, axAnnoPropDecl));
        Assertions.assertFalse(containsConsiderEx(ont2, axAnnoPropDecl));
        Assertions.assertTrue(containsConsider(ont2, axAnnoPropDecl));
        // A is in ont1 and in the import closure of ont2
        Assertions.assertTrue(containsConsiderEx(ont1, axADecl));
        Assertions.assertFalse(containsConsiderEx(ont2, axADecl));
        Assertions.assertTrue(containsConsider(ont2, axADecl));
        // B is in only in ont2
        Assertions.assertFalse(containsConsider(ont1, axBDecl));
        Assertions.assertTrue(containsConsiderEx(ont2, axBDecl));
        Assertions.assertTrue(containsConsider(ont2, axBDecl));
        // A is a subclass of B is in only in ont2
        Assertions.assertFalse(containsConsider(ont1, axAsubB));
        Assertions.assertTrue(containsConsiderEx(ont2, axAsubB));
        Assertions.assertTrue(containsConsider(ont2, axAsubB));

        File savedLocation1 = TempDirectory.createFile("testont1B", ".owl").toFile();
        FileOutputStream out1 = new FileOutputStream(savedLocation1);
        StreamDocumentTarget writer1 = new StreamDocumentTarget(out1);
        ont1.getOWLOntologyManager().saveOntology(ont1, format, writer1);

        File savedLocation2 = TempDirectory.createFile("testont2B", ".owl").toFile();
        FileOutputStream out2 = new FileOutputStream(savedLocation2);
        StreamDocumentTarget writer2 = new StreamDocumentTarget(out2);
        ont2.getOWLOntologyManager().saveOntology(ont2, format, writer2);
        OWLOntologyManager man = setupManager();
        @SuppressWarnings("unused")
        OWLOntology ont1L = man.loadOntologyFromOntologyDocument(savedLocation1);
        OWLOntology ont2L = man.loadOntologyFromOntologyDocument(savedLocation2);
        ont2L.imports().forEach(o -> o.axioms().forEach(ax -> {
            Assertions.assertTrue(containsConsiderEx(o, ax));
            Assertions.assertFalse(containsConsiderEx(ont2L, ax));
        }));
    }
}
