package ru.avicomp.ontapi.jena.impl;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;

import ru.avicomp.ontapi.jena.model.OntDatatypeEntity;
import ru.avicomp.ontapi.jena.model.OntEntity;

/**
 * rdfs:Datatype
 * Created by szuev on 03.11.2016.
 */
class OntDatatypeImpl extends OntEntityImpl implements OntDatatypeEntity {

    OntDatatypeImpl(Resource inModel) {
        super(inModel);
    }

    OntDatatypeImpl(Node n, EnhGraph g) {
        super(n, g);
    }

    @Override
    public Class<? extends OntEntity> getActualClass() {
        return OntDatatypeEntity.class;
    }

    @Override
    public Resource getRDFType() {
        return RDFS.Datatype;
    }

}
