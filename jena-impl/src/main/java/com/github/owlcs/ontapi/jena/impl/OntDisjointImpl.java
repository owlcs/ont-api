/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2023, owl.cs group.
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
import com.github.owlcs.ontapi.jena.impl.conf.ObjectFactory;
import com.github.owlcs.ontapi.jena.impl.conf.OntFilter;
import com.github.owlcs.ontapi.jena.impl.conf.OntFinder;
import com.github.owlcs.ontapi.jena.impl.conf.OntMaker;
import com.github.owlcs.ontapi.jena.model.OntClass;
import com.github.owlcs.ontapi.jena.model.OntDataProperty;
import com.github.owlcs.ontapi.jena.model.OntDisjoint;
import com.github.owlcs.ontapi.jena.model.OntIndividual;
import com.github.owlcs.ontapi.jena.model.OntObject;
import com.github.owlcs.ontapi.jena.model.OntObjectProperty;
import com.github.owlcs.ontapi.jena.model.OntRealProperty;
import com.github.owlcs.ontapi.jena.model.OntStatement;
import com.github.owlcs.ontapi.jena.utils.Iterators;
import com.github.owlcs.ontapi.jena.vocabulary.OWL;
import com.github.owlcs.ontapi.jena.vocabulary.RDF;
import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.impl.RDFListImpl;
import org.apache.jena.util.iterator.ExtendedIterator;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Implementation for anonymous {@code owl:AllDisjointProperties}, {@code owl:AllDisjointClasses}, {@code owl:AllDifferent} sections.
 * <p>
 * Created by @ssz on 15.11.2016.
 */
@SuppressWarnings("WeakerAccess")
public abstract class OntDisjointImpl<O extends OntObject> extends OntObjectImpl implements OntDisjoint<O> {
    public static final OntFinder PROPERTIES_FINDER = new OntFinder.ByType(OWL.AllDisjointProperties);

    public static ObjectFactory disjointClassesFactory = createFactory(ClassesImpl.class,
            OWL.AllDisjointClasses, OntClass.class, true, OWL.members);

    public static ObjectFactory differentIndividualsFactory = createFactory(IndividualsImpl.class,
            OWL.AllDifferent, OntIndividual.class, true, OWL.members, OWL.distinctMembers);

    public static ObjectFactory objectPropertiesFactory =
            createFactory(ObjectPropertiesImpl.class, OWL.AllDisjointProperties, OntObjectProperty.class, false, OWL.members);

    public static ObjectFactory dataPropertiesFactory = createFactory(DataPropertiesImpl.class,
            OWL.AllDisjointProperties, OntDataProperty.class, false, OWL.members);

    public static ObjectFactory abstractPropertiesFactory = Factories.createFrom(PROPERTIES_FINDER
            , ObjectProperties.class
            , DataProperties.class);

    public static OntFinder DISJOINT_FINDER = Factories.createFinder(OWL.AllDisjointClasses,
            OWL.AllDifferent, OWL.AllDisjointProperties);
    public static ObjectFactory abstractDisjointFactory = Factories.createFrom(DISJOINT_FINDER
            , ObjectProperties.class
            , DataProperties.class
            , Classes.class
            , Individuals.class);

    public OntDisjointImpl(Node n, EnhGraph m) {
        super(n, m);
    }

    @Override
    public Optional<OntStatement> findRootStatement() {
        return getRequiredRootStatement(this, getResourceType());
    }

    protected Property getPredicate() {
        return OWL.members;
    }

    protected abstract Class<O> getComponentType();

    protected abstract Resource getResourceType();

    @Override
    public Stream<O> members() {
        return Iterators.asStream(listMembers());
    }

    public ExtendedIterator<O> listMembers() {
        return getList().listMembers();
    }

    @Override
    public OntListImpl<O> getList() {
        return getModel().asOntList(getRequiredObject(getPredicate(), RDFList.class),
                this, getPredicate(), getComponentType());
    }

    @Override
    public ExtendedIterator<OntStatement> listSpec() {
        return Iterators.concat(super.listSpec(), getList().listContent());
    }

    private static ObjectFactory createFactory(Class<? extends OntDisjointImpl<?>> impl,
                                               Resource type,
                                               Class<? extends RDFNode> view,
                                               boolean allowEmptyList,
                                               Property... predicates) {
        OntMaker maker = new OntMaker.WithType(impl, type);
        OntFinder finder = new OntFinder.ByType(type);
        OntFilter filter = OntFilter.BLANK.and(new OntFilter.HasType(type));
        return Factories.createCommon(maker, finder, filter
                .and(getHasPredicatesFilter(predicates))
                .and(getHasMembersOfFilter(view, allowEmptyList, predicates)));
    }

    private static OntFilter getHasPredicatesFilter(Property... predicates) {
        OntFilter res = OntFilter.FALSE;
        for (Property p : predicates) {
            res = res.or(new OntFilter.HasPredicate(p));
        }
        return res;
    }

    private static OntFilter getHasMembersOfFilter(Class<? extends RDFNode> view,
                                                   boolean allowEmptyList,
                                                   Property... predicates) {
        return (node, eg) -> {
            ExtendedIterator<Node> res = listRoots(node, eg.asGraph(), predicates);
            try {
                while (res.hasNext()) {
                    if (testList(res.next(), eg, view, allowEmptyList)) return true;
                }
            } finally {
                res.close();
            }
            return false;
        };
    }

    private static ExtendedIterator<Node> listRoots(Node node, Graph graph, Property... predicates) {
        return Iterators.flatMap(Iterators.of(predicates),
                p -> graph.find(node, p.asNode(), Node.ANY).mapWith(Triple::getObject));
    }

    private static boolean testList(Node node, EnhGraph graph, Class<? extends RDFNode> view, boolean allowEmptyList) {
        if (!RDFListImpl.factory.canWrap(node, graph)) return false;
        if (view == null) return true;
        RDFList list = RDFListImpl.factory.wrap(node, graph).as(RDFList.class);
        return (list.isEmpty() && allowEmptyList) ||
                Iterators.anyMatch(list.iterator().mapWith(RDFNode::asNode), n -> PersonalityModel.canAs(view, n, graph));
    }

    public static Classes createDisjointClasses(OntGraphModelImpl model, Stream<OntClass> classes) {
        return create(model, OWL.AllDisjointClasses, Classes.class, OntClass.class, classes);
    }

    /**
     * Creates blank node {@code _:x rdf:type owl:AllDifferent. _:x owl:members (a1 ... an).}
     * <p>
     * Note: the predicate is {@link OWL#members owl:members},
     * not {@link OWL#distinctMembers owl:distinctMembers} (but the last one is correct also)
     * It is chosen as the preferred from considerations of uniformity.
     *
     * @param model       {@link OntGraphModelImpl}
     * @param individuals stream of {@link OntIndividual}
     * @return {@link Individuals}
     * @see <a href='https://www.w3.org/TR/owl2-quick-reference/#Additional_Vocabulary_in_OWL_2_RDF_Syntax'>4.2 Additional Vocabulary in OWL 2 RDF Syntax</a>
     */
    public static Individuals createDifferentIndividuals(OntGraphModelImpl model, Stream<OntIndividual> individuals) {
        return create(model, OWL.AllDifferent, Individuals.class, OntIndividual.class, individuals);
    }

    public static ObjectProperties createDisjointObjectProperties(OntGraphModelImpl model, Stream<OntObjectProperty> properties) {
        return create(model, OWL.AllDisjointProperties, ObjectProperties.class, OntObjectProperty.class, properties);
    }

    public static DataProperties createDisjointDataProperties(OntGraphModelImpl model, Stream<OntDataProperty> properties) {
        return create(model, OWL.AllDisjointProperties, DataProperties.class, OntDataProperty.class, properties);
    }

    public static <R extends OntDisjoint<?>, E extends OntObject> R create(OntGraphModelImpl model,
                                                                           Resource type,
                                                                           Class<R> resultType,
                                                                           Class<E> memberType,
                                                                           Stream<E> members) {
        OntJenaException.notNull(members, "Null " + OntObjectImpl.viewAsString(memberType) + " members stream.");
        Resource res = model.createResource()
                .addProperty(RDF.type, type)
                .addProperty(OWL.members, model.createList(members
                        .peek(x -> OntJenaException.notNull(x,
                                "OntDisjoint: Null " + OntObjectImpl.viewAsString(memberType) + " is specified"))
                        .iterator()));
        return model.getNodeAs(res.asNode(), resultType);
    }

    public static class ClassesImpl extends OntDisjointImpl<OntClass> implements Classes {
        public ClassesImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        public Class<? extends OntObject> getActualClass() {
            return Classes.class;
        }

        @Override
        protected Class<OntClass> getComponentType() {
            return OntClass.class;
        }

        @Override
        protected Resource getResourceType() {
            return OWL.AllDisjointClasses;
        }
    }

    public static class IndividualsImpl extends OntDisjointImpl<OntIndividual> implements Individuals {
        public IndividualsImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        public ExtendedIterator<OntIndividual> listMembers() {
            return Iterators.flatMap(lists(), OntListImpl::listMembers);
        }

        @Override
        public ExtendedIterator<OntStatement> listSpec() {
            return Iterators.concat(super.listSpec(), Iterators.flatMap(lists(), OntListImpl::listContent));
        }

        public ExtendedIterator<Property> listPredicates() {
            return Iterators.of(getPredicate(), getAlternativePredicate());
        }

        public ExtendedIterator<OntListImpl<OntIndividual>> lists() {
            return listPredicates()
                    .mapWith(this::findList)
                    .filterKeep(Optional::isPresent)
                    .mapWith(Optional::get);
        }

        @Override
        public OntListImpl<OntIndividual> getList() {
            Optional<OntListImpl<OntIndividual>> p = findList(getPredicate());
            Optional<OntListImpl<OntIndividual>> a = findList(getAlternativePredicate());
            if (p.isPresent() && a.isPresent()) {
                if (p.get().size() > a.get().size()) return p.get();
                if (p.get().size() < a.get().size()) return a.get();
            }
            if (p.isPresent()) {
                return p.get();
            }
            if (a.isPresent()) {
                return a.get();
            }
            throw new OntJenaException.IllegalState("Neither owl:members or owl:distinctMembers could be found");
        }

        public Optional<OntListImpl<OntIndividual>> findList(Property predicate) {
            if (!hasProperty(predicate)) return Optional.empty();
            return Optional.of(getModel().asOntList(getRequiredObject(predicate, RDFList.class),
                    this, predicate, getComponentType()));
        }

        @Override
        public Class<? extends OntObject> getActualClass() {
            return Individuals.class;
        }

        protected Property getAlternativePredicate() {
            return OWL.distinctMembers;
        }

        @Override
        protected Class<OntIndividual> getComponentType() {
            return OntIndividual.class;
        }

        @Override
        protected Resource getResourceType() {
            return OWL.AllDifferent;
        }
    }

    public abstract static class PropertiesImpl<P extends OntRealProperty>
            extends OntDisjointImpl<P> implements Properties<P> {

        public PropertiesImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        protected Resource getResourceType() {
            return OWL.AllDisjointProperties;
        }
    }

    public static class ObjectPropertiesImpl extends PropertiesImpl<OntObjectProperty> implements ObjectProperties {
        public ObjectPropertiesImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        public Class<? extends OntObject> getActualClass() {
            return ObjectProperties.class;
        }

        @Override
        protected Class<OntObjectProperty> getComponentType() {
            return OntObjectProperty.class;
        }
    }

    public static class DataPropertiesImpl extends PropertiesImpl<OntDataProperty> implements DataProperties {
        public DataPropertiesImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        public Class<? extends OntObject> getActualClass() {
            return DataProperties.class;
        }

        @Override
        protected Class<OntDataProperty> getComponentType() {
            return OntDataProperty.class;
        }
    }
}
