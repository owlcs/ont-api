package ru.avicomp.ontapi.translators;

import java.util.Set;
import java.util.stream.Stream;

import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;

import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntIndividual;
import ru.avicomp.ontapi.jena.model.OntOPE;
import ru.avicomp.ontapi.jena.model.OntStatement;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectPropertyAssertionAxiomImpl;

/**
 * example:
 * gr:Saturday rdf:type owl:NamedIndividual , gr:hasNext gr:Sunday ;
 * Created by @szuev on 01.10.2016.
 */
class ObjectPropertyAssertionTranslator extends AxiomTranslator<OWLObjectPropertyAssertionAxiom> {

    /**
     * Note: ObjectPropertyAssertion(ObjectInverseOf(P) S O) = ObjectPropertyAssertion(P O S)
     *
     * @param axiom {@link OWLObjectPropertyAssertionAxiom}
     * @param model {@link OntGraphModel}
     */
    @Override
    public void write(OWLObjectPropertyAssertionAxiom axiom, OntGraphModel model) {
        OWLObjectPropertyExpression property = axiom.getProperty().isAnonymous() ? axiom.getProperty().getInverseProperty() : axiom.getProperty();
        OWLIndividual subject = axiom.getProperty().isAnonymous() ? axiom.getObject() : axiom.getSubject();
        OWLIndividual object = axiom.getProperty().isAnonymous() ? axiom.getSubject() : axiom.getObject();
        WriteHelper.writeAssertionTriple(model, subject, property, object, axiom.annotations());
    }

    @Override
    Stream<OntStatement> statements(OntGraphModel model) {
        // skip everything that is not correspond axiom rule
        // (e.g. OWL-API allows only Individual as an assertion object. rdf:List is not supported by this level of our api also)
        return model.statements()
                .filter(OntStatement::isLocal)
                .filter(OntStatement::isObject)
                .filter(s -> s.getSubject().canAs(OntIndividual.class))
                .filter(s -> s.getObject().canAs(OntIndividual.class));
    }

    @Override
    OWLObjectPropertyAssertionAxiom create(OntStatement statement, Set<OWLAnnotation> annotations) {
        OWLIndividual subject = ReadHelper.getIndividual(statement.getSubject().as(OntIndividual.class));
        OWLObjectPropertyExpression property = ReadHelper.getObjectProperty(statement.getPredicate().as(OntOPE.class));
        OWLIndividual object = ReadHelper.getIndividual(statement.getObject().as(OntIndividual.class));
        return new OWLObjectPropertyAssertionAxiomImpl(subject, property, object, annotations);
    }

    @Override
    Wrap<OWLObjectPropertyAssertionAxiom> asAxiom(OntStatement statement) {
        Wrap<? extends OWLIndividual> subject = ReadHelper._getIndividual(statement.getSubject().as(OntIndividual.class), getDataFactory());
        Wrap<OWLObjectPropertyExpression> property = ReadHelper._getObjectProperty(statement.getPredicate().as(OntOPE.class), getDataFactory());
        Wrap<? extends OWLIndividual> object = ReadHelper._getIndividual(statement.getObject().as(OntIndividual.class), getDataFactory());
        Wrap.Collection<OWLAnnotation> annotations = annotations(statement);
        OWLObjectPropertyAssertionAxiom res = getDataFactory().getOWLObjectPropertyAssertionAxiom(property.getObject(), subject.getObject(), object.getObject(),
                annotations.getObjects());
        return Wrap.create(res, statement).add(annotations.getTriples()).append(subject).append(property).append(object);
    }
}
