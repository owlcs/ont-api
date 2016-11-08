package ru.avicomp.ontapi.jena.impl;

import java.util.stream.Stream;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDFS;

import ru.avicomp.ontapi.jena.model.OntCE;
import ru.avicomp.ontapi.jena.model.OntClassEntity;

/**
 * owl:Class
 * Created by szuev on 03.11.2016.
 */
public class OntClassEntityImpl extends OntEntityImpl implements OntClassEntity {

    OntClassEntityImpl(Resource inModel) {
        super(inModel);
    }

    public OntClassEntityImpl(Node n, EnhGraph eg) {
        super(n, eg);
    }

    @Override
    public Class<OntClassEntity> getActualClass() {
        return OntClassEntity.class;
    }

    @Override
    public Resource getRDFType() {
        return OWL2.Class;
    }

    @Override
    public Stream<OntCE> subClassOf() {
        return getModel().classExpressions(this, RDFS.subClassOf);
    }
}
