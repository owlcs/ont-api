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

package com.github.owlcs.ontapi;

import com.github.owlcs.ontapi.internal.InternalCache;
import com.github.owlcs.ontapi.internal.InternalConfig;
import com.github.owlcs.ontapi.internal.InternalModel;
import com.github.owlcs.ontapi.internal.InternalModelImpl;
import com.github.owlcs.ontapi.jena.impl.conf.OntModelConfig;
import com.github.owlcs.ontapi.jena.impl.conf.OntPersonality;
import com.github.owlcs.ontapi.jena.model.OntModel;
import org.apache.jena.graph.Graph;
import org.semanticweb.owlapi.model.OWLPrimitive;

import java.util.Map;

/**
 * A technical interface-helper that serves as a bridge between {@link OntModel Jena RDF model} and
 * {@link org.semanticweb.owlapi.model.OWLOntology OWLAPI ontology} through the {@link InternalModel} on the one hand,
 * and between all these things and {@link org.semanticweb.owlapi.model.OWLOntologyManager OWLAPI manager}
 * through the {@link ModelConfig} on the other hand.
 * Also, it is a collection of factory-methods to produce various {@link InternalModel} instances.
 * Note: this is an internal mechanism that can be changed at any time.
 * <p>
 * Created by @szuev on 07.04.2017.
 */
public interface BaseModel {

    /**
     * Returns an encapsulated {@link InternalModel} instance -
     * the facility to work both with Jena and OWL-API objects simultaneously.
     *
     * @return {@link InternalModel}
     */
    InternalModel getBase();

    /**
     * Sets new internals.
     * Not for public use: only Java Serialization mechanisms can explicitly call this method.
     *
     * @param m {@link InternalModel}, not {@code null}
     */
    void setBase(InternalModel m);

    /**
     * Returns a model config instance, that is a collection of settings and, also,
     * a facility which binds together the ontology and manager.
     *
     * @return {@link ModelConfig}
     */
    ModelConfig getConfig();

    /**
     * Sets new model config.
     * Not for public use: only Java Serialization mechanisms can explicitly call this method.
     *
     * @param conf {@link ModelConfig}, not {@code null}
     */
    void setConfig(ModelConfig conf);

    /**
     * A factory method to create {@link InternalModel} instance with default settings.
     * Can be used to provide a dummy instance for testing or debugging.
     *
     * @param graph {@link Graph}, not {@code null}
     * @return {@link InternalModel}
     */
    static InternalModel createInternalModel(Graph graph) {
        return createInternalModel(graph,
                OntModelConfig.getPersonality(),
                InternalConfig.DEFAULT,
                OntManagers.getDataFactory(),
                null);
    }

    /**
     * A primary factory method to create fresh {@link InternalModel}.
     *
     * @param graph       {@link Graph}, not {@code null}, a base data-store
     * @param personality {@link OntPersonality}, not {@code null}, to manage {@code OntObject}s
     * @param config      {@link InternalConfig}, not {@code null}, to control behavior
     * @param dataFactory {@link DataFactory}, not {@code null}, to produces {@code OWLObject}s
     * @param caches      a {@code Map} with {@link OWLPrimitive} class-types as keys
     *                    and manager-wide {@link InternalCache}s as values
     *                    to enable data sharing between different ontologies
     * @return {@link InternalModel}
     */
    static InternalModel createInternalModel(Graph graph,
                                             OntPersonality personality,
                                             InternalConfig config,
                                             DataFactory dataFactory,
                                             Map<Class<? extends OWLPrimitive>, InternalCache<?, ?>> caches) {
        return new InternalModelImpl(OntApiException.notNull(graph, "Null graph."),
                OntApiException.notNull(personality, "Null personality."),
                OntApiException.notNull(config, "Null config."),
                OntApiException.notNull(dataFactory, "Null data-factory"),
                caches);
    }

}
