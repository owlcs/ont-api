package ru.avicomp.ontapi.jena.impl;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import ru.avicomp.ontapi.OntException;
import ru.avicomp.ontapi.jena.model.OntEntity;

/**
 * Entity.
 * Created by szuev on 03.11.2016.
 */
abstract class OntEntityImpl extends OntObjectImpl implements OntEntity {

    public static OntConfiguration.OntSimpleObjectFactory classFactory = new OntConfiguration.OntSimpleObjectFactory(OntClassEntityImpl.class, OWL.Class, null);
    public static OntConfiguration.OntSimpleObjectFactory aPFactory = new OntConfiguration.OntSimpleObjectFactory(OntAPropertyImpl.class, OWL.AnnotationProperty, true);
    public static OntConfiguration.OntSimpleObjectFactory dPFactory = new OntConfiguration.OntSimpleObjectFactory(OntDPropertyImpl.class, OWL.DatatypeProperty, true);
    public static OntConfiguration.OntSimpleObjectFactory oPFactory = new OntConfiguration.OntSimpleObjectFactory(OntOPropertyImpl.class, OWL.ObjectProperty, true);
    public static OntConfiguration.OntSimpleObjectFactory datatypeFactory = new OntConfiguration.OntSimpleObjectFactory(OntDatatypeImpl.class, RDFS.Datatype, true);
    public static OntConfiguration.OntSimpleObjectFactory individualFactory = new OntConfiguration.OntSimpleObjectFactory(OntNamedIndividualImpl.class, OWL2.NamedIndividual, true);

    public static OntConfiguration.OntMultiObjectFactory abstractEntityFactory =
            new OntConfiguration.OntMultiObjectFactory(classFactory, aPFactory, dPFactory, oPFactory, datatypeFactory, individualFactory);

    OntEntityImpl(Resource inModel) {
        super(checkEntityResource(inModel));
    }

    OntEntityImpl(Node n, EnhGraph g) {
        super(n, g);
    }

    @Override
    public boolean isLocal() {
        return getModel().isInBaseModel(this, RDF.type, getRDFType());
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", getURI(), getActualClass().getSimpleName());
    }

    private static Resource checkEntityResource(Resource res) {
        if (OntException.notNull(res, "Null resource").isURIResource()) {
            return res;
        }
        throw new OntException("Not uri resource " + res);
    }

    public abstract Class<? extends OntEntity> getActualClass();

    public abstract Resource getRDFType();

}
