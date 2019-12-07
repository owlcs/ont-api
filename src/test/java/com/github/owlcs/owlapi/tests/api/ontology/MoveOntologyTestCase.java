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
package com.github.owlcs.owlapi.tests.api.ontology;

import com.github.owlcs.ontapi.OntApiException;
import com.github.owlcs.owlapi.OWLManager;
import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.parameters.OntologyCopy;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Matthew Horridge, The University Of Manchester, Bio-Health Informatics Group
 */
public class MoveOntologyTestCase extends TestBase {

    private final static
    String s = "<?xml version=\"1.0\"?>\n" + "<rdf:RDF xmlns=\"urn:test#test\"\n"
            + "     xml:base=\"urn:test#test\"\n" + "     xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"\n"
            + "     xmlns:swrl=\"http://www.w3.org/2003/11/swrl#\"\n"
            + "     xmlns:swrlb=\"http://www.w3.org/2003/11/swrlb#\"\n"
            + "     xmlns:xsd=\"http://www.w3.org/2001/XMLSchema#\"\n"
            + "     xmlns:owl=\"http://www.w3.org/2002/07/owl#\"\n"
            + "     xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n"
            + "    <owl:Ontology rdf:about=\"urn:testcopy\"><owl:imports rdf:resource=\"urn:test#test\"/></owl:Ontology>\n"
            + "    <rdfs:Datatype rdf:about=\"urn:mydatatype\">\n" + "        <owl:equivalentClass>\n"
            + "            <rdfs:Datatype rdf:about=\"http://www.w3.org/2001/XMLSchema#double\"/>\n"
            + "        </owl:equivalentClass>\n" + "    </rdfs:Datatype>\n" + "    <owl:Axiom>\n"
            + "        <rdfs:label >datatype definition</rdfs:label>\n"
            + "        <owl:annotatedProperty rdf:resource=\"http://www.w3.org/2002/07/owl#equivalentClass\"/>\n"
            + "        <owl:annotatedSource rdf:resource=\"urn:mydatatype\"/>\n" + "        <owl:annotatedTarget>\n"
            + "            <rdfs:Datatype rdf:about=\"http://www.w3.org/2001/XMLSchema#double\"/>\n"
            + "        </owl:annotatedTarget>\n" + "    </owl:Axiom></rdf:RDF>";

    private static <T> Set<T> asSet(Stream<T> s) {
        return s.collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Before
    public void setUp() throws OWLOntologyCreationException {
        m.createOntology(IRI.create("urn:test#", "test"));
    }

    @Test
    public void testMove() throws OWLOntologyCreationException {
        boolean isONT = !OWLManager.DEBUG_USE_OWL;
        OWLOntology o = m.loadOntologyFromOntologyDocument(new StringDocumentSource(s));
        OWLOntology copy;
        try {
            copy = m1.copyOntology(o, OntologyCopy.MOVE);
            if (isONT) {
                Assert.fail("This functionality is expected to be disabled");
            }
        } catch (OntApiException.Unsupported e) {
            if (isONT) {
                LOGGER.debug("OK: '{}'", e.getMessage());
                return;
            }
            throw e;
        }
        Assert.assertSame(o, copy);
        Assert.assertEquals(m1, copy.getOWLOntologyManager());
        Assert.assertFalse(m.contains(o));
        Assert.assertTrue(m1.contains(copy));
        Assert.assertEquals(asSet(o.annotations()), asSet(copy.annotations()));
        Assert.assertNull(m.getOntologyFormat(o));
    }

    @Test
    public void testShallow() throws OWLOntologyCreationException {
        OWLOntology o = m.loadOntologyFromOntologyDocument(new StringDocumentSource(s));
        OWLOntology copy = m1.copyOntology(o, OntologyCopy.SHALLOW);
        Assert.assertEquals(m1, copy.getOWLOntologyManager());
        Assert.assertTrue(m.contains(o));
        Assert.assertTrue(m1.contains(copy));
        Assert.assertNotNull(m.getOntologyFormat(o));
        Assert.assertEquals(asSet(o.annotations()), asSet(copy.annotations()));
        Assert.assertEquals(asSet(o.importsDeclarations()), asSet(copy.importsDeclarations()));
    }

    @Test
    public void testDeep() throws OWLOntologyCreationException {
        OWLOntology o = m.loadOntologyFromOntologyDocument(new StringDocumentSource(s));
        OWLOntology copy = m1.copyOntology(o, OntologyCopy.DEEP);
        Assert.assertEquals(m1, copy.getOWLOntologyManager());
        Assert.assertTrue(m.contains(o));
        Assert.assertTrue(m1.contains(copy));
        Assert.assertNotNull(m.getOntologyFormat(o));
        Assert.assertNotNull(m1.getOntologyFormat(o));
        Assert.assertEquals(asSet(o.annotations()), asSet(copy.annotations()));
        Assert.assertEquals(asSet(o.importsDeclarations()), asSet(copy.importsDeclarations()));
    }
}
