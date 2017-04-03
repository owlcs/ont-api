package ru.avicomp.ontapi.jena.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDFS;

import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.impl.configuration.*;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.utils.Models;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

/**
 * base class for any class-expression.
 * <p>
 * Created by szuev on 03.11.2016.
 */
public abstract class OntCEImpl extends OntObjectImpl implements OntCE {

    public static final OntFinder CLASS_FINDER = new OntFinder.ByType(OWL.Class);
    public static final OntFinder RESTRICTION_FINDER = new OntFinder.ByType(OWL.Restriction);
    public static final OntFilter RESTRICTION_FILTER = OntFilter.BLANK.and(new OntFilter.HasType(OWL.Restriction));

    public static Configurable<OntObjectFactory> unionOfCEFactory = createCEFactory(UnionOfImpl.class, OWL.unionOf);
    public static Configurable<OntObjectFactory> intersectionOfCEFactory = createCEFactory(IntersectionOfImpl.class, OWL.intersectionOf);
    public static Configurable<OntObjectFactory> oneOfCEFactory = createCEFactory(OneOfImpl.class, OWL.oneOf);
    public static Configurable<OntObjectFactory> complementOfCEFactory = createCEFactory(ComplementOfImpl.class, OWL.complementOf);

    public static Configurable<OntObjectFactory> objectSomeValuesOfCEFactory = createRestrictionFactory(ObjectSomeValuesFromImpl.class,
            RestrictionType.OBJECT, OWL.someValuesFrom);
    public static Configurable<OntObjectFactory> dataSomeValuesOfCEFactory = createRestrictionFactory(DataSomeValuesFromImpl.class,
            RestrictionType.DATA, OWL.someValuesFrom);

    public static Configurable<OntObjectFactory> objectAllValuesOfCEFactory = createRestrictionFactory(ObjectAllValuesFromImpl.class,
            RestrictionType.OBJECT, OWL.allValuesFrom);
    public static Configurable<OntObjectFactory> dataAllValuesOfCEFactory = createRestrictionFactory(DataAllValuesFromImpl.class,
            RestrictionType.DATA, OWL.allValuesFrom);

    public static Configurable<OntObjectFactory> objectHasValueCEFactory = createRestrictionFactory(ObjectHasValueImpl.class,
            RestrictionType.OBJECT, OWL.hasValue);
    public static Configurable<OntObjectFactory> dataHasValueCEFactory = createRestrictionFactory(DataHasValueImpl.class,
            RestrictionType.DATA, OWL.hasValue);

    public static Configurable<OntObjectFactory> dataMinCardinalityCEFactory = createRestrictionFactory(DataMinCardinalityImpl.class,
            RestrictionType.DATA, CardinalityType.MIN);
    public static Configurable<OntObjectFactory> objectMinCardinalityCEFactory = createRestrictionFactory(ObjectMinCardinalityImpl.class,
            RestrictionType.OBJECT, CardinalityType.MIN);

    public static Configurable<OntObjectFactory> dataMaxCardinalityCEFactory = createRestrictionFactory(DataMaxCardinalityImpl.class,
            RestrictionType.DATA, CardinalityType.MAX);
    public static Configurable<OntObjectFactory> objectMaxCardinalityCEFactory = createRestrictionFactory(ObjectMaxCardinalityImpl.class,
            RestrictionType.OBJECT, CardinalityType.MAX);

    public static Configurable<OntObjectFactory> dataCardinalityCEFactory = createRestrictionFactory(DataCardinalityImpl.class,
            RestrictionType.DATA, CardinalityType.EXACTLY);
    public static Configurable<OntObjectFactory> objectCardinalityCEFactory = createRestrictionFactory(ObjectCardinalityImpl.class,
            RestrictionType.OBJECT, CardinalityType.EXACTLY);

    public static Configurable<OntObjectFactory> hasSelfCEFactory = mode -> new CommonOntObjectFactory(new HasSelfMaker(),
            RESTRICTION_FINDER, OntFilter.BLANK.and(new HasSelfFilter()));

    //see <a href='https://www.w3.org/TR/owl2-quick-reference/#Class_Expressions'>Restrictions Using n-ary Data Range</a>
    public static Configurable<OntObjectFactory> naryDataAllValuesFromCEFactory = createNaryFactory(NaryDataAllValuesFromImpl.class, OWL.allValuesFrom);
    public static Configurable<OntObjectFactory> naryDataSomeValuesFromCEFactory = createNaryFactory(NaryDataSomeValuesFromImpl.class, OWL.someValuesFrom);

    public static Configurable<MultiOntObjectFactory> abstractNaryRestrictionCEFactory = createMultiFactory(RESTRICTION_FINDER, naryDataAllValuesFromCEFactory, naryDataSomeValuesFromCEFactory);

    //Boolean Connectives and Enumeration of Individuals
    public static Configurable<MultiOntObjectFactory> abstractComponentsCEFactory = createMultiFactory(CLASS_FINDER,
            unionOfCEFactory, intersectionOfCEFactory, oneOfCEFactory);
    public static Configurable<MultiOntObjectFactory> abstractNoneRestrictionCEFactory = createMultiFactory(CLASS_FINDER,
            abstractComponentsCEFactory, complementOfCEFactory);

    public static Configurable<MultiOntObjectFactory> abstractCardinalityRestrictionCEFactory = createMultiFactory(RESTRICTION_FINDER,
            objectMinCardinalityCEFactory, dataMinCardinalityCEFactory,
            objectMaxCardinalityCEFactory, dataMaxCardinalityCEFactory,
            objectCardinalityCEFactory, dataCardinalityCEFactory);
    public static Configurable<MultiOntObjectFactory> abstractNoneCardinalityRestrictionCEFactory = createMultiFactory(RESTRICTION_FINDER,
            objectSomeValuesOfCEFactory, dataSomeValuesOfCEFactory,
            objectAllValuesOfCEFactory, dataAllValuesOfCEFactory,
            objectHasValueCEFactory, dataHasValueCEFactory,
            abstractNaryRestrictionCEFactory);

    public static Configurable<MultiOntObjectFactory> abstractComponentRestrictionCEFactory =
            createMultiFactory(RESTRICTION_FINDER, abstractCardinalityRestrictionCEFactory, abstractNoneCardinalityRestrictionCEFactory);

    public static Configurable<MultiOntObjectFactory> abstractRestrictionCEFactory =
            createMultiFactory(RESTRICTION_FINDER, abstractComponentRestrictionCEFactory, hasSelfCEFactory);

    public static Configurable<MultiOntObjectFactory> abstractAnonymousCEFactory =
            createMultiFactory(OntFinder.TYPED, abstractNoneRestrictionCEFactory, abstractRestrictionCEFactory);

    public static Configurable<MultiOntObjectFactory> abstractCEFactory =
            createMultiFactory(OntFinder.TYPED, Entities.CLASS, abstractAnonymousCEFactory);

    public OntCEImpl(Node n, EnhGraph m) {
        super(n, m);
    }

    @Override
    public OntIndividual.Anonymous createIndividual() {
        return createAnonymousIndividual(getModel(), this);
    }

    @Override
    public OntIndividual.Named createIndividual(String uri) {
        return createNamedIndividual(getModel(), this, uri);
    }

    @Override
    public OntStatement addHasKey(Collection<OntOPE> objectProperties, Collection<OntNDP> dataProperties) {
        return addHasKey(this, objectProperties, dataProperties);
    }

    @Override
    public void removeHasKey() {
        clearAll(OWL.hasKey);
    }

    @Override
    public Stream<OntPE> hasKey() {
        return rdfListMembers(OWL.hasKey, OntPE.class);
    }

    @Override
    public abstract Class<? extends OntCE> getActualClass();

    public static class ObjectSomeValuesFromImpl extends ComponentRestrictionCEImpl<OntCE, OntOPE> implements ObjectSomeValuesFrom {
        public ObjectSomeValuesFromImpl(Node n, EnhGraph m) {
            super(n, m, OWL.someValuesFrom, OntCE.class, OntOPE.class);
        }

        @Override
        public Class<ObjectSomeValuesFrom> getActualClass() {
            return ObjectSomeValuesFrom.class;
        }
    }

    public static class DataSomeValuesFromImpl extends ComponentRestrictionCEImpl<OntDR, OntNDP> implements DataSomeValuesFrom {
        public DataSomeValuesFromImpl(Node n, EnhGraph m) {
            super(n, m, OWL.someValuesFrom, OntDR.class, OntNDP.class);
        }

        @Override
        public Class<DataSomeValuesFrom> getActualClass() {
            return DataSomeValuesFrom.class;
        }
    }

    public static class ObjectAllValuesFromImpl extends ComponentRestrictionCEImpl<OntCE, OntOPE> implements ObjectAllValuesFrom {
        public ObjectAllValuesFromImpl(Node n, EnhGraph m) {
            super(n, m, OWL.allValuesFrom, OntCE.class, OntOPE.class);
        }

        @Override
        public Class<ObjectAllValuesFrom> getActualClass() {
            return ObjectAllValuesFrom.class;
        }
    }

    public static class DataAllValuesFromImpl extends ComponentRestrictionCEImpl<OntDR, OntNDP> implements DataAllValuesFrom {
        public DataAllValuesFromImpl(Node n, EnhGraph m) {
            super(n, m, OWL.allValuesFrom, OntDR.class, OntNDP.class);
        }

        @Override
        public Class<DataAllValuesFrom> getActualClass() {
            return DataAllValuesFrom.class;
        }
    }

    public static class ObjectHasValueImpl extends ComponentRestrictionCEImpl<OntIndividual, OntOPE> implements ObjectHasValue {
        public ObjectHasValueImpl(Node n, EnhGraph m) {
            super(n, m, OWL.hasValue, OntIndividual.class, OntOPE.class);
        }

        @Override
        public Class<ObjectHasValue> getActualClass() {
            return ObjectHasValue.class;
        }
    }

    public static class DataHasValueImpl extends ComponentRestrictionCEImpl<Literal, OntNDP> implements DataHasValue {
        public DataHasValueImpl(Node n, EnhGraph m) {
            super(n, m, OWL.hasValue, Literal.class, OntNDP.class);
        }

        @Override
        public Class<DataHasValue> getActualClass() {
            return DataHasValue.class;
        }
    }

    public static class UnionOfImpl extends ComponentsCEImpl<OntCE> implements UnionOf {
        public UnionOfImpl(Node n, EnhGraph m) {
            super(n, m, OWL.unionOf, OntCE.class);
        }

        @Override
        public Class<UnionOf> getActualClass() {
            return UnionOf.class;
        }
    }

    public static class IntersectionOfImpl extends ComponentsCEImpl<OntCE> implements IntersectionOf {
        public IntersectionOfImpl(Node n, EnhGraph m) {
            super(n, m, OWL.intersectionOf, OntCE.class);
        }

        @Override
        public Class<IntersectionOf> getActualClass() {
            return IntersectionOf.class;
        }
    }

    public static class OneOfImpl extends ComponentsCEImpl<OntIndividual> implements OneOf {
        public OneOfImpl(Node n, EnhGraph m) {
            super(n, m, OWL.oneOf, OntIndividual.class);
        }

        @Override
        public Class<OneOf> getActualClass() {
            return OneOf.class;
        }
    }

    public static class DataMinCardinalityImpl extends CardinalityRestrictionCEImpl<OntDR, OntNDP> implements DataMinCardinality {
        public DataMinCardinalityImpl(Node n, EnhGraph m) {
            super(n, m, OWL.onDataRange, OntDR.class, OntNDP.class, CardinalityType.MIN);
        }

        @Override
        public Class<DataMinCardinality> getActualClass() {
            return DataMinCardinality.class;
        }
    }

    public static class ObjectMinCardinalityImpl extends CardinalityRestrictionCEImpl<OntCE, OntOPE> implements ObjectMinCardinality {
        public ObjectMinCardinalityImpl(Node n, EnhGraph m) {
            super(n, m, OWL.onClass, OntCE.class, OntOPE.class, CardinalityType.MIN);
        }

        @Override
        public Class<ObjectMinCardinality> getActualClass() {
            return ObjectMinCardinality.class;
        }
    }

    public static class DataMaxCardinalityImpl extends CardinalityRestrictionCEImpl<OntDR, OntNDP> implements DataMaxCardinality {
        public DataMaxCardinalityImpl(Node n, EnhGraph m) {
            super(n, m, OWL.onDataRange, OntDR.class, OntNDP.class, CardinalityType.MAX);
        }

        @Override
        public Class<DataMaxCardinality> getActualClass() {
            return DataMaxCardinality.class;
        }
    }

    public static class ObjectMaxCardinalityImpl extends CardinalityRestrictionCEImpl<OntCE, OntOPE> implements ObjectMaxCardinality {
        public ObjectMaxCardinalityImpl(Node n, EnhGraph m) {
            super(n, m, OWL.onClass, OntCE.class, OntOPE.class, CardinalityType.MAX);
        }

        @Override
        public Class<ObjectMaxCardinality> getActualClass() {
            return ObjectMaxCardinality.class;
        }
    }

    public static class DataCardinalityImpl extends CardinalityRestrictionCEImpl<OntDR, OntNDP> implements DataCardinality {
        public DataCardinalityImpl(Node n, EnhGraph m) {
            super(n, m, OWL.onDataRange, OntDR.class, OntNDP.class, CardinalityType.EXACTLY);
        }

        @Override
        public Class<DataCardinality> getActualClass() {
            return DataCardinality.class;
        }
    }

    public static class ObjectCardinalityImpl extends CardinalityRestrictionCEImpl<OntCE, OntOPE> implements ObjectCardinality {
        public ObjectCardinalityImpl(Node n, EnhGraph m) {
            super(n, m, OWL.onClass, OntCE.class, OntOPE.class, CardinalityType.EXACTLY);
        }

        @Override
        public Class<ObjectCardinality> getActualClass() {
            return ObjectCardinality.class;
        }
    }

    public static class HasSelfImpl extends OnPropertyRestrictionCEImpl<OntOPE> implements HasSelf {
        public HasSelfImpl(Node n, EnhGraph m) {
            super(n, m, OntOPE.class);
        }

        @Override
        public Class<HasSelf> getActualClass() {
            return HasSelf.class;
        }

        Stream<OntStatement> hasSelf() {
            return statement(OWL.hasSelf, Models.TRUE).map(Stream::of).orElse(Stream.empty());
        }

        @Override
        public Stream<OntStatement> content() {
            return Stream.concat(super.content(), hasSelf());
        }
    }

    public static class ComplementOfImpl extends OntCEImpl implements ComplementOf {
        public ComplementOfImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        public Class<ComplementOf> getActualClass() {
            return ComplementOf.class;
        }

        @Override
        public OntCE getValue() {
            return getModel().getNodeAs(getPropertyResourceValue(OWL.complementOf).asNode(), OntCE.class);
        }

        @Override
        public void setValue(OntCE c) {
            clear();
            addProperty(OWL.complementOf, c);
        }

        void clear() {
            removeAll(OWL.complementOf);
        }

        @Override
        public Stream<OntStatement> content() {
            return Stream.concat(super.content(), statement(OWL.complementOf).map(Stream::of).orElse(Stream.empty()));
        }
    }

    public static class NaryDataAllValuesFromImpl extends NaryRestrictionCEImpl<OntDR, OntNDP> implements NaryDataAllValuesFrom {
        public NaryDataAllValuesFromImpl(Node n, EnhGraph m) {
            super(n, m, OWL.allValuesFrom, OntDR.class, OntNDP.class);
        }

        @Override
        public Class<? extends OntCE> getActualClass() {
            return NaryDataAllValuesFrom.class;
        }
    }

    public static class NaryDataSomeValuesFromImpl extends NaryRestrictionCEImpl<OntDR, OntNDP> implements NaryDataSomeValuesFrom {
        public NaryDataSomeValuesFromImpl(Node n, EnhGraph m) {
            super(n, m, OWL.someValuesFrom, OntDR.class, OntNDP.class);
        }

        @Override
        public Class<? extends OntCE> getActualClass() {
            return NaryDataSomeValuesFrom.class;
        }
    }

    /**
     * Abstract base components CE (IntersectionOf, OneOf, UnionOf)
     *
     * @param <O> OntObject
     */
    static abstract class ComponentsCEImpl<O extends OntObject> extends OntCEImpl implements ComponentsCE<O> {
        private final Property predicate;
        private final Class<O> view;

        ComponentsCEImpl(Node n, EnhGraph m, Property predicate, Class<O> view) {
            super(n, m);
            this.predicate = OntJenaException.notNull(predicate, "Null predicate.");
            this.view = OntJenaException.notNull(view, "Null view.");
        }

        @Override
        public Stream<O> components() {
            return rdfListMembers(predicate, view);
        }

        @Override
        public void setComponents(Collection<O> components) {
            clearAll(predicate);
            addProperty(predicate, getModel().createList(components.iterator()));
        }

        Stream<OntStatement> listStatements() {
            return Stream.of(statements(predicate), rdfListContent(predicate)).flatMap(Function.identity());
        }

        @Override
        public Stream<OntStatement> content() {
            return Stream.concat(super.content(), listStatements());
        }
    }

    /**
     * for any restriction with owl:onProperty
     *
     * @param <P> OntPE (DataProperty or ObjectProperty Expression)
     */
    static abstract class OnPropertyRestrictionCEImpl<P extends OntPE> extends OntCEImpl implements ONProperty<P> {
        private final Class<P> propertyView;

        /**
         * @param n            Node
         * @param m            EnhGraph
         * @param propertyView Class for OntPE
         */
        private OnPropertyRestrictionCEImpl(Node n, EnhGraph m, Class<P> propertyView) {
            super(n, m);
            this.propertyView = propertyView;
        }

        @Override
        public P getOnProperty() {
            return getRequiredObject(OWL.onProperty, propertyView);
        }

        @Override
        public void setOnProperty(P p) {
            clearProperty(OWL.onProperty);
            addProperty(OWL.onProperty, p);
        }

        void clearProperty(Property property) {
            removeAll(property);
        }

        Stream<OntStatement> onPropertyStatement() {
            return statement(OWL.onProperty, getOnProperty()).map(Stream::of).orElse(Stream.empty());
        }

        @Override
        public Stream<OntStatement> content() {
            return Stream.concat(super.content(), onPropertyStatement());
        }
    }

    /**
     * Abstract base component-restriction class.
     * It's for CE which has owl:onProperty and some component also (with predicate owl:dataRange,owl:onClass, owl:someValuesFrom, owl:allValuesFrom)
     *
     * @param <O>
     */
    static abstract class ComponentRestrictionCEImpl<O extends RDFNode, P extends OntPE> extends OnPropertyRestrictionCEImpl<P> implements ComponentRestrictionCE<O, P> {
        protected final Property predicate;
        final Class<O> objectView;

        /**
         * @param n            Node
         * @param m            EnhGraph
         * @param predicate    predicate for value
         * @param objectView   Class
         * @param propertyView Class
         */
        private ComponentRestrictionCEImpl(Node n, EnhGraph m, Property predicate, Class<O> objectView, Class<P> propertyView) {
            super(n, m, propertyView);
            this.predicate = OntJenaException.notNull(predicate, "Null predicate.");
            this.objectView = OntJenaException.notNull(objectView, "Null view.");
        }

        @Override
        public O getValue() {
            return getModel().getNodeAs(getRequiredProperty(predicate).getObject().asNode(), objectView);
        }

        @Override
        public void setValue(O c) {
            clearProperty(predicate);
            addProperty(predicate, c);
        }

        Stream<OntStatement> valueStatement() {
            return statement(predicate, getValue()).map(Stream::of).orElse(Stream.empty());
        }

        @Override
        public Stream<OntStatement> content() {
            return Stream.concat(super.content(), valueStatement());
        }
    }

    static abstract class CardinalityRestrictionCEImpl<O extends OntObject, P extends OntPE> extends ComponentRestrictionCEImpl<O, P> implements CardinalityRestrictionCE<O, P> {
        private final CardinalityType cardinalityType;

        /**
         * @param n               Node
         * @param m               Model
         * @param predicate       can be owl:onDataRange or owl:onClass
         * @param objectView      interface of class expression or data range
         * @param propertyView    interface, property expression
         * @param cardinalityType type of cardinality.
         */
        private CardinalityRestrictionCEImpl(Node n, EnhGraph m, Property predicate, Class<O> objectView, Class<P> propertyView, CardinalityType cardinalityType) {
            super(n, m, predicate, objectView, propertyView);
            this.cardinalityType = cardinalityType;
        }

        @Override
        public O getValue() {
            return getObject(predicate, objectView);
        }

        @Override
        public int getCardinality() {
            return getRequiredObject(getCardinalityPredicate(), Literal.class).getInt();
        }

        @Override
        public void setCardinality(int cardinality) {
            Property p = getCardinalityPredicate();
            clearProperty(p);
            Literal l = ResourceFactory.createTypedLiteral(String.valueOf(cardinality), XSDDatatype.XSDnonNegativeInteger);
            addLiteral(p, l);
        }

        private Property getCardinalityPredicate() {
            return cardinalityType.getPredicate(isQualified());
        }

        @Override
        public boolean isQualified() {
            return isQualified(getValue());
        }

        Stream<OntStatement> cardinalityStatement() {
            return statement(getCardinalityPredicate()).map(Stream::of).orElse(Stream.empty());
        }

        @Override
        public Stream<OntStatement> content() { // note: value <O> could be null for qualified restrictions:
            return Stream.concat(super.content(), cardinalityStatement());
        }
    }

    /**
     * TODO: currently it is read-only
     *
     * @param <O>
     * @param <P>
     */
    static abstract class NaryRestrictionCEImpl<O extends OntObject, P extends OntPE> extends OntCEImpl implements NaryRestrictionCE<O, P> {
        private final Property predicate;
        private final Class<O> objectView;
        private final Class<P> propertyView;

        private NaryRestrictionCEImpl(Node n, EnhGraph m, Property predicate, Class<O> objectView, Class<P> propertyView) {
            super(n, m);
            this.predicate = predicate;
            this.objectView = objectView;
            this.propertyView = propertyView;
        }

        @Override
        public Stream<P> onProperties() {
            return rdfListMembers(OWL.onProperties, propertyView);
        }

        @Override
        public void setOnProperties(Collection<P> properties) {
            throw new OntJenaException("TODO");
        }

        @Override
        public O getValue() {
            return getRequiredObject(predicate, objectView);
        }

        @Override
        public void setValue(O value) {
            throw new OntJenaException("TODO");
        }

        @Override
        public Class<? extends OntCE> getActualClass() {
            return NaryRestrictionCE.class;
        }

        Stream<OntStatement> valueStatement() {
            return statement(predicate, getValue()).map(Stream::of).orElse(Stream.empty());
        }

        Stream<OntStatement> onPropertiesStatements() {
            return Stream.of(statements(OWL.onProperties),
                    rdfListContent(OWL.onProperties)).flatMap(Function.identity());
        }

        @Override
        public Stream<OntStatement> content() {
            return Stream.of(super.content(), valueStatement(), onPropertiesStatements()).flatMap(Function.identity());
        }
    }

    private enum RestrictionType {
        DATA(Entities.DATA_PROPERTY),
        OBJECT(OntPEImpl.abstractOPEFactory),;

        private final Configurable<? extends OntObjectFactory> factory;

        RestrictionType(Configurable<? extends OntObjectFactory> factory) {
            this.factory = factory;
        }

        public Configurable<OntFilter> getFilter() {
            return mode -> (OntFilter) (n, g) -> Iter.asStream(g.asGraph().find(n, OWL.onProperty.asNode(), Node.ANY))
                    .map(Triple::getObject)
                    .map(_n -> factory.get(mode).canWrap(_n, g))
                    .findAny().orElse(false);
        }
    }

    private enum CardinalityType {
        EXACTLY(OWL.qualifiedCardinality, OWL.cardinality),
        MAX(OWL.maxQualifiedCardinality, OWL.maxCardinality),
        MIN(OWL.minQualifiedCardinality, OWL.minCardinality);
        private final Property qualifiedPredicate, predicate;

        CardinalityType(Property qualifiedPredicate, Property predicate) {
            this.qualifiedPredicate = qualifiedPredicate;
            this.predicate = predicate;
        }

        public OntFilter getFilter() {
            return (n, g) -> g.asGraph().contains(n, qualifiedPredicate.asNode(), Node.ANY) || g.asGraph().contains(n, predicate.asNode(), Node.ANY);
        }

        public Property getPredicate(boolean isQualified) {
            return isQualified ? qualifiedPredicate : predicate;
        }
    }

    private static class HasSelfFilter implements OntFilter {
        @Override
        public boolean test(Node n, EnhGraph g) {
            return g.asGraph().contains(n, OWL.hasSelf.asNode(), Models.TRUE.asNode());
        }
    }

    private static class HasSelfMaker extends OntMaker.WithType {
        private HasSelfMaker() {
            super(HasSelfImpl.class, OWL.Restriction);
        }

        @Override
        public void make(Node node, EnhGraph eg) {
            super.make(node, eg);
            eg.asGraph().add(Triple.create(node, OWL.hasSelf.asNode(), Models.TRUE.asNode()));
        }
    }

    private static Configurable<OntObjectFactory> createCEFactory(Class<? extends OntCEImpl> impl, Property predicate) {
        OntMaker maker = new OntMaker.WithType(impl, OWL.Class);
        OntFilter filter = OntFilter.BLANK.and(new OntFilter.HasType(OWL.Class)).and(new OntFilter.HasPredicate(predicate));
        return mode -> new CommonOntObjectFactory(maker, CLASS_FINDER, filter);
    }

    private static Configurable<OntObjectFactory> createRestrictionFactory(Class<? extends CardinalityRestrictionCEImpl> impl, RestrictionType restrictionType, CardinalityType cardinalityType) {
        OntMaker maker = new OntMaker.WithType(impl, OWL.Restriction);
        Configurable<OntFilter> filter = m -> RESTRICTION_FILTER
                .and(cardinalityType.getFilter())
                .and(restrictionType.getFilter().get(m));
        return mode -> new CommonOntObjectFactory(maker, RESTRICTION_FINDER, filter.get(mode));
    }

    private static Configurable<OntObjectFactory> createRestrictionFactory(Class<? extends ComponentRestrictionCEImpl> impl, RestrictionType restrictionType, Property predicate) {
        OntMaker maker = new OntMaker.WithType(impl, OWL.Restriction);
        Configurable<OntFilter> filter = m -> RESTRICTION_FILTER
                .and(new OntFilter.HasPredicate(predicate))
                .and(restrictionType.getFilter().get(m));
        return mode -> new CommonOntObjectFactory(maker, RESTRICTION_FINDER, filter.get(mode));
    }

    private static Configurable<OntObjectFactory> createNaryFactory(Class<? extends NaryRestrictionCEImpl> impl, Property predicate) {
        OntMaker maker = new OntMaker.WithType(impl, OWL.Restriction);
        OntFilter filter = RESTRICTION_FILTER
                .and(new OntFilter.HasPredicate(predicate))
                .and(new OntFilter.HasPredicate(OWL.onProperties));
        return mode -> new CommonOntObjectFactory(maker, RESTRICTION_FINDER, filter);
    }

    static boolean isQualified(OntObject c) {
        return c != null && !OWL.Thing.equals(c) && !RDFS.Literal.equals(c);
    }

    private static CardinalityType getCardinalityType(Class<? extends CardinalityRestrictionCE> view) {
        if (ObjectMinCardinality.class.equals(view) || DataMinCardinality.class.equals(view)) {
            return CardinalityType.MIN;
        }
        if (ObjectMaxCardinality.class.equals(view) || DataMaxCardinality.class.equals(view)) {
            return CardinalityType.MAX;
        }
        return CardinalityType.EXACTLY;
    }

    private static Resource createOnPropertyRestriction(OntGraphModelImpl model, OntPE onProperty) {
        OntJenaException.notNull(onProperty, "Null property.");
        Resource res = model.createResource();
        model.add(res, RDF.type, OWL.Restriction);
        model.add(res, OWL.onProperty, onProperty);
        return res;
    }

    public static <CE extends ComponentRestrictionCE> CE createComponentRestrictionCE(OntGraphModelImpl model, Class<CE> view, OntPE onProperty, RDFNode other, Property predicate) {
        OntJenaException.notNull(other, "Null expression.");
        Resource res = createOnPropertyRestriction(model, onProperty);
        model.add(res, predicate, other);
        return model.getNodeAs(res.asNode(), view);
    }

    public static <CE extends CardinalityRestrictionCE> CE createCardinalityRestrictionCE(OntGraphModelImpl model, Class<CE> view, OntPE onProperty, int cardinality, OntObject object) {
        Resource res = createOnPropertyRestriction(model, onProperty);
        Literal value = ResourceFactory.createTypedLiteral(String.valueOf(cardinality), XSDDatatype.XSDnonNegativeInteger);
        model.add(res, getCardinalityType(view).getPredicate(isQualified(object)), value);
        if (object != null) {
            model.add(res, OntOPE.class.isInstance(onProperty) ? OWL.onClass : OWL.onDataRange, object);
        }
        return model.getNodeAs(res.asNode(), view);
    }

    public static <CE extends ComponentsCE> CE createComponentsCE(OntGraphModelImpl model, Class<CE> view, Property predicate, Stream<? extends OntObject> components) {
        OntJenaException.notNull(components, "Null components stream.");
        Resource res = model.createResource();
        model.add(res, RDF.type, OWL.Class);
        model.add(res, predicate, model.createList(components.iterator()));
        return model.getNodeAs(res.asNode(), view);
    }

    public static HasSelf createHasSelf(OntGraphModelImpl model, OntOPE onProperty) {
        Resource res = createOnPropertyRestriction(model, onProperty);
        model.add(res, OWL.hasSelf, Models.TRUE);
        return model.getNodeAs(res.asNode(), HasSelf.class);
    }

    public static ComplementOf createComplementOf(OntGraphModelImpl model, OntCE other) {
        OntJenaException.notNull(other, "Null class expression.");
        Resource res = model.createResource();
        model.add(res, RDF.type, OWL.Class);
        model.add(res, OWL.complementOf, other);
        return model.getNodeAs(res.asNode(), ComplementOf.class);
    }

    public static OntIndividual.Anonymous createAnonymousIndividual(OntGraphModelImpl model, OntCE source) {
        Resource res = model.createResource();
        model.add(res, RDF.type, source);
        return model.getNodeAs(res.asNode(), OntIndividual.Anonymous.class);
    }

    public static OntIndividual.Named createNamedIndividual(OntGraphModelImpl model, OntCE source, String uri) {
        Resource res = model.createResource(OntJenaException.notNull(uri, "Null uri"));
        model.add(res, RDF.type, source);
        model.add(res, RDF.type, OWL.NamedIndividual);
        return model.getNodeAs(res.asNode(), OntIndividual.Named.class);
    }

    public static OntStatement addHasKey(OntCE clazz, Collection<OntOPE> objectProperties, Collection<OntNDP> dataProperties) {
        List<OntPE> properties = new ArrayList<>();
        if (objectProperties != null) properties.addAll(objectProperties);
        if (dataProperties != null) properties.addAll(dataProperties);
        return clazz.addStatement(OWL.hasKey, clazz.getModel().createList(properties.iterator()));
    }
}
