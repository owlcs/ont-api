package ru.avicomp.ontapi.translators;

import java.util.Set;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Resource;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyDomainAxiom;

import ru.avicomp.ontapi.jena.model.*;
import uk.ac.manchester.cs.owl.owlapi.OWLDataPropertyDomainAxiomImpl;

/**
 * see {@link AbstractPropertyDomainTranslator}
 * rdfs:domain
 * <p>
 * Created by @szuev on 28.09.2016.
 */
class DataPropertyDomainTranslator extends AbstractPropertyDomainTranslator<OWLDataPropertyDomainAxiom> {
    @Override
    Stream<OntStatement> statements(OntGraphModel model) {
        return statements(model, OntNDP.class);
    }

    @Override
    OWLDataPropertyDomainAxiom create(OntPE property, Resource domain, Set<OWLAnnotation> annotations) {
        OWLDataProperty p = RDF2OWLHelper.getDataProperty(property.as(OntNDP.class));
        OWLClassExpression ce = RDF2OWLHelper.getClassExpression(domain.as(OntCE.class));
        return new OWLDataPropertyDomainAxiomImpl(p, ce, annotations);
    }
}
