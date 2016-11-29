package ru.avicomp.ontapi.translators;

import java.util.Set;

import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;

import ru.avicomp.ontapi.jena.model.OntCE;
import ru.avicomp.ontapi.jena.model.OntOPE;
import ru.avicomp.ontapi.jena.model.OntStatement;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectPropertyRangeAxiomImpl;

/**
 * see {@link AbstractPropertyRangeTranslator}
 * <p>
 * Created by @szuev on 28.09.2016.
 */
class ObjectPropertyRangeTranslator extends AbstractPropertyRangeTranslator<OWLObjectPropertyRangeAxiom, OntOPE> {
    @Override
    Class<OntOPE> getView() {
        return OntOPE.class;
    }

    @Override
    OWLObjectPropertyRangeAxiom create(OntStatement statement, Set<OWLAnnotation> annotations) {
        OWLObjectPropertyExpression p = RDF2OWLHelper.getObjectProperty(statement.getSubject().as(OntOPE.class));
        OWLClassExpression ce = RDF2OWLHelper.getClassExpression(statement.getObject().as(OntCE.class));
        return new OWLObjectPropertyRangeAxiomImpl(p, ce, annotations);
    }
}
