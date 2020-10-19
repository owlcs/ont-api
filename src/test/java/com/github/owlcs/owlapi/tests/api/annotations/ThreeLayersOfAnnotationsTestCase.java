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

package com.github.owlcs.owlapi.tests.api.annotations;

import com.github.owlcs.owlapi.tests.api.baseclasses.AbstractRoundTrippingTestCase;
import org.junit.jupiter.api.Assertions;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

public class ThreeLayersOfAnnotationsTestCase extends AbstractRoundTrippingTestCase {

    @Override
    protected OWLOntology createOntology() {
        String oboInOwl = "urn:obo:";
        OWLOntology o;
        try {
            o = m.createOntology(IRI.create("urn:nested:", "ontology"));
        } catch (OWLOntologyCreationException e) {
            return Assertions.fail(e);
        }
        OWLClass dbxref = df.getOWLClass(IRI.create(oboInOwl, "DbXref"));
        OWLClass definition = df.getOWLClass(IRI.create(oboInOwl, "Definition"));
        OWLObjectProperty adjacent_to = df.getOWLObjectProperty(IRI.create(oboInOwl, "adjacent_to"));
        OWLAnnotationProperty hasDefinition = df.getOWLAnnotationProperty(IRI.create(oboInOwl, "hasDefinition"));
        OWLAnnotationProperty hasdbxref = df.getOWLAnnotationProperty(IRI.create(oboInOwl, "hasDbXref"));
        OWLDataProperty hasuri = df.getOWLDataProperty(IRI.create(oboInOwl, "hasURI"));
        OWLAnonymousIndividual ind1 = df.getOWLAnonymousIndividual();
        m.addAxiom(o, df.getOWLClassAssertionAxiom(dbxref, ind1));
        m.addAxiom(o, df.getOWLDataPropertyAssertionAxiom(hasuri, ind1, df.getOWLLiteral("urn:SO:SO_ke",
                OWL2Datatype.XSD_ANY_URI)));
        OWLAnonymousIndividual ind2 = df.getOWLAnonymousIndividual();
        m.addAxiom(o, df.getOWLClassAssertionAxiom(definition, ind2));
        m.addAxiom(o, df.getOWLAnnotationAssertionAxiom(hasdbxref, ind2, ind1));
        m.addAxiom(o, df.getOWLAnnotationAssertionAxiom(hasDefinition, adjacent_to.getIRI(), ind2));
        return o;
    }

    @Override
    public void testManchesterOWLSyntax() {
        // not supported in Manchester syntax
    }
}
