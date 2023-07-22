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
package com.github.owlcs.owlapi.tests.api.annotations;

import com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory;
import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.search.Filters;
import org.semanticweb.owlapi.search.Searcher;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * @author Matthew Horridge, The University of Manchester, Bio-Health Informatics Group
 */
public class AnnotationPropertyConvenienceMethodTestCase extends TestBase {

    @Test
    public void testGetSuperProperties() {
        OWLOntology ont = getOWLOntology();
        OWLAnnotationProperty propP = OWLFunctionalSyntaxFactory.AnnotationProperty(iri("propP"));
        OWLAnnotationProperty propQ = OWLFunctionalSyntaxFactory.AnnotationProperty(iri("propQ"));
        OWLAnnotationProperty propR = OWLFunctionalSyntaxFactory.AnnotationProperty(iri("propR"));
        ont.getOWLOntologyManager().addAxiom(ont, df.getOWLSubAnnotationPropertyOfAxiom(propP, propQ));
        ont.getOWLOntologyManager().addAxiom(ont, df.getOWLSubAnnotationPropertyOfAxiom(propP, propR));
        Collection<OWLAxiom> axioms = ont.axioms(Filters.subAnnotationWithSub, propP, Imports.INCLUDED).collect(Collectors.toSet());
        Assertions.assertTrue(Searcher.sup(axioms.stream()).anyMatch(propQ::equals));
        Assertions.assertTrue(Searcher.sup(axioms.stream()).anyMatch(propR::equals));
        axioms = ont.axioms(Filters.subAnnotationWithSub, propP, Imports.EXCLUDED).collect(Collectors.toSet());
        Assertions.assertTrue(Searcher.sup(axioms.stream()).anyMatch(propQ::equals));
        Assertions.assertTrue(Searcher.sup(axioms.stream()).anyMatch(propR::equals));
    }

    @Test
    public void testGetSubProperties() {
        OWLOntology ont = getOWLOntology();
        OWLAnnotationProperty propP = OWLFunctionalSyntaxFactory.AnnotationProperty(iri("propP"));
        OWLAnnotationProperty propQ = OWLFunctionalSyntaxFactory.AnnotationProperty(iri("propQ"));
        OWLAnnotationProperty propR = OWLFunctionalSyntaxFactory.AnnotationProperty(iri("propR"));
        ont.getOWLOntologyManager().addAxiom(ont, df.getOWLSubAnnotationPropertyOfAxiom(propP, propQ));
        ont.getOWLOntologyManager().addAxiom(ont, df.getOWLSubAnnotationPropertyOfAxiom(propP, propR));
        Assertions.assertTrue(Searcher.sub(ont.axioms(Filters.subAnnotationWithSuper, propQ, Imports.INCLUDED)).anyMatch(propP::equals));
        Assertions.assertTrue(Searcher.sub(ont.axioms(Filters.subAnnotationWithSuper, propQ, Imports.EXCLUDED)).anyMatch(propP::equals));
        Assertions.assertTrue(Searcher.sub(ont.axioms(Filters.subAnnotationWithSuper, propR, Imports.INCLUDED)).anyMatch(propP::equals));
        Assertions.assertTrue(Searcher.sub(ont.axioms(Filters.subAnnotationWithSuper, propR, Imports.EXCLUDED)).anyMatch(propP::equals));
    }
}
