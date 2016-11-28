package ru.avicomp.ontapi.translators;

import java.util.Set;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Resource;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;

import ru.avicomp.ontapi.jena.model.*;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectPropertyDomainAxiomImpl;

/**
 * see {@link AbstractPropertyDomainTranslator}
 * <p>
 * Created by @szuev on 29.09.2016.
 */
class ObjectPropertyDomainTranslator extends AbstractPropertyDomainTranslator<OWLObjectPropertyDomainAxiom> {

    @Override
    Stream<OntStatement> statements(OntGraphModel model) {
        return statements(model, OntOPE.class);
    }

    @Override
    OWLObjectPropertyDomainAxiom create(OntPE property, Resource domain, Set<OWLAnnotation> annotations) {
        OWLObjectPropertyExpression p = RDF2OWLHelper.getObjectProperty(property.as(OntOPE.class));
        OWLClassExpression ce = RDF2OWLHelper.getClassExpression(domain.as(OntCE.class));
        return new OWLObjectPropertyDomainAxiomImpl(p, ce, annotations);
    }
}
