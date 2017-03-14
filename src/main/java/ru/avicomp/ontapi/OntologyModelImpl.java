package ru.avicomp.ontapi;

import javax.annotation.Nonnull;
import java.util.stream.Stream;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.ChangeApplied;
import org.semanticweb.owlapi.model.parameters.Imports;

import com.google.inject.assistedinject.Assisted;
import ru.avicomp.ontapi.internal.InternalModel;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import uk.ac.manchester.cs.owl.owlapi.concurrent.ConcurrentOWLOntologyImpl;

import static org.semanticweb.owlapi.model.parameters.ChangeApplied.NO_OPERATION;
import static org.semanticweb.owlapi.model.parameters.ChangeApplied.SUCCESSFULLY;

/**
 * The main ontology model. Editable. Provides access to {@link OntGraphModel}
 * <p>
 * Created by @szuev on 27.09.2016.
 */
public class OntologyModelImpl extends OntBaseModelImpl implements OntologyModel {

    private transient RDFChangeProcessor changer;

    /**
     * @param manager ontology manager
     * @param id      the id
     */
    public OntologyModelImpl(@Assisted OntologyManager manager, @Assisted OWLOntologyID id) {
        super(manager, id);
    }

    public OntologyModelImpl(OntologyManager manager, InternalModel base) {
        super(manager, base);
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
     * returns jena model shadow.
     *
     * @return {@link OntGraphModel}
     */
    @Override
    public OntGraphModel asGraphModel() {
        return getBase();
    }

    public OntologyModel toConcurrentModel() {
        OntologyManagerImpl manager = getOWLOntologyManager();
        if (!manager.isConcurrent()) throw new OntApiException.Unsupported("Concurrency is not allowed");
        return new Concurrent();
    }

    private class RDFChangeProcessor implements OWLOntologyChangeVisitorEx<ChangeApplied> {

        private void addImport(OWLImportsDeclaration declaration) {
            // to match behaviour of OWL-API add to graph only single IRI - either ontology IRI or specified declaration IRI.
            OntologyModel ont = getOWLOntologyManager().getImportedOntology(declaration);
            if (ont == null) {
                getBase().getID().addImport(declaration.getIRI().getIRIString());
                return;
            }
            // todo: move this logic to internal model or manager, make this configurable (writer conf)
            Stream<OWLDeclarationAxiom> duplicates = ont.axioms(AxiomType.DECLARATION, Imports.INCLUDED).filter(OntologyModelImpl.this::containsAxiom);
            getBase().addImport(((OntologyModelImpl) ont).getBase());
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
            getBase().removeImport(((OntologyModelImpl) ont).getBase());
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
    public class Concurrent extends ConcurrentOWLOntologyImpl implements OntologyModel {
        private Concurrent() {
            super(OntologyModelImpl.this, OntologyModelImpl.this.getOWLOntologyManager().getLock());
        }

        /**
         * todo: jena model is not synchronized. prepare some concurrent graph with owl-style synchronization.
         *
         * @return {@link OntGraphModel}
         */
        @Override
        public OntGraphModel asGraphModel() { // todo: not concurrent
            return OntologyModelImpl.this.asGraphModel();
        }

        @Override
        public void clearCache() {
            // todo: write lock
            OntologyModelImpl.this.clearCache();
        }

        @Override
        public OntologyManager getOWLOntologyManager() {
            return (OntologyManager) super.getOWLOntologyManager();
        }
    }
}
