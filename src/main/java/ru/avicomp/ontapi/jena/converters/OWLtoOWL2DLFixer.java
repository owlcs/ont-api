package ru.avicomp.ontapi.jena.converters;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDFS;

import ru.avicomp.ontapi.jena.utils.Models;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

/**
 * OWL 1 DL -> OWL 2 DL
 * See <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Parsing_of_the_Ontology_Header_and_Declarations'>Chapter 3, Table 5 and Table 6</a>
 */
public class OWLtoOWL2DLFixer extends TransformAction {
    public OWLtoOWL2DLFixer(Graph graph) {
        super(graph);
    }

    @Override
    public void perform() {
        findByType(OWL.OntologyProperty).forEachRemaining(node -> addType(node, OWL.AnnotationProperty));
        findByType(OWL.AnnotationProperty).forEachRemaining(node -> deleteType(node, OWL.OntologyProperty));
        Stream.of(OWL.DataRange, RDFS.Datatype, OWL.Restriction, OWL.Class).
                forEach(type -> findByType(type).forEachRemaining(node -> deleteType(node, RDFS.Class)));
        Stream.of(OWL.ObjectProperty, OWL.FunctionalProperty, OWL.InverseFunctionalProperty, OWL.TransitiveProperty, OWL.DatatypeProperty, OWL.AnnotationProperty).
                forEach(type -> findByType(type).forEachRemaining(node -> deleteType(node, RDF.Property)));
        Stream.of(OWL.InverseFunctionalProperty, OWL.TransitiveProperty, OWL.SymmetricProperty).
                forEach(type -> findByType(type).forEachRemaining(node -> addType(node, OWL.ObjectProperty)));
        fixOntology();
    }

    /**
     * merge several owl:Ontology to single one.
     * as primary choose the one that has the largest number of triplets.
     * if there is no any owl:Ontology -> add new anonymous owl:Ontology
     */
    private void fixOntology() {
        Model m = getBaseModel();
        // choose or create the new one:
        Resource ontology = Models.asStream(m.listStatements(null, RDF.type, OWL.Ontology))
                .map(Statement::getSubject)
                .sorted(Comparator.comparing(this::statementsCount).reversed())
                .findFirst()
                .orElse(m.createResource().addProperty(RDF.type, OWL.Ontology));
        // move all content from other ontologies to the selected one
        Stream<Resource> other = Models.asStream(m.listStatements(null, RDF.type, OWL.Ontology)
                .mapWith(Statement::getSubject)
                .filterDrop(ontology::equals));
        List<Statement> rest = other
                .map(o -> Models.asStream(m.listStatements(o, null, (RDFNode) null)))
                .flatMap(Function.identity()).collect(Collectors.toList());
        rest.forEach(s -> ontology.addProperty(s.getPredicate(), s.getObject()));
        // remove all other ontologies
        m.remove(rest);
    }

    private Integer statementsCount(Resource subject) {
        return (int) Models.asStream(subject.listProperties()).count();
    }

    @Override
    public boolean test() {
        return !containsType(OWL.Ontology) || containsType(RDFS.Class) || containsType(RDF.Property) || containsType(OWL.OntologyProperty);
    }

    private ExtendedIterator<Node> findByType(Resource type) {
        return getBaseGraph().find(Node.ANY, RDF_TYPE, type.asNode()).mapWith(Triple::getSubject);
    }
}
