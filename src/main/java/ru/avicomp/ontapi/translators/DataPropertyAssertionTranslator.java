package ru.avicomp.ontapi.translators;

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

    @Override
    Stream<OntStatement> statements(OntGraphModel model) {
        // skip everything that is not correspond axiom rule
        // (e.g. OWL-API doesn't allow rdf:List of literals as assertion object)
        return model.statements()
                .filter(OntStatement::isLocal)
                .filter(OntStatement::isData)
                .filter(s -> s.getSubject().canAs(OntIndividual.class))
                .filter(s -> s.getObject().isLiteral());
    }

    @Override
    Wrap<OWLDataPropertyAssertionAxiom> asAxiom(OntStatement statement) {
        OWLDataFactory df = getDataFactory(statement.getModel());
        Wrap<? extends OWLIndividual> i = ReadHelper.getIndividual(statement.getSubject().as(OntIndividual.class), df);
        Wrap<OWLDataProperty> p = ReadHelper.getDataProperty(statement.getPredicate().as(OntNDP.class), df);
        Wrap<OWLLiteral> l = ReadHelper.getLiteral(statement.getObject().asLiteral(), df);
        Wrap.Collection<OWLAnnotation> annotations = ReadHelper.getStatementAnnotations(statement, df);
        OWLDataPropertyAssertionAxiom res = df.getOWLDataPropertyAssertionAxiom(p.getObject(), i.getObject(), l.getObject(),
                annotations.getObjects());
        return Wrap.create(res, statement).add(annotations.getTriples()).append(i).append(p).append(l);
    }
}
