package ru.avicomp.ontapi.parsers;

import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLDataPropertyRangeAxiom;

import static ru.avicomp.ontapi.NodeIRIUtils.fromResource;

/**
 * Created by @szuev on 28.09.2016.
 */
class DataPropertyRangeParser extends SingleTripletParser<OWLDataPropertyRangeAxiom> {


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
        return getAxiom().getRange().getDataRangeType().getIRI();
    }

}
