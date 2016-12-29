package ru.avicomp.ontapi.jena.impl;

import java.util.List;
import java.util.stream.Stream;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.*;
import org.apache.jena.util.iterator.UniqueFilter;
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
    private static final Node OWL_ON_PROPERTY = OWL.onProperty.asNode();
    private static final Node OWL_RESTRICTION = OWL.Restriction.asNode();
    private static final Node OWL_CLASS = OWL.Class.asNode();

    private static final OntFilter ON_PROPERTY_DATA_FILTER = new OnPropertyTypeFilter(RestrictionType.DATA);
    private static final OntFilter ON_PROPERTY_OBJECT_FILTER = new OnPropertyTypeFilter(RestrictionType.OBJECT);
    private static final OntFinder CE_FINDER = g -> Models.asStream(g.asGraph().find(Node.ANY, RDF_TYPE, OWL_CLASS).
            andThen(g.asGraph().find(Node.ANY, RDF_TYPE, OWL_RESTRICTION)).mapWith(Triple::getSubject)).distinct();

    public static OntObjectFactory unionOfCEFactory = new CEFactory(UnionOfImpl.class, OWL.Class, OWL.unionOf);
    public static OntObjectFactory intersectionOfCEFactory = new CEFactory(IntersectionOfImpl.class, OWL.Class, OWL.intersectionOf);
    public static OntObjectFactory oneOfCEFactory = new CEFactory(OneOfImpl.class, OWL.Class, OWL.oneOf);
    public static OntObjectFactory complementOfCEFactory = new CEFactory(ComplementOfImpl.class, OWL.Class, OWL.complementOf);

    public static OntObjectFactory objectSomeValuesOfCEFactory = new CEFactory(ObjectSomeValuesFromImpl.class, OWL.Restriction,
            OWL.someValuesFrom, ON_PROPERTY_OBJECT_FILTER);
    public static OntObjectFactory dataSomeValuesOfCEFactory = new CEFactory(DataSomeValuesFromImpl.class, OWL.Restriction,
            OWL.someValuesFrom, ON_PROPERTY_DATA_FILTER);

    public static OntObjectFactory objectAllValuesOfCEFactory = new CEFactory(ObjectAllValuesFromImpl.class, OWL.Restriction,
            OWL.allValuesFrom, ON_PROPERTY_OBJECT_FILTER);
    public static OntObjectFactory dataAllValuesOfCEFactory = new CEFactory(DataAllValuesFromImpl.class, OWL.Restriction,
            OWL.allValuesFrom, ON_PROPERTY_DATA_FILTER);

    public static OntObjectFactory objectHasValueCEFactory = new CEFactory(ObjectHasValueImpl.class, OWL.Restriction,
            OWL.hasValue, ON_PROPERTY_OBJECT_FILTER);
    public static OntObjectFactory dataHasValueCEFactory = new CEFactory(DataHasValueImpl.class, OWL.Restriction,
            OWL.hasValue, ON_PROPERTY_DATA_FILTER);

    public static OntObjectFactory dataMinCardinalityCEFactory = new CEFactory(DataMinCardinalityImpl.class, OWL.Restriction,
            new CardinalityPredicateFilter(CardinalityType.MIN), ON_PROPERTY_DATA_FILTER);
    public static OntObjectFactory objectMinCardinalityCEFactory = new CEFactory(ObjectMinCardinalityImpl.class, OWL.Restriction,
            new CardinalityPredicateFilter(CardinalityType.MIN), ON_PROPERTY_OBJECT_FILTER);

    public static OntObjectFactory dataMaxCardinalityCEFactory = new CEFactory(DataMaxCardinalityImpl.class, OWL.Restriction,
            new CardinalityPredicateFilter(CardinalityType.MAX), ON_PROPERTY_DATA_FILTER);
    public static OntObjectFactory objectMaxCardinalityCEFactory = new CEFactory(ObjectMaxCardinalityImpl.class, OWL.Restriction,
            OntFilter.BLANK, ON_PROPERTY_OBJECT_FILTER, new CardinalityPredicateFilter(CardinalityType.MAX));

    public static OntObjectFactory dataCardinalityCEFactory = new CEFactory(DataCardinalityImpl.class, OWL.Restriction,
            new CardinalityPredicateFilter(CardinalityType.EXACTLY), ON_PROPERTY_DATA_FILTER);
    public static OntObjectFactory objectCardinalityCEFactory = new CEFactory(ObjectCardinalityImpl.class, OWL.Restriction,
            new CardinalityPredicateFilter(CardinalityType.EXACTLY), ON_PROPERTY_OBJECT_FILTER);

    public static OntObjectFactory hasSelfCEFactory = new CommonOntObjectFactory(new HasSelfMaker(), new OntFinder.ByType(OWL.Restriction),
            OntFilter.BLANK, new HasSelfFilter());

    public static OntObjectFactory abstractCEFactory = new MultiOntObjectFactory(CE_FINDER,
            OntEntityImpl.classFactory,
            objectSomeValuesOfCEFactory, dataSomeValuesOfCEFactory,
            objectAllValuesOfCEFactory, dataAllValuesOfCEFactory,
            objectHasValueCEFactory, dataHasValueCEFactory,
            objectMinCardinalityCEFactory, dataMinCardinalityCEFactory,
            objectMaxCardinalityCEFactory, dataMaxCardinalityCEFactory,
            objectCardinalityCEFactory, dataCardinalityCEFactory,
            unionOfCEFactory, intersectionOfCEFactory, oneOfCEFactory, complementOfCEFactory, hasSelfCEFactory);

    public static OntObjectFactory abstractComponentsCEFactory = new MultiOntObjectFactory(CE_FINDER,
            unionOfCEFactory, intersectionOfCEFactory, oneOfCEFactory);

    public static OntObjectFactory abstractCardinalityRestrictionCEFactory = new MultiOntObjectFactory(CE_FINDER,
            objectMinCardinalityCEFactory, dataMinCardinalityCEFactory,
            objectMaxCardinalityCEFactory, dataMaxCardinalityCEFactory,
            objectCardinalityCEFactory, dataCardinalityCEFactory);

    public static MultiOntObjectFactory abstractComponentRestrictionCEFactory = new MultiOntObjectFactory(CE_FINDER,
            objectSomeValuesOfCEFactory, dataSomeValuesOfCEFactory,
            objectAllValuesOfCEFactory, dataAllValuesOfCEFactory,
            objectHasValueCEFactory, dataHasValueCEFactory,
            objectMinCardinalityCEFactory, dataMinCardinalityCEFactory,
            objectMaxCardinalityCEFactory, dataMaxCardinalityCEFactory,
            objectCardinalityCEFactory, dataCardinalityCEFactory);

    // todo: + nary restrictions:
    public static OntObjectFactory abstractRestrictionCEFactory = new MultiOntObjectFactory(CE_FINDER,
            objectSomeValuesOfCEFactory, dataSomeValuesOfCEFactory,
            objectAllValuesOfCEFactory, dataAllValuesOfCEFactory,
            objectHasValueCEFactory, dataHasValueCEFactory,
            objectMinCardinalityCEFactory, dataMinCardinalityCEFactory,
            objectMaxCardinalityCEFactory, dataMaxCardinalityCEFactory,
            objectCardinalityCEFactory, dataCardinalityCEFactory,
            hasSelfCEFactory);

    private OntCEImpl(Node n, EnhGraph m) {
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
    public OntStatement addHasKey(Stream<OntOPE> objectProperties, Stream<OntNDP> dataProperties) {
        return addHasKey(this, objectProperties, dataProperties);
    }

    @Override
    public void removeHasKey() {
        clearAll(OWL.hasKey);
    }

    @Override
    public Stream<OntPE> hasKey() {
        return rdfList(OWL.hasKey, OntPE.class);
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
    }

    /**
     * Abstract base components CE.
     *
     * @param <O> OntObject
     */
    abstract static class ComponentsCEImpl<O extends OntObject> extends OntCEImpl implements ComponentsCE<O> {
        private final Property predicate;
        private final Class<O> view;

        ComponentsCEImpl(Node n, EnhGraph m, Property predicate, Class<O> view) {
            super(n, m);
            this.predicate = OntJenaException.notNull(predicate, "Null predicate.");
            this.view = OntJenaException.notNull(view, "Null view.");
        }
        @Override
        public Stream<O> components() {
            return rdfList(predicate, view);
        }

        @Override
        public void setComponents(Stream<O> components) {
            clearAll(predicate);
            addProperty(predicate, getModel().createList(components.iterator()));
        }
    }

    /**
     * for any restriction with owl:onProperty
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
            return getRequiredOntProperty(OWL.onProperty, propertyView);
        }

        @Override
        public void setOnProperty(P p) {
            clearProperty(OWL.onProperty);
            addProperty(OWL.onProperty, p);
        }

        void clearProperty(Property property) {
            removeAll(property);
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
            return getOntProperty(predicate, objectView);
        }

        @Override
        public int getCardinality() {
            return getRequiredProperty(getCardinalityPredicate()).getObject().asLiteral().getInt();
        }

        @Override
        public void setCardinality(int cardinality) {
            Property p = getCardinalityPredicate();
            clearProperty(p);
            Literal l = ResourceFactory.createTypedLiteral(String.valueOf(cardinality), XSDDatatype.XSDnonNegativeInteger);
            addLiteral(p, l);
        }

        private Property getCardinalityPredicate() {
            return getCardinalityPredicate(isQualified(), cardinalityType);
        }

        @Override
        public boolean isQualified() {
            return isQualified(getValue());
        }
    }

    private static class OnPropertyTypeFilter implements OntFilter {
        private final RestrictionType type;

        private OnPropertyTypeFilter(RestrictionType type) {
            this.type = OntJenaException.notNull(type, "Null restriction type.");
        }

        @Override
        public boolean test(Node n, EnhGraph g) {
            List<Node> nodes = g.asGraph().find(n, OWL_ON_PROPERTY, Node.ANY).mapWith(Triple::getObject).filterKeep(new UniqueFilter<>()).toList();
            if (nodes.size() != 1) return false;
            Node node = nodes.get(0);
            return RestrictionType.DATA.equals(type) ?
                    OntEntityImpl.dataPropertyFactory.canWrap(node, g) :
                    !RestrictionType.OBJECT.equals(type) || OntPEImpl.abstractOPEFactory.canWrap(node, g);
        }
    }

    private static class CardinalityPredicateFilter implements OntFilter {
        private final CardinalityType type;

        private CardinalityPredicateFilter(CardinalityType type) {
            this.type = OntJenaException.notNull(type, "Null cardinality type.");
        }

        @Override
        public boolean test(Node n, EnhGraph g) {
            if (CardinalityType.MAX.equals(type)) {
                return g.asGraph().contains(n, OWL.maxQualifiedCardinality.asNode(), Node.ANY) || g.asGraph().contains(n, OWL.maxCardinality.asNode(), Node.ANY);
            } else if (CardinalityType.MIN.equals(type)) {
                return g.asGraph().contains(n, OWL.minQualifiedCardinality.asNode(), Node.ANY) || g.asGraph().contains(n, OWL.minCardinality.asNode(), Node.ANY);
            }
            return g.asGraph().contains(n, OWL.qualifiedCardinality.asNode(), Node.ANY) || g.asGraph().contains(n, OWL.cardinality.asNode(), Node.ANY);
        }
    }

    private static class HasSelfFilter implements OntFilter {
        @Override
        public boolean test(Node n, EnhGraph g) {
            return g.asGraph().contains(n, OWL.hasSelf.asNode(), ResourceFactory.createTypedLiteral(Boolean.TRUE).asNode());
        }
    }

    private static class HasSelfMaker extends OntMaker.WithType {
        private HasSelfMaker() {
            super(HasSelfImpl.class, OWL.Restriction);
        }

        @Override
        public void make(Node node, EnhGraph eg) {
            super.make(node, eg);
            eg.asGraph().add(Triple.create(node, OWL.hasSelf.asNode(), ResourceFactory.createTypedLiteral(Boolean.TRUE).asNode()));
        }
    }

    private static class CEFactory extends CommonOntObjectFactory {
        private CEFactory(Class<? extends OntObjectImpl> impl, Resource type, Property predicate, OntFilter... filters) {
            super(makeCEMaker(impl, type), makeCEFinder(type), makeCEFilter(type, predicate), filters);
        }

        private CEFactory(Class<? extends OntObjectImpl> impl, Resource type, OntFilter... filters) {
            super(makeCEMaker(impl, type), makeCEFinder(type), makeCEFilter(type), filters);
        }

        private static OntMaker makeCEMaker(Class<? extends OntObjectImpl> impl, Resource type) {
            return new OntMaker.WithType(impl, type);
        }

        private static OntFinder makeCEFinder(Resource type) {
            return new OntFinder.ByType(type);
        }

        private static OntFilter makeCEFilter(Resource type) {
            return OntFilter.BLANK.and(new OntFilter.HasType(type));
        }

        private static OntFilter makeCEFilter(Resource type, Property predicate) {
            return makeCEFilter(type).and(new OntFilter.HasPredicate(predicate));
        }
    }

    protected static boolean isQualified(OntObject c) {
        return c != null && !OWL.Thing.equals(c) && !RDFS.Literal.equals(c);
    }

    protected static Property getCardinalityPredicate(boolean isQualified, CardinalityType cardinalityType) {
        if (CardinalityType.MAX.equals(cardinalityType)) {
            return isQualified ? OWL.maxQualifiedCardinality : OWL.maxCardinality;
        }
        if (CardinalityType.MIN.equals(cardinalityType)) {
            return isQualified ? OWL.minQualifiedCardinality : OWL.minCardinality;
        }
        return isQualified ? OWL.qualifiedCardinality : OWL.cardinality;
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

    private enum RestrictionType {
        UNDEFINED,
        DATA,
        OBJECT,
    }

    private enum CardinalityType {
        EXACTLY,
        MAX,
        MIN
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
        model.add(res, getCardinalityPredicate(isQualified(object), getCardinalityType(view)), value);
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

    public static OntCE.HasSelf createHasSelf(OntGraphModelImpl model, OntOPE onProperty) {
        Resource res = createOnPropertyRestriction(model, onProperty);
        model.add(res, OWL.hasSelf, ResourceFactory.createTypedLiteral(Boolean.TRUE));
        return model.getNodeAs(res.asNode(), OntCE.HasSelf.class);
    }

    public static OntCE.ComplementOf createComplementOf(OntGraphModelImpl model, OntCE other) {
        OntJenaException.notNull(other, "Null class expression.");
        Resource res = model.createResource();
        model.add(res, RDF.type, OWL.Class);
        model.add(res, OWL.complementOf, other);
        return model.getNodeAs(res.asNode(), OntCE.ComplementOf.class);
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

    public static OntStatement addHasKey(OntCE clazz, Stream<OntOPE> objectProperties, Stream<OntNDP> dataProperties) {
        if (objectProperties == null) objectProperties = Stream.empty();
        if (dataProperties == null) dataProperties = Stream.empty();
        Stream<OntPE> properties = Stream.concat(objectProperties, dataProperties);
        return clazz.addStatement(OWL.hasKey, clazz.getModel().createList(properties.iterator()));
    }
}
