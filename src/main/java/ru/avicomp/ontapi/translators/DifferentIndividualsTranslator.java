package ru.avicomp.ontapi.translators;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDifferentIndividualsAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;

import ru.avicomp.ontapi.jena.model.OntDisjoint;
import ru.avicomp.ontapi.jena.model.OntIndividual;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
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
        return OWL.differentFrom;
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
        return OWL.AllDifferent;
    }

    @Override
    Property getMembersPredicate() {
        return OWL.distinctMembers;
    }

    @Override
    Class<OntDisjoint.Individuals> getDisjointView() {
        return OntDisjoint.Individuals.class;
    }

    @Override
    Wrap<OWLDifferentIndividualsAxiom> asAxiom(OntStatement statement) {
        OWLDataFactory df = getDataFactory();
        Wrap.Collection<? extends OWLIndividual> members;
        Stream<OntStatement> content;
        if (statement.getSubject().canAs(getDisjointView())) {
            OntDisjoint.Individuals disjoint = statement.getSubject().as(getDisjointView());
            content = disjoint.content();
            members = Wrap.Collection.create(disjoint.members().map(m -> ReadHelper._getIndividual(m, df)));
        } else {
            content = Stream.of(statement);
            members = Wrap.Collection.create(Stream.of(statement.getSubject(), statement.getObject())
                    .map(r -> r.as(getView())).map(m -> ReadHelper._getIndividual(m, df)));
        }
        Wrap.Collection<OWLAnnotation> annotations = annotations(statement);
        OWLDifferentIndividualsAxiom res = df.getOWLDifferentIndividualsAxiom(members.getObjects(), annotations.getObjects());
        return Wrap.create(res, content).add(annotations.getTriples()).add(members.getTriples());
    }
}
