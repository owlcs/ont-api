/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2018, Avicomp Services, AO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 *
 */
package org.semanticweb.owlapi.profiles;

import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.api.test.baseclasses.TestBase;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.search.Searcher;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import ru.avicomp.owlapi.OWLManager;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * ONT-API WARNING:
 * This testcase has never worked before this fixing, BUT it was so designed that it always passed.
 * There are still some problems with this test:
 * 1) in the 'all.rdf' there are some broken ontologies (wrong rdf:List, invalid IRI),
 * and it is OK for OWL-API, but not for ONT-API
 * 2) there are also ontologies with web-resources in imports
 * 3) In ONT-API the transforms mechanism makes all graphs to be ontological consistent,
 * so there could not be axiom rdfs:subClassOf with missed class declaration (just for example).
 * On the other hand these ontologies still could be not DL (but always FULL).
 * Example of such violation 'Not enough operands; at least two needed' for owl:intersectionOf.
 * The all.rdf is very huge and I don't want to edit it special for ONT-API.
 * 4) OWL-API also does not always pass checking for DL.
 * By these reasons the DL checking is temporary disabled.
 *
 * @author Matthew Horridge, The University of Manchester, Information
 *         Management Group
 * @since 3.0.0
 */
@ru.avicomp.ontapi.utils.ModifiedForONTApi
@SuppressWarnings("javadoc")
public class ProfileValidationTestCase extends TestBase {
    private static final String ALL_NS = "http://www.w3.org/2007/OWL/testOntology#";
    private static final String ALL_PATH = "/owlapi/all.rdf";

    @Test
    public void testProfiles() throws Exception {
        IRI allTestURI = IRI.create(ProfileValidationTestCase.class.getResource(ALL_PATH));
        OWLOntology testCasesOntology = m.loadOntologyFromOntologyDocument(allTestURI);
        OWLClass profileIdentificationTestClass = df.getOWLClass(IRI.create(ALL_NS, "ProfileIdentificationTest"));

        OWLObjectProperty speciesProperty = df.getOWLObjectProperty(IRI.create(ALL_NS, "species"));
        OWLAnnotationProperty rdfXMLPremiseOntologyProperty = df.getOWLAnnotationProperty(IRI.create(ALL_NS, "rdfXmlPremiseOntology"));
        // new: they forgot about fs ontology:
        OWLAnnotationProperty fsPremiseOntologyProperty = df.getOWLAnnotationProperty(IRI.create(ALL_NS, "fsPremiseOntology"));

        int count = 0;
        List<OWLClassAssertionAxiom> axioms = testCasesOntology.classAssertionAxioms(profileIdentificationTestClass).collect(Collectors.toList());
        for (OWLClassAssertionAxiom ax : axioms) {
            LOGGER.debug(String.valueOf(ax));
            OWLNamedIndividual ind = ax.getIndividual().asOWLNamedIndividual();
            List<OWLLiteral> values = testCasesOntology.annotationAssertionAxioms(ind.getIRI())
                    .filter(a -> Stream.of(rdfXMLPremiseOntologyProperty, fsPremiseOntologyProperty).anyMatch(p -> p.equals(a.getProperty())))
                    .map(a -> a.getValue().asLiteral())
                    .filter(Optional::isPresent)
                    .map(Optional::get).collect(Collectors.toList());

            Assert.assertFalse("No values found", values.isEmpty());
            IRI iri = ind.asOWLNamedIndividual().getIRI();
            LOGGER.debug("{}:::IRI:::{}", ++count, iri);
            Collection<OWLIndividual> finder = Searcher.values(testCasesOntology.objectPropertyAssertionAxioms(ind), speciesProperty).collect(Collectors.toSet());
            Collection<OWLIndividual> negativeFinder = Searcher.negValues(testCasesOntology.negativeObjectPropertyAssertionAxioms(ind), speciesProperty).collect(Collectors.toSet());
            for (OWLLiteral v : values) {
                testInnerOntology(v.getLiteral(), finder, negativeFinder);
            }
        }
    }

    private static void testInnerOntology(String txt, Collection<OWLIndividual> finder, Collection<OWLIndividual> negativeFinder) throws Exception {
        OWLNamedIndividual el = df.getOWLNamedIndividual(IRI.create(ALL_NS, "EL"));
        OWLNamedIndividual ql = df.getOWLNamedIndividual(IRI.create(ALL_NS, "QL"));
        OWLNamedIndividual rl = df.getOWLNamedIndividual(IRI.create(ALL_NS, "RL"));
        OWLNamedIndividual full = df.getOWLNamedIndividual(IRI.create(ALL_NS, "FULL"));
        OWLNamedIndividual dl = df.getOWLNamedIndividual(IRI.create(ALL_NS, "DL"));
        OWLOntologyManager manager = manager();
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new StringDocumentSource(txt));
        //ru.avicomp.ontapi.utils.ReadWriteUtils.print(ontology);
        // Always FULL:
        checkProfile(ontology, new OWL2Profile(), true);
        // DL? // todo: temporary disabled both for ONT-API and OWL-API:
        /*
        if (finder.contains(dl)) {
            checkProfile(ontology, new OWL2DLProfile(), true);
        }
        if (negativeFinder.contains(dl)) {
            checkProfile(ontology, new OWL2DLProfile(), false);
        }
        */
        // EL?
        if (finder.contains(el)) {
            checkProfile(ontology, new OWL2ELProfile(), true);
        }
        if (negativeFinder.contains(el)) {
            checkProfile(ontology, new OWL2ELProfile(), false);
        }
        // QL?
        if (finder.contains(ql)) {
            checkProfile(ontology, new OWL2QLProfile(), true);
        }
        if (negativeFinder.contains(ql)) {
            checkProfile(ontology, new OWL2QLProfile(), false);
        }
        // RL?
        if (finder.contains(rl)) {
            checkProfile(ontology, new OWL2RLProfile(), true);
        }
        if (negativeFinder.contains(rl)) {
            checkProfile(ontology, new OWL2RLProfile(), false);
        }
        manager.removeOntology(ontology);
    }

    private static OWLOntologyManager manager() {
        OWLOntologyManager m = setupManager();
        m.getOntologyConfigurator().setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
        if (OWLManager.DEBUG_USE_OWL) return m;
        ru.avicomp.ontapi.config.OntLoaderConfiguration conf = ru.avicomp.ontapi.OntologyFactoryImpl.asONT(m.getOntologyLoaderConfiguration());
        m.setOntologyLoaderConfiguration(
                conf
                //.setAllowReadDeclarations(false)
                //.setPerformTransformation(false)
                        .setGraphTransformers(conf.getGraphTransformers().addFirst(ru.avicomp.ontapi.utils.WrongRDFListTransform::new))
                .setSupportedSchemes(Stream.of(ru.avicomp.ontapi.config.OntConfig.DefaultScheme.FILE).collect(Collectors.toList()))
                        .setPersonality(ru.avicomp.ontapi.jena.impl.configuration.OntModelConfig.ONT_PERSONALITY_LAX));
        return m;
    }

    private static void checkProfile(OWLOntology ontology, OWLProfile profile, boolean shouldBeInProfile) {
        OWLProfileReport report = profile.checkOntology(ontology);
        Assert.assertEquals(String.format("[%s] VIOLATIONS:\n%s", profile.getClass().getSimpleName(), report.getViolations()),
                shouldBeInProfile, report.isInProfile());
    }

    @Test
    public void shouldNotFailELBecauseOfBoolean() {
        OWLOntology o = getOWLOntology();
        OWLAnnotation ann = df.getRDFSLabel(df.getOWLLiteral(true));
        OWLAnnotationAssertionAxiom ax = df.getOWLAnnotationAssertionAxiom(IRI.create("urn:test#", "ELProfile"), ann);
        o.add(ax, df.getOWLDeclarationAxiom(OWL2Datatype.XSD_BOOLEAN.getDatatype(df)));
        checkProfile(o, new OWL2ELProfile(), true);
    }
}
