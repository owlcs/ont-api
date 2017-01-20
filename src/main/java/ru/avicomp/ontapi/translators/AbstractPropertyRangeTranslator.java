package ru.avicomp.ontapi.translators;

import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.model.HasProperty;
import org.semanticweb.owlapi.model.HasRange;
import org.semanticweb.owlapi.model.OWLAxiom;

import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntPE;
import ru.avicomp.ontapi.jena.model.OntStatement;

/**
 * base class for {@link DataPropertyRangeTranslator} and {@link ObjectPropertyRangeTranslator} and {@link AnnotationPropertyRangeTranslator}
 * example: foaf:name rdfs:range rdfs:Literal
 * <p>
 * Created by @szuev on 30.09.2016.
 */
abstract class AbstractPropertyRangeTranslator<Axiom extends OWLAxiom & HasProperty & HasRange, P extends OntPE> extends AxiomTranslator<Axiom> {
    @Override
    public void write(Axiom axiom, OntGraphModel graph) {
        OWL2RDFHelper.writeTriple(graph, axiom.getProperty(), RDFS.range, axiom.getRange(), axiom.annotations());
    }

    abstract Class<P> getView();

    Stream<OntStatement> statements(OntGraphModel model) {
        return model.ontObjects(getView())
                .map(p -> p.range().map(r -> p.getStatement(RDFS.range, r)))
                .flatMap(Function.identity())
                .filter(OntStatement::isLocal);
    }

}
