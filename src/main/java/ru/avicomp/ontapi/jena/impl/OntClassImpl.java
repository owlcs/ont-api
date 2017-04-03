package ru.avicomp.ontapi.jena.impl;

import java.util.Collection;
import java.util.stream.Stream;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;

import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.utils.BuiltIn;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

/**
 * owl:Class Implementation
 *
 * Created by szuev on 03.11.2016.
 */
public class OntClassImpl extends OntObjectImpl implements OntClass {

    public OntClassImpl(Node n, EnhGraph eg) {
        super(OntObjectImpl.checkNamed(n), eg);
    }

    @Override
    public boolean isBuiltIn() {
        return BuiltIn.CLASSES.contains(this);
    }

    @Override
    public Class<OntClass> getActualClass() {
        return OntClass.class;
    }

    @Override
    public OntIndividual.Anonymous createIndividual() {
        return OntCEImpl.createAnonymousIndividual(getModel(), this);
    }

    @Override
    public OntIndividual.Named createIndividual(String uri) {
        return OntCEImpl.createNamedIndividual(getModel(), this, uri);
    }

    @Override
    public OntStatement addHasKey(Collection<OntOPE> objectProperties, Collection<OntNDP> dataProperties) {
        return OntCEImpl.addHasKey(this, objectProperties, dataProperties);
    }

    @Override
    public void removeHasKey() {
        clearAll(OWL.hasKey);
    }

    @Override
    public Stream<OntPE> hasKey() {
        return rdfListMembers(OWL.hasKey, OntPE.class);
    }

    @Override
    public OntStatement addDisjointUnionOf(Collection<OntCE> classes) {
        return addStatement(OWL.disjointUnionOf, getModel().createList(OntJenaException.notNull(classes, "Null classes collection.").iterator()));
    }

    @Override
    public void removeDisjointUnionOf() {
        clearAll(OWL.disjointUnionOf);
    }

    @Override
    public Stream<OntCE> disjointUnionOf() {
        return rdfListMembers(OWL.disjointUnionOf, OntCE.class);
    }

    @Override
    public OntStatement getRoot() {
        return getRoot(RDF.type, OWL.Class);
    }
}
