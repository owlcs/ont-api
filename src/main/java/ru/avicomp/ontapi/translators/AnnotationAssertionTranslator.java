package ru.avicomp.ontapi.translators;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationSubject;
import org.semanticweb.owlapi.model.OWLAnnotationValue;

import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntIndividual;
import ru.avicomp.ontapi.jena.model.OntNAP;
import ru.avicomp.ontapi.jena.model.OntStatement;
import uk.ac.manchester.cs.owl.owlapi.OWLAnnotationAssertionAxiomImpl;

/**
 * Examples:
 * foaf:LabelProperty vs:term_status "unstable" .
 * foaf:LabelProperty rdfs:isDefinedBy <http://xmlns.com/foaf/0.1/> .
 * pizza:UnclosedPizza rdfs:label "PizzaAberta"@pt .
 * <p>
 * Created by @szuev on 28.09.2016.
 */
class AnnotationAssertionTranslator extends AxiomTranslator<OWLAnnotationAssertionAxiom> {
    @Override
    public void write(OWLAnnotationAssertionAxiom axiom, OntGraphModel model) {
        OWL2RDFHelper.writeTriple(model, axiom.getSubject(), axiom.getProperty(), axiom.getValue(), axiom.annotations());
    }

    @Override
    public Set<OWLTripleSet<OWLAnnotationAssertionAxiom>> read(OntGraphModel model) {
        Stream<OntStatement> assertions = model.statements()
                .filter(OntStatement::isLocal)
                .filter(OntStatement::isAnnotation)
                .filter(s -> s.getSubject().isURIResource() || s.getSubject().canAs(OntIndividual.Anonymous.class));
        Set<OWLTripleSet<OWLAnnotationAssertionAxiom>> res = new HashSet<>();
        assertions.forEach(assertion -> {
            RDF2OWLHelper.StatementContent content = new RDF2OWLHelper.StatementContent(assertion);

            OWLAnnotationSubject subject = RDF2OWLHelper.getAnnotationSubject(assertion.getSubject());
            OWLAnnotationProperty property = RDF2OWLHelper.getAnnotationProperty(assertion.getPredicate().as(OntNAP.class));
            OWLAnnotationValue value = RDF2OWLHelper.getAnnotationValue(assertion.getObject());

            res.add(wrap(new OWLAnnotationAssertionAxiomImpl(subject, property, value, content.getAnnotations()), content.getTriples()));
        });
        return res;
    }
}
