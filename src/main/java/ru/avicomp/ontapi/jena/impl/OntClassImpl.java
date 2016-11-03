package ru.avicomp.ontapi.jena.impl;

import java.util.stream.Stream;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;

import ru.avicomp.ontapi.jena.model.OntCE;
import ru.avicomp.ontapi.jena.model.OntClassEntity;

/**
 * owl:Class
 * Created by szuev on 03.11.2016.
 */
class OntClassImpl extends OntEntityImpl implements OntClassEntity {
    OntClassImpl(Resource r) {
        super(r);
    }

    @Override
    Type getOntType() {
        return Type.CLASS;
    }

    @Override
    public Stream<OntCE> subClassOf() {
        return getModel().classExpressions(this, RDFS.subClassOf);
    }
}
