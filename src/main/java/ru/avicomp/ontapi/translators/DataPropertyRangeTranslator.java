package ru.avicomp.ontapi.translators;

import java.util.Set;

import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLDataRange;

import ru.avicomp.ontapi.jena.model.OntDR;
import ru.avicomp.ontapi.jena.model.OntNDP;
import ru.avicomp.ontapi.jena.model.OntStatement;
import uk.ac.manchester.cs.owl.owlapi.OWLDataPropertyRangeAxiomImpl;

/**
 * see {@link AbstractPropertyRangeTranslator}
 * <p>
 * Created by @szuev on 28.09.2016.
 */
class DataPropertyRangeTranslator extends AbstractPropertyRangeTranslator<OWLDataPropertyRangeAxiom, OntNDP> {
    @Override
    Class<OntNDP> getView() {
        return OntNDP.class;
    }

    @Override
    OWLDataPropertyRangeAxiom create(OntStatement statement, Set<OWLAnnotation> annotations) {
        OWLDataProperty p = RDF2OWLHelper.getDataProperty(statement.getSubject().as(OntNDP.class));
        OWLDataRange ce = RDF2OWLHelper.getDataRange(statement.getObject().as(OntDR.class));
        return new OWLDataPropertyRangeAxiomImpl(p, ce, annotations);
    }
}
