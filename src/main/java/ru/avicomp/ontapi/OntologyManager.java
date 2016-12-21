package ru.avicomp.ontapi;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.jena.graph.Graph;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import ru.avicomp.ontapi.jena.model.OntGraphModel;

import static org.semanticweb.owlapi.util.OWLAPIPreconditions.emptyOptional;
import static org.semanticweb.owlapi.util.OWLAPIPreconditions.optional;

/**
 * Ontology manager.
 * Base class: {@link OWLOntologyManager}
 * It is the main point for creating, loading and accessing {@link OntologyModel}s models.
 *
 * Created by szuev on 24.10.2016.
 */
public interface OntologyManager extends OWLOntologyManager {

    OntologyModel getOntology(@Nullable IRI iri);

    OntologyModel getOntology(@Nonnull OWLOntologyID id);

    OntologyModel createOntology(@Nonnull OWLOntologyID id);

    void setGraphFactory(GraphFactory factory);

    GraphFactory getGraphFactory();

    default OntGraphModel getGraphModel(@Nullable String uri) {
        OntologyModel res = getOntology(uri == null ? null : IRI.create(uri));
        return res == null ? null : res.asGraphModel();
    }

    default OntGraphModel createGraphModel(String uri) {
        return createOntology(IRI.create(uri)).asGraphModel();
    }

    default OntologyModel createOntology() {
        return createOntology(new OWLOntologyID());
    }

    default OntologyModel createOntology(IRI iri) {
        return createOntology(new OWLOntologyID(optional(iri), emptyOptional(IRI.class)));
    }

    interface GraphFactory {
        Graph create();
    }
}
