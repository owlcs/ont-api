package ru.avicomp.ontapi.jena.impl;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.FrontsNode;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

import ru.avicomp.ontapi.jena.impl.configuration.CommonOntObjectFactory;
import ru.avicomp.ontapi.jena.impl.configuration.Configurable;
import ru.avicomp.ontapi.jena.impl.configuration.OntMaker;
import ru.avicomp.ontapi.jena.impl.configuration.OntObjectFactory;
import ru.avicomp.ontapi.jena.model.OntAnnotation;
import ru.avicomp.ontapi.jena.model.OntNAP;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

/**
 * The implementation of Annotation OntObject.
 * <p>
 * Created by @szuev on 26.03.2017.
 */
public class OntAnnotationImpl extends OntObjectImpl implements OntAnnotation {
    public static final Set<Property> SPEC =
            Stream.of(RDF.type, OWL.annotatedSource, OWL.annotatedProperty, OWL.annotatedTarget)
                    .collect(Collectors.toSet());
    public static final Set<Resource> EXTRA_ROOT_TYPES =
            Stream.of(OWL.AllDisjointClasses, OWL.AllDisjointProperties, OWL.AllDifferent, OWL.NegativePropertyAssertion)
                    .collect(Collectors.toSet());
    public static final Set<Node> EXTRA_ROOT_TYPES_AS_NODES = EXTRA_ROOT_TYPES.stream()
            .map(FrontsNode::asNode)
            .collect(Collectors.toSet());
    public static Configurable<OntObjectFactory> annotationFactory = m -> new CommonOntObjectFactory(
            new OntMaker.Default(OntAnnotationImpl.class),
            OntAnnotationImpl::findRootAnnotations,
            OntAnnotationImpl::testAnnotation);

    public OntAnnotationImpl(Node n, EnhGraph m) {
        super(n, m);
    }

    @Override
    public Stream<OntStatement> content() {
        return SPEC.stream().map(this::getRequiredProperty);
    }

    @Override
    public Stream<OntStatement> assertions() {
        return Iter.asStream(listProperties())
                .filter(st -> !SPEC.contains(st.getPredicate()))
                .filter(st -> st.getPredicate().canAs(OntNAP.class))
                .map(st -> new OntStatementImpl(this, st.getPredicate().as(OntNAP.class), st.getObject(), getModel()))
                .map(OntStatement.class::cast);
    }

    public static Stream<Node> findRootAnnotations(EnhGraph eg) {
        return Stream.concat(Stream.of(OWL.Axiom.asNode()), EXTRA_ROOT_TYPES_AS_NODES.stream())
                .map(t -> eg.asGraph().find(Node.ANY, RDF.type.asNode(), t))
                .map(Iter::asStream)
                .flatMap(Function.identity())
                .map(Triple::getSubject);
    }

    public static boolean testAnnotation(Node node, EnhGraph graph) {
        if (!node.isBlank()) return false;
        Set<Node> types = graph.asGraph().find(node, RDF.type.asNode(), Node.ANY).mapWith(Triple::getObject).toSet();
        if ((types.contains(OWL.Axiom.asNode()) || types.contains(OWL.Annotation.asNode())) &&
                Stream.of(OWL.annotatedSource, OWL.annotatedProperty, OWL.annotatedTarget)
                        .map(FrontsNode::asNode)
                        .allMatch(p -> graph.asGraph().contains(node, p, Node.ANY))) {
            return true;
        }
        // special cases: owl:AllDisjointClasses, owl:AllDisjointProperties, owl:AllDifferent or owl:NegativePropertyAssertion
        return EXTRA_ROOT_TYPES_AS_NODES.stream().anyMatch(types::contains);
    }
}
