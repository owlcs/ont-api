package ru.avicomp.ontapi.jena.impl;

import java.util.stream.Stream;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;

import ru.avicomp.ontapi.jena.model.OntCE;
import ru.avicomp.ontapi.jena.model.OntDPEntity;
import ru.avicomp.ontapi.jena.model.OntDR;

/**
 * owl:DatatypeProperty
 * Created by szuev on 03.11.2016.
 */
class OntDPropertyImpl extends OntEntityImpl implements OntDPEntity {
    OntDPropertyImpl(Resource r) {
        super(r);
    }

    @Override
    Type getOntType() {
        return Type.DATA_PROPERTY;
    }

    @Override
    public Stream<OntCE> domain() {
        return getModel().classExpressions(this, RDFS.domain);
    }

    @Override
    public Stream<OntDR> range() {
        return GraphModelImpl.asStream(getModel().listObjectsOfProperty(this, RDFS.range).mapWith(node -> getModel().wrapDR(node.asResource())));
    }
}
