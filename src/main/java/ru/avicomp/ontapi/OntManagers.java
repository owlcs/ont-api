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

import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.jena.system.JenaSystem;
import org.semanticweb.owlapi.OWLAPIParsersModule;
import org.semanticweb.owlapi.OWLAPIServiceLoaderModule;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLParserFactory;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyManagerFactory;
import org.semanticweb.owlapi.model.OWLStorerFactory;

import com.google.common.collect.Sets;
import com.google.inject.Guice;
import com.google.inject.Injector;
import uk.ac.manchester.cs.owl.owlapi.OWLAPIImplModule;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;
import uk.ac.manchester.cs.owl.owlapi.concurrent.Concurrency;
import uk.ac.manchester.cs.owl.owlapi.concurrent.NoOpReadWriteLock;

/**
 * The main (static) access point to {@link OWLOntologyManager} instances.
 * The analogue of {@link org.semanticweb.owlapi.apibinding.OWLManager}
 * <p>
 * Created by @szuev on 27.09.2016.
 */
@SuppressWarnings("WeakerAccess")
public class OntManagers implements OWLOntologyManagerFactory {

    static { // init jena system. see description in ru.avicomp.ontapi.jena.OntModelFactory
        JenaSystem.init();
    }

    public static final ONTManagerProfile DEFAULT_PROFILE = new ONTManagerProfile(Concurrency.NON_CONCURRENT);
    public static final OWLDataFactory OWL_DATA_FACTORY = new OWLDataFactoryImpl();

    private static Profile<? extends OWLOntologyManager> profile = DEFAULT_PROFILE;

    /**
     * @return {@link OWLDataFactory} impl
     * @see OWLManager#getOWLDataFactory()
     */
    public static OWLDataFactory getDataFactory() {
        return OWL_DATA_FACTORY;
    }

    public static OntologyManager createONT() {
        return DEFAULT_PROFILE.create();
    }

    public static OntologyManager createConcurrentONT() {
        return new ONTManagerProfile(Concurrency.CONCURRENT).create();
    }

    public static OWLOntologyManager createOWL() {
        return new OWLManagerProfile(Concurrency.NON_CONCURRENT).create();
    }

    public static OWLOntologyManager createConcurrentOWL() {
        return new OWLManagerProfile(Concurrency.CONCURRENT).create();
    }

    public static Profile<? extends OWLOntologyManager> getProfile() {
        return profile;
    }

    public static void setProfile(Profile<? extends OWLOntologyManager> p) {
        profile = OntApiException.notNull(p, "Null manager profile specified.");
    }

    @Override
    public OWLOntologyManager get() {
        return profile.create();
    }

    public interface Profile<M extends OWLOntologyManager> {
        M create();
    }

    private static abstract class BaseProfile {
        protected final Concurrency concurrency;

        private BaseProfile(Concurrency concurrent) {
            this.concurrency = concurrent;
        }

        public Concurrency getConcurrency() {
            return concurrency;
        }
    }

    /**
     * The ONT-API impl of {@link Profile}
     */
    public static class ONTManagerProfile extends BaseProfile implements Profile<OntologyManager> {
        private static final OWLOntologyManager FACTORIES = new OWLManagerProfile(Concurrency.NON_CONCURRENT).create();

        public ONTManagerProfile(Concurrency concurrent) {
            super(concurrent);
        }

        @Override
        public OntologyManager create() {
            // todo: passing OWL-factories created by Injector is a temporary solution.
            // (and actually we don' need them to work with ONT-API if we don't use OWL-specific formats to load or save ontologies)
            Set<OWLStorerFactory> storers = Sets.newHashSet(FACTORIES.getOntologyStorers());
            Set<OWLParserFactory> parsers = Sets.newHashSet(FACTORIES.getOntologyParsers());
            ReadWriteLock lock = Concurrency.CONCURRENT.equals(concurrency) ? new ReentrantReadWriteLock() : new NoOpReadWriteLock();
            OntologyManager res = new OntologyManagerImpl(new OWLDataFactoryImpl(), lock);
            res.setOntologyStorers(storers);
            res.setOntologyParsers(parsers);
            return res;
        }
    }

    /**
     * the OWL-API impl of {@link Profile}
     */
    public static class OWLManagerProfile extends BaseProfile implements Profile<OWLOntologyManager> {
        public OWLManagerProfile(Concurrency concurrent) {
            super(concurrent);
        }

        public OWLDataFactory getOWLDataFactory() {
            return createInjector().getInstance(OWLDataFactory.class);
        }

        @Override
        public OWLOntologyManager create() {
            Injector injector = createInjector();
            OWLOntologyManager instance = injector.getInstance(OWLOntologyManager.class);
            injector.injectMembers(instance);
            return instance;
        }

        public Injector createInjector() {
            return Guice.createInjector(new OWLAPIImplModule(concurrency), new OWLAPIParsersModule(),
                    new OWLAPIServiceLoaderModule());
        }
    }

}