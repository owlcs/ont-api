package ru.avicomp.ontapi.jena.impl.configuration;

import java.util.stream.Stream;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;

/**
 * To search resources in model
 * Used in factory.
 * <p>
 * Created by szuev on 07.11.2016.
 */
@FunctionalInterface
public interface Finder {
    Stream<Node> find(EnhGraph eg);
}
