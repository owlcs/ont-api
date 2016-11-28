package ru.avicomp.ontapi.translators;

import java.util.Set;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Resource;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;

import ru.avicomp.ontapi.jena.model.*;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectPropertyRangeAxiomImpl;

/**
 * see {@link AbstractPropertyRangeTranslator}
 * <p>
 * Created by @szuev on 28.09.2016.
 */
class ObjectPropertyRangeTranslator extends AbstractPropertyRangeTranslator<OWLObjectPropertyRangeAxiom> {
    @Override
    Stream<OntStatement> statements(OntGraphModel model) {
        return statements(model, OntOPE.class);
    }

    @Override
    OWLObjectPropertyRangeAxiom create(OntPE property, Resource range, Set<OWLAnnotation> annotations) {
        OWLObjectPropertyExpression p = RDF2OWLHelper.getObjectProperty(property.as(OntOPE.class));
        OWLClassExpression ce = RDF2OWLHelper.getClassExpression(range.as(OntCE.class));
        return new OWLObjectPropertyRangeAxiomImpl(p, ce, annotations);
    }
}
