package ru.avicomp.ontapi.internal;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Property;
import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.jena.model.OntNDP;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import uk.ac.manchester.cs.owl.owlapi.OWLEquivalentDataPropertiesAxiomImpl;

/**
 * example:
 * gr:description rdf:type owl:DatatypeProperty ;  owl:equivalentProperty <http://schema.org/description> ;
 * Created by @szuev on 01.10.2016.
 */
class EquivalentDataPropertiesTranslator extends AbstractNaryTranslator<OWLEquivalentDataPropertiesAxiom, OWLDataPropertyExpression, OntNDP> {

    @Override
    Property getPredicate() {
        return OWL.equivalentProperty;
    }

    @Override
    Class<OntNDP> getView() {
        return OntNDP.class;
    }

    @Override
    OWLEquivalentDataPropertiesAxiom create(Stream<OWLDataPropertyExpression> components, Set<OWLAnnotation> annotations) {
        return new OWLEquivalentDataPropertiesAxiomImpl(components.collect(Collectors.toSet()), annotations);
    }

    @Override
    Wrap<OWLEquivalentDataPropertiesAxiom> asAxiom(OntStatement statement) {
        OWLDataFactory df = getDataFactory(statement.getModel());
        Wrap<OWLDataProperty> a = ReadHelper.fetchDataProperty(statement.getSubject().as(getView()), df);
        Wrap<OWLDataProperty> b = ReadHelper.fetchDataProperty(statement.getObject().as(getView()), df);
        Wrap.Collection<OWLAnnotation> annotations = ReadHelper.getStatementAnnotations(statement, df);
        OWLEquivalentDataPropertiesAxiom res = df.getOWLEquivalentDataPropertiesAxiom(a.getObject(), b.getObject(), annotations.getObjects());
        return Wrap.create(res, statement).add(annotations.getTriples()).append(a).append(b);
    }
}
