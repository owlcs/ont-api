package ru.avicomp.ontapi;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.shared.AddDeniedException;
import org.apache.jena.shared.DeleteDeniedException;
import org.apache.jena.sparql.graph.GraphWrapper;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.NodeID;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.rdf.turtle.parser.OWLRDFConsumerAdapter;

/**
 * Pair for OntologyModel
 * <p>
 * Created by @szuev on 02.10.2016.
 */
public class OntGraph extends GraphWrapper implements Graph {
    private OWLRDFConsumerAdapter owlConsumer;
    private Map<Node, IRI> nodes = new HashMap<>();
    private Set<OwlTriple> triples = new HashSet<>();

    OntGraph(OntologyModel owlOntology) {
        super(owlOntology.getInnerGraph());
        this.owlConsumer = new OWLRDFConsumerAdapter(owlOntology, new OntLoaderConfiguration());
    }

    @Override
    public void add(Triple t) throws AddDeniedException {
        //super.add(t);
        addOwlTriple(t.getSubject(), t.getPredicate(), t.getObject());
    }

    @Override
    public void delete(Triple t) throws DeleteDeniedException {
        throw new OntException.Unsupported(getClass(), "delete");
    }

    @Override
    public void remove(Node s, Node p, Node o) {
        throw new OntException.Unsupported(getClass(), "remove");
    }

    @Override
    public void clear() {
        super.clear();
        this.nodes.clear();
        this.triples.clear();
    }

    @Override
    public void close() {
        super.close();
        finish();
    }

    /**
     * tell that bulk of triplets that correspond to the axiom has been completed.
     */
    public void finish() {
        handleTriples();
        owlConsumer.handleEnd();
    }

    private void addOwlTriple(Node subject, Node predicate, Node object) {
        OwlTriple triple = new OwlTriple(subject, predicate, object);
        triples.add(triple);
        handleTriples();
    }

    private void handleTriples() {
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
            // first set parent and children to make graph consistent, then decide is it root or not.
            return findParent() == null && !subject.isBlank();
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
                    Literal literal = (Literal) object;
                    owlConsumer.handleTriple(_subject, _predicate, literal.getLexicalForm(), IRI.create(literal.getDatatypeURI()));
                } else {
                    IRI _object = toIRI(object);
                    owlConsumer.handleTriple(_subject, _predicate, _object);
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
}
