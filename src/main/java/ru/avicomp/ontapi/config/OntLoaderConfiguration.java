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
 */

package ru.avicomp.ontapi.config;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.Namespaces;
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.OntFormat;
import ru.avicomp.ontapi.jena.impl.conf.OntPersonality;
import ru.avicomp.ontapi.transforms.GraphTransformers;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.*;

/**
 * This is an extended {@link OWLOntologyLoaderConfiguration} with ONT-API specific settings.
 * Note: this config is immutable.
 * Used to configure loading a particular ontology to manager, different ontologies might have different load configs.
 *
 * @see OntConfig
 */
@SuppressWarnings({"WeakerAccess", "SameParameterValue", "unused"})
public class OntLoaderConfiguration extends OWLOntologyLoaderConfiguration {

    protected final Map<OntConfig.OptionSetting, Object> map = new HashMap<>();

    // WARNING: OntPersonality is not serializable!
    protected transient OntPersonality personality;
    protected GraphTransformers.Store transformers;

    public OntLoaderConfiguration(OWLOntologyLoaderConfiguration owl) {
        if (owl == null) return;
        if (owl instanceof OntLoaderConfiguration) {
            copyONTSettings((OntLoaderConfiguration) owl);
        } else {
            copyOWLSettings(owl);
        }
    }

    /**
     * the analogue of {@link OWLOntologyLoaderConfiguration#copyConfiguration()}, since the original method is private.
     *
     * @param owl to copy from.
     * @return new instance of {@link OntLoaderConfiguration}
     */
    protected OntLoaderConfiguration copy(OWLOntologyLoaderConfiguration owl) {
        return new OntLoaderConfiguration(owl);
    }

    protected Object get(OntConfig.OptionSetting key) {
        return map.getOrDefault(key, key.getDefaultValue());
    }

    protected OntLoaderConfiguration set(OntConfig.OptionSetting key, Object o) {
        if (Objects.equals(get(key), o)) return this;
        OntLoaderConfiguration copy = copy(this);
        copy.map.put(key, o);
        return copy;
    }

    protected void copyOWLSettings(OWLOntologyLoaderConfiguration conf) {
        this.map.put(OntSettings.OWL_API_LOAD_CONF_IGNORED_IMPORTS, new ArrayList<>(ignoredImports(conf)));
        this.map.put(OntSettings.OWL_API_LOAD_CONF_ACCEPT_HTTP_COMPRESSION, conf.isAcceptingHTTPCompression());
        this.map.put(OntSettings.OWL_API_LOAD_CONF_CONNECTION_TIMEOUT, conf.getConnectionTimeout());
        this.map.put(OntSettings.OWL_API_LOAD_CONF_FOLLOW_REDIRECTS, conf.isFollowRedirects());
        this.map.put(OntSettings.OWL_API_LOAD_CONF_LOAD_ANNOTATIONS, conf.isLoadAnnotationAxioms());
        this.map.put(OntSettings.OWL_API_LOAD_CONF_MISSING_IMPORT_HANDLING_STRATEGY, conf.getMissingImportHandlingStrategy());
        this.map.put(OntSettings.OWL_API_LOAD_CONF_MISSING_ONTOLOGY_HEADER_STRATEGY, conf.getMissingOntologyHeaderStrategy());
        this.map.put(OntSettings.OWL_API_LOAD_CONF_REPORT_STACK_TRACES, conf.isReportStackTrace());
        this.map.put(OntSettings.OWL_API_LOAD_CONF_RETRIES_TO_ATTEMPT, conf.getRetriesToAttempt());
        this.map.put(OntSettings.OWL_API_LOAD_CONF_PARSE_WITH_STRICT_CONFIGURATION, conf.isStrict());
        this.map.put(OntSettings.OWL_API_LOAD_CONF_TREAT_DUBLINCORE_AS_BUILTIN, conf.isTreatDublinCoreAsBuiltIn());
        this.map.put(OntSettings.OWL_API_LOAD_CONF_PRIORITY_COLLECTION_SORTING, conf.getPriorityCollectionSorting());
        this.map.put(OntSettings.OWL_API_LOAD_CONF_BANNED_PARSERS, conf.getBannedParsers());
        this.map.put(OntSettings.OWL_API_LOAD_CONF_ENTITY_EXPANSION_LIMIT, conf.getEntityExpansionLimit());
    }

    protected void copyONTSettings(OntLoaderConfiguration conf) {
        this.personality = conf.getPersonality();
        this.transformers = conf.getGraphTransformers();
        this.map.putAll(conf.map);
    }

    @SuppressWarnings("unchecked")
    protected static Set<IRI> ignoredImports(OWLOntologyLoaderConfiguration owl) {
        try {
            Field field = owl.getClass().getDeclaredField("ignoredImports");
            field.setAccessible(true);
            return (Set<IRI>) field.get(owl);
        } catch (NoSuchFieldException | IllegalAccessException | ClassCastException e) {
            throw new OntApiException("Can't get OWLOntologyLoaderConfiguration#ignoredImports.", e);
        }
    }

    /**
     * ONT-API config method.
     *
     * @return if true some graph transformations will be performed after loading graph.
     * @see #getGraphTransformers()
     * @see #setGraphTransformers(GraphTransformers.Store)
     */
    public boolean isPerformTransformation() {
        return (boolean) get(OntSettings.ONT_API_LOAD_CONF_PERFORM_TRANSFORMATIONS);
    }

    /**
     * ONT-API config setter.
     *
     * @param b if false all graph transformations will be disabled.
     * @return {@link OntLoaderConfiguration}
     * @see #getGraphTransformers()
     * @see #setGraphTransformers(GraphTransformers.Store)
     */
    public OntLoaderConfiguration setPerformTransformation(boolean b) {
        return set(OntSettings.ONT_API_LOAD_CONF_PERFORM_TRANSFORMATIONS, b);
    }

    /**
     * ONT-API config method.
     *
     * @return {@link GraphTransformers.Store} a collection with {@link GraphTransformers.Maker}s.
     * @see #isPerformTransformation()
     * @see #setPerformTransformation(boolean)
     */
    public GraphTransformers.Store getGraphTransformers() {
        return transformers == null ? transformers = OntConfig.getDefaultTransformers() : transformers;
    }

    /**
     * ONT-API config setter.
     *
     * @param t {@link GraphTransformers.Store} new graph transformers store. null means the system defaults
     * @return {@link OntLoaderConfiguration} new instance.
     * @see OntLoaderConfiguration#isPerformTransformation()
     * @see OntLoaderConfiguration#setPerformTransformation(boolean)
     */
    public OntLoaderConfiguration setGraphTransformers(GraphTransformers.Store t) {
        if (Objects.equals(transformers, t)) return this;
        OntLoaderConfiguration res = copy(this);
        res.transformers = t;
        return res;
    }

    /**
     * ONT-API config method.
     * Note: after deserialization it is always default.
     *
     * @return the {@link org.apache.jena.enhanced.Personality}, if null then default ({@link OntConfig#getDefaultPersonality()})
     */
    public OntPersonality getPersonality() {
        return personality == null ? personality = OntConfig.getDefaultPersonality() : personality;
    }

    /**
     * ONT-API config setter.
     *
     * @param p {@link OntPersonality} new personality. Null means default ({@link OntConfig#getDefaultPersonality()})
     * @return {@link OntLoaderConfiguration}
     */
    public OntLoaderConfiguration setPersonality(OntPersonality p) {
        if (Objects.equals(personality, p)) return this;
        OntLoaderConfiguration res = copy(this);
        res.personality = p;
        return res;
    }

    /**
     * ONT-API config method.
     *
     * @return Set of {@link OntConfig.Scheme}
     */
    @SuppressWarnings("unchecked")
    public List<OntConfig.Scheme> getSupportedSchemes() {
        return (List<OntConfig.Scheme>) get(OntSettings.ONT_API_LOAD_CONF_SUPPORTED_SCHEMES);
    }

    /**
     * ONT-API config setter.
     *
     * @param schemes the collection of {@link OntConfig.Scheme}
     * @return {@link OntLoaderConfiguration}
     */
    public OntLoaderConfiguration setSupportedSchemes(List<OntConfig.Scheme> schemes) {
        List<OntConfig.Scheme> res = schemes instanceof Serializable ? schemes : new ArrayList<>(schemes);
        return set(OntSettings.ONT_API_LOAD_CONF_SUPPORTED_SCHEMES, res);
    }

    /**
     * ONT-API config method.
     * The additional to the {@link #isLoadAnnotationAxioms()} optional setting to manage behaviour of annotation axioms.
     * By default annotated annotation assertions are allowed.
     * See the example in the description of {@link #setAllowBulkAnnotationAssertions(boolean)}
     *
     * @return true if annotation assertions could be annotated.
     * @see #setAllowBulkAnnotationAssertions(boolean)
     * @see #setLoadAnnotationAxioms(boolean)
     * @see #isLoadAnnotationAxioms()
     */
    public boolean isAllowBulkAnnotationAssertions() {
        return (boolean) get(OntSettings.ONT_API_LOAD_CONF_ALLOW_BULK_ANNOTATION_ASSERTIONS);
    }

    /**
     * ONT-API config setter.
     * This option manages annotation assertion axioms in conjunction with declaration axioms.
     * In depends of parameter specified bulk annotations fall either into declaration or annotation assertion.
     * Consider the following example:
     * <pre>{@code
     * <http://class>   a                       owl:Class ;
     *                  rdfs:comment            "plain assertion" ;
     *                  rdfs:label              "bulk assertion" .
     * [                a                       owl:Axiom ;
     *                  rdfs:comment            "the child" ;
     *                  owl:annotatedProperty   rdfs:label ;
     *                  owl:annotatedSource     <http://class> ;
     *                  owl:annotatedTarget     "bulk assertion"
     * ] .
     * }</pre>
     * In case {@link #isAllowBulkAnnotationAssertions()} equals {@code true} this slice of graph corresponds to the following list of axioms:
     * <ul>
     * <li>AnnotationAssertion(rdfs:comment &lt;http://class&gt; "plain assertion"^^xsd:string)</li>
     * <li>AnnotationAssertion(Annotation(rdfs:comment "the child"^^xsd:string) rdfs:label &lt;http://class&gt; "bulk assertion"^^xsd:string)</li>
     * <li>Declaration(Class(&lt;http://class&gt;))</li>
     * </ul>
     * In case {@link #isAllowBulkAnnotationAssertions()} equals {@code false} there would be following axioms:
     * <ul>
     * <li>Declaration(Annotation(Annotation(rdfs:comment "the child"^^xsd:string) rdfs:label "bulk assertion"^^xsd:string) Class(&lt;http://class&gt;))</li>
     * <li>AnnotationAssertion(rdfs:comment &lt;http://class&gt; "plain assertion"^^xsd:string)</li>
     * </ul>
     * Note: the {@link org.semanticweb.owlapi.formats.ManchesterSyntaxDocumentFormat} does NOT work correctly
     * in the second case (to test try to reload ontology in manchester syntax. The loss of annotations is expected).
     *
     * @param b if false only plain annotation assertions axioms expected.
     * @return {@link OntLoaderConfiguration}
     * @see #isAllowBulkAnnotationAssertions()
     * @see #setLoadAnnotationAxioms(boolean)
     * @see #isLoadAnnotationAxioms()
     * @see OntFormat#MANCHESTER_SYNTAX
     */
    public OntLoaderConfiguration setAllowBulkAnnotationAssertions(boolean b) {
        return set(OntSettings.ONT_API_LOAD_CONF_ALLOW_BULK_ANNOTATION_ASSERTIONS, b);
    }

    /**
     * ONT-API config method.
     * By default it is {@code true}.
     * See description of {@link #setAllowReadDeclarations(boolean)}.
     *
     * @return true if declarations are allowed in the structural view
     */
    public boolean isAllowReadDeclarations() {
        return (boolean) get(OntSettings.ONT_API_LOAD_CONF_ALLOW_READ_DECLARATIONS);
    }

    /**
     * ONT-API config setter.
     * This method is invited to match OWL-API behaviour.
     * Some of the true-OWL-API parsers skips declarations on loading.
     * It seems to me incorrect.
     * You never can know whether there are declarations in the structural form or not
     * if you have the same ontology but different formats.
     * Using this method you can change global behaviour.
     *
     * @param b true to skip declarations while reading graph
     * @return new or the same instance of config.
     */
    public OntLoaderConfiguration setAllowReadDeclarations(boolean b) {
        return set(OntSettings.ONT_API_LOAD_CONF_ALLOW_READ_DECLARATIONS, b);
    }

    /**
     * ONT-API config method.
     * By default it is {@code true}.
     * See description of {@link #setIgnoreAnnotationAxiomOverlaps(boolean)}.
     *
     * @return true if possible ambiguities with annotation axioms should be ignored.
     */
    public boolean isIgnoreAnnotationAxiomOverlaps() {
        return (boolean) get(OntSettings.ONT_API_LOAD_CONF_IGNORE_ANNOTATION_AXIOM_OVERLAPS);
    }

    /**
     * ONT-API config setter.
     * Determines the behavior while reading annotation axioms
     * if there is a 'punning' entity as subject in the root statement.
     * There are three types of annotation axioms with following defining statements:
     * <ul>
     * <li>{@code A rdfs:subPropertyOf Aj}</li>
     * <li>{@code A rdfs:domain U}</li>
     * <li>{@code A rdfs:range U}</li>
     * </ul>
     * and in case 'A' is also object property ('P') or data property ('R')
     * then these statements define also corresponded object or data property axioms.
     *
     * @param b if true all such axioms will be skipped in favour of data or/and object property axioms
     * @return this or new config.
     */
    public OntLoaderConfiguration setIgnoreAnnotationAxiomOverlaps(boolean b) {
        return set(OntSettings.ONT_API_LOAD_CONF_IGNORE_ANNOTATION_AXIOM_OVERLAPS, b);
    }

    /**
     * ONT-API config method.
     * By default it is {@code false}.
     *
     * @return true if loading through Jena is disabled (the loading is done through the OWL-API mechanisms by one axiom at a time).
     */
    public boolean isUseOWLParsersToLoad() {
        return (boolean) get(OntSettings.ONT_API_LOAD_CONF_USE_OWL_PARSERS_TO_LOAD);
    }

    /**
     * ONT-API config setter.
     * To choose the preferable way to load ontology from a document source (ONT-API (Jena+OWL-API) vs pure OWL-API).
     *
     * @param b true to use pure OWL-API parsers to load.
     * @return this or new config.
     */
    public OntLoaderConfiguration setUseOWLParsersToLoad(boolean b) {
        return set(OntSettings.ONT_API_LOAD_CONF_USE_OWL_PARSERS_TO_LOAD, b);
    }

    /**
     * ONT-API config getter.
     * Answers should be throwing exception in case unable to read axioms of some type from a graph.
     * Note: it manages behaviour of a whole axiom type, not a single axiom instance.
     *
     * @return true if errors while axioms reading must be ignored
     * @see OntConfig#isIgnoreAxiomsReadErrors()
     * @since 1.1.0
     */
    public boolean isIgnoreAxiomsReadErrors() {
        return (boolean) get(OntSettings.ONT_API_LOAD_CONF_IGNORE_AXIOMS_READ_ERRORS);
    }

    /**
     * ONT-API config setter.
     * Changes 'ont.api.load.conf.ignore.axioms.read.errors' parameter.
     *
     * @param b true to ignore errors while reading axioms of some type from a graph, false to trow exception
     * @return this or new config
     * @see OntConfig#setIgnoreAxiomsReadErrors(boolean)
     * @since 1.1.0
     */
    public OntLoaderConfiguration setIgnoreAxiomsReadErrors(boolean b) {
        return set(OntSettings.ONT_API_LOAD_CONF_IGNORE_AXIOMS_READ_ERRORS, b);
    }

    /**
     * Determines whether or not annotation axioms (instances of {@code OWLAnnotationAxiom}) should be loaded.
     * By default the loading of annotation axioms is enabled.
     *
     * @return if {@code false} all annotation axioms (assertion, range and domain) will be discarded on loading.
     * @see OWLOntologyLoaderConfiguration#isLoadAnnotationAxioms()
     */
    @Override
    public boolean isLoadAnnotationAxioms() {
        return (boolean) get(OntSettings.OWL_API_LOAD_CONF_LOAD_ANNOTATIONS);
    }

    /**
     * see description for {@link #isLoadAnnotationAxioms()}
     *
     * @param b true to enable reading and writing annotation axioms
     * @return instance of new config.
     * @see OWLOntologyLoaderConfiguration#setLoadAnnotationAxioms(boolean)
     */
    @Override
    public OntLoaderConfiguration setLoadAnnotationAxioms(boolean b) {
        return set(OntSettings.OWL_API_LOAD_CONF_LOAD_ANNOTATIONS, b);
    }

    /**
     * @return List of IRIs (Strings)
     */
    @SuppressWarnings("unchecked")
    protected List<String> getIgnoredImports() {
        return (List<String>) map.computeIfAbsent(OntSettings.OWL_API_LOAD_CONF_IGNORED_IMPORTS, OntConfig.OptionSetting::getDefaultValue);
    }

    /**
     * @see OWLOntologyLoaderConfiguration#addIgnoredImport(IRI)
     */
    @Override
    public OntLoaderConfiguration addIgnoredImport(@Nonnull IRI iri) {
        if (getIgnoredImports().contains(iri.getIRIString())) {
            return this;
        }
        OntLoaderConfiguration res = copy(this);
        res.getIgnoredImports().add(iri.getIRIString());
        return res;
    }

    /**
     * @see OWLOntologyLoaderConfiguration#clearIgnoredImports()
     */
    @Override
    public OntLoaderConfiguration clearIgnoredImports() {
        if (getIgnoredImports().isEmpty()) {
            return this;
        }
        OntLoaderConfiguration res = copy(this);
        res.getIgnoredImports().clear();
        return res;
    }

    /**
     * @see OWLOntologyLoaderConfiguration#isIgnoredImport(IRI)
     */
    @Override
    public boolean isIgnoredImport(@Nonnull IRI iri) {
        return Namespaces.isDefaultIgnoredImport(iri) || getIgnoredImports().contains(iri.getIRIString());
    }

    /**
     * @see OWLOntologyLoaderConfiguration#removeIgnoredImport(IRI)
     */
    @Override
    public OntLoaderConfiguration removeIgnoredImport(@Nonnull IRI iri) {
        if (!getIgnoredImports().contains(iri.getIRIString())) {
            return this;
        }
        OntLoaderConfiguration res = copy(this);
        res.getIgnoredImports().remove(iri.getIRIString());
        return res;
    }

    /**
     * @see OWLOntologyLoaderConfiguration#getPriorityCollectionSorting()
     */
    @Override
    public PriorityCollectionSorting getPriorityCollectionSorting() {
        return (PriorityCollectionSorting) get(OntSettings.OWL_API_LOAD_CONF_PRIORITY_COLLECTION_SORTING);
    }

    /**
     * @see OWLOntologyLoaderConfiguration#setPriorityCollectionSorting(PriorityCollectionSorting)
     */
    @Override
    public OntLoaderConfiguration setPriorityCollectionSorting(PriorityCollectionSorting sorting) {
        return set(OntSettings.OWL_API_LOAD_CONF_PRIORITY_COLLECTION_SORTING, sorting);
    }

    /**
     * @see OWLOntologyLoaderConfiguration#getConnectionTimeout()
     */
    @Override
    public int getConnectionTimeout() {
        return (int) get(OntSettings.OWL_API_LOAD_CONF_CONNECTION_TIMEOUT);
    }

    /**
     * @see OWLOntologyLoaderConfiguration#setConnectionTimeout(int)
     */
    @Override
    public OntLoaderConfiguration setConnectionTimeout(int time) {
        return set(OntSettings.OWL_API_LOAD_CONF_CONNECTION_TIMEOUT, time);
    }

    /**
     * @see OWLOntologyLoaderConfiguration#getMissingImportHandlingStrategy()
     */
    @Override
    public MissingImportHandlingStrategy getMissingImportHandlingStrategy() {
        return (MissingImportHandlingStrategy) get(OntSettings.OWL_API_LOAD_CONF_MISSING_IMPORT_HANDLING_STRATEGY);
    }

    /**
     * @see OWLOntologyLoaderConfiguration#setMissingImportHandlingStrategy(MissingImportHandlingStrategy)
     */
    @Override
    public OntLoaderConfiguration setMissingImportHandlingStrategy(@Nonnull MissingImportHandlingStrategy strategy) {
        return set(OntSettings.OWL_API_LOAD_CONF_MISSING_IMPORT_HANDLING_STRATEGY, strategy);
    }

    /**
     * @see OWLOntologyLoaderConfiguration#getMissingOntologyHeaderStrategy()
     */
    @Override
    public MissingOntologyHeaderStrategy getMissingOntologyHeaderStrategy() {
        return (MissingOntologyHeaderStrategy) get(OntSettings.OWL_API_LOAD_CONF_MISSING_ONTOLOGY_HEADER_STRATEGY);
    }

    /**
     * @see OWLOntologyLoaderConfiguration#setMissingOntologyHeaderStrategy(MissingOntologyHeaderStrategy)
     */
    @Override
    public OntLoaderConfiguration setMissingOntologyHeaderStrategy(@Nonnull MissingOntologyHeaderStrategy strategy) {
        return set(OntSettings.OWL_API_LOAD_CONF_MISSING_ONTOLOGY_HEADER_STRATEGY, strategy);
    }

    /**
     * @see OWLOntologyLoaderConfiguration#getRetriesToAttempt()
     */
    @Override
    public int getRetriesToAttempt() {
        return (int) get(OntSettings.OWL_API_LOAD_CONF_RETRIES_TO_ATTEMPT);
    }

    /**
     * @see OWLOntologyLoaderConfiguration#setRetriesToAttempt(int)
     */
    @Override
    public OntLoaderConfiguration setRetriesToAttempt(int retries) {
        return set(OntSettings.OWL_API_LOAD_CONF_RETRIES_TO_ATTEMPT, retries);
    }

    /**
     * @see OWLOntologyLoaderConfiguration#isAcceptingHTTPCompression()
     */
    @Override
    public boolean isAcceptingHTTPCompression() {
        return (boolean) get(OntSettings.OWL_API_LOAD_CONF_ACCEPT_HTTP_COMPRESSION);
    }

    /**
     * @see OWLOntologyLoaderConfiguration#setAcceptingHTTPCompression(boolean)
     */
    @Override
    public OntLoaderConfiguration setAcceptingHTTPCompression(boolean b) {
        return set(OntSettings.OWL_API_LOAD_CONF_ACCEPT_HTTP_COMPRESSION, b);
    }

    /**
     * @see OWLOntologyLoaderConfiguration#isFollowRedirects()
     */
    @Override
    public boolean isFollowRedirects() {
        return (boolean) get(OntSettings.OWL_API_LOAD_CONF_FOLLOW_REDIRECTS);
    }

    /**
     * @see OWLOntologyLoaderConfiguration#setFollowRedirects(boolean)
     */
    @Override
    public OntLoaderConfiguration setFollowRedirects(boolean b) {
        return set(OntSettings.OWL_API_LOAD_CONF_FOLLOW_REDIRECTS, b);
    }

    /**
     * @see OWLOntologyLoaderConfiguration#isReportStackTrace()
     */
    @Override
    public boolean isReportStackTrace() {
        return (boolean) get(OntSettings.OWL_API_LOAD_CONF_REPORT_STACK_TRACES);
    }

    /**
     * @see OWLOntologyLoaderConfiguration#setReportStackTraces(boolean)
     */
    @Override
    public OntLoaderConfiguration setReportStackTraces(boolean b) {
        return set(OntSettings.OWL_API_LOAD_CONF_REPORT_STACK_TRACES, b);
    }

    /**
     * @see OWLOntologyLoaderConfiguration#isStrict()
     */
    @Override
    public boolean isStrict() {
        return (boolean) get(OntSettings.OWL_API_LOAD_CONF_PARSE_WITH_STRICT_CONFIGURATION);
    }

    /**
     * @see OWLOntologyLoaderConfiguration#setStrict(boolean)
     */
    @Override
    public OntLoaderConfiguration setStrict(boolean b) {
        return set(OntSettings.OWL_API_LOAD_CONF_PARSE_WITH_STRICT_CONFIGURATION, b);
    }

    /**
     * @see OWLOntologyLoaderConfiguration#isTreatDublinCoreAsBuiltIn()
     */
    @Override
    public boolean isTreatDublinCoreAsBuiltIn() {
        return (boolean) get(OntSettings.OWL_API_LOAD_CONF_TREAT_DUBLINCORE_AS_BUILTIN);
    }

    /**
     * @see OWLOntologyLoaderConfiguration#setTreatDublinCoreAsBuiltIn(boolean)
     */
    @Override
    public OntLoaderConfiguration setTreatDublinCoreAsBuiltIn(boolean b) {
        return set(OntSettings.OWL_API_LOAD_CONF_TREAT_DUBLINCORE_AS_BUILTIN, b);
    }

    /**
     * @see OWLOntologyLoaderConfiguration#getBannedParsers()
     */
    @Override
    public String getBannedParsers() {
        return (String) get(OntSettings.OWL_API_LOAD_CONF_BANNED_PARSERS);
    }

    /**
     * @see OWLOntologyLoaderConfiguration#setBannedParsers(String)
     */
    @Override
    public OntLoaderConfiguration setBannedParsers(@Nonnull String s) {
        return set(OntSettings.OWL_API_LOAD_CONF_BANNED_PARSERS, s);
    }

    /**
     * @see OWLOntologyLoaderConfiguration#getEntityExpansionLimit()
     */
    @Override
    public String getEntityExpansionLimit() {
        return (String) get(OntSettings.OWL_API_LOAD_CONF_ENTITY_EXPANSION_LIMIT);
    }

    /**
     * @see OWLOntologyLoaderConfiguration#setEntityExpansionLimit(String)
     */
    @Override
    public OntLoaderConfiguration setEntityExpansionLimit(@Nonnull String s) {
        return set(OntSettings.OWL_API_LOAD_CONF_ENTITY_EXPANSION_LIMIT, s);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OntLoaderConfiguration)) return false;
        OntLoaderConfiguration that = (OntLoaderConfiguration) o;
        return Objects.equals(this.getPersonality(), that.getPersonality()) &&
                Objects.equals(this.getGraphTransformers(), that.getGraphTransformers()) &&
                Objects.equals(this.map, that.map);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getPersonality(), this.getGraphTransformers(), this.map);
    }
}
