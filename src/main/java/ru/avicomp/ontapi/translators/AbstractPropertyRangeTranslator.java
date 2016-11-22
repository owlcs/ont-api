package ru.avicomp.ontapi.translators;

import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.model.HasProperty;
import org.semanticweb.owlapi.model.HasRange;
import org.semanticweb.owlapi.model.OWLAxiom;

import ru.avicomp.ontapi.jena.model.OntGraphModel;

/**
 * base class for {@link DataPropertyRangeTranslator} and {@link ObjectPropertyRangeTranslator} and {@link AnnotationPropertyRangeTranslator}
 * example: foaf:name rdfs:range rdfs:Literal
 * <p>
 * Created by @szuev on 30.09.2016.
 */
abstract class AbstractPropertyRangeTranslator<Axiom extends OWLAxiom & HasProperty & HasRange> extends AxiomTranslator<Axiom> {
    @Override
    public void write(Axiom axiom, OntGraphModel graph) {
        TranslationHelper.processAnnotatedTriple(graph, axiom.getProperty(), RDFS.range, axiom.getRange(), axiom);
    }
}
