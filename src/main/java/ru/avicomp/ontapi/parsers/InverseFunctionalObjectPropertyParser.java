package ru.avicomp.ontapi.parsers;

import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLInverseFunctionalObjectPropertyAxiom;

import ru.avicomp.ontapi.NodeIRIUtils;

/**
 * example:
 * pizza:hasBase rdf:type owl:FunctionalProperty
 * <p>
 * Created by @szuev on 28.09.2016.
 */
class InverseFunctionalObjectPropertyParser extends SingleTripletParser<OWLInverseFunctionalObjectPropertyAxiom> {


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
        return NodeIRIUtils.fromResource(OWL.InverseFunctionalProperty);
    }

}
