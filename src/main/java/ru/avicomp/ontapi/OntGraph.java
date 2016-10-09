package ru.avicomp.ontapi;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.jena.graph.*;
import org.apache.jena.graph.impl.LiteralLabel;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.rdf.turtle.parser.OWLRDFConsumerAdapter;

/**
 * Graph wrapper.
 * <p>
 * There are two graphs underling.
 * One of them ({@link OntGraph#graph}) is associated with this wrapper and through it with jena-model,
 * The second one ({@link OntGraph#original}) is associated with owl-ontology {@link OntologyModel} and triple-handler {@link OntTripleHandler} works with it.
 * Two-graphs architecture allows to avoid duplicating of anonymous nodes.
 * The adding of triples occurs with help of OntTripleHandler (base class {@link org.semanticweb.owlapi.rdf.rdfxml.parser.OWLRDFConsumer},
 * which produces axioms and related with them triplets in the original graph (through {@link ru.avicomp.ontapi.parsers.AxiomParser}).
 * The removing occurs using events recorded by {@link OntGraphListener} (base class {@link GraphListener}) during axiom parsing.
 * <p>
 * Created by @szuev on 02.10.2016.
 */
public class OntGraph implements Graph {
    private static final OntLoaderConfiguration ONT_LOADER_CONFIGURATION = new OntLoaderConfiguration();
    // graph, that is attached to jena graph ont-model
    private final Graph graph;
    // graph, that is attached to owl-ontology
    private final Graph original;
    // triplet handler, which works original graph through owl-ontology
    private final OntTripleHandler tripleHandler;

    public OntGraph(OntologyModel owlOntology) {
        this(owlOntology.getInnerGraph(), new OntTripleHandler(owlOntology, ONT_LOADER_CONFIGURATION));
    }

    public OntGraph(Graph base, OntTripleHandler tripletHandler) {
        this.original = base;
        this.graph = Factory.createGraphMem();
        this.tripleHandler = tripletHandler;
        GraphUtil.addInto(graph, original);
    }

    public Graph getBaseGraph() {
        return original;
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
        tripleHandler.addTriple(t);
    }

    /**
     * if the specified triple is included in some axiom then ALL triples which belong to that axiom will be deleted.
     *
     * @param t Triple
     */
    @Override
    public void delete(Triple t) {
        // first remove using axiom:
        tripleHandler.deleteTriple(t);
        graph.delete(t);
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
     * tell that bulk of triplets that correspond to the current axiom has been completed.
     * call this method when {@link InfModel#rebind()}
     */
    void flush() {
        tripleHandler.handleEnd();
        // the removed and handled triplets may be cleaned out:
        tripleHandler.getTriples().removeIf(t -> !t.isPending());
    }

    /**
     * synchronize graphs pair.
     * call this method after adding axiom.
     */
    void sync() {
        // remove deleted triples, we don't need them any more (?)
        tripleHandler.getTriples().removeIf(OntTripleHandler.OntTriple::isRemoved);
        // overwriting graph:
        graph.clear();
        // return back pending triples (just in case):
        tripleHandler.triples().filter(OntTripleHandler.OntTriple::isPending).forEach(t -> graph.add(t.triple()));
        GraphUtil.addInto(graph, original);
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

        OntTripleHandler(OntologyModel ontology, OWLOntologyLoaderConfiguration configuration) {
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
            if (triples.isEmpty()) return;
            handleTriples();
            handleAnonRoots();
            super.handleEnd();
        }

        void handleTriples() {
            handleOntologyID();
            triples().filter(OntTriple::isURIRoot).forEach(OntTriple::handle);
        }

        /**
         * handle OWLOntologyID while changing ontology iri.
         * <p>
         * The consumer ({@link org.semanticweb.owlapi.rdf.rdfxml.parser.OWLRDFConsumer})
         * doesn't allow to change ontology id, so we are doing it ourselves here.
         */
        private void handleOntologyID() {
            OntTriple iriTriple = triples().filter(OntTriple::isPending).filter(OntTriple::isOntologyIRI).findFirst().orElse(null);
            if (iriTriple == null) return;
            iriTriple.markHandled();
            Node subject = iriTriple.subject();
            IRI iri = toIRI(subject);
            OWLOntologyID old = getOntology().getOntologyID();
            IRI version = null;
            OntTriple versionTriple = triples().filter(OntTriple::isPending).filter(OntTriple::isVersionIRI).findFirst().orElse(null);
            if (subject.isURI()) { // An ontology without an ontology IRI MUST NOT contain a version IRI.
                if (versionTriple != null) {
                    version = toIRI(versionTriple.object());
                    versionTriple.markHandled();
                } else {
                    version = old.getVersionIRI().orElse(null);
                }
            } else if (versionTriple != null) {
                versionTriple.markRemoved();
            }
            OWLOntologyID id = new OWLOntologyID(iri, version);
            OntologyModel ontology = getOntology();
            if (same(old, id)) { // nothing to change
                return;
            }
            getOntology().applyChange(new SetOntologyID(ontology, id));
            // change primary iri in the depths of consumer:
            getOntologies().clear();
            addOntology(iri);
        }

        /**
         * @param a OWLOntologyID
         * @param b OWLOntologyID
         * @return true if equals
         */
        private static boolean same(OWLOntologyID a, OWLOntologyID b) {
            return (a.isAnonymous() && b.isAnonymous()) || a.equals(b);
        }

        /**
         * OWL2 allows anonymous roots (owl:AllDisjointClasses, owl:AllDifferent, owl:Axiom, etc)
         */
        void handleAnonRoots() {
            triples().filter(OntTriple::isRoot).forEach(OntTriple::handle);
        }

        public void addTriple(Triple t) {
            OntTriple triple = new OntTriple(t);
            triples.add(triple);
            handleTriples();
        }

        public OntTriple find(Triple triple) {
            return triples().filter(t -> triple.equals(t.triple)).findFirst().orElse(null);
        }

        OntTriple fetch(Triple triple) {
            OntTriple res = find(triple);
            if (res == null) {
                res = new OntTriple(triple);
                triples.add(res);
            }
            return res;
        }

        public void deleteTriple(Triple t) {
            //TODO: remove only roots?
            fetch(t).markRemoved();
            OntologyModel ontology = getOntology();
            OntGraphEventStore store = getStore();
            OntGraphEventStore.OWLEvent event = store.find(OntGraphEventStore.createAdd(t));
            if (event == null) return;
            if (event.is(OWLAxiom.class)) {
                OWLAxiom axiom = event.get(OWLAxiom.class);
                if (ontology.axioms().filter(axiom::equals).findFirst().isPresent()) {
                    ontology.remove(axiom);
                }
            } else if (event.is(OWLImportsDeclaration.class)) {
                ontology.applyChange(new RemoveImport(ontology, event.get(OWLImportsDeclaration.class)));
            } else if (event.is(OWLAnnotation.class)) {
                ontology.applyChange(new RemoveOntologyAnnotation(ontology, event.get(OWLAnnotation.class)));
            }
        }

        public void clear() {
            nodes.clear();
            triples.clear();
        }

        Stream<OntTriple> triples() {
            return triples.stream();
        }

        Stream<OntTriple> triples(TripleStatus status) {
            return triples.stream().filter(t -> status.equals(t.status));
        }

        Set<OntTriple> getTriples() {
            return triples;
        }

        private IRI toIRI(Node node) {
            return nodes.computeIfAbsent(node, _node -> {
                if (_node.isBlank()) {
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

            Triple triple() {
                return triple;
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

            private boolean isPending() {
                return TripleStatus.PENDING.equals(status);
            }

            private boolean isHandled() {
                return TripleStatus.HANDLED.equals(status);
            }

            private boolean isRemoved() {
                return TripleStatus.REMOVED.equals(status);
            }

            private void markRemoved() {
                this.status = TripleStatus.REMOVED;
            }

            private void markHandled() {
                this.status = TripleStatus.HANDLED;
            }

            private boolean isOntologyIRI() {
                return object().isURI() && object().equals(OWL.Ontology.asNode()) && predicate().equals(RDF.type.asNode());
            }

            private boolean isVersionIRI() {
                return object().isURI() && predicate().equals(OWL2.versionIRI.asNode());
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
                    markHandled();
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
