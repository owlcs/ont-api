package ru.avicomp.ontapi.parsers;

import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLTransitiveObjectPropertyAxiom;

import ru.avicomp.ontapi.NodeIRIUtils;

/**
 * Created by @szuev on 28.09.2016.
 */
class TransitiveObjectPropertyParser extends SingleTripletParser<OWLTransitiveObjectPropertyAxiom> {

    @Override
    public OWLAnnotationValue getSubject() {
        return ParseUtils.toIRI(getAxiom().getProperty());
    }

    @Override
    public IRI getPredicate() {
        return NodeIRIUtils.fromResource(RDF.type);
    }

    @Override
    public OWLAnnotationValue getObject() {
        return NodeIRIUtils.fromResource(OWL.TransitiveProperty);
    }

}
