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

package ru.avicomp.ontapi.tests.managers;

import org.apache.jena.rdf.model.Resource;
import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avicomp.ontapi.OntManagers;
import ru.avicomp.ontapi.OntologyManager;
import ru.avicomp.ontapi.OntologyModel;
import ru.avicomp.ontapi.config.OntLoaderConfiguration;
import ru.avicomp.ontapi.jena.impl.conf.OntModelConfig;
import ru.avicomp.ontapi.jena.model.OntEntity;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.utils.ReadWriteUtils;
import ru.avicomp.ontapi.utils.TestUtils;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * for testing pizza, foaf and googrelations ontologies.
 * <p>
 * Created by @szuev on 30.09.2016.
 */
public class SimpleLoadTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleLoadTest.class);

    @Test
    public void testPizza() throws Exception {
        test("pizza.ttl");
    }

    @Test
    public void testFoaf() throws Exception {
        String fileName = "foaf.rdf";
        OntologyManager manager = OntManagers.createONT();
        OntLoaderConfiguration conf = manager.getOntologyLoaderConfiguration().setPersonality(OntModelConfig.ONT_PERSONALITY_STRICT);
        manager.setOntologyLoaderConfiguration(conf);

        IRI fileIRI = IRI.create(ReadWriteUtils.getResourceURI(fileName));
        LOGGER.debug("The source document file {}", fileIRI);
        OntologyModel ont = manager.loadOntologyFromOntologyDocument(fileIRI);
        OntGraphModel model = ont.asGraphModel();
        ReadWriteUtils.print(model);

        Set<Resource> illegalPunningURIs = TestUtils.getIllegalPunnings(model, OntModelConfig.StdMode.STRICT);
        LOGGER.debug("There are following illegal punnins inside original graph: " + illegalPunningURIs);
        List<OntEntity> illegalPunnings = model.ontEntities().filter(illegalPunningURIs::contains).collect(Collectors.toList());
        Assert.assertTrue("Has illegal punnings: " + illegalPunnings, illegalPunnings.isEmpty());

        List<OWLAxiom> ontList = ont.axioms().sorted().collect(Collectors.toList());

        OWLOntology owl = OntManagers.createOWL().loadOntologyFromOntologyDocument(fileIRI);
        Set<OWLAxiom> punningAxioms = illegalPunningURIs.stream()
                .map(Resource::getURI).map(IRI::create)
                .map(owl::referencingAxioms).flatMap(Function.identity()).collect(Collectors.toSet());
        LOGGER.debug("OWL Axioms to exclude from consideration (" + punningAxioms.size() + "): ");
        punningAxioms.stream().map(String::valueOf).forEach(LOGGER::debug);
        List<OWLAxiom> owlList = owl.axioms().filter(axiom -> !punningAxioms.contains(axiom)).sorted().collect(Collectors.toList());
        // by some mysterious reason OWL-API skips owl:equivalentProperty although it seems a good axiom.
        test(owlList, ontList, Stream.of(AxiomType.DECLARATION, AxiomType.ANNOTATION_ASSERTION, AxiomType.EQUIVALENT_OBJECT_PROPERTIES).collect(Collectors.toSet()));

    }

    @Test
    public void testGoodrelations() throws Exception {
        String fileName = "goodrelations.rdf";
        OWLDataFactory factory = OntManagers.getDataFactory();

        IRI fileIRI = IRI.create(ReadWriteUtils.getResourceURI(fileName));
        LOGGER.debug("The source document file {}", fileIRI);

        OntologyModel ont = OntManagers.createONT().loadOntologyFromOntologyDocument(fileIRI);
        OWLOntology owl = OntManagers.createOWL().loadOntologyFromOntologyDocument(fileIRI);

        List<OWLAxiom> owlList = TestUtils.splitAxioms(owl).sorted().collect(Collectors.toList());
        List<OWLAxiom> ontList = TestUtils.splitAxioms(ont).sorted().collect(Collectors.toList());

        ReadWriteUtils.print(ont.asGraphModel());

        Set<AxiomType> excluded = Stream.of(AxiomType.DECLARATION, AxiomType.CLASS_ASSERTION, AxiomType.DATA_PROPERTY_ASSERTION)
                .collect(Collectors.toSet());

        test(owlList, ontList, excluded);

        LOGGER.debug("Test separately skipped axioms:");
        LOGGER.debug("Test type <{}>", AxiomType.DECLARATION);
        List<OWLAxiom> expectedDeclarations = Stream.of(
                owlList.stream()
                        .filter(a -> AxiomType.CLASS_ASSERTION.equals(a.getAxiomType()))
                        .map(OWLClassAssertionAxiom.class::cast)
                        .map(OWLClassAssertionAxiom::getIndividual)
                        .filter(OWLIndividual::isNamed)
                        .map(OWLIndividual::asOWLNamedIndividual)
                        .map(factory::getOWLDeclarationAxiom),
                owlList.stream()
                        .filter(a -> AxiomType.DECLARATION.equals(a.getAxiomType()))).flatMap(Function.identity())
                .sorted().distinct().collect(Collectors.toList());
        List<OWLAxiom> actualDeclarations = ontList.stream()
                .filter(a -> AxiomType.DECLARATION.equals(a.getAxiomType())).collect(Collectors.toList());
        Assert.assertThat("Incorrect declaration axioms (actual=" + actualDeclarations.size() + ", expected=" +
                        expectedDeclarations.size() + ")",
                actualDeclarations, IsEqual.equalTo(expectedDeclarations));

        LOGGER.debug("Test type <{}>", AxiomType.CLASS_ASSERTION);
        List<OWLAxiom> expectedClassAssertions = Stream.of(
                owlList.stream()
                        .filter(a -> AxiomType.CLASS_ASSERTION.equals(a.getAxiomType()))
                        .map(OWLClassAssertionAxiom.class::cast).filter(a -> a.getIndividual().isNamed()),
                ontList.stream().filter(a -> AxiomType.CLASS_ASSERTION.equals(a.getAxiomType()))
                        .map(OWLClassAssertionAxiom.class::cast)
                        .filter(a -> a.getIndividual().isAnonymous())).flatMap(Function.identity())
                .sorted().distinct().collect(Collectors.toList());
        List<OWLAxiom> actualClassAssertions = ontList.stream()
                .filter(a -> AxiomType.CLASS_ASSERTION.equals(a.getAxiomType())).collect(Collectors.toList());
        Assert.assertThat("Incorrect class-assertions axioms (actual=" + actualClassAssertions.size() + ", expected=" +
                        expectedClassAssertions.size() + ")",
                actualClassAssertions, IsEqual.equalTo(expectedClassAssertions));

        LOGGER.debug("Test type <{}>", AxiomType.DATA_PROPERTY_ASSERTION);
        List<OWLAxiom> expectedDataPropertyAssertions = Stream.of(
                owlList.stream()
                        .filter(a -> AxiomType.DATA_PROPERTY_ASSERTION.equals(a.getAxiomType()))
                        .map(OWLDataPropertyAssertionAxiom.class::cast).filter(a -> a.getSubject().isNamed()),
                ontList.stream().filter(a -> AxiomType.DATA_PROPERTY_ASSERTION.equals(a.getAxiomType()))
                        .map(OWLDataPropertyAssertionAxiom.class::cast)
                        .filter(a -> a.getSubject().isAnonymous())).flatMap(Function.identity())
                .sorted().distinct().collect(Collectors.toList());
        List<OWLAxiom> actualDataPropertyAssertions = ontList.stream()
                .filter(a -> AxiomType.DATA_PROPERTY_ASSERTION.equals(a.getAxiomType())).collect(Collectors.toList());
        Assert.assertThat("Incorrect data-property-assertions axioms (actual=" + actualDataPropertyAssertions.size() +
                        ", expected=" + expectedDataPropertyAssertions.size() + ")",
                actualDataPropertyAssertions, IsEqual.equalTo(expectedDataPropertyAssertions));

    }

    @SuppressWarnings("SameParameterValue")
    private void test(String fileName, AxiomType... toExclude) throws Exception {
        IRI fileIRI = IRI.create(ReadWriteUtils.getResourceURI(fileName));
        LOGGER.debug("The source document file {}", fileIRI);

        OntologyModel ont = OntManagers.createONT().loadOntologyFromOntologyDocument(fileIRI);
        OWLOntology owl = OntManagers.createOWL().loadOntologyFromOntologyDocument(fileIRI);

        List<OWLAxiom> owlList = TestUtils.splitAxioms(owl).sorted().collect(Collectors.toList());
        List<OWLAxiom> ontList = TestUtils.splitAxioms(ont).sorted().collect(Collectors.toList());

        ReadWriteUtils.print(ont.asGraphModel());

        Set<AxiomType> excluded = Stream.of(toExclude).collect(Collectors.toSet());

        test(owlList, ontList, excluded);
    }

    private void test(List<OWLAxiom> owlList, List<OWLAxiom> ontList, Set<AxiomType> excluded) {
        AxiomType.AXIOM_TYPES.forEach(type -> {
            if (excluded.contains(type)) {
                LOGGER.warn("Skip <" + type + ">");
                return;
            }
            List<OWLAxiom> actual = ontList.stream()
                    .filter(axiom -> type.equals(axiom.getAxiomType()))
                    .collect(Collectors.toList());
            List<OWLAxiom> expected = owlList.stream().filter(axiom -> type.equals(axiom.getAxiomType())).collect(Collectors.toList());
            LOGGER.debug("Test type <{}> ::: {}", type, expected.size());
            Assert.assertThat("Incorrect axioms for type <" + type + "> (actual(ont)=" + actual.size() + ", expected(owl)=" + expected.size() + ")", actual, IsEqual.equalTo(expected));
        });
    }

}
