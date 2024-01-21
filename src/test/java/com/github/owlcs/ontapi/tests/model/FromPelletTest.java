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
import com.github.owlcs.ontapi.testutils.OWLIOUtils;
import com.github.sszuev.jena.ontapi.model.OntObjectProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The ontologies or/and test scenarios (but in some general terms only) were taken from
 * <a href="https://github.com/Galigator/openllet">openllet</a> (it is an alive fork of Pellet).
 * <p>
 * Created by @ssz on 19.04.2017.
 */
public class FromPelletTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(FromPelletTest.class);

    @Test
    public void testAddRemoveAxioms() throws Exception {
        OWLOntologyManager m = OntManagers.createConcurrentManager();

        OWLDataFactory df = m.getOWLDataFactory();

        IRI iri = IRI.create("http://www.example.org/xxx");

        OWLNamedIndividual a = df.getOWLNamedIndividual("http://www.example.org/test#a");
        OWLDataProperty dr = df.getOWLDataProperty(IRI.create("http://www.example.org/test#dr"));
        OWLDataProperty dp = df.getOWLDataProperty(IRI.create("http://www.example.org/test#dp"));
        OWLDataProperty dq = df.getOWLDataProperty(IRI.create("http://www.example.org/test#dq"));

        OWLDatatype integer = OWL2Datatype.XSD_INTEGER.getDatatype(df);
        OWLLiteral l = df.getOWLLiteral("1", integer);
        OWLClassExpression c = df.getOWLDataSomeValuesFrom(dr, integer);

        OWLAxiom FunctionalDataProperty = df.getOWLFunctionalDataPropertyAxiom(dp);
        OWLAxiom SubDataPropertyOf_dq = df.getOWLSubDataPropertyOfAxiom(dq, dp);
        OWLAxiom SubDataPropertyOf_dr = df.getOWLSubDataPropertyOfAxiom(dr, dp);
        OWLAxiom DataPropertyAssertion = df.getOWLDataPropertyAssertionAxiom(dq, a, l);
        OWLAxiom ClassAssertion = df.getOWLClassAssertionAxiom(c, a);
        OWLAxiom Declaration_a = df.getOWLDeclarationAxiom(a);
        OWLAxiom Declaration_dq = df.getOWLDeclarationAxiom(dq);
        OWLAxiom Declaration_dp = df.getOWLDeclarationAxiom(dp);
        OWLAxiom Declaration_dr = df.getOWLDeclarationAxiom(dr);
        OWLAxiom Declaration_integer = df.getOWLDeclarationAxiom(integer);

        OWLOntology o = m.createOntology(iri);

        o.add(FunctionalDataProperty);
        o.add(SubDataPropertyOf_dq);
        o.add(SubDataPropertyOf_dr);
        o.add(DataPropertyAssertion);
        o.add(ClassAssertion);

        o.remove(DataPropertyAssertion);

        o.add(Declaration_a);
        o.add(Declaration_dq);
        o.add(Declaration_integer);

        o.remove(Declaration_a);
        o.remove(Declaration_dq);

        o.remove(Declaration_integer);
        o.add(DataPropertyAssertion);
        o.remove(SubDataPropertyOf_dq);
        o.add(Declaration_dq);
        o.add(Declaration_dp);

        o.remove(Declaration_dq);

        o.remove(Declaration_dp);
        o.add(SubDataPropertyOf_dq);

        o.remove(DataPropertyAssertion);

        o.add(Declaration_a);
        o.add(Declaration_dq);
        o.add(Declaration_integer);

        o.remove(Declaration_a);
        o.remove(Declaration_dq);
        o.remove(Declaration_integer);

        o.add(DataPropertyAssertion);

        o.remove(SubDataPropertyOf_dr);
        o.add(Declaration_dp);
        o.add(Declaration_dr);


        o.remove(Declaration_dp);

        o.remove(Declaration_dr);

        o.add(SubDataPropertyOf_dr);
        o.remove(SubDataPropertyOf_dq);
        o.add(Declaration_dq);
        o.add(Declaration_dp);

        o.remove(Declaration_dq);
        o.remove(Declaration_dp);
        o.add(SubDataPropertyOf_dq);
        o.remove(FunctionalDataProperty);
        o.add(Declaration_dp);

        o.remove(Declaration_dp);

        o.add(FunctionalDataProperty);

        o.remove(ClassAssertion);

        o.add(Declaration_a);

        o.add(Declaration_integer);
        o.add(Declaration_dr);

        o.remove(Declaration_a);

        o.remove(Declaration_integer);
        o.remove(Declaration_dr);
        o.add(ClassAssertion);

        OWLIOUtils.print(o);
        o.axioms().map(String::valueOf).forEach(LOGGER::debug);
    }

    @Test
    public void testPropertyChain() throws Exception {
        IRI iri = IRI.create(OWLIOUtils.getResourceURI("/ontapi/propertyChain.owl"));
        LOGGER.debug("{}", iri);
        OntologyManager m = OntManagers.createManager();
        Ontology o = m.loadOntology(iri);
        OWLIOUtils.print(o);
        o.axioms().map(String::valueOf).forEach(LOGGER::debug);
        Assertions.assertEquals(4, o.axioms(AxiomType.SUB_PROPERTY_CHAIN_OF).count());
        OntObjectProperty p = o.asGraphModel().getObjectProperty("http://www.example.org/test#s");
        Assertions.assertEquals(3, p.propertyChains().count());
    }

    @Test
    public void testSWRLOntology() throws Exception {
        IRI iri = IRI.create(OWLIOUtils.getResourceURI("/ontapi/anyURI-premise.rdf"));
        LOGGER.debug("{}", iri);
        OWLOntologyManager m = OntManagers.createManager();
        OWLOntology o = m.loadOntology(iri);
        OWLIOUtils.print(o);
        o.axioms().map(String::valueOf).forEach(LOGGER::debug);
        Assertions.assertEquals(7, o.dataPropertiesInSignature().peek(x -> LOGGER.debug("DP: {}", x)).count());
    }
}
