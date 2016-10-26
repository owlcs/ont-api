package ru.avicomp.ontapi.translators;

import org.apache.jena.graph.Graph;
import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.model.HasDomain;
import org.semanticweb.owlapi.model.HasProperty;
import org.semanticweb.owlapi.model.OWLAxiom;

/**
 * base class for {@link ObjectPropertyDomainTranslator} and {@link DataPropertyDomainTranslator} and {@link AnnotationPropertyDomainTranslator}
 * for rdfs:domain tripler.
 * <p>
 * Created by @szuev on 30.09.2016.
 */
abstract class AbstractPropertyDomainTranslator<Axiom extends OWLAxiom & HasDomain & HasProperty> extends AxiomTranslator<Axiom> {
    @Override
    public void process(Graph graph) {
        TranslationHelper.processAnnotatedTriple(graph, getAxiom().getProperty(), RDFS.domain, getAxiom().getDomain(), getAxiom());
    }
}
