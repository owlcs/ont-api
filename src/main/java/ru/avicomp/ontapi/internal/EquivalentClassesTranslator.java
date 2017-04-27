package ru.avicomp.ontapi.internal;

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
public class EquivalentClassesTranslator extends AbstractNaryTranslator<OWLEquivalentClassesAxiom, OWLClassExpression, OntCE> {
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
    public InternalObject<OWLEquivalentClassesAxiom> asAxiom(OntStatement statement) {
        ConfigProvider.Config conf = getConfig(statement);
        InternalObject<? extends OWLClassExpression> a = ReadHelper.fetchClassExpression(statement.getSubject().as(getView()), conf.dataFactory());
        InternalObject<? extends OWLClassExpression> b = ReadHelper.fetchClassExpression(statement.getObject().as(getView()), conf.dataFactory());
        InternalObject.Collection<OWLAnnotation> annotations = ReadHelper.getStatementAnnotations(statement, conf.dataFactory(), conf.loaderConfig());
        OWLEquivalentClassesAxiom res = conf.dataFactory().getOWLEquivalentClassesAxiom(a.getObject(), b.getObject(), annotations.getObjects());
        return InternalObject.create(res, statement).add(annotations.getTriples()).append(a).append(b);
    }
}
