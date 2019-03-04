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
import ru.avicomp.ontapi.NoOpReadWriteLock;
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.jena.impl.conf.OntModelConfig;
import ru.avicomp.ontapi.jena.impl.conf.OntPersonality;
import ru.avicomp.ontapi.transforms.GraphTransformers;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.stream.Stream;

/**
 * This is the global config and also the builder for the separated load and write configs.
 * Used to manage general load/store behaviour of a manager (while immutable configs are for particular ontologies).
 * It overrides OWL-API {@link OntologyConfigurator} and provides access to the new (ONT-API) settings.
 * Note: this configuration is mutable, while load and write configs are not.
 * Additional (new) ONT-API methods:
 * <ul>
 * <li>{@link #getManagerIRICacheSize()} (<b>since 1.4.0</b>)</li>
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

    /**
     * Creates a new config with the given R/W lock ans IRI cache size.
     *
     * @param lock         {@link ReadWriteLock}
     * @param iriCacheSize int, possible non-positive for disabling cache
     * @return {@link OntConfig}
     * @see #setManagerIRICacheSize(int)
     * @since 1.4.0
     */
    public static OntConfig createConfig(ReadWriteLock lock, int iriCacheSize) {
        return createConfig(lock).setManagerIRICacheSize(iriCacheSize);
    }

    /**
     * Creates a new config instance.
     * All its settings are taken from {@code ./resources/ontapi.properties} file
     * or default, if missed in the file.
     * The returned instance is mutable.
     *
     * @param lock {@link ReadWriteLock} or {@code null} for non-concurrent version
     * @return {@link OntConfig}
     * @since 1.4.0
     */
    public static OntConfig createConfig(ReadWriteLock lock) {
        return withLock(new OntConfig(), lock);
    }

    /**
     * Makes a concurrent version of the specified config with the given R/W lock if needed,
     * otherwise returns the same config.
     * A concurrent config is backed by the given and vice versa.
     *
     * @param conf {@link OntConfig}, not {@code null}
     * @param lock {@link ReadWriteLock}, possible {@code null}
     * @return an instance with lock
     * @since 1.4.0
     */
    public static OntConfig withLock(OntConfig conf, ReadWriteLock lock) {
        if (conf instanceof Concurrent) {
            conf = ((Concurrent) conf).delegate;
        }
        if (NoOpReadWriteLock.isConcurrent(lock)) {
            return conf;
        }
        return new Concurrent(conf, lock);
    }

    /**
     * Copies all settings from the given {@link OntologyConfigurator} to a new instance.
     *
     * @param from {@link OntologyConfigurator} or {@code null} to create config with default settings.
     * @return {@link OntConfig} new instance
     */
    public static OntConfig copy(OntologyConfigurator from) {
        return copy(from, new OntConfig());
    }

    /**
     * Copies configuration from one config to another.
     *
     * @param from {@link OntologyConfigurator}, the source, can be {@code null}
     * @param to   {@link OntConfig}, the destination, not {@code null}
     * @return {@link OntConfig}, the same {@code to}-config
     * @since 1.4.0
     */
    public static OntConfig copy(OntologyConfigurator from, OntConfig to) {
        Objects.requireNonNull(to);
        if (from == null || from == to) return to;
        if (from instanceof OntConfig) {
            to.asMap().putAll(((OntConfig) from).asMap());
            to.setPersonality(((OntConfig) from).getPersonality());
            to.setGraphTransformers(((OntConfig) from).getGraphTransformers());
            return to;
        }
        Map<OptionSetting, Object> map = to.asMap();
        map.put(OntSettings.OWL_API_LOAD_CONF_IGNORED_IMPORTS, new ArrayList<>(ignoredImports(from)));
        map.put(OntSettings.OWL_API_LOAD_CONF_ACCEPT_HTTP_COMPRESSION, from.shouldAcceptHTTPCompression());
        map.put(OntSettings.OWL_API_LOAD_CONF_CONNECTION_TIMEOUT, from.getConnectionTimeout());
        map.put(OntSettings.OWL_API_LOAD_CONF_FOLLOW_REDIRECTS, from.shouldFollowRedirects());
        map.put(OntSettings.OWL_API_LOAD_CONF_LOAD_ANNOTATIONS, from.shouldLoadAnnotations());
        map.put(OntSettings.OWL_API_LOAD_CONF_MISSING_IMPORT_HANDLING_STRATEGY, from.getMissingImportHandlingStrategy());
        map.put(OntSettings.OWL_API_LOAD_CONF_MISSING_ONTOLOGY_HEADER_STRATEGY, from.getMissingOntologyHeaderStrategy());
        map.put(OntSettings.OWL_API_LOAD_CONF_REPORT_STACK_TRACES, from.shouldReportStackTraces());
        map.put(OntSettings.OWL_API_LOAD_CONF_RETRIES_TO_ATTEMPT, from.getRetriesToAttempt());
        map.put(OntSettings.OWL_API_LOAD_CONF_PARSE_WITH_STRICT_CONFIGURATION, from.shouldParseWithStrictConfiguration());
        map.put(OntSettings.OWL_API_LOAD_CONF_TREAT_DUBLINCORE_AS_BUILTIN, from.shouldTreatDublinCoreAsBuiltin());
        map.put(OntSettings.OWL_API_LOAD_CONF_PRIORITY_COLLECTION_SORTING, from.getPriorityCollectionSorting());
        map.put(OntSettings.OWL_API_LOAD_CONF_BANNED_PARSERS, from.getBannedParsers());
        // NOTE: there is no ConfigurationOption.ENTITY_EXPANSION_LIMIT inside original (OWL-API, ver 5.0.5) class.

        map.put(OntSettings.OWL_API_WRITE_CONF_SAVE_IDS, from.shouldSaveIds());
        map.put(OntSettings.OWL_API_WRITE_CONF_REMAP_IDS, from.shouldRemapIds());
        map.put(OntSettings.OWL_API_WRITE_CONF_USE_NAMESPACE_ENTITIES, from.shouldUseNamespaceEntities());
        map.put(OntSettings.OWL_API_WRITE_CONF_INDENTING, from.shouldIndent());
        map.put(OntSettings.OWL_API_WRITE_CONF_LABEL_AS_BANNER, from.shouldUseLabelsAsBanner());
        map.put(OntSettings.OWL_API_WRITE_CONF_BANNERS_ENABLED, from.shouldUseBanners());
        map.put(OntSettings.OWL_API_WRITE_CONF_INDENT_SIZE, from.getIndentSize());

        return to;
    }

    static <N extends Integer> N requirePositive(N n, Object message) {
        if (n.intValue() > 0) return n;
        throw new IllegalArgumentException(message + " must be positive: " + n);
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

    protected Map<OptionSetting, Object> asMap() {
        return map;
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

    @SuppressWarnings("unchecked")
    protected <X> X get(OptionSetting key) {
        return (X) key.fromMap(map);
    }

    @SuppressWarnings({"unchecked", "SameParameterValue"})
    protected <X> X getOrCompute(OptionSetting key) {
        return (X) map.computeIfAbsent(key, x -> key.getDefaultValue());
    }

    protected OntConfig put(OptionSetting key, Object value) {
        map.put(key, value);
        return this;
    }

    protected OntConfig putPositive(OntSettings k, int v) {
        return put(k, requirePositive(v, k));
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
     * @return {@link ru.avicomp.ontapi.transforms.GraphTransformers.Store}
     * @see OntLoaderConfiguration#getGraphTransformers()
     */
    public GraphTransformers.Store getGraphTransformers() {
        return transformers == null ? transformers = getDefaultTransformers() : transformers;
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
     * ONT-API manager load config setter.
     * Sets a new IRI cache size.
     * Protected, since this is a manager's initialization setting,
     * which must not be changed during manager's lifetime.
     *
     * @param size int, possible negative
     * @return this instance
     */
    protected OntConfig setManagerIRICacheSize(int size) {
        return put(OntSettings.ONT_API_CONF_MANAGER_CACHE_IRI, size);
    }

    /**
     * ONT-API manager load config getter.
     * Returns the IRI cache size, that is used inside a manager to share IRIs between ontologies.
     * The default size is {@code 2048}, it is a magic number which is taken from OWL-API impl
     * (see uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryInternalsImpl, v5)
     * A negative or zero number means that IRIs cache is disabled.
     *
     * @return int, possible non-positive to disable IRIs caching
     * @since 1.4.0
     */
    public int getManagerIRICacheSize() {
        Integer res = get(OntSettings.ONT_API_CONF_MANAGER_CACHE_IRI);
        return res != null ? res : -1;
    }

    /**
     * ONT-API manager load config getter.
     *
     * @return List of supported {@link Scheme schemes}
     * @see OntLoaderConfiguration#getSupportedSchemes()
     */
    public List<OntConfig.Scheme> getSupportedSchemes() {
        return get(OntSettings.ONT_API_LOAD_CONF_SUPPORTED_SCHEMES);
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
        return get(OntSettings.ONT_API_LOAD_CONF_PERFORM_TRANSFORMATIONS);
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
        return get(OntSettings.ONT_API_LOAD_CONF_ALLOW_BULK_ANNOTATION_ASSERTIONS);
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
        return get(OntSettings.ONT_API_LOAD_CONF_ALLOW_READ_DECLARATIONS);
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
        return get(OntSettings.ONT_API_LOAD_CONF_IGNORE_ANNOTATION_AXIOM_OVERLAPS);
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
        return get(OntSettings.ONT_API_LOAD_CONF_USE_OWL_PARSERS_TO_LOAD);
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
        return get(OntSettings.ONT_API_LOAD_CONF_IGNORE_AXIOMS_READ_ERRORS);
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
        return get(OntSettings.ONT_API_LOAD_CONF_SPLIT_AXIOM_ANNOTATIONS);
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
        return get(OntSettings.ONT_API_WRITE_CONF_CONTROL_IMPORTS);
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
        return get(OntSettings.OWL_API_LOAD_CONF_LOAD_ANNOTATIONS);
    }

    /**
     * OWL-API(NEW) manager load config setter.
     * This is NOT override method:
     * there is NO such method in the original general OWL-API config ({@link OntologyConfigurator}),
     * but it present in load-config.
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
        return get(OntSettings.OWL_API_LOAD_CONF_ENTITY_EXPANSION_LIMIT);
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
        return get(OntSettings.OWL_API_LOAD_CONF_BANNED_PARSERS);
    }

    /**
     * @see OntologyConfigurator#getPriorityCollectionSorting()
     */
    @Override
    public PriorityCollectionSorting getPriorityCollectionSorting() {
        return get(OntSettings.OWL_API_LOAD_CONF_PRIORITY_COLLECTION_SORTING);
    }

    /**
     * @see OntologyConfigurator#setPriorityCollectionSorting(PriorityCollectionSorting)
     */
    @Override
    public OntConfig setPriorityCollectionSorting(@Nonnull PriorityCollectionSorting sorting) {
        return put(OntSettings.OWL_API_LOAD_CONF_PRIORITY_COLLECTION_SORTING, sorting);
    }

    protected List<String> getIgnoredImports() {
        return getOrCompute(OntSettings.OWL_API_LOAD_CONF_IGNORED_IMPORTS);
    }

    /**
     * @see OntologyConfigurator#addIgnoredImport(IRI)
     */
    @Override
    public OntConfig addIgnoredImport(@Nonnull IRI iri) {
        List<String> list = getIgnoredImports();
        if (!list.contains(iri.getIRIString())) {
            list.add(iri.getIRIString());
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
        return get(OntSettings.OWL_API_LOAD_CONF_ACCEPT_HTTP_COMPRESSION);
    }

    /**
     * @see OntologyConfigurator#getConnectionTimeout()
     */
    @Override
    public int getConnectionTimeout() {
        return get(OntSettings.OWL_API_LOAD_CONF_CONNECTION_TIMEOUT);
    }

    /**
     * @see OntologyConfigurator#setConnectionTimeout(int)
     */
    @Override
    public OntConfig setConnectionTimeout(int t) {
        return putPositive(OntSettings.OWL_API_LOAD_CONF_CONNECTION_TIMEOUT, t);
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
        return get(OntSettings.OWL_API_LOAD_CONF_FOLLOW_REDIRECTS);
    }

    /**
     * @see OntologyConfigurator#getMissingImportHandlingStrategy()
     */
    @Override
    public MissingImportHandlingStrategy getMissingImportHandlingStrategy() {
        return get(OntSettings.OWL_API_LOAD_CONF_MISSING_IMPORT_HANDLING_STRATEGY);
    }

    /**
     * @see OntologyConfigurator#setMissingImportHandlingStrategy(MissingImportHandlingStrategy)
     */
    @Override
    public OntConfig setMissingImportHandlingStrategy(@Nonnull MissingImportHandlingStrategy strategy) {
        return put(OntSettings.OWL_API_LOAD_CONF_MISSING_IMPORT_HANDLING_STRATEGY, strategy);
    }

    /**
     * @see OntologyConfigurator#getMissingOntologyHeaderStrategy()
     */
    @Override
    public MissingOntologyHeaderStrategy getMissingOntologyHeaderStrategy() {
        return get(OntSettings.OWL_API_LOAD_CONF_MISSING_ONTOLOGY_HEADER_STRATEGY);
    }

    /**
     * @see OntologyConfigurator#setMissingOntologyHeaderStrategy(MissingOntologyHeaderStrategy)
     */
    @Override
    public OntConfig setMissingOntologyHeaderStrategy(@Nonnull MissingOntologyHeaderStrategy strategy) {
        return put(OntSettings.OWL_API_LOAD_CONF_MISSING_ONTOLOGY_HEADER_STRATEGY, strategy);
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
        return get(OntSettings.OWL_API_LOAD_CONF_REPORT_STACK_TRACES);
    }

    /**
     * @see OntologyConfigurator#getRetriesToAttempt()
     */
    @Override
    public int getRetriesToAttempt() {
        return get(OntSettings.OWL_API_LOAD_CONF_RETRIES_TO_ATTEMPT);
    }

    /**
     * @see OntologyConfigurator#setRetriesToAttempt(int)
     */
    @Override
    public OntConfig setRetriesToAttempt(int retries) {
        return putPositive(OntSettings.OWL_API_LOAD_CONF_RETRIES_TO_ATTEMPT, retries);
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
        return get(OntSettings.OWL_API_LOAD_CONF_PARSE_WITH_STRICT_CONFIGURATION);
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
        return get(OntSettings.OWL_API_LOAD_CONF_TREAT_DUBLINCORE_AS_BUILTIN);
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
        return get(OntSettings.OWL_API_WRITE_CONF_SAVE_IDS);
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
        return get(OntSettings.OWL_API_WRITE_CONF_REMAP_IDS);
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
        return get(OntSettings.OWL_API_WRITE_CONF_USE_NAMESPACE_ENTITIES);
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
        return get(OntSettings.OWL_API_WRITE_CONF_INDENTING);
    }

    /**
     * @see OntologyConfigurator#withIndentSize(int)
     */
    @Override
    public OntConfig withIndentSize(int indent) {
        return putPositive(OntSettings.OWL_API_WRITE_CONF_INDENT_SIZE, indent);
    }

    /**
     * @see OntologyConfigurator#getIndentSize()
     */
    @Override
    public int getIndentSize() {
        return get(OntSettings.OWL_API_WRITE_CONF_INDENT_SIZE);
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
        return get(OntSettings.OWL_API_WRITE_CONF_LABEL_AS_BANNER);
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
        return get(OntSettings.OWL_API_WRITE_CONF_BANNERS_ENABLED);
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

    public enum DefaultScheme implements Scheme {
        HTTP,
        HTTPS,
        FTP,
        FILE,
        ;

        public static Stream<DefaultScheme> all() {
            return Stream.of(values());
        }

        @Override
        public String key() {
            return name().toLowerCase();
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
         * Gets a value from the map.
         * It is a functional equivalent of the expression {@code map.getOrDefault(key, key.getDefaultValue()},
         * but the calling {@link #getDefaultValue()} is postponed.
         * The given {@code Map} is not modified in any case.
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

    /**
     * A {@link OntConfig config}-wrapper with {@link ReadWriteLock R/W}-locked access.
     * <p>
     * Created by @szuev on 05.07.2018.
     */
    public static class Concurrent extends OntConfig {
        private static final long serialVersionUID = 5910609264963651991L;
        protected final ReadWriteLock lock;
        protected final OntConfig delegate;

        protected Concurrent(OntConfig from, ReadWriteLock lock) {
            this.delegate = Objects.requireNonNull(from);
            this.lock = lock == null ? NoOpReadWriteLock.NO_OP_RW_LOCK : lock;
        }

        @Override
        protected Map<OptionSetting, Object> asMap() {
            return delegate.asMap();
        }

        @Override
        protected <X> X get(OptionSetting key) {
            lock.readLock().lock();
            try {
                return delegate.get(key);
            } finally {
                lock.readLock().unlock();
            }
        }

        @Override
        protected <X> X getOrCompute(OptionSetting key) {
            lock.readLock().lock();
            try {
                return delegate.getOrCompute(key);
            } finally {
                lock.readLock().unlock();
            }
        }

        @Override
        protected Concurrent put(OptionSetting key, Object value) {
            lock.writeLock().lock();
            try {
                delegate.put(key, value);
                return this;
            } finally {
                lock.writeLock().unlock();
            }
        }

        @Override
        public OntPersonality getPersonality() {
            lock.readLock().lock();
            try {
                return delegate.getPersonality();
            } finally {
                lock.readLock().unlock();
            }
        }

        @Override
        public Concurrent setPersonality(OntPersonality p) {
            lock.writeLock().lock();
            try {
                delegate.setPersonality(p);
                return this;
            } finally {
                lock.writeLock().unlock();
            }
        }

        @Override
        public GraphTransformers.Store getGraphTransformers() {
            lock.readLock().lock();
            try {
                return delegate.getGraphTransformers();
            } finally {
                lock.readLock().unlock();
            }
        }

        @Override
        public Concurrent setGraphTransformers(GraphTransformers.Store t) {
            lock.writeLock().lock();
            try {
                delegate.setGraphTransformers(t);
                return this;
            } finally {
                lock.writeLock().unlock();
            }
        }

        @Override
        public OntLoaderConfiguration buildLoaderConfiguration() {
            lock.readLock().lock();
            try {
                return delegate.buildLoaderConfiguration();
            } finally {
                lock.readLock().unlock();
            }
        }

        @Override
        public OntWriterConfiguration buildWriterConfiguration() {
            lock.readLock().lock();
            try {
                return delegate.buildWriterConfiguration();
            } finally {
                lock.readLock().unlock();
            }
        }

        @Override
        public OntConfig addIgnoredImport(@Nonnull IRI iri) {
            lock.writeLock().lock();
            try {
                delegate.addIgnoredImport(iri);
                return this;
            } finally {
                lock.writeLock().unlock();
            }
        }

        @Override
        public OntConfig clearIgnoredImports() {
            lock.writeLock().lock();
            try {
                delegate.clearIgnoredImports();
                return this;
            } finally {
                lock.writeLock().unlock();
            }
        }

        /**
         * @see OntologyConfigurator#removeIgnoredImport(IRI)
         */
        @Override
        public OntConfig removeIgnoredImport(@Nonnull IRI iri) {
            lock.writeLock().lock();
            try {
                delegate.removeIgnoredImport(iri);
                return this;
            } finally {
                lock.writeLock().unlock();
            }
        }

        @Override
        public boolean equals(Object o) {
            lock.readLock().lock();
            try {
                if (o instanceof Concurrent) {
                    o = ((Concurrent) o).delegate;
                }
                return delegate.equals(o);
            } finally {
                lock.readLock().unlock();
            }
        }

        @Override
        public int hashCode() {
            lock.readLock().lock();
            try {
                return delegate.hashCode();
            } finally {
                lock.readLock().unlock();
            }
        }
    }
}
