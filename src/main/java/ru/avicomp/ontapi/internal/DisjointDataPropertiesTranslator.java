package ru.avicomp.ontapi.internal;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.jena.model.OntDisjoint;
import ru.avicomp.ontapi.jena.model.OntNDP;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
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
        return OWL.propertyDisjointWith;
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
        return OWL.AllDisjointProperties;
    }

    @Override
    Property getMembersPredicate() {
        return OWL.members;
    }

    @Override
    Class<OntDisjoint.DataProperties> getDisjointView() {
        return OntDisjoint.DataProperties.class;
    }

    @Override
    Wrap<OWLDisjointDataPropertiesAxiom> asAxiom(OntStatement statement) {
        OWLDataFactory df = getDataFactory(statement.getModel());
        Wrap.Collection<OWLDataProperty> members;
        Stream<OntStatement> content;
        if (statement.getSubject().canAs(getDisjointView())) {
            OntDisjoint.DataProperties disjoint = statement.getSubject().as(getDisjointView());
            content = disjoint.content();
            members = Wrap.Collection.create(disjoint.members().map(m -> ReadHelper.fetchDataProperty(m, df)));
        } else {
            content = Stream.of(statement);
            members = Wrap.Collection.create(Stream.of(statement.getSubject(), statement.getObject()).map(r -> r.as(getView())).map(m -> ReadHelper.fetchDataProperty(m, df)));
        }
        Wrap.Collection<OWLAnnotation> annotations = ReadHelper.getStatementAnnotations(statement, df);
        OWLDisjointDataPropertiesAxiom res = df.getOWLDisjointDataPropertiesAxiom(members.getObjects(), annotations.getObjects());
        return Wrap.create(res, content).add(annotations.getTriples()).add(members.getTriples());
    }
}
