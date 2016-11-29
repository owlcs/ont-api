package ru.avicomp.ontapi.translators;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL2;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;

/**
 * see {@link AbstractTwoWayNaryTranslator}
 * example:
 * :Complex2 owl:disjointWith  :Simple2 , :Simple1 .
 * OWL2 alternative way:
 * [ a owl:AllDisjointClasses ; owl:members ( :Complex2 :Simple1 :Simple2 ) ] .
 * <p>
 * Created by @szuev on 28.09.2016.
 */
class DisjointClassesTranslator extends AbstractTwoWayNaryTranslator<OWLDisjointClassesAxiom> {
    @Override
    public Property getPredicate() {
        return OWL2.disjointWith;
    }

    @Override
    public Resource getMembersType() {
        return OWL2.AllDisjointClasses;
    }

    @Override
    public Property getMembersPredicate() {
        return OWL2.members;
    }
}
