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

package ru.avicomp.ontapi.tests.transforms;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.FileManager;
import org.apache.jena.vocabulary.RDFS;
import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avicomp.ontapi.OntFormat;
import ru.avicomp.ontapi.OntManagers;
import ru.avicomp.ontapi.OntologyManager;
import ru.avicomp.ontapi.OntologyModel;
import ru.avicomp.ontapi.jena.OntModelFactory;
import ru.avicomp.ontapi.jena.UnionGraph;
import ru.avicomp.ontapi.jena.impl.conf.OntModelConfig;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.utils.Graphs;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;
import ru.avicomp.ontapi.jena.vocabulary.SWRL;
import ru.avicomp.ontapi.transforms.GraphTransformers;
import ru.avicomp.ontapi.transforms.Transform;
import ru.avicomp.ontapi.utils.*;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * to test {@link GraphTransformers}
 * <p>
 * Created by @szuev on 30.10.2016.
 */
public class GraphTransformersTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(GraphTransformersTest.class);

    @Test
    public void testLoadSpinLibraries() throws Exception {
        int axiomsCountSPINMAPL = 856;
        int axiomsCountTotal = 7625;
        OntologyManager m = setUpSpinManager(OntManagers.createONT());

        IRI iri = SpinModels.SPINMAPL.getIRI();
        OntologyModel spinmapl = m.loadOntology(iri);
        String actualTree = Graphs.importsTreeAsString(spinmapl.asGraphModel().getGraph());
        LOGGER.debug("Tree:\n{}", actualTree);
        Assert.assertEquals(27, actualTree.split("\n").length);

        OntologyModel spl = m.getOntology(SpinModels.SPL.getIRI());
        Assert.assertNotNull("Can't find SPL", spl);

        String splAsString = ReadWriteUtils.toString(spl.asGraphModel(), OntFormat.TURTLE);
        LOGGER.debug(splAsString);

        Assert.assertEquals("Incorrect spinmapl axioms count", axiomsCountSPINMAPL, spinmapl.getAxiomCount());
        Assert.assertEquals("Incorrect total axioms count", axiomsCountTotal, spinmapl.axioms(Imports.INCLUDED).count());

        OWLAnnotationProperty spText = m.getOWLDataFactory().getOWLAnnotationProperty(IRI.create(SP.text.getURI()));
        OWLAnnotationAssertionAxiom axiom = spl.axioms(AxiomType.ANNOTATION_ASSERTION).filter(a -> Objects.equals(a.getProperty(), spText))
                .findAny().orElseThrow(() -> new AssertionError("Can't find any sp:text annotation assertion"));
        Optional<OWLLiteral> literal = axiom.getValue().asLiteral();
        Optional<OWLAnonymousIndividual> individual = axiom.getSubject().asAnonymousIndividual();
        Assert.assertTrue("No literal", literal.isPresent());
        Assert.assertTrue("No individual", individual.isPresent());
        LOGGER.info("Axioms related to query <" + literal.get().getLiteral().replace("\n", " ") + ">");
        spl.referencingAxioms(individual.get()).map(String::valueOf).forEach(LOGGER::debug);
    }

    public static OntologyManager setUpSpinManager(OntologyManager m) {
        m.getOntologyConfigurator()
                .setGraphTransformers(GraphTransformers.getTransformers().addFirst(SpinTransform::new))
                .setPersonality(SpinModels.ONT_SPIN_PERSONALITY)
                .disableWebAccess();
        SpinModels.addMappings(m);
        SpinModels.addMappings(FileManager.get());
        return m;
    }

    @Test
    public void testSPSignature() throws Exception {
        // global transforms:
        GraphTransformers.getTransformers().add(g -> new Transform(g) {
            @Override
            public void perform() {
                LOGGER.info("Finish transformation (" + Graphs.getName(g) + ").");
            }
        });

        OWLOntologyManager manager = OntManagers.createOWL();
        OWLOntologyManager testManager = OntManagers.createOWL();

        OntGraphModel jenaSP = OntModelFactory.createModel(GraphTransformers.convert(load("etc/sp.ttl").getGraph()), OntModelConfig.ONT_PERSONALITY_LAX);
        OWLOntology owlSP = load(manager, "etc/sp.ttl");
        LOGGER.info("SP(Jena): ");
        ReadWriteUtils.print(jenaSP);
        //LOGGER.info("SP(OWL): ");
        //ReadWriteUtils.print(owlSP);
        signatureTest(owlSP, jenaSP);
        OWLOntology testSP = testManager.loadOntologyFromOntologyDocument(ReadWriteUtils.toInputStream(jenaSP, OntFormat.TURTLE));
        LOGGER.info("SP signature:");
        testSP.signature().forEach(entity -> LOGGER.debug(String.format("%s(%s)", entity, entity.getEntityType())));

        // WARNING:
        // There is a difference in behaviour between ONT-API and OWL-API,
        // Example: spin:violationDetail is ObjectProperty and spin:labelTemplate is DataProperty due to rdfs:range.
        // But OWL-API treats them as AnnotationProperty only.
        // spin:Modules is treated as NamedIndividual by OWL-API and as Class by ONT-API.
        UnionGraph spinGraph = new UnionGraph(load("etc/spin.ttl").getGraph());
        spinGraph.addGraph(jenaSP.getBaseGraph());
        OntGraphModel jenaSPIN = OntModelFactory.createModel(GraphTransformers.convert(spinGraph));
        OWLOntology owlSPIN = load(manager, "etc/spin.ttl");
        LOGGER.info("SPIN(Jena): ");
        ReadWriteUtils.print(jenaSPIN);
        LOGGER.info("SPIN(OWL): ");
        ReadWriteUtils.print(owlSPIN);

        //testSignature(owlSPIN, jenaSPIN);
        OWLOntology testSPIN = testManager.loadOntologyFromOntologyDocument(ReadWriteUtils.toInputStream(jenaSPIN, OntFormat.TURTLE));
        LOGGER.info("SPIN signature:");
        testSPIN.signature().forEach(entity -> LOGGER.debug(String.format("%s(%s)", entity, entity.getEntityType())));
        LOGGER.info("Origin SPIN signature:");
        owlSPIN.signature().forEach(e -> LOGGER.debug(String.format("%s(%s)", e, e.getEntityType())));

        UnionGraph splGraph = new UnionGraph(load("etc/spl.spin.ttl").getGraph());
        splGraph.addGraph(jenaSPIN.getBaseGraph());
        OntGraphModel jenaSPL = OntModelFactory.createModel(GraphTransformers.convert(splGraph));
        LOGGER.info("SPL-SPIN(Jena): ");
        ReadWriteUtils.print(jenaSPL);
        LOGGER.info("SPL-SPIN(Jena) All entities: ");
        jenaSPL.ontEntities().map(String::valueOf).forEach(LOGGER::debug);
    }

    @Test
    public void testSWRLVocabulary() throws Exception {
        IRI iri = IRI.create("http://www.w3.org/2003/11/swrl");
        IRI file = IRI.create(ReadWriteUtils.getResourceURI("swrl.owl.rdf"));
        OntologyManager m = OntManagers.createONT();
        m.getOntologyConfigurator().setPersonality(OntModelConfig.ONT_PERSONALITY_LAX);
        OntologyModel o = m.loadOntology(file);
        Assert.assertTrue("No ontology", m.contains(iri));

        ReadWriteUtils.print(o);
        o.axioms().map(String::valueOf).forEach(LOGGER::info);

        Assert.assertNull("rdfs:Literal should not be class", o.asGraphModel().getOntEntity(OntClass.class, RDFS.Literal));
        Assert.assertEquals("Should be DataAllValuesFrom", 1, o.asGraphModel().ontObjects(OntCE.DataAllValuesFrom.class).count());
        Assert.assertNotNull(SWRL.argument2 + " should be data property", o.asGraphModel().getOntEntity(OntNDP.class, SWRL.argument2));
        Assert.assertNotNull(SWRL.argument2 + " should be object property", o.asGraphModel().getOntEntity(OntNOP.class, SWRL.argument2));
    }

    private static void signatureTest(OWLOntology owl, OntGraphModel jena) {
        List<String> expectedClasses = owlToList(owl.classesInSignature(Imports.INCLUDED));
        List<String> actualClasses = jenaToList(jena.listClasses());
        Assert.assertTrue("Classes", actualClasses.containsAll(expectedClasses));

        List<String> expectedAnnotationProperties = owlToList(owl.annotationPropertiesInSignature(Imports.INCLUDED));//, RDFS.comment, RDFS.label, OWL2.deprecated, OWL.versionInfo);
        List<String> actualAnnotationProperties = jenaToList(jena.listAnnotationProperties());
        List<String> expectedDataProperties = owlToList(owl.dataPropertiesInSignature(Imports.INCLUDED));
        List<String> actualDataProperties = jenaToList(jena.listDataProperties());
        List<String> expectedObjectProperties = owlToList(owl.objectPropertiesInSignature(Imports.INCLUDED));
        List<String> actualObjectProperties = jenaToList(jena.listObjectProperties());
        LOGGER.debug("Actual AnnotationProperties: " + actualAnnotationProperties);
        LOGGER.debug("Actual ObjectProperties: " + actualObjectProperties);
        LOGGER.debug("Actual DataProperties: " + actualDataProperties);

        Assert.assertThat("AnnotationProperties", actualAnnotationProperties, IsEqual.equalTo(expectedAnnotationProperties));
        //Assert.assertThat("DataProperties", actualDataProperties, IsEqual.equalTo(expectedDataProperties));
        //Assert.assertThat("ObjectProperties", actualObjectProperties, IsEqual.equalTo(expectedObjectProperties));

        List<String> expectedDatatypes = owlToList(owl.datatypesInSignature(Imports.INCLUDED));
        List<String> actualDatatypes = jenaToList(jena.listDatatypes());
        Assert.assertThat("Datatypes", actualDatatypes, IsEqual.equalTo(expectedDatatypes));

        List<String> expectedIndividuals = owlToList(owl.individualsInSignature(Imports.INCLUDED));
        List<String> actualIndividuals = jenaToList(jena.listNamedIndividuals());
        Assert.assertThat("Individuals", actualIndividuals, IsEqual.equalTo(expectedIndividuals));
    }

    private static final Set<IRI> ADDITIONAL_BUILT_IN_ENTITIES = Stream.of(RDF.List, RDFS.Resource, RDF.Property, RDFS.Class, OWL.Ontology).map(r -> IRI.create(r.getURI())).collect(Collectors.toSet());

    private static boolean isNotBuiltIn(OWLEntity entity) {
        return !entity.isBuiltIn() && !ADDITIONAL_BUILT_IN_ENTITIES.contains(entity.getIRI());
    }

    private static Stream<String> owlToStream(Stream<? extends OWLEntity> entities) {
        return entities.filter(GraphTransformersTest::isNotBuiltIn).distinct().map(HasIRI::getIRI).map(IRI::getIRIString).sorted();
    }

    private static List<String> owlToList(Stream<? extends OWLEntity> entities) {
        return owlToStream(entities).collect(Collectors.toList());
    }

    private static Stream<String> jenaToStream(Stream<? extends OntEntity> entities) {
        return entities.map(Resource::getURI).sorted();
    }

    private static List<String> jenaToList(Stream<? extends OntEntity> entities) {
        return jenaToStream(entities).sorted().collect(Collectors.toList());
    }

    private static Model load(String file) throws Exception {
        Model m = ModelFactory.createDefaultModel();
        try (InputStream in = ReadWriteUtils.getResourceURI(file).toURL().openStream()) {
            m.read(in, null, "ttl");
        }
        return m;
    }

    private static OWLOntology load(OWLOntologyManager manager, String file) throws Exception {
        return manager.loadOntology(OntIRI.create(ReadWriteUtils.getResourceURI(file)));
    }
}
