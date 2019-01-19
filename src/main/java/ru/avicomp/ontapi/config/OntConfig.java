/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2019, Avicomp Services, AO
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.jena.impl.conf.OntModelConfig;
import ru.avicomp.ontapi.jena.impl.conf.OntPersonality;
import ru.avicomp.ontapi.transforms.GraphTransformers;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

/**
 * This is the global config and also the builder for the separated load and write configs.
 * Used to manage general load/store behaviour of a manager (while immutable configs are for particular ontologies).
 * It overrides OWL-API {@link OntologyConfigurator} and provides access to the new (ONT-API) settings.
 * Note: this configuration is mutable, while load and write configs are not.
 * Additional (new) ONT-API methods:
 * <ul>
 * <li>{@link #getPersonality()} and {@link #setPersonality(OntPersonality)}</li>
 * <li>{@link #getGraphTransformers()} amd {@link #setGraphTransformers(GraphTransformers.Store)}</li>
 * <li>{@link #isPerformTransformation()} and {@link #setPerformTransformation(boolean)}</li>
 * <li>{@link #getSupportedSchemes()} and {@link #setSupportedSchemes(List)}</li>
 * <li>{@link #disableWebAccess()} (<b>since 1.1.0</b>)</li>
 * <li>{@link #isAllowReadDeclarations()} and {@link #setAllowReadDeclarations(boolean)}</li>
 * <li>{@link #isAllowBulkAnnotationAssertions()} and {@link #setAllowBulkAnnotationAssertions(boolean)}</li>
 * <li>{@link #isIgnoreAnnotationAxiomOverlaps()} and {@link #setIgnoreAnnotationAxiomOverlaps(boolean)}</li>
 * <li>{@link #isUseOWLParsersToLoad()} and {@link #setUseOWLParsersToLoad(boolean)}</li>
 * <li>{@link #isControlImports()} and {@link #setControlImports(boolean)} </li>
 * <li>{@link #isIgnoreAxiomsReadErrors()} and {@link #setIgnoreAxiomsReadErrors(boolean)} (<b>since 1.1.0</b>)</li>
 * <li>{@link #isSplitAxiomAnnotations()} and {@link #setSplitAxiomAnnotations(boolean)} (<b>since 1.3.0</b>)</li>
 * </ul>
 * <p>
 * Created by szuev on 27.02.2017.
 *
 * @see OntSettings
 * @see OntLoaderConfiguration
 * @see OntWriterConfiguration
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class OntConfig extends OntologyConfigurator {
    private static final Logger LOGGER = LoggerFactory.getLogger(OntConfig.class);
    private static final long serialVersionUID = 656765031127374396L;

    protected final Map<OptionSetting, Object> map = new HashMap<>();
    // WARNING: OntPersonality is not serializable!
    protected transient OntPersonality personality;
    protected GraphTransformers.Store transformers;

    protected Object get(OptionSetting key) {
        return key.fromMap(map);
    }

    protected OntConfig put(OptionSetting key, Object value) {
        map.put(key, value);
        return this;
    }

    /**
     * ONT-API manager load config setter.
     *
     * @param p {@link OntPersonality} the personality
     * @return this instance
     * @see OntLoaderConfiguration#setPersonality(OntPersonality)
     */
    public OntConfig setPersonality(OntPersonality p) {
        personality = OntApiException.notNull(p, "Null personality.");
        return this;
    }

    /**
     * ONT-API manager load config getter.
     *
     * @return {@link OntPersonality}
     * @see OntLoaderConfiguration#getPersonality()
     */
    public OntPersonality getPersonality() {
        return personality == null ? personality = getDefaultPersonality() : personality;
    }

    /**
     * ONT-API manager load config setter.
     *
     * @param t {@link ru.avicomp.ontapi.transforms.GraphTransformers.Store}
     * @return this instance
     * @see OntLoaderConfiguration#setGraphTransformers(GraphTransformers.Store)
     */
    public OntConfig setGraphTransformers(GraphTransformers.Store t) {
        transformers = OntApiException.notNull(t, "Null graph transformer store.");
        return this;
    }

    /**
     * ONT-API manager load config getter.
     *
     * @return {@link ru.avicomp.ontapi.transforms.GraphTransformers.Store}
     * @see OntLoaderConfiguration#getGraphTransformers()
     */
    public GraphTransformers.Store getGraphTransformers() {
        return transformers == null ? transformers = getDefaultTransformers() : transformers;
    }

    /**
     * ONT-API manager load config getter.
     *
     * @return List of supported {@link Scheme schemes}
     * @see OntLoaderConfiguration#getSupportedSchemes()
     */
    @SuppressWarnings("unchecked")
    public List<OntConfig.Scheme> getSupportedSchemes() {
        return (List<OntConfig.Scheme>) get(OntSettings.ONT_API_LOAD_CONF_SUPPORTED_SCHEMES);
    }

    /**
     * ONT-API manager load config setter.
     *
     * @param schemes List of {@link Scheme}
     * @return this instance
     * @see OntLoaderConfiguration#setSupportedSchemes(List)
     */
    public OntConfig setSupportedSchemes(List<OntConfig.Scheme> schemes) {
        return put(OntSettings.ONT_API_LOAD_CONF_SUPPORTED_SCHEMES, schemes instanceof Serializable ? schemes : new ArrayList<>(schemes));
    }

    /**
     * Disables all schemes with except 'file://' to prevent internet diving.
     *
     * @return this manager
     * @see OntConfig#setSupportedSchemes(List)
     * @since 1.1.0
     */
    public OntConfig disableWebAccess() {
        return setSupportedSchemes(Collections.singletonList(DefaultScheme.FILE));
    }

    /**
     * ONT-API manager load config getter.
     *
     * @return {@code true} if transformation is enabled
     * @see OntLoaderConfiguration#isPerformTransformation()
     * @see ru.avicomp.ontapi.transforms.Transform
     */
    public boolean isPerformTransformation() {
        return (boolean) get(OntSettings.ONT_API_LOAD_CONF_PERFORM_TRANSFORMATIONS);
    }

    /**
     * ONT-API manager load config setter.
     *
     * @param b {@code true} to enable transformation (by default it is enabled)
     * @return {@link OntConfig} this instance
     * @see OntLoaderConfiguration#setPerformTransformation(boolean)
     */
    public OntConfig setPerformTransformation(boolean b) {
        return put(OntSettings.ONT_API_LOAD_CONF_PERFORM_TRANSFORMATIONS, b);
    }

    /**
     * ONT-API manager load config getter.
     *
     * @return {@code true} if bulk annotations are allowed (it is by default)
     * @see OntLoaderConfiguration#isAllowBulkAnnotationAssertions()
     */
    public boolean isAllowBulkAnnotationAssertions() {
        return (boolean) get(OntSettings.ONT_API_LOAD_CONF_ALLOW_BULK_ANNOTATION_ASSERTIONS);
    }

    /**
     * ONT-API manager load config setter.
     *
     * @param b {@code true} to enable bulk annotations
     * @return this instance
     * @see OntLoaderConfiguration#setAllowBulkAnnotationAssertions(boolean)
     */
    public OntConfig setAllowBulkAnnotationAssertions(boolean b) {
        return put(OntSettings.ONT_API_LOAD_CONF_ALLOW_BULK_ANNOTATION_ASSERTIONS, b);
    }

    /**
     * ONT-API manager load config getter.
     *
     * @return {@code true} if declarations are enabled (default)
     * @see OntLoaderConfiguration#isAllowReadDeclarations()
     */
    public boolean isAllowReadDeclarations() {
        return (boolean) get(OntSettings.ONT_API_LOAD_CONF_ALLOW_READ_DECLARATIONS);
    }

    /**
     * ONT-API manager load config setter.
     *
     * @param b boolean enable/disable declarations
     * @return this instance
     * @see OntLoaderConfiguration#setAllowReadDeclarations(boolean)
     */
    public OntConfig setAllowReadDeclarations(boolean b) {
        return put(OntSettings.ONT_API_LOAD_CONF_ALLOW_READ_DECLARATIONS, b);
    }

    /**
     * ONT-API manager load config getter.
     *
     * @return {@code true} if annotation axiom overlaps are ignored (default)
     * @see OntLoaderConfiguration#isIgnoreAnnotationAxiomOverlaps()
     */
    public boolean isIgnoreAnnotationAxiomOverlaps() {
        return (boolean) get(OntSettings.ONT_API_LOAD_CONF_IGNORE_ANNOTATION_AXIOM_OVERLAPS);
    }

    /**
     * ONT-API manager load config setter.
     *
     * @param b boolean to enable/disable this config parameter
     * @return this instance
     * @see OntLoaderConfiguration#setIgnoreAnnotationAxiomOverlaps(boolean)
     */
    public OntConfig setIgnoreAnnotationAxiomOverlaps(boolean b) {
        return put(OntSettings.ONT_API_LOAD_CONF_IGNORE_ANNOTATION_AXIOM_OVERLAPS, b);
    }

    /**
     * ONT-API manager load config getter.
     *
     * @return {@code true} if ONT-API loading disabled (false by default)
     * @see OntLoaderConfiguration#isUseOWLParsersToLoad()
     */
    public boolean isUseOWLParsersToLoad() {
        return (boolean) get(OntSettings.ONT_API_LOAD_CONF_USE_OWL_PARSERS_TO_LOAD);
    }

    /**
     * Sets an {@link #isUseOWLParsersToLoad()} parameter.
     * It is an ONT-API manager load config setter.
     * Used in {@link ru.avicomp.ontapi.OntologyFactoryImpl Default Ontology Factory Implementation} to choose preferable way to load.
     * If this parameter is set to {@code true} then Apache Jena loading mechanisms are used in case it is supported both by Jena and OWL-API.
     * Otherwise, loading is performed by using native OWL-API Parsers, which do not read full graph, but assemble it axiom by axiom.
     * Please note, OWL-API loading mechanisms are OWL-centric and, in fact, work buggy:
     * a graph may be corrupted after that loading, if source document does not fully correspond OWL2 specification.
     * For example, <a href='http://spinrdf.org/spin'>spin</a> ontology contains a lot of SPARQL queries in spin form,
     * which are using {@code rdf:List}s (an example of such []-list is the right part of any triple with predicate {@code sp:where}).
     * After loading this ontology with OWL Turtle Parser (checked v 5.1.4), it will contain garbage instead of the original constructs.
     * So please use this method with great care!
     *
     * @param b boolean to enable/disable this config parameter
     * @return this instance
     * @see OntLoaderConfiguration#setUseOWLParsersToLoad(boolean)
     */
    public OntConfig setUseOWLParsersToLoad(boolean b) {
        return put(OntSettings.ONT_API_LOAD_CONF_USE_OWL_PARSERS_TO_LOAD, b);
    }

    /**
     * ONT-API manager load config getter.
     * See {@link #setUseOWLParsersToLoad(boolean)} description.
     *
     * @return {@code true} if any errors while reading axioms are ignored (by default false)
     * @see OntLoaderConfiguration#isIgnoreAxiomsReadErrors()
     * @since 1.1.0
     */
    public boolean isIgnoreAxiomsReadErrors() {
        return (boolean) get(OntSettings.ONT_API_LOAD_CONF_IGNORE_AXIOMS_READ_ERRORS);
    }

    /**
     * ONT-API manager load config setter.
     *
     * @param b boolean to enable/disable ignoring axioms reading errors
     * @return this instance
     * @see OntLoaderConfiguration#setIgnoreAxiomsReadErrors(boolean)
     * @since 1.1.0
     */
    public OntConfig setIgnoreAxiomsReadErrors(boolean b) {
        return put(OntSettings.ONT_API_LOAD_CONF_IGNORE_AXIOMS_READ_ERRORS, b);
    }

    /**
     * ONT-API manager load config getter.
     * Answers {@code true} if the axiom-annotations-split functionality is enabled in the config.
     * If this parameter is set to {@code true}, each bulk annotation will generate a separated axiom.
     * Otherwise, all bulk-annotations go together with the main triple as a single axiom.
     * Consider the following ontology snippet:
     * <pre>{@code
     * <A>     a                owl:Class ;
     *         rdfs:subClassOf  owl:Thing .
     * [ a                      owl:Axiom ;
     *   rdfs:comment           "X" ;
     *   rdfs:label             "Z" ;
     *   owl:annotatedProperty  rdfs:subClassOf ;
     *   owl:annotatedSource    <A> ;
     *   owl:annotatedTarget    owl:Thing
     * ] .
     * [ a                      owl:Axiom ;
     *   rdfs:comment           "W" ;
     *   owl:annotatedProperty  rdfs:subClassOf ;
     *   owl:annotatedSource    <A> ;
     *   owl:annotatedTarget    owl:Thing
     * ] .
     * }</pre>
     * If {@code isSplitAxiomAnnotations()} equals {@code true} the ontology above gives the two following axioms:
     * <pre>{@code
     * SubClassOf(Annotation(rdfs:comment "W"^^xsd:string) <A> owl:Thing)
     * SubClassOf(Annotation(rdfs:comment "X"^^xsd:string) Annotation(rdfs:label "Z"^^xsd:string) <A> owl:Thing)
     * }</pre>
     * If {@code isSplitAxiomAnnotations()} equals {@code false}, there is only single {@code SubClassOf} axiom:
     * <pre>{@code
     * SubClassOf(Annotation(rdfs:comment "W"^^xsd:string) Annotation(rdfs:comment "X"^^xsd:string) Annotation(rdfs:label "Z"^^xsd:string) <string:A> owl:Thing)
     * }</pre>
     *
     * @return boolean
     * @see OntLoaderConfiguration#isSplitAxiomAnnotations()
     * @since 1.3.0
     */
    public boolean isSplitAxiomAnnotations() {
        return (boolean) get(OntSettings.ONT_API_LOAD_CONF_SPLIT_AXIOM_ANNOTATIONS);
    }

    /**
     * ONT-API manager load config setter.
     * Changes the axiom-annotations-split setting to the given state.
     *
     * @param b boolean
     * @return this instance
     * @see OntLoaderConfiguration#setSplitAxiomAnnotations(boolean)
     * @since 1.3.0
     */
    public OntConfig setSplitAxiomAnnotations(boolean b) {
        return put(OntSettings.ONT_API_LOAD_CONF_SPLIT_AXIOM_ANNOTATIONS, b);
    }

    /**
     * ONT-API manager write config getter.
     * By default 'ont.api.write.conf.control.imports' is enabled.
     *
     * @return {@code true} if 'ont.api.write.conf.control.imports' is enabled
     * @see OntWriterConfiguration#isControlImports()
     */
    public boolean isControlImports() {
        return (boolean) get(OntSettings.ONT_API_WRITE_CONF_CONTROL_IMPORTS);
    }

    /**
     * ONT-API manager write config setter.
     *
     * @param b boolean to enable/disable this config parameter
     * @return this instance
     * @see OntWriterConfiguration#setControlImports(boolean)
     */
    public OntConfig setControlImports(boolean b) {
        return put(OntSettings.ONT_API_WRITE_CONF_CONTROL_IMPORTS, b);
    }

    /**
     * Specifies whether or not annotation axioms (instances of {@code OWLAnnotationAxiom}) should be loaded or
     * whether they should be discarded on loading. By default, the loading of annotation axioms is enabled.
     * <p>
     * Note(1): The behaviour is slightly different from OWL-API (v5.1.4).
     * If loading axioms is disabled all annotation property assertion axioms turn into annotations in the composition of nearest declaration axioms.
     * E.g. the snippet
     * {@code
     * <http://class> a       owl:Class ;
     * rdfs:comment "comment1"@es .
     * } looks like {@code Declaration(Annotation(rdfs:comment "comment1"@es) Class(<http://class>))} in ON-API structural view, while in OWL-API
     * it would be just naked declaration (i.e. {@code Declaration(Class(<http://class>))}).
     * Note(2): this method does not affect underling graph.
     *
     * @see OntologyConfigurator#setLoadAnnotationAxioms(boolean)
     */
    @Override
    public OntConfig setLoadAnnotationAxioms(boolean b) {
        return put(OntSettings.OWL_API_LOAD_CONF_LOAD_ANNOTATIONS, b);
    }

    /**
     * @see OntologyConfigurator#shouldLoadAnnotations()
     */
    @Override
    public boolean shouldLoadAnnotations() {
        return (boolean) get(OntSettings.OWL_API_LOAD_CONF_LOAD_ANNOTATIONS);
    }

    /**
     * OWL-API(NEW) manager load config setter.
     * This is NOT override method:
     * there is NO such method in the original general OWL-API config ({@link OntologyConfigurator}), but it present in load-config.
     *
     * @param s String
     * @return this instance
     * @see OWLOntologyLoaderConfiguration#setEntityExpansionLimit(String)
     * @see OntLoaderConfiguration#setEntityExpansionLimit(String)
     */
    public OntConfig withEntityExpansionLimit(@Nonnull String s) {
        return put(OntSettings.OWL_API_LOAD_CONF_ENTITY_EXPANSION_LIMIT, s);
    }

    /**
     * OWL-API(NEW) manager load config getter.
     * This is NOT override method:
     * there is NO such method in the original general OWL-API config ({@link OntologyConfigurator}), but it present in load-config.
     *
     * @return String, for more info see:
     * @see OWLOntologyLoaderConfiguration#getEntityExpansionLimit()
     * @see OntLoaderConfiguration#getEntityExpansionLimit()
     */
    public String getEntityExpansionLimit() {
        return (String) get(OntSettings.OWL_API_LOAD_CONF_ENTITY_EXPANSION_LIMIT);
    }


    /**
     * @see OntologyConfigurator#withBannedParsers(String)
     */
    @Override
    public OntConfig withBannedParsers(@Nonnull String parsers) {
        return put(OntSettings.OWL_API_LOAD_CONF_BANNED_PARSERS, parsers);
    }

    /**
     * @see OntologyConfigurator#getBannedParsers()
     */
    @Override
    public String getBannedParsers() {
        return (String) get(OntSettings.OWL_API_LOAD_CONF_BANNED_PARSERS);
    }

    /**
     * @see OntologyConfigurator#setPriorityCollectionSorting(PriorityCollectionSorting)
     */
    @Override
    public OntConfig setPriorityCollectionSorting(@Nonnull PriorityCollectionSorting sorting) {
        return put(OntSettings.OWL_API_LOAD_CONF_PRIORITY_COLLECTION_SORTING, sorting);
    }

    /**
     * @see OntologyConfigurator#getPriorityCollectionSorting()
     */
    @Override
    public PriorityCollectionSorting getPriorityCollectionSorting() {
        return (PriorityCollectionSorting) get(OntSettings.OWL_API_LOAD_CONF_PRIORITY_COLLECTION_SORTING);
    }

    @SuppressWarnings("unchecked")
    protected List<String> getIgnoredImports() {
        return (List<String>) map.computeIfAbsent(OntSettings.OWL_API_LOAD_CONF_IGNORED_IMPORTS, OntConfig.OptionSetting::getDefaultValue);
    }

    /**
     * @see OntologyConfigurator#addIgnoredImport(IRI)
     */
    @Override
    public OntConfig addIgnoredImport(@Nonnull IRI iri) {
        if (!getIgnoredImports().contains(iri.getIRIString())) {
            getIgnoredImports().add(iri.getIRIString());
        }
        return this;
    }

    /**
     * @see OntologyConfigurator#clearIgnoredImports()
     */
    @Override
    public OntConfig clearIgnoredImports() {
        getIgnoredImports().clear();
        return this;
    }

    /**
     * @see OntologyConfigurator#removeIgnoredImport(IRI)
     */
    @Override
    public OntConfig removeIgnoredImport(@Nonnull IRI iri) {
        getIgnoredImports().remove(iri.getIRIString());
        return this;
    }

    /**
     * @see OntologyConfigurator#setAcceptingHTTPCompression(boolean)
     */
    @Override
    public OntConfig setAcceptingHTTPCompression(boolean b) {
        return put(OntSettings.OWL_API_LOAD_CONF_ACCEPT_HTTP_COMPRESSION, b);
    }

    /**
     * @see OntologyConfigurator#shouldAcceptHTTPCompression()
     */
    @Override
    public boolean shouldAcceptHTTPCompression() {
        return (boolean) get(OntSettings.OWL_API_LOAD_CONF_ACCEPT_HTTP_COMPRESSION);
    }

    /**
     * @see OntologyConfigurator#setConnectionTimeout(int)
     */
    @Override
    public OntConfig setConnectionTimeout(int t) {
        return put(OntSettings.OWL_API_LOAD_CONF_CONNECTION_TIMEOUT, t);
    }

    /**
     * @see OntologyConfigurator#getConnectionTimeout()
     */
    @Override
    public int getConnectionTimeout() {
        return (int) get(OntSettings.OWL_API_LOAD_CONF_CONNECTION_TIMEOUT);
    }

    /**
     * @see OntologyConfigurator#setFollowRedirects(boolean)
     */
    @Override
    public OntConfig setFollowRedirects(boolean b) {
        return put(OntSettings.OWL_API_LOAD_CONF_FOLLOW_REDIRECTS, b);
    }

    /**
     * @see OntologyConfigurator#shouldFollowRedirects()
     */
    @Override
    public boolean shouldFollowRedirects() {
        return (boolean) get(OntSettings.OWL_API_LOAD_CONF_FOLLOW_REDIRECTS);
    }

    /**
     * @see OntologyConfigurator#setMissingImportHandlingStrategy(MissingImportHandlingStrategy)
     */
    @Override
    public OntConfig setMissingImportHandlingStrategy(@Nonnull MissingImportHandlingStrategy strategy) {
        return put(OntSettings.OWL_API_LOAD_CONF_MISSING_IMPORT_HANDLING_STRATEGY, strategy);
    }

    /**
     * @see OntologyConfigurator#getMissingImportHandlingStrategy()
     */
    @Override
    public MissingImportHandlingStrategy getMissingImportHandlingStrategy() {
        return (MissingImportHandlingStrategy) get(OntSettings.OWL_API_LOAD_CONF_MISSING_IMPORT_HANDLING_STRATEGY);
    }

    /**
     * @see OntologyConfigurator#setMissingOntologyHeaderStrategy(MissingOntologyHeaderStrategy)
     */
    @Override
    public OntConfig setMissingOntologyHeaderStrategy(@Nonnull MissingOntologyHeaderStrategy strategy) {
        return put(OntSettings.OWL_API_LOAD_CONF_MISSING_ONTOLOGY_HEADER_STRATEGY, strategy);
    }

    /**
     * @see OntologyConfigurator#getMissingOntologyHeaderStrategy()
     */
    @Override
    public MissingOntologyHeaderStrategy getMissingOntologyHeaderStrategy() {
        return (MissingOntologyHeaderStrategy) get(OntSettings.OWL_API_LOAD_CONF_MISSING_ONTOLOGY_HEADER_STRATEGY);
    }

    /**
     * @see OntologyConfigurator#setReportStackTraces(boolean)
     */
    @Override
    public OntConfig setReportStackTraces(boolean b) {
        return put(OntSettings.OWL_API_LOAD_CONF_REPORT_STACK_TRACES, b);
    }

    /**
     * @see OntologyConfigurator#shouldReportStackTraces()
     */
    @Override
    public boolean shouldReportStackTraces() {
        return (boolean) get(OntSettings.OWL_API_LOAD_CONF_REPORT_STACK_TRACES);
    }

    /**
     * @see OntologyConfigurator#setRetriesToAttempt(int)
     */
    @Override
    public OntConfig setRetriesToAttempt(int retries) {
        return put(OntSettings.OWL_API_LOAD_CONF_RETRIES_TO_ATTEMPT, retries);
    }

    /**
     * @see OntologyConfigurator#getRetriesToAttempt()
     */
    @Override
    public int getRetriesToAttempt() {
        return (int) get(OntSettings.OWL_API_LOAD_CONF_RETRIES_TO_ATTEMPT);
    }

    /**
     * @see OntologyConfigurator#setStrict(boolean)
     */
    @Override
    public OntConfig setStrict(boolean strict) {
        return put(OntSettings.OWL_API_LOAD_CONF_PARSE_WITH_STRICT_CONFIGURATION, strict);
    }

    /**
     * @see OntologyConfigurator#shouldParseWithStrictConfiguration()
     */
    @Override
    public boolean shouldParseWithStrictConfiguration() {
        return (boolean) get(OntSettings.OWL_API_LOAD_CONF_PARSE_WITH_STRICT_CONFIGURATION);
    }

    /**
     * @see OntologyConfigurator#setTreatDublinCoreAsBuiltIn(boolean)
     */
    @Override
    public OntConfig setTreatDublinCoreAsBuiltIn(boolean value) {
        return put(OntSettings.OWL_API_LOAD_CONF_TREAT_DUBLINCORE_AS_BUILTIN, value);
    }

    /**
     * @see OntologyConfigurator#shouldTreatDublinCoreAsBuiltin()
     */
    @Override
    public boolean shouldTreatDublinCoreAsBuiltin() {
        return (boolean) get(OntSettings.OWL_API_LOAD_CONF_TREAT_DUBLINCORE_AS_BUILTIN);
    }

    /**
     * @see OntologyConfigurator#withSaveIdsForAllAnonymousIndividuals(boolean)
     */
    @Override
    public OntConfig withSaveIdsForAllAnonymousIndividuals(boolean b) {
        return put(OntSettings.OWL_API_WRITE_CONF_SAVE_IDS, b);
    }

    /**
     * @see OntologyConfigurator#shouldSaveIds()
     */
    @Override
    public boolean shouldSaveIds() {
        return (boolean) get(OntSettings.OWL_API_WRITE_CONF_SAVE_IDS);
    }

    /**
     * @see OntologyConfigurator#withRemapAllAnonymousIndividualsIds(boolean)
     */
    @Override
    public OntConfig withRemapAllAnonymousIndividualsIds(boolean b) {
        return put(OntSettings.OWL_API_WRITE_CONF_REMAP_IDS, b);
    }

    /**
     * @see OntologyConfigurator#shouldRemapIds()
     */
    @Override
    public boolean shouldRemapIds() {
        return (boolean) get(OntSettings.OWL_API_WRITE_CONF_REMAP_IDS);
    }

    /**
     * @see OntologyConfigurator#withUseNamespaceEntities(boolean)
     */
    @Override
    public OntConfig withUseNamespaceEntities(boolean b) {
        return put(OntSettings.OWL_API_WRITE_CONF_USE_NAMESPACE_ENTITIES, b);
    }

    /**
     * @see OntologyConfigurator#shouldUseNamespaceEntities()
     */
    @Override
    public boolean shouldUseNamespaceEntities() {
        return (boolean) get(OntSettings.OWL_API_WRITE_CONF_USE_NAMESPACE_ENTITIES);
    }

    /**
     * @see OntologyConfigurator#withIndenting(boolean)
     */
    @Override
    public OntConfig withIndenting(boolean b) {
        return put(OntSettings.OWL_API_WRITE_CONF_INDENTING, b);
    }

    /**
     * @see OntologyConfigurator#shouldIndent()
     */
    @Override
    public boolean shouldIndent() {
        return (boolean) get(OntSettings.OWL_API_WRITE_CONF_INDENTING);
    }

    /**
     * @see OntologyConfigurator#withIndentSize(int)
     */
    @Override
    public OntConfig withIndentSize(int indent) {
        return put(OntSettings.OWL_API_WRITE_CONF_INDENT_SIZE, indent);
    }

    /**
     * @see OntologyConfigurator#getIndentSize()
     */
    @Override
    public int getIndentSize() {
        return (int) get(OntSettings.OWL_API_WRITE_CONF_INDENT_SIZE);
    }

    /**
     * @see OntologyConfigurator#withLabelsAsBanner(boolean)
     */
    @Override
    public OntConfig withLabelsAsBanner(boolean b) {
        return put(OntSettings.OWL_API_WRITE_CONF_LABEL_AS_BANNER, b);
    }

    /**
     * @see OntologyConfigurator#shouldUseLabelsAsBanner()
     */
    @Override
    public boolean shouldUseLabelsAsBanner() {
        return (boolean) get(OntSettings.OWL_API_WRITE_CONF_LABEL_AS_BANNER);
    }

    /**
     * @see OntologyConfigurator#withBannersEnabled(boolean)
     */
    @Override
    public OntConfig withBannersEnabled(boolean b) {
        return put(OntSettings.OWL_API_WRITE_CONF_BANNERS_ENABLED, b);
    }

    /**
     * @see OntologyConfigurator#shouldUseBanners()
     */
    @Override
    public boolean shouldUseBanners() {
        return (boolean) get(OntSettings.OWL_API_WRITE_CONF_BANNERS_ENABLED);
    }

    /**
     * Always returns {@code true} since this functionality is not supported by ONT-API.
     *
     * @return {@code true}
     * @since OWL-API 5.1.1
     * @since ONT-API 1.3.0
     */
    public boolean shouldRepairIllegalPunnings() {
        LOGGER.warn("ONT-API does not support RepairIllegalPunnings");
        return true;
    }

    /**
     * No-op since this functionality is not supported by ONT-API.
     *
     * @param b anything
     * @return this instance
     * @since OWL-API 5.1.1
     * @since ONT-API 1.3.0
     */
    public OntConfig withRepairIllegalPunnings(boolean b) {
        LOGGER.warn("ONT-API does not support RepairIllegalPunnings");
        return this;
    }

    /**
     * Builds new loader configuration.
     *
     * @return new {@link OntLoaderConfiguration}
     * @see OntologyConfigurator#buildLoaderConfiguration()
     */
    @Override
    public OntLoaderConfiguration buildLoaderConfiguration() {
        OntLoaderConfiguration res = new OntLoaderConfiguration(null);
        res.personality = getPersonality();
        res.transformers = getGraphTransformers();
        for (OntSettings s : OntSettings.values()) {
            if (!s.isLoad()) continue;
            res.map.put(s, get(s));
        }
        return res;
    }

    /**
     * Builds new writer configuration.
     *
     * @return new {@link OntWriterConfiguration}
     * @see OntologyConfigurator#buildWriterConfiguration()
     */
    @Override
    public OntWriterConfiguration buildWriterConfiguration() {
        OntWriterConfiguration res = new OntWriterConfiguration(null);
        for (OntSettings s : OntSettings.values()) {
            if (!s.isWrite()) continue;
            res.map.put(s, get(s));
        }
        return res;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OntConfig)) return false;
        OntConfig that = (OntConfig) o;
        return Objects.equals(this.getPersonality(), that.getPersonality()) &&
                Objects.equals(this.getGraphTransformers(), that.getGraphTransformers()) &&
                Objects.equals(this.map, that.map);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getPersonality(), this.getGraphTransformers(), this.map);
    }

    public static OntConfig copy(OntologyConfigurator from) {
        OntConfig res = new OntConfig();
        if (from == null) return res;
        if (from instanceof OntConfig) {
            res.map.putAll(((OntConfig) from).map);
            res.setPersonality(((OntConfig) from).getPersonality());
            res.setGraphTransformers(((OntConfig) from).getGraphTransformers());
            return res;
        }

        res.map.put(OntSettings.OWL_API_LOAD_CONF_IGNORED_IMPORTS, new ArrayList<>(ignoredImports(from)));
        res.map.put(OntSettings.OWL_API_LOAD_CONF_ACCEPT_HTTP_COMPRESSION, from.shouldAcceptHTTPCompression());
        res.map.put(OntSettings.OWL_API_LOAD_CONF_CONNECTION_TIMEOUT, from.getConnectionTimeout());
        res.map.put(OntSettings.OWL_API_LOAD_CONF_FOLLOW_REDIRECTS, from.shouldFollowRedirects());
        res.map.put(OntSettings.OWL_API_LOAD_CONF_LOAD_ANNOTATIONS, from.shouldLoadAnnotations());
        res.map.put(OntSettings.OWL_API_LOAD_CONF_MISSING_IMPORT_HANDLING_STRATEGY, from.getMissingImportHandlingStrategy());
        res.map.put(OntSettings.OWL_API_LOAD_CONF_MISSING_ONTOLOGY_HEADER_STRATEGY, from.getMissingOntologyHeaderStrategy());
        res.map.put(OntSettings.OWL_API_LOAD_CONF_REPORT_STACK_TRACES, from.shouldReportStackTraces());
        res.map.put(OntSettings.OWL_API_LOAD_CONF_RETRIES_TO_ATTEMPT, from.getRetriesToAttempt());
        res.map.put(OntSettings.OWL_API_LOAD_CONF_PARSE_WITH_STRICT_CONFIGURATION, from.shouldParseWithStrictConfiguration());
        res.map.put(OntSettings.OWL_API_LOAD_CONF_TREAT_DUBLINCORE_AS_BUILTIN, from.shouldTreatDublinCoreAsBuiltin());
        res.map.put(OntSettings.OWL_API_LOAD_CONF_PRIORITY_COLLECTION_SORTING, from.getPriorityCollectionSorting());
        res.map.put(OntSettings.OWL_API_LOAD_CONF_BANNED_PARSERS, from.getBannedParsers());
        // NOTE: there is no ConfigurationOption.ENTITY_EXPANSION_LIMIT inside original (OWL-API, ver 5.0.5) class.

        res.map.put(OntSettings.OWL_API_WRITE_CONF_SAVE_IDS, from.shouldSaveIds());
        res.map.put(OntSettings.OWL_API_WRITE_CONF_REMAP_IDS, from.shouldRemapIds());
        res.map.put(OntSettings.OWL_API_WRITE_CONF_USE_NAMESPACE_ENTITIES, from.shouldUseNamespaceEntities());
        res.map.put(OntSettings.OWL_API_WRITE_CONF_INDENTING, from.shouldIndent());
        res.map.put(OntSettings.OWL_API_WRITE_CONF_LABEL_AS_BANNER, from.shouldUseLabelsAsBanner());
        res.map.put(OntSettings.OWL_API_WRITE_CONF_BANNERS_ENABLED, from.shouldUseBanners());
        res.map.put(OntSettings.OWL_API_WRITE_CONF_INDENT_SIZE, from.getIndentSize());

        return res;
    }

    @SuppressWarnings("unchecked")
    protected static Set<IRI> ignoredImports(OntologyConfigurator owl) {
        try {
            Field field = owl.getClass().getDeclaredField("ignoredImports");
            field.setAccessible(true);
            return (Set<IRI>) field.get(owl);
        } catch (NoSuchFieldException | IllegalAccessException | ClassCastException e) {
            throw new OntApiException("Can't get OntologyConfigurator#ignoredImports.", e);
        }
    }

    public static OntPersonality getDefaultPersonality() {
        OntModelConfig.StdMode mode = (OntModelConfig.StdMode) OntSettings.ONT_API_LOAD_CONF_PERSONALITY_MODE.getDefaultValue();
        switch (mode) {
            case LAX:
                return OntModelConfig.ONT_PERSONALITY_LAX;
            case MEDIUM:
                return OntModelConfig.ONT_PERSONALITY_MEDIUM;
            case STRICT:
                return OntModelConfig.ONT_PERSONALITY_STRICT;
            default:
                throw new OntApiException.Unsupported("Unsupported personality mode " + mode);
        }
    }

    @SuppressWarnings("unchecked")
    public static GraphTransformers.Store getDefaultTransformers() {
        List<Class> transformers = (List<Class>) OntSettings.ONT_API_LOAD_CONF_TRANSFORMERS.getDefaultValue();
        GraphTransformers.Store res = new GraphTransformers.Store();
        for (Class c : transformers) {
            res = res.add(new GraphTransformers.DefaultMaker(c));
        }
        return res;
    }

    public enum DefaultScheme implements Scheme {
        HTTP,
        HTTPS,
        FTP,
        FILE,
        ;

        @Override
        public String key() {
            return name().toLowerCase();
        }

        public static Stream<DefaultScheme> all() {
            return Stream.of(values());
        }
    }

    /**
     * Interface for working with IRI scheme.
     * It is used as parameter in {@link OntConfig} and {@link OntLoaderConfiguration}.
     */
    @FunctionalInterface
    public interface Scheme extends Serializable {

        /**
         * Returns this scheme as String
         *
         * @return String
         */
        String key();

        /**
         * Answers {@code true} if the given IRI has this schema.
         *
         * @param iri {@link IRI}
         * @return boolean
         */
        default boolean same(IRI iri) {
            return iri != null && Objects.equals(key(), iri.getScheme());
        }
    }

    /**
     * Auxiliary interface, which provides an uniform way to work with option settings.
     */
    @FunctionalInterface
    public interface OptionSetting {

        /**
         * Returns the default value.
         *
         * @return a {@link Serializable} object
         */
        Serializable getDefaultValue();

        /**
         * Gets a value from map.
         * It is a functional equivalent of the expression {@code map.getOrDefault(key, key.getDefaultValue()}.
         *
         * @param map {@link Map} where {@link OptionSetting} is a key, any object is a value
         * @return Object, value
         */
        default Object fromMap(Map<OptionSetting, Object> map) {
            Object res = map.get(this);
            if (res != null) {
                return res;
            }
            return getDefaultValue();
        }
    }
}
