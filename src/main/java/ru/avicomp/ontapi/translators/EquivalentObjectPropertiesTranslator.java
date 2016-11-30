package ru.avicomp.ontapi.translators;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.vocabulary.OWL2;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLEquivalentObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;

import ru.avicomp.ontapi.jena.model.OntOPE;
import ru.avicomp.ontapi.jena.model.OntStatement;
import uk.ac.manchester.cs.owl.owlapi.OWLEquivalentObjectPropertiesAxiomImpl;

/**
 * example:
 * <http://schema.org/image> rdf:type owl:ObjectProperty ;  owl:equivalentProperty foaf:depiction .
 * Created by @szuev on 01.10.2016.
 */
class EquivalentObjectPropertiesTranslator extends AbstractNaryTranslator<OWLEquivalentObjectPropertiesAxiom, OWLObjectPropertyExpression, OntOPE> {

    @Override
    Property getPredicate() {
        return OWL2.equivalentProperty;
    }

    @Override
    Class<OntOPE> getView() {
        return OntOPE.class;
    }

    @Override
    OWLEquivalentObjectPropertiesAxiom create(Stream<OWLObjectPropertyExpression> components, Set<OWLAnnotation> annotations) {
        return new OWLEquivalentObjectPropertiesAxiomImpl(components.collect(Collectors.toSet()), annotations);
    }

    @Override
    OWLEquivalentObjectPropertiesAxiom create(OntStatement statement, Set<OWLAnnotation> annotations) {
        return create(components(statement).map(RDF2OWLHelper::getObjectProperty), annotations);
    }
}
