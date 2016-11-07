package ru.avicomp.ontapi.jena.impl;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.enhanced.EnhNode;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.*;
import org.apache.jena.util.iterator.UniqueFilter;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import ru.avicomp.ontapi.OntException;
import ru.avicomp.ontapi.jena.model.*;

/**
 * base class for any class-expression.
 * <p>
 * Created by szuev on 03.11.2016.
 */
abstract class OntCEImpl extends OntObjectImpl implements OntCE {
    public static CEFactory unionOfCEFactory = new CEFactory(UnionOfImpl.class, OWL.unionOf, OWL.Class);
    public static CEFactory intersectionOfCEFactory = new CEFactory(IntersectionOfImpl.class, OWL.intersectionOf, OWL.Class);
    public static CEFactory oneOfCEFactory = new CEFactory(OneOfImpl.class, OWL.oneOf, OWL.Class);
    public static CEFactory complementOfCEFactory = new CEFactory(ComplementOfImpl.class, OWL.complementOf, OWL.Class);

    public static RestrictionCEFactory objectSomeValuesOfCEFactory = new RestrictionCEFactory(ObjectSomeValuesFromImpl.class, OWL.someValuesFrom, RestrictionType.OBJECT);
    public static RestrictionCEFactory dataSomeValuesOfCEFactory = new RestrictionCEFactory(DataSomeValuesFromImpl.class, OWL.someValuesFrom, RestrictionType.DATA);

    public static OntConfiguration.OntObjectFactory abstractCEFactory = new OntConfiguration.OntMultiObjectFactory(
            OntEntityImpl.classFactory,
            objectSomeValuesOfCEFactory,
            dataSomeValuesOfCEFactory,
            unionOfCEFactory, intersectionOfCEFactory, oneOfCEFactory, complementOfCEFactory);

    private OntCEImpl(Node n, EnhGraph m) {
        super(n, m);
    }

    @Override
    public Stream<OntCE> subClassOf() {
        return getModel().classExpressions(this, RDFS.subClassOf);
    }

    /**
     * Base factory for any CE
     */
    private static class CEFactory extends OntConfiguration.BaseOntObjectFactory {
        protected final Node predicate;
        private final Node type;
        protected static final Node RDF_TYPE = RDF.type.asNode();

        CEFactory(Class<? extends OntObjectImpl> impl, Property predicate, Resource type) {
            super(impl);
            this.predicate = OntException.notNull(predicate, "Null predicate").asNode();
            this.type = OntException.notNull(type, "Null type").asNode();
        }

        @Override
        public boolean canWrap(Node node, EnhGraph eg) {
            return node.isBlank() && eg.asGraph().contains(node, RDF_TYPE, type) && eg.asGraph().contains(node, predicate, Node.ANY) && test(node, eg);
        }

        protected boolean test(Node node, EnhGraph eg) {
            return true;
        }

        protected void make(Node node, EnhGraph eg) {
            eg.asGraph().add(Triple.create(node, RDF_TYPE, type));
        }

        @Override
        public EnhNode create(Node node, EnhGraph eg) {
            if (!node.isBlank()) throw new OntException("Not blank node " + node);
            make(node, eg);
            return newInstance(node, eg);
        }

        @Override
        public Stream<EnhNode> find(EnhGraph eg) {
            return GraphModelImpl.asStream(eg.asGraph().find(Node.ANY, RDF_TYPE, type).mapWith(Triple::getSubject))
                    .filter(Node::isBlank)
                    .filter(n -> eg.asGraph().contains(n, predicate, Node.ANY))
                    .filter(n -> CEFactory.this.test(n, eg))
                    .distinct().map(n -> newInstance(n, eg));
        }
    }

    /**
     * factory for Object and Data Restrictions
     */
    private static class RestrictionCEFactory extends CEFactory {
        private final RestrictionType restrictionType;

        RestrictionCEFactory(Class<? extends OntObjectImpl> view, Property predicate, RestrictionType restrictionType) {
            super(view, predicate, OWL.Restriction);
            this.restrictionType = restrictionType;
        }

        @Override
        protected boolean test(Node node, EnhGraph eg) {
            return eg.asGraph().contains(node, OWL.onProperty.asNode(), Node.ANY) && testRestrictionType(node, eg);
        }

        private boolean testRestrictionType(Node node, EnhGraph eg) {
            if (RestrictionType.UNDEFINED.equals(restrictionType)) {
                return true;
            }
            List<Node> nodes = eg.asGraph().find(node, predicate, Node.ANY).mapWith(Triple::getObject).filterKeep(new UniqueFilter<>()).toList();
            if (nodes.size() != 1) {
                return false;
            }
            if (RestrictionType.DATA.equals(restrictionType)) {
                return eg.asGraph().contains(node, RDF_TYPE, RDFS.Datatype.asNode());
            }
            return eg.asGraph().contains(node, RDF_TYPE, OWL.Class.asNode()) || eg.asGraph().contains(node, RDF_TYPE, OWL.Restriction.asNode());
        }
    }

    /**
     * Abstract base method.
     *
     * @param <T>
     */
    abstract static class ComponentsCEImpl<T extends OntObject> extends OntCEImpl implements OntCE.ComponentsCE<T> {
        private final Property predicate;
        private final Class<T> view;

        ComponentsCEImpl(Node n, EnhGraph m, Property predicate, Class<T> view) {
            super(n, m);
            this.predicate = OntException.notNull(predicate, "Null predicate.");
            this.view = OntException.notNull(view, "Null view.");
        }

        protected Stream<RDFNode> components(Property predicate) {
            return GraphModelImpl.asStream(getModel().listObjectsOfProperty(this, predicate).mapWith(n -> n.as(RDFList.class)))
                    .map(list -> list.asJavaList().stream()).flatMap(Function.identity()).distinct();
        }

        @Override
        public Stream<T> components() {
            return components(predicate)
                    //.filter(n -> n.canAs(view))
                    .map(n -> getModel().getNodeAs(n.asNode(), view));
        }

        @Override
        public void clear() {
            getModel().listObjectsOfProperty(this, predicate).mapWith(n -> n.as(RDFList.class)).forEachRemaining(RDFList::removeList);
            removeAll(predicate);
        }

        @Deprecated
        public void setComponents(Stream<T> components) {
            clear();
            addProperty(predicate, getModel().createList(components.iterator()));
        }
    }

    static class UnionOfImpl extends ComponentsCEImpl<OntCE> implements OntCE.UnionOf {
        UnionOfImpl(Node n, EnhGraph m) {
            super(n, m, OWL.unionOf, OntCE.class);
        }
    }

    static class IntersectionOfImpl extends ComponentsCEImpl<OntCE> implements OntCE.IntersectionOf {
        IntersectionOfImpl(Node n, EnhGraph m) {
            super(n, m, OWL.intersectionOf, OntCE.class);
        }
    }

    static class OneOfImpl extends ComponentsCEImpl<OntIndividual> implements OntCE.OneOf {
        OneOfImpl(Node n, EnhGraph m) {
            super(n, m, OWL.oneOf, OntIndividual.class);
        }
    }

    private static class ComplementOfImpl extends OntCEImpl implements OntCE.ComplementOf {
        ComplementOfImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        public OntCE getComponent() {
            return getModel().getNodeAs(getPropertyResourceValue(OWL.complementOf).asNode(), OntCE.class);
        }

        @Override
        public void setComponent(OntCE c) {
            clear();
            addProperty(OWL.complementOf, c);
        }

        void clear() {
            removeAll(OWL.complementOf);
        }
    }

    /**
     * Abstract base method.
     *
     * @param <T>
     */
    static abstract class ComponentRestrictionCEImpl<T extends OntObject> extends OntCEImpl implements OntCE.ComponentRestrictionCE<T> {
        protected final Property predicate;
        protected final Class<T> view;

        private ComponentRestrictionCEImpl(Node n, EnhGraph m, Property predicate, Class<T> view) {
            super(n, m);
            this.predicate = OntException.notNull(predicate, "Null predicate.");
            this.view = OntException.notNull(view, "Null view.");
        }

        @Override
        public OntPE getOnProperty() {
            return getModel().getNodeAs(getRequiredProperty(OWL.onProperty).getObject().asNode(), OntPE.class);
        }

        @Override
        public void setOntProperty(OntPE p) {
            clearProperty(OWL.onProperty);
            addProperty(OWL.onProperty, p);
        }

        void clearProperty(Property p) {
            removeAll(p);
        }

        @Override
        public T getComponent() {
            return getModel().getNodeAs(getRequiredProperty(predicate).getObject().asNode(), view);
        }

        @Override
        public void setComponent(T c) {
            clearProperty(predicate);
            addProperty(predicate, c);
        }
    }

    static class ObjectSomeValuesFromImpl extends ComponentRestrictionCEImpl<OntCE> implements OntCE.ObjectSomeValuesFrom {
        ObjectSomeValuesFromImpl(Node n, EnhGraph m) {
            super(n, m, OWL.someValuesFrom, OntCE.class);
        }
    }

    static class DataSomeValuesFromImpl extends ComponentRestrictionCEImpl<OntDR> implements OntCE.DataSomeValuesFrom {
        DataSomeValuesFromImpl(Node n, EnhGraph m) {
            super(n, m, OWL.someValuesFrom, OntDR.class);
        }
    }

    static class CardinalityRestrictionCEImpl<T extends OntObject> extends ComponentRestrictionCEImpl<T> implements CardinalityRestrictionCE<T> {
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
        public T getComponent() {
            Statement st = getProperty(predicate);
            return st == null ? null : getModel().getNodeAs(st.getObject().asNode(), view);
        }

        @Override
        public Integer getCardinality() {
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
            T c = getComponent();
            return c != null && !OWL.Thing.equals(c) && !RDFS.Literal.equals(c);
        }
    }

    static class DataMinCardinalityImpl extends CardinalityRestrictionCEImpl<OntDR> implements DataMinCardinality {
        private DataMinCardinalityImpl(Node n, EnhGraph m) {
            super(n, m, OWL2.onDataRange, OntDR.class, CardinalityType.MIN);
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

    @Deprecated
    enum Type {
        CLASS(null), // anon
        OBJECT_SOME_VALUES_FROM(null), DATA_SOME_VALUES_FROM(null),
        OBJECT_ALL_VALUES_FROM(null), DATA_ALL_VALUES_FROM(null),
        OBJECT_MIN_CARDINALITY(null), DATA_MIN_CARDINALITY(null),
        OBJECT_MAX_CARDINALITY(null), DATA_MAX_CARDINALITY(null),
        OBJECT_EXACT_CARDINALITY(null), DATA_EXACT_CARDINALITY(null),
        OBJECT_HAS_VALUE(null), DATA_HAS_VALUE(null),
        // object only CE types:
        HAS_SELF(null), INTERSECTION_OF(OntCE.IntersectionOf.class), UNION_OF(OntCE.UnionOf.class), ONE_OF(OntCE.OneOf.class), COMPLEMENT_OF(null),
        // data only (n-ary, not used inside OWL-API) types:
        NARY_DATA_ALL_VALUES_FROM(null), NARY_DATA_SOME_VALUES_FROM(null),;

        Type(Class<? extends OntCE> view) {
            this.view = view;
        }

        public Class<? extends OntCE> getView() {
            return view;
        }

        private Class<? extends OntCE> view;

        public static Type valueOf(Class<? extends OntCE> view) {
            OntException.notNull(view, "Null view.");
            for (Type t : values()) {
                if (t.view.equals(view)) return t;
            }
            throw new OntException("Unsupported entity type " + view);
        }
    }
}
