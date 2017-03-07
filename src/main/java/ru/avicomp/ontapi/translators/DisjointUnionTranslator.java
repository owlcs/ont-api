package ru.avicomp.ontapi.translators;

import java.util.stream.Stream;

import org.apache.jena.rdf.model.Property;
import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.jena.model.OntClass;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.vocabulary.OWL;

/**
 * base class: {@link AbstractSubChainedTranslator}
 * for DisjointUnion
 * example: :MyClass1 owl:disjointUnionOf ( :MyClass2 [ a owl:Class ; owl:unionOf ( :MyClass3 :MyClass4  ) ] ) ;
 * <p>
 * Created by @szuev on 17.10.2016.
 */
class DisjointUnionTranslator extends AbstractSubChainedTranslator<OWLDisjointUnionAxiom, OntClass> {
    @Override
    public OWLObject getSubject(OWLDisjointUnionAxiom axiom) {
        return axiom.getOWLClass();
    }

    @Override
    public Property getPredicate() {
        return OWL.disjointUnionOf;
    }

    @Override
    public Stream<? extends OWLObject> getObjects(OWLDisjointUnionAxiom axiom) {
        return axiom.classExpressions();
    }

    @Override
    Class<OntClass> getView() {
        return OntClass.class;
    }

    @Override
    Wrap<OWLDisjointUnionAxiom> asAxiom(OntStatement statement) {
        OWLDataFactory df = getDataFactory();
        OntClass clazz = statement.getSubject().as(OntClass.class);
        Wrap<? extends OWLClassExpression> subject = ReadHelper.getClassExpression(clazz, df);
        Wrap.Collection<? extends OWLClassExpression> members = Wrap.Collection.create(clazz.disjointUnionOf().map(s -> ReadHelper.getClassExpression(s, df)));
        Wrap.Collection<OWLAnnotation> annotations = ReadHelper.getStatementAnnotations(statement, getDataFactory());
        OWLDisjointUnionAxiom res = df.getOWLDisjointUnionAxiom(subject.getObject().asOWLClass(), members.getObjects(), annotations.getObjects());
        return Wrap.create(res, content(statement)).add(annotations.getTriples()).add(members.getTriples());
    }
}
