package ru.avicomp.ontapi;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphListener;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.impl.OntModelImpl;
import org.apache.jena.rdf.model.ModelFactory;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.ChangeApplied;

import com.google.inject.assistedinject.Assisted;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.translators.AxiomParserProvider;
import ru.avicomp.ontapi.translators.TranslationHelper;
import ru.avicomp.ontapi.translators.rdf2axiom.GraphParseHelper;
import uk.ac.manchester.cs.owl.owlapi.Internals;
import uk.ac.manchester.cs.owl.owlapi.OWLImmutableOntologyImpl;

import static org.semanticweb.owlapi.model.parameters.ChangeApplied.NO_OPERATION;
import static org.semanticweb.owlapi.model.parameters.ChangeApplied.SUCCESSFULLY;

/**
 * TODO:
 * Created by @szuev on 27.09.2016.
 */
public class OntologyModelImpl extends OWLImmutableOntologyImpl implements OntologyModel {
    private final RDFChangeProcessor rdfProcessor;
    private transient OntGraph outer;
    private final OWLRDFModel base;

    /**
     * @param manager    ontology manager
     * @param ontologyID the id
     */
    @Inject
    public OntologyModelImpl(@Assisted OntologyManager manager, @Assisted OWLOntologyID ontologyID) {
        super(manager, ontologyID);
        base = new OWLRDFModel(manager.getGraphFactory().create());
        base.setOwlID(ontologyID);
        rdfProcessor = new RDFChangeProcessor();
    }

    public OntologyModelImpl(OntologyManager manager, OWLRDFModel base) {
        super(manager, base.getOwlID());
        this.base = base;
        rdfProcessor = new RDFChangeProcessor();
    }

    public OntGraphEventStore getEventStore() {
        return rdfProcessor.eventStore();
    }

    @Override
    public ChangeApplied applyDirectChange(OWLOntologyChange change) {
        ChangeApplied res = change.accept(getRDFChangeProcessor());
        if (SUCCESSFULLY.equals(res)) {
            sync();
        }
        return res;
    }

    @Override
    public ChangeApplied applyChanges(@Nonnull List<? extends OWLOntologyChange> changes) {
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

    RDFChangeProcessor getRDFChangeProcessor() {
        return rdfProcessor;
    }

    private OntGraph getOntGraph() {
        return outer == null ? outer = new OntGraph(this) : outer;
    }

    private void sync() {
        if (outer != null)
            outer.sync();
    }

    private Stream<OWLDeclarationAxiom> declarationAxioms(OWLImportsDeclaration declaration) {
        // what if there are several ontologies with the same IRI ?
        OWLOntology importedOntology = getOWLOntologyManager().getImportedOntology(declaration);
        return importedOntology == null ? Stream.empty() : importedOntology.signature().map(e -> getOWLOntologyManager().getOWLDataFactory().getOWLDeclarationAxiom(e));
    }

    private Stream<OWLDeclarationAxiom> declarationAxioms() {
        return signature().map(e -> getOWLOntologyManager().getOWLDataFactory().getOWLDeclarationAxiom(e));
    }

    /**
     * gets all ontologies from imports
     *
     * @return Stream
     */
    public Stream<OntologyModelImpl> ontologies() {
        return imports().map(OntologyModelImpl.class::cast);
    }

    /**
     * recursively gets all entites.
     *
     * @return Stream
     */
    public Stream<OWLEntity> entities() {
        Stream<OWLEntity> res = signature();
        for (OntologyModelImpl ont : ontologies().collect(Collectors.toSet())) {
            res = Stream.concat(res, ont.entities());
        }
        return res;
    }

    /**
     * todo replace with base model.
     * don't forget to call {@link OntModel#rebind()} after adding bulk axiom.
     *
     * @return OntModel
     */
    public OntModel asGraphModel() {
        return new DeprecatedOntGraphModel(this);
    }

    // todo
    public OntGraphModel getBase() {
        return base;
    }

    @Deprecated
    private static class DeprecatedOntGraphModel extends OntModelImpl {
        DeprecatedOntGraphModel(OntologyModelImpl ontology) {
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

    /**
     * package-private access.
     * WARNING: High Coupling with {@link OntGraph}, {@link OntologyFactoryImpl} and this {@link OntologyModelImpl}!
     */
    class RDFChangeProcessor implements OWLOntologyChangeVisitorEx<ChangeApplied> {

        OntGraphEventStore eventStore() {
            return base.getEventStore();
        }

        OntologyModelImpl getOntology() {
            return OntologyModelImpl.this;
        }

        private OWLOntologyID getOntologyID() {
            return ontologyID;
        }

        private void setOntologyID(OWLOntologyID id) {
            ontologyID = id;
        }

        Internals getInternals() {
            return ints;
        }

        Graph getGraph() {
            return base.getBaseGraph();
        }

        /**
         * puts axioms to this OWLOntology inner graph from external graph
         * these graphs may be different (see specification
         * <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Parsing_of_the_Ontology_Header_and_Declarations'>Parsing of the Ontology Header and Declarations</a>).
         *
         * @param external Graph from.
         */
        @Deprecated
        void load(Graph external) {
            GraphParseHelper.imports(getOntologyID(), external).forEachRemaining(this::addImport);
            GraphParseHelper.declarationAxioms(external).forEachRemaining(axiom -> {
                getInternals().addAxiom(axiom);
                RDFChangeProcessor.this.addAxiom(axiom);
            });
        }

        /**
         * changes ontology id.
         * to produce events doesn't use {@link org.apache.jena.util.ResourceUtils#renameResource}:
         * it's important for changing ontology-id through outer-graph.
         *
         * @param id new OWLOntologyID
         */
        private void changeOntologyID(OWLOntologyID id) {
            GraphListener listener = OntGraphListener.create(eventStore(), OntGraphEventStore.createChange(getOntologyID()));
            Graph inner = getGraph();
            try {
                inner.getEventManager().register(listener);
                Set<OWLImportsDeclaration> imports = getOntology().importsDeclarations().collect(Collectors.toSet());
                Set<OWLAnnotation> annotations = getOntology().annotations().collect(Collectors.toSet());
                // first remove imports and annotations
                imports.forEach(this::removeImport);
                annotations.forEach(this::removeAnnotation);
                // remove ontology whole triplet set
                base.setOwlID(id);
                /*inner.remove(nodeIRI, Node.ANY, Node.ANY);
                // change version iri:
                IRI version = id.getVersionIRI().orElse(null);
                if (version != null) {
                    inner.add(Triple.create(nodeIRI, OWL2.versionIRI.asNode(), NodeIRIUtils.toNode(version)));
                }
                // add new one owl:Ontology
                inner.add(Triple.create(nodeIRI = createNodeIRI(id), RDF.type.asNode(), OWL.Ontology.asNode()));*/
                // return back imports:
                imports.forEach(this::addImport);
                // return back annotations:
                annotations.forEach(this::addAnnotation);
                setOntologyID(base.getOwlID());
            } finally {
                inner.getEventManager().unregister(listener);
            }
        }

        private void addImport(OWLImportsDeclaration declaration) {
            OntGraphEventStore.OWLEvent event = OntGraphEventStore.createAdd(declaration);
            GraphListener listener = OntGraphListener.create(eventStore(), event);
            Graph inner = getGraph();
            try {
                inner.getEventManager().register(listener); // todo
                OntologyModelImpl ont = (OntologyModelImpl) getOWLOntologyManager().getImportedOntology(declaration);
                if (ont != null) {
                    base.addImport(ont.getBase());
                } else {
                    base.addImport(declaration.getIRI().getIRIString());
                }
                //inner.add(Triple.create(nodeIRI(), OWL.imports.asNode(), NodeIRIUtils.toNode(declaration.getIRI())));
            } finally {
                eventStore().clear(event.reverse());
                inner.getEventManager().unregister(listener);
            }
            // remove duplicated Declaration Axioms if they are present in the imported ontology
            Set<OWLAxiom> declarationAxiomsFromImport = getOntology().declarationAxioms(declaration).collect(Collectors.toSet());
            getOntology().declarationAxioms().filter(declarationAxiomsFromImport::contains).forEach(axiom -> {
                getInternals().removeAxiom(axiom); // don't use this.removeAxiom to avoid breaking other non-declaration axioms which could use these triplets also
                eventStore().triples(OntGraphEventStore.createAdd(axiom)).map(OntGraphEventStore.TripleEvent::get).forEach(inner::delete);
            });
        }

        private void removeImport(OWLImportsDeclaration declaration) {
            OntGraphEventStore.OWLEvent event = OntGraphEventStore.createRemove(declaration);
            GraphListener listener = OntGraphListener.create(eventStore(), event);
            Graph inner = getGraph();
            try {
                inner.getEventManager().register(listener);
                OntologyModelImpl ont = (OntologyModelImpl) getOWLOntologyManager().getImportedOntology(declaration);
                if (ont != null) { // todo
                    base.removeImport(ont.getBase());
                } else {
                    base.removeImport(declaration.getIRI().getIRIString());
                }
                //inner.remove(nodeIRI(), OWL.imports.asNode(), NodeIRIUtils.toNode(declaration.getIRI()));
            } finally {
                eventStore().clear(event.reverse());
                inner.getEventManager().unregister(listener);
            }
            // return back Declaration Axioms which is in use:
            Set<OWLAxiom> declarationAxiomsFromThis = getOntology().declarationAxioms().collect(Collectors.toSet());
            getOntology().declarationAxioms(declaration).filter(declarationAxiomsFromThis::contains).forEach(this::addAxiom);
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
            GraphListener listener = OntGraphListener.create(eventStore(), event);
            Graph inner = getGraph();
            try {
                inner.getEventManager().register(listener);
                TranslationHelper.addAnnotations(base.getID(), Stream.of(annotation));
            } finally {
                eventStore().clear(event.reverse());
                inner.getEventManager().unregister(listener);
            }
        }

        private void removeAnnotation(OWLAnnotation annotation) {
            OntGraphEventStore.OWLEvent event = OntGraphEventStore.createRemove(annotation);
            GraphListener listener = OntGraphListener.create(eventStore(), event);
            Graph inner = getGraph();
            try {
                inner.getEventManager().register(listener);
                removeAllTriples(inner, event.reverse());
            } finally {
                eventStore().clear(event.reverse());
                inner.getEventManager().unregister(listener);
            }
        }

        /**
         * @param axiom OWLAxiom
         */
        private void addAxiom(OWLAxiom axiom) {
            OntGraphEventStore.OWLEvent event = OntGraphEventStore.createAdd(axiom);
            GraphListener listener = OntGraphListener.create(eventStore(), event);
            Graph inner = getGraph();
            try {
                inner.getEventManager().register(listener);
                AxiomParserProvider.get(axiom).write(axiom, base);
            } catch (Exception e) {
                throw new OntApiException("Add axiom " + axiom, e);
            } finally {
                eventStore().clear(event.reverse());
                inner.getEventManager().unregister(listener);
            }
        }

        private void removeAxiom(OWLAxiom axiom) {
            OntGraphEventStore.OWLEvent event = OntGraphEventStore.createRemove(axiom);
            GraphListener listener = OntGraphListener.create(eventStore(), event);
            Graph inner = getGraph();
            try {
                inner.getEventManager().register(listener);
                removeAllTriples(inner, event.reverse());
            } catch (Exception e) {
                throw new OntApiException("Remove axiom " + axiom, e);
            } finally {
                eventStore().clear(event.reverse());
                inner.getEventManager().unregister(listener);
            }
        }

        private void removeAllTriples(Graph graph, OntGraphEventStore.OWLEvent event) {
            eventStore().triples(event).
                    filter(t -> eventStore().count(t) < 2). // skip triplets which are included in several axioms
                    map(OntGraphEventStore.TripleEvent::get).forEach(graph::delete);
        }

        /**
         * @param change AddAxiom object
         * @return ChangeApplied enum
         */
        @Override
        public ChangeApplied visit(@Nonnull AddAxiom change) {
            OWLAxiom axiom = change.getAxiom();
            if (getInternals().addAxiom(axiom)) {
                addAxiom(axiom);
                return SUCCESSFULLY;
            }
            return NO_OPERATION;
        }

        @Override
        public ChangeApplied visit(@Nonnull RemoveAxiom change) {
            OWLAxiom axiom = change.getAxiom();
            if (getInternals().removeAxiom(axiom)) {
                removeAxiom(axiom);
                return SUCCESSFULLY;
            }
            return NO_OPERATION;
        }

        @Override
        public ChangeApplied visit(@Nonnull AddImport change) {
            OWLImportsDeclaration importDeclaration = change.getImportDeclaration();
            if (getInternals().addImportsDeclaration(importDeclaration)) {
                addImport(importDeclaration);
                return SUCCESSFULLY;
            }
            return NO_OPERATION;
        }

        @Override
        public ChangeApplied visit(@Nonnull RemoveImport change) {
            OWLImportsDeclaration importDeclaration = change.getImportDeclaration();
            if (getInternals().removeImportsDeclaration(importDeclaration)) {
                removeImport(importDeclaration);
                return SUCCESSFULLY;
            }
            return NO_OPERATION;
        }

        @Override
        public ChangeApplied visit(@Nonnull AddOntologyAnnotation change) {
            OWLAnnotation annotation = change.getAnnotation();
            if (getInternals().addOntologyAnnotation(annotation)) {
                addAnnotation(annotation);
                return SUCCESSFULLY;
            }
            return NO_OPERATION;
        }

        @Override
        public ChangeApplied visit(@Nonnull RemoveOntologyAnnotation change) {
            OWLAnnotation annotation = change.getAnnotation();
            if (getInternals().removeOntologyAnnotation(annotation)) {
                removeAnnotation(annotation);
                return SUCCESSFULLY;
            }
            return NO_OPERATION;
        }

        @Override
        public ChangeApplied visit(@Nonnull SetOntologyID change) {
            OWLOntologyID id = change.getNewOntologyID();
            if (!getOntologyID().equals(id)) {
                changeOntologyID(id);
                return SUCCESSFULLY;
            }
            return NO_OPERATION;
        }
    }
}
