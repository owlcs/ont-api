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
import org.apache.jena.enhanced.UnsupportedPolymorphismException;
import org.apache.jena.graph.FrontsNode;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.shared.PropertyNotFoundException;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.impl.conf.*;
import ru.avicomp.ontapi.jena.model.OntAnnotation;
import ru.avicomp.ontapi.jena.model.OntNAP;
import ru.avicomp.ontapi.jena.model.OntObject;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.utils.Iter;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * base resource.
 * <p>
 * Created by szuev on 03.11.2016.
 */
@SuppressWarnings("WeakerAccess")
public class OntObjectImpl extends ResourceImpl implements OntObject {

    public static OntObjectFactory objectFactory = new CommonOntObjectFactory(new OntMaker.Default(OntObjectImpl.class), OntFinder.ANY_SUBJECT, OntFilter.URI.or(OntFilter.BLANK));

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
        return rootStatement().orElse(null);
    }

    public Optional<OntStatement> rootStatement() {
        OntGraphModelImpl m = getModel();
        try (Stream<RDFNode> objects = m.statements(this, RDF.type, null).map(Statement::getObject)) {
            return objects.findFirst()
                    .map(o -> m.createStatement(this, RDF.type, o))
                    .map(OntStatementImpl::asRootStatement);
        }
    }

    protected OntStatement getDeclarationStatement(Resource type) {
        return getRoot(RDF.type, Objects.requireNonNull(type));
    }

    protected OntStatement getRoot(Property property, Resource type) {
        return hasProperty(property, type) ? getModel().createStatement(this, property, type).asRootStatement() : null;
    }

    @Override
    public Stream<OntStatement> spec() {
        return rootStatement().map(Stream::of).orElse(Stream.empty());
    }

    @Override
    public Stream<OntStatement> content() {
        return Stream.concat(spec(), statements().filter(x -> !x.isAnnotation()).collect(Collectors.toSet()).stream()).distinct();
    }

    @Override
    public boolean isLocal() {
        OntStatement root = getRoot(); // built-ins have null root-declaration
        return root != null && root.isLocal();
    }

    @Override
    public Optional<OntStatement> statement(Property property) {
        try (Stream<OntStatement> res = statements(property)) {
            return res.findFirst();
        }
    }

    @Override
    public Optional<OntStatement> statement(Property property, RDFNode object) {
        try (Stream<OntStatement> res = statements(property).filter(s -> Objects.equals(s.getObject(), object))) {
            return res.findFirst();
        }
    }

    /**
     * Returns an ont-statement with given subject and property.
     * If more than one statement with the given subject and property exists in the model, it is undefined which will be returned.
     * If none exist, an exception is thrown.
     *
     * @param property {@link Property}, the predicate
     * @return {@link OntStatement}
     * @throws PropertyNotFoundException no statement are found
     */
    @Override
    public OntStatement getRequiredProperty(Property property) {
        return statement(property).orElseThrow(() -> new PropertyNotFoundException(property));
    }

    @Override
    public OntStatement addStatement(Property property, RDFNode value) {
        OntStatement res = getModel().createStatement(this,
                OntJenaException.notNull(property, "Null property."),
                OntJenaException.notNull(value, "Null value."));
        getModel().add(res);
        return res;
    }

    @Override
    public OntObjectImpl remove(Property property, RDFNode value) {
        getModel().removeAll(this, OntJenaException.notNull(property, "Null property."), OntJenaException.notNull(value, "Null value."));
        return this;
    }

    @Override
    public Stream<OntStatement> statements(Property property) {
        return Iter.asStream(listProperties(property)).map(OntStatement.class::cast);
    }

    @Override
    public Stream<OntStatement> statements() {
        return Iter.asStream(listProperties()).map(OntStatement.class::cast);
    }

    @Override
    public StmtIterator listProperties() {
        return listProperties(null);
    }

    @Override
    public StmtIterator listProperties(Property p) {
        return Iter.createStmtIterator(getModel().getGraph().find(asNode(), OntGraphModelImpl.asNode(p), Node.ANY),
                t -> createOntStatement(p, t));
    }

    protected OntStatement createOntStatement(Property p, Triple t) {
        OntGraphModelImpl m = getModel();
        Property property = p == null ? m.getNodeAs(t.getPredicate(), Property.class) : p;
        RDFNode object = m.getNodeAs(t.getObject(), RDFNode.class);
        return OntStatementImpl.createOntStatementImpl(this, property, object, getModel());
    }

    @Override
    public Stream<OntStatement> annotations() {
        Stream<OntStatement> res = assertions();
        Optional<OntStatement> main = rootStatement();
        if (main.isPresent()) {
            res = Stream.concat(res, main.get().annotationResources().flatMap(OntAnnotation::assertions));
        }
        return res;
    }

    public Stream<OntStatement> assertions() {
        return statements().filter(OntStatement::isAnnotation);
    }

    /**
     * Adds an annotation assertion.
     * It could be expanded to bulk form by adding sub-annotation.
     *
     * @param property {@link OntNAP}, Named annotation property.
     * @param value    {@link RDFNode} the value: uri-resource, literal or anonymous individual.
     * @return OntStatement for newly added annotation
     * @throws OntJenaException in case input is wrong
     */
    @Override
    public OntStatement addAnnotation(OntNAP property, RDFNode value) {
        return rootStatement().map(r -> r.addAnnotation(property, value))
                .orElseGet(() -> getModel().createStatement(addProperty(property, value), property, value));
    }

    @Override
    public OntObject clearAnnotations() {
        // for built-ins
        assertions().peek(OntStatement::clearAnnotations).collect(Collectors.toSet()).forEach(a -> getModel().remove(a));
        // for others
        rootStatement().ifPresent(OntStatement::clearAnnotations);
        return this;
    }

    /**
     * OntObject is allowed to present literals.
     *
     * @return {@link Literal}
     * @throws UnsupportedPolymorphismException if not a literal
     */
    @Override
    public Literal asLiteral() throws UnsupportedPolymorphismException {
        return as(Literal.class);
    }

    /**
     * Gets rdf:List content as Stream of RDFNode's.
     * if the object is not rdf:List then empty stream expected.
     * if there are several lists with the same predicate then contents all of them will be merged.
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
                .flatMap(l -> Iter.asStream(l.iterator()))
                .distinct();
    }

    /**
     * Removes all objects for predicate (if object is rdf:List removes all content)
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

    /**
     * Returns an object from a first found statement with specified predicate.
     * Since the order in the graph is undefined
     * in case there are more then one statement for a property the result is unpredictable.
     * TODO: throw exception in case there is more than one object
     *
     * @param predicate {@link Property}
     * @param view      Class
     * @param <T>       {@link RDFNode} type
     * @return an object from statement
     * @throws OntJenaException in case no object by predicate has been found
     * @see #getRequiredProperty(Property)
     */
    public <T extends RDFNode> T getRequiredObject(Property predicate, Class<T> view) {
        return object(predicate, view)
                .orElseThrow(() -> new OntJenaException(String.format("Can't find required object [%s @%s %s]", this, predicate, viewAsString(view))));
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
        return objects(predicate)
                .filter(n -> n.canAs(view))
                .map(FrontsNode::asNode)
                .map(n -> getModel().getNodeAs(n, view));
    }

    @Override
    public OntGraphModelImpl getModel() {
        return (OntGraphModelImpl) super.getModel();
    }

    /**
     * Gets a public ont-object type identifier.
     *
     * @return Class, the actual type of this object
     */
    @SuppressWarnings("unchecked")
    public Class<? extends OntObject> getActualClass() {
        return Arrays.stream(getClass().getInterfaces())
                .filter(OntObject.class::isAssignableFrom)
                .map(c -> (Class<? extends OntObject>) c)
                .findFirst().orElse(null);
    }

    static String viewAsString(Class<? extends RDFNode> view) {
        return view.getName().replace(OntObject.class.getPackage().getName() + ".", "");
    }

    @Override
    public String toString() {
        Class<? extends RDFNode> view = getActualClass();
        return view == null ? super.toString() : String.format("[%s]%s", viewAsString(view), asNode());
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

    protected static Configurable<OntObjectFactory> buildMultiFactory(OntFinder finder,
                                                                      OntFilter filter,
                                                                      Configurable<? extends OntObjectFactory> configurable,
                                                                      OntObjectFactory... other) {
        return mode -> new MultiOntObjectFactory(finder, filter,
                Stream.concat(Stream.of(configurable.get(mode)), Arrays.stream(other)).toArray(OntObjectFactory[]::new));
    }

    @SuppressWarnings({"unchecked", "SameParameterValue"})
    protected static Configurable<OntObjectFactory> concatFactories(OntFinder finder,
                                                                    Configurable<? extends OntObjectFactory>... factories) {
        return mode -> new MultiOntObjectFactory(finder, null, Stream.of(factories).map(c -> c.get(mode)).toArray(OntObjectFactory[]::new));
    }

    protected static boolean canAs(Class<? extends RDFNode> view, Node node, EnhGraph graph) {
        return ((OntGraphModelImpl) graph).fetchNodeAs(node, view) != null;
    }

}
