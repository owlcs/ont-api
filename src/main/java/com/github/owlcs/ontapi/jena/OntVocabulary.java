/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2020, The University of Manchester, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.jena;

import com.github.owlcs.ontapi.jena.utils.Iter;
import com.github.owlcs.ontapi.jena.vocabulary.*;
import org.apache.jena.datatypes.BaseDatatype;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An Ontology Vocabulary.
 * This is a generic interface that simply maps an {@code uri}-key to a {@code Set} of
 * {@link Resource RDF Resources} or {@link Property RDF Properties},
 * that are defined in vocabulary schemas and represent some family determined by that key.
 * A schema is an java class containing public static final constants.
 * Schemas are usually located inside the packages
 * {@link com.github.owlcs.ontapi.jena.vocabulary} and {@link org.apache.jena.vocabulary}.
 * There are two kind of property/resources described by this vocabulary: system and builtin.
 * A system resource/property is simply an URI defined in any scheme.
 * A builtin resource/property is a URI with known type, that does not require explicit declaration.
 * Note that all methods of this interface return unmodifiable {@code Set}s.
 * <p>
 * Created by @szuev on 04.04.2017.
 *
 * @see Factory
 */
@SuppressWarnings("unused")
public interface OntVocabulary {

    /**
     * Answers a {@code Set} of system/builtin {@link Resource}s for the specified URI-{@code key}.
     * An URI-{@code key} - is a schema URI that determines a family of desired resources.
     * For example to get all resources a key {@link RDFS#Resource rdfs:Resource} should be used,
     * because it is a supertype of everything.
     *
     * @param key String, not {@code null}
     * @return a {@code Set} of {@link Resource}s (possibly empty)
     */
    Set<? extends Resource> get(String key);

    /**
     * Answers a {@code Set} of system/builtin {@link Resource}s for the specified URI-key.
     *
     * @param uri a URI-{@link Resource}, not {@code null}
     * @param <X> either {@link Resource} or {@link Property}
     * @return a {@code Set} of {@link X}s, not {@code null} but possibly empty
     */
    @SuppressWarnings("unchecked")
    default <X extends Resource> Set<X> get(Resource uri) {
        return (Set<X>) get(uri.getURI());
    }

    /**
     * Returns a collection of all built-in properties
     * with implicit {@code rdf:type} equal to {@link OWL#AnnotationProperty owl:AnnotationProperty}.
     *
     * @return {@code Set} of {@link Property Properties}
     */
    default Set<Property> getBuiltinAnnotationProperties() {
        return get(OWL.AnnotationProperty);
    }

    /**
     * Returns a collection of all built-in properties
     * with implicit {@code rdf:type} equal to {@link OWL#DatatypeProperty owl:DatatypeProperty}.
     *
     * @return {@code Set} of {@link Property Properties}
     */
    default Set<Property> getBuiltinDatatypeProperties() {
        return get(OWL.DatatypeProperty);
    }

    /**
     * Returns a collection of all built-in properties
     * with implicit {@code rdf:type} equal to {@link OWL#ObjectProperty owl:ObjectProperty}.
     *
     * @return {@code Set} of {@link Property Properties}
     */
    default Set<Property> getBuiltinObjectProperties() {
        return get(OWL.ObjectProperty);
    }

    /**
     * Returns a collection of all built-in uri-resources
     * with implicit {@code rdf:type} equal to {@link RDFS#Datatype rdfs:Datatype}.
     *
     * @return {@code Set} of {@link Resource Resources}
     */
    default Set<Resource> getBuiltinDatatypes() {
        return get(RDFS.Datatype);
    }

    /**
     * Returns a collection of all built-in uri resources
     * with implicit {@code rdf:type} equal to {@link OWL#Class owl:Class}.
     *
     * @return {@code Set} of {@link Resource Resources}
     */
    default Set<Resource> getBuiltinClasses() {
        return get(OWL.Class);
    }

    /**
     * Returns a collection of all built-in uri resources
     * with implicit {@code rdf:type} equal to {@link SWRL#Builtin swrl:Builtin}.
     *
     * @return {@code Set} of {@link Resource Resources}
     */
    default Set<Resource> getBuiltinSWRLs() {
        return get(SWRL.Builtin);
    }

    /**
     * Returns all reserved resources:
     * OWL entities can not have an uri belonging to the return collection.
     *
     * @return {@code Set} of {@link Resource Resources}
     */
    default Set<Resource> getSystemResources() {
        return get(RDFS.Resource);
    }

    /**
     * Returns all reserved properties:
     * OWL2 ontology can not contain assertion with predicate belonging to the return collection.
     *
     * @return {@code Set} of {@link Property Properties}
     */
    default Set<Property> getSystemProperties() {
        return get(RDF.Property);
    }

    /**
     * Returns a {@code Set} of all {@link Resource}s.
     *
     * @return {@code Set} of {@link Resource Resources}
     */
    default Set<Resource> getSystemALL() {
        return Stream.of(getSystemProperties(), getSystemResources())
                .flatMap(Collection::stream)
                .collect(Iter.toUnmodifiableSet());
    }

    /**
     * Answers a {@code Set} containing all builtin properties (annotation, object or datatype).
     *
     * @return {@code Set} of {@link Property Properties}
     */
    default Set<Property> getBuiltinOWLProperties() {
        return Stream.of(getBuiltinAnnotationProperties(), getBuiltinDatatypeProperties(), getBuiltinObjectProperties())
                .flatMap(Collection::stream)
                .collect(Iter.toUnmodifiableSet());
    }

    /**
     * Answers a {@code Set} containing all builtin entity resources.
     *
     * @return {@code Set} of {@link Resource Resources}
     */
    default Set<Resource> getBuiltinOWLEntities() {
        return Stream.of(getBuiltinClasses(), getBuiltinDatatypes(), getBuiltinOWLProperties())
                .flatMap(Collection::stream)
                .collect(Iter.toUnmodifiableSet());
    }

    /**
     * A factory-helper to work with {@link OntVocabulary} instances, that wrap constant-holders
     * from the packages {@link com.github.owlcs.ontapi.jena.vocabulary}
     * and {@link org.apache.jena.vocabulary} (such as {@link OWL}).
     * <p>
     * In ONT-API, a {@link OntVocabulary} singleton is used
     * to build {@link com.github.owlcs.ontapi.jena.impl.conf.OntPersonality}
     * and, also, in {@link com.github.owlcs.ontapi.transforms} subsystem.
     * <p>
     * Created by @szuev on 21.12.2016.
     */
    class Factory {

        public static final OntVocabulary DUMMY = new Impl(Collections.emptyMap());
        public static final OntVocabulary OWL_VOCABULARY = new OWLImpl();
        public static final OntVocabulary DC_VOCABULARY = new DCImpl();
        public static final OntVocabulary SKOS_VOCABULARY = new SKOSImpl();
        public static final OntVocabulary SWRL_VOCABULARY = new SWRLImpl();
        public static final OntVocabulary DEFAULT_VOCABULARY = create(OWL_VOCABULARY, DC_VOCABULARY, SKOS_VOCABULARY, SWRL_VOCABULARY);

        /**
         * The default instance includes OWL, SWRL, DC, SKOS values.
         */
        protected static OntVocabulary def = DEFAULT_VOCABULARY;

        /**
         * Gets a system-wide vocabulary.
         *
         * @return {@link OntVocabulary}
         */
        public static OntVocabulary get() {
            return def;
        }

        /**
         * Sets a new system-wide vocabulary.
         *
         * @param vocabulary {@link OntVocabulary}, not {@code null}
         * @return {@link OntVocabulary}
         */
        public static OntVocabulary set(OntVocabulary vocabulary) {
            OntVocabulary prev = get();
            def = Objects.requireNonNull(vocabulary, "Null vocabulary specified.");
            return prev;
        }

        /**
         * Creates a fresh union vocabulary that combines the given ones.
         *
         * @param vocabularies an {@code Array} of {@link OntVocabulary}s
         * @return {@link OntVocabulary}
         * @see #create(String, Collection)
         */
        public static OntVocabulary create(OntVocabulary... vocabularies) {
            return new Impl(Arrays.stream(vocabularies)
                    .map(Factory::asMap)
                    .flatMap(x -> x.entrySet().stream())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> {
                        Set<Resource> res = new HashSet<>(a);
                        res.addAll(b);
                        return res;
                    })));
        }

        /**
         * Creates a {@link OntVocabulary} that contains the specified mapping ({@code key -> Set}).
         *
         * @param key    an URI-{@link Resource}, not {@code null}
         * @param values an {@code Array} with {@link Resource}s to map, not {@code null}
         * @return a {@link OntVocabulary} with single (specified) mapping
         */
        public static OntVocabulary create(Resource key, Resource... values) {
            return create(key, Arrays.stream(values).collect(Iter.toUnmodifiableSet()));
        }

        /**
         * Creates a {@link OntVocabulary} that contains the specified mapping ({@code key -> Set}).
         *
         * @param key    an URI-{@link Resource}, not {@code null}
         * @param values a {@code Collection} of {@link Resource}s to map, not {@code null}
         * @return a {@link OntVocabulary} with single (specified) mapping
         */
        public static OntVocabulary create(Resource key, Collection<? extends Resource> values) {
            return create(Objects.requireNonNull(key).getURI(), values);
        }

        /**
         * Creates a {@link OntVocabulary} that contains the specified mapping ({@code key -> Set}).
         *
         * @param key    {@code String}, a URI of resource-family, not {@code null}
         * @param values a {@code Collection} of {@link Resource}s to map, not {@code null}
         * @return a {@link OntVocabulary} with single mapping
         * @see #create(OntVocabulary...)
         */
        public static OntVocabulary create(String key, Collection<? extends Resource> values) {
            Map<String, Set<? extends Resource>> map = new HashMap<>();
            map.put(Objects.requireNonNull(key), toUnmodifiableSet(Objects.requireNonNull(values)));
            return new Impl(map);
        }

        /**
         * Creates a {@link OntVocabulary} with mapping for system resource/properties.
         *
         * @param schemas an {@code Array} of schemas
         *                - constant-holders with {@link Resource} and {@link Property} public static final fields,
         *                not {@code null}
         * @return a {@link OntVocabulary} with mapping for system resources and properties
         * (keys: {@link RDFS#Resource rdfs:Resource} and {@link RDF#Property rdf:Property})
         */
        public static OntVocabulary create(Class<?>... schemas) {
            return new Impl(getConstants(Property.class, schemas), getConstants(Resource.class, schemas));
        }

        private static Stream<Field> directFields(Class<?> vocabulary, Class<?> type) {
            return Arrays.stream(vocabulary.getDeclaredFields())
                    .filter(x -> Modifier.isPublic(x.getModifiers()))
                    .filter(x -> Modifier.isStatic(x.getModifiers()))
                    .filter(x -> type.equals(x.getType()));
        }

        private static Stream<Field> fields(Class<?> vocabulary, Class<?> type) {
            Stream<Field> res = directFields(vocabulary, type);
            return vocabulary.getSuperclass() != null ? Stream.concat(res, fields(vocabulary.getSuperclass(), type)) : res;
        }

        private static <T> Stream<T> constants(Class<?> vocabulary, Class<T> type) {
            return fields(vocabulary, type).map(x -> getValue(x, type)).filter(Objects::nonNull);
        }

        private static <T> T getValue(Field field, Class<T> type) {
            try {
                return type.cast(field.get(null));
            } catch (IllegalAccessException e) {
                throw new OntJenaException.IllegalState("Unable to get an object of the type " + type.getSimpleName() +
                        " from the field " + field.getName(), e);
            }
        }

        protected static <T> Set<T> getConstants(Class<? extends T> type, Class<?>... vocabularies) {
            return Arrays.stream(vocabularies)
                    .flatMap(x -> constants(x, type))
                    .collect(Iter.toUnmodifiableSet());
        }

        private static Map<String, Set<? extends Resource>> asMap(OntVocabulary voc) {
            if (voc instanceof Impl) {
                return ((Impl) voc).map;
            }
            Map<String, Set<? extends Resource>> res = new HashMap<>();
            Stream.of(OWL.AnnotationProperty, OWL.DatatypeProperty, OWL.ObjectProperty,
                    RDFS.Datatype, OWL.Class, SWRL.Builtin, RDF.Property, RDFS.Resource)
                    .forEach(x -> res.put(x.getURI(), voc.get(x)));
            return res;
        }

        private static <X> Set<X> toUnmodifiableSet(Collection<X> input) {
            if (input instanceof Set && input.getClass().getName().equals("java.util.Collections$UnmodifiableSet")) {
                return (Set<X>) input;
            }
            return input.stream().peek(Objects::requireNonNull).collect(Iter.toUnmodifiableSet());
        }

        /**
         * Access to the {@link OWL OWL2} vocabulary.
         */
        @SuppressWarnings("WeakerAccess")
        protected static class OWLImpl extends Impl {
            private static final Class<?>[] VOCABULARIES = new Class<?>[]{XSD.class, RDF.class, RDFS.class, OWL.class};
            /**
             * The list of datatypes from owl-2 specification (35 types)
             * (see <a href='https://www.w3.org/TR/owl2-quick-reference/'>Quick References, 3.1 Built-in Datatypes</a>).
             * It seems it is not full:
             */
            public static final Set<Resource> OWL2_DATATYPES =
                    Stream.of(RDF.xmlLiteral, RDF.PlainLiteral, RDF.langString,
                            RDFS.Literal, OWL.real, OWL.rational, XSD.xstring, XSD.normalizedString,
                            XSD.token, XSD.language, XSD.Name, XSD.NCName, XSD.NMTOKEN, XSD.decimal, XSD.integer,
                            XSD.xdouble, XSD.xfloat, XSD.xboolean,
                            XSD.nonNegativeInteger, XSD.nonPositiveInteger, XSD.positiveInteger, XSD.negativeInteger,
                            XSD.xlong, XSD.xint, XSD.xshort, XSD.xbyte,
                            XSD.unsignedLong, XSD.unsignedInt, XSD.unsignedShort, XSD.unsignedByte,
                            XSD.hexBinary, XSD.base64Binary,
                            XSD.anyURI, XSD.dateTime, XSD.dateTimeStamp
                    ).collect(Iter.toUnmodifiableSet());
            public static final Set<RDFDatatype> JENA_RDF_DATATYPE_SET = initBuiltInRDFDatatypes(TypeMapper.getInstance());
            public static final Set<Resource> DATATYPES = JENA_RDF_DATATYPE_SET.stream().map(RDFDatatype::getURI).
                    map(ResourceFactory::createResource).collect(Iter.toUnmodifiableSet());
            public static final Set<Resource> CLASSES = Stream.of(OWL.Nothing, OWL.Thing).collect(Iter.toUnmodifiableSet());
            public static final Set<Property> ANNOTATION_PROPERTIES =
                    Stream.of(RDFS.label, RDFS.comment, RDFS.seeAlso, RDFS.isDefinedBy, OWL.versionInfo,
                            OWL.backwardCompatibleWith, OWL.priorVersion, OWL.incompatibleWith, OWL.deprecated)
                            .collect(Iter.toUnmodifiableSet());
            public static final Set<Property> DATA_PROPERTIES =
                    Stream.of(OWL.topDataProperty, OWL.bottomDataProperty).collect(Iter.toUnmodifiableSet());
            public static final Set<Property> OBJECT_PROPERTIES =
                    Stream.of(OWL.topObjectProperty, OWL.bottomObjectProperty).collect(Iter.toUnmodifiableSet());
            public static final Set<Property> PROPERTIES = getConstants(Property.class, VOCABULARIES);
            public static final Set<Resource> RESOURCES = getConstants(Resource.class, VOCABULARIES);

            protected OWLImpl() {
                super(ANNOTATION_PROPERTIES, DATA_PROPERTIES, OBJECT_PROPERTIES,
                        CLASSES, DATATYPES, null, PROPERTIES, RESOURCES);
            }

            private static Set<RDFDatatype> initBuiltInRDFDatatypes(TypeMapper types) {
                Stream.of(OWL.real, OWL.rational).forEach(d -> types.registerDatatype(new BaseDatatype(d.getURI())));
                OWL2_DATATYPES.forEach(iri -> types.getSafeTypeByName(iri.getURI()));
                Set<RDFDatatype> res = new HashSet<>();
                types.listTypes().forEachRemaining(res::add);
                return Collections.unmodifiableSet(res);
            }
        }

        /**
         * Access to {@link DC} vocabulary.
         */
        protected static class DCImpl extends Impl {
            public static final Set<Property> ALL_PROPERTIES = getConstants(Property.class, DC.class);

            protected DCImpl() {
                super(ALL_PROPERTIES, null, null, null, null, null, ALL_PROPERTIES, null);
            }
        }

        /**
         * Access to {@link SKOS} vocabulary.
         */
        @SuppressWarnings("WeakerAccess")
        protected static class SKOSImpl extends Impl {
            public static final Set<Property> ANNOTATION_PROPERTIES =
                    Stream.of(SKOS.altLabel, SKOS.changeNote, SKOS.definition,
                            SKOS.editorialNote, SKOS.example, SKOS.hiddenLabel, SKOS.historyNote,
                            SKOS.note, SKOS.prefLabel, SKOS.scopeNote)
                            .collect(Iter.toUnmodifiableSet());
            public static final Set<Property> OBJECT_PROPERTIES =
                    Stream.of(SKOS.broadMatch, SKOS.broader, SKOS.broaderTransitive,
                            SKOS.closeMatch, SKOS.exactMatch, SKOS.hasTopConcept, SKOS.inScheme,
                            SKOS.mappingRelation, SKOS.member, SKOS.memberList, SKOS.narrowMatch,
                            SKOS.narrower, SKOS.narrowerTransitive, SKOS.related,
                            SKOS.relatedMatch, SKOS.semanticRelation, SKOS.topConceptOf)
                            .collect(Iter.toUnmodifiableSet());
            /**
             * NOTE: In the {@link org.semanticweb.owlapi.vocab.SKOSVocabulary} there is also skos:TopConcept
             * But in fact there is no such resource in the <a href='https://www.w3.org/2009/08/skos-reference/skos.htm'>specification</a>.
             */
            public static final Set<Resource> CLASSES =
                    Stream.of(SKOS.Collection, SKOS.Concept, SKOS.ConceptScheme, SKOS.OrderedCollection)
                            .collect(Iter.toUnmodifiableSet());

            public static final Set<Property> PROPERTIES = getConstants(Property.class, SKOS.class);
            public static final Set<Resource> RESOURCES = getConstants(Resource.class, SKOS.class);


            protected SKOSImpl() {
                super(ANNOTATION_PROPERTIES, null, OBJECT_PROPERTIES, CLASSES, null, null, PROPERTIES, RESOURCES);
            }
        }

        /**
         * For SWRL modeling.
         *
         * @see SWRL
         * @see SWRLB
         */
        protected static class SWRLImpl extends Impl {
            private static final Class<?>[] VOCABULARIES = new Class<?>[]{SWRL.class, SWRLB.class};
            public static final Set<Resource> BUILTINS = getConstants(Property.class, SWRLB.class);
            public static final Set<Property> PROPERTIES = getConstants(Property.class, VOCABULARIES);
            public static final Set<Resource> RESOURCES = getConstants(Resource.class, VOCABULARIES);

            protected SWRLImpl() {
                super(null, null, null, null, null, BUILTINS, PROPERTIES, RESOURCES);
            }
        }

        /**
         * The base implementation.
         */
        protected static class Impl implements OntVocabulary {
            private final Map<String, Set<? extends Resource>> map;

            private Impl(Set<Property> properties,
                         Set<Resource> resources) {
                this(null, null, null, null, null, null, properties, resources);
            }

            protected Impl(Set<Property> annotationProperties,
                           Set<Property> dataProperties,
                           Set<Property> objectProperties,
                           Set<Resource> classes,
                           Set<Resource> datatypes,
                           Set<Resource> swrlBuiltins,
                           Set<Property> allProperties,
                           Set<Resource> allResources) {
                this(new HashMap<String, Set<? extends Resource>>() {
                    {
                        put(OWL.AnnotationProperty, annotationProperties);
                        put(OWL.DatatypeProperty, dataProperties);
                        put(OWL.ObjectProperty, objectProperties);
                        put(OWL.Class, classes);
                        put(RDFS.Datatype, datatypes);
                        put(SWRL.Builtin, swrlBuiltins);
                        put(RDF.Property, allProperties);
                        put(RDFS.Resource, allResources);
                    }

                    private void put(Resource k, Set<? extends Resource> v) {
                        if (v == null) return;
                        put(k.getURI(), v);
                    }
                });
            }

            protected Impl(Map<String, Set<? extends Resource>> map) {
                this.map = Objects.requireNonNull(map);
            }

            @Override
            public Set<? extends Resource> get(String key) {
                return map.getOrDefault(key, Collections.emptySet());
            }
        }
    }
}
