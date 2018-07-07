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

package ru.avicomp.ontapi;

import org.apache.jena.graph.Graph;
import org.semanticweb.owlapi.io.*;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.OntologyCopy;
import ru.avicomp.ontapi.config.OntConfig;
import ru.avicomp.ontapi.config.OntLoaderConfiguration;
import ru.avicomp.ontapi.config.OntWriterConfiguration;
import ru.avicomp.ontapi.jena.model.OntGraphModel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;


/**
 * An ONT-API Ontology manager, which is an extended {@link OWLOntologyManager OWL-API manager}
 * It is the main point for creating, loading and accessing {@link OntologyModel Ontology Model}s.
 * Any ontology in this manager is a wrapper around a {@link Graph Jena Graph},
 * which may be linked to the another ontology through {@link ru.avicomp.ontapi.jena.UnionGraph UnionGraph} interface.
 * <p>
 * The following methods are new (i.e. added in ONT-API) and extend the original functionality provided by the OWL-API:
 * <ul>
 * <li>{@link #addOntology(Graph)} - since 1.0.1</li>
 * <li>{@link #addOntology(Graph, OntLoaderConfiguration)} - since 1.2.0</li>
 * <li>{@link #createGraphModel(String)}</li>
 * <li>{@link #createGraphModel(String, String)}</li>
 * <li>{@link #models()}</li>
 * <li>{@link #getGraphModel(String)}</li>
 * <li>{@link #getGraphModel(String, String)}</li>
 * <li>{@code addDocumentSourceMapper(mapping)} - since 1.0.1, now deprecated</li>
 * <li>{@code removeDocumentSourceMapper(mapping)} - since 1.0.1, now deprecated</li>
 * <li>{@code documentSourceMappers()} - since 1.0.1, now deprecated</li>
 * <li>{@link #getDocumentSourceMappers()} - since 1.2.1</li>
 * </ul>
 * <p>
 * Created by szuev on 24.10.2016.
 */
public interface OntologyManager extends OWLOntologyManager {

    /**
     * Returns a loading config, that is an immutable extended instance of
     * {@link OWLOntologyLoaderConfiguration OWL-API Ontology Loader Configuration}.
     * Be warned: it is a read only accessor, to change configuration create a new config instance (using any its setter)
     * and pass it back to the manager using
     * the {@link #setOntologyLoaderConfiguration(OWLOntologyLoaderConfiguration)} method.
     * Any ontology has its own config,
     * which reflects managers state at the moment of creating or loading.
     *
     * @return {@link OntLoaderConfiguration}
     */
    @Override
    OntLoaderConfiguration getOntologyLoaderConfiguration();

    /**
     * Returns a writer config, that is an immutable extended instance of
     * {@link OWLOntologyWriterConfiguration OWL-API Ontology Writer Configuration}.
     * For more information see notes in the description of the {@link #getOntologyLoaderConfiguration()} method.
     *
     * @return {@link OntWriterConfiguration}
     */
    @Override
    OntWriterConfiguration getOntologyWriterConfiguration();

    /**
     * Returns the managers config, which is an extended {@link OntologyConfigurator OWL API Global Configuration} and
     * also a factory to create the snapshot configs {@link OntLoaderConfiguration} and {@link  OntWriterConfiguration}).
     * This configuration is modifiable, but any change in it does not affect existing ontologies,
     * only newly added ontologies pick up these settings (unless specific configurations are explicitly specified).
     * The initial state is copied from {@code /ontapi.properties} file, which should be placed in class path of application.
     *
     * @return {@link OntConfig}
     */
    @Override
    OntConfig getOntologyConfigurator();

    /**
     * Gets an {@link RWLockedCollection extended OWL-API PriorityCollection} of {@link OntologyFactory Ontology Factories}
     * - an iterable object, which allows to iterate and modify an internal collection.
     * Warning: any attempt to add OWLOntologyFactory into that Priority Collection
     * will cause throwing an {@link OntApiException ONT-API runtime exception}
     * in case that factory does not extend {@code OntologyFactory} interface.
     *
     * @return {@link RWLockedCollection} of {@link OntologyFactory}
     */
    @Override
    RWLockedCollection<OWLOntologyFactory> getOntologyFactories();


    /**
     * Gets an {@link RWLockedCollection extended OWL-API PriorityCollection} of {@link OWLOntologyIRIMapper IRI Mappers}
     * The mappers are used to obtain ontology document IRIs for ontology IRIs.
     * If their type is annotated with a {@link org.semanticweb.owlapi.annotations.HasPriority HasPriority} type,
     * this will be used to decide the order they are used.
     * Otherwise, the order in which the collection is iterated will determine the order in which the mappers are used.
     *
     * @return {@link RWLockedCollection} of {@link OWLOntologyIRIMapper}s
     */
    @Override
    RWLockedCollection<OWLOntologyIRIMapper> getIRIMappers();

    /**
     * Gets an {@link RWLockedCollection extended OWL-API PriorityCollection} of {@link DocumentSourceMapping ONT Document Source Mapping}.
     * A {@link DocumentSourceMapping} is more general mechanism to conduct ontology mapping than {@link OWLOntologyIRIMapper},
     * it is widely used in dependent projects.
     *
     * @return {@link RWLockedCollection} of {@link DocumentSourceMapping}s
     * @since 1.2.1
     */
    RWLockedCollection<DocumentSourceMapping> getDocumentSourceMappers();

    /**
     * Gets an {@link RWLockedCollection extended OWL-API PriorityCollection} of {@link OWLParserFactory OWL Parsers}.
     * If the parsers are annotated with a {@link org.semanticweb.owlapi.annotations.HasPriority HasPriority} type,
     * this will be used to decide the order they are used.
     * Otherwise, the order in which the collection is iterated will determine the order in which the parsers are used.
     *
     * @return {@link RWLockedCollection} of {@link OWLParserFactory}s
     */
    @Override
    RWLockedCollection<OWLParserFactory> getOntologyParsers();

    /**
     * Gets an {@link RWLockedCollection extended OWL-API PriorityCollection} of {@link OWLStorerFactory OWL Storers}.
     * About ordering see {@link org.semanticweb.owlapi.annotations.HasPriority HasPriority} annotation.
     *
     * @return {@link RWLockedCollection} of {@link OWLStorerFactory}s
     */
    @Override
    RWLockedCollection<OWLStorerFactory> getOntologyStorers();

    /**
     * Gets an ontology by IRI.
     * Contrary to the original description this method works with version IRI also if it fails with ontology IRI.
     *
     * @param iri {@link IRI} ontology IRI or version IRI as can be seen from the OWL-API implementation
     *            (see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLOntologyManagerImpl.java'>uk.ac.manchester.cs.owl.owlapi.OWLOntologyManagerImpl#getOntology(IRI)</a>)
     * @return {@link OntologyModel} or {@code null}
     */
    @Override
    OntologyModel getOntology(@Nonnull IRI iri);

    /**
     * Finds ontology by the specified {@code id} (could be anonymous).
     * If there is no such ontology it tries to find the first with the same ontology IRI as in the specified {@code id}.
     *
     * @param id {@link OWLOntologyID} ID
     * @return {@link OntologyModel} or {@code null}
     * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLOntologyManagerImpl.java'>uk.ac.manchester.cs.owl.owlapi.OWLOntologyManagerImpl#getOntology(OWLOntologyID)</a>
     * @see #contains(OWLOntologyID)
     */
    @Override
    OntologyModel getOntology(@Nonnull OWLOntologyID id);

    /**
     * Answers {@code true} if the manager contains an ontology with the given ontology {@code iri}.
     * See description for {@link #getOntology(IRI)} and be warned!
     *
     * @param iri {@link IRI} the ontology iri or version iri
     * @return true if ontology exists
     * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLOntologyManagerImpl.java'>uk.ac.manchester.cs.owl.owlapi.OWLOntologyManagerImpl#contains(IRI)</a>
     */
    @Override
    boolean contains(@Nonnull IRI iri);

    /**
     * Answers {@code true} if the manager contains an ontology with the given ontology {@code id}.
     * Be warned: this method returns always {@code false} for any anonymous id.
     * For non-anonymous id it performs searching by ontology iri ignoring version iri.
     * This is in order to make the behaviour the same as OWL-API method.
     * To find an anonymous ontology use {@link #ontologies()} stream with filters.
     *
     * @param id {@link OWLOntologyID} the id
     * @return true if {@code id} is not anonymous and there is an ontology with the same iri as in the specified {@code id}
     * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLOntologyManagerImpl.java'>uk.ac.manchester.cs.owl.owlapi.OWLOntologyManagerImpl#contains(OWLOntologyID)</a>
     */
    @Override
    boolean contains(@Nonnull OWLOntologyID id);

    /**
     * Given an imports declaration, obtains the ontology that this import has been resolved to.
     *
     * @param iri {@link OWLImportsDeclaration} the declaration that points to the imported ontology
     * @return {@link OntologyModel} the ontology that the imports declaration resolves to,
     * or {@code null} if the imports declaration could not be resolved to an ontology,
     * because the ontology was not loaded or has been removed from this manager
     * @see OWLOntologyManager#getImportedOntology(OWLImportsDeclaration)
     */
    @Nullable
    OntologyModel getImportedOntology(@Nonnull OWLImportsDeclaration iri);

    /**
     * Creates a fresh ontology with the specified {@code id}.
     * <p>
     * Note: this method doesn't throw a checked exception {@link OWLOntologyCreationException} like OWL-API.
     * Instead it there is an unchecked exception {@link OntApiException} which may wrap {@link OWLOntologyCreationException}.
     * OWL-API and ONT-API work in different ways.
     * So I see it is impossible to save the same places for exceptions.
     * For example in OWL-API you could expect some kind of exception during saving ontology-graph,
     * but in ONT-API there would be another kind of exception during adding axiom before any saving.
     * I believe there is no reasons to save the same behaviour with exceptions everywhere.
     * The return type is also changed from {@link org.semanticweb.owlapi.model.OWLOntology} to our class {@link OntologyModel}.
     * Also, it is looks a bit strange when a method, which is not affected by any resources throws a checked exception.
     *
     * @param id {@link OWLOntologyID}
     * @return ontology {@link OntologyModel}
     * @throws OntApiException in case something wrong.
     */
    @Override
    OntologyModel createOntology(@Nonnull OWLOntologyID id);

    /**
     * Creates an ontology model from the {@link Graph Jena Graph}, taking into account the given loading settings.
     * This is a new (ONT-API) method.
     *
     * @param graph {@link Graph}
     * @param conf  {@link OntLoaderConfiguration}
     * @return {@link OntologyModel}
     * @see OntGraphDocumentSource
     * @since 1.2.0
     */
    OntologyModel addOntology(@Nonnull Graph graph, @Nonnull OntLoaderConfiguration conf);

    /**
     * Copies an ontology to the manager.
     * Note: the axioms list may differ in source and result due to different config settings etc.
     * TODO: this method should not throw checked exception, in ONT-API it doesn't make sense, see {@link #createOntology(OWLOntologyID)} explanation.
     *
     * @param source   {@link OWLOntology} the source, could be pure OWL-API ontology
     * @param settings {@link OntologyCopy} the settings
     * @return new {@link OntologyModel}
     * @throws OWLOntologyCreationException in case of error.
     * @throws OntApiException              if any
     * @see OWLOntologyManager#copyOntology(OWLOntology, OntologyCopy)
     */
    @Override
    OntologyModel copyOntology(@Nonnull OWLOntology source, @Nonnull OntologyCopy settings) throws OWLOntologyCreationException;

    /**
     * Loads an ontology by {@code source} IRI.
     * Notice: if source contains any {@code owl:imports} they will be processed also,
     * i.e. this method loads not only single ontology in general case.
     *
     * @param source {@link IRI} the IRI that identifies the desirable ontology:
     *               it can be an ontology IRI or document IRI to load directly,
     *               also it can be mapped to some other IRI using {@link OWLOntologyIRIMapper}
     *               or to some document source using {@link DocumentSourceMapping}.
     * @return {@link OntologyModel}
     * @throws OWLOntologyCreationException if there is a problem in creating and loading the ontology
     * @see OWLOntologyManager#loadOntology(IRI)
     */
    @Override
    OntologyModel loadOntology(@Nonnull IRI source) throws OWLOntologyCreationException;

    /**
     * Loads an ontology from the specified {@link OWLOntologyDocumentSource document source} using
     * {@link OWLOntologyLoaderConfiguration Loader Configuration} to setup loading process.
     * It is most general way to load an ontology to the manager.
     * Notice: if the source contains any {@code owl:imports}, which absent in the manager, they will be processed also,
     * i.e. this method loads not only single ontology in general case.
     *
     * @param source {@link OWLOntologyDocumentSource} the document source to produce i
     * @param config {@link OWLOntologyLoaderConfiguration} the loading settings, see {@link OntLoaderConfiguration}
     * @return {@link OntologyModel}
     * @throws OWLOntologyCreationException in case any error occurs during reading source or construct the ontology
     * @see OWLOntologyManager#loadOntologyFromOntologyDocument(OWLOntologyDocumentSource, OWLOntologyLoaderConfiguration)
     */
    @Override
    OntologyModel loadOntologyFromOntologyDocument(@Nonnull OWLOntologyDocumentSource source,
                                                   @Nonnull OWLOntologyLoaderConfiguration config) throws OWLOntologyCreationException;

    /**
     * Sets the collection of ontology factories.
     * Warning: if the given collection ({@code factories}) contains an instance that does not implement {@link OntologyFactory}
     * an exception is expected.
     * This method also takes into account {@link org.semanticweb.owlapi.annotations.HasPriority} annotation.
     * But I don't think anyone uses that ordering mechanism, at least with ONT-API.
     *
     * @param factories the factories to be injected
     * @throws OntApiException in case input Set contains a not {@link OntologyFactory} implementation
     * @see #getOntologyFactories()
     * @deprecated use {@code getOntologyFactories().set(factories)} instead
     */
    @Deprecated
    @Override
    default void setOntologyFactories(@Nonnull Set<OWLOntologyFactory> factories) throws OntApiException {
        getOntologyFactories().set(factories);
    }

    /**
     * Sets the collection of IRI mappers.
     * The mappers are used to obtain ontology document IRIs for ontology IRIs.
     * If their type is annotated with a {@link org.semanticweb.owlapi.annotations.HasPriority HasPriority} type,
     * this will be used to decide the order they are used.
     * Otherwise, the order in which the collection is iterated will determine the order in which the mappers are used.
     *
     * @param mappers Set of {@link OWLOntologyIRIMapper IRI mappers} to be injected
     * @see #getIRIMappers()
     * @deprecated use {@code getIRIMappers().set(mappers)} instead
     */
    @Deprecated
    @Override
    default void setIRIMappers(@Nonnull Set<OWLOntologyIRIMapper> mappers) {
        getIRIMappers().set(mappers);
    }

    /**
     * Adds an IRI mapper to the manager.
     *
     * @param mapper {@link OWLOntologyIRIMapper}, not null
     * @see #getIRIMappers()
     * @deprecated use {@code getIRIMappers().add(mapper)} instead
     */
    @Deprecated
    @Override
    default void addIRIMapper(@Nonnull OWLOntologyIRIMapper mapper) {
        getIRIMappers().add(mapper);
    }

    /**
     * Removes an IRI mapper from the manager.
     *
     * @param mapper {@link OWLOntologyIRIMapper}
     * @see #getIRIMappers()
     * @deprecated use {@code getIRIMappers().remove(mapper)} instead
     */
    @Deprecated
    @Override
    default void removeIRIMapper(@Nonnull OWLOntologyIRIMapper mapper) {
        getIRIMappers().remove(mapper);
    }

    /**
     * Clears the manager mappers.
     *
     * @see #getIRIMappers()
     * @deprecated use {@code getIRIMappers().clear()} instead
     */
    @Deprecated
    @Override
    default void clearIRIMappers() {
        getIRIMappers().clear();
    }

    /**
     * Returns document-source-mappings as stream.
     * New (ONT-API) method.
     *
     * @return Stream of {@link DocumentSourceMapping}
     * @since 1.0.1
     * @deprecated use {@code getDocumentSourceMappers().stream()} instead
     */
    @Deprecated
    default Stream<DocumentSourceMapping> documentSourceMappers() {
        return getDocumentSourceMappers().stream();
    }

    /**
     * Adds Document Source Mapping to the manager.
     * New (ONT-API) method.
     *
     * @param mapper {@link DocumentSourceMapping}
     * @since 1.0.1
     * @deprecated use {@code getDocumentSourceMappers().add(mapper)} instead
     */
    @Deprecated
    default void addDocumentSourceMapper(DocumentSourceMapping mapper) {
        getDocumentSourceMappers().add(mapper);
    }

    /**
     * Removes Document Source Mapping from the manager.
     * New (ONT-API) method.
     *
     * @param mapper {@link DocumentSourceMapping}
     * @since 1.0.1
     * @deprecated use {@code getDocumentSourceMappers().remove(mapper)} instead
     */
    @Deprecated
    default void removeDocumentSourceMapper(DocumentSourceMapping mapper) {
        getDocumentSourceMappers().remove(mapper);
    }

    /**
     * Sets the java.util.Set of OWL parsers into the manager.
     *
     * @param parsers Set of {@link OWLParserFactory}s
     * @see #getOntologyParsers()
     * @deprecated use {@code getOntologyParsers().set(parsers)} instead
     */
    @Deprecated
    @Override
    default void setOntologyParsers(@Nonnull Set<OWLParserFactory> parsers) {
        getOntologyParsers().set(parsers);
    }

    /**
     * Sets the java.util.Set of OWL storers into the manager.
     *
     * @param storers Set of {@link OWLStorerFactory}s
     * @see #getOntologyStorers()
     * @deprecated use {@code getOntologyStorers().set(storers)} instead
     */
    @Deprecated
    @Override
    default void setOntologyStorers(@Nonnull Set<OWLStorerFactory> storers) {
        getOntologyStorers().set(storers);
    }

    /**
     * Adds the OWL storer factory into the manager.
     *
     * @param storer {@link OWLStorerFactory}
     * @see #getOntologyStorers()
     * @deprecated use {@code getOntologyStorers().add(storer)} instead
     */
    @Deprecated
    @Override
    default void addOntologyStorer(@Nonnull OWLStorerFactory storer) {
        getOntologyStorers().add(storer);
    }

    /**
     * Removes the OWL storer factory from the manager.
     *
     * @param storer {@link OWLStorerFactory}
     * @see #getOntologyStorers()
     * @deprecated use {@code getOntologyStorers().remove(storer)} instead
     */
    @Deprecated
    @Override
    default void removeOntologyStorer(@Nonnull OWLStorerFactory storer) {
        getOntologyStorers().remove(storer);
    }

    /**
     * Clears the manager OWL storer factories collection.
     *
     * @see #getOntologyStorers()
     * @deprecated use {@code getOntologyStorers().clear()} instead
     */
    @Deprecated
    @Override
    default void clearOntologyStorers() {
        getOntologyStorers().clear();
    }

    /**
     * Puts a graph to the manager.
     * This is a new (ONT-API) method.
     * Note: graph transformation are not performed.
     *
     * @param graph {@link Graph}
     * @return {@link OntologyModel}
     * @since 1.0.1
     */
    default OntologyModel addOntology(@Nonnull Graph graph) {
        return addOntology(graph, getOntologyLoaderConfiguration().setPerformTransformation(false));
    }

    /**
     * Creates a new (empty) ontology that does not have an ontology IRI
     * (and therefore does not have a version IRI).
     * A document IRI will automatically be generated.
     *
     * @return {@link OntologyModel} the newly created ONT-API ontology instance
     * @see OWLOntologyManager#createOntology()
     */
    default OntologyModel createOntology() {
        return createOntology(new OWLOntologyID());
    }

    /**
     * Creates a new (empty) ontology that has the specified ontology IRI (and no version IRI).
     * The ontology document IRI of the created ontology will be set to the value returned by any
     * installed {@link OWLOntologyIRIMapper IRI Mapper}s.
     * If no mappers are installed or the ontology IRI was not mapped to a document IRI,
     * then the ontology document IRI will be set to the value of {@code iri}.
     *
     * @param iri {@link IRI} the IRI of the ontology to be created
     * @return {@link OntologyModel} the newly created ONT-API ontology instance
     * @throws OntApiException in case of error, this exception can wrap one of the OWL-API checked exception,
     *                         e.g. {@link OWLOntologyCreationException}, {@link OWLOntologyAlreadyExistsException},
     *                         {@link  OWLOntologyDocumentAlreadyExistsException}.
     * @see OWLOntologyManager#createOntology(IRI)
     */
    default OntologyModel createOntology(@Nullable IRI iri) {
        return createOntology(new OWLOntologyID(Optional.ofNullable(iri), Optional.empty()));
    }

    /**
     * Loads an ontology from a given document source using default settings.
     * Notice: if ontology contains reference to other ontologies (using {@code owl:imports}),
     * they will be processed also.
     * In this case, take into account the possible internet diving to retrieve missed ontologies.
     *
     * @param source {@link OWLOntologyDocumentSource}
     * @return {@link OntologyModel}
     * @throws OWLOntologyCreationException if something is wrong
     * @see OWLOntologyManager#loadOntologyFromOntologyDocument(OWLOntologyDocumentSource)
     */
    @Override
    default OntologyModel loadOntologyFromOntologyDocument(@Nonnull OWLOntologyDocumentSource source)
            throws OWLOntologyCreationException {
        return loadOntologyFromOntologyDocument(source, getOntologyLoaderConfiguration());
    }

    /**
     * Loads an ontology from an ontology document specified by an IRI.
     * In contrast the the {@link #loadOntology(IRI)} method, <i>no mapping</i> is performed on the specified IRI.
     * The loading settings are default (see {@link #getOntologyLoaderConfiguration()}).
     * Notice: if ontology contains reference to other ontologies (using {@code owl:imports}),
     * they will be processed also, i.e. in general case if process finishes without any error,
     * not only a single ontology may be loaded.
     * In case of dependent ontologies all mappings
     * ({@link OWLOntologyIRIMapper IRI Mapping} and @link DocumentSourceMapping Document Source Mapping})
     * are included into consideration.
     *
     * @param iri {@link IRI} the ontology document IRI where the ontology will be loaded from
     * @return {@link OntologyModel} the newly loaded ONT-API ontology instance
     * @throws OWLOntologyCreationException if there is a problem in creating or loading the ontology
     * @see OWLOntologyManager#loadOntologyFromOntologyDocument(IRI)
     */
    @Override
    default OntologyModel loadOntologyFromOntologyDocument(@Nonnull IRI iri) throws OWLOntologyCreationException {
        return loadOntologyFromOntologyDocument(new IRIDocumentSource(iri));
    }

    /**
     * Loads an ontology from a file using default {@link OntLoaderConfiguration Loader Settings}.
     * Notice: if ontology contains reference to other ontologies (using {@code owl:imports}),
     * they will be processed also, i.e. in general case if process finishes without any error,
     * not only a single ontology may be loaded.
     * Also note, if there are no any {@link OWLOntologyIRIMapper IRI Mapping} or
     * {@link DocumentSourceMapping Document Source Mapping} in the manager,
     * then API may dive into the Internet to retrieve them.
     *
     * @param file {@link File}, not null
     * @return {@link OntologyModel}
     * @throws OWLOntologyCreationException if something is wrong in loading process
     * @see OWLOntologyManager#loadOntologyFromOntologyDocument(File)
     */
    @Override
    default OntologyModel loadOntologyFromOntologyDocument(@Nonnull File file) throws OWLOntologyCreationException {
        return loadOntologyFromOntologyDocument(new FileDocumentSource(file));
    }

    /**
     * Loads an ontology form an {@code InputStream} using default {@link OntLoaderConfiguration Loader Settings}.
     * Note: this method is not suitable for very large ontologies, since it caches the input stream to the memory.
     * Also note, if an ontology contains reference to other ontologies (using {@code owl:imports}),
     * they will be processed also (if they absent in the manager), i.e. in general case (if process finishes without any error)
     * not only a single ontology may be loaded.
     * Also note, if there are no any {@link OWLOntologyIRIMapper IRI Mapping}s or
     * {@link DocumentSourceMapping Document Source Mapping}s in the manager,
     * then API may dive into the Internet to retrieve dependent ontologies.
     *
     * @param input {@link InputStream}
     * @return {@link OntologyModel}
     * @throws OWLOntologyCreationException if something is wrong in loading process
     * @see OWLOntologyManager#loadOntologyFromOntologyDocument(InputStream)
     */
    @Override
    default OntologyModel loadOntologyFromOntologyDocument(@Nonnull InputStream input) throws OWLOntologyCreationException {
        return loadOntologyFromOntologyDocument(new StreamDocumentSource(input));
    }

    /**
     * Gets {@link OntGraphModel Ontology Graph Model} by the ontology and version IRIs passed as strings.
     *
     * @param iri     String, nullable
     * @param version String, nullable
     * @return {@link OntGraphModel} or {@code null} if no ontology found
     */
    default OntGraphModel getGraphModel(@Nullable String iri, @Nullable String version) {
        OWLOntologyID id = new OWLOntologyID(Optional.ofNullable(iri).map(IRI::create), Optional.ofNullable(version).map(IRI::create));
        OntologyModel res = getOntology(id);
        return res == null ? null : res.asGraphModel();
    }

    /**
     * Gets {@link OntGraphModel Ontology Graph Model} by the ontology IRI.
     *
     * @param iri String, nullable
     * @return {@link OntGraphModel} or {@code null} if no ontology found
     */
    default OntGraphModel getGraphModel(@Nullable String iri) {
        return getGraphModel(iri, null);
    }

    /**
     * Creates an {@link OntGraphModel Ontology Graph Model} with specified ontology and version IRIs.
     *
     * @param iri     String, nullable
     * @param version String, nullable
     * @return {@link OntGraphModel}
     */
    default OntGraphModel createGraphModel(@Nullable String iri, @Nullable String version) {
        OWLOntologyID id = new OWLOntologyID(Optional.ofNullable(iri).map(IRI::create), Optional.ofNullable(version).map(IRI::create));
        return createOntology(id).asGraphModel();
    }

    /**
     * Creates an {@link OntGraphModel Ontology Graph Model} with specified IRI.
     *
     * @param iri String, nullable
     * @return {@link OntGraphModel}
     */
    default OntGraphModel createGraphModel(@Nullable String iri) {
        return createGraphModel(iri, null);
    }

    /**
     * Lists all {@link OntGraphModel Ontology Graph Model}s.
     *
     * @return Stream of {@link OntGraphModel}
     */
    default Stream<OntGraphModel> models() {
        return ontologies().map(OntologyModel.class::cast).map(OntologyModel::asGraphModel);
    }

    /**
     * The Document Source mapping.
     * To customize ontology loading.
     *
     * @see OWLOntologyDocumentSource
     * @since 1.0.1
     */
    @FunctionalInterface
    interface DocumentSourceMapping extends Serializable {
        OWLOntologyDocumentSource map(OWLOntologyID id);
    }
}
