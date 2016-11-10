package ru.avicomp.ontapi.jena.impl;

import java.util.Set;
import java.util.stream.Stream;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;

import ru.avicomp.ontapi.OntException;
import ru.avicomp.ontapi.jena.JenaUtils;
import ru.avicomp.ontapi.jena.impl.configuration.*;
import ru.avicomp.ontapi.jena.model.OntClass;
import ru.avicomp.ontapi.jena.model.OntIndividual;
import ru.avicomp.ontapi.jena.model.OntNIndividual;

/**
 * both for anon and named individuals.
 * <p>
 * Created by szuev on 09.11.2016.
 */
public class OntIndividualImpl extends OntObjectImpl implements OntIndividual {

    public static OntObjectFactory anonymousIndividualFactory = new CommonOntObjectFactory(
            new OntMaker.Default(OntIndividualImpl.AnonymousIndividual.class), new AnonymousIndividual.Finder(), new AnonymousIndividual.Filter());
    public static OntObjectFactory abstractIndividualFactory = new MultiOntObjectFactory(OntFinder.TYPED,
            OntEntityImpl.individualFactory, anonymousIndividualFactory);

    public OntIndividualImpl(Node n, EnhGraph m) {
        super(n, m);
    }

    @Override
    public void attachClass(OntClass clazz) {
        addType(clazz);
    }

    @Override
    public void detachClass(OntClass clazz) {
        removeType(clazz);
    }

    @Override
    public Stream<OntClass> classes() {
        return JenaUtils.asStream(getModel().listStatements(this, RDF.type, (RDFNode) null).mapWith(Statement::getObject).
                filterKeep(r -> r.canAs(OntClass.class)).
                mapWith(r -> getModel().getNodeAs(r.asNode(), OntClass.class)));
    }

    public static class NamedIndividual extends OntIndividualImpl implements OntNIndividual {
        public NamedIndividual(Node n, EnhGraph m) {
            super(OntEntityImpl.checkNamed(n), m);
        }

        @Override
        public boolean isLocal() {
            return getModel().isInBaseModel(this, RDF.type, OWL2.NamedIndividual);
        }

        @Override
        public boolean isBuiltIn() {
            return false;
        }
    }

    public static class AnonymousIndividual extends OntIndividualImpl {
        public AnonymousIndividual(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        public void detachClass(OntClass clazz) {
            if (classes().filter(c -> !clazz.equals(c)).count() == 0) {
                // otherwise this would no longer be an individual
                throw new OntException("Can't detach last class " + clazz);
            }
            super.detachClass(clazz);
        }

        static class Finder implements OntFinder {
            @Override
            public Stream<Node> find(EnhGraph eg) {
                return JenaUtils.asStream(eg.asGraph().find(Node.ANY, RDF_TYPE, Node.ANY).
                        filterKeep(t -> t.getSubject().isBlank()).
                        filterKeep(t -> isOntClass(t.getObject(), eg)).mapWith(Triple::getSubject));
            }
        }

        static class Filter implements OntFilter {
            @Override
            public boolean test(Node node, EnhGraph graph) {
                if (!node.isBlank()) return false;
                Set<Node> nodes = graph.asGraph().find(node, RDF_TYPE, Node.ANY).mapWith(Triple::getObject).filterKeep(n -> isOntClass(n, graph)).toSet();
                return !nodes.isEmpty();
            }
        }

        private static boolean isOntClass(Node node, EnhGraph eg) {
            return OntEntityImpl.classFactory.canWrap(node, eg);
        }
    }
}
