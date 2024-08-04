package com.github.owlcs.ontapi.internal;

import org.apache.jena.graph.Node;
import org.apache.jena.ontapi.model.OntAnnotationProperty;
import org.apache.jena.ontapi.model.OntClass;
import org.apache.jena.ontapi.model.OntDataProperty;
import org.apache.jena.ontapi.model.OntDataRange;
import org.apache.jena.ontapi.model.OntEntity;
import org.apache.jena.ontapi.model.OntIndividual;
import org.apache.jena.ontapi.model.OntObjectProperty;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDFS;

import java.util.Objects;
import java.util.Optional;

public enum OWLEntity {
    CLASS(OWL.Class, OntClass.Named.class),
    DATATYPE(RDFS.Datatype, OntDataRange.Named.class),
    ANNOTATION_PROPERTY(OWL.AnnotationProperty, OntAnnotationProperty.class),
    DATA_PROPERTY(OWL.DatatypeProperty, OntDataProperty.class),
    OBJECT_PROPERTY(OWL.ObjectProperty, OntObjectProperty.Named.class),
    INDIVIDUAL(OWL.NamedIndividual, OntIndividual.Named.class),
    ;

    final Class<? extends OntEntity> classType;
    final Resource resourceType;

    OWLEntity(Resource resourceType, Class<? extends OntEntity> classType) {
        this.classType = classType;
        this.resourceType = resourceType;
    }

    public static Optional<OWLEntity> find(Resource type) {
        return find(type.asNode());
    }

    public static Optional<OWLEntity> find(Node type) {
        for (OWLEntity e : values()) {
            if (Objects.equals(e.getResourceType().asNode(), type)) return Optional.of(e);
        }
        return Optional.empty();
    }

    public Class<? extends OntEntity> getOntType() {
        return classType;
    }

    public Resource getResourceType() {
        return resourceType;
    }

}
