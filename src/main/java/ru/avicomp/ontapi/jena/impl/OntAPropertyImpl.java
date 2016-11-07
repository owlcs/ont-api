package ru.avicomp.ontapi.jena.impl;

import java.util.stream.Stream;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDFS;

import ru.avicomp.ontapi.jena.model.OntAPEntity;
import ru.avicomp.ontapi.jena.model.OntEntity;

/**
 * owl:AnnotationProperty
 * <p>
 * Created by szuev on 03.11.2016.
 */
class OntAPropertyImpl extends OntEntityImpl implements OntAPEntity {

    OntAPropertyImpl(Resource inModel) {
        super(inModel);
    }

    OntAPropertyImpl(Node n, EnhGraph g) {
        super(n, g);
    }

    @Override
    public Class<? extends OntEntity> getActualClass() {
        return OntAPEntity.class;
    }

    @Override
    public Resource getRDFType() {
        return OWL.AnnotationProperty;
    }

    @Override
    public Stream<Resource> domain() {
        return GraphModelImpl.asStream(getModel().listObjectsOfProperty(this, RDFS.domain).mapWith(RDFNode::asResource));
    }

    @Override
    public Stream<Resource> range() {
        return GraphModelImpl.asStream(getModel().listObjectsOfProperty(this, RDFS.range).mapWith(RDFNode::asResource));
    }
}
