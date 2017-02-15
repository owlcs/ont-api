package ru.avicomp.ontapi.translators;

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLIndividual;

import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.vocabulary.RDF;
import uk.ac.manchester.cs.owl.owlapi.OWLClassAssertionAxiomImpl;

/**
 * Creating individual (both named and anonymous):
 * pizza:France rdf:type owl:NamedIndividual, pizza:Country, owl:Thing.
 * <p>
 * Created by @szuev on 28.09.2016.
 */
class ClassAssertionTranslator extends AxiomTranslator<OWLClassAssertionAxiom> {
    @Override
    public void write(OWLClassAssertionAxiom axiom, OntGraphModel model) {
        OntCE ce = OWL2RDFHelper.addClassExpression(model, axiom.getClassExpression());
        OWLIndividual individual = axiom.getIndividual();
        OntObject subject = individual.isAnonymous() ?
                OWL2RDFHelper.toResource(individual).inModel(model).as(OntObject.class) :
                OWL2RDFHelper.addIndividual(model, individual);
        OntStatement statement = subject.addStatement(RDF.type, ce);
        OWL2RDFHelper.addAnnotations(statement, axiom.annotations());
    }

    @Override
    Stream<OntStatement> statements(OntGraphModel model) {
        return model.ontObjects(OntIndividual.class)
                .map(i -> i.classes().map(ce -> i.statement(RDF.type, ce)))
                .flatMap(Function.identity())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(OntStatement::isLocal);
    }

    @Override
    OWLClassAssertionAxiom create(OntStatement statement, Set<OWLAnnotation> annotations) {
        OWLIndividual i = RDF2OWLHelper.getIndividual(statement.getSubject().as(OntIndividual.class));
        OWLClassExpression ce = RDF2OWLHelper.getClassExpression(statement.getObject().as(OntCE.class));
        return new OWLClassAssertionAxiomImpl(i, ce, annotations);
    }
}
