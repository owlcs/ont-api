package ru.avicomp.ontapi.parsers;

import org.apache.jena.vocabulary.OWL;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLInverseObjectPropertiesAxiom;

import ru.avicomp.ontapi.NodeIRIUtils;

/**
 * example:
 * pizza:hasBase owl:inverseOf pizza:isBaseOf ;
 * <p>
 * Created by @szuev on 30.09.2016.
 */
class InverseObjectPropertiesParser extends SingleTripletParser<OWLInverseObjectPropertiesAxiom> {
    @Override
    public OWLAnnotationValue getSubject() {
        return ParseUtils.toIRI(getAxiom().getFirstProperty());
    }

    @Override
    public IRI getPredicate() {
        return NodeIRIUtils.fromResource(OWL.inverseOf);
    }

    @Override
    public OWLAnnotationValue getObject() {
        return ParseUtils.toIRI(getAxiom().getSecondProperty());
    }
}
