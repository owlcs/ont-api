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

package com.github.owlcs.owlapi.tests.decomposition;

import com.github.owlcs.owlapi.OWLManager;
import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.OWLAPIStreamUtils;
import uk.ac.manchester.cs.atomicdecomposition.AtomicDecomposition;
import uk.ac.manchester.cs.atomicdecomposition.AtomicDecompositionImpl;
import uk.ac.manchester.cs.owlapi.modularity.ModuleType;
import uk.ac.manchester.cs.owlapi.modularity.SyntacticLocalityModuleExtractor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OldModularisationEquivalenceTestCase extends TestBase {

    public static final String KOALA = "<?xml version=\"1.0\"?>\n"
            + "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\" xmlns=\"http://protege.stanford.edu/plugins/owl/owl-library/koala.owl#\" xml:base=\"http://protege.stanford.edu/plugins/owl/owl-library/koala.owl\">\n"
            + "  <owl:Ontology rdf:about=\"\"/>\n"
            + "  <owl:Class rdf:ID=\"Female\"><owl:equivalentClass><owl:Restriction><owl:onProperty><owl:FunctionalProperty rdf:about=\"#hasGender\"/></owl:onProperty><owl:hasValue><Gender rdf:ID=\"female\"/></owl:hasValue></owl:Restriction></owl:equivalentClass></owl:Class>\n"
            + "  <owl:Class rdf:ID=\"Marsupials\"><owl:disjointWith><owl:Class rdf:about=\"#Person\"/></owl:disjointWith><rdfs:subClassOf><owl:Class rdf:about=\"#Animal\"/></rdfs:subClassOf></owl:Class>\n"
            + "  <owl:Class rdf:ID=\"Student\"><owl:equivalentClass><owl:Class><owl:intersectionOf rdf:parseType=\"Collection\"><owl:Class rdf:about=\"#Person\"/><owl:Restriction><owl:onProperty><owl:FunctionalProperty rdf:about=\"#isHardWorking\"/></owl:onProperty><owl:hasValue rdf:datatype=\"http://www.w3.org/2001/XMLSchema#boolean\">true</owl:hasValue></owl:Restriction><owl:Restriction><owl:someValuesFrom><owl:Class rdf:about=\"#University\"/></owl:someValuesFrom><owl:onProperty><owl:ObjectProperty rdf:about=\"#hasHabitat\"/></owl:onProperty></owl:Restriction></owl:intersectionOf></owl:Class></owl:equivalentClass></owl:Class>\n"
            + "  <owl:Class rdf:ID=\"KoalaWithPhD\"><owl:versionInfo>1.2</owl:versionInfo><owl:equivalentClass><owl:Class><owl:intersectionOf rdf:parseType=\"Collection\"><owl:Restriction><owl:hasValue><Degree rdf:ID=\"PhD\"/></owl:hasValue><owl:onProperty><owl:ObjectProperty rdf:about=\"#hasDegree\"/></owl:onProperty></owl:Restriction><owl:Class rdf:about=\"#Koala\"/></owl:intersectionOf></owl:Class></owl:equivalentClass></owl:Class>\n"
            + "  <owl:Class rdf:ID=\"University\"><rdfs:subClassOf><owl:Class rdf:ID=\"Habitat\"/></rdfs:subClassOf></owl:Class>\n"
            + "  <owl:Class rdf:ID=\"Koala\"><rdfs:subClassOf><owl:Restriction><owl:hasValue rdf:datatype=\"http://www.w3.org/2001/XMLSchema#boolean\">false</owl:hasValue><owl:onProperty><owl:FunctionalProperty rdf:about=\"#isHardWorking\"/></owl:onProperty></owl:Restriction></rdfs:subClassOf><rdfs:subClassOf><owl:Restriction><owl:someValuesFrom><owl:Class rdf:about=\"#DryEucalyptForest\"/></owl:someValuesFrom><owl:onProperty><owl:ObjectProperty rdf:about=\"#hasHabitat\"/></owl:onProperty></owl:Restriction></rdfs:subClassOf><rdfs:subClassOf rdf:resource=\"#Marsupials\"/></owl:Class>\n"
            + "  <owl:Class rdf:ID=\"Animal\"><rdfs:seeAlso>Male</rdfs:seeAlso><rdfs:subClassOf><owl:Restriction><owl:onProperty><owl:ObjectProperty rdf:about=\"#hasHabitat\"/></owl:onProperty><owl:minCardinality rdf:datatype=\"http://www.w3.org/2001/XMLSchema#int\">1</owl:minCardinality></owl:Restriction></rdfs:subClassOf><rdfs:subClassOf><owl:Restriction><owl:cardinality rdf:datatype=\"http://www.w3.org/2001/XMLSchema#int\">1</owl:cardinality><owl:onProperty><owl:FunctionalProperty rdf:about=\"#hasGender\"/></owl:onProperty></owl:Restriction></rdfs:subClassOf><owl:versionInfo>1.1</owl:versionInfo></owl:Class>\n"
            + "  <owl:Class rdf:ID=\"Forest\"><rdfs:subClassOf rdf:resource=\"#Habitat\"/></owl:Class>\n"
            + "  <owl:Class rdf:ID=\"Rainforest\"><rdfs:subClassOf rdf:resource=\"#Forest\"/></owl:Class>\n"
            + "  <owl:Class rdf:ID=\"GraduateStudent\"><rdfs:subClassOf><owl:Restriction><owl:onProperty><owl:ObjectProperty rdf:about=\"#hasDegree\"/></owl:onProperty><owl:someValuesFrom><owl:Class><owl:oneOf rdf:parseType=\"Collection\"><Degree rdf:ID=\"BA\"/><Degree rdf:ID=\"BS\"/></owl:oneOf></owl:Class></owl:someValuesFrom></owl:Restriction></rdfs:subClassOf><rdfs:subClassOf rdf:resource=\"#Student\"/></owl:Class>\n"
            + "  <owl:Class rdf:ID=\"Parent\"><owl:equivalentClass><owl:Class><owl:intersectionOf rdf:parseType=\"Collection\"><owl:Class rdf:about=\"#Animal\"/><owl:Restriction><owl:onProperty><owl:ObjectProperty rdf:about=\"#hasChildren\"/></owl:onProperty><owl:minCardinality rdf:datatype=\"http://www.w3.org/2001/XMLSchema#int\">1</owl:minCardinality></owl:Restriction></owl:intersectionOf></owl:Class></owl:equivalentClass><rdfs:subClassOf rdf:resource=\"#Animal\"/></owl:Class>\n"
            + "  <owl:Class rdf:ID=\"DryEucalyptForest\"><rdfs:subClassOf rdf:resource=\"#Forest\"/></owl:Class>\n"
            + "  <owl:Class rdf:ID=\"Quokka\"><rdfs:subClassOf><owl:Restriction><owl:hasValue rdf:datatype=\"http://www.w3.org/2001/XMLSchema#boolean\">true</owl:hasValue><owl:onProperty><owl:FunctionalProperty rdf:about=\"#isHardWorking\"/></owl:onProperty></owl:Restriction></rdfs:subClassOf><rdfs:subClassOf rdf:resource=\"#Marsupials\"/></owl:Class>\n"
            + "  <owl:Class rdf:ID=\"TasmanianDevil\"><rdfs:subClassOf rdf:resource=\"#Marsupials\"/></owl:Class>\n"
            + "  <owl:Class rdf:ID=\"MaleStudentWith3Daughters\"><owl:equivalentClass><owl:Class><owl:intersectionOf rdf:parseType=\"Collection\"><owl:Class rdf:about=\"#Student\"/><owl:Restriction><owl:onProperty><owl:FunctionalProperty rdf:about=\"#hasGender\"/></owl:onProperty><owl:hasValue><Gender rdf:ID=\"male\"/></owl:hasValue></owl:Restriction><owl:Restriction><owl:onProperty><owl:ObjectProperty rdf:about=\"#hasChildren\"/></owl:onProperty><owl:cardinality rdf:datatype=\"http://www.w3.org/2001/XMLSchema#int\">3</owl:cardinality></owl:Restriction><owl:Restriction><owl:allValuesFrom rdf:resource=\"#Female\"/><owl:onProperty><owl:ObjectProperty rdf:about=\"#hasChildren\"/></owl:onProperty></owl:Restriction></owl:intersectionOf></owl:Class></owl:equivalentClass></owl:Class>\n"
            + "  <owl:Class rdf:ID=\"Degree\"/>\n  <owl:Class rdf:ID=\"Gender\"/>\n"
            + "  <owl:Class rdf:ID=\"Male\"><owl:equivalentClass><owl:Restriction><owl:hasValue rdf:resource=\"#male\"/><owl:onProperty><owl:FunctionalProperty rdf:about=\"#hasGender\"/></owl:onProperty></owl:Restriction></owl:equivalentClass></owl:Class>\n"
            + "  <owl:Class rdf:ID=\"Person\"><rdfs:subClassOf rdf:resource=\"#Animal\"/><owl:disjointWith rdf:resource=\"#Marsupials\"/></owl:Class>\n"
            + "  <owl:ObjectProperty rdf:ID=\"hasHabitat\"><rdfs:range rdf:resource=\"#Habitat\"/><rdfs:domain rdf:resource=\"#Animal\"/></owl:ObjectProperty>\n"
            + "  <owl:ObjectProperty rdf:ID=\"hasDegree\"><rdfs:domain rdf:resource=\"#Person\"/><rdfs:range rdf:resource=\"#Degree\"/></owl:ObjectProperty>\n"
            + "  <owl:ObjectProperty rdf:ID=\"hasChildren\"><rdfs:range rdf:resource=\"#Animal\"/><rdfs:domain rdf:resource=\"#Animal\"/></owl:ObjectProperty>\n"
            + "  <owl:FunctionalProperty rdf:ID=\"hasGender\"><rdfs:range rdf:resource=\"#Gender\"/><rdf:type rdf:resource=\"http://www.w3.org/2002/07/owl#ObjectProperty\"/><rdfs:domain rdf:resource=\"#Animal\"/></owl:FunctionalProperty>\n"
            + "  <owl:FunctionalProperty rdf:ID=\"isHardWorking\"><rdfs:range rdf:resource=\"http://www.w3.org/2001/XMLSchema#boolean\"/><rdfs:domain rdf:resource=\"#Person\"/><rdf:type rdf:resource=\"http://www.w3.org/2002/07/owl#DatatypeProperty\"/></owl:FunctionalProperty>\n"
            + "  <Degree rdf:ID=\"MA\"/>\n</rdf:RDF>";
    private static final String ns = "http://protege.stanford.edu/plugins/owl/owl-library/koala.owl#";
    private static final OWLDataFactory f = OWLManager.getOWLDataFactory();

    private static Set<OWLEntity> l(String... s) {
        return OWLAPIStreamUtils.asSet(Stream.of(s).map(st -> f.getOWLClass(ns, st)), OWLEntity.class);
    }

    public static List<Set<OWLEntity>> params() {
        List<Set<OWLEntity>> res = new ArrayList<>();
        res.add(l("Person"));
        res.add(l("Habitat"));
        res.add(l("Forest"));
        res.add(l("Degree"));
        res.add(l("Parent"));
        res.add(l("GraduateStudent"));
        res.add(l("Rainforest"));
        res.add(l("Marsupials"));
        res.add(l("KoalaWithPhD"));
        res.add(l("TasmanianDevil"));
        res.add(l("University"));
        res.add(l("Animal"));
        res.add(l("Male"));
        res.add(l("MaleStudentWith3Daughters"));
        res.add(l("Female"));
        res.add(l("Koala"));
        res.add(l("Student"));
        res.add(l("Quokka"));
        res.add(l("Gender"));
        res.add(l("DryEucalyptForest"));
        res.add(l("GraduateStudent", "Koala", "KoalaWithPhD", "MaleStudentWith3Daughters", "Person", "Quokka",
                "Student"));
        res.add(l("DryEucalyptForest", "Forest", "Habitat", "Koala", "KoalaWithPhD", "Quokka", "Rainforest",
                "University"));
        res.add(l("DryEucalyptForest", "Forest", "Koala", "KoalaWithPhD", "Quokka", "Rainforest"));
        res.add(l("Degree", "Koala", "KoalaWithPhD", "Quokka"));
        res.add(l("Koala", "KoalaWithPhD", "MaleStudentWith3Daughters", "Parent", "Quokka"));
        res.add(l("GraduateStudent", "Koala", "KoalaWithPhD", "Quokka"));
        res.add(l("Koala", "KoalaWithPhD", "Quokka", "Rainforest"));
        res.add(l("Koala", "KoalaWithPhD", "Marsupials", "Quokka", "TasmanianDevil"));
        res.add(l("Koala", "KoalaWithPhD", "Quokka"));
        res.add(l("Koala", "KoalaWithPhD", "Quokka", "TasmanianDevil"));
        res.add(l("Koala", "KoalaWithPhD", "Quokka", "University"));
        res.add(l("Animal", "Female", "GraduateStudent", "Koala", "KoalaWithPhD", "Male", "MaleStudentWith3Daughters",
                "Marsupials", "Parent", "Person", "Quokka", "Student", "TasmanianDevil"));
        res.add(l("Koala", "KoalaWithPhD", "Male", "MaleStudentWith3Daughters", "Quokka"));
        res.add(l("Koala", "KoalaWithPhD", "MaleStudentWith3Daughters", "Quokka"));
        res.add(l("Female", "Koala", "KoalaWithPhD", "Quokka"));
        res.add(l("Koala", "KoalaWithPhD", "Quokka"));
        res.add(l("GraduateStudent", "Koala", "KoalaWithPhD", "MaleStudentWith3Daughters", "Quokka", "Student"));
        res.add(l("Koala", "KoalaWithPhD", "Quokka"));
        res.add(l("Gender", "Koala", "KoalaWithPhD", "Quokka"));
        res.add(l("DryEucalyptForest", "Koala", "KoalaWithPhD", "Quokka"));
        return res;
    }

    @ParameterizedTest
    @MethodSource("params")
    public void testModularizationWithAtomicDecompositionStar(Set<OWLEntity> signature) throws OWLException {
        testModularizationWithAtomicDecomposition(signature, ModuleType.STAR);
    }

    @ParameterizedTest
    @MethodSource("params")
    public void testModularizationWithAtomicDecompositionTop(Set<OWLEntity> signature) throws OWLException {
        testModularizationWithAtomicDecomposition(signature, ModuleType.TOP);
    }

    @ParameterizedTest
    @MethodSource("params")
    public void testModularizationWithAtomicDecompositionBottom(Set<OWLEntity> signature) throws OWLException {
        testModularizationWithAtomicDecomposition(signature, ModuleType.BOT);
    }

    private void testModularizationWithAtomicDecomposition(Set<OWLEntity> signature, ModuleType type) throws OWLException {
        OWLOntology o = m.loadOntologyFromOntologyDocument(new StringDocumentSource(KOALA));
        List<OWLAxiom> module1 = getADModule1(o, signature, type).stream().sorted().collect(Collectors.toList());
        List<OWLAxiom> module2 = getTraditionalModule(m, o, signature, type).stream()
                .filter(OWLAxiom::isLogicalAxiom).sorted().collect(Collectors.toList());
        makeAssertion(module1, module2);
    }

    protected void makeAssertion(List<OWLAxiom> module1, List<OWLAxiom> module2) {
        List<OWLAxiom> l = new ArrayList<>(module1);
        module1.removeAll(module2);
        module2.removeAll(l);
        String s1 = module1.toString().replace(ns, "");
        String s2 = module2.toString().replace(ns, "");
        if (!s1.equals(s2)) {
            LOGGER.debug("OldModularisationEquivalenceTestCase.testModularizationWithAtomicDecomposition() \n{},\n{}",
                    s1, s2);
        }
        Assertions.assertEquals(s1, s2);
    }

    protected Set<OWLAxiom> getTraditionalModule(OWLOntologyManager man, OWLOntology o, Set<OWLEntity> seedSig, ModuleType type) {
        SyntacticLocalityModuleExtractor sme = new SyntacticLocalityModuleExtractor(man, o, type);
        return sme.extract(seedSig);
    }

    protected Set<OWLAxiom> getADModule1(OWLOntology o, Set<OWLEntity> sig, ModuleType mt) {
        AtomicDecomposition ad = new AtomicDecompositionImpl(o);
        return ad.getModule(sig.stream(), false, mt).collect(Collectors.toSet());
    }
}
