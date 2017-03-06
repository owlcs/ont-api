package ru.avicomp.ontapi.translators;

import java.util.Set;

import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLPropertyExpression;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;

import ru.avicomp.ontapi.jena.model.OntOPE;
import ru.avicomp.ontapi.jena.model.OntStatement;
import uk.ac.manchester.cs.owl.owlapi.OWLSubObjectPropertyOfAxiomImpl;

/**
 * see {@link AbstractSubPropertyTranslator}
 * <p>
 * Created by @szuev on 29.09.2016.
 */
class SubObjectPropertyOfTranslator extends AbstractSubPropertyTranslator<OWLSubObjectPropertyOfAxiom, OntOPE> {
    @Override
    OWLPropertyExpression getSubProperty(OWLSubObjectPropertyOfAxiom axiom) {
        return axiom.getSubProperty();
    }

    @Override
    OWLPropertyExpression getSuperProperty(OWLSubObjectPropertyOfAxiom axiom) {
        return axiom.getSuperProperty();
    }

    @Override
    Class<OntOPE> getView() {
        return OntOPE.class;
    }

    @Override
    OWLSubObjectPropertyOfAxiom create(OntStatement statement, Set<OWLAnnotation> annotations) {
        OWLObjectPropertyExpression sub = ReadHelper.getObjectProperty(statement.getSubject().as(OntOPE.class));
        OWLObjectPropertyExpression sup = ReadHelper.getObjectProperty(statement.getObject().as(OntOPE.class));
        return new OWLSubObjectPropertyOfAxiomImpl(sub, sup, annotations);
    }
}
