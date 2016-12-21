package ru.avicomp.ontapi.jena.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.datatypes.BaseDatatype;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDFS;

import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.vocabulary.OWL2;
import ru.avicomp.ontapi.jena.vocabulary.RDF;
import ru.avicomp.ontapi.jena.vocabulary.XSD;

/**
 * Helper to work with constants from {@link ru.avicomp.ontapi.jena.vocabulary} package.
 * <p>
 * Created by @szuev on 21.12.2016.
 */
public class BuiltIn {
    // list of datatypes from owl-2 specification (35 types):
    public static final Set<Resource> OWL2_DATATYPES =
            Stream.of(RDF.xmlLiteral, RDF.PlainLiteral, RDF.langString,
                    RDFS.Literal, OWL2.real, OWL2.rational, XSD.xstring, XSD.normalizedString,
                    XSD.token, XSD.language, XSD.Name, XSD.NCName, XSD.NMTOKEN, XSD.decimal, XSD.integer,
                    XSD.xdouble, XSD.xfloat, XSD.xboolean,
                    XSD.nonNegativeInteger, XSD.nonPositiveInteger, XSD.positiveInteger, XSD.negativeInteger,
                    XSD.xlong, XSD.xint, XSD.xshort, XSD.xbyte,
                    XSD.unsignedLong, XSD.unsignedInt, XSD.unsignedShort, XSD.unsignedByte,
                    XSD.hexBinary, XSD.base64Binary,
                    XSD.anyURI, XSD.dateTime, XSD.dateTimeStamp
            ).collect(Collectors.toSet());
    public static final Set<RDFDatatype> RDF_DATATYPE_SET = createBuiltInTypes();

    // full list of datatypes:
    public static final Set<Resource> DATATYPES = RDF_DATATYPE_SET.stream().map(RDFDatatype::getURI).
            map(ResourceFactory::createResource).collect(Collectors.toSet());
    public static final Set<Resource> CLASSES = Stream.of(OWL2.Nothing, OWL2.Thing).collect(Collectors.toSet());
    public static final Set<Resource> ANNOTATION_PROPERTIES = Stream.of(RDFS.label, RDFS.comment, RDFS.seeAlso, RDFS.isDefinedBy,
            OWL2.versionInfo, OWL2.backwardCompatibleWith, OWL2.priorVersion, OWL2.incompatibleWith, OWL2.deprecated).collect(Collectors.toSet());
    public static final Set<Resource> DATA_PROPERTIES = Stream.of(OWL2.topDataProperty, OWL2.bottomDataProperty).collect(Collectors.toSet());
    public static final Set<Resource> OBJECT_PROPERTIES = Stream.of(OWL2.topObjectProperty, OWL2.bottomObjectProperty).collect(Collectors.toSet());
    public static final Set<Resource> ENTITIES =
            Stream.of(CLASSES, DATATYPES, ANNOTATION_PROPERTIES, DATA_PROPERTIES, OBJECT_PROPERTIES)
                    .flatMap(Collection::stream).collect(Collectors.toSet());

    public static final Set<Property> PROPERTIES = getConstants(Property.class, XSD.class, RDF.class, RDFS.class, OWL2.class);
    public static final Set<Resource> RESOURCES = getConstants(Resource.class, XSD.class, RDF.class, RDFS.class, OWL2.class);
    public static final Set<Resource> ALL = Stream.of(PROPERTIES, RESOURCES).flatMap(Collection::stream).collect(Collectors.toSet());

    private static Set<RDFDatatype> createBuiltInTypes() {
        TypeMapper mapper = TypeMapper.getInstance();
        Stream.of(OWL2.real, OWL2.rational).forEach(d -> mapper.registerDatatype(new BaseDatatype(d.getURI()) {
            @Override
            public Class<?> getJavaClass() {
                return Double.class;
            }
        }));
        OWL2_DATATYPES.forEach(iri -> mapper.getSafeTypeByName(iri.getURI()));
        Set<RDFDatatype> res = new HashSet<>();
        mapper.listTypes().forEachRemaining(res::add);
        return res;
    }

    private static Stream<Field> directFields(Class vocabulary, Class<?> type) {
        return Arrays.stream(vocabulary.getDeclaredFields()).
                filter(field -> Modifier.isPublic(field.getModifiers())).
                filter(field -> Modifier.isStatic(field.getModifiers())).
                filter(field -> type.equals(field.getType()));
    }

    private static Stream<Field> fields(Class vocabulary, Class<?> type) {
        Stream<Field> res = directFields(vocabulary, type);
        return vocabulary.getSuperclass() != null ? Stream.concat(res, fields(vocabulary.getSuperclass(), type)) : res;
    }

    private static <T> Stream<T> constants(Class vocabulary, Class<T> type) {
        return fields(vocabulary, type).map(field -> getValue(field, type)).filter(Objects::nonNull);
    }

    private static <T> T getValue(Field field, Class<T> type) {
        try {
            return type.cast(field.get(null));
        } catch (IllegalAccessException e) {
            throw new OntJenaException(e);
        }
    }

    private static <T> Set<T> getConstants(Class<T> type, Class... vocabularies) {
        return Arrays.stream(vocabularies).map(voc -> constants(voc, type)).flatMap(Function.identity()).collect(Collectors.toSet());
    }
}
