package ru.avicomp.ontapi.translators;

import java.util.Set;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Resource;
import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.jena.model.*;
import uk.ac.manchester.cs.owl.owlapi.OWLAnnotationAssertionAxiomImpl;

/**
 * Examples:
 * foaf:LabelProperty vs:term_status "unstable" .
 * foaf:LabelProperty rdfs:isDefinedBy <http://xmlns.com/foaf/0.1/> .
 * pizza:UnclosedPizza rdfs:label "PizzaAberta"@pt .
 * <p>
 * Created by @szuev on 28.09.2016.
 */
class AnnotationAssertionTranslator extends AxiomTranslator<OWLAnnotationAssertionAxiom> {
    @Override
    public void write(OWLAnnotationAssertionAxiom axiom, OntGraphModel model) {
        WriteHelper.writeAssertionTriple(model, axiom.getSubject(), axiom.getProperty(), axiom.getValue(), axiom.annotations());
    }

    @Override
    Stream<OntStatement> statements(OntGraphModel model) {
        OntID id = model.getID();
        return model.statements()
                .filter(OntStatement::isLocal)
                .filter(OntStatement::isAnnotation)
                .filter(s -> testAnnotationSubject(s.getSubject(), id));
    }

    private static boolean testAnnotationSubject(Resource candidate, OntID id) {
        return !candidate.equals(id) && (candidate.isURIResource() || candidate.canAs(OntIndividual.Anonymous.class));
    }

    @Override
    OWLAnnotationAssertionAxiom create(OntStatement statement, Set<OWLAnnotation> annotations) {
        OWLAnnotationSubject subject = ReadHelper.getAnnotationSubject(statement.getSubject());
        OWLAnnotationProperty property = ReadHelper.getAnnotationProperty(statement.getPredicate().as(OntNAP.class));
        OWLAnnotationValue value = ReadHelper.getAnnotationValue(statement.getObject());
        return new OWLAnnotationAssertionAxiomImpl(subject, property, value, annotations);
    }

    @Override
    Wrap<OWLAnnotationAssertionAxiom> asAxiom(OntStatement statement) {
        Wrap<? extends OWLAnnotationSubject> s = ReadHelper._getAnnotationSubject(statement.getSubject(), getDataFactory());
        Wrap<OWLAnnotationProperty> p = ReadHelper._getAnnotationProperty(statement.getPredicate().as(OntNAP.class), getDataFactory());
        Wrap<? extends OWLAnnotationValue> v = ReadHelper._getAnnotationValue(statement.getObject(), getDataFactory());
        Wrap.Collection<OWLAnnotation> annotations = annotations(statement);
        OWLAnnotationAssertionAxiom res = getDataFactory().getOWLAnnotationAssertionAxiom(p.getObject(), s.getObject(), v.getObject(),
                annotations.getObjects());
        return Wrap.create(res, statement).add(annotations.getTriples()).append(s).append(p).append(v);
    }

}
