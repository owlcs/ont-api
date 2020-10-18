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
package com.github.owlcs.owlapi.tests.api.baseclasses;

import com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory;
import com.github.owlcs.owlapi.OWLManager;
import com.google.common.collect.Sets;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.semanticweb.owlapi.vocab.OWLFacet;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.Class;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.Integer;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.*;

/**
 * @author Matthew Horridge, The University of Manchester, Information Management Group
 */
public class AxiomsRoundTrippingTestCase extends AxiomsRoundTrippingBase {

    private static final IRI iriA = iri("A");
    private static final OWLClass clsA = Class(iriA);
    private static final OWLClass clsB = Class(iri("B"));
    private static final OWLDataProperty dp = DataProperty(iri("p"));
    private static final OWLDataProperty dq = DataProperty(iri("q"));
    private static final OWLObjectProperty op = ObjectProperty(iri("op"));
    private static final OWLObjectProperty oq = ObjectProperty(iri("oq"));
    private static final OWLDataProperty dpA = DataProperty(iri("dpropA"));
    private static final OWLDataProperty dpB = DataProperty(iri("dpropB"));
    private static final OWLDataProperty dpC = DataProperty(iri("dpropC"));
    private static final OWLObjectProperty propA = ObjectProperty(iri("propA"));
    private static final OWLObjectProperty propB = ObjectProperty(iri("propB"));
    private static final OWLObjectProperty propC = ObjectProperty(iri("propC"));
    private static final OWLObjectProperty propD = ObjectProperty(iri("propD"));
    private static final OWLAnnotationProperty apropA = AnnotationProperty(iri("apropA"));
    private static final OWLAnnotationProperty apropB = AnnotationProperty(iri("apropB"));
    private static final OWLNamedIndividual ind = NamedIndividual(iri("i"));
    private static final OWLNamedIndividual indj = NamedIndividual(iri("j"));
    private static final OWLEntity peter = NamedIndividual(IRI("http://www.another.com/ont#", "peter"));
    private static final OWLAnnotation ann1 = Annotation(RDFSLabel(), Literal("Annotation 1"));
    private static final OWLAnnotation ann2 = Annotation(RDFSLabel(), Literal("Annotation 2"));
    private static final OWLAnnotation eAnn1 = Annotation(RDFSLabel(), Literal("EntityAnnotation 1"));
    private static final OWLAnnotation eAnn2 = Annotation(RDFSLabel(), Literal("EntityAnnotation 2"));
    private static final OWLDatatype datatype = Datatype(IRI("http://www.ont.com/myont/", "mydatatype"));
    private static final OWLAnnotation annoOuterOuter1 = Annotation(AnnotationProperty(iri("myOuterOuterLabel1")),
            Literal("Outer Outer label 1"));
    private static final OWLAnnotation annoOuterOuter2 = Annotation(AnnotationProperty(iri("myOuterOuterLabel2")),
            Literal("Outer Outer label 2"));
    /**
     * ONT-API:
     * The IRI <file:/c/test.owlapi#SSN> has been changed to <file:///c/test.owlapi#SSN>.
     * This is due to behaviour of org.apache.jena.riot.system.IRIResolver.
     * While reading Jena puts the uris in order (for most formats with several exceptions).
     * And the IRI <file:/c/test.owlapi#SSN> is treated as wrong (bad scheme) and automatically corrected.
     * See {@link org.apache.jena.riot.system.IRIResolver#resolveSilent(String)} and
     * {@link org.apache.jena.riot.system.RiotLib}.
     * TODO: Currently I don't see how to change jena-reading behaviour easily.
     * and this is of course a hack to make these tests passed.
     * But i believe that Jena works more correctly than OWL-API.
     * Anyway it does not break tests logic.
     * So it seems OK for me.
     * It fixes {@link #testJSONLD()}, {@link #testTurtle()} and {@link #testTrig()}
     * (for these formats Jena uses common way ({@link org.apache.jena.riot.system.IRIResolver.IRIResolverNormal})).
     */
    @SuppressWarnings("JavadocReference")
    private static final OWLDatatype dt = Datatype(IRI("file:///c/test.owlapi#", "SSN"));
    private static final OWLFacetRestriction fr = FacetRestriction(OWLFacet.PATTERN, Literal(
            "[0-9]{3}-[0-9]{2}-[0-9]{4}"));
    private static final OWLDataRange dr = DatatypeRestriction(Datatype(IRI("http://www.w3.org/2001/XMLSchema#",
            "string")), fr);
    private static final OWLDataIntersectionOf disj1 = DataIntersectionOf(DataComplementOf(dr), dt);
    private static final OWLDataIntersectionOf disj2 = DataIntersectionOf(DataComplementOf(dt), dr);
    private static final OWLAnnotation annoOuter = Annotation(AnnotationProperty(iri("myOuterLabel")), Literal(
            "Outer label"), annoOuterOuter1, annoOuterOuter2);
    private static final OWLAnnotation annoInner = Annotation(AnnotationProperty(iri("myLabel")), Literal("Label"),
            annoOuter);

    public static Stream<OWLOntology> data() {
        OWLOntologyManager m = OWLManager.createOWLOntologyManager();
        return Arrays.stream(getData()).map(x -> createOntology(m, x));
    }

    public static AxiomBuilder[] getData() {
        return new AxiomBuilder[]{
                // SWRLRuleAlternateNS
                () -> {
                    Set<OWLAxiom> axioms = new HashSet<>();
                    SWRLVariable varX = df.getSWRLVariable("http://www.owlapi#", "x");
                    SWRLVariable varY = df.getSWRLVariable("http://www.owlapi#", "y");
                    SWRLVariable varZ = df.getSWRLVariable("http://www.owlapi#", "z");
                    Set<SWRLAtom> body = new HashSet<>();
                    body.add(df.getSWRLClassAtom(Class(iri("A")), varX));
                    SWRLIndividualArgument indIArg = df.getSWRLIndividualArgument(ind);
                    SWRLIndividualArgument indJArg = df.getSWRLIndividualArgument(indj);
                    body.add(df.getSWRLClassAtom(Class(iri("D")), indIArg));
                    body.add(df.getSWRLClassAtom(Class(iri("B")), varX));
                    SWRLVariable varQ = df.getSWRLVariable("http://www.owlapi#", "q");
                    SWRLVariable varR = df.getSWRLVariable("http://www.owlapi#", "r");
                    body.add(df.getSWRLDataPropertyAtom(dp, varX, varQ));
                    OWLLiteral lit = Literal(33);
                    SWRLLiteralArgument litArg = df.getSWRLLiteralArgument(lit);
                    body.add(df.getSWRLDataPropertyAtom(dp, varY, litArg));
                    Set<SWRLAtom> head = new HashSet<>();
                    head.add(df.getSWRLClassAtom(Class(iri("C")), varX));
                    head.add(df.getSWRLObjectPropertyAtom(op, varY, varZ));
                    head.add(df.getSWRLSameIndividualAtom(varX, varY));
                    head.add(df.getSWRLSameIndividualAtom(indIArg, indJArg));
                    head.add(df.getSWRLDifferentIndividualsAtom(varX, varZ));
                    head.add(df.getSWRLDifferentIndividualsAtom(varX, varZ));
                    head.add(df.getSWRLDifferentIndividualsAtom(indIArg, indJArg));
                    OWLObjectSomeValuesFrom svf = ObjectSomeValuesFrom(op, Class(iri("A")));
                    head.add(df.getSWRLClassAtom(svf, varX));
                    List<SWRLDArgument> args = new ArrayList<>();
                    args.add(varQ);
                    args.add(varR);
                    args.add(litArg);
                    head.add(df.getSWRLBuiltInAtom(IRI("http://www.owlapi#", "myBuiltIn"), args));
                    axioms.add(df.getSWRLRule(body, head));
                    return axioms;
                },
                // SWRLRule
                () -> {
                    Set<OWLAxiom> axioms = new HashSet<>();
                    SWRLVariable varX = df.getSWRLVariable("urn:swrl:var#", "x");
                    SWRLVariable varY = df.getSWRLVariable("urn:swrl:var#", "y");
                    SWRLVariable varZ = df.getSWRLVariable("urn:swrl:var#", "z");
                    Set<SWRLAtom> body = new HashSet<>();
                    body.add(df.getSWRLClassAtom(Class(iri("A")), varX));
                    SWRLIndividualArgument indIArg = df.getSWRLIndividualArgument(ind);
                    SWRLIndividualArgument indJArg = df.getSWRLIndividualArgument(indj);
                    body.add(df.getSWRLClassAtom(Class(iri("D")), indIArg));
                    body.add(df.getSWRLClassAtom(Class(iri("B")), varX));
                    SWRLVariable varQ = df.getSWRLVariable("urn:swrl:var#", "q");
                    SWRLVariable varR = df.getSWRLVariable("urn:swrl:var#", "r");
                    body.add(df.getSWRLDataPropertyAtom(dp, varX, varQ));
                    OWLLiteral lit = Literal(33);
                    SWRLLiteralArgument litArg = df.getSWRLLiteralArgument(lit);
                    body.add(df.getSWRLDataPropertyAtom(dp, varY, litArg));
                    Set<SWRLAtom> head = new HashSet<>();
                    head.add(df.getSWRLClassAtom(Class(iri("C")), varX));
                    head.add(df.getSWRLObjectPropertyAtom(op, varY, varZ));
                    head.add(df.getSWRLSameIndividualAtom(varX, varY));
                    head.add(df.getSWRLSameIndividualAtom(indIArg, indJArg));
                    head.add(df.getSWRLDifferentIndividualsAtom(varX, varZ));
                    head.add(df.getSWRLDifferentIndividualsAtom(varX, varZ));
                    head.add(df.getSWRLDifferentIndividualsAtom(indIArg, indJArg));
                    OWLObjectSomeValuesFrom svf = ObjectSomeValuesFrom(op, Class(iri("A")));
                    head.add(df.getSWRLClassAtom(svf, varX));
                    List<SWRLDArgument> args = new ArrayList<>();
                    args.add(varQ);
                    args.add(varR);
                    args.add(litArg);
                    head.add(df.getSWRLBuiltInAtom(IRI("http://www.owlapi#", "myBuiltIn"), args));
                    axioms.add(df.getSWRLRule(body, head));
                    return axioms;
                },
                // 2:
                () -> singleton(SubPropertyChainOf(Arrays.asList(propA, propB, propC), propD)),
                // 3:
                () -> singleton(AsymmetricObjectProperty(op)),
                // 4:
                () -> singleton(DifferentIndividuals(createIndividual(), createIndividual(), createIndividual(),
                        createIndividual(), createIndividual(), createIndividual(), createIndividual(), createIndividual(),
                        createIndividual(), createIndividual())),
                // 5:
                () -> Sets.newHashSet(SubClassOf(clsA, ObjectSomeValuesFrom(op, ObjectSomeValuesFrom(op, clsB))),
                        Declaration(clsA), Declaration(clsB)),
                // 6:
                () -> Sets.newHashSet(Declaration(RDFSLabel()), Declaration(peter), AnnotationAssertion(RDFSLabel(), peter
                        .getIRI(), Literal("X", "en"), ann1, ann2)),
                //
                () -> Sets.newHashSet(Declaration(RDFSLabel()), Declaration(peter, eAnn1, eAnn2), AnnotationAssertion(
                        RDFSLabel(), peter.getIRI(), Literal("X", "en"), ann1, ann2)),
                // 8:
                () -> singleton(InverseObjectProperties(oq, op)),
                //
                () -> singleton(InverseObjectProperties(op, oq)),
                // 10:
                () -> Sets.newHashSet(Declaration(clsA), AnnotationAssertion(apropA, clsA.getIRI(), IRI(
                        "http://www.semanticweb.org/owlapi#", "object"))),
                // 11:
                () -> singleton(SubClassOf(clsA, clsB, singleton(annoInner))),
                // 12:
                () -> singleton(AnnotationPropertyDomain(RDFSComment(), iriA)),
                //
                () -> singleton(AnnotationPropertyRange(RDFSComment(), iriA)),
                // 14:
                () -> singleton(SubAnnotationPropertyOf(apropA, RDFSLabel())),
                // 15:
                () -> singleton(SubClassOf(clsA, DataMaxCardinality(3, dp, Integer()))),
                // 16:
                () -> singleton(SubClassOf(clsA, DataMinCardinality(3, dp, Integer()))),
                // 17:
                () -> singleton(SubClassOf(clsA, DataExactCardinality(3, dp, Integer()))),
                // 18:
                () -> singleton(DataPropertyRange(dp, DataUnionOf(disj1, disj2))),
                //
                () -> Sets.newHashSet(HasKey(singleton(Annotation(apropA, Literal("Test", ""))), clsA, propA, propB, propC),
                        Declaration(apropA), Declaration(propA), Declaration(propB), Declaration(propC)),
                // 20:
                () -> singleton(DisjointClasses(Stream.generate(OWLFunctionalSyntaxFactory::createClass).limit(1000).collect(Collectors.toSet()))),
                // 21:
                () -> singleton(SubClassOf(clsB, ObjectSomeValuesFrom(op.getInverseProperty(), clsA))),
                // 22:
                () -> singleton(SubDataPropertyOf(dp, dq)),
                // 23:
                () -> singleton(DataPropertyAssertion(dp, ind, Literal(33.3))),
                // 24:
                () -> Sets.newHashSet(NegativeDataPropertyAssertion(dp, ind, Literal(33.3)), NegativeDataPropertyAssertion(
                        dp, ind, Literal("weasel", "")), NegativeDataPropertyAssertion(dp, ind, Literal("weasel"))),
                // 25:
                () -> singleton(FunctionalDataProperty(dp)),
                // 26:
                () -> singleton(DataPropertyDomain(dp, Class(iri("A")))),
                // 27:
                () -> singleton(DataPropertyRange(dp, TopDatatype())),
                // 28:
                () -> Sets.newHashSet(DisjointDataProperties(dpA, dpB, dpC), Declaration(dpA), Declaration(dpB),
                        Declaration(dpC)),
                // 29:
                () -> singleton(DisjointDataProperties(dpA, dpB)),
                // 30:
                () -> singleton(EquivalentDataProperties(dp, dq)),
                // 31:
                () -> singleton(AsymmetricObjectProperty(op)),
                // 32:
                () -> Sets.newHashSet(DatatypeDefinition(datatype, DataComplementOf(Integer())), Declaration(datatype)),
                // 33:
                () -> Sets.newHashSet(DifferentIndividuals(ind, indj), DifferentIndividuals(ind, NamedIndividual(iri(
                        "k")))),
                // 34:
                () -> singleton(DifferentIndividuals(ind, indj, NamedIndividual(iri("k")), NamedIndividual(iri("l")))),
                // 35:
                () -> Sets.newHashSet(DisjointObjectProperties(propA, propB, propC), Declaration(propA), Declaration(propB),
                        Declaration(propC)),
                // 36:
                () -> singleton(DisjointObjectProperties(propA, propB)),
                // 37:
                () -> Sets.newHashSet(EquivalentObjectProperties(propA, propB), Declaration(propA), Declaration(propB)),
                // 38:
                () -> singleton(FunctionalObjectProperty(op)),
                // 39:
                () -> singleton(InverseFunctionalObjectProperty(op)),
                // 40:
                () -> singleton(IrreflexiveObjectProperty(op)),
                // 41:
                () -> singleton(DifferentIndividuals(Stream.generate(OWLFunctionalSyntaxFactory::createIndividual).limit(
                        1000).collect(Collectors.toSet()))),
                // 42:
                () -> Sets.newHashSet(AnnotationAssertion(apropA, clsA.getIRI(), Literal("abc", "en")), Declaration(clsA)),
                // 43:
                () -> Sets.newHashSet(AnnotationAssertion(apropA, iriA, Literal("abc", "en")), AnnotationAssertion(apropA,
                        iriA, Literal("abcd", "")), AnnotationAssertion(apropA, iriA, Literal("abcde")), AnnotationAssertion(
                        apropA, iriA, Literal("abcdef", OWL2Datatype.XSD_STRING)), Declaration(clsA)),
                // 44:
                () -> singleton(NegativeObjectPropertyAssertion(op, ind, indj)),
                // 45:
                () -> singleton(ObjectPropertyAssertion(op, ind, indj)),
                // 46:
                () -> singleton(SubPropertyChainOf(Arrays.asList(propA, propB, propC), propD, Sets.newHashSet(Annotation(
                        apropA, Literal("Test", "en")), Annotation(apropB, Literal("Test", ""))))),
                // 47:
                () -> singleton(ObjectPropertyDomain(op, clsA)),
                // 48:
                () -> singleton(ObjectPropertyRange(op, clsA)),
                // 49:
                () -> Sets.newHashSet(Declaration(Class(IRI("http://www.test.com/ontology#", "Class%37A"))), Declaration(
                        ObjectProperty(IRI("http://www.test.com/ontology#", "prop%37A")))),
                // 50:
                () -> singleton(ReflexiveObjectProperty(op)),
                // 51:
                () -> singleton(SameIndividual(ind, indj)),
                // 52:
                () -> singleton(DataPropertyAssertion(dp, ind, Literal("Test \"literal\"\nStuff"))),
                // 53:
                () -> Sets.newHashSet(DataPropertyAssertion(dp, ind, Literal("Test \"literal\"")), DataPropertyAssertion(dp,
                        ind, Literal("Test 'literal'")), DataPropertyAssertion(dp, ind, Literal("Test \"\"\"literal\"\"\""))),
                // 54:
                () -> singleton(SubObjectPropertyOf(op, oq)),
                // 55:
                () -> singleton(SymmetricObjectProperty(op)),
                // 56:
                () -> singleton(TransitiveObjectProperty(op)),
                // 57:
                () -> Sets.newHashSet(DataPropertyAssertion(dp, ind, Literal(3)), DataPropertyAssertion(dp, ind, Literal(
                        33.3)), DataPropertyAssertion(dp, ind, Literal(true)), DataPropertyAssertion(dp, ind, Literal(33.3f)),
                        DataPropertyAssertion(dp, ind, Literal("33.3")))
        };
    }
}
