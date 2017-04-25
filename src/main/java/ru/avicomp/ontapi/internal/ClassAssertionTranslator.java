package ru.avicomp.ontapi.internal;

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
public class ClassAssertionTranslator extends AxiomTranslator<OWLClassAssertionAxiom> {
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
    public Stream<OntStatement> statements(OntGraphModel model) {
        return model.statements(null, RDF.type, null)
                .filter(OntStatement::isLocal)
                .filter(s -> s.getObject().canAs(OntCE.class))
                .filter(s -> s.getSubject().canAs(OntIndividual.class));
    }

    @Override
    public boolean testStatement(OntStatement statement) {
        return statement.getPredicate().equals(RDF.type)
                && statement.getObject().canAs(OntCE.class)
                && statement.getSubject().canAs(OntIndividual.class);
    }

    @Override
    public Wrap<OWLClassAssertionAxiom> asAxiom(OntStatement statement) {
        ConfigProvider.Config conf = getConfig(statement);
        Wrap<? extends OWLIndividual> i = ReadHelper.fetchIndividual(statement.getSubject().as(OntIndividual.class), conf.dataFactory());
        Wrap<? extends OWLClassExpression> ce = ReadHelper.fetchClassExpression(statement.getObject().as(OntCE.class), conf.dataFactory());
        Wrap.Collection<OWLAnnotation> annotations = ReadHelper.getStatementAnnotations(statement, conf.dataFactory(), conf.loaderConfig());
        OWLClassAssertionAxiom res = conf.dataFactory().getOWLClassAssertionAxiom(ce.getObject(), i.getObject(), annotations.getObjects());
        return Wrap.create(res, statement).add(annotations.getTriples()).append(i).append(ce);
    }
}
