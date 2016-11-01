package ru.avicomp.ontapi.jena.model;

import java.util.stream.Stream;

import org.apache.jena.rdf.model.Resource;

/**
 * Annotation Property resource.
 * <p>
 * Created by szuev on 01.11.2016.
 */
public interface OntAPEntity extends OntPropertyEntity {
    @Override
    default boolean isAnnotationProperty() {
        return true;
    }

    @Override
    Stream<Resource> getDomain();

    @Override
    Stream<Resource> getRange();
}
