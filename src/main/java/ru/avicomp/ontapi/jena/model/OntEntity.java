package ru.avicomp.ontapi.jena.model;

/**
 * OWL-Entity here.
 * TODO:
 * Created by szuev on 01.11.2016.
 */
public interface OntEntity extends OntObject {

    /**
     * Determines is entity-resource local defined.
     *
     * @return true if this resource is local to the base graph.
     */
    boolean isLocal();

    @Override
    Type getOntType();

    enum Type implements OntType {
        CLASS,
        ANNOTATION_PROPERTY,
        DATA_PROPERTY,
        OBJECT_PROPERTY,
        DATATYPE,
        INDIVIDUAL,;

        @Override
        public boolean isEntity() {
            return true;
        }
    }
}
