package ru.avicomp.ontapi;

import javax.annotation.Nonnull;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.stream.Stream;

import org.apache.jena.graph.Graph;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.ChangeApplied;
import org.semanticweb.owlapi.model.parameters.Imports;

import ru.avicomp.ontapi.internal.ConfigProvider;
import ru.avicomp.ontapi.internal.InternalModel;
import ru.avicomp.ontapi.internal.InternalModelHolder;
import ru.avicomp.ontapi.jena.ConcurrentGraph;
import ru.avicomp.ontapi.jena.OntFactory;
import ru.avicomp.ontapi.jena.UnionGraph;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import uk.ac.manchester.cs.owl.owlapi.concurrent.ConcurrentOWLOntologyImpl;

import static org.semanticweb.owlapi.model.parameters.ChangeApplied.NO_OPERATION;
import static org.semanticweb.owlapi.model.parameters.ChangeApplied.SUCCESSFULLY;

/**
 * The main ontology model. Editable. Provides access to {@link OntGraphModel}
 * <p>
 * Created by @szuev on 27.09.2016.
 */
@SuppressWarnings("WeakerAccess")
public class OntologyModelImpl extends OntBaseModelImpl implements OntologyModel {

    private transient RDFChangeProcessor changer;

    /**
     * @param manager ontology manager
     * @param id      the id
     */
    public OntologyModelImpl(OntologyManagerImpl manager, OWLOntologyID id) {
        super(manager, id);
    }

    public OntologyModelImpl(Graph graph, OntologyManagerImpl.ModelConfig config) {
        super(graph, config);
    }

    @Override
    public ChangeApplied applyDirectChange(OWLOntologyChange change) {
        return change.accept(getRDFChangeProcessor());
    }

    @Override
    public OntologyManagerImpl getOWLOntologyManager() {
        return (OntologyManagerImpl) super.getOWLOntologyManager();
    }

    private RDFChangeProcessor getRDFChangeProcessor() {
        return changer == null ? changer = new RDFChangeProcessor() : changer;
    }

    @Override
    public void clearCache() {
        getBase().clearCache();
    }

    /**
     * Returns the jena model shadow.
     *
     * @return {@link OntGraphModel}
     */
    @Override
    public OntGraphModel asGraphModel() {
        return getBase();
    }

    /**
     * Returns concurrent representation of model if it is allowed by manager.
     * For internal usage only
     *
     * @return {@link OntologyModel}
     */
    public OntologyModel asConcurrent() {
        OntologyManagerImpl manager = getOWLOntologyManager();
        if (!manager.isConcurrent()) {
            throw new OntApiException.Unsupported("Concurrency is not allowed.");
        }

        return new Concurrent(this, manager.getLock());
    }

    private class RDFChangeProcessor implements OWLOntologyChangeVisitorEx<ChangeApplied> {

        private void addImport(OWLImportsDeclaration declaration) {
            // to match behaviour of OWL-API add to graph only single IRI - either ontology IRI or specified declaration IRI.
            OntologyModel ont = getOWLOntologyManager().getImportedOntology(declaration);
            if (ont == null) {
                getBase().getID().addImport(declaration.getIRI().getIRIString());
                return;
            }
            // todo: move this logic to the manager, make this configurable (writer conf)
            Stream<OWLDeclarationAxiom> duplicates = ont.axioms(AxiomType.DECLARATION, Imports.INCLUDED).filter(OntologyModelImpl.this::containsAxiom);
            getBase().addImport(((InternalModelHolder) ont).getBase());
            // remove duplicated Declaration Axioms if they are present in the imported ontology
            duplicates.forEach(a -> getBase().remove(a));
        }

        private void removeImport(OWLImportsDeclaration declaration) {
            // to match behaviour of OWL-API removes both declaration IRI and ontology IRI (could be different in case of renaming)
            OntologyModel ont = getOWLOntologyManager().getImportedOntology(declaration);
            getBase().getID().removeImport(declaration.getIRI().getIRIString());
            if (ont == null) {
                return;
            }
            // todo: move somewhere (manager)
            Stream<OWLEntity> back = ont.signature(Imports.INCLUDED).filter(OntologyModelImpl.this::containsReference);
            getBase().removeImport(((InternalModelHolder) ont).getBase());
            // return back Declaration Axioms which is in use:
            back.map(e -> getOWLOntologyManager().getOWLDataFactory().getOWLDeclarationAxiom(e)).forEach(a -> getBase().add(a));
        }

        /**
         * @param change AddAxiom object
         * @return ChangeApplied enum
         */
        @Override
        public ChangeApplied visit(@Nonnull AddAxiom change) {
            OWLAxiom axiom = change.getAxiom();
            if (containsAxiom(axiom)) {
                return NO_OPERATION;
            }
            getBase().add(axiom);
            return SUCCESSFULLY;
        }

        @Override
        public ChangeApplied visit(@Nonnull RemoveAxiom change) {
            OWLAxiom axiom = change.getAxiom();
            if (containsAxiom(axiom)) {
                getBase().remove(axiom);
                return SUCCESSFULLY;
            }
            return NO_OPERATION;
        }

        @Override
        public ChangeApplied visit(@Nonnull AddImport change) {
            OWLImportsDeclaration importDeclaration = change.getImportDeclaration();
            if (importsDeclarations().noneMatch(importDeclaration::equals)) {
                addImport(importDeclaration);
                return SUCCESSFULLY;
            }
            return NO_OPERATION;
        }

        @Override
        public ChangeApplied visit(@Nonnull RemoveImport change) {
            OWLImportsDeclaration importDeclaration = change.getImportDeclaration();
            if (importsDeclarations().anyMatch(importDeclaration::equals)) {
                removeImport(importDeclaration);
                return SUCCESSFULLY;
            }
            return NO_OPERATION;
        }

        @Override
        public ChangeApplied visit(@Nonnull AddOntologyAnnotation change) {
            OWLAnnotation annotation = change.getAnnotation();
            if (annotations().noneMatch(annotation::equals)) {
                getBase().add(annotation);
                return SUCCESSFULLY;
            }
            return NO_OPERATION;
        }

        @Override
        public ChangeApplied visit(@Nonnull RemoveOntologyAnnotation change) {
            OWLAnnotation annotation = change.getAnnotation();
            if (annotations().anyMatch(annotation::equals)) {
                getBase().remove(annotation);
                return SUCCESSFULLY;
            }
            return NO_OPERATION;
        }

        @Override
        public ChangeApplied visit(@Nonnull SetOntologyID change) {
            OWLOntologyID id = change.getNewOntologyID();
            if (getOntologyID().equals(id)) {
                return NO_OPERATION;
            }
            setOntologyID(id);
            return SUCCESSFULLY;
        }
    }

    /**
     * Analogue of {@link ConcurrentOWLOntologyImpl}
     * <p>
     * Created by szuev on 22.12.2016.
     */
    @SuppressWarnings("WeakerAccess")
    public static class Concurrent extends ConcurrentOWLOntologyImpl implements OntologyModel, ConfigProvider, InternalModelHolder {
        protected OntologyModelImpl delegate;
        protected ReadWriteLock lock;

        protected Concurrent(OntologyModelImpl delegate, ReadWriteLock lock) {
            super(delegate, lock);
            this.delegate = delegate;
            this.lock = lock;
        }

        public ReadWriteLock getLock() {
            return lock;
        }

        /**
         * @return {@link OntGraphModel}
         */
        @Override
        public OntGraphModel asGraphModel() {
            try {
                lock.readLock().lock();
                return makeGraphModel();
            } finally {
                lock.readLock().unlock();
            }
        }

        protected OntGraphModel makeGraphModel() {
            UnionGraph thisGraph = getBase().getGraph();
            UnionGraph newGraph = new UnionGraph(new ConcurrentGraph(thisGraph.getBaseGraph(), lock), thisGraph.getEventManager());
            thisGraph.getUnderlying().graphs().forEach(newGraph::addGraph);
            return OntFactory.createModel(newGraph, getConfig().loaderConfig().getPersonality());
        }

        @Override
        public void clearCache() {
            try {
                lock.writeLock().lock();
                delegate.clearCache();
            } finally {
                lock.writeLock().unlock();
            }
        }

        @Override
        public ConfigProvider.Config getConfig() {
            try {
                lock.readLock().lock();
                return delegate.getConfig();
            } finally {
                lock.readLock().unlock();
            }
        }

        @Override
        public OntologyManager getOWLOntologyManager() {
            return (OntologyManager) super.getOWLOntologyManager();
        }

        @Override
        public InternalModel getBase() {
            return delegate.getBase();
        }

        @Override
        public void setBase(InternalModel m) {
            delegate.setBase(m);
        }

    }
}
