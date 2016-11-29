package ru.avicomp.ontapi.translators;

import java.util.stream.Stream;

import org.apache.jena.rdf.model.Property;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLObject;

import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntObject;
import ru.avicomp.ontapi.jena.model.OntStatement;

/**
 * base class for {@link HasKeyTranslator}, {@link SubPropertyChainOfTranslator}, {@link DisjointUnionTranslator}
 * <p>
 * Created by @szuev on 18.10.2016.
 */
abstract class AbstractSubChainedTranslator<Axiom extends OWLLogicalAxiom, View extends OntObject> extends AxiomTranslator<Axiom> {

    abstract OWLObject getSubject(Axiom axiom);

    abstract Property getPredicate();

    abstract Stream<? extends OWLObject> getObjects(Axiom axiom);

    abstract Class<View> getView();

    @Override
    public void write(Axiom axiom, OntGraphModel model) {
        OWL2RDFHelper.writeTriple(model, getSubject(axiom), getPredicate(), getObjects(axiom), axiom.annotations(), true);
    }

    @Override
    Stream<OntStatement> statements(OntGraphModel model) {
        return model.ontObjects(getView()).filter(o -> o.hasProperty(getPredicate())).map(o -> o.getStatement(getPredicate())).filter(OntStatement::isLocal);
    }
}
