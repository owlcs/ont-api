package ru.avicomp.ontapi.translators;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL2;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLDifferentIndividualsAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;

import ru.avicomp.ontapi.jena.model.OntDisjoint;
import ru.avicomp.ontapi.jena.model.OntIndividual;
import ru.avicomp.ontapi.jena.model.OntStatement;
import uk.ac.manchester.cs.owl.owlapi.OWLDifferentIndividualsAxiomImpl;

/**
 * Note! it is for an owl-expression without any root!
 * see {@link AbstractTwoWayNaryTranslator}
 * Example:
 * [ a owl:AllDifferent; owl:distinctMembers (pizza:America pizza:Italy pizza:Germany pizza:England pizza:France) ].
 * <p>
 * Created by @szuev on 29.09.2016.
 */
class DifferentIndividualsTranslator extends AbstractTwoWayNaryTranslator<OWLDifferentIndividualsAxiom, OWLIndividual, OntIndividual> {
    @Override
    Property getPredicate() {
        return OWL2.differentFrom;
    }

    @Override
    Class<OntIndividual> getView() {
        return OntIndividual.class;
    }

    @Override
    OWLDifferentIndividualsAxiom create(Stream<OWLIndividual> components, Set<OWLAnnotation> annotations) {
        return new OWLDifferentIndividualsAxiomImpl(components.collect(Collectors.toSet()), annotations);
    }

    @Override
    Resource getMembersType() {
        return OWL2.AllDifferent;
    }

    @Override
    Property getMembersPredicate() {
        return OWL2.distinctMembers;
    }

    @Override
    Class<OntDisjoint.Individuals> getDisjointView() {
        return OntDisjoint.Individuals.class;
    }

    @Override
    OWLDifferentIndividualsAxiom create(OntStatement statement, Set<OWLAnnotation> annotations) {
        return create(components(statement).map(RDF2OWLHelper::getIndividual), annotations);
    }
}
