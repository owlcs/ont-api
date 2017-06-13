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

package ru.avicomp.ontapi.jena;

import java.util.Iterator;
import java.util.stream.Stream;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphListener;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.compose.MultiUnion;
import org.apache.jena.graph.compose.Union;
import org.apache.jena.graph.impl.SimpleEventManager;
import org.apache.jena.shared.PrefixMapping;

/**
 * Union Graph.
 * The difference between {@link MultiUnion} and this graph is it doesn't allow to modify any graphs with except of base.
 * Underling sub graphs are used only for searching, the adding and removing are performed only over the base (left) graph.
 * <p>
 * Created by szuev on 28.10.2016.
 */
public class UnionGraph extends Union {

    /**
     * @param base Graph
     */
    public UnionGraph(Graph base) {
        this(base, new OntEventManager());
    }

    /**
     * @param base Graph
     * @param gem  {@link OntEventManager}
     */
    public UnionGraph(Graph base, OntEventManager gem) {
        super(base, new OntMultiUnion());
        this.gem = OntJenaException.notNull(gem, "Null event manager.");
    }

    public Graph getBaseGraph() {
        return L;
    }

    public OntMultiUnion getUnderlying() {
        return (OntMultiUnion) R;
    }

    @Override
    public void performDelete(Triple t) {
        L.delete(t);
    }

    @Override
    public void performAdd(Triple t) {
        if (!R.contains(t))
            L.add(t);
    }

    @Override
    public OntEventManager getEventManager() {
        return (OntEventManager) gem;
    }

    @Override
    public PrefixMapping getPrefixMapping() {
        return L.getPrefixMapping();
    }

    public void addGraph(Graph graph) {
        getUnderlying().addGraph(graph);
    }

    public void removeGraph(Graph graph) {
        getUnderlying().removeGraph(graph);
    }

    public static class OntMultiUnion extends MultiUnion {

        public OntMultiUnion() {
            super();
        }

        public OntMultiUnion(Graph[] graphs) {
            super(graphs);
        }

        public OntMultiUnion(Iterator<Graph> graphs) {
            super(graphs);
        }

        public Stream<Graph> graphs() {
            return m_subGraphs.stream();
        }

        public boolean hasSubGraphs() {
            return !m_subGraphs.isEmpty();
        }
    }

    public static class OntEventManager extends SimpleEventManager {

        public Stream<GraphListener> listeners() {
            return listeners.stream();
        }

        public boolean hasListeners(Class<? extends GraphListener> view) {
            return listeners().anyMatch(l -> view.isAssignableFrom(l.getClass()));
        }
    }
}
