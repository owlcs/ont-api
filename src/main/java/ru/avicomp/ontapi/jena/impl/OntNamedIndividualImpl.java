package ru.avicomp.ontapi.jena.impl;

import org.apache.jena.rdf.model.Resource;

import ru.avicomp.ontapi.jena.model.OntIndividualEntity;

/**
 * owl:NamedIndividual
 * <p>
 * Created by szuev on 03.11.2016.
 */
class OntNamedIndividualImpl extends OntEntityImpl implements OntIndividualEntity {
    OntNamedIndividualImpl(Resource r) {
        super(r);
    }

    @Override
    public Type getOntType() {
        return Type.INDIVIDUAL;
    }
}
