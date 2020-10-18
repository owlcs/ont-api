/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2020, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.tests.managers;

import com.github.owlcs.ontapi.*;
import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.jena.utils.Graphs;
import com.github.owlcs.ontapi.utils.FileMap;
import com.github.owlcs.ontapi.utils.ReadWriteUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.model.parameters.OntologyCopy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by @szuev on 26.01.2018.
 */
public class CopyManagerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(CopyManagerTest.class);

    /**
     * Copies managers content to new instance
     *
     * @param from {@link OntologyManager}
     * @return new instance of {@link OntologyManager}
     */
    private static OntologyManager deepCopyManager(OntologyManager from) {
        OntologyManager res = OntManagers.createManager();
        copyManager(from, res, OntologyCopy.DEEP);
        return res;
    }

    /**
     * Copies the whole managers content.
     *
     * @param from source
     * @param to   destination
     * @param mode {@link OntologyCopy}
     * @throws OntApiException if there are some exceptions and {@code silently = true}
     */
    @SuppressWarnings("SameParameterValue")
    private static void copyManager(OntologyManager from, OntologyManager to, OntologyCopy mode) throws OntApiException {
        OntApiException ex = new OntApiException("Can't copy manager:");
        from.ontologies()
                .sorted(Comparator.comparingInt(o -> (int) o.imports().count()))
                .forEach(o -> {
                    try {
                        to.copyOntology(o, mode);
                    } catch (Exception e) {
                        ex.addSuppressed(e);
                    }
                });
        if (ex.getSuppressed().length != 0) {
            throw ex;
        }
    }

    private static void compareManagersContentTest(OWLOntologyManager left, OWLOntologyManager right) {
        Assertions.assertEquals(left.ontologies().count(), right.ontologies().count());
        left.ontologies()
                .forEach(src -> {
                    OWLOntologyID id = src.getOntologyID();
                    LOGGER.debug("Test {}", id);
                    OWLOntology dst = right.getOntology(id);
                    Assertions.assertNotNull(dst, "Can't find ontology " + id);
                    List<OWLAxiom> expectedAxioms = src.axioms(Imports.EXCLUDED)
                            .filter(t -> !t.getAxiomType().equals(AxiomType.DECLARATION))
                            .collect(Collectors.toList());
                    List<OWLAxiom> actualAxioms = dst.axioms(Imports.EXCLUDED)
                            .filter(t -> !t.getAxiomType().equals(AxiomType.DECLARATION))
                            .collect(Collectors.toList());
                    Assertions.assertEquals(expectedAxioms.size(), actualAxioms.size(), "Axioms list differ for " + id);
                });
    }

    private static OWLImportsDeclaration getImportsDeclaration(OWLOntology o) {
        return o.getOWLOntologyManager().getOWLDataFactory()
                .getOWLImportsDeclaration(o.getOntologyID()
                        .getDefaultDocumentIRI().orElseThrow(AssertionError::new));
    }

    @Test
    public void testSimpleCoping() throws Exception {
        simpleCopyTest(OntManagers.createManager(), OntManagers.createOWLAPIImplManager(), OntologyCopy.SHALLOW);
        simpleCopyTest(OntManagers.createManager(), OntManagers.createOWLAPIImplManager(), OntologyCopy.DEEP);
        simpleCopyTest(OntManagers.createOWLAPIImplManager(), OntManagers.createManager(), OntologyCopy.SHALLOW);
        simpleCopyTest(OntManagers.createOWLAPIImplManager(), OntManagers.createManager(), OntologyCopy.DEEP);
        simpleCopyTest(OntManagers.createManager(), OntManagers.createManager(), OntologyCopy.SHALLOW);
        simpleCopyTest(OntManagers.createManager(), OntManagers.createManager(), OntologyCopy.DEEP);
    }

    private void simpleCopyTest(OWLOntologyManager from, OWLOntologyManager to, OntologyCopy mode) throws Exception {
        LOGGER.debug("Copy ({}) {} -> {}", mode,
                from.getClass().getInterfaces()[0].getSimpleName(), to.getClass().getInterfaces()[0].getSimpleName());
        long fromCount = from.ontologies().count();
        long toCount = to.ontologies().count();

        OWLDataFactory df = from.getOWLDataFactory();
        IRI iri = IRI.create("test" + System.currentTimeMillis());
        LOGGER.debug("Create ontology " + iri);
        OWLClass clazz = df.getOWLClass("x");
        OWLOntology o1 = from.createOntology(iri);
        o1.add(df.getOWLDeclarationAxiom(clazz));

        to.copyOntology(o1, mode);
        Assertions.assertEquals(fromCount + 1, from.ontologies().count(), "Incorrect ontologies count inside OWL-manager");
        Assertions.assertEquals(toCount + 1, to.ontologies().count(), "Incorrect ontologies count inside ONT-manager");
        Assertions.assertTrue(to.contains(iri), "Can't find " + iri);
        OWLOntology o2 = to.getOntology(iri);
        Assertions.assertNotNull(o2, "Can't find " + to);
        Assertions.assertNotSame(o1, o2, "Should not be same");
        Set<OWLClass> classes = o2.classesInSignature().collect(Collectors.toSet());
        Assertions.assertEquals(1, classes.size(), "Should be single class inside");
        Assertions.assertTrue(classes.contains(clazz), "Can't find " + clazz);
    }

    @Test
    public void testMoveFromDefaultOWLAPIManager() {
        Assertions.assertThrows(OntApiException.Unsupported.class, this::moveFromDefaultOWLAPIManager);
    }

    private void moveFromDefaultOWLAPIManager() throws OWLOntologyCreationException {
        OWLOntologyManager src = OntManagers.createOWLAPIImplManager();
        OWLOntologyManager dst = OntManagers.createManager();
        OWLOntology o = src.createOntology();
        dst.copyOntology(o, OntologyCopy.MOVE);
    }

    @Test
    public void testMoveFromONTAPIManager() {
        Assertions.assertThrows(OntApiException.Unsupported.class, this::moveFromONTAPIManager);
    }

    private void moveFromONTAPIManager() throws OWLOntologyCreationException {
        OWLOntologyManager src = OntManagers.createManager();
        OWLOntologyManager dst = OntManagers.createManager();
        OWLOntology o = src.createOntology();
        dst.copyOntology(o, OntologyCopy.MOVE);
    }

    @Test
    public void testCopyWholeManager1() throws Exception {
        IRI iri1 = IRI.create("http://spinrdf.org/sp");
        IRI doc1 = IRI.create(ReadWriteUtils.getResourcePath("etc", "sp.ttl").toUri());
        IRI iri2 = IRI.create("http://spinrdf.org/spin");
        IRI doc2 = IRI.create(ReadWriteUtils.getResourcePath("etc", "spin.ttl").toUri());
        IRI iri3 = IRI.create("http://spinrdf.org/spl");
        IRI doc3 = IRI.create(ReadWriteUtils.getResourcePath("etc", "spl.spin.ttl").toUri());
        IRI iri4 = IRI.create("http://spinrdf.org/spif");
        IRI doc4 = IRI.create(ReadWriteUtils.getResourcePath("etc", "spif.ttl").toUri());

        OntologyManager from = OntManagers.createManager();
        from.getIRIMappers().add(FileMap.create(iri1, doc1));
        from.getIRIMappers().add(FileMap.create(iri2, doc2));
        from.getIRIMappers().add(FileMap.create(iri3, doc3));
        from.getIRIMappers().add(FileMap.create(iri4, doc4));

        // loads sp.ttl, then spif.ttl, then try to load spin.ttl
        from.loadOntologyFromOntologyDocument(iri1);
        Assertions.assertEquals(1, from.ontologies().count());
        from.loadOntologyFromOntologyDocument(iri4);
        Assertions.assertEquals(4, from.ontologies().count());
        try {
            from.loadOntologyFromOntologyDocument(doc2);
            Assertions.fail(doc2 + " has been successfully loaded.");
        } catch (OWLOntologyAlreadyExistsException e) {
            LOGGER.debug("It is ok : {}", e.getMessage());
        }
        Assertions.assertEquals(4, from.ontologies().count());


        LOGGER.debug("Copy manager");
        OntologyManager to = CopyManagerTest.deepCopyManager(from);
        Assertions.assertEquals(4, to.ontologies().count());

        // validate doc iris:
        OWLOntology o2 = to.getOntology(iri2);
        Assertions.assertNotNull(o2);
        Assertions.assertEquals(doc2, to.getOntologyDocumentIRI(o2));
        OWLOntology o3 = to.getOntology(iri3);
        Assertions.assertNotNull(o3);
        Assertions.assertEquals(doc3, to.getOntologyDocumentIRI(o3));
        // Note: the same behaviour as OWL-API (tested: 5.1.4): the primary ontology has ontology-iri as document-iri.
        OWLOntology o4 = to.getOntology(iri4);
        Assertions.assertNotNull(o4);
        Assertions.assertEquals(iri4, to.getOntologyDocumentIRI(o4));
        OWLOntology o1 = to.getOntology(iri1);
        Assertions.assertNotNull(o1);
        Assertions.assertEquals(iri1, to.getOntologyDocumentIRI(o1));

        compareManagersContentTest(from, to);
    }

    @Test
    public void testCopyWholeManager2() throws Exception {
        IRI iri1 = IRI.create("http://spinrdf.org/sp");
        IRI doc1 = IRI.create(ReadWriteUtils.getResourcePath("omn", "sp.omn").toUri());
        IRI iri2 = IRI.create("http://spinrdf.org/spin");
        IRI doc2 = IRI.create(ReadWriteUtils.getResourcePath("omn", "spin.omn").toUri());

        OntologyManager from = OntManagers.createManager();
        from.getOntologyConfigurator().disableWebAccess();
        from.getIRIMappers().add(FileMap.create(iri1, doc1));
        from.getIRIMappers().add(FileMap.create(iri2, doc2));

        from.loadOntologyFromOntologyDocument(iri2);
        Assertions.assertEquals(2, from.ontologies().count());

        LOGGER.debug("Copy manager");
        OntologyManager to = CopyManagerTest.deepCopyManager(from);
        Assertions.assertEquals(2, to.ontologies().count());

        // validate doc iris:
        OWLOntology o1 = to.getOntology(iri1);
        Assertions.assertNotNull(o1);
        Assertions.assertEquals(doc1, to.getOntologyDocumentIRI(o1));
        OWLOntology o2 = to.getOntology(iri2);
        Assertions.assertNotNull(o2);
        Assertions.assertEquals(iri2, to.getOntologyDocumentIRI(o2));

        compareManagersContentTest(from, to);
    }

    @Test
    public void testCopyWholeManager3() throws Exception {
        OntologyManager from = OntManagers.createManager();
        from.getOntologyConfigurator().disableWebAccess()
                .setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
        from.loadOntologyFromOntologyDocument(IRI.create(ReadWriteUtils.getResourceURI("owlapi/importscyclic", "relaMath.owl")));
        from.loadOntologyFromOntologyDocument(IRI.create(ReadWriteUtils.getResourceURI("owlapi/importscyclic", "reprMath.owl")));
        from.ontologies().forEach(o -> LOGGER.debug("{}:{}", o.getOntologyID(), o.getAxiomCount()));
        OntologyManager to = CopyManagerTest.deepCopyManager(from);
        to.ontologies().forEach(x -> LOGGER.debug("{}", x));
        compareManagersContentTest(from, to);
    }

    @Test
    public void testShallowCopingIfSourceIsOntologyModel() {
        String uri_a = "urn:a";
        String uri_b = "urn:b";
        String uri_c = "urn:c";
        OntologyManager m1 = OntManagers.createManager();
        OntModel a = m1.createGraphModel(uri_a);
        OntModel b = m1.createGraphModel(uri_b);
        OntModel c = m1.createGraphModel(uri_c);
        a.addImport(b).addImport(c);

        OntologyManager m2 = OntManagers.createManager();
        m2.createGraphModel(uri_b);

        Ontology src = m1.getOntology(IRI.create(uri_a));
        Assertions.assertNotNull(src);
        m1.setOntologyDocumentIRI(src, IRI.create("x"));
        m1.setOntologyFormat(src, OntFormat.OBO.createOwlFormat());

        Ontology dst = m2.copyOntology(src, OntologyCopy.SHALLOW);
        Assertions.assertEquals(2, m2.ontologies().count());
        Assertions.assertEquals(1, dst.imports().count());
        Assertions.assertEquals(2, dst.importsDeclarations().count());

        Assertions.assertSame(a.getBaseGraph(), dst.asGraphModel().getBaseGraph());

        OWLDocumentFormat f = m2.getOntologyFormat(dst);
        IRI di = m2.getOntologyDocumentIRI(dst);
        LOGGER.debug("New doc IRI: {}", di);
        Assertions.assertNotNull(f);
        Assertions.assertNotNull(di);
        Assertions.assertEquals(OntFormat.TURTLE.createOwlFormat().getKey(), f.getKey());
        Assertions.assertEquals(OntGraphDocumentSource.wrap(a.getBaseGraph()).getDocumentIRI(), di);
    }

    @Test
    public void testDeepCopingIfSourceIsOntologyModel() {
        String uri_a = "urn:a";
        String uri_b = "urn:b";
        String uri_c = "urn:c";
        String uri_d = "urn:d";
        OntologyManager m1 = OntManagers.createManager();
        OntModel a = m1.createGraphModel(uri_a);
        OntModel b = m1.createGraphModel(uri_b);
        OntModel c = m1.createGraphModel(uri_c);
        OntModel d = m1.createGraphModel(uri_d);
        a.addImport(b.addImport(d)).addImport(c.addImport(d));

        OntologyManager m2 = OntManagers.createManager();
        m2.createGraphModel(uri_b);

        Ontology src_a = m1.getOntology(IRI.create(uri_a));
        Assertions.assertNotNull(src_a);
        IRI di_src_a = IRI.create("x");
        OWLDocumentFormat f_src_a = OntFormat.LATEX.createOwlFormat();
        m1.setOntologyDocumentIRI(src_a, di_src_a);
        m1.setOntologyFormat(src_a, f_src_a);

        OWLOntology dst_a = m2.copyOntology(src_a, OntologyCopy.DEEP);
        Assertions.assertEquals(2, m2.ontologies().count());
        Assertions.assertEquals(1, dst_a.imports().count());
        Assertions.assertEquals(2, dst_a.importsDeclarations().count());
        Ontology dst_b = m2.getOntology(IRI.create(uri_b));
        Assertions.assertNotNull(dst_b);
        Assertions.assertEquals(0, dst_b.importsDeclarations().count());

        OWLDocumentFormat f_dst_a = m2.getOntologyFormat(dst_a);
        IRI di_dst_a = m2.getOntologyDocumentIRI(dst_a);
        Assertions.assertSame(f_src_a, f_dst_a);
        Assertions.assertSame(di_src_a, di_dst_a);

        Ontology src_c = m1.getOntology(IRI.create(uri_c));
        Assertions.assertNotNull(src_c);
        OWLOntology dst_c = m2.copyOntology(src_c, OntologyCopy.DEEP);
        Assertions.assertEquals(3, m2.ontologies().count());
        Assertions.assertEquals(2, dst_a.imports().count());
        Assertions.assertEquals(1, dst_c.importsDeclarations().count());
        Assertions.assertEquals(0, dst_c.imports().count());
        Assertions.assertEquals(0, dst_b.importsDeclarations().count());
        Assertions.assertSame(dst_b, m2.getOntology(IRI.create(uri_b)));
        IRI di_dst_c = m2.getOntologyDocumentIRI(dst_c);
        LOGGER.debug("Doc IRI (c): {}", di_dst_c);
        Assertions.assertEquals(uri_c, di_dst_c.getIRIString());
        Assertions.assertEquals(OntFormat.TURTLE.createOwlFormat(), m2.getOntologyFormat(dst_c));

        OWLOntology dst_d = m2.getOntology(IRI.create(uri_d));
        Assertions.assertNull(dst_d);
        Ontology src_d = m1.getOntology(IRI.create(uri_d));
        Assertions.assertNotNull(src_d);
        dst_d = m2.copyOntology(src_d, OntologyCopy.DEEP);

        Assertions.assertEquals(4, m2.ontologies().count());
        Assertions.assertEquals(3, dst_a.imports().count());
        Assertions.assertEquals(1, dst_c.importsDeclarations().count());
        Assertions.assertEquals(1, dst_c.imports().count());
        Assertions.assertEquals(0, dst_b.importsDeclarations().count());
        Assertions.assertEquals(0, dst_d.importsDeclarations().count());
    }

    @Test
    public void testDeepCopingIfSourceIsNotOntologyModel() throws OWLOntologyCreationException {
        String uri_a = "urn:a";
        String uri_b = "urn:b";
        String uri_c = "urn:c";
        OWLOntologyManager m1 = OntManagers.createOWLAPIImplManager();
        OWLOntology a = m1.createOntology(IRI.create(uri_a));
        OWLOntology b = m1.createOntology(IRI.create(uri_b));
        OWLOntology c = m1.createOntology(IRI.create(uri_c));
        m1.applyChange(new AddImport(a, getImportsDeclaration(b)));
        m1.applyChange(new AddImport(a, getImportsDeclaration(c)));
        m1.applyChange(new AddImport(b, getImportsDeclaration(c)));

        OntologyManager m2 = OntManagers.createManager();
        m2.createGraphModel(uri_b);

        OWLOntology src_a = m1.getOntology(IRI.create(uri_a));
        Assertions.assertNotNull(src_a);
        IRI di_src_a = IRI.create("x");
        OWLDocumentFormat f_src_a = OntFormat.LATEX.createOwlFormat();
        m1.setOntologyDocumentIRI(src_a, di_src_a);
        m1.setOntologyFormat(src_a, f_src_a);

        OWLOntology dst_a = m2.copyOntology(src_a, OntologyCopy.DEEP);
        Assertions.assertEquals(2, m2.ontologies().count());
        Assertions.assertEquals(1, dst_a.imports().count());
        Assertions.assertEquals(2, dst_a.importsDeclarations().count());
        Ontology dst_b = m2.getOntology(IRI.create(uri_b));
        Assertions.assertNotNull(dst_b);
        Assertions.assertEquals(0, dst_b.importsDeclarations().count());

        OWLDocumentFormat f_dst_a = m2.getOntologyFormat(dst_a);
        IRI di_dst_a = m2.getOntologyDocumentIRI(dst_a);
        Assertions.assertSame(f_src_a, f_dst_a);
        Assertions.assertSame(di_src_a, di_dst_a);

        OWLOntology src_c = m1.getOntology(IRI.create(uri_c));
        Assertions.assertNotNull(src_c);
        OWLOntology dst_c = m2.copyOntology(src_c, OntologyCopy.DEEP);
        Assertions.assertEquals(3, m2.ontologies().count());
        Assertions.assertEquals(2, dst_a.imports().count());
        Assertions.assertEquals(0, dst_c.importsDeclarations().count());
        Assertions.assertEquals(0, dst_c.imports().count());
        Assertions.assertEquals(0, dst_b.importsDeclarations().count());
        Assertions.assertSame(dst_b, m2.getOntology(IRI.create(uri_b)));
        IRI di_dst_c = m2.getOntologyDocumentIRI(dst_c);
        LOGGER.debug("Doc IRI (c): {}", di_dst_c);
        Assertions.assertEquals(uri_c, di_dst_c.getIRIString());
        Assertions.assertEquals(OntFormat.RDF_XML.createOwlFormat(), m2.getOntologyFormat(dst_c));
    }

    @Test
    public void testShallowCopingManagerWithCyclingImports() {
        testCopingManagerWithCyclingImports(OntManagers.createManager(), OntManagers.createConcurrentManager(), OntologyCopy.SHALLOW);
    }

    @Test
    public void testDeepCopingManagerWithCyclingImports() {
        testCopingManagerWithCyclingImports(OntManagers.createConcurrentManager(), OntManagers.createManager(), OntologyCopy.DEEP);
    }

    private void testCopingManagerWithCyclingImports(OntologyManager m1, OntologyManager m2, OntologyCopy mode) {
        setupManagerWithCyclicImports(m1);
        testManagerWithCyclicImports(m1);

        m1.ontologies().forEach(o -> m2.copyOntology(o, mode));
        testManagerWithCyclicImports(m2);
    }

    private void setupManagerWithCyclicImports(OntologyManager m) {
        Ontology a = m.createOntology(IRI.create("A"));
        Ontology b = m.createOntology(IRI.create("B"));
        a.asGraphModel().addImport(b.asGraphModel());
        b.asGraphModel().addImport(a.asGraphModel());
    }

    private void testManagerWithCyclicImports(OntologyManager m) {
        Assertions.assertEquals(2, m.ontologies().peek(x -> {
            LOGGER.debug("TEST: {}", x);
            Assertions.assertEquals(2, Graphs.baseGraphs(((Ontology) x).asGraphModel().getGraph()).count());
        }).count());
    }

    @Test
    public void testShallowCopingManagerWithAnonymousOntologies() {
        testCopingManagerWithAnonymousOntologies(OntManagers.createConcurrentManager(),
                OntManagers.createManager(), OntologyCopy.SHALLOW);
    }

    @Test
    public void testDeepCopingManagerWithAnonymousOntologies() {
        testCopingManagerWithAnonymousOntologies(OntManagers.createManager(),
                OntManagers.createConcurrentManager(), OntologyCopy.DEEP);
    }

    private void testCopingManagerWithAnonymousOntologies(OntologyManager m1, OntologyManager m2, OntologyCopy mode) {
        setupManagerWithAnonymousOntologies(m1);
        // validate
        testManagerWithAnonymousOntologies(m1);
        // copy
        m1.ontologies().forEach(o -> m2.copyOntology(o, mode));
        // test
        testManagerWithAnonymousOntologies(m2);
    }

    private void setupManagerWithAnonymousOntologies(OntologyManager m) {
        m.createGraphModel("A").addImport(m.createGraphModel("B"));
        m.createGraphModel(null).addImport(m.createGraphModel("C"));
        m.createOntology();
    }

    private void testManagerWithAnonymousOntologies(OntologyManager m) {
        Assertions.assertEquals(5, m.ontologies().count());
        Assertions.assertEquals(2, m.ontologies().filter(IsAnonymous::isAnonymous).count());
        Assertions.assertEquals(3, m.ontologies().filter(x -> !x.imports().findFirst().isPresent()).count());
        Assertions.assertEquals(1, m.ontologies().filter(IsAnonymous::isAnonymous)
                .filter(x -> !x.imports().findFirst().isPresent()).count());
    }


}
