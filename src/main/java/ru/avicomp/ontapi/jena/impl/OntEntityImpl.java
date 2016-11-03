package ru.avicomp.ontapi.jena.impl;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import ru.avicomp.ontapi.OntException;
import ru.avicomp.ontapi.jena.model.*;

/**
 * Entity.
 * Created by szuev on 03.11.2016.
 */
abstract class OntEntityImpl extends OntObjectImpl implements OntEntity {

    OntEntityImpl(Resource r) {
        super(checkEntityResource(r));
    }

    @Override
    public boolean isLocal() {
        return getModel().isInBaseModel(this, RDF.type, getRDFType(getOntType()));
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", getURI(), getOntType());
    }

    private static Resource checkEntityResource(Resource res) {
        if (OntException.notNull(res, "Null resource").isURIResource()) {
            return res;
        }
        throw new OntException("Not uri resource " + res);
    }

    static <T extends OntEntity> T wrapEntity(Class<T> view, Resource r) {
        return view.cast(getInstance(view, r));
    }

    private static OntEntityImpl getInstance(Class<? extends OntEntity> view, Resource r) {
        switch (Type.valueOf(view)) {
            case CLASS:
                return new OntClassImpl(r);
            case ANNOTATION_PROPERTY:
                return new OntAPropertyImpl(r);
            case DATA_PROPERTY:
                return new OntDPropertyImpl(r);
            case OBJECT_PROPERTY:
                return new OntOPropertyImpl(r);
            case DATATYPE:
                return new OntDatatypeImpl(r);
            case INDIVIDUAL:
                return new OntNamedIndividualImpl(r);
            default:
                throw new OntException("Unsupported entity type " + view);
        }
    }

    static Resource getRDFType(Class<? extends OntEntity> view) {
        return getRDFType(Type.valueOf(view));
    }

    private static Resource getRDFType(Type t) {
        switch (t) {
            case CLASS:
                return OWL.Class;
            case ANNOTATION_PROPERTY:
                return OWL.AnnotationProperty;
            case DATA_PROPERTY:
                return OWL.DatatypeProperty;
            case OBJECT_PROPERTY:
                return OWL.ObjectProperty;
            case DATATYPE:
                return RDFS.Datatype;
            case INDIVIDUAL:
                return OWL2.NamedIndividual;
            default:
                throw new OntException("Unsupported entity type " + t);
        }
    }

    abstract Type getOntType();

    enum Type {
        CLASS(OntClassEntity.class),
        ANNOTATION_PROPERTY(OntAPEntity.class),
        DATA_PROPERTY(OntDPEntity.class),
        OBJECT_PROPERTY(OntOPEntity.class),
        DATATYPE(OntDatatypeEntity.class),
        INDIVIDUAL(OntIndividualEntity.class),;

        Type(Class<? extends OntEntity> view) {
            this.view = view;
        }

        public Class<? extends OntEntity> getView() {
            return view;
        }

        private Class<? extends OntEntity> view;

        public static Type valueOf(Class<? extends OntEntity> view) {
            OntException.notNull(view, "Null view.");
            for (Type t : values()) {
                if (t.view.equals(view)) return t;
            }
            throw new OntException("Unsupported entity type " + view);
        }

    }
}
