package ru.avicomp.ontapi.parsers;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;

import ru.avicomp.ontapi.NodeIRIUtils;

/**
 * Helper for Axiom Annotations Parsing (operator 'TANN').
 * see <a href='https://www.w3.org/TR/owl2-mapping-to-rdf'>2.2 Translation of Annotations</a>
 * <p>
 * Created by szuev on 11.10.2016.
 */
public class AnnotationsParseUtils {

    /**
     * recursive operator TANN
     * see specification:
     *  <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Axioms_that_Generate_a_Main_Triple'>2.3 Translation of Axioms with Annotations</a> and
     *  <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Axioms_that_are_Translated_to_Multiple_Triples'>2.3.2 Axioms that are Translated to Multiple Triples</a>
     * <p>
     * This is the case if ax' is of type
     *  SubClassOf,
     *  SubObjectPropertyOf without a property chain as the subproperty expression,
     *  SubDataPropertyOf, ObjectPropertyDomain, DataPropertyDomain, ObjectPropertyRange, DataPropertyRange,
     *  InverseObjectProperties, FunctionalObjectProperty, FunctionalDataProperty, InverseFunctionalObjectProperty,
     *  ReflexiveObjectProperty, IrreflexiveObjectProperty, SymmetricObjectProperty, AsymmetricObjectProperty,
     *  TransitiveObjectProperty, ClassAssertion, ObjectPropertyAssertion, DataPropertyAssertion, Declaration,
     *  DisjointObjectProperties with two properties,
     *  DisjointDataProperties with two properties,
     *  DisjointClasses with two classes,
     *  DifferentIndividuals with two individuals, or
     *  AnnotationAssertion.
     * Also for
     *  EquivalentClasses, EquivalentObjectProperties, EquivalentDataProperties, or SameIndividual (see {@link AbstractNaryParser});
     * in last case call this method for each of triple from inner axiom.
     *
     * TODO:  DisjointUnion, SubObjectPropertyOf with a subproperty chain, or HasKey
     *
     * @param graph Graph
     * @param axiom OWLAxiom
     */
    public static void addAnnotations(Graph graph, Triple triple, OWLAxiom axiom) {
        if (!axiom.isAnnotated()) return;
        Node blank = NodeIRIUtils.toNode();
        graph.add(Triple.create(blank, RDF.type.asNode(), OWL2.Axiom.asNode()));
        graph.add(Triple.create(blank, OWL2.annotatedSource.asNode(), triple.getSubject()));
        graph.add(Triple.create(blank, OWL2.annotatedProperty.asNode(), triple.getPredicate()));
        graph.add(Triple.create(blank, OWL2.annotatedTarget.asNode(), triple.getObject()));
        addAnnotations(graph, blank, axiom);
    }

    /**
     * see <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Axioms_Represented_by_Blank_Nodes'>2.3.3 Axioms Represented by Blank Nodes </a>
     * for following axioms with more than two entities:
     * NegativeObjectPropertyAssertion,
     * NegativeDataPropertyAssertion,
     * DisjointClasses,
     * DisjointObjectProperties,
     * DisjointDataProperties,
     * DifferentIndividuals
     *
     * @param graph Graph
     * @param root  {@link org.apache.jena.graph.Node_Blank} anonymous node
     * @param axiom OWLAxiom
     */
    public static void addAnnotations(Graph graph, Node root, OWLAxiom axiom) {
        if (!axiom.isAnnotated()) return;
        axiom.annotations().forEach(a -> {
            graph.add(Triple.create(root, toNode(graph, a.getProperty()), NodeIRIUtils.toNode(a.getValue())));
        });
        axiom.annotations().forEach(a -> translate(graph, root, a));
    }

    public static void addAnnotations(Model model, Resource subject, Property predicate, RDFNode object, OWLAxiom axiom) {
        addAnnotations(model.getGraph(), Triple.create(subject.asNode(), predicate.asNode(), object.asNode()), axiom);
    }

    private static void translate(Graph graph, Node source, OWLAnnotation annotation) {
        if (annotation.annotations().count() == 0) return;
        Node blank = NodeIRIUtils.toNode();
        graph.add(Triple.create(blank, RDF.type.asNode(), OWL2.Annotation.asNode()));
        graph.add(Triple.create(blank, OWL2.annotatedSource.asNode(), source));
        graph.add(Triple.create(blank, OWL2.annotatedProperty.asNode(), NodeIRIUtils.toNode(annotation.getProperty())));
        graph.add(Triple.create(blank, OWL2.annotatedTarget.asNode(), NodeIRIUtils.toNode(annotation.getValue())));
        annotation.annotations().forEach(child -> {
            graph.add(Triple.create(blank, toNode(graph, child.getProperty()), NodeIRIUtils.toNode(child.getValue())));
        });
        annotation.annotations().filter(a -> a.annotations().count() != 0).forEach(a -> translate(graph, blank, a));
    }

    private static Node toNode(Graph graph, OWLAnnotationProperty property) {
        Node res = NodeIRIUtils.toNode(property);
        if (res.isURI() && !property.isBuiltIn()) {
            graph.add(Triple.create(res, RDF.type.asNode(), OWL.AnnotationProperty.asNode()));
        }
        return res;
    }
}
