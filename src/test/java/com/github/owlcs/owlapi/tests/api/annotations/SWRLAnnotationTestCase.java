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

import com.github.owlcs.ontapi.testutils.OWLIOUtils;
import com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory;
import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.apache.jena.vocabulary.SWRL;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.SWRLAtom;
import org.semanticweb.owlapi.model.SWRLVariable;
import org.semanticweb.owlapi.util.SimpleRenderer;

import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public class SWRLAnnotationTestCase extends TestBase {

    private static final String NS = "http://protege.org/ontologies/SWRLAnnotation.owl";
    private static final String HEAD = """
            <?xml version="1.0"?>
            <rdf:RDF\
             xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"\
             xmlns:protege="http://protege.stanford.edu/plugins/owl/protege#"\
             xmlns="urn:test#"\
             xmlns:xsp="http://www.owl-ontologies.com/2005/08/07/xsp.owl#"
             xmlns:owl="http://www.w3.org/2002/07/owl#"\
             xmlns:sqwrl="http://sqwrl.stanford.edu/ontologies/built-ins/3.4/sqwrl.owl#"\
             xmlns:xsd="http://www.w3.org/2001/XMLSchema#"\
             xmlns:swrl="http://www.w3.org/2003/11/swrl#"
             xmlns:swrlb="http://www.w3.org/2003/11/swrlb#"\
             xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"\
             xmlns:swrla="http://swrl.stanford.edu/ontologies/3.3/swrla.owl#"\
             xml:base="urn:test">
              <owl:Ontology rdf:about=""></owl:Ontology>
              <owl:AnnotationProperty rdf:about="http://swrl.stanford.edu/ontologies/3.3/swrla.owl#isRuleEnabled"/>
              <owl:ObjectProperty rdf:ID="hasDriver"><owl:inverseOf><owl:ObjectProperty rdf:ID="drives"/></owl:inverseOf></owl:ObjectProperty>
              <owl:ObjectProperty rdf:about="#drives"><owl:inverseOf rdf:resource="#hasDriver"/></owl:ObjectProperty>
              <swrl:Imp""";

    private static final String TAIL = """
            ><swrl:body><swrl:AtomList/></swrl:body>
                <swrl:head><swrl:AtomList><rdf:rest rdf:resource="http://www.w3.org/1999/02/22-rdf-syntax-ns#nil"/>
                    <rdf:first><swrl:IndividualPropertyAtom>
                        <swrl:argument2><Person rdf:ID="i62"/></swrl:argument2>
                        <swrl:argument1><Person rdf:ID="i61"/></swrl:argument1>
                        <swrl:propertyPredicate rdf:resource="#drives"/>
                      </swrl:IndividualPropertyAtom></rdf:first></swrl:AtomList></swrl:head>
                <swrla:isRuleEnabled rdf:datatype="http://www.w3.org/2001/XMLSchema#boolean">true</swrla:isRuleEnabled>
                <rdfs:comment rdf:datatype="http://www.w3.org/2001/XMLSchema#string">:i62, :i61</rdfs:comment></swrl:Imp>
            </rdf:RDF>""";

    protected final OWLClass a = OWLFunctionalSyntaxFactory.Class(OWLFunctionalSyntaxFactory.IRI(NS + "#", "A"));
    protected final OWLClass b = OWLFunctionalSyntaxFactory.Class(OWLFunctionalSyntaxFactory.IRI(NS + "#", "B"));
    protected OWLAxiom axiom;

    @BeforeEach
    public void setUpAtoms() {
        SWRLVariable x = df.getSWRLVariable(NS + "#", "x");
        SWRLAtom atom1 = df.getSWRLClassAtom(a, x);
        SWRLAtom atom2 = df.getSWRLClassAtom(b, x);
        Set<SWRLAtom> consequent = new TreeSet<>();
        consequent.add(atom1);
        OWLAnnotation annotation = df.getRDFSComment("Not a great rule");
        Set<OWLAnnotation> annotations = new TreeSet<>();
        annotations.add(annotation);
        Set<SWRLAtom> body = new TreeSet<>();
        body.add(atom2);
        axiom = df.getSWRLRule(body, consequent, annotations);
        m.setOntologyLoaderConfiguration(m.getOntologyLoaderConfiguration());
    }

    @Test
    public void testShouldRoundTripAnnotation() throws Exception {
        OWLOntology ontology = createOntology();
        Assertions.assertTrue(ontology.containsAxiom(axiom));
        StringDocumentTarget saveOntology = saveOntology(ontology);
        ontology = loadOntologyFromString(saveOntology);
        Assertions.assertTrue(ontology.containsAxiom(axiom));
    }

    public OWLOntology createOntology() {
        OWLOntology ontology = getOWLOntology();
        ontology.add(axiom);
        return ontology;
    }

    @Test
    public void testReplicateFailure() throws Exception {
        String input = HEAD + " rdf:ID=\"test-table5-prp-inv2-rule\"" + TAIL;
        OWLOntologyManager manager = setupManager();
        manager.setOntologyLoaderConfiguration(manager.getOntologyLoaderConfiguration().setLoadAnnotationAxioms(false));
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new StringDocumentSource(input,
                "test2test", new RDFXMLDocumentFormat(), null));
        debug(ontology);
        test(ontology);
    }

    @Test
    public void testReplicateSuccess() throws Exception {
        String input = HEAD + TAIL;
        OWLOntology ontology = setupManager().loadOntologyFromOntologyDocument(new StringDocumentSource(input,
                "test", new RDFXMLDocumentFormat(), null));
        debug(ontology);
        test(ontology);
    }

    private void debug(OWLOntology ontology) {
        LOGGER.debug("Model: ");
        OWLIOUtils.print(ontology);
        ontology.axioms().forEach(x -> LOGGER.debug(x.toString()));
    }

    private void test(OWLOntology ontology) {
        String actual = ontology.axioms(AxiomType.SWRL_RULE).map(x -> new SimpleRenderer().render(x))
                .findFirst().orElseThrow(AssertionError::new);
        Assertions.assertEquals(makeSWRLRuleAnnotatedAxiomString(ontology), actual);
    }

    private String makeSWRLRuleAnnotatedAxiomString(OWLOntology ontology) {
        OWLObjectProperty drives = ontology.objectPropertiesInSignature()
                .filter(o -> Objects.equals(o.getIRI().getRemainder().orElse(null), "drives"))
                .findFirst().orElseThrow(() -> new AssertionError("Can't find #drives"));
        OWLNamedIndividual i61 = ontology.individualsInSignature()
                .filter(o -> Objects.equals(o.getIRI().getRemainder().orElse(null), "i61"))
                .findFirst().orElseThrow(() -> new AssertionError("Can't find #i61"));
        OWLNamedIndividual i62 = ontology.individualsInSignature()
                .filter(o -> Objects.equals(o.getIRI().getRemainder().orElse(null), "i62"))
                .findFirst().orElseThrow(() -> new AssertionError("Can't find #i62"));
        // such way works both for OWL-API and ONT-API (in the OWL-API there are broken IRIs for all entities,
        // e.g. <#drives> instead of <urn:test#drives>. I presume that absolute IRI's are always correct)
        return String.format("DLSafeRule(" +
                        "Annotation(<%s> \"true\"^^xsd:boolean) " +
                        "Annotation(rdfs:comment \":i62, :i61\"^^xsd:string) " +
                        "Body() " +
                        "Head(ObjectPropertyAtom(<%s> <%s> <%s>)))",
                SWRLA.isRuleEnabled.getURI(), drives.getIRI(), i61.getIRI(), i62.getIRI());
    }

    /**
     * <a href="http://swrl.stanford.edu/ontologies/3.3/swrla.owl#">SWRLA scheme</a>
     *
     * @see SWRL
     */
    public static class SWRLA {
        public final static String URI = "http://swrl.stanford.edu/ontologies/3.3/swrla.owl";
        public final static String NS = URI + "#";

        public static final org.apache.jena.rdf.model.Property isRuleEnabled = property("isRuleEnabled");

        protected static org.apache.jena.rdf.model.Resource resource(String local) {
            return org.apache.jena.rdf.model.ResourceFactory.createResource(NS + local);
        }

        protected static org.apache.jena.rdf.model.Property property(String local) {
            return org.apache.jena.rdf.model.ResourceFactory.createProperty(NS + local);
        }
    }

}
