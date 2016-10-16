package ru.avicomp.ontapi.parsers;

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.model.HasProperty;
import org.semanticweb.owlapi.model.HasRange;
import org.semanticweb.owlapi.model.OWLAxiom;

/**
 * base class for {@link DataPropertyRangeParser} and {@link ObjectPropertyRangeParser} and {@link AnnotationPropertyRangeParser}
 * example: foaf:name rdfs:range rdfs:Literal
 * <p>
 * Created by @szuev on 30.09.2016.
 */
abstract class AbstractPropertyRangeParser<Axiom extends OWLAxiom & HasProperty & HasRange> extends AxiomParser<Axiom> {

    public Resource getSubject() {
        return AxiomParseUtils.toResource(getAxiom().getProperty());
    }

    public Property getPredicate() {
        return RDFS.range;
    }

    @Override
    public void process(Graph graph) {
        Model model = AxiomParseUtils.createModel(graph);
        Resource subject = getSubject();
        Property predicate = getPredicate();
        RDFNode object = AxiomParseUtils.addRDFNode(model, getAxiom().getRange());
        model.add(subject, predicate, object);
        AnnotationsParseUtils.addAnnotations(model, subject, predicate, object, getAxiom());
    }

}
