package ru.avicomp.ontapi.jena.impl;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL2;

import ru.avicomp.ontapi.jena.model.OntEntity;
import ru.avicomp.ontapi.jena.model.OntIndividualEntity;

/**
 * owl:NamedIndividual
 * <p>
 * Created by szuev on 03.11.2016.
 */
public class OntNamedIndividualImpl extends OntEntityImpl implements OntIndividualEntity {

    OntNamedIndividualImpl(Resource inModel) {
        super(inModel);
    }

    public OntNamedIndividualImpl(Node n, EnhGraph g) {
        super(n, g);
    }

    @Override
    public Class<? extends OntEntity> getActualClass() {
        return OntIndividualEntity.class;
    }

    @Override
    public Resource getRDFType() {
        return OWL2.NamedIndividual;
    }

}
