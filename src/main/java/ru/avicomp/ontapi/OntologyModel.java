package ru.avicomp.ontapi;

import org.apache.jena.ontology.OntModel;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * Overwritten {@link OWLOntology}
 *
 * Created by szuev on 24.10.2016.
 */
public interface OntologyModel extends OWLOntology {
    OntModel asGraphModel();

    OntologyManager getOWLOntologyManager();
}
