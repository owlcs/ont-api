package ru.avicomp.ontapi.translators;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Resource;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLIndividual;

import ru.avicomp.ontapi.jena.model.OntCE;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntIndividual;
import ru.avicomp.ontapi.jena.model.OntStatement;
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
        Resource subject = individual.isAnonymous() ?
                OWL2RDFHelper.toResource(individual).inModel(model) :
                OWL2RDFHelper.addIndividual(model, individual);
        model.add(subject, RDF.type, ce);
        OntIndividual res = subject.inModel(model).as(OntIndividual.class);
        OWL2RDFHelper.addAnnotations(res.getStatement(RDF.type, ce), axiom.annotations());
    }

    @Override
    Stream<OntStatement> statements(OntGraphModel model) {
        return model.ontObjects(OntIndividual.class)
                .map(i -> i.classes().map(ce -> i.getStatement(RDF.type, ce)))
                .flatMap(Function.identity())
                .filter(OntStatement::isLocal);
    }

    @Override
    OWLClassAssertionAxiom create(OntStatement statement, Set<OWLAnnotation> annotations) {
        OWLIndividual i = RDF2OWLHelper.getIndividual(statement.getSubject().as(OntIndividual.class));
        OWLClassExpression ce = RDF2OWLHelper.getClassExpression(statement.getObject().as(OntCE.class));
        return new OWLClassAssertionAxiomImpl(i, ce, annotations);
    }
}
