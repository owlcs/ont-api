package ru.avicomp.ontapi.translators;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLIndividual;

import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

/**
 * Creating individual (both named and anonymous):
 * pizza:France rdf:type owl:NamedIndividual, pizza:Country, owl:Thing.
 * <p>
 * Created by @szuev on 28.09.2016.
 */
class ClassAssertionTranslator extends AxiomTranslator<OWLClassAssertionAxiom> {
    @Override
    public void write(OWLClassAssertionAxiom axiom, OntGraphModel model) {
        OntCE ce = WriteHelper.addClassExpression(model, axiom.getClassExpression());
        OWLIndividual individual = axiom.getIndividual();
        OntObject subject = individual.isAnonymous() ?
                WriteHelper.toResource(individual).inModel(model).as(OntObject.class) :
                WriteHelper.addIndividual(model, individual);
        OntStatement statement = subject.addStatement(RDF.type, ce);
        WriteHelper.addAnnotations(statement, axiom.annotations());
    }

    @Override
    Stream<OntStatement> statements(OntGraphModel model) {
        return model.ontObjects(OntIndividual.class)
                .map(i -> i.classes().map(ce -> i.statement(RDF.type, ce)))
                .flatMap(Function.identity())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(OntStatement::isLocal);
    }

    @Override
    Wrap<OWLClassAssertionAxiom> asAxiom(OntStatement statement) {
        Wrap<? extends OWLIndividual> i = ReadHelper.getIndividual(statement.getSubject().as(OntIndividual.class), getDataFactory());
        Wrap<? extends OWLClassExpression> ce = ReadHelper.getClassExpression(statement.getObject().as(OntCE.class), getDataFactory());
        Wrap.Collection<OWLAnnotation> annotations = ReadHelper.getStatementAnnotations(statement, getDataFactory());
        OWLClassAssertionAxiom res = getDataFactory().getOWLClassAssertionAxiom(ce.getObject(), i.getObject(), annotations.getObjects());
        return Wrap.create(res, statement).add(annotations.getTriples()).append(i).append(ce);
    }
}
