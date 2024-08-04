/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2023, owl.cs group.
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

import com.github.owlcs.ontapi.OntManagers;
import com.github.owlcs.ontapi.Ontology;
import com.github.owlcs.ontapi.OntologyManager;
import com.github.owlcs.ontapi.testutils.OntIRI;
import org.apache.jena.ontapi.model.OntClass;
import org.apache.jena.ontapi.model.OntIndividual;
import org.apache.jena.ontapi.model.OntModel;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.RemoveAxiom;

/**
 * test individuals using jena-graph and owl-api
 * <p>
 * Created by @ssz on 08.10.2016.
 */
public class IndividualsOntModelTest extends OntModelTestBase {

    @Test
    public void test() {
        OntIRI iri = OntIRI.create("http://test.test/add-class-individual");
        OntologyManager manager = OntManagers.createManager();
        OWLDataFactory factory = manager.getOWLDataFactory();

        Ontology owl = manager.createOntology(iri.toOwlOntologyID());
        OntModel jena = owl.asGraphModel();

        OntIRI class1 = iri.addFragment("ClassN1");
        OntIRI class2 = iri.addFragment("ClassN2");
        OntIRI individual1 = iri.addFragment("TestIndividualN1");
        OntIRI individual2 = iri.addFragment("TestIndividualN2");
        OntIRI individual3 = iri.addFragment("TestIndividualN3");
        int classesCount = 2;
        int individualsCount = 3;

        LOGGER.debug("Add classes.");
        manager.applyChange(new AddAxiom(owl, factory.getOWLDeclarationAxiom(factory.getOWLClass(class1))));
        jena.add(class2.toResource(), RDF.type, OWL.Class);

        LOGGER.debug("Add individuals.");
        LOGGER.debug("Add individuals using OWL");
        manager.applyChange(new AddAxiom(owl, factory.getOWLClassAssertionAxiom(factory.getOWLClass(class1), factory.getOWLNamedIndividual(individual1))));
        LOGGER.debug("Add individuals using ONT");
        jena.add(individual2.toResource(), RDF.type, class1.toResource())
                .add(individual2.toResource(), RDF.type, OWL.NamedIndividual)
                .getOntClass(class2.getIRIString()).createIndividual(individual3.getIRIString());

        debug(owl);

        Assertions.assertEquals(classesCount + individualsCount, owl.axioms(AxiomType.DECLARATION).count());
        Assertions.assertEquals(classesCount, jena.ontEntities(OntClass.Named.class).count());
        Assertions.assertEquals(individualsCount, owl.axioms(AxiomType.CLASS_ASSERTION).count());
        Assertions.assertEquals(individualsCount, jena.ontObjects(OntIndividual.class).count());

        LOGGER.debug("Remove individuals");
        // remove class assertion and declaration:
        jena.removeAll(individual3.toResource(), null, null);
        // remove class-assertion:
        manager.applyChange(new RemoveAxiom(owl, factory.getOWLClassAssertionAxiom(factory.getOWLClass(class1), factory.getOWLNamedIndividual(individual1))));
        // remove declaration:
        owl.remove(factory.getOWLDeclarationAxiom(factory.getOWLNamedIndividual(individual1)));
        individualsCount = 1;

        debug(owl);

        Assertions.assertEquals(individualsCount, owl.axioms(AxiomType.CLASS_ASSERTION).count());
        Assertions.assertEquals(individualsCount, jena.ontObjects(OntIndividual.class).count());
    }

}
