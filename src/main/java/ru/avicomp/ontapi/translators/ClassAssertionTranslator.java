package ru.avicomp.ontapi.translators;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;

import ru.avicomp.ontapi.jena.model.OntCE;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntIndividual;

/**
 * creating individual (both named and anonymous):
 * pizza:France rdf:type owl:NamedIndividual, pizza:Country, owl:Thing.
 * <p>
 * Created by @szuev on 28.09.2016.
 */
class ClassAssertionTranslator extends AxiomTranslator<OWLClassAssertionAxiom> {
    @Override
    public void write(OWLClassAssertionAxiom axiom, OntGraphModel model) {
        OntCE ce = TranslationHelper.addClassExpression(model, axiom.getClassExpression());
        OWLIndividual individual = axiom.getIndividual();
        Resource subject = individual.isAnonymous() ?
                TranslationHelper.toResource(individual) :
                TranslationHelper.addIndividual(model, individual);
        model.add(subject, RDF.type, ce);
        TranslationHelper.addAnnotations(subject.inModel(model).as(OntIndividual.class), axiom.annotations());
    }
}
