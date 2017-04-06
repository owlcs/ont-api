package ru.avicomp.ontapi.internal;

import java.util.stream.Stream;

import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;

import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntIndividual;
import ru.avicomp.ontapi.jena.model.OntOPE;
import ru.avicomp.ontapi.jena.model.OntStatement;

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

    /**
     * positive object property assertion: "a1 PN a2"
     * see <a href='https://www.w3.org/TR/owl2-quick-reference/'>Assertions</a>
     *
     * @param model {@link OntGraphModel} the model
     * @return Stream of {@link OntStatement}
     */
    @Override
    public Stream<OntStatement> statements(OntGraphModel model) {
        return model.statements()
                .filter(OntStatement::isLocal)
                .filter(OntStatement::isObject)
                .filter(s -> s.getSubject().canAs(OntIndividual.class))
                .filter(s -> s.getObject().canAs(OntIndividual.class));
    }

    @Override
    public boolean testStatement(OntStatement statement) {
        return statement.isObject()
                && statement.getSubject().canAs(OntIndividual.class)
                && statement.getObject().canAs(OntIndividual.class);
    }

    @Override
    public Wrap<OWLObjectPropertyAssertionAxiom> asAxiom(OntStatement statement) {
        ConfigProvider.Config conf = getConfig(statement);
        Wrap<? extends OWLIndividual> subject = ReadHelper.fetchIndividual(statement.getSubject().as(OntIndividual.class), conf.dataFactory());
        Wrap<? extends OWLObjectPropertyExpression> property = ReadHelper.fetchObjectPropertyExpression(statement.getPredicate().as(OntOPE.class), conf.dataFactory());
        Wrap<? extends OWLIndividual> object = ReadHelper.fetchIndividual(statement.getObject().as(OntIndividual.class), conf.dataFactory());
        Wrap.Collection<OWLAnnotation> annotations = ReadHelper.getStatementAnnotations(statement, conf.dataFactory(), conf.loaderConfig());
        OWLObjectPropertyAssertionAxiom res = conf.dataFactory().getOWLObjectPropertyAssertionAxiom(property.getObject(), subject.getObject(), object.getObject(),
                annotations.getObjects());
        return Wrap.create(res, statement).add(annotations.getTriples()).append(subject).append(property).append(object);
    }
}
