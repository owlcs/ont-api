package ru.avicomp.ontapi;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.List;
import java.util.stream.Stream;

import org.apache.jena.graph.Graph;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.impl.OntModelImpl;
import org.apache.jena.rdf.model.ModelFactory;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.ChangeApplied;
import org.semanticweb.owlapi.model.parameters.Imports;

import com.google.inject.assistedinject.Assisted;
import ru.avicomp.ontapi.jena.model.OntGraphModel;

import static org.semanticweb.owlapi.model.parameters.ChangeApplied.NO_OPERATION;
import static org.semanticweb.owlapi.model.parameters.ChangeApplied.SUCCESSFULLY;

/**
 * TODO:
 * Created by @szuev on 27.09.2016.
 */
public class OntologyModelImpl extends OntBaseModelImpl implements OntologyModel {
    private final RDFChangeProcessor rdfProcessor;
    private transient OntGraph outer;

    /**
     * @param manager ontology manager
     * @param id      the id
     */
    @Inject
    public OntologyModelImpl(@Assisted OntologyManager manager, @Assisted OWLOntologyID id) {
        super(manager, id);
        rdfProcessor = new RDFChangeProcessor();
    }

    public OntologyModelImpl(OntologyManager manager, OntInternalModel base) {
        super(manager, base);
        rdfProcessor = new RDFChangeProcessor();
    }

    @Deprecated
    public OntGraphEventStore getEventStore() {
        return base.getEventStore();
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

    @Override //todo:
    public OntologyManager getOWLOntologyManager() {
        return (OntologyManager) super.getOWLOntologyManager();
    }

    private RDFChangeProcessor getRDFChangeProcessor() {
        return rdfProcessor;
    }

    private OntGraph getOntGraph() {
        return outer == null ? outer = new OntGraph(this) : outer;
    }

    Graph getInnerGraph() {
        return base.getBaseGraph();
    }

    private void sync() {
        if (outer != null)
            outer.sync();
    }

    /**
     * todo replace with base model.
     * don't forget to call {@link OntModel#rebind()} after adding bulk axiom.
     *
     * @return OntModel
     */
    public OntGraphModel asGraphModel() {
        return getBase();
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

    private class RDFChangeProcessor implements OWLOntologyChangeVisitorEx<ChangeApplied> {

        private void addImport(OWLImportsDeclaration declaration) {
            OntologyModelImpl ont = (OntologyModelImpl) getOWLOntologyManager().getImportedOntology(declaration);
            if (ont == null) {
                getBase().addImport(declaration.getIRI().getIRIString());
                return;
            }
            Stream<OWLDeclarationAxiom> duplicates = ont.axioms(AxiomType.DECLARATION, Imports.INCLUDED).filter(OntologyModelImpl.this::containsAxiom);
            getBase().addImport(ont.getBase());
            // remove duplicated Declaration Axioms if they are present in the imported ontology
            duplicates.forEach(a -> getBase().remove(a));
        }

        private void removeImport(OWLImportsDeclaration declaration) {
            OntologyModelImpl ont = (OntologyModelImpl) getOWLOntologyManager().getImportedOntology(declaration);
            if (ont == null) {
                getBase().removeImport(declaration.getIRI().getIRIString());
                return;
            }
            Stream<OWLEntity> back = ont.signature(Imports.INCLUDED).filter(OntologyModelImpl.this::containsReference);
            getBase().removeImport(ont.getBase());
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
            if (!getOntologyID().equals(id)) {
                base.setOwlID(id);
                return SUCCESSFULLY;
            }
            return NO_OPERATION;
        }
    }
}
