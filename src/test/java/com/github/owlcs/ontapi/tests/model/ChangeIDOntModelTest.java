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

package com.github.owlcs.ontapi.tests.model;

import com.github.owlcs.ontapi.OntApiException;
import com.github.owlcs.ontapi.OntManagers;
import com.github.owlcs.ontapi.Ontology;
import com.github.owlcs.ontapi.OntologyManager;
import com.github.owlcs.ontapi.jena.OntModelFactory;
import com.github.owlcs.ontapi.jena.model.OntClass;
import com.github.owlcs.ontapi.jena.model.OntID;
import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.jena.utils.Models;
import com.github.owlcs.ontapi.jena.vocabulary.OWL;
import com.github.owlcs.ontapi.jena.vocabulary.RDF;
import com.github.owlcs.ontapi.owlapi.objects.OWLAnnotationImplNotAnnotated;
import com.github.owlcs.ontapi.utils.OntIRI;
import org.apache.jena.rdf.model.*;
import org.apache.jena.util.ResourceUtils;
import org.apache.jena.vocabulary.RDFS;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Test changing OWLOntologyID through jena and owl-api
 * <p>
 * Created by @szuev on 08.10.2016.
 */
public class ChangeIDOntModelTest extends OntModelTestBase {

    @Test
    public void testIDIsChangedExternally() {
        OntModel g1 = OntModelFactory.createModel();
        OntologyManager m = OntManagers.createONT();
        m.addOntology(g1.getGraph());
        String iri1 = "http://x.com";
        g1.setID(iri1);
        Assert.assertNotNull(m.getOntology(IRI.create(iri1)));

        OntModel g2 = m.createOntology().asGraphModel();
        String iri2 = "http://y.com";
        g2.setID(iri2);
        Assert.assertNotNull(m.getOntology(IRI.create(iri2)));

        OntologyManager m2 = OntManagers.createConcurrentONT();
        Ontology o3 = m2.createOntology();
        OntModel g3 = o3.asGraphModel();
        String iri3 = "http://z.com";
        g3.setID(iri3);
        Assert.assertNotNull(m2.getOntology(IRI.create(iri3)));
        Assert.assertTrue(m2.contains(o3));
    }

    /**
     * WARNING: this test shows that there is a bug in OWL-API (5.0.5):
     * the original way (see {@link uk.ac.manchester.cs.owl.owlapi.OWLOntologyImpl#applyChanges(List)})
     * doesn't work correctly in OWL-API.
     * The default method {@link OWLOntology#applyChanges(List)}) is never called due to it has explicit implementation.
     * In ONT-API there is no such problem.
     */
    @Test
    public void testApplyChanges() throws Exception {
        ApplyChangesWrapper w1 = new ApplyChangesWrapper((o, i) -> o
                .applyChange(new SetOntologyID(o, i)), "OWLOntology#applyChange()");
        ApplyChangesWrapper w3 = new ApplyChangesWrapper((o, i) -> o.getOWLOntologyManager()
                .applyChanges(new SetOntologyID(o, i)), "OWLOntologyManager#applyChanges(...)");
        ApplyChangesWrapper w2 = new ApplyChangesWrapper((o, i) -> o.getOWLOntologyManager()
                .applyChange(new SetOntologyID(o, i)), "OWLOntologyManager#applyChange()");
        ApplyChangesWrapper w4 = new ApplyChangesWrapper((o, i) -> o
                .applyChanges(new SetOntologyID(o, i)), "OWLOntology#applyChanges(...)");
        for (ApplyChangesWrapper w : Arrays.asList(w1, w2, w3, w4)) {
            //testApplyChanges(OntManagers.createOWL(), w); // <-- will fail on w4
            testApplyChanges(OntManagers.createONT(), w);
        }
    }

    private void testApplyChanges(OWLOntologyManager m, ApplyChangesWrapper p) throws Exception {
        String msg = "Test[" + (m instanceof OntologyManager ? "ONT" : "OWL") + "] " + p;
        LOGGER.debug(msg);
        IRI x = IRI.create("x");
        OWLOntology o = m.createOntology(x);
        OWLOntologyID id1 = o.getOntologyID();
        LOGGER.debug("2)iri=<{}>, id=<{}>", x, id1);
        Assert.assertTrue("can't find " + x, m.contains(x));
        Assert.assertTrue("can't find " + id1, m.contains(id1));
        IRI y = IRI.create("y");
        p.process(o, y);
        OWLOntologyID id2 = o.getOntologyID();
        LOGGER.debug("2)iri=<{}>, id=<{}>", y, id2);
        Assert.assertFalse("still " + x, m.contains(x));
        Assert.assertFalse("still " + id1, m.contains(id1));
        Assert.assertTrue("can't find " + y, m.contains(y));
        Assert.assertTrue("can't find " + id2, m.contains(id2));
        LOGGER.debug("PASS: " + msg);
    }

    @Test
    public void testDifferent() {
        OntologyManager manager = OntManagers.createONT();

        // anon ontology
        Ontology anon = manager.createOntology();
        Assert.assertEquals("Should be one ontology inside jena-graph", 1,
                anon.asGraphModel().listStatements(null, RDF.type, OWL.Ontology).toList().size());

        LOGGER.debug("Create owl ontology.");
        OntIRI iri = OntIRI.create("http://test.test/change-id");
        OntIRI clazz = iri.addFragment("SomeClass1");
        List<Resource> imports = Stream.of(ResourceFactory.createResource("http://test.test/some-import")).collect(Collectors.toList());
        Map<Property, List<RDFNode>> annotations = new HashMap<>();
        annotations.computeIfAbsent(RDFS.comment, p -> new ArrayList<>())
                .add(ResourceFactory.createLangLiteral("Some comment N1", "xyx"));
        annotations.computeIfAbsent(RDFS.comment, p -> new ArrayList<>())
                .add(ResourceFactory.createPlainLiteral("Some comment N2"));
        annotations.computeIfAbsent(OWL.incompatibleWith, p -> new ArrayList<>())
                .add(ResourceFactory.createResource("http://yyy/zzz"));

        OWLDataFactory df = manager.getOWLDataFactory();
        OWLOntologyID id = iri.toOwlOntologyID();
        LOGGER.debug("Create ontology, ID={}", id);
        Ontology owl = manager.createOntology(id);
        createOntologyProperties(owl, imports, annotations);
        OWLAnnotationProperty ap1 = df.getOWLAnnotationProperty(iri.addFragment("annotation-property-1"));
        OWLAnnotation a1 = df.getOWLAnnotation(ap1, df.getOWLLiteral("tess-annotation-1"));
        manager.applyChange(new AddAxiom(owl, df.getOWLDeclarationAxiom(df.getOWLClass(clazz),
                Collections.singletonList(a1))));
        OntModel jena = owl.asGraphModel();
        debug(owl);

        long numOfOnt = manager.ontologies().count();

        OWLOntologyID test1 = iri.addPath("test1").toOwlOntologyID(OntIRI.create("http://version/1.0"));
        LOGGER.debug("1)Change ontology iri to {} through owl-api.", test1);
        owl.applyChanges(new SetOntologyID(owl, test1));
        testIRIChanged(manager, owl, jena, test1, imports, annotations);
        testHasClass(owl, jena, clazz);
        Assert.assertEquals("Incorrect number of ontologies", numOfOnt, manager.ontologies().count());

        Resource ont = jena.listStatements(null, RDF.type, OWL.Ontology).mapWith(Statement::getSubject).toList().get(0);
        OWLOntologyID test2 = iri.addPath("test2").toOwlOntologyID(test1.getVersionIRI().orElse(null));
        LOGGER.debug("2)Change ontology iri to {} through jena.", test2);
        ResourceUtils.renameResource(ont, OntIRI.toStringIRI(test2));
        testIRIChanged(manager, owl, jena, test2, imports, annotations);
        testHasClass(owl, jena, clazz);
        Assert.assertEquals("Incorrect number of ontologies", numOfOnt, manager.ontologies().count());

        ont = jena.listStatements(null, RDF.type, OWL.Ontology).mapWith(Statement::getSubject).toList().get(0);
        OWLOntologyID test3 = new OWLOntologyID(); //iri.addPath("test3").toOwlOntologyID();
        LOGGER.debug("3)Change ontology iri to {} through jena.", test3);
        ResourceUtils.renameResource(ont, null);
        try {
            OWLOntologyID actual = owl.getOntologyID();
            Assert.fail("Possible to get id: " + actual);
        } catch (OntApiException a) {
            LOGGER.debug("Expected '{}'", a.getMessage());
            // fix broken id:
            jena.removeAll(null, OWL.versionIRI, null);
        }
        testIRIChanged(manager, owl, jena, test3, imports, annotations);
        testHasClass(owl, jena, clazz);
        Assert.assertEquals("Incorrect number of ontologies", numOfOnt, manager.ontologies().count());

        OWLOntologyID test4 = iri.addPath("test4").toOwlOntologyID();
        LOGGER.debug("4)Change ontology iri to {} through owl-api.", test4);
        manager.applyChange(new SetOntologyID(owl, test4));
        testIRIChanged(manager, owl, jena, test4, imports, annotations);
        testHasClass(owl, jena, clazz);
        Assert.assertEquals("Incorrect number of ontologies", numOfOnt, manager.ontologies().count());

        //anon:
        OWLOntologyID test5 = new OWLOntologyID();
        LOGGER.debug("5)Change ontology iri to {} through owl-api.", test5);
        manager.applyChange(new SetOntologyID(owl, test5));
        testIRIChanged(manager, owl, jena, test5, imports, annotations);
        testHasClass(owl, jena, clazz);
        Assert.assertEquals("Incorrect number of ontologies", numOfOnt, manager.ontologies().count());
    }

    private static void testIRIChanged(OntologyManager manager,
                                       Ontology owl,
                                       OntModel jena,
                                       OWLOntologyID id,
                                       List<Resource> imports,
                                       Map<Property,
                                               List<RDFNode>> annotations) {
        debug(owl);

        if (!id.isAnonymous())
            Assert.assertTrue("Can't find ontology " + id + " by ID", manager.contains(id));
        Assert.assertTrue("Can't find ontology " + id + " in manager", manager.contains(owl));
        if (id.getOntologyIRI().isPresent()) {
            Assert.assertTrue("Can't find " + id.getOntologyIRI().get() + " in manager",
                    manager.contains(id.getOntologyIRI().get()));
        }

        String iri = id.getOntologyIRI().isPresent() ?
                id.getOntologyIRI().orElseThrow(AssertionError::new).getIRIString() : null;
        OntID ontID = jena.getID();
        Assert.assertNotNull("Can't find new ontology for iri " + id, ontID);
        Assert.assertNotNull("Can't find new ontology in jena", owl.asGraphModel().getID());
        Assert.assertEquals("Incorrect jena id-iri", iri, ontID.getURI());
        Assert.assertTrue("Incorrect ID expected=" + id + ", actual=" + owl.getOntologyID(),
                (id.isAnonymous() && owl.getOntologyID().isAnonymous()) || owl.getOntologyID().equals(id));
        // check imports:
        List<String> expected = imports.stream().map(Resource::getURI).sorted().collect(Collectors.toList());
        List<String> actualOwl = owl.importsDeclarations()
                .map(OWLImportsDeclaration::getIRI).map(IRI::getIRIString).sorted().collect(Collectors.toList());
        List<String> actualJena = jena.getID().imports().sorted().collect(Collectors.toList());
        Assert.assertEquals("Incorrect owl imports", expected, actualOwl);
        Assert.assertEquals("Incorrect jena imports", expected, actualJena);
        // check owl-annotations:
        int count = 0;
        for (Property p : annotations.keySet()) {
            count += annotations.get(p).size();
            annotations.get(p).forEach(node -> {
                OWLAnnotation a = toOWLAnnotation(p, node);
                Assert.assertTrue("Can't find annotation " + a, owl.annotations().anyMatch(a::equals));
            });
        }
        Assert.assertEquals("Incorrect annotation count", count, owl.annotations().count());
        // check jena annotations:
        for (Property p : annotations.keySet()) {
            List<RDFNode> actualList = jena.listStatements(ontID, p, (RDFNode) null).mapWith(Statement::getObject).
                    toList().stream().sorted(Models.RDF_NODE_COMPARATOR).collect(Collectors.toList());
            List<RDFNode> expectedList = annotations.get(p).stream()
                    .sorted(Models.RDF_NODE_COMPARATOR).collect(Collectors.toList());
            Assert.assertEquals("Incorrect list of annotations", expectedList, actualList);
        }
    }

    private static void testHasClass(Ontology owl, OntModel jena, IRI classIRI) {
        OWLEntity entity = owl.axioms(AxiomType.DECLARATION)
                .map(OWLDeclarationAxiom::getEntity)
                .filter(AsOWLClass::isOWLClass).findFirst().orElseThrow(AssertionError::new);
        Assert.assertEquals("Incorrect owl-class uri", classIRI, entity.getIRI());
        List<OntClass> classes = jena.ontEntities(OntClass.class).collect(Collectors.toList());
        Assert.assertFalse("Can't find any jena-class", classes.isEmpty());
        Assert.assertEquals("Incorrect jena-class uri", classIRI.getIRIString(), classes.get(0).getURI());
    }

    private static void createOntologyProperties(Ontology owl,
                                                 List<Resource> imports,
                                                 Map<Property, List<RDFNode>> annotations) {
        OWLOntologyManager m = owl.getOWLOntologyManager();
        OWLDataFactory df = m.getOWLDataFactory();
        imports.forEach(x -> m.applyChange(new AddImport(owl, df.getOWLImportsDeclaration(OntIRI.create(x.getURI())))));
        for (Property p : annotations.keySet()) {
            annotations.get(p)
                    .forEach(node -> m.applyChange(new AddOntologyAnnotation(owl, toOWLAnnotation(df, p, node))));
        }
    }

    private static OWLAnnotation toOWLAnnotation(Property property, RDFNode node) {
        return toOWLAnnotation(OntManagers.getDataFactory(), property, node);
    }

    private static OWLAnnotation toOWLAnnotation(OWLDataFactory factory, Property property, RDFNode node) {
        OWLAnnotationProperty p = factory.getOWLAnnotationProperty(OntIRI.create(property));
        OWLAnnotationValue v = null;
        if (node.isURIResource()) {
            v = OntIRI.create(node.asResource());
        } else if (node.isLiteral()) {
            Literal literal = node.asLiteral();
            v = factory.getOWLLiteral(literal.getLexicalForm(), literal.getLanguage());
        } else {
            Assert.fail("Unknown node " + node);
        }
        return new OWLAnnotationImplNotAnnotated(p, v);
    }

    private static class ApplyChangesWrapper {
        private final SetOntologyIRI op;
        private final String msg;

        private ApplyChangesWrapper(SetOntologyIRI applyChanges, String msg) {
            this.op = applyChanges;
            this.msg = msg;
        }

        void process(OWLOntology o, IRI iri) {
            op.apply(o, iri);
        }

        @Override
        public String toString() {
            return msg;
        }

        private interface SetOntologyIRI {
            void apply(OWLOntology o, IRI iri);
        }
    }
}