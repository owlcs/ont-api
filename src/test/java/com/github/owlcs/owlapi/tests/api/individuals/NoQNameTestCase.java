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
package com.github.owlcs.owlapi.tests.api.individuals;

import com.github.owlcs.owlapi.OWLManager;
import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.rdf.rdfxml.renderer.IllegalElementNameException;

import java.lang.Class;
import java.util.HashSet;
import java.util.Set;

import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.*;

/**
 * @author Matthew Horridge, The University of Manchester, Information  Management Group
 */
public class NoQNameTestCase extends TestBase {

    @Test
    public void testCreate() throws Exception {
        Class<? extends Throwable> expectedCause = OWLManager.DEBUG_USE_OWL ?
                IllegalElementNameException.class : org.apache.jena.shared.InvalidPropertyURIException.class;
        try {
            roundTripOntology(createOntology());
            Assert.fail("Expected an exception specifying that a QName could not be generated");
        } catch (OWLOntologyStorageException e) {
            LOGGER.debug("Exception:::{}", e.getMessage());
            Throwable cause = e.getCause();
            Assert.assertNotNull(cause);
            LOGGER.debug("Cause:::{}", cause.getMessage());
            if (!expectedCause.isInstance(cause)) {
                throw e;
            }
        }
    }

    protected OWLOntology createOntology() {
        Set<OWLAxiom> axioms = new HashSet<>();
        OWLNamedIndividual indA = NamedIndividual(IRI("http://example.com/place/112013e2-df48-4a34-8a9d-99ef572a395A", ""));
        OWLNamedIndividual indB = NamedIndividual(IRI("http://example.com/place/112013e2-df48-4a34-8a9d-99ef572a395B", ""));
        OWLObjectProperty property = ObjectProperty(IRI("http://example.com/place/123", ""));
        axioms.add(ObjectPropertyAssertion(property, indA, indB));
        OWLOntology ont = getOWLOntology();
        ont.add(axioms);
        ont.signature().filter(e -> !e.isBuiltIn() && !ont.isDeclared(e, Imports.INCLUDED)).forEach(e -> ont.add(Declaration(e)));
        return ont;
    }
}
