package ru.avicomp.ontapi.jena.impl;

import java.util.stream.Stream;

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;

import ru.avicomp.ontapi.jena.model.OntAPEntity;

/**
 * owl:AnnotationProperty
 * <p>
 * Created by szuev on 03.11.2016.
 */
class OntAPropertyImpl extends OntEntityImpl implements OntAPEntity {
    OntAPropertyImpl(Resource r) {
        super(r);
    }

    @Override
    Type getOntType() {
        return Type.ANNOTATION_PROPERTY;
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
