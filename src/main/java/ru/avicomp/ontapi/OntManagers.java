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

package ru.avicomp.ontapi;

import com.google.common.collect.LinkedListMultimap;
import org.semanticweb.owlapi.io.OWLParserFactory;
import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.jena.OntModelFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * The main (static) accessor to an {@link OWLOntologyManager} with its commonly required features,
 * that provides facilities for creating both <b>ONT-API</b> and native <b>OWL-API</b> instances,
 * but the latter facilities are optional, which have been included just for convenience and completeness,
 * and which only work if {@code owl-api-impl} is present in the classpath.
 * This is an analogue of
 * <a href='https://github.com/owlcs/owlapi/blob/version5/apibinding/src/main/java/org/semanticweb/owlapi/apibinding/OWLManager.java'>org.semanticweb.owlapi.apibinding.OWLManager</a>.
 * <p>
 * Implementation notes:
 * Unlike the {@code OWL-API-apibinding} implementation, no injections are used to construct any object.
 * To create ONT-API instances there are direct factory methods.
 * To create OWL-API instances the straightforward reflection is used.
 * There are no injections since I still don't see any good reason to support it,
 * and also don't need one more place for bugs.
 * If you plan to work with original (pure) OWL-API managers and factories, please include either
 * <a href='https://github.com/owlcs/owlapi/blob/version5/apibinding/'>net.sourceforge.owlapi:owlapi-apibinding</a> or
 * <a href='https://github.com/owlcs/owlapi/blob/version5/impl/'>net.sourceforge.owlapi:owlapi-impl</a> artifacts
 * into your classpath, otherwise the corresponding methods ({@link #createOWL()}, {@link #createConcurrentOWL()})
 * will throw a runtime exception.
 * <p>
 * Created by @szuev on 27.09.2016.
 */
@SuppressWarnings("WeakerAccess")
public class OntManagers implements OWLOntologyManagerFactory {

    static {
        OntModelFactory.init();
    }

    public static final ONTAPIProfile DEFAULT_PROFILE = new ONTAPIProfile();
    private static OWLOntologyManagerFactory managerFactory = () -> DEFAULT_PROFILE.create(false);

    /**
     * Gets the global data factory that can be used to create any OWL API object,
     * including {@link OWLAxiom OWL Axionms}, {@link OWLEntity OWL Entities} and {@link OWLLiteral}.
     * This allows to assembly ontology by adding into it any {@link OWLObject}
     * derived from the {@link DataFactory OWL Data Factory}.
     * <p>
     * Alternative way to assembly ontology is direct working with the {@link org.apache.jena.graph.Graph Graph}
     * through the {@link ru.avicomp.ontapi.jena.model.OntGraphModel} view of the ontology,
     * that can be obtained using the method {@link OntologyModel#asGraphModel()}.
     * The first way of ontology editing is native for OWL-API,
     * the second way is native for Apache Jena and provided by ONT-API as a feature,
     * that actually underlies any interaction with ontologies in ONT-API.
     *
     * @return {@link DataFactory} impl
     */
    public static DataFactory getDataFactory() {
        return DEFAULT_PROFILE.dataFactory();
    }

    /**
     * Creates a ready to use ONT-API ontology manager with a default configuration,
     * that includes settings from a properties file {@code /ontapi.properties},
     * various format-syntaxes for saving/loading ontologies from the {@code jena-arq} package
     * and, if they are in a classpath, additional format-syntaxes from the OWL-API supply
     * (packages {@code owlapi-rio}, {@code owlapi-oboformat}).
     * More about format-syntaxes can be found in {@link OntFormat} class.
     * <p>
     * The returned manager is not thread-safe:
     * safety of a manager's ontology, that is shared between threads,
     * is not guaranteed even if there is no write operations.
     * <p>
     * This is the primary factory method to produce {@link OntologyManager}s
     * that should be used when there is no reason to use any other method to create manager's instances.
     * In other words, if you have doubt what method to use, choose this one.
     *
     * @return {@link OntologyManager} a fresh ONT-API manager instance
     */
    public static OntologyManager createONT() {
        return DEFAULT_PROFILE.create(false);
    }

    /**
     * Creates a ready to use ONT-API ontology manager with a default configuration
     * and locking to work in a concurrent environment.
     * The returned manager itself and any its component (i.e. ontologies) are thread-safe,
     * i.e. can be safely shared between different threads.
     *
     * @return {@link OntologyManager} a fresh ONT-API manager instance with concurrency
     */
    public static OntologyManager createConcurrentONT() {
        return DEFAULT_PROFILE.create(true);
    }

    /**
     * Creates an original OWL-API (i.e. pure native impl) ontology manager instance with a default configuration.
     * Notes:
     * <ul>
     * <li>This method is not a direct part of ONT-API, it is here for convenience and/or test purposes only.
     * Better to use a similar method from {@code OWL-API-apibinding} supply, if it is available.
     * See {@code org.semanticweb.owlapi.apibinding.OWLManager}.</li>
     * <li><a href='https://github.com/owlcs/owlapi/blob/version5/impl/'>owlapi-impl</a> must be in the classpath</li>
     * </ul>
     *
     * @return {@link OWLOntologyManager} a fresh OWL-API manager instance
     * @throws OntApiException if there is no {@code owlapi-impl} in class-path or some unexpected error is occurred
     */
    public static OWLOntologyManager createOWL() throws OntApiException {
        return createOWLProfile().create(false);
    }

    /**
     * Creates an original OWL-API (i.e pure native impl) ontology manager instance
     * with a default configuration and locking to work in a concurrent environment.
     * Notes:
     * <ul>
     * <li>This method is not a direct part of ONT-API, it is here for convenience and/or test purposes only.
     * Better to use a similar method from {@code OWL-API-apibinding} supply, if it is available.
     * See {@code org.semanticweb.owlapi.apibinding.OWLManager}.</li>
     * <li><a href='https://github.com/owlcs/owlapi/blob/version5/impl/'>owlapi-impl</a> must be in the class-path</li>
     * </ul>
     *
     * @return {@link OWLOntologyManager} a fresh OWL-API manager instance with concurrency
     * @throws OntApiException if there is no {@code owlapi-impl} in class-path or some unexpected error is occurred
     */
    public static OWLOntologyManager createConcurrentOWL() throws OntApiException {
        return createOWLProfile().create(true);
    }

    /**
     * Returns a default static {@link OWLOntologyManagerFactory factory}.
     *
     * @return profile
     */
    public static OWLOntologyManagerFactory getFactory() {
        return managerFactory;
    }

    /**
     * Changes a default static {@link OWLOntologyManagerFactory factory}.
     * This a way to manage {@link #get()} method behaviour and should not be used without a really good reason.
     * Though, in ONT-API it is unused.
     *
     * @param p profile object, not {@code null}
     * @see #get()
     */
    public static void setFactory(OWLOntologyManagerFactory p) {
        managerFactory = OntApiException.notNull(p, "Null manager profile specified.");
    }

    @Override
    public OWLOntologyManager get() {
        return managerFactory.get();
    }

    /**
     * A factory abstraction to provide a manager and data-factory instances.
     */
    public interface Profile {

        /**
         * Creates a new OWLOntologyManager instance.
         *
         * @param concurrent boolean, if true the result manager expected to be thread-safe
         * @return {@link OWLOntologyManager}
         */
        OWLOntologyManager create(boolean concurrent);

        /**
         * Provides OWLDataFactory instance.
         *
         * @return {@link OWLDataFactory}
         */
        OWLDataFactory dataFactory();
    }

    /**
     * An ONT-API impl of the {@link Profile}.
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
         * Creates a ready to use fresh ONT-API-impl Ontology Manager.
         *
         * @param dataFactory {@link DataFactory} instance
         * @param lock        {@link ReadWriteLock} r/w lock
         * @return {@link OntologyManager}
         */
        public OntologyManager createManager(DataFactory dataFactory, ReadWriteLock lock) {
            OntologyFactory factory = createOntologyFactory(createOntologyBuilder());
            return createManager(dataFactory, factory, lock);
        }

        /**
         * Creates {@link OntologyManager Ontology Manager}.
         *
         * @param dataFactory {@link DataFactory}
         * @param factory     {@link OntologyFactory}
         * @param lock        {@link ReadWriteLock}
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
         * Creates an {@link OntologyFactory.Builder Ontology Builder} - an interface to create standalone ontologies.
         *
         * @return {@link OntologyFactory.Builder}
         */
        public OntologyFactory.Builder createOntologyBuilder() {
            return new OntologyBuilderImpl();
        }

        /**
         * Creates an {@link OntologyFactory Ontology Factory} based on the given Builder.
         *
         * @param builder {@link OntologyFactory.Builder Ontology Builder}
         * @return {@link OntologyFactory} instance
         */
        public OntologyFactory createOntologyFactory(OntologyFactory.Builder builder) {
            OntologyFactory.Loader loader = new OntologyLoaderImpl(builder,
                    new OWLLoaderImpl(builder));
            return createOntologyFactory(builder, loader);
        }

        /**
         * Creates an {@link OntologyFactory Ontology Factory} based on the given Builder and Loader.
         *
         * @param builder {@link OntologyFactory.Builder Ontology Builder}
         * @param loader  {@link OntologyFactory.Loader Ontology Loader}
         * @return {@link OntologyFactory} instance
         */
        public OntologyFactory createOntologyFactory(OntologyFactory.Builder builder, OntologyFactory.Loader loader) {
            return new OntologyFactoryImpl(builder, loader);
        }
    }

    /**
     * Creates a {@link Profile profile} to retrieve
     * <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLOntologyManagerImpl.java'>manager</a>
     * and
     * <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLDataFactoryImpl.java'>data factory</a>
     * instances from OWL-API supply
     * (i.e. from {@code owlapi-apibinding} or directly from {@code owlapi-impl}) using reflection.
     *
     * @return Profile
     * @throws OntApiException in case no owlapi-* in class-path
     */
    public static Profile createOWLProfile() throws OntApiException {
        return new OWLAPIImplProfile();
    }

    /**
     * The OWL-API impl of {@link Profile} based on straightforward reflection.
     * The dependency owlapi-impl must be in class-paths,
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
         * Creates a ready to use fresh OWL-API-impl Ontology Manager.
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