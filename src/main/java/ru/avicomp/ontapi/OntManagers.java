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
 *
 */

package ru.avicomp.ontapi;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.reflect.Reflection;
import org.semanticweb.owlapi.io.OWLParserFactory;
import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.jena.OntModelFactory;
import ru.avicomp.owlapi.NoOpReadWriteLock;
import ru.avicomp.owlapi.OWLDataFactoryImpl;
import ru.avicomp.owlapi.OWLOntologyFactoryImpl;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Collections;
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

    public static final ONTManagerProfile DEFAULT_PROFILE = new ONTManagerProfile(false);
    private static Profile<? extends OWLOntologyManager> profile = DEFAULT_PROFILE;

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
        return DEFAULT_PROFILE.create();
    }

    /**
     * Creates an ONT-API ontology manager with default settings and locking for concurrent access.
     *
     * @return {@link OntologyManager} the new manager instance.
     */
    public static OntologyManager createConcurrentONT() {
        return new ONTManagerProfile(true).create();
    }

    /**
     * Provides an original pure OWL-API ontology manager instance with default settings.
     * Notes:
     * <ul>
     * <li>This method is not a direct part of ONT-API, it is for convenience and/or test purposes only.
     * Better to use the original solution from OWL-API supply.</li>
     * <li><a href='https://github.com/owlcs/owlapi/blob/version5/impl/'>owlapi-impl</a> must be in class-path</li>
     * </ul>
     *
     * @return {@link OWLOntologyManager}
     * @throws OntApiException       if there is no owlapi-impl in class-path
     * @throws IllegalStateException if unable to find factories methods inside owlapi-apibinding manager provider
     * @see <a href='https://github.com/owlcs/owlapi/blob/version5/apibinding/src/main/java/org/semanticweb/owlapi/apibinding/OWLManager.java#L43'>org.semanticweb.owlapi.apibinding.OWLManager#createOWLOntologyManager()</a>
     */
    public static OWLOntologyManager createOWL() {
        return createOWLProfile(false).create();
    }

    /**
     * Provides an original pure OWL-API ontology manager with instance default settings and locking for concurrent access.
     * Notes:
     * <ul>
     * <li>This method is not a direct part of ONT-API, it is for convenience and/or test purposes only.
     * Better to use the original solution from OWL-API supply.</li>
     * <li><a href='https://github.com/owlcs/owlapi/blob/version5/impl/'>owlapi-impl</a> must be in class-path</li>
     * </ul>
     *
     * @return {@link OWLOntologyManager}
     * @throws OntApiException       if there is no owlapi-apibinding in class-path
     * @throws IllegalStateException if unable to find factories methods inside owlapi-apibinding manager provider
     * @see <a href='https://github.com/owlcs/owlapi/blob/version5/apibinding/src/main/java/org/semanticweb/owlapi/apibinding/OWLManager.java#L53'>org.semanticweb.owlapi.apibinding.OWLManager#createConcurrentOWLOntologyManager()</a>
     */
    public static OWLOntologyManager createConcurrentOWL() {
        return createOWLProfile(true).create();
    }

    /**
     * Gets a default global {@link Profile profile}
     *
     * @return profile
     */
    public static Profile<? extends OWLOntologyManager> getProfile() {
        return profile;
    }

    /**
     * Sets new default global {@link Profile profile}
     *
     * @param p profile object, not null
     */
    public static void setProfile(Profile<? extends OWLOntologyManager> p) {
        profile = OntApiException.notNull(p, "Null manager profile specified.");
    }

    @Override
    public OWLOntologyManager get() {
        return profile.create();
    }

    /**
     * Creates an OWL-API load factory instance.
     * Static accessor to the {@link OWLOntologyFactory} OWL-API implementation.
     *
     * @param builder {@link OWLOntologyBuilder}, required parameter
     * @return {@link OWLOntologyFactory} instance or null if it absents in class-path.
     */
    @SuppressWarnings("ConstantConditions")
    @Nullable
    static OWLOntologyFactory createOWLOntologyLoadFactory(OWLOntologyBuilder builder) {
        return new OWLOntologyFactoryImpl(builder);
    }

    /**
     * The provider for manager and data-factory.
     *
     * @param <M> a subtype of {@link OWLOntologyManager}
     */
    @FunctionalInterface
    public interface Profile<M extends OWLOntologyManager> {
        OWLDataFactory DEFAULT_DATA_FACTORY = new OWLDataFactoryImpl();

        /**
         * Creates a new OWLOntologyManager instance.
         *
         * @return {@link OWLOntologyManager}
         */
        M create();

        /**
         * Provides OWLDataFactory instance.
         *
         * @return {@link OWLDataFactory}
         */
        default OWLDataFactory dataFactory() {
            return DEFAULT_DATA_FACTORY;
        }
    }

    /**
     * Abstract Profile which supports concurrency mode.
     */
    protected static abstract class BaseProfile {
        final boolean concurrency;

        private BaseProfile(boolean concurrent) {
            this.concurrency = concurrent;
        }

        public boolean isConcurrent() {
            return concurrency;
        }

    }

    /**
     * The ONT-API impl of the {@link Profile}.
     */
    public static class ONTManagerProfile extends BaseProfile implements Profile<OntologyManager> {

        public ONTManagerProfile(boolean concurrent) {
            super(concurrent);
        }

        @Override
        public OntologyManager create() {
            Set<OWLStorerFactory> storers = OWLLangRegistry.storerFactories().collect(Collectors.toSet());
            Set<OWLParserFactory> parsers = OWLLangRegistry.parserFactories().collect(Collectors.toSet());
            ReadWriteLock lock = isConcurrent() ? new ReentrantReadWriteLock() : new NoOpReadWriteLock();
            OntologyManager res = new OntologyManagerImpl(this.dataFactory(), lock);
            res.setOntologyStorers(storers);
            res.setOntologyParsers(parsers);
            return res;
        }
    }

    private static Class<?> findClass(String name) throws OntApiException {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new OntApiException("No " + name + " in class-path found. " +
                    "Please include corresponding module to maven dependencies.", e);
        }
    }

    /**
     * Creates a {@link Profile profile} to retrieve
     * <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLOntologyManagerImpl.java'>manager</a> and
     * <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLDataFactoryImpl.java'>data factory</a> instances from OWL-API supply
     * (i.e. from owlapi-apibinding or owlapi-impl) using reflection.
     *
     * @param concurrency boolean
     * @return Profile
     * @throws OntApiException in case no owlapi-* in class-path
     */
    public static Profile<OWLOntologyManager> createOWLProfile(boolean concurrency) throws OntApiException {
        try {
            return new OWLAPIBindingProfile(concurrency);
        } catch (OntApiException i) {
            try {
                return new OWLAPIImplProfile(concurrency);
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
     * must be in class-path.
     */
    private static class OWLAPIBindingProfile extends BaseProfile implements Profile<OWLOntologyManager> {
        private final Class<?> provider;

        private OWLAPIBindingProfile(boolean concurrent) throws OntApiException, IllegalStateException {
            super(concurrent);
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
        public OWLOntologyManager create() throws IllegalStateException {
            Method manager = findStaticMethod(provider, isConcurrent() ? "createConcurrentOWLOntologyManager" : "createOWLOntologyManager");
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
     * The dependency owlapi-impl must be in class-path.
     *
     * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLOntologyManagerImpl.java'>uk.ac.manchester.cs.owl.owlapi.OWLOntologyManagerImpl</a>
     * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLOntologyImpl.java'>uk.ac.manchester.cs.owl.owlapi.OWLOntologyImpl</a>
     * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLOntologyImplementationFactory.java'>uk.ac.manchester.cs.owl.owlapi.OWLOntologyImplementationFactory</a>
     * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/concurrent/NonConcurrentOWLOntologyBuilder.java'>uk.ac.manchester.cs.owl.owlapi.concurrent.NonConcurrentOWLOntologyBuilder</a>
     * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/concurrent/ConcurrentOWLOntologyBuilder.java'>uk.ac.manchester.cs.owl.owlapi.concurrent.ConcurrentOWLOntologyBuilder</a>
     * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLOntologyFactoryImpl.java'>uk.ac.manchester.cs.owl.owlapi.OWLOntologyFactoryImpl</a>
     * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLDataFactoryImpl.java'>uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl</a>
     */
    private static class OWLAPIImplProfile extends BaseProfile implements Profile<OWLOntologyManager> {
        private final Class<?> managerClass;
        private ReadWriteLock lock;

        private OWLAPIImplProfile(boolean concurrent) {
            super(concurrent);
            this.managerClass = findClass("uk.ac.manchester.cs.owl.owlapi.OWLOntologyManagerImpl");
        }

        public OWLOntologyBuilder createOWLOntologyBuilder() throws IllegalStateException {
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
            if (!concurrency) return res;
            LinkedListMultimap<Class<?>, Object> concurrentParams = LinkedListMultimap.create();
            concurrentParams.put(OWLOntologyBuilder.class, res);
            concurrentParams.put(ReadWriteLock.class, lock());
            return (OWLOntologyBuilder) newInstance("uk.ac.manchester.cs.owl.owlapi.concurrent.ConcurrentOWLOntologyBuilder", concurrentParams);
        }

        public OWLOntology createOWLOntologyImpl(OWLOntologyManager manager, OWLOntologyID id) {
            LinkedListMultimap<Class<?>, Object> params = LinkedListMultimap.create();
            params.put(OWLOntologyManager.class, manager);
            params.put(OWLOntologyID.class, id);
            return (OWLOntology) newInstance("uk.ac.manchester.cs.owl.owlapi.OWLOntologyImpl", params);
        }

        public ReadWriteLock lock() {
            if (lock != null) return lock;
            synchronized (this) {
                if (lock != null) return lock;
                return lock = isConcurrent() ? new ReentrantReadWriteLock() : new NoOpReadWriteLock();
            }
        }

        @Override
        public OWLOntologyManager create() {
            Constructor<?> constructor;
            try {
                constructor = managerClass.getConstructor(OWLDataFactory.class, ReadWriteLock.class);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(managerClass.getName() + ": can't find constructor", e);
            }
            OWLDataFactory dataFactory = createDataFactory(false);
            OWLOntologyFactory loadFactory = createLoadFactory(createOWLOntologyBuilder());
            OWLOntologyManager res;
            try {
                res = (OWLOntologyManager) constructor.newInstance(dataFactory, lock());
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException(managerClass.getName() + ": can't create new instance", e);
            }
            Set<OWLStorerFactory> storers = OWLLangRegistry.storerFactories().collect(Collectors.toSet());
            Set<OWLParserFactory> parsers = OWLLangRegistry.parserFactories().collect(Collectors.toSet());

            res.setOntologyStorers(storers);
            res.setOntologyParsers(parsers);
            res.setOntologyFactories(Collections.singleton(loadFactory));
            return res;
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