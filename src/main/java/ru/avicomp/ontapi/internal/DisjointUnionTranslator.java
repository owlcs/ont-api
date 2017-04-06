package ru.avicomp.ontapi.internal;

import java.util.stream.Stream;

import org.apache.jena.rdf.model.Property;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDisjointUnionAxiom;
import org.semanticweb.owlapi.model.OWLObject;

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
    public Wrap<OWLDisjointUnionAxiom> asAxiom(OntStatement statement) {
        ConfigProvider.Config conf = getConfig(statement);
        OntClass clazz = statement.getSubject().as(OntClass.class);
        Wrap<? extends OWLClassExpression> subject = ReadHelper.fetchClassExpression(clazz, conf.dataFactory());
        Wrap.Collection<? extends OWLClassExpression> members = Wrap.Collection.create(clazz.disjointUnionOf().map(s -> ReadHelper.fetchClassExpression(s, conf.dataFactory())));
        Wrap.Collection<OWLAnnotation> annotations = ReadHelper.getStatementAnnotations(statement, conf.dataFactory(), conf.loaderConfig());
        OWLDisjointUnionAxiom res = conf.dataFactory().getOWLDisjointUnionAxiom(subject.getObject().asOWLClass(), members.getObjects(), annotations.getObjects());
        return Wrap.create(res, content(statement)).add(annotations.getTriples()).add(members.getTriples());
    }
}
