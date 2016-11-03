package ru.avicomp.ontapi.jena.impl;

import java.util.stream.Stream;

import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.vocabulary.RDF;

import ru.avicomp.ontapi.jena.model.OntObject;

/**
 * base resource.
 * <p>
 * Created by szuev on 03.11.2016.
 */
public abstract class OntObjectImpl extends ResourceImpl implements OntObject {

    OntObjectImpl(Resource r) {
        this(r.asNode(), (GraphModelImpl) r.getModel());
    }

    public OntObjectImpl(Node n, GraphModelImpl m) {
        super(n, m);
    }

    @Override
    public Stream<Resource> types() {
        return GraphModelImpl.asStream(getModel().listObjectsOfProperty(this, RDF.type)
                .filterKeep(RDFNode::isURIResource).mapWith(Resource.class::cast));
    }

    @Override
    public GraphModelImpl getModel() {
        return (GraphModelImpl) super.getModel();
    }
}
