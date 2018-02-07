/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2017, Avicomp Services, AO
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

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import org.semanticweb.owlapi.io.OWLParserFactory;
import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.jena.OntModelFactory;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLOntologyFactoryImpl;
import uk.ac.manchester.cs.owl.owlapi.concurrent.Concurrency;
import uk.ac.manchester.cs.owl.owlapi.concurrent.NoOpReadWriteLock;

/**
 * The main (static) access point to {@link OWLOntologyManager} instances.
 * This is an analogue of
 * <a href='https://github.com/owlcs/owlapi/blob/version5/apibinding/src/main/java/org/semanticweb/owlapi/apibinding/OWLManager.java'>org.semanticweb.owlapi.apibinding.OWLManager</a>.
 * Note: to produce pure OWL-API managers need include owlapi-apibinding module to class-path.
 * <p>
 * Created by @szuev on 27.09.2016.
 */
@SuppressWarnings("WeakerAccess")
public class OntManagers implements OWLOntologyManagerFactory {

    static {
        OntModelFactory.init();
    }

    public static final ONTManagerProfile DEFAULT_PROFILE = new ONTManagerProfile(Concurrency.NON_CONCURRENT);
    private static Profile<? extends OWLOntologyManager> profile = DEFAULT_PROFILE;

    /**
     * Gets a global data factory that can be used to create OWL API objects.
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
        return new ONTManagerProfile(Concurrency.CONCURRENT).create();
    }

    /**
     * Creates a pure OWL-API ontology manager with default settings.
     *
     * @return {@link OWLOntologyManager}
     * @throws OntApiException       if there is no owlapi-apibinding in class-path
     * @throws IllegalStateException if unable to find factories methods inside owlapi-apibinding manager provider
     */
    public static OWLOntologyManager createOWL() {
        return new OWLManagerProfile(Concurrency.NON_CONCURRENT).create();
    }

    /**
     * Creates a pure OWL-API ontology manager with default settings and locking for concurrent access.
     *
     * @return {@link OWLOntologyManager}
     * @throws OntApiException       if there is no owlapi-apibinding in class-path
     * @throws IllegalStateException if unable to find factories methods inside owlapi-apibinding manager provider
     */
    public static OWLOntologyManager createConcurrentOWL() {
        return new OWLManagerProfile(Concurrency.CONCURRENT).create();
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
     * Abstract Profile with {@link Concurrency}.
     */
    protected static abstract class BaseProfile {
        protected final Concurrency concurrency;

        private BaseProfile(Concurrency concurrent) {
            this.concurrency = Objects.requireNonNull(concurrent, "Null concurrency mode");
        }

        public boolean isConcurrent() {
            return Concurrency.CONCURRENT.equals(concurrency);
        }

        public Concurrency getConcurrency() {
            return concurrency;
        }
    }

    /**
     * The ONT-API impl of {@link Profile}
     */
    public static class ONTManagerProfile extends BaseProfile implements Profile<OntologyManager> {

        public ONTManagerProfile(Concurrency concurrent) {
            super(concurrent);
        }

        @Override
        public OntologyManager create() {
            Set<OWLStorerFactory> storers = OWLLangRegistry.storerFactories().collect(Collectors.toSet());
            Set<OWLParserFactory> parsers = OWLLangRegistry.parserFactories().collect(Collectors.toSet());
            ReadWriteLock lock = isConcurrent() ? new ReentrantReadWriteLock() : new NoOpReadWriteLock();
            OntologyManager res = new OntologyManagerImpl(new OWLDataFactoryImpl(), lock);
            res.setOntologyStorers(storers);
            res.setOntologyParsers(parsers);
            return res;
        }
    }

    /**
     * The OWL-API impl of {@link Profile}.
     * This implementation uses reflection, not injections.
     */
    public static class OWLManagerProfile extends BaseProfile implements Profile<OWLOntologyManager> {
        public static final String OWL_API_BINDING_MANAGERS_PROVIDER_CLASS = "org.semanticweb.owlapi.apibinding.OWLManager";
        private final Method manager, factory;

        public OWLManagerProfile(Concurrency concurrent) throws OntApiException, IllegalStateException {
            super(concurrent);
            Class<?> provider;
            try {
                provider = Class.forName(OWL_API_BINDING_MANAGERS_PROVIDER_CLASS);
            } catch (ClassNotFoundException e) {
                throw new OntApiException("No " + OWL_API_BINDING_MANAGERS_PROVIDER_CLASS + " in class-path. " +
                        "Please include owlapi-apibinding module to maven dependencies.", e);
            }
            this.manager = findStaticMethod(provider, isConcurrent() ? "createConcurrentOWLOntologyManager" : "createOWLOntologyManager");
            this.factory = findStaticMethod(provider, "getOWLDataFactory");
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
            try {
                return (OWLOntologyManager) manager.invoke(null);
            } catch (IllegalAccessException | InvocationTargetException | ClassCastException e) {
                throw new IllegalStateException("Can't create manager using " + manager, e);
            }
        }

        @Override
        public OWLDataFactory dataFactory() {
            try {
                return (OWLDataFactory) factory.invoke(null);
            } catch (IllegalAccessException | InvocationTargetException | ClassCastException e) {
                throw new IllegalStateException("Can't create data factory using " + factory, e);
            }
        }
    }

}