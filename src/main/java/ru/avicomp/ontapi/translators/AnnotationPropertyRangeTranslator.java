package ru.avicomp.ontapi.translators;

import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyRangeAxiom;

import ru.avicomp.ontapi.jena.model.OntNAP;
import ru.avicomp.ontapi.jena.model.OntStatement;
import uk.ac.manchester.cs.owl.owlapi.OWLAnnotationPropertyRangeAxiomImpl;

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

    @Override
    OWLAnnotationPropertyRangeAxiom create(OntStatement statement, Set<OWLAnnotation> annotations) {
        OWLAnnotationProperty p = RDF2OWLHelper.getAnnotationProperty(statement.getSubject().as(OntNAP.class));
        IRI r = IRI.create(statement.getObject().asResource().getURI());
        return new OWLAnnotationPropertyRangeAxiomImpl(p, r, annotations);
    }
}
