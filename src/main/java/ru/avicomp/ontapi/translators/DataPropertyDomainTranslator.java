package ru.avicomp.ontapi.translators;

import java.util.Set;

import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyDomainAxiom;

import ru.avicomp.ontapi.jena.model.OntCE;
import ru.avicomp.ontapi.jena.model.OntNDP;
import ru.avicomp.ontapi.jena.model.OntStatement;
import uk.ac.manchester.cs.owl.owlapi.OWLDataPropertyDomainAxiomImpl;

/**
 * see {@link AbstractPropertyDomainTranslator}
 * rdfs:domain
 * <p>
 * Created by @szuev on 28.09.2016.
 */
class DataPropertyDomainTranslator extends AbstractPropertyDomainTranslator<OWLDataPropertyDomainAxiom, OntNDP> {
    @Override
    Class<OntNDP> getView() {
        return OntNDP.class;
    }

    @Override
    OWLDataPropertyDomainAxiom create(OntStatement statement, Set<OWLAnnotation> annotations) {
        OWLDataProperty p = RDF2OWLHelper.getDataProperty(statement.getSubject().as(OntNDP.class));
        OWLClassExpression ce = RDF2OWLHelper.getClassExpression(statement.getObject().as(OntCE.class));
        return new OWLDataPropertyDomainAxiomImpl(p, ce, annotations);
    }
}
