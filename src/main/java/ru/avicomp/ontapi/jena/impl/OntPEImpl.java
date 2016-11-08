package ru.avicomp.ontapi.jena.impl;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;

import ru.avicomp.ontapi.jena.impl.configuration.CommonOntObjectFactory;
import ru.avicomp.ontapi.jena.impl.configuration.MultiOntObjectFactory;
import ru.avicomp.ontapi.jena.impl.configuration.OntMaker;
import ru.avicomp.ontapi.jena.impl.configuration.OntObjectFactory;

/**
 * Property Expression Implementation.
 * <p>
 * Created by @szuev on 08.11.2016.
 */
public abstract class OntPEImpl extends OntObjectImpl {

    public static OntObjectFactory inversePropertyFactory = new CommonOntObjectFactory(OntOPEImpl.InverseProperty.class,
            OntMaker.UNSUPPORTED, new OntOPEImpl.InverseProperty.Finder(), new OntOPEImpl.InverseProperty.Filter());
    public static OntObjectFactory abstractOPEFactory = new MultiOntObjectFactory(OntEntityImpl.objectPropertyFactory,
            inversePropertyFactory);
    public static OntObjectFactory abstractPEFactory = new MultiOntObjectFactory(OntEntityImpl.objectPropertyFactory,
            OntEntityImpl.dataPropertyFactory, OntEntityImpl.annotationPropertyFactory, inversePropertyFactory);

    OntPEImpl(Node n, EnhGraph m) {
        super(n, m);
    }
}
