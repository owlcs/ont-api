package ru.avicomp.ontapi.parsers.axiom2rdf;

import org.apache.jena.graph.Graph;
import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.model.HasDomain;
import org.semanticweb.owlapi.model.HasProperty;
import org.semanticweb.owlapi.model.OWLAxiom;

/**
 * base class for {@link ObjectPropertyDomainParser} and {@link DataPropertyDomainParser} and {@link AnnotationPropertyDomainParser}
 * for rdfs:domain tripler.
 * <p>
 * Created by @szuev on 30.09.2016.
 */
abstract class AbstractPropertyDomainParser<Axiom extends OWLAxiom & HasDomain & HasProperty> extends AxiomParser<Axiom> {
    @Override
    public void process(Graph graph) {
        TranslationHelper.processAnnotatedTriple(graph, getAxiom().getProperty(), RDFS.domain, getAxiom().getDomain(), getAxiom());
    }
}
