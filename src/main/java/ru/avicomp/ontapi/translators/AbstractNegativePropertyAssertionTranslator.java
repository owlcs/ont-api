package ru.avicomp.ontapi.translators;

import java.util.stream.Stream;

import org.semanticweb.owlapi.model.OWLPropertyAssertionAxiom;

import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntNPA;
import ru.avicomp.ontapi.jena.model.OntObject;
import ru.avicomp.ontapi.jena.model.OntStatement;

/**
 * for data and object negative property assertion
 * children:
 * {@link NegativeDataPropertyAssertionTranslator}
 * {@link NegativeObjectPropertyAssertionTranslator}
 * <p>
 * Created by szuev on 29.11.2016.
 */
abstract class AbstractNegativePropertyAssertionTranslator<Axiom extends OWLPropertyAssertionAxiom, NPA extends OntNPA> extends AxiomTranslator<Axiom> {

    abstract NPA createNPA(Axiom axiom, OntGraphModel model);

    abstract Class<NPA> getView();

    @Override
    public void write(Axiom axiom, OntGraphModel model) {
        OWL2RDFHelper.addAnnotations(createNPA(axiom, model), axiom.annotations());
    }

    @Override
    Stream<OntStatement> statements(OntGraphModel model) {
        return model.ontObjects(getView()).map(OntObject::getRoot).filter(OntStatement::isLocal);
    }
}
