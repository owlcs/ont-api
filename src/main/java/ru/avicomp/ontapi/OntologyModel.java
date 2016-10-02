package ru.avicomp.ontapi;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.graph.Factory;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.ProfileRegistry;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
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

    private Graph graph;
    private Map<OWLAxiom, Node> rootAnonNodes = new HashMap<>();

    private ChangeFilter filter;
    private OntModelSpec spec;

    /**
     * @param manager    ontology manager
     * @param ontologyID the id
     */
    @Inject
    public OntologyModel(@Assisted OWLOntologyManager manager, @Assisted OWLOntologyID ontologyID) {
        super(manager, ontologyID);
        spec = OntModelSpec.getDefaultSpec(ProfileRegistry.OWL_LANG);
        graph = Factory.createGraphMem();
    }

    @Override
    public ChangeApplied applyDirectChange(OWLOntologyChange change) {
        return change.accept(getChangeFilter());
    }

    @Override
    public ChangeApplied applyChanges(List<? extends OWLOntologyChange> changes) {
        ChangeApplied appliedChanges = SUCCESSFULLY;
        for (OWLOntologyChange change : changes) {
            ChangeApplied result = change.accept(getChangeFilter());
            if (appliedChanges == SUCCESSFULLY) {
                // overwrite only if appliedChanges is still successful. If one
                // change has been unsuccessful, we want to preserve that
                // information
                appliedChanges = result;
            }
        }
        return appliedChanges;
    }

    public OWLOntologyChangeVisitorEx<ChangeApplied> getChangeFilter() {
        return filter == null ? filter = new ChangeFilter() : filter;
    }

    private void addToGraph(OWLAnnotationValue s, IRI p, OWLAnnotationValue o) {
        addToGraph(toTriple(s, p, o));
    }

    private void addToGraph(Triple triple) {
        graph.add(triple);
    }

    public Model getModel() {
        return ModelFactory.createModelForGraph(graph);
    }

    public OntModel getOntModel() {
        return ModelFactory.createOntologyModel(spec, getModel());
    }

    private class ChangeFilter implements OWLOntologyChangeVisitorEx<ChangeApplied> {

        private void initOntologyTriplets() {
            IRI iri = ontologyID.getOntologyIRI().orElse(null);
            IRI versionIRI = ontologyID.getVersionIRI().orElse(null);
            if (iri == null) return;
            addToGraph(toTriple(iri, fromResource(RDF.type), fromResource(OWL.Ontology)));
            if (versionIRI == null) return;
            addToGraph(toTriple(iri, fromResource(OWL2.versionIRI), versionIRI));
        }

        private IRI getOntologyIRI() {
            return ontologyID.getOntologyIRI().orElseThrow(() -> new OntException("Null ontology iri"));
        }

        @Override
        public ChangeApplied visit(@Nonnull RemoveAxiom change) {
            if (ints.removeAxiom(change.getAxiom())) {
                return SUCCESSFULLY;
            }
            return NO_OPERATION;
        }

        @Override
        public ChangeApplied visit(@Nonnull SetOntologyID change) {
            OWLOntologyID id = change.getNewOntologyID();
            if (!id.equals(ontologyID)) {
                ontologyID = id;
                initOntologyTriplets();
                return SUCCESSFULLY;
            }
            return NO_OPERATION;
        }

        /**
         * todo:
         *
         * @param change
         * @return
         */
        @Override
        public ChangeApplied visit(@Nonnull AddAxiom change) {
            OWLAxiom axiom = change.getAxiom();
            if (ints.addAxiom(axiom)) {
                AxiomParserFactory.get(axiom).process(graph);
                return SUCCESSFULLY;
            }
            return NO_OPERATION;
        }

        @Override
        public ChangeApplied visit(@Nonnull AddImport change) {
            OWLImportsDeclaration importDeclaration = change.getImportDeclaration();
            if (ints.addImportsDeclaration(importDeclaration)) {
                initOntologyTriplets();
                addToGraph(getOntologyIRI(), fromResource(OWL.imports), importDeclaration.getIRI());
                return SUCCESSFULLY;
            }
            return NO_OPERATION;
        }

        @Override
        public ChangeApplied visit(@Nonnull RemoveImport change) {
            if (ints.removeImportsDeclaration(change.getImportDeclaration())) {
                return SUCCESSFULLY;
            }
            return NO_OPERATION;
        }

        @Override
        public ChangeApplied visit(@Nonnull AddOntologyAnnotation change) {
            if (ints.addOntologyAnnotation(change.getAnnotation())) {
                initOntologyTriplets();
                OWLAnnotation annotation = change.getAnnotation();
                OWLAnnotationProperty property = annotation.getProperty();
                OWLAnnotationValue value = annotation.getValue();
                OWLAnnotationValue literal = value.isIRI() ? value : value.asLiteral().orElse(null);
                addToGraph(getOntologyIRI(), property.getIRI(), OntException.notNull(literal, "Null literal, axiom: " + change));
                return SUCCESSFULLY;
            }
            return NO_OPERATION;
        }

        @Override
        public ChangeApplied visit(@Nonnull RemoveOntologyAnnotation change) {
            if (ints.removeOntologyAnnotation(change.getAnnotation())) {
                return SUCCESSFULLY;
            }
            return NO_OPERATION;
        }
    }
}
