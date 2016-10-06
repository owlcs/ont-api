package ru.avicomp.ontapi;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.List;
import java.util.Objects;

import org.apache.jena.graph.Factory;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphListener;
import org.apache.jena.graph.Triple;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.impl.OntModelImpl;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.ResourceUtils;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.ChangeApplied;

import com.google.inject.assistedinject.Assisted;
import ru.avicomp.ontapi.parsers.AxiomParserFactory;
import uk.ac.manchester.cs.owl.owlapi.OWLOntologyImpl;

import static org.semanticweb.owlapi.model.parameters.ChangeApplied.NO_OPERATION;
import static org.semanticweb.owlapi.model.parameters.ChangeApplied.SUCCESSFULLY;
import static ru.avicomp.ontapi.NodeIRIUtils.fromResource;
import static ru.avicomp.ontapi.NodeIRIUtils.toTriple;

/**
 * TODO:
 * Created by @szuev on 27.09.2016.
 */
public class OntologyModel extends OWLOntologyImpl {

    private final Graph inner;
    private final ChangeFilter filter;
    private final OntGraphEventStore eventStore;

    private transient OntGraph outer;

    /**
     * @param manager    ontology manager
     * @param ontologyID the id
     */
    @Inject
    public OntologyModel(@Assisted OWLOntologyManager manager, @Assisted OWLOntologyID ontologyID) {
        super(manager, ontologyID);
        inner = Factory.createGraphMem();
        filter = new ChangeFilter();
        eventStore = new OntGraphEventStore();
        filter.initOntologyTriplets();
    }

    public OntGraphEventStore getEventStore() {
        return eventStore;
    }

    @Override
    public ChangeApplied applyDirectChange(OWLOntologyChange change) {
        ChangeApplied res = change.accept(getChangeFilter());
        if (SUCCESSFULLY.equals(res)) {
            getOntGraph().sync();
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
        return inner;
    }

    private OntGraph getOntGraph() {
        return outer == null ? outer = new OntGraph(this) : outer;
    }

    private void rebind() {
        getOntGraph().flush();
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

        private IRI getOntologyIRI() {
            return ontologyID.getOntologyIRI().orElseThrow(() -> new OntException("Null ontology iri"));
        }

        private void addToGraph(OWLAnnotationValue s, IRI p, OWLAnnotationValue o) {
            addToGraph(toTriple(s, p, o));
        }

        private void addToGraph(Triple triple) {
            inner.add(triple);
        }

        private void deleteFromGraph(OWLAnnotationValue s, IRI p, OWLAnnotationValue o) {
            deleteFromGraph(toTriple(s, p, o));
        }

        private void deleteFromGraph(Triple triple) {
            inner.delete(triple);
        }

        private void initOntologyTriplets() {
            GraphListener listener = OntGraphListener.createChangeID(eventStore, ontologyID);
            try {
                inner.getEventManager().register(listener);
                IRI iri = ontologyID.getOntologyIRI().orElse(null);
                if (iri == null) return;
                addToGraph(toTriple(iri, fromResource(RDF.type), fromResource(OWL.Ontology)));
                IRI versionIRI = ontologyID.getVersionIRI().orElse(null);
                if (versionIRI == null) return;
                addToGraph(toTriple(iri, fromResource(OWL2.versionIRI), versionIRI));
            } finally {
                inner.getEventManager().unregister(listener);
            }
        }

        private void changeOntologyID(OWLOntologyID id) {
            GraphListener listener = OntGraphListener.createChangeID(eventStore, ontologyID);
            try {
                inner.getEventManager().register(listener);
                IRI newIRI = OntException.notNull(id.getOntologyIRI().orElse(null), "Ontology iri must not be null.");
                IRI newVersionIRI = id.getVersionIRI().orElse(null);
                IRI oldIRI = ontologyID.getOntologyIRI().orElse(null);
                IRI oldVersionIRI = ontologyID.getVersionIRI().orElse(null);
                if (oldIRI == null) {
                    addToGraph(toTriple(newIRI, fromResource(RDF.type), fromResource(OWL.Ontology)));
                } else {
                    Resource resource = OntException.notNull(ModelFactory.createModelForGraph(inner).getResource(oldIRI.getIRIString()), "Can't find onology");
                    ResourceUtils.renameResource(resource, newIRI.getIRIString());
                }
                if (!Objects.equals(oldVersionIRI, newVersionIRI)) {
                    if (oldVersionIRI != null) {
                        deleteFromGraph(newIRI, fromResource(OWL2.versionIRI), oldVersionIRI);
                    }
                    if (newVersionIRI != null) {
                        addToGraph(toTriple(newIRI, fromResource(OWL2.versionIRI), newVersionIRI));
                    }
                }
                ontologyID = id;
            } finally {
                inner.getEventManager().unregister(listener);
            }
        }

        private void addImport(OWLImportsDeclaration declaration) {
            GraphListener listener = OntGraphListener.createAdd(eventStore, declaration);
            try {
                inner.getEventManager().register(listener);
                addToGraph(getOntologyIRI(), fromResource(OWL.imports), declaration.getIRI());
            } finally {
                inner.getEventManager().unregister(listener);
            }
        }

        private void removeImport(OWLImportsDeclaration declaration) {
            GraphListener listener = OntGraphListener.createRemove(eventStore, declaration);
            try {
                inner.getEventManager().register(listener);
                deleteFromGraph(getOntologyIRI(), fromResource(OWL.imports), declaration.getIRI());
            } finally {
                inner.getEventManager().unregister(listener);
            }
        }

        private void addAnnotation(OWLAnnotation annotation) {
            GraphListener listener = OntGraphListener.createAdd(eventStore, annotation);
            try {
                inner.getEventManager().register(listener);
                OWLAnnotationProperty property = annotation.getProperty();
                OWLAnnotationValue value = annotation.getValue();
                OWLAnnotationValue literal = value.isIRI() ? value : value.asLiteral().orElse(null);
                addToGraph(getOntologyIRI(), property.getIRI(), OntException.notNull(literal, "Null literal, annotation: " + annotation));
            } finally {
                inner.getEventManager().unregister(listener);
            }
        }

        private void removeAnnotation(OWLAnnotation annotation) {
            GraphListener listener = OntGraphListener.createRemove(eventStore, annotation);
            try {
                inner.getEventManager().register(listener);
                OWLAnnotationProperty property = annotation.getProperty();
                OWLAnnotationValue value = annotation.getValue();
                OWLAnnotationValue literal = value.isIRI() ? value : value.asLiteral().orElse(null);
                deleteFromGraph(getOntologyIRI(), property.getIRI(), OntException.notNull(literal, "Null literal, annotation: " + annotation));
            } finally {
                inner.getEventManager().unregister(listener);
            }
        }

        /**
         * @param change AddAxiom object
         * @return ChangeApplied enum
         */
        @Override
        public ChangeApplied visit(@Nonnull AddAxiom change) {
            OWLAxiom axiom = change.getAxiom();
            if (ints.addAxiom(axiom)) {
                AxiomParserFactory.get(axiom).process(eventStore, inner);
                return SUCCESSFULLY;
            }
            return NO_OPERATION;
        }

        @Override
        public ChangeApplied visit(@Nonnull RemoveAxiom change) {
            OWLAxiom axiom = change.getAxiom();
            if (ints.removeAxiom(axiom)) {
                AxiomParserFactory.get(axiom).reverse(eventStore, inner);
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
