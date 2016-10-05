package ru.avicomp.ontapi.parsers;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.model.HasDomain;
import org.semanticweb.owlapi.model.HasProperty;
import org.semanticweb.owlapi.model.OWLAxiom;

/**
 * base class for {@link ObjectPropertyDomainParser} and {@link DataPropertyDomainParser} and {@link AnnotationPropertyDomainParser}
 * for rdfs:domain tripler.
 * <p>
 * Created by @szuev on 30.09.2016.
 */
abstract class AbstractPropertyDomainParser<Axiom extends OWLAxiom & HasDomain & HasProperty> extends AxiomParser<Axiom> {
    public Resource getSubject() {
        return ParseUtils.toResource(getAxiom().getProperty());
    }

    public Property getPredicate() {
        return RDFS.domain;
    }

    @Override
    public void process(Graph graph) {
        Model model = ModelFactory.createModelForGraph(graph);
        model.add(getSubject(), getPredicate(), ParseUtils.toResource(model, getAxiom().getDomain()));
    }

    @Override
    public void reverse(Graph graph) {
        graph.remove(getSubject().asNode(), getPredicate().asNode(), Node.ANY);
    }
}
