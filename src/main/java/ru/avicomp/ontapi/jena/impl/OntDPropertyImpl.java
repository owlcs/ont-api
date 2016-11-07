package ru.avicomp.ontapi.jena.impl;

import java.util.stream.Stream;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDFS;

import ru.avicomp.ontapi.jena.model.OntCE;
import ru.avicomp.ontapi.jena.model.OntDPEntity;
import ru.avicomp.ontapi.jena.model.OntDR;
import ru.avicomp.ontapi.jena.model.OntEntity;

/**
 * owl:DatatypeProperty
 * Created by szuev on 03.11.2016.
 */
public class OntDPropertyImpl extends OntEntityImpl implements OntDPEntity {

    OntDPropertyImpl(Resource inModel) {
        super(inModel);
    }

    public OntDPropertyImpl(Node n, EnhGraph g) {
        super(n, g);
    }

    @Override
    public Class<? extends OntEntity> getActualClass() {
        return OntDPEntity.class;
    }

    @Override
    public Resource getRDFType() {
        return OWL.DatatypeProperty;
    }

    @Override
    public Stream<OntCE> domain() {
        return getModel().classExpressions(this, RDFS.domain);
    }

    @Override
    public Stream<OntDR> range() {
        return getModel().dataRanges(this, RDFS.range);
    }
}
