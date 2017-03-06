package ru.avicomp.ontapi.translators;

import java.util.Optional;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Property;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLObject;

import ru.avicomp.ontapi.jena.impl.OntObjectImpl;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntObject;
import ru.avicomp.ontapi.jena.model.OntStatement;

/**
 * base class for {@link HasKeyTranslator}, {@link SubPropertyChainOfTranslator}, {@link DisjointUnionTranslator}
 * <p>
 * Created by @szuev on 18.10.2016.
 */
abstract class AbstractSubChainedTranslator<Axiom extends OWLLogicalAxiom, O extends OntObject> extends AxiomTranslator<Axiom> {

    abstract OWLObject getSubject(Axiom axiom);

    abstract Property getPredicate();

    abstract Stream<? extends OWLObject> getObjects(Axiom axiom);

    abstract Class<O> getView();

    @Override
    public void write(Axiom axiom, OntGraphModel model) {
        WriteHelper.writeList(model, getSubject(axiom), getPredicate(), getObjects(axiom), axiom.annotations());
    }

    @Override
    Stream<OntStatement> statements(OntGraphModel model) {
        return model.ontObjects(getView())
                .map(o -> o.statement(getPredicate()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(OntStatement::isLocal);
    }

    Stream<OntStatement> content(OntStatement statement) {
        return Stream.concat(Stream.of(statement),
                ((OntObjectImpl) statement.getSubject()).rdfListContent(getPredicate()));
    }
}
