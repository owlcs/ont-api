package ru.avicomp.ontapi.parsers;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLEntity;

import ru.avicomp.ontapi.OntException;

/**
 * simple triplet with rdf:type predicate.
 * <p>
 * Created by @szuev on 28.09.2016.
 */
class DeclarationParser extends SingleTripletParser<OWLDeclarationAxiom> {
    @Override
    public Resource getSubject() {
        return AxiomParseUtils.toResource(getAxiom().getEntity());
    }

    @Override
    public Property getPredicate() {
        return RDF.type;
    }

    @Override
    public RDFNode getObject() {
        OWLEntity entity = getAxiom().getEntity();
        if (entity.isOWLClass()) {
            return OWL.Class;
        } else if (entity.isOWLDataProperty()) {
            return OWL.DatatypeProperty;
        } else if (entity.isOWLObjectProperty()) {
            return OWL.ObjectProperty;
        } else if (entity.isOWLNamedIndividual()) {
            return OWL2.NamedIndividual;
        } else if (entity.isOWLAnnotationProperty()) {
            return OWL.AnnotationProperty;
        }
        throw new OntException("Unsupported " + getAxiom());
    }
}
