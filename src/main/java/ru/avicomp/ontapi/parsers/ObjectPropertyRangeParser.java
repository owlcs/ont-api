package ru.avicomp.ontapi.parsers;

import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;

import static ru.avicomp.ontapi.NodeIRIUtils.fromResource;

/**
 * for rdfs:range
 * Created by @szuev on 28.09.2016.
 */
class ObjectPropertyRangeParser extends SingleTripletParser<OWLObjectPropertyRangeAxiom> {
    @Override
    public OWLAnnotationValue getSubject() {
        return ParseUtils.toIRI(getAxiom().getProperty());
    }

    @Override
    public IRI getPredicate() {
        return fromResource(RDFS.range);
    }

    @Override
    public OWLAnnotationValue getObject() {
        return ParseUtils.toIRI(getAxiom().getRange());
    }

}
