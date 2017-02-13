package ru.avicomp.ontapi.translators;

import java.util.*;
import java.util.stream.Stream;

import org.apache.jena.graph.Factory;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Triple;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLObject;

import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntStatement;

/**
 * Base class for any Axiom Graph Translator (operator 'T').
 * Specification: <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Mapping_from_the_Structural_Specification_to_RDF_Graphs'>2.1 Translation of Axioms without Annotations</a>
 * <p>
 * Created by @szuev on 28.09.2016.
 */
public abstract class AxiomTranslator<Axiom extends OWLAxiom> {
    private AxiomParserProvider.Config config = AxiomParserProvider.DEFAULT_CONFIG;

    /**
     * writes axiom to model.
     *
     * @param axiom {@link OWLAxiom}
     * @param model {@link OntGraphModel}
     */
    public abstract void write(Axiom axiom, OntGraphModel model);

    /**
     * reads axioms and triples from model.
     *
     * @param model {@link OntGraphModel}
     * @return Set of {@link Triples} with {@link OWLAxiom} as key and Set of {@link Triple} as value
     */
    public Set<Triples<Axiom>> read(OntGraphModel model) {
        try {
            Map<Axiom, Triples<Axiom>> res = new HashMap<>();
            statements(model).map(RDF2OWLHelper.AxiomStatement::new).forEach(c -> {
                Axiom axiom = create(c.getStatement(), c.getAnnotations());
                Set<Triple> triples = c.getTriples();
                res.compute(axiom, (a, container) -> container == null ? new Triples<>(a, triples) : container.add(triples));
            });
            return new HashSet<>(res.values());
        } catch (Exception e) {
            throw new OntApiException(String.format("Can't process reading. Translator <%s>.", getClass()), e);
        }
    }

    abstract Stream<OntStatement> statements(OntGraphModel model);

    abstract Axiom create(OntStatement statement, Set<OWLAnnotation> annotations);

    public AxiomParserProvider.Config getConfig() {
        return config;
    }

    public void setConfig(AxiomParserProvider.Config config) {
        this.config = OntApiException.notNull(config, "Null config.");
    }

    /**
     * Immutable container for {@link OWLObject} and associated with it set of rdf-graph {@link Triple}s.
     * <p>
     * Created by @szuev on 27.11.2016.
     */
    public static class Triples<O extends OWLObject> {
        private final O object;
        private final Set<Triple> triples;
        private int hashCode;

        public Triples(O object, Set<Triple> triples) {
            this.object = OntApiException.notNull(object, "Null OWLObject.");
            if (OntApiException.notNull(triples, "Null triples.").isEmpty()) {
                throw new OntApiException("Empty triples.");
            }
            this.triples = Collections.unmodifiableSet(triples);
        }

        public Triples(O object, Triple triple) {
            this(object, Collections.singleton(triple));
        }

        public O getObject() {
            return object;
        }

        public Set<Triple> getTriples() {
            return triples;
        }

        public Stream<Triple> triples() {
            return triples.stream();
        }

        public Graph asGraph() {
            Graph res = Factory.createGraphMem();
            GraphUtil.add(res, triples.iterator());
            return res;
        }

        public Triples<O> add(Collection<Triple> triples) {
            Set<Triple> set = new HashSet<>(OntApiException.notNull(this.triples, "Null triples."));
            set.addAll(triples);
            return new Triples<>(object, set);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Triples<?> that = (Triples<?>) o;
            return object.equals(that.object);
        }

        @Override
        public int hashCode() {
            if (hashCode != 0) return hashCode;
            return hashCode = object.hashCode();
        }

        public static <O extends OWLObject> Optional<Triples<O>> find(Collection<Triples<O>> set, O key) {
            int h = OntApiException.notNull(key, "null key").hashCode();
            return set.stream().filter(Objects::nonNull).filter(o -> o.hashCode() == h).filter(o -> key.equals(o.getObject())).findAny();
        }
    }
}
