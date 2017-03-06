package ru.avicomp.ontapi.translators;

import java.util.Set;

import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.jena.model.OntNAP;
import ru.avicomp.ontapi.jena.model.OntStatement;
import uk.ac.manchester.cs.owl.owlapi.OWLSubAnnotationPropertyOfAxiomImpl;

/**
 * see {@link AbstractSubPropertyTranslator}
 *
 * Created by @szuev on 30.09.2016.
 */
class SubAnnotationPropertyOfTranslator extends AbstractSubPropertyTranslator<OWLSubAnnotationPropertyOfAxiom, OntNAP> {
    @Override
    OWLPropertyExpression getSubProperty(OWLSubAnnotationPropertyOfAxiom axiom) {
        return axiom.getSubProperty();
    }

    @Override
    OWLPropertyExpression getSuperProperty(OWLSubAnnotationPropertyOfAxiom axiom) {
        return axiom.getSuperProperty();
    }

    @Override
    Class<OntNAP> getView() {
        return OntNAP.class;
    }

    @Override
    OWLSubAnnotationPropertyOfAxiom create(OntStatement statement, Set<OWLAnnotation> annotations) {
        OWLAnnotationProperty sub = ReadHelper.getAnnotationProperty(statement.getSubject().as(OntNAP.class));
        OWLAnnotationProperty sup = ReadHelper.getAnnotationProperty(statement.getObject().as(OntNAP.class));
        return new OWLSubAnnotationPropertyOfAxiomImpl(sub, sup, annotations);
    }

    @Override
    Wrap<OWLSubAnnotationPropertyOfAxiom> asAxiom(OntStatement statement) {
        OWLDataFactory df = getDataFactory();
        Wrap<OWLAnnotationProperty> sub = ReadHelper._getAnnotationProperty(statement.getSubject().as(OntNAP.class), df);
        Wrap<OWLAnnotationProperty> sup = ReadHelper._getAnnotationProperty(statement.getObject().as(OntNAP.class), df);
        Wrap.Collection<OWLAnnotation> annotations = annotations(statement);
        OWLSubAnnotationPropertyOfAxiom res = df.getOWLSubAnnotationPropertyOfAxiom(sub.getObject(), sup.getObject(), annotations.getObjects());
        return Wrap.create(res, statement).add(annotations.getTriples()).append(sub).append(sup);
    }
}
