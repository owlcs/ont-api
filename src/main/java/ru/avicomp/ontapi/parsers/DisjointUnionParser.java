package ru.avicomp.ontapi.parsers;

import java.util.stream.Stream;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.vocabulary.OWL2;
import org.semanticweb.owlapi.model.OWLDisjointUnionAxiom;
import org.semanticweb.owlapi.model.OWLObject;

/**
 * base class: {@link AbstractSubChainedParser}
 * for DisjointUnion
 * example: :MyClass1 owl:disjointUnionOf ( :MyClass2 [ a owl:Class ; owl:unionOf ( :MyClass3 :MyClass4  ) ] ) ;
 * <p>
 * Created by @szuev on 17.10.2016.
 */
class DisjointUnionParser extends AbstractSubChainedParser<OWLDisjointUnionAxiom> {
    @Override
    public OWLObject getSubject() {
        return getAxiom().getOWLClass();
    }

    @Override
    public Property getPredicate() {
        return OWL2.disjointUnionOf;
    }

    @Override
    public Stream<? extends OWLObject> getObjects() {
        return getAxiom().classExpressions();
    }
}
