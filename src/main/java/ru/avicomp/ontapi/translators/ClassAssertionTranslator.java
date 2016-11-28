package ru.avicomp.ontapi.translators;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLIndividual;

import ru.avicomp.ontapi.jena.model.OntCE;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntIndividual;
import ru.avicomp.ontapi.jena.model.OntStatement;
import uk.ac.manchester.cs.owl.owlapi.OWLClassAssertionAxiomImpl;

/**
 * creating individual (both named and anonymous):
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
                OWL2RDFHelper.toResource(individual) :
                OWL2RDFHelper.addIndividual(model, individual);
        model.add(subject, RDF.type, ce);
        OWL2RDFHelper.addAnnotations(subject.inModel(model).as(OntIndividual.class), axiom.annotations());
    }

    @Override
    public Set<OWLTripleSet<OWLClassAssertionAxiom>> read(OntGraphModel model) {
        Stream<OntStatement> assertions = model.ontObjects(OntIndividual.class)
                .map(i -> i.classes().map(ce -> i.getStatement(RDF.type, ce)))
                .flatMap(Function.identity())
                .filter(OntStatement::isLocal);
        Set<OWLTripleSet<OWLClassAssertionAxiom>> res = new HashSet<>();
        assertions.forEach(s -> {
            RDF2OWLHelper.StatementContent content = new RDF2OWLHelper.StatementContent(s);
            OWLIndividual i = RDF2OWLHelper.getIndividual(s.getSubject().as(OntIndividual.class));
            OWLClassExpression ce = RDF2OWLHelper.getClassExpression(s.getObject().as(OntCE.class));
            OWLClassAssertionAxiom axiom = new OWLClassAssertionAxiomImpl(i, ce, content.getAnnotations());
            res.add(wrap(axiom, content.getTriples()));
        });
        return res;
    }
}
