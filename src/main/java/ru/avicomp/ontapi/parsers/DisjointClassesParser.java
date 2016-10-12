package ru.avicomp.ontapi.parsers;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.OWL2;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;

/**
 * see {@link AbstractNaryParser}
 * example:
 * :Complex2 owl:disjointWith  :Simple2 , :Simple1 .
 * OWL2 alternative way:
 * [ a owl:AllDisjointClasses ; owl:members ( :Complex2 :Simple1 :Simple2 ) ] .
 * <p>
 * Created by @szuev on 28.09.2016.
 */
class DisjointClassesParser extends AbstractNaryParser<OWLDisjointClassesAxiom> {

    @Override
    public Property getPredicate() {
        return OWL.disjointWith;
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
