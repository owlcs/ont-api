package ru.avicomp.ontapi.jena.impl.configuration;

import java.util.function.Function;
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
    OntFinder ANY_SUBJECT = eg -> JenaUtils.asStream(eg.asGraph().find(Node.ANY, Node.ANY, Node.ANY).mapWith(Triple::getSubject));
    OntFinder ANY_SUBJECT_AND_OBJECT = eg -> JenaUtils.asStream(eg.asGraph().find(Node.ANY, Node.ANY, Node.ANY))
            .map(t -> Stream.of(t.getSubject(), t.getObject()))
            .flatMap(Function.identity()).distinct();
    OntFinder ANYTHING = eg -> JenaUtils.asStream(eg.asGraph().find(Node.ANY, Node.ANY, Node.ANY))
            .map(t -> Stream.of(t.getSubject(), t.getPredicate(), t.getObject()))
            .flatMap(Function.identity()).distinct();
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
