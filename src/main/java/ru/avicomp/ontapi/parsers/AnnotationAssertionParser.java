package ru.avicomp.ontapi.parsers;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationValue;

import ru.avicomp.ontapi.OntException;

/**
 * Examples:
 * foaf:LabelProperty vs:term_status "unstable" .
 * foaf:LabelProperty rdfs:isDefinedBy <http://xmlns.com/foaf/0.1/> .
 * pizza:UnclosedPizza rdfs:label "PizzaAberta"@pt .
 * <p>
 * Created by @szuev on 28.09.2016.
 */
class AnnotationAssertionParser extends SingleTripletParser<OWLAnnotationAssertionAxiom> {
    @Override
    public Resource getSubject() {
        return AxiomParseUtils.toResource(getAxiom().getSubject());
    }

    @Override
    public Property getPredicate() {
        return AxiomParseUtils.toProperty(getAxiom().getProperty().getIRI());
    }

    @Override
    public RDFNode getObject() {
        OWLAnnotationValue value = getAxiom().getValue();
        return value.isIRI() ? AxiomParseUtils.toResource(value) :
                AxiomParseUtils.toLiteral(value.asLiteral().
                        orElseThrow(() -> new OntException("Can't determine object " + getAxiom())));
    }
}
