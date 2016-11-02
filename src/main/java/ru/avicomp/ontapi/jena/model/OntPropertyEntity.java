package ru.avicomp.ontapi.jena.model;

import java.util.stream.Stream;

import org.apache.jena.rdf.model.Resource;

/**
 * common interface for three types of property: Annotation, Data and Object.
 * <p>
 * Created by szuev on 01.11.2016.
 */
public interface OntPropertyEntity extends OntEntity {

    Stream<? extends Resource> domain();

    Stream<? extends Resource> range();
}
