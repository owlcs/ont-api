package ru.avicomp.ontapi.internal;

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
abstract class AbstractPropertyDomainTranslator<Axiom extends OWLAxiom & HasDomain & HasProperty, P extends OntPE> extends AxiomTranslator<Axiom> {
    @Override
    public void write(Axiom axiom, OntGraphModel model) {
        WriteHelper.writeTriple(model, axiom.getProperty(), RDFS.domain, axiom.getDomain(), axiom.annotations());
    }

    abstract Class<P> getView();

    @Override
    Stream<OntStatement> statements(OntGraphModel model) {
        return model.statements(null, RDFS.domain, null)
                .filter(OntStatement::isLocal)
                .filter(s -> s.getSubject().canAs(getView()));
    }
}
