package ru.avicomp.ontapi.jena.model;

import org.apache.jena.rdf.model.Resource;

/**
 * Base Resource.
 * TODO:
 * Created by szuev on 01.11.2016.
 */
public interface OntObject extends Resource {
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
