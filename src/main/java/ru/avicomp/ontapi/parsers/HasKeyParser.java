package ru.avicomp.ontapi.parsers;

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL2;
import org.semanticweb.owlapi.model.OWLHasKeyAxiom;

/**
 * for HasKey axiom.
 * example:
 * :MyClass1 owl:hasKey ( :ob-prop-1 ) .
 * <p>
 * Created by @szuev on 17.10.2016.
 */
class HasKeyParser extends AxiomParser<OWLHasKeyAxiom> {
    @Override
    public void process(Graph graph) {
        Model model = TranslationHelper.createModel(graph);
        Resource subject = TranslationHelper.addRDFNode(model, getAxiom().getClassExpression()).asResource();
        Property predicate = OWL2.hasKey;
        RDFNode object = TranslationHelper.addRDFList(model, getAxiom().propertyExpressions());
        model.add(subject, predicate, object);
        TranslationHelper.addAnnotations(model, subject, predicate, object, getAxiom());
    }
}
