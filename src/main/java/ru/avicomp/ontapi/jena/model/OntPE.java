package ru.avicomp.ontapi.jena.model;

import java.util.stream.Stream;

import org.apache.jena.rdf.model.Resource;

/**
 * Common interface for any Property Expressions (DataProperty, ObjectProperty(Entity and InverseOf), AnnotationProperty).
 * See for example <a href='https://www.w3.org/TR/owl2-quick-reference/'>2.2 Properties</a>
 *
 * Created by @szuev on 02.11.2016.
 */
public interface OntPE extends OntObject {

    Stream<? extends Resource> domain();

    Stream<? extends Resource> range();
}
