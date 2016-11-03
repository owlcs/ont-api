package ru.avicomp.ontapi.jena.impl;

import java.util.stream.Stream;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;

import ru.avicomp.ontapi.jena.model.OntCE;
import ru.avicomp.ontapi.jena.model.OntOPEntity;

/**
 * owl:ObjectProperty
 * <p>
 * Created by szuev on 03.11.2016.
 */
class OntOPropertyImpl extends OntEntityImpl implements OntOPEntity {
    OntOPropertyImpl(Resource r) {
        super(r);
    }

    @Override
    public Stream<OntCE> domain() {
        return getModel().classExpressions(this, RDFS.domain);
    }

    @Override
    public Stream<OntCE> range() {
        return getModel().classExpressions(this, RDFS.range);
    }

    @Override
    public Type getOntType() {
        return Type.OBJECT_PROPERTY;
    }
}
