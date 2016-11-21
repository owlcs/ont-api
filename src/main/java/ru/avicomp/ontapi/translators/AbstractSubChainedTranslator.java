package ru.avicomp.ontapi.translators;

import java.util.stream.Stream;

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.Property;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLObject;

/**
 * base class for {@link HasKeyTranslator}, {@link SubPropertyChainOfTranslator}, {@link DisjointUnionTranslator}
 * <p>
 * Created by @szuev on 18.10.2016.
 */
abstract class AbstractSubChainedTranslator<Axiom extends OWLLogicalAxiom> extends AxiomTranslator<Axiom> {

    public abstract OWLObject getSubject(Axiom axiom);

    public abstract Property getPredicate();

    public abstract Stream<? extends OWLObject> getObjects(Axiom axiom);

    @Override
    public void write(Axiom axiom, Graph graph) {
        TranslationHelper.processAnnotatedTriple(graph, getSubject(axiom), getPredicate(), getObjects(axiom), axiom, true);
    }
}
