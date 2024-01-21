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

import com.github.owlcs.ontapi.Ontology;
import com.github.sszuev.jena.ontapi.impl.conf.ObjectFactory;
import com.github.sszuev.jena.ontapi.model.OntModel;

/**
 * A common interface to access cache settings.
 * Currently, there are several caches:
 * <ul>
 * <li>nodes cache, that is used in {@link com.github.owlcs.ontapi.internal.SearchModel} as a read optimization</li>
 * <li>objects cache, that is encapsulated in {@link com.github.owlcs.ontapi.internal.CacheObjectFactory},
 * and is used to reduce memory memory footprint when constructing OWL content</li>
 * <li>model cache, that has several levels:
 * {@link #CACHE_ALL}, {@link #CACHE_CONTENT}, {@link #CACHE_COMPONENT}, {@link #CACHE_ITERATOR}</li>
 * </ul>
 * Note: since ONT-API is an evolving system, all these settings may be changed in the future releases.
 * <p>
 * Created by @ssz on 15.03.2019.
 *
 * @since 1.4.0
 */
public interface CacheSettings {

    /**
     * A constant value signifying that iterator cache is enabled.
     * This cache helps to get axioms (and header annotations) faster using an in-memory array.
     */
    int CACHE_ITERATOR = 2;
    /**
     * A constant value signifying that component cache is enabled.
     * A component cache contains OWL-entities and OWL anonymous individuals, which are parts of OWL-content.
     *
     * @see org.semanticweb.owlapi.model.OWLEntity
     * @see org.semanticweb.owlapi.model.OWLAnonymousIndividual
     */
    int CACHE_COMPONENT = 4;
    /**
     * A constant value signifying that content cache is enabled.
     * A content cache contains OWL-axioms and ontology header annotations.
     *
     * @see org.semanticweb.owlapi.model.OWLAxiom
     * @see org.semanticweb.owlapi.model.OWLAnnotation
     */
    int CACHE_CONTENT = 16;
    /**
     * A constant value signifying that all model's caches are enabled.
     * It is default value.
     * Note that all these constants do not relate to nodes and objects caches.
     */
    int CACHE_ALL = CACHE_ITERATOR | CACHE_CONTENT | CACHE_COMPONENT;

    /**
     * Returns the maximum size of nodes cache,
     * which is used as optimization while reading OWLObjects from a graph
     * (see {@link com.github.owlcs.ontapi.internal.SearchModel}).
     * The system default size is {@code 50_000}.
     * <p>
     * Each {@link ObjectFactory object factory}
     * has its own nodes cache with the same size, but, as a rule, only a few factories have many nodes in their cache.
     * Average {@link org.apache.jena.graph.Node Node} (uri and blank) size is about 160 bytes (internal string ~ 150byte),
     * Experiments show that for the limit = 100_000, the total number of cached nodes is not more than 190_000
     * (it is for teleost and galen, significantly less for the rest tested ontologies),
     * The number 190_000 uri or blank nodes means about 30 MB.
     * Here the list of tested ontologies:
     * <ul>
     * <li>teleost(59mb, 336_291 axioms, 650_339 triples)</li>
     * <li>hp(38mb, 143_855 axioms, 367_315 triples)</li>
     * <li>galen(33mb, 96_463 axioms, 281_492 triples)</li>
     * <li>psychology(4mb, 38_872 axioms, 38_873 triples)</li>
     * <li>family(0.2mb, 2_845 axioms)</li>
     * <li>pizza(0.1mb, 945 axioms)</li>
     * </ul>
     *
     * @return int
     * @see OntSettings#ONT_API_LOAD_CONF_CACHE_NODES
     * @see CacheControl#setLoadNodesCacheSize(int)
     */
    int getLoadNodesCacheSize();

    /**
     * Returns the maximum size of objects factory cache,
     * which is used as optimization while reading OWLObjects from a graph
     * (see {@link com.github.owlcs.ontapi.internal.CacheObjectFactory}).
     * The system default size is {@code 2048}.
     * This is magic number from OWL-API impl, which has also similar caches.
     *
     * @return int
     * @see OntSettings#ONT_API_LOAD_CONF_CACHE_OBJECTS
     * @see CacheControl#setLoadObjectsCacheSize(int)
     */
    int getLoadObjectsCacheSize();

    /**
     * Returns the model content cache level.
     * Currently, there are following possible levels:
     * <ul>
     * <li>{@link #CACHE_ITERATOR} - use cache-optimization to speed up iteration over
     * the content (axioms/ontology annotations) and components (entities/anonymous individuals) found in a graph</li>
     * <li>{@link #CACHE_COMPONENT} - use cache-optimization to optimize iteration over components found in a graph</li>
     * <li>{@link #CACHE_CONTENT} - use cache-optimization to optimize iteration over content and its modification</li>
     * <li>{@link #CACHE_ALL} - all possible cache-optimizations</li>
     * </ul>
     * Note: the list above may be changed in the ONT-API evolution.
     *
     * @return int, the current level (positive) or {@code 0} in case of no cache should be used
     * @see OntSettings#ONT_API_LOAD_CONF_CACHE_MODEL
     * @see CacheControl#setModelCacheLevel(int)
     */
    int getModelCacheLevel();

    /**
     * Answers whether an internal model content cache is enabled, that is {@code true} by default.
     * <p>
     * Disabling internal content cache may be useful in case the ontology is too large to fit in memory.
     * In the normal case, it is better not to turn off this cache.
     * <p>
     * An internal model content cache speedups axiom listing and controls add/remove components behaviour.
     * If it is turned off,
     * then the direct graph traversal is used for retrieving axioms and ontology header (annotations).
     * Warning: in that case the adding and removing axioms is disabled in the model level.
     * For this there are two reasons:
     * <ul>
     * <li>OWL-API allows some uncertainty in axiom's definition,
     * the same data amount can be represented as an axiom in various ways
     * (more about this see in the description of
     * the method {@link Ontology#clearCache()}).
     * It follows from this that the newly added axiom may not be found in the same form as it was,
     * which may confuse</li>
     * <li>Currently,
     * the {@link org.semanticweb.owlapi.model.OWLOntology#removeAxiom(org.semanticweb.owlapi.model.OWLAxiom)}
     * operation requires some analytics to decide which part of an axiom can be really deleted from the graph,
     * which is also a consequence of OWL-API ambiguity.
     * This implies some calculations that may take a long time if there is no cache</li>
     * </ul>
     * But non-modifiability concerns only the top-level {@link Ontology OWL Model} interface.
     * If it is not restricted in some other place, a graph is editable.
     * So it is possible to modify model using {@link OntModel} interface
     * (see the method {@link Ontology#asGraphModel()}).
     * Also, to add axiom a {@link com.github.owlcs.ontapi.internal.AxiomTranslator} mechanism can be used,
     * e.g. to add the axiom {@code A} into the RDF Model {@code m},
     * the expression {@code AxiomParserProvider.get(A).writeAxiom(A, m)} can be used.
     * <p>
     * Also, please note: the cache ensures no duplicates in any {@code Stream}, returned by axioms-listing methods.
     * When cache is disabled, this restriction is removed.
     * For example, the couple of triples {@code x owl:differentFrom y . y owl:differentFrom x}
     * will produce two identical (by {@code equals(Object)} and {@code hashCode()}) axioms.
     *
     * @return boolean
     * @see CacheControl#setModelCacheLevel(int, boolean)
     * @see CacheControl#setModelCacheLevel(int)
     */
    default boolean useContentCache() {
        return (getModelCacheLevel() & CACHE_CONTENT) == CACHE_CONTENT;
    }

    /**
     * Answers whether an internal model component cache is enabled, that is {@code true} by default.
     * This cache consists of {@link org.semanticweb.owlapi.model.OWLEntity OWL entities}
     * and {@link org.semanticweb.owlapi.model.OWLAnonymousIndividual anonymous individuals}
     * and used when any ontology signature method is called.
     *
     * @return boolean
     * @see CacheControl#setModelCacheLevel(int)
     */
    default boolean useComponentCache() {
        return (getModelCacheLevel() & CACHE_COMPONENT) == CACHE_COMPONENT;
    }

    /**
     * Answers {@code true} iff cache iterator optimization is enabled.
     *
     * @return boolean
     * @see CacheControl#setModelCacheLevel(int)
     */
    default boolean useIteratorCache() {
        return (getModelCacheLevel() & CACHE_ITERATOR) == CACHE_ITERATOR;
    }

    /**
     * Answers {@code true} if the nodes cache is enabled.
     * This cache is located in the search model, that is used as optimization while read operations.
     * The search model contains several other minor optimizations, so
     * if the method returns {@code true} all of them are enabled, and opposite:
     * if the method returns {@code false}, all optimizations, not only the nodes cache, are disabled
     *
     * @return boolean
     */
    default boolean useLoadNodesCache() {
        return getLoadNodesCacheSize() > 0;
    }

    /**
     * Answers {@code true} if objects cache is enabled.
     *
     * @return boolean
     */
    default boolean useLoadObjectsCache() {
        return getLoadObjectsCacheSize() > 0;
    }
}
