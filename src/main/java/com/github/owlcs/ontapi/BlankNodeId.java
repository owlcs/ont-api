package com.github.owlcs.ontapi;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;

import java.io.Serializable;

public record BlankNodeId(String label) implements AsNode, Serializable {

    public static BlankNodeId of(Node node) {
        if (node.isBlank()) {
            return new BlankNodeId(node.getBlankNodeLabel());
        }
        throw new IllegalArgumentException();
    }

    public static BlankNodeId of(String id) {
        return new BlankNodeId(id);
    }

    public static BlankNodeId of() {
        return new BlankNodeId(org.apache.jena.graph.BlankNodeId.createFreshId());
    }

    @Override
    public Node asNode() {
        return NodeFactory.createBlankNode(label);
    }

    @Override
    public String toString() {
        return label;
    }

    @Override
    public int hashCode() {
        return label.hashCode();
    }
}
