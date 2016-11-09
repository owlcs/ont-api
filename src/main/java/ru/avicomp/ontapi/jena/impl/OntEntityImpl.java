package ru.avicomp.ontapi.jena.impl;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import ru.avicomp.ontapi.OntException;
import ru.avicomp.ontapi.jena.impl.configuration.MultiOntObjectFactory;
import ru.avicomp.ontapi.jena.impl.configuration.OntFilter;
import ru.avicomp.ontapi.jena.impl.configuration.OntObjectFactory;
import ru.avicomp.ontapi.jena.impl.configuration.TypedOntObjectFactory;
import ru.avicomp.ontapi.jena.model.OntEntity;

/**
 * Entity.
 * Created by szuev on 03.11.2016.
 */
public abstract class OntEntityImpl extends OntObjectImpl implements OntEntity {

    public static OntObjectFactory classFactory = new TypedOntObjectFactory(OntClassImpl.class, OWL2.Class, OntFilter.URI);
    public static OntObjectFactory annotationPropertyFactory = new TypedOntObjectFactory(OntAPropertyImpl.class, OWL2.AnnotationProperty, OntFilter.URI);
    public static OntObjectFactory dataPropertyFactory = new TypedOntObjectFactory(OntDPropertyImpl.class, OWL2.DatatypeProperty, OntFilter.URI);
    public static OntObjectFactory objectPropertyFactory = new TypedOntObjectFactory(OntOPEImpl.NamedProperty.class, OWL2.ObjectProperty, OntFilter.URI);
    public static OntObjectFactory datatypeFactory = new TypedOntObjectFactory(OntDatatypeImpl.class, RDFS.Datatype, OntFilter.URI);
    public static OntObjectFactory individualFactory = new TypedOntObjectFactory(OntIndividualImpl.NamedIndividual.class, OWL2.NamedIndividual, OntFilter.URI);

    public static OntObjectFactory abstractEntityFactory =
            new MultiOntObjectFactory(classFactory, annotationPropertyFactory, dataPropertyFactory, objectPropertyFactory, datatypeFactory, individualFactory);

    public OntEntityImpl(Node n, EnhGraph g) {
        super(n, g);
    }

    @Override
    public boolean isLocal() {
        return getModel().isInBaseModel(this, RDF.type, getRDFType());
    }

    static Node checkNamed(Node res) {
        if (OntException.notNull(res, "Null node").isURI()) {
            return res;
        }
        throw new OntException("Not uri node " + res);
    }

    public abstract Class<? extends OntEntity> getActualClass();

    public abstract Resource getRDFType();

}
