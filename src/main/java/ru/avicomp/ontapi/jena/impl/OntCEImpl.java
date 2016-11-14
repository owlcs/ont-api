package ru.avicomp.ontapi.jena.impl;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.*;
import org.apache.jena.util.iterator.UniqueFilter;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDFS;

import ru.avicomp.ontapi.OntException;
import ru.avicomp.ontapi.jena.JenaUtils;
import ru.avicomp.ontapi.jena.impl.configuration.*;
import ru.avicomp.ontapi.jena.model.*;

/**
 * base class for any class-expression.
 * <p>
 * Created by szuev on 03.11.2016.
 */
public abstract class OntCEImpl extends OntObjectImpl implements OntCE {
    private static final Node OWL_ON_PROPERTY = OWL2.onProperty.asNode();

    private static final OntFilter ON_PROPERTY_DATA_FILTER = new OnPropertyTypeFilter(RestrictionType.DATA);
    private static final OntFilter ON_PROPERTY_OBJECT_FILTER = new OnPropertyTypeFilter(RestrictionType.OBJECT);
    private static final OntFinder CE_FINDER = g -> JenaUtils.asStream(g.asGraph().find(Node.ANY, RDF_TYPE, OWL_CLASS).
            andThen(g.asGraph().find(Node.ANY, RDF_TYPE, OWL_RESTRICTION)).mapWith(Triple::getSubject)).distinct();

    public static OntObjectFactory unionOfCEFactory = new CEFactory(UnionOfImpl.class, OWL2.Class, OWL2.unionOf);
    public static OntObjectFactory intersectionOfCEFactory = new CEFactory(IntersectionOfImpl.class, OWL2.Class, OWL2.intersectionOf);
    public static OntObjectFactory oneOfCEFactory = new CEFactory(OneOfImpl.class, OWL2.Class, OWL2.oneOf);
    public static OntObjectFactory complementOfCEFactory = new CEFactory(ComplementOfImpl.class, OWL2.Class, OWL2.complementOf);

    public static OntObjectFactory objectSomeValuesOfCEFactory = new CEFactory(ObjectSomeValuesFromImpl.class, OWL2.Restriction,
            OWL2.someValuesFrom, ON_PROPERTY_OBJECT_FILTER);
    public static OntObjectFactory dataSomeValuesOfCEFactory = new CEFactory(DataSomeValuesFromImpl.class, OWL2.Restriction,
            OWL2.someValuesFrom, ON_PROPERTY_DATA_FILTER);

    public static OntObjectFactory objectAllValuesOfCEFactory = new CEFactory(ObjectAllValuesFromImpl.class, OWL2.Restriction,
            OWL2.allValuesFrom, ON_PROPERTY_OBJECT_FILTER);
    public static OntObjectFactory dataAllValuesOfCEFactory = new CEFactory(DataAllValuesFromImpl.class, OWL2.Restriction,
            OWL2.allValuesFrom, ON_PROPERTY_DATA_FILTER);

    public static OntObjectFactory objectHasValueCEFactory = new CEFactory(ObjectHasValueImpl.class, OWL2.Restriction,
            OWL2.hasValue, ON_PROPERTY_OBJECT_FILTER);
    public static OntObjectFactory dataHasValueCEFactory = new CEFactory(DataHasValueImpl.class, OWL2.Restriction,
            OWL2.hasValue, ON_PROPERTY_DATA_FILTER);

    public static OntObjectFactory dataMinCardinalityCEFactory = new CEFactory(DataMinCardinalityImpl.class, OWL2.Restriction,
            new CardinalityPredicateFilter(CardinalityType.MIN), ON_PROPERTY_DATA_FILTER);
    public static OntObjectFactory objectMinCardinalityCEFactory = new CEFactory(ObjectMinCardinalityImpl.class, OWL2.Restriction,
            new CardinalityPredicateFilter(CardinalityType.MIN), ON_PROPERTY_OBJECT_FILTER);

    public static OntObjectFactory dataMaxCardinalityCEFactory = new CEFactory(DataMaxCardinalityImpl.class, OWL2.Restriction,
            new CardinalityPredicateFilter(CardinalityType.MAX), ON_PROPERTY_DATA_FILTER);
    public static OntObjectFactory objectMaxCardinalityCEFactory = new CEFactory(ObjectMaxCardinalityImpl.class, OWL2.Restriction,
            OntFilter.BLANK, ON_PROPERTY_OBJECT_FILTER, new CardinalityPredicateFilter(CardinalityType.MAX));

    public static OntObjectFactory dataCardinalityCEFactory = new CEFactory(DataCardinalityImpl.class, OWL2.Restriction,
            new CardinalityPredicateFilter(CardinalityType.EXACTLY), ON_PROPERTY_DATA_FILTER);
    public static OntObjectFactory objectCardinalityCEFactory = new CEFactory(ObjectCardinalityImpl.class, OWL2.Restriction,
            new CardinalityPredicateFilter(CardinalityType.EXACTLY), ON_PROPERTY_OBJECT_FILTER);

    public static OntObjectFactory hasSelfCEFactory = new CommonOntObjectFactory(new HasSelfMaker(), new OntFinder.ByType(OWL2.Restriction),
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
    public Stream<OntCE> subClassOf() {
        return getModel().classExpressions(this, RDFS.subClassOf);
    }

    @Override
    public void addSubClassOf(OntCE superClass) {
        getModel().add(this, RDFS.subClassOf, OntException.notNull(superClass, "Null Super Class."));
    }

    @Override
    public void deleteSubClassOf(OntCE superClass) {
        getModel().remove(this, RDFS.subClassOf, OntException.notNull(superClass, "Null Super Class."));
    }

    @Override
    public abstract Class<? extends OntCE> getActualClass();

    public static class ObjectSomeValuesFromImpl extends ComponentRestrictionCEImpl<OntCE, OntOPE> implements ObjectSomeValuesFrom {
        public ObjectSomeValuesFromImpl(Node n, EnhGraph m) {
            super(n, m, OWL2.someValuesFrom, OntCE.class, OntOPE.class);
        }

        @Override
        public Class<ObjectSomeValuesFrom> getActualClass() {
            return ObjectSomeValuesFrom.class;
        }
    }

    public static class DataSomeValuesFromImpl extends ComponentRestrictionCEImpl<OntDR, OntNDP> implements DataSomeValuesFrom {
        public DataSomeValuesFromImpl(Node n, EnhGraph m) {
            super(n, m, OWL2.someValuesFrom, OntDR.class, OntNDP.class);
        }

        @Override
        public Class<DataSomeValuesFrom> getActualClass() {
            return DataSomeValuesFrom.class;
        }
    }

    public static class ObjectAllValuesFromImpl extends ComponentRestrictionCEImpl<OntCE, OntOPE> implements ObjectAllValuesFrom {
        public ObjectAllValuesFromImpl(Node n, EnhGraph m) {
            super(n, m, OWL2.allValuesFrom, OntCE.class, OntOPE.class);
        }

        @Override
        public Class<ObjectAllValuesFrom> getActualClass() {
            return ObjectAllValuesFrom.class;
        }
    }

    public static class DataAllValuesFromImpl extends ComponentRestrictionCEImpl<OntDR, OntNDP> implements DataAllValuesFrom {
        public DataAllValuesFromImpl(Node n, EnhGraph m) {
            super(n, m, OWL2.allValuesFrom, OntDR.class, OntNDP.class);
        }

        @Override
        public Class<DataAllValuesFrom> getActualClass() {
            return DataAllValuesFrom.class;
        }
    }

    public static class ObjectHasValueImpl extends ComponentRestrictionCEImpl<OntIndividual, OntOPE> implements ObjectHasValue {
        public ObjectHasValueImpl(Node n, EnhGraph m) {
            super(n, m, OWL2.hasValue, OntIndividual.class, OntOPE.class);
        }

        @Override
        public Class<ObjectHasValue> getActualClass() {
            return ObjectHasValue.class;
        }
    }

    public static class DataHasValueImpl extends ComponentRestrictionCEImpl<Literal, OntNDP> implements DataHasValue {
        public DataHasValueImpl(Node n, EnhGraph m) {
            super(n, m, OWL2.hasValue, Literal.class, OntNDP.class);
        }

        @Override
        public Class<DataHasValue> getActualClass() {
            return DataHasValue.class;
        }
    }

    public static class UnionOfImpl extends ComponentsCEImpl<OntCE> implements UnionOf {
        public UnionOfImpl(Node n, EnhGraph m) {
            super(n, m, OWL2.unionOf, OntCE.class);
        }

        @Override
        public Class<UnionOf> getActualClass() {
            return UnionOf.class;
        }
    }

    public static class IntersectionOfImpl extends ComponentsCEImpl<OntCE> implements IntersectionOf {
        public IntersectionOfImpl(Node n, EnhGraph m) {
            super(n, m, OWL2.intersectionOf, OntCE.class);
        }

        @Override
        public Class<IntersectionOf> getActualClass() {
            return IntersectionOf.class;
        }
    }

    public static class OneOfImpl extends ComponentsCEImpl<OntIndividual> implements OneOf {
        public OneOfImpl(Node n, EnhGraph m) {
            super(n, m, OWL2.oneOf, OntIndividual.class);
        }

        @Override
        public Class<OneOf> getActualClass() {
            return OneOf.class;
        }
    }

    public static class DataMinCardinalityImpl extends CardinalityRestrictionCEImpl<OntDR, OntNDP> implements DataMinCardinality {
        public DataMinCardinalityImpl(Node n, EnhGraph m) {
            super(n, m, OWL2.onDataRange, OntDR.class, OntNDP.class, CardinalityType.MIN);
        }

        @Override
        public Class<DataMinCardinality> getActualClass() {
            return DataMinCardinality.class;
        }
    }

    public static class ObjectMinCardinalityImpl extends CardinalityRestrictionCEImpl<OntCE, OntOPE> implements ObjectMinCardinality {
        public ObjectMinCardinalityImpl(Node n, EnhGraph m) {
            super(n, m, OWL2.onClass, OntCE.class, OntOPE.class, CardinalityType.MIN);
        }

        @Override
        public Class<ObjectMinCardinality> getActualClass() {
            return ObjectMinCardinality.class;
        }
    }

    public static class DataMaxCardinalityImpl extends CardinalityRestrictionCEImpl<OntDR, OntNDP> implements DataMaxCardinality {
        public DataMaxCardinalityImpl(Node n, EnhGraph m) {
            super(n, m, OWL2.onDataRange, OntDR.class, OntNDP.class, CardinalityType.MAX);
        }

        @Override
        public Class<DataMaxCardinality> getActualClass() {
            return DataMaxCardinality.class;
        }
    }

    public static class ObjectMaxCardinalityImpl extends CardinalityRestrictionCEImpl<OntCE, OntOPE> implements ObjectMaxCardinality {
        public ObjectMaxCardinalityImpl(Node n, EnhGraph m) {
            super(n, m, OWL2.onClass, OntCE.class, OntOPE.class, CardinalityType.MAX);
        }

        @Override
        public Class<ObjectMaxCardinality> getActualClass() {
            return ObjectMaxCardinality.class;
        }
    }

    public static class DataCardinalityImpl extends CardinalityRestrictionCEImpl<OntDR, OntNDP> implements DataCardinality {
        public DataCardinalityImpl(Node n, EnhGraph m) {
            super(n, m, OWL2.onDataRange, OntDR.class, OntNDP.class, CardinalityType.EXACTLY);
        }

        @Override
        public Class<DataCardinality> getActualClass() {
            return DataCardinality.class;
        }
    }

    public static class ObjectCardinalityImpl extends CardinalityRestrictionCEImpl<OntCE, OntOPE> implements ObjectCardinality {
        public ObjectCardinalityImpl(Node n, EnhGraph m) {
            super(n, m, OWL2.onClass, OntCE.class, OntOPE.class, CardinalityType.EXACTLY);
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
            return getModel().getNodeAs(getPropertyResourceValue(OWL2.complementOf).asNode(), OntCE.class);
        }

        @Override
        public void setValue(OntCE c) {
            clear();
            addProperty(OWL2.complementOf, c);
        }

        void clear() {
            removeAll(OWL2.complementOf);
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
            this.predicate = OntException.notNull(predicate, "Null predicate.");
            this.view = OntException.notNull(view, "Null view.");
        }

        Stream<RDFNode> components(Property predicate) {
            return JenaUtils.asStream(getModel().listObjectsOfProperty(this, predicate).mapWith(n -> n.as(RDFList.class)))
                    .map(list -> list.asJavaList().stream()).flatMap(Function.identity()).distinct();
        }

        @Override
        public Stream<O> components() {
            return components(predicate)
                    //.filter(n -> n.canAs(view))
                    .map(n -> getModel().getNodeAs(n.asNode(), view));
        }

        void clear() {
            getModel().listObjectsOfProperty(this, predicate).mapWith(n -> n.as(RDFList.class)).forEachRemaining(RDFList::removeList);
            removeAll(predicate);
        }

        @Deprecated
        public void setComponents(Stream<O> components) {
            clear();
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
            return getRequiredOntProperty(OWL2.onProperty, propertyView);
        }

        @Override
        public void setOnProperty(P p) {
            clearProperty(OWL2.onProperty);
            addProperty(OWL2.onProperty, p);
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
            this.predicate = OntException.notNull(predicate, "Null predicate.");
            this.objectView = OntException.notNull(objectView, "Null view.");
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
            if (CardinalityType.MAX.equals(cardinalityType)) {
                return isQualified() ? OWL2.maxQualifiedCardinality : OWL2.maxCardinality;
            }
            if (CardinalityType.MIN.equals(cardinalityType)) {
                return isQualified() ? OWL2.minQualifiedCardinality : OWL2.minCardinality;
            }
            return isQualified() ? OWL2.qualifiedCardinality : OWL2.cardinality;
        }

        @Override
        public boolean isQualified() {
            O c = getValue();
            return c != null && !OWL2.Thing.equals(c) && !RDFS.Literal.equals(c);
        }
    }

    private static class OnPropertyTypeFilter implements OntFilter {
        private final RestrictionType type;

        private OnPropertyTypeFilter(RestrictionType type) {
            this.type = OntException.notNull(type, "Null restriction type.");
        }

        @Override
        public boolean test(Node n, EnhGraph g) {
            List<Node> nodes = g.asGraph().find(n, OWL_ON_PROPERTY, Node.ANY).mapWith(Triple::getObject).filterKeep(new UniqueFilter<>()).toList();
            if (nodes.size() != 1) return false;
            Node node = nodes.get(0);
            return RestrictionType.DATA.equals(type) ? OntEntityImpl.dataPropertyFactory.canWrap(node, g) : !RestrictionType.OBJECT.equals(type) || OntEntityImpl.objectPropertyFactory.canWrap(node, g);
        }
    }

    private static class CardinalityPredicateFilter implements OntFilter {
        private final CardinalityType type;

        private CardinalityPredicateFilter(CardinalityType type) {
            this.type = OntException.notNull(type, "Null cardinality type.");
        }

        @Override
        public boolean test(Node n, EnhGraph g) {
            if (CardinalityType.MAX.equals(type)) {
                return g.asGraph().contains(n, OWL2.maxQualifiedCardinality.asNode(), Node.ANY) || g.asGraph().contains(n, OWL2.maxCardinality.asNode(), Node.ANY);
            } else if (CardinalityType.MIN.equals(type)) {
                return g.asGraph().contains(n, OWL2.minQualifiedCardinality.asNode(), Node.ANY) || g.asGraph().contains(n, OWL2.minCardinality.asNode(), Node.ANY);
            }
            return g.asGraph().contains(n, OWL2.qualifiedCardinality.asNode(), Node.ANY) || g.asGraph().contains(n, OWL2.cardinality.asNode(), Node.ANY);
        }
    }

    private static class HasSelfFilter implements OntFilter {
        @Override
        public boolean test(Node n, EnhGraph g) {
            return g.asGraph().contains(n, OWL2.hasSelf.asNode(), ResourceFactory.createTypedLiteral(Boolean.TRUE).asNode());
        }
    }

    private static class HasSelfMaker extends OntMaker.WithType {
        private HasSelfMaker() {
            super(HasSelfImpl.class, OWL2.Restriction);
        }

        @Override
        public void make(Node node, EnhGraph eg) {
            super.make(node, eg);
            eg.asGraph().add(Triple.create(node, OWL2.hasSelf.asNode(), ResourceFactory.createTypedLiteral(Boolean.TRUE).asNode()));
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

}
