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

package ru.avicomp.ontapi.jena;

import org.apache.jena.shared.JenaException;

/**
 * A base jena exception that is used inside ONT RDF Model subsystem (i.e. inside package {@link ru.avicomp.ontapi.jena}).
 * <p>
 * Created by @szuev on 24.11.2016.
 */
@SuppressWarnings({"unused"})
public class OntJenaException extends JenaException {

    public OntJenaException() {
        super();
    }

    public OntJenaException(String message) {
        super(message);
    }

    public OntJenaException(Throwable cause) {
        super(cause);
    }

    public OntJenaException(String message, Throwable cause) {
        super(message, cause);
    }

    public static <T> T notNull(T obj, String message) {
        if (obj == null)
            throw message == null ? new IllegalArgument() : new IllegalArgument(message);
        return obj;
    }

    /**
     * This is an analogue of {@link org.apache.jena.ontology.ConversionException},
     * used inside top level api ({@link ru.avicomp.ontapi.jena.model.OntGraphModel}
     * and (maybe) inside {@link ru.avicomp.ontapi.jena.model.OntObject}) implementation.
     * In the personality level a standard jena exception (ConversionException) should be used.
     */
    public static class Conversion extends OntJenaException {
        public Conversion(String message, Throwable cause) {
            super(message, cause);
        }

        public Conversion(String message) {
            super(message);
        }
    }

    /**
     * Exception, which may happen while creation of ont-object.
     */
    public static class Creation extends OntJenaException {
        public Creation(String message, Throwable cause) {
            super(message, cause);
        }

        public Creation(String message) {
            super(message);
        }
    }

    /**
     * An exception to indicate that a feature is not supported right now or by design.
     */
    public static class Unsupported extends OntJenaException {
        public Unsupported() {
            super();
        }

        public Unsupported(String message) {
            super(message);
        }
    }

    /**
     * An exception that is thrown if a recursion is found in the graph.
     * Example of such graph recursion:
     * <pre>{@code  _:b0 a owl:Class ; owl:complementOf  _:b0 .}</pre>
     */
    public static class Recursion extends OntJenaException {

        public Recursion(String message) {
            super(message);
        }
    }

    /**
     * A Jena exception that indicates wrong input.
     */
    public static class IllegalArgument extends OntJenaException {
        public IllegalArgument() {
        }

        public IllegalArgument(String message) {
            super(message);
        }

        public IllegalArgument(String message, Throwable cause) {
            super(message, cause);
        }

    }

    /**
     * A Jena exception that indicates that Jena-object state is broken,
     * which may happen in multithreading or in other uncommon situations.
     */
    public static class IllegalState extends OntJenaException {

        public IllegalState() {
        }

        public IllegalState(String message) {
            super(message);
        }

        public IllegalState(String message, Throwable cause) {
            super(message, cause);
        }

    }
}
