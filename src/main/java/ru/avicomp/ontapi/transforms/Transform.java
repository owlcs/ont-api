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

package ru.avicomp.ontapi.transforms;

import org.apache.jena.graph.Graph;
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
 * The base class for any graph-converter.
 * todo: add configurable logger to record all changes.
 */
@SuppressWarnings("WeakerAccess")
public abstract class Transform {
    // todo: logger will be removed later (when listener-mechanism would be implemented)
    protected static final Logger LOGGER = LoggerFactory.getLogger(Transform.class);

    protected final Graph graph;
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
     * Performs the graph transformation.
     *
     * @throws TransformException if something wrong during operation.
     */
    public abstract void perform() throws TransformException;

    /**
     * decides is the transformation needed or not.
     *
     * @return true to process, false to skip
     */
    public boolean test() {
        return true;
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

    public Graph getBaseGraph() {
        return graph instanceof UnionGraph ? ((UnionGraph) graph).getBaseGraph() : graph;
    }

    public Model getModel() {
        return model == null ? model = ModelFactory.createModelForGraph(getGraph()) : model;
    }

    public Model getBaseModel() {
        return base == null ? base = ModelFactory.createModelForGraph(getBaseGraph()) : base;
    }

    public void process() throws TransformException {
        if (test()) {
            if (LOGGER.isDebugEnabled())
                LOGGER.debug(String.format("Process <%s> on <%s>", name(), Graphs.getName(getBaseGraph())));
            perform();
        }
    }

    protected void changeType(Resource realType, Resource newType) {
        Set<Resource> toFix = statements(null, RDF.type, realType).map(Statement::getSubject).collect(Collectors.toSet());
        toFix.forEach(subject -> {
            undeclare(subject, realType);
            declare(subject, newType);
        });
    }

    protected void declare(Resource subject, Resource type) {
        subject.addProperty(RDF.type, Objects.requireNonNull(type, "Declare: null type for resource '" + subject + "'"));
    }

    protected void undeclare(Resource subject, Resource type) {
        getBaseModel().removeAll(subject, RDF.type, Objects.requireNonNull(type, "Undeclare: null type for resource '" + subject + "'"));
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
