package ru.avicomp.ontapi.translators;

import java.util.Set;

import org.apache.jena.rdf.model.Resource;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLInverseFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;

import ru.avicomp.ontapi.jena.model.OntOPE;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import uk.ac.manchester.cs.owl.owlapi.OWLInverseFunctionalObjectPropertyAxiomImpl;

/**
 * example:
 * pizza:hasBase rdf:type owl:FunctionalProperty
 * <p>
 * Created by @szuev on 28.09.2016.
 */
class InverseFunctionalObjectPropertyTranslator extends AbstractPropertyTypeTranslator<OWLInverseFunctionalObjectPropertyAxiom, OntOPE> {
    @Override
    OWLInverseFunctionalObjectPropertyAxiom create(OntStatement statement, Set<OWLAnnotation> annotations) {
        return new OWLInverseFunctionalObjectPropertyAxiomImpl(ReadHelper.getObjectProperty(getSubject(statement)), annotations);
    }

    @Override
    Resource getType() {
        return OWL.InverseFunctionalProperty;
    }

    @Override
    Class<OntOPE> getView() {
        return OntOPE.class;
    }

    @Override
    Wrap<OWLInverseFunctionalObjectPropertyAxiom> asAxiom(OntStatement statement) {
        Wrap<OWLObjectPropertyExpression> p = ReadHelper._getObjectProperty(getSubject(statement), getDataFactory());
        Wrap.Collection<OWLAnnotation> annotations = annotations(statement);
        OWLInverseFunctionalObjectPropertyAxiom res = getDataFactory().getOWLInverseFunctionalObjectPropertyAxiom(p.getObject(), annotations.getObjects());
        return Wrap.create(res, statement).add(annotations.getTriples()).append(p);
    }
}
