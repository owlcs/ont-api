package ru.avicomp.ontapi.translators;

import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.model.HasDomain;
import org.semanticweb.owlapi.model.HasProperty;
import org.semanticweb.owlapi.model.OWLAxiom;

import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntPE;
import ru.avicomp.ontapi.jena.model.OntStatement;

/**
 * base class for {@link ObjectPropertyDomainTranslator} and {@link DataPropertyDomainTranslator} and {@link AnnotationPropertyDomainTranslator}
 * for rdfs:domain tripler.
 * <p>
 * Created by @szuev on 30.09.2016.
 */
abstract class AbstractPropertyDomainTranslator<Axiom extends OWLAxiom & HasDomain & HasProperty, View extends OntPE> extends AxiomTranslator<Axiom> {
    @Override
    public void write(Axiom axiom, OntGraphModel model) {
        OWL2RDFHelper.writeTriple(model, axiom.getProperty(), RDFS.domain, axiom.getDomain(), axiom.annotations());
    }

    abstract Class<View> getView();

    Stream<OntStatement> statements(OntGraphModel model) {
        return model.ontObjects(getView())
                .map(p -> p.domain().map(d -> p.getStatement(RDFS.domain, d)))
                .flatMap(Function.identity())
                .filter(OntStatement::isLocal);
    }
}
