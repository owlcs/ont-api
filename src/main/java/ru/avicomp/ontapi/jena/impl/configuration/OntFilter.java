package ru.avicomp.ontapi.jena.impl.configuration;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Property;

import ru.avicomp.ontapi.OntException;

/**
 * To filter resources.
 * Used in factory.
 * <p>
 * Created by szuev on 07.11.2016.
 */
@FunctionalInterface
public interface OntFilter {
    boolean test(Node n, EnhGraph g);

    default OntFilter and(OntFilter other) {
        OntException.notNull(other, "Null and-filter.");
        return (Node t, EnhGraph u) -> test(t, u) && other.test(t, u);
    }

    default OntFilter or(OntFilter other) {
        OntException.notNull(other, "Null or-filter.");
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

    class Predicate implements OntFilter {
        private final Node predicate;

        public Predicate(Property predicate) {
            this.predicate = OntException.notNull(predicate, "Null predicate.").asNode();
        }

        @Override
        public boolean test(Node n, EnhGraph g) {
            return g.asGraph().contains(n, predicate, Node.ANY);
        }
    }
}
