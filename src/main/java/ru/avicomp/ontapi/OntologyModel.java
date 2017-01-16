package ru.avicomp.ontapi;

import org.semanticweb.owlapi.model.OWLMutableOntology;
import org.semanticweb.owlapi.model.OWLOntology;

import ru.avicomp.ontapi.jena.model.OntGraphModel;

/**
 * OWL 2 Ontology.
 * See base class {@link OWLOntology}
 *
 * Created by szuev on 24.10.2016.
 */
public interface OntologyModel extends OWLOntology, OWLMutableOntology {
    /**
     * returns jena model shadow, i.e. interface to work with graph directly.
     *
     * @return {@link OntGraphModel}
     */
    OntGraphModel asGraphModel();

    /**
     * clears axiom and entity caches.
     * After recalling the method {@link #axioms()} this cache would be collected again.
     * This method is needed to be sure that the list of axioms is the same.
     * OWL-API allows the ambiguity in the definition of axioms.
     * For example you can add axiom with plain annotation attached,
     * but after recalculation the axiom cache would contain those annotations as separated annotation assertion axioms,
     * not as part of the other axioms as it was initially.
     */
    void clearCache();

    /**
     * returns the manager.
     *
     * @return {@link OntologyManager}
     */
    OntologyManager getOWLOntologyManager();

}
