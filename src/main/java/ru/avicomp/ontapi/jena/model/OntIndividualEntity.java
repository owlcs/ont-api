package ru.avicomp.ontapi.jena.model;

/**
 * Named Individual here.
 * <p>
 * Created by szuev on 01.11.2016.
 */
public interface OntIndividualEntity extends OntObjectEntity {
    @Override
    default boolean isIndividual() {
        return true;
    }
}
