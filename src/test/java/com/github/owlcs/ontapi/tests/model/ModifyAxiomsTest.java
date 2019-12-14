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

import com.github.owlcs.ontapi.*;
import com.github.owlcs.ontapi.jena.impl.conf.OntModelConfig;
import com.github.owlcs.ontapi.jena.model.*;
import com.github.owlcs.ontapi.jena.vocabulary.OWL;
import com.github.owlcs.ontapi.jena.vocabulary.RDF;
import com.github.owlcs.ontapi.jena.vocabulary.SWRL;
import com.github.owlcs.ontapi.jena.vocabulary.XSD;
import com.github.owlcs.ontapi.utils.ReadWriteUtils;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.model.*;
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
        OntologyManager m = OntManagers.createONT();
        DataFactory df = m.getOWLDataFactory();

        Ontology o = m.createOntology();
        OntModel g = o.asGraphModel();
        g.createOntClass(ns + "X").addSuperClass(g.createOntClass(ns + "Y"));
        ReadWriteUtils.print(g);
        Assert.assertEquals(4, g.size());
        Assert.assertEquals(3, o.axioms().peek(x -> LOGGER.debug("1): {}", x)).count());

        o.remove(df.getOWLDeclarationAxiom(df.getOWLClass(ns + "X")));
        Assert.assertEquals(2, o.axioms().peek(x -> LOGGER.debug("2): {}", x)).count());
        Assert.assertEquals(4, g.size());

        o.remove(df.getOWLDeclarationAxiom(df.getOWLClass(ns + "Y")));
        Assert.assertEquals(1, o.axioms().peek(x -> LOGGER.debug("3): {}", x)).count());
        Assert.assertEquals(4, g.size());

        o.remove(df.getOWLSubClassOfAxiom(df.getOWLClass(ns + "X"), df.getOWLClass(ns + "Y")));
        Assert.assertEquals(0, o.axioms().peek(x -> LOGGER.debug("4): {}", x)).count());
        Assert.assertEquals(1, g.size());
    }

    @Test
    public void testAddRemoveSeveralAxioms() {
        OntologyManager m = OntManagers.createONT();
        OWLDataFactory df = m.getOWLDataFactory();
        Ontology o = m.createOntology(IRI.create("X"));

        OWLAxiom a = df.getOWLDeclarationAxiom(df.getOWLClass("x"));
        OWLAxiom b = df.getOWLSubClassOfAxiom(df.getOWLClass("x"), df.getOWLThing());
        o.add(b);
        o.add(a);
        o.remove(a);
        o.remove(b);

        ReadWriteUtils.print(o);
        Assert.assertTrue(o.isEmpty());
        Assert.assertEquals(1, o.asGraphModel().size());
    }

    @Test
    public void testRemoveAxiomWithDirectlySharedClassExpression() {
        OntologyManager man = OntManagers.createONT();
        OWLDataFactory df = man.getOWLDataFactory();
        Ontology o = man.createOntology(IRI.create("X"));

        OntModel m = o.asGraphModel();
        OntClass ce = m.createUnionOf(m.createOntClass("y"), m.createOntClass("z"));
        m.createOntClass("x").addSuperClass(ce);
        m.createOntClass("y").addSuperClass(ce);
        ReadWriteUtils.print(m);
        Assert.assertEquals(5, o.axioms().peek(a -> LOGGER.debug("1:{}", a)).count());
        Assert.assertEquals(12, m.size());

        OWLAxiom x = o.subClassAxiomsForSubClass(df.getOWLClass("x")).findFirst().orElseThrow(AssertionError::new);
        o.remove(x);

        ReadWriteUtils.print(m);
        Assert.assertEquals(4, o.axioms().peek(a -> LOGGER.debug("2:{}", a)).count());
        Assert.assertEquals(11, m.size());

        OWLAxiom y = o.subClassAxiomsForSubClass(df.getOWLClass("y")).findFirst().orElseThrow(AssertionError::new);
        o.remove(y);

        ReadWriteUtils.print(m);
        Assert.assertEquals(3, o.axioms().peek(a -> LOGGER.debug("3:{}", a)).count());
        Assert.assertEquals(4, m.size());

        o.axioms(AxiomType.DECLARATION).collect(Collectors.toList()).forEach(o::remove);
        Assert.assertTrue(o.isEmpty());
        Assert.assertEquals(1, m.size());
    }

    @Test
    public void testRemoveAxiomWithIndirectlySharedClassExpression() {
        OntologyManager man = OntManagers.createONT();
        OWLDataFactory df = man.getOWLDataFactory();
        Ontology o = man.createOntology(IRI.create("X"));

        OntModel m = o.asGraphModel();
        OntClass ce1 = m.createUnionOf(m.createOntClass("y"), m.createOntClass("z"));
        OntClass ce2 = m.createObjectAllValuesFrom(m.getOWLTopObjectProperty(), m.createComplementOf(ce1));
        m.createOntClass("x").addSuperClass(ce2);
        m.createOntClass("y").addSuperClass(ce1);
        ReadWriteUtils.print(m);
        Assert.assertEquals(5, o.axioms().peek(a -> LOGGER.debug("1:{}", a)).count());
        Assert.assertEquals(17, m.size());

        OWLAxiom x = o.subClassAxiomsForSubClass(df.getOWLClass("x")).findFirst().orElseThrow(AssertionError::new);
        o.remove(x);

        ReadWriteUtils.print(m);
        Assert.assertEquals(4, o.axioms().peek(a -> LOGGER.debug("2:{}", a)).count());
        Assert.assertEquals(11, m.size());

        OWLAxiom y = o.subClassAxiomsForSubClass(df.getOWLClass("y")).findFirst().orElseThrow(AssertionError::new);
        o.remove(y);

        ReadWriteUtils.print(m);
        Assert.assertEquals(3, o.axioms().peek(a -> LOGGER.debug("3:{}", a)).count());
        Assert.assertEquals(4, m.size());

        o.axioms(AxiomType.DECLARATION).collect(Collectors.toList()).forEach(o::remove);
        Assert.assertTrue(o.isEmpty());
        Assert.assertEquals(1, m.size());
    }

    @Test
    public void testRemoveAxiomWithSimilarClassExpression() {
        OntologyManager man = OntManagers.createONT();
        OWLDataFactory df = man.getOWLDataFactory();
        Ontology o = man.createOntology(IRI.create("X"));

        OntModel m = o.asGraphModel();
        m.createOntClass("x").addSuperClass(m.createUnionOf(m.createOntClass("y"), m.createOntClass("z")));
        m.createOntClass("y").addSuperClass(m.createUnionOf(m.createOntClass("y"), m.createOntClass("z")));
        ReadWriteUtils.print(m);
        Assert.assertEquals(5, o.axioms().peek(a -> LOGGER.debug("1:{}", a)).count());
        Assert.assertEquals(18, m.size());

        OWLAxiom x = o.subClassAxiomsForSubClass(df.getOWLClass("x")).findFirst().orElseThrow(AssertionError::new);
        o.remove(x);

        ReadWriteUtils.print(m);
        Assert.assertEquals(4, o.axioms().peek(a -> LOGGER.debug("2:{}", a)).count());
        Assert.assertEquals(11, m.size());

        OWLAxiom y = o.subClassAxiomsForSubClass(df.getOWLClass("y")).findFirst().orElseThrow(AssertionError::new);
        o.remove(y);

        ReadWriteUtils.print(m);
        Assert.assertEquals(3, o.axioms().peek(a -> LOGGER.debug("3:{}", a)).count());
        Assert.assertEquals(4, m.size());

        o.axioms(AxiomType.DECLARATION).collect(Collectors.toList()).forEach(o::remove);
        Assert.assertTrue(o.isEmpty());
        Assert.assertEquals(1, m.size());
    }

    @Test
    public void testRemoveAxiomWithSharedSWRL() {
        OntologyManager man = OntManagers.createONT();
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

        ReadWriteUtils.print(m);
        Assert.assertEquals(5, o.axioms().peek(x -> LOGGER.debug("1:{}", x)).count());
        Assert.assertEquals(43, m.size());

        List<OWLAxiom> axioms = o.axioms(AxiomType.SWRL_RULE).collect(Collectors.toList());
        Assert.assertEquals(2, axioms.size());

        o.remove(axioms.get(0));

        ReadWriteUtils.print(m);
        Assert.assertEquals(4, o.axioms().peek(x -> LOGGER.debug("2:{}", x)).count());
        Assert.assertEquals(27, m.size());
        Assert.assertTrue(o.containsAxiom(axioms.get(1)));

        o.clearCache();
        Assert.assertEquals(1, o.axioms(AxiomType.SWRL_RULE).peek(x -> LOGGER.debug("3:{}", x)).count());
        Assert.assertTrue(o.containsAxiom(axioms.get(1)));

        o.remove(axioms.get(1));

        ReadWriteUtils.print(m);
        Assert.assertEquals(3, o.axioms().peek(x -> LOGGER.debug("4:{}", x)).count());
        Assert.assertEquals(4, m.size());

        o.axioms(AxiomType.DECLARATION).collect(Collectors.toList()).forEach(o::remove);
        Assert.assertTrue(o.isEmpty());
        Assert.assertEquals(1, m.size());
    }

    @Test
    public void testRemoveAxiomWithSharedDataRange() {
        OntologyManager man = OntManagers.createONT();
        Ontology o = man.createOntology(IRI.create("http://dr-test"));

        OntModel m = o.asGraphModel();
        OntFacetRestriction fr = m.createFacetRestriction(OntFacetRestriction.TotalDigits.class, m.createTypedLiteral(2));
        OntDataRange dr1 = m.createRestrictionDataRange(m.getDatatype(XSD.positiveInteger), fr);
        OntDataRange dr2 = m.createRestrictionDataRange(m.getDatatype(XSD.integer),
                fr, m.createFacetRestriction(OntFacetRestriction.MaxInclusive.class, m.createTypedLiteral(23)));

        m.createOntClass("C1").addDisjointClass(m.createDataSomeValuesFrom(m.createDataProperty("P1").addRange(dr1), dr2));
        m.createOntClass("C2").addDisjointUnion(m.createDataAllValuesFrom(m.createDataProperty("P2"), dr2));

        ReadWriteUtils.print(m);
        Assert.assertEquals(7, o.axioms().peek(x -> LOGGER.debug("1:{}", x)).count());
        Assert.assertEquals(30, m.size());

        OWLAxiom a1 = o.axioms(AxiomType.DISJOINT_UNION).findFirst().orElseThrow(AssertionError::new);
        o.remove(a1);
        ReadWriteUtils.print(m);
        Assert.assertEquals(6, o.axioms().peek(x -> LOGGER.debug("2:{}", x)).count());
        Assert.assertEquals(24, m.size());

        OWLAxiom a2 = o.axioms(AxiomType.DATA_PROPERTY_RANGE).findFirst().orElseThrow(AssertionError::new);
        o.remove(a2);
        ReadWriteUtils.print(m);
        Assert.assertEquals(5, o.axioms().peek(x -> LOGGER.debug("3:{}", x)).count());
        Assert.assertEquals(18, m.size());

        OWLAxiom a3 = o.axioms(AxiomType.DISJOINT_CLASSES).findFirst().orElseThrow(AssertionError::new);
        o.remove(a3);
        ReadWriteUtils.print(m);
        Assert.assertEquals(4, o.axioms().peek(x -> LOGGER.debug("4:{}", x)).count());
        Assert.assertEquals(5, m.size());

        o.axioms(AxiomType.DECLARATION).collect(Collectors.toList()).forEach(o::remove);
        Assert.assertTrue(o.isEmpty());
        Assert.assertEquals(1, m.size());
    }

    @Test
    public void testAddRemoveSingleAxiom() {
        OntologyManager m = OntManagers.createONT();
        OWLDataFactory df = m.getOWLDataFactory();
        Ontology o = m.createOntology(IRI.create("X"));

        OWLAxiom a = df.getOWLSubClassOfAxiom(df.getOWLClass("A"),
                df.getOWLObjectMaxCardinality(1, df.getOWLObjectProperty("P2"), df.getOWLClass("B")),
                Arrays.asList(df.getOWLAnnotation(df.getRDFSComment(), df.getOWLLiteral("comm")),
                        df.getOWLAnnotation(df.getOWLAnnotationProperty("P2"), df.getOWLLiteral(3))));

        o.add(a);
        ReadWriteUtils.print(o);
        o.remove(a);
        ReadWriteUtils.print(o);

        Assert.assertTrue(o.isEmpty());
        Assert.assertEquals(1, o.asGraphModel().size());
    }

    @Test
    public void testAddRemoveSingleHeaderAnnotation() {
        OntologyManager m = OntManagers.createONT();
        OWLDataFactory df = m.getOWLDataFactory();
        Ontology o = m.createOntology(IRI.create("X"));

        OWLAnnotation a = df.getOWLAnnotation(df.getRDFSComment(), df.getOWLLiteral("comm"),
                Arrays.asList(df.getOWLAnnotation(df.getOWLAnnotationProperty("P1"), df.getOWLLiteral(23.3)),
                        df.getOWLAnnotation(df.getOWLAnnotationProperty("P2"), df.getOWLLiteral(3))));

        m.applyChange(new AddOntologyAnnotation(o, a));
        ReadWriteUtils.print(o);
        m.applyChange(new RemoveOntologyAnnotation(o, a));
        ReadWriteUtils.print(o);

        Assert.assertTrue(o.isEmpty());
        Assert.assertEquals(1, o.asGraphModel().size());
    }

    @Test
    public void testRemoveAxiomWithBulkAnnotation() throws OWLOntologyCreationException {
        OntologyManager man = OntManagers.createONT();
        OWLOntologyDocumentSource source = ReadWriteUtils.getStringDocumentSource("" +
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
                "<http://testX>  a  owl:Ontology .", OntFormat.TURTLE);
        Ontology o = man.loadOntologyFromOntologyDocument(source);
        ReadWriteUtils.print(o);
        OWLAxiom subClassOf = o.axioms(AxiomType.SUBCLASS_OF).findFirst().orElseThrow(AssertionError::new);
        OWLAxiom declaration = o.axioms(AxiomType.DECLARATION).findFirst().orElseThrow(AssertionError::new);
        LOGGER.debug("{}", subClassOf);
        o.remove(subClassOf);
        ReadWriteUtils.print(o);
        Assert.assertEquals(2, o.asGraphModel().size());
        o.remove(declaration);
        Assert.assertEquals(1, o.asGraphModel().size());
        Assert.assertTrue(o.isEmpty());
    }

    @Test
    public void testEquivalentClassIntersection() {
        OntologyManager man = OntManagers.createONT();
        man.getOntologyConfigurator().setPersonality(OntModelConfig.ONT_PERSONALITY_LAX);

        Ontology o = man.createOntology(IRI.create("http://test1"));
        OntModel m = o.asGraphModel();
        OntDataRange.Named dt = m.createDatatype("X");
        OntDataRange dr = m.createOneOfDataRange(m.createLiteral("l"));
        dt.addEquivalentClass(dr);
        OntClass ce = dr.addProperty(RDF.type, OWL.Class).addProperty(OWL.complementOf, OWL.Thing).as(OntClass.class);
        OntClass.Named c = m.createOntClass("X");
        c.addEquivalentClassStatement(ce).annotate(m.getRDFSComment(), "x");
        ReadWriteUtils.print(o);

        Assert.assertEquals(4, o.axioms().peek(x -> LOGGER.debug("A: {}", x)).count());
        Assert.assertEquals(15, m.statements().peek(x -> LOGGER.debug("S: {}", x)).count());
        OWLEquivalentClassesAxiom eca = o.axioms(AxiomType.EQUIVALENT_CLASSES).findFirst()
                .orElseThrow(AssertionError::new);
        OWLDatatypeDefinitionAxiom dda = o.axioms(AxiomType.DATATYPE_DEFINITION).findFirst()
                .orElseThrow(AssertionError::new);
        Assert.assertEquals(ClassExpressionType.OBJECT_COMPLEMENT_OF,
                eca.operands().filter(IsAnonymous::isAnonymous)
                        .findFirst().orElseThrow(AssertionError::new).getClassExpressionType());
        Assert.assertEquals(DataRangeType.DATA_ONE_OF, dda.getDataRange().getDataRangeType());

        o.remove(dda);
        ReadWriteUtils.print(o);
        Assert.assertEquals(3, o.axioms().peek(x -> LOGGER.debug("{}", x)).count());
        Assert.assertEquals(11, m.size());

        o.remove(eca);
        ReadWriteUtils.print(o);
        Assert.assertEquals(2, o.axioms().peek(x -> LOGGER.debug("{}", x)).count());
        Assert.assertEquals(3, m.size());
    }

    @Test
    public void testNegativeDataPropertyIntersection() {
        OntologyManager man = OntManagers.createONT();
        man.getOntologyConfigurator().setPersonality(OntModelConfig.ONT_PERSONALITY_LAX);
        OWLAdapter ad = OWLAdapter.get();
        OWLDataFactory df = man.getOWLDataFactory();

        Ontology o = man.createOntology(IRI.create("http://test2"));
        int system = ad.asBaseModel(o).getBase().getSystemResources(OntClass.Named.class).size();
        OntModel m = o.asGraphModel();
        m.createOntClass(OWL.NegativePropertyAssertion.getURI());
        m.createDataProperty(OWL.targetValue.getURI());
        m.createIndividual("I").addNegativeAssertion(m.createDataProperty("P"), m.createLiteral("x"));

        ReadWriteUtils.print(o);
        Assert.assertEquals(system - 1, ad.asBaseModel(o).getBase().getSystemResources(OntClass.Named.class).size());
        Assert.assertEquals(7, o.axioms().peek(x -> LOGGER.debug("{}", x)).count());
        Assert.assertEquals(9, m.size());

        OWLAxiom ndpa = o.axioms(AxiomType.NEGATIVE_DATA_PROPERTY_ASSERTION).findFirst().orElseThrow(AssertionError::new);
        OWLAxiom dpa = o.axioms(AxiomType.DATA_PROPERTY_ASSERTION).findFirst().orElseThrow(AssertionError::new);
        OWLAxiom ca = o.axioms(AxiomType.CLASS_ASSERTION).findFirst().orElseThrow(AssertionError::new);
        o.remove(ndpa);
        ReadWriteUtils.print(o);
        Assert.assertEquals(6, o.axioms().peek(x -> LOGGER.debug("{}", x)).count());
        Assert.assertEquals(7, m.size());

        o.remove(dpa);
        o.remove(ca);
        ReadWriteUtils.print(o);
        Assert.assertEquals(4, o.axioms().peek(x -> LOGGER.debug("{}", x)).count());
        Assert.assertEquals(5, m.size());

        o.remove(df.getOWLDeclarationAxiom(df.getOWLClass(OWL.NegativePropertyAssertion.getURI())));
        o.remove(df.getOWLDeclarationAxiom(df.getOWLDataProperty(OWL.targetValue.getURI())));
        ReadWriteUtils.print(o);
        Assert.assertEquals(system, ad.asBaseModel(o).getBase().getSystemResources(OntClass.Named.class).size());
        Assert.assertEquals(2, o.axioms().peek(x -> LOGGER.debug("{}", x)).count());
        Assert.assertEquals(3, m.size());

    }

    @Test
    public void testSubPropertyPunnings() {
        OntologyManager man = OntManagers.createONT();
        man.getOntologyConfigurator().setPersonality(OntModelConfig.ONT_PERSONALITY_LAX);

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
        ReadWriteUtils.print(o);

        Assert.assertEquals(6, o.axioms().peek(x -> LOGGER.debug("{}", x)).count());
        Assert.assertEquals(6, o.asGraphModel().size());

        o.removeAxiom(df.getOWLSubAnnotationPropertyOfAxiom(ap_a, ap_b));
        ReadWriteUtils.print(o);
        Assert.assertEquals(5, o.axioms().peek(x -> LOGGER.debug("{}", x)).count());
        Assert.assertEquals(6, o.asGraphModel().size());

        o.removeAxiom(df.getOWLDeclarationAxiom(ap_a));
        ReadWriteUtils.print(o);
        Assert.assertEquals(4, o.axioms().peek(x -> LOGGER.debug("{}", x)).count());
        Assert.assertEquals(5, o.asGraphModel().size());

        o.removeAxiom(df.getOWLDeclarationAxiom(ap_b));
        ReadWriteUtils.print(o);
        Assert.assertEquals(3, o.axioms().peek(x -> LOGGER.debug("{}", x)).count());
        Assert.assertEquals(4, o.asGraphModel().size());
    }
}
