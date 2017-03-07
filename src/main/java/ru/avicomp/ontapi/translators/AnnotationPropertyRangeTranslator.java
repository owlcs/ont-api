package ru.avicomp.ontapi.translators;

import java.util.stream.Stream;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyRangeAxiom;

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
        return super.statements(model);
    }

    @Override
    Wrap<OWLAnnotationPropertyRangeAxiom> asAxiom(OntStatement statement) {
        Wrap<OWLAnnotationProperty> p = ReadHelper.getAnnotationProperty(statement.getSubject().as(getView()), getDataFactory());
        Wrap<IRI> d = ReadHelper.wrapIRI(statement.getObject().as(OntObject.class));
        Wrap.Collection<OWLAnnotation> annotations = ReadHelper.getStatementAnnotations(statement, getDataFactory());
        OWLAnnotationPropertyRangeAxiom res = getDataFactory().getOWLAnnotationPropertyRangeAxiom(p.getObject(), d.getObject(), annotations.getObjects());
        return Wrap.create(res, statement).add(annotations.getTriples()).append(p).append(d);
    }
}
