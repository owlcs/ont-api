package ru.avicomp.ontapi.translators;

import java.util.Set;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Resource;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLDataRange;

import ru.avicomp.ontapi.jena.model.*;
import uk.ac.manchester.cs.owl.owlapi.OWLDataPropertyRangeAxiomImpl;

/**
 * see {@link AbstractPropertyRangeTranslator}
 * <p>
 * Created by @szuev on 28.09.2016.
 */
class DataPropertyRangeTranslator extends AbstractPropertyRangeTranslator<OWLDataPropertyRangeAxiom> {

    @Override
    Stream<OntStatement> statements(OntGraphModel model) {
        return statements(model, OntNDP.class);
    }

    @Override
    OWLDataPropertyRangeAxiom create(OntPE property, Resource range, Set<OWLAnnotation> annotations) {
        OWLDataProperty p = RDF2OWLHelper.getDataProperty(property.as(OntNDP.class));
        OWLDataRange ce = RDF2OWLHelper.getDataRange(range.as(OntDR.class));
        return new OWLDataPropertyRangeAxiomImpl(p, ce, annotations);
    }
}
