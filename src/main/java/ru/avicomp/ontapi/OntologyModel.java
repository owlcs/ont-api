package ru.avicomp.ontapi;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.graph.*;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.impl.OntModelImpl;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.ChangeApplied;

import com.google.inject.assistedinject.Assisted;
import ru.avicomp.ontapi.parsers.AxiomParserProvider;
import ru.avicomp.ontapi.parsers.TranslationHelper;
import uk.ac.manchester.cs.owl.owlapi.OWLOntologyImpl;

import static org.semanticweb.owlapi.model.parameters.ChangeApplied.NO_OPERATION;
import static org.semanticweb.owlapi.model.parameters.ChangeApplied.SUCCESSFULLY;

/**
 * TODO:
 * Created by @szuev on 27.09.2016.
 */
public class OntologyModel extends OWLOntologyImpl {
    private final ChangeFilter filter;
    private transient OntGraph outer;

    /**
     * @param manager    ontology manager
     * @param ontologyID the id
     */
    @Inject
    public OntologyModel(@Assisted OWLOntologyManager manager, @Assisted OWLOntologyID ontologyID) {
        super(manager, ontologyID);
        filter = new ChangeFilter();
    }

    public OntGraphEventStore getEventStore() {
        return filter.getEventStore();
    }

    @Override
    public ChangeApplied applyDirectChange(OWLOntologyChange change) {
        ChangeApplied res = change.accept(getChangeFilter());
        if (SUCCESSFULLY.equals(res)) {
            sync();
        }
        return res;
    }

    @Override
    public ChangeApplied applyChanges(List<? extends OWLOntologyChange> changes) {
        ChangeApplied appliedChanges = SUCCESSFULLY;
        for (OWLOntologyChange change : changes) {
            ChangeApplied result = applyDirectChange(change);
            if (SUCCESSFULLY.equals(appliedChanges)) {
                // overwrite only if appliedChanges is still successful. If one
                // change has been unsuccessful, we want to preserve that
                // information
                appliedChanges = result;
            }
        }
        return appliedChanges;
    }

    @Override
    public OntologyManager getOWLOntologyManager() {
        return (OntologyManager) manager;
    }

    public OWLOntologyChangeVisitorEx<ChangeApplied> getChangeFilter() {
        return filter;
    }

    /**
     * package-private access,
     * to use only inside {@link OntGraph}
     *
     * @return Graph
     */
    Graph getInnerGraph() {
        return filter.getGraph();
    }

    private OntGraph getOntGraph() {
        return outer == null ? outer = new OntGraph(this) : outer;
    }

    private void rebind() {
        if (outer != null)
            outer.flush();
    }

    private void sync() {
        if (outer != null)
            outer.sync();
    }

    /**
     * don't forget to call {@link OntModel#rebind()} after adding bulk axiom.
     *
     * @return OntModel
     */
    public OntModel asGraphModel() {
        return new OntGraphModel(this);
    }

    public static class OntGraphModel extends OntModelImpl {
        public OntGraphModel(OntologyModel ontology) {
            super(ontology.getOWLOntologyManager().getSpec(), ModelFactory.createModelForGraph(ontology.getOntGraph()));
        }

        @Override
        public OntGraph getBaseGraph() {
            return (OntGraph) super.getBaseGraph();
        }

        @Override
        public void rebind() {
            super.rebind();
            getBaseGraph().flush();
        }
    }

    private class ChangeFilter implements OWLOntologyChangeVisitorEx<ChangeApplied> {
        private final OntGraphEventStore eventStore;
        private final Graph inner;
        private Node nodeIRI;

        ChangeFilter() {
            this.eventStore = new OntGraphEventStore();
            this.inner = Factory.createGraphMem();
            initOntologyID();
        }

        OntGraphEventStore getEventStore() {
            return eventStore;
        }

        Graph getGraph() {
            return inner;
        }

        /**
         * returns {@link Node} (blank for anonymous) for associated {@link OWLOntologyID}.
         *
         * @return {@link Node_Blank} or {@link Node_URI}
         */
        private Node getNodeIRI() {
            if (nodeIRI != null) return nodeIRI;
            return nodeIRI = createNodeIRI(ontologyID);
        }

        private Node createNodeIRI(OWLOntologyID id) {
            return id.isAnonymous() ? NodeIRIUtils.toNode() : NodeIRIUtils.toNode(id.getOntologyIRI().orElse(null));
        }

        private void initOntologyID() {
            GraphListener listener = OntGraphListener.create(eventStore, OntGraphEventStore.createChange(ontologyID));
            try {
                inner.getEventManager().register(listener);
                inner.add(Triple.create(getNodeIRI(), RDF.type.asNode(), OWL.Ontology.asNode()));
                IRI versionIRI = ontologyID.getVersionIRI().orElse(null);
                if (versionIRI == null) return;
                inner.add(Triple.create(getNodeIRI(), OWL2.versionIRI.asNode(), NodeIRIUtils.toNode(versionIRI)));
            } finally {
                inner.getEventManager().unregister(listener);
            }
        }

        /**
         * changes ontology id.
         * to produce events doesn't use {@link org.apache.jena.util.ResourceUtils#renameResource}:
         * it's important for changing ontology-id through outer-graph.
         *
         * @param id new OWLOntologyID
         */
        private void changeOntologyID(OWLOntologyID id) {
            GraphListener listener = OntGraphListener.create(eventStore, OntGraphEventStore.createChange(ontologyID));
            try {
                inner.getEventManager().register(listener);
                Set<OWLImportsDeclaration> imports = OntologyModel.this.importsDeclarations().collect(Collectors.toSet());
                Set<OWLAnnotation> annotations = OntologyModel.this.annotations().collect(Collectors.toSet());
                // first remove imports and annotations
                imports.forEach(this::removeImport);
                annotations.forEach(this::removeAnnotation);
                // remove ontology whole triplet set
                inner.remove(nodeIRI, Node.ANY, Node.ANY);
                // change version iri:
                IRI version = id.getVersionIRI().orElse(null);
                if (version != null) {
                    inner.add(Triple.create(nodeIRI, OWL2.versionIRI.asNode(), NodeIRIUtils.toNode(version)));
                }
                // add new one owl:Ontology
                inner.add(Triple.create(nodeIRI = createNodeIRI(id), RDF.type.asNode(), OWL.Ontology.asNode()));
                // return back imports:
                imports.forEach(this::addImport);
                // return back annotations:
                annotations.forEach(this::addAnnotation);
                ontologyID = id;
            } finally {
                inner.getEventManager().unregister(listener);
            }
        }

        private void addImport(OWLImportsDeclaration declaration) {
            OntGraphEventStore.OWLEvent event = OntGraphEventStore.createAdd(declaration);
            GraphListener listener = OntGraphListener.create(eventStore, event);
            try {
                inner.getEventManager().register(listener);
                inner.add(Triple.create(getNodeIRI(), OWL.imports.asNode(), NodeIRIUtils.toNode(declaration.getIRI())));
            } finally {
                eventStore.clear(event.reverse());
                inner.getEventManager().unregister(listener);
            }
        }

        private void removeImport(OWLImportsDeclaration declaration) {
            OntGraphEventStore.OWLEvent event = OntGraphEventStore.createRemove(declaration);
            GraphListener listener = OntGraphListener.create(eventStore, event);
            try {
                inner.getEventManager().register(listener);
                inner.remove(getNodeIRI(), OWL.imports.asNode(), NodeIRIUtils.toNode(declaration.getIRI()));
            } finally {
                eventStore.clear(event.reverse());
                inner.getEventManager().unregister(listener);
            }
        }

        /**
         * WARNING: Complex ANNOTATIONS are not supported for Ontology Object by the OWL API (version 5.0.3).
         * Also not all axioms it is possible to annotated, e.g. DifferentIndividuals.
         * BUT we still provide a fully correct set of triplets for these cases in accordance with the specification
         * (for ontology annotations see <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Translation_of_Axioms_without_Annotations'>2.1 Translation of Axioms without Annotations</a>, Table 1).
         * So after reloading a graph model back to OWL API loss of information about woody nested annotations is expected.
         * TODO: need to fix OWL-API graph loader also.
         *
         * @param annotation OWLAnnotation Object
         */
        private void addAnnotation(OWLAnnotation annotation) {
            OntGraphEventStore.OWLEvent event = OntGraphEventStore.createAdd(annotation);
            GraphListener listener = OntGraphListener.create(eventStore, event);
            try {
                inner.getEventManager().register(listener);
                TranslationHelper.addAnnotations(inner, getNodeIRI(), Stream.of(annotation).collect(Collectors.toList()));
            } finally {
                eventStore.clear(event.reverse());
                inner.getEventManager().unregister(listener);
            }
        }

        private void removeAnnotation(OWLAnnotation annotation) {
            OntGraphEventStore.OWLEvent event = OntGraphEventStore.createRemove(annotation);
            GraphListener listener = OntGraphListener.create(eventStore, event);
            try {
                inner.getEventManager().register(listener);
                removeAllTriples(event.reverse());
            } finally {
                eventStore.clear(event.reverse());
                inner.getEventManager().unregister(listener);
            }
        }

        /**
         * todo: use {@link org.apache.jena.graph.compose.MultiUnion} graph wrapper
         *
         * @param axiom OWLAxiom
         */
        private void addAxiom(OWLAxiom axiom) {
            OntGraphEventStore.OWLEvent event = OntGraphEventStore.createAdd(axiom);
            GraphListener listener = OntGraphListener.create(eventStore, event);
            try {
                inner.getEventManager().register(listener);
                AxiomParserProvider.get(axiom).process(inner);
            } catch (Exception e) {
                throw new OntException("Add axiom " + axiom, e);
            } finally {
                eventStore.clear(event.reverse());
                inner.getEventManager().unregister(listener);
            }
        }

        private void removeAxiom(OWLAxiom axiom) {
            OntGraphEventStore.OWLEvent event = OntGraphEventStore.createRemove(axiom);
            GraphListener listener = OntGraphListener.create(eventStore, event);
            try {
                inner.getEventManager().register(listener);
                removeAllTriples(event.reverse());
            } catch (Exception e) {
                throw new OntException("Remove axiom " + axiom, e);
            } finally {
                eventStore.clear(event.reverse());
                inner.getEventManager().unregister(listener);
            }
        }

        private void removeAllTriples(OntGraphEventStore.OWLEvent event) {
            eventStore.triples(event).
                    filter(t -> eventStore.count(t) < 2). // skip triplets which are included in several axioms
                    map(OntGraphEventStore.TripleEvent::get).forEach(inner::delete);
        }

        /**
         * @param change AddAxiom object
         * @return ChangeApplied enum
         */
        @Override
        public ChangeApplied visit(@Nonnull AddAxiom change) {
            OWLAxiom axiom = change.getAxiom();
            if (ints.addAxiom(axiom)) {
                addAxiom(axiom);
                return SUCCESSFULLY;
            }
            return NO_OPERATION;
        }

        @Override
        public ChangeApplied visit(@Nonnull RemoveAxiom change) {
            OWLAxiom axiom = change.getAxiom();
            if (ints.removeAxiom(axiom)) {
                removeAxiom(axiom);
                return SUCCESSFULLY;
            }
            return NO_OPERATION;
        }

        @Override
        public ChangeApplied visit(@Nonnull AddImport change) {
            OWLImportsDeclaration importDeclaration = change.getImportDeclaration();
            if (ints.addImportsDeclaration(importDeclaration)) {
                addImport(importDeclaration);
                return SUCCESSFULLY;
            }
            return NO_OPERATION;
        }

        @Override
        public ChangeApplied visit(@Nonnull RemoveImport change) {
            OWLImportsDeclaration importDeclaration = change.getImportDeclaration();
            if (ints.removeImportsDeclaration(importDeclaration)) {
                removeImport(importDeclaration);
                return SUCCESSFULLY;
            }
            return NO_OPERATION;
        }

        @Override
        public ChangeApplied visit(@Nonnull AddOntologyAnnotation change) {
            OWLAnnotation annotation = change.getAnnotation();
            if (ints.addOntologyAnnotation(annotation)) {
                addAnnotation(annotation);
                return SUCCESSFULLY;
            }
            return NO_OPERATION;
        }

        @Override
        public ChangeApplied visit(@Nonnull RemoveOntologyAnnotation change) {
            OWLAnnotation annotation = change.getAnnotation();
            if (ints.removeOntologyAnnotation(annotation)) {
                removeAnnotation(annotation);
                return SUCCESSFULLY;
            }
            return NO_OPERATION;
        }

        @Override
        public ChangeApplied visit(@Nonnull SetOntologyID change) {
            OWLOntologyID id = change.getNewOntologyID();
            if (!ontologyID.equals(id)) {
                changeOntologyID(id);
                return SUCCESSFULLY;
            }
            return NO_OPERATION;
        }
    }
}
