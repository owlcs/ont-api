package ru.avicomp.ontapi.internal;

import java.util.stream.Stream;

import org.semanticweb.owlapi.model.*;

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
        return model.statements(null, RDF.type, null)
                .filter(OntStatement::isLocal)
                .filter(s -> s.getObject().canAs(OntCE.class))
                .filter(s -> s.getSubject().canAs(OntIndividual.class));
    }

    @Override
    Wrap<OWLClassAssertionAxiom> asAxiom(OntStatement statement) {
        OWLDataFactory df = getDataFactory(statement.getModel());
        Wrap<? extends OWLIndividual> i = ReadHelper.fetchIndividual(statement.getSubject().as(OntIndividual.class), df);
        Wrap<? extends OWLClassExpression> ce = ReadHelper.fetchClassExpression(statement.getObject().as(OntCE.class), df);
        Wrap.Collection<OWLAnnotation> annotations = ReadHelper.getStatementAnnotations(statement, df);
        OWLClassAssertionAxiom res = df.getOWLClassAssertionAxiom(ce.getObject(), i.getObject(), annotations.getObjects());
        return Wrap.create(res, statement).add(annotations.getTriples()).append(i).append(ce);
    }
}
