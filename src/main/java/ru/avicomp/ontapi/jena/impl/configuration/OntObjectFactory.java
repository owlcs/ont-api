package ru.avicomp.ontapi.jena.impl.configuration;

import java.util.stream.Stream;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.enhanced.EnhNode;
import org.apache.jena.enhanced.Implementation;
import org.apache.jena.graph.Node;

import ru.avicomp.ontapi.OntException;

/**
 * Base for any OntObject factories.
 * Used to bind implementation and interface.
 * <p>
 * Created by @szuev on 03.11.2016.
 */
public abstract class OntObjectFactory extends Implementation {
    public EnhNode create(Node node, EnhGraph eg) {
        throw new OntException("Creation is not allowed: " + node);
    }

    public Stream<EnhNode> find(EnhGraph eg) {
        return Stream.empty();
    }
}
