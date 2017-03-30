package ru.avicomp.ontapi.jena.impl;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.*;

import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.impl.configuration.Configurable;
import ru.avicomp.ontapi.jena.impl.configuration.OntFilter;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.utils.BuiltIn;
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

    public static class NamedPropertyImpl extends OntOPEImpl implements OntNOP {

        public NamedPropertyImpl(Node n, EnhGraph g) {
            super(n, g);
            checkNamedProperty(this);
        }

        @Override
        public Inverse createInverse() {
            Resource res = getModel().createResource();
            getModel().add(res, OWL.inverseOf, this);
            return new InversePropertyImpl(res.asNode(), getModel());
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

    public static class InversePropertyImpl extends OntOPEImpl implements OntOPE.Inverse {

        public static final Configurable<OntFilter> FILTER = mode -> (n, g) -> {
            if (!n.isBlank()) return false;
            Set<Node> nodes = g.asGraph().find(n, OWL.inverseOf.asNode(), Node.ANY)
                    .mapWith(Triple::getObject)
                    .filterKeep(o -> Entities.OBJECT_PROPERTY.get(mode).canWrap(o, g))
                    .toSet();
            return !nodes.isEmpty();
        };

        public InversePropertyImpl(Node n, EnhGraph g) {
            super(n, g);
        }

        @Override
        public OntStatement getRoot() {
            return new OntStatementImpl.RootImpl(this, OWL.inverseOf, getRequiredDirectProperty(), getModel());
        }

        protected Resource getRequiredDirectProperty() {
            return getModel().statements(this, OWL.inverseOf, null).findFirst()
                    .map(Statement::getObject).map(RDFNode::asResource)
                    .orElseThrow(OntJenaException.supplier("Can't find owl:inverseOf object prop"));
        }

        @Override
        public OntOPE getDirect() {
            Resource res = getRequiredDirectProperty();
            return res.as(OntOPE.class);
        }
    }

    @Override
    public OntNPA.ObjectAssertion addNegativeAssertion(OntIndividual source, OntIndividual target) {
        return OntNPAImpl.create(getModel(), source, this, target);
    }

    @Override
    public OntStatement addSuperPropertyOf(Collection<OntOPE> chain) {
        OntJenaException.notNull(chain, "Null properties chain");
        return addStatement(OWL.propertyChainAxiom, getModel().createList(chain.iterator()));
    }

    @Override
    public void removeSuperPropertyOf() {
        clearAll(OWL.propertyChainAxiom);
    }

    @Override
    public Stream<OntOPE> superPropertyOf() {
        return getRequiredProperty(OWL.propertyChainAxiom).getObject().as(RDFList.class).asJavaList().stream().map(r -> r.as(OntOPE.class));
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
        return getObject(OWL.inverseOf, OntOPE.class);
    }
}

