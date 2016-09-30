package ru.avicomp.ontapi.parsers;

import org.apache.jena.graph.Graph;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAxiom;

import ru.avicomp.ontapi.NodeIRIUtils;

/**
 * Class for parse axiom which is related to single triplet.
 * <p>
 * Created by @szuev on 28.09.2016.
 */
abstract class SingleTripletParser<Axiom extends OWLAxiom> extends AxiomParser<Axiom> {

    public abstract OWLAnnotationValue getSubject();

    public abstract IRI getPredicate();

    public abstract OWLAnnotationValue getObject();

    @Override
    public void process(Graph graph) {
        graph.add(NodeIRIUtils.toTriple(getSubject(), getPredicate(), getObject()));
    }
}
