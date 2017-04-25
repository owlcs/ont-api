package ru.avicomp.ontapi.internal;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Property;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyExpression;
import org.semanticweb.owlapi.model.OWLEquivalentDataPropertiesAxiom;

import ru.avicomp.ontapi.jena.model.OntNDP;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import uk.ac.manchester.cs.owl.owlapi.OWLEquivalentDataPropertiesAxiomImpl;

/**
 * example:
 * gr:description rdf:type owl:DatatypeProperty ;  owl:equivalentProperty <http://schema.org/description> ;
 * Created by @szuev on 01.10.2016.
 */
public class EquivalentDataPropertiesTranslator extends AbstractNaryTranslator<OWLEquivalentDataPropertiesAxiom, OWLDataPropertyExpression, OntNDP> {

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
    public Wrap<OWLEquivalentDataPropertiesAxiom> asAxiom(OntStatement statement) {
        ConfigProvider.Config conf = getConfig(statement);
        Wrap<OWLDataProperty> a = ReadHelper.fetchDataProperty(statement.getSubject().as(getView()), conf.dataFactory());
        Wrap<OWLDataProperty> b = ReadHelper.fetchDataProperty(statement.getObject().as(getView()), conf.dataFactory());
        Wrap.Collection<OWLAnnotation> annotations = ReadHelper.getStatementAnnotations(statement, conf.dataFactory(), conf.loaderConfig());
        OWLEquivalentDataPropertiesAxiom res = conf.dataFactory().getOWLEquivalentDataPropertiesAxiom(a.getObject(), b.getObject(), annotations.getObjects());
        return Wrap.create(res, statement).add(annotations.getTriples()).append(a).append(b);
    }
}
