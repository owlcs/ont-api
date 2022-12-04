/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2022, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.github.owlcs.owlapi.tests.api.literals;

import com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory;
import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.RemoveAxiom;
import org.semanticweb.owlapi.util.OWLLiteralReplacer;
import org.semanticweb.owlapi.util.OWLObjectTransformer;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Matthew Horridge, The University of Manchester, Information Management Group
 */
public class TypedLiteralsTestCase extends TestBase {

    private final OWLDataProperty prop = OWLFunctionalSyntaxFactory.DataProperty(iri("p"));
    private final OWLNamedIndividual ind = OWLFunctionalSyntaxFactory.NamedIndividual(iri("i"));

    protected OWLOntology createAxioms() throws OWLOntologyCreationException {
        OWLOntology o = m.createOntology();
        o.add(OWLFunctionalSyntaxFactory.DataPropertyAssertion(prop, ind, OWLFunctionalSyntaxFactory.Literal(3)));
        o.add(OWLFunctionalSyntaxFactory.DataPropertyAssertion(prop, ind, OWLFunctionalSyntaxFactory.Literal(33.3)));
        o.add(OWLFunctionalSyntaxFactory.DataPropertyAssertion(prop, ind, OWLFunctionalSyntaxFactory.Literal(true)));
        o.add(OWLFunctionalSyntaxFactory.DataPropertyAssertion(prop, ind, OWLFunctionalSyntaxFactory.Literal(33.3f)));
        o.add(OWLFunctionalSyntaxFactory.DataPropertyAssertion(prop, ind, OWLFunctionalSyntaxFactory.Literal("33.3")));
        return o;
    }

    @Test
    public void testShouldReplaceLiterals() throws OWLOntologyCreationException {
        OWLOntology o = createAxioms();
        OWLLiteralReplacer replacer = new OWLLiteralReplacer(o.getOWLOntologyManager(), Collections.singleton(o));
        Map<OWLLiteral, OWLLiteral> replacements = new HashMap<>();
        replacements.put(OWLFunctionalSyntaxFactory.Literal(true), OWLFunctionalSyntaxFactory.Literal(false));
        replacements.put(OWLFunctionalSyntaxFactory.Literal(3), OWLFunctionalSyntaxFactory.Literal(4));
        List<OWLOntologyChange> results = replacer.changeLiterals(replacements);
        Assertions.assertTrue(results.contains(new AddAxiom(o,
                OWLFunctionalSyntaxFactory.DataPropertyAssertion(prop, ind, OWLFunctionalSyntaxFactory.Literal(4)))));
        Assertions.assertTrue(results.contains(new AddAxiom(o,
                OWLFunctionalSyntaxFactory.DataPropertyAssertion(prop, ind, OWLFunctionalSyntaxFactory.Literal(false)))));
        Assertions.assertTrue(results.contains(new RemoveAxiom(o,
                OWLFunctionalSyntaxFactory.DataPropertyAssertion(prop, ind, OWLFunctionalSyntaxFactory.Literal(3)))));
        Assertions.assertTrue(results.contains(new RemoveAxiom(o,
                OWLFunctionalSyntaxFactory.DataPropertyAssertion(prop, ind, OWLFunctionalSyntaxFactory.Literal(true)))));
        Assertions.assertEquals(4, results.size());
    }

    @Test
    public void testShouldReplaceLiteralsWithTransformer() throws OWLOntologyCreationException {
        OWLOntology o = createAxioms();
        final Map<OWLLiteral, OWLLiteral> replacements = new HashMap<>();
        replacements.put(OWLFunctionalSyntaxFactory.Literal(true), OWLFunctionalSyntaxFactory.Literal(false));
        replacements.put(OWLFunctionalSyntaxFactory.Literal(3), OWLFunctionalSyntaxFactory.Literal(4));
        OWLObjectTransformer<OWLLiteral> replacer = new OWLObjectTransformer<>((x) -> true, (input) -> {
            OWLLiteral l = replacements.get(input);
            if (l == null) {
                return input;
            }
            return l;
        }, df, OWLLiteral.class);
        List<OWLOntologyChange> results = replacer.change(o);
        Assertions.assertTrue(results.contains(new AddAxiom(o,
                OWLFunctionalSyntaxFactory.DataPropertyAssertion(prop, ind, OWLFunctionalSyntaxFactory.Literal(4)))));
        Assertions.assertTrue(results.contains(new AddAxiom(o,
                OWLFunctionalSyntaxFactory.DataPropertyAssertion(prop, ind, OWLFunctionalSyntaxFactory.Literal(false)))));
        Assertions.assertTrue(results.contains(new RemoveAxiom(o,
                OWLFunctionalSyntaxFactory.DataPropertyAssertion(prop, ind, OWLFunctionalSyntaxFactory.Literal(3)))));
        Assertions.assertTrue(results.contains(new RemoveAxiom(o,
                OWLFunctionalSyntaxFactory.DataPropertyAssertion(prop, ind, OWLFunctionalSyntaxFactory.Literal(true)))));
        Assertions.assertEquals(4, results.size());
    }
}
