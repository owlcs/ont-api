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

package com.github.owlcs.ontapi.tests.model;

import com.github.owlcs.ontapi.OntManagers;
import com.github.owlcs.ontapi.Ontology;
import com.github.owlcs.ontapi.utils.OntIRI;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * for SWRLRule Axiom.
 * <p>
 * Created by szuev on 19.10.2016.
 */
public class SWRLRuleOntModelTest extends OntModelTestBase {

    @Test
    public void test() throws OWLOntologyCreationException {
        OWLOntologyManager manager = OntManagers.createONT();
        OWLOntology owl = make(manager, OntIRI.create("http://test.org/rule"));

        owl.axioms().map(String::valueOf).forEach(LOGGER::debug);

        debug(owl);

        checkAxioms((Ontology) owl, AxiomType.DECLARATION);
    }

    private OWLOntology make(OWLOntologyManager manager, OntIRI iri) throws OWLOntologyCreationException {
        OWLDataFactory factory = manager.getOWLDataFactory();
        LOGGER.debug("Create ontology {}", iri);
        OWLOntology res = manager.createOntology(iri);

        OWLClass class1 = factory.getOWLClass(iri.addFragment("Class-1"));
        OWLIndividual individual1 = factory.getOWLNamedIndividual(iri.addFragment("indi-1"));
        OWLIndividual individual2 = factory.getOWLNamedIndividual(iri.addFragment("indi-2"));
        OWLObjectProperty objectProperty1 = factory.getOWLObjectProperty(iri.addFragment("object-prop-1"));
        OWLObjectProperty objectProperty2 = factory.getOWLObjectProperty(iri.addFragment("object-prop-2"));
        OWLDataProperty dataProperty1 = factory.getOWLDataProperty(iri.addFragment("data-prop-1"));

        OWLClassExpression ce1 = factory.getOWLObjectHasSelf(objectProperty1);

        SWRLIndividualArgument arg1 = factory.getSWRLIndividualArgument(individual1);
        SWRLIndividualArgument arg2 = factory.getSWRLIndividualArgument(individual2);
        SWRLAtom atom1 = factory.getSWRLClassAtom(class1, arg1);
        SWRLAtom atom2 = factory.getSWRLClassAtom(ce1, arg2);
        List<SWRLAtom> head = Stream.of(atom1, atom2).collect(Collectors.toList());

        SWRLDArgument argD1 = factory.getSWRLVariable(iri.addPath("swrl").addFragment("var1"));
        SWRLDArgument argD2 = factory.getSWRLLiteralArgument(factory.getOWLLiteral("literal", "zb"));
        SWRLAtom atom3_1 = factory.getSWRLBuiltInAtom(class1.getIRI(), Stream.of(argD1, argD2).collect(Collectors.toList()));
        SWRLAtom atom3_2 = factory.getSWRLBuiltInAtom(iri.addPath("swrl").addFragment("built-in"), Stream.of(argD1, argD2).collect(Collectors.toList()));
        SWRLAtom atom4 = factory.getSWRLDataPropertyAtom(dataProperty1, arg1, argD2);
        SWRLAtom atom5 = factory.getSWRLDataRangeAtom(OWL2Datatype.XSD_ANY_URI, argD1);
        SWRLIArgument argD3 = factory.getSWRLVariable(iri.addPath("swrl").addFragment("var2"));
        SWRLAtom atom6 = factory.getSWRLObjectPropertyAtom(objectProperty2, argD3, argD3);
        SWRLAtom atom7 = factory.getSWRLDifferentIndividualsAtom(arg1, argD3);


        SWRLIArgument argD4 = factory.getSWRLVariable(iri.addPath("swrl").addFragment("var3"));
        SWRLIArgument argD5 = factory.getSWRLVariable(iri.addPath("swrl").addFragment("var4"));
        SWRLAtom atom8 = factory.getSWRLSameIndividualAtom(argD4, argD5);

        List<SWRLAtom> body = Stream.of(atom3_1, atom3_2, atom4, atom5, atom6, atom7, atom8).collect(Collectors.toList());
        List<OWLAxiom> axioms = new ArrayList<>();
        axioms.add(factory.getSWRLRule(head, body));

        axioms.add(factory.getOWLClassAssertionAxiom(class1, individual2));

        axioms.forEach(axiom -> res.applyChanges(new AddAxiom(res, axiom)));
        return res;
    }
}
