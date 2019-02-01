/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2019, Avicomp Services, AO
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

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.InfModelImpl;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.util.iterator.NullIterator;
import org.apache.jena.util.iterator.WrappedIterator;
import org.apache.jena.vocabulary.RDFS;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.UnionGraph;
import ru.avicomp.ontapi.jena.impl.conf.OntPersonality;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.utils.Graphs;
import ru.avicomp.ontapi.jena.utils.Iter;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

import java.io.OutputStream;
import java.io.Writer;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Base model ONT-API implementation to work through jena only.
 * This is an analogue of {@link org.apache.jena.ontology.impl.OntModelImpl} to work in accordance with OWL2 DL specification.
 * <p>
 * Created by @szuev on 27.10.2016.
 *
 * @see UnionGraph
 */
@SuppressWarnings({"WeakerAccess", "SameParameterValue"})
public class OntGraphModelImpl extends UnionModel implements OntGraphModel, PersonalityModel {

    /**
     * @param graph       {@link Graph}
     * @param personality {@link OntPersonality}
     */
    public OntGraphModelImpl(Graph graph, OntPersonality personality) {
        super(graph, OntPersonality.asJenaPersonality(personality));
    }

    @Override
    public OntPersonality getOntPersonality() {
        return (OntPersonality) super.getPersonality();
    }

    @Override
    public OntID getID() {
        return getNodeAs(Graphs.ontologyNode(getBaseGraph())
                .orElseGet(() -> createResource().addProperty(RDF.type, OWL.Ontology).asNode()), OntID.class);
    }

    @Override
    public OntID setID(String uri) {
        return getNodeAs(createOntologyID(getBaseModel(), uri).asNode(), OntID.class);
    }

    /**
     * Creates a fresh ontology resource (i.e. {@code @uri rdf:type owl:Ontology} triple)
     * and moves to it all content from existing ontology resources (if they present).
     *
     * @param model {@link Model} graph holder
     * @param uri   String an ontology iri, null for anonymous ontology
     * @return {@link Resource} in model
     * @throws OntJenaException if creation is not possible by some reason.
     */
    public static Resource createOntologyID(Model model, String uri) throws OntJenaException {
        return createOntologyID(model, uri == null ? NodeFactory.createBlankNode() : NodeFactory.createURI(uri));
    }

    /**
     * Creates a fresh ontology resource from the given {@link Node}
     * and moves to it all content from existing ontology resources (if they present).
     *
     * @param model {@link Model} graph holder, not {@code null}
     * @param node  {@link Node}, must be either uri or blank, not {@code null}
     * @return {@link Resource} in model
     * @throws OntJenaException         in case the given node is uri and it takes part in {@code owl:imports}
     * @throws IllegalArgumentException in case the given node is not uri or blank (i.e. literal)
     */
    public static Resource createOntologyID(Model model, Node node) throws OntJenaException, IllegalArgumentException {
        if (!Objects.requireNonNull(node, "Null node").isURI() && !node.isBlank())
            throw new IllegalArgumentException("Expected uri or blank node: " + node);
        List<Statement> prev = Iter.flatMap(model.listStatements(null, RDF.type, OWL.Ontology),
                s -> s.getSubject().listProperties()).toList();
        if (prev.stream()
                .filter(s -> OWL.imports.equals(s.getPredicate()))
                .map(Statement::getObject)
                .filter(RDFNode::isURIResource)
                .map(RDFNode::asNode).anyMatch(node::equals)) {
            throw new OntJenaException.IllegalArgument("Can't create ontology: " +
                    "the specified uri (<" + node + ">) is present in the imports.");
        }
        model.remove(prev);
        Resource res = model.wrapAsResource(node).addProperty(RDF.type, OWL.Ontology);
        prev.forEach(s -> res.addProperty(s.getPredicate(), s.getObject()));
        return res;
    }

    @Override
    public OntGraphModelImpl addImport(OntGraphModel m) {
        if (Objects.requireNonNull(m, "Null model specified.").getID().isAnon()) {
            throw new OntJenaException.IllegalArgument("Anonymous sub models are not allowed.");
        }
        String importsURI = Objects.requireNonNull(m.getID().getImportsIRI());
        if (importsURI.equals(getID().getURI())) {
            throw new OntJenaException.IllegalArgument("Attempt to import ontology with the same name: " + importsURI);
        }
        if (hasImport(importsURI)) {
            throw new OntJenaException.IllegalArgument("Ontology <" + importsURI + "> is already in imports.");
        }
        addImportModel(m.getGraph(), importsURI);
        return this;
    }

    @Override
    public boolean hasImport(OntGraphModel m) {
        Objects.requireNonNull(m);
        return findImport(x -> Graphs.isSameBase(x.getGraph(), m.getGraph())).isPresent();
    }

    @Override
    public boolean hasImport(String uri) {
        return findImport(x -> Objects.equals(x.getID().getImportsIRI(), uri)).isPresent();
    }

    @Override
    public OntGraphModelImpl removeImport(OntGraphModel m) {
        Objects.requireNonNull(m);
        findImport(x -> Graphs.isSameBase(x.getGraph(), m.getGraph()))
                .ifPresent(x -> removeImportModel(x.getGraph(), x.getID().getImportsIRI()));
        return this;
    }

    @Override
    public OntGraphModelImpl removeImport(String uri) {
        findImport(x -> Objects.equals(uri, x.getID().getImportsIRI()))
                .ifPresent(x -> removeImportModel(x.getGraph(), x.getID().getImportsIRI()));
        return this;
    }

    @Override
    public Stream<OntGraphModel> imports() {
        return imports(getOntPersonality());
    }

    /**
     * Lists all top-level sub-models built with the the given {@code personality}.
     *
     * @param personality {@link OntPersonality}, not {@code null}
     * @return Stream of {@link OntGraphModel}s
     */
    public Stream<OntGraphModel> imports(OntPersonality personality) {
        return importModels(personality).map(Function.identity());
    }

    /**
     * Finds a model impl from the internals using the given {@code filter}.
     *
     * @param filter {@code Predicate} to filter {@link OntGraphModelImpl}s
     * @return {@code Optional} around {@link OntGraphModelImpl}
     */
    protected Optional<OntGraphModelImpl> findImport(Predicate<OntGraphModelImpl> filter) {
        return importModels(getOntPersonality()).filter(filter).findFirst();
    }

    /**
     * Adds the graph-uri pair into the internals.
     *
     * @param g {@link Graph}, not {@code null}
     * @param u String, not {@code null}
     */
    protected void addImportModel(Graph g, String u) {
        getGraph().addGraph(g);
        getID().addImport(u);
    }

    /**
     * Removes the graph-uri pair from the internals.
     *
     * @param g {@link Graph}, not {@code null}
     * @param u String, not {@code null}
     */
    protected void removeImportModel(Graph g, String u) {
        getGraph().removeGraph(g);
        getID().removeImport(u);
    }

    /**
     * Lists {@link OntGraphModelImpl model impl}s with the specified {@code personality}
     * from the top tier of the imports hierarchy.
     *
     * @param personality {@link OntPersonality}, not {@code null}
     * @return Stream of {@link OntGraphModelImpl}s
     * @since 1.4.0
     */
    protected final Stream<OntGraphModelImpl> importModels(OntPersonality personality) {
        return importGraphs().map(u -> new OntGraphModelImpl(u, personality));
    }

    /**
     * Lists all {@link OntGraphModelImpl model impl}s with the specified {@code personality}
     * from the whole imports hierarchy.
     *
     * @param personality {@link OntPersonality}, not {@code null}
     * @return Stream of {@link OntGraphModelImpl}s
     * @since 1.4.0
     */
    protected final Stream<OntGraphModelImpl> allModels(OntPersonality personality) {
        return Iter.asStream(getGraph().listUnionGraphs().mapWith(u -> new OntGraphModelImpl(u, personality)));
    }

    /**
     * Lists all top-level {@link UnionGraph}s of the model's hierarchy.
     *
     * @return Stream of {@link UnionGraph}
     * @since 1.4.0
     */
    protected final Stream<UnionGraph> importGraphs() {
        return Iter.asStream(getGraph().getUnderlying().listGraphs()
                .filterKeep(x -> x instanceof UnionGraph)
                .mapWith(x -> (UnionGraph) x));
    }

    /**
     * Gets the top-level {@link OntGraphModelImpl Ontology Graph Model impl}.
     * The returned model may contain import declarations, but cannot contain sub-models.
     * Be warned: any listeners, attached on the {@link #getGraph()}
     *
     * @return {@link OntGraphModelImpl}
     * @see #getBaseModel()
     * @since 1.3.0
     */
    public OntGraphModelImpl getTopModel() {
        if (independent()) {
            return this;
        }
        return new OntGraphModelImpl(getBaseGraph(), getOntPersonality());
    }

    /**
     * Determines whether this model is independent.
     *
     * @return {@code true} if this model is independent of others
     */
    @Override
    public boolean independent() {
        return getGraph().getUnderlying().isEmpty();
    }

    @Override
    public InfModel getInferenceModel(Reasoner reasoner) {
        return new InfModelImpl(OntJenaException.notNull(reasoner, "Null reasoner.").bind(getGraph()));
    }

    /**
     * Answers {@code true} if the given entity is built-in.
     *
     * @param e   {@link OntEntity} object impl
     * @param <E> subtype of {@link OntObjectImpl} and {@link OntEntity}
     * @return boolean
     */
    @SuppressWarnings("unchecked")
    public <E extends OntObjectImpl & OntEntity> boolean isBuiltIn(E e) {
        return getOntPersonality().getBuiltins()
                .get((Class<? extends OntEntity>) e.getActualClass())
                .contains(e.asNode());
    }

    /**
     * Retrieves the stream of {@link OntObject Ontology Object}s.
     * The result object will be cached inside model.
     *
     * @param type {@link Class} the type of {@link OntObject}, not null
     * @param <O>  subtype of {@link OntObject}
     * @return Stream of {@link OntObject}s
     */
    @Override
    public <O extends OntObject> Stream<O> ontObjects(Class<? extends O> type) {
        return Iter.asStream(listOntObjects(type));
    }

    /**
     * Lists all {@link OntObject Ontology Object}s and caches them inside this model.
     *
     * @param type {@link Class} the type of {@link OntObject}, not null
     * @param <O>  subtype of {@link OntObject}
     * @return {@link ExtendedIterator Extended Iterator} of {@link OntObject}s
     */
    public <O extends OntObject> ExtendedIterator<O> listOntObjects(Class<? extends O> type) {
        return listOntObjects(this, type);
    }

    /**
     * The same as {@link #listOntObjects(Class)}, but for the base graph.
     *
     * @param type {@link Class} the type of {@link OntObject}, not null
     * @param <O>  subtype of {@link OntObject}
     * @return {@link ExtendedIterator Extended Iterator} of {@link OntObject}s
     */
    public <O extends OntObject> ExtendedIterator<O> listLocalOntObjects(Class<? extends O> type) {
        return listOntObjects(getTopModel(), type);
    }

    /**
     * Lists all {@code OntObject}s for the given {@code OntGraphModelImpl}.
     *
     * @param m    {@link OntGraphModelImpl} the impl to cache
     * @param type {@link Class} the type of {@link OntObject}, not null
     * @param <O>  subtype of {@link OntObject}
     * @return {@link ExtendedIterator Extended Iterator} of {@link OntObject}s
     */
    public static <O extends OntObject> ExtendedIterator<O> listOntObjects(OntGraphModelImpl m,
                                                                           Class<? extends O> type) {
        return m.getOntPersonality().getObjectFactory(type).iterator(m).mapWith(e -> m.getNodeAs(e.asNode(), type));
    }

    @Override
    public Stream<OntEntity> ontEntities() {
        /*return Iter.asStream(listSubjectsWithProperty(RDF.type))
                .filter(RDFNode::isURIResource)
                .flatMap(r -> OntEntity.entityTypes().map(t -> getOntEntity(t, r)).filter(Objects::nonNull));*/
        // this looks faster:
        return Iter.asStream(listOntEntities());
    }

    /**
     * Lists all Ontology Entities.
     * Built-ins are not included.
     *
     * @return {@link ExtendedIterator Extended Iterator} of {@link OntEntity}s
     * @see #listLocalOntEntities()
     * @see OntEntity#listEntityTypes()
     */
    public ExtendedIterator<OntEntity> listOntEntities() {
        return Iter.flatMap(OntEntity.listEntityTypes(), this::listOntObjects);
    }

    /**
     * The same as {@link #listOntEntities()} but for the base graph.
     *
     * @return {@link ExtendedIterator Extended Iterator} of {@link OntEntity}s
     * @see #listOntEntities()
     */
    public ExtendedIterator<OntEntity> listLocalOntEntities() {
        return Iter.flatMap(OntEntity.listEntityTypes(), this::listLocalOntObjects);
    }

    /**
     * Gets 'punnings', i.e. the {@link OntEntity}s which have not only single type.
     *
     * @param withImports if false takes into account only base model
     * @return Stream of {@link OntEntity}s.
     */
    public Stream<OntEntity> ambiguousEntities(boolean withImports) {
        Set<Class<? extends OntEntity>> types = OntEntity.listEntityTypes().toSet();
        return ontEntities().filter(e -> withImports || e.isLocal()).filter(e -> types.stream()
                .filter(view -> e.canAs(view) && (withImports || e.as(view).isLocal())).count() > 1);
    }

    @Override
    public Stream<OntIndividual> classAssertions() {
        return statements(null, RDF.type, null)
                .filter(s -> s.getObject().canAs(OntCE.class))
                .map(OntStatement::getSubject)
                .map(s -> findNodeAs(s.asNode(), OntIndividual.class))
                .filter(Objects::nonNull);
    }

    @Override
    public <E extends OntEntity> E getOntEntity(Class<E> type, String uri) {
        return findNodeAs(NodeFactory.createURI(OntJenaException.notNull(uri, "Null uri.")), type);
    }

    @Override
    public <T extends OntEntity> T createOntEntity(Class<T> type, String iri) {
        try {
            return createOntObject(type, iri);
        } catch (OntJenaException.Creation e) { // illegal punning:
            throw new OntJenaException(String.format("Can't add entity [%s: %s]: perhaps it's illegal punning.",
                    type.getSimpleName(), iri), e);
        }
    }

    /**
     * Creates and caches an ontology object resource by the given type and uri.
     *
     * @param type Class, object type
     * @param uri  String, URI (IRI), can be {@code null} for anonymous resource
     * @param <T>  class-type of {@link OntObject}
     * @return {@link OntObject}, new instance
     */
    public <T extends OntObject> T createOntObject(Class<T> type, String uri) {
        Node key = Graphs.createNode(uri);
        T res = getOntPersonality().getObjectFactory(type).createInGraph(key, this).as(type);
        getNodeCache().put(key, res);
        return res;
    }

    @Override
    public OntGraphModelImpl removeOntObject(OntObject obj) {
        obj.clearAnnotations().content()
                .peek(OntStatement::clearAnnotations)
                .collect(Collectors.toSet()).forEach(this::remove);
        getNodeCache().remove(obj.asNode());
        return this;
    }

    @Override
    public OntGraphModelImpl removeOntStatement(OntStatement statement) {
        statement.clearAnnotations();
        return remove(statement);
    }

    @Override
    public Stream<OntStatement> statements() {
        return Iter.asStream(listStatements().mapWith(OntStatement.class::cast));
    }

    @Override
    public Stream<OntStatement> statements(Resource s, Property p, RDFNode o) {
        return Iter.asStream(listOntStatements(s, p, o));
    }

    @Override
    public Stream<OntStatement> localStatements(Resource s, Property p, RDFNode o) {
        return Iter.asStream(listLocalStatements(s, p, o));
    }

    /**
     * {@inheritDoc}
     *
     * @param s {@link Resource} the subject sought, can be {@code null}
     * @param p {@link Property} the predicate sought, can be {@code null}
     * @param o {@link RDFNode} the object sought, can be {@code null}
     * @return {@link StmtIterator} of {@link OntStatement}s
     */
    @Override
    public StmtIterator listStatements(Resource s, Property p, RDFNode o) {
        return Iter.createStmtIterator(getGraph().find(asNode(s), asNode(p), asNode(o)), this::asStatement);
    }

    /**
     * Returns an {@link ExtendedIterator extended iterator} over all the statements in the model that match a pattern.
     * The statements selected are those whose subject matches the {@code s} argument,
     * whose predicate matches the {@code p} argument
     * and whose object matches the {@code o} argument.
     * If an argument is {@code null} it matches anything.
     * The method is equivalent to the expression {@code listStatements(s, p, o).mapWith(OntStatement.class::cast)}.
     *
     * @param s {@link Resource} the subject sought, can be {@code null}
     * @param p {@link Property} the predicate sought, can be {@code null}
     * @param o {@link RDFNode} the object sought, can be {@code null}
     * @return {@link ExtendedIterator} of {@link OntStatement}s
     * @see #listStatements(Resource, Property, RDFNode)
     * @since 1.3.0
     */
    public ExtendedIterator<OntStatement> listOntStatements(Resource s, Property p, RDFNode o) {
        return WrappedIterator.create(getGraph().find(asNode(s), asNode(p), asNode(o)).mapWith(this::asStatement));
    }

    /**
     * Lists all statements in the <b>base</b> model that match a pattern
     * in the form of {@link ExtendedIterator Extended Iterator}.
     * The method is equivalent to the expression
     * {@code listStatements(s, p, o).mapWith(OntStatement.class::cast).filterKeep(OntStatement::isLocal)}.
     *
     * @param s {@link Resource} the subject sought, can be {@code null}
     * @param p {@link Property} the predicate sought, can be {@code null}
     * @param o {@link RDFNode} the object sought, can be {@code null}
     * @return {@link ExtendedIterator} of {@link OntStatement}s, which are local to the base graph
     * @see #listStatements(Resource, Property, RDFNode)
     * @since 1.3.0
     */
    public ExtendedIterator<OntStatement> listLocalStatements(Resource s, Property p, RDFNode o) {
        return WrappedIterator.create(getBaseGraph().find(asNode(s), asNode(p), asNode(o)).mapWith(this::asStatement));
    }

    @Override
    public OntStatementImpl createStatement(Resource s, Property p, RDFNode o) {
        return OntStatementImpl.createOntStatementImpl(s, p, o, this);
    }

    @Override
    public OntStatement asStatement(Triple triple) {
        return OntStatementImpl.createOntStatementImpl(triple, this);
    }

    /**
     * Deletes the specified {@code OntList} including its annotations.
     *
     * @param subject   {@link OntObject} the subject of the OntList root statement
     * @param predicate {@link Property} the predicate of the OntList root statement
     * @param object    {@link OntList} to be deleted
     */
    public void deleteOntList(OntObject subject, Property predicate, OntList object) {
        Objects.requireNonNull(subject);
        Objects.requireNonNull(predicate);
        OntJenaException.notNull(object, "Null list for subject " + subject + " and predicate " + predicate);
        object.getRoot().clearAnnotations();
        object.clear();
        subject.remove(predicate, object);
    }

    @Override
    public <E extends OntEntity> Stream<E> ontBuiltins(Class<E> type, boolean local) {
        return getBuiltinEntities(type, local).stream();
    }

    /**
     * Gets all builtin entities of the given type and returns them as {@code Set}.
     * It is not expected a huge amount of builtins,
     * so a {@code Set} is more applicable here as returned object than {@code Stream} or {@code ExtendedIterator}.
     *
     * @param type  a concrete class-type of entity
     * @param local if {@code true} only the base graph is considered
     * @param <E>   any subtype of {@link OntEntity}
     * @return Stream of builtin {@link OntEntity}s
     */
    public <E extends OntEntity> Set<E> getBuiltinEntities(Class<E> type, boolean local) {
        if (OntClass.class == type) {
            return getEntitySet(OntClassImpl::getBuiltinClasses, local);
        }
        // TODO:
        throw new UnsupportedOperationException("TODO");
    }

    @SuppressWarnings("unchecked")
    private <E extends OntEntity> Set<E> getEntitySet(Function<OntGraphModelImpl, Set<? extends OntEntity>> getLocalSet,
                                                      boolean local) {
        if (local) {
            return (Set<E>) getLocalSet.apply(this);
        }
        Set<E> res = new HashSet<>();
        allModels(getOntPersonality())
                .map(getLocalSet::apply).forEach(x -> res.addAll((Set<E>) x));
        return Collections.unmodifiableSet(res);
    }

    /**
     * Extracts all members from []-list, that is an object in a <b>local</b> SPO with the specified predicate.
     * The returned iterator includes the subject of the []-list root statement, if it is specified.
     *
     * @param subject        {@code Class}-type of subject
     * @param predicate      {@link Property}
     * @param object         {@code Class}-type of object
     * @param includeSubject if {@code true} then the subject of []-list root statement is also included
     * @param <O>            subtype of {@link OntObject}, the []-list members type
     * @param <S>            subtype of {@link OntObject} in the subject position of the SPO, where O is a []-list
     * @return {@link ExtendedIterator} of {@link O}s
     */
    protected <O extends OntObject, S extends O> ExtendedIterator<O> fromLocalList(Class<S> subject,
                                                                                   Property predicate,
                                                                                   Class<O> object,
                                                                                   boolean includeSubject) {
        return Iter.flatMap(listLocalStatements(null, predicate, null), s -> {
            S a = findNodeAs(s.getSubject().asNode(), subject);
            if (a == null) return NullIterator.instance();
            if (!s.getObject().canAs(RDFList.class)) return NullIterator.instance();
            OntListImpl<O> list = OntListImpl.asOntList(s.getObject().as(RDFList.class),
                    this, s.getSubject(), predicate, null, object);
            if (!includeSubject) return list.listMembers();
            return Iter.concat(Iter.of(a), list.listMembers());
        });
    }

    /**
     * Lists all <b>local</b> objects for the given predicate and types of subject and object.
     *
     * @param subject   {@code Class}-type of subject
     * @param predicate {@link Property}
     * @param object    {@code Class}-type of object
     * @param <O>       subtype of {@link OntObject} in the object position of the found SPO
     * @param <S>       subtype of {@link OntObject} in the subject position of the found SPO
     * @return {@link ExtendedIterator} of {@link O}s
     */
    protected <O extends OntObject, S extends OntObject> ExtendedIterator<O> listLocalObjects(Class<S> subject,
                                                                                              Property predicate,
                                                                                              Class<O> object) {
        return listLocalStatements(null, predicate, null).mapWith(s -> {
            S left = findNodeAs(s.getSubject().asNode(), subject);
            return left == null ? null : findNodeAs(s.getObject().asNode(), object);
        }).filterDrop(Objects::isNull);
    }

    /**
     * Lists <b>local</b> subjects and objects for the given predicate and the type of subject and object.
     *
     * @param predicate {@link Property}
     * @param type      {@code Class}-type of subject and object
     * @param <R>       subtype of {@link OntObject}, S and P from SPO must be of this type
     * @return {@link ExtendedIterator} of {@link R}s
     */
    protected <R extends OntObject> ExtendedIterator<R> listLocalSubjectAndObjects(Property predicate,
                                                                                   Class<R> type) {
        return Iter.flatMap(listLocalStatements(null, predicate, null), s -> {
            R a = findNodeAs(s.getSubject().asNode(), type);
            if (a == null) return NullIterator.instance();
            R b = findNodeAs(s.getObject().asNode(), type);
            if (b == null) return NullIterator.instance();
            return Iter.of(a, b);
        });
    }

    @Override
    public OntDisjoint.Classes createDisjointClasses(Collection<OntCE> classes) {
        return OntDisjointImpl.createDisjointClasses(this, classes.stream());
    }

    @Override
    public OntDisjoint.Individuals createDifferentIndividuals(Collection<OntIndividual> individuals) {
        return OntDisjointImpl.createDifferentIndividuals(this, individuals.stream());
    }

    @Override
    public OntDisjoint.ObjectProperties createDisjointObjectProperties(Collection<OntOPE> properties) {
        return OntDisjointImpl.createDisjointObjectProperties(this, properties.stream());
    }

    @Override
    public OntDisjoint.DataProperties createDisjointDataProperties(Collection<OntNDP> properties) {
        return OntDisjointImpl.createDisjointDataProperties(this, properties.stream());
    }

    @Override
    public <T extends OntFR> T createFacetRestriction(Class<T> view, Literal literal) {
        return OntFRImpl.create(this, view, literal);
    }

    @Override
    public OntDR.OneOf createOneOfDataRange(Collection<Literal> values) {
        return OntDRImpl.createOneOf(this, values.stream());
    }

    @Override
    public OntDR.Restriction createRestrictionDataRange(OntDT datatype, Collection<OntFR> values) {
        return OntDRImpl.createRestriction(this, datatype, values.stream());
    }

    @Override
    public OntDR.ComplementOf createComplementOfDataRange(OntDR other) {
        return OntDRImpl.createComplementOf(this, other);
    }

    @Override
    public OntDR.UnionOf createUnionOfDataRange(Collection<OntDR> values) {
        return OntDRImpl.createUnionOf(this, values.stream());
    }

    @Override
    public OntDR.IntersectionOf createIntersectionOfDataRange(Collection<OntDR> values) {
        return OntDRImpl.createIntersectionOf(this, values.stream());
    }

    @Override
    public OntCE.ObjectSomeValuesFrom createObjectSomeValuesFrom(OntOPE onProperty, OntCE other) {
        return OntCEImpl.createComponentRestrictionCE(this, OntCE.ObjectSomeValuesFrom.class, onProperty, other, OWL.someValuesFrom);
    }

    @Override
    public OntCE.DataSomeValuesFrom createDataSomeValuesFrom(OntNDP onProperty, OntDR other) {
        return OntCEImpl.createComponentRestrictionCE(this, OntCE.DataSomeValuesFrom.class, onProperty, other, OWL.someValuesFrom);
    }

    @Override
    public OntCE.ObjectAllValuesFrom createObjectAllValuesFrom(OntOPE onProperty, OntCE other) {
        return OntCEImpl.createComponentRestrictionCE(this, OntCE.ObjectAllValuesFrom.class, onProperty, other, OWL.allValuesFrom);
    }

    @Override
    public OntCE.DataAllValuesFrom createDataAllValuesFrom(OntNDP onProperty, OntDR other) {
        return OntCEImpl.createComponentRestrictionCE(this, OntCE.DataAllValuesFrom.class, onProperty, other, OWL.allValuesFrom);
    }

    @Override
    public OntCE.ObjectHasValue createObjectHasValue(OntOPE onProperty, OntIndividual other) {
        return OntCEImpl.createComponentRestrictionCE(this, OntCE.ObjectHasValue.class, onProperty, other, OWL.hasValue);
    }

    @Override
    public OntCE.DataHasValue createDataHasValue(OntNDP onProperty, Literal other) {
        return OntCEImpl.createComponentRestrictionCE(this, OntCE.DataHasValue.class, onProperty, other, OWL.hasValue);
    }

    @Override
    public OntCE.ObjectMinCardinality createObjectMinCardinality(OntOPE onProperty, int cardinality, OntCE onObject) {
        return OntCEImpl.createCardinalityRestrictionCE(this, OntCE.ObjectMinCardinality.class, onProperty, cardinality, onObject);
    }

    @Override
    public OntCE.DataMinCardinality createDataMinCardinality(OntNDP onProperty, int cardinality, OntDR onObject) {
        return OntCEImpl.createCardinalityRestrictionCE(this, OntCE.DataMinCardinality.class, onProperty, cardinality, onObject);
    }

    @Override
    public OntCE.ObjectMaxCardinality createObjectMaxCardinality(OntOPE onProperty, int cardinality, OntCE onObject) {
        return OntCEImpl.createCardinalityRestrictionCE(this, OntCE.ObjectMaxCardinality.class, onProperty, cardinality, onObject);
    }

    @Override
    public OntCE.DataMaxCardinality createDataMaxCardinality(OntNDP onProperty, int cardinality, OntDR onObject) {
        return OntCEImpl.createCardinalityRestrictionCE(this, OntCE.DataMaxCardinality.class, onProperty, cardinality, onObject);
    }

    @Override
    public OntCE.ObjectCardinality createObjectCardinality(OntOPE onProperty, int cardinality, OntCE onObject) {
        return OntCEImpl.createCardinalityRestrictionCE(this, OntCE.ObjectCardinality.class, onProperty, cardinality, onObject);
    }

    @Override
    public OntCE.DataCardinality createDataCardinality(OntNDP onProperty, int cardinality, OntDR onObject) {
        return OntCEImpl.createCardinalityRestrictionCE(this, OntCE.DataCardinality.class, onProperty, cardinality, onObject);
    }

    @Override
    public OntCE.UnionOf createUnionOf(Collection<OntCE> classes) {
        return OntCEImpl.createComponentsCE(this, OntCE.UnionOf.class, OWL.unionOf, classes.iterator());
    }

    @Override
    public OntCE.IntersectionOf createIntersectionOf(Collection<OntCE> classes) {
        return OntCEImpl.createComponentsCE(this, OntCE.IntersectionOf.class, OWL.intersectionOf, classes.iterator());
    }

    @Override
    public OntCE.OneOf createOneOf(Collection<OntIndividual> individuals) {
        return OntCEImpl.createComponentsCE(this, OntCE.OneOf.class, OWL.oneOf, individuals.iterator());
    }

    @Override
    public OntCE.HasSelf createHasSelf(OntOPE onProperty) {
        return OntCEImpl.createHasSelf(this, onProperty);
    }

    @Override
    public OntCE.NaryDataAllValuesFrom createDataAllValuesFrom(Collection<OntNDP> onProperties, OntDR other) {
        //todo
        throw new OntJenaException.Unsupported("TODO: " + OntCE.NaryDataAllValuesFrom.class);
    }

    @Override
    public OntCE.NaryDataSomeValuesFrom createDataSomeValuesFrom(Collection<OntNDP> onProperties, OntDR other) {
        //todo
        throw new OntJenaException.Unsupported("TODO: " + OntCE.NaryDataSomeValuesFrom.class);
    }

    @Override
    public OntCE.ComplementOf createComplementOf(OntCE other) {
        return OntCEImpl.createComplementOf(this, other);
    }

    @Override
    public OntSWRL.Variable createSWRLVariable(String uri) {
        return OntSWRLImpl.createVariable(this, uri);
    }

    @Override
    public OntSWRL.Atom.BuiltIn createBuiltInSWRLAtom(Resource predicate, Collection<OntSWRL.DArg> arguments) {
        return OntSWRLImpl.createBuiltInAtom(this, predicate, arguments);
    }

    @Override
    public OntSWRL.Atom.OntClass createClassSWRLAtom(OntCE clazz, OntSWRL.IArg arg) {
        return OntSWRLImpl.createClassAtom(this, clazz, arg);
    }

    @Override
    public OntSWRL.Atom.DataRange createDataRangeSWRLAtom(OntDR range, OntSWRL.DArg arg) {
        return OntSWRLImpl.createDataRangeAtom(this, range, arg);
    }

    @Override
    public OntSWRL.Atom.DataProperty createDataPropertySWRLAtom(OntNDP dataProperty,
                                                                OntSWRL.IArg firstArg,
                                                                OntSWRL.DArg secondArg) {
        return OntSWRLImpl.createDataPropertyAtom(this, dataProperty, firstArg, secondArg);
    }

    @Override
    public OntSWRL.Atom.ObjectProperty createObjectPropertySWRLAtom(OntOPE dataProperty,
                                                                    OntSWRL.IArg firstArg,
                                                                    OntSWRL.IArg secondArg) {
        return OntSWRLImpl.createObjectPropertyAtom(this, dataProperty, firstArg, secondArg);
    }

    @Override
    public OntSWRL.Atom.DifferentIndividuals createDifferentIndividualsSWRLAtom(OntSWRL.IArg firstArg,
                                                                                OntSWRL.IArg secondArg) {
        return OntSWRLImpl.createDifferentIndividualsAtom(this, firstArg, secondArg);
    }

    @Override
    public OntSWRL.Atom.SameIndividuals createSameIndividualsSWRLAtom(OntSWRL.IArg firstArg,
                                                                      OntSWRL.IArg secondArg) {
        return OntSWRLImpl.createSameIndividualsAtom(this, firstArg, secondArg);
    }

    @Override
    public OntSWRL.Imp createSWRLImp(Collection<OntSWRL.Atom> head, Collection<OntSWRL.Atom> body) {
        return OntSWRLImpl.createImp(this, head, body);
    }

    public PrefixMapping getPrefixMapping() {
        return getGraph().getPrefixMapping();
    }

    @Override
    public OntGraphModelImpl setNsPrefix(String prefix, String uri) {
        getPrefixMapping().setNsPrefix(prefix, uri);
        return this;
    }

    @Override
    public OntGraphModelImpl removeNsPrefix(String prefix) {
        getPrefixMapping().removeNsPrefix(prefix);
        return this;
    }

    @Override
    public OntGraphModelImpl clearNsPrefixMap() {
        getPrefixMapping().clearNsPrefixMap();
        return this;
    }

    @Override
    public OntGraphModelImpl setNsPrefixes(PrefixMapping pm) {
        getPrefixMapping().setNsPrefixes(pm);
        return this;
    }

    @Override
    public OntGraphModelImpl setNsPrefixes(Map<String, String> map) {
        getPrefixMapping().setNsPrefixes(map);
        return this;
    }

    @Override
    public OntGraphModelImpl withDefaultMappings(PrefixMapping other) {
        getPrefixMapping().withDefaultMappings(other);
        return this;
    }

    @Override
    public OntGraphModelImpl lock() {
        getPrefixMapping().lock();
        return this;
    }

    @Override
    public OntGraphModelImpl add(Statement s) {
        super.add(s);
        return this;
    }

    @Override
    public OntGraphModelImpl remove(Statement s) {
        super.remove(s);
        return this;
    }

    @Override
    public OntGraphModelImpl add(Resource s, Property p, RDFNode o) {
        super.add(s, p, o);
        return this;
    }

    @Override
    public OntGraphModelImpl remove(Resource s, Property p, RDFNode o) {
        super.remove(s, p, o);
        return this;
    }

    @Override
    public OntGraphModelImpl add(Model m) {
        super.add(m);
        return this;
    }

    @Override
    public OntGraphModelImpl remove(Model m) {
        super.remove(m);
        return this;
    }

    @Override
    public OntGraphModelImpl add(StmtIterator iter) {
        super.add(iter);
        return this;
    }

    @Override
    public OntGraphModelImpl remove(StmtIterator iter) {
        super.remove(iter);
        return this;
    }

    @Override
    public OntGraphModelImpl removeAll(Resource s, Property p, RDFNode o) {
        super.removeAll(s, p, o);
        return this;
    }

    @Override
    public OntGraphModelImpl removeAll() {
        super.removeAll();
        return this;
    }

    @Override
    public OntGraphModelImpl write(Writer writer) {
        super.write(writer);
        return this;
    }

    @Override
    public OntGraphModelImpl write(Writer writer, String lang) {
        super.write(writer, lang);
        return this;
    }

    @Override
    public OntGraphModelImpl write(Writer writer, String lang, String base) {
        super.write(writer, lang, base);
        return this;
    }

    @Override
    public OntGraphModelImpl write(OutputStream out) {
        super.write(out);
        return this;
    }

    @Override
    public OntGraphModelImpl write(OutputStream out, String lang) {
        super.write(out, lang);
        return this;
    }

    @Override
    public OntGraphModelImpl write(OutputStream out, String lang, String base) {
        super.write(out, lang, base);
        return this;
    }

    @Override
    public OntNAP getRDFSComment() {
        return findNodeAs(RDFS.Nodes.comment, OntNAP.class);
    }

    @Override
    public OntNAP getRDFSLabel() {
        return findNodeAs(RDFS.Nodes.label, OntNAP.class);
    }

    @Override
    public OntClass getOWLThing() {
        return findNodeAs(OWL.Thing.asNode(), OntClass.class);
    }

    @Override
    public OntClass getOWLNothing() {
        return findNodeAs(OWL.Nothing.asNode(), OntClass.class);
    }

    @Override
    public OntNOP getOWLTopObjectProperty() {
        return findNodeAs(OWL.topObjectProperty.asNode(), OntNOP.class);
    }

    @Override
    public OntNOP getOWLBottomObjectProperty() {
        return findNodeAs(OWL.bottomObjectProperty.asNode(), OntNOP.class);
    }

    @Override
    public OntNDP getOWLTopDataProperty() {
        return findNodeAs(OWL.topDataProperty.asNode(), OntNDP.class);
    }

    @Override
    public OntNDP getOWLBottomDataProperty() {
        return findNodeAs(OWL.bottomDataProperty.asNode(), OntNDP.class);
    }

    @Override
    public String toString() {
        return String.format("OntGraphModel{%s}", Graphs.getName(getBaseGraph()));
    }


}
