package ru.avicomp.ontapi.jena.model;

import java.util.stream.Stream;

import org.apache.jena.rdf.model.Resource;

/**
 * Base Resource.
 *
 * Created by szuev on 01.11.2016.
 */
public interface OntObject extends Resource {

    Stream<Resource> types();

}
