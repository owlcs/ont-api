package ru.avicomp.ontapi.jena.impl.configuration;

import java.util.stream.Stream;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;

import ru.avicomp.ontapi.OntException;
import ru.avicomp.ontapi.jena.JenaUtils;

/**
 * To perform the preliminary search resources in model,
 * then the result stream will be filtered by {@link OntFilter}
 * Used in factory ({@link CommonOntObjectFactory}).
 * <p>
 * Created by szuev on 07.11.2016.
 */
@FunctionalInterface
public interface OntFinder {
    OntFinder ANYTHING = eg -> JenaUtils.asStream(eg.asGraph().find(Node.ANY, Node.ANY, Node.ANY).mapWith(Triple::getSubject));
    OntFinder TYPED = new ByPredicate(RDF.type);

    Stream<Node> find(EnhGraph eg);

    class ByType implements OntFinder {
        protected final Node type;

        public ByType(Resource type) {
            this.type = OntException.notNull(type, "Null type.").asNode();
        }

        @Override
        public Stream<Node> find(EnhGraph eg) {
            return JenaUtils.asStream(eg.asGraph().find(Node.ANY, RDF.type.asNode(), type).mapWith(Triple::getSubject));
        }
    }

    class ByPredicate implements OntFinder {
        protected final Node predicate;

        public ByPredicate(Property predicate) {
            this.predicate = OntException.notNull(predicate, "Null predicate.").asNode();
        }

        @Override
        public Stream<Node> find(EnhGraph eg) {
            return JenaUtils.asStream(eg.asGraph().find(Node.ANY, predicate, Node.ANY).mapWith(Triple::getSubject));
        }
    }
}
