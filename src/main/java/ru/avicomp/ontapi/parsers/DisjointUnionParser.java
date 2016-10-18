package ru.avicomp.ontapi.parsers;

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL2;
import org.semanticweb.owlapi.model.OWLDisjointUnionAxiom;

/**
 * for DisjointUnion
 * example: :MyClass1 owl:disjointUnionOf ( :MyClass2 [ a owl:Class ; owl:unionOf ( :MyClass3 :MyClass4  ) ] ) ;
 * <p>
 * Created by @szuev on 17.10.2016.
 */
class DisjointUnionParser extends AxiomParser<OWLDisjointUnionAxiom> {
    @Override
    public void process(Graph graph) {
        Model model = TranslationHelper.createModel(graph);
        Resource subject = TranslationHelper.addRDFNode(model, getAxiom().getOWLClass()).asResource();
        Property predicate = OWL2.disjointUnionOf;
        RDFNode object = TranslationHelper.addRDFList(model, getAxiom().classExpressions());
        model.add(subject, predicate, object);
        TranslationHelper.addAnnotations(model, subject, predicate, object, getAxiom());
    }
}
