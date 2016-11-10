package ru.avicomp.ontapi.jena.impl;

import java.util.Set;
import java.util.stream.Stream;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import ru.avicomp.ontapi.OntException;
import ru.avicomp.ontapi.jena.JenaUtils;
import ru.avicomp.ontapi.jena.impl.configuration.OntFilter;
import ru.avicomp.ontapi.jena.impl.configuration.OntFinder;
import ru.avicomp.ontapi.jena.model.OntCE;
import ru.avicomp.ontapi.jena.model.OntOPE;
import ru.avicomp.ontapi.jena.model.OntOProperty;

/**
 * owl:ObjectProperty (could be also Annotation, InverseFunctional, Transitive, SymmetricProperty, etc)
 * <p>
 * Created by szuev on 03.11.2016.
 */
public abstract class OntOPEImpl extends OntPEImpl implements OntOPE {

    public OntOPEImpl(Node n, EnhGraph g) {
        super(n, g);
    }

    public static class NamedProperty extends OntOPEImpl implements OntOProperty {

        public NamedProperty(Node n, EnhGraph g) {
            super(OntEntityImpl.checkNamed(n), g);
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
            //TODO:
            return false;
        }

        @Override
        public Class<OntOProperty> getActualClass() {
            return OntOProperty.class;
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
    public Stream<OntCE> domain() {
        return getModel().classExpressions(this, RDFS.domain);
    }

    @Override
    public Stream<OntCE> range() {
        return getModel().classExpressions(this, RDFS.range);
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
    public void addInverseOf(OntOPE other) {
        getModel().add(this, OWL2.inverseOf, OntException.notNull(other, "Null object property expression."));
    }

    @Override
    public void removeInverseOf(OntOPE other) {
        getModel().remove(this, OWL2.inverseOf, OntException.notNull(other, "Null object property expression."));
    }

    @Override
    public OntOPE getInverseOf() {
        return getOntProperty(OWL2.inverseOf, OntOPE.class);
    }
}

