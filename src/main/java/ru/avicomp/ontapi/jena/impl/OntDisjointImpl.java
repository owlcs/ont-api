/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2018, Avicomp Services, AO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package ru.avicomp.ontapi.jena.impl;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.impl.RDFListImpl;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.impl.conf.*;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.utils.Iter;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * for anonymous owl:AllDisjointProperties,  owl:AllDisjointClasses, owl:AllDifferent sections.
 * <p>
 * Created by @szuev on 15.11.2016.
 */
public abstract class OntDisjointImpl<O extends OntObject> extends OntObjectImpl implements OntDisjoint<O> {
    public static final OntFinder PROPERTIES_FINDER = new OntFinder.ByType(OWL.AllDisjointProperties);

    public static OntObjectFactory disjointClassesFactory =
            createFactory(ClassesImpl.class, OWL.AllDisjointClasses, OntCE.class, true, OWL.members);

    public static OntObjectFactory differentIndividualsFactory =
            createFactory(IndividualsImpl.class, OWL.AllDifferent, OntIndividual.class, true, OWL.members, OWL.distinctMembers);

    public static OntObjectFactory objectPropertiesFactory =
            createFactory(ObjectPropertiesImpl.class, OWL.AllDisjointProperties, OntOPE.class, false, OWL.members);

    public static OntObjectFactory dataPropertiesFactory =
            createFactory(DataPropertiesImpl.class, OWL.AllDisjointProperties, OntNDP.class, false, OWL.members);

    public static OntObjectFactory abstractPropertiesFactory = new MultiOntObjectFactory(PROPERTIES_FINDER, null, objectPropertiesFactory, dataPropertiesFactory);
    public static OntObjectFactory abstractDisjointFactory = new MultiOntObjectFactory(OntFinder.TYPED, null, abstractPropertiesFactory, disjointClassesFactory, differentIndividualsFactory);

    public OntDisjointImpl(Node n, EnhGraph m) {
        super(n, m);
    }

    protected abstract Stream<Property> predicates();

    protected abstract Class<O> componentClass();

    @Override
    public Stream<O> members() {
        return predicates().map(p -> rdfListMembers(p, componentClass())).flatMap(Function.identity());
    }

    @Override
    public Stream<OntStatement> spec() {
        Stream<OntStatement> thisDeclaration = super.spec();
        Stream<OntStatement> listDeclaration = predicates().map(this::statement).filter(Optional::isPresent).map(Optional::get);
        Stream<OntStatement> listContent = predicates().map(this::rdfListContent).flatMap(Function.identity());
        return Stream.of(thisDeclaration, listDeclaration, listContent).flatMap(Function.identity());
    }

    private static OntObjectFactory createFactory(Class<? extends OntDisjointImpl> impl,
                                                  Resource type,
                                                  Class<? extends RDFNode> view,
                                                  boolean allowEmptyList,
                                                  Property... predicates) {
        OntMaker maker = new OntMaker.WithType(impl, type);
        OntFinder finder = new OntFinder.ByType(type);
        OntFilter filter = OntFilter.BLANK.and(new OntFilter.HasType(type));
        return new CommonOntObjectFactory(maker, finder, filter
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

    private static OntFilter getHasMembersOfFilter(Class<? extends RDFNode> view, boolean allowEmptyList, Property... predicates) {
        return (node, eg) -> {
            try (Stream<Node> nodes = listRoots(node, eg.asGraph(), predicates)) {
                return nodes.anyMatch(n -> testList(n, eg, view, allowEmptyList));
            }
        };
    }

    private static Stream<Node> listRoots(Node node, Graph graph, Property... predicates) {
        return Stream.of(predicates)
                .map(predicate -> Iter.asStream(graph.find(node, predicate.asNode(), Node.ANY).mapWith(Triple::getObject)))
                .flatMap(Function.identity());
    }

    private static boolean testList(Node node, EnhGraph graph, Class<? extends RDFNode> view, boolean allowEmptyList) {
        if (!RDFListImpl.factory.canWrap(node, graph)) return false;
        if (view == null) return true;
        RDFList list = RDFListImpl.factory.wrap(node, graph).as(RDFList.class);
        return (list.isEmpty() && allowEmptyList) || Iter.asStream(list.iterator())
                .map(RDFNode::asNode)
                .anyMatch(n -> OntObjectImpl.canAs(view, n, graph));
    }

    public static Classes createDisjointClasses(OntGraphModelImpl model, Stream<OntCE> classes) {
        OntJenaException.notNull(classes, "Null classes stream.");
        Resource res = model.createResource();
        res.addProperty(RDF.type, OWL.AllDisjointClasses);
        res.addProperty(OWL.members, model.createList(classes.iterator()));
        return model.getNodeAs(res.asNode(), Classes.class);
    }

    /**
     * Creates blank node "_:x rdf:type owl:AllDifferent. _:x owl:members (a1 … an)."
     * <p>
     * Note: the predicate is "owl:members", not "owl:distinctMembers" (but the last one is correct also)
     * see <a href='https://www.w3.org/TR/owl2-quick-reference/#Additional_Vocabulary_in_OWL_2_RDF_Syntax'>4.2 Additional Vocabulary in OWL 2 RDF Syntax</a>
     *
     * @param model       {@link OntGraphModelImpl}
     * @param individuals stream of {@link OntIndividual}
     * @return {@link ru.avicomp.ontapi.jena.model.OntDisjoint.Individuals}
     */
    public static Individuals createDifferentIndividuals(OntGraphModelImpl model, Stream<OntIndividual> individuals) {
        OntJenaException.notNull(individuals, "Null individuals stream.");
        Resource res = model.createResource();
        res.addProperty(RDF.type, OWL.AllDifferent);
        res.addProperty(OWL.members, model.createList(individuals.iterator()));
        return model.getNodeAs(res.asNode(), Individuals.class);
    }

    public static ObjectProperties createDisjointObjectProperties(OntGraphModelImpl model, Stream<OntOPE> properties) {
        OntJenaException.notNull(properties, "Null properties stream.");
        Resource res = model.createResource();
        res.addProperty(RDF.type, OWL.AllDisjointProperties);
        res.addProperty(OWL.members, model.createList(properties.iterator()));
        return model.getNodeAs(res.asNode(), ObjectProperties.class);
    }

    public static DataProperties createDisjointDataProperties(OntGraphModelImpl model, Stream<OntNDP> properties) {
        OntJenaException.notNull(properties, "Null properties stream.");
        Resource res = model.createResource();
        res.addProperty(RDF.type, OWL.AllDisjointProperties);
        res.addProperty(OWL.members, model.createList(properties.iterator()));
        return model.getNodeAs(res.asNode(), DataProperties.class);
    }

    public static class ClassesImpl extends OntDisjointImpl<OntCE> implements Classes {
        public ClassesImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        protected Stream<Property> predicates() {
            return Stream.of(OWL.members);
        }

        @Override
        protected Class<OntCE> componentClass() {
            return OntCE.class;
        }
    }

    public static class IndividualsImpl extends OntDisjointImpl<OntIndividual> implements Individuals {
        public IndividualsImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        protected Stream<Property> predicates() {
            return Stream.of(OWL.members, OWL.distinctMembers);
        }

        @Override
        protected Class<OntIndividual> componentClass() {
            return OntIndividual.class;
        }
    }

    public abstract static class PropertiesImpl<P extends OntPE> extends OntDisjointImpl<P> implements Properties<P> {

        public PropertiesImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        protected Stream<Property> predicates() {
            return Stream.of(OWL.members);
        }
    }

    public static class ObjectPropertiesImpl extends PropertiesImpl<OntOPE> implements ObjectProperties {
        public ObjectPropertiesImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        protected Class<OntOPE> componentClass() {
            return OntOPE.class;
        }
    }

    public static class DataPropertiesImpl extends PropertiesImpl<OntNDP> implements DataProperties {
        public DataPropertiesImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        protected Class<OntNDP> componentClass() {
            return OntNDP.class;
        }
    }
}
