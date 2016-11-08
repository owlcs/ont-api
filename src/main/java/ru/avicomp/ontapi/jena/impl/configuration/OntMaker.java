package ru.avicomp.ontapi.jena.impl.configuration;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;

/**
 * To make some preparation while creating.
 * Used in factory.
 * <p>
 * Created by szuev on 07.11.2016.
 */
@FunctionalInterface
public interface OntMaker {
    void prepare(Node node, EnhGraph eg);
}
