package ru.avicomp.ontapi.jena;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.jena.datatypes.BaseDatatype;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.rdf.model.*;
import org.apache.jena.sparql.util.NodeUtils;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import ru.avicomp.ontapi.jena.vocabulary.XSD;

/**
 * helper to work with jena
 *
 * Created by szuev on 20.10.2016.
 */
public class JenaUtils {
    public static final Comparator<RDFNode> RDF_NODE_COMPARATOR = (o1, o2) -> NodeUtils.compareRDFTerms(o1.asNode(), o2.asNode());

    public static final Set<Property> BUILT_IN_PROPERTIES = getConstants(Property.class, XSD.class, RDF.class, RDFS.class, OWL2.class);
    public static final Set<Resource> BUILT_IN_RESOURCES = getConstants(Resource.class, XSD.class, RDF.class, RDFS.class, OWL2.class);
    public static final Set<Resource> BUILT_IN_ALL = Stream.of(BUILT_IN_PROPERTIES, BUILT_IN_RESOURCES).flatMap(Collection::stream).collect(Collectors.toSet());

    public static final Set<RDFDatatype> BUILT_IN_RDF_DATATYPES = createBuiltInTypes();

    public static final Set<Resource> BUILT_IN_DATATYPES = BUILT_IN_RDF_DATATYPES.stream().map(RDFDatatype::getURI).
            map(ResourceFactory::createResource).collect(Collectors.toSet());
    public static final Set<Resource> BUILT_IN_CLASSES = Stream.of(OWL2.Nothing, OWL2.Thing).collect(Collectors.toSet());
    public static final Set<Resource> BUILT_IN_ANNOTATION_PROPERTIES = Stream.of(RDFS.label, RDFS.comment, RDFS.seeAlso, RDFS.isDefinedBy,
            OWL2.versionInfo, OWL2.backwardCompatibleWith, OWL2.priorVersion, OWL2.incompatibleWith, OWL2.deprecated).collect(Collectors.toSet());
    public static final Set<Resource> BUILT_IN_DATA_PROPERTIES = Stream.of(OWL2.topDataProperty, OWL2.bottomDataProperty).collect(Collectors.toSet());
    public static final Set<Resource> BUILT_IN_OBJECT_PROPERTIES = Stream.of(OWL2.topObjectProperty, OWL2.bottomObjectProperty).collect(Collectors.toSet());
    public static final Set<Resource> BUILT_IN_ENTITIES = Stream.of(BUILT_IN_CLASSES, BUILT_IN_DATATYPES,
            BUILT_IN_ANNOTATION_PROPERTIES, BUILT_IN_DATA_PROPERTIES, BUILT_IN_OBJECT_PROPERTIES)
            .flatMap(Collection::stream).collect(Collectors.toSet());

    /**
     * creates typed list: the anonymous section which is built using the same rules as true rdf:List,
     * i.e. by using rdf:first, rdf:rest and rdf:nil predicates.
     *
     * @param model   Model
     * @param type    Resource
     * @param members List of {@link RDFNode}'s
     * @return Anonymous resource - the header for typed list.
     */
    public static Resource createTypedList(Model model, Resource type, List<? extends RDFNode> members) {
        if (members.isEmpty()) return RDF.nil.inModel(model);
        Resource res = model.createResource();
        res.addProperty(RDF.type, type);
        res.addProperty(RDF.first, members.remove(0));
        res.addProperty(RDF.rest, createTypedList(model, type, members));
        return res;
    }

    /**
     * Builds typed list from Stream of RDFNode's
     *
     * @param model   Model
     * @param type    type of list to create
     * @param members Stream of members
     * @return the head of created list.
     */
    public static Resource createTypedList(Model model, Resource type, Stream<? extends RDFNode> members) {
        return createTypedList(model, type, members.collect(Collectors.toList()));
    }

    /**
     * Recursively gets the content of rdf:List as Stream of {@link RDFNode}s
     *
     * @param model   Model
     * @param anyList Resource. could be true rdf:List or typed List (which consists of rdf:first, rdf:rest and rdf:nil nodes)
     * @return Stream, could be empty if list is empty or specified resource is not a list.
     */
    public static Stream<RDFNode> rdfListContent(Model model, Resource anyList) {
        RDFNode first = null;
        RDFNode rest = null;
        Statement rdfFirst = model.getProperty(anyList, RDF.first);
        if (rdfFirst != null) first = rdfFirst.getObject();
        if (first == null) return Stream.empty();

        Statement rdfRest = model.getProperty(anyList, RDF.rest);
        if (rdfRest != null && !RDF.nil.equals(rdfRest.getObject())) {
            rest = rdfRest.getObject();
        }
        if (rest == null) {
            return Stream.of(first);
        }
        Stream<RDFNode> a = Stream.of(first);
        Stream<RDFNode> b = rest.isResource() ? rdfListContent(model, rest.asResource()) : Stream.of(rest);
        return Stream.concat(a, b);
    }

    private static Set<RDFDatatype> createBuiltInTypes() {
        TypeMapper mapper = TypeMapper.getInstance();
        // todo: no entries of OWL-API should be in this class
        Stream.of(OWL2Datatype.OWL_REAL, OWL2Datatype.OWL_RATIONAL).forEach(d -> mapper.registerDatatype(new BaseDatatype(d.getIRI().getIRIString()) {
            @Override
            public Class<?> getJavaClass() {
                return Double.class;
            }
        }));
        OWL2Datatype.getDatatypeIRIs().forEach(iri -> mapper.getSafeTypeByName(iri.getIRIString()));
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

    public static <T> Stream<T> asStream(Iterator<T> iterator) {
        return asStream(iterator, true, false);
    }

    public static <T> Stream<T> asStream(Iterator<T> iterator, boolean distinct, boolean parallel) {
        Iterable<T> iterable = () -> iterator;
        Stream<T> res = StreamSupport.stream(iterable.spliterator(), parallel);
        return distinct ? res.distinct() : res;
    }

    public static Set<Statement> getAssociatedStatements(Resource inModel) {
        Set<Statement> statements = inModel.listProperties().toSet();
        Set<Statement> res = new HashSet<>(statements);
        statements.stream().map(Statement::getObject).filter(RDFNode::isAnon).map(RDFNode::asResource)
                .forEach(r -> res.addAll(getAssociatedStatements(r)));
        return res;
    }
}
