package ru.avicomp.ontapi.parsers;

import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLFunctionalObjectPropertyAxiom;

import static ru.avicomp.ontapi.NodeIRIUtils.fromResource;

/**
 * example:
 * pizza:hasBase rdf:type owl:FunctionalProperty.
 * <p>
 * Created by @szuev on 29.09.2016.
 */
class FunctionalObjectPropertyParser extends SingleTripletParser<OWLFunctionalObjectPropertyAxiom> {
    @Override
    public OWLAnnotationValue getSubject() {
        return ParseUtils.toIRI(getAxiom().getProperty());
    }

    @Override
    public IRI getPredicate() {
        return fromResource(RDF.type);
    }

    @Override
    public OWLAnnotationValue getObject() {
        return fromResource(OWL.FunctionalProperty);
    }
}
