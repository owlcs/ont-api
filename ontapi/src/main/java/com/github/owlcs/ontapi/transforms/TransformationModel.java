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

package com.github.owlcs.ontapi.transforms;

import com.github.owlcs.ontapi.OntologyManager;
import com.github.owlcs.ontapi.jena.OntVocabulary;
import com.github.owlcs.ontapi.jena.UnionGraph;
import com.github.owlcs.ontapi.jena.utils.Graphs;
import com.github.owlcs.ontapi.jena.utils.Iterators;
import com.github.owlcs.ontapi.jena.vocabulary.RDF;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.impl.WrappedGraph;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * The base interface for any graph transformer implementations.
 * A Graph Transform is a general mechanism to perform any transformations on a graph
 * before it becomes available through the main system interfaces
 * (i.e. through {@link OntologyManager Ontology Manager}).
 * It is for restoring missed OWL declarations, removing RDFS garbage,
 * creating OWL ontology id and some other actions
 * to be sure that graph contains an OWL ontology,
 * that is ready up to process by other ONT-API subsystems.
 *
 * @see Transform
 */
@SuppressWarnings("WeakerAccess")
public abstract class TransformationModel {
    protected static final Logger LOGGER = LoggerFactory.getLogger(TransformationModel.class);

    protected final Graph graph;
    protected final OntVocabulary builtins;

    protected final Model queryModel;
    protected final Model workModel;

    /**
     * Creates an instance for the given {@code Graph}.
     *
     * @param graph {@link Graph}, not {@code null}
     */
    public TransformationModel(Graph graph) {
        this(graph, OntVocabulary.Factory.get());
    }

    /**
     * Creates an instance for the given {@code Graph} and {@code Vocabulary} with builtins.
     *
     * @param graph      {@link Graph}, not {@code null}
     * @param vocabulary {@link OntVocabulary}, not {@code null}
     * @throws NullPointerException some arguments are {@code null}
     */
    public TransformationModel(Graph graph, OntVocabulary vocabulary) throws NullPointerException {
        this.builtins = Objects.requireNonNull(vocabulary, "Null builtins vocabulary.");
        this.graph = Objects.requireNonNull(graph, "Null graph.");
        if (graph instanceof UnionGraph) {
            UnionGraph u = (UnionGraph) graph;
            UnionGraph g = Graphs.withBase(new TrackedGraph(u.getBaseGraph()), u);
            queryModel = createModel(u.getBaseGraph());
            workModel = createModel(g);
        } else {
            queryModel = createModel(graph);
            workModel = createModel(new TrackedGraph(graph));
        }
    }

    /**
     * Performs the graph transformation if the encapsulated graph is applicable for it.
     *
     * @return Stream of {@link Triple}s that the transformer could not handle
     * @throws TransformException in case something is wrong
     */
    public Stream<Triple> process() throws TransformException {
        if (test()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Process <%s> on <%s>", name(), Graphs.getName(getBaseGraph())));
            }
            perform();
        }
        return uncertainTriples();
    }

    /**
     * Performs the graph transformation.
     *
     * @throws TransformException if something wrong during operation.
     */
    public abstract void perform() throws TransformException;

    /**
     * Decides whether the transformation needed or not.
     *
     * @return {@code true} to process, {@code false} to skip
     */
    public boolean test() {
        return true;
    }

    /**
     * Returns a problematic triples found while processing as a {@code Stream}.
     * I.e. those triples, which this parser was not able to handle correctly.
     * An empty Stream is expected before first calling of {@link #perform() process method}.
     *
     * @return Stream of {@link Triple}s
     */
    public Stream<Triple> uncertainTriples() {
        return Stream.empty();
    }

    /**
     * Returns the displaying name of this {@code Transform}.
     *
     * @return String
     */
    public String name() {
        return getClass().getSimpleName();
    }

    /**
     * Returns the {@code Graph} for which this class is intended.
     *
     * @return {@link Graph}, not {@code null}
     */
    public Graph getGraph() {
        return graph;
    }

    /**
     * Returns the base {@code Graph} from whole {@link #getGraph() graph}
     * if it is composite (instance of {@link UnionGraph}).
     * Otherwise, it results the same graph.
     *
     * @return {@link Graph}, not {@code null}
     */
    protected Graph getBaseGraph() {
        return graph instanceof UnionGraph ? ((UnionGraph) graph).getBaseGraph() : graph;
    }

    /**
     * Returns a {@link Model} to perform querying.
     *
     * @return {@link Model}
     */
    protected Model getQueryModel() {
        return queryModel;
    }

    /**
     * Returns a {@link Model} to perform {@code add} and {@code remove} operations.
     *
     * @return {@link Model}
     */
    protected Model getWorkModel() {
        return workModel;
    }

    /**
     * Answers a model that encapsulates the given graph.
     * Existing prefixes are undisturbed.
     * A factory method to allow implementation replacement.
     *
     * @param graph {@link Graph}
     * @return {@link Model}
     */
    protected Model createModel(Graph graph) {
        return ModelFactory.createModelForGraph(Objects.requireNonNull(graph));
    }

    /**
     * Replaces the found {@code rdf:type} with new one.
     *
     * @param foundType {@link Resource}, not {@code null}
     * @param newType   {@link Resource}, not {@code null}
     */
    protected void changeType(Resource foundType, Resource newType) {
        listStatements(null, RDF.type, foundType)
                .toList()
                .forEach(s -> undeclare(s.getSubject(), foundType).declare(s.getSubject(), newType));
    }

    /**
     * Adds a declaration triple into the base model.
     *
     * @param subject {@link Resource}, not {@code null}
     * @param type    {@link Resource}, not {@code null}
     * @return this {@code Transform} instance
     */
    protected TransformationModel declare(Resource subject, Resource type) {
        subject.addProperty(RDF.type,
                Objects.requireNonNull(type, "Declare: null type for resource '" + subject + "'"));
        return this;
    }

    /**
     * Removes a declaration triple from the model.
     *
     * @param subject {@link Resource}, not {@code null}
     * @param type    {@link Resource}, not {@code null}
     * @return this {@code Transform} instance
     */
    protected TransformationModel undeclare(Resource subject, Resource type) {
        getWorkModel().removeAll(subject, RDF.type,
                Objects.requireNonNull(type, "Undeclare: null type for resource '" + subject + "'"));
        return this;
    }

    /**
     * Answers {@code true} if the encapsulated base model contains the specified {@code rdf:type}.
     *
     * @param type {@link Resource}, not {@code null}
     * @return boolean
     */
    boolean containsType(Resource type) {
        return getQueryModel().contains(null, RDF.type, type);
    }

    /**
     * Answers {@code true} if the given resource has the given {@code rdf:type}.
     *
     * @param resource {@link Resource}
     * @param type     {@link Resource}
     * @return boolean
     */
    boolean hasType(Resource resource, Resource type) {
        return resource.hasProperty(RDF.type, type);
    }

    /**
     * Answers {@code true} if the given resource has any of the specified predicates.
     *
     * @param resource   {@link Resource}
     * @param predicates collection of {@link Property}s
     * @return boolean
     */
    boolean hasAnyPredicate(Resource resource, Collection<Property> predicates) {
        return predicates.stream().anyMatch(resource::hasProperty);
    }

    /**
     * Answers {@code true} if the given resource has any of the specified {@code rdf:type}s.
     *
     * @param resource {@link Resource}
     * @param types    collection of {@link Resource}s
     * @return boolean
     */
    boolean hasAnyType(Resource resource, Collection<Resource> types) {
        return types.stream().anyMatch(t -> hasType(resource, t));
    }

    /**
     * Lists all statements from the base model according to the given SPO pattern.
     *
     * @param s {@link Resource} or {@code null}, a subject in SPO
     * @param p {@link Property} or {@code null}, a predicate in SPO
     * @param o {@link RDFNode} or {@code null}, an object in SPO
     * @return {@code Stream} of {@link Statement}s
     * @see #listStatements(Resource, Property, RDFNode)
     */
    protected final Stream<Statement> statements(Resource s, Property p, RDFNode o) {
        return Iterators.asStream(listStatements(s, p, o));
    }

    /**
     * Returns an extended iterator over all the statements in the base ({@link #getQueryModel() query}) model
     * that match a given SPO pattern.
     * Each of the {@code Statement}s is attached to the whole ({@link #getWorkModel() working}) model,
     * not to the query model.
     * If any SPO argument is {@code null} it matches anything.
     *
     * @param s {@link Resource}, a subject in SPO or {@code null} for any
     * @param p {@link Property}, a predicate in SPO or {@code null} for any
     * @param o {@link RDFNode}, an object in SPO or {@code null} for any
     * @return {@link ExtendedIterator} of {@link Statement}s
     * @see #getWorkModel()
     * @see #getQueryModel()
     */
    protected ExtendedIterator<Statement> listStatements(Resource s, Property p, RDFNode o) {
        return queryModel.listStatements(s, p, o).mapWith(x -> workModel.asStatement(x.asTriple()));
    }

    @Override
    public String toString() {
        return String.format("[%s:%s]", name(), Graphs.getName(getBaseGraph()));
    }

    /**
     * A Graph-wrapper to use as base in a {@link #getWorkModel() working model}.
     * <p>
     * Created by @ssz on 01.03.2019.
     */
    public static class TrackedGraph extends WrappedGraph {
        public TrackedGraph(Graph base) {
            super(Objects.requireNonNull(base));
        }

        @Override
        public void add(Triple t) {
            if (base.contains(t)) return;
            super.add(t);
        }

        @Override
        public void delete(Triple t) {
            if (!base.contains(t)) return;
            super.delete(t);
        }
    }
}
