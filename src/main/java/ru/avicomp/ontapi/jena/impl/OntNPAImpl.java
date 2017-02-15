package ru.avicomp.ontapi.jena.impl;

import java.util.Optional;
import java.util.stream.Stream;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import ru.avicomp.ontapi.jena.impl.configuration.*;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

/**
 * Negative Property Assertion Implementation.
 * <p>
 * Created by @szuev on 15.11.2016.
 */
public abstract class OntNPAImpl<P extends OntPE, T extends RDFNode> extends OntObjectImpl implements OntNPA<P, T> {
    private static OntFinder NPA_FINDER = new OntFinder.ByType(OWL.NegativePropertyAssertion);
    private static OntFilter NPA_FILTER = OntFilter.BLANK
            .and(new OntFilter.HasPredicate(OWL.sourceIndividual))
            .and(new OntFilter.HasPredicate(OWL.assertionProperty));

    public static Configurable<OntObjectFactory> objectNPAFactory = m ->
            new CommonOntObjectFactory(new OntMaker.Default(ObjectAssertionImpl.class), NPA_FINDER, NPA_FILTER, new OntFilter.HasPredicate(OWL.targetIndividual));
    public static Configurable<OntObjectFactory> dataNPAFactory = m ->
            new CommonOntObjectFactory(new OntMaker.Default(DataAssertionImpl.class), NPA_FINDER, NPA_FILTER, new OntFilter.HasPredicate(OWL.targetValue));
    public static Configurable<MultiOntObjectFactory> abstractNPAFactory = createMultiFactory(NPA_FINDER, objectNPAFactory, dataNPAFactory);

    public OntNPAImpl(Node n, EnhGraph m) {
        super(n, m);
    }

    public static DataAssertion create(OntGraphModelImpl model, OntIndividual source, OntNDP property, Literal target) {
        Resource res = create(model, source);
        res.addProperty(OWL.assertionProperty, property);
        res.addProperty(OWL.targetValue, target);
        return model.getNodeAs(res.asNode(), DataAssertion.class);
    }

    public static ObjectAssertion create(OntGraphModelImpl model, OntIndividual source, OntOPE property, OntIndividual target) {
        Resource res = create(model, source);
        res.addProperty(OWL.assertionProperty, property);
        res.addProperty(OWL.targetIndividual, target);
        return model.getNodeAs(res.asNode(), ObjectAssertion.class);
    }

    abstract Class<P> propertyClass();

    abstract Property targetPredicate();

    @Override
    public OntIndividual getSource() {
        return getRequiredObject(OWL.sourceIndividual, OntIndividual.class);
    }

    @Override
    public P getProperty() {
        return getRequiredObject(OWL.assertionProperty, propertyClass());
    }

    private static Resource create(OntGraphModel model, OntIndividual source) {
        Resource res = model.createResource();
        res.addProperty(RDF.type, OWL.NegativePropertyAssertion);
        res.addProperty(OWL.sourceIndividual, source);
        return res;
    }

    @Override
    public Stream<OntStatement> content() {
        return Stream.of(statement(RDF.type, OWL.NegativePropertyAssertion),
                statement(OWL.sourceIndividual),
                statement(OWL.annotatedProperty),
                statement(targetPredicate())).filter(Optional::isPresent).map(Optional::get);
    }

    public static class ObjectAssertionImpl extends OntNPAImpl<OntOPE, OntIndividual> implements ObjectAssertion {
        public ObjectAssertionImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        Class<OntOPE> propertyClass() {
            return OntOPE.class;
        }

        @Override
        Property targetPredicate() {
            return OWL.targetIndividual;
        }

        @Override
        public Class<ObjectAssertion> getActualClass() {
            return ObjectAssertion.class;
        }


        @Override
        public OntIndividual getTarget() {
            return getRequiredObject(targetPredicate(), OntIndividual.class);
        }

    }

    public static class DataAssertionImpl extends OntNPAImpl<OntNDP, Literal> implements DataAssertion {
        public DataAssertionImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        Class<OntNDP> propertyClass() {
            return OntNDP.class;
        }

        @Override
        Property targetPredicate() {
            return OWL.targetValue;
        }

        @Override
        public Class<DataAssertion> getActualClass() {
            return DataAssertion.class;
        }


        @Override
        public Literal getTarget() {
            return getRequiredObject(targetPredicate(), Literal.class);
        }

    }
}
