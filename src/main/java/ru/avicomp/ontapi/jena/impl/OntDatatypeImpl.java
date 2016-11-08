package ru.avicomp.ontapi.jena.impl;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;

import ru.avicomp.ontapi.jena.model.OntDatatypeEntity;

/**
 * rdfs:Datatype
 * Created by szuev on 03.11.2016.
 */
public class OntDatatypeImpl extends OntEntityImpl implements OntDatatypeEntity {

    OntDatatypeImpl(Resource inModel) {
        super(inModel);
    }

    public OntDatatypeImpl(Node n, EnhGraph g) {
        super(n, g);
    }

    @Override
    public Class<OntDatatypeEntity> getActualClass() {
        return OntDatatypeEntity.class;
    }

    @Override
    public Resource getRDFType() {
        return RDFS.Datatype;
    }

}
