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

import com.github.owlcs.ontapi.OntFormat;
import com.github.owlcs.ontapi.OntGraphDocumentSource;
import com.github.owlcs.ontapi.Ontology;
import com.github.owlcs.ontapi.OntologyFactory;
import com.github.owlcs.ontapi.OntologyManager;
import com.github.owlcs.ontapi.transforms.GraphFilter;
import com.github.owlcs.ontapi.transforms.GraphTransformers;
import org.apache.jena.graph.Graph;
import org.apache.jena.ontapi.OntSpecification;
import org.apache.jena.ontapi.model.OntModel;
import org.semanticweb.owlapi.model.IRI;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * A common interface to access loading settings.
 * The settings from configs {@link AxiomsSettings} and {@link CacheSettings}
 * can be changed during the ontology model evolution,
 * but the settings of this config cannot be changed after the ontology is loaded.
 * Note: all original OWL-API options are actually loading settings.
 * Created by @szz on 14.05.2019.
 *
 * @since 1.4.0
 */
public interface LoadSettings {

    /**
     * Gets {@link OntSpecification} configuration object.
     *
     * @return {@link OntSpecification}
     * @see LoadControl#setSpecification(OntSpecification, String)
     * @see OntSettings#ONT_API_LOAD_CONF_SPECIFICATION
     */
    OntSpecification getSpecification();

    /**
     * Gets {@link GraphTransformers Transformers Store}.
     * <p>
     * Transformers Store is a collection of actions that are performed on {@code Graph} before
     * it enters into the main system.
     * One of the goals of this mechanism is a data transformation in accordance with OWL2 specification.
     * Any {@link org.apache.jena.graph.Graph RDF Graph} can be presented by the system
     * in the form of {@link OntModel RDF Model}
     * or {@link Ontology Axiomatic Model}, but without a proper transformation
     * you may not find a valid {@link org.semanticweb.owlapi.model.OWLAxiom Axiom}s inside that data.
     * Transformations can be simply disabled through
     * the method {@link LoadControl#setGraphTransformers(GraphTransformers)}.
     * It is recommended to do this in case there is confidence that the data is OWL2,
     * since transformations may take significant processor time.
     * With the method {@link GraphTransformers#setFilter(GraphFilter)}
     * a whole graph family may be skipped from the transformation process.
     * And there is one more facility to selectively turn off transformations:
     * the method {@link OntGraphDocumentSource#withTransforms()},
     * which is used while passing externally loaded graph into the manager
     * (see {@link OntologyManager#addOntology(Graph)}).
     *
     * @return {@link GraphTransformers}
     * @see LoadControl#setGraphTransformers(GraphTransformers)
     * @see LoadControl#setPerformTransformation(boolean)
     * @see OntSettings#ONT_API_LOAD_CONF_TRANSFORMERS
     */
    GraphTransformers getGraphTransformers();

    /**
     * Answers {@code true} if the mechanism of graph transformers is enabled, which is {@code true} by default.
     * If it is enabled, the process of any document RDF source will begin with the graph transformers,
     * otherwise a raw graph will be loaded as it is.
     * In the last case, the final ontology may not contain OWL2 data (with except of Ontology ID).
     * If you pretty sure that you are loading OWL2 ontology,
     * it might make sense to disable transformers using
     * the method {@link LoadControl#setPerformTransformation(boolean)},
     * since graph transformers require an additional processor time.
     * For more info see the description for the {@link #getGraphTransformers()} method.
     *
     * @return boolean, ({@code true} by default)
     * @see LoadControl#setPerformTransformation(boolean)
     * @see OntSettings#ONT_API_LOAD_CONF_PERFORM_TRANSFORMATIONS
     */
    boolean isPerformTransformation();

    /**
     * Answers {@code true} if the imports' processing is enabled.
     * In this case the document source loading entails {@code owl:imports} processing
     * and subsequent loading all related to them sources.
     * If the imports' processing is disabled,
     * then no additional work is performed and one document source is matched with one ontology.
     * Note: there is also possible to prohibit the processing of one IRI
     * (see {@link OntConfig#addIgnoredImport(IRI)} and {@link OntLoaderConfiguration#addIgnoredImport(IRI)})
     * or whole scheme (see {@link LoadControl#setSupportedSchemes(List)}).
     *
     * @return boolean, ({@code true} by default)
     * @see LoadControl#setProcessImports(boolean)
     * @see org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration#getMissingImportHandlingStrategy()
     * @see org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration#isIgnoredImport(IRI)
     * @see OntSettings#ONT_API_LOAD_CONF_PROCESS_IMPORTS
     * @since 1.4.1
     */
    boolean isProcessImports();

    /**
     * Answers {@code true} if the native OWL-API parsers must be preferred while loading ontology,
     * which is strongly not recommended (and the method returns {@code false} by default).
     * <p>
     * The method is used by the {@link OntologyFactory Ontology Factory} default implementation
     * to choose the preferable way to load.
     * If this parameter is set to {@code false} then ONT-API (i.e., Apache Jena) loading mechanisms
     * are used in case the document format is supported both by Jena and OWL-API
     * (like Turtle, RDF/XML, etc. See {@link OntFormat}).
     * Otherwise, loading is always performed using only the native OWL-API Parsers,
     * which, actually, do not read RDF graph fully, but rather assemble it axiom by axiom.
     * Remember, the OWL-API loading mechanisms (as well as OWL-API default impl) are OWL-centric.
     * As a result, in fact, they work buggy in many cases (e.g., when data is not produced by OWL-API itself).
     * A data {@code Graph} may not present correct OWL2, but be rather RDFS, or whatever else.
     * For example, <a href="http://spinrdf.org/spin">SPIN</a> ontology contains a lot of SPARQL queries
     * in a special spin form, that is using custom {@code rdf:List}s
     * (an example of such []-list is the right part of any triple with predicate {@code sp:where}).
     * After loading such an ontology with the help of OWL Turtle Parser (checked v 5.1.4),
     * it will contain garbage instead of the original constructs.
     * With the default ONT-API loading mechanism you get {@code Graph} as it is,
     * but with, maybe, several controlled changes caused by the transformers (see {@link #getGraphTransformers()}).
     * <b>So please do not change the default value without a good reason</b>
     *
     * @return boolean, ({@code false} by default)
     * @see LoadControl#setUseOWLParsersToLoad(boolean)
     * @see OntSettings#ONT_API_LOAD_CONF_USE_OWL_PARSERS_TO_LOAD
     */
    boolean isUseOWLParsersToLoad();

    /**
     * Answers a {@code Collection} of allowed {@link Scheme}-controllers.
     * This mechanism is used during preliminary analysis of {@link IRI} before loading a document.
     * If an {@link IRI} fails any of the {@link Scheme}-tests from the {@code Collection},
     * the document source is rejected and the system throws an exception.
     * Note: there is also possible to prohibit the processing of only one IRI
     * (see {@link OntConfig#addIgnoredImport(IRI)} and {@link OntLoaderConfiguration#addIgnoredImport(IRI)})
     * or all imports (see {@link LoadControl#setProcessImports(boolean)}).
     *
     * @return unmodifiable {@code Collection} of supported {@link Scheme schema}s
     * @see LoadControl#setSupportedSchemes(List)
     * @see OntSettings#ONT_API_LOAD_CONF_SUPPORTED_SCHEMES
     * @see LoadControl#isProcessImports()
     */
    Collection<Scheme> getSupportedSchemes();

    /**
     * An interface that represents an IRI schema,
     * intended to control source {@link IRI}s while loading.
     */
    interface Scheme extends Serializable {

        /**
         * Answers the scheme controller identifier.
         *
         * @return String
         */
        String key();

        /**
         * Answers {@code true} if the given IRI is allowed by this controller.
         * It is free for overriding.
         *
         * @param iri {@link IRI}
         * @return boolean
         */
        default boolean same(IRI iri) {
            return iri != null && Objects.equals(key(), iri.getScheme());
        }
    }
}
