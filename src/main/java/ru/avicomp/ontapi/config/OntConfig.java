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
import ru.avicomp.ontapi.jena.impl.conf.OntPersonality;
import ru.avicomp.ontapi.transforms.GraphTransformers;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.ObjectOutputStream;
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
 * <li>{@link #getManagerIRIsCacheSize()} (<b>since 1.4.0</b>)</li>
 * <li>{@link #getLoadNodesCacheSize()} and {@link #setLoadNodesCacheSize(int)} (<b>since 1.4.0</b>)</li>
 * <li>{@link #getLoadObjectsCacheSize()} and {@link #setLoadObjectsCacheSize(int)} (<b>since 1.4.0</b>)</li>
 * <li>{@link #isContentCacheEnabled()} and {@link #setUseContentCache(boolean)} (<b>since 1.4.0</b>)</li>
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
public class OntConfig extends OntologyConfigurator implements
        LoadControl<OntConfig>,
        CacheControl<OntConfig>,
        AxiomsControl<OntConfig> {
    private static final Logger LOGGER = LoggerFactory.getLogger(OntConfig.class);
    private static final long serialVersionUID = 656765031127374396L;

    protected final Map<OntSettings, Object> map;
    private transient OntLoaderConfiguration loader;
    private transient OntWriterConfiguration writer;

    public OntConfig() {
        this.map = new EnumMap<>(OntSettings.class);
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
        return new OntConfig().putAll(from);
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

    static <N extends Integer> N requirePositive(N n, Object message) {
        if (n.intValue() > 0) return n;
        throw new IllegalArgumentException(message + " must be positive: " + n);
    }

    static Map<OntSettings, Object> loadMap(Map<OntSettings, Object> map, OntSettings... keys) {
        if (map.size() != keys.length) {
            // load all values
            for (OntSettings k : keys) {
                map.computeIfAbsent(k, OntSettings::getDefaultValue);
            }
        }
        return map;
    }

    /**
     * Copies configuration from the given config.
     *
     * @param from {@link OntologyConfigurator}, the source, can be {@code null}
     * @return {@link OntConfig} this config
     * @since 1.4.0
     */
    public OntConfig putAll(OntologyConfigurator from) {
        if (from == null) return this;
        if (from instanceof OntConfig) {
            Map<OntSettings, Object> tmp;
            if (from instanceof Concurrent) {
                tmp = ((Concurrent) from).delegate.map;
            } else {
                tmp = ((OntConfig) from).map;
            }
            tmp.forEach(this::put);
            return this;
        }
        this.put(OntSettings.OWL_API_LOAD_CONF_IGNORED_IMPORTS, new ArrayList<>(ignoredImports(from)));
        this.put(OntSettings.OWL_API_LOAD_CONF_ACCEPT_HTTP_COMPRESSION, from.shouldAcceptHTTPCompression());
        this.put(OntSettings.OWL_API_LOAD_CONF_CONNECTION_TIMEOUT, from.getConnectionTimeout());
        this.put(OntSettings.OWL_API_LOAD_CONF_FOLLOW_REDIRECTS, from.shouldFollowRedirects());
        this.put(OntSettings.OWL_API_LOAD_CONF_LOAD_ANNOTATIONS, from.shouldLoadAnnotations());
        this.put(OntSettings.OWL_API_LOAD_CONF_MISSING_IMPORT_HANDLING_STRATEGY, from.getMissingImportHandlingStrategy());
        this.put(OntSettings.OWL_API_LOAD_CONF_MISSING_ONTOLOGY_HEADER_STRATEGY, from.getMissingOntologyHeaderStrategy());
        this.put(OntSettings.OWL_API_LOAD_CONF_REPORT_STACK_TRACES, from.shouldReportStackTraces());
        this.put(OntSettings.OWL_API_LOAD_CONF_RETRIES_TO_ATTEMPT, from.getRetriesToAttempt());
        this.put(OntSettings.OWL_API_LOAD_CONF_PARSE_WITH_STRICT_CONFIGURATION, from.shouldParseWithStrictConfiguration());
        this.put(OntSettings.OWL_API_LOAD_CONF_TREAT_DUBLINCORE_AS_BUILTIN, from.shouldTreatDublinCoreAsBuiltin());
        this.put(OntSettings.OWL_API_LOAD_CONF_PRIORITY_COLLECTION_SORTING, from.getPriorityCollectionSorting());
        this.put(OntSettings.OWL_API_LOAD_CONF_BANNED_PARSERS, from.getBannedParsers());
        // NOTE: there is no ConfigurationOption.ENTITY_EXPANSION_LIMIT inside original (OWL-API, ver 5.0.5) class.

        this.put(OntSettings.OWL_API_WRITE_CONF_SAVE_IDS, from.shouldSaveIds());
        this.put(OntSettings.OWL_API_WRITE_CONF_REMAP_IDS, from.shouldRemapIds());
        this.put(OntSettings.OWL_API_WRITE_CONF_USE_NAMESPACE_ENTITIES, from.shouldUseNamespaceEntities());
        this.put(OntSettings.OWL_API_WRITE_CONF_INDENTING, from.shouldIndent());
        this.put(OntSettings.OWL_API_WRITE_CONF_LABEL_AS_BANNER, from.shouldUseLabelsAsBanner());
        this.put(OntSettings.OWL_API_WRITE_CONF_BANNERS_ENABLED, from.shouldUseBanners());
        this.put(OntSettings.OWL_API_WRITE_CONF_INDENT_SIZE, from.getIndentSize());

        return this;
    }

    @SuppressWarnings("unchecked")
    protected <X> X get(OntSettings key) {
        return (X) map.computeIfAbsent(key, x -> key.getDefaultValue());
    }

    protected OntConfig put(OntSettings key, Object value) {
        if (Objects.requireNonNull(value).equals(map.put(key, value))) {
            return this;
        }
        // the config's change is detected
        loader = null;
        writer = null;
        return this;
    }

    private OntConfig putPositive(OntSettings k, int v) {
        return put(k, requirePositive(v, k));
    }

    /**
     * An ONT-API manager's load config getter.
     * {@inheritDoc}
     *
     * @return {@link OntPersonality}
     * @see OntLoaderConfiguration#getPersonality()
     */
    @Override
    public OntPersonality getPersonality() {
        return get(OntSettings.ONT_API_LOAD_CONF_PERSONALITY_MODE);
    }

    /**
     * An ONT-API manager's load config setter.
     * {@inheritDoc}
     *
     * @param p {@link OntPersonality} the personality
     * @return this instance
     * @see OntLoaderConfiguration#setPersonality(OntPersonality)
     */
    @Override
    public OntConfig setPersonality(OntPersonality p) {
        return put(OntSettings.ONT_API_LOAD_CONF_PERSONALITY_MODE, p);
    }

    /**
     * An ONT-API manager's load config getter.
     * {@inheritDoc}
     */
    @Override
    public GraphTransformers.Store getGraphTransformers() {
        return get(OntSettings.ONT_API_LOAD_CONF_TRANSFORMERS);
    }

    /**
     * An ONT-API manager's load config setter.
     * {@inheritDoc}
     *
     * @param t {@link ru.avicomp.ontapi.transforms.GraphTransformers.Store}
     * @return this instance
     * @see OntLoaderConfiguration#setGraphTransformers(GraphTransformers.Store)
     */
    @Override
    public OntConfig setGraphTransformers(GraphTransformers.Store t) {
        return put(OntSettings.ONT_API_LOAD_CONF_TRANSFORMERS, t);
    }

    /**
     * An ONT-API manager's load config getter.
     * Returns the IRI cache size, that is used inside a manager to share IRIs between ontologies.
     * The default size is {@code 2048}, it is a magic number which is taken from OWL-API impl
     * (see uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryInternalsImpl, v5)
     * A negative or zero number means that IRIs cache is disabled.
     *
     * @return int, possible non-positive to disable IRIs caching
     * @since 1.4.0
     */
    public int getManagerIRIsCacheSize() {
        return get(OntSettings.ONT_API_MANAGER_CACHE_IRIS);
    }

    /**
     * An ONT-API manager's load config setter.
     * Sets a new IRIs cache size.
     * Protected, since this is a manager's initialization setting,
     * that must not be changed during manager's lifetime.
     *
     * @param size int, possible negative
     * @return this instance
     */
    protected OntConfig setManagerIRIsCacheSize(int size) {
        return put(OntSettings.ONT_API_MANAGER_CACHE_IRIS, size);
    }

    /**
     * An ONT-API manager's load config getter.
     * {@inheritDoc}
     *
     * @return int
     */
    @Override
    public int getLoadNodesCacheSize() {
        return get(OntSettings.ONT_API_LOAD_CONF_CACHE_NODES);
    }

    /**
     * An ONT-API manager's load config setter.
     * {@inheritDoc}
     *
     * @param size int
     * @return this instance
     */
    @Override
    public OntConfig setLoadNodesCacheSize(int size) {
        return put(OntSettings.ONT_API_LOAD_CONF_CACHE_NODES, size);
    }

    /**
     * An ONT-API manager's load config getter.
     * {@inheritDoc}
     *
     * @return int
     */
    @Override
    public int getLoadObjectsCacheSize() {
        return get(OntSettings.ONT_API_LOAD_CONF_CACHE_OBJECTS);
    }

    /**
     * An ONT-API manager's load config setter.
     * {@inheritDoc}
     *
     * @param size int
     * @return this instance
     */
    @Override
    public OntConfig setLoadObjectsCacheSize(int size) {
        return put(OntSettings.ONT_API_LOAD_CONF_CACHE_OBJECTS, size);
    }

    /**
     * An ONT-API manager's load config setter.
     * {@inheritDoc}
     *
     * @param level int
     * @return this instance
     */
    @Override
    public OntConfig setContentCacheLevel(int level) {
        return put(OntSettings.ONT_API_LOAD_CONF_CACHE_CONTENT, level);
    }

    /**
     * An ONT-API manager's load config getter.
     * {@inheritDoc}
     *
     * @return boolean
     */
    @Override
    public int getContentCacheLevel() {
        return get(OntSettings.ONT_API_LOAD_CONF_CACHE_CONTENT);
    }

    /**
     * An ONT-API manager's load config getter.
     * {@inheritDoc}
     */
    @Override
    public List<OntConfig.Scheme> getSupportedSchemes() {
        return get(OntSettings.ONT_API_LOAD_CONF_SUPPORTED_SCHEMES);
    }

    /**
     * An ONT-API manager's load config setter.
     * {@inheritDoc}
     *
     * @return this manager
     * @see OntConfig#setSupportedSchemes(List)
     * @since 1.1.0
     */
    @Override
    public OntConfig disableWebAccess() {
        return setSupportedSchemes(Collections.singletonList(DefaultScheme.FILE));
    }

    /**
     * An ONT-API manager's load config setter.
     * {@inheritDoc}
     *
     * @param schemes List of {@link Scheme}
     * @return {@link OntConfig} this instance
     * @see OntLoaderConfiguration#setSupportedSchemes(List)
     */
    @Override
    public OntConfig setSupportedSchemes(List<Scheme> schemes) {
        return put(OntSettings.ONT_API_LOAD_CONF_SUPPORTED_SCHEMES, Collections.unmodifiableList(schemes));
    }

    /**
     * An ONT-API manager's load config getter.
     * {@inheritDoc}
     */
    @Override
    public boolean isPerformTransformation() {
        return get(OntSettings.ONT_API_LOAD_CONF_PERFORM_TRANSFORMATIONS);
    }

    /**
     * An ONT-API manager's load config setter.
     * {@inheritDoc}
     *
     * @param b {@code true} to enable transformation (by default it is enabled)
     * @return {@link OntConfig} this instance
     * @see OntLoaderConfiguration#setPerformTransformation(boolean)
     */
    @Override
    public OntConfig setPerformTransformation(boolean b) {
        return put(OntSettings.ONT_API_LOAD_CONF_PERFORM_TRANSFORMATIONS, b);
    }

    /**
     * An ONT-API manager's load config getter.
     * {@inheritDoc}
     *
     * @return {@code true} if bulk annotations are allowed (that is by default)
     * @see OntLoaderConfiguration#isAllowBulkAnnotationAssertions()
     */
    @Override
    public boolean isAllowBulkAnnotationAssertions() {
        return get(OntSettings.ONT_API_LOAD_CONF_ALLOW_BULK_ANNOTATION_ASSERTIONS);
    }

    /**
     * An ONT-API manager's load config setter.
     * {@inheritDoc}
     *
     * @param b {@code true} to enable bulk annotations
     * @return this instance
     * @see OntLoaderConfiguration#setAllowBulkAnnotationAssertions(boolean)
     */
    @Override
    public OntConfig setAllowBulkAnnotationAssertions(boolean b) {
        return put(OntSettings.ONT_API_LOAD_CONF_ALLOW_BULK_ANNOTATION_ASSERTIONS, b);
    }

    /**
     * An ONT-API manager's load config getter.
     * {@inheritDoc}
     *
     * @return {@code true} if declarations are enabled (default)
     * @see OntLoaderConfiguration#isAllowReadDeclarations()
     */
    @Override
    public boolean isAllowReadDeclarations() {
        return get(OntSettings.ONT_API_LOAD_CONF_ALLOW_READ_DECLARATIONS);
    }

    /**
     * An ONT-API manager's load config setter.
     * {@inheritDoc}
     *
     * @param b boolean enable/disable declarations
     * @return this instance
     * @see OntLoaderConfiguration#setAllowReadDeclarations(boolean)
     */
    @Override
    public OntConfig setAllowReadDeclarations(boolean b) {
        return put(OntSettings.ONT_API_LOAD_CONF_ALLOW_READ_DECLARATIONS, b);
    }

    /**
     * An ONT-API manager's load config getter.
     * {@inheritDoc}
     *
     * @return {@code true} if annotation axiom overlaps are ignored (default)
     * @see OntLoaderConfiguration#isIgnoreAnnotationAxiomOverlaps()
     */
    @Override
    public boolean isIgnoreAnnotationAxiomOverlaps() {
        return get(OntSettings.ONT_API_LOAD_CONF_IGNORE_ANNOTATION_AXIOM_OVERLAPS);
    }

    /**
     * An ONT-API manager's load config setter.
     * {@inheritDoc}
     *
     * @param b boolean to enable/disable this config parameter
     * @return this instance
     * @see OntLoaderConfiguration#setIgnoreAnnotationAxiomOverlaps(boolean)
     */
    @Override
    public OntConfig setIgnoreAnnotationAxiomOverlaps(boolean b) {
        return put(OntSettings.ONT_API_LOAD_CONF_IGNORE_ANNOTATION_AXIOM_OVERLAPS, b);
    }

    /**
     * An ONT-API manager's load config getter.
     * {@inheritDoc}
     */
    @Override
    public boolean isUseOWLParsersToLoad() {
        return get(OntSettings.ONT_API_LOAD_CONF_USE_OWL_PARSERS_TO_LOAD);
    }

    /**
     * An ONT-API manager's load config setter.
     * {@inheritDoc}
     *
     * @param b boolean to enable/disable this config parameter
     * @return this instance
     */
    @Override
    public OntConfig setUseOWLParsersToLoad(boolean b) {
        return put(OntSettings.ONT_API_LOAD_CONF_USE_OWL_PARSERS_TO_LOAD, b);
    }

    /**
     * An ONT-API manager's load config getter.
     * {@inheritDoc}
     *
     * @return {@code true} if any errors while reading axioms are ignored (by default it is {@code false})
     * @see OntLoaderConfiguration#isIgnoreAxiomsReadErrors()
     * @since 1.1.0
     */
    @Override
    public boolean isIgnoreAxiomsReadErrors() {
        return get(OntSettings.ONT_API_LOAD_CONF_IGNORE_AXIOMS_READ_ERRORS);
    }

    /**
     * An ONT-API manager's load config setter.
     * {@inheritDoc}
     *
     * @param b boolean to enable/disable ignoring axioms reading errors
     * @return this instance
     * @see OntLoaderConfiguration#setIgnoreAxiomsReadErrors(boolean)
     * @since 1.1.0
     */
    @Override
    public OntConfig setIgnoreAxiomsReadErrors(boolean b) {
        return put(OntSettings.ONT_API_LOAD_CONF_IGNORE_AXIOMS_READ_ERRORS, b);
    }

    /**
     * An ONT-API manager's load config getter.
     * {@inheritDoc}
     */
    @Override
    public boolean isSplitAxiomAnnotations() {
        return get(OntSettings.ONT_API_LOAD_CONF_SPLIT_AXIOM_ANNOTATIONS);
    }

    /**
     * An ONT-API manager's load config setter.
     * {@inheritDoc}
     *
     * @param b boolean
     * @return this instance
     * @see OntLoaderConfiguration#setSplitAxiomAnnotations(boolean)
     * @since 1.3.0
     */
    @Override
    public OntConfig setSplitAxiomAnnotations(boolean b) {
        return put(OntSettings.ONT_API_LOAD_CONF_SPLIT_AXIOM_ANNOTATIONS, b);
    }

    /**
     * An ONT-API manager's write config getter.
     * By default 'ont.api.write.conf.control.imports' is disabled.
     *
     * @return {@code true} if 'ont.api.write.conf.control.imports' is enabled
     * @see OntWriterConfiguration#isControlImports()
     */
    public boolean isControlImports() {
        return get(OntSettings.ONT_API_WRITE_CONF_CONTROL_IMPORTS);
    }

    /**
     * An ONT-API manager's write config setter.
     *
     * @param b boolean to enable/disable this config parameter
     * @return this instance
     * @see OntWriterConfiguration#setControlImports(boolean)
     */
    public OntConfig setControlImports(boolean b) {
        return put(OntSettings.ONT_API_WRITE_CONF_CONTROL_IMPORTS, b);
    }

    /**
     * The same as {@link #isLoadAnnotationAxioms()}
     *
     * @return boolean
     * @see OntologyConfigurator#shouldLoadAnnotations()
     */
    @Override
    public boolean shouldLoadAnnotations() {
        return isLoadAnnotationAxioms();
    }

    /**
     * OWL-API(NEW) manager load config getter.
     * {@inheritDoc}
     *
     * @return boolean
     */
    @Override
    public boolean isLoadAnnotationAxioms() {
        return get(OntSettings.OWL_API_LOAD_CONF_LOAD_ANNOTATIONS);
    }

    /**
     * An ONT-API manager's load config setter.
     * {@inheritDoc}
     *
     * @see OntologyConfigurator#setLoadAnnotationAxioms(boolean)
     */
    @Override
    public OntConfig setLoadAnnotationAxioms(boolean b) {
        return put(OntSettings.OWL_API_LOAD_CONF_LOAD_ANNOTATIONS, b);
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
     * {@inheritDoc}
     * @see OntologyConfigurator#withBannedParsers(String)
     */
    @Override
    public OntConfig withBannedParsers(@Nonnull String parsers) {
        return put(OntSettings.OWL_API_LOAD_CONF_BANNED_PARSERS, parsers);
    }

    /**
     * {@inheritDoc}
     * @see OntologyConfigurator#getBannedParsers()
     */
    @Override
    public String getBannedParsers() {
        return get(OntSettings.OWL_API_LOAD_CONF_BANNED_PARSERS);
    }

    /**
     * {@inheritDoc}
     * @see OntologyConfigurator#getPriorityCollectionSorting()
     */
    @Override
    public PriorityCollectionSorting getPriorityCollectionSorting() {
        return get(OntSettings.OWL_API_LOAD_CONF_PRIORITY_COLLECTION_SORTING);
    }

    /**
     * {@inheritDoc}
     * @see OntologyConfigurator#setPriorityCollectionSorting(PriorityCollectionSorting)
     */
    @Override
    public OntConfig setPriorityCollectionSorting(@Nonnull PriorityCollectionSorting sorting) {
        return put(OntSettings.OWL_API_LOAD_CONF_PRIORITY_COLLECTION_SORTING, sorting);
    }

    protected List<String> getIgnoredImports() {
        return new ArrayList<>(get(OntSettings.OWL_API_LOAD_CONF_IGNORED_IMPORTS));
    }

    protected OntConfig putIgnoredImports(List<String> imports) {
        return put(OntSettings.OWL_API_LOAD_CONF_IGNORED_IMPORTS, Collections.unmodifiableList(imports));
    }

    /**
     * {@inheritDoc}
     * @see OntologyConfigurator#addIgnoredImport(IRI)
     */
    @Override
    public OntConfig addIgnoredImport(@Nonnull IRI iri) {
        List<String> list = getIgnoredImports();
        if (list.contains(iri.getIRIString())) {
            return this;
        }
        list.add(iri.getIRIString());
        return putIgnoredImports(list);
    }

    /**
     * {@inheritDoc}
     * @see OntologyConfigurator#clearIgnoredImports()
     */
    @Override
    public OntConfig clearIgnoredImports() {
        List<String> list = getIgnoredImports();
        if (list.isEmpty())
            return this;
        return putIgnoredImports(new ArrayList<>());
    }

    /**
     * {@inheritDoc}
     * @see OntologyConfigurator#removeIgnoredImport(IRI)
     */
    @Override
    public OntConfig removeIgnoredImport(@Nonnull IRI iri) {
        List<String> list = getIgnoredImports();
        if (!list.contains(iri.getIRIString())) {
            return this;
        }
        list.remove(iri.getIRIString());
        return putIgnoredImports(list);
    }

    /**
     * {@inheritDoc}
     * @see OntologyConfigurator#setAcceptingHTTPCompression(boolean)
     */
    @Override
    public OntConfig setAcceptingHTTPCompression(boolean b) {
        return put(OntSettings.OWL_API_LOAD_CONF_ACCEPT_HTTP_COMPRESSION, b);
    }

    /**
     * {@inheritDoc}
     * @see OntologyConfigurator#shouldAcceptHTTPCompression()
     */
    @Override
    public boolean shouldAcceptHTTPCompression() {
        return get(OntSettings.OWL_API_LOAD_CONF_ACCEPT_HTTP_COMPRESSION);
    }

    /**
     * {@inheritDoc}
     * @see OntologyConfigurator#getConnectionTimeout()
     */
    @Override
    public int getConnectionTimeout() {
        return get(OntSettings.OWL_API_LOAD_CONF_CONNECTION_TIMEOUT);
    }

    /**
     * {@inheritDoc}
     * @see OntologyConfigurator#setConnectionTimeout(int)
     */
    @Override
    public OntConfig setConnectionTimeout(int t) {
        return putPositive(OntSettings.OWL_API_LOAD_CONF_CONNECTION_TIMEOUT, t);
    }

    /**
     * {@inheritDoc}
     * @see OntologyConfigurator#setFollowRedirects(boolean)
     */
    @Override
    public OntConfig setFollowRedirects(boolean b) {
        return put(OntSettings.OWL_API_LOAD_CONF_FOLLOW_REDIRECTS, b);
    }

    /**
     * {@inheritDoc}
     * @see OntologyConfigurator#shouldFollowRedirects()
     */
    @Override
    public boolean shouldFollowRedirects() {
        return get(OntSettings.OWL_API_LOAD_CONF_FOLLOW_REDIRECTS);
    }

    /**
     * {@inheritDoc}
     * @see OntologyConfigurator#getMissingImportHandlingStrategy()
     */
    @Override
    public MissingImportHandlingStrategy getMissingImportHandlingStrategy() {
        return get(OntSettings.OWL_API_LOAD_CONF_MISSING_IMPORT_HANDLING_STRATEGY);
    }

    /**
     * {@inheritDoc}
     * @see OntologyConfigurator#setMissingImportHandlingStrategy(MissingImportHandlingStrategy)
     */
    @Override
    public OntConfig setMissingImportHandlingStrategy(@Nonnull MissingImportHandlingStrategy strategy) {
        return put(OntSettings.OWL_API_LOAD_CONF_MISSING_IMPORT_HANDLING_STRATEGY, strategy);
    }

    /**
     * {@inheritDoc}
     * @see OntologyConfigurator#getMissingOntologyHeaderStrategy()
     */
    @Override
    public MissingOntologyHeaderStrategy getMissingOntologyHeaderStrategy() {
        return get(OntSettings.OWL_API_LOAD_CONF_MISSING_ONTOLOGY_HEADER_STRATEGY);
    }

    /**
     * {@inheritDoc}
     * @see OntologyConfigurator#setMissingOntologyHeaderStrategy(MissingOntologyHeaderStrategy)
     */
    @Override
    public OntConfig setMissingOntologyHeaderStrategy(@Nonnull MissingOntologyHeaderStrategy strategy) {
        return put(OntSettings.OWL_API_LOAD_CONF_MISSING_ONTOLOGY_HEADER_STRATEGY, strategy);
    }

    /**
     * {@inheritDoc}
     * @see OntologyConfigurator#setReportStackTraces(boolean)
     */
    @Override
    public OntConfig setReportStackTraces(boolean b) {
        return put(OntSettings.OWL_API_LOAD_CONF_REPORT_STACK_TRACES, b);
    }

    /**
     * {@inheritDoc}
     * @see OntologyConfigurator#shouldReportStackTraces()
     */
    @Override
    public boolean shouldReportStackTraces() {
        return get(OntSettings.OWL_API_LOAD_CONF_REPORT_STACK_TRACES);
    }

    /**
     * {@inheritDoc}
     * @see OntologyConfigurator#getRetriesToAttempt()
     */
    @Override
    public int getRetriesToAttempt() {
        return get(OntSettings.OWL_API_LOAD_CONF_RETRIES_TO_ATTEMPT);
    }

    /**
     * {@inheritDoc}
     * @see OntologyConfigurator#setRetriesToAttempt(int)
     */
    @Override
    public OntConfig setRetriesToAttempt(int retries) {
        return putPositive(OntSettings.OWL_API_LOAD_CONF_RETRIES_TO_ATTEMPT, retries);
    }

    /**
     * {@inheritDoc}
     * @see OntologyConfigurator#setStrict(boolean)
     */
    @Override
    public OntConfig setStrict(boolean strict) {
        return put(OntSettings.OWL_API_LOAD_CONF_PARSE_WITH_STRICT_CONFIGURATION, strict);
    }

    /**
     * {@inheritDoc}
     * @see OntologyConfigurator#shouldParseWithStrictConfiguration()
     */
    @Override
    public boolean shouldParseWithStrictConfiguration() {
        return get(OntSettings.OWL_API_LOAD_CONF_PARSE_WITH_STRICT_CONFIGURATION);
    }

    /**
     * {@inheritDoc}
     * @see OntologyConfigurator#setTreatDublinCoreAsBuiltIn(boolean)
     */
    @Override
    public OntConfig setTreatDublinCoreAsBuiltIn(boolean value) {
        return put(OntSettings.OWL_API_LOAD_CONF_TREAT_DUBLINCORE_AS_BUILTIN, value);
    }

    /**
     * {@inheritDoc}
     * @see OntologyConfigurator#shouldTreatDublinCoreAsBuiltin()
     */
    @Override
    public boolean shouldTreatDublinCoreAsBuiltin() {
        return get(OntSettings.OWL_API_LOAD_CONF_TREAT_DUBLINCORE_AS_BUILTIN);
    }

    /**
     * {@inheritDoc}
     * @see OntologyConfigurator#withSaveIdsForAllAnonymousIndividuals(boolean)
     */
    @Override
    public OntConfig withSaveIdsForAllAnonymousIndividuals(boolean b) {
        return put(OntSettings.OWL_API_WRITE_CONF_SAVE_IDS, b);
    }

    /**
     * {@inheritDoc}
     * @see OntologyConfigurator#shouldSaveIds()
     */
    @Override
    public boolean shouldSaveIds() {
        return get(OntSettings.OWL_API_WRITE_CONF_SAVE_IDS);
    }

    /**
     * {@inheritDoc}
     * @see OntologyConfigurator#withRemapAllAnonymousIndividualsIds(boolean)
     */
    @Override
    public OntConfig withRemapAllAnonymousIndividualsIds(boolean b) {
        return put(OntSettings.OWL_API_WRITE_CONF_REMAP_IDS, b);
    }

    /**
     * {@inheritDoc}
     * @see OntologyConfigurator#shouldRemapIds()
     */
    @Override
    public boolean shouldRemapIds() {
        return get(OntSettings.OWL_API_WRITE_CONF_REMAP_IDS);
    }

    /**
     * {@inheritDoc}
     * @see OntologyConfigurator#withUseNamespaceEntities(boolean)
     */
    @Override
    public OntConfig withUseNamespaceEntities(boolean b) {
        return put(OntSettings.OWL_API_WRITE_CONF_USE_NAMESPACE_ENTITIES, b);
    }

    /**
     * {@inheritDoc}
     * @see OntologyConfigurator#shouldUseNamespaceEntities()
     */
    @Override
    public boolean shouldUseNamespaceEntities() {
        return get(OntSettings.OWL_API_WRITE_CONF_USE_NAMESPACE_ENTITIES);
    }

    /**
     * {@inheritDoc}
     * @see OntologyConfigurator#withIndenting(boolean)
     */
    @Override
    public OntConfig withIndenting(boolean b) {
        return put(OntSettings.OWL_API_WRITE_CONF_INDENTING, b);
    }

    /**
     * {@inheritDoc}
     * @see OntologyConfigurator#shouldIndent()
     */
    @Override
    public boolean shouldIndent() {
        return get(OntSettings.OWL_API_WRITE_CONF_INDENTING);
    }

    /**
     * {@inheritDoc}
     * @see OntologyConfigurator#withIndentSize(int)
     */
    @Override
    public OntConfig withIndentSize(int indent) {
        return putPositive(OntSettings.OWL_API_WRITE_CONF_INDENT_SIZE, indent);
    }

    /**
     * {@inheritDoc}
     * @see OntologyConfigurator#getIndentSize()
     */
    @Override
    public int getIndentSize() {
        return get(OntSettings.OWL_API_WRITE_CONF_INDENT_SIZE);
    }

    /**
     * {@inheritDoc}
     * @see OntologyConfigurator#withLabelsAsBanner(boolean)
     */
    @Override
    public OntConfig withLabelsAsBanner(boolean b) {
        return put(OntSettings.OWL_API_WRITE_CONF_LABEL_AS_BANNER, b);
    }

    /**
     * {@inheritDoc}
     * @see OntologyConfigurator#shouldUseLabelsAsBanner()
     */
    @Override
    public boolean shouldUseLabelsAsBanner() {
        return get(OntSettings.OWL_API_WRITE_CONF_LABEL_AS_BANNER);
    }

    /**
     * {@inheritDoc}
     * @see OntologyConfigurator#withBannersEnabled(boolean)
     */
    @Override
    public OntConfig withBannersEnabled(boolean b) {
        return put(OntSettings.OWL_API_WRITE_CONF_BANNERS_ENABLED, b);
    }

    /**
     * {@inheritDoc}
     * @see OntologyConfigurator#shouldUseBanners()
     */
    @Override
    public boolean shouldUseBanners() {
        return get(OntSettings.OWL_API_WRITE_CONF_BANNERS_ENABLED);
    }

    /**
     * {@inheritDoc}
     * Always returns {@code true} since this functionality is not supported by ONT-API.
     *
     * @return {@code true}
     * @since OWL-API 5.1.1
     * @since ONT-API 1.3.0
     */
    @Override
    public boolean shouldRepairIllegalPunnings() {
        LOGGER.warn("ONT-API does not support RepairIllegalPunnings");
        return true;
    }

    /**
     * {@inheritDoc}
     * No-op since this functionality is not supported by ONT-API.
     *
     * @param b anything
     * @return this instance
     * @since OWL-API 5.1.1
     * @since ONT-API 1.3.0
     */
    @Override
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
        if (loader != null) return loader;
        OntLoaderConfiguration res = new OntLoaderConfiguration();
        for (OntSettings s : OntSettings.LOAD_CONFIG_KEYS) {
            res.map.put(s, get(s));
        }
        return loader = res;
    }

    /**
     * Builds new writer configuration.
     *
     * @return new {@link OntWriterConfiguration}
     * @see OntologyConfigurator#buildWriterConfiguration()
     */
    @Override
    public OntWriterConfiguration buildWriterConfiguration() {
        if (writer != null) return writer;
        OntWriterConfiguration res = new OntWriterConfiguration();
        for (OntSettings s : OntSettings.WRITE_CONFIG_KEYS) {
            res.map.put(s, get(s));
        }
        return writer = res;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OntConfig)) return false;
        OntConfig that = (OntConfig) o;
        return Objects.equals(this.asMap(), that.asMap());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.asMap());
    }

    protected Map<OntSettings, Object> asMap() {
        return loadMap(this.map, OntSettings.values());
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        Map<OntSettings, Object> tmp = new EnumMap<>(OntSettings.class);
        this.map.forEach((k, v) -> {
            if (v instanceof Serializable) {
                tmp.put(k, v);
            }
        });
        out.writeObject(tmp);
    }

    public enum DefaultScheme implements Scheme {
        HTTP,
        HTTPS,
        FTP,
        FILE,
        ;
        private final String value;

        DefaultScheme() {
            this.value = name().toLowerCase();
        }

        public static Stream<DefaultScheme> all() {
            return Stream.of(values());
        }

        @Override
        public String key() {
            return value;
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
        protected Map<OntSettings, Object> asMap() {
            return delegate.asMap();
        }

        @Override
        public OntConfig putAll(OntologyConfigurator from) {
            lock.writeLock().lock();
            try {
                delegate.putAll(from);
                return this;
            } finally {
                lock.writeLock().unlock();
            }
        }

        @Override
        protected <X> X get(OntSettings key) {
            lock.readLock().lock();
            try {
                return delegate.get(key);
            } finally {
                lock.readLock().unlock();
            }
        }

        @Override
        protected Concurrent put(OntSettings key, Object value) {
            lock.writeLock().lock();
            try {
                delegate.put(key, value);
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
