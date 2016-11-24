package ru.avicomp.ontapi.jena.impl.configuration;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;

import ru.avicomp.ontapi.jena.OntJenaException;

/**
 * To filter resources.
 * Used in factory ({@link CommonOntObjectFactory}).
 * <p>
 * Created by szuev on 07.11.2016.
 */
@FunctionalInterface
public interface OntFilter {
    OntFilter URI = new Named(true);
    OntFilter BLANK = new Named(false);

    boolean test(Node n, EnhGraph g);

    default OntFilter and(OntFilter other) {
        OntJenaException.notNull(other, "Null and-filter.");
        return (Node t, EnhGraph u) -> test(t, u) && other.test(t, u);
    }

    default OntFilter or(OntFilter other) {
        OntJenaException.notNull(other, "Null or-filter.");
        return (Node t, EnhGraph u) -> test(t, u) || other.test(t, u);
    }

    default OntFilter accumulate(OntFilter... filters) {
        OntFilter res = this;
        for (OntFilter o : filters) {
            res = res.and(o);
        }
        return res;
    }

    class Named implements OntFilter {
        private final Boolean named;

        public Named(Boolean named) {
            this.named = named;
        }

        @Override
        public boolean test(Node node, EnhGraph enhGraph) {
            return named == null || named && node.isURI() || !named && node.isBlank();
        }
    }

    class HasPredicate implements OntFilter {
        protected final Node predicate;

        public HasPredicate(Property predicate) {
            this.predicate = OntJenaException.notNull(predicate, "Null predicate.").asNode();
        }

        @Override
        public boolean test(Node n, EnhGraph g) {
            return g.asGraph().contains(n, predicate, Node.ANY);
        }
    }

    class HasType implements OntFilter {
        protected final Node type;

        public HasType(Resource type) {
            this.type = OntJenaException.notNull(type, "Null type.").asNode();
        }

        @Override
        public boolean test(Node node, EnhGraph eg) {
            return eg.asGraph().contains(node, RDF.type.asNode(), type);
        }
    }

    class OneOf implements OntFilter {
        protected final Set<Node> nodes;

        public OneOf(Collection<Resource> types) {
            nodes = types.stream().map(RDFNode::asNode).collect(Collectors.toSet());
        }

        @Override
        public boolean test(Node n, EnhGraph g) {
            return nodes.contains(n);
        }
    }
}
