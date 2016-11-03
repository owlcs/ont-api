package ru.avicomp.ontapi.jena.impl;

import org.apache.jena.rdf.model.Resource;

import ru.avicomp.ontapi.jena.model.OntDatatypeEntity;

/**
 * rdfs:Datatype
 * Created by szuev on 03.11.2016.
 */
class OntDatatypeImpl extends OntEntityImpl implements OntDatatypeEntity {
    OntDatatypeImpl(Resource r) {
        super(r);
    }

    @Override
    Type getOntType() {
        return Type.DATATYPE;
    }
}
