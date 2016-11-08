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
import org.apache.jena.vocabulary.RDF;
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
    private static final Node RDF_TYPE = RDF.type.asNode();
    private static final Node RDFS_DATATYPE = RDFS.Datatype.asNode();
    private static final Node OWL_CLASS = OWL2.Class.asNode();
    private static final Node OWL_RESTRICTION = OWL2.Restriction.asNode();
    private static final Node OWL_ON_PROPERTY = OWL2.onProperty.asNode();
    private static final Node OWL_DATATYPE_PROPERTY = OWL2.DatatypeProperty.asNode();
    private static final Node OWL_OBJECT_PROPERTY = OWL2.ObjectProperty.asNode();

    private static final OntFilter BLANK_FILTER = (n, g) -> n.isBlank();
    private static final OntFilter ON_PROPERTY_DATA = new OnPropertyTypeFilter(RestrictionType.DATA);
    private static final OntFilter ON_PROPERTY_OBJECT = new OnPropertyTypeFilter(RestrictionType.OBJECT);

    public static OntObjectFactory unionOfCEFactory = new TypedOntObjectFactory(UnionOfImpl.class, OWL2.Class,
            BLANK_FILTER, new OntFilter.Predicate(OWL2.unionOf));
    public static OntObjectFactory intersectionOfCEFactory = new TypedOntObjectFactory(IntersectionOfImpl.class, OWL2.Class,
            BLANK_FILTER, new OntFilter.Predicate(OWL2.intersectionOf));
    public static OntObjectFactory oneOfCEFactory = new TypedOntObjectFactory(OneOfImpl.class, OWL2.Class,
            BLANK_FILTER, new OntFilter.Predicate(OWL2.oneOf));
    public static OntObjectFactory complementOfCEFactory = new TypedOntObjectFactory(ComplementOfImpl.class, OWL2.Class,
            BLANK_FILTER, new OntFilter.Predicate(OWL2.complementOf));

    public static OntObjectFactory objectSomeValuesOfCEFactory = new TypedOntObjectFactory(ObjectSomeValuesFromImpl.class, OWL2.Restriction,
            BLANK_FILTER, ON_PROPERTY_OBJECT, new OntFilter.Predicate(OWL2.someValuesFrom));
    public static OntObjectFactory dataSomeValuesOfCEFactory = new TypedOntObjectFactory(DataSomeValuesFromImpl.class, OWL2.Restriction,
            BLANK_FILTER, ON_PROPERTY_DATA, new OntFilter.Predicate(OWL2.someValuesFrom));

    public static OntObjectFactory objectAllValuesOfCEFactory = new TypedOntObjectFactory(ObjectAllValuesFromImpl.class, OWL2.Restriction,
            BLANK_FILTER, ON_PROPERTY_OBJECT, new OntFilter.Predicate(OWL2.allValuesFrom));
    public static OntObjectFactory dataAllValuesOfCEFactory = new TypedOntObjectFactory(DataAllValuesFromImpl.class, OWL2.Restriction,
            BLANK_FILTER, ON_PROPERTY_DATA, new OntFilter.Predicate(OWL2.allValuesFrom));

    public static OntObjectFactory objectHasValueCEFactory = new TypedOntObjectFactory(ObjectHasValueImpl.class, OWL2.Restriction,
            BLANK_FILTER, ON_PROPERTY_OBJECT, new OntFilter.Predicate(OWL2.hasValue));
    public static OntObjectFactory dataHasValueCEFactory = new TypedOntObjectFactory(DataHasValueImpl.class, OWL2.Restriction,
            BLANK_FILTER, ON_PROPERTY_DATA, new OntFilter.Predicate(OWL2.hasValue));

    public static OntObjectFactory dataMinCardinalityCEFactory = new TypedOntObjectFactory(DataMinCardinalityImpl.class, OWL2.Restriction,
            BLANK_FILTER, ON_PROPERTY_DATA, new CardinalityPredicateFilter(CardinalityType.MIN));
    public static OntObjectFactory objectMinCardinalityCEFactory = new TypedOntObjectFactory(ObjectMinCardinalityImpl.class, OWL2.Restriction,
            BLANK_FILTER, ON_PROPERTY_OBJECT, new CardinalityPredicateFilter(CardinalityType.MIN));

    public static OntObjectFactory dataMaxCardinalityCEFactory = new TypedOntObjectFactory(DataMaxCardinalityImpl.class, OWL2.Restriction,
            BLANK_FILTER, ON_PROPERTY_DATA, new CardinalityPredicateFilter(CardinalityType.MAX));
    public static OntObjectFactory objectMaxCardinalityCEFactory = new TypedOntObjectFactory(ObjectMaxCardinalityImpl.class, OWL2.Restriction,
            BLANK_FILTER, ON_PROPERTY_OBJECT, new CardinalityPredicateFilter(CardinalityType.MAX));

    public static OntObjectFactory dataCardinalityCEFactory = new TypedOntObjectFactory(DataCardinalityImpl.class, OWL2.Restriction,
            BLANK_FILTER, ON_PROPERTY_DATA, new CardinalityPredicateFilter(CardinalityType.EXACTLY));
    public static OntObjectFactory objectCardinalityCEFactory = new TypedOntObjectFactory(ObjectCardinalityImpl.class, OWL2.Restriction,
            BLANK_FILTER, ON_PROPERTY_OBJECT, new CardinalityPredicateFilter(CardinalityType.EXACTLY));

    public static OntObjectFactory hasSelfCEFactory = new CommonOntObjectFactory(HasSelfImpl.class, new HasSelfMaker(), new TypedOntObjectFactory.TypeFinder(OWL2.Restriction), new HasSelfFilter());

    public static OntObjectFactory abstractCEFactory = new MultiOntObjectFactory(OntEntityImpl.classFactory,
            objectSomeValuesOfCEFactory, dataSomeValuesOfCEFactory,
            objectAllValuesOfCEFactory, dataAllValuesOfCEFactory,
            objectHasValueCEFactory, dataHasValueCEFactory,
            objectMinCardinalityCEFactory, dataMinCardinalityCEFactory,
            objectMaxCardinalityCEFactory, dataMaxCardinalityCEFactory,
            objectCardinalityCEFactory, dataCardinalityCEFactory,
            unionOfCEFactory, intersectionOfCEFactory, oneOfCEFactory, complementOfCEFactory, hasSelfCEFactory);

    public static OntObjectFactory abstractComponentsCEFactory = new MultiOntObjectFactory(unionOfCEFactory, intersectionOfCEFactory, oneOfCEFactory);

    public static OntObjectFactory abstractCardinalityRestrictionCEFactory = new MultiOntObjectFactory(
            objectMinCardinalityCEFactory, dataMinCardinalityCEFactory,
            objectMaxCardinalityCEFactory, dataMaxCardinalityCEFactory,
            objectCardinalityCEFactory, dataCardinalityCEFactory);

    public static MultiOntObjectFactory abstractComponentRestrictionCEFactory = new MultiOntObjectFactory(
            objectSomeValuesOfCEFactory, dataSomeValuesOfCEFactory,
            objectAllValuesOfCEFactory, dataAllValuesOfCEFactory,
            objectHasValueCEFactory, dataHasValueCEFactory,
            objectMinCardinalityCEFactory, dataMinCardinalityCEFactory,
            objectMaxCardinalityCEFactory, dataMaxCardinalityCEFactory,
            objectCardinalityCEFactory, dataCardinalityCEFactory);

    // todo: + nary restrictions:
    public static OntObjectFactory abstractRestrictionCEFactory = new MultiOntObjectFactory(
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
    public abstract Class<? extends OntCE> getActualClass();

    public static class ObjectSomeValuesFromImpl extends ComponentRestrictionCEImpl<OntCE> implements ObjectSomeValuesFrom {
        public ObjectSomeValuesFromImpl(Node n, EnhGraph m) {
            super(n, m, OWL2.someValuesFrom, OntCE.class);
        }

        @Override
        public Class<ObjectSomeValuesFrom> getActualClass() {
            return ObjectSomeValuesFrom.class;
        }
    }

    public static class DataSomeValuesFromImpl extends ComponentRestrictionCEImpl<OntDR> implements DataSomeValuesFrom {
        public DataSomeValuesFromImpl(Node n, EnhGraph m) {
            super(n, m, OWL2.someValuesFrom, OntDR.class);
        }

        @Override
        public Class<DataSomeValuesFrom> getActualClass() {
            return DataSomeValuesFrom.class;
        }
    }

    public static class ObjectAllValuesFromImpl extends ComponentRestrictionCEImpl<OntCE> implements ObjectAllValuesFrom {
        public ObjectAllValuesFromImpl(Node n, EnhGraph m) {
            super(n, m, OWL2.allValuesFrom, OntCE.class);
        }

        @Override
        public Class<ObjectAllValuesFrom> getActualClass() {
            return ObjectAllValuesFrom.class;
        }
    }

    public static class DataAllValuesFromImpl extends ComponentRestrictionCEImpl<OntDR> implements DataAllValuesFrom {
        public DataAllValuesFromImpl(Node n, EnhGraph m) {
            super(n, m, OWL2.allValuesFrom, OntDR.class);
        }

        @Override
        public Class<DataAllValuesFrom> getActualClass() {
            return DataAllValuesFrom.class;
        }
    }

    public static class ObjectHasValueImpl extends ComponentRestrictionCEImpl<OntIndividual> implements ObjectHasValue {
        public ObjectHasValueImpl(Node n, EnhGraph m) {
            super(n, m, OWL2.hasValue, OntIndividual.class);
        }

        @Override
        public Class<ObjectHasValue> getActualClass() {
            return ObjectHasValue.class;
        }
    }

    public static class DataHasValueImpl extends ComponentRestrictionCEImpl<Literal> implements DataHasValue {
        public DataHasValueImpl(Node n, EnhGraph m) {
            super(n, m, OWL2.hasValue, Literal.class);
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

    public static class DataMinCardinalityImpl extends CardinalityRestrictionCEImpl<OntDR> implements DataMinCardinality {
        public DataMinCardinalityImpl(Node n, EnhGraph m) {
            super(n, m, OWL2.onDataRange, OntDR.class, CardinalityType.MIN);
        }

        @Override
        public Class<DataMinCardinality> getActualClass() {
            return DataMinCardinality.class;
        }
    }

    public static class ObjectMinCardinalityImpl extends CardinalityRestrictionCEImpl<OntCE> implements ObjectMinCardinality {
        public ObjectMinCardinalityImpl(Node n, EnhGraph m) {
            super(n, m, OWL2.onClass, OntCE.class, CardinalityType.MIN);
        }

        @Override
        public Class<ObjectMinCardinality> getActualClass() {
            return ObjectMinCardinality.class;
        }
    }

    public static class DataMaxCardinalityImpl extends CardinalityRestrictionCEImpl<OntDR> implements DataMaxCardinality {
        public DataMaxCardinalityImpl(Node n, EnhGraph m) {
            super(n, m, OWL2.onDataRange, OntDR.class, CardinalityType.MAX);
        }

        @Override
        public Class<DataMaxCardinality> getActualClass() {
            return DataMaxCardinality.class;
        }
    }

    public static class ObjectMaxCardinalityImpl extends CardinalityRestrictionCEImpl<OntCE> implements ObjectMaxCardinality {
        public ObjectMaxCardinalityImpl(Node n, EnhGraph m) {
            super(n, m, OWL2.onClass, OntCE.class, CardinalityType.MAX);
        }

        @Override
        public Class<ObjectMaxCardinality> getActualClass() {
            return ObjectMaxCardinality.class;
        }
    }

    public static class DataCardinalityImpl extends CardinalityRestrictionCEImpl<OntDR> implements DataCardinality {
        public DataCardinalityImpl(Node n, EnhGraph m) {
            super(n, m, OWL2.onDataRange, OntDR.class, CardinalityType.EXACTLY);
        }

        @Override
        public Class<DataCardinality> getActualClass() {
            return DataCardinality.class;
        }
    }

    public static class ObjectCardinalityImpl extends CardinalityRestrictionCEImpl<OntCE> implements ObjectCardinality {
        public ObjectCardinalityImpl(Node n, EnhGraph m) {
            super(n, m, OWL2.onClass, OntCE.class, CardinalityType.EXACTLY);
        }

        @Override
        public Class<ObjectCardinality> getActualClass() {
            return ObjectCardinality.class;
        }
    }

    public static class HasSelfImpl extends OnPropertyRestrictionCEImpl implements HasSelf {
        public HasSelfImpl(Node n, EnhGraph m) {
            super(n, m);
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
     * @param <T>
     */
    abstract static class ComponentsCEImpl<T extends OntObject> extends OntCEImpl implements ComponentsCE<T> {
        private final Property predicate;
        private final Class<T> view;

        ComponentsCEImpl(Node n, EnhGraph m, Property predicate, Class<T> view) {
            super(n, m);
            this.predicate = OntException.notNull(predicate, "Null predicate.");
            this.view = OntException.notNull(view, "Null view.");
        }

        Stream<RDFNode> components(Property predicate) {
            return JenaUtils.asStream(getModel().listObjectsOfProperty(this, predicate).mapWith(n -> n.as(RDFList.class)))
                    .map(list -> list.asJavaList().stream()).flatMap(Function.identity()).distinct();
        }

        @Override
        public Stream<T> components() {
            return components(predicate)
                    //.filter(n -> n.canAs(view))
                    .map(n -> getModel().getNodeAs(n.asNode(), view));
        }

        void clear() {
            getModel().listObjectsOfProperty(this, predicate).mapWith(n -> n.as(RDFList.class)).forEachRemaining(RDFList::removeList);
            removeAll(predicate);
        }

        @Deprecated
        public void setComponents(Stream<T> components) {
            clear();
            addProperty(predicate, getModel().createList(components.iterator()));
        }
    }

    /**
     * for any restriction with owl:onProperty
     */
    static abstract class OnPropertyRestrictionCEImpl extends OntCEImpl {
        private OnPropertyRestrictionCEImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        public OntPE getOnProperty() {
            return getModel().getNodeAs(getRequiredProperty(OWL2.onProperty).getObject().asNode(), OntPE.class);
        }

        public void setOnProperty(OntPE p) {
            clearProperty(OWL2.onProperty);
            addProperty(OWL2.onProperty, p);
        }

        void clearProperty(Property p) {
            removeAll(p);
        }
    }

    /**
     * Abstract base component-restriction class.
     * It's for CE which has owl:onProperty and some component also (with predicate owl:dataRange,owl:onClass, owl:someValuesFrom, owl:allValuesFrom)
     *
     * @param <T>
     */
    static abstract class ComponentRestrictionCEImpl<T extends RDFNode> extends OnPropertyRestrictionCEImpl implements ComponentRestrictionCE<T> {
        protected final Property predicate;
        final Class<T> view;

        private ComponentRestrictionCEImpl(Node n, EnhGraph m, Property predicate, Class<T> view) {
            super(n, m);
            this.predicate = OntException.notNull(predicate, "Null predicate.");
            this.view = OntException.notNull(view, "Null view.");
        }

        @Override
        public T getValue() {
            return getModel().getNodeAs(getRequiredProperty(predicate).getObject().asNode(), view);
        }

        @Override
        public void setValue(T c) {
            clearProperty(predicate);
            addProperty(predicate, c);
        }
    }

    static abstract class CardinalityRestrictionCEImpl<T extends OntObject> extends ComponentRestrictionCEImpl<T> implements CardinalityRestrictionCE<T> {
        private final CardinalityType cardinalityType;

        /**
         * @param n               Node
         * @param m               Model
         * @param predicate       can be owl:onDataRange or owl:onClass
         * @param view            interface class
         * @param cardinalityType type of cardinality.
         */
        private CardinalityRestrictionCEImpl(Node n, EnhGraph m, Property predicate, Class<T> view, CardinalityType cardinalityType) {
            super(n, m, predicate, view);
            this.cardinalityType = cardinalityType;
        }

        @Override
        public T getValue() {
            Statement st = getProperty(predicate);
            return st == null ? null : getModel().getNodeAs(st.getObject().asNode(), view);
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
            T c = getValue();
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
            return nodes.size() == 1 && (RestrictionType.UNDEFINED.equals(type) || g.asGraph().contains(nodes.get(0), RDF_TYPE, RestrictionType.DATA.equals(type) ? OWL_DATATYPE_PROPERTY : OWL_OBJECT_PROPERTY));
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

    private static class HasSelfMaker extends TypedOntObjectFactory.TypeMaker {
        private HasSelfMaker() {
            super(OWL2.Restriction);
        }

        @Override
        public void prepare(Node node, EnhGraph eg) {
            super.prepare(node, eg);
            eg.asGraph().add(Triple.create(node, OWL2.hasSelf.asNode(), ResourceFactory.createTypedLiteral(Boolean.TRUE).asNode()));
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
