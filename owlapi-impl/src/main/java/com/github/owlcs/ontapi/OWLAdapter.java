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

import com.github.owlcs.ontapi.config.OntConfig;
import com.github.owlcs.ontapi.config.OntLoaderConfiguration;
import com.github.owlcs.ontapi.config.OntWriterConfiguration;
import com.github.owlcs.ontapi.internal.InternalConfig;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyFactory;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyWriterConfiguration;
import org.semanticweb.owlapi.model.OntologyConfigurator;

import java.util.Objects;

/**
 * An 'adapter' to convert OWL-API things to ONT-API ones and to gain access to internal objects.
 * Mostly it is a collection of sugar-methods:
 * anything produced by ONT-API is also OWL-API just because ONT-API is overridden OWL-API.
 * It is for internal usage only, and, therefore,
 * there is no guarantee that any methods will work in case of external call.
 *
 * @since 1.3.0
 * Created by @ssz on 01.07.2018.
 */
@SuppressWarnings("WeakerAccess")
public class OWLAdapter implements HasAdapter.Adapter {

    private static OWLAdapter instance = new OWLAdapter();

    /**
     * Gets the default {@code Adapter}.
     *
     * @return {@link HasAdapter.Adapter}
     */
    public static OWLAdapter get() {
        return instance;
    }

    /**
     * A backdoor to change behavior.
     * @param adapter {@link OWLAdapter}, an overridden instance, not {@code null}
     */
    public static void set(OWLAdapter adapter) {
        instance = Objects.requireNonNull(adapter, "Null adapter");
    }

    /**
     * Wraps {@link OWLOntologyManager} as {@link OntologyManager}.
     *
     * @param manager {@link OWLOntologyManager}
     * @return {@link OntologyManager}
     * @throws OntApiException if wrong instance specified
     */
    @Override
    public OntologyManager asONT(OWLOntologyManager manager) {
        try {
            return (OntologyManager) OntApiException.notNull(manager);
        } catch (ClassCastException c) {
            throw new OntApiException.IllegalState("Wrong Ontology Manager", c);
        }
    }

    /**
     * Wraps {@link OWLDataFactory} as {@link DataFactory}.
     *
     * @param factory {@link OWLDataFactory}
     * @return {@link DataFactory}
     * @throws OntApiException if wrong instance specified
     */
    public DataFactory asONT(OWLDataFactory factory) {
        try {
            return (DataFactory) OntApiException.notNull(factory);
        } catch (ClassCastException c) {
            throw new OntApiException.IllegalState("Wrong Ontology Data Factory", c);
        }
    }

    /**
     * Wraps {@link OntologyConfigurator} as {@link OntConfig}.
     *
     * @param conf {@link OntologyConfigurator}
     * @return {@link OntConfig}
     */
    public OntConfig asONT(OntologyConfigurator conf) {
        return conf instanceof OntConfig ? (OntConfig) conf : OntConfig.copy(conf);
    }

    /**
     * Wraps {@link OWLOntologyLoaderConfiguration} as {@link OntLoaderConfiguration}.
     *
     * @param conf {@link OWLOntologyLoaderConfiguration}
     * @return {@link OntLoaderConfiguration}
     */
    @Override
    public OntLoaderConfiguration asONT(OWLOntologyLoaderConfiguration conf) {
        return conf instanceof OntLoaderConfiguration ? (OntLoaderConfiguration) conf : new OntLoaderConfiguration(conf);
    }

    /**
     * Wraps {@link OWLOntologyWriterConfiguration} as {@link OntWriterConfiguration}.
     *
     * @param conf {@link OWLOntologyWriterConfiguration}
     * @return {@link OntWriterConfiguration}
     */
    public OntWriterConfiguration asONT(OWLOntologyWriterConfiguration conf) {
        return conf instanceof OntWriterConfiguration ? (OntWriterConfiguration) conf : new OntWriterConfiguration(conf);
    }

    /**
     * Converts id.
     *
     * @param id {@link OWLOntologyID}, not {@code null}
     * @return {@link ID}, must not be {@code null}
     */
    @Override
    public ID asONT(OWLOntologyID id) {
        return ID.asONT(id);
    }

    /**
     * Gets ontology creation handler.
     *
     * @param m {@link OWLOntologyManager} instance, not {@code null}
     * @return {@link OWLOntologyFactory.OWLOntologyCreationHandler}
     */
    public OWLOntologyFactory.OWLOntologyCreationHandler asHandler(OWLOntologyManager m) {
        return asIMPL(m);
    }

    /**
     * Casts to the default ONT-API manager implementation.
     * The implementation contains a lot of useful methods,
     * that are used by the implementation classes (e.g. by the {@link OntologyFactoryImpl Ontogy Factory Impl}),
     * but can not be moved to the manager interface, as they are not general enough, or require careful use, or for some other reasons.
     * Creation one more technical interface to describe and access them may seem a good idea, but currently I am not sure enough.
     *
     * @param manager {@link OWLOntologyManager manager}
     * @return {@link OntologyManagerImpl}
     * @throws OntApiException in case of wrong instance specified
     */
    public OntologyManagerImpl asIMPL(OWLOntologyManager manager) {
        try {
            return (OntologyManagerImpl) OntApiException.notNull(manager);
        } catch (ClassCastException c) {
            throw new OntApiException.IllegalState("Wrong Ontology Manager Impl", c);
        }
    }

    /**
     * Performs mapping {@code OWLOntology -> OntologyModel}.
     *
     * @param ont {@link OWLOntology}, not {@code null}
     * @return {@link Ontology}
     */
    public Ontology asONT(OWLOntology ont) {
        return (Ontology) ont;
    }

    /**
     * Performs mapping {@code OntologyCreator -> Builder}.
     *
     * @param creator {@link OntologyCreator}, not {@code null}
     * @return {@link OntologyFactory.Builder}
     */
    public OntologyFactory.Builder asBuilder(OntologyCreator creator) {
        return (OntologyFactory.Builder) creator;
    }

    /**
     * Performs mapping {@code OWLOntologyFactory -> OntologyFactory}.
     *
     * @param factory {@link OntologyFactory}, not {@code null}
     * @return {@link OntologyFactory}
     */
    public OntologyFactory asONT(OWLOntologyFactory factory) {
        try {
            return (OntologyFactory) OntApiException.notNull(factory);
        } catch (ClassCastException c) {
            throw new OntApiException.IllegalState("Unexpected factory instance: " + factory, c);
        }
    }

    /**
     * Performs mapping {@code OntologyModel -> InternalModelHolder}.
     *
     * @param ont {@link Ontology}, not {@code null}
     * @return {@link BaseModel}
     */
    public BaseModel asBaseModel(Ontology ont) {
        return (BaseModel) ont;
    }

    /**
     * Performs mapping {@code InternalConfig -> ModelConfig}.
     *
     * @param conf {@link InternalConfig}, not {@code null}
     * @return {@link ModelConfig}
     */
    public ModelConfig asModelConfig(InternalConfig conf) {
        return (ModelConfig) conf;
    }

}
