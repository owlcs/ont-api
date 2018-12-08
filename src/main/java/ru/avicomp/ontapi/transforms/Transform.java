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

package ru.avicomp.ontapi.transforms;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avicomp.ontapi.jena.UnionGraph;
import ru.avicomp.ontapi.jena.utils.BuiltIn;
import ru.avicomp.ontapi.jena.utils.Graphs;
import ru.avicomp.ontapi.jena.utils.Iter;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The base class for any graph-converter (transform).
 * It is a general mechanism to perform any transformations on a graph before it gets into the main system
 * (i.e. into the {@code ru.avicomp.ontapi.OntologyManager}).
 * Usually this mechanism is for restoring missed OWL declarations,
 * creating OWL ontology id and some other actions to be sure that graph contains an OWL ontology,
 * which is required by ONT-API.
 */
@SuppressWarnings("WeakerAccess")
public abstract class Transform {
    protected static final Logger LOGGER = LoggerFactory.getLogger(Transform.class);

    protected final Graph graph;
    // todo: move vocabulary to the up level where it is used
    protected final BuiltIn.Vocabulary builtIn;
    private Model model;
    private Model base;

    protected Transform(Graph graph, BuiltIn.Vocabulary vocabulary) throws NullPointerException {
        this.graph = Objects.requireNonNull(graph, "Null graph.");
        this.builtIn = Objects.requireNonNull(vocabulary, "Null built-in vocabulary.");
    }

    public Transform(Graph graph) {
        this(graph, BuiltIn.get());
    }

    /**
     * Performs the graph transformation if the encapsulated graph is applicable for it.
     *
     * @return Stream of {@link Triple}s that the transformer could not handle
     * @throws TransformException in case something is wrong
     */
    public Stream<Triple> process() throws TransformException {
        if (test()) {
            if (LOGGER.isDebugEnabled())
                LOGGER.debug(String.format("Process <%s> on <%s>", name(), Graphs.getName(getBaseGraph())));
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
     * @return true to process, false to skip
     */
    public boolean test() {
        return true;
    }

    /**
     * Returns a problematic triple set as a Stream.
     * I.e. those triples, which this parser was not able to handle correctly.
     * Before process ({@link #perform()} this method should return empty Stream.
     *
     * @return Stream of {@link Triple}
     */
    public Stream<Triple> uncertainTriples() {
        return Stream.empty();
    }

    protected static Stream<Statement> statements(Model m, Resource s, Property p, RDFNode o) {
        return Iter.asStream(m.listStatements(s, p, o));
    }

    public String name() {
        return getClass().getSimpleName();
    }

    public Graph getGraph() {
        return graph;
    }

    protected Graph getBaseGraph() {
        return graph instanceof UnionGraph ? ((UnionGraph) graph).getBaseGraph() : graph;
    }

    protected Model getModel() {
        return model == null ? model = ModelFactory.createModelForGraph(getGraph()) : model;
    }

    protected Model getBaseModel() {
        return base == null ? base = ModelFactory.createModelForGraph(getBaseGraph()) : base;
    }

    protected void changeType(Resource realType, Resource newType) {
        Set<Resource> toFix = statements(null, RDF.type, realType)
                .map(Statement::getSubject).collect(Collectors.toSet());
        toFix.forEach(subject -> {
            undeclare(subject, realType);
            declare(subject, newType);
        });
    }

    protected void declare(Resource subject, Resource type) {
        if (subject.hasProperty(RDF.type,
                Objects.requireNonNull(type, "Declare: null type for resource '" + subject + "'")))
            return;
        subject.addProperty(RDF.type, type);
    }

    protected void undeclare(Resource subject, Resource type) {
        getBaseModel().removeAll(subject, RDF.type,
                Objects.requireNonNull(type, "Undeclare: null type for resource '" + subject + "'"));
    }

    protected boolean containsType(Resource type) {
        return getBaseModel().contains(null, RDF.type, type);
    }

    protected boolean hasType(Resource resource, Resource type) {
        return resource.hasProperty(RDF.type, type);
    }

    protected Stream<Statement> statements(Resource s, Property p, RDFNode o) {
        return statements(getBaseModel(), s, p, o).map(st -> getModel().asStatement(st.asTriple()));
    }

    @Override
    public String toString() {
        return String.format("[%s:%s]", name(), Graphs.getName(getBaseGraph()));
    }
}
