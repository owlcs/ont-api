package ru.avicomp.ontapi.jena.impl;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.vocabulary.RDFS;

import ru.avicomp.ontapi.jena.model.OntDT;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

/**
 * Named entity with rdf:type = rdfs:Datatype
 *
 * Created by szuev on 03.11.2016.
 */
public class OntDatatypeImpl extends OntObjectImpl implements OntDT {

    public OntDatatypeImpl(Node n, EnhGraph g) {
        super(OntObjectImpl.checkNamed(n), g);
    }

    @Override
    public Class<OntDT> getActualClass() {
        return OntDT.class;
    }

    @Override
    public boolean isBuiltIn() {
        return Entities.DATATYPE.builtInURIs().contains(this);
    }

    @Override
    public OntStatement getRoot() {
        return getRoot(RDF.type, RDFS.Datatype);
    }

}
