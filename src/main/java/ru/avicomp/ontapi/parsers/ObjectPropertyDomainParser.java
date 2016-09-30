package ru.avicomp.ontapi.parsers;

import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;

import static ru.avicomp.ontapi.NodeIRIUtils.fromResource;

/**
 * todo: what is difference from {@link DataPropertyDomainParser} ?
 * <p>
 * Created by @szuev on 29.09.2016.
 */
class ObjectPropertyDomainParser extends SingleTripletParser<OWLObjectPropertyDomainAxiom> {
    @Override
    public OWLAnnotationValue getSubject() {
        return ParseUtils.toIRI(getAxiom().getProperty());
    }

    @Override
    public IRI getPredicate() {
        return fromResource(RDFS.domain);
    }

    @Override
    public OWLAnnotationValue getObject() {
        return ParseUtils.toIRI(getAxiom().getDomain());
    }
}
