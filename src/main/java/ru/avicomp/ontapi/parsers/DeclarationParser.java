package ru.avicomp.ontapi.parsers;

import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLEntity;

import ru.avicomp.ontapi.OntException;

import static ru.avicomp.ontapi.NodeIRIUtils.fromResource;

/**
 * simple triplet with rdf:type predicate.
 *
 * Created by @szuev on 28.09.2016.
 */
class DeclarationParser extends SingleTripletParser<OWLDeclarationAxiom> {

    @Override
    public OWLAnnotationValue getSubject() {
        return getAxiom().getEntity().getIRI();
    }

    @Override
    public IRI getPredicate() {
        return fromResource(RDF.type);
    }

    @Override
    public OWLAnnotationValue getObject() {
        OWLEntity entity = getAxiom().getEntity();
        if (entity.isOWLClass()) {
            return fromResource(OWL.Class);
        } else if (entity.isOWLDataProperty()) {
            return fromResource(OWL.DatatypeProperty);
        } else if (entity.isOWLObjectProperty()) {
            return fromResource(OWL.ObjectProperty);
        } else if (entity.isOWLNamedIndividual()) {
            return fromResource(OWL2.NamedIndividual);
        }
        throw new OntException("Unsupported " + getAxiom());
    }
}
