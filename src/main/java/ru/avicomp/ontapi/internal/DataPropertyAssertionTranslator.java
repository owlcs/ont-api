package ru.avicomp.ontapi.internal;

import java.util.stream.Stream;

import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntIndividual;
import ru.avicomp.ontapi.jena.model.OntNDP;
import ru.avicomp.ontapi.jena.model.OntStatement;

/**
 * property that belongs to individual.
 * individual could be anonymous!
 * <p>
 * Created by @szuev on 28.09.2016.
 */
class DataPropertyAssertionTranslator extends AxiomTranslator<OWLDataPropertyAssertionAxiom> {
    @Override
    public void write(OWLDataPropertyAssertionAxiom axiom, OntGraphModel model) {
        WriteHelper.writeAssertionTriple(model, axiom.getSubject(), axiom.getProperty(), axiom.getObject(), axiom.annotations());
    }

    /**
     * positive data property assertion: the rule "a R v":
     * see <a href='https://www.w3.org/TR/owl2-quick-reference/'>Assertions</a>
     *
     * @param model {@link OntGraphModel} the model
     * @return Stream of {@link OntStatement}
     */
    @Override
    public Stream<OntStatement> statements(OntGraphModel model) {
        return model.statements()
                .filter(OntStatement::isLocal)
                .filter(OntStatement::isData)
                .filter(s -> s.getObject().isLiteral())
                .filter(s -> s.getSubject().canAs(OntIndividual.class));
    }

    @Override
    public boolean testStatement(OntStatement statement) {
        return statement.isData()
                && statement.getObject().isLiteral()
                && statement.getSubject().canAs(OntIndividual.class);
    }

    @Override
    public Wrap<OWLDataPropertyAssertionAxiom> asAxiom(OntStatement statement) {
        ConfigProvider.Config conf = getConfig(statement);
        Wrap<? extends OWLIndividual> i = ReadHelper.fetchIndividual(statement.getSubject().as(OntIndividual.class), conf.dataFactory());
        Wrap<OWLDataProperty> p = ReadHelper.fetchDataProperty(statement.getPredicate().as(OntNDP.class), conf.dataFactory());
        Wrap<OWLLiteral> l = ReadHelper.getLiteral(statement.getObject().asLiteral(), conf.dataFactory());
        Wrap.Collection<OWLAnnotation> annotations = ReadHelper.getStatementAnnotations(statement, conf.dataFactory(), conf.loaderConfig());
        OWLDataPropertyAssertionAxiom res = conf.dataFactory().getOWLDataPropertyAssertionAxiom(p.getObject(), i.getObject(), l.getObject(),
                annotations.getObjects());
        return Wrap.create(res, statement).add(annotations.getTriples()).append(i).append(p).append(l);
    }
}
