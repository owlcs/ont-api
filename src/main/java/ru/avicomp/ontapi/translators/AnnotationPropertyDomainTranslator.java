package ru.avicomp.ontapi.translators;

import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyDomainAxiom;

import ru.avicomp.ontapi.jena.model.OntNAP;
import ru.avicomp.ontapi.jena.model.OntStatement;
import uk.ac.manchester.cs.owl.owlapi.OWLAnnotationPropertyDomainAxiomImpl;

/**
 * domain for annotation property.
 * see {@link AbstractPropertyDomainTranslator}
 * <p>
 * Created by @szuev on 30.09.2016.
 */
class AnnotationPropertyDomainTranslator extends AbstractPropertyDomainTranslator<OWLAnnotationPropertyDomainAxiom, OntNAP> {
    @Override
    Class<OntNAP> getView() {
        return OntNAP.class;
    }

    @Override
    OWLAnnotationPropertyDomainAxiom create(OntStatement statement, Set<OWLAnnotation> annotations) {
        OWLAnnotationProperty p = RDF2OWLHelper.getAnnotationProperty(statement.getSubject().as(getView()));
        IRI d = IRI.create(statement.getObject().asResource().getURI());
        return new OWLAnnotationPropertyDomainAxiomImpl(p, d, annotations);
    }
}
