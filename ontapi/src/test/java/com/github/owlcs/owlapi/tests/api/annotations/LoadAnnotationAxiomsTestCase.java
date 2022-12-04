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
package com.github.owlcs.owlapi.tests.api.annotations;

import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat;
import org.semanticweb.owlapi.formats.OWLXMLDocumentFormat;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLSubAnnotationPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.AnnotationAssertion;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.AnnotationProperty;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.Class;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.IRI;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.Literal;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.RDFSComment;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.SubClassOf;

/**
 * @author Matthew Horridge, The University of Manchester, Bio-Health Informatics Group
 */
public class LoadAnnotationAxiomsTestCase extends TestBase {

    private OWLOntology createOntology() {
        OWLOntology res = getOWLOntology();
        OWLClass clsA = Class(IRI("http://ont.com#", "A"));
        OWLClass clsB = Class(IRI("http://ont.com#", "B"));
        OWLSubClassOfAxiom sca = SubClassOf(clsA, clsB);
        res.add(sca);
        OWLAnnotationProperty rdfsComment = RDFSComment();
        OWLLiteral lit = Literal("Hello world");
        OWLAnnotationAssertionAxiom annoAx1 = AnnotationAssertion(rdfsComment, clsA.getIRI(), lit);
        res.add(annoAx1);
        OWLAnnotationPropertyDomainAxiom annoAx2 = df.getOWLAnnotationPropertyDomainAxiom(rdfsComment, clsA.getIRI());
        res.add(annoAx2);
        OWLAnnotationPropertyRangeAxiom annoAx3 = df.getOWLAnnotationPropertyRangeAxiom(rdfsComment, clsB.getIRI());
        res.add(annoAx3);
        OWLAnnotationProperty myComment = AnnotationProperty(IRI("http://ont.com#", "myComment"));
        OWLSubAnnotationPropertyOfAxiom annoAx4 = df.getOWLSubAnnotationPropertyOfAxiom(myComment, rdfsComment);
        res.add(annoAx4);
        return res;
    }

    @Test
    public void testIgnoreAnnotations() throws Exception {
        OWLOntology ont = createOntology();
        reload(ont, new RDFXMLDocumentFormat());
        reload(ont, new OWLXMLDocumentFormat());
        reload(ont, new TurtleDocumentFormat());
        reload(ont, new FunctionalSyntaxDocumentFormat());
    }

    private void reload(OWLOntology ontology, OWLDocumentFormat format) throws Exception {
        LOGGER.debug("The format is [{}]", format.getClass().getSimpleName());
        OWLOntologyLoaderConfiguration withAnnotationsConfig = new OWLOntologyLoaderConfiguration();
        OWLOntologyLoaderConfiguration withoutAnnotationsConfig = withAnnotationsConfig.setLoadAnnotationAxioms(false);

        Set<OWLAxiom> axioms = ontology.axioms().filter(notDeclaration()).collect(Collectors.toSet());
        Set<OWLAxiom> annotationAxioms = axioms.stream().filter(OWLAxiom::isAnnotationAxiom).collect(Collectors.toSet());

        OWLOntology o1 = reload(ontology, format, withAnnotationsConfig);
        Set<OWLAxiom> axioms2 = o1.axioms().filter(notDeclaration()).collect(Collectors.toSet());
        Assertions.assertEquals(axioms, axioms2);
        OWLOntology o2 = reload(ontology, format, withoutAnnotationsConfig);
        Assertions.assertNotEquals(axioms, o2.axioms().filter(notDeclaration()).collect(Collectors.toSet()));
        Set<OWLAxiom> axiomsMinusAnnotationAxioms = new HashSet<>(axioms);
        axiomsMinusAnnotationAxioms.removeAll(annotationAxioms);
        Assertions.assertEquals(axiomsMinusAnnotationAxioms, o2.axioms().filter(notDeclaration()).collect(Collectors.toSet()));
    }

    private static Predicate<OWLAxiom> notDeclaration() {
        return a -> !AxiomType.DECLARATION.equals(a.getAxiomType());
    }

    private OWLOntology reload(OWLOntology ontology, OWLDocumentFormat format, OWLOntologyLoaderConfiguration conf) throws Exception {
        return loadOntologyWithConfig(saveOntology(ontology, format), conf);
    }
}
