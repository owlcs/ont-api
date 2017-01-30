package ru.avicomp.ontapi.jena.impl;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.enhanced.EnhNode;
import org.apache.jena.graph.FrontsNode;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.impl.RDFListImpl;
import org.apache.jena.util.iterator.ExtendedIterator;

import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.impl.configuration.*;
import ru.avicomp.ontapi.jena.model.OntCE;
import ru.avicomp.ontapi.jena.model.OntIndividual;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.utils.Streams;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

/**
 * both for anon and named individuals.
 * <p>
 * Created by szuev on 09.11.2016.
 */
public class OntIndividualImpl extends OntObjectImpl implements OntIndividual {

    public static Configurable<OntObjectFactory> anonymousIndividualFactory = mode -> new CommonOntObjectFactory(
            new OntMaker.Default(AnonymousImpl.class), OntFinder.ANY_SUBJECT_AND_OBJECT, AnonymousImpl.FILTER.get(mode));

    public static Configurable<MultiOntObjectFactory> abstractIndividualFactory = createMultiFactory(OntFinder.ANY_SUBJECT_AND_OBJECT,
            OntEntityImpl.individualFactory, anonymousIndividualFactory);


    public OntIndividualImpl(Node n, EnhGraph m) {
        super(n, m);
    }

    @Override
    public OntStatement attachClass(OntCE clazz) {
        return addType(clazz);
    }

    @Override
    public void detachClass(OntCE clazz) {
        removeType(clazz);
    }

    public static class NamedImpl extends OntIndividualImpl implements OntIndividual.Named {
        public NamedImpl(Node n, EnhGraph m) {
            super(OntObjectImpl.checkNamed(n), m);
        }

        @Override
        public boolean isBuiltIn() {
            return false;
        }

        @Override
        public OntStatement getRoot() {
            return getRoot(RDF.type, OWL.NamedIndividual);
        }
    }

    /**
     * see description to the interface {@link OntIndividual.Anonymous}
     * It seems that checking for conditions 7, 8, 9 could be displaced by checking that tested b-node is
     * an object in a triple from annotation or object property assertion (triple where predicate is object or annotation property).
     * About this there are following reflections:
     * - in the well-formed ontology anonymous subject should be declared as individual (condition 1),
     * otherwise it is just any other b-node (e.g. root for owl:Axiom).
     * - the bulk annotations consist of annotation assertions.
     */
    public static class AnonymousImpl extends OntIndividualImpl implements OntIndividual.Anonymous {

        public static final Configurable<OntFilter> FILTER = mode -> (node, graph) -> node.isBlank() &&
                (!getDeclarations(node, graph, mode).mapWith(Triple::getObject).toSet().isEmpty() ||
                        positiveAssertionAnonIndividuals(graph, mode).anyMatch(node::equals) ||
                        negativeAssertionAnonIndividuals(graph).anyMatch(node::equals) ||
                        hasValueOPEAnonIndividuals(graph).anyMatch(node::equals) ||
                        sameAnonIndividuals(graph).anyMatch(node::equals) ||
                        differentAnonIndividuals(graph).anyMatch(node::equals) ||
                        oneOfAnonIndividuals(graph).anyMatch(node::equals) ||
                        disjointAnonIndividuals(graph).anyMatch(node::equals));

        public AnonymousImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        public void detachClass(OntCE clazz) {
            if (classes().filter(c -> !clazz.equals(c)).count() == 0) {
                // otherwise this the anonymous individual could be lost. use another way to last remove class-assertion.
                throw new OntJenaException("Can't detach class " + clazz + ": it is a single for individual " + this);
            }
            super.detachClass(clazz);
        }

        private static ExtendedIterator<Triple> getDeclarations(Node node, EnhGraph eg, Configurable.Mode mode) {
            return eg.asGraph().find(node, RDF.type.asNode(), Node.ANY).
                    filterKeep(t -> OntCEImpl.abstractCEFactory.get(mode).canWrap(t.getObject(), eg));
        }

        private static Stream<Node> negativeAssertionAnonIndividuals(EnhGraph eg) {
            return Stream.of(OWL.sourceIndividual, OWL.targetIndividual)
                    .map(FrontsNode::asNode)
                    .map(predicate -> Streams.asStream(eg.asGraph().find(Node.ANY, predicate, Node.ANY)).map(Triple::getObject))
                    .flatMap(Function.identity()).filter(Node::isBlank);
            // it seems we don't need full validation:
            /*return Models.asStream(eg.asGraph().find(Node.ANY, RDF.type.asNode(), OWL.NegativePropertyAssertion.asNode()))
                    .map(Triple::getSubject).map(subject ->
                            Stream.of(OWL.sourceIndividual, OWL.targetIndividual)
                                    .map(FrontsNode::asNode)
                                    .map(predicate -> Models.asStream(eg.asGraph().find(subject, predicate, Node.ANY)).map(Triple::getObject))
                                    .flatMap(Function.identity()))
                    .flatMap(Function.identity())
                    .filter(Node::isBlank);*/
        }

        private static Stream<Node> hasValueOPEAnonIndividuals(EnhGraph eg) {
            return Streams.asStream(eg.asGraph().find(Node.ANY, OWL.hasValue.asNode(), Node.ANY)).map(Triple::getObject).filter(Node::isBlank);
        }

        private static Stream<Node> sameAnonIndividuals(EnhGraph eg) {
            return anonsForPredicate(eg.asGraph(), OWL.sameAs.asNode());
        }

        private static Stream<Node> differentAnonIndividuals(EnhGraph eg) {
            return anonsForPredicate(eg.asGraph(), OWL.differentFrom.asNode());
        }

        private static Stream<Node> anonsForPredicate(Graph graph, Node predicate) {
            return Streams.asStream(graph.find(Node.ANY, predicate, Node.ANY))
                    .map(triple -> Stream.of(triple.getSubject(), triple.getObject()))
                    .flatMap(Function.identity()).filter(Node::isBlank);
        }

        /**
         * returns stream of blank nodes ("_:a"), where blank node is an object in a triple
         * which corresponds object property assertion "_:a1 PN _:a2" or annotation property assertion "s A t"
         *
         * @param eg     {@link OntGraphModelImpl}
         * @param m       exclude illegal punnings if {@link Configurable.Mode#STRICT}
         * @return Stream of {@link Node}
         */
        private static Stream<Node> positiveAssertionAnonIndividuals(EnhGraph eg, Configurable.Mode m) {
            return positiveAssertionProperties(eg, m)
                    .map(EnhNode::asNode)
                    .map(node -> anonAssertionObjects(eg.asGraph(), node))
                    .flatMap(Function.identity());
        }

        private static Stream<EnhNode> positiveAssertionProperties(EnhGraph eg, Configurable.Mode mode) {
            return Stream.of(OntEntityImpl.annotationPropertyFactory.get(mode).find(eg), OntEntityImpl.objectPropertyFactory.get(mode).find(eg))
                            .flatMap(Function.identity());

        }

        private static Stream<Node> anonAssertionObjects(Graph graph, Node predicate) {
            return Streams.asStream(graph.find(Node.ANY, predicate, Node.ANY))
                    .map(Triple::getObject)
                    .filter(Node::isBlank);
            //.filter(node -> !graph.contains(node, Node.ANY, Node.ANY));
        }

        private static Stream<Node> disjointAnonIndividuals(EnhGraph eg) {
            return blankNodesFromList(eg, OWL.AllDifferent.asNode(), OWL.distinctMembers.asNode(), OWL.members.asNode());
        }

        private static Stream<Node> oneOfAnonIndividuals(EnhGraph eg) {
            return blankNodesFromList(eg, OWL.Class.asNode(), OWL.oneOf.asNode());
        }

        private static Stream<Node> blankNodesFromList(EnhGraph eg, Node type, Node... predicates) {
            Stream<Node> roots = Streams.asStream(eg.asGraph().find(Node.ANY, RDF.type.asNode(), type))
                    .map(Triple::getSubject)
                    .filter(Node::isBlank);
            return objects(eg.asGraph(), roots, predicates)
                    .filter(node -> RDFListImpl.factory.canWrap(node, eg))
                    .map(node -> RDFListImpl.factory.wrap(node, eg))
                    .map(enhNode -> enhNode.as(RDFList.class))
                    .map(RDFList::asJavaList)
                    .map(Collection::stream)
                    .flatMap(Function.identity())
                    .map(FrontsNode::asNode)
                    .filter(Node::isBlank);

        }

        private static Stream<Node> objects(Graph graph, Node subject, Node predicate) {
            return Streams.asStream(graph.find(subject, predicate, Node.ANY).mapWith(Triple::getObject));
        }

        private static Stream<Node> objects(Graph graph, Node subject, Node... predicates) {
            return Stream.of(predicates).map(p -> objects(graph, subject, p)).flatMap(Function.identity());
        }

        private static Stream<Node> objects(Graph graph, Stream<Node> subjects, Node... predicates) {
            return subjects.map(r -> objects(graph, r, predicates)).flatMap(Function.identity());
        }
    }
}
