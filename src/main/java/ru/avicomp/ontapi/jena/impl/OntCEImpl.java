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
import ru.avicomp.ontapi.jena.utils.Streams;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

/**
 * base class for any class-expression.
 * <p>
 * Created by szuev on 03.11.2016.
 */
public abstract class OntCEImpl extends OntObjectImpl implements OntCE {

    private static final OntFinder CE_FINDER = g -> Streams.asStream(g.asGraph().find(Node.ANY, RDF.type.asNode(), OWL.Class.asNode()).
            andThen(g.asGraph().find(Node.ANY, RDF.type.asNode(), OWL.Restriction.asNode())).mapWith(Triple::getSubject));

    public static OntObjectFactory unionOfCEFactory = new CEFactory(UnionOfImpl.class, OWL.unionOf);
    public static OntObjectFactory intersectionOfCEFactory = new CEFactory(IntersectionOfImpl.class, OWL.intersectionOf);
    public static OntObjectFactory oneOfCEFactory = new CEFactory(OneOfImpl.class, OWL.oneOf);
    public static OntObjectFactory complementOfCEFactory = new CEFactory(ComplementOfImpl.class, OWL.complementOf);

    public static OntObjectFactory objectSomeValuesOfCEFactory = new RestrictionFactory(ObjectSomeValuesFromImpl.class,
            RestrictionType.OBJECT, OWL.someValuesFrom, false);
    public static OntObjectFactory objectSomeValuesOfCEFactoryStrict = new RestrictionFactory(ObjectSomeValuesFromImpl.class,
            RestrictionType.OBJECT, OWL.someValuesFrom, true);
    public static OntObjectFactory dataSomeValuesOfCEFactory = new RestrictionFactory(DataSomeValuesFromImpl.class,
            RestrictionType.DATA, OWL.someValuesFrom, false);
    public static OntObjectFactory dataSomeValuesOfCEFactoryStrict = new RestrictionFactory(DataSomeValuesFromImpl.class,
            RestrictionType.DATA, OWL.someValuesFrom, true);

    public static OntObjectFactory objectAllValuesOfCEFactory = new RestrictionFactory(ObjectAllValuesFromImpl.class,
            RestrictionType.OBJECT, OWL.allValuesFrom, false);
    public static OntObjectFactory objectAllValuesOfCEFactoryStrict = new RestrictionFactory(ObjectAllValuesFromImpl.class,
            RestrictionType.OBJECT, OWL.allValuesFrom, true);
    public static OntObjectFactory dataAllValuesOfCEFactory = new RestrictionFactory(DataAllValuesFromImpl.class,
            RestrictionType.DATA, OWL.allValuesFrom, false);
    public static OntObjectFactory dataAllValuesOfCEFactoryStrict = new RestrictionFactory(DataAllValuesFromImpl.class,
            RestrictionType.DATA, OWL.allValuesFrom, true);

    public static OntObjectFactory objectHasValueCEFactory = new RestrictionFactory(ObjectHasValueImpl.class,
            RestrictionType.OBJECT, OWL.hasValue, false);
    public static OntObjectFactory objectHasValueCEFactoryStrict = new RestrictionFactory(ObjectHasValueImpl.class,
            RestrictionType.OBJECT, OWL.hasValue, true);
    public static OntObjectFactory dataHasValueCEFactory = new RestrictionFactory(DataHasValueImpl.class,
            RestrictionType.DATA, OWL.hasValue, false);
    public static OntObjectFactory dataHasValueCEFactoryStrict = new RestrictionFactory(DataHasValueImpl.class,
            RestrictionType.DATA, OWL.hasValue, true);

    public static OntObjectFactory dataMinCardinalityCEFactory = new RestrictionFactory(DataMinCardinalityImpl.class,
            RestrictionType.DATA, CardinalityType.MIN, false);
    public static OntObjectFactory dataMinCardinalityCEFactoryStrict = new RestrictionFactory(DataMinCardinalityImpl.class,
            RestrictionType.DATA, CardinalityType.MIN, true);
    public static OntObjectFactory objectMinCardinalityCEFactory = new RestrictionFactory(ObjectMinCardinalityImpl.class,
            RestrictionType.OBJECT, CardinalityType.MIN, false);
    public static OntObjectFactory objectMinCardinalityCEFactoryStrict = new RestrictionFactory(ObjectMinCardinalityImpl.class,
            RestrictionType.OBJECT, CardinalityType.MIN, true);

    public static OntObjectFactory dataMaxCardinalityCEFactory = new RestrictionFactory(DataMaxCardinalityImpl.class,
            RestrictionType.DATA, CardinalityType.MAX, false);
    public static OntObjectFactory dataMaxCardinalityCEFactoryStrict = new RestrictionFactory(DataMaxCardinalityImpl.class,
            RestrictionType.DATA, CardinalityType.MAX, true);
    public static OntObjectFactory objectMaxCardinalityCEFactory = new RestrictionFactory(ObjectMaxCardinalityImpl.class,
            RestrictionType.OBJECT, CardinalityType.MAX, false);
    public static OntObjectFactory objectMaxCardinalityCEFactoryStrict = new RestrictionFactory(ObjectMaxCardinalityImpl.class,
            RestrictionType.OBJECT, CardinalityType.MAX, true);

    public static OntObjectFactory dataCardinalityCEFactory = new RestrictionFactory(DataCardinalityImpl.class,
            RestrictionType.DATA, CardinalityType.EXACTLY, false);
    public static OntObjectFactory dataCardinalityCEFactoryStrict = new RestrictionFactory(DataCardinalityImpl.class,
            RestrictionType.DATA, CardinalityType.EXACTLY, true);
    public static OntObjectFactory objectCardinalityCEFactory = new RestrictionFactory(ObjectCardinalityImpl.class,
            RestrictionType.OBJECT, CardinalityType.EXACTLY, false);
    public static OntObjectFactory objectCardinalityCEFactoryStrict = new RestrictionFactory(ObjectCardinalityImpl.class,
            RestrictionType.OBJECT, CardinalityType.EXACTLY, true);


    public static OntObjectFactory hasSelfCEFactory = new CommonOntObjectFactory(new HasSelfMaker(), new OntFinder.ByType(OWL.Restriction),
            OntFilter.BLANK, new HasSelfFilter());

    /**
     * todo:
     * see <a href='https://www.w3.org/TR/owl2-quick-reference/#Class_Expressions'>Restrictions Using n-ary Data Range</a>
     */
    public static OntObjectFactory naryDataAllValuesFromCEFactory = null;
    public static OntObjectFactory naryDataSomeValuesFromCEFactory = null;

    //Boolean Connectives and Enumeration of Individuals
    public static MultiOntObjectFactory abstractComponentsCEFactory = new MultiOntObjectFactory(CE_FINDER,
            unionOfCEFactory, intersectionOfCEFactory, oneOfCEFactory);
    public static MultiOntObjectFactory abstractNoneRestrictionCEFactory = abstractComponentsCEFactory.concat(complementOfCEFactory);

    public static MultiOntObjectFactory abstractCardinalityRestrictionCEFactory = new MultiOntObjectFactory(CE_FINDER,
            objectMinCardinalityCEFactory, dataMinCardinalityCEFactory,
            objectMaxCardinalityCEFactory, dataMaxCardinalityCEFactory,
            objectCardinalityCEFactory, dataCardinalityCEFactory);
    public static MultiOntObjectFactory abstractCardinalityRestrictionCEFactoryStrict = new MultiOntObjectFactory(CE_FINDER,
            objectMinCardinalityCEFactoryStrict, dataMinCardinalityCEFactoryStrict,
            objectMaxCardinalityCEFactoryStrict, dataMaxCardinalityCEFactoryStrict,
            objectCardinalityCEFactoryStrict, dataCardinalityCEFactoryStrict);

    public static MultiOntObjectFactory abstractComponentRestrictionCEFactory = abstractCardinalityRestrictionCEFactory.concat(
            objectSomeValuesOfCEFactory, dataSomeValuesOfCEFactory,
            objectAllValuesOfCEFactory, dataAllValuesOfCEFactory,
            objectHasValueCEFactory, dataHasValueCEFactory);
    public static MultiOntObjectFactory abstractComponentRestrictionCEFactoryStrict = abstractCardinalityRestrictionCEFactoryStrict.concat(
            objectSomeValuesOfCEFactoryStrict, dataSomeValuesOfCEFactoryStrict,
            objectAllValuesOfCEFactoryStrict, dataAllValuesOfCEFactoryStrict,
            objectHasValueCEFactoryStrict, dataHasValueCEFactoryStrict);


    // todo: add nary restrictions:
    public static OntObjectFactory abstractRestrictionCEFactory = abstractComponentRestrictionCEFactory.concat(hasSelfCEFactory);
    public static OntObjectFactory abstractRestrictionCEFactoryStrict = abstractComponentRestrictionCEFactoryStrict.concat(hasSelfCEFactory);

    public static MultiOntObjectFactory abstractAnonymousCEFactory = abstractNoneRestrictionCEFactory.concat(abstractRestrictionCEFactory);
    public static MultiOntObjectFactory abstractAnonymousCEFactoryStrict = abstractNoneRestrictionCEFactory.concat(abstractRestrictionCEFactoryStrict);

    public static OntObjectFactory abstractCEFactory = abstractAnonymousCEFactory.concat(OntEntityImpl.classFactory);
    public static OntObjectFactory abstractCEFactoryStrict = abstractAnonymousCEFactoryStrict.concat(OntEntityImpl.classFactoryStrict);


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
            return cardinalityType.getPredicate(isQualified());
        }

        @Override
        public boolean isQualified() {
            return isQualified(getValue());
        }
    }

    private enum RestrictionType {
        DATA(OntEntityImpl.dataPropertyFactory, OntEntityImpl.dataPropertyFactoryStrict),
        OBJECT(OntPEImpl.abstractOPEFactory, OntPEImpl.abstractOPEFactoryStrict),;
        private final OntObjectFactory factory, strictFactory;

        RestrictionType(OntObjectFactory factory, OntObjectFactory alternative) {
            this.factory = factory;
            this.strictFactory = alternative;
        }

        private OntObjectFactory choose(boolean strict) {
            return strict ? strictFactory : factory;
        }

        public OntFilter getFilter(boolean strict) {
            return (n, g) -> {
                List<Node> nodes = g.asGraph().find(n, OWL.onProperty.asNode(), Node.ANY).mapWith(Triple::getObject).filterKeep(new UniqueFilter<>()).toList();
                if (nodes.size() != 1) return false;
                Node node = nodes.get(0);
                return choose(strict).canWrap(node, g);
            };
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
        private CEFactory(Class<? extends OntObjectImpl> impl, Property predicate) {
            super(new OntMaker.WithType(impl, OWL.Class), new OntFinder.ByType(OWL.Class), makeCEFilter(predicate));
        }

        private static OntFilter makeCEFilter(Property predicate) {
            return OntFilter.BLANK.and(new OntFilter.HasType(OWL.Class)).and(new OntFilter.HasPredicate(predicate));
        }
    }

    private static class RestrictionFactory extends CommonOntObjectFactory {
        private RestrictionFactory(Class<? extends OntObjectImpl> impl, RestrictionType restrictionType, CardinalityType cardinalityType, boolean strict) {
            super(new OntMaker.WithType(impl, OWL.Restriction), new OntFinder.ByType(OWL.Restriction), makeCEFilter(cardinalityType, restrictionType, strict));
        }

        private RestrictionFactory(Class<? extends OntObjectImpl> impl, RestrictionType restrictionType, Property predicate, boolean strict) {
            super(new OntMaker.WithType(impl, OWL.Restriction), new OntFinder.ByType(OWL.Restriction), makeCEFilter(predicate, restrictionType, strict));
        }

        private static OntFilter makeCEFilter() {
            return OntFilter.BLANK.and(new OntFilter.HasType(OWL.Restriction));
        }

        private static OntFilter makeCEFilter(CardinalityType cardinalityType, RestrictionType restrictionType, boolean strict) {
            return makeCEFilter().and(cardinalityType.getFilter()).and(restrictionType.getFilter(strict));
        }

        private static OntFilter makeCEFilter(Property predicate, RestrictionType restrictionType, boolean strict) {
            return makeCEFilter().and(new OntFilter.HasPredicate(predicate)).and(restrictionType.getFilter(strict));
        }
    }

    protected static boolean isQualified(OntObject c) {
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
