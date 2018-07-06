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

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.reflect.Reflection;
import org.semanticweb.owlapi.io.OWLParserFactory;
import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.jena.OntModelFactory;
import ru.avicomp.owlapi.OWLDataFactoryImpl;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * The main (static) access point to {@link OWLOntologyManager} instances.
 * This is an analogue of
 * <a href='https://github.com/owlcs/owlapi/blob/version5/apibinding/src/main/java/org/semanticweb/owlapi/apibinding/OWLManager.java'>org.semanticweb.owlapi.apibinding.OWLManager</a>.
 * Notes:
 * <ul>
 * <li>To produce original pure OWL-API managers and factories need to include owlapi-apibinding or owlapi-impl modules to class-path</li>
 * <li>Instead of injections by google guice the straightforward reflection mechanisms are used to construct OWL-API impls</li>
 * </ul>
 * Created by @szuev on 27.09.2016.
 */
@SuppressWarnings("WeakerAccess")
public class OntManagers implements OWLOntologyManagerFactory {

    static {
        OntModelFactory.init();
    }

    public static final ONTManagerProfile DEFAULT_PROFILE = new ONTManagerProfile();
    private static OWLOntologyManagerFactory delegate = () -> DEFAULT_PROFILE.create(false);

    /**
     * Gets the global data factory that can be used to create OWL API objects.
     *
     * @return {@link OWLDataFactory} impl
     */
    public static OWLDataFactory getDataFactory() {
        return DEFAULT_PROFILE.dataFactory();
    }

    /**
     * Creates an ONT-API ontology manager with default settings.
     *
     * @return {@link OntologyManager} the new manager instance.
     */
    public static OntologyManager createONT() {
        return DEFAULT_PROFILE.create(false);
    }

    /**
     * Creates an ONT-API ontology manager with default settings and locking to work in a concurrent environment.
     *
     * @return {@link OntologyManager} the new manager instance.
     */
    public static OntologyManager createConcurrentONT() {
        return DEFAULT_PROFILE.create(true);
    }

    /**
     * Creates an original (pure) OWL-API ontology manager instance with default settings.
     * Notes:
     * <ul>
     * <li>This method is not a direct part of ONT-API, it is for convenience and/or test purposes only.
     * Better to use a similar method from OWL-API(apibinding) supply, if it is available.</li>
     * <li><a href='https://github.com/owlcs/owlapi/blob/version5/impl/'>owlapi-impl</a> must be in class-path</li>
     * </ul>
     *
     * @return {@link OWLOntologyManager}
     * @throws OntApiException       if there is no owlapi-impl in class-path
     * @throws IllegalStateException some unexpected exception while constructing manager using reflection
     * @see <a href='https://github.com/owlcs/owlapi/blob/version5/apibinding/src/main/java/org/semanticweb/owlapi/apibinding/OWLManager.java#L43'>org.semanticweb.owlapi.apibinding.OWLManager#createOWLOntologyManager()</a>
     */
    public static OWLOntologyManager createOWL() {
        return createOWLProfile().create(false);
    }

    /**
     * Creates an original (pure) OWL-API ontology manager instance with default settings and locking to work in a concurrent environment.
     * Notes:
     * <ul>
     * <li>This method is not a direct part of ONT-API, it is for convenience and/or test purposes only.
     * Better to use a similar method from OWL-API(apibinding) supply, if it is available.</li>
     * <li><a href='https://github.com/owlcs/owlapi/blob/version5/impl/'>owlapi-impl</a> must be in class-path</li>
     * </ul>
     *
     * @return {@link OWLOntologyManager}
     * @throws OntApiException       if there is no owlapi-impl in class-path
     * @throws IllegalStateException some unexpected exception while constructing manager using reflection
     * @see <a href='https://github.com/owlcs/owlapi/blob/version5/apibinding/src/main/java/org/semanticweb/owlapi/apibinding/OWLManager.java#L53'>org.semanticweb.owlapi.apibinding.OWLManager#createConcurrentOWLOntologyManager()</a>
     */
    public static OWLOntologyManager createConcurrentOWL() {
        return createOWLProfile().create(true);
    }

    /**
     * Returns a default static {@link OWLOntologyManagerFactory factory}.
     *
     * @return profile
     */
    public static OWLOntologyManagerFactory getFactory() {
        return delegate;
    }

    /**
     * Changes a default static {@link OWLOntologyManagerFactory factory}.
     *
     * @param p profile object, not null
     */
    public static void setFactory(OWLOntologyManagerFactory p) {
        delegate = OntApiException.notNull(p, "Null manager profile specified.");
    }

    @Override
    public OWLOntologyManager get() {
        return delegate.get();
    }

    /**
     * A factory to provide manager and data-factory.
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
    public static class ONTManagerProfile implements Profile {

        public static final OWLDataFactory DEFAULT_DATA_FACTORY = new OWLDataFactoryImpl();

        @Override
        public OntologyManager create(boolean concurrent) {
            ReadWriteLock lock = concurrent ? new ReentrantReadWriteLock() : NoOpReadWriteLock.NO_OP_RW_LOCK;
            Set<OWLStorerFactory> storers = OWLLangRegistry.storerFactories().collect(Collectors.toSet());
            Set<OWLParserFactory> parsers = OWLLangRegistry.parserFactories().collect(Collectors.toSet());
            OntologyManager res = create(dataFactory(), lock);
            res.getOntologyStorers().set(storers);
            res.getOntologyParsers().set(parsers);
            return res;
        }

        /**
         * Creates a ready to use fresh ONT-API-impl Ontology Manager.
         *
         * @param dataFactory {@link OWLDataFactory} instance
         * @param lock        {@link ReadWriteLock} r/w lock
         * @return {@link OntologyManager}
         */
        public OntologyManager create(OWLDataFactory dataFactory, ReadWriteLock lock) {
            OntologyFactory factory = createOntologyFactory(createOntologyBuilder());
            return create(dataFactory, factory, lock);
        }

        public OntologyManager create(OWLDataFactory dataFactory, OntologyFactory factory, ReadWriteLock lock) {
            return new OntologyManagerImpl(dataFactory, factory, lock);
        }

        @Override
        public OWLDataFactory dataFactory() {
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

    private static Class<?> findClass(String name) throws OntApiException {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new OntApiException("No " + name + " in class-path found. " +
                    "Please include corresponding library to maven dependencies.", e);
        }
    }

    /**
     * Creates a {@link Profile profile} to retrieve
     * <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLOntologyManagerImpl.java'>manager</a> and
     * <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLDataFactoryImpl.java'>data factory</a> instances from OWL-API supply
     * (i.e. from owlapi-apibinding or owlapi-impl) using reflection.
     *
     * @return Profile
     * @throws OntApiException in case no owlapi-* in class-path
     */
    public static Profile createOWLProfile() throws OntApiException {
        try {
            return new OWLAPIBindingProfile();
        } catch (OntApiException i) {
            try {
                return new OWLAPIImplProfile();
            } catch (OntApiException j) {
                i.addSuppressed(j);
            }
            throw i;
        }
    }

    /**
     * The OWL-API impl of {@link Profile}.
     * The owlapi-apibinding
     * (class <a href='https://github.com/owlcs/owlapi/blob/version5/apibinding/src/main/java/org/semanticweb/owlapi/apibinding/OWLManager.java'>org.semanticweb.owlapi.apibinding.OWLManager</a>)
     * must be in class-path otherwise OntApiException is expected while initialization.
     */
    public static class OWLAPIBindingProfile implements Profile {
        private final Class<?> provider;

        public OWLAPIBindingProfile() throws OntApiException {
            this.provider = findClass("org.semanticweb.owlapi.apibinding.OWLManager");
        }

        private static Method findStaticMethod(Class<?> provider, String name) throws IllegalStateException {
            try {
                return provider.getMethod(name);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("Can't find method '" + name + "' in " + provider, e);
            }
        }

        @Override
        public OWLOntologyManager create(boolean concurrent) throws IllegalStateException {
            Method manager = findStaticMethod(provider, concurrent ? "createConcurrentOWLOntologyManager" : "createOWLOntologyManager");
            try {
                return (OWLOntologyManager) manager.invoke(null);
            } catch (IllegalAccessException | InvocationTargetException | ClassCastException e) {
                throw new IllegalStateException("Can't create manager using " + manager, e);
            }
        }

        @Override
        public OWLDataFactory dataFactory() {
            Method factory = findStaticMethod(provider, "getOWLDataFactory");
            try {
                return (OWLDataFactory) factory.invoke(null);
            } catch (IllegalAccessException | InvocationTargetException | ClassCastException e) {
                throw new IllegalStateException("Can't create data factory using " + factory, e);
            }
        }
    }

    /**
     * The OWL-API impl of {@link Profile} based on straightforward reflection.
     * The dependency owlapi-impl must be in class-path otherwise OntApiException is expected while initialization.
     *
     * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLOntologyManagerImpl.java'>uk.ac.manchester.cs.owl.owlapi.OWLOntologyManagerImpl</a>
     * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLOntologyImpl.java'>uk.ac.manchester.cs.owl.owlapi.OWLOntologyImpl</a>
     * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLOntologyImplementationFactory.java'>uk.ac.manchester.cs.owl.owlapi.OWLOntologyImplementationFactory</a>
     * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/concurrent/NonConcurrentOWLOntologyBuilder.java'>uk.ac.manchester.cs.owl.owlapi.concurrent.NonConcurrentOWLOntologyBuilder</a>
     * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/concurrent/ConcurrentOWLOntologyBuilder.java'>uk.ac.manchester.cs.owl.owlapi.concurrent.ConcurrentOWLOntologyBuilder</a>
     * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLOntologyFactoryImpl.java'>uk.ac.manchester.cs.owl.owlapi.OWLOntologyFactoryImpl</a>
     * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLDataFactoryImpl.java'>uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl</a>
     */
    public static class OWLAPIImplProfile implements Profile {
        private final Class<?> managerClass;

        public OWLAPIImplProfile() throws OntApiException {
            this.managerClass = findClass("uk.ac.manchester.cs.owl.owlapi.OWLOntologyManagerImpl");
        }

        public OWLOntologyBuilder createOWLOntologyBuilder(ReadWriteLock lock) throws IllegalStateException {
            Class<?> owlOntologyImplementationFactoryType = findClass("uk.ac.manchester.cs.owl.owlapi.OWLOntologyImplementationFactory");
            Object owlOntologyImplementationFactoryInstance = Reflection.newProxy(owlOntologyImplementationFactoryType,
                    (proxy, method, args) -> {
                        if ("createOWLOntology".equals(method.getName()) && args != null && args.length == 2) {
                            OWLOntologyManager m = (OWLOntologyManager) args[0];
                            OWLOntologyID id = (OWLOntologyID) args[1];
                            return createOWLOntologyImpl(m, id);
                        }
                        String name = "Instance of " + owlOntologyImplementationFactoryType.getName();
                        if ("toString".equals(method.getName()) && args == null) {
                            return name;
                        }
                        throw new OntApiException("[" + name + "] unsupported method call: " + method);
                    });
            LinkedListMultimap<Class<?>, Object> nonConcurrentParams = LinkedListMultimap.create();
            nonConcurrentParams.put(owlOntologyImplementationFactoryType, owlOntologyImplementationFactoryInstance);
            OWLOntologyBuilder res = (OWLOntologyBuilder) newInstance("uk.ac.manchester.cs.owl.owlapi.concurrent.NonConcurrentOWLOntologyBuilder",
                    nonConcurrentParams);
            if (lock == null || NoOpReadWriteLock.NO_OP_RW_LOCK.equals(lock)) return res;
            LinkedListMultimap<Class<?>, Object> concurrentParams = LinkedListMultimap.create();
            concurrentParams.put(OWLOntologyBuilder.class, res);
            concurrentParams.put(ReadWriteLock.class, lock);
            return (OWLOntologyBuilder) newInstance("uk.ac.manchester.cs.owl.owlapi.concurrent.ConcurrentOWLOntologyBuilder", concurrentParams);
        }

        public OWLOntology createOWLOntologyImpl(OWLOntologyManager manager, OWLOntologyID id) {
            LinkedListMultimap<Class<?>, Object> params = LinkedListMultimap.create();
            params.put(OWLOntologyManager.class, manager);
            params.put(OWLOntologyID.class, id);
            return (OWLOntology) newInstance("uk.ac.manchester.cs.owl.owlapi.OWLOntologyImpl", params);
        }

        @Override
        public OWLOntologyManager create(boolean concurrent) {
            ReadWriteLock lock = concurrent ? new ReentrantReadWriteLock() : NoOpReadWriteLock.NO_OP_RW_LOCK;
            OWLDataFactory dataFactory = createDataFactory(false);
            OWLOntologyFactory loadFactory = createLoadFactory(createOWLOntologyBuilder(lock));
            OWLOntologyManager res = create(dataFactory, lock);
            Set<OWLStorerFactory> storers = OWLLangRegistry.storerFactories().collect(Collectors.toSet());
            Set<OWLParserFactory> parsers = OWLLangRegistry.parserFactories().collect(Collectors.toSet());
            res.getOntologyStorers().set(storers);
            res.getOntologyParsers().set(parsers);
            res.getOntologyFactories().add(loadFactory);
            return res;
        }

        /**
         * Creates a ready to use fresh OWL-API-impl Ontology Manager.
         * @param dataFactory {@link OWLDataFactory} instance
         * @param lock {@link ReadWriteLock} r/w lock
         * @return {@link OWLOntologyManager}
         */
        public OWLOntologyManager create(OWLDataFactory dataFactory, ReadWriteLock lock) {
            Constructor<?> constructor;
            try {
                constructor = managerClass.getConstructor(OWLDataFactory.class, ReadWriteLock.class);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(managerClass.getName() + ": can't find constructor", e);
            }
            try {
                return (OWLOntologyManager) constructor.newInstance(dataFactory, lock);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException(managerClass.getName() + ": can't create new instance", e);
            }
        }

        @Override
        public OWLDataFactory dataFactory() {
            return createDataFactory(false);
        }

        public OWLDataFactory createDataFactory(boolean withCompression) {
            LinkedListMultimap<Class<?>, Object> params = LinkedListMultimap.create();
            params.put(Boolean.TYPE, withCompression);
            return (OWLDataFactory) newInstance("uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl", params);
        }

        public OWLOntologyFactory createLoadFactory(OWLOntologyBuilder builder) {
            LinkedListMultimap<Class<?>, Object> params = LinkedListMultimap.create();
            params.put(OWLOntologyBuilder.class, builder);
            return (OWLOntologyFactory) newInstance("uk.ac.manchester.cs.owl.owlapi.OWLOntologyFactoryImpl", params);
        }

        /**
         * Creates an instance.
         *
         * @param classPath String, full class-path
         * @param params    {@link ListMultimap} collection of parameters, class-type as key, object as value
         * @return new instance of specified class.
         * @throws OntApiException       if no class found
         * @throws IllegalStateException if class does not meet expectations
         */
        private static Object newInstance(String classPath, LinkedListMultimap<Class<?>, Object> params) {
            Class<?> clazz = findClass(classPath);
            String name = MessageFormat.format("{0}({1})", clazz.getName(),
                    params.keys().stream().map(Class::getSimpleName).collect(Collectors.joining(", ")));
            Constructor<?> constructor;
            try {
                constructor = clazz.getConstructor(params.keys().toArray(new Class[params.size()]));
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("Unable to find constructor " + name, e);
            }
            try {
                return constructor.newInstance(params.values().toArray());
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException("Can't init " + name);
            }
        }
    }

}