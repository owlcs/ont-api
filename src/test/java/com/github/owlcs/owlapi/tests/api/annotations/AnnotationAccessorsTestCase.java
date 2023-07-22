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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLPrimitive;
import org.semanticweb.owlapi.search.EntitySearcher;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author Matthew Horridge, The University of Manchester, Bio-Health Informatics Group
 */
public class AnnotationAccessorsTestCase extends TestBase {

    private static final IRI SUBJECT = IRI.create("http://owlapi.sourceforge.net/ontologies/test#", "X");

    public static Collection<OWLPrimitive> getData() {
        return Arrays.asList(OWLFunctionalSyntaxFactory.Class(SUBJECT)
                , OWLFunctionalSyntaxFactory.NamedIndividual(SUBJECT)
                , OWLFunctionalSyntaxFactory.DataProperty(SUBJECT)
                , OWLFunctionalSyntaxFactory.ObjectProperty(SUBJECT)
                , OWLFunctionalSyntaxFactory.Datatype(SUBJECT)
                , OWLFunctionalSyntaxFactory.AnnotationProperty(SUBJECT)
                , OWLFunctionalSyntaxFactory.AnonymousIndividual());
    }

    private static OWLAnnotationAssertionAxiom createAnnotationAssertionAxiom() {
        OWLAnnotationProperty prop = OWLFunctionalSyntaxFactory.AnnotationProperty(iri("prop"));
        OWLAnnotationValue value = OWLFunctionalSyntaxFactory.Literal("value");
        return OWLFunctionalSyntaxFactory.AnnotationAssertion(prop, SUBJECT, value);
    }

    @ParameterizedTest
    @MethodSource("getData")
    public void testClassAccessor(OWLPrimitive e) {
        OWLOntology ont = getOWLOntology();
        OWLAnnotationAssertionAxiom ax = createAnnotationAssertionAxiom();
        ont.getOWLOntologyManager().addAxiom(ont, ax);
        Assertions.assertTrue(ont.annotationAssertionAxioms(SUBJECT).anyMatch(a -> a.equals(ax)));
        if (e instanceof OWLEntity) {
            Assertions.assertTrue(ont.annotationAssertionAxioms(((OWLEntity) e).getIRI()).anyMatch(a -> a.equals(ax)));
            Assertions.assertTrue(EntitySearcher.getAnnotationObjects((OWLEntity) e, ont).anyMatch(x -> x.equals(ax.getAnnotation())));
        }
    }
}
