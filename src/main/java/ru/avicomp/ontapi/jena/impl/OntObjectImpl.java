/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2017, Avicomp Services, AO
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
import org.apache.jena.graph.FrontsNode;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.RDFListImpl;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.shared.PropertyNotFoundException;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.impl.configuration.*;
import ru.avicomp.ontapi.jena.model.OntObject;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.utils.Iter;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * base resource.
 * <p>
 * Created by szuev on 03.11.2016.
 */
@SuppressWarnings("WeakerAccess")
public class OntObjectImpl extends ResourceImpl implements OntObject {

    public static Configurable<OntObjectFactory> objectFactory = m ->
            new CommonOntObjectFactory(new OntMaker.Default(OntObjectImpl.class), OntFinder.ANY_SUBJECT, OntFilter.URI.or(OntFilter.BLANK));

    public OntObjectImpl(Node n, EnhGraph m) {
        super(n, m);
    }

    OntStatement addType(Resource type) {
        return addStatement(RDF.type, OntJenaException.notNull(type, "Null rdf:type"));
    }

    void removeType(Resource type) {
        remove(RDF.type, type);
    }

    void changeType(Resource property, boolean add) {
        if (add) {
            addType(property);
        } else {
            removeType(property);
        }
    }

    @Override
    public OntStatement getRoot() {
        try (Stream<OntStatement> types = types().map(t -> getModel().createOntStatement(true, this, RDF.type, t))) {
            return types.findFirst().orElse(null);
        }
    }

    protected OntStatement getRoot(Property property, Resource type) {
        return hasProperty(property, type) ? getModel().createOntStatement(true, this, property, type) : null;
    }

    @Override
    public Stream<OntStatement> content() {
        OntStatement root = getRoot();
        return root == null ? Stream.empty() : Stream.of(root);
    }

    @Override
    public boolean isLocal() {
        OntStatement declaration = getRoot(); // built-ins have null root-declaration
        return declaration != null && declaration.isLocal();
    }

    @Override
    public Optional<OntStatement> statement(Property property) {
        try (Stream<OntStatement> statements = statements(property)) {
            return statements.findFirst();
        }
    }

    @Override
    public Optional<OntStatement> statement(Property property, RDFNode object) {
        try (Stream<OntStatement> statements = statements(property).filter(s -> s.getObject().equals(object))) {
            return statements.findFirst();
        }
    }

    @Override
    public Stream<Resource> types() {
        return objects(RDF.type, Resource.class);
    }

    @Override
    public boolean hasType(Resource type) {
        try (Stream<Resource> types = types()) {
            return types.anyMatch(type::equals);
        }
    }

    @Override
    public OntStatement getRequiredProperty(Property property) {
        return statement(property).orElseThrow(() -> new PropertyNotFoundException(property));
    }

    @Override
    public OntStatement addStatement(Property property, RDFNode value) {
        Statement st = getModel().createStatement(this,
                OntJenaException.notNull(property, "Null property."),
                OntJenaException.notNull(value, "Null value."));
        getModel().add(st);
        return getModel().toOntStatement(getRoot(), st);
    }

    @Override
    public void remove(Property property, RDFNode value) {
        getModel().removeAll(this, OntJenaException.notNull(property, "Null property."), OntJenaException.notNull(value, "Null value."));
    }

    @Override
    public Stream<OntStatement> statements(Property property) {
        OntStatement root = getRoot();
        return Iter.asStream(listProperties(property))
                .map(s -> getModel().toOntStatement(root, s));
        //return statements().filter(s -> s.getPredicate().equals(property));
    }

    @Override
    public Stream<OntStatement> statements() {
        OntStatement main = getRoot();
        return Iter.asStream(listProperties())
                .map(s -> getModel().toOntStatement(main, s));
    }

    @Override
    public Literal asLiteral() {
        return as(Literal.class);
    }

    /**
     * gets rdf:List content as Stream of RDFNode's.
     * if the object is not rdf:List then empty stream expected.
     * if there are several lists with the same predicate the contents of all will be merged.
     * <p>
     * Note: here we use the "tolerant" approach.
     * Generally speaking, the case when we have several lists on a single predicate is _not_ correct (in terms of OWL2).
     * This case indicates that we deal with the wrong ontology.
     *
     * @param property the predicate to search for rdf:List.
     * @return Distinct Stream of RDFNode (maybe empty if there are no rdf:List)
     */
    public Stream<RDFNode> rdfListMembers(Property property) {
        return Iter.asStream(listProperties(property)
                .mapWith(Statement::getObject)
                .filterKeep(n -> n.canAs(RDFList.class))
                .mapWith(n -> n.as(RDFList.class)))
                .map(RDFList::asJavaList)
                .map(Collection::stream)
                .flatMap(Function.identity()).distinct();
    }

    /**
     * gets the stream of nodes with the specified type from rdf:List.
     * Note: In OWL2 the type of rdf:List members is always the same (with except of owl:hasKey construction).
     *
     * @param predicate to search for rdf:Lists
     * @param view      Class, the type of returned nodes.
     * @param <O>       a class-type of rdf-node
     * @return Stream of {@link RDFNode} with specified type.
     */
    public <O extends RDFNode> Stream<O> rdfListMembers(Property predicate, Class<O> view) {
        return rdfListMembers(predicate).map(n -> getModel().getNodeAs(n.asNode(), view));
    }

    /**
     * gets content of list in view of OntStatement streams.
     * Note: if there are several rdf:List objects the contents will be merged.
     *
     * @param property predicate
     * @return Stream
     */
    public Stream<OntStatement> rdfListContent(Property property) {
        OntStatement r = getRoot();
        return Iter.asStream(listProperties(property)
                .mapWith(Statement::getObject)
                .filterKeep(n -> n.canAs(RDFList.class))
                .mapWith(n -> n.as(RDFList.class)))
                .map(RDFListImpl.class::cast)
                .filter(list -> !list.isEmpty())
                .map(RDFListImpl::collectStatements)
                .map(Collection::stream)
                .flatMap(Function.identity())
                .map(s -> getModel().toOntStatement(r, s));
    }

    /**
     * removes all objects for predicate (if object is rdf:List removes all content)
     *
     * @param predicate Property
     */
    public void clearAll(Property predicate) {
        listProperties(predicate).mapWith(Statement::getObject)
                .filterKeep(n -> n.canAs(RDFList.class))
                .mapWith(n -> n.as(RDFList.class)).forEachRemaining(RDFList::removeList);
        removeAll(predicate);
    }


    public <T extends RDFNode> T getObject(Property predicate, Class<T> view) {
        return object(predicate, view).orElse(null);
    }

    public <T extends RDFNode> T getRequiredObject(Property predicate, Class<T> view) {
        return object(predicate, view)
                .orElseThrow(OntJenaException.supplier(String.format("Can't find required object [%s @%s %s]", this, predicate, view)));
    }

    public Stream<RDFNode> objects(Property predicate) {
        return Iter.asStream(listProperties(predicate).mapWith(Statement::getObject));
    }

    public <T extends RDFNode> Optional<T> object(Property predicate, Class<T> view) {
        try (Stream<T> objects = objects(predicate, view)) {
            return objects.findFirst();
        }
    }

    @Override
    public <O extends RDFNode> Stream<O> objects(Property predicate, Class<O> view) {
        return objects(predicate).filter(node -> node.canAs(view)).map(FrontsNode::asNode).map(node -> getModel().getNodeAs(node, view));
    }

    @Override
    public OntGraphModelImpl getModel() {
        return (OntGraphModelImpl) super.getModel();
    }

    @SuppressWarnings("unchecked")
    public Class<? extends OntObject> getActualClass() {
        return Arrays.stream(getClass().getInterfaces()).filter(OntObject.class::isAssignableFrom).map(c -> (Class<? extends OntObject>) c).findFirst().orElse(null);
    }

    public static String toString(Class<? extends RDFNode> view) {
        return view.getName().replace(OntObject.class.getPackage().getName() + ".", "");
    }

    @Override
    public String toString() {
        Class<? extends RDFNode> view = getActualClass();
        return view == null ? super.toString() : String.format("%s(%s)", asNode(), toString(view));
    }

    public static Node checkNamed(Node res) {
        if (OntJenaException.notNull(res, "Null node").isURI()) {
            return res;
        }
        throw new OntJenaException("Not uri node " + res);
    }

    public static Resource checkNamed(Resource res) {
        if (OntJenaException.notNull(res, "Null resource").isURIResource()) {
            return res;
        }
        throw new OntJenaException("Not uri resource " + res);
    }

    @SafeVarargs
    protected static Configurable<MultiOntObjectFactory> createMultiFactory(OntFinder finder, Configurable<? extends OntObjectFactory>... factories) {
        return createMultiFactory(finder, null, factories);
    }

    @SafeVarargs
    protected static Configurable<MultiOntObjectFactory> createMultiFactory(OntFinder finder, OntFilter filter, Configurable<? extends OntObjectFactory>... factories) {
        return mode -> new MultiOntObjectFactory(finder, filter, Stream.of(factories).map(c -> c.get(mode)).toArray(OntObjectFactory[]::new));
    }

}
