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
package com.github.owlcs.owlapi.tests.api.baseclasses;

import com.github.owlcs.ontapi.NoOpReadWriteLock;
import com.github.owlcs.ontapi.OntFormat;
import com.github.owlcs.ontapi.Ontology;
import com.github.owlcs.ontapi.OntologyModelImpl;
import com.github.owlcs.ontapi.testutils.OWLIOUtils;
import com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory;
import com.github.owlcs.owlapi.OWLManager;
import com.github.owlcs.owlapi.tests.api.anonymous.AnonymousIndividualsNormaliser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.semanticweb.owlapi.formats.PrefixDocumentFormat;
import org.semanticweb.owlapi.formats.RDFJsonLDDocumentFormat;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.io.StreamDocumentSource;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.util.OWLAPIStreamUtils;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * @author Matthew Horridge, The University Of Manchester, Bio-Health Informatics Group
 */
public abstract class TestBase {
    public static final File RESOURCES = OWLIOUtils.getResourcePath("/owlapi").toFile();
    protected static final Logger LOGGER = LoggerFactory.getLogger(TestBase.class);
    protected static final String URI_BASE = "http://www.semanticweb.org/owlapi/test";

    protected static OWLDataFactory df;
    protected static OWLOntologyManager masterManager;

    protected final OWLOntologyLoaderConfiguration config = new OWLOntologyLoaderConfiguration();
    protected OWLOntologyManager m;
    protected OWLOntologyManager m1;

    public static OWLOntologyManager createOWLManager() {
        return OWLManager.createOWLOntologyManager();
    }

    @BeforeAll
    public static void setupManagers() {
        masterManager = createOWLManager();
        df = masterManager.getOWLDataFactory();
    }

    protected static OWLOntologyManager setupManager() {
        OWLOntologyManager manager = OWLManager.newManager(df, NoOpReadWriteLock.NO_OP_RW_LOCK);
        manager.getOntologyFactories().set(masterManager.getOntologyFactories());
        manager.getOntologyParsers().set(masterManager.getOntologyParsers());
        manager.getOntologyStorers().set(masterManager.getOntologyStorers());
        manager.getIRIMappers().set(masterManager.getIRIMappers());
        manager.setOntologyConfigurator(masterManager.getOntologyConfigurator());
        return manager;
    }

    private static Set<OWLAnnotation> reannotate(Stream<OWLAnnotation> anns) {
        OWLDatatype stringType = df.getOWLDatatype(OWL2Datatype.XSD_STRING);
        Set<OWLAnnotation> toReturn = new HashSet<>();
        anns.forEach(a -> {
            Optional<OWLLiteral> asLiteral = a.getValue().asLiteral();
            if (asLiteral.isPresent() && asLiteral.get().isRDFPlainLiteral()) {
                OWLAnnotation replacement = df.getOWLAnnotation(a.getProperty(), df.getOWLLiteral(asLiteral.get()
                        .getLiteral(), stringType));
                toReturn.add(replacement);
            } else {
                toReturn.add(a);
            }
        });
        return toReturn;
    }

    private static boolean verifyErrorIsDueToBlankNodesId(Set<OWLAxiom> leftOnly, Set<OWLAxiom> rightOnly) {
        Set<String> leftOnlyStrings = new HashSet<>();
        Set<String> rightOnlyStrings = new HashSet<>();
        for (OWLAxiom ax : leftOnly) {
            leftOnlyStrings.add(replaceOWLAPIAnonIndexes(ax.toString(), "blank"));
        }
        for (OWLAxiom ax : rightOnly) {
            rightOnlyStrings.add(replaceOWLAPIAnonIndexes(ax.toString(), "blank"));
        }
        return rightOnlyStrings.equals(leftOnlyStrings);
    }

    @SuppressWarnings("SameParameterValue")
    private static String replaceOWLAPIAnonIndexes(String str, String temp) {
        return str.replaceAll("_:anon-ind-[0-9]+", "blank").replaceAll("_:genid[0-9]+", temp);
    }

    public static IRI iri(String name) {
        return OWLFunctionalSyntaxFactory.IRI(URI_BASE + '#', name);
    }

    private static OWLAxiom reannotate(OWLAxiom ax) {
        return ax.getAxiomWithoutAnnotations().getAnnotatedAxiom(reannotate(ax.annotations()));
    }

    public static boolean equal(OWLOntology ont1, OWLOntology ont2) {
        if (!ont1.isAnonymous() && !ont2.isAnonymous()) {
            Assertions.assertEquals(ont1.getOntologyID(), ont2.getOntologyID(), "Ontologies supposed to be the same");
        }
        Set<OWLAnnotation> expectedAnnotations = OWLAPIStreamUtils.asSet(ont1.annotations());
        Set<OWLAnnotation> actualAnnotations = OWLAPIStreamUtils.asSet(ont2.annotations());
        Assertions.assertEquals(expectedAnnotations, actualAnnotations);
        Set<OWLAxiom> axioms1;
        Set<OWLAxiom> axioms2;
        // This isn't great - we normalise axioms by changing the ids of
        // individuals. This relies on the fact that
        // we iterate over objects in the same order for the same set of axioms!
        axioms1 = new AnonymousIndividualsNormaliser(ont1.getOWLOntologyManager()).getNormalisedAxioms(ont1.axioms());
        axioms2 = new AnonymousIndividualsNormaliser(ont1.getOWLOntologyManager()).getNormalisedAxioms(ont2.axioms());
        OWLDocumentFormat ontologyFormat = ont2.getFormat();
        applyEquivalentsRoundtrip(axioms1, axioms2, ontologyFormat);
        PlainLiteralTypeFoldingAxiomSet a = new PlainLiteralTypeFoldingAxiomSet(axioms1);
        PlainLiteralTypeFoldingAxiomSet b = new PlainLiteralTypeFoldingAxiomSet(axioms2);
        if (a.equals(b)) {
            return true;
        }
        int counter = 0;
        StringBuilder sb = new StringBuilder();
        Set<OWLAxiom> leftOnly = new HashSet<>();
        Set<OWLAxiom> rightOnly = new HashSet<>();
        for (OWLAxiom ax : a) {
            if (b.contains(ax)) {
                continue;
            }
            if (isIgnorableAxiom(ax, false)) {
                continue;
            }
            leftOnly.add(ax);
            sb.append("Rem axiom: ").append(ax).append('\n');
            counter++;
        }
        for (OWLAxiom ax : b) {
            if (a.contains(ax)) {
                continue;
            }
            if (isIgnorableAxiom(ax, true)) {
                continue;
            }
            rightOnly.add(ax);
            sb.append("Add axiom: ").append(ax).append('\n');
            counter++;
        }
        if (counter > 0 && !rightOnly.equals(leftOnly)) {
            // a test fails on OpenJDK implementations because of ordering
            // testing here if blank node ids are the only difference
            if (verifyErrorIsDueToBlankNodesId(leftOnly, rightOnly)) {
                return true;
            }
            return Assertions.fail("roundTripOntology() Failing to match axioms: \n" + sb);
        }
        return true;
        // assertEquals(axioms1, axioms2);
    }

    /**
     * equivalent entity axioms with more than two entities are broken up by RDF syntaxes.
     * Ensure they are still recognized as correct roundtripping
     */
    private static void applyEquivalentsRoundtrip(Set<OWLAxiom> axioms1,
                                                  Set<OWLAxiom> axioms2,
                                                  OWLDocumentFormat destination) {
        if (axioms1.equals(axioms2)) return;
        // remove axioms that differ only because of n-ary equivalence axioms
        // http://www.w3.org/TR/owl2-mapping-to-rdf/#Axioms_that_are_Translated_to_Multiple_Triples
        for (OWLAxiom ax : new ArrayList<>(axioms1)) {
            if (ax instanceof OWLEquivalentClassesAxiom ax2) {
                if (ax2.classExpressions().count() > 2) {
                    Collection<OWLEquivalentClassesAxiom> pairs = ax2.splitToAnnotatedPairs();
                    if (removeIfContainsAll(axioms2, pairs, destination)) {
                        axioms1.remove(ax);
                        axioms2.removeAll(pairs);
                    }
                }
            } else if (ax instanceof OWLEquivalentDataPropertiesAxiom ax2) {
                if (ax2.properties().count() > 2) {
                    Collection<OWLEquivalentDataPropertiesAxiom> pairs = ax2.splitToAnnotatedPairs();
                    if (removeIfContainsAll(axioms2, pairs, destination)) {
                        axioms1.remove(ax);
                        axioms2.removeAll(pairs);
                    }
                }
            } else if (ax instanceof OWLEquivalentObjectPropertiesAxiom ax2) {
                if (ax2.properties().count() > 2) {
                    Collection<OWLEquivalentObjectPropertiesAxiom> pairs = ax2.splitToAnnotatedPairs();
                    if (removeIfContainsAll(axioms2, pairs, destination)) {
                        axioms1.remove(ax);
                        axioms2.removeAll(pairs);
                    }
                }
            }
        }
        if (destination instanceof RDFJsonLDDocumentFormat) {
            // other axioms can have their annotations changed to string type
            Set<OWLAxiom> reannotated1 = new HashSet<>();
            axioms1.forEach(a -> reannotated1.add(reannotate(a)));
            axioms1.clear();
            axioms1.addAll(reannotated1);
            Set<OWLAxiom> reannotated2 = new HashSet<>();
            axioms2.forEach(a -> reannotated2.add(reannotate(a)));
            axioms2.clear();
            axioms2.addAll(reannotated2);
        }
    }

    private static boolean removeIfContainsAll(Collection<OWLAxiom> axioms,
                                               Collection<? extends OWLAxiom> others,
                                               OWLDocumentFormat destination) {
        if (axioms.containsAll(others)) {
            axioms.removeAll(others);
            return true;
        }
        // some syntaxes attach xsd:string to annotation values that did not
        // have it previously
        if (!(destination instanceof RDFJsonLDDocumentFormat)) {
            return false;
        }
        Set<OWLAxiom> toRemove = new HashSet<>();
        for (OWLAxiom ax : others) {
            OWLAxiom reannotated = reannotate(ax);
            toRemove.add(reannotated);
        }
        axioms.removeAll(toRemove);
        return true;
    }

    /**
     * ignore declarations of builtins and of named individuals - named
     * individuals do not /need/ a declaration, but adding one is not an error.
     *
     * @param parse true if the axiom belongs to the parsed ones, false for the input
     * @return true if the axiom can be ignored
     */
    private static boolean isIgnorableAxiom(OWLAxiom ax, boolean parse) {
        if (!(ax instanceof OWLDeclarationAxiom d)) {
            return false;
        }
        if (parse) {
            // all extra declarations in the parsed ontology are fine
            return true;
        }
        // declarations of builtin and named individuals can be ignored
        return d.getEntity().isBuiltIn() || d.getEntity().isOWLNamedIndividual();
    }

    protected OWLOntology ontologyFromClasspathFile(String fileName) {
        return ontologyFromClasspathFile(fileName, config);
    }

    protected OWLOntology ontologyFromClasspathFile(String fileName, OWLOntologyLoaderConfiguration conf) {
        try (InputStream in = OWLIOUtils.openResourceStream("/owlapi/" + fileName)) {
            return m1.loadOntologyFromOntologyDocument(new StreamDocumentSource(in), conf);
        } catch (OWLOntologyCreationException e) {
            return Assertions.fail(e);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @BeforeEach
    public void setupManagersClean() {
        m = setupManager();
        m1 = setupManager();
    }

    public OWLOntology getOWLOntology() {
        try {
            return m.createOntology(IRI.getNextDocumentIRI(URI_BASE));
        } catch (OWLOntologyCreationException e) {
            return Assertions.fail(e);
        }
    }

    public OWLOntology getOWLOntology(IRI iri) throws OWLOntologyCreationException {
        return m.createOntology(iri);
    }

    public OWLOntology getOWLOntology(OWLOntologyID iri) throws OWLOntologyCreationException {
        return m.createOntology(iri);
    }

    protected OWLOntology getAnonymousOWLOntology() {
        try {
            return m.createOntology();
        } catch (OWLOntologyCreationException e) {
            return Assertions.fail(e);
        }
    }

    protected void roundTripOntology(OWLOntology ont) throws OWLOntologyStorageException, OWLOntologyCreationException {
        OWLDocumentFormat f = OWLManager.DEBUG_USE_OWL ?
                new RDFXMLDocumentFormat() :
                OntFormat.RDF_XML.createOwlFormat(); // 'xml' namespace is ignored by Jena as illegal (reserved)
        roundTripOntology(ont, f);
    }

    /**
     * see {@link #roundTrip(OWLOntology, OWLDocumentFormat)}.
     *
     * @param ont                    {@link OWLOntology} original ontology to test.
     * @param format                 {@link OWLDocumentFormat} to reload
     * @param recalculateAxiomsCache only for ONT-API.
     *                               if true clears the axioms cache inside specified {@link OntologyModelImpl} ontology.
     * @return {@link OWLOntology} the reloaded ontology.
     */
    @SuppressWarnings("WeakerAccess")
    protected OWLOntology roundTripOntology(OWLOntology ont, OWLDocumentFormat format, boolean recalculateAxiomsCache)
            throws OWLOntologyStorageException, OWLOntologyCreationException {
        if (!OWLManager.DEBUG_USE_OWL && recalculateAxiomsCache) {
            ((Ontology) ont).clearCache();
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Origin (source) ontology:");
            OWLIOUtils.print(ont);
            ont.axioms().forEach(a -> LOGGER.debug(a.toString()));
        }
        StringDocumentTarget target = new StringDocumentTarget();
        OWLDocumentFormat fromFormat = ont.getFormat();
        Assertions.assertNotNull(fromFormat);
        if (fromFormat.isPrefixOWLDocumentFormat() && format.isPrefixOWLDocumentFormat()) {
            PrefixDocumentFormat fromPrefixFormat = fromFormat.asPrefixOWLDocumentFormat();
            PrefixDocumentFormat toPrefixFormat = format.asPrefixOWLDocumentFormat();
            toPrefixFormat.copyPrefixesFrom(fromPrefixFormat);
            toPrefixFormat.setDefaultPrefix(null);
        }
        format.setAddMissingTypes(true);
        ont.saveOntology(format, target);
        String txt = target.toString();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Target ontology:");
            LOGGER.debug(txt);
        }
        OWLOntology ont2 = setupManager().loadOntologyFromOntologyDocument(
                new StringDocumentSource(txt, "string:ontology", format, null),
                new OWLOntologyLoaderConfiguration().setReportStackTraces(true));
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("TestBase.roundTripOntology() ontology parsed");
            ont2.axioms().forEach(ax -> LOGGER.debug(ax.toString()));
        }
        //com.github.owlcs.ontapi.utils.TestUtils.compareAxioms(ont.axioms(), ont2.axioms());
        equal(ont, ont2);
        return ont2;
    }

    /**
     * Saves the specified ontology in the specified format and reloads it.
     * Calling this method from a test will cause the test to fail if the
     * ontology could not be stored, could not be reloaded, or was reloaded and
     * the reloaded version is not equal (in terms of ontology URI and axioms)
     * with the original.
     *
     * @param ont    The ontology to be round tripped.
     * @param format The format to use when doing the round trip.
     */
    public OWLOntology roundTripOntology(OWLOntology ont, OWLDocumentFormat format)
            throws OWLOntologyStorageException, OWLOntologyCreationException {
        return roundTripOntology(ont, format, false);
    }

    protected OWLOntology loadOntologyFromString(String input) throws OWLOntologyCreationException {
        return setupManager().loadOntologyFromOntologyDocument(new StringDocumentSource(input));
    }

    protected OWLOntology loadOntologyFromString(String input, IRI i, OWLDocumentFormat f) {
        StringDocumentSource documentSource = new StringDocumentSource(input, i, f, null);
        try {
            return setupManager().loadOntologyFromOntologyDocument(documentSource);
        } catch (OWLOntologyCreationException e) {
            return Assertions.fail(e);
        }
    }

    protected OWLOntology loadOntologyFromString(StringDocumentSource input) throws OWLOntologyCreationException {
        return setupManager().loadOntologyFromOntologyDocument(input);
    }

    protected OWLOntology loadOntologyFromString(StringDocumentTarget input) throws OWLOntologyCreationException {
        return setupManager().loadOntologyFromOntologyDocument(new StringDocumentSource(input));
    }

    protected OWLOntology loadOntologyFromString(StringDocumentTarget input, OWLDocumentFormat f)
            throws OWLOntologyCreationException {
        return setupManager().loadOntologyFromOntologyDocument(new StringDocumentSource(input.toString(), "string:ontology", f, null));
    }

    protected OWLOntology loadOntologyWithConfig(StringDocumentTarget o, OWLOntologyLoaderConfiguration c)
            throws OWLOntologyCreationException {
        return loadOntologyWithConfig(new StringDocumentSource(o), c);
    }

    protected OWLOntology loadOntologyWithConfig(StringDocumentSource o, OWLOntologyLoaderConfiguration c)
            throws OWLOntologyCreationException {
        return setupManager().loadOntologyFromOntologyDocument(o, c);
    }

    protected StringDocumentTarget saveOntology(OWLOntology o) throws OWLOntologyStorageException {
        return saveOntology(o, o.getFormat());
    }

    protected StringDocumentTarget saveOntology(OWLOntology o, OWLDocumentFormat format) throws OWLOntologyStorageException {
        StringDocumentTarget t = new StringDocumentTarget();
        o.getOWLOntologyManager().saveOntology(o, format, t);
        return t;
    }

    protected OWLOntology roundTrip(OWLOntology o, OWLDocumentFormat format) throws OWLOntologyCreationException,
            OWLOntologyStorageException {
        return loadOntologyFromString(saveOntology(o, format), format);
    }

    protected OWLOntology roundTrip(OWLOntology o, OWLDocumentFormat format, OWLOntologyLoaderConfiguration c)
            throws OWLOntologyCreationException, OWLOntologyStorageException {
        return loadOntologyWithConfig(saveOntology(o, format), c);
    }

    protected OWLOntology roundTrip(OWLOntology o) throws OWLOntologyCreationException, OWLOntologyStorageException {
        return loadOntologyFromString(saveOntology(o));
    }

    @FunctionalInterface
    public interface AxiomBuilder {
        Set<OWLAxiom> build();
    }
}
