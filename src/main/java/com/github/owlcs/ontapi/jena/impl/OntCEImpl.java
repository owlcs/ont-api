/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2022, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.jena.impl;

import com.github.owlcs.ontapi.jena.OntJenaException;
import com.github.owlcs.ontapi.jena.impl.conf.*;
import com.github.owlcs.ontapi.jena.model.*;
import com.github.owlcs.ontapi.jena.utils.Iter;
import com.github.owlcs.ontapi.jena.utils.Models;
import com.github.owlcs.ontapi.jena.vocabulary.OWL;
import com.github.owlcs.ontapi.jena.vocabulary.RDF;
import com.github.owlcs.ontapi.jena.vocabulary.XSD;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.enhanced.EnhNode;
import org.apache.jena.enhanced.Implementation;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.RDFListImpl;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDFS;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.owlcs.ontapi.jena.impl.WrappedFactoryImpl.of;

/**
 * A base class for any class-expression implementation.
 * <p>
 * Created by szuev on 03.11.2016.
 */
@SuppressWarnings("WeakerAccess")
public abstract class OntCEImpl extends OntObjectImpl implements OntClass {

    public static final OntFinder CLASS_FINDER = new OntFinder.ByType(OWL.Class);
    public static final OntFinder RESTRICTION_FINDER = new OntFinder.ByType(OWL.Restriction);
    public static final OntFilter RESTRICTION_FILTER = OntFilter.BLANK.and(new OntFilter.HasType(OWL.Restriction));

    public static ObjectFactory unionOfCEFactory = createCEFactory(UnionOfImpl.class, OWL.unionOf, RDFList.class);
    public static ObjectFactory intersectionOfCEFactory = createCEFactory(IntersectionOfImpl.class,
            OWL.intersectionOf, RDFList.class);
    public static ObjectFactory oneOfCEFactory = createCEFactory(OneOfImpl.class, OWL.oneOf, RDFList.class);
    public static ObjectFactory complementOfCEFactory = createCEFactory(ComplementOfImpl.class,
            OWL.complementOf, OntClass.class);

    public static ObjectFactory objectSomeValuesOfCEFactory = createRestrictionFactory(ObjectSomeValuesFromImpl.class,
            RestrictionType.OBJECT, ObjectRestrictionType.CLASS, OWL.someValuesFrom);
    public static ObjectFactory dataSomeValuesOfCEFactory = createRestrictionFactory(DataSomeValuesFromImpl.class,
            RestrictionType.DATA, ObjectRestrictionType.DATA_RANGE, OWL.someValuesFrom);

    public static ObjectFactory objectAllValuesOfCEFactory = createRestrictionFactory(ObjectAllValuesFromImpl.class,
            RestrictionType.OBJECT, ObjectRestrictionType.CLASS, OWL.allValuesFrom);
    public static ObjectFactory dataAllValuesOfCEFactory = createRestrictionFactory(DataAllValuesFromImpl.class,
            RestrictionType.DATA, ObjectRestrictionType.DATA_RANGE, OWL.allValuesFrom);

    public static ObjectFactory objectHasValueCEFactory = createRestrictionFactory(ObjectHasValueImpl.class,
            RestrictionType.OBJECT, ObjectRestrictionType.INDIVIDUAL, OWL.hasValue);
    public static ObjectFactory dataHasValueCEFactory = createRestrictionFactory(DataHasValueImpl.class,
            RestrictionType.DATA, ObjectRestrictionType.LITERAL, OWL.hasValue);

    public static ObjectFactory dataMinCardinalityCEFactory = createRestrictionFactory(DataMinCardinalityImpl.class,
            RestrictionType.DATA, ObjectRestrictionType.DATA_RANGE, CardinalityType.MIN);
    public static ObjectFactory objectMinCardinalityCEFactory = createRestrictionFactory(ObjectMinCardinalityImpl.class,
            RestrictionType.OBJECT, ObjectRestrictionType.CLASS, CardinalityType.MIN);

    public static ObjectFactory dataMaxCardinalityCEFactory = createRestrictionFactory(DataMaxCardinalityImpl.class,
            RestrictionType.DATA, ObjectRestrictionType.DATA_RANGE, CardinalityType.MAX);
    public static ObjectFactory objectMaxCardinalityCEFactory = createRestrictionFactory(ObjectMaxCardinalityImpl.class,
            RestrictionType.OBJECT, ObjectRestrictionType.CLASS, CardinalityType.MAX);

    public static ObjectFactory dataCardinalityCEFactory = createRestrictionFactory(DataCardinalityImpl.class,
            RestrictionType.DATA, ObjectRestrictionType.DATA_RANGE, CardinalityType.EXACTLY);
    public static ObjectFactory objectCardinalityCEFactory = createRestrictionFactory(ObjectCardinalityImpl.class,
            RestrictionType.OBJECT, ObjectRestrictionType.CLASS, CardinalityType.EXACTLY);

    public static ObjectFactory hasSelfCEFactory = Factories.createCommon(new HasSelfMaker(),
            RESTRICTION_FINDER, OntFilter.BLANK.and(new HasSelfFilter()));

    //see <a href='https://www.w3.org/TR/owl2-quick-reference/#Class_Expressions'>Restrictions Using n-ary Data Range</a>
    public static ObjectFactory naryDataAllValuesFromCEFactory = createNaryFactory(NaryDataAllValuesFromImpl.class,
            OWL.allValuesFrom);
    public static ObjectFactory naryDataSomeValuesFromCEFactory = createNaryFactory(NaryDataSomeValuesFromImpl.class,
            OWL.someValuesFrom);

    //Boolean Connectives and Enumeration of Individuals:
    public static ObjectFactory abstractComponentsCEFactory = Factories.createFrom(CLASS_FINDER
            , UnionOf.class
            , IntersectionOf.class
            , OneOf.class);

    // Cardinality Restrictions:
    public static ObjectFactory abstractCardinalityRestrictionCEFactory = Factories.createFrom(RESTRICTION_FINDER
            , ObjectMaxCardinality.class
            , DataMaxCardinality.class
            , ObjectMinCardinality.class
            , DataMinCardinality.class
            , ObjectCardinality.class
            , DataCardinality.class);

    // Cardinality + Existential/Universal Restrictions + Value Restrictions:
    public static ObjectFactory abstractComponentRestrictionCEFactory = Factories.createFrom(RESTRICTION_FINDER
            , ObjectMaxCardinality.class
            , DataMaxCardinality.class
            , ObjectMinCardinality.class
            , DataMinCardinality.class
            , ObjectCardinality.class
            , DataCardinality.class
            , ObjectSomeValuesFrom.class
            , DataSomeValuesFrom.class
            , ObjectAllValuesFrom.class
            , DataAllValuesFrom.class
            , ObjectHasValue.class
            , DataHasValue.class);

    // Cardinality + Existential/Universal Restrictions + Local reflexivity (hasSelf) + Value Restrictions
    // (all them have owl:onProperty):
    public static ObjectFactory abstractPropertyRestrictionCEFactory = Factories.createFrom(RESTRICTION_FINDER
            , ObjectMaxCardinality.class
            , DataMaxCardinality.class
            , ObjectMinCardinality.class
            , DataMinCardinality.class
            , ObjectCardinality.class
            , DataCardinality.class
            , ObjectSomeValuesFrom.class
            , DataSomeValuesFrom.class
            , ObjectAllValuesFrom.class
            , DataAllValuesFrom.class
            , ObjectHasValue.class
            , DataHasValue.class
            , HasSelf.class);

    // Cardinality + Existential/Universal Restrictions + N-ary existential/universal +
    // Local reflexivity (hasSelf) + Value Restrictions:
    public static ObjectFactory abstractRestrictionCEFactory = Factories.createFrom(RESTRICTION_FINDER
            , ObjectMaxCardinality.class
            , DataMaxCardinality.class
            , ObjectMinCardinality.class
            , DataMinCardinality.class
            , ObjectCardinality.class
            , DataCardinality.class
            , ObjectSomeValuesFrom.class
            , DataSomeValuesFrom.class
            , ObjectAllValuesFrom.class
            , DataAllValuesFrom.class
            , ObjectHasValue.class
            , DataHasValue.class
            , NaryDataSomeValuesFrom.class
            , NaryDataAllValuesFrom.class
            , HasSelf.class);

    // All:
    public static ObjectFactory abstractCEFactory = ClassExpressionFactory.createFactory();

    public OntCEImpl(Node n, EnhGraph m) {
        super(n, m);
    }

    protected static ObjectFactory createCEFactory(Class<? extends OntCEImpl> impl,
                                                   Property predicate,
                                                   Class<? extends RDFNode> view) {
        OntMaker maker = new OntMaker.WithType(impl, OWL.Class);
        OntFilter filter = OntFilter.BLANK.and(new OntFilter.HasType(OWL.Class))
                .and((n, g) -> {
                    ExtendedIterator<Triple> res = g.asGraph().find(n, predicate.asNode(), Node.ANY);
                    try {
                        while (res.hasNext()) {
                            if (PersonalityModel.canAs(view, res.next().getObject(), g)) return true;
                        }
                    } finally {
                        res.close();
                    }
                    return false;
                });
        return Factories.createCommon(maker, CLASS_FINDER, filter);
    }

    protected static ObjectFactory createRestrictionFactory(Class<? extends CardinalityRestrictionCEImpl<?, ?, ?>> impl,
                                                            RestrictionType restrictionType,
                                                            ObjectRestrictionType objectType,
                                                            CardinalityType cardinalityType) {
        OntMaker maker = new OntMaker.WithType(impl, OWL.Restriction);
        OntFilter filter = RESTRICTION_FILTER
                .and(cardinalityType.getFilter(objectType.view()))
                .and(restrictionType.getFilter());
        return Factories.createCommon(maker, RESTRICTION_FINDER, filter);
    }

    protected static ObjectFactory createRestrictionFactory(Class<? extends ComponentRestrictionCEImpl<?, ?, ?>> impl,
                                                            RestrictionType propertyType,
                                                            ObjectRestrictionType objectType,
                                                            Property predicate) {
        OntMaker maker = new OntMaker.WithType(impl, OWL.Restriction);
        OntFilter filter = RESTRICTION_FILTER
                .and(propertyType.getFilter())
                .and(objectType.getFilter(predicate));
        return Factories.createCommon(maker, RESTRICTION_FINDER, filter);
    }

    protected static ObjectFactory createNaryFactory(Class<? extends NaryRestrictionCEImpl<?, ?, ?>> impl,
                                                     Property predicate) {
        OntMaker maker = new OntMaker.WithType(impl, OWL.Restriction);
        OntFilter filter = RESTRICTION_FILTER
                .and(new OntFilter.HasPredicate(OWL.onProperties))
                .and(new OntFilter.HasPredicate(predicate));
        return Factories.createCommon(maker, RESTRICTION_FINDER, filter);
    }

    public static boolean isQualified(OntObject c) {
        return c != null && !(OWL.Thing.equals(c) || RDFS.Literal.equals(c));
    }

    protected static CardinalityType getCardinalityType(Class<? extends CardinalityRestrictionCE<?, ?>> view) {
        if (ObjectMinCardinality.class.equals(view) || DataMinCardinality.class.equals(view)) {
            return CardinalityType.MIN;
        }
        if (ObjectMaxCardinality.class.equals(view) || DataMaxCardinality.class.equals(view)) {
            return CardinalityType.MAX;
        }
        return CardinalityType.EXACTLY;
    }

    protected static Literal createNonNegativeIntegerLiteral(int n) {
        if (n < 0) throw new OntJenaException.IllegalArgument("Can't accept negative value: " + n);
        return ResourceFactory.createTypedLiteral(String.valueOf(n), CardinalityType.NON_NEGATIVE_INTEGER);
    }

    private static Resource createRestriction(OntModel model) {
        return model.createResource(OWL.Restriction);
    }

    protected static Resource createOnPropertyRestriction(OntGraphModelImpl model, OntProperty onProperty) {
        OntJenaException.notNull(onProperty, "Null property.");
        return createRestriction(model).addProperty(OWL.onProperty, onProperty);
    }

    public static <CE extends ComponentRestrictionCE<?, ?>> CE createComponentRestrictionCE(OntGraphModelImpl model,
                                                                                            Class<CE> view,
                                                                                            OntProperty onProperty,
                                                                                            RDFNode other,
                                                                                            Property predicate) {
        OntJenaException.notNull(other, "Null expression.");
        Resource res = createOnPropertyRestriction(model, onProperty).addProperty(predicate, other);
        return model.getNodeAs(res.asNode(), view);
    }

    public static <CE extends CardinalityRestrictionCE<?, ?>> CE createCardinalityRestrictionCE(OntGraphModelImpl model,
                                                                                                Class<CE> view,
                                                                                                OntProperty onProperty,
                                                                                                int cardinality,
                                                                                                OntObject object) {
        Literal value = createNonNegativeIntegerLiteral(cardinality);
        Resource res = createOnPropertyRestriction(model, onProperty);
        boolean qualified = isQualified(object);
        model.add(res, getCardinalityType(view).getPredicate(qualified), value);
        if (qualified) {
            model.add(res, onProperty instanceof OntObjectProperty ? OWL.onClass : OWL.onDataRange, object);
        }
        return model.getNodeAs(res.asNode(), view);
    }

    public static <CE extends NaryRestrictionCE<?, ?>> CE createNaryRestrictionCE(OntGraphModelImpl model,
                                                                                  Class<CE> type,
                                                                                  OntDataRange dr,
                                                                                  Collection<OntDataProperty> properties) {
        NaryRestrictionCEImpl.validateArity(dr, properties);
        Property predicate = OntClass.NaryDataAllValuesFrom.class.equals(type) ? OWL.allValuesFrom : OWL.someValuesFrom;
        Resource res = createRestriction(model)
                .addProperty(predicate, dr)
                .addProperty(OWL.onProperties, model.createList(properties.iterator()));
        return model.getNodeAs(res.asNode(), type);
    }

    public static <CE extends ComponentsCE<?>, R extends OntObject> CE createComponentsCE(OntGraphModelImpl model,
                                                                                          Class<CE> returnType,
                                                                                          Class<R> componentType,
                                                                                          Property predicate,
                                                                                          Stream<R> components) {
        OntJenaException.notNull(components, "Null components stream.");
        Resource res = model.createResource(OWL.Class)
                .addProperty(predicate, model.createList(components
                        .peek(x -> OntJenaException.notNull(x,
                                viewAsString(returnType) + ": null " + viewAsString(componentType) + " member"))
                        .iterator()));
        return model.getNodeAs(res.asNode(), returnType);
    }

    public static HasSelf createHasSelf(OntGraphModelImpl model, OntObjectProperty onProperty) {
        Resource res = createOnPropertyRestriction(model, onProperty).addProperty(OWL.hasSelf, Models.TRUE);
        return model.getNodeAs(res.asNode(), HasSelf.class);
    }

    public static ComplementOf createComplementOf(OntGraphModelImpl model, OntClass other) {
        OntJenaException.notNull(other, "Null class expression.");
        Resource res = model.createResource(OWL.Class).addProperty(OWL.complementOf, other);
        return model.getNodeAs(res.asNode(), ComplementOf.class);
    }

    public static OntIndividual.Anonymous createAnonymousIndividual(OntGraphModelImpl model, OntClass source) {
        return model.getNodeAs(model.createResource(source).asNode(), OntIndividual.Anonymous.class);
    }

    public static OntIndividual.Named createNamedIndividual(OntGraphModelImpl model, OntClass source, String uri) {
        OntIndividual.Named res = model.createIndividual(OntJenaException.notNull(uri, "Null uri"));
        res.attachClass(source);
        return res;
    }

    public static OntList<OntRealProperty> createHasKey(OntGraphModelImpl m, OntClass clazz, Stream<? extends OntRealProperty> collection) {
        return m.createOntList(clazz, OWL.hasKey, OntRealProperty.class,
                collection.distinct().map(OntRealProperty.class::cast).iterator());
    }

    public static Stream<OntList<OntRealProperty>> listHasKeys(OntGraphModelImpl m, OntClass clazz) {
        return OntListImpl.stream(m, clazz, OWL.hasKey, OntRealProperty.class);
    }

    public static void removeHasKey(OntGraphModelImpl model,
                                    OntClass clazz,
                                    RDFNode rdfList) throws OntJenaException.IllegalArgument {
        model.deleteOntList(clazz, OWL.hasKey, clazz.findHasKey(rdfList).orElse(null));
    }

    public static Stream<OntRealProperty> declaredProperties(OntClass clazz, boolean direct) {
        OntModel m = clazz.getModel();
        Stream<OntRealProperty> properties = Stream.concat(
                m.statements(null, RDF.type, OWL.ObjectProperty).map(s -> s.getSubject().getAs(OntObjectProperty.class)),
                m.statements(null, RDF.type, OWL.DatatypeProperty).map(s -> s.getSubject().getAs(OntDataProperty.class))
        );
        return properties.filter(p -> p != null && testDomain(clazz, p, direct)).map(p -> p.as(OntRealProperty.class));
    }

    public static boolean testDomain(OntClass clazz, OntRealProperty property, boolean direct) {
        if (property.isURIResource() && property.as(OntEntity.class).isBuiltIn()) {
            return false;
        }
        AtomicBoolean isGlobal = new AtomicBoolean(true);
        AtomicBoolean seenDirect = new AtomicBoolean(false);
        try (Stream<OntClass> domains = property.domains()) {
            if (!domains.allMatch(domain -> {
                if (domain.equals(OWL.Thing) || domain.equals(RDFS.Resource)) {
                    return true;
                }
                isGlobal.set(false);
                if (clazz.equals(domain)) {
                    seenDirect.set(true);
                    return true;
                } else {
                    try (Stream<OntClass> superClasses = clazz.superClasses(false)) {
                        return superClasses.anyMatch(domain::equals);
                    }
                }
            })) {
                return false;
            }
            if (direct) {
                return seenDirect.get() || (isGlobal.get() && clazz.isHierarchyRoot());
            }
            return true;
        }
    }

    public static boolean isHierarchyRoot(OntClass clazz) {
        if (OWL.Nothing.equals(clazz)) {
            return false;
        }
        try (Stream<OntClass> superClasses = clazz.superClasses(true)) {
            return superClasses.allMatch(s -> s.equals(OWL.Thing) || s.equals(RDFS.Resource) || s.equals(clazz));
        }
    }

    public static boolean isNotBuiltin(OntClass clazz) {
        return !clazz.isURIResource() || !clazz.asNamed().isBuiltIn();
    }

    public static boolean isDisjoint(OntClass clazz, Resource candidate) {
        if (!candidate.canAs(OntClass.class)) {
            return false;
        }
        OntClass other = candidate.as(OntClass.class);
        try (Stream<OntClass> disjoints = other.disjointClasses()) {
            if (disjoints.anyMatch(clazz::equals)) {
                return true;
            }
        }
        try (Stream<OntClass> disjoints = clazz.disjointClasses()) {
            if (disjoints.anyMatch(other::equals)) {
                return true;
            }
        }
        try (Stream<OntDisjoint.Classes> disjoints = clazz.getModel().ontObjects(OntDisjoint.Classes.class)) {
            return disjoints.anyMatch(d -> {
                Set<OntClass> members = d.members().collect(Collectors.toSet());
                return members.contains(clazz) && members.contains(other);
            });
        }
    }

    @Override
    public Optional<OntStatement> findRootStatement() {
        return getRequiredRootStatement(this, OWL.Class);
    }

    @Override
    public abstract Class<? extends OntClass> getActualClass();

    @Override
    public OntIndividual.Anonymous createIndividual() {
        return createAnonymousIndividual(getModel(), this);
    }

    @Override
    public OntIndividual.Named createIndividual(String uri) {
        return createNamedIndividual(getModel(), this, uri);
    }

    @Override
    public OntList<OntRealProperty> createHasKey(Collection<OntObjectProperty> ope, Collection<OntDataProperty> dpe) {
        return createHasKey(getModel(), this, Stream.of(ope, dpe).flatMap(Collection::stream));
    }

    @Override
    public OntStatement addHasKeyStatement(OntRealProperty... properties) {
        return createHasKey(getModel(), this, Arrays.stream(properties)).getMainStatement();
    }

    @Override
    public Stream<OntList<OntRealProperty>> hasKeys() {
        return listHasKeys(getModel(), this);
    }

    @Override
    public OntCEImpl removeHasKey(Resource list) throws OntJenaException.IllegalArgument {
        removeHasKey(getModel(), this, list);
        return this;
    }

    @Override
    public boolean isDisjoint(Resource candidate) {
        return isDisjoint(this, candidate);
    }

    @Override
    public Stream<OntClass> superClasses(boolean direct) {
        return hierarchy(this, OntClass.class, RDFS.subClassOf, false, direct);
    }

    @Override
    public Stream<OntClass> subClasses(boolean direct) {
        return hierarchy(this, OntClass.class, RDFS.subClassOf, true, direct);
    }

    @Override
    public boolean hasDeclaredProperty(OntRealProperty property, boolean direct) {
        return testDomain(this, property, direct);
    }

    @Override
    public Stream<OntRealProperty> declaredProperties(boolean direct) {
        return declaredProperties(this, direct);
    }

    @Override
    public boolean isHierarchyRoot() {
        return isHierarchyRoot(this);
    }

    protected enum ObjectRestrictionType implements PredicateFilterProvider {
        CLASS {
            @Override
            public Class<OntClass> view() {
                return OntClass.class;
            }
        },
        DATA_RANGE {
            @Override
            public Class<OntDataRange> view() {
                return OntDataRange.class;
            }
        },
        INDIVIDUAL {
            @Override
            public Class<OntIndividual> view() {
                return OntIndividual.class;
            }
        },
        LITERAL {
            @Override
            public Class<Literal> view() {
                return Literal.class;
            }

            @Override
            public boolean testObject(Node node, EnhGraph graph) {
                return node.isLiteral();
            }
        },
    }

    protected enum RestrictionType implements PredicateFilterProvider {
        DATA(OntDataProperty.class),
        OBJECT(OntObjectProperty.class),
        ;
        private final Class<? extends OntProperty> type;
        private final ObjectFactory propertyFactory;

        RestrictionType(Class<? extends OntProperty> type) {
            this.type = type;
            this.propertyFactory = of(type);
        }

        @Override
        public Class<? extends OntProperty> view() {
            return type;
        }

        public OntFilter getFilter() {
            return getFilter(OWL.onProperty);
        }

        @Override
        public boolean testObject(Node node, EnhGraph graph) {
            return propertyFactory.canWrap(node, graph);
        }
    }

    protected enum CardinalityType {
        EXACTLY(OWL.qualifiedCardinality, OWL.cardinality),
        MAX(OWL.maxQualifiedCardinality, OWL.maxCardinality),
        MIN(OWL.minQualifiedCardinality, OWL.minCardinality);
        static final RDFDatatype NON_NEGATIVE_INTEGER = XSDDatatype.XSDnonNegativeInteger;

        private final Property qualifiedPredicate, predicate;
        static final Node CLASS_REFERENCE = OWL.onClass.asNode();
        static final Node RANGE_REFERENCE = OWL.onDataRange.asNode();

        CardinalityType(Property qualifiedPredicate, Property predicate) {
            this.qualifiedPredicate = qualifiedPredicate;
            this.predicate = predicate;
        }

        public OntFilter getFilter(Class<? extends RDFNode> objectType) {
            return (n, g) -> isNonQualified(n, g) || isQualified(n, g, objectType);
        }

        public Property getPredicate(boolean isQualified) {
            return isQualified ? qualifiedPredicate : predicate;
        }

        private boolean isQualified(Node s, EnhGraph g, Class<? extends RDFNode> objectType) {
            if (!hasCardinality(s, qualifiedPredicate, g)) return false;
            Node p;
            if (objectType == OntClass.class) {
                p = CLASS_REFERENCE;
            } else if (objectType == OntDataRange.class) {
                p = RANGE_REFERENCE;
            } else {
                return false;
            }
            return Iter.findFirst(g.asGraph().find(s, p, Node.ANY)
                    .filterKeep(t -> isObjectOfType(g, t.getObject(), objectType))).isPresent();
        }

        private boolean isNonQualified(Node s, EnhGraph g) {
            return hasCardinality(s, predicate, g);
        }

        private boolean hasCardinality(Node s, Property p, EnhGraph g) {
            return Iter.findFirst(g.asGraph().find(s, p.asNode(), Node.ANY)
                    .filterKeep(t -> isNonNegativeInteger(t.getObject()))).isPresent();
        }

        private static boolean isObjectOfType(EnhGraph g, Node n, Class<? extends RDFNode> type) {
            return PersonalityModel.canAs(type, n, g);
        }

        public static boolean isNonNegativeInteger(Node n) {
            return n.isLiteral() && NON_NEGATIVE_INTEGER.equals(n.getLiteralDatatype());
        }
    }

    /**
     * Technical interface to make predicate filter for restrictions
     */
    private interface PredicateFilterProvider {

        Class<? extends RDFNode> view();

        default OntFilter getFilter(Property predicate) {
            return (node, graph) -> testObjects(predicate, node, graph);
        }

        default boolean testObjects(Property predicate, Node node, EnhGraph graph) {
            return Iter.anyMatch(graph.asGraph().find(node, predicate.asNode(), Node.ANY),
                    t -> testObject(t.getObject(), graph));
        }

        default boolean testObject(Node node, EnhGraph graph) {
            return PersonalityModel.canAs(view(), node, graph);
        }
    }

    public static class ObjectSomeValuesFromImpl
            extends ComponentRestrictionCEImpl<OntClass, OntObjectProperty, ObjectSomeValuesFromImpl> implements ObjectSomeValuesFrom {
        public ObjectSomeValuesFromImpl(Node n, EnhGraph m) {
            super(n, m, OWL.someValuesFrom, OntClass.class, OntObjectProperty.class);
        }

        @Override
        public Class<ObjectSomeValuesFrom> getActualClass() {
            return ObjectSomeValuesFrom.class;
        }
    }

    public static class DataSomeValuesFromImpl
            extends ComponentRestrictionCEImpl<OntDataRange, OntDataProperty, DataSomeValuesFromImpl> implements DataSomeValuesFrom {
        public DataSomeValuesFromImpl(Node n, EnhGraph m) {
            super(n, m, OWL.someValuesFrom, OntDataRange.class, OntDataProperty.class);
        }

        @Override
        public Class<DataSomeValuesFrom> getActualClass() {
            return DataSomeValuesFrom.class;
        }
    }

    public static class ObjectAllValuesFromImpl
            extends ComponentRestrictionCEImpl<OntClass, OntObjectProperty, ObjectAllValuesFromImpl> implements ObjectAllValuesFrom {
        public ObjectAllValuesFromImpl(Node n, EnhGraph m) {
            super(n, m, OWL.allValuesFrom, OntClass.class, OntObjectProperty.class);
        }

        @Override
        public Class<ObjectAllValuesFrom> getActualClass() {
            return ObjectAllValuesFrom.class;
        }
    }

    public static class DataAllValuesFromImpl
            extends ComponentRestrictionCEImpl<OntDataRange, OntDataProperty, DataAllValuesFromImpl> implements DataAllValuesFrom {
        public DataAllValuesFromImpl(Node n, EnhGraph m) {
            super(n, m, OWL.allValuesFrom, OntDataRange.class, OntDataProperty.class);
        }

        @Override
        public Class<DataAllValuesFrom> getActualClass() {
            return DataAllValuesFrom.class;
        }
    }

    public static class ObjectHasValueImpl
            extends ComponentRestrictionCEImpl<OntIndividual, OntObjectProperty, ObjectHasValueImpl> implements ObjectHasValue {
        public ObjectHasValueImpl(Node n, EnhGraph m) {
            super(n, m, OWL.hasValue, OntIndividual.class, OntObjectProperty.class);
        }

        @Override
        public Class<ObjectHasValue> getActualClass() {
            return ObjectHasValue.class;
        }
    }

    public static class DataHasValueImpl
            extends ComponentRestrictionCEImpl<Literal, OntDataProperty, DataHasValueImpl> implements DataHasValue {
        public DataHasValueImpl(Node n, EnhGraph m) {
            super(n, m, OWL.hasValue, Literal.class, OntDataProperty.class);
        }

        @Override
        public Class<DataHasValue> getActualClass() {
            return DataHasValue.class;
        }
    }

    public static class UnionOfImpl extends ComponentsCEImpl<OntClass> implements UnionOf {
        public UnionOfImpl(Node n, EnhGraph m) {
            super(n, m, OWL.unionOf, OntClass.class);
        }

        @Override
        public Class<UnionOf> getActualClass() {
            return UnionOf.class;
        }
    }

    public static class IntersectionOfImpl extends ComponentsCEImpl<OntClass> implements IntersectionOf {
        public IntersectionOfImpl(Node n, EnhGraph m) {
            super(n, m, OWL.intersectionOf, OntClass.class);
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

    public static class DataMinCardinalityImpl
            extends CardinalityRestrictionCEImpl<OntDataRange, OntDataProperty, DataMinCardinalityImpl> implements DataMinCardinality {
        public DataMinCardinalityImpl(Node n, EnhGraph m) {
            super(n, m, OWL.onDataRange, OntDataRange.class, OntDataProperty.class, CardinalityType.MIN);
        }

        @Override
        public Class<DataMinCardinality> getActualClass() {
            return DataMinCardinality.class;
        }
    }

    public static class ObjectMinCardinalityImpl
            extends CardinalityRestrictionCEImpl<OntClass, OntObjectProperty, ObjectMinCardinalityImpl> implements ObjectMinCardinality {
        public ObjectMinCardinalityImpl(Node n, EnhGraph m) {
            super(n, m, OWL.onClass, OntClass.class, OntObjectProperty.class, CardinalityType.MIN);
        }

        @Override
        public Class<ObjectMinCardinality> getActualClass() {
            return ObjectMinCardinality.class;
        }
    }

    public static class DataMaxCardinalityImpl
            extends CardinalityRestrictionCEImpl<OntDataRange, OntDataProperty, DataMaxCardinalityImpl> implements DataMaxCardinality {
        public DataMaxCardinalityImpl(Node n, EnhGraph m) {
            super(n, m, OWL.onDataRange, OntDataRange.class, OntDataProperty.class, CardinalityType.MAX);
        }

        @Override
        public Class<DataMaxCardinality> getActualClass() {
            return DataMaxCardinality.class;
        }
    }

    public static class ObjectMaxCardinalityImpl
            extends CardinalityRestrictionCEImpl<OntClass, OntObjectProperty, ObjectMaxCardinalityImpl> implements ObjectMaxCardinality {
        public ObjectMaxCardinalityImpl(Node n, EnhGraph m) {
            super(n, m, OWL.onClass, OntClass.class, OntObjectProperty.class, CardinalityType.MAX);
        }

        @Override
        public Class<ObjectMaxCardinality> getActualClass() {
            return ObjectMaxCardinality.class;
        }
    }

    public static class DataCardinalityImpl
            extends CardinalityRestrictionCEImpl<OntDataRange, OntDataProperty, DataCardinalityImpl> implements DataCardinality {
        public DataCardinalityImpl(Node n, EnhGraph m) {
            super(n, m, OWL.onDataRange, OntDataRange.class, OntDataProperty.class, CardinalityType.EXACTLY);
        }

        @Override
        public Class<DataCardinality> getActualClass() {
            return DataCardinality.class;
        }
    }

    public static class ObjectCardinalityImpl
            extends CardinalityRestrictionCEImpl<OntClass, OntObjectProperty, ObjectCardinalityImpl> implements ObjectCardinality {
        public ObjectCardinalityImpl(Node n, EnhGraph m) {
            super(n, m, OWL.onClass, OntClass.class, OntObjectProperty.class, CardinalityType.EXACTLY);
        }

        @Override
        public Class<ObjectCardinality> getActualClass() {
            return ObjectCardinality.class;
        }
    }

    public static class HasSelfImpl extends OnPropertyRestrictionCEImpl<OntObjectProperty, HasSelfImpl> implements HasSelf {
        public HasSelfImpl(Node n, EnhGraph m) {
            super(n, m, OntObjectProperty.class);
        }

        @Override
        public ExtendedIterator<OntStatement> listSpec() {
            return Iter.concat(super.listSpec(), listRequired(OWL.hasSelf));
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
        public ExtendedIterator<OntStatement> listSpec() {
            return Iter.concat(super.listSpec(), listRequired(OWL.complementOf));
        }

        @Override
        public Class<ComplementOf> getActualClass() {
            return ComplementOf.class;
        }

        @Override
        public OntClass getValue() {
            return getRequiredObject(OWL.complementOf, OntClass.class);
        }

        @Override
        public ComplementOfImpl setValue(OntClass c) {
            Objects.requireNonNull(c, "Null component");
            clear();
            addProperty(OWL.complementOf, c);
            return this;
        }

        void clear() {
            removeAll(OWL.complementOf);
        }
    }

    public static class NaryDataAllValuesFromImpl
            extends NaryRestrictionCEImpl<OntDataRange, OntDataProperty, NaryDataAllValuesFromImpl> implements NaryDataAllValuesFrom {

        public NaryDataAllValuesFromImpl(Node n, EnhGraph m) {
            super(n, m, OWL.allValuesFrom, OntDataRange.class, OntDataProperty.class);
        }

        @Override
        public Class<? extends OntClass> getActualClass() {
            return NaryDataAllValuesFrom.class;
        }
    }

    public static class NaryDataSomeValuesFromImpl
            extends NaryRestrictionCEImpl<OntDataRange, OntDataProperty, NaryDataSomeValuesFromImpl> implements NaryDataSomeValuesFrom {

        public NaryDataSomeValuesFromImpl(Node n, EnhGraph m) {
            super(n, m, OWL.someValuesFrom, OntDataRange.class, OntDataProperty.class);
        }

        @Override
        public Class<? extends OntClass> getActualClass() {
            return NaryDataSomeValuesFrom.class;
        }
    }

    /**
     * An abstract super class for {@link IntersectionOf}, {@link OneOf}, {@link UnionOf}.
     *
     * @param <O> {@link OntObject}
     */
    protected static abstract class ComponentsCEImpl<O extends OntObject>
            extends OntCEImpl implements ComponentsCE<O> {
        protected final Property predicate;
        protected final Class<O> type;

        protected ComponentsCEImpl(Node n, EnhGraph m, Property predicate, Class<O> type) {
            super(n, m);
            this.predicate = OntJenaException.notNull(predicate, "Null predicate.");
            this.type = OntJenaException.notNull(type, "Null view.");
        }

        @Override
        public ExtendedIterator<OntStatement> listSpec() {
            return Iter.concat(super.listSpec(), getList().listContent());
        }

        @Override
        public OntListImpl<O> getList() {
            return getModel().asOntList(getRequiredObject(predicate, RDFList.class), this, predicate, true, null, type);
        }
    }

    /**
     * Abstract implementation for any restriction with {@code owl:onProperty} predicate.
     *
     * @param <P> a subtype of {@link OntRealProperty Data or Object Property Expression}
     * @param <R> return type for {@link OWL#onProperty} setter
     */
    protected static abstract class OnPropertyRestrictionCEImpl<P extends OntRealProperty, R extends OntCEImpl>
            extends OntCEImpl implements UnaryRestrictionCE<P> {
        protected final Class<P> propertyView;

        /**
         * @param n            {@link Node}
         * @param m            {@link EnhGraph}
         * @param propertyType Class-type for {@link OntRealProperty}
         */
        protected OnPropertyRestrictionCEImpl(Node n, EnhGraph m, Class<P> propertyType) {
            super(n, m);
            this.propertyView = propertyType;
        }

        @Override
        public Optional<OntStatement> findRootStatement() {
            return getRequiredRootStatement(this, OWL.Restriction);
        }

        @Override
        public P getProperty() {
            return getRequiredObject(OWL.onProperty, propertyView);
        }

        @SuppressWarnings("unchecked")
        public R setProperty(P property) {
            Objects.requireNonNull(property, "Null " + viewAsString(propertyView));
            removeAll(OWL.onProperty).addProperty(OWL.onProperty, property);
            return (R) this;
        }

        @Override
        public ExtendedIterator<OntStatement> listSpec() {
            return Iter.concat(super.listSpec(), listRequired(OWL.onProperty));
        }
    }

    /**
     * Abstract base component-restriction class.
     * It's for CE which has owl:onProperty and some component also (with predicate owl:dataRange,owl:onClass, owl:someValuesFrom, owl:allValuesFrom)
     *
     * @param <O> a class-type of {@link RDFNode rdf-node}
     * @param <P> a class-type of {@link OntRealProperty data or object property-expression}
     * @param <R> a subtype of {@link ComponentRestrictionCEImpl}
     */
    protected static abstract class ComponentRestrictionCEImpl<O extends RDFNode,
            P extends OntRealProperty,
            R extends ComponentRestrictionCEImpl<?, ?, ?>>
            extends OnPropertyRestrictionCEImpl<P, R> implements ComponentRestrictionCE<O, P> {
        protected final Property predicate;
        protected final Class<O> objectView;

        /**
         * @param n            Node
         * @param m            EnhGraph
         * @param predicate    predicate for value
         * @param objectView   Class
         * @param propertyView Class
         */
        protected ComponentRestrictionCEImpl(Node n,
                                             EnhGraph m,
                                             Property predicate,
                                             Class<O> objectView,
                                             Class<P> propertyView) {
            super(n, m, propertyView);
            this.predicate = OntJenaException.notNull(predicate, "Null predicate.");
            this.objectView = OntJenaException.notNull(objectView, "Null object view.");
        }

        @Override
        public ExtendedIterator<OntStatement> listSpec() {
            return listSpec(true);
        }

        protected ExtendedIterator<OntStatement> listSpec(boolean requireObject) {
            return requireObject ? Iter.concat(super.listSpec(), listRequired(predicate)) : super.listSpec();
        }

        @Override
        public O getValue() {
            return getModel().getNodeAs(getRequiredProperty(predicate).getObject().asNode(), objectView);
        }

        @SuppressWarnings("unchecked")
        public R setValue(O c) {
            Objects.requireNonNull(c, "Null filler");
            removeAll(predicate).addProperty(predicate, c);
            return (R) this;
        }
    }

    /**
     * Abstraction for any cardinality restriction.
     *
     * @param <O> either {@link OntClass} (predicate {@link OWL#onClass owl:onClass}) or {@link OntDataRange}
     *            (predicate: {@link OWL#onDataRange owl:onDataRange})
     * @param <P> either {@link OntObjectProperty} or {@link OntDataProperty}
     * @param <R> subtype of {@link CardinalityRestrictionCEImpl}
     */
    protected static abstract class CardinalityRestrictionCEImpl<O extends OntObject,
            P extends OntRealProperty,
            R extends CardinalityRestrictionCEImpl<?, ?, ?>>
            extends ComponentRestrictionCEImpl<O, P, R> implements CardinalityRestrictionCE<O, P> {
        protected final CardinalityType cardinalityType;

        /**
         * @param n               {@link Node}
         * @param m               {@link EnhGraph}
         * @param predicate       either {@code owl:onDataRange} or {@code owl:onClass}
         * @param objectView      interface of class expression or data range
         * @param propertyView    interface, property expression
         * @param cardinalityType type of cardinality.
         */
        protected CardinalityRestrictionCEImpl(Node n,
                                               EnhGraph m,
                                               Property predicate,
                                               Class<O> objectView,
                                               Class<P> propertyView,
                                               CardinalityType cardinalityType) {
            super(n, m, predicate, objectView, propertyView);
            this.cardinalityType = cardinalityType;
        }

        @Override
        public ExtendedIterator<OntStatement> listSpec() {
            // note: object value <O> is null for non-qualified restrictions.
            boolean q;
            return Iter.concat(super.listSpec(q = isQualified()), listRequired(getCardinalityPredicate(q)));
        }

        @Override
        public O getValue() { // null for non-qualified restrictions:
            return object(predicate, objectView).orElseGet(this::getUnqualifiedValue);
        }

        private O getUnqualifiedValue() {
            OntModel m = getModel();
            return (O) (OntClass.class.isAssignableFrom(objectView) ? m.getOWLThing() : m.getRDFSLiteral());
        }

        @SuppressWarnings("unchecked")
        @Override
        public R setValue(O value) {
            Literal c = getCardinalityLiteral();
            removeAll(predicate);
            if (!isQualified(value)) { // null, owl:Thing, rdfs:Label
                removeAll(getCardinalityPredicate(true))
                        .addProperty(getCardinalityPredicate(false), c);
            } else {
                removeAll(getCardinalityPredicate(false))
                        .addProperty(getCardinalityPredicate(true), c)
                        .addProperty(predicate, value);
            }
            return (R) this;
        }

        @Override
        public int getCardinality() {
            return getCardinalityLiteral().getInt();
        }

        private Literal getCardinalityLiteral() {
            return getRequiredObject(getCardinalityPredicate(), Literal.class);
        }

        @SuppressWarnings("unchecked")
        public R setCardinality(int cardinality) {
            Literal value = createNonNegativeIntegerLiteral(cardinality);
            Property property = getCardinalityPredicate();
            removeAll(property).addLiteral(property, value);
            return (R) this;
        }

        protected Property getCardinalityPredicate() {
            return getCardinalityPredicate(isQualified());
        }

        private Property getCardinalityPredicate(boolean q) {
            return cardinalityType.getPredicate(q);
        }

        @Override
        public boolean isQualified() {
            return isQualified(getValue()) && hasProperty(cardinalityType.getPredicate(true));
        }
    }

    protected static abstract class NaryRestrictionCEImpl<O extends OntObject,
            P extends OntRealProperty, R extends NaryRestrictionCEImpl<?, ?, ?>>
            extends OntCEImpl implements NaryRestrictionCE<O, P> {
        protected final Property predicate;
        protected final Class<O> objectType; // always OntDR
        protected final Class<P> propertyType;

        protected NaryRestrictionCEImpl(Node n,
                                        EnhGraph m,
                                        Property predicate,
                                        Class<O> objectType,
                                        Class<P> propertyType) {
            super(n, m);
            this.predicate = predicate;
            this.objectType = objectType;
            this.propertyType = propertyType;
        }

        @Override
        public Optional<OntStatement> findRootStatement() {
            return getRequiredRootStatement(this, OWL.Restriction);
        }

        @Override
        public O getValue() {
            return getRequiredObject(predicate, objectType);
        }

        @SuppressWarnings("unchecked")
        public R setValue(O value) {
            Objects.requireNonNull(value);
            removeAll(predicate).addProperty(predicate, value);
            return (R) this;
        }

        @SuppressWarnings("unchecked")
        public R setComponents(Collection<P> properties) {
            validateArity((OntDataRange) getValue(), (Collection<OntDataProperty>) properties);
            getList().clear().addAll(properties);
            return (R) this;
        }

        @Override
        public Class<? extends OntClass> getActualClass() {
            return NaryRestrictionCE.class;
        }

        @Override
        public ExtendedIterator<OntStatement> listSpec() {
            return Iter.concat(super.listSpec(), listRequired(predicate), getList().listContent());
        }

        @Override
        public OntListImpl<P> getList() {
            return getModel().asOntList(getRequiredObject(OWL.onProperties, RDFList.class),
                    this, OWL.onProperties, propertyType);
        }

        public static void validateArity(OntDataRange dr, Collection<OntDataProperty> properties) {
            properties.forEach(x -> OntJenaException.notNull(x, "Null data property"));
            if (dr.arity() == properties.size()) return;
            throw new OntJenaException.IllegalArgument("The number of data properties (" + properties.size() + ") " +
                    "must be equal to the data range arity (" + dr.arity() + ").");
        }
    }

    protected static class HasSelfFilter implements OntFilter {
        @Override
        public boolean test(Node n, EnhGraph g) {
            return g.asGraph().contains(n, OWL.hasSelf.asNode(), Models.TRUE.asNode());
        }
    }

    protected static class HasSelfMaker extends OntMaker.WithType {
        protected HasSelfMaker() {
            super(HasSelfImpl.class, OWL.Restriction);
        }

        @Override
        public void make(Node node, EnhGraph eg) {
            super.make(node, eg);
            eg.asGraph().add(Triple.create(node, OWL.hasSelf.asNode(), Models.TRUE.asNode()));
        }
    }

    /**
     * A factory to produce {@link OntClass}s.
     * <p>
     * Although it would be easy to produce this factory using {@link Factories#createFrom(OntFinder, Class[])},
     * this variant with explicit methods must be a little faster,
     * since there is a reduction of number of some possible repetition calls.
     * Also, everything here is under control.
     * <p>
     * Created by @ssz on 01.09.2018.
     */
    @SuppressWarnings("WeakerAccess")
    public static class ClassExpressionFactory extends BaseFactoryImpl {

        private static final Node ANY = Node.ANY;
        private static final Node TYPE = RDF.Nodes.type;
        private static final Node CLASS = OWL.Class.asNode();
        private static final Node RESTRICTION = OWL.Restriction.asNode();
        private static final Node ON_PROPERTY = OWL.onProperty.asNode();
        private static final Node HAS_VALUE = OWL.hasValue.asNode();
        private static final Node QUALIFIED_CARDINALITY = OWL.qualifiedCardinality.asNode();
        private static final Node CARDINALITY = OWL.cardinality.asNode();
        private static final Node MIN_QUALIFIED_CARDINALITY = OWL.minQualifiedCardinality.asNode();
        private static final Node MIN_CARDINALITY = OWL.minCardinality.asNode();
        private static final Node MAX_QUALIFIED_CARDINALITY = OWL.maxQualifiedCardinality.asNode();
        private static final Node MAX_CARDINALITY = OWL.maxCardinality.asNode();
        private static final Node SOME_VALUES_FROM = OWL.someValuesFrom.asNode();
        private static final Node ALL_VALUES_FROM = OWL.allValuesFrom.asNode();
        private static final Node ON_CLASS = OWL.onClass.asNode();
        private static final Node ON_DATA_RANGE = OWL.onDataRange.asNode();
        private static final Node HAS_SELF = OWL.hasSelf.asNode();
        private static final Node ON_PROPERTIES = OWL.onProperties.asNode();
        private static final Node INTERSECTION_OF = OWL.intersectionOf.asNode();
        private static final Node UNION_OF = OWL.unionOf.asNode();
        private static final Node ONE_OF = OWL.oneOf.asNode();
        private static final Node COMPLEMENT_OF = OWL.complementOf.asNode();

        private static final Node TRUE = NodeFactory.createLiteralByValue(Boolean.TRUE, XSDDatatype.XSDboolean);
        private static final String NON_NEGATIVE_INTEGER_URI = XSD.nonNegativeInteger.getURI();

        protected static final Implementation LIST_FACTORY = RDFListImpl.factory;
        protected final ObjectFactory objectPropertyFactory = of(OntObjectProperty.class);
        protected final ObjectFactory dataPropertyFactory = of(OntDataProperty.class);

        public static ObjectFactory createFactory() {
            return new ClassExpressionFactory();
        }

        private static boolean isDataCardinality(Node n, EnhGraph eg, Node p, Node qp) {
            return isCardinality(n, eg, p) || isQualifiedCardinality(n, eg, qp, ON_DATA_RANGE, OntDataRange.class);
        }

        private static boolean isObjectCardinality(Node n, EnhGraph eg, Node p, Node qp) {
            return isCardinality(n, eg, p) || isQualifiedCardinality(n, eg, qp, ON_CLASS, OntClass.class);
        }

        private static boolean isQualifiedCardinality(Node n,
                                                      EnhGraph eg,
                                                      Node p,
                                                      Node o,
                                                      Class<? extends OntObject> t) {
            return isCardinality(n, eg, p) && isObjectOfType(n, eg, o, t);
        }

        private static boolean isCardinality(Node n, EnhGraph eg, Node p) {
            return Iter.findFirst(listObjects(n, eg, p)
                    .filterKeep(x -> isLiteral(x.getObject(), NON_NEGATIVE_INTEGER_URI))).isPresent();
        }

        private static boolean isList(Node n, EnhGraph eg, Node p) {
            return Iter.findFirst(listObjects(n, eg, p)
                    .filterKeep(x -> LIST_FACTORY.canWrap(x.getObject(), eg))).isPresent();
        }

        @SuppressWarnings("SameParameterValue")
        private static boolean isLiteral(Node n, String dt) {
            return n.isLiteral() && dt.equals(n.getLiteralDatatypeURI());
        }

        private static boolean isObjectOfType(Node n, EnhGraph eg, Node p, Class<? extends OntObject> t) {
            return Iter.findFirst(listObjects(n, eg, p).filterKeep(x -> hasType(x.getObject(), eg, t))).isPresent();
        }

        private static boolean hasType(Node n, EnhGraph eg, Class<? extends OntObject> type) {
            return PersonalityModel.canAs(type, n, eg);
        }

        private static ExtendedIterator<Triple> listObjects(Node n, EnhGraph eg, Node p) {
            return eg.asGraph().find(n, p, ANY);
        }

        @Override
        public ExtendedIterator<EnhNode> iterator(EnhGraph g) {
            return g.asGraph().find(ANY, RDF.Nodes.type, CLASS)
                    .mapWith(t -> {
                        Node n = t.getSubject();
                        return n.isURI() ? safeWrap(n, g, Factory.CLASS.factory) : safeWrap(n, g, Factory.ANONYMOUS);
                    })
                    .andThen(g.asGraph().find(ANY, RDF.Nodes.type, RESTRICTION)
                            .mapWith(t -> safeWrap(t.getSubject(), g, Factory.RESTRICTIONS)))
                    .filterDrop(Objects::isNull);
        }

        @Override
        public EnhNode createInstance(Node node, EnhGraph eg) {
            Factory f = map(node, eg);
            if (f == null) return null;
            return f.factory.createInstance(node, eg);
        }

        @Override
        public boolean canWrap(Node node, EnhGraph eg) {
            return map(node, eg) != null;
        }

        @Override
        public EnhNode wrap(Node node, EnhGraph eg) throws OntJenaException {
            Factory f = map(node, eg);
            if (f == null) {
                throw new OntJenaException.Conversion("Can't convert node " + node + " to Class Expression.");
            }
            EnhNode res = f.factory.createInstance(node, eg);
            if (res == null) {
                throw new OntJenaException.IllegalState("Can't create Class Expression for node " + node);
            }
            return res;
        }

        public Factory map(Node n, EnhGraph eg) {
            if (n.isURI()) {
                if (Factory.CLASS.factory.canWrap(n, eg)) {
                    return Factory.CLASS;
                }
                return null;
            }
            if (!n.isBlank()) {
                return null;
            }
            Graph g = eg.asGraph();
            if (g.contains(n, TYPE, RESTRICTION)) {
                ExtendedIterator<Node> props = listObjects(n, eg, ON_PROPERTY).mapWith(Triple::getObject);
                try {
                    while (props.hasNext()) {
                        Node p = props.next();
                        if (objectPropertyFactory.canWrap(p, eg)) {
                            // ObjectSomeValuesFrom, ObjectAllValuesFrom
                            if (isObjectOfType(n, eg, SOME_VALUES_FROM, OntClass.class)) {
                                return Factory.OBJECT_SOME_VALUES_FROM;
                            }
                            if (isObjectOfType(n, eg, ALL_VALUES_FROM, OntClass.class)) {
                                return Factory.OBJECT_ALL_VALUES_FROM;
                            }
                            // ObjectMinCardinality, ObjectMaxCardinality, ObjectCardinality
                            if (isObjectCardinality(n, eg, MIN_CARDINALITY, MIN_QUALIFIED_CARDINALITY)) {
                                return Factory.OBJECT_MIN_CARDINALITY;
                            }
                            if (isObjectCardinality(n, eg, MAX_CARDINALITY, MAX_QUALIFIED_CARDINALITY)) {
                                return Factory.OBJECT_MAX_CARDINALITY;
                            }
                            if (isObjectCardinality(n, eg, CARDINALITY, QUALIFIED_CARDINALITY)) {
                                return Factory.OBJECT_EXACT_CARDINALITY;
                            }
                            // ObjectHasValue
                            if (isObjectOfType(n, eg, HAS_VALUE, OntIndividual.class)) {
                                return Factory.OBJECT_HAS_VALUE;
                            }
                            // HasSelf
                            if (Iter.findFirst(listObjects(n, eg, HAS_SELF)
                                    .filterKeep(x -> TRUE.equals(x.getObject()))).isPresent()) {
                                return Factory.OBJECT_HAS_SELF;
                            }
                        }
                        if (dataPropertyFactory.canWrap(p, eg)) {
                            // DataSomeValuesFrom, DataAllValuesFrom
                            if (isObjectOfType(n, eg, SOME_VALUES_FROM, OntDataRange.class)) {
                                return Factory.DATA_SOME_VALUES_FROM;
                            }
                            if (isObjectOfType(n, eg, ALL_VALUES_FROM, OntDataRange.class)) {
                                return Factory.DATA_ALL_VALUES_FROM;
                            }
                            // DataMinCardinality, DataMaxCardinality, DataCardinality
                            if (isDataCardinality(n, eg, MIN_CARDINALITY, MIN_QUALIFIED_CARDINALITY)) {
                                return Factory.DATA_MIN_CARDINALITY;
                            }
                            if (isDataCardinality(n, eg, MAX_CARDINALITY, MAX_QUALIFIED_CARDINALITY)) {
                                return Factory.DATA_MAX_CARDINALITY;
                            }
                            if (isDataCardinality(n, eg, CARDINALITY, QUALIFIED_CARDINALITY)) {
                                return Factory.DATA_EXACT_CARDINALITY;
                            }
                            // DataHasValue
                            if (Iter.findFirst(listObjects(n, eg, HAS_VALUE)
                                    .filterKeep(x -> x.getObject().isLiteral())).isPresent()) {
                                return Factory.DATA_HAS_VALUE;
                            }
                        }
                    }
                } finally {
                    props.close();
                }

                // very simplified factories for nary-restrictions:
                if (g.contains(n, ON_PROPERTIES, ANY)) {
                    if (Iter.findFirst(listObjects(n, eg, SOME_VALUES_FROM)).isPresent()) {
                        return Factory.DATA_NARY_SOME_VALUES_FROM;
                    }
                    if (Iter.findFirst(listObjects(n, eg, ALL_VALUES_FROM)).isPresent()) {
                        return Factory.DATA_NARY_ALL_VALUES_FROM;
                    }
                }
            }
            if (g.contains(n, TYPE, CLASS)) {
                // first check owl:complementOf, since it is more accurately defined
                if (isObjectOfType(n, eg, COMPLEMENT_OF, OntClass.class)) {
                    return Factory.COMPLEMENT_OF;
                }
                // simplified checks for []-lists
                // todo: need more accurate check - also for content, to avoid intersections with data ranges
                if (isList(n, eg, INTERSECTION_OF)) {
                    return Factory.INTERSECTION_OF;
                }
                if (isList(n, eg, UNION_OF)) {
                    return Factory.UNION_OF;
                }
                if (isList(n, eg, ONE_OF)) {
                    return Factory.ONE_OF;
                }
            }
            return null;
        }

        enum Factory {
            CLASS(Named.class, false),
            OBJECT_SOME_VALUES_FROM(OntClass.ObjectSomeValuesFrom.class),
            OBJECT_ALL_VALUES_FROM(OntClass.ObjectAllValuesFrom.class),
            OBJECT_MIN_CARDINALITY(OntClass.ObjectMinCardinality.class),
            OBJECT_MAX_CARDINALITY(OntClass.ObjectMaxCardinality.class),
            OBJECT_EXACT_CARDINALITY(OntClass.ObjectCardinality.class),
            OBJECT_HAS_VALUE(OntClass.ObjectHasValue.class),
            OBJECT_HAS_SELF(OntClass.HasSelf.class),

            DATA_SOME_VALUES_FROM(OntClass.DataSomeValuesFrom.class),
            DATA_ALL_VALUES_FROM(OntClass.DataAllValuesFrom.class),
            DATA_MIN_CARDINALITY(OntClass.DataMinCardinality.class),
            DATA_MAX_CARDINALITY(OntClass.DataMaxCardinality.class),
            DATA_EXACT_CARDINALITY(OntClass.DataCardinality.class),
            DATA_HAS_VALUE(OntClass.DataHasValue.class),
            DATA_NARY_SOME_VALUES_FROM(NaryDataSomeValuesFrom.class),
            DATA_NARY_ALL_VALUES_FROM(NaryDataAllValuesFrom.class),

            UNION_OF(OntClass.UnionOf.class, false),
            INTERSECTION_OF(OntClass.IntersectionOf.class, false),
            ONE_OF(OntClass.OneOf.class, false),
            COMPLEMENT_OF(OntClass.ComplementOf.class, false),
            ;
            private static final Collection<ObjectFactory> RESTRICTIONS = Arrays.stream(values())
                    .filter(x -> x.isRestriction)
                    .map(x -> x.factory)
                    .collect(Collectors.toUnmodifiableList());
            private static final Collection<ObjectFactory> ANONYMOUS = Arrays.stream(values())
                    .filter(x -> !x.isRestriction && CLASS != x)
                    .map(x -> x.factory)
                    .collect(Collectors.toUnmodifiableList());

            private final ObjectFactory factory;
            private final boolean isRestriction;

            Factory(Class<? extends OntObject> type) {
                this(type, true);
            }

            Factory(Class<? extends OntObject> type, boolean restriction) {
                this.factory = of(type);
                this.isRestriction = restriction;
            }
        }
    }
}
