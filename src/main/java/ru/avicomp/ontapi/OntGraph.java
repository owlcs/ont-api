package ru.avicomp.ontapi;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.jena.graph.*;
import org.apache.jena.graph.impl.LiteralLabel;
import org.apache.jena.shared.AddDeniedException;
import org.apache.jena.shared.DeleteDeniedException;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.NodeID;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.rdf.turtle.parser.OWLRDFConsumerAdapter;
import org.semanticweb.owlapi.rdf.turtle.parser.TripleHandler;

/**
 * Pair for {@link OntologyModel}, wrapper for inner graph that belongs to the owl-ontology.
 * <p>
 * Created by @szuev on 02.10.2016.
 */
public class OntGraph implements Graph {
    private final Graph graph;
    private final TripleHandler tripleHandler;

    private transient Map<Node, IRI> nodes = new HashMap<>();
    private transient Set<OwlTriple> triples = new HashSet<>();


    public OntGraph(OntologyModel owlOntology) {
        this(owlOntology.getInnerGraph(), new OWLRDFConsumerAdapter(owlOntology, new OntLoaderConfiguration()));
    }

    public OntGraph(Graph base, TripleHandler tripletHandler) {
        this.graph = base;
        this.tripleHandler = tripletHandler;
    }

    @Override
    public void add(Triple t) throws AddDeniedException {
        graph.add(t);
        addOwlTriple(t.getSubject(), t.getPredicate(), t.getObject());
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
    public void delete(Triple t) throws DeleteDeniedException {
        //TODO:
        //throw new OntException.Unsupported(getClass(), "delete");
        graph.delete(t);
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
        //TODO:
        throw new OntException.Unsupported(getClass(), "remove");
    }

    @Override
    public void clear() {
        reset();
        graph.clear();
    }

    @Override
    public void close() {
        flush();
        reset();
        graph.close();
    }

    /**
     * tell that bulk of triplets that correspond to the axiom has been completed.
     */
    public void flush() {
        handleOwlTriples();
        handleOwlAnonRootTriples();
        tripleHandler.handleEnd();
        ;
    }

    public void reset() {
        this.nodes.clear();
        this.triples.clear();
    }

    private void addOwlTriple(Node subject, Node predicate, Node object) {
        OwlTriple triple = new OwlTriple(subject, predicate, object);
        triples.add(triple);
        handleOwlTriples();
    }

    private void handleOwlTriples() {
        triples.stream().filter(OwlTriple::isURIRoot).forEach(OwlTriple::handle);
    }

    /**
     * OWL2 allows anon roots (owl:AllDisjointClasses, owl:AllDifferent, etc)
     */
    private void handleOwlAnonRootTriples() {
        triples.stream().filter(OwlTriple::isRoot).forEach(OwlTriple::handle);
    }

    private IRI toIRI(Node rdfNode) {
        return nodes.computeIfAbsent(rdfNode, node -> {
            if (node.isBlank()) {
                return IRI.create(NodeID.getNodeID().getID());
            } else if (node.isLiteral()) {
                return IRI.create(node.getLiteral().getDatatypeURI());
            } else if (node.isURI()) {
                return IRI.create(node.getURI());
            }
            throw new OntException("this should never happen.");
        });
    }

    private enum TripleStatus {
        PENDING,
        HANDLED,
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

    private class OwlTriple {
        private final Node subject;
        private final Node predicate;
        private final Node object;
        private TripleStatus status;
        private Set<OwlTriple> children = new HashSet<>();
        private OwlTriple parent;

        private OwlTriple(Node subject, Node predicate, Node object) {
            this.subject = subject;
            this.predicate = predicate;
            this.object = object;
            this.status = TripleStatus.PENDING;
        }

        boolean isRoot() {
            return findParent() == null;
        }

        boolean isURIRoot() {
            // first set parent and children to make graph consistent, then decide is it root or not.
            return isRoot() && !subject.isBlank();
        }

        OwlTriple findParent() {
            if (parent != null) return parent;
            if (!subject.isBlank()) return null;
            parent = triples.stream().filter(t -> subject.equals(t.object)).findFirst().orElse(null);
            if (parent != null) {
                parent.children.add(this);
            }
            return parent;
        }

        private void innerHandle() {
            try {
                IRI _subject = toIRI(subject);
                IRI _predicate = toIRI(predicate);
                if (object.isLiteral()) {
                    LiteralLabel literal = object.getLiteral();
                    if (literal.language() != null) {
                        tripleHandler.handleTriple(_subject, _predicate, literal.getLexicalForm(), literal.language());
                    } else {
                        tripleHandler.handleTriple(_subject, _predicate, literal.getLexicalForm(), IRI.create(literal.getDatatypeURI()));
                    }
                } else {
                    IRI _object = toIRI(object);
                    tripleHandler.handleTriple(_subject, _predicate, _object);
                }
            } finally {
                status = TripleStatus.HANDLED;
            }
        }

        void handle() {
            // first process parent (this):
            if (isPending()) {
                innerHandle();
            }
            // then process children using recursion:
            children.stream().filter(OwlTriple::isPending).forEach(OwlTriple::handle);
        }

        private boolean isPending() {
            return TripleStatus.PENDING.equals(status);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OwlTriple that = (OwlTriple) o;
            return subject.equals(that.subject) && predicate.equals(that.predicate) && object.equals(that.object);
        }

        @Override
        public int hashCode() {
            return Triple.hashCode(subject, predicate, object);
        }

        private String toStringSubject() {
            return subject.isURI() ? subject.getURI() : String.valueOf(subject);
        }

        private String toStringPredicate() {
            return predicate.getURI();
        }

        private String toStringObject() {
            return object.isURI() ? object.getURI() : String.valueOf(object);
        }

        @Override
        public String toString() {
            return String.format("%s, %s, %s", toStringSubject(), toStringPredicate(), toStringObject());
        }
    }

}
