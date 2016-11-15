package ru.avicomp.ontapi.jena.impl;

import java.util.stream.Stream;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import ru.avicomp.ontapi.OntException;
import ru.avicomp.ontapi.jena.model.OntCE;
import ru.avicomp.ontapi.jena.model.OntClass;
import ru.avicomp.ontapi.jena.model.OntIndividual;

/**
 * owl:Class
 * Created by szuev on 03.11.2016.
 */
public class OntClassImpl extends OntEntityImpl implements OntClass {

    public OntClassImpl(Node n, EnhGraph eg) {
        super(OntObjectImpl.checkNamed(n), eg);
    }

    @Override
    public boolean isBuiltIn() {
        return BUILT_IN_CLASSES.contains(this);
    }

    @Override
    public Class<OntClass> getActualClass() {
        return OntClass.class;
    }

    @Override
    public Resource getRDFType() {
        return OWL2.Class;
    }

    @Override
    public Stream<OntCE> subClassOf() {
        return getModel().classExpressions(this, RDFS.subClassOf);
    }

    @Override
    public OntIndividual.Anonymous createIndividual() {
        Resource res = getModel().createResource();
        getModel().add(res, RDF.type, this);
        return new OntIndividualImpl.AnonymousImpl(res.asNode(), getModel());
    }

    @Override
    public OntIndividual.Named createIndividual(String uri) {
        Resource res = getModel().createResource(OntException.notNull(uri, "Null uri"));
        getModel().add(res, RDF.type, this);
        getModel().add(res, RDF.type, OWL2.NamedIndividual);
        return new OntIndividualImpl.NamedImpl(res.asNode(), getModel());
    }
}
