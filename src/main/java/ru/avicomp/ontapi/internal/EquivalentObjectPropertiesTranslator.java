package ru.avicomp.ontapi.internal;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Property;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLEquivalentObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;

import ru.avicomp.ontapi.jena.model.OntOPE;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import uk.ac.manchester.cs.owl.owlapi.OWLEquivalentObjectPropertiesAxiomImpl;

/**
 * example:
 * <http://schema.org/image> rdf:type owl:ObjectProperty ;  owl:equivalentProperty foaf:depiction .
 * Created by @szuev on 01.10.2016.
 */
public class EquivalentObjectPropertiesTranslator extends AbstractNaryTranslator<OWLEquivalentObjectPropertiesAxiom, OWLObjectPropertyExpression, OntOPE> {

    @Override
    Property getPredicate() {
        return OWL.equivalentProperty;
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
    public InternalObject<OWLEquivalentObjectPropertiesAxiom> asAxiom(OntStatement statement) {
        ConfigProvider.Config conf = getConfig(statement);
        InternalObject<? extends OWLObjectPropertyExpression> a = ReadHelper.fetchObjectPropertyExpression(statement.getSubject().as(getView()), conf.dataFactory());
        InternalObject<? extends OWLObjectPropertyExpression> b = ReadHelper.fetchObjectPropertyExpression(statement.getObject().as(getView()), conf.dataFactory());
        InternalObject.Collection<OWLAnnotation> annotations = ReadHelper.getStatementAnnotations(statement, conf.dataFactory(), conf.loaderConfig());
        OWLEquivalentObjectPropertiesAxiom res = conf.dataFactory().getOWLEquivalentObjectPropertiesAxiom(a.getObject(), b.getObject(), annotations.getObjects());
        return InternalObject.create(res, statement).add(annotations.getTriples()).append(a).append(b);
    }
}
