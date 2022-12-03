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
import com.github.owlcs.ontapi.jena.UnionGraph;
import com.github.owlcs.ontapi.jena.impl.conf.OntPersonality;
import com.github.owlcs.ontapi.jena.model.*;
import com.github.owlcs.ontapi.jena.utils.Graphs;
import com.github.owlcs.ontapi.jena.utils.Iter;
import com.github.owlcs.ontapi.jena.vocabulary.OWL;
import com.github.owlcs.ontapi.jena.vocabulary.RDF;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.InfModelImpl;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDFS;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.*;
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
public class OntGraphModelImpl extends UnionModel implements OntModel, PersonalityModel {

    // the model's types mapper
    protected final Map<String, RDFDatatype> dtTypes = new HashMap<>();

    /**
     * @param graph       {@link Graph}
     * @param personality {@link OntPersonality}
     */
    public OntGraphModelImpl(Graph graph, OntPersonality personality) {
        super(graph, OntPersonality.asJenaPersonality(personality));
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
     * @throws OntJenaException         in case the given node is uri, and it takes part in {@code owl:imports}
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

    /**
     * Lists all {@code OntObject}s for the given {@code OntGraphModelImpl}.
     *
     * @param m    {@link OntGraphModelImpl} the impl to cache
     * @param type {@link Class} the type of {@link OntObject}, not null
     * @param <M>  a subtype of {@link EnhGraph} and {@link PersonalityModel}
     * @param <O>  subtype of {@link OntObject}
     * @return an {@link ExtendedIterator Extended Iterator} of {@link OntObject}s
     */
    public static <M extends EnhGraph & PersonalityModel, O extends OntObject> ExtendedIterator<O> listOntObjects(M m,
                                                                                                                  Class<? extends O> type) {
        return m.getOntPersonality().getObjectFactory(type).iterator(m).mapWith(e -> m.getNodeAs(e.asNode(), type));
    }

    /**
     * Filters {@code OntIndividual}s from the specified {@code ExtendedIterator}.
     *
     * @param model      {@link M}, not {@code null}
     * @param system     a {@code Set} of {@link Node}s,
     *                   that cannot be treated as {@link OntClass Ontology Class}es, not {@code null}
     * @param assertions {@link ExtendedIterator} of {@link Triple}s
     *                   with the {@link RDF#type rdf:type} as predicate, not {@code null}
     * @param <M>        a subtype of {@link OntModel} and {@link PersonalityModel}
     * @return {@link ExtendedIterator} of {@link OntIndividual}s that are attached to the {@code model}
     */
    public static <M extends OntModel & PersonalityModel> ExtendedIterator<OntIndividual> listIndividuals(M model,
                                                                                                          Set<Node> system,
                                                                                                          ExtendedIterator<Triple> assertions) {
        Set<Triple> seen = new HashSet<>();
        return assertions
                .mapWith(t -> {
                    // to speed up the process,
                    // the investigation (that includes TTO, PS, HP, GALEN, FAMILY and PIZZA ontologies),
                    // shows that the profit exists, and it is significant sometimes:
                    if (system.contains(t.getObject())) {
                        return null;
                    }

                    // skip duplicates (an individual may have several class-assertions):
                    if (seen.remove(t)) {
                        return null;
                    }
                    // checking for the primary rule that determines a class assertion.
                    // use cache - it couldn't hurt, classes are often used
                    if (model.findNodeAs(t.getObject(), OntClass.class) == null) {
                        return null;
                    }
                    return model.asStatement(t);
                })
                .filterKeep(s -> {
                    if (s == null) return false;
                    // an individual may have a factory with punnings restrictions,
                    // so need to check its type also.
                    // this time do not cache in model
                    OntIndividual i = s.getSubject().getAs(OntIndividual.class);
                    if (i == null) return false;

                    // update the set with duplicates to ensure the stream is distinct
                    ((OntIndividualImpl) i).listClasses()
                            .forEachRemaining(x -> {
                                if (s.getObject().equals(x)) {
                                    // skip this statement, otherwise all individuals fall into memory
                                    return;
                                }
                                seen.add(Triple.create(i.asNode(), RDF.Nodes.type, x.asNode()));
                            });
                    return true;
                })
                .mapWith(s -> s.getSubject(OntIndividual.class));
    }

    /**
     * Creates a {@code Stream} for a graph.
     *
     * @param graph    {@link Graph} to test
     * @param it       {@code ExtendedIterator} obtained from the {@code graph}
     * @param withSize if {@code true} attempts to include graph size as an estimated size of a future {@code Stream}
     * @param <X>      type of stream items
     * @return {@code Stream} of {@link X}s
     */
    private static <X> Stream<X> asStream(Graph graph,
                                          ExtendedIterator<X> it,
                                          boolean withSize) {
        int characteristics = getSpliteratorCharacteristics(graph);
        long size = -1;
        if (withSize && Graphs.isSized(graph)) {
            size = Graphs.size(graph);
            characteristics = characteristics | Spliterator.SIZED;
        }
        return Iter.asStream(it, size, characteristics);
    }

    /**
     * Returns a {@link Spliterator} characteristics based on graph analysis.
     *
     * @param graph {@link Graph}
     * @return int
     */
    protected static int getSpliteratorCharacteristics(Graph graph) {
        // a graph cannot return iterator with null-elements
        int res = Spliterator.NONNULL;
        if (Graphs.isDistinct(graph)) {
            return res | Spliterator.DISTINCT;
        }
        return res;
    }

    /**
     * Answers {@code true} iff the given {@code SPO} corresponds {@link Triple#ANY}.
     *
     * @param s {@link Resource}, the subject
     * @param p {@link Property}, the predicate
     * @param o {@link RDFNode}, the object
     * @return boolean
     */
    private static boolean isANY(Resource s, Property p, RDFNode o) {
        if (s != null) return false;
        if (p != null) return false;
        return o == null;
    }

    @Override
    public OntPersonality getOntPersonality() {
        return (OntPersonality) super.getPersonality();
    }

    @Override
    public OntID getID() {
        return getNodeAs(Graphs.ontologyNode(getBaseGraph())
                .orElseGet(() -> createResource(OWL.Ontology).asNode()), OntID.class);
    }

    @Override
    public Optional<OntID> id() {
        return Graphs.ontologyNode(getBaseGraph()).map(x -> getNodeAs(x, OntID.class));
    }

    @Override
    public OntID setID(String uri) {
        return getNodeAs(createOntologyID(getBaseModel(), uri).asNode(), OntID.class);
    }

    @Override
    public OntGraphModelImpl addImport(OntModel m) {
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
    public boolean hasImport(OntModel m) {
        Objects.requireNonNull(m);
        return findImport(x -> Graphs.isSameBase(x.getGraph(), m.getGraph())).isPresent();
    }

    @Override
    public boolean hasImport(String uri) {
        return findImport(x -> Objects.equals(x.getID().getImportsIRI(), uri)).isPresent();
    }

    @Override
    public OntGraphModelImpl removeImport(OntModel m) {
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
    public Stream<OntModel> imports() {
        return imports(getOntPersonality());
    }

    /**
     * Lists all top-level sub-models built with the given {@code personality}.
     *
     * @param personality {@link OntPersonality}, not {@code null}
     * @return {@code Stream} of {@link OntModel}s
     */
    public Stream<OntModel> imports(OntPersonality personality) {
        return Iter.asStream(listImportModels(personality));
    }

    /**
     * Finds a model impl from the internals using the given {@code filter}.
     *
     * @param filter {@code Predicate} to filter {@link OntGraphModelImpl}s
     * @return {@code Optional} around {@link OntGraphModelImpl}
     */
    protected Optional<OntGraphModelImpl> findImport(Predicate<OntGraphModelImpl> filter) {
        return Iter.findFirst(listImportModels(getOntPersonality()).filterKeep(filter));
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
     * from the top tier of the imports' hierarchy.
     *
     * @param personality {@link OntPersonality}, not {@code null}
     * @return <b>non-distinct</b> {@code ExtendedIterator} of {@link OntGraphModelImpl}s
     */
    public final ExtendedIterator<OntGraphModelImpl> listImportModels(OntPersonality personality) {
        return listImportGraphs().mapWith(u -> new OntGraphModelImpl(u, personality));
    }

    /**
     * Lists all top-level {@link UnionGraph}s of the model's {@code owl:import} hierarchy.
     * This model graph is not included.
     *
     * @return <b>non-distinct</b> {@code ExtendedIterator} of {@link UnionGraph}
     */
    protected final ExtendedIterator<UnionGraph> listImportGraphs() {
        return getGraph().getUnderlying().listGraphs()
                .filterKeep(x -> x instanceof UnionGraph)
                .mapWith(x -> (UnionGraph) x);
    }

    /**
     * Gets the top-level {@link OntGraphModelImpl Ontology Graph Model impl}.
     * The returned model may contain import declarations, but cannot contain sub-models.
     * Be warned: any listeners, attached on the {@link #getGraph()}
     *
     * @return {@link OntGraphModelImpl}
     * @see #getBaseModel()
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
    public <E extends OntObjectImpl & OntEntity> boolean isBuiltIn(E e) {
        return getOntPersonality().getBuiltins()
                .get(e.getActualClass())
                .contains(e.asNode());
    }

    /**
     * Retrieves the stream of {@link OntObject Ontology Object}s.
     * The result object will be cached inside model.
     *
     * @param type {@link Class} the type of {@link OntObject}, not null
     * @param <O>  subtype of {@link OntObject}
     * @return {@code Stream} of {@link OntObject}s
     */
    @Override
    public <O extends OntObject> Stream<O> ontObjects(Class<? extends O> type) {
        return Iter.asStream(listOntObjects(type), getSpliteratorCharacteristics(getGraph()));
    }

    /**
     * Lists all {@link OntObject Ontology Object}s and caches them inside this model.
     *
     * @param type {@link Class} the type of {@link OntObject}, not null
     * @param <O>  subtype of {@link OntObject}
     * @return an {@link ExtendedIterator Extended Iterator} of {@link OntObject}s
     */
    public <O extends OntObject> ExtendedIterator<O> listOntObjects(Class<? extends O> type) {
        return listOntObjects(this, type);
    }

    /**
     * The same as {@link OntGraphModelImpl#listOntObjects(Class)}, but for the base graph.
     *
     * @param type {@link Class} the type of {@link OntObject}, not null
     * @param <O>  subtype of {@link OntObject}
     * @return {@link ExtendedIterator Extended Iterator} of {@link OntObject}s
     */
    public <O extends OntObject> ExtendedIterator<O> listLocalOntObjects(Class<? extends O> type) {
        return listOntObjects(getTopModel(), type);
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
     * @return {@code Stream} of {@link OntEntity}s.
     */
    public Stream<OntEntity> ambiguousEntities(boolean withImports) {
        Set<Class<? extends OntEntity>> types = OntEntity.listEntityTypes().toSet();
        return ontEntities().filter(e -> withImports || e.isLocal()).filter(e -> types.stream()
                .filter(view -> e.canAs(view) && (withImports || e.as(view).isLocal())).count() > 1);
    }

    /**
     * {@inheritDoc}
     * Currently there are {@code 185} such resources for a {@link OntClass.Named}
     * (from OWL, RDFS, RDF, XSD, SWRL, SWRLB vocabularies).
     * It is an auxiliary method for iteration optimization.
     *
     * @param type a {@code Class}-type of {@link OntObject}, not {@code null}
     * @return an unmodifiable {@code Set} of {@link Node}s
     */
    @Override
    public Set<Node> getSystemResources(Class<? extends OntObject> type) {
        return getOntPersonality().getReserved().getResources().stream() // do not use model's cache
                .filter(x -> !OntObjectImpl.wrapAsOntObject(x, OntGraphModelImpl.this).canAs(type))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Stream<OntIndividual> individuals() {
        return Iter.asStream(listIndividuals(), getSpliteratorCharacteristics(getGraph()));
    }

    /**
     * Returns an {@code ExtendedIterator} over all individuals
     * that participate in class assertion statement {@code a rdf:type C}.
     *
     * @return {@link ExtendedIterator} of {@link OntIndividual}s
     */
    public ExtendedIterator<OntIndividual> listIndividuals() {
        return listIndividuals(this,
                getSystemResources(OntClass.Named.class),
                getGraph().find(Node.ANY, RDF.Nodes.type, Node.ANY));
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
            throw new OntJenaException.Creation(String.format("Can't add entity [%s: %s]: perhaps it's illegal punning.",
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
        return remove(statement.clearAnnotations());
    }

    @Override
    public Stream<OntStatement> statements() {
        UnionGraph g = getGraph();
        return asStream(g, g.find().mapWith(this::asStatement), true);
    }

    @Override
    public Stream<OntStatement> statements(Resource s, Property p, RDFNode o) {
        return asStream(getGraph(), listOntStatements(s, p, o), isANY(s, p, o));
    }

    @Override
    public Stream<OntStatement> localStatements(Resource s, Property p, RDFNode o) {
        return asStream(getBaseGraph(), listLocalStatements(s, p, o), isANY(s, p, o));
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
     */
    public ExtendedIterator<OntStatement> listOntStatements(Resource s, Property p, RDFNode o) {
        return Iter.create(getGraph().find(asNode(s), asNode(p), asNode(o)).mapWith(this::asStatement));
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
     */
    public ExtendedIterator<OntStatement> listLocalStatements(Resource s, Property p, RDFNode o) {
        return Iter.create(getBaseGraph().find(asNode(s), asNode(p), asNode(o)).mapWith(this::asStatement));
    }

    @Override
    public OntStatementImpl createStatement(Resource s, Property p, RDFNode o) {
        return OntStatementImpl.createOntStatementImpl(s, p, o, this);
    }

    @Override
    public OntStatementImpl asStatement(Triple triple) {
        return OntStatementImpl.createOntStatementImpl(triple, this);
    }

    /**
     * Determines if the given {@code (s, p, o)} pattern is present in the base graph,
     * with {@code null} allowed to represent a wildcard match.
     *
     * @param s - {@link Resource} - the subject of the statement tested ({@code null} as wildcard)
     * @param p - {@link Property} - the predicate of the statement tested ({@code null} as wildcard)
     * @param o - {@link RDFNode} - the object of the statement tested ({@code null} as wildcard)
     * @return boolean
     * @see Model#contains(Resource, Property, RDFNode)
     */
    public boolean containsLocal(Resource s, Property p, RDFNode o) {
        return getBaseGraph().contains(asNode(s), asNode(p), asNode(o));
    }

    /**
     * Wraps the existing given {@link RDFList []-list} as {@link OntList ONT-list}.
     *
     * @param list      {@link RDFList}, not {@code null}
     * @param subject   {@link OntObject}, not {@code null}
     * @param predicate {@link Property}, not {@code null}
     * @param type      a {@code Class}-type for list element {@link E}, not {@code null}
     * @param <E>       any {@link RDFNode}
     * @return {@code OntList}
     */
    public <E extends RDFNode> OntListImpl<E> asOntList(RDFList list,
                                                        OntObject subject,
                                                        Property predicate,
                                                        Class<E> type) {
        return asOntList(list, subject, predicate, false, null, type);
    }

    /**
     * Wraps the existing given {@link RDFList []-list} as {@link OntList ONT-list}.
     *
     * @param list            {@link RDFList}, not {@code null}
     * @param subject         {@link OntObject}, not {@code null}
     * @param predicate       {@link Property}, not {@code null}
     * @param checkRecursions boolean, if {@code true} more careful and expensive checking for list content is performed
     * @param listType        an uri-{@link Resource}, used as an archaic RDF-type, usually this parameter should be {@code null}
     * @param elementType     a {@code Class}-type for list element {@link E}, not {@code null}
     * @param <E>             any {@link RDFNode}
     * @return {@code OntList}
     */
    public <E extends RDFNode> OntListImpl<E> asOntList(RDFList list,
                                                        OntObject subject,
                                                        Property predicate,
                                                        boolean checkRecursions,
                                                        Resource listType,
                                                        Class<E> elementType) {
        OntListImpl.checkRequiredInputs(subject, predicate, list, listType, elementType);
        return checkRecursions ?
                OntListImpl.asSafeOntList(list, this, subject, predicate, listType, elementType) :
                OntListImpl.asOntList(list, this, subject, predicate, listType, elementType);
    }

    /**
     * Creates ONT-List with given elements and other settings.
     *
     * @param subject   {@link OntObject}, not {@code null}
     * @param predicate {@link Property}, not {@code null}
     * @param type      a {@code Class}-type for element {@link E}, not {@code null}
     * @param elements  and {@code Iterator} of {@link E}-elements (the order is preserved), not {@code null}
     * @param <E>       any {@link RDFNode}
     * @return {@code OntList}
     */
    public <E extends RDFNode> OntListImpl<E> createOntList(OntObject subject,
                                                            Property predicate,
                                                            Class<E> type,
                                                            Iterator<E> elements) {
        return createOntList(subject, predicate, null, type, elements);
    }

    /**
     * Creates ONT-List with given elements and other settings.
     *
     * @param subject     {@link OntObject}, not {@code null}
     * @param predicate   {@link Property}, not {@code null}
     * @param listType    an uri-{@link Resource}, used as an archaic RDF-type, usually this parameter should be {@code null}
     * @param elementType a {@code Class}-type for element {@link E}, not {@code null}
     * @param elements    and {@code Iterator} of {@link E}-elements (the order is preserved), not {@code null}
     * @param <E>         any {@link RDFNode}
     * @return {@code OntList}
     */
    public <E extends RDFNode> OntListImpl<E> createOntList(OntObject subject,
                                                            Property predicate,
                                                            Resource listType,
                                                            Class<E> elementType,
                                                            Iterator<E> elements) {
        OntListImpl.checkRequiredInputs(subject, predicate, listType, elementType);
        return OntListImpl.create(this, subject, predicate, listType, elementType, Iter.create(elements));
    }

    /**
     * Lists all (bulk) annotation anonymous resources for the given {@code rdf:type} and SPO.
     *
     * @param t {@link Resource} either {@link OWL#Axiom owl:Axiom} or {@link OWL#Annotation owl:Annotation}
     * @param s {@link Resource} subject
     * @param p {@link Property} predicate
     * @param o {@link RDFNode} object
     * @return {@link ExtendedIterator} of annotation {@link Resource resource}s
     */
    public ExtendedIterator<Resource> listAnnotations(Resource t, Resource s, Property p, RDFNode o) {
        return getGraph().find(Node.ANY, OWL.annotatedSource.asNode(), s.asNode())
                .mapWith(this::asStatement)
                .filterKeep(x -> (OWL.Axiom == t ? x.belongsToOWLAxiom() : x.belongsToOWLAnnotation())
                        && x.hasAnnotatedProperty(p) && x.hasAnnotatedTarget(o))
                .mapWith(Statement::getSubject);
    }

    /**
     * Deletes the specified {@code OntList} including its annotations.
     *
     * @param subject   {@link OntObject} the subject of the OntList root statement
     * @param predicate {@link Property} the predicate of the OntList root statement
     * @param object    {@link OntList} to be deleted
     * @return this model instance
     */
    @SuppressWarnings("UnusedReturnValue")
    public OntGraphModelImpl deleteOntList(OntObject subject, Property predicate, OntList<?> object) {
        Objects.requireNonNull(subject);
        Objects.requireNonNull(predicate);
        OntJenaException.notNull(object, "Null list for subject " + subject + " and predicate " + predicate);
        boolean hasNil = !object.isNil() && contains(subject, predicate, RDF.nil);
        object.getMainStatement().clearAnnotations();
        object.clear(); // now it is nil-list
        if (!hasNil) {
            return remove(subject, predicate, object);
        }
        return this;
    }

    @Override
    public OntDisjoint.Classes createDisjointClasses(Collection<OntClass> classes) {
        return OntDisjointImpl.createDisjointClasses(this, classes.stream());
    }

    @Override
    public OntDisjoint.Individuals createDifferentIndividuals(Collection<OntIndividual> individuals) {
        return OntDisjointImpl.createDifferentIndividuals(this, individuals.stream());
    }

    @Override
    public OntDisjoint.ObjectProperties createDisjointObjectProperties(Collection<OntObjectProperty> properties) {
        return OntDisjointImpl.createDisjointObjectProperties(this, properties.stream());
    }

    @Override
    public OntDisjoint.DataProperties createDisjointDataProperties(Collection<OntDataProperty> properties) {
        return OntDisjointImpl.createDisjointDataProperties(this, properties.stream());
    }

    @Override
    public <T extends OntFacetRestriction> T createFacetRestriction(Class<T> view, Literal literal) {
        return OntFRImpl.create(this, view, literal);
    }

    @Override
    public OntDataRange.OneOf createDataOneOf(Collection<Literal> values) {
        return OntDRImpl.createOneOf(this, values.stream());
    }

    @Override
    public OntDataRange.Restriction createDataRestriction(OntDataRange.Named datatype, Collection<OntFacetRestriction> values) {
        return OntDRImpl.createRestriction(this, datatype, values.stream());
    }

    @Override
    public OntDataRange.ComplementOf createDataComplementOf(OntDataRange other) {
        return OntDRImpl.createComplementOf(this, other);
    }

    @Override
    public OntDataRange.UnionOf createDataUnionOf(Collection<OntDataRange> values) {
        return OntDRImpl.createUnionOf(this, values.stream());
    }

    @Override
    public OntDataRange.IntersectionOf createDataIntersectionOf(Collection<OntDataRange> values) {
        return OntDRImpl.createIntersectionOf(this, values.stream());
    }

    @Override
    public OntClass.ObjectSomeValuesFrom createObjectSomeValuesFrom(OntObjectProperty property, OntClass ce) {
        return OntCEImpl.createComponentRestrictionCE(this,
                OntClass.ObjectSomeValuesFrom.class, property, ce, OWL.someValuesFrom);
    }

    @Override
    public OntClass.DataSomeValuesFrom createDataSomeValuesFrom(OntDataProperty property, OntDataRange dr) {
        return OntCEImpl.createComponentRestrictionCE(this,
                OntClass.DataSomeValuesFrom.class, property, dr, OWL.someValuesFrom);
    }

    @Override
    public OntClass.ObjectAllValuesFrom createObjectAllValuesFrom(OntObjectProperty property, OntClass ce) {
        return OntCEImpl.createComponentRestrictionCE(this,
                OntClass.ObjectAllValuesFrom.class, property, ce, OWL.allValuesFrom);
    }

    @Override
    public OntClass.DataAllValuesFrom createDataAllValuesFrom(OntDataProperty property, OntDataRange dr) {
        return OntCEImpl.createComponentRestrictionCE(this,
                OntClass.DataAllValuesFrom.class, property, dr, OWL.allValuesFrom);
    }

    @Override
    public OntClass.ObjectHasValue createObjectHasValue(OntObjectProperty property, OntIndividual individual) {
        return OntCEImpl.createComponentRestrictionCE(this,
                OntClass.ObjectHasValue.class, property, individual, OWL.hasValue);
    }

    @Override
    public OntClass.DataHasValue createDataHasValue(OntDataProperty property, Literal literal) {
        return OntCEImpl.createComponentRestrictionCE(this, OntClass.DataHasValue.class, property, literal, OWL.hasValue);
    }

    @Override
    public OntClass.ObjectMinCardinality createObjectMinCardinality(OntObjectProperty property, int cardinality, OntClass ce) {
        return OntCEImpl.createCardinalityRestrictionCE(this,
                OntClass.ObjectMinCardinality.class, property, cardinality, ce);
    }

    @Override
    public OntClass.DataMinCardinality createDataMinCardinality(OntDataProperty property, int cardinality, OntDataRange dr) {
        return OntCEImpl.createCardinalityRestrictionCE(this,
                OntClass.DataMinCardinality.class, property, cardinality, dr);
    }

    @Override
    public OntClass.ObjectMaxCardinality createObjectMaxCardinality(OntObjectProperty property, int cardinality, OntClass ce) {
        return OntCEImpl.createCardinalityRestrictionCE(this,
                OntClass.ObjectMaxCardinality.class, property, cardinality, ce);
    }

    @Override
    public OntClass.DataMaxCardinality createDataMaxCardinality(OntDataProperty property, int cardinality, OntDataRange dr) {
        return OntCEImpl.createCardinalityRestrictionCE(this,
                OntClass.DataMaxCardinality.class, property, cardinality, dr);
    }

    @Override
    public OntClass.ObjectCardinality createObjectCardinality(OntObjectProperty property, int cardinality, OntClass ce) {
        return OntCEImpl.createCardinalityRestrictionCE(this,
                OntClass.ObjectCardinality.class, property, cardinality, ce);
    }

    @Override
    public OntClass.DataCardinality createDataCardinality(OntDataProperty property, int cardinality, OntDataRange dr) {
        return OntCEImpl.createCardinalityRestrictionCE(this, OntClass.DataCardinality.class, property, cardinality, dr);
    }

    @Override
    public OntClass.UnionOf createObjectUnionOf(Collection<OntClass> classes) {
        return OntCEImpl.createComponentsCE(this, OntClass.UnionOf.class, OntClass.class, OWL.unionOf, classes.stream());
    }

    @Override
    public OntClass.IntersectionOf createObjectIntersectionOf(Collection<OntClass> classes) {
        return OntCEImpl.createComponentsCE(this,
                OntClass.IntersectionOf.class, OntClass.class, OWL.intersectionOf, classes.stream());
    }

    @Override
    public OntClass.OneOf createObjectOneOf(Collection<OntIndividual> individuals) {
        return OntCEImpl.createComponentsCE(this,
                OntClass.OneOf.class, OntIndividual.class, OWL.oneOf, individuals.stream());
    }

    @Override
    public OntClass.HasSelf createHasSelf(OntObjectProperty property) {
        return OntCEImpl.createHasSelf(this, property);
    }

    @Override
    public OntClass.NaryDataAllValuesFrom createDataAllValuesFrom(Collection<OntDataProperty> properties, OntDataRange dr) {
        return OntCEImpl.createNaryRestrictionCE(this, OntClass.NaryDataAllValuesFrom.class, dr, properties);
    }

    @Override
    public OntClass.NaryDataSomeValuesFrom createDataSomeValuesFrom(Collection<OntDataProperty> properties, OntDataRange dr) {
        return OntCEImpl.createNaryRestrictionCE(this, OntClass.NaryDataSomeValuesFrom.class, dr, properties);
    }

    @Override
    public OntClass.ComplementOf createObjectComplementOf(OntClass ce) {
        return OntCEImpl.createComplementOf(this, ce);
    }

    @Override
    public OntSWRL.Variable createSWRLVariable(String uri) {
        return OntSWRLImpl.createVariable(this, uri);
    }

    @Override
    public OntSWRL.Atom.WithBuiltin createBuiltInSWRLAtom(Resource predicate, Collection<OntSWRL.DArg> arguments) {
        return OntSWRLImpl.createBuiltInAtom(this, predicate, arguments);
    }

    @Override
    public OntSWRL.Atom.WithClass createClassSWRLAtom(OntClass clazz, OntSWRL.IArg arg) {
        return OntSWRLImpl.createClassAtom(this, clazz, arg);
    }

    @Override
    public OntSWRL.Atom.WithDataRange createDataRangeSWRLAtom(OntDataRange range, OntSWRL.DArg arg) {
        return OntSWRLImpl.createDataRangeAtom(this, range, arg);
    }

    @Override
    public OntSWRL.Atom.WithDataProperty createDataPropertySWRLAtom(OntDataProperty dataProperty,
                                                                    OntSWRL.IArg firstArg,
                                                                    OntSWRL.DArg secondArg) {
        return OntSWRLImpl.createDataPropertyAtom(this, dataProperty, firstArg, secondArg);
    }

    @Override
    public OntSWRL.Atom.WithObjectProperty createObjectPropertySWRLAtom(OntObjectProperty dataProperty,
                                                                        OntSWRL.IArg firstArg,
                                                                        OntSWRL.IArg secondArg) {
        return OntSWRLImpl.createObjectPropertyAtom(this, dataProperty, firstArg, secondArg);
    }

    @Override
    public OntSWRL.Atom.WithDifferentIndividuals createDifferentIndividualsSWRLAtom(OntSWRL.IArg firstArg,
                                                                                    OntSWRL.IArg secondArg) {
        return OntSWRLImpl.createDifferentIndividualsAtom(this, firstArg, secondArg);
    }

    @Override
    public OntSWRL.Atom.WithSameIndividuals createSameIndividualsSWRLAtom(OntSWRL.IArg firstArg,
                                                                          OntSWRL.IArg secondArg) {
        return OntSWRLImpl.createSameIndividualsAtom(this, firstArg, secondArg);
    }

    @Override
    public OntSWRL.Imp createSWRLImp(Collection<OntSWRL.Atom<?>> head,
                                     Collection<OntSWRL.Atom<?>> body) {
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
    public OntGraphModelImpl add(Statement[] statements) {
        super.add(statements);
        return this;
    }

    @Override
    public OntGraphModelImpl remove(Statement[] statements) {
        super.remove(statements);
        return this;
    }

    @Override
    public OntGraphModelImpl add(List<Statement> statements) {
        super.add(statements);
        return this;
    }

    @Override
    public OntGraphModelImpl remove(List<Statement> statements) {
        super.remove(statements);
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
    public OntGraphModelImpl addLiteral(Resource s, Property p, boolean v) {
        super.addLiteral(s, p, v);
        return this;
    }

    @Override
    public OntGraphModelImpl addLiteral(Resource s, Property p, long v) {
        super.addLiteral(s, p, v);
        return this;
    }

    @Override
    public OntGraphModelImpl addLiteral(Resource s, Property p, int v) {
        super.addLiteral(s, p, v);
        return this;
    }

    @Override
    public OntGraphModelImpl addLiteral(Resource s, Property p, char v) {
        super.addLiteral(s, p, v);
        return this;
    }

    @Override
    public OntGraphModelImpl addLiteral(Resource s, Property p, float v) {
        super.addLiteral(s, p, v);
        return this;
    }

    @Override
    public OntGraphModelImpl addLiteral(Resource s, Property p, double v) {
        super.addLiteral(s, p, v);
        return this;
    }

    @Override
    public OntGraphModelImpl addLiteral(Resource s, Property p, Literal o) {
        super.addLiteral(s, p, o);
        return this;
    }

    @Override
    public OntGraphModelImpl add(Resource s, Property p, String lex) {
        super.add(s, p, lex);
        return this;
    }

    @Override
    public OntGraphModelImpl add(Resource s, Property p, String lex, RDFDatatype datatype) {
        super.add(s, p, lex, datatype);
        return this;
    }

    @Override
    public OntGraphModelImpl add(Resource s, Property p, String lex, boolean wellFormed) {
        super.add(s, p, lex, wellFormed);
        return this;
    }

    @Override
    public OntGraphModelImpl add(Resource s, Property p, String lex, String lang) {
        super.add(s, p, lex, lang);
        return this;
    }


    @Override
    public OntGraphModelImpl read(String url) {
        super.read(url);
        return this;
    }

    @Override
    public OntGraphModelImpl read(Reader reader, String base) {
        super.read(reader, base);
        return this;
    }

    @Override
    public OntGraphModelImpl read(InputStream reader, String base) {
        super.read(reader, base);
        return this;
    }

    @Override
    public OntGraphModelImpl read(String url, String lang) {
        super.read(url, lang);
        return this;
    }

    @Override
    public OntGraphModelImpl read(String url, String base, String lang) {
        super.read(url, base, lang);
        return this;
    }

    @Override
    public OntGraphModelImpl read(Reader reader, String base, String lang) {
        super.read(reader, base, lang);
        return this;
    }

    @Override
    public OntGraphModelImpl read(InputStream reader, String base, String lang) {
        super.read(reader, base, lang);
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
    public OntAnnotationProperty getRDFSComment() {
        return findNodeAs(RDFS.Nodes.comment, OntAnnotationProperty.class);
    }

    @Override
    public OntAnnotationProperty getRDFSLabel() {
        return findNodeAs(RDFS.Nodes.label, OntAnnotationProperty.class);
    }

    @Override
    public OntClass.Named getOWLThing() {
        return findNodeAs(OWL.Thing.asNode(), OntClass.Named.class);
    }

    @Override
    public OntDataRange.Named getRDFSLiteral() {
        return findNodeAs(RDFS.Literal.asNode(), OntDataRange.Named.class);
    }

    @Override
    public OntClass.Named getOWLNothing() {
        return findNodeAs(OWL.Nothing.asNode(), OntClass.Named.class);
    }

    @Override
    public OntObjectProperty.Named getOWLTopObjectProperty() {
        return findNodeAs(OWL.topObjectProperty.asNode(), OntObjectProperty.Named.class);
    }

    @Override
    public OntObjectProperty.Named getOWLBottomObjectProperty() {
        return findNodeAs(OWL.bottomObjectProperty.asNode(), OntObjectProperty.Named.class);
    }

    @Override
    public OntDataProperty getOWLTopDataProperty() {
        return findNodeAs(OWL.topDataProperty.asNode(), OntDataProperty.class);
    }

    @Override
    public OntDataProperty getOWLBottomDataProperty() {
        return findNodeAs(OWL.bottomDataProperty.asNode(), OntDataProperty.class);
    }

    @Override
    public String toString() {
        return String.format("OntGraphModel{%s}", Graphs.getName(getBaseGraph()));
    }

}
