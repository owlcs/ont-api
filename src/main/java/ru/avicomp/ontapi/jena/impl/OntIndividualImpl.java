package ru.avicomp.ontapi.jena.impl;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.FrontsNode;
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
import ru.avicomp.ontapi.jena.utils.Models;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

/**
 * both for anon and named individuals.
 * <p>
 * Created by szuev on 09.11.2016.
 */
public class OntIndividualImpl extends OntObjectImpl implements OntIndividual {

    public static OntObjectFactory anonymousIndividualFactory = new CommonOntObjectFactory(
            new OntMaker.Default(AnonymousImpl.class), new AnonymousImpl.Finder(), new AnonymousImpl.Filter());
    public static OntObjectFactory abstractIndividualFactory = new MultiOntObjectFactory(OntFinder.TYPED,
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

    public static class AnonymousImpl extends OntIndividualImpl implements OntIndividual.Anonymous {
        public AnonymousImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        public void detachClass(OntCE clazz) {
            if (classes().filter(c -> !clazz.equals(c)).count() == 0) {
                // otherwise this would no longer be an individual
                throw new OntJenaException("Can't detach last class " + clazz);
            }
            super.detachClass(clazz);
        }

        static class Finder implements OntFinder {
            @Override
            public Stream<Node> find(EnhGraph eg) {
                Stream<Node> declarations = Models.asStream(getDeclarations(Node.ANY, eg).mapWith(Triple::getSubject).filterKeep(Node::isBlank));
                Stream<Node> disjoint = disjointAnonIndividuals(eg);
                return Stream.concat(declarations, disjoint).distinct();
            }
        }

        static class Filter implements OntFilter {
            @Override
            public boolean test(Node node, EnhGraph graph) {
                return node.isBlank() &&
                        (!getDeclarations(node, graph).mapWith(Triple::getObject).toSet().isEmpty() || disjointAnonIndividuals(graph).anyMatch(node::equals));
            }
        }

        private static boolean isOntClass(Node node, EnhGraph eg) {
            return OntCEImpl.abstractCEFactory.canWrap(node, eg);
        }

        private static ExtendedIterator<Triple> getDeclarations(Node node, EnhGraph eg) {
            return eg.asGraph().find(node, RDF_TYPE, Node.ANY).
                    filterKeep(t -> isOntClass(t.getObject(), eg));
        }

        private static Stream<Node> disjointAnonIndividuals(EnhGraph eg) {
            Stream<Node> roots = Models.asStream(eg.asGraph().find(Node.ANY, RDF.type.asNode(), OWL.AllDifferent.asNode()).mapWith(Triple::getSubject).filterKeep(Node::isBlank));
            return roots.map(root -> Models.asStream(eg.asGraph().find(root, OWL.distinctMembers.asNode(), Node.ANY).mapWith(Triple::getObject)))
                    .flatMap(Function.identity())
                    .filter(node -> RDFListImpl.factory.canWrap(node, eg))
                    .map(node -> RDFListImpl.factory.wrap(node, eg))
                    .map(enhNode -> enhNode.as(RDFList.class))
                    .map(RDFList::asJavaList)
                    .map(Collection::stream)
                    .flatMap(Function.identity())
                    .map(FrontsNode::asNode)
                    .filter(Node::isBlank);
        }
    }
}
