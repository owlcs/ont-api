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
package ru.avicomp.owlapi;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.mansyntax.ManchesterOWLSyntaxParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avicomp.ontapi.OntManagers;
import ru.avicomp.ontapi.OntologyFactoryImpl;
import ru.avicomp.ontapi.config.OntConfig;

import java.util.concurrent.locks.ReadWriteLock;

/**
 * The access point to ONT-API or OWL-API parts in tests.
 *
 * @see <a href='https://github.com/owlcs/owlapi/blob/version5/apibinding/src/main/java/org/semanticweb/owlapi/apibinding/OWLManager.java'>org.semanticweb.owlapi.apibinding.OWLManager</a>
 * @see OntManagers
 * @since 1.1.0
 */
public class OWLManager {

    // specify VM option '-Ddebug.use.owl=true' to run "pure" OWL-tests:
    public static final boolean DEBUG_USE_OWL = Boolean.parseBoolean(System.getProperty("debug.use.owl", Boolean.FALSE.toString()));

    private static final Logger LOGGER = LoggerFactory.getLogger(OWLManager.class);

    /**
     * Creates an OWL ontology manager that is configured with standard parsers,
     * storeres etc.
     *
     * @return The new manager.
     */
    public static OWLOntologyManager createOWLOntologyManager() {
        LOGGER.debug("Create common {}", typeName("OntologyManager"));
        return DEBUG_USE_OWL ? OntManagers.createOWL() : OntManagers.createONT();
    }

    /**
     * Creates an OWL ontology manager that is configured with the standard
     * parsers and storers and provides locking for concurrent access.
     *
     * @return The new manager.
     */
    public static OWLOntologyManager createConcurrentOWLOntologyManager() {
        LOGGER.debug("Create concurrent {}", typeName("OntologyManager"));
        return DEBUG_USE_OWL ? OntManagers.createConcurrentOWL() : OntManagers.createConcurrentONT();
    }


    /**
     * Gets a global data factory that can be used to create OWL API objects.
     *
     * @return An OWLDataFactory that can be used for creating OWL API objects.
     */
    public static OWLDataFactory getOWLDataFactory() {
        return OntManagers.getDataFactory();
    }

    /**
     * Creates new manager instance without any settings inside
     *
     * @param df   {@link OWLDataFactory}
     * @param lock {@link ReadWriteLock}
     * @return {@link OWLOntologyManager}
     */
    public static OWLOntologyManager newManager(OWLDataFactory df, ReadWriteLock lock) {
        LOGGER.debug("New {}", typeName("OntologyManager"));
        return DEBUG_USE_OWL ?
                new uk.ac.manchester.cs.owl.owlapi.OWLOntologyManagerImpl(df, lock) :
                new ru.avicomp.ontapi.OntologyManagerImpl(df, lock);
    }

    private static String typeName(String base) {
        return String.format("[%s]%s", DEBUG_USE_OWL ? "OWL" : "ONT", base);
    }

    public static OWLDataFactory newOWLDataFactory(boolean withCompression) {
        LOGGER.debug("New {}", typeName("DataFactory"));
        return DEBUG_USE_OWL ?
                new uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl(withCompression) :
                new ru.avicomp.owlapi.OWLDataFactoryImpl();
    }

    public static OWLOntologyFactory newOWLLoadFactory(OWLOntologyBuilder builder) {
        LOGGER.debug("New {}", typeName("LoadFactory"));
        return DEBUG_USE_OWL ?
                new uk.ac.manchester.cs.owl.owlapi.OWLOntologyFactoryImpl(builder) :
                new ru.avicomp.owlapi.OWLOntologyFactoryImpl(builder);
    }

    public static OntologyConfigurator newConfig() {
        LOGGER.debug("New {}", typeName("OntologyConfigurator"));
        return DEBUG_USE_OWL ?
                new OntologyConfigurator() :
                new OntConfig();
    }

    /**
     * From owlapi-parsers
     *
     * @return an initialized manchester syntax parser for parsing strings
     */
    public static ManchesterOWLSyntaxParser createManchesterParser() {
        return new org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxParserImpl(newConfig(), getOWLDataFactory());
    }

    /**
     * Creates a builder.
     *
     * @return {@link OWLOntologyBuilder} the builder
     */
    public static OWLOntologyBuilder createOntologyBuilder() {
        return OWLManager.DEBUG_USE_OWL ?
                (OWLOntologyBuilder) uk.ac.manchester.cs.owl.owlapi.OWLOntologyImpl::new :
                new OntologyFactoryImpl.ONTBuilderImpl();
    }

}