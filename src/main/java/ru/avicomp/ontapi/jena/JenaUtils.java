package ru.avicomp.ontapi.jena;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.datatypes.BaseDatatype;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.util.NodeUtils;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import ru.avicomp.ontapi.OntException;

/**
 * to work with {@link org.apache.jena.rdf.model.Model}
 * Created by szuev on 20.10.2016.
 */
public class JenaUtils {

    public static final Set<Property> BUILT_IN_PROPERTIES = getConstants(Property.class, RDF.class, RDFS.class, OWL.class, OWL2.class);
    public static final Set<Resource> BUILT_IN_RESOURCES = getConstants(Resource.class, RDF.class, RDFS.class, OWL.class, OWL2.class);

    public static final Set<RDFDatatype> BUILT_IN_DATATYPES = createBuiltInTypes();

    public static final Comparator<RDFNode> RDF_NODE_COMPARATOR = (o1, o2) -> NodeUtils.compareRDFTerms(o1.asNode(), o2.asNode());

    public static Resource createTypedList(Model model, Resource type, List<? extends RDFNode> members) {
        if (members.isEmpty()) return RDF.nil.inModel(model);
        Resource res = model.createResource();
        res.addProperty(RDF.type, type);
        res.addProperty(RDF.first, members.remove(0));
        res.addProperty(RDF.rest, createTypedList(model, type, members));
        return res;
    }

    public static Resource createTypedList(Model model, Resource type, Stream<? extends RDFNode> members) {
        return createTypedList(model, type, members.collect(Collectors.toList()));
    }

    private static Set<RDFDatatype> createBuiltInTypes() {
        TypeMapper mapper = TypeMapper.getInstance();
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

    private static Stream<Field> fields(Class vocabulary, Class<?> type) {
        return Arrays.stream(vocabulary.getDeclaredFields()).
                filter(field -> Modifier.isPublic(field.getModifiers())).
                filter(field -> Modifier.isStatic(field.getModifiers())).
                filter(field -> type.equals(field.getType()));
    }

    private static <T> Stream<T> constants(Class vocabulary, Class<T> type) {
        return fields(vocabulary, type).map(field -> getValue(field, type));
    }

    private static <T> T getValue(Field field, Class<T> type) {
        try {
            return type.cast(field.get(null));
        } catch (IllegalAccessException e) {
            throw new OntException(e);
        }
    }

    private static <T> Set<T> getConstants(Class<T> type, Class... vocabularies) {
        return Arrays.stream(vocabularies).map(voc -> constants(voc, type)).flatMap(Function.identity()).collect(Collectors.toSet());
    }
}
