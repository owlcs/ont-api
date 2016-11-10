package ru.avicomp.ontapi.jena.model;

import java.util.stream.Stream;

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

/**
 * Base Resource.
 *
 * Created by szuev on 01.11.2016.
 */
public interface OntObject extends Resource {

    /**
     * Gets annotation values for specified annotation property.
     * According to OWL2-DL specification OntObject should be an uri-resource (i.e. not anonymous)
     * It is placed here for generality.
     *
     * @param property Annotation Property
     * @return Stream of {@link RDFNode}, each of them could be resource-uri, anonymous individual, or literal
     * @throws ru.avicomp.ontapi.OntException in case this is not uri-resource.
     */
    Stream<RDFNode> annotationAssertions(OntNAP property);
}
