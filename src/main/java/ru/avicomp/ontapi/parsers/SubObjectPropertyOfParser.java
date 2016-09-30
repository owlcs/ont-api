package ru.avicomp.ontapi.parsers;

import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;

import ru.avicomp.ontapi.NodeIRIUtils;

/**
 * for rdfs:subPropertyOf
 * <p>
 * Created by @szuev on 29.09.2016.
 */
class SubObjectPropertyOfParser extends SingleTripletParser<OWLSubObjectPropertyOfAxiom> {
    @Override
    public OWLAnnotationValue getSubject() {
        return ParseUtils.toIRI(getAxiom().getSubProperty());
    }

    @Override
    public IRI getPredicate() {
        return NodeIRIUtils.fromResource(RDFS.subPropertyOf);
    }

    @Override
    public OWLAnnotationValue getObject() {
        return ParseUtils.toIRI(getAxiom().getSuperProperty());
    }
}
