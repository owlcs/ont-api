package ru.avicomp.ontapi.jena.impl;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;

import ru.avicomp.ontapi.jena.model.OntDT;

/**
 * rdfs:Datatype
 *
 * Created by szuev on 03.11.2016.
 */
public class OntDatatypeImpl extends OntEntityImpl implements OntDT {

    public OntDatatypeImpl(Node n, EnhGraph g) {
        super(OntObjectImpl.checkNamed(n), g);
    }

    @Override
    public Class<OntDT> getActualClass() {
        return OntDT.class;
    }

    @Override
    public boolean isBuiltIn() {
        return BUILT_IN_DATATYPES.contains(this);
    }
}
