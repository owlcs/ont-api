package ru.avicomp.ontapi.jena.impl;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.vocabulary.OWL;

import ru.avicomp.ontapi.jena.impl.configuration.*;

/**
 * Property Expression base class.
 * <p>
 * Created by @szuev on 08.11.2016.
 */
public abstract class OntPEImpl extends OntObjectImpl {

    public static OntObjectFactory inversePropertyFactory = new CommonOntObjectFactory(new OntMaker.Default(OntOPEImpl.InverseProperty.class),
            new OntFinder.ByPredicate(OWL.inverseOf), new OntOPEImpl.InverseProperty.Filter(false));
    public static OntObjectFactory inversePropertyFactoryStrict = new CommonOntObjectFactory(new OntMaker.Default(OntOPEImpl.InverseProperty.class),
            new OntFinder.ByPredicate(OWL.inverseOf), new OntOPEImpl.InverseProperty.Filter(true));

    public static MultiOntObjectFactory abstractOPEFactory = new MultiOntObjectFactory(OntFinder.TYPED,
            OntEntityImpl.objectPropertyFactory,
            inversePropertyFactory);
    public static MultiOntObjectFactory abstractOPEFactoryStrict = new MultiOntObjectFactory(OntFinder.TYPED,
            OntEntityImpl.objectPropertyFactoryStrict,
            inversePropertyFactoryStrict);

    public static OntObjectFactory abstractNamedPropertyFactory = new MultiOntObjectFactory(OntFinder.TYPED,
            OntEntityImpl.objectPropertyFactory, OntEntityImpl.dataPropertyFactory, OntEntityImpl.annotationPropertyFactory);
    public static OntObjectFactory abstractNamedPropertyFactoryStrict = new MultiOntObjectFactory(OntFinder.TYPED,
            OntEntityImpl.objectPropertyFactoryStrict, OntEntityImpl.dataPropertyFactoryStrict, OntEntityImpl.annotationPropertyFactoryStrict);

    public static OntObjectFactory abstractPEFactory =
            new MultiOntObjectFactory(OntFinder.TYPED, abstractNamedPropertyFactory, inversePropertyFactory);
    public static OntObjectFactory abstractPEFactoryStrict =
            new MultiOntObjectFactory(OntFinder.TYPED, abstractNamedPropertyFactoryStrict, inversePropertyFactoryStrict);

    public OntPEImpl(Node n, EnhGraph m) {
        super(n, m);
    }
}
