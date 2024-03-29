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

package com.github.owlcs.ontapi.tests.formats;

import com.github.owlcs.ontapi.OntFormat;
import com.github.owlcs.ontapi.OntManagers;
import com.github.owlcs.ontapi.Ontology;
import com.github.owlcs.ontapi.testutils.OWLIOUtils;
import com.github.owlcs.ontapi.testutils.OntIRI;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Test loading from different formats.
 * TODO: At the moment it is only for four "good" owl-formats which are not supported by jena: fss, obo, omn, owl-rdf.
 * "Good" means it is passed reloading test.
 * The pure OWL-API mechanism is used for loading a document in these formats.
 * <p>
 * Created @ssz on 20.12.2016.
 */
public class SimpleFormatsTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleFormatsTest.class);
    private static final String fileName = "test2";
    private static List<OWLAxiom> expected;

    public static List<OntFormat> getData() {
        //return OntFormat.owlOnly().collect(Collectors.toList());
        return Arrays.asList(OntFormat.OWL_XML, OntFormat.MANCHESTER_SYNTAX, OntFormat.FUNCTIONAL_SYNTAX, OntFormat.OBO);
    }

    @BeforeAll
    public static void before() {
        OWLDataFactory factory = OntManagers.getDataFactory();

        OntIRI iri = OntIRI.create("http://test/formats");
        OWLClass clazz = factory.getOWLClass(iri.addFragment("ClassN1"));
        OWLDataProperty ndp = factory.getOWLDataProperty(iri.addFragment("DataPropertyN1"));
        OWLObjectProperty nop = factory.getOWLObjectProperty(iri.addFragment("ObjectPropertyN1"));
        OWLDatatype dt = OWL2Datatype.XSD_ANY_URI.getDatatype(factory);

        expected = Stream.of(
                factory.getOWLDeclarationAxiom(clazz),
                factory.getOWLDeclarationAxiom(ndp),
                factory.getOWLDeclarationAxiom(nop),
                factory.getOWLObjectPropertyDomainAxiom(nop, clazz),
                factory.getOWLObjectPropertyRangeAxiom(nop, clazz),
                factory.getOWLDataPropertyDomainAxiom(ndp, clazz),
                factory.getOWLDataPropertyRangeAxiom(ndp, dt)).sorted().collect(Collectors.toList());
    }

    @ParameterizedTest
    @MethodSource("getData")
    public void testFormat(OntFormat format) {
        IRI fileIRI = IRI.create(OWLIOUtils.getResourceURI("/ontapi/" + fileName + "." + format.getExt()));
        LOGGER.debug("Load ontology {}. Format: {}", fileIRI, format);
        Ontology o;
        try {
            o = OntManagers.createManager().loadOntology(fileIRI);
        } catch (OWLOntologyCreationException e) {
            throw new AssertionError("Can't load " + fileIRI + "[" + format + "] :: ", e);
        }
        OWLIOUtils.print(o);
        o.axioms().map(String::valueOf).forEach(LOGGER::debug);

        List<OWLAxiom> actual = o.axioms()
                .filter(axiom -> !AxiomType.ANNOTATION_ASSERTION.equals(axiom.getAxiomType()))
                .filter(axiom -> {
                    if (AxiomType.DECLARATION.equals(axiom.getAxiomType())) {
                        OWLDeclarationAxiom declarationAxiom = (OWLDeclarationAxiom) axiom;
                        if (declarationAxiom.getEntity().isBuiltIn()) return false;
                        return !declarationAxiom.getEntity().isOWLAnnotationProperty();
                    }
                    return true;
                })
                .sorted().collect(Collectors.toList());
        Assertions.assertEquals(expected, actual,
                String.format("[%s] Incorrect list of axioms (expected=%d,actual=%d)", format, expected.size(), actual.size()));
    }
}
