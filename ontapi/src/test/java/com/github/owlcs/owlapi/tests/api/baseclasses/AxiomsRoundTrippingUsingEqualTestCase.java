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
package com.github.owlcs.owlapi.tests.api.baseclasses;

import com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory;
import com.github.owlcs.owlapi.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * @author Matthew Horridge, The University of Manchester, Information Management Group
 */
public class AxiomsRoundTrippingUsingEqualTestCase extends AxiomsRoundTrippingBase {

    public static Stream<OWLOntology> data() {
        OWLOntologyManager m = OWLManager.createOWLOntologyManager();
        return getData().stream().map(x -> createOntology(m, x));
    }

    public static List<AxiomBuilder> getData() {
        return Arrays.asList(
                // AnonymousIndividualRoundtrip
                () -> {
                    Set<OWLAxiom> axioms = new HashSet<>();
                    OWLAnonymousIndividual ind = OWLFunctionalSyntaxFactory.AnonymousIndividual();
                    OWLClass cls = OWLFunctionalSyntaxFactory.Class(iri("A"));
                    OWLAnnotationProperty prop = OWLFunctionalSyntaxFactory.AnnotationProperty(iri("prop"));
                    OWLAnnotationAssertionAxiom ax = OWLFunctionalSyntaxFactory.AnnotationAssertion(prop, cls.getIRI(), ind);
                    axioms.add(ax);
                    axioms.add(OWLFunctionalSyntaxFactory.Declaration(cls));
                    OWLObjectProperty p = OWLFunctionalSyntaxFactory.ObjectProperty(iri("p"));
                    axioms.add(OWLFunctionalSyntaxFactory.Declaration(p));
                    OWLAnonymousIndividual anon1 = OWLFunctionalSyntaxFactory.AnonymousIndividual();
                    OWLAnonymousIndividual anon2 = OWLFunctionalSyntaxFactory.AnonymousIndividual();
                    OWLNamedIndividual ind1 = OWLFunctionalSyntaxFactory.NamedIndividual(iri("j"));
                    OWLNamedIndividual ind2 = OWLFunctionalSyntaxFactory.NamedIndividual(iri("i"));
                    axioms.add(df.getOWLObjectPropertyAssertionAxiom(p, ind1, ind2));
                    axioms.add(df.getOWLObjectPropertyAssertionAxiom(p, anon1, anon1));
                    axioms.add(df.getOWLObjectPropertyAssertionAxiom(p, anon2, ind2));
                    axioms.add(df.getOWLObjectPropertyAssertionAxiom(p, ind2, anon2));
                    return axioms;
                },
                // AnonymousIndividuals2
                () -> {
                    // Originally submitted by Timothy Redmond
                    String ns = "http://another.com/ont";
                    OWLClass a = OWLFunctionalSyntaxFactory.Class(OWLFunctionalSyntaxFactory.IRI(ns + "#", "A"));
                    OWLAnnotationProperty p = OWLFunctionalSyntaxFactory.AnnotationProperty(OWLFunctionalSyntaxFactory.IRI(ns + "#", "p"));
                    OWLObjectProperty q = OWLFunctionalSyntaxFactory.ObjectProperty(OWLFunctionalSyntaxFactory.IRI(ns + "#", "q"));
                    OWLAnonymousIndividual h = OWLFunctionalSyntaxFactory.AnonymousIndividual();
                    OWLAnonymousIndividual i = OWLFunctionalSyntaxFactory.AnonymousIndividual();
                    Set<OWLAxiom> axioms = new HashSet<>();
                    axioms.add(OWLFunctionalSyntaxFactory.AnnotationAssertion(p, a.getIRI(), h));
                    axioms.add(OWLFunctionalSyntaxFactory.ClassAssertion(a, h));
                    axioms.add(OWLFunctionalSyntaxFactory.ObjectPropertyAssertion(q, h, i));
                    axioms.add(OWLFunctionalSyntaxFactory.AnnotationAssertion(OWLFunctionalSyntaxFactory.RDFSLabel(), h, OWLFunctionalSyntaxFactory.Literal("Second", "en")));
                    return axioms;
                },
                // AnonymousIndividuals
                () -> {
                    Set<OWLAxiom> axioms = new HashSet<>();
                    OWLAnonymousIndividual ind = OWLFunctionalSyntaxFactory.AnonymousIndividual();
                    axioms.add(OWLFunctionalSyntaxFactory.ObjectPropertyAssertion(OWLFunctionalSyntaxFactory.ObjectProperty(iri("p")), OWLFunctionalSyntaxFactory.NamedIndividual(iri("i1")), ind));
                    axioms.add(OWLFunctionalSyntaxFactory.ObjectPropertyAssertion(OWLFunctionalSyntaxFactory.ObjectProperty(iri("p")), ind, OWLFunctionalSyntaxFactory.NamedIndividual(iri("i2"))));
                    return axioms;
                },
                // ChainedAnonymousIndividuals
                () -> {
                    Set<OWLAxiom> axioms = new HashSet<>();
                    IRI annoPropIRI = OWLFunctionalSyntaxFactory.IRI("http://owlapi.sourceforge.net/ontology#", "annoProp");
                    OWLAnnotationProperty property = OWLFunctionalSyntaxFactory.AnnotationProperty(annoPropIRI);
                    IRI subject = OWLFunctionalSyntaxFactory.IRI("http://owlapi.sourceforge.net/ontology#", "subject");
                    axioms.add(OWLFunctionalSyntaxFactory.Declaration(OWLFunctionalSyntaxFactory.NamedIndividual(subject)));
                    OWLAnonymousIndividual individual1 = OWLFunctionalSyntaxFactory.AnonymousIndividual();
                    OWLAnonymousIndividual individual2 = OWLFunctionalSyntaxFactory.AnonymousIndividual();
                    OWLAnonymousIndividual individual3 = OWLFunctionalSyntaxFactory.AnonymousIndividual();
                    OWLAnnotationAssertionAxiom annoAssertion1 = OWLFunctionalSyntaxFactory.AnnotationAssertion(property, subject, individual1);
                    OWLAnnotationAssertionAxiom annoAssertion2 = OWLFunctionalSyntaxFactory.AnnotationAssertion(property, individual1, individual2);
                    OWLAnnotationAssertionAxiom annoAssertion3 = OWLFunctionalSyntaxFactory.AnnotationAssertion(property, individual2, individual3);
                    axioms.add(annoAssertion1);
                    axioms.add(annoAssertion2);
                    axioms.add(annoAssertion3);
                    return axioms;
                },
                // ClassAssertionWithAnonymousIndividual
                () -> {
                    Set<OWLAxiom> axioms = new HashSet<>();
                    OWLIndividual ind = OWLFunctionalSyntaxFactory.AnonymousIndividual("a");
                    OWLClass cls = OWLFunctionalSyntaxFactory.Class(iri("A"));
                    axioms.add(OWLFunctionalSyntaxFactory.ClassAssertion(cls, ind));
                    axioms.add(OWLFunctionalSyntaxFactory.Declaration(cls));
                    return axioms;
                },
                // DifferentIndividualsAnonymous
                () -> {
                    Set<OWLAxiom> axioms = new HashSet<>();
                    axioms.add(OWLFunctionalSyntaxFactory.DifferentIndividuals(OWLFunctionalSyntaxFactory.AnonymousIndividual(), OWLFunctionalSyntaxFactory.AnonymousIndividual(), OWLFunctionalSyntaxFactory.AnonymousIndividual()));
                    return axioms;
                },
                // DifferentIndividualsPairwiseAnonymous
                () -> {
                    Set<OWLAxiom> axioms = new HashSet<>();
                    axioms.add(OWLFunctionalSyntaxFactory.DifferentIndividuals(OWLFunctionalSyntaxFactory.AnonymousIndividual(), OWLFunctionalSyntaxFactory.AnonymousIndividual()));
                    return axioms;
                },
                // ObjectPropertyAssertionWithAnonymousIndividuals
                () -> {
                    OWLIndividual subject = OWLFunctionalSyntaxFactory.AnonymousIndividual();
                    OWLIndividual object = OWLFunctionalSyntaxFactory.AnonymousIndividual();
                    OWLObjectProperty prop = OWLFunctionalSyntaxFactory.ObjectProperty(iri("prop"));
                    Set<OWLAxiom> axioms = new HashSet<>();
                    axioms.add(OWLFunctionalSyntaxFactory.ObjectPropertyAssertion(prop, subject, object));
                    axioms.add(OWLFunctionalSyntaxFactory.Declaration(prop));
                    return axioms;
                },
                // SameIndividualsAnonymous
                () -> {
                    Set<OWLAxiom> axioms = new HashSet<>();
                    // Can't round trip more than two in RDF! Also, same
                    // individuals
                    // axiom
                    // with anon individuals is not allowed
                    // in OWL 2, but it should at least round trip
                    axioms.add(OWLFunctionalSyntaxFactory.SameIndividual(OWLFunctionalSyntaxFactory.AnonymousIndividual(), OWLFunctionalSyntaxFactory.AnonymousIndividual()));
                    return axioms;
                });
    }
}
