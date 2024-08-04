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
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.apache.jena.graph.Graph;
import org.apache.jena.ontapi.UnionGraph;
import org.apache.jena.ontapi.model.OntModel;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.io.IRIDocumentSource;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.io.OWLOntologyDocumentTarget;
import org.semanticweb.owlapi.io.OWLParserFactory;
import org.semanticweb.owlapi.io.StreamDocumentSource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyDocumentAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyFactory;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLOntologyWriterConfiguration;
import org.semanticweb.owlapi.model.OWLStorerFactory;
import org.semanticweb.owlapi.model.OntologyConfigurator;
import org.semanticweb.owlapi.model.UnknownOWLOntologyException;
import org.semanticweb.owlapi.model.parameters.OntologyCopy;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.Writer;
import java.util.Set;
import java.util.stream.Stream;

/**
 * An ONT-API Ontology manager, which is an extended {@link OWLOntologyManager OWL-API manager}.
 * It is the main point for creating, loading and accessing {@link Ontology Ontology Model}s.
 * Any ontology in this manager is a wrapper around a {@link Graph Jena Graph},
 * which may be linked to another ontology through {@link UnionGraph UnionGraph} interface.
 * <p>
 * The following methods are new (i.e., added in ONT-API) and extend the original functionality provided by the OWL-API:
 * <ul>
 * <li>{@link #addOntology(Graph)} - since 1.0.1</li>
 * <li>{@link #addOntology(Graph, OntLoaderConfiguration)} - since 1.2.0</li>
 * <li>{@link #createGraphModel(String)}</li>
 * <li>{@link #createGraphModel(String, String)}</li>
 * <li>{@link #models()}</li>
 * <li>{@link #getGraphModel(String)}</li>
 * <li>{@link #getGraphModel(String, String)}</li>
 * <li>{@link #getDocumentSourceMappers()} - since 1.3.0</li>
 * </ul>
 *
 * @see <a href="https://github.com/owlcs/owlapi/blob/version5/api/src/main/java/org/semanticweb/owlapi/model/OWLOntologyManager.java">org.semanticweb.owlapi.model.OWLOntologyManager</a>
 */
@ParametersAreNonnullByDefault
public interface OntologyManager extends OWLOntologyManager {

    /**
     * Gets a data factory which can be used to create OWL API objects
     * such as classes, properties, individuals, axioms, etc.
     *
     * @return a {@link DataFactory data factory} for creating OWL API objects
     */
    @Override
    DataFactory getOWLDataFactory();

    /**
     * Returns the manager's global config,
     * which is an extended {@link OntologyConfigurator OWL API Configurator} and
     * also a factory to create the snapshot configs {@link OntLoaderConfiguration} and {@link OntWriterConfiguration}.
     * It contains settings to manage both reading and writing behavior,
     * including a wide range of ONT-API specific settings.
     * This configuration is modifiable, but any change in it affects existing ontologies only
     * if the methods {@link #setOntologyLoaderConfiguration(OWLOntologyLoaderConfiguration)} or
     * {@link #setOntologyWriterConfiguration(OWLOntologyWriterConfiguration)} have not been called.
     * If none of these two methods were called,
     * then both newly added and existing ontologies pick up the config changes.
     * Otherwise, the global config is almost useless. This behavior is inherited from OWL-API.
     * Also, changes in this configuration do not affect on ontologies, loaded with the
     * {@link #loadOntologyFromOntologyDocument(OWLOntologyDocumentSource, OWLOntologyLoaderConfiguration)} method
     * - they already have their own overwritten configurations.
     * <p>
     * The initial state of the {@link OntConfig} is copied from {@code /ontapi.properties} file,
     * which should be placed in the classpath of the application.
     *
     * @return {@link OntConfig}, not {@code null}
     * @see #setOntologyConfigurator(OntologyConfigurator) a setter to pass some external configurator
     */
    @Override
    OntConfig getOntologyConfigurator();

    /**
     * Sets {@link OntologyConfigurator}.
     * If both {@link #setOntologyLoaderConfiguration(OWLOntologyLoaderConfiguration)} and
     * {@link #setOntologyWriterConfiguration(OWLOntologyWriterConfiguration)} methods were called,
     * then the configuration has already been overridden and there is no point in calling this method.
     * The same applies to the direct modifying configuration using {@link #getOntologyLoaderConfiguration()} reference.
     * Also note, the changes in global config have no effect on ontologies, loaded by the method
     * {@link #loadOntologyFromOntologyDocument(OWLOntologyDocumentSource, OWLOntologyLoaderConfiguration)}.
     *
     * @param conf {@link OntologyConfigurator} or {@code null} to reset defaults
     * @see #getOntologyLoaderConfiguration() to get direct reference to modify
     */
    void setOntologyConfigurator(@Nullable OntologyConfigurator conf);

    /**
     * Returns a loading config, that is an immutable version of the
     * {@link OWLOntologyLoaderConfiguration OWL-API Ontology Loader Configuration}.
     * Be warned: it is a read only accessor,
     * to change the configuration please create a new config instance (using any setter)
     * and pass it back to the manager using
     * the {@link #setOntologyLoaderConfiguration(OWLOntologyLoaderConfiguration)} method.
     *
     * @return {@link OntLoaderConfiguration}, not {@code null}
     */
    @Override
    OntLoaderConfiguration getOntologyLoaderConfiguration();

    /**
     * Sets the {@link OntLoaderConfiguration Ontology Loader Configuration}.
     * Any ontology from the manager will reflect this configuration, but only if it was not loaded by the
     * {@link #loadOntologyFromOntologyDocument(OWLOntologyDocumentSource, OWLOntologyLoaderConfiguration)} method
     * or passed by the {@link #addOntology(Graph, OntLoaderConfiguration)} method.
     * Also note: calling this method overrides the global configuration settings related to the loading process.
     *
     * @param conf {@link OWLOntologyLoaderConfiguration} or {@code null} to reset defaults
     */
    void setOntologyLoaderConfiguration(@Nullable OWLOntologyLoaderConfiguration conf);

    /**
     * Returns a writer config, that is an immutable version of
     * {@link OWLOntologyWriterConfiguration OWL-API Ontology Writer Configuration}.
     * For more information see notes in the description of the {@link #getOntologyLoaderConfiguration()} method.
     *
     * @return {@link OntWriterConfiguration}
     */
    @Override
    OntWriterConfiguration getOntologyWriterConfiguration();

    /**
     * Sets {@link OntWriterConfiguration Ontology Writer Configuration}.
     * For more information see notes in the description of
     * the {@link #setOntologyLoaderConfiguration(OWLOntologyLoaderConfiguration)} method.
     *
     * @param conf {@link OWLOntologyWriterConfiguration} or {@code null} to reset defaults
     */
    void setOntologyWriterConfiguration(@Nullable OWLOntologyWriterConfiguration conf);

    /**
     * Gets an {@link RWLockedCollection extended OWL-API PriorityCollection}
     * of {@link OntologyFactory Ontology Factories} - iterable object,
     * which allows to iterate and modify an internal collection.
     * Warning: any attempt to add OWLOntologyFactory into that Priority Collection
     * will cause throwing an {@link OntApiException ONT-API runtime exception}
     * in case that factory does not extend {@code OntologyFactory} interface.
     *
     * @return {@link RWLockedCollection} of {@link OntologyFactory}
     */
    @Override
    RWLockedCollection<OWLOntologyFactory> getOntologyFactories();

    /**
     * Sets the collection of ontology factories.
     * Warning: if the given collection ({@code factories})
     * contains an instance that does not implement {@link OntologyFactory}, then an exception is expected.
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
    default void setOntologyFactories(Set<OWLOntologyFactory> factories) throws OntApiException {
        getOntologyFactories().set(factories);
    }

    /**
     * Gets an {@link RWLockedCollection extended OWL-API PriorityCollection} of {@link OWLOntologyIRIMapper IRI Mappers}
     * The mappers are used to obtain ontology document IRIs for ontology IRIs.
     * If their type is annotated with a {@link org.semanticweb.owlapi.annotations.HasPriority HasPriority} type,
     * this will be used to decide the order they are used.
     * Otherwise, the order in which the collection is iterated will determine the order in which the mappers are used.
     *
     * @return {@link RWLockedCollection} of {@link OWLOntologyIRIMapper}s
     * @see #getDocumentSourceMappers()
     */
    @Override
    RWLockedCollection<OWLOntologyIRIMapper> getIRIMappers();

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
    default void setIRIMappers(Set<OWLOntologyIRIMapper> mappers) {
        getIRIMappers().set(mappers);
    }

    /**
     * Gets an {@link RWLockedCollection extended OWL-API PriorityCollection}
     * of {@link DocumentSourceMapping ONT Document Source Mapping}s.
     * A {@link DocumentSourceMapping} is more general mechanism to conduct ontology mapping
     * than {@link OWLOntologyIRIMapper}, and it is widely used in APIs depths and dependent projects.
     *
     * @return {@link RWLockedCollection} of {@link DocumentSourceMapping}s
     * @since 1.3.0
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
     * Sets the java.util.Set of OWL parsers into the manager.
     *
     * @param parsers Set of {@link OWLParserFactory}s
     * @see #getOntologyParsers()
     * @deprecated use {@code getOntologyParsers().set(parsers)} instead
     */
    @Deprecated
    @Override
    default void setOntologyParsers(Set<OWLParserFactory> parsers) {
        getOntologyParsers().set(parsers);
    }

    /**
     * Gets an {@link RWLockedCollection extended OWL-API PriorityCollection} of {@link OWLStorerFactory OWL Storers}.
     * About ordering see {@link org.semanticweb.owlapi.annotations.HasPriority HasPriority} annotation.
     *
     * @return {@link RWLockedCollection} of {@link OWLStorerFactory}s
     */
    @Override
    RWLockedCollection<OWLStorerFactory> getOntologyStorers();

    /**
     * Sets the java.util.Set of OWL storers into the manager.
     *
     * @param storers Set of {@link OWLStorerFactory}s
     * @see #getOntologyStorers()
     * @deprecated use {@code getOntologyStorers().set(storers)} instead
     */
    @Deprecated
    @Override
    default void setOntologyStorers(Set<OWLStorerFactory> storers) {
        getOntologyStorers().set(storers);
    }

    /**
     * Gets the ontology by the given {@code iri}.
     * The method also works with version IRI if it fails with ontology IRI.
     * So, the resulting ontology may have an ontology IRI that does not match the {@code iri}
     * specified as the method parameter.
     * This behavior is caused by the {@link OWLOntologyID#match(IRI)} method,
     * and present in all versions of the OWL-API v5.
     *
     * @param iri {@link IRI} which is an ontology IRI or a version IRI as described above, cannot be {@code null}
     * @return {@link Ontology} or {@code null}
     * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLOntologyManagerImpl.java#L392'>uk.ac.manchester.cs.owl.owlapi.OWLOntologyManagerImpl#getOntology(IRI)</a>
     * @see OWLOntologyID#match(IRI)
     * @see OntologyManager#contains(IRI)
     */
    @Override
    Ontology getOntology(IRI iri);

    /**
     * Finds the ontology by the specified {@code id}, which is allowed to be anonymous.
     * If there is no such ontology, it tries to find the first with the same ontology IRI as in the given {@code id}.
     *
     * @param id {@link OWLOntologyID} ID, cannot be {@code null}
     * @return {@link Ontology} or {@code null}
     * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLOntologyManagerImpl.java#L410'>uk.ac.manchester.cs.owl.owlapi.OWLOntologyManagerImpl#getOntology(OWLOntologyID)</a>
     * @see OntologyManager#contains(OWLOntologyID)
     * @see OWLOntologyID#matchOntology(IRI)
     */
    @Override
    Ontology getOntology(OWLOntologyID id);

    /**
     * Answers {@code true} if the manager contains an ontology with the given ontology {@code iri}.
     * If there is no ontology that has the input {@code iri} as ontology IRI, the IRI is matched against the version IRI.
     * See the description for {@link #getOntology(IRI)} method and be warned!
     *
     * @param iri {@link IRI} the ontology iri or version iri
     * @return true if ontology exists
     * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLOntologyManagerImpl.java#L328'>uk.ac.manchester.cs.owl.owlapi.OWLOntologyManagerImpl#contains(IRI)</a>
     * @see OWLOntologyID#match(IRI)
     * @see OntologyManager#getOntology(IRI)
     */
    @Override
    boolean contains(IRI iri);

    /**
     * Answers {@code true} if the manager contains an ontology with the given ontology {@code id}.
     * Be warned: this method returns always {@code false} for any anonymous id.
     * For non-anonymous id it performs searching by ontology iri ignoring a version IRI.
     * This is to make the behavior the same as the original OWL-API method.
     * To find an anonymous ontology use either the method {@link OntologyManager#getOntology(OWLOntologyID)}
     * or the method {@link OntologyManager#ontologies()} stream with filters.
     *
     * @param id {@link OWLOntologyID ontology ID}, not {@code null}
     * @return true if {@code id} is not anonymous and there is an ontology with the same iri as in the specified {@code id}
     * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLOntologyManagerImpl.java#L355'>uk.ac.manchester.cs.owl.owlapi.OWLOntologyManagerImpl#contains(OWLOntologyID)</a>
     */
    @Override
    boolean contains(OWLOntologyID id);

    /**
     * Given an import declaration, obtains the ontology that this import has been resolved to.
     *
     * @param iri {@link OWLImportsDeclaration} the declaration that points to the imported ontology
     * @return {@link Ontology} the ontology that the import declaration resolves to,
     * or {@code null} if the import declaration could not be resolved to an ontology,
     * because the ontology was not loaded or has been removed from this manager
     * @see OWLOntologyManager#getImportedOntology(OWLImportsDeclaration)
     * @see #getImportedOntology(IRI)
     */
    @Nullable
    Ontology getImportedOntology(OWLImportsDeclaration iri);

    /**
     * Lists all ontologies contained within the manager.
     * Each of the returned ontologies is an instance of {@link Ontology} interface
     * and encapsulates a {@link Graph Jena RDF Graph}.
     *
     * @return {@code Stream} of {@link Ontology}s
     * @see #models()
     */
    @Override
    Stream<OWLOntology> ontologies();

    /**
     * Creates a fresh ontology with the specified {@code id}.
     * <p>
     * Note: this method doesn't throw a checked exception {@link OWLOntologyCreationException} as does OWL-API.
     * Instead, there is an unchecked exception {@link OntApiException}
     * that may wrap {@link OWLOntologyCreationException}.
     * This is because OWL-API and ONT-API physically work in different ways,
     * and sometimes there is no possibility to retain the behavior completely.
     * Moreover, a method which does not work with resources
     * and just creates an object should not throw a checked exception.
     *
     * @param id {@link OWLOntologyID}
     * @return ontology {@link Ontology}
     * @throws OntApiException in case something is wrong
     */
    @Override
    Ontology createOntology(OWLOntologyID id);

    /**
     * Creates an ontology model from the given {@link Graph Jena Graph},
     * taking into account the specified loading settings.
     * If the graph is composite and hierarchical
     * with {@code owl:imports} declarations as references between sub-graphs,
     * it will be re-assembled retaining hierarchy, but in a new container - {@link UnionGraph}.
     * Otherwise, if the graph is indivisible, it is passed into the manager as is.
     * This is a new (ONT-API) method.
     *
     * @param graph {@link Graph}
     * @param conf  {@link OntLoaderConfiguration}
     * @return {@link Ontology}
     * @see OntGraphDocumentSource
     * @since 1.2.0
     */
    Ontology addOntology(Graph graph, OntLoaderConfiguration conf);

    /**
     * Copies the specified ontology from the original manager into this one.
     * The returned {@code OntologyModel} will be linked to this manager instance, i.e.,
     * the method {@link Ontology#getOWLOntologyManager()} called for this ontology
     * should return an instance of this manager.
     * The {@code source} could be any {@link OWLOntology ontology} implementation,
     * not necessarily provided by ONT-API.
     * <p>
     * Note: the axiom's list, retrieved from the returned ontology, may differ with the source axioms
     * due to the different config settings (see {@link com.github.owlcs.ontapi.config.AxiomsSettings}).
     * <p>
     * Note: the second parameter is allowed to be either
     * {@link OntologyCopy#SHALLOW SHALLOW} or {@link OntologyCopy#DEEP DEEP}.
     * In ONT-API the moving operation (i.e., {@code settings} = {@link OntologyCopy#MOVE MOVE}) is not supported,
     * since the original (default-impl) OWL-API method requires the same instance returned.
     * This condition cannot be satisfied since we are dealing with different implementation.
     * To perform moving it always possible copy ontology using this method
     * and then delete the source ontology using the method {@link #removeOntology(OWLOntology)}.
     * <p>
     * If the {@code settings} parameter is {@link OntologyCopy#SHALLOW SHALLOW} and
     * the source ontology is {@link Ontology}
     * (i.e., not an {@link OWLOntology} instance produced by the default OWL-API impl),
     * then this operation is almost effectively equivalent to calling
     * the method {@link #addOntology(Graph, OntLoaderConfiguration)}
     * with the parameter-configuration where transformations and import processing are disabled
     * (for more info about these parameters see
     * {@link com.github.owlcs.ontapi.config.LoadSettings#isPerformTransformation()
     * and {@link com.github.owlcs.ontapi.config.LoadSettings#isProcessImports()}}).
     * In this case, only the base graph reference is copied; all actual data remains untouched.
     * It results in a possibility to share ontology data between different managers.
     * <p>
     * If the second parameter is {@link OntologyCopy#DEEP DEEP},
     * then the ontology data is really copied, triple by triple.
     * The document format (see {@link #getOntologyFormat(OWLOntology)}) and
     * the document source iri (see {@link #getOntologyDocumentIRI(OWLOntology)}) are also copied -
     * and this matches the OWL-API default behavior.
     * <p>
     * In all cases, the method not only works with the base graph,
     * but also tries to restore all missed import references.
     * I.e., if the source ontology has an import to another one,
     * the returned ontology would also have a reference to an ontology with the same import declaration IRI,
     * if it is found in the manager.
     *
     * @param source   {@link OWLOntology} the source, possible, not an {@link Ontology} instance
     * @param settings {@link OntologyCopy} the settings,
     *                 either {@link OntologyCopy#DEEP} or {@link OntologyCopy#SHALLOW}
     * @return a new (copied) {@link Ontology}
     * @throws OntApiException if any unexpected error occurs or input parameters are wrong
     * @see OWLOntologyManager#copyOntology(OWLOntology, OntologyCopy)
     */
    @Override
    Ontology copyOntology(OWLOntology source, OntologyCopy settings);

    /**
     * Loads an ontology by the specified {@code source} IRI.
     * Note: if a loaded ontology contains any {@code owl:imports} they will also be processed,
     * i.e., in general case this method loads not only single ontology but the whole ontology family.
     * If there is a {@link DocumentSourceMapping Docuemnt Source Mapping} in the manager for the given IRI,
     * then the method processes the corresponding {@link OWLOntologyDocumentSource Document Source}.
     * Similar, if there is {@link OWLOntologyIRIMapper IRI Mapping} for the given IRI,
     * then the method uses a document IRI obtained from that mapper.
     * In other cases the {@link IRIDocumentSource} is used, and the system tries to find a file or web-resource for it.
     *
     * @param source {@link IRI} the IRI that identifies the desirable ontology:
     *               it can be an ontology IRI, version IRI or document IRI to load directly,
     *               also it can be mapped to some other IRI using {@link OWLOntologyIRIMapper}
     *               or to some prepared document source using {@link DocumentSourceMapping}
     * @return {@link Ontology}
     * @throws OWLOntologyCreationException if there is a problem when creating or loading an ontology
     * @see OWLOntologyManager#loadOntology(IRI)
     */
    @Override
    Ontology loadOntology(IRI source) throws OWLOntologyCreationException;

    /**
     * Loads an ontology from the specified {@link OWLOntologyDocumentSource document source} using
     * {@link OWLOntologyLoaderConfiguration Loader Configuration} to setup loading processes.
     * It is the most general way to load an ontology to the manager.
     * Notice: if the source contains any {@code owl:imports}, which absent in the manager, they will be processed also,
     * i.e., this method loads not only single ontology in general case.
     *
     * @param source {@link OWLOntologyDocumentSource} the document source to produce i
     * @param config {@link OWLOntologyLoaderConfiguration} the loading settings, see {@link OntLoaderConfiguration}
     * @return {@link Ontology}
     * @throws OWLOntologyCreationException in case any error occurs during a reading source or a construct the ontology
     * @see OWLOntologyManager#loadOntologyFromOntologyDocument(OWLOntologyDocumentSource, OWLOntologyLoaderConfiguration)
     */
    @Override
    Ontology loadOntologyFromOntologyDocument(OWLOntologyDocumentSource source,
                                              OWLOntologyLoaderConfiguration config) throws OWLOntologyCreationException;

    /**
     * {@inheritDoc}
     * <b>Note: The caller is responsible for closing the {@code  outputStream}</b>
     *
     * @param ontology     The ontology to be saved.
     * @param outputStream The output stream where the ontology will be saved to
     * @throws OWLOntologyStorageException If the ontology could not be saved
     * @throws UnknownOWLOntologyException If the specified ontology is not managed by this manager
     */
    @Override
    default void saveOntology(OWLOntology ontology, OutputStream outputStream) throws OWLOntologyStorageException {
        OWLOntologyManager.super.saveOntology(ontology, outputStream);
    }

    /**
     * {@inheritDoc}
     * <b>Note: the method calls {@link Writer#close()} produced by {@link OWLOntologyDocumentTarget#getWriter()},
     * but DOES not call {@link OutputStream#close()} produced by {@link OWLOntologyDocumentTarget#getOutputStream()}</b>.
     *
     * @param ontology       The ontology to be saved.
     * @param ontologyFormat The output format in which to save the ontology
     * @param documentTarget The output target where the ontology will be saved to
     * @throws OWLOntologyStorageException If the ontology could not be saved
     * @throws UnknownOWLOntologyException If the specified ontology is not managed by this manager
     */
    @Override
    void saveOntology(OWLOntology ontology, OWLDocumentFormat ontologyFormat, OWLOntologyDocumentTarget documentTarget)
            throws OWLOntologyStorageException;

    /**
     * Resolves the given IRI to the ontology if possible.
     * According to the specification,
     * a return ontology must have either a version IRI, an ontology IRI or a document IRI (in this order),
     * that matches the specified IRI.
     * In the case of ontology IRI, there should be only a single ontology in the manager that has this ontology IRI,
     * otherwise this IRI cannot be considered as a current version of the ontology series.
     *
     * @param iri the declaration {@link IRI} to find the imported ontology
     * @return {@link Ontology} or {@code null}
     * @see #getImportedOntology(OWLImportsDeclaration)
     * @see <a href='https://www.w3.org/TR/owl2-syntax/#Ontology_Documents'>3.2 Ontology Documents</a>
     * @see <a href='https://www.w3.org/TR/owl2-syntax/#Imports'>3.4 Imports</a>
     * @since 1.3.2
     */
    @Nullable
    default Ontology getImportedOntology(IRI iri) {
        return getImportedOntology(getOWLDataFactory().getOWLImportsDeclaration(iri));
    }

    /**
     * Adds an IRI mapper to the manager.
     *
     * @param mapper {@link OWLOntologyIRIMapper}, not {@code null}
     * @see #getIRIMappers()
     * @deprecated use {@code getIRIMappers().add(mapper)} instead
     */
    @Deprecated
    @Override
    default void addIRIMapper(OWLOntologyIRIMapper mapper) {
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
    default void removeIRIMapper(OWLOntologyIRIMapper mapper) {
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
     * Adds the OWL storer factory into the manager.
     *
     * @param storer {@link OWLStorerFactory}
     * @see #getOntologyStorers()
     * @deprecated use {@code getOntologyStorers().add(storer)} instead
     */
    @Deprecated
    @Override
    default void addOntologyStorer(OWLStorerFactory storer) {
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
    default void removeOntologyStorer(OWLStorerFactory storer) {
        getOntologyStorers().remove(storer);
    }

    /**
     * Clears the manager OWL storer factories' collection.
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
     * Note: graph transformation is not performed.
     *
     * @param graph {@link Graph}
     * @return {@link Ontology}
     * @since 1.0.1
     */
    default Ontology addOntology(Graph graph) {
        return addOntology(graph, getOntologyLoaderConfiguration().setPerformTransformation(false));
    }

    /**
     * Creates a new (empty) ontology that does not have an ontology IRI
     * (and therefore does not have a version IRI).
     * A document IRI will automatically be generated.
     *
     * @return {@link Ontology} the newly created ONT-API ontology instance
     * @see OWLOntologyManager#createOntology()
     */
    @Override
    default Ontology createOntology() {
        return createOntology(new ID());
    }

    /**
     * Creates a new (empty) ontology that has the specified ontology IRI (and no version IRI).
     * The ontology document IRI of the created ontology will be set to the value returned by any
     * installed {@link OWLOntologyIRIMapper IRI Mapper}s.
     * If no mappers are installed or the ontology IRI was not mapped to a document IRI,
     * then the ontology document IRI will be set to the value of {@code iri}.
     *
     * @param iri {@link IRI} the IRI of the ontology to be created
     * @return {@link Ontology} the newly created ONT-API ontology instance
     * @throws OntApiException in case of error, this exception can wrap one of the OWL-API checked exceptions,
     *                         e.g. {@link OWLOntologyCreationException}, {@link OWLOntologyAlreadyExistsException},
     *                         {@link  OWLOntologyDocumentAlreadyExistsException}.
     * @see OWLOntologyManager#createOntology(IRI)
     */
    @Override
    default Ontology createOntology(@Nullable IRI iri) {
        return createOntology(ID.create(iri));
    }

    /**
     * Loads an ontology from a given document source using default settings.
     * Notice: if ontology contains reference to other ontologies (using {@code owl:imports}),
     * they will be processed also.
     * In this case, take into account the possible internet diving to retrieve missed ontologies.
     *
     * @param source {@link OWLOntologyDocumentSource}
     * @return {@link Ontology}
     * @throws OWLOntologyCreationException if something is wrong
     * @see OWLOntologyManager#loadOntologyFromOntologyDocument(OWLOntologyDocumentSource)
     */
    @Override
    default Ontology loadOntologyFromOntologyDocument(OWLOntologyDocumentSource source)
            throws OWLOntologyCreationException {
        return loadOntologyFromOntologyDocument(source, getOntologyLoaderConfiguration());
    }

    /**
     * Loads an ontology from an ontology document specified by an IRI.
     * In contrast, the {@link #loadOntology(IRI)} method, <i>no mapping</i> is performed on the specified IRI.
     * The loading settings are default (see {@link #getOntologyLoaderConfiguration()}).
     * Notice: if ontology contains reference to other ontologies (using {@code owl:imports}),
     * they will be processed also, i.e., in general case if a process finishes without any error,
     * not only a single ontology may be loaded.
     * In case of dependent ontologies all mappings
     * ({@link OWLOntologyIRIMapper IRI Mapping} and {@link DocumentSourceMapping Document Source Mapping})
     * are included in consideration.
     *
     * @param iri {@link IRI} the ontology document IRI where the ontology will be loaded from
     * @return {@link Ontology} the newly loaded ONT-API ontology instance
     * @throws OWLOntologyCreationException if there is a problem in creating or loading the ontology
     * @see OWLOntologyManager#loadOntologyFromOntologyDocument(IRI)
     */
    @Override
    default Ontology loadOntologyFromOntologyDocument(IRI iri) throws OWLOntologyCreationException {
        return loadOntologyFromOntologyDocument(new IRIDocumentSource(iri));
    }

    /**
     * Loads an ontology from a file using default {@link OntLoaderConfiguration Loader Settings}.
     * Notice: if ontology contains reference to other ontologies (using {@code owl:imports}),
     * they will be processed also, i.e., in general case if the process finishes without any error,
     * not only a single ontology may be loaded.
     * Also note, if there are no any {@link OWLOntologyIRIMapper IRI Mapping} or
     * {@link DocumentSourceMapping Document Source Mapping} in the manager,
     * then API may dive into the Internet to retrieve them.
     *
     * @param file {@link File}, not {@code null}
     * @return {@link Ontology}
     * @throws OWLOntologyCreationException if something is wrong in the loading process
     * @see OWLOntologyManager#loadOntologyFromOntologyDocument(File)
     */
    @Override
    default Ontology loadOntologyFromOntologyDocument(File file) throws OWLOntologyCreationException {
        return loadOntologyFromOntologyDocument(new FileDocumentSource(file));
    }

    /**
     * Loads an ontology form an {@code InputStream} using default {@link OntLoaderConfiguration Loader Settings}.
     * Note: this method is not suitable for a huge ontologies, since it caches the input stream to the memory.
     * Also note, if an ontology contains reference to other ontologies (using {@code owl:imports}),
     * they will be processed also (if they absent in the manager),
     * i.e., in general case (if the process finishes without any error)
     * not only a single ontology may be loaded.
     * Also note, if there are no any {@link OWLOntologyIRIMapper IRI Mapping}s or
     * {@link DocumentSourceMapping Document Source Mapping}s in the manager,
     * then API may dive into the Internet to retrieve dependent ontologies.
     *
     * @param input {@link InputStream}
     * @return {@link Ontology}
     * @throws OWLOntologyCreationException if something is wrong in the loading process
     * @see OWLOntologyManager#loadOntologyFromOntologyDocument(InputStream)
     */
    @Override
    default Ontology loadOntologyFromOntologyDocument(InputStream input) throws OWLOntologyCreationException {
        return loadOntologyFromOntologyDocument(new StreamDocumentSource(input));
    }

    /**
     * Gets {@link OntModel Ontology Graph Model} by the ontology and version IRIs passed as strings.
     *
     * @param iri     String, can be {@code null} to find anonymous ontology
     * @param version String, must be {@code null} if {@code iri} is {@code null}
     * @return {@link OntModel} or {@code null} if no ontology found
     */
    default OntModel getGraphModel(@Nullable String iri, @Nullable String version) {
        ID id = ID.create(iri, version);
        Ontology res = getOntology(id);
        return res == null ? null : res.asGraphModel();
    }

    /**
     * Gets {@link OntModel Ontology Graph Model} by the ontology IRI.
     *
     * @param iri String, can be {@code null} to find anonymous ontology
     * @return {@link OntModel} or {@code null} if no ontology found
     */
    default OntModel getGraphModel(@Nullable String iri) {
        return getGraphModel(iri, null);
    }

    /**
     * Creates an {@link OntModel Ontology Graph Model} with specified ontology and version IRIs.
     *
     * @param iri     String, can be {@code null} to create anonymous ontology
     * @param version String, must be {@code null} if {@code iri} is {@code null}
     * @return {@link OntModel}
     */
    default OntModel createGraphModel(@Nullable String iri, @Nullable String version) {
        return createOntology(ID.create(iri, version)).asGraphModel();
    }

    /**
     * Creates an {@link OntModel Ontology Graph Model} with specified IRI.
     *
     * @param iri String, can be {@code null} to create anonymous ontology
     * @return {@link OntModel}
     */
    default OntModel createGraphModel(@Nullable String iri) {
        return createGraphModel(iri, null);
    }

    /**
     * Lists all {@link OntModel Ontology Graph Model}s from the manager.
     *
     * @return {@code Stream} of {@link OntModel}
     * @see #ontologies()
     */
    default Stream<OntModel> models() {
        return ontologies().map(Ontology.class::cast).map(Ontology::asGraphModel);
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
        /**
         * Finds a {@link OWLOntologyDocumentSource OWL Document Source} object
         * by the {@link OWLOntologyID Ontology ID}.
         *
         * @param id {@link OWLOntologyID}, not {@code null}
         * @return {@link OWLOntologyDocumentSource} or {@code null}
         */
        OWLOntologyDocumentSource map(OWLOntologyID id);

        /**
         * Finds a {@link OWLOntologyDocumentSource OWL Document Source}
         * object by the {@link IRI} that may be either version IRI or ontology IRI.
         *
         * @param iri {@link IRI}, not {@code null}
         * @return {@link OWLOntologyDocumentSource} or {@code null} if no mapping found
         * @since 1.3.2
         */
        default OWLOntologyDocumentSource map(IRI iri) {
            return map(ID.create(iri));
        }
    }
}
