package ru.avicomp.ontapi.internal;

import java.util.stream.Stream;

import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntNAP;
import ru.avicomp.ontapi.jena.model.OntObject;
import ru.avicomp.ontapi.jena.model.OntStatement;

/**
 * base class {@link AbstractPropertyRangeTranslator}
 * Note: OWL Axiom Type is "AnnotationPropertyRangeOf", not "AnnotationPropertyRange"
 * <p>
 * Created by @szuev on 30.09.2016.
 */
class AnnotationPropertyRangeTranslator extends AbstractPropertyRangeTranslator<OWLAnnotationPropertyRangeAxiom, OntNAP> {
    @Override
    Class<OntNAP> getView() {
        return OntNAP.class;
    }

    /**
     * todo: invite config option to skip annotation range in favor of another range if there is a punning
     *
     * @param model {@link OntGraphModel}
     * @return {@link OntStatement}
     */
    @Override
    Stream<OntStatement> statements(OntGraphModel model) {
        return super.statements(model).filter(s -> s.getObject().isURIResource());
    }

    @Override
    Wrap<OWLAnnotationPropertyRangeAxiom> asAxiom(OntStatement statement) {
        OWLDataFactory df = getDataFactory(statement.getModel());
        Wrap<OWLAnnotationProperty> p = ReadHelper.getAnnotationProperty(statement.getSubject().as(getView()), df);
        Wrap<IRI> d = ReadHelper.wrapIRI(statement.getObject().as(OntObject.class));
        Wrap.Collection<OWLAnnotation> annotations = ReadHelper.getStatementAnnotations(statement, df);
        OWLAnnotationPropertyRangeAxiom res = df.getOWLAnnotationPropertyRangeAxiom(p.getObject(), d.getObject(), annotations.getObjects());
        return Wrap.create(res, statement).add(annotations.getTriples()).append(p).append(d);
    }
}
