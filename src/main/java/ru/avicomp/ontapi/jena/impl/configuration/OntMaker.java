package ru.avicomp.ontapi.jena.impl.configuration;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;

import ru.avicomp.ontapi.OntException;

/**
 * To make some preparation while creating.
 * Used in factory.
 * <p>
 * Created by szuev on 07.11.2016.
 */
@FunctionalInterface
public interface OntMaker {
    OntMaker UNSUPPORTED = (n, g) -> {
        throw new OntException("Creation is not allowed for node " + n);
    };

    void prepare(Node node, EnhGraph eg);
}
