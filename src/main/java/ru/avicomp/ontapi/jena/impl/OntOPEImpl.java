package ru.avicomp.ontapi.jena.impl;

import java.util.Set;
import java.util.stream.Stream;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.impl.configuration.OntFilter;
import ru.avicomp.ontapi.jena.impl.configuration.OntFinder;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.utils.BuiltIn;
import ru.avicomp.ontapi.jena.utils.Models;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

/**
 * owl:ObjectProperty (could be also Annotation, InverseFunctional, Transitive, SymmetricProperty, etc)
 * <p>
 * Created by szuev on 03.11.2016.
 */
public abstract class OntOPEImpl extends OntPEImpl implements OntOPE {

    public OntOPEImpl(Node n, EnhGraph g) {
        super(n, g);
    }

    public static class NamedProperty extends OntOPEImpl implements OntNOP {

        public NamedProperty(Node n, EnhGraph g) {
            super(OntObjectImpl.checkNamed(n), g);
        }

        @Override
        public Inverse createInverse() {
            Resource res = getModel().createResource();
            getModel().add(res, OWL.inverseOf, this);
            return new InverseProperty(res.asNode(), getModel());
        }

        @Override
        public boolean isBuiltIn() {
            return BuiltIn.OBJECT_PROPERTIES.contains(this);
        }

        @Override
        public Class<OntNOP> getActualClass() {
            return OntNOP.class;
        }

        @Override
        public Property inModel(Model m) {
            return getModel() == m ? this : m.createProperty(getURI());
        }

        @Override
        public OntStatement getRoot() {
            return getRoot(RDF.type, OWL.ObjectProperty);
        }
    }

    public static class InverseProperty extends OntOPEImpl implements OntOPE.Inverse {

        public InverseProperty(Node n, EnhGraph g) {
            super(n, g);
        }

        @Override
        public OntStatement getRoot() {
            return new OntStatementImpl.RootImpl(this, OWL.inverseOf, getDirect(), getModel());
        }

        @Override
        public OntOPE getDirect() {
            return getRequiredOntProperty(OWL.inverseOf, OntOPE.class);
        }

        static class Finder implements OntFinder {
            @Override
            public Stream<Node> find(EnhGraph eg) {
                return Models.asStream(eg.asGraph().find(Node.ANY, OWL.inverseOf.asNode(), Node.ANY)
                        .filterKeep(t -> t.getSubject().isBlank() && isObjectPropertyNode(t.getObject(), eg))
                        .mapWith(Triple::getSubject));
            }
        }

        static class Filter implements OntFilter {
            @Override
            public boolean test(Node node, EnhGraph graph) {
                if (!node.isBlank()) return false;
                Set<Node> nodes = graph.asGraph().find(node, OWL.inverseOf.asNode(), Node.ANY).mapWith(Triple::getObject).filterKeep(n -> isObjectPropertyNode(n, graph)).toSet();
                return !nodes.isEmpty();
            }
        }

        private static boolean isObjectPropertyNode(Node node, EnhGraph eg) {
            return OntEntityImpl.objectPropertyFactory.canWrap(node, eg);
        }
    }

    @Override
    public OntNPA.ObjectAssertion addNegativeAssertion(OntIndividual source, OntIndividual target) {
        return OntNPAImpl.create(getModel(), source, this, target);
    }

    @Override
    public OntStatement addSuperPropertyOf(Stream<OntOPE> chain) {
        OntJenaException.notNull(chain, "Null properties chain");
        return addStatement(OWL.propertyChainAxiom, getModel().createList(chain.iterator()));
    }

    @Override
    public void removeSuperPropertyOf() {
        clearAll(OWL.propertyChainAxiom);
    }

    @Override
    public Stream<OntOPE> superPropertyOf() {
        return rdfList(OWL.propertyChainAxiom, OntOPE.class);
    }

    @Override
    public void setFunctional(boolean functional) {
        changeType(OWL.FunctionalProperty, functional);
    }

    @Override
    public void setInverseFunctional(boolean inverseFunctional) {
        changeType(OWL.InverseFunctionalProperty, inverseFunctional);
    }

    @Override
    public void setAsymmetric(boolean asymmetric) {
        changeType(OWL.AsymmetricProperty, asymmetric);
    }

    @Override
    public void setTransitive(boolean transitive) {
        changeType(OWL.TransitiveProperty, transitive);
    }

    @Override
    public void setReflexive(boolean reflexive) {
        changeType(OWL.ReflexiveProperty, reflexive);
    }

    @Override
    public void setIrreflexive(boolean irreflexive) {
        changeType(OWL.IrreflexiveProperty, irreflexive);
    }

    @Override
    public void setSymmetric(boolean symmetric) {
        changeType(OWL.SymmetricProperty, symmetric);
    }

    @Override
    public OntOPE getInverseOf() {
        return getOntProperty(OWL.inverseOf, OntOPE.class);
    }
}

