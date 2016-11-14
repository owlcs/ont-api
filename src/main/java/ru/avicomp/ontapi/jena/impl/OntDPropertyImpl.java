package ru.avicomp.ontapi.jena.impl;

import java.util.stream.Stream;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDFS;

import ru.avicomp.ontapi.jena.model.OntCE;
import ru.avicomp.ontapi.jena.model.OntDR;
import ru.avicomp.ontapi.jena.model.OntNDP;
import ru.avicomp.ontapi.jena.model.OntStatement;

/**
 * owl:DatatypeProperty
 * Created by szuev on 03.11.2016.
 */
public class OntDPropertyImpl extends OntEntityImpl implements OntNDP {

    public OntDPropertyImpl(Node n, EnhGraph g) {
        super(OntObjectImpl.checkNamed(n), g);
    }

    @Override
    public Class<OntNDP> getActualClass() {
        return OntNDP.class;
    }

    @Override
    public Resource getRDFType() {
        return OWL2.DatatypeProperty;
    }

    @Override
    public OntStatement addDomain(OntCE domain) {
        return addStatement(RDFS.domain, domain);
    }

    @Override
    public OntStatement addRange(OntDR range) {
        return addStatement(RDFS.range, range);
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

    @Override
    public boolean isBuiltIn() {
        return BUILT_IN_DATA_PROPERTIES.contains(this);
    }

    @Override
    public Property inModel(Model m) {
        return getModel() == m ? this : m.createProperty(getURI());
    }
}
