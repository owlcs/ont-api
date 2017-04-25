package ru.avicomp.ontapi.internal;

import java.util.stream.Stream;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFList;
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
public abstract class AbstractSubChainedTranslator<Axiom extends OWLLogicalAxiom, O extends OntObject> extends AxiomTranslator<Axiom> {

    abstract OWLObject getSubject(Axiom axiom);

    abstract Property getPredicate();

    abstract Stream<? extends OWLObject> getObjects(Axiom axiom);

    abstract Class<O> getView();

    @Override
    public void write(Axiom axiom, OntGraphModel model) {
        WriteHelper.writeList(model, getSubject(axiom), getPredicate(), getObjects(axiom), axiom.annotations());
    }

    @Override
    public Stream<OntStatement> statements(OntGraphModel model) {
        return model.statements(null, getPredicate(), null)
                .filter(OntStatement::isLocal)
                .filter(s -> s.getSubject().canAs(getView()))
                .filter(s -> s.getObject().canAs(RDFList.class));
    }

    @Override
    public boolean testStatement(OntStatement statement) {
        return statement.getSubject().equals(getPredicate())
                && statement.getSubject().canAs(getView())
                && statement.getObject().canAs(RDFList.class);
    }

    Stream<OntStatement> content(OntStatement statement) {
        return Stream.concat(Stream.of(statement),
                ((OntObjectImpl) statement.getSubject()).rdfListContent(getPredicate()));
    }
}
