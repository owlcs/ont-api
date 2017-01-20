package ru.avicomp.ontapi.translators;

import java.util.Set;
import java.util.stream.Stream;

import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntIndividual;
import ru.avicomp.ontapi.jena.model.OntNDP;
import ru.avicomp.ontapi.jena.model.OntStatement;
import uk.ac.manchester.cs.owl.owlapi.OWLDataPropertyAssertionAxiomImpl;

/**
 * property that belongs to individual.
 * individual could be anonymous!
 * <p>
 * Created by @szuev on 28.09.2016.
 */
class DataPropertyAssertionTranslator extends AxiomTranslator<OWLDataPropertyAssertionAxiom> {
    @Override
    public void write(OWLDataPropertyAssertionAxiom axiom, OntGraphModel model) {
        OWL2RDFHelper.writeAssertionTriple(model, axiom.getSubject(), axiom.getProperty(), axiom.getObject(), axiom.annotations());
    }

    @Override
    Stream<OntStatement> statements(OntGraphModel model) {
        // skip everything that is not correspond axiom rule
        // (e.g. OWL-API doesn't allow rdf:List of literals as assertion object)
        return model.statements()
                .filter(OntStatement::isLocal)
                .filter(OntStatement::isData)
                .filter(s -> s.getSubject().canAs(OntIndividual.class))
                .filter(s -> s.getObject().isLiteral());
    }

    @Override
    OWLDataPropertyAssertionAxiom create(OntStatement statement, Set<OWLAnnotation> annotations) {
        OWLIndividual i = RDF2OWLHelper.getIndividual(statement.getSubject().as(OntIndividual.class));
        OWLDataProperty p = RDF2OWLHelper.getDataProperty(statement.getPredicate().as(OntNDP.class));
        OWLLiteral l = RDF2OWLHelper.getLiteral(statement.getObject().asLiteral());
        return new OWLDataPropertyAssertionAxiomImpl(i, p, l, annotations);
    }
}
