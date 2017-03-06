package ru.avicomp.ontapi.translators;

import java.util.Set;

import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.jena.model.OntNDP;
import ru.avicomp.ontapi.jena.model.OntStatement;
import uk.ac.manchester.cs.owl.owlapi.OWLSubDataPropertyOfAxiomImpl;

/**
 * see {@link AbstractSubPropertyTranslator}
 * <p>
 * Created by @szuev on 30.09.2016.
 */
class SubDataPropertyOfTranslator extends AbstractSubPropertyTranslator<OWLSubDataPropertyOfAxiom, OntNDP> {
    @Override
    OWLPropertyExpression getSubProperty(OWLSubDataPropertyOfAxiom axiom) {
        return axiom.getSubProperty();
    }

    @Override
    OWLPropertyExpression getSuperProperty(OWLSubDataPropertyOfAxiom axiom) {
        return axiom.getSuperProperty();
    }

    @Override
    Class<OntNDP> getView() {
        return OntNDP.class;
    }

    @Override
    OWLSubDataPropertyOfAxiom create(OntStatement statement, Set<OWLAnnotation> annotations) {
        OWLDataProperty sub = ReadHelper.getDataProperty(statement.getSubject().as(OntNDP.class));
        OWLDataProperty sup = ReadHelper.getDataProperty(statement.getObject().as(OntNDP.class));
        return new OWLSubDataPropertyOfAxiomImpl(sub, sup, annotations);
    }

    @Override
    Wrap<OWLSubDataPropertyOfAxiom> asAxiom(OntStatement statement) {
        OWLDataFactory df = getDataFactory();
        Wrap<OWLDataProperty> sub = ReadHelper._getDataProperty(statement.getSubject().as(OntNDP.class), df);
        Wrap<OWLDataProperty> sup = ReadHelper._getDataProperty(statement.getObject().as(OntNDP.class), df);
        Wrap.Collection<OWLAnnotation> annotations = annotations(statement);
        OWLSubDataPropertyOfAxiom res = df.getOWLSubDataPropertyOfAxiom(sub.getObject(), sup.getObject(), annotations.getObjects());
        return Wrap.create(res, statement).add(annotations.getTriples()).append(sub).append(sup);
    }
}
