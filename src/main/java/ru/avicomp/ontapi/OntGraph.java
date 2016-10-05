package ru.avicomp.ontapi;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.jena.graph.*;
import org.apache.jena.graph.impl.LiteralLabel;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.NodeID;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.rdf.turtle.parser.OWLRDFConsumerAdapter;

/**
 * Graph wrapper.
 * There are two graphs underling.
 * One of them is associated with this wrapper and through it with jena-model,
 * The second one is associated with owl-ontology {@link OntologyModel} and triple-handler {@link OntTripleHandler} works with it.
 * It's to avoid duplicating of anonymous nodes.
 * <p>
 * Created by @szuev on 02.10.2016.
 */
public class OntGraph implements Graph {
    // graph, that is attached to jena graph ont-model
    private final Graph graph;
    // graph, that is attached to owl-ontology
    private final Graph original;
    // triplet handler, which works original graph through owl-ontology
    private final OntTripleHandler tripleHandler;

    public OntGraph(OntologyModel owlOntology) {
        this(owlOntology.getGraph(), new OntTripleHandler(owlOntology, new OntLoaderConfiguration()));
    }

    public OntGraph(Graph base, OntTripleHandler tripletHandler) {
        this.original = base;
        this.graph = Factory.createGraphMem();
        this.tripleHandler = tripletHandler;
        GraphUtil.addInto(graph, original);
    }

    public Graph getBaseGraph() {
        return graph;
    }

    @Override
    public boolean dependsOn(Graph other) {
        return graph.dependsOn(other);
    }

    @Override
    public TransactionHandler getTransactionHandler() {
        return graph.getTransactionHandler();
    }

    @Override
    public Capabilities getCapabilities() {
        return graph.getCapabilities();
    }

    @Override
    public GraphEventManager getEventManager() {
        return graph.getEventManager();
    }

    @Override
    public GraphStatisticsHandler getStatisticsHandler() {
        return graph.getStatisticsHandler();
    }

    @Override
    public PrefixMapping getPrefixMapping() {
        return graph.getPrefixMapping();
    }

    @Override
    public ExtendedIterator<Triple> find(Triple t) {
        return graph.find(t);
    }

    @Override
    public ExtendedIterator<Triple> find(Node s, Node p, Node o) {
        return graph.find(s, p, o);
    }

    @Override
    public boolean isIsomorphicWith(Graph g) {
        return graph.isIsomorphicWith(g);
    }

    @Override
    public boolean contains(Node s, Node p, Node o) {
        return graph.contains(s, p, o);
    }

    @Override
    public boolean contains(Triple t) {
        return graph.contains(t);
    }

    @Override
    public boolean isEmpty() {
        return graph.isEmpty();
    }

    @Override
    public int size() {
        return graph.size();
    }

    @Override
    public boolean isClosed() {
        return graph.isClosed();
    }

    @Override
    public void remove(Node s, Node p, Node o) {
        graph.remove(s, p, o);
    }

    @Override
    public void add(Triple t) {
        graph.add(t);
        if (isURITriple(t)) { // could be added directly to original graph
            original.add(t);
        }
        tripleHandler.addTriple(t);
    }

    @Override
    public void delete(Triple t) {
        graph.delete(t);
        if (isURITriple(t)) {
            original.delete(t);
        }
        tripleHandler.deleteTriple(t);
    }

    @Override
    public void clear() {
        tripleHandler.clear();
        graph.clear();
    }

    @Override
    public void close() {
        flush();
        graph.close();
    }

    /**
     * tell that bulk of triplets that correspond to the axiom has been completed.
     */
    void flush() {
        tripleHandler.handleEnd();
        tripleHandler.clear();
    }

    /**
     * synchronize graphs pair
     */
    void sync() {
        graph.clear();
        GraphUtil.addInto(graph, original);
    }

    private static boolean isURITriple(Triple triple) {
        return !triple.getSubject().isBlank() && !triple.getObject().isBlank();
    }

    public static class OntLoaderConfiguration extends OWLOntologyLoaderConfiguration {
        private boolean ignoreImports;

        public OntLoaderConfiguration(boolean ignoreImports) {
            super();
            this.ignoreImports = ignoreImports;
        }

        public OntLoaderConfiguration() {
            this(true);
        }

        @Override
        public boolean isIgnoredImport(@Nonnull IRI any) {
            return ignoreImports || super.isIgnoredImport(any);
        }
    }

    public static class OntTripleHandler extends OWLRDFConsumerAdapter {
        private Map<Node, IRI> nodes = new HashMap<>();
        private Set<OntTriple> triples = new HashSet<>();

        public OntTripleHandler(OntologyModel ontology, OWLOntologyLoaderConfiguration configuration) {
            super(ontology, configuration);
        }

        @Override
        public OntologyModel getOntology() {
            return (OntologyModel) super.getOntology();
        }

        public OntGraphEventStore getStore() {
            return getOntology().getEventStore();
        }

        @Override
        public void handleEnd() {
            handleTriples();
            handleAnonRoots();
            super.handleEnd();
        }

        public void addTriple(Triple t) {
            OntTriple triple = new OntTriple(t);
            triples.add(triple);
            handleTriples();
        }

        public OntTriple find(Triple triple) {
            return triples.stream().filter(t -> triple.equals(t.triple)).findFirst().orElse(null);
        }

        public void deleteTriple(Triple t) {
            OntTriple res = find(t);
            if (res != null) res.delete();
            OntologyModel ontology = getOntology();
            OWLAxiom axiom = ontology.getEventStore().findAxiom(t);
            if (axiom != null && ontology.axioms().filter(axiom::equals).findFirst().isPresent()) {
                ontology.remove(axiom);
            }
        }

        public void clear() {
            nodes.clear();
            triples.clear();
        }

        public void handleTriples() {
            triples.stream().filter(OntTriple::isURIRoot).forEach(OntTriple::handle);
        }

        /**
         * OWL2 allows anonymous roots (owl:AllDisjointClasses, owl:AllDifferent, owl:Axiom etc)
         */
        public void handleAnonRoots() {
            triples.stream().filter(OntTriple::isRoot).forEach(OntTriple::handle);
        }

        private IRI toIRI(Node node) {
            return nodes.computeIfAbsent(node, _node -> {
                if (_node.isBlank()) { //todo?
                    return IRI.create(NodeID.getNodeID().getID());
                } else if (_node.isLiteral()) {
                    return IRI.create(_node.getLiteral().getDatatypeURI());
                } else if (_node.isURI()) {
                    return IRI.create(_node.getURI());
                }
                throw new OntException("this should never happen.");
            });
        }

        private enum TripleStatus {
            PENDING,
            HANDLED,
            REMOVED,
        }

        private class OntTriple {
            private final Triple triple;
            private TripleStatus status;
            private Set<OntTriple> children = new HashSet<>();
            private OntTriple parent;

            private OntTriple(Triple triple) {
                this.triple = triple;
                this.status = TripleStatus.PENDING;
            }

            Node subject() {
                return triple.getSubject();
            }

            Node predicate() {
                return triple.getPredicate();
            }

            Node object() {
                return triple.getObject();
            }

            boolean isRoot() {
                return findParent() == null;
            }

            boolean isURIRoot() {
                // first set parent and children to make graph consistent, then decide is it root or not.
                return isRoot() && !subject().isBlank();
            }

            OntTriple findParent() {
                if (parent != null) return parent;
                if (!subject().isBlank()) return null;
                parent = triples.stream().filter(t -> subject().equals(t.object())).findFirst().orElse(null);
                if (parent != null) {
                    parent.children.add(this);
                }
                return parent;
            }

            private void innerHandle() {
                try {
                    IRI _subject = toIRI(subject());
                    IRI _predicate = toIRI(predicate());
                    if (object().isLiteral()) {
                        LiteralLabel literal = object().getLiteral();
                        if (literal.language() != null) {
                            handleTriple(_subject, _predicate, literal.getLexicalForm(), literal.language());
                        } else {
                            handleTriple(_subject, _predicate, literal.getLexicalForm(), IRI.create(literal.getDatatypeURI()));
                        }
                    } else {
                        IRI _object = toIRI(object());
                        handleTriple(_subject, _predicate, _object);
                    }
                } finally {
                    status = TripleStatus.HANDLED;
                }
            }

            private void handle() {
                // first process parent (this):
                if (isPending()) {
                    innerHandle();
                }
                // then process children using recursion:
                children.stream().filter(OntTriple::isPending).forEach(OntTriple::handle);
            }

            private boolean isPending() {
                return TripleStatus.PENDING.equals(status);
            }

            private boolean isHandled() {
                return TripleStatus.HANDLED.equals(status);
            }

            private void delete() {
                this.status = TripleStatus.REMOVED;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                OntTriple that = (OntTriple) o;
                return triple.equals(that.triple);
            }

            @Override
            public int hashCode() {
                return triple.hashCode();
            }

            @Override
            public String toString() {
                return String.valueOf(triple);
            }
        }
    }

}
