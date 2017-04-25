package ru.avicomp.ontapi;

import org.semanticweb.owlapi.model.OWLMutableOntology;
import org.semanticweb.owlapi.model.OWLOntology;

import ru.avicomp.ontapi.jena.model.OntGraphModel;

/**
 * OWL 2 Ontology.
 * It is access point to the structural (OWL) representation of underlying graph.
 *
 * @see OWLOntology
 * Created by szuev on 24.10.2016.
 */
public interface OntologyModel extends OWLOntology, OWLMutableOntology {

    /**
     * Returns the jena model shadow, i.e. the interface to work with the graph directly.
     *
     * @return {@link OntGraphModel}
     */
    OntGraphModel asGraphModel();

    /**
     * Clears the axioms & entities cache.
     * <p>
     * The cache are restored by recalling method {@link #axioms()}, which is called by any other axioms getter.
     * This method is necessary to obtain the list of axioms which uniquely correspond to the graph,
     * since OWL-API allows some ambiguity in the axioms definition.
     * In the structural view there could be composite and bulky axioms specified,
     * which can be replaced by different set of axioms without any loss of information.
     * This method allows to bring structural representation to the one strictly defined (by inner implementation) form.
     * An example.
     * Consider the ontology which contains only the following two axioms:
     * <pre>
     *  SubClassOf(Annotation(<p> "comment1"^^xsd:string) <a> <b>)
     *  Declaration(Annotation(rdfs:label "label"^^xsd:string) Datatype(<d>))
     * </pre>
     * After re-caching the full list of axioms would be the following:
     * <pre>
     *  Declaration(Class(<a>))
     *  Declaration(Class(<b>))
     *  Declaration(AnnotationProperty(<p>))
     *  Declaration(Datatype(<d>))
     *  SubClassOf(Annotation(<p> "comment"^^xsd:string) <a> <b>)
     *  AnnotationAssertion(rdfs:label <d> "label"^^xsd:string)
     * </pre>
     * Note: the loading behaviour and the axioms list above may vary according to various config settings,
     * for more details see {@link ru.avicomp.ontapi.config.OntLoaderConfiguration}.
     */
    void clearCache();

    /**
     * Returns the manager.
     *
     * @return {@link OntologyManager}
     */
    OntologyManager getOWLOntologyManager();

}
