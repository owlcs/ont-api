package ru.avicomp.ontapi.translators;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.vocabulary.OWL2;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLSameIndividualAxiom;

import ru.avicomp.ontapi.jena.model.OntIndividual;
import ru.avicomp.ontapi.jena.model.OntStatement;
import uk.ac.manchester.cs.owl.owlapi.OWLSameIndividualAxiomImpl;

/**
 * base class {@link AbstractNaryTranslator}
 * example:
 * :indi1 owl:sameAs :indi2, :indi3 .
 * <p>
 * Created by szuev on 13.10.2016.
 */
class SameIndividualTranslator extends AbstractNaryTranslator<OWLSameIndividualAxiom, OWLIndividual, OntIndividual> {
    @Override
    public Property getPredicate() {
        return OWL2.sameAs;
    }

    @Override
    Class<OntIndividual> getView() {
        return OntIndividual.class;
    }

    @Override
    OWLSameIndividualAxiom create(Stream<OWLIndividual> components, Set<OWLAnnotation> annotations) {
        return new OWLSameIndividualAxiomImpl(components.collect(Collectors.toSet()), annotations);
    }

    @Override
    OWLSameIndividualAxiom create(OntStatement statement, Set<OWLAnnotation> annotations) {
        return create(components(statement).map(RDF2OWLHelper::getIndividual), annotations);
    }
}
