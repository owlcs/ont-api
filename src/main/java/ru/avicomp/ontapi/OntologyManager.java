package ru.avicomp.ontapi;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.jena.graph.Graph;
import org.apache.jena.ontology.OntModelSpec;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import static org.semanticweb.owlapi.util.OWLAPIPreconditions.emptyOptional;
import static org.semanticweb.owlapi.util.OWLAPIPreconditions.optional;

/**
 * Overwritten {@link OWLOntologyManager}
 *
 * Created by szuev on 24.10.2016.
 */
public interface OntologyManager extends OWLOntologyManager {
    @Deprecated
    OntModelSpec getSpec();

    OntologyModel getOntology(@Nullable IRI ontologyIRI);

    OntologyModel getOntology(@Nonnull OWLOntologyID ontologyID);

    OntologyModel createOntology(@Nonnull OWLOntologyID ontologyID);

    default OntologyModel createOntology() {
        return createOntology(new OWLOntologyID());
    }

    default OntologyModel createOntology(IRI ontologyIRI) {
        return createOntology(new OWLOntologyID(optional(ontologyIRI), emptyOptional(IRI.class)));
    }

    void setGraphFactory(GraphFactory factory);

    GraphFactory getGraphFactory();

    interface GraphFactory {
        Graph create();
    }
}
