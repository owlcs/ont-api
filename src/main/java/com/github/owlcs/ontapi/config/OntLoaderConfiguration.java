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

package com.github.owlcs.ontapi.config;

import com.github.owlcs.ontapi.OntApiException;
import com.github.owlcs.ontapi.transforms.GraphTransformers;
import com.github.sszuev.jena.ontapi.common.OntPersonality;
import javax.annotation.Nonnull;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.MissingImportHandlingStrategy;
import org.semanticweb.owlapi.model.MissingOntologyHeaderStrategy;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.PriorityCollectionSorting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * This is an extended {@link OWLOntologyLoaderConfiguration} with ONT-API specific settings.
 * Note: this config is immutable.
 * Used to configure loading a particular ontology to manager, different ontologies might have different load configs.
 * <p>
 * The new config methods (that are present in ONT-API, but are absent in the original OWL-API class),
 * which are listed in the description of the class {@link OntConfig},
 * have its own pair in this class.
 *
 * @see OntConfig
 */
@SuppressWarnings({"WeakerAccess", "SameParameterValue", "unused"})
public class OntLoaderConfiguration extends OWLOntologyLoaderConfiguration implements
        LoadControl<OntLoaderConfiguration>,
        CacheControl<OntLoaderConfiguration>,
        AxiomsControl<OntLoaderConfiguration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(OntLoaderConfiguration.class);
    private static final long serialVersionUID = 1599596390911768315L;

    protected final Map<OntSettings, Object> data;

    protected OntLoaderConfiguration() {
        this.data = new EnumMap<>(OntSettings.class);
    }

    public OntLoaderConfiguration(OWLOntologyLoaderConfiguration from) {
        this();
        if (from == null) return;
        if (from instanceof OntLoaderConfiguration) {
            copyONTSettings((OntLoaderConfiguration) from);
        } else {
            copyOWLSettings(from);
        }
    }

    @SuppressWarnings("unchecked")
    protected <X> X get(OntSettings key) {
        return (X) data.computeIfAbsent(key, OntSettings::getDefaultValue);
    }

    private OntLoaderConfiguration setPositive(OntSettings k, int v) {
        return set(k, OntConfig.requirePositive(v, k));
    }

    private OntLoaderConfiguration setNonNegative(OntSettings k, int v) {
        return set(k, OntConfig.requireNonNegative(v, k));
    }

    protected OntLoaderConfiguration set(OntSettings key, Object v) {
        Objects.requireNonNull(v);
        if (Objects.equals(get(key), v)) return this;
        OntLoaderConfiguration copy = new OntLoaderConfiguration(this);
        copy.data.put(key, v);
        return copy;
    }

    protected void copyOWLSettings(OWLOntologyLoaderConfiguration conf) {
        this.data.put(OntSettings.OWL_API_LOAD_CONF_IGNORED_IMPORTS, new ArrayList<>(ignoredImports(conf)));
        this.data.put(OntSettings.OWL_API_LOAD_CONF_ACCEPT_HTTP_COMPRESSION, conf.isAcceptingHTTPCompression());
        this.data.put(OntSettings.OWL_API_LOAD_CONF_CONNECTION_TIMEOUT, conf.getConnectionTimeout());
        this.data.put(OntSettings.OWL_API_LOAD_CONF_FOLLOW_REDIRECTS, conf.isFollowRedirects());
        this.data.put(OntSettings.OWL_API_LOAD_CONF_LOAD_ANNOTATIONS, conf.isLoadAnnotationAxioms());
        this.data.put(OntSettings.OWL_API_LOAD_CONF_MISSING_IMPORT_HANDLING_STRATEGY, conf.getMissingImportHandlingStrategy());
        this.data.put(OntSettings.OWL_API_LOAD_CONF_MISSING_ONTOLOGY_HEADER_STRATEGY, conf.getMissingOntologyHeaderStrategy());
        this.data.put(OntSettings.OWL_API_LOAD_CONF_REPORT_STACK_TRACES, conf.isReportStackTrace());
        this.data.put(OntSettings.OWL_API_LOAD_CONF_RETRIES_TO_ATTEMPT, conf.getRetriesToAttempt());
        this.data.put(OntSettings.OWL_API_LOAD_CONF_PARSE_WITH_STRICT_CONFIGURATION, conf.isStrict());
        this.data.put(OntSettings.OWL_API_LOAD_CONF_TREAT_DUBLINCORE_AS_BUILTIN, conf.isTreatDublinCoreAsBuiltIn());
        this.data.put(OntSettings.OWL_API_LOAD_CONF_PRIORITY_COLLECTION_SORTING, conf.getPriorityCollectionSorting());
        this.data.put(OntSettings.OWL_API_LOAD_CONF_BANNED_PARSERS, conf.getBannedParsers());
        this.data.put(OntSettings.OWL_API_LOAD_CONF_ENTITY_EXPANSION_LIMIT, conf.getEntityExpansionLimit());
        this.data.put(OntSettings.OWL_API_AUTHORIZATION_VALUE, conf.getAuthorizationValue());
    }

    protected void copyONTSettings(OntLoaderConfiguration conf) {
        this.data.putAll(conf.data);
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
     * An ONT-API config getter.
     * {@inheritDoc}
     */
    @Override
    public boolean isPerformTransformation() {
        return get(OntSettings.ONT_API_LOAD_CONF_PERFORM_TRANSFORMATIONS);
    }


    /**
     * An ONT-API config setter.
     * {@inheritDoc}
     *
     * @param b if {@code false} all graph transformations will be disabled
     * @return {@link OntLoaderConfiguration}, a copied (new) or this instance in case no changes is made
     */
    @Override
    public OntLoaderConfiguration setPerformTransformation(boolean b) {
        return set(OntSettings.ONT_API_LOAD_CONF_PERFORM_TRANSFORMATIONS, b);
    }

    /**
     * An ONT-API config getter.
     * {@inheritDoc}
     */
    @Override
    public GraphTransformers getGraphTransformers() {
        return get(OntSettings.ONT_API_LOAD_CONF_TRANSFORMERS);
    }

    /**
     * An ONT-API config setter.
     * {@inheritDoc}
     *
     * @param t {@link GraphTransformers} new graph transformers store
     * @return {@link OntLoaderConfiguration}, a copied (new) or this instance in case no changes is made
     */
    @Override
    public OntLoaderConfiguration setGraphTransformers(GraphTransformers t) {
        return set(OntSettings.ONT_API_LOAD_CONF_TRANSFORMERS, t);
    }

    /**
     * An ONT-API config getter.
     * {@inheritDoc}
     */
    @Override
    public OntPersonality getPersonality() {
        return get(OntSettings.ONT_API_LOAD_CONF_PERSONALITY_MODE);
    }

    /**
     * An ONT-API config setter.
     * {@inheritDoc}
     *
     * @param p {@link OntPersonality} new personality
     * @return {@link OntLoaderConfiguration}, a copied (new) or this instance in case no changes is made
     */
    @Override
    public OntLoaderConfiguration setPersonality(OntPersonality p) {
        return set(OntSettings.ONT_API_LOAD_CONF_PERSONALITY_MODE, p);
    }

    /**
     * An ONT-API config getter.
     * {@inheritDoc}
     */
    @Override
    public boolean isProcessImports() {
        return get(OntSettings.ONT_API_LOAD_CONF_PROCESS_IMPORTS);
    }

    /**
     * An ONT-API config setter.
     * {@inheritDoc}
     */
    @Override
    public OntLoaderConfiguration setProcessImports(boolean b) {
        return set(OntSettings.ONT_API_LOAD_CONF_PROCESS_IMPORTS, b);
    }

    /**
     * An ONT-API config setter.
     * {@inheritDoc}
     *
     * @param size int, non-negative integer
     * @return {@link OntLoaderConfiguration}, a copied (new) or this instance in case no changes is made
     */
    @Override
    public OntLoaderConfiguration setLoadNodesCacheSize(int size) {
        return set(OntSettings.ONT_API_LOAD_CONF_CACHE_NODES, size);
    }

    /**
     * An ONT-API config getter.
     * {@inheritDoc}
     */
    @Override
    public int getLoadNodesCacheSize() {
        return get(OntSettings.ONT_API_LOAD_CONF_CACHE_NODES);
    }

    /**
     * An ONT-API config setter.
     * {@inheritDoc}
     *
     * @param size int, non-negative integer
     * @return {@link OntLoaderConfiguration}, a copied (new) or this instance in case no changes is made
     */
    @Override
    public OntLoaderConfiguration setLoadObjectsCacheSize(int size) {
        return set(OntSettings.ONT_API_LOAD_CONF_CACHE_OBJECTS, size);
    }

    /**
     * An ONT-API config getter.
     * {@inheritDoc}
     */
    @Override
    public int getLoadObjectsCacheSize() {
        return get(OntSettings.ONT_API_LOAD_CONF_CACHE_OBJECTS);
    }

    /**
     * An ONT-API config setter.
     * {@inheritDoc}
     *
     * @param level, int, non-negative integer
     * @return {@link OntLoaderConfiguration}, a copied (new) or this instance in case no changes is made
     */
    @Override
    public OntLoaderConfiguration setModelCacheLevel(int level) {
        return setNonNegative(OntSettings.ONT_API_LOAD_CONF_CACHE_MODEL, level);
    }

    /**
     * An ONT-API config getter.
     * {@inheritDoc}
     */
    @Override
    public int getModelCacheLevel() {
        return get(OntSettings.ONT_API_LOAD_CONF_CACHE_MODEL);
    }

    /**
     * An ONT-API config getter.
     * {@inheritDoc}
     */
    @Override
    public List<Scheme> getSupportedSchemes() {
        return get(OntSettings.ONT_API_LOAD_CONF_SUPPORTED_SCHEMES);
    }

    /**
     * An ONT-API config setter.
     * {@inheritDoc}
     *
     * @param schemes the collection of {@link Scheme}s
     * @return {@link OntLoaderConfiguration}, a copied (new) or this instance in case no changes is made
     */
    @Override
    public OntLoaderConfiguration setSupportedSchemes(List<Scheme> schemes) {
        return set(OntSettings.ONT_API_LOAD_CONF_SUPPORTED_SCHEMES, Collections.unmodifiableList(schemes));
    }


    /**
     * An ONT-API config setter.
     * {@inheritDoc}
     *
     * @return {@link OntLoaderConfiguration}, a copied (new) or this instance in case no changes is made
     * @see OntConfig#disableWebAccess()
     * @since 1.3.0
     */
    @Override
    public OntLoaderConfiguration disableWebAccess() {
        return setSupportedSchemes(Collections.singletonList(OntConfig.DefaultScheme.FILE));
    }

    /**
     * An ONT-API config method.
     * {@inheritDoc}
     */
    @Override
    public boolean isAllowBulkAnnotationAssertions() {
        return get(OntSettings.ONT_API_LOAD_CONF_ALLOW_BULK_ANNOTATION_ASSERTIONS);
    }

    /**
     * An ONT-API config setter.
     * {@inheritDoc}
     *
     * @param b if {@code false} only plain annotation assertions axioms expected
     * @return {@link OntLoaderConfiguration}, a copied (new) or this instance in case no changes is made
     */
    @Override
    public OntLoaderConfiguration setAllowBulkAnnotationAssertions(boolean b) {
        return set(OntSettings.ONT_API_LOAD_CONF_ALLOW_BULK_ANNOTATION_ASSERTIONS, b);
    }

    /**
     * An ONT-API config getter.
     * {@inheritDoc}
     */
    @Override
    public boolean isAllowReadDeclarations() {
        return get(OntSettings.ONT_API_LOAD_CONF_ALLOW_READ_DECLARATIONS);
    }

    /**
     * An ONT-API config setter.
     * {@inheritDoc}
     *
     * @param b {@code true} to skip declarations while reading graph
     * @return {@link OntLoaderConfiguration}, a copied (new) or this instance in case no changes is made
     */
    @Override
    public OntLoaderConfiguration setAllowReadDeclarations(boolean b) {
        return set(OntSettings.ONT_API_LOAD_CONF_ALLOW_READ_DECLARATIONS, b);
    }

    /**
     * An ONT-API config getter.
     * {@inheritDoc}
     */
    @Override
    public boolean isIgnoreAnnotationAxiomOverlaps() {
        return get(OntSettings.ONT_API_LOAD_CONF_IGNORE_ANNOTATION_AXIOM_OVERLAPS);
    }

    /**
     * An ONT-API config setter.
     * {@inheritDoc}
     *
     * @param b if {@code false} all overlapping annotation axioms
     *          will be skipped in favour of data or/and object property axioms
     * @return {@link OntLoaderConfiguration}, a copied (new) or this instance in case no changes is made
     */
    @Override
    public OntLoaderConfiguration setIgnoreAnnotationAxiomOverlaps(boolean b) {
        return set(OntSettings.ONT_API_LOAD_CONF_IGNORE_ANNOTATION_AXIOM_OVERLAPS, b);
    }

    /**
     * An ONT-API config getter.
     * {@inheritDoc}
     */
    @Override
    public boolean isUseOWLParsersToLoad() {
        return get(OntSettings.ONT_API_LOAD_CONF_USE_OWL_PARSERS_TO_LOAD);
    }

    /**
     * An ONT-API config setter.
     * {@inheritDoc}
     *
     * @param b {@code true} to use pure OWL-API parsers to load
     * @return {@link OntLoaderConfiguration}, a copied (new) or this instance in case no changes is made
     */
    @Override
    public OntLoaderConfiguration setUseOWLParsersToLoad(boolean b) {
        return set(OntSettings.ONT_API_LOAD_CONF_USE_OWL_PARSERS_TO_LOAD, b);
    }

    /**
     * An ONT-API config getter.
     * {@inheritDoc}
     */
    @Override
    public boolean isIgnoreAxiomsReadErrors() {
        return get(OntSettings.ONT_API_LOAD_CONF_IGNORE_AXIOMS_READ_ERRORS);
    }

    /**
     * An ONT-API config getter.
     * {@inheritDoc}
     */
    @Override
    public boolean isReadONTObjects() {
        return get(OntSettings.ONT_API_LOAD_CONF_READ_ONT_OBJECTS);
    }

    /**
     * An ONT-API config setter.
     * {@inheritDoc}
     *
     * @param b {@code true} to ignore errors while reading axioms of some type from a graph,
     *          {@code false} to trow exception
     * @return {@link OntLoaderConfiguration}, a copied (new) or this instance in case no changes is made
     */
    @Override
    public OntLoaderConfiguration setIgnoreAxiomsReadErrors(boolean b) {
        return set(OntSettings.ONT_API_LOAD_CONF_IGNORE_AXIOMS_READ_ERRORS, b);
    }

    /**
     * An ONT-API config setter.
     * {@inheritDoc}
     *
     * @param b {@code true} to use {@code ONTObject}s as output
     * @return {@link OntLoaderConfiguration}, a copied (new) or this instance in case no changes is made
     */
    @Override
    public OntLoaderConfiguration setReadONTObjects(boolean b) {
        return set(OntSettings.ONT_API_LOAD_CONF_READ_ONT_OBJECTS, b);
    }

    /**
     * An ONT-API config getter.
     * {@inheritDoc}
     *
     * @return {@code true} if this setting is enabled
     * @see OntConfig#isSplitAxiomAnnotations()
     * @since 1.3.0
     */
    @Override
    public boolean isSplitAxiomAnnotations() {
        return get(OntSettings.ONT_API_LOAD_CONF_SPLIT_AXIOM_ANNOTATIONS);
    }

    /**
     * An ONT-API config setter.
     * {@inheritDoc}
     *
     * @param b boolean {@code true} to enable 'ont.api.load.conf.split.axiom.annotations' setting
     * @return {@link OntLoaderConfiguration}, a copied (new) or this instance in case no changes is made
     */
    @Override
    public OntLoaderConfiguration setSplitAxiomAnnotations(boolean b) {
        return set(OntSettings.ONT_API_LOAD_CONF_SPLIT_AXIOM_ANNOTATIONS, b);
    }

    /**
     * An ONT-API config getter.
     * {@inheritDoc}
     */
    @Override
    public boolean isLoadAnnotationAxioms() {
        return get(OntSettings.OWL_API_LOAD_CONF_LOAD_ANNOTATIONS);
    }

    /**
     * An ONT-API config setter.
     * {@inheritDoc}
     *
     * @param b {@code true} to enable reading and writing annotation axioms
     * @return {@link OntLoaderConfiguration}, a copied (new) or this instance in case no changes is made
     */
    @Override
    public OntLoaderConfiguration setLoadAnnotationAxioms(boolean b) {
        return set(OntSettings.OWL_API_LOAD_CONF_LOAD_ANNOTATIONS, b);
    }

    protected List<String> getIgnoredImports() {
        return get(OntSettings.OWL_API_LOAD_CONF_IGNORED_IMPORTS);
    }

    /**
     * @return unmodifiable {@code List} of IRIs (Strings)
     */
    protected List<String> getIgnoredImportsModifiableList() {
        return new ArrayList<>(getIgnoredImports());
    }

    protected OntLoaderConfiguration setIgnoredImports(List<String> imports) {
        return set(OntSettings.OWL_API_LOAD_CONF_IGNORED_IMPORTS, Collections.unmodifiableList(imports));
    }

    /**
     * {@inheritDoc}
     *
     * @see OWLOntologyLoaderConfiguration#addIgnoredImport(IRI)
     */
    @Override
    public OntLoaderConfiguration addIgnoredImport(@Nonnull IRI iri) {
        List<String> list = getIgnoredImportsModifiableList();
        if (list.contains(iri.getIRIString())) {
            return this;
        }
        list.add(iri.getIRIString());
        return setIgnoredImports(list);
    }

    /**
     * {@inheritDoc}
     *
     * @see OWLOntologyLoaderConfiguration#removeIgnoredImport(IRI)
     */
    @Override
    public OntLoaderConfiguration removeIgnoredImport(@Nonnull IRI iri) {
        List<String> list = getIgnoredImportsModifiableList();
        if (!list.contains(iri.getIRIString())) {
            return this;
        }
        list.remove(iri.getIRIString());
        return setIgnoredImports(list);
    }

    /**
     * {@inheritDoc}
     *
     * @see OWLOntologyLoaderConfiguration#clearIgnoredImports()
     */
    @Override
    public OntLoaderConfiguration clearIgnoredImports() {
        List<String> list = getIgnoredImportsModifiableList();
        if (list.isEmpty()) {
            return this;
        }
        return setIgnoredImports(new ArrayList<>());
    }

    /**
     * {@inheritDoc}
     *
     * @see OWLOntologyLoaderConfiguration#isIgnoredImport(IRI)
     */
    @Override
    public boolean isIgnoredImport(@Nonnull IRI iri) {
        return getIgnoredImports().contains(iri.getIRIString());
    }

    /**
     * {@inheritDoc}
     *
     * @see OWLOntologyLoaderConfiguration#getPriorityCollectionSorting()
     */
    @Override
    public PriorityCollectionSorting getPriorityCollectionSorting() {
        return get(OntSettings.OWL_API_LOAD_CONF_PRIORITY_COLLECTION_SORTING);
    }

    /**
     * {@inheritDoc}
     *
     * @see OWLOntologyLoaderConfiguration#setPriorityCollectionSorting(PriorityCollectionSorting)
     */
    @Override
    public OntLoaderConfiguration setPriorityCollectionSorting(@Nonnull PriorityCollectionSorting sorting) {
        return set(OntSettings.OWL_API_LOAD_CONF_PRIORITY_COLLECTION_SORTING, sorting);
    }

    /**
     * {@inheritDoc}
     *
     * @see OWLOntologyLoaderConfiguration#getConnectionTimeout()
     */
    @Override
    public int getConnectionTimeout() {
        return get(OntSettings.OWL_API_LOAD_CONF_CONNECTION_TIMEOUT);
    }

    /**
     * {@inheritDoc}
     *
     * @see OWLOntologyLoaderConfiguration#setConnectionTimeout(int)
     */
    @Override
    public OntLoaderConfiguration setConnectionTimeout(int time) {
        return setPositive(OntSettings.OWL_API_LOAD_CONF_CONNECTION_TIMEOUT, time);
    }

    /**
     * {@inheritDoc}
     *
     * @see OWLOntologyLoaderConfiguration#getMissingImportHandlingStrategy()
     */
    @Override
    public MissingImportHandlingStrategy getMissingImportHandlingStrategy() {
        return get(OntSettings.OWL_API_LOAD_CONF_MISSING_IMPORT_HANDLING_STRATEGY);
    }

    /**
     * {@inheritDoc}
     *
     * @see OWLOntologyLoaderConfiguration#setMissingImportHandlingStrategy(MissingImportHandlingStrategy)
     */
    @Override
    public OntLoaderConfiguration setMissingImportHandlingStrategy(@Nonnull MissingImportHandlingStrategy strategy) {
        return set(OntSettings.OWL_API_LOAD_CONF_MISSING_IMPORT_HANDLING_STRATEGY, strategy);
    }

    /**
     * {@inheritDoc}
     *
     * @see OWLOntologyLoaderConfiguration#getMissingOntologyHeaderStrategy()
     */
    @Override
    public MissingOntologyHeaderStrategy getMissingOntologyHeaderStrategy() {
        return get(OntSettings.OWL_API_LOAD_CONF_MISSING_ONTOLOGY_HEADER_STRATEGY);
    }

    /**
     * {@inheritDoc}
     *
     * @see OWLOntologyLoaderConfiguration#setMissingOntologyHeaderStrategy(MissingOntologyHeaderStrategy)
     */
    @Override
    public OntLoaderConfiguration setMissingOntologyHeaderStrategy(@Nonnull MissingOntologyHeaderStrategy strategy) {
        return set(OntSettings.OWL_API_LOAD_CONF_MISSING_ONTOLOGY_HEADER_STRATEGY, strategy);
    }

    /**
     * {@inheritDoc}
     *
     * @see OWLOntologyLoaderConfiguration#getRetriesToAttempt()
     */
    @Override
    public int getRetriesToAttempt() {
        return get(OntSettings.OWL_API_LOAD_CONF_RETRIES_TO_ATTEMPT);
    }

    /**
     * {@inheritDoc}
     *
     * @see OWLOntologyLoaderConfiguration#setRetriesToAttempt(int)
     */
    @Override
    public OntLoaderConfiguration setRetriesToAttempt(int retries) {
        return setPositive(OntSettings.OWL_API_LOAD_CONF_RETRIES_TO_ATTEMPT, retries);
    }

    /**
     * {@inheritDoc}
     *
     * @see OWLOntologyLoaderConfiguration#isAcceptingHTTPCompression()
     */
    @Override
    public boolean isAcceptingHTTPCompression() {
        return get(OntSettings.OWL_API_LOAD_CONF_ACCEPT_HTTP_COMPRESSION);
    }

    /**
     * {@inheritDoc}
     *
     * @see OWLOntologyLoaderConfiguration#setAcceptingHTTPCompression(boolean)
     */
    @Override
    public OntLoaderConfiguration setAcceptingHTTPCompression(boolean b) {
        return set(OntSettings.OWL_API_LOAD_CONF_ACCEPT_HTTP_COMPRESSION, b);
    }

    /**
     * {@inheritDoc}
     *
     * @see OWLOntologyLoaderConfiguration#isFollowRedirects()
     */
    @Override
    public boolean isFollowRedirects() {
        return get(OntSettings.OWL_API_LOAD_CONF_FOLLOW_REDIRECTS);
    }

    /**
     * {@inheritDoc}
     *
     * @see OWLOntologyLoaderConfiguration#setFollowRedirects(boolean)
     */
    @Override
    public OntLoaderConfiguration setFollowRedirects(boolean b) {
        return set(OntSettings.OWL_API_LOAD_CONF_FOLLOW_REDIRECTS, b);
    }

    /**
     * {@inheritDoc}
     *
     * @see OWLOntologyLoaderConfiguration#isReportStackTrace()
     */
    @Override
    public boolean isReportStackTrace() {
        return get(OntSettings.OWL_API_LOAD_CONF_REPORT_STACK_TRACES);
    }

    /**
     * {@inheritDoc}
     *
     * @see OWLOntologyLoaderConfiguration#setReportStackTraces(boolean)
     */
    @Override
    public OntLoaderConfiguration setReportStackTraces(boolean b) {
        return set(OntSettings.OWL_API_LOAD_CONF_REPORT_STACK_TRACES, b);
    }

    /**
     * {@inheritDoc}
     *
     * @see OWLOntologyLoaderConfiguration#isStrict()
     */
    @Override
    public boolean isStrict() {
        return get(OntSettings.OWL_API_LOAD_CONF_PARSE_WITH_STRICT_CONFIGURATION);
    }

    /**
     * {@inheritDoc}
     *
     * @see OWLOntologyLoaderConfiguration#setStrict(boolean)
     */
    @Override
    public OntLoaderConfiguration setStrict(boolean b) {
        return set(OntSettings.OWL_API_LOAD_CONF_PARSE_WITH_STRICT_CONFIGURATION, b);
    }

    /**
     * {@inheritDoc}
     *
     * @see OWLOntologyLoaderConfiguration#isTreatDublinCoreAsBuiltIn()
     */
    @Override
    public boolean isTreatDublinCoreAsBuiltIn() {
        return get(OntSettings.OWL_API_LOAD_CONF_TREAT_DUBLINCORE_AS_BUILTIN);
    }

    /**
     * {@inheritDoc}
     *
     * @see OWLOntologyLoaderConfiguration#setTreatDublinCoreAsBuiltIn(boolean)
     */
    @Override
    public OntLoaderConfiguration setTreatDublinCoreAsBuiltIn(boolean b) {
        return set(OntSettings.OWL_API_LOAD_CONF_TREAT_DUBLINCORE_AS_BUILTIN, b);
    }

    /**
     * {@inheritDoc}
     *
     * @see OWLOntologyLoaderConfiguration#getBannedParsers()
     */
    @Override
    public String getBannedParsers() {
        return get(OntSettings.OWL_API_LOAD_CONF_BANNED_PARSERS);
    }

    /**
     * {@inheritDoc}
     *
     * @see OWLOntologyLoaderConfiguration#setBannedParsers(String)
     */
    @Override
    public OntLoaderConfiguration setBannedParsers(@Nonnull String s) {
        return set(OntSettings.OWL_API_LOAD_CONF_BANNED_PARSERS, s);
    }

    /**
     * {@inheritDoc}
     *
     * @see OWLOntologyLoaderConfiguration#getEntityExpansionLimit()
     */
    @Override
    public String getEntityExpansionLimit() {
        return get(OntSettings.OWL_API_LOAD_CONF_ENTITY_EXPANSION_LIMIT);
    }

    /**
     * {@inheritDoc}
     *
     * @see OWLOntologyLoaderConfiguration#setEntityExpansionLimit(String)
     */
    @Override
    public OntLoaderConfiguration setEntityExpansionLimit(@Nonnull String s) {
        return set(OntSettings.OWL_API_LOAD_CONF_ENTITY_EXPANSION_LIMIT, s);
    }

    /**
     * {@inheritDoc}
     *
     * @return String authorization header value
     * @see OWLOntologyLoaderConfiguration#getAuthorizationValue()
     * @since OWL-API 5.1.4
     * @since ONT-API 1.3.0
     */
    @Override
    public String getAuthorizationValue() {
        return get(OntSettings.OWL_API_AUTHORIZATION_VALUE);
    }

    /**
     * {@inheritDoc}
     *
     * @param s Authorization header value.
     * @return an {@link OntLoaderConfiguration} with the new option set
     * @see OWLOntologyLoaderConfiguration#setAuthorizationValue(String)
     * @since OWL-API 5.1.4
     * @since ONT-API 1.3.0
     */
    @Override
    public OntLoaderConfiguration setAuthorizationValue(@Nonnull String s) {
        return set(OntSettings.OWL_API_AUTHORIZATION_VALUE, s);
    }

    /**
     * {@inheritDoc}
     * Always returns {@code true} since this functionality is not supported by ONT-API.
     *
     * @return {@code true}
     * @since OWL-API 5.1.5
     * @since ONT-API 1.3.0
     */
    @Override
    public boolean shouldTrimToSize() {
        LOGGER.warn("ONT-API does not support TrimToSize");
        return true;
    }

    /**
     * {@inheritDoc}
     * No-op since this functionality is not supported by ONT-API.
     *
     * @param b anything
     * @return this config instance
     * @since OWL-API 5.1.5
     * @since ONT-API 1.3.0
     */
    @Override
    public OntLoaderConfiguration setTrimToSize(boolean b) {
        LOGGER.warn("ONT-API does not support TrimToSize");
        return this;
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
     * @return this config instance
     * @since OWL-API 5.1.1
     * @since ONT-API 1.3.0
     */
    @Override
    public OntLoaderConfiguration setRepairIllegalPunnings(boolean b) {
        LOGGER.warn("ONT-API does not support RepairIllegalPunnings");
        return this;
    }

    protected Map<OntSettings, Object> asMap() {
        return OntConfig.loadMap(this.data, OntSettings.LOAD_CONFIG_KEYS);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OntLoaderConfiguration)) return false;
        OntLoaderConfiguration that = (OntLoaderConfiguration) o;
        return Objects.equals(this.asMap(), that.asMap());
    }

    @Override
    public int hashCode() {
        return Objects.hash(asMap());
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeObject(OntConfig.serializableOnly(data));
    }

}
