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
package com.github.owlcs.owlapi.tests.api.searcher;

import com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory;
import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.search.Filters;
import org.semanticweb.owlapi.search.Searcher;

import java.util.Collection;
import java.util.stream.Collectors;

public class SearcherTestCase extends TestBase {

    @Test
    public void testShouldSearch() {
        // given
        OWLOntology o = getOWLOntology();
        OWLClass c = OWLFunctionalSyntaxFactory.Class(IRI.create("urn:test#", "c"));
        OWLClass d = OWLFunctionalSyntaxFactory.Class(IRI.create("urn:test#", "d"));
        OWLAxiom ax = OWLFunctionalSyntaxFactory.SubClassOf(c, d);
        o.getOWLOntologyManager().addAxiom(o, ax);
        Assertions.assertTrue(o.axioms(AxiomType.SUBCLASS_OF).anyMatch(ax::equals));
        Assertions.assertTrue(o.axioms(c).anyMatch(ax::equals));
    }

    @Test
    public void testShouldSearchObjectProperties() {
        // given
        OWLOntology o = getOWLOntology();
        OWLObjectProperty c = OWLFunctionalSyntaxFactory.ObjectProperty(IRI.create("urn:test#", "c"));
        OWLObjectProperty d = OWLFunctionalSyntaxFactory.ObjectProperty(IRI.create("urn:test#", "d"));
        OWLObjectProperty e = OWLFunctionalSyntaxFactory.ObjectProperty(IRI.create("urn:test#", "e"));
        OWLClass x = OWLFunctionalSyntaxFactory.Class(IRI.create("urn:test#", "x"));
        OWLClass y = OWLFunctionalSyntaxFactory.Class(IRI.create("urn:test#", "Y"));
        OWLAxiom ax = OWLFunctionalSyntaxFactory.SubObjectPropertyOf(c, d);
        OWLAxiom ax2 = OWLFunctionalSyntaxFactory.ObjectPropertyDomain(c, x);
        OWLAxiom ax3 = OWLFunctionalSyntaxFactory.ObjectPropertyRange(c, y);
        OWLAxiom ax4 = OWLFunctionalSyntaxFactory.EquivalentObjectProperties(c, e);
        o.getOWLOntologyManager().addAxiom(o, ax);
        o.getOWLOntologyManager().addAxiom(o, ax2);
        o.getOWLOntologyManager().addAxiom(o, ax3);
        o.getOWLOntologyManager().addAxiom(o, ax4);
        Assertions.assertTrue(o.axioms(AxiomType.SUB_OBJECT_PROPERTY).anyMatch(ax::equals));
        Collection<OWLAxiom> axioms = o.axioms(Filters.subObjectPropertyWithSuper, d, Imports.INCLUDED).collect(Collectors.toSet());
        Assertions.assertTrue(Searcher.sub(axioms.stream()).anyMatch(c::equals));
        axioms = o.axioms(Filters.subObjectPropertyWithSub, c, Imports.INCLUDED).collect(Collectors.toSet());
        Assertions.assertTrue(Searcher.sup(axioms.stream()).anyMatch(d::equals));
        Assertions.assertTrue(Searcher.domain(o.objectPropertyDomainAxioms(c)).anyMatch(x::equals));
        Assertions.assertTrue(Searcher.equivalent(o.equivalentObjectPropertiesAxioms(c)).anyMatch(e::equals));
    }

    @Test
    public void testShouldSearchDataProperties() {
        // given
        OWLOntology o = getOWLOntology();
        OWLDataProperty c = OWLFunctionalSyntaxFactory.DataProperty(IRI.create("urn:test#", "c"));
        OWLDataProperty d = OWLFunctionalSyntaxFactory.DataProperty(IRI.create("urn:test#", "d"));
        OWLDataProperty e = OWLFunctionalSyntaxFactory.DataProperty(IRI.create("urn:test#", "e"));
        OWLAxiom ax = OWLFunctionalSyntaxFactory.SubDataPropertyOf(c, d);
        OWLClass x = OWLFunctionalSyntaxFactory.Class(IRI.create("urn:test#", "x"));
        OWLAxiom ax2 = OWLFunctionalSyntaxFactory.DataPropertyDomain(c, x);
        OWLAxiom ax3 = OWLFunctionalSyntaxFactory.DataPropertyRange(c, OWLFunctionalSyntaxFactory.Boolean());
        OWLAxiom ax4 = OWLFunctionalSyntaxFactory.EquivalentDataProperties(c, e);
        o.getOWLOntologyManager().addAxiom(o, ax);
        o.getOWLOntologyManager().addAxiom(o, ax2);
        o.getOWLOntologyManager().addAxiom(o, ax3);
        o.getOWLOntologyManager().addAxiom(o, ax4);
        Assertions.assertTrue(o.axioms(AxiomType.SUB_DATA_PROPERTY).anyMatch(ax::equals));
        Assertions.assertTrue(Searcher.sub(o.axioms(Filters.subDataPropertyWithSuper, d, Imports.INCLUDED)).anyMatch(c::equals));
        Collection<OWLAxiom> axioms = o.axioms(Filters.subDataPropertyWithSub, c, Imports.INCLUDED).collect(Collectors.toSet());
        Assertions.assertTrue(Searcher.sup(axioms.stream()).anyMatch(d::equals));
        Assertions.assertTrue(Searcher.domain(o.dataPropertyDomainAxioms(c)).anyMatch(x::equals));
        Assertions.assertTrue(Searcher.range(o.dataPropertyRangeAxioms(c)).anyMatch(OWLFunctionalSyntaxFactory.Boolean()::equals));
        Assertions.assertTrue(Searcher.equivalent(o.equivalentDataPropertiesAxioms(c)).anyMatch(e::equals));
    }
}
