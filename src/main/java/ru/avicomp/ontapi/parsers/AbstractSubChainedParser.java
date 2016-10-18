package ru.avicomp.ontapi.parsers;

import java.util.stream.Stream;

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.Property;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLObject;

/**
 * base class for {@link HasKeyParser}, {@link SubPropertyChainOfParser}, {@link DisjointUnionParser}
 * <p>
 * Created by @szuev on 18.10.2016.
 */
abstract class AbstractSubChainedParser<Axiom extends OWLLogicalAxiom> extends AxiomParser<Axiom> {

    public abstract OWLObject getSubject();

    public abstract Property getPredicate();

    public abstract Stream<? extends OWLObject> getObjects();

    @Override
    public void process(Graph graph) {
        TranslationHelper.processAnnotatedTriple(graph, getSubject(), getPredicate(), getObjects(), getAxiom(), true);
    }
}
