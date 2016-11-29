package ru.avicomp.ontapi.translators;

import java.util.Set;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.vocabulary.OWL2;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDisjointUnionAxiom;
import org.semanticweb.owlapi.model.OWLObject;

import ru.avicomp.ontapi.jena.model.OntClass;
import ru.avicomp.ontapi.jena.model.OntStatement;
import uk.ac.manchester.cs.owl.owlapi.OWLDisjointUnionAxiomImpl;

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
        return OWL2.disjointUnionOf;
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
    OWLDisjointUnionAxiom create(OntStatement statement, Set<OWLAnnotation> annotations) {
        OntClass clazz = statement.getSubject().as(OntClass.class);
        Stream<OWLClassExpression> ces = clazz.disjointUnionOf().map(RDF2OWLHelper::getClassExpression);
        return new OWLDisjointUnionAxiomImpl(RDF2OWLHelper.getClassExpression(clazz).asOWLClass(), ces, annotations);
    }
}
