package ru.avicomp.ontapi.jena.model;

/**
 * Object type.
 * <p>
 * Created by @szuev on 02.11.2016.
 */
public interface OntType {
    default boolean isCE() {
        return false;
    }

    default boolean isDR() {
        return false;
    }

    default boolean isEntity() {
        return false;
    }
}
