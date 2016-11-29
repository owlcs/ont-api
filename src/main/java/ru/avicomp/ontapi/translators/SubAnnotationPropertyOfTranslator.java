package ru.avicomp.ontapi.translators;

import java.util.Set;

import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLPropertyExpression;
import org.semanticweb.owlapi.model.OWLSubAnnotationPropertyOfAxiom;

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
        OWLAnnotationProperty sub = RDF2OWLHelper.getAnnotationProperty(statement.getSubject().as(OntNAP.class));
        OWLAnnotationProperty sup = RDF2OWLHelper.getAnnotationProperty(statement.getObject().as(OntNAP.class));
        return new OWLSubAnnotationPropertyOfAxiomImpl(sub, sup, annotations);
    }
}
