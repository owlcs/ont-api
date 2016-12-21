package ru.avicomp.ontapi.translators;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLDataPropertyExpression;
import org.semanticweb.owlapi.model.OWLDisjointDataPropertiesAxiom;

import ru.avicomp.ontapi.jena.model.OntDisjoint;
import ru.avicomp.ontapi.jena.model.OntNDP;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.vocabulary.OWL2;
import uk.ac.manchester.cs.owl.owlapi.OWLDisjointDataPropertiesAxiomImpl;

/**
 * see {@link AbstractTwoWayNaryTranslator}
 * examples:
 * :objProperty1 owl:propertyDisjointWith :objProperty2
 * [ rdf:type owl:AllDisjointProperties; owl:members ( :objProperty1 :objProperty2 :objProperty3 ) ]
 * <p>
 * Created by szuev on 12.10.2016.
 */
class DisjointDataPropertiesTranslator extends AbstractTwoWayNaryTranslator<OWLDisjointDataPropertiesAxiom, OWLDataPropertyExpression, OntNDP> {
    @Override
    Property getPredicate() {
        return OWL2.propertyDisjointWith;
    }

    @Override
    Class<OntNDP> getView() {
        return OntNDP.class;
    }

    @Override
    OWLDisjointDataPropertiesAxiom create(Stream<OWLDataPropertyExpression> components, Set<OWLAnnotation> annotations) {
        return new OWLDisjointDataPropertiesAxiomImpl(components.collect(Collectors.toSet()), annotations);
    }

    @Override
    Resource getMembersType() {
        return OWL2.AllDisjointProperties;
    }

    @Override
    Property getMembersPredicate() {
        return OWL2.members;
    }

    @Override
    Class<OntDisjoint.DataProperties> getDisjointView() {
        return OntDisjoint.DataProperties.class;
    }

    @Override
    OWLDisjointDataPropertiesAxiom create(OntStatement statement, Set<OWLAnnotation> annotations) {
        return create(components(statement).map(RDF2OWLHelper::getDataProperty), annotations);
    }
}
