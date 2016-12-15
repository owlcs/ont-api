package ru.avicomp.ontapi.translators;

import java.util.Set;
import java.util.stream.Stream;

import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;

import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntIndividual;
import ru.avicomp.ontapi.jena.model.OntOPE;
import ru.avicomp.ontapi.jena.model.OntStatement;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectPropertyAssertionAxiomImpl;

/**
 * example:
 * gr:Saturday rdf:type owl:NamedIndividual , gr:hasNext gr:Sunday ;
 * Created by @szuev on 01.10.2016.
 */
class ObjectPropertyAssertionTranslator extends AxiomTranslator<OWLObjectPropertyAssertionAxiom> {
    @Override
    public void write(OWLObjectPropertyAssertionAxiom axiom, OntGraphModel model) {
        OWL2RDFHelper.writeTriple(model, axiom.getSubject(), axiom.getProperty(), axiom.getObject(), axiom.annotations());
    }

    @Override
    Stream<OntStatement> statements(OntGraphModel model) {
        // skip everything that is not correspond axiom rule
        // (e.g. OWL-API allows only Individual as assertion object. rdf:List is not supported by this level of our api also)
        return model.statements()
                .filter(OntStatement::isLocal)
                .filter(OntStatement::isObject)
                .filter(s -> s.getSubject().canAs(OntIndividual.class))
                .filter(s -> s.getObject().canAs(OntIndividual.class));
    }

    @Override
    OWLObjectPropertyAssertionAxiom create(OntStatement statement, Set<OWLAnnotation> annotations) {
        OWLIndividual subject = RDF2OWLHelper.getIndividual(statement.getSubject().as(OntIndividual.class));
        OWLObjectPropertyExpression property = RDF2OWLHelper.getObjectProperty(statement.getPredicate().as(OntOPE.class));
        OWLIndividual object = RDF2OWLHelper.getIndividual(statement.getObject().as(OntIndividual.class));
        return new OWLObjectPropertyAssertionAxiomImpl(subject, property, object, annotations);
    }
}
