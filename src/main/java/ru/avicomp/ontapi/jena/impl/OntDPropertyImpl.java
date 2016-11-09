package ru.avicomp.ontapi.jena.impl;

import java.util.stream.Stream;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDFS;

import ru.avicomp.ontapi.jena.model.OntCE;
import ru.avicomp.ontapi.jena.model.OntDProperty;
import ru.avicomp.ontapi.jena.model.OntDR;

/**
 * owl:DatatypeProperty
 * Created by szuev on 03.11.2016.
 */
public class OntDPropertyImpl extends OntEntityImpl implements OntDProperty {

    public OntDPropertyImpl(Node n, EnhGraph g) {
        super(OntEntityImpl.checkNamed(n), g);
    }

    @Override
    public Class<OntDProperty> getActualClass() {
        return OntDProperty.class;
    }

    @Override
    public Resource getRDFType() {
        return OWL2.DatatypeProperty;
    }

    @Override
    public Stream<OntCE> domain() {
        return getModel().classExpressions(this, RDFS.domain);
    }

    @Override
    public Stream<OntDR> range() {
        return getModel().dataRanges(this, RDFS.range);
    }

    @Override
    public void setFunctional(boolean functional) {
        changeType(OWL2.FunctionalProperty, functional);
    }

    @Override
    public boolean isFunctional() {
        return hasType(OWL2.FunctionalProperty);
    }
}
