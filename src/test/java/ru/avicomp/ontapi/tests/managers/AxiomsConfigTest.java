/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2019, Avicomp Services, AO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package ru.avicomp.ontapi.tests.managers;

import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avicomp.ontapi.OntFormat;
import ru.avicomp.ontapi.OntManagers;
import ru.avicomp.ontapi.OntologyManager;
import ru.avicomp.ontapi.OntologyModel;
import ru.avicomp.ontapi.config.AxiomsSettings;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by @ssz on 18.03.2019.
 *
 * @see AxiomsSettings
 */
public class AxiomsConfigTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AxiomsConfigTest.class);

    @Test
    public void testLoadAnnotationsOption() {
        OntologyManager m = OntManagers.createONT();
        Assert.assertTrue("Incorrect default settings", m.getOntologyLoaderConfiguration().isLoadAnnotationAxioms());
        OWLDataFactory df = m.getOWLDataFactory();

        OntologyModel o1 = m.createOntology();
        OWLClass cl = df.getOWLClass(IRI.create("C"));
        OWLAnnotationProperty ap = df.getOWLAnnotationProperty(IRI.create("http://a"));
        OWLAnnotation a1 = df.getOWLAnnotation(ap, df.getOWLLiteral("assertion1"));
        OWLAnnotation a2 = df.getOWLAnnotation(df.getRDFSComment(), df.getOWLLiteral("assertion2"));

        o1.add(df.getOWLDeclarationAxiom(cl));
        o1.add(df.getOWLDeclarationAxiom(ap));
        o1.add(df.getOWLAnnotationPropertyDomainAxiom(ap, IRI.create("domain")));
        o1.add(df.getOWLAnnotationPropertyRangeAxiom(ap, IRI.create("range")));
        o1.add(df.getOWLAnnotationAssertionAxiom(cl.getIRI(), a1));
        o1.add(df.getOWLAnnotationAssertionAxiom(cl.getIRI(), a2));
        List<OWLAxiom> axioms = o1.axioms().collect(Collectors.toList());
        axioms.forEach(a -> LOGGER.debug("{}", a));
        ReadWriteUtils.print(o1);

        LOGGER.debug("Change Load Annotation settings");
        m.setOntologyLoaderConfiguration(m.getOntologyLoaderConfiguration().setLoadAnnotationAxioms(false));
        Assert.assertFalse("Incorrect settings", m.getOntologyLoaderConfiguration().isLoadAnnotationAxioms());
        // check the axioms changed.
        List<OWLAxiom> axioms1 = o1.axioms().collect(Collectors.toList());
        axioms1.forEach(a -> LOGGER.debug("{}", a));
        Assert.assertEquals("Should be 2 axioms only", 2, axioms1.size());
        Assert.assertTrue("Can't find declaration for " + ap, axioms1.contains(df.getOWLDeclarationAxiom(ap)));
        Assert.assertTrue("The declaration for " + cl + " should be with annotations now",
                axioms1.contains(df.getOWLDeclarationAxiom(cl, Arrays.asList(a1, a2))));

        LOGGER.debug("Create new ontology ");
        OntologyModel o2 = m.createOntology();
        axioms.forEach(o2::add);
        ReadWriteUtils.print(o2);
        List<OWLAxiom> axioms2 = o2.axioms().collect(Collectors.toList());
        axioms2.forEach(a -> LOGGER.debug("{}", a));
        Assert.assertEquals("Should be 2 axioms only", 2, axioms2.size());
        Assert.assertTrue("Can't find declaration for " + ap, axioms2.contains(df.getOWLDeclarationAxiom(ap)));
        Assert.assertTrue("The declaration for " + cl + " should not be unannotated now",
                axioms2.contains(df.getOWLDeclarationAxiom(cl)));
    }

    @Test
    public void testBulkAnnotationsSetting() throws Exception {
        OntologyManager m = OntManagers.createONT();
        Assert.assertTrue("Incorrect default settings",
                m.getOntologyLoaderConfiguration().isAllowBulkAnnotationAssertions());
        OWLDataFactory df = m.getOWLDataFactory();

        OntologyModel o1 = m.createOntology();
        OWLClass cl = df.getOWLClass(IRI.create("http://class"));
        OWLAnnotation a1 = df.getOWLAnnotation(df.getRDFSComment(), df.getOWLLiteral("plain assertion"));
        OWLAnnotation a2 = df.getOWLAnnotation(df.getRDFSLabel(), df.getOWLLiteral("bulk assertion"),
                df.getOWLAnnotation(df.getRDFSComment(), df.getOWLLiteral("the child")));
        Set<OWLAxiom> axioms1 = Stream.of(
                df.getOWLDeclarationAxiom(cl),
                df.getOWLAnnotationAssertionAxiom(cl.getIRI(), a1),
                df.getOWLAnnotationAssertionAxiom(cl.getIRI(), a2)
        ).collect(Collectors.toSet());
        LOGGER.debug("Axioms to be added: ");
        axioms1.forEach(a -> LOGGER.debug("{}", a));
        axioms1.forEach(o1::add);

        LOGGER.debug("Create second ontology with the same content.");
        String txt = ReadWriteUtils.toString(o1, OntFormat.TURTLE);
        LOGGER.debug("\n" + txt);
        OWLOntology o2 = m.loadOntologyFromOntologyDocument(ReadWriteUtils.toInputStream(txt));
        Assert.assertEquals("Incorrect axioms collection in the copied ontology",
                axioms1, o2.axioms().collect(Collectors.toSet()));

        LOGGER.debug("Change Allow Bulk Annotation Assertion setting");
        m.setOntologyLoaderConfiguration(m.getOntologyLoaderConfiguration().setAllowBulkAnnotationAssertions(false));
        Assert.assertFalse("Incorrect settings", m.getOntologyLoaderConfiguration().isAllowBulkAnnotationAssertions());
        o1.axioms().forEach(a -> LOGGER.debug("{}", a));
        Set<OWLAxiom> axioms2 = Stream.of(
                df.getOWLAnnotationAssertionAxiom(cl.getIRI(), a1),
                df.getOWLDeclarationAxiom(cl, Stream.of(a2).collect(Collectors.toSet()))
        ).collect(Collectors.toSet());

        Assert.assertEquals("Incorrect axioms count", axioms2.size(), o1.getAxiomCount());
        Assert.assertEquals("Incorrect axioms collection in the first ontology",
                axioms2, o1.axioms().collect(Collectors.toSet()));
        Assert.assertEquals("Incorrect axioms collection in the second ontology",
                axioms2, o2.axioms().collect(Collectors.toSet()));
        LOGGER.debug("Create third ontology with the same content.");
        OWLOntology o3 = m.loadOntologyFromOntologyDocument(ReadWriteUtils.toInputStream(txt));
        Assert.assertEquals("Incorrect axioms collection in the third ontology",
                axioms2, o3.axioms().collect(Collectors.toSet()));
    }

    @Test
    public void testLoadSplitBulkRootAnnotations() throws OWLOntologyCreationException {
        OntologyManager m = OntManagers.createONT();
        Assert.assertTrue(m.getOntologyConfigurator().shouldLoadAnnotations());
        Assert.assertFalse(m.getOntologyConfigurator().isSplitAxiomAnnotations());
        m.getOntologyConfigurator().setLoadAnnotationAxioms(false).setSplitAxiomAnnotations(true);

        String file = "ontapi/test-annotations-3.ttl";
        OWLOntology o = m.loadOntologyFromOntologyDocument(IRI.create(ReadWriteUtils.getResourceURI(file)));
        o.axioms().map(String::valueOf).forEach(LOGGER::debug);
        Assert.assertEquals(0, o.axioms(AxiomType.ANNOTATION_ASSERTION).count());
        Assert.assertEquals(3, o.axioms(AxiomType.DECLARATION).count());
        long annotationsCount = o.axioms(AxiomType.DECLARATION)
                .filter(a -> a.getEntity().isOWLClass()).mapToLong(a -> a.annotations().count()).sum();
        Assert.assertEquals("Wrong annotations count", 3, annotationsCount);
    }

    @Test
    public void testLoadWithIgnoreReadAxiomsErrors() throws OWLOntologyCreationException {
        IRI iri = IRI.create(ReadWriteUtils.getResourceURI("ontapi/recursive-graph.ttl"));
        LOGGER.debug("The file: {}", iri);
        OntologyManager m = OntManagers.createONT();
        m.getOntologyConfigurator().setIgnoreAxiomsReadErrors(true).setPerformTransformation(false);
        OntologyModel o = m.loadOntology(iri);
        ReadWriteUtils.print(o.asGraphModel());
        o.axioms().forEach(a -> LOGGER.debug("{}", a));
        Assert.assertEquals("Wrong axioms count", 5, o.getAxiomCount());
        Assert.assertEquals(1, o.axioms(AxiomType.SUBCLASS_OF).count());
    }
}
