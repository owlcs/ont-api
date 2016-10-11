package ru.avicomp.ontapi.parsers;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.NodeIRIUtils;
import ru.avicomp.ontapi.OntException;

/**
 * Helper for Axiom Annotations Parsing (operator 'TANN').
 * see <a href='https://www.w3.org/TR/owl2-mapping-to-rdf'>2.2 Translation of Annotations</a>
 * <p>
 * Created by szuev on 11.10.2016.
 */
public class AnnotationsParseUtils {

    /**
     * recursive operator TANN
     * see <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Axioms_that_Generate_a_Main_Triple'>2.3 Translation of Axioms with Annotations</a>
     * TODO: in case of OWLAnnotationProperty is not built-in we have to provide MultiUnion Graph to find this property in imports, if it exists. Otherwise we can create corresponding triplet.
     *
     * @param graph Graph
     * @param axiom OWLAxiom
     */
    public static void translate(Graph graph, OWLAxiom axiom) {
        if (!axiom.isAnnotated()) return;
        Node blank = NodeIRIUtils.toNode();
        graph.add(Triple.create(blank, RDF.type.asNode(), OWL2.Axiom.asNode()));
        Triple axiomTriple = toAnnotationTriple(axiom);
        graph.add(Triple.create(blank, OWL2.annotatedSource.asNode(), axiomTriple.getSubject()));
        graph.add(Triple.create(blank, OWL2.annotatedProperty.asNode(), axiomTriple.getPredicate()));
        graph.add(Triple.create(blank, OWL2.annotatedTarget.asNode(), axiomTriple.getObject()));
        axiom.annotations().forEach(a -> {
            graph.add(Triple.create(blank, NodeIRIUtils.toNode(a.getProperty()), NodeIRIUtils.toNode(a.getValue())));
        });
        axiom.annotations().forEach(a -> translate(graph, blank, a));
    }

    private static Triple toAnnotationTriple(OWLAxiom axiom) {
        Node annotationSource = null;
        Node annotationProperty = null;
        Node annotationTarget = null;
        if (AxiomType.DECLARATION.equals(axiom.getAxiomType())) {
            OWLDeclarationAxiom _axiom = (OWLDeclarationAxiom) axiom;
            OWLEntity source = _axiom.getEntity();
            annotationSource = NodeIRIUtils.toNode(_axiom.getEntity());
            if (source.isOWLClass()) {
                annotationTarget = OWL.Class.asNode();
            }
            annotationProperty = RDF.type.asNode();
        } else if (AxiomType.CLASS_ASSERTION.equals(axiom.getAxiomType())) {
            OWLClassAssertionAxiom _axiom = (OWLClassAssertionAxiom) axiom;
            OWLIndividual individual = _axiom.getIndividual();
            if (individual.isNamed()) {
                annotationSource = NodeIRIUtils.toNode(individual.asOWLNamedIndividual());
            }
            OWLClassExpression expression = _axiom.getClassExpression();
            if (expression.isOWLClass()) {
                annotationTarget = NodeIRIUtils.toNode(expression.asOWLClass());
            }
            annotationProperty = RDF.type.asNode();
        } else {
            //todo: not ready yet
        }
        return Triple.create(OntException.notNull(annotationSource, "Can't determine annotation source."),
                OntException.notNull(annotationProperty, "Can't determine annotation property."),
                OntException.notNull(annotationTarget, "Can't determine annotation target."));
    }

    private static void translate(Graph graph, Node source, OWLAnnotation annotation) {
        if (annotation.annotations().count() == 0) return;
        Node blank = NodeIRIUtils.toNode();
        graph.add(Triple.create(blank, RDF.type.asNode(), OWL2.Annotation.asNode()));
        graph.add(Triple.create(blank, OWL2.annotatedSource.asNode(), source));
        graph.add(Triple.create(blank, OWL2.annotatedProperty.asNode(), NodeIRIUtils.toNode(annotation.getProperty())));
        graph.add(Triple.create(blank, OWL2.annotatedTarget.asNode(), NodeIRIUtils.toNode(annotation.getValue())));
        annotation.annotations().forEach(child -> {
            graph.add(Triple.create(blank, NodeIRIUtils.toNode(child.getProperty()), NodeIRIUtils.toNode(child.getValue())));
        });
        annotation.annotations().filter(a -> a.annotations().count() != 0).forEach(a -> translate(graph, blank, a));
    }
}
