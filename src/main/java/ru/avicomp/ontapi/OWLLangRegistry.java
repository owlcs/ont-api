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

import org.semanticweb.owlapi.formats.PrefixDocumentFormat;
import org.semanticweb.owlapi.io.OWLOntologyLoaderMetaData;
import org.semanticweb.owlapi.io.OWLParserFactory;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLDocumentFormatImpl;
import org.semanticweb.owlapi.model.OWLStorerFactory;

import java.io.Serializable;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * The global OWL-API lang registry.
 * To control I/O behaviour with OWL-API formats depending on maven-dependencies.
 * Created by @szuev on 31.01.2018.
 *
 * @see OWLDocumentFormat
 * @see LangKey
 * @see LangDetails
 */
public class OWLLangRegistry {
    private static Map<String, LangDetails> registry = LangKey.asMap();

    /**
     * Registers a new LangDetail in global scope using format class-name as key.
     *
     * @param lang {@link LangDetails}, not null
     * @return {@link LangDetails lang-details}, the previous value or null.
     */
    public static LangDetails register(LangDetails lang) {
        return register(lang.getName(), lang);
    }

    /**
     * Registers a new LangDetail in global scope with specified key.
     *
     * @param key  the key to inner map, not null
     * @param lang {@link LangDetails}, nullable
     * @return {@link LangDetails lang-details}, the previous value or null.
     */
    public static LangDetails register(String key, LangDetails lang) {
        return registry.put(key, lang);
    }

    /**
     * Removes global registration for specified owl-api-lang by string key (usually class-name of owl-format).
     *
     * @param key String, the key
     * @return the previous {@link LangDetails lang-details} associated with the key or null
     */
    public static LangDetails unregister(String key) {
        return registry.remove(key);
    }

    /**
     * Gets lang-details by key.
     *
     * @param key String
     * @return Optional containing {@link LangDetails} or null.
     */
    public static Optional<LangDetails> getLang(String key) {
        return Optional.ofNullable(registry.get(key));
    }

    /**
     * Creates a OWL-API format by lang-key
     *
     * @param key String key
     * @return {@link OWLDocumentFormat}, not null
     * @throws LangException if no possible to create format.
     */
    public static OWLDocumentFormat createFormat(String key) throws LangException {
        return getLang(key)
                .map(LangDetails::getFormatFactory)
                .map(Supplier::get)
                .orElseThrow(() -> new LangException(key + " is not registered"));
    }

    /**
     * Gets all registry keys.
     *
     * @return Stream of keys
     */
    public static Stream<String> keys() {
        return registry.keySet().stream();
    }

    /**
     * Returns all registered storer factories.
     *
     * @return Stream of {@link OWLStorerFactory}s.
     */
    public static Stream<OWLStorerFactory> storerFactories() {
        return registry.values().stream().filter(Objects::nonNull).map(LangDetails::getStorerFactory).filter(Objects::nonNull);
    }

    /**
     * Returns all registered parser factories.
     *
     * @return Stream of {@link OWLParserFactory}s.
     */
    public static Stream<OWLParserFactory> parserFactories() {
        return registry.values().stream().filter(Objects::nonNull).map(LangDetails::getParserFactory).filter(Objects::nonNull);
    }

    /**
     * Finds class by path and base type.
     *
     * @param baseType  the base type
     * @param className full class-name path
     * @param <T>       class-type
     * @return a Class or null in case nothing was found.
     * @throws LangException if wrong arguments
     */
    @SuppressWarnings("unchecked")
    private static <T> Class<? extends T> findClass(Class<T> baseType, String className) throws LangException {
        Class<? extends T> res = null;
        try {
            Class clazz = Class.forName(className);
            if (baseType.isAssignableFrom(clazz)) {
                res = (Class<? extends T>) clazz;
            } else {
                throw new LangException(className + " is not subtype of " + baseType.getName());
            }
        } catch (ClassNotFoundException e) {
            // ignore
        }
        return res;
    }

    /**
     * Constructs LangDetails container using storer and parser factories classes.
     * Each of arguments can be null but not at the same time.
     *
     * @param storerFactoryClass Class-type of {@link OWLStorerFactory}, nullable
     * @param parserFactoryClass Class-type of {@link OWLParserFactory}, nullable
     * @return the {@link LangDetails} object
     * @throws LangException if input is wrong and lang-details can not be constructed.
     */
    public static LangDetails create(Class<? extends OWLStorerFactory> storerFactoryClass,
                                     Class<? extends OWLParserFactory> parserFactoryClass) throws LangException {
        OWLStorerFactory storer = null;
        OWLParserFactory parser = null;
        if (storerFactoryClass != null) {
            try {
                storer = storerFactoryClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new LangException("Wrong storer type " + storerFactoryClass.getName(), e);
            }
        }
        if (parserFactoryClass != null) {
            try {
                parser = parserFactoryClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new LangException("Wrong parser type " + parserFactoryClass.getName(), e);
            }
        }
        return create(storer, parser);
    }

    /**
     * Constructs LangDetails container using storer and parser factories instances.
     * Each of arguments can be null but not at the same time.
     *
     * @param storer {@link OWLStorerFactory}, nullable
     * @param parser {@link OWLParserFactory}, nullable
     * @return the {@link LangDetails} object
     * @throws LangException if input is wrong and lang-details can not be constructed.
     */
    public static LangDetails create(OWLStorerFactory storer, OWLParserFactory parser) throws LangException {
        OWLDocumentFormat format = null;
        if (storer != null) {
            format = storer.getFormatFactory().createFormat();
        }
        if (parser != null) {
            OWLDocumentFormat parserFormat = parser.getSupportedFormat().createFormat();
            if (format == null) {
                format = parserFormat;
            } else if (!format.getClass().equals(parserFormat.getClass())) {
                throw new LangException(String.format("Storer/Parser formats do not match. Storer: %s. Parser: %s.", toClassName(storer), toClassName(parser)));
            }
        }
        if (format == null) {
            throw new LangException(String.format("Unable to determine format. Storer: %s. Parser: %s.", toClassName(storer), toClassName(parser)));
        }
        return new LangDetails(format.getClass(), storer, parser);
    }

    private static String toClassName(Object object) {
        return object == null ? null : object.getClass().getName();
    }

    private static OWLDocumentFormat newFormatInstance(Class<? extends OWLDocumentFormat> type) throws LangException {
        try {
            return type.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new LangException("Can't create " + type.getName(), e);
        }
    }

    private static LangDetails createRegistryDetails(String storerFactoryTypeName,
                                                     String parserFactoryTypeName,
                                                     Class<? extends OWLDocumentFormat> formatType,
                                                     Supplier<OWLDocumentFormat> factory) throws LangException {
        LangDetails res;
        Class<? extends OWLStorerFactory> storerType = storerFactoryTypeName == null ? null : findClass(OWLStorerFactory.class, storerFactoryTypeName);
        Class<? extends OWLParserFactory> parserType = parserFactoryTypeName == null ? null : findClass(OWLParserFactory.class, parserFactoryTypeName);
        if (storerType == null && parserType == null) {
            res = new LangDetails(formatType, factory);
        } else {
            res = create(storerType, parserType);
            if (!formatType.equals(res.getType()))
                throw new LangException("Wrong format. Expected:" + formatType + ". Actual: " + res.getType() + ".");
        }
        return res;
    }

    /**
     * A set of constants reflecting OWL-API I/O subsystem.
     * Each constant contains string reference to {@link OWLDocumentFormat}, {@link OWLStorerFactory} and {@link OWLParserFactory},
     * The corresponding classes are contained in different modules (owlapi-impl, owlapi-parsers, owlapi-rio, owlapi-oboformat).
     * This enum provides load information about all these things.
     */
    public enum LangKey {
        RIORDFXML("RioRDFXMLDocumentFormat", "rio.RioRDFXMLStorerFactory", "rio.RioRDFXMLParserFactory"),
        TRIX("TrixDocumentFormat", "rio.RioTrixStorerFactory", "rio.RioTrixParserFactory"),
        NQUADS("NQuadsDocumentFormat", "rio.RioNQuadsStorerFactory", "rio.RioNQuadsParserFactory"),
        NTRIPLES("NTriplesDocumentFormat", "rio.RioNTriplesStorerFactory", "rio.RioNTriplesParserFactory"),
        TRIG("TrigDocumentFormat", "rio.RioTrigStorerFactory", "rio.RioTrigParserFactory"),
        RDFJSON("RDFJsonDocumentFormat", "rio.RioJsonStorerFactory", "rio.RioJsonParserFactory"),
        BINARYRDF("BinaryRDFDocumentFormat", "rio.RioBinaryRdfStorerFactory", "rio.RioBinaryRdfParserFactory") {
            /**
             * A hack method.
             * OWL-API (owlapi-rio:5.1.4) contains a bug of {@link org.semanticweb.owlapi.formats.BinaryRDFDocumentFormat BinaryRDFDocumentFormat}
             * implementation: it is marked as textual.
             * Tge snippet
             * <pre>{@code
             *  OWLOntology o = org.semanticweb.owlapi.apibinding.OWLManager.createOWLOntologyManager().createOntology(IRI.create("empty"));
             *  o.saveOntology(new BinaryRDFDocumentFormat(), new StreamDocumentTarget(System.out));
             * }</pre>
             * leads to exception:
             * <pre>{@code Exception in thread "main" java.lang.UnsupportedOperationException
             * at org.eclipse.rdf4j.rio.binary.BinaryRDFWriterFactory.getWriter(BinaryRDFWriterFactory.java:42)
             * at org.eclipse.rdf4j.rio.Rio.createWriter(Rio.java:154)
             * at org.semanticweb.owlapi.rio.RioStorer.getRDFHandlerForWriter(RioStorer.java:159)
             * at org.semanticweb.owlapi.rio.RioStorer.storeOntology(RioStorer.java:229)
             * at org.semanticweb.owlapi.util.AbstractOWLStorer.storeOntology(AbstractOWLStorer.java:106)
             * at uk.ac.manchester.cs.owl.owlapi.OWLOntologyManagerImpl.saveOntology(OWLOntologyManagerImpl.java:1339)
             * }</pre>
             *
             * @return {@link OWLDocumentFormat} provider.
             */
            @Override
            protected Supplier<OWLDocumentFormat> formatFactory() {
                return () -> new FormatWrapper(super.formatFactory().get()) {

                    @Override
                    public boolean isTextual() {
                        return false;
                    }
                };
            }
        },
        RIOTURTLE("RioTurtleDocumentFormat", "rio.RioTurtleStorerFactory", "rio.RioTurtleParserFactory"),
        RDFJSONLD("RDFJsonLDDocumentFormat", "rio.RioJsonLDStorerFactory", "rio.RioJsonLDParserFactory"),
        N3("N3DocumentFormat", "rio.RioN3StorerFactory", "rio.RioN3ParserFactory"),
        RDFXML("RDFXMLDocumentFormat", "rdf.rdfxml.renderer.RDFXMLStorerFactory", "rdf.rdfxml.parser.RDFXMLParserFactory"),
        DLSYNTAXHTML("DLSyntaxHTMLDocumentFormat", "dlsyntax.renderer.DLSyntaxHTMLStorerFactory", null),
        OWLXML("OWLXMLDocumentFormat", "owlxml.renderer.OWLXMLStorerFactory", "owlxml.parser.OWLXMLParserFactory"),
        MANCHESTERSYNTAX("ManchesterSyntaxDocumentFormat", "manchestersyntax.renderer.ManchesterSyntaxStorerFactory", "manchestersyntax.parser.ManchesterOWLSyntaxOntologyParserFactory"),
        FUNCTIONALSYNTAX("FunctionalSyntaxDocumentFormat", "functional.renderer.FunctionalSyntaxStorerFactory", "functional.parser.OWLFunctionalSyntaxOWLParserFactory"),
        DLSYNTAX("DLSyntaxDocumentFormat", "dlsyntax.renderer.DLSyntaxStorerFactory", "dlsyntax.parser.DLSyntaxOWLParserFactory"),
        TURTLE("TurtleDocumentFormat", "rdf.turtle.renderer.TurtleStorerFactory", "rdf.turtle.parser.TurtleOntologyParserFactory"),
        LATEX("LatexDocumentFormat", "latex.renderer.LatexStorerFactory", null),
        OBO("OBODocumentFormat", "oboformat.OBOFormatStorerFactory", "oboformat.OBOFormatOWLAPIParserFactory"),
        KRSS2("KRSS2DocumentFormat", "krss2.renderer.KRSS2OWLSyntaxStorerFactory", "krss2.parser.KRSS2OWLParserFactory"),
        KRSS("KRSSDocumentFormat", "krss2.renderer.KRSSSyntaxStorerFactory", "krss1.parser.KRSSOWLParserFactory") {
            /**
             * By some unclear reason this format is not included in OWL-API supply, maybe it is due to replacement by KRSS2.
             * Of course we can implement properties control for this case, but right now it seems to be excess.
             * @return false
             */
            public boolean enabled() {
                return false;
            }
        },
        RDFA("RDFaDocumentFormat", null, "rio.RioRDFaParserFactory"),;

        private static final String OWL_API_PACKAGE_NAME = "org.semanticweb.owlapi";
        private static final String OWL_API_FORMATS_PACKAGE_NAME = OWL_API_PACKAGE_NAME + ".formats";

        private final String format, storer, parser;

        LangKey(String format, String storer, String parser) {
            this.format = format;
            this.storer = storer;
            this.parser = parser;
        }

        /**
         * Represents the content of this enum as a Map: format class names as keys, {@link LangDetails} as values.
         *
         * @return LinkedHashMap
         */
        public static Map<String, LangDetails> asMap() {
            Map<String, LangDetails> res = new LinkedHashMap<>();
            Arrays.stream(values()).forEach(key -> res.put(key.getKey(), key.getDetails()));
            return res;
        }

        /**
         * Returns a {@link OWLDocumentFormat} class name as string.
         *
         * @return String
         */
        public String getKey() {
            return OWL_API_FORMATS_PACKAGE_NAME + "." + format;
        }

        private Class<? extends OWLDocumentFormat> getType() throws LangException {
            Class<? extends OWLDocumentFormat> res = findClass(OWLDocumentFormat.class, getKey());
            if (res == null) throw new LangException(getKey() + " not found in system.");
            return res;
        }

        /**
         * Returns a {@link OWLStorerFactory} class name as string.
         *
         * @return String
         */
        public String getStorerClassName() {
            return OWL_API_PACKAGE_NAME + "." + storer;
        }

        /**
         * Returns a {@link OWLParserFactory} class name as string.
         *
         * @return String
         */
        public String getParserClassName() {
            return OWL_API_PACKAGE_NAME + "." + parser;
        }

        /**
         * Answers iff format can be used while manager's creation.
         *
         * @return boolean
         */
        public boolean enabled() {
            return true;
        }

        /**
         * Returns a factory to create format instance.
         * By default using reflection.
         * The result factory can throw {@link LangException} in case it is not possible to create format instance.
         *
         * @return a {@link OWLDocumentFormat}s provider.
         */
        protected Supplier<OWLDocumentFormat> formatFactory() {
            return () -> newFormatInstance(getType());
        }

        /**
         * Gets details for this constant containing storer and parser.
         *
         * @return {@link LangDetails} object or null in case format is not present in system dependencies or disabled.
         */
        public LangDetails getDetails() {
            if (!enabled()) return null;
            Class<? extends OWLDocumentFormat> type = findClass(OWLDocumentFormat.class, getKey());
            if (type == null) return null;
            return createRegistryDetails(getStorerClassName(), getParserClassName(), type, formatFactory());
        }
    }

    /**
     * A container for OWL-Language details (owl-format).
     *
     * @see OWLStorerFactory
     * @see OWLParserFactory
     * @see OWLDocumentFormat
     * @see OWLLangRegistry#create(OWLStorerFactory, OWLParserFactory)
     * @see OWLLangRegistry#create(Class, Class)
     */
    public static class LangDetails {
        private final Class<? extends OWLDocumentFormat> format;
        private final OWLStorerFactory storer;
        private final OWLParserFactory parser;
        private final Supplier<OWLDocumentFormat> factory;

        /**
         * Main constructor.
         *
         * @param type    Class-type of {@link OWLDocumentFormat}, can not be null
         * @param storer  {@link OWLStorerFactory} the storer factory, nullable
         * @param parser  {@link OWLParserFactory} the parser factory, nullable
         * @param factory the {@link OWLDocumentFormat} provider, nullable
         */
        protected LangDetails(Class<? extends OWLDocumentFormat> type, OWLStorerFactory storer, OWLParserFactory parser, Supplier<OWLDocumentFormat> factory) {
            this.format = Objects.requireNonNull(type, "Format type can not be null.");
            this.storer = storer;
            this.parser = parser;
            this.factory = factory;
        }

        public LangDetails(Class<? extends OWLDocumentFormat> type, OWLStorerFactory storer, OWLParserFactory parser) {
            this(type, storer, parser,
                    storer != null ? () -> storer.getFormatFactory().createFormat() : parser != null ? () -> parser.getSupportedFormat().createFormat() : null);
        }

        public LangDetails(Class<? extends OWLDocumentFormat> type, Supplier<OWLDocumentFormat> factory) {
            this(type, null, null, factory);
        }

        @Override
        public String toString() {
            return format.getName() + "[" + (storer != null) + ", " + (parser != null) + "]";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof LangDetails)) return false;
            LangDetails other = (LangDetails) o;
            return Objects.equals(format, other.format);
        }

        @Override
        public int hashCode() {
            return format.hashCode();
        }

        public Class<? extends OWLDocumentFormat> getType() {
            return format;
        }

        public String getName() {
            return toClassName(format);
        }

        public OWLStorerFactory getStorerFactory() {
            return storer;
        }

        public OWLParserFactory getParserFactory() {
            return parser;
        }

        public Supplier<OWLDocumentFormat> getFormatFactory() {
            return factory;
        }
    }

    /**
     * Ont-API exception which could be throw only by this class
     */
    public static class LangException extends OntApiException {
        public LangException() {
        }

        public LangException(String message) {
            super(message);
        }

        public LangException(String message, Throwable cause) {
            super(message, cause);
        }

        public LangException(Throwable cause) {
            super(cause);
        }

    }

    /**
     * A typical wrapper for {@link OWLDocumentFormat}
     */
    @SuppressWarnings("NullableProblems")
    public static class FormatWrapper extends OWLDocumentFormatImpl {
        private final OWLDocumentFormat delegate;

        public FormatWrapper(OWLDocumentFormat format) {
            this.delegate = Objects.requireNonNull(format, "Null format.");
        }

        @Override
        public boolean isAddMissingTypes() {
            return delegate.isAddMissingTypes();
        }

        @Override
        public void setAddMissingTypes(boolean addMissingTypes) {
            delegate.setAddMissingTypes(addMissingTypes);
        }

        @Override
        public void setParameter(Serializable key, Serializable value) {
            delegate.setParameter(key, value);
        }

        @Override
        public <T> T getParameter(Serializable key, T defaultValue) {
            return delegate.getParameter(key, defaultValue);
        }

        @Override
        public boolean isPrefixOWLDocumentFormat() {
            return delegate.isPrefixOWLDocumentFormat();
        }

        @Override
        public PrefixDocumentFormat asPrefixOWLDocumentFormat() {
            return delegate.asPrefixOWLDocumentFormat();
        }

        @Override
        public Optional<OWLOntologyLoaderMetaData> getOntologyLoaderMetaData() {
            return delegate.getOntologyLoaderMetaData();
        }

        @Override
        public void setOntologyLoaderMetaData(OWLOntologyLoaderMetaData loaderMetaData) {
            delegate.setOntologyLoaderMetaData(loaderMetaData);
        }

        @Override
        public String getKey() {
            return delegate.getKey();
        }

        @Override
        public boolean isTextual() {
            return delegate.isTextual();
        }
    }

}
