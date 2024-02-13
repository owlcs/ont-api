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

package com.github.owlcs.ontapi.tests.model;

import com.github.owlcs.ontapi.DataFactory;
import com.github.owlcs.ontapi.OntFormat;
import com.github.owlcs.ontapi.OntManagers;
import com.github.owlcs.ontapi.Ontology;
import com.github.owlcs.ontapi.OntologyManager;
import com.github.owlcs.ontapi.TestOntSpecifications;
import com.github.owlcs.ontapi.testutils.OWLIOUtils;
import com.github.sszuev.jena.ontapi.model.OntClass;
import com.github.sszuev.jena.ontapi.model.OntDataRange;
import com.github.sszuev.jena.ontapi.model.OntFacetRestriction;
import com.github.sszuev.jena.ontapi.model.OntModel;
import com.github.sszuev.jena.ontapi.model.OntSWRL;
import com.github.sszuev.jena.ontapi.vocabulary.OWL;
import com.github.sszuev.jena.ontapi.vocabulary.RDF;
import com.github.sszuev.jena.ontapi.vocabulary.SWRL;
import com.github.sszuev.jena.ontapi.vocabulary.XSD;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.model.AddOntologyAnnotation;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.ClassExpressionType;
import org.semanticweb.owlapi.model.DataRangeType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.IsAnonymous;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDatatypeDefinitionAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.RemoveOntologyAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * To test {@link OWLOntology#addAxiom(OWLAxiom)} and {@link OWLOntology#removeAxiom(OWLAxiom)}.
 * <p>
 * Created by @ssz on 22.07.2019.
 */
public class ModifyAxiomsTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModifyAxiomsTest.class);

    @Test
    public void testRemoveAllAxiomsFromLoadedOntology() {
        String ns = "http://x#";
        OntologyManager m = OntManagers.createManager();
        DataFactory df = m.getOWLDataFactory();

        Ontology o = m.createOntology();
        OntModel g = o.asGraphModel();
        g.createOntClass(ns + "X").addSuperClass(g.createOntClass(ns + "Y"));
        OWLIOUtils.print(g);
        Assertions.assertEquals(4, g.size());
        Assertions.assertEquals(3, o.axioms().peek(x -> LOGGER.debug("1): {}", x)).count());

        o.remove(df.getOWLDeclarationAxiom(df.getOWLClass(ns + "X")));
        Assertions.assertEquals(2, o.axioms().peek(x -> LOGGER.debug("2): {}", x)).count());
        Assertions.assertEquals(4, g.size());

        o.remove(df.getOWLDeclarationAxiom(df.getOWLClass(ns + "Y")));
        Assertions.assertEquals(1, o.axioms().peek(x -> LOGGER.debug("3): {}", x)).count());
        Assertions.assertEquals(4, g.size());

        o.remove(df.getOWLSubClassOfAxiom(df.getOWLClass(ns + "X"), df.getOWLClass(ns + "Y")));
        Assertions.assertEquals(0, o.axioms().peek(x -> LOGGER.debug("4): {}", x)).count());
        Assertions.assertEquals(1, g.size());
    }

    @Test
    public void testAddRemoveSeveralAxioms() {
        OntologyManager m = OntManagers.createManager();
        OWLDataFactory df = m.getOWLDataFactory();
        Ontology o = m.createOntology(IRI.create("X"));

        OWLAxiom a = df.getOWLDeclarationAxiom(df.getOWLClass("x"));
        OWLAxiom b = df.getOWLSubClassOfAxiom(df.getOWLClass("x"), df.getOWLThing());
        o.add(b);
        o.add(a);
        o.remove(a);
        o.remove(b);

        OWLIOUtils.print(o);
        Assertions.assertTrue(o.isEmpty());
        Assertions.assertEquals(1, o.asGraphModel().size());
    }

    @Test
    public void testRemoveAxiomWithDirectlySharedClassExpression() {
        OntologyManager man = OntManagers.createManager();
        OWLDataFactory df = man.getOWLDataFactory();
        Ontology o = man.createOntology(IRI.create("X"));

        OntModel m = o.asGraphModel();
        OntClass ce = m.createObjectUnionOf(m.createOntClass("y"), m.createOntClass("z"));
        m.createOntClass("x").addSuperClass(ce);
        m.createOntClass("y").addSuperClass(ce);
        OWLIOUtils.print(m);
        Assertions.assertEquals(5, o.axioms().peek(a -> LOGGER.debug("1:{}", a)).count());
        Assertions.assertEquals(12, m.size());

        OWLAxiom x = o.subClassAxiomsForSubClass(df.getOWLClass("x")).findFirst().orElseThrow(AssertionError::new);
        o.remove(x);

        OWLIOUtils.print(m);
        Assertions.assertEquals(4, o.axioms().peek(a -> LOGGER.debug("2:{}", a)).count());
        Assertions.assertEquals(11, m.size());

        OWLAxiom y = o.subClassAxiomsForSubClass(df.getOWLClass("y")).findFirst().orElseThrow(AssertionError::new);
        o.remove(y);

        OWLIOUtils.print(m);
        Assertions.assertEquals(3, o.axioms().peek(a -> LOGGER.debug("3:{}", a)).count());
        Assertions.assertEquals(4, m.size());

        o.axioms(AxiomType.DECLARATION).collect(Collectors.toList()).forEach(o::remove);
        Assertions.assertTrue(o.isEmpty());
        Assertions.assertEquals(1, m.size());
    }

    @Test
    public void testRemoveAxiomWithIndirectlySharedClassExpression() {
        OntologyManager man = OntManagers.createManager();
        OWLDataFactory df = man.getOWLDataFactory();
        Ontology o = man.createOntology(IRI.create("X"));

        OntModel m = o.asGraphModel();
        OntClass ce1 = m.createObjectUnionOf(m.createOntClass("y"), m.createOntClass("z"));
        OntClass ce2 = m.createObjectAllValuesFrom(m.getOWLTopObjectProperty(), m.createObjectComplementOf(ce1));
        m.createOntClass("x").addSuperClass(ce2);
        m.createOntClass("y").addSuperClass(ce1);
        OWLIOUtils.print(m);
        Assertions.assertEquals(5, o.axioms().peek(a -> LOGGER.debug("1:{}", a)).count());
        Assertions.assertEquals(17, m.size());

        OWLAxiom x = o.subClassAxiomsForSubClass(df.getOWLClass("x")).findFirst().orElseThrow(AssertionError::new);
        o.remove(x);

        OWLIOUtils.print(m);
        Assertions.assertEquals(4, o.axioms().peek(a -> LOGGER.debug("2:{}", a)).count());
        Assertions.assertEquals(11, m.size());

        OWLAxiom y = o.subClassAxiomsForSubClass(df.getOWLClass("y")).findFirst().orElseThrow(AssertionError::new);
        o.remove(y);

        OWLIOUtils.print(m);
        Assertions.assertEquals(3, o.axioms().peek(a -> LOGGER.debug("3:{}", a)).count());
        Assertions.assertEquals(4, m.size());

        o.axioms(AxiomType.DECLARATION).collect(Collectors.toList()).forEach(o::remove);
        Assertions.assertTrue(o.isEmpty());
        Assertions.assertEquals(1, m.size());
    }

    @Test
    public void testRemoveAxiomWithSimilarClassExpression() {
        OntologyManager man = OntManagers.createManager();
        OWLDataFactory df = man.getOWLDataFactory();
        Ontology o = man.createOntology(IRI.create("X"));

        OntModel m = o.asGraphModel();
        m.createOntClass("x").addSuperClass(m.createObjectUnionOf(m.createOntClass("y"), m.createOntClass("z")));
        m.createOntClass("y").addSuperClass(m.createObjectUnionOf(m.createOntClass("y"), m.createOntClass("z")));
        OWLIOUtils.print(m);
        Assertions.assertEquals(5, o.axioms().peek(a -> LOGGER.debug("1:{}", a)).count());
        Assertions.assertEquals(18, m.size());

        OWLAxiom x = o.subClassAxiomsForSubClass(df.getOWLClass("x")).findFirst().orElseThrow(AssertionError::new);
        o.remove(x);

        OWLIOUtils.print(m);
        Assertions.assertEquals(4, o.axioms().peek(a -> LOGGER.debug("2:{}", a)).count());
        Assertions.assertEquals(11, m.size());

        OWLAxiom y = o.subClassAxiomsForSubClass(df.getOWLClass("y")).findFirst().orElseThrow(AssertionError::new);
        o.remove(y);

        OWLIOUtils.print(m);
        Assertions.assertEquals(3, o.axioms().peek(a -> LOGGER.debug("3:{}", a)).count());
        Assertions.assertEquals(4, m.size());

        o.axioms(AxiomType.DECLARATION).collect(Collectors.toList()).forEach(o::remove);
        Assertions.assertTrue(o.isEmpty());
        Assertions.assertEquals(1, m.size());
    }

    @Test
    public void testRemoveAxiomWithSharedSWRL() {
        OntologyManager man = OntManagers.createManager();
        Ontology o = man.createOntology(IRI.create("http://swrl-test"));

        OntModel m = o.asGraphModel().setNsPrefix("swrl", SWRL.NS);

        OntSWRL.Variable v = m.createSWRLVariable("v");
        OntSWRL.DArg id1 = v.as(OntSWRL.DArg.class);
        OntSWRL.IArg ia1 = v.as(OntSWRL.IArg.class);
        OntSWRL.IArg ia2 = m.createIndividual("I").as(OntSWRL.IArg.class);
        OntSWRL.Atom<?> a1 = m.createDataRangeSWRLAtom(m.getDatatype(XSD.xstring), id1);
        OntSWRL.Atom<?> a2 = m.createClassSWRLAtom(m.getOWLThing(), ia1);
        OntSWRL.Atom<?> a3 = m.createObjectPropertySWRLAtom(m.createObjectProperty("P1"), ia1, ia2);
        OntSWRL.Atom<?> a4 = m.createDataPropertySWRLAtom(m.createDataProperty("P2"), ia1, id1);

        m.createSWRLImp(Collections.emptyList(), Arrays.asList(a1, a2, a3));
        m.createSWRLImp(Arrays.asList(a2, a1, a4), Collections.emptyList());

        OWLIOUtils.print(m);
        Assertions.assertEquals(5, o.axioms().peek(x -> LOGGER.debug("1:{}", x)).count());
        Assertions.assertEquals(43, m.size());

        List<OWLAxiom> axioms = o.axioms(AxiomType.SWRL_RULE).collect(Collectors.toList());
        Assertions.assertEquals(2, axioms.size());

        o.remove(axioms.get(0));

        OWLIOUtils.print(m);
        Assertions.assertEquals(4, o.axioms().peek(x -> LOGGER.debug("2:{}", x)).count());
        Assertions.assertEquals(27, m.size());
        Assertions.assertTrue(o.containsAxiom(axioms.get(1)));

        o.clearCache();
        Assertions.assertEquals(1, o.axioms(AxiomType.SWRL_RULE).peek(x -> LOGGER.debug("3:{}", x)).count());
        Assertions.assertTrue(o.containsAxiom(axioms.get(1)));

        o.remove(axioms.get(1));

        OWLIOUtils.print(m);
        Assertions.assertEquals(3, o.axioms().peek(x -> LOGGER.debug("4:{}", x)).count());
        Assertions.assertEquals(4, m.size());

        o.axioms(AxiomType.DECLARATION).collect(Collectors.toList()).forEach(o::remove);
        Assertions.assertTrue(o.isEmpty());
        Assertions.assertEquals(1, m.size());
    }

    @Test
    public void testRemoveAxiomWithSharedDataRange() {
        OntologyManager man = OntManagers.createManager();
        Ontology o = man.createOntology(IRI.create("http://dr-test"));

        OntModel m = o.asGraphModel();
        OntFacetRestriction fr = m.createFacetRestriction(OntFacetRestriction.TotalDigits.class, m.createTypedLiteral(2));
        OntDataRange dr1 = m.createDataRestriction(m.getDatatype(XSD.positiveInteger), fr);
        OntDataRange dr2 = m.createDataRestriction(m.getDatatype(XSD.integer),
                fr, m.createFacetRestriction(OntFacetRestriction.MaxInclusive.class, m.createTypedLiteral(23)));

        m.createOntClass("C1").addDisjointClass(m.createDataSomeValuesFrom(m.createDataProperty("P1").addRange(dr1), dr2));
        m.createOntClass("C2").addDisjointUnion(m.createDataAllValuesFrom(m.createDataProperty("P2"), dr2));

        OWLIOUtils.print(m);
        Assertions.assertEquals(7, o.axioms().peek(x -> LOGGER.debug("1:{}", x)).count());
        Assertions.assertEquals(30, m.size());

        OWLAxiom a1 = o.axioms(AxiomType.DISJOINT_UNION).findFirst().orElseThrow(AssertionError::new);
        o.remove(a1);
        OWLIOUtils.print(m);
        Assertions.assertEquals(6, o.axioms().peek(x -> LOGGER.debug("2:{}", x)).count());
        Assertions.assertEquals(24, m.size());

        OWLAxiom a2 = o.axioms(AxiomType.DATA_PROPERTY_RANGE).findFirst().orElseThrow(AssertionError::new);
        o.remove(a2);
        OWLIOUtils.print(m);
        Assertions.assertEquals(5, o.axioms().peek(x -> LOGGER.debug("3:{}", x)).count());
        Assertions.assertEquals(18, m.size());

        OWLAxiom a3 = o.axioms(AxiomType.DISJOINT_CLASSES).findFirst().orElseThrow(AssertionError::new);
        o.remove(a3);
        OWLIOUtils.print(m);
        Assertions.assertEquals(4, o.axioms().peek(x -> LOGGER.debug("4:{}", x)).count());
        Assertions.assertEquals(5, m.size());

        o.axioms(AxiomType.DECLARATION).collect(Collectors.toList()).forEach(o::remove);
        Assertions.assertTrue(o.isEmpty());
        Assertions.assertEquals(1, m.size());
    }

    @Test
    public void testAddRemoveSingleAxiom() {
        OntologyManager m = OntManagers.createManager();
        OWLDataFactory df = m.getOWLDataFactory();
        Ontology o = m.createOntology(IRI.create("X"));

        OWLAxiom a = df.getOWLSubClassOfAxiom(df.getOWLClass("A"),
                df.getOWLObjectMaxCardinality(1, df.getOWLObjectProperty("P2"), df.getOWLClass("B")),
                Arrays.asList(df.getOWLAnnotation(df.getRDFSComment(), df.getOWLLiteral("comm")),
                        df.getOWLAnnotation(df.getOWLAnnotationProperty("P2"), df.getOWLLiteral(3))));

        o.add(a);
        OWLIOUtils.print(o);
        o.remove(a);
        OWLIOUtils.print(o);

        Assertions.assertTrue(o.isEmpty());
        Assertions.assertEquals(1, o.asGraphModel().size());
    }

    @Test
    public void testAddRemoveSingleHeaderAnnotation() {
        OntologyManager m = OntManagers.createManager();
        OWLDataFactory df = m.getOWLDataFactory();
        Ontology o = m.createOntology(IRI.create("X"));

        OWLAnnotation a = df.getOWLAnnotation(df.getRDFSComment(), df.getOWLLiteral("comm"),
                Arrays.asList(df.getOWLAnnotation(df.getOWLAnnotationProperty("P1"), df.getOWLLiteral(23.3)),
                        df.getOWLAnnotation(df.getOWLAnnotationProperty("P2"), df.getOWLLiteral(3))));

        m.applyChange(new AddOntologyAnnotation(o, a));
        OWLIOUtils.print(o);
        m.applyChange(new RemoveOntologyAnnotation(o, a));
        OWLIOUtils.print(o);

        Assertions.assertTrue(o.isEmpty());
        Assertions.assertEquals(1, o.asGraphModel().size());
    }

    @Test
    public void testRemoveAxiomWithBulkAnnotation() throws OWLOntologyCreationException {
        OntologyManager man = OntManagers.createManager();
        OWLOntologyDocumentSource source = OWLIOUtils.getStringDocumentSource(
                //@formatter:off
                "@prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
                "@prefix owl:   <http://www.w3.org/2002/07/owl#> .\n" +
                "@prefix xml:   <http://www.w3.org/XML/1998/namespace> .\n" +
                "@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .\n" +
                "@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .\n" +
                "\n" +
                "[ a                      owl:Axiom ;\n" +
                "  rdfs:comment           \"x\" ;\n" +
                "  owl:annotatedProperty  rdfs:subClassOf ;\n" +
                "  owl:annotatedSource    <X> ;\n" +
                "  owl:annotatedTarget    owl:Thing\n" +
                "] .\n" +
                "\n" +
                "<X>     a                owl:Class ;\n" +
                "        rdfs:subClassOf  owl:Thing .\n" +
                "\n" +
                "<http://testX>  a  owl:Ontology ."
                //@formatter:on
                , OntFormat.TURTLE);
        Ontology o = man.loadOntologyFromOntologyDocument(source);
        OWLIOUtils.print(o);
        OWLAxiom subClassOf = o.axioms(AxiomType.SUBCLASS_OF).findFirst().orElseThrow(AssertionError::new);
        OWLAxiom declaration = o.axioms(AxiomType.DECLARATION).findFirst().orElseThrow(AssertionError::new);
        LOGGER.debug("{}", subClassOf);
        o.remove(subClassOf);
        OWLIOUtils.print(o);
        Assertions.assertEquals(2, o.asGraphModel().size());
        o.remove(declaration);
        Assertions.assertEquals(1, o.asGraphModel().size());
        Assertions.assertTrue(o.isEmpty());
    }

    @Test
    public void testEquivalentClassIntersection() {
        OntologyManager man = OntManagers.createManager();
        man.getOntologyConfigurator().setSpecification(TestOntSpecifications.OWL2_FULL_NO_INF);

        Ontology o = man.createOntology(IRI.create("http://test1"));
        OntModel m = o.asGraphModel();
        OntDataRange.Named dt = m.createDatatype("X");
        OntDataRange dr = m.createDataOneOf(m.createLiteral("l"));
        dt.addEquivalentClass(dr);
        OntClass ce = dr.addProperty(RDF.type, OWL.Class).addProperty(OWL.complementOf, OWL.Thing).as(OntClass.class);
        OntClass.Named c = m.createOntClass("X");
        c.addEquivalentClassStatement(ce).annotate(m.getRDFSComment(), "x");
        OWLIOUtils.print(o);

        Assertions.assertEquals(4, o.axioms().peek(x -> LOGGER.debug("A: {}", x)).count());
        Assertions.assertEquals(15, m.statements().peek(x -> LOGGER.debug("S: {}", x)).count());
        OWLEquivalentClassesAxiom eca = o.axioms(AxiomType.EQUIVALENT_CLASSES).findFirst()
                .orElseThrow(AssertionError::new);
        OWLDatatypeDefinitionAxiom dda = o.axioms(AxiomType.DATATYPE_DEFINITION).findFirst()
                .orElseThrow(AssertionError::new);
        Assertions.assertEquals(ClassExpressionType.OBJECT_COMPLEMENT_OF,
                eca.operands().filter(IsAnonymous::isAnonymous)
                        .findFirst().orElseThrow(AssertionError::new).getClassExpressionType());
        Assertions.assertEquals(DataRangeType.DATA_ONE_OF, dda.getDataRange().getDataRangeType());

        o.remove(dda);
        OWLIOUtils.print(o);
        Assertions.assertEquals(3, o.axioms().peek(x -> LOGGER.debug("{}", x)).count());
        Assertions.assertEquals(11, m.size());

        o.remove(eca);
        OWLIOUtils.print(o);
        Assertions.assertEquals(2, o.axioms().peek(x -> LOGGER.debug("{}", x)).count());
        Assertions.assertEquals(3, m.size());
    }

    @Test
    public void testSubPropertyPunnings() {
        OntologyManager man = OntManagers.createManager();
        man.getOntologyConfigurator().setSpecification(TestOntSpecifications.OWL2_FULL_NO_INF);

        OWLDataFactory df = man.getOWLDataFactory();
        OWLDataProperty dp_a = df.getOWLDataProperty("A");
        OWLDataProperty dp_b = df.getOWLDataProperty("B");
        OWLAnnotationProperty ap_a = df.getOWLAnnotationProperty("A");
        OWLAnnotationProperty ap_b = df.getOWLAnnotationProperty("B");
        Ontology o = man.createOntology(IRI.create("http://test3"));
        o.add(df.getOWLDeclarationAxiom(dp_a));
        o.add(df.getOWLDeclarationAxiom(dp_b));
        o.add(df.getOWLDeclarationAxiom(ap_a));
        o.add(df.getOWLDeclarationAxiom(ap_b));
        o.add(df.getOWLSubDataPropertyOfAxiom(dp_a, dp_b));
        o.add(df.getOWLSubAnnotationPropertyOfAxiom(ap_a, ap_b));
        OWLIOUtils.print(o);

        Assertions.assertEquals(6, o.axioms().peek(x -> LOGGER.debug("{}", x)).count());
        Assertions.assertEquals(6, o.asGraphModel().size());

        o.removeAxiom(df.getOWLSubAnnotationPropertyOfAxiom(ap_a, ap_b));
        OWLIOUtils.print(o);
        Assertions.assertEquals(5, o.axioms().peek(x -> LOGGER.debug("{}", x)).count());
        Assertions.assertEquals(6, o.asGraphModel().size());

        o.removeAxiom(df.getOWLDeclarationAxiom(ap_a));
        OWLIOUtils.print(o);
        Assertions.assertEquals(4, o.axioms().peek(x -> LOGGER.debug("{}", x)).count());
        Assertions.assertEquals(5, o.asGraphModel().size());

        o.removeAxiom(df.getOWLDeclarationAxiom(ap_b));
        OWLIOUtils.print(o);
        Assertions.assertEquals(3, o.axioms().peek(x -> LOGGER.debug("{}", x)).count());
        Assertions.assertEquals(4, o.asGraphModel().size());
    }
}
