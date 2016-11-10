package ru.avicomp.ontapi.jena.impl;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;

import ru.avicomp.ontapi.jena.impl.configuration.*;

/**
 * Property Expression base class.
 * <p>
 * Created by @szuev on 08.11.2016.
 */
public abstract class OntPEImpl extends OntObjectImpl {

    public static OntObjectFactory inversePropertyFactory = new CommonOntObjectFactory(new OntMaker.Default(OntOPEImpl.InverseProperty.class),
            new OntOPEImpl.InverseProperty.Finder(), new OntOPEImpl.InverseProperty.Filter());
    public static OntObjectFactory abstractOPEFactory = new MultiOntObjectFactory(OntFinder.TYPED,
            OntEntityImpl.objectPropertyFactory,
            inversePropertyFactory);
    public static OntObjectFactory abstractPEFactory = new MultiOntObjectFactory(OntFinder.TYPED,
            OntEntityImpl.objectPropertyFactory,
            OntEntityImpl.dataPropertyFactory, OntEntityImpl.annotationPropertyFactory, inversePropertyFactory);

    OntPEImpl(Node n, EnhGraph m) {
        super(n, m);
    }
}
