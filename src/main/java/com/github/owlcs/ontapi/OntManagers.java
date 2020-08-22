/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2020, owl.cs group.
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

import com.github.owlcs.ontapi.jena.OntModelFactory;
import com.google.common.collect.LinkedListMultimap;
import org.semanticweb.owlapi.io.OWLParserFactory;
import org.semanticweb.owlapi.model.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * {@code com.github.owlcs.ontapi.OntManagers} is a main access point to the system, a collection of factory methods
 * to provide different implementations of {@link OWLOntologyManager} interface with its commonly required features.
 * The class contains facilities for creating both <b>ONT-API</b> instances (RDF-centric approach)
 * as well as instances from the default implementation of <b>OWL-API</b> (OWL-centric approach).
 * The first family of methods returns instances of the extended {@code ONT-API} interface - {@link OntologyManager}.
 * The second one is an optional way, it is not a direct part of {@code ONT-API}.
 * It was included just for convenience and completeness,
 * and only works if the {@code OWL-API-impl} module is present in the classpath.
 * <p>
 * {@code OntManagers} is a full {@code ONT-API} analogue of
 * <a href='https://github.com/owlcs/owlapi/blob/version5/apibinding/src/main/java/org/semanticweb/owlapi/apibinding/OWLManager.java'>org.semanticweb.owlapi.apibinding.OWLManager</a>.
 * <p>
 * Implementation notes:
 * Unlike the {@code OWL-API-apibinding}, there is no injections support.
 * To create {@code ONT-API} instances the direct factory methods are used.
 * To create {@code OWL-API} instances the straightforward reflection is used.
 * If you plan to work with original (pure) {@code OWL-API} managers and factories, please include either
 * <a href='https://github.com/owlcs/owlapi/blob/version5/apibinding/'>net.sourceforge.owlapi:owlapi-apibinding</a> or
 * <a href='https://github.com/owlcs/owlapi/blob/version5/impl/'>net.sourceforge.owlapi:owlapi-impl</a> artifacts
 * into your classpath, otherwise the corresponding methods ({@link #createOWLAPIImplManager()}, {@link #createConcurrentOWLAPIImplManager()})
 * will throw a runtime exception. Or better use {@code org.semanticweb.owlapi.apibinding.OWLManager}.
 * <p>
 * Created by @szuev on 27.09.2016.
 *
 * @see OntModelFactory
 */
@SuppressWarnings("WeakerAccess")
public class OntManagers implements OWLOntologyManagerFactory {

    static {
        OntModelFactory.init();
    }

    public static final ONTAPIProfile DEFAULT_PROFILE = new ONTAPIProfile();
    private static OWLOntologyManagerFactory managerFactory = () -> DEFAULT_PROFILE.create(false);

    /**
     * Returns the global data factory that can be used to create any {@code OWL-API} object,
     * including {@link OWLAxiom OWL Axionms}, {@link OWLEntity OWL Entities} and {@link OWLLiteral}.
     * This allows to assembly ontology by adding into it any {@link OWLObject}
     * derived from the {@link DataFactory OWL Data Factory}.
     * <p>
     * Alternative way to assembly ontology is direct working with the {@link org.apache.jena.graph.Graph Graph}
     * through the {@link com.github.owlcs.ontapi.jena.model.OntModel} view of the ontology,
     * that can be obtained using the method {@link Ontology#asGraphModel()}.
     * The first way of ontology editing is native for {@code OWL-API},
     * the second way is native for {@code Apache Jena} and provided by {@code ONT-API} as a feature,
     * that actually underlies any interaction with ontologies in {@code ONT-API}.
     *
     * @return {@link DataFactory} impl
     */
    public static DataFactory getDataFactory() {
        return DEFAULT_PROFILE.dataFactory();
    }

    /**
     * Creates a ready to use {@code ONT-API} ontology manager with default configuration,
     * that includes settings from {@link com.github.owlcs.ontapi.config.OntSettings /ontapi.properties},
     * various format-syntaxes for saving/loading ontologies from the {@code jena-arq}
     * and additional format-syntaxes from the {@code OWL-API} supply ({@code owlapi-rio}, {@code owlapi-oboformat}),
     * if they are in classpath.
     * <p>
     * Some additional notes:
     * <ul>
     * <li>The returned manager is RDF-centric, that means the internal data is RDF and OWL support is done on top of RDF</li>
     * <li>If you don't need OWL-Axioms,
     * the lightweight {@link com.github.owlcs.ontapi.jena.model.OntModel OntModel} can be used,
     * see {@link OntModelFactory}.</li>
     * <li>More about format-syntaxes can be found in {@link OntFormat} class</li>
     * <li>This is the primary factory method to produce {@link OntologyManager}s
     * that should be used when there is no reason to use any other method to create manager's instances.
     * In other words, if you have doubt what method to use, choose this one.</li>
     * <li>The returned manager is not thread-safe: concurrent edition of a manager's ontology
     * may cause {@link java.util.ConcurrentModificationException ConcurrentModificationException} or inconsistent state.
     * To use in a concurrent environment
     * the manager provided by the method {@link #createConcurrentManager()} can be used.</li>
     * </ul>
     *
     * @return {@link OntologyManager} a fresh {@code ONT-API} manager instance
     */
    public static OntologyManager createManager() {
        return DEFAULT_PROFILE.create(false);
    }

    /**
     * Creates a ready to use {@code ONT-API} ontology manager with default configuration
     * and locking mechanism to work in a concurrent environment.
     * The returned manager itself and any its component (i.e. ontologies) are thread-safe,
     * i.e. can be safely shared between different threads.
     * <p>
     * Notes:
     * <ul>
     * <li>The returned manager is RDF-centric, that means the internal data is RDF and OWL support is done on top of RDF</li>
     * <li>To manage concurrency a single {@link ReadWriteLock} is used</li>
     * </ul>
     *
     * @return {@link OntologyManager} a fresh {@code ONT-API} manager instance with concurrency
     */
    public static OntologyManager createConcurrentManager() {
        return DEFAULT_PROFILE.create(true);
    }

    /**
     * Creates an original {@code OWL-API} (i.e. pure native impl) ontology manager instance with default configuration.
     * Notes:
     * <ul>
     * <li>The returned manager is OWL-centric, it does not support RDF out-of-the-box</li>
     * <li>The returned manager is not a direct part of {@code ONT-API}.
     * The method is here for convenience and/or test purposes only.
     * Better to use a similar method from {@code OWL-API-apibinding} supply, if it is available.
     * See also {@code org.semanticweb.owlapi.apibinding.OWLManager}.</li>
     * <li>Make sure that <a href='https://github.com/owlcs/owlapi/blob/version5/impl/'>owlapi-impl</a> is in classpath</li>
     * </ul>
     *
     * @return {@link OWLOntologyManager} a fresh {@code OWL-API} manager instance
     * @throws OntApiException if there is no {@code owlapi-impl} in classpath or some unexpected error is occurred
     */
    public static OWLOntologyManager createOWLAPIImplManager() throws OntApiException {
        return createOWLProfile().create(false);
    }

    /**
     * Creates an original {@code OWL-API} (i.e pure native impl) ontology manager instance
     * with default configuration and locking to work in a concurrent environment.
     * Notes:
     * <ul>
     * <li>The returned manager is OWL-centric, it does not support RDF out-of-the-box</li>
     * <li>To manage concurrency a single {@link ReadWriteLock} is used</li>
     * <li>The returned manager is not a direct part of {@code ONT-API}.
     * The method is here for convenience and/or test purposes only.
     * Better to use a similar method from {@code OWL-API-apibinding} supply, if it is available.
     * See also {@code org.semanticweb.owlapi.apibinding.OWLManager}.</li>
     * <li>Make sure that <a href='https://github.com/owlcs/owlapi/blob/version5/impl/'>owlapi-impl</a> is in classpath</li>
     * </ul>
     *
     * @return {@link OWLOntologyManager} a fresh {@code OWL-API} manager instance with concurrency
     * @throws OntApiException if there is no {@code owlapi-impl} in classpath or some unexpected error is occurred
     */
    public static OWLOntologyManager createConcurrentOWLAPIImplManager() throws OntApiException {
        return createOWLProfile().create(true);
    }

    /**
     * Returns the default static {@link OWLOntologyManagerFactory factory}.
     *
     * @return profile
     */
    public static OWLOntologyManagerFactory getFactory() {
        return managerFactory;
    }

    /**
     * Changes a default static {@link OWLOntologyManagerFactory factory}.
     * This a way to manage {@link #get()} method behaviour and should not be used without a really good reason.
     *
     * @param factory profile object, not {@code null}
     * @see OWLOntologyManagerFactory#get()
     */
    public static void setFactory(OWLOntologyManagerFactory factory) {
        managerFactory = OntApiException.notNull(factory, "Null manager factory is given.");
    }

    @Override
    public OWLOntologyManager get() {
        return managerFactory.get();
    }

    /**
     * Creates a ready to use {@code ONT-API} ontology manager with default configuration.
     *
     * @return {@link OntologyManager}
     * @deprecated - use {@link #createManager()} instead
     */
    @Deprecated
    public static OntologyManager createONT() {
        return createManager();
    }

    /**
     * Creates a ready to use concurrent {@code ONT-API} ontology manager with default configuration.
     *
     * @return {@link OntologyManager}
     * @deprecated - use {@link #createConcurrentManager()} instead
     */
    @Deprecated
    public static OntologyManager createConcurrentONT() {
        return createConcurrentManager();
    }

    /**
     * Creates an original {@code OWL-API} ontology manager instance with default configuration.
     *
     * @return {@link OWLOntologyManager}
     * @deprecated - use {@link #createOWLAPIImplManager()} instead
     */
    @Deprecated
    public static OWLOntologyManager createOWL() {
        return createOWLAPIImplManager();
    }

    /**
     * Creates an original {@code OWL-API} concurrent ontology manager instance with default configuration.
     *
     * @return {@link OWLOntologyManager}
     * @deprecated - use {@link #createConcurrentOWLAPIImplManager()} instead
     */
    @Deprecated
    public static OWLOntologyManager createConcurrentOWL() {
        return createConcurrentOWLAPIImplManager();
    }

    /**
     * A factory abstraction to provide a manager and data-factory instances.
     */
    public interface Profile {

        /**
         * Creates a new {@code OWLOntologyManager} instance.
         *
         * @param concurrent boolean, if {@code true} the result manager expected to be thread-safe
         * @return {@link OWLOntologyManager}
         */
        OWLOntologyManager create(boolean concurrent);

        /**
         * Provides a {@code OWLDataFactory} instance.
         *
         * @return {@link OWLDataFactory}
         */
        default OWLDataFactory dataFactory() {
            return create(false).getOWLDataFactory();
        }
    }

    /**
     * An {@code ONT-API} impl of the {@link Profile}.
     */
    public static class ONTAPIProfile implements Profile {

        public static final DataFactory DEFAULT_DATA_FACTORY = new DataFactoryImpl();

        @Override
        public OntologyManager create(boolean concurrent) {
            ReadWriteLock lock = concurrent ? new ReentrantReadWriteLock() : NoOpReadWriteLock.NO_OP_RW_LOCK;
            Set<OWLStorerFactory> storers = OWLLangRegistry.storerFactories().collect(Collectors.toSet());
            Set<OWLParserFactory> parsers = OWLLangRegistry.parserFactories().collect(Collectors.toSet());
            OntologyManager res = createManager(dataFactory(), lock);
            res.getOntologyStorers().set(storers);
            res.getOntologyParsers().set(parsers);
            return res;
        }

        /**
         * Creates a fresh {@code ONT-API}-impl {@link OWLOntologyManager Ontology Manager}
         * with the given data factory and with the default ontology factory.
         * The returned manager does not have any {@code OWL-API} storers and parsers,
         * so it does not support reading and writing in formats that are not supported by {@code Apache Jena}.
         *
         * @param dataFactory {@link DataFactory} instance, not {@code null}
         * @param lock        {@link ReadWriteLock} r/w lock, can be {@code null}
         * @return {@link OntologyManager}
         */
        public OntologyManager createManager(DataFactory dataFactory, ReadWriteLock lock) {
            return createManager(dataFactory, createOntologyFactory(), lock);
        }

        /**
         * Creates a fresh {@link OntologyManager Ontology Manager}
         * with the given data and ontology factories.
         * The returned manager does not have any {@code OWL-API} storers and parsers,
         * so ir does not support reading and writing in formats that are not supported by {@code Apache Jena}.
         *
         * @param dataFactory {@link DataFactory}, not {@code null}
         * @param factory     {@link OntologyFactory}, not {@code null}
         * @param lock        {@link ReadWriteLock} or {@code null} for non-concurrent instance
         * @return {@link OntologyManager}
         */
        public OntologyManager createManager(DataFactory dataFactory, OntologyFactory factory, ReadWriteLock lock) {
            return new OntologyManagerImpl(dataFactory, factory, lock);
        }

        @Override
        public DataFactory dataFactory() {
            return DEFAULT_DATA_FACTORY;
        }

        /**
         * Creates a default {@link OntologyFactory.Builder Ontology Builder} -
         * an interface to create standalone ontologies.
         *
         * @return {@link OntologyFactory.Builder}
         */
        public OntologyFactory.Builder createOntologyBuilder() {
            return new OntologyBuilderImpl();
        }

        /**
         * Creates a default {@link OntologyFactory.Loader Ontology Load} -
         * an interface to read ontology documents.
         * The returned loader is capable to read a document using both {@code Apache Jena} and {@code OWL-API} mechanisms.
         * The priority is for {@code Apache Jena}. The {@code OWL-API} native mechanisms are used as last attempt to read a document,
         * and will work only if the corresponding {@link OWLParserFactory parsers}s are registered inside the manager.
         *
         * @return {@link OntologyFactory.Loader}
         * @see OWLLangRegistry#storerFactories()
         * @see OntologyManager#getOntologyStorers()
         */
        public OntologyFactory.Loader createOntologyLoader() {
            return new OntologyLoaderImpl(new OWLFactoryWrapper());
        }

        /**
         * Creates a default ontology factory instance.
         *
         * @return {@link OntologyFactory}
         */
        public OntologyFactory createOntologyFactory() {
            return createOntologyFactory(createOntologyBuilder());
        }

        /**
         * Creates an {@link OntologyFactory Ontology Factory} based on the given Builder.
         *
         * @param builder {@link OntologyFactory.Builder Ontology Builder}
         * @return {@link OntologyFactory} instance
         */
        public OntologyFactory createOntologyFactory(OntologyFactory.Builder builder) {
            return createOntologyFactory(builder, createOntologyLoader());
        }

        /**
         * Creates an {@link OntologyFactory Ontology Factory} based on the given Builder and Loader.
         *
         * @param builder {@link OntologyFactory.Builder Ontology Builder}
         * @param loader  {@link OntologyFactory.Loader Ontology Loader}
         * @return {@link OntologyFactory} instance
         */
        public OntologyFactory createOntologyFactory(OntologyFactory.Builder builder, OntologyFactory.Loader loader) {
            return loader.asOntologyFactory(builder);
        }
    }

    /**
     * Creates a {@link Profile profile} to retrieve
     * <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLOntologyManagerImpl.java'>manager</a>
     * and
     * <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLDataFactoryImpl.java'>data factory</a>
     * instances from {@code OWL-API} supply
     * (i.e. from {@code owlapi-apibinding} or directly from {@code owlapi-impl}) using reflection.
     *
     * @return Profile
     * @throws OntApiException in case no owlapi-* in classpath
     */
    public static Profile createOWLProfile() throws OntApiException {
        return new OWLAPIImplProfile();
    }

    /**
     * The {@code OWL-API} impl of {@link Profile} based on straightforward reflection.
     * The dependency owlapi-impl must be in classpath,
     * otherwise {@link OntApiException} is expected while initialization.
     *
     * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLOntologyManagerImpl.java'>uk.ac.manchester.cs.owl.owlapi.OWLOntologyManagerImpl</a>
     * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLOntologyImpl.java'>uk.ac.manchester.cs.owl.owlapi.OWLOntologyImpl</a>
     * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/concurrent/NonConcurrentOWLOntologyBuilder.java'>uk.ac.manchester.cs.owl.owlapi.concurrent.NonConcurrentOWLOntologyBuilder</a>
     * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/concurrent/ConcurrentOWLOntologyBuilder.java'>uk.ac.manchester.cs.owl.owlapi.concurrent.ConcurrentOWLOntologyBuilder</a>
     * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLOntologyFactoryImpl.java'>uk.ac.manchester.cs.owl.owlapi.OWLOntologyFactoryImpl</a>
     * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLDataFactoryImpl.java'>uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl</a>
     */
    public static class OWLAPIImplProfile implements Profile {
        private final Class<OWLOntologyManager> managerClass;

        public OWLAPIImplProfile() throws OntApiException {
            this.managerClass = ReflectionUtils.getClass(OWLOntologyManager.class,
                    "uk.ac.manchester.cs.owl.owlapi.OWLOntologyManagerImpl");
        }

        public OWLOntologyBuilder createOWLOntologyBuilder(ReadWriteLock lock) throws OntApiException {
            OWLOntologyBuilder res = ReflectionUtils.newInstance(OWLOntologyBuilder.class,
                    "uk.ac.manchester.cs.owl.owlapi.concurrent.NonConcurrentOWLOntologyBuilder",
                    LinkedListMultimap.create());
            if (!NoOpReadWriteLock.isConcurrent(lock)) return res;
            LinkedListMultimap<Class<?>, Object> concurrentParams = LinkedListMultimap.create();
            concurrentParams.put(OWLOntologyBuilder.class, res);
            concurrentParams.put(ReadWriteLock.class, lock);
            return ReflectionUtils.newInstance(OWLOntologyBuilder.class,
                    "uk.ac.manchester.cs.owl.owlapi.concurrent.ConcurrentOWLOntologyBuilder", concurrentParams);
        }

        public OWLOntology createOWLOntologyImpl(OWLOntologyManager manager, OWLOntologyID id) {
            LinkedListMultimap<Class<?>, Object> params = LinkedListMultimap.create();
            params.put(OWLOntologyManager.class, manager);
            params.put(OWLOntologyID.class, id);
            return ReflectionUtils.newInstance(OWLOntology.class,
                    "uk.ac.manchester.cs.owl.owlapi.OWLOntologyImpl", params);
        }

        @Override
        public OWLOntologyManager create(boolean concurrent) {
            ReadWriteLock lock = concurrent ? new ReentrantReadWriteLock() : NoOpReadWriteLock.NO_OP_RW_LOCK;
            OWLDataFactory dataFactory = createDataFactory();
            OWLOntologyFactory loadFactory = createOntologyFactory(createOWLOntologyBuilder(lock));
            OWLOntologyManager res = createManager(dataFactory, lock);
            Set<OWLStorerFactory> storers = OWLLangRegistry.storerFactories().collect(Collectors.toSet());
            Set<OWLParserFactory> parsers = OWLLangRegistry.parserFactories().collect(Collectors.toSet());
            res.getOntologyStorers().set(storers);
            res.getOntologyParsers().set(parsers);
            res.getOntologyFactories().add(loadFactory);
            return res;
        }

        /**
         * Creates a ready to use fresh {@code OWL-API-impl} Ontology Manager.
         *
         * @param dataFactory {@link OWLDataFactory} instance
         * @param lock        {@link ReadWriteLock} r/w lock
         * @return {@link OWLOntologyManager}
         */
        public OWLOntologyManager createManager(OWLDataFactory dataFactory, ReadWriteLock lock) {
            Constructor<OWLOntologyManager> constructor;
            try {
                constructor = managerClass.getConstructor(OWLDataFactory.class, ReadWriteLock.class);
            } catch (NoSuchMethodException e) {
                throw new OntApiException(managerClass.getName() + ": can't find constructor", e);
            }
            try {
                return constructor.newInstance(dataFactory, lock);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new OntApiException(managerClass.getName() + ": can't create new instance", e);
            }
        }

        @Override
        public OWLDataFactory dataFactory() {
            return createDataFactory();
        }

        public OWLDataFactory createDataFactory() {
            return ReflectionUtils.newInstance(OWLDataFactory.class,
                    "uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl", LinkedListMultimap.create());
        }

        public OWLOntologyFactory createOntologyFactory(OWLOntologyBuilder builder) {
            LinkedListMultimap<Class<?>, Object> params = LinkedListMultimap.create();
            params.put(OWLOntologyBuilder.class, builder);
            return ReflectionUtils.newInstance(OWLOntologyFactory.class,
                    "uk.ac.manchester.cs.owl.owlapi.OWLOntologyFactoryImpl", params);
        }

    }

}