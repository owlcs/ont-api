package ru.avicomp.ontapi;

import org.semanticweb.owlapi.model.OWLMutableOntology;
import org.semanticweb.owlapi.model.OWLOntology;

import ru.avicomp.ontapi.jena.model.OntGraphModel;

/**
 * Overwritten {@link OWLOntology}
 *
 * Created by szuev on 24.10.2016.
 */
public interface OntologyModel extends OWLOntology, OWLMutableOntology {
    OntGraphModel asGraphModel();

    OntologyManager getOWLOntologyManager();
}
