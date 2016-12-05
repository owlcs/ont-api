package ru.avicomp.ontapi.translators;

import java.util.Set;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Resource;
import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.jena.model.*;
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
    Stream<OntStatement> statements(OntGraphModel model) {
        OntID id = model.getID();
        return model.statements()
                .filter(OntStatement::isLocal)
                .filter(OntStatement::isAnnotation)
                .filter(s -> testAnnotationSubject(s.getSubject(), id));
    }

    private static boolean testAnnotationSubject(Resource candidate, OntID id) {
        return !candidate.equals(id) && (candidate.isURIResource() || candidate.canAs(OntIndividual.Anonymous.class));
    }

    @Override
    OWLAnnotationAssertionAxiom create(OntStatement statement, Set<OWLAnnotation> annotations) {
        OWLAnnotationSubject subject = RDF2OWLHelper.getAnnotationSubject(statement.getSubject());
        OWLAnnotationProperty property = RDF2OWLHelper.getAnnotationProperty(statement.getPredicate().as(OntNAP.class));
        OWLAnnotationValue value = RDF2OWLHelper.getAnnotationValue(statement.getObject());
        return new OWLAnnotationAssertionAxiomImpl(subject, property, value, annotations);
    }

}
