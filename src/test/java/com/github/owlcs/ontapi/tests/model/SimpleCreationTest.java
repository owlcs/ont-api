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

package com.github.owlcs.ontapi.tests.model;

import com.github.owlcs.ontapi.OntFormat;
import com.github.owlcs.ontapi.OntManagers;
import com.github.owlcs.ontapi.Ontology;
import com.github.owlcs.ontapi.OntologyManager;
import com.github.owlcs.ontapi.jena.OntModelFactory;
import com.github.owlcs.ontapi.utils.OntIRI;
import com.github.owlcs.ontapi.utils.ReadWriteUtils;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;

/**
 * Just simple example test.
 *
 * Created by @szuev on 27.09.2016.
 */
public class SimpleCreationTest {
    private static final Logger LOGGER = Logger.getLogger(SimpleCreationTest.class);

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
    public void testAssemblyOntology() {
        OntIRI owlURI = OntIRI.create("http://test.test/example");
        int statements = 15;
        OntologyManager m = OntManagers.createManager();
        OWLDataFactory df = m.getOWLDataFactory();

        Ontology ontology = m.createOntology(owlURI.toOwlOntologyID());
        m.applyChange(new AddImport(ontology,
                df.getOWLImportsDeclaration(IRI.create(ReadWriteUtils.getResourceURI("etc/sp.ttl")))));
        //manager.applyChange(new AddImport(ontology, factory.getOWLImportsDeclaration(IRI.create(SPINMAP_SPIN.BASE_URI))));
        m.applyChange(new AddOntologyAnnotation(ontology,
                df.getOWLAnnotation(df.getRDFSComment(), df.getOWLLiteral("test-comment"))));
        m.applyChange(new AddOntologyAnnotation(ontology,
                df.getOWLAnnotation(df.getOWLVersionInfo(), df.getOWLLiteral("test-version-info"))));

        OWLClass owlClass = df.getOWLClass(owlURI.addFragment("SomeClass"));
        m.applyChange(new AddAxiom(ontology, df.getOWLDeclarationAxiom(owlClass)));
        m.applyChange(new AddAxiom(ontology, df.getOWLSubClassOfAxiom(owlClass, df.getOWLThing())));

        OWLAnnotation classLabel = df.getOWLAnnotation(df.getRDFSLabel(), df.getOWLLiteral("some-class-label"));
        m.applyChange(new AddAxiom(ontology, df.getOWLAnnotationAssertionAxiom(owlClass.getIRI(), classLabel)));

        OWLDataProperty dp = df.getOWLDataProperty(owlURI.addFragment("someDataProperty"));
        m.applyChange(new AddAxiom(ontology, df.getOWLDeclarationAxiom(dp)));
        m.applyChange(new AddAxiom(ontology, df.getOWLDataPropertyDomainAxiom(dp, owlClass)));
        m.applyChange(new AddAxiom(ontology, df.getOWLDataPropertyRangeAxiom(dp, df.getStringOWLDatatype())));
        OWLAnnotation propertyLabel = df.getOWLAnnotation(df.getRDFSLabel(), df.getOWLLiteral("some-property-label"));
        m.applyChange(new AddAxiom(ontology, df.getOWLAnnotationAssertionAxiom(dp.getIRI(), propertyLabel)));
        OWLAnnotation propertyComment = df.getOWLAnnotation(df.getRDFSComment(), df.getOWLLiteral("some property comment"));
        m.applyChange(new AddAxiom(ontology, df.getOWLAnnotationAssertionAxiom(dp.getIRI(), propertyComment)));

        OWLIndividual individual = df.getOWLNamedIndividual(owlURI.addFragment("the-individual"));
        m.applyChange(new AddAxiom(ontology, df.getOWLClassAssertionAxiom(owlClass, individual)));
        m.applyChange(new AddAxiom(ontology,
                df.getOWLDataPropertyAssertionAxiom(dp, individual, df.getOWLLiteral("TheName"))));

        ReadWriteUtils.print(ontology, OntFormat.TURTLE);

        ontology.axioms().forEach(LOGGER::debug);

        ReadWriteUtils.print(ontology.asGraphModel(), OntFormat.TURTLE);
        LOGGER.debug("All statements: " + ontology.asGraphModel().listStatements().toList().size());
        Assert.assertEquals("incorrect statements size", statements,
                ontology.asGraphModel().getBaseModel().listStatements().toList().size());
    }
}
