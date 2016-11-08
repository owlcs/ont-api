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
    private static final OntFilter.Named URI_FILTER = new OntFilter.Named(true);

    public static OntObjectFactory classFactory = new TypedOntObjectFactory(OntClassEntityImpl.class, OWL2.Class, URI_FILTER);
    public static OntObjectFactory annotationPropertyFactory = new TypedOntObjectFactory(OntAPropertyImpl.class, OWL2.AnnotationProperty, URI_FILTER);
    public static OntObjectFactory dataPropertyFactory = new TypedOntObjectFactory(OntDPropertyImpl.class, OWL2.DatatypeProperty, URI_FILTER);
    public static OntObjectFactory objectPropertyFactory = new TypedOntObjectFactory(OntOPropertyImpl.class, OWL2.ObjectProperty, URI_FILTER);
    public static OntObjectFactory datatypeFactory = new TypedOntObjectFactory(OntDatatypeImpl.class, RDFS.Datatype, URI_FILTER);
    public static OntObjectFactory individualFactory = new TypedOntObjectFactory(OntNamedIndividualImpl.class, OWL2.NamedIndividual, URI_FILTER);

    public static OntObjectFactory abstractEntityFactory =
            new MultiOntObjectFactory(classFactory, annotationPropertyFactory, dataPropertyFactory, objectPropertyFactory, datatypeFactory, individualFactory);

    OntEntityImpl(Resource inModel) {
        super(checkEntityResource(inModel));
    }

    public OntEntityImpl(Node n, EnhGraph g) {
        super(n, g);
    }

    @Override
    public boolean isLocal() {
        return getModel().isInBaseModel(this, RDF.type, getRDFType());
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
