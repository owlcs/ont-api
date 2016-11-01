package ru.avicomp.ontapi.jena.model;

/**
 * OWLClass
 * <p>
 * Created by szuev on 01.11.2016.
 */
public interface OntClassEntity extends OntObjectEntity, OntCE {
    @Override
    default boolean isClass() {
        return true;
    }
}
