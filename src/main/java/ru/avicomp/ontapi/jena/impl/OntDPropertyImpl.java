package ru.avicomp.ontapi.jena.impl;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;

import ru.avicomp.ontapi.jena.JenaUtils;
import ru.avicomp.ontapi.jena.model.OntIndividual;
import ru.avicomp.ontapi.jena.model.OntNDP;
import ru.avicomp.ontapi.jena.model.OntNPA;
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
    public OntNPA.DataAssertion addNegativeAssertion(OntIndividual source, Literal target) {
        return OntNPAImpl.create(getModel(), source, this, target);
    }

    @Override
    public void setFunctional(boolean functional) {
        changeType(OWL2.FunctionalProperty, functional);
    }

    @Override
    public boolean isBuiltIn() {
        return JenaUtils.BUILT_IN_DATA_PROPERTIES.contains(this);
    }

    @Override
    public Property inModel(Model m) {
        return getModel() == m ? this : m.createProperty(getURI());
    }

    @Override
    public OntStatement getRoot() {
        return getRoot(RDF.type, OWL2.DatatypeProperty);
    }

}
