package ru.avicomp.ontapi;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

/**
 * to work with {@link org.apache.jena.rdf.model.Model}
 * Created by szuev on 20.10.2016.
 */
public class JenaUtils {

    public static Resource createTypedList(Model model, Resource type, List<? extends RDFNode> members) {
        if (members.isEmpty()) return RDF.nil.inModel(model);
        Resource res = model.createResource();
        res.addProperty(RDF.type, type);
        res.addProperty(RDF.first, members.remove(0));
        res.addProperty(RDF.rest, createTypedList(model, type, members));
        return res;
    }

    public static Resource createTypedList(Model model, Resource type, Stream<? extends RDFNode> members) {
        return createTypedList(model, type, members.collect(Collectors.toList()));
    }

    public static Graph convert(Graph graph) {
        return convertOWL1DLtoOWL2DL(convertRDFStoOWL(graph));
    }

    /**
     * see <a href='https://www.w3.org/TR/rdf-schema'>RDFS</a>
     *
     * @param graph Graph
     * @return Graph
     */
    public static Graph convertRDFStoOWL(Graph graph) {
        replaceType(graph, RDFS.Class, OWL.Class);
        //>>OWLDataProperty:range=OWLDataRange, domain=OWLClassExpression
        //>>OWLObjectProperty:range=CE, domain=CE
        // rdf:Property:
        // if no domain, range and subPropertyOf then it is AnnotationProperty

        // if range is Literal -> DataProperty
        // else -> ObjectProperty
        return graph;
    }

    /**
     * OWL 1 DL -> OWL 2 DL
     * see <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Parsing_of_the_Ontology_Header_and_Declarations'>Chapter 3, Table 5 and Table 6</a>
     */
    public static Graph convertOWL1DLtoOWL2DL(Graph graph) {
        Stream.of(OWL.DataRange, RDFS.Datatype, OWL.Restriction, OWL.Class).forEach(r -> deleteType(graph, r, RDFS.Class));
        Stream.of(OWL.ObjectProperty, OWL.FunctionalProperty, OWL.InverseFunctionalProperty, OWL.TransitiveProperty,
                OWL.DatatypeProperty, OWL.AnnotationProperty, OWL.OntologyProperty).forEach(r -> deleteType(graph, r, RDF.Property));
        Stream.of(OWL.InverseFunctionalProperty, OWL.TransitiveProperty, OWL.SymmetricProperty).forEach(r -> addType(graph, r, OWL.ObjectProperty));
        replaceType(graph, OWL.OntologyProperty, OWL.AnnotationProperty);
        return graph;
    }

    private static void deleteType(Graph graph, Resource typeToFind, Resource typeToRemove) {
        graph.find(Node.ANY, RDF.type.asNode(), typeToFind.asNode()).mapWith(t -> Triple.create(t.getSubject(), RDF.type.asNode(), typeToRemove.asNode()))
                .forEachRemaining(graph::delete);
    }

    private static void addType(Graph graph, Resource typeToFind, Resource typeToAdd) {
        graph.find(Node.ANY, RDF.type.asNode(), typeToFind.asNode()).mapWith(t -> Triple.create(t.getSubject(), RDF.type.asNode(), typeToAdd.asNode())).
                forEachRemaining(graph::add);
    }

    private static void replaceType(Graph graph, Resource typeToFind, Resource typeToReplace) {
        graph.find(Node.ANY, RDF.type.asNode(), typeToFind.asNode()).toSet().forEach(t -> {
            graph.delete(t);
            graph.add(Triple.create(t.getSubject(), RDF.type.asNode(), typeToReplace.asNode()));
        });
    }
}
