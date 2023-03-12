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
package com.github.owlcs.owlapi.tests.api.syntax;

import com.github.owlcs.ontapi.DataFactory;
import com.github.owlcs.ontapi.testutils.OWLIOUtils;
import com.github.owlcs.owlapi.OWLManager;
import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.impl.LiteralLabelFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLProperty;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import java.util.Set;
import java.util.stream.Collectors;

import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.AnnotationAssertion;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.AnnotationProperty;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.Class;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.ClassAssertion;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.DF;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.Literal;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.NamedIndividual;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.ObjectProperty;


@SuppressWarnings("javadoc")
public class TurtleTestCase extends TestBase {

    @Test
    public void testLoadingUTF8BOM() throws Exception {
        IRI uri = IRI.create(OWLIOUtils.getResourceURI("/owlapi/ttl-with-bom.ttl"));
        m.loadOntologyFromOntologyDocument(uri);
    }

    private final IRI iri = IRI.create("urn:test#", "literals");
    private final TurtleDocumentFormat tf = new TurtleDocumentFormat();
    private final IRI s = IRI.create("urn:test#", "s");

    @Test
    public void testShouldParseFixedQuotesLiterals1() throws OWLOntologyCreationException {
        OWLOntology o = loadOntologyFromString(new StringDocumentSource("<urn:test#s> <urn:test#p> ''' ''\\' ''' .",
                iri, tf, null));
        o.annotationAssertionAxioms(s).forEach(ax -> Assertions.assertEquals(" ''' ", ((OWLLiteral) ax.getValue()).getLiteral()));
    }

    @Test
    public void testShouldParseFixedQuotesLiterals2() throws OWLOntologyCreationException {
        OWLOntology o = loadOntologyFromString(new StringDocumentSource(
                "<urn:test#s> <urn:test#p> \"\"\" \"\"\\\" \"\"\" .", iri, tf, null));
        o.annotationAssertionAxioms(s).forEach(ax -> Assertions.assertEquals(" \"\"\" ", ((OWLLiteral) ax.getValue()).getLiteral()));
    }

    @Test
    public void testShouldParseFixedQuotesLiterals3() throws OWLOntologyCreationException {
        OWLOntology o = loadOntologyFromString(new StringDocumentSource(
                "<urn:test#s> <urn:test#p> \"\"\" \"\"\\u0061 \"\"\" .", iri, tf, null));
        o.annotationAssertionAxioms(s).forEach(ax -> Assertions.assertEquals(" \"\"a ", ((OWLLiteral) ax.getValue()).getLiteral()));
    }

    @Test
    public void testShouldParseFixedQuotesLiterals4() throws OWLOntologyCreationException {
        OWLOntology o = loadOntologyFromString(new StringDocumentSource(
                "<urn:test#s> <urn:test#p> \"\"\"\"\"\\\"\"\"\" .", iri, tf, null));
        o.annotationAssertionAxioms(s).forEach(ax -> Assertions.assertEquals("\"\"\"", ((OWLLiteral) ax.getValue()).getLiteral()));
    }

    @Test
    public void testShouldParseFixedQuotesLiterals5() throws OWLOntologyCreationException {
        OWLOntology o = loadOntologyFromString(new StringDocumentSource(
                "<urn:test#s> <urn:test#p> \"\"\"\"\"\\u0061\"\"\" .", iri, tf, null));
        o.annotationAssertionAxioms(s).forEach(a -> Assertions.assertEquals("\"\"a", ((OWLLiteral) a.getValue()).getLiteral()));
    }

    @Test
    public void testShouldParseOntologyThatworked() throws OWLOntologyCreationException {
        // given
        String working = "@prefix rdfs:    <http://www.w3.org/2000/01/rdf-schema#> .\n"
                + " @prefix foaf:    <http://xmlns.com/foaf/0.1/> .\n"
                + " foaf:fundedBy rdfs:isDefinedBy <http://xmlns.com/foaf/0.1/> .";
        OWLAxiom expected = AnnotationAssertion(df.getRDFSIsDefinedBy(),
                IRI.create("http://xmlns.com/foaf/0.1/", "fundedBy"),
                IRI.create("http://xmlns.com/foaf/0.1/", ""));
        // when
        OWLOntology o = loadOntologyFromString(working);
        // then
        Assertions.assertTrue(o.containsAxiom(expected));
    }

    @Test
    public void testShouldParseOntologyThatBroke() throws OWLOntologyCreationException {
        // given
        String input = "@prefix f:    <urn:test/> . f:r f:p f: .";
        OWLAxiom expected = df.getOWLAnnotationAssertionAxiom(df.getOWLAnnotationProperty("urn:test/", "p"),
                IRI.create("urn:test/", "r"), IRI.create("urn:test/", ""));
        // when
        OWLOntology o = loadOntologyFromString(input);
        // then
        Assertions.assertTrue(o.containsAxiom(expected));
    }

    @Test
    public void testShouldResolveAgainstBase() throws OWLOntologyCreationException {
        // given
        String input = "@base <http://test.org/path#> .\n <a1> <b1> <c1> .";
        // when
        OWLOntology o = loadOntologyFromString(input);
        OWLIOUtils.print(o);
        // then
        OWLAxiom axioms = o.axioms(AxiomType.ANNOTATION_ASSERTION).findFirst()
                .orElseThrow(() -> new AssertionError("Can't find annotation assertion."));
        String s = axioms.toString();
        LOGGER.debug(s);
        Assertions.assertTrue(s.contains("http://test.org/a1"));
        Assertions.assertTrue(s.contains("http://test.org/b1"));
        Assertions.assertTrue(s.contains("http://test.org/c1"));
    }

    // test for 3543488
    @Test
    public void testShouldRoundTripTurtleWithsharedBnodes() throws Exception {
        masterManager.getOntologyConfigurator().withRemapAllAnonymousIndividualsIds(false);
        try {
            String input = "@prefix ex: <http://example.com/test> .\n ex:ex1 a ex:Something ; ex:prop1 _:a ." +
                    "\n _:a a ex:Something1 ; ex:prop2 _:b .\n _:b a ex:Something ; ex:prop3 _:a .";
            OWLOntology ontology = loadOntologyFromString(input);
            OWLOntology onto2 = roundTrip(ontology, new TurtleDocumentFormat());
            equal(ontology, onto2);
        } finally {
            masterManager.getOntologyConfigurator().withRemapAllAnonymousIndividualsIds(true);
        }
    }

    // test for 335
    @Test
    public void testShouldParseScientificNotation() throws OWLOntologyCreationException {
        String input = "<http://dbpedia.org/resource/South_Africa> <http://dbpedia.org/ontology/areaTotal> 1e+07 .";
        OWLOntology ontology = loadOntologyFromString(input);
        OWLAnnotationProperty p = AnnotationProperty(IRI.create("http://dbpedia.org/ontology/", "areaTotal"));
        Assertions.assertTrue(ontology.annotationPropertiesInSignature().anyMatch(ap -> ap.equals(p)));
        IRI i = IRI.create("http://dbpedia.org/resource/", "South_Africa");
        checkParsingScientificNotation(ontology, p, i, "1e+07");
    }

    @Test
    public void testShouldParseScientificNotationWithMinus() throws OWLOntologyCreationException {
        String input = "<http://dbpedia.org/resource/South_Africa> <http://dbpedia.org/ontology/areaTotal> 1e-07 .";
        OWLOntology ontology = loadOntologyFromString(input);
        OWLAnnotationProperty p = AnnotationProperty(IRI.create("http://dbpedia.org/ontology/", "areaTotal"));
        Assertions.assertTrue(ontology.annotationPropertiesInSignature().anyMatch(ap -> ap.equals(p)));
        IRI i = IRI.create("http://dbpedia.org/resource/", "South_Africa");
        checkParsingScientificNotation(ontology, p, i, "1e-07");
    }

    private static void checkParsingScientificNotation(OWLOntology ontology, OWLAnnotationProperty p, IRI i, String lexForm) {
        // Note: the literal "'1e+07'^^xsd:double" is valid, Jena does not perform any special transformations over literals
        // Therefore here is a question: should two literals "'1e+07'^^xsd:double" and "'1.OE7'^^xsd:double" be equal or not?
        // Since OWL-API compares literals by lexical form, datatype and lang-tag, the answer is NOT.
        // That's why the original test won't work in ONT-API:
        //Assertions.assertTrue(ontology.containsAxiom(AnnotationAssertion(p, i, Literal("1.0E7", OWL2Datatype.XSD_DOUBLE))));
        OWLLiteral literal_0 = Literal(lexForm, OWL2Datatype.XSD_DOUBLE);
        OWLLiteral literal_1 = OWLManager.DEBUG_USE_OWL ? literal_0 :
                ((DataFactory) DF).getOWLLiteral(LiteralLabelFactory.create(lexForm, XSDDatatype.XSDdouble));
        Assertions.assertTrue(ontology.containsAxiom(AnnotationAssertion(p, i, literal_1)));
        OWLAnnotationAssertionAxiom a = ontology.axioms(AxiomType.ANNOTATION_ASSERTION).findFirst().orElseThrow(AssertionError::new);
        OWLLiteral literal_2 = a.getValue().asLiteral().orElseThrow(AssertionError::new);
        Assertions.assertEquals(0, literal_0.parseDouble(), literal_2.parseDouble());
    }

    @Test
    public void testShouldParseScientificNotationWithMinusFromBug() throws OWLOntologyCreationException {
        String input = "<http://www.example.com/ontologies/2014/6/medicine#m.0hycptl> "
                + "<http://www.example.com/ontologies/2014/6/medicine#medicine.drug_strength.strength_value> 8e-05 . \n"
                + "    <http://www.example.com/ontologies/2014/6/medicine#m.0hyckjg> "
                + "<http://www.example.com/ontologies/2014/6/medicine#medicine.drug_strength.strength_value> 0.03 . \n"
                + "    <http://www.example.com/ontologies/2014/6/medicine#m.0hyckjg> "
                + "<http://www.example.com/ontologies/2014/6/medicine#medicine.drug_strength.strength_value> 20.0 . \n"
                + "    <http://www.example.com/ontologies/2014/6/medicine#m.0hyckjg> "
                + "<http://www.example.com/ontologies/2014/6/medicine#medicine.drug_strength.strength_value> 30.0 . \n"
                + "    <http://www.example.com/ontologies/2014/6/medicine#m.0hyckjg> "
                + "<http://www.example.com/ontologies/2014/6/medicine#medicine.drug_strength.strength_value> 3.5 . ";
        loadOntologyFromString(input);
    }

    @Test
    public void testShouldParseTwo() throws OWLOntologyCreationException {
        String input = "<http://dbpedia.org/resource/South_Africa> <http://dbpedia.org/ontology/areaTotal> 1 .";
        OWLOntology ontology = loadOntologyFromString(input);
        OWLAnnotationProperty p = AnnotationProperty(IRI.create("http://dbpedia.org/ontology/", "areaTotal"));
        Assertions.assertTrue(ontology.annotationPropertiesInSignature().anyMatch(ap -> ap.equals(p)));
        IRI i = IRI.create("http://dbpedia.org/resource/", "South_Africa");
        Assertions.assertTrue(ontology.containsAxiom(AnnotationAssertion(p, i, Literal(1))));
    }

    @Test
    public void testShouldParseOne() throws OWLOntologyCreationException {
        String input = "<http://dbpedia.org/resource/South_Africa> <http://dbpedia.org/ontology/areaTotal> 1.0.";
        OWLOntology ontology = loadOntologyFromString(input);
        OWLAnnotationProperty p = AnnotationProperty(IRI.create("http://dbpedia.org/ontology/", "areaTotal"));
        Assertions.assertTrue(ontology.annotationPropertiesInSignature().anyMatch(ap -> ap.equals(p)));
        IRI i = IRI.create("http://dbpedia.org/resource/", "South_Africa");
        Assertions.assertTrue(ontology.containsAxiom(AnnotationAssertion(p, i, Literal("1.0", OWL2Datatype.XSD_DECIMAL))));
    }

    /**
     * ONT-API comment:
     * It seems that OWL-API has incorrect behaviour in this testcase.
     * 1) {@link org.semanticweb.owlapi.vocab.SKOSVocabulary#BROADER} is an object-property, not an annotation property.
     * 2) Even if the SKOS vocabulary is not taken into account skos:broader should be treated as object-property,
     * not an annotation-property, since the statements have individuals both as object and as subject.
     *
     * @throws OWLOntologyCreationException
     */
    @Test
    public void testShouldParseEmptySpaceInBnode() throws OWLOntologyCreationException {
        String input = "<http://taxonomy.wolterskluwer.de/practicearea/10112>\n"
                + " a <http://schema.wolterskluwer.de/TaxonomyTerm> , <http://www.w3.org/2004/02/skos/core#Concept> ;\n"
                + "      <http://www.w3.org/2004/02/skos/core#broader>\n [] ;\n"
                + "      <http://www.w3.org/2004/02/skos/core#broader>\n [] .";
        OWLOntology ontology = loadOntologyFromString(input);
        OWLIOUtils.print(ontology);
        OWLIndividual i = NamedIndividual(IRI.create("http://taxonomy.wolterskluwer.de/practicearea/10112", ""));
        OWLProperty p = OWLManager.DEBUG_USE_OWL ?
                AnnotationProperty(IRI.create("http://www.w3.org/2004/02/skos/core#", "broader")) :
                ObjectProperty(IRI.create("http://www.w3.org/2004/02/skos/core#", "broader"));
        OWLClass c = Class(IRI.create("http://www.w3.org/2004/02/skos/core#", "Concept"));
        OWLClass term = Class(IRI.create("http://schema.wolterskluwer.de/", "TaxonomyTerm"));
        Assertions.assertTrue(ontology.containsAxiom(ClassAssertion(c, i)));
        Assertions.assertTrue(ontology.containsAxiom(ClassAssertion(term, i)));
        Assertions.assertTrue(ontology.containsEntityInSignature(p));
    }

    @Test
    public void testShouldRoundTripAxiomAnnotation() throws Exception {
        masterManager.getOntologyConfigurator().withRemapAllAnonymousIndividualsIds(false);
        try {
            String input = "@prefix : <urn:fm2#> .\n" + "@prefix fm:    <urn:fm2#> .\n"
                    + "@prefix owl: <http://www.w3.org/2002/07/owl#> .\n"
                    + "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n"
                    + "@prefix xml: <http://www.w3.org/XML/1998/namespace> .\n"
                    + "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n"
                    + "@prefix prov: <urn:prov#> .\n"
                    + "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n"
                    + "@base <urn:fm2> .\n\n"
                    + "<http://www.ida.org/fm2.owl> rdf:type owl:Ontology.\n"
                    + ":prov rdf:type owl:AnnotationProperty .\n\n"
                    + ":Manage rdf:type owl:Class ; rdfs:subClassOf :ManagementType .\n"
                    + "[ rdf:type owl:Axiom ;\n"
                    + "  owl:annotatedSource :Manage ;\n"
                    + "  owl:annotatedTarget :ManagementType ;\n"
                    + "  owl:annotatedProperty rdfs:subClassOf ;\n"
                    + "  :prov [\n prov:gen :FMDomain ;\n prov:att :DM .\n ]\n ] .\n"
                    + ":ManagementType rdf:type owl:Class .\n"
                    + ":DM rdf:type owl:NamedIndividual , prov:Person .\n"
                    + ":FMDomain rdf:type owl:NamedIndividual , prov:Activity ; prov:ass :DM .";
            OWLOntology ontology = loadOntologyFromString(input);
            OWLOntology o = roundTrip(ontology, new TurtleDocumentFormat());
            equal(ontology, o);
            Set<OWLSubClassOfAxiom> axioms = o.axioms(AxiomType.SUBCLASS_OF).collect(Collectors.toSet());
            Assertions.assertEquals(1, axioms.size());
            OWLAnnotation next = axioms.iterator().next().annotations().iterator().next();
            Assertions.assertTrue(next.getValue() instanceof OWLAnonymousIndividual);
            OWLAnonymousIndividual ind = (OWLAnonymousIndividual) next.getValue();
            Set<OWLAxiom> anns = o.axioms().filter(ax -> ax.anonymousIndividuals().anyMatch(ind::equals)).collect(Collectors.toSet());
            Assertions.assertEquals(3, anns.size());
        } finally {
            masterManager.getOntologyConfigurator().withRemapAllAnonymousIndividualsIds(true);
        }
    }

    @Test
    public void testShouldRoundTripAxiomAnnotationWithSlashOntologyIRI() throws Exception {
        String input = "@prefix : <urn:test#test.owl/> .\n" + "@prefix owl: <http://www.w3.org/2002/07/owl#> .\n"
                + "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n"
                + "@prefix xml: <http://www.w3.org/XML/1998/namespace> .\n"
                + "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n"
                + "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n" + "@base <urn:test#test.owl/> .\n"
                + "<urn:test#test.owl/> rdf:type owl:Ontology .\n" + ":q rdf:type owl:Class .\n"
                + ":t rdf:type owl:Class ; rdfs:subClassOf :q .";
        OWLOntology in = loadOntologyFromString(input);
        String string = "urn:test#test.owl/";
        OWLOntology ontology = getOWLOntology(IRI.create(string, ""));
        ontology.add(df.getOWLSubClassOfAxiom(df.getOWLClass(string, "t"), df.getOWLClass(string, "q")));
        OWLOntology o = roundTrip(ontology, new TurtleDocumentFormat());
        equal(o, in);
    }

    @Test
    public void presentDeclaration() throws OWLOntologyCreationException {
        // given
        String input = "<urn:test#Settlement> rdf:type owl:Class.\n"
                + " <urn:test#fm2.owl> rdf:type owl:Ontology.\n"
                + " <urn:test#numberOfPads> rdf:type owl:ObjectProperty ;\n"
                + " rdfs:domain <urn:test#Settlement> .";
        // when
        OWLOntology o = loadOntologyFromString(input);
        // then
        o.logicalAxioms().forEach(ax -> Assertions.assertTrue(ax instanceof OWLObjectPropertyDomainAxiom));
    }

    @Test
    public void missingDeclaration() throws OWLOntologyCreationException {
        // given
        String input = "<urn:test#fm2.owl> rdf:type owl:Ontology.\n"
                + " <urn:test#numberOfPads> rdf:type owl:ObjectProperty ;\n"
                + " rdfs:domain <urn:test#Settlement> .";
        // when
        OWLOntology o = loadOntologyFromString(input);
        // then
        o.logicalAxioms().forEach(ax -> Assertions.assertTrue(ax instanceof OWLObjectPropertyDomainAxiom));
    }

    @Test
    public void testShouldReloadSamePrefixAbbreviations() throws OWLOntologyCreationException, OWLOntologyStorageException {
        String input = "@prefix : <http://www.hbp.FIXME.org/hbp_abam_ontology/> .\n"
                + "@prefix owl: <http://www.w3.org/2002/07/owl#> .\n"
                + "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n"
                + "@prefix xml: <http://www.w3.org/XML/1998/namespace> .\n"
                + "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n"
                + "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n"
                + "@prefix nsu: <http://www.FIXME.org/nsupper#> .\n"
                + "@prefix ABA: <http://api.brain-map.org/api/v2/data/Structure/> .\n"
                + "@base <http://www.hbp.FIXME.org/hbp_abam_ontology> .\n"
                + "<http://www.hbp.FIXME.org/hbp_abam_ontology> rdf:type owl:Ontology .\n"
                + "ABA:1 rdf:type owl:Class ;\n"
                + "      rdfs:subClassOf [ rdf:type owl:Restriction ; owl:onProperty nsu:part_of ; owl:someValuesFrom ABA:10 ] .\n"
                + "ABA:10 rdf:type owl:Class ;\n"
                + "       rdfs:subClassOf [ rdf:type owl:Restriction ; owl:onProperty nsu:part_of ; owl:someValuesFrom owl:Thing ] .\n";
        OWLOntology o = loadOntologyFromString(input);
        StringDocumentTarget t = saveOntology(o);
        Assertions.assertTrue(t.toString().contains("ABA:10"));
    }

    @Test
    public void testShouldFindExpectedAxiomsForBlankNodes() throws OWLOntologyCreationException {
        OWLObjectProperty r = ObjectProperty(IRI.create(
                "http://www.derivo.de/ontologies/examples/anonymous-individuals#", "r"));
        String input = "@prefix : <http://www.derivo.de/ontologies/examples/anonymous-individuals#> .\n"
                + "@prefix owl: <http://www.w3.org/2002/07/owl#> .\n"
                + "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n"
                + "@prefix xml: <http://www.w3.org/XML/1998/namespace> .\n"
                + "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n"
                + "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n"
                + "<http://www.derivo.de/ontologies/examples/anonymous-individuals> a owl:Ontology .\n"
                + ":r a owl:ObjectProperty .\n" + ":C a owl:Class .\n" + "_:genid1 a :C ; :r _:genid1 .";
        OWLOntology o = loadOntologyFromString(input);
        o.axioms(AxiomType.CLASS_ASSERTION).forEach(ax -> {
            OWLAxiom expected = df.getOWLObjectPropertyAssertionAxiom(r, ax.getIndividual(), ax.getIndividual());
            Assertions.assertTrue(o.containsAxiom(expected), expected + " not found");
        });
    }
}
