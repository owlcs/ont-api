package ru.avicomp.ontapi.translators;

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import ru.avicomp.ontapi.jena.model.OntCE;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntStatement;
import uk.ac.manchester.cs.owl.owlapi.OWLSubClassOfAxiomImpl;

/**
 * Examples:
 * pizza:JalapenoPepperTopping rdfs:subClassOf pizza:PepperTopping.
 * pizza:JalapenoPepperTopping rdfs:subClassOf [ a owl:Restriction; owl:onProperty pizza:hasSpiciness; owl:someValuesFrom pizza:Hot].
 * <p>
 * Created by @szuev on 28.09.2016.
 */
class SubClassOfTranslator extends AxiomTranslator<OWLSubClassOfAxiom> {
    @Override
    public void write(OWLSubClassOfAxiom axiom, OntGraphModel model) {
        WriteHelper.writeTriple(model, axiom.getSubClass(), RDFS.subClassOf, axiom.getSuperClass(), axiom.annotations());
    }

    @Override
    Stream<OntStatement> statements(OntGraphModel model) {
        return model.ontObjects(OntCE.class)
                .map(subj -> subj.subClassOf().map(obj -> subj.statement(RDFS.subClassOf, obj)))
                .flatMap(Function.identity())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(OntStatement::isLocal);
    }

    @Override
    OWLSubClassOfAxiom create(OntStatement statement, Set<OWLAnnotation> annotations) {
        OWLClassExpression sub = ReadHelper.getClassExpression(statement.getSubject().as(OntCE.class));
        OWLClassExpression sup = ReadHelper.getClassExpression(statement.getObject().as(OntCE.class));
        return new OWLSubClassOfAxiomImpl(sub, sup, annotations);
    }

    @Override
    Wrap<OWLSubClassOfAxiom> asAxiom(OntStatement statement) {
        Wrap<? extends OWLClassExpression> sub = ReadHelper._getClassExpression(statement.getSubject().as(OntCE.class), getDataFactory());
        Wrap<? extends OWLClassExpression> sup = ReadHelper._getClassExpression(statement.getObject().as(OntCE.class), getDataFactory());
        Wrap.Collection<OWLAnnotation> annotations = annotations(statement);
        OWLSubClassOfAxiom res = getDataFactory().getOWLSubClassOfAxiom(sub.getObject(), sup.getObject(), annotations.getObjects());
        return Wrap.create(res, statement).add(annotations.getTriples()).append(sub).append(sup);
    }
}
