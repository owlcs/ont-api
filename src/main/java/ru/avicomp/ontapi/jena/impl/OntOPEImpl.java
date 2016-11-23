package ru.avicomp.ontapi.jena.impl;

import java.util.Set;
import java.util.stream.Stream;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;

import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.jena.JenaUtils;
import ru.avicomp.ontapi.jena.impl.configuration.OntFilter;
import ru.avicomp.ontapi.jena.impl.configuration.OntFinder;
import ru.avicomp.ontapi.jena.model.*;

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
            getModel().add(res, OWL2.inverseOf, this);
            return new InverseProperty(res.asNode(), getModel());
        }

        @Override
        public boolean isLocal() {
            return getModel().isInBaseModel(this, RDF.type, OWL2.ObjectProperty);
        }

        @Override
        public boolean isBuiltIn() {
            return OntEntityImpl.BUILT_IN_OBJECT_PROPERTIES.contains(this);
        }

        @Override
        public Class<OntNOP> getActualClass() {
            return OntNOP.class;
        }

        @Override
        public Property inModel(Model m) {
            return getModel() == m ? this : m.createProperty(getURI());
        }
    }

    public static class InverseProperty extends OntOPEImpl implements OntOPE.Inverse {

        public InverseProperty(Node n, EnhGraph g) {
            super(n, g);
        }

        static class Finder implements OntFinder {
            @Override
            public Stream<Node> find(EnhGraph eg) {
                return JenaUtils.asStream(eg.asGraph().find(Node.ANY, OWL2.inverseOf.asNode(), Node.ANY)
                        .filterKeep(t -> t.getSubject().isBlank() && isObjectPropertyNode(t.getObject(), eg))
                        .mapWith(Triple::getSubject));
            }
        }

        static class Filter implements OntFilter {
            @Override
            public boolean test(Node node, EnhGraph graph) {
                if (!node.isBlank()) return false;
                Set<Node> nodes = graph.asGraph().find(node, OWL2.inverseOf.asNode(), Node.ANY).mapWith(Triple::getObject).filterKeep(n -> isObjectPropertyNode(n, graph)).toSet();
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
    public OntStatement addSubPropertiesOf(Stream<OntOPE> chain) {
        OntApiException.notNull(chain, "Null properties chain");
        return addStatement(OWL2.propertyChainAxiom, getModel().createList(chain.iterator()));
    }

    @Override
    public void removeSubPropertiesOf() {
        clearAll(OWL2.propertyChainAxiom);
    }

    @Override
    public Stream<OntOPE> subPropertiesOf() {
        return rdfList(OWL2.propertyChainAxiom, OntOPE.class);
    }

    @Override
    public boolean isInverseFunctional() {
        return hasType(OWL2.InverseFunctionalProperty);
    }

    @Override
    public boolean isTransitive() {
        return hasType(OWL2.TransitiveProperty);
    }

    @Override
    public boolean isFunctional() {
        return hasType(OWL2.FunctionalProperty);
    }

    @Override
    public boolean isSymmetric() {
        return hasType(OWL2.SymmetricProperty);
    }

    @Override
    public boolean isAsymmetric() {
        return hasType(OWL2.AsymmetricProperty);
    }

    @Override
    public boolean isReflexive() {
        return hasType(OWL2.ReflexiveProperty);
    }

    @Override
    public boolean isIrreflexive() {
        return hasType(OWL2.IrreflexiveProperty);
    }

    @Override
    public void setFunctional(boolean functional) {
        changeType(OWL2.FunctionalProperty, functional);
    }

    @Override
    public void setInverseFunctional(boolean inverseFunctional) {
        changeType(OWL2.InverseFunctionalProperty, inverseFunctional);
    }

    @Override
    public void setAsymmetric(boolean asymmetric) {
        changeType(OWL2.AsymmetricProperty, asymmetric);
    }

    @Override
    public void setTransitive(boolean transitive) {
        changeType(OWL2.TransitiveProperty, transitive);
    }

    @Override
    public void setReflexive(boolean reflexive) {
        changeType(OWL2.ReflexiveProperty, reflexive);
    }

    @Override
    public void setIrreflexive(boolean irreflexive) {
        changeType(OWL2.IrreflexiveProperty, irreflexive);
    }

    @Override
    public void setSymmetric(boolean symmetric) {
        changeType(OWL2.SymmetricProperty, symmetric);
    }

    @Override
    public OntOPE getInverseOf() {
        return getOntProperty(OWL2.inverseOf, OntOPE.class);
    }
}

