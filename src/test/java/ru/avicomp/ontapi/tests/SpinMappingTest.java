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

import org.apache.jena.rdf.model.*;
import org.apache.jena.util.FileManager;
import org.apache.jena.vocabulary.RDF;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;
import org.topbraid.spin.inference.SPINInferences;
import org.topbraid.spin.system.SPINModuleRegistry;
import org.topbraid.spin.vocabulary.SPIN;
import org.topbraid.spin.vocabulary.SPINMAP;
import ru.avicomp.ontapi.OntManagers;
import ru.avicomp.ontapi.OntologyManager;
import ru.avicomp.ontapi.OntologyModel;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.vocabulary.XSD;
import ru.avicomp.ontapi.utils.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * To test SPIN inferences under ONT-API.
 * <p>
 * Created by @szuev on 25.02.2017.
 */
@SuppressWarnings("WeakerAccess")
public class SpinMappingTest {

    protected static final Logger LOGGER = Logger.getLogger(SpinMappingTest.class);

    private static final String DATA_SEPARATOR = ", ";

    protected OntologyManager manager;

    @Before
    public void before() {
        LOGGER.info("Set up manager.");
        manager = OntManagers.createONT();
        setUpManager(manager);
    }

    public void setUpManager(OntologyManager manager) {
        // do not convert spin rdfs-ontologies:
        manager.setOntologyLoaderConfiguration(manager.getOntologyLoaderConfiguration().setPerformTransformation(false));
        SpinModels.addMappings(manager);
        // this is needed for SPINInferences:
        SpinModels.addMappings(FileManager.get());
    }

    public void loadSpinModels() throws OWLOntologyCreationException {
        LOGGER.info("Load spin models to manager.");
        manager.loadOntology(SpinModels.SPINMAPL.getIRI()).asGraphModel();
        List<IRI> actual = manager.ontologies().map(HasOntologyID::getOntologyID).map(OWLOntologyID::getOntologyIRI).
                filter(Optional::isPresent).map(Optional::get).sorted().collect(Collectors.toList());
        List<IRI> expected = Stream.of(SpinModels.values()).map(SpinModels::getIRI).sorted().collect(Collectors.toList());
        Assert.assertEquals("Incorrect collection of ontologies", expected, actual);
    }

    @Test
    public void main() throws Exception {
        loadSpinModels();

        OntGraphModel source = createSourceModel();
        OntGraphModel target = createTargetModel();
        OntGraphModel mapping = composeMapping(source, target);
        Assert.assertEquals("Incorrect ontologies count", SpinModels.values().length + 3, manager.ontologies().count());

        ReadWriteUtils.print(mapping);
        runInferences(manager.getOntology(IRI.create(mapping.getID().getURI())), target);
        ReadWriteUtils.print(target);

        validate(source, target);
    }

    public void validate(OntGraphModel source, OntGraphModel target) {
        LOGGER.info("Validate.");
        OntClass targetClass = target.listClasses().findFirst().orElse(null);
        OntNDP targetProperty = target.listDataProperties().findFirst().orElse(null);
        List<OntIndividual> sourceIndividuals = source.listNamedIndividuals().collect(Collectors.toList());
        List<Resource> targetIndividuals = target.listSubjectsWithProperty(RDF.type, targetClass).toList();
        LOGGER.debug("Individuals count: " + targetIndividuals.size());
        Assert.assertEquals("Incorrect count of individuals", sourceIndividuals.size(), targetIndividuals.size());
        sourceIndividuals.forEach(named -> {
            Resource i = target.getResource(named.getURI());
            Assert.assertTrue("Can't find individual " + i, target.contains(i, RDF.type, targetClass));
            List<RDFNode> objects = target.listObjectsOfProperty(i, targetProperty).toList();
            Assert.assertEquals("Incorrect data for " + i, 1, objects.size());
            Literal res = objects.get(0).asLiteral();
            Assert.assertTrue("Incorrect literal value for " + i, res.getString().contains(DATA_SEPARATOR));
        });
    }

    /**
     * Creates a simple mapping model for the specified simple source and target ontologies with particular structure.
     * The <a href='http://topbraid.org/spin/spinmapl#self'>spinmapl:self</a> is used as target function.
     * To make new DataProperty Assertion there is <a href='http://topbraid.org/spin/spinmapl#concatWithSeparator'>spinmapl:concatWithSeparator</a>
     *
     * @param source {@link OntGraphModel} the model which contains one class, two datatype properties, several individuals and DataProperty Sssertions for them.
     * @param target {@link OntGraphModel} the model which contains one class and one datatype property
     * @return {@link OntGraphModel} mapping model.
     */
    public OntGraphModel composeMapping(OntGraphModel source, OntGraphModel target) {
        LOGGER.info("Compose mapping.");
        OntClass sourceClass = source.listClasses().findFirst().orElseThrow(AssertionError::new);
        OntClass targetClass = target.listClasses().findFirst().orElseThrow(AssertionError::new);
        List<OntNDP> sourceProperties = source.listDataProperties().collect(Collectors.toList());
        OntNDP targetProperty = target.listDataProperties().findFirst().orElse(null);

        OntGraphModel mapping = manager.createGraphModel("http://spin.avicomp.ru");
        OntGraphModel spinmapl = manager.getGraphModel(SpinModels.SPINMAPL.getIRI().getIRIString());

        mapping.addImport(spinmapl).addImport(source).addImport(target);

        TestUtils.setDefaultPrefixes(mapping);
        Stream.of(SpinModels.SP, SpinModels.SPIN, SpinModels.SPINMAP, SpinModels.SPINMAPL)
                .forEach(m -> mapping.setNsPrefix(m.name().toLowerCase(), m.getIRI() + "#"));

        String contextNS = String.format("%s#%s-%s", mapping.getID().getURI(), sourceClass.getLocalName(), targetClass.getLocalName());
        Resource context = mapping.createResource(contextNS, SPINMAP.Context);

        context.addProperty(SPINMAP.sourceClass, sourceClass);
        context.addProperty(SPINMAP.targetClass, targetClass);
        Resource anon = mapping.createResource(null, SPINMAPL.self);
        anon.addProperty(SPINMAP.source, SPINMAP.sourceVariable);
        context.addProperty(SPINMAP.target, anon);
        Resource rule1 = mapping.createResource(null, SPINMAP.Mapping_0_1);
        mapping.getResource(sourceClass.getURI()).addProperty(SPINMAP.rule, rule1);
        rule1.addProperty(SPINMAP.context, context);
        rule1.addProperty(SPINMAP.expression, targetClass);
        rule1.addProperty(SPINMAP.targetPredicate1, RDF.type);
        Resource rule2 = mapping.createResource(null, SPINMAP.Mapping_2_1);
        mapping.getResource(sourceClass.getURI()).addProperty(SPINMAP.rule, rule2);
        rule2.addProperty(SPINMAP.context, context);
        rule2.addProperty(SPINMAP.sourcePredicate1, sourceProperties.get(0));
        rule2.addProperty(SPINMAP.sourcePredicate2, sourceProperties.get(1));
        rule2.addProperty(SPINMAP.targetPredicate1, targetProperty);
        Resource expression = mapping.createResource(null, SPINMAPL.concatWithSeparator);
        rule2.addProperty(SPINMAP.expression, expression);
        expression.addProperty(SP.arg1, SPIN._arg1);
        expression.addProperty(SP.arg2, SPIN._arg2);
        expression.addLiteral(SPINMAPL.separator, DATA_SEPARATOR);
        return mapping;
    }

    /**
     * result model must contain one owl:Class, two owl:DatatypeProperty
     * and several individuals (owl:NamedIndividual) with DataProperty assertions.
     *
     * @return {@link OntGraphModel} the model.
     */
    public OntGraphModel createSourceModel() {
        LOGGER.info("Create the source model.");
        String uri = "http://source.avicomp.ru";
        String ns = uri + "#";
        OntGraphModel res = manager.createGraphModel(uri);
        TestUtils.setDefaultPrefixes(res);
        OntClass clazz = res.createOntEntity(OntClass.class, ns + "ClassSource");
        OntNDP prop1 = res.createOntEntity(OntNDP.class, ns + "prop1");
        OntNDP prop2 = res.createOntEntity(OntNDP.class, ns + "prop2");
        prop1.addRange(res.getOntEntity(OntDT.class, XSD.xstring));
        prop2.addRange(res.getOntEntity(OntDT.class, XSD.integer));
        prop1.addDomain(clazz);
        prop2.addDomain(clazz);
        OntIndividual i1 = clazz.createIndividual(ns + "Inst1");
        OntIndividual i2 = clazz.createIndividual(ns + "Inst2");
        i1.addLiteral(prop1, "val1");
        i1.addLiteral(prop2, Integer.valueOf(2));
        i2.addLiteral(prop1, "val2");
        i2.addLiteral(prop2, Integer.valueOf(99090));
        OntologyModel o = manager.getOntology(IRI.create(uri));
        Assert.assertNotNull("Can't find ontology " + uri, o);
        o.axioms().forEach(LOGGER::debug);
        Assert.assertEquals("Incorrect number of data-property assertions", 4, o.axioms(AxiomType.DATA_PROPERTY_ASSERTION).count());
        ReadWriteUtils.print(res);
        return res;
    }

    /**
     * result model must contain one owl:Class and one owl:DatatypeProperty.
     *
     * @return {@link OntGraphModel} the model.
     */
    public OntGraphModel createTargetModel() {
        LOGGER.info("Create the target model.");
        String uri = "http://target.avicomp.ru";
        String ns = uri + "#";
        OntGraphModel res = manager.createGraphModel(uri);
        TestUtils.setDefaultPrefixes(res);
        OntClass clazz = res.createOntEntity(OntClass.class, ns + "ClassTarget");
        OntNDP prop = res.createOntEntity(OntNDP.class, ns + "targetProperty");
        prop.addRange(res.getOntEntity(OntDT.class, XSD.xstring));
        prop.addDomain(clazz);
        OntologyModel o = manager.getOntology(IRI.create(uri));
        Assert.assertNotNull("Can't find ontology " + uri, o);
        o.axioms().forEach(LOGGER::debug);
        ReadWriteUtils.print(res);
        return res;
    }

    public void runInferences(OntologyModel mapping, Model target) {
        // recreate model since there is spin specific personalities inside org.topbraid.spin.vocabulary.SP#init
        Model source = ModelFactory.createModelForGraph(mapping.asGraphModel().getGraph());
        LOGGER.info("Run Inferences");
        SPINModuleRegistry.get().init();
        SPINModuleRegistry.get().registerAll(source, null);
        SPINInferences.run(source, target, null, null, false, null);
    }

}
