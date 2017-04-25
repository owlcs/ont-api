package ru.avicomp.ontapi.internal;

import java.util.stream.Stream;

import org.apache.jena.vocabulary.RDF;
import org.semanticweb.owlapi.model.OWLPropertyAssertionAxiom;

import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntNPA;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.vocabulary.OWL;

/**
 * for data and object negative property assertion
 * children:
 * {@link NegativeDataPropertyAssertionTranslator}
 * {@link NegativeObjectPropertyAssertionTranslator}
 * <p>
 * Created by szuev on 29.11.2016.
 */
public abstract class AbstractNegativePropertyAssertionTranslator<Axiom extends OWLPropertyAssertionAxiom, NPA extends OntNPA> extends AxiomTranslator<Axiom> {

    abstract NPA createNPA(Axiom axiom, OntGraphModel model);

    abstract Class<NPA> getView();

    @Override
    public void write(Axiom axiom, OntGraphModel model) {
        WriteHelper.addAnnotations(createNPA(axiom, model), axiom.annotations());
    }

    @Override
    public Stream<OntStatement> statements(OntGraphModel model) {
        return model.statements(null, RDF.type, OWL.NegativePropertyAssertion)
                .filter(OntStatement::isLocal)
                .filter(s -> s.getSubject().canAs(getView()));
    }

    @Override
    public boolean testStatement(OntStatement statement) {
        return statement.getObject().equals(OWL.NegativePropertyAssertion)
                && statement.getPredicate().equals(RDF.type)
                && statement.getSubject().canAs(getView());
    }
}
