/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2017, Avicomp Services, AO
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

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.OntFormat;
import ru.avicomp.ontapi.OntManagers;
import ru.avicomp.ontapi.OntologyManager;
import ru.avicomp.ontapi.OntologyModel;
import ru.avicomp.ontapi.jena.OntModelFactory;
import ru.avicomp.ontapi.utils.OntIRI;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

/**
 * Just simple example test.
 *
 * Created by @szuev on 27.09.2016.
 */
public class ExampleTest {
    private static final Logger LOGGER = Logger.getLogger(ExampleTest.class);

    @BeforeClass
    public static void before() {
        LOGGER.debug("Before -- START");
        OntModelFactory.init();
        LOGGER.debug("Before -- END");
    }

    @After
    public void after() {
        LOGGER.debug("After");
    }

    @Test
    public void test() throws OWLOntologyCreationException {
        OntIRI owlURI = OntIRI.create("http://test.test/example");
        int statementsNumber = 15;
        OntologyManager manager = OntManagers.createONT();
        OWLDataFactory factory = manager.getOWLDataFactory();

        OntologyModel ontology = manager.createOntology(owlURI.toOwlOntologyID());
        manager.applyChange(new AddImport(ontology, factory.getOWLImportsDeclaration(IRI.create(ReadWriteUtils.getResourceURI("spin/sp.ttl")))));
        //manager.applyChange(new AddImport(ontology, factory.getOWLImportsDeclaration(IRI.create(SPINMAP_SPIN.BASE_URI))));
        manager.applyChange(new AddOntologyAnnotation(ontology, factory.getOWLAnnotation(factory.getRDFSComment(), factory.getOWLLiteral("test-comment"))));
        manager.applyChange(new AddOntologyAnnotation(ontology, factory.getOWLAnnotation(factory.getOWLVersionInfo(), factory.getOWLLiteral("test-version-info"))));

        OWLClass owlClass = factory.getOWLClass(owlURI.addFragment("SomeClass"));
        manager.applyChange(new AddAxiom(ontology, factory.getOWLDeclarationAxiom(owlClass)));
        manager.applyChange(new AddAxiom(ontology, factory.getOWLSubClassOfAxiom(owlClass, factory.getOWLThing())));

        OWLAnnotation classAnnotationLabel = factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral("some-class-label"));
        manager.applyChange(new AddAxiom(ontology, factory.getOWLAnnotationAssertionAxiom(owlClass.getIRI(), classAnnotationLabel)));

        OWLDataProperty owlProperty = factory.getOWLDataProperty(owlURI.addFragment("someDataProperty"));
        manager.applyChange(new AddAxiom(ontology, factory.getOWLDeclarationAxiom(owlProperty)));
        manager.applyChange(new AddAxiom(ontology, factory.getOWLDataPropertyDomainAxiom(owlProperty, owlClass)));
        manager.applyChange(new AddAxiom(ontology, factory.getOWLDataPropertyRangeAxiom(owlProperty, factory.getStringOWLDatatype())));
        OWLAnnotation propertyAnnotationLabel = factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral("some-property-label"));
        manager.applyChange(new AddAxiom(ontology, factory.getOWLAnnotationAssertionAxiom(owlProperty.getIRI(), propertyAnnotationLabel)));
        OWLAnnotation propertyAnnotationComment = factory.getOWLAnnotation(factory.getRDFSComment(), factory.getOWLLiteral("some property comment"));
        manager.applyChange(new AddAxiom(ontology, factory.getOWLAnnotationAssertionAxiom(owlProperty.getIRI(), propertyAnnotationComment)));

        OWLIndividual individual = factory.getOWLNamedIndividual(owlURI.addFragment("the-individual"));
        manager.applyChange(new AddAxiom(ontology, factory.getOWLClassAssertionAxiom(owlClass, individual)));
        manager.applyChange(new AddAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(owlProperty, individual, factory.getOWLLiteral("TheName"))));

        //System.out.println(ontology.directImports().collect(Collectors.toList()));
        ReadWriteUtils.print(ontology, OntFormat.TURTLE);

        ontology.axioms().forEach(LOGGER::debug);

        ReadWriteUtils.print(ontology.asGraphModel(), OntFormat.TURTLE);
        LOGGER.debug("All statements: " + ontology.asGraphModel().listStatements().toList().size());
        Assert.assertEquals("incorrect statements size", statementsNumber, ontology.asGraphModel().getBaseModel().listStatements().toList().size());
    }
}
