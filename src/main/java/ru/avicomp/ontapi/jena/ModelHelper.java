package ru.avicomp.ontapi.jena;

import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;

import ru.avicomp.ontapi.OntException;
import ru.avicomp.ontapi.jena.model.OntCE;
import ru.avicomp.ontapi.jena.model.OntDR;
import ru.avicomp.ontapi.jena.model.OntObject;

/**
 * to work with {@link GraphModelImpl}
 * <p>
 * Created by szuev on 01.11.2016.
 */
public class ModelHelper {
    public static <T> Stream<T> asStream(Iterator<T> iterator) {
        return asStream(iterator, false);
    }

    public static <T> Stream<T> asStream(Iterator<T> iterator, boolean parallel) {
        Iterable<T> iterable = () -> iterator;
        return StreamSupport.stream(iterable.spliterator(), parallel);
    }

    public static Stream<RDFNode> asStream(GraphModelImpl model, RDFList list) {
        return list.asJavaList().stream().map(n -> wrapRDFNode(model, n)).distinct();
    }

    public static boolean isCE(Model model, RDFNode node) {
        return node.isResource() && (model.contains(node.asResource(), RDF.type, OWL.Class) || model.contains(node.asResource(), RDF.type, OWL.Restriction));
    }

    public static RDFNode wrapRDFNode(GraphModelImpl model, RDFNode node) {
        if (node.isLiteral()) return node;
        return toOntObject(model, node);
    }

    public static OntObject toOntObject(GraphModelImpl model, RDFNode node) {
        if (isCE(model, node)) {
            return toCE(model, node.asResource());
        }
        throw new OntException("Unsupported resource " + node);
    }

    public static OntCE toCE(GraphModelImpl model, Resource resource) {
        if (resource.isURIResource()) {
            return model.new ClassImpl(resource);
        }
        if (model.contains(resource, OWL.unionOf)) {
            return model.new UnionOf(resource);
        }
        if (model.contains(resource, OWL.intersectionOf)) {
            return model.new IntersectionOf(resource);
        }
        if (model.contains(resource, OWL.oneOf)) {
            return model.new OneOf(resource);
        }
        throw new OntException("Unsupported class expression " + resource);
    }

    public static OntDR toDR(GraphModelImpl model, Resource resource) {
        throw new OntException("Unsupported data-range expression " + resource);
    }
}
