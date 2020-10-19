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

package com.github.owlcs.owlapi.tests.api.syntax;

import com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory;
import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.Namespaces;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by ses on 6/23/14.
 */
public class IRIShorteningTestCase extends TestBase {

    @Test
    public void testIriEqualToPrefixNotShortenedInFSS() throws Exception {
        OWLOntology o = createTestOntology();
        String output = saveOntology(o, new FunctionalSyntaxDocumentFormat()).toString();
        testMatchExact(output, "NamedIndividual(rdf:)", false);
        testMatchExact(output, "NamedIndividual(rdf:type)", true);
    }

    private void testMatchExact(String output, String text, boolean expected) {
        String message = "should " + (expected ? "" : "not ") + "contain" + text + " - " + output;
        Assertions.assertEquals(expected, output.contains(text), message);
    }

    @Test
    public void testIriEqualToPrefixShortenedInTurtle() throws Exception {
        OWLOntology o = createTestOntology();
        String output = saveOntology(o, new TurtleDocumentFormat()).toString();
        LOGGER.debug(output);
        Set<String> patterns = Stream.of("%s\\s+rdf:type\\s+%s", "%s\\s+a\\s+%s").collect(Collectors.toSet());
        Stream.of("rdf:", "rdf:type").forEach(test ->
                Assertions.assertTrue(patterns.stream().anyMatch(p -> matchRegex(output, String.format(p, test, "owl:NamedIndividual"))),
                        "Can't find <" + test + "> named individual"));
    }

    public boolean matchRegex(String output, String regex) {
        Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(output);
        return matcher.find();
    }

    private OWLOntology createTestOntology() {
        OWLOntology o = getOWLOntology();
        OWLNamedIndividual i = df.getOWLNamedIndividual(OWLFunctionalSyntaxFactory.IRI(Namespaces.RDF.getPrefixIRI(), ""));
        o.add(df.getOWLDeclarationAxiom(i));
        i = df.getOWLNamedIndividual(OWLRDFVocabulary.RDF_TYPE);
        o.add(df.getOWLDeclarationAxiom(i));
        return o;
    }

    @Test
    public void shouldOutputURNsCorrectly() throws OWLOntologyCreationException, OWLOntologyStorageException {
        OWLOntology o = m.createOntology(IRI.create("urn:ontology:", "test"));
        o.add(df.getOWLObjectPropertyAssertionAxiom(
                df.getOWLObjectProperty("urn:test#", "p"),
                df.getOWLNamedIndividual("urn:test#", "test"), df.getOWLNamedIndividual("urn:other:", "test")));
        equal(o, roundTrip(o));
    }
}
