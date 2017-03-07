package ru.avicomp.ontapi.translators;

import org.apache.jena.rdf.model.Resource;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLIrreflexiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;

import ru.avicomp.ontapi.jena.model.OntOPE;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.vocabulary.OWL;

/**
 * Object property with owl:IrreflexiveProperty type
 *
 * NOTE: owl AxiomType is "IrrefexiveObjectProperty" ({@link org.semanticweb.owlapi.model.AxiomType#IRREFLEXIVE_OBJECT_PROPERTY}), not "IrreflexiveObjectProperty"
 * example: :ob-prop-2 rdf:type owl:ObjectProperty , owl:IrreflexiveProperty .
 * Created by @szuev on 18.10.2016.
 */
class IrreflexiveObjectPropertyTranslator extends AbstractPropertyTypeTranslator<OWLIrreflexiveObjectPropertyAxiom, OntOPE> {
    @Override
    Resource getType() {
        return OWL.IrreflexiveProperty;
    }

    @Override
    Class<OntOPE> getView() {
        return OntOPE.class;
    }

    @Override
    Wrap<OWLIrreflexiveObjectPropertyAxiom> asAxiom(OntStatement statement) {
        Wrap<OWLObjectPropertyExpression> p = ReadHelper._getObjectProperty(getSubject(statement), getDataFactory());
        Wrap.Collection<OWLAnnotation> annotations = annotations(statement);
        OWLIrreflexiveObjectPropertyAxiom res = getDataFactory().getOWLIrreflexiveObjectPropertyAxiom(p.getObject(), annotations.getObjects());
        return Wrap.create(res, statement).add(annotations.getTriples()).append(p);
    }
}
