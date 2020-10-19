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

import com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory;
import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.*;

import java.util.Collections;

/**
 * @author Matthew Horridge, The University of Manchester, Information Management Group
 */
public class GetAxiomsIgnoringAnnotationsTestCase extends TestBase {

    @Test
    public void testGetAxiomsIgnoringAnnotations() {
        OWLLiteral annoLiteral = OWLFunctionalSyntaxFactory.Literal("value");
        OWLAnnotationProperty annoProp = OWLFunctionalSyntaxFactory.AnnotationProperty(iri("annoProp"));
        OWLAnnotation anno = df.getOWLAnnotation(annoProp, annoLiteral);
        OWLAxiom axiom = df.getOWLSubClassOfAxiom(OWLFunctionalSyntaxFactory.Class(iri("A")),
                OWLFunctionalSyntaxFactory.Class(iri("B")), Collections.singleton(anno));
        OWLOntology ont = getOWLOntology();
        ont.getOWLOntologyManager().addAxiom(ont, axiom);
        Assertions.assertTrue(ont.axiomsIgnoreAnnotations(axiom).anyMatch(axiom::equals));
        OWLAxiom noAnnotations = axiom.getAxiomWithoutAnnotations();
        Assertions.assertFalse(ont.axiomsIgnoreAnnotations(axiom).anyMatch(noAnnotations::equals));
        Assertions.assertTrue(ont.axiomsIgnoreAnnotations(noAnnotations).anyMatch(axiom::equals));
        Assertions.assertFalse(ont.axiomsIgnoreAnnotations(noAnnotations).anyMatch(noAnnotations::equals));
    }
}
