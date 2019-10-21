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

package com.github.owlcs.owlapi.tests.api.syntax;

import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.rdf.rdfxml.renderer.RDFXMLStorerFactory;
import org.semanticweb.owlapi.util.OWLAPIStreamUtils;
import com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory;
import com.github.owlcs.owlapi.OWLManager;
import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * test for 3294629 - currently disabled. Not clear whether structure sharing is
 * allowed or disallowed. Data is equivalent, ontology annotations are not
 */

@SuppressWarnings("javadoc")
public class SharedBlankNodeTestCase extends TestBase {

    @Test
    public void shouldSaveOneIndividual() throws Exception {
        OWLOntology ontology = createOntology();
        StringDocumentTarget s = saveOntology(ontology, new RDFXMLDocumentFormat());
        StringDocumentTarget functionalSyntax = saveOntology(ontology, new FunctionalSyntaxDocumentFormat());
        testAnnotation(loadOntologyFromString(functionalSyntax));
        testAnnotation(loadOntologyFromString(s));
    }

    public OWLOntology createOntology() throws OWLOntologyCreationException {
        String NS = "urn:test";
        OWLDataProperty P = OWLFunctionalSyntaxFactory.DataProperty(IRI.create(NS + "#", "p"));
        OWLObjectProperty P1 = OWLFunctionalSyntaxFactory.ObjectProperty(IRI.create(NS + "#", "p1"));
        OWLObjectProperty P2 = OWLFunctionalSyntaxFactory.ObjectProperty(IRI.create(NS + "#", "p2"));
        OWLAnnotationProperty ann = OWLFunctionalSyntaxFactory.AnnotationProperty(IRI.create(NS + "#", "ann"));
        OWLOntology ontology = m.createOntology(IRI.create(NS, ""));
        OWLAnonymousIndividual i = OWLFunctionalSyntaxFactory.AnonymousIndividual();
        m.addAxiom(ontology, OWLFunctionalSyntaxFactory.Declaration(P));
        m.addAxiom(ontology, OWLFunctionalSyntaxFactory.Declaration(P1));
        m.addAxiom(ontology, OWLFunctionalSyntaxFactory.Declaration(P2));
        m.addAxiom(ontology, OWLFunctionalSyntaxFactory.Declaration(ann));
        m.applyChange(new AddOntologyAnnotation(ontology, OWLFunctionalSyntaxFactory.Annotation(ann, i)));
        OWLAxiom ass = OWLFunctionalSyntaxFactory.DataPropertyAssertion(P, i, OWLFunctionalSyntaxFactory.Literal("hello world"));
        OWLNamedIndividual ind = OWLFunctionalSyntaxFactory.NamedIndividual(IRI.create(NS + "#", "test"));
        OWLAxiom ax1 = OWLFunctionalSyntaxFactory.ObjectPropertyAssertion(P1, ind, i);
        OWLAxiom ax2 = OWLFunctionalSyntaxFactory.ObjectPropertyAssertion(P2, ind, i);
        m.addAxiom(ontology, ass);
        m.addAxiom(ontology, ax1);
        m.addAxiom(ontology, ax2);
        return ontology;
    }

    private static void testAnnotation(OWLOntology o) {
        o.individualsInSignature().forEach(i -> Assert.assertEquals(2L, o.objectPropertyAssertionAxioms(i).count()));
        o.annotations().map(a -> (OWLIndividual) a.getValue()).forEach(i ->
                Assert.assertEquals(1L, o.dataPropertyAssertionAxioms(i).count()));
    }

    @Test
    public void shouldRoundtripBlankNodeAnnotations() throws OWLOntologyCreationException, OWLOntologyStorageException {
        String input = "<?xml version=\"1.0\"?>\r\n"
                + "<rdf:RDF "
                + "xmlns:owl=\"http://www.w3.org/2002/07/owl#\" "
                + "xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" "
                + "xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\">"
                + "<owl:Class rdf:about=\"http://E\">"
                + "<rdfs:comment><rdf:Description><rdfs:comment>E</rdfs:comment></rdf:Description></rdfs:comment>"
                + "</owl:Class>"
                + "</rdf:RDF>";
        OWLOntology o = loadOntologyFromString(input);
        OWLOntology o1 = loadOntologyFromString(saveOntology(o, new FunctionalSyntaxDocumentFormat()));
        OWLOntology o2 = loadOntologyFromString(saveOntology(o1, new RDFXMLDocumentFormat()));
        Assert.assertEquals(1L, o2.annotationAssertionAxioms(IRI.create("http://E", "")).count());
        Stream<OWLAnnotationSubject> s = o2.annotationAssertionAxioms(IRI.create("http://E", "")).map(
                a -> (OWLAnnotationSubject) a.getValue());
        s.forEach(a -> Assert.assertEquals(1L, o2.annotationAssertionAxioms(a).count()));
    }

    @Test
    public void shouldRemapUponReading() throws OWLOntologyCreationException {
        String input = "Prefix(rdf:=<http://www.w3.org/1999/02/22-rdf-syntax-ns#>)\r\n"
                + "Prefix(xml:=<http://www.w3.org/XML/1998/namespace>)\r\n"
                + "Prefix(xsd:=<http://www.w3.org/2001/XMLSchema#>)\r\n"
                + "Prefix(rdfs:=<http://www.w3.org/2000/01/rdf-schema#>)\r\n" + "Ontology(\r\n"
                + "Declaration(Class(<http://E>))\r\n"
                + "AnnotationAssertion(rdfs:comment <http://E> _:genid1)\r\n"
                + "AnnotationAssertion(rdfs:comment _:genid1 \"E\"))";
        OWLOntology o1 = loadOntologyFromString(input);
        OWLOntology o2 = loadOntologyFromString(input);
        Set<OWLAnnotationValue> values1 = o1.axioms(AxiomType.ANNOTATION_ASSERTION)
                .map(OWLAnnotationAssertionAxiom::getValue).filter(OWLAnonymousIndividual.class::isInstance).collect(Collectors.toSet());
        Set<OWLAnnotationValue> values2 = o2.axioms(AxiomType.ANNOTATION_ASSERTION)
                .map(OWLAnnotationAssertionAxiom::getValue).filter(OWLAnonymousIndividual.class::isInstance).collect(Collectors.toSet());
        Assert.assertEquals(values1.toString(), values1.size(), 1);
        Assert.assertEquals(values1.toString(), values2.size(), 1);
        Assert.assertNotEquals(values1, values2);
    }

    @Test
    public void shouldHaveOnlyOneAnonIndividual() throws OWLOntologyCreationException {
        String input = "<?xml version=\"1.0\"?>\r\n"
                + "<rdf:RDF "
                + "xmlns:owl=\"http://www.w3.org/2002/07/owl#\" "
                + "xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" "
                + "xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\">"
                + "<owl:Class rdf:about=\"http://E\">"
                + "<rdfs:comment>"
                + "<rdf:Description><rdfs:comment>E</rdfs:comment></rdf:Description>"
                + "</rdfs:comment>"
                + "</owl:Class>"
                + "</rdf:RDF>";
        OWLOntology o1 = loadOntologyFromString(input);
        OWLOntology o2 = loadOntologyFromString(input);
        Set<OWLAnnotationValue> values1 = o1.axioms(AxiomType.ANNOTATION_ASSERTION)
                .map(OWLAnnotationAssertionAxiom::getValue).filter(OWLAnonymousIndividual.class::isInstance).collect(Collectors.toSet());
        Set<OWLAnnotationValue> values2 = o2.axioms(AxiomType.ANNOTATION_ASSERTION)
                .map(OWLAnnotationAssertionAxiom::getValue).filter(OWLAnonymousIndividual.class::isInstance).collect(Collectors.toSet());
        Assert.assertEquals(values1.toString(), values1.size(), 1);
        Assert.assertEquals(values1.toString(), values2.size(), 1);
        Assert.assertNotEquals(values1, values2);
    }

    /**
     * ONT-API comment:
     * The tested functionality does NOT work for Jena.
     * But we always can use original OWL-API loader mechanism
     * to be sure that anonymous individuals have the same blank-node-ids as it expected by OWL-API.
     */
    @Test
    public void shouldNotRemapUponReloading() throws Exception {
        try {
            m.getOntologyConfigurator().withRemapAllAnonymousIndividualsIds(false);
            String input = "<?xml version=\"1.0\"?>\r\n"
                    + "<rdf:RDF xmlns=\"http://www.w3.org/2002/07/owl#\"\r\n"
                    + "     xml:base=\"http://www.w3.org/2002/07/owl\"\r\n"
                    + "     xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\r\n"
                    + "     xmlns:owl=\"http://www.w3.org/2002/07/owl#\"\r\n"
                    + "     xmlns:xsd=\"http://www.w3.org/2001/XMLSchema#\"\r\n"
                    + "     xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\">\r\n"
                    + "    <Ontology/>\r\n"
                    + "    <Class rdf:about=\"http://E\">\r\n"
                    + "        <rdfs:comment>\r\n"
                    + "            <rdf:Description rdf:nodeID=\"1058025095\">\r\n"
                    + "                <rdfs:comment>E</rdfs:comment>\r\n"
                    + "            </rdf:Description>\r\n"
                    + "        </rdfs:comment>\r\n"
                    + "    </Class>\r\n"
                    + "</rdf:RDF>";
            Set<OWLAnnotationValue> values = new HashSet<>();
            values.add(m.getOWLDataFactory().getOWLAnonymousIndividual("_:genid-nodeid-1058025095"));

            OWLOntologyManager m1 = setupManager();
            OWLOntologyLoaderConfiguration conf = m1.getOntologyLoaderConfiguration();
            if (!OWLManager.DEBUG_USE_OWL) {
                conf = ((com.github.owlcs.ontapi.config.OntLoaderConfiguration) conf).setUseOWLParsersToLoad(true);
            }
            OWLOntology o1 = m1.loadOntologyFromOntologyDocument(new StringDocumentSource(input), conf);
            OWLAPIStreamUtils.add(values, o1.axioms(AxiomType.ANNOTATION_ASSERTION)
                    .map(OWLAnnotationAssertionAxiom::getValue).filter(OWLAnonymousIndividual.class::isInstance));

            OWLOntologyManager m2 = setupManager();
            OWLOntology o2 = m2.loadOntologyFromOntologyDocument(new StringDocumentSource(input), conf);
            OWLAPIStreamUtils.add(values, o2.axioms(AxiomType.ANNOTATION_ASSERTION)
                    .map(OWLAnnotationAssertionAxiom::getValue).filter(OWLAnonymousIndividual.class::isInstance));
            Assert.assertEquals(values.toString(), values.size(), 1);
        } finally {
            m.getOntologyConfigurator().withRemapAllAnonymousIndividualsIds(true);
        }

    }

    @Test
    public void shouldNotOutputNodeIdWhenNotNeeded() throws OWLOntologyCreationException, OWLOntologyStorageException {
        String input = "<?xml version=\"1.0\"?>\r\n"
                + "<rdf:RDF xmlns=\"http://www.w3.org/2002/07/owl#\"\r\n"
                + "     xml:base=\"http://www.w3.org/2002/07/owl\"\r\n"
                + "     xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\r\n"
                + "     xmlns:owl=\"http://www.w3.org/2002/07/owl#\"\r\n"
                + "     xmlns:xsd=\"http://www.w3.org/2001/XMLSchema#\"\r\n"
                + "     xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\">\r\n"
                + "    <Ontology/>\r\n"
                + "    <Class rdf:about=\"http://E\">\r\n"
                + "        <rdfs:comment>\r\n"
                + "            <rdf:Description rdf:nodeID=\"1058025095\">\r\n"
                + "                <rdfs:comment>E</rdfs:comment>\r\n"
                + "            </rdf:Description>\r\n"
                + "        </rdfs:comment>\r\n"
                + "    </Class>\r\n"
                + "</rdf:RDF>";
        OWLOntology o1 = loadOntologyFromString(input);
        StringDocumentTarget result = saveOntology(o1, new RDFXMLDocumentFormat());
        Assert.assertFalse(result.toString().contains("rdf:nodeID"));
    }

    /**
     * ONT-API comment:
     * In the ONT-API there is a substitution of formats and instead OWL-Storers there is Jena mechanism.
     * There is a 'hack' instead a saving through manager there is saving through {@link OWLStorerFactory}.
     * This is to make test passed both for OWL-API and ONT-API.
     */
    @Test
    public void shouldOutputNodeIdEvenIfNotNeeded() throws Exception {
        String input = "<?xml version=\"1.0\"?>\r\n"
                + "<rdf:RDF xmlns=\"http://www.w3.org/2002/07/owl#\"\r\n"
                + "     xml:base=\"http://www.w3.org/2002/07/owl\"\r\n"
                + "     xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\r\n"
                + "     xmlns:owl=\"http://www.w3.org/2002/07/owl#\"\r\n"
                + "     xmlns:xsd=\"http://www.w3.org/2001/XMLSchema#\"\r\n"
                + "     xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\">\r\n"
                + "    <Ontology/>\r\n"
                + "    <Class rdf:about=\"http://E\">\r\n"
                + "        <rdfs:comment>\r\n"
                + "            <rdf:Description>\r\n"
                + "                <rdfs:comment>E</rdfs:comment>\r\n"
                + "            </rdf:Description>\r\n"
                + "        </rdfs:comment>\r\n"
                + "    </Class>\r\n"
                + "</rdf:RDF>";
        OWLOntology o1 = loadOntologyFromString(input);
        masterManager.getOntologyConfigurator().withSaveIdsForAllAnonymousIndividuals(true);
        try {
            StringDocumentTarget result = new StringDocumentTarget();

            new RDFXMLStorerFactory().createStorer().storeOntology(o1, result, new RDFXMLDocumentFormat());
            LOGGER.debug("(1)Result:\n{}", result);
            Assert.assertTrue(result.toString().contains("rdf:nodeID"));

            OWLOntology reloaded = loadOntologyFromString(result);
            StringDocumentTarget resaved = new StringDocumentTarget();
            new RDFXMLStorerFactory().createStorer().storeOntology(reloaded, resaved, new RDFXMLDocumentFormat());
            LOGGER.debug("(2)Result:\n{}", resaved);
            Assert.assertEquals(result.toString(), resaved.toString());
        } finally {
            // make sure the static variable is reset after the test
            masterManager.getOntologyConfigurator().withSaveIdsForAllAnonymousIndividuals(false);
        }
    }

    @Test
    public void shouldOutputNodeIdWhenNeeded() throws OWLOntologyCreationException, OWLOntologyStorageException {
        String input = "<?xml version=\"1.0\"?>\r\n"
                + "<rdf:RDF xmlns=\"http://www.w3.org/2002/07/owl#\"\r\n"
                + "     xml:base=\"http://www.w3.org/2002/07/owl\"\r\n"
                + "     xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\r\n"
                + "     xmlns:owl=\"http://www.w3.org/2002/07/owl#\"\r\n"
                + "     xmlns:xsd=\"http://www.w3.org/2001/XMLSchema#\"\r\n"
                + "     xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\">\r\n"
                + "    <Ontology/>\r\n"
                + "    <Class rdf:about=\"http://E\">\r\n"
                + "        <rdfs:comment>\r\n"
                + "            <rdf:Description rdf:nodeID=\"1058025095\">\r\n"
                + "                <rdfs:comment>E</rdfs:comment>\r\n"
                + "            </rdf:Description>\r\n"
                + "        </rdfs:comment>\r\n"
                + "    </Class>\r\n"
                + "    <Class rdf:about=\"http://F\">\r\n"
                + "        <rdfs:comment>\r\n"
                + "            <rdf:Description rdf:nodeID=\"1058025095\">\r\n"
                + "                <rdfs:comment>E</rdfs:comment>\r\n"
                + "            </rdf:Description>\r\n"
                + "        </rdfs:comment>\r\n"
                + "    </Class>\r\n"
                + "</rdf:RDF>";
        OWLOntology o1 = loadOntologyFromString(input);
        StringDocumentTarget result = saveOntology(o1, new RDFXMLDocumentFormat());
        Assert.assertTrue(result.toString().contains("rdf:nodeID"));
    }
}
