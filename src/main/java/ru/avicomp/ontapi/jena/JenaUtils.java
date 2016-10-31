package ru.avicomp.ontapi.jena;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.datatypes.BaseDatatype;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.util.NodeUtils;
import org.apache.jena.vocabulary.RDF;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

/**
 * to work with {@link org.apache.jena.rdf.model.Model}
 * Created by szuev on 20.10.2016.
 */
public class JenaUtils {

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

}
