package ru.avicomp.ontapi.jena.model;

/**
 * OWL-Entity here.
 * TODO:
 * Created by szuev on 01.11.2016.
 */
public interface OntEntity extends OntObject {

    default boolean isClass() {
        return false;
    }

    default boolean isProperty() {
        return false;
    }

    default boolean isAnnotationProperty() {
        return false;
    }

    default boolean isDataProperty() {
        return false;
    }

    default boolean isObjectProperty() {
        return false;
    }

    default boolean isDatatype() {
        return false;
    }

    default boolean isIndividual() {
        return false;
    }

    @Override
    default boolean isEntity() {
        return true;
    }

    /**
     * Determines is resource local defined.
     *
     * @return true if this resource is local to the base graph.
     */
    boolean isLocal();
}
