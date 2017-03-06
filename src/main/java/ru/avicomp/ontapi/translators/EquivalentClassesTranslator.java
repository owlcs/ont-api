package ru.avicomp.ontapi.translators;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Property;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;

import ru.avicomp.ontapi.jena.model.OntCE;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import uk.ac.manchester.cs.owl.owlapi.OWLEquivalentClassesAxiomImpl;

/**
 * Base class {@link AbstractNaryTranslator}
 * Example of ttl:
 * pizza:SpicyTopping owl:equivalentClass [ a owl:Class; owl:intersectionOf ( pizza:PizzaTopping [a owl:Restriction; owl:onProperty pizza:hasSpiciness; owl:someValuesFrom pizza:Hot] )] ;
 * <p>
 * Created by @szuev on 29.09.2016.
 */
class EquivalentClassesTranslator extends AbstractNaryTranslator<OWLEquivalentClassesAxiom, OWLClassExpression, OntCE> {
    @Override
    public Property getPredicate() {
        return OWL.equivalentClass;
    }

    @Override
    Class<OntCE> getView() {
        return OntCE.class;
    }

    @Override
    OWLEquivalentClassesAxiom create(Stream<OWLClassExpression> components, Set<OWLAnnotation> annotations) {
        return new OWLEquivalentClassesAxiomImpl(components.collect(Collectors.toSet()), annotations);
    }

    @Override
    OWLEquivalentClassesAxiom create(OntStatement statement, Set<OWLAnnotation> annotations) {
        return create(components(statement).map(ReadHelper::getClassExpression), annotations);
    }
}
