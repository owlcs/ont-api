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

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.semanticweb.owlapi.io.OWLParserFactory;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLStorerFactory;

import com.google.common.reflect.Reflection;

/**
 * The global OWL-API format syntax registry.
 * To control I/O behaviour with OWL-API formats.
 * Created by @szuev on 31.01.2018.
 *
 * @see OWLDocumentFormat
 * @see LangKey
 * @see OWLLang
 */
public class OWLLangRegistry {
    private static Map<String, OWLLang> registry = LangKey.asMap();

    /**
     * Registers a new OWLLang in global scope using format class-name as key.
     *
     * @param lang {@link OWLLang}, not null
     * @return {@link OWLLang lang-details}, the previous value or null.
     */
    public static OWLLang register(OWLLang lang) {
        return register(lang.getType().getName(), lang);
    }

    /**
     * Registers a new OWLLang in global scope with specified key.
     *
     * @param key  the key to inner map, not null
     * @param lang {@link OWLLang}, nullable
     * @return {@link OWLLang lang-details}, the previous value or null.
     */
    public static OWLLang register(String key, OWLLang lang) {
        return registry.put(key, lang);
    }

    /**
     * Removes global registration for specified owl-api-lang by string key (usually class-name of owl-format).
     *
     * @param key String, the key
     * @return the previous {@link OWLLang lang-details} associated with the key or null
     */
    public static OWLLang unregister(String key) {
        return registry.remove(key);
    }

    /**
     * Gets lang-details by lang-key.
     *
     * @param key String
     * @return Optional containing {@link OWLLang} can be empty.
     */
    public static Optional<OWLLang> getLang(String key) {
        return Optional.ofNullable(registry.get(key));
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
        return registry.values().stream().filter(Objects::nonNull).map(OWLLang::getStorerFactory).filter(Objects::nonNull);
    }

    /**
     * Returns all registered parser factories.
     *
     * @return Stream of {@link OWLParserFactory}s.
     */
    public static Stream<OWLParserFactory> parserFactories() {
        return registry.values().stream().filter(Objects::nonNull).map(OWLLang::getParserFactory).filter(Objects::nonNull);
    }

    /**
     * Constructs OWLLang container using storer and parser factories classes.
     * Each of arguments can be null but not at the same time.
     *
     * @param storerFactoryClass Class-type of {@link OWLStorerFactory}, nullable
     * @param parserFactoryClass Class-type of {@link OWLParserFactory}, nullable
     * @return the {@link OWLLang} object
     * @throws LangException if input is wrong and lang-details can not be constructed.
     */
    private static OWLLang create(Class<? extends OWLStorerFactory> storerFactoryClass,
                                  Class<? extends OWLParserFactory> parserFactoryClass,
                                  Supplier<OWLDocumentFormat> factory) throws LangException {
        OWLStorerFactory storer = null;
        OWLParserFactory parser = null;
        if (storerFactoryClass != null) {
            storer = newInstance(storerFactoryClass);
        }
        if (parserFactoryClass != null) {
            parser = newInstance(parserFactoryClass);
        }
        return create(storer, parser, factory);
    }

    /**
     * Constructs OWLLang container using storer and parser factories instances.
     * Each of arguments can be null but not at the same time.
     *
     * @param storer  {@link OWLStorerFactory}, nullable
     * @param parser  {@link OWLParserFactory}, nullable
     * @param factory {@link OWLDocumentFormat} provider, nullable
     * @return the {@link OWLLang} object
     * @throws LangException if input is wrong and lang-details can not be constructed.
     */
    private static OWLLang create(OWLStorerFactory storer, OWLParserFactory parser, Supplier<OWLDocumentFormat> factory) throws LangException {
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
        if (factory == null) {
            factory = makeFormatFactory(storer, parser);
        }
        return new OWLLang(format.getClass(), storer, parser, factory);
    }

    /**
     * Creates a {@link OWLLang lang} object.
     * A factory method to make new lang-details object based on specified storer and parser
     * Storer and parser can not be null at the same time.
     *
     * @param storer {@link OWLStorerFactory}, nullable
     * @param parser {@link OWLParserFactory}, nullable
     * @return the {@link OWLLang} object
     * @throws LangException if input is wrong and lang-details can not be constructed.
     */
    public static OWLLang create(OWLStorerFactory storer, OWLParserFactory parser) {
        return create(storer, parser, null);
    }

    /**
     * Creates a format factory from storer and parser.
     *
     * @param storer {@link OWLStorerFactory}
     * @param parser {@link OWLDocumentFormat}
     * @return Supplier which returns {@link OWLDocumentFormat}
     */
    private static Supplier<OWLDocumentFormat> makeFormatFactory(OWLStorerFactory storer, OWLParserFactory parser) {
        return storer != null ? () -> storer.getFormatFactory().createFormat() : parser != null ? () -> parser.getSupportedFormat().createFormat() : null;
    }

    private static String toClassName(Object object) {
        return object == null ? null : object.getClass().getName();
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
     * Creates an instance of specified class.
     *
     * @param type class
     * @param <T>  class-type wildcard
     * @return instance of {@code T}
     * @throws LangException in case no possible to create instance
     */
    private static <T> T newInstance(Class<T> type) throws LangException {
        try {
            return type.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new LangException("Can't create instance of " + type.getName(), e);
        }
    }

    /**
     * Overrides {@link OWLDocumentFormat#isTextual()} method to return false.
     * It is a reflection hack.
     *
     * @param implType      class-type of concrete class.
     * @param interfaceType class-type of interface
     * @return binary {@link OWLDocumentFormat}.
     */
    private static OWLDocumentFormat makeBinaryFormat(Class<? extends OWLDocumentFormat> implType, Class<? extends OWLDocumentFormat> interfaceType) {
        OWLDocumentFormat target = newInstance(implType);
        return Reflection.newProxy(interfaceType, (proxy, method, args) -> {
            if (method.getName().equals("isTextual")) return false;
            return method.invoke(target, args);
        });
    }

    /**
     * A set of constants reflecting OWL-API I/O subsystem.
     * Each constant contains string reference to {@link OWLDocumentFormat}, {@link OWLStorerFactory} and {@link OWLParserFactory} classes,
     * which are contained in different modules (owlapi-impl, owlapi-parsers, owlapi-rio, owlapi-oboformat).
     * This enum provides load information about all these things which assemblies during initialization.
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
             * This is a hack method.
             * OWL-API (owlapi-rio:5.1.4) contains a bug of
             * <a href='https://github.com/owlcs/owlapi/blob/version5/rio/src/main/java/org/semanticweb/owlapi/formats/BinaryRDFDocumentFormat.java'>
             * org.semanticweb.owlapi.formats.BinaryRDFDocumentFormat</a> implementation: it is marked as textual.
             * The snippet
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
             * To prevent this situation the method returns a patched format: the method {@link OWLDocumentFormat#isTextual()} returns false.
             *
             * @return {@link OWLDocumentFormat} provider.
             */
            @Override
            protected Supplier<OWLDocumentFormat> getDefaultFormatFactory() {
                return () -> {
                    String name = OWL_API_FORMATS_PACKAGE_NAME + ".RioRDFDocumentFormat";
                    Class<? extends OWLDocumentFormat> type = findClass(OWLDocumentFormat.class, name);
                    if (type == null) {
                        throw new LangException("Can't find " + name + " in class-path");
                    }
                    return makeBinaryFormat(getType(), type);
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
             * To match with OWL-API: skip KRSS.
             * By some unclear reason this format is not included in OWL-API supply configuration, maybe it is due to replacement by KRSS2.
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
         * Represents the content of this enum as a Map: format class name as key, {@link OWLLang} as value.
         *
         * @return LinkedHashMap, which may contain nulls as values.
         */
        public static Map<String, OWLLang> asMap() {
            Map<String, OWLLang> res = new LinkedHashMap<>();
            Arrays.stream(values()).forEach(key -> res.put(key.getKey(), key.createLang()));
            return res;
        }

        /**
         * Returns a {@link OWLDocumentFormat} class-name as string.
         *
         * @return String
         */
        public String getKey() {
            return OWL_API_FORMATS_PACKAGE_NAME + "." + format;
        }

        /**
         * Finds particular format class type by name.
         *
         * @return Class
         * @throws LangException in case no class found
         */
        protected Class<? extends OWLDocumentFormat> getType() throws LangException {
            Class<? extends OWLDocumentFormat> res = findClass(OWLDocumentFormat.class, getKey());
            if (res == null) throw new LangException(getKey() + " not found in system.");
            return res;
        }

        /**
         * Returns a {@link OWLStorerFactory} class-name as string.
         *
         * @return String
         */
        public String getStorerClassName() {
            return OWL_API_PACKAGE_NAME + "." + storer;
        }

        /**
         * Returns a {@link OWLParserFactory} class-name as string.
         *
         * @return String
         */
        public String getParserClassName() {
            return OWL_API_PACKAGE_NAME + "." + parser;
        }

        /**
         * Answers iff format can be used while owl-manager creation.
         *
         * @return boolean
         */
        public boolean enabled() {
            return true;
        }

        /**
         * Returns a custom factory to create format instance.
         * The result factory can throw {@link LangException} in case it is not possible to create format instance.
         *
         * @return a {@link OWLDocumentFormat}s provider or null to use storer or parser format factory.
         */
        protected Supplier<OWLDocumentFormat> getDefaultFormatFactory() {
            return null;
        }

        /**
         * Creates an owl-lang details object associated with this constant.
         *
         * @return {@link OWLLang} object or null in case format is not present in system dependencies or marked as disabled.
         */
        private OWLLang createLang() throws LangException {
            if (!enabled()) return null;
            Class<? extends OWLDocumentFormat> type = findClass(OWLDocumentFormat.class, getKey());
            if (type == null) return null;
            Class<? extends OWLStorerFactory> storerType = findClass(OWLStorerFactory.class, getStorerClassName());
            Class<? extends OWLParserFactory> parserType = findClass(OWLParserFactory.class, getParserClassName());
            if (storerType == null && parserType == null) {
                return new OWLLang(type, null, null, () -> newInstance(type));
            }
            OWLLang res = create(storerType, parserType, getDefaultFormatFactory());
            if (!type.equals(res.getType()))
                throw new LangException("Wrong format. Expected:" + type + ". Actual: " + res.getType() + ".");
            return res;
        }
    }

    /**
     * A container for OWL-Language details (owl-format).
     *
     * @see OWLStorerFactory
     * @see OWLParserFactory
     * @see OWLDocumentFormat
     * @see OWLLangRegistry#create(OWLStorerFactory, OWLParserFactory)
     */
    @SuppressWarnings("WeakerAccess")
    public static class OWLLang {
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
        protected OWLLang(Class<? extends OWLDocumentFormat> type, OWLStorerFactory storer, OWLParserFactory parser, Supplier<OWLDocumentFormat> factory) {
            this.format = Objects.requireNonNull(type, "Format type can not be null.");
            this.storer = storer;
            this.parser = parser;
            this.factory = factory;
        }

        @Override
        public String toString() {
            return format.getName() + "[" + (storer != null) + ", " + (parser != null) + "]";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof OWLLang)) return false;
            OWLLang other = (OWLLang) o;
            return Objects.equals(format, other.format);
        }

        @Override
        public int hashCode() {
            return format.hashCode();
        }

        public Class<? extends OWLDocumentFormat> getType() {
            return format;
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

        public boolean isWritable() {
            return storer != null;
        }

        public boolean isReadable() {
            return parser != null;
        }
    }

    /**
     * Ont-API exception which could be throw only inside this class.
     */
    @SuppressWarnings({"WeakerAccess", "unused"})
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

}
