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

package ru.avicomp.ontapi.tests;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.model.parameters.OntologyCopy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.avicomp.ontapi.*;
import ru.avicomp.ontapi.utils.FileMap;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

/**
 * Created by @szuev on 26.01.2018.
 */
public class CopyManagerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(CopyManagerTest.class);

    @Test
    public void testSimpleCoping() throws Exception {
        simpleCopyTest(OntManagers.createONT(), OntManagers.createOWL(), OntologyCopy.SHALLOW);
        simpleCopyTest(OntManagers.createONT(), OntManagers.createOWL(), OntologyCopy.DEEP);
        simpleCopyTest(OntManagers.createOWL(), OntManagers.createONT(), OntologyCopy.SHALLOW);
        simpleCopyTest(OntManagers.createOWL(), OntManagers.createONT(), OntologyCopy.DEEP);
        simpleCopyTest(OntManagers.createONT(), OntManagers.createONT(), OntologyCopy.SHALLOW);
        simpleCopyTest(OntManagers.createONT(), OntManagers.createONT(), OntologyCopy.DEEP);
    }

    @Test
    public void testSimpleMoving() throws Exception {
        LOGGER.info("1) Move OWL -> ONT");
        OWLDataFactory df = OntManagers.getDataFactory();
        OWLOntologyManager m1 = OntManagers.createOWL();
        OWLOntologyManager m2 = OntManagers.createONT();
        OWLOntologyManager m3 = OntManagers.createONT();
        IRI iri1 = IRI.create("http://test/1");
        OWLOntology o1 = m1.createOntology(iri1);
        o1.add(df.getOWLSubClassOfAxiom(df.getOWLClass("a"), df.getOWLClass("b")));
        try {
            m2.copyOntology(o1, OntologyCopy.MOVE);
            Assert.fail("Moving from OWL to ONT should be disabled");
        } catch (OWLOntologyCreationException e) {
            LOGGER.info("Expected.", e);
        }
        Assert.assertEquals("Incorrect ont-count in source", 1, m1.ontologies().count());
        Assert.assertEquals("Incorrect ont-count in destinaction", 0, m2.ontologies().count());

        LOGGER.info("2) Move ONT -> OWL");
        IRI iri2 = IRI.create("http://test/2");
        IRI docIRI = IRI.create("file://nothing");
        OWLDocumentFormat format = OntFormat.JSON_LD.createOwlFormat();
        OWLOntology o2 = m2.createOntology(iri2);
        m2.setOntologyFormat(o2, format);
        m2.setOntologyDocumentIRI(o2, docIRI);
        o2.add(df.getOWLEquivalentClassesAxiom(df.getOWLClass("a"), df.getOWLClass("b")));

        try {
            m1.copyOntology(o2, OntologyCopy.MOVE);
            Assert.fail("Expected exception while moving from ONT -> OWL");
        } catch (OntApiException a) {
            LOGGER.info("Expected.", a);
        }
        // check ONT manager
        // And don't care about OWL manager, we can't help him anymore.
        Assert.assertTrue("Can't find " + iri2, m2.contains(iri2));
        Assert.assertTrue("Can't find " + o2, m2.contains(o2));
        Assert.assertSame("Incorrect manager!", m2, o2.getOWLOntologyManager());
        Assert.assertEquals("Incorrect document IRI", docIRI, m2.getOntologyDocumentIRI(o2));
        Assert.assertEquals("Incorrect format", format, m2.getOntologyFormat(o2));

        LOGGER.info("3) Move ONT -> ONT");
        Assert.assertSame("Not same ontology!", o2, m3.copyOntology(o2, OntologyCopy.MOVE));
        Assert.assertTrue("Can't find " + iri2, m3.contains(iri2));
        Assert.assertFalse("There is still " + iri2, m2.contains(iri2));
        Assert.assertTrue("Can't find " + o2, m3.contains(o2));
        Assert.assertFalse("There is still " + o2, m2.contains(o2));
        Assert.assertSame("Not the same ontology", o2, m3.getOntology(iri2));
        Assert.assertSame("Incorrect manager!", m3, o2.getOWLOntologyManager());
        Assert.assertEquals("Incorrect document IRI", docIRI, m3.getOntologyDocumentIRI(o2));
        Assert.assertEquals("Incorrect format", format, m3.getOntologyFormat(o2));
        Assert.assertNull("Still have ont-format", m2.getOntologyFormat(o2));
        try {
            Assert.fail("Expected exception, but found some doc iri " + m2.getOntologyDocumentIRI(o2));
        } catch (UnknownOWLOntologyException u) {
            LOGGER.info("Expected.", u);
        }
    }

    @Test
    public void testCopyWholeManager1() throws Exception {
        IRI iri1 = IRI.create("http://spinrdf.org/sp");
        IRI doc1 = IRI.create(ReadWriteUtils.getResourcePath("etc", "sp.ttl").toUri());
        IRI iri2 = IRI.create("http://spinrdf.org/spin");
        IRI doc2 = IRI.create(ReadWriteUtils.getResourcePath("etc", "spin.ttl").toUri());

        OntologyManager from = OntManagers.createONT();
        from.getIRIMappers().add(FileMap.create(iri1, doc1));
        from.getIRIMappers().add(FileMap.create(iri2, doc2));
        from.loadOntologyFromOntologyDocument(iri2);
        Assert.assertEquals(2, from.ontologies().count());

        LOGGER.info("Copy manager");
        OntologyManager to = CopyManagerTest.copyManager(from);
        Assert.assertEquals(2, to.ontologies().count());

        // validate doc iris:
        OWLOntology o1 = to.getOntology(iri1);
        Assert.assertNotNull(o1);
        Assert.assertEquals(doc1, to.getOntologyDocumentIRI(o1));
        // Note: the same behaviour as OWL-API (tested: 5.1.4): the primary ontology has ontology-iri as document-iri.
        OWLOntology o2 = to.getOntology(iri2);
        Assert.assertNotNull(o2);
        Assert.assertEquals(iri2, to.getOntologyDocumentIRI(o2));
        compareManagersContentTest(from, to);
    }

    @Test
    public void testCopyWholeManager2() throws Exception {
        OntologyManager from = OntManagers.createONT();
        from.getOntologyConfigurator().disableWebAccess().setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
        from.loadOntologyFromOntologyDocument(IRI.create(ReadWriteUtils.getResourceURI("owlapi/importscyclic", "relaMath.owl")));
        from.loadOntologyFromOntologyDocument(IRI.create(ReadWriteUtils.getResourceURI("owlapi/importscyclic", "reprMath.owl")));
        from.ontologies().forEach(o -> LOGGER.info("{}:{}", o.getOntologyID(), o.getAxiomCount()));
        OntologyManager to = CopyManagerTest.copyManager(from);
        to.ontologies().forEach(System.out::println);
        compareManagersContentTest(from, to);
    }

    /**
     * Copies managers content to new instance
     *
     * @param from {@link OntologyManager}
     * @return new instance of {@link OntologyManager}
     */
    public static OntologyManager copyManager(OntologyManager from) {
        OntologyManager res = OntManagers.createONT();
        copyManager(from, res, false);
        return res;
    }

    /**
     * Copies managers content.
     *
     * @param from     source
     * @param to       destination
     * @param silently if true ignore {@link OWLOntologyCreationException} while coping
     * @throws OntApiException if there are some exceptions and {@code silently = true}
     */
    public static void copyManager(OntologyManager from, OntologyManager to, boolean silently) throws OntApiException {
        OntApiException ex = new OntApiException("Can't copy manager:");
        from.ontologies()
                .sorted(Comparator.comparingInt(o -> (int) o.imports().count()))
                .forEach(o -> {
                    try {
                        to.copyOntology(o, OntologyCopy.DEEP);
                    } catch (OWLOntologyCreationException e) {
                        ex.addSuppressed(e);
                    }
                });
        if (!silently && ex.getSuppressed().length != 0) {
            throw ex;
        }
    }

    private static void simpleCopyTest(OWLOntologyManager from, OWLOntologyManager to, OntologyCopy mode) throws Exception {
        LOGGER.info("Copy (" + mode + ") " + from.getClass().getInterfaces()[0].getSimpleName() + " -> " + to.getClass().getInterfaces()[0].getSimpleName());
        long fromCount = from.ontologies().count();
        long toCount = to.ontologies().count();

        OWLDataFactory df = from.getOWLDataFactory();
        IRI iri = IRI.create("test" + System.currentTimeMillis());
        LOGGER.debug("Create ontology " + iri);
        OWLClass clazz = df.getOWLClass("x");
        OWLOntology o1 = from.createOntology(iri);
        o1.add(df.getOWLDeclarationAxiom(clazz));

        to.copyOntology(o1, OntologyCopy.DEEP);
        Assert.assertEquals("Incorrect ontologies count inside OWL-manager", fromCount + 1, from.ontologies().count());
        Assert.assertEquals("Incorrect ontologies count inside ONT-manager", toCount + 1, to.ontologies().count());
        Assert.assertTrue("Can't find " + iri, to.contains(iri));
        OWLOntology o2 = to.getOntology(iri);
        Assert.assertNotNull("Can't find " + to, o2);
        Assert.assertNotSame("Should not be same", o1, o2);
        Set<OWLClass> classes = o2.classesInSignature().collect(Collectors.toSet());
        Assert.assertEquals("Should be single class inside", 1, classes.size());
        Assert.assertTrue("Can't find " + clazz, classes.contains(clazz));
    }

    private static void compareManagersContentTest(OntologyManager left, OntologyManager right) {
        Assert.assertEquals(left.ontologies().count(), right.ontologies().count());
        left.ontologies().forEach(src -> {
            OWLOntologyID id = src.getOntologyID();
            OntologyModel dst = right.getOntology(id);
            Assert.assertNotNull("Can't find ontology " + id, dst);
            List<OWLAxiom> expectedAxioms = src.axioms(Imports.EXCLUDED).filter(t -> !t.getAxiomType().equals(AxiomType.DECLARATION)).collect(Collectors.toList());
            List<OWLAxiom> actualAxioms = dst.axioms(Imports.EXCLUDED).filter(t -> !t.getAxiomType().equals(AxiomType.DECLARATION)).collect(Collectors.toList());
            Assert.assertEquals("Axioms list differ for " + id, expectedAxioms.size(), actualAxioms.size());
        });
    }

}
