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

package com.github.owlcs.ontapi.jena.impl.conf;

import com.github.owlcs.ontapi.jena.OntJenaException;
import com.github.owlcs.ontapi.jena.impl.objects.OntObjectImpl;
import com.github.owlcs.ontapi.jena.vocabulary.RDF;
import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.enhanced.EnhNode;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.shared.JenaException;

import java.lang.reflect.InvocationTargetException;

/**
 * To make some preparation while creating (create main triple).
 * Also, to create new instance of the resource ({@link EnhNode}).
 * Used in factory ({@link CommonFactoryImpl}).
 * <p>
 * Created @ssz on 07.11.2016.
 */
public interface OntMaker {

    /**
     * Wraps the given {@code node} as a {@link EnhNode Jena RDFNode}.
     * No changes in the given {@link EnhGraph} are made.
     *
     * @param node {@link Node}
     * @param eg   {@link EnhGraph}
     * @return {@link EnhNode}
     */
    EnhNode instance(Node node, EnhGraph eg);

    /**
     * Changes the {@link EnhGraph} according to the encapsulated rules.
     *
     * @param node {@link Node}
     * @param eg   {@link EnhGraph}
     */
    default void make(Node node, EnhGraph eg) {
        throw new OntJenaException.Unsupported();
    }

    /**
     * Returns a {@link OntFilter}, that is used as tester to decide does this maker support graph modification or not.
     *
     * @return {@link OntFilter}
     */
    default OntFilter getTester() {
        return OntFilter.FALSE;
    }

    /**
     * Returns an interface view implementation.
     *
     * @return a class-type of a concrete {@link com.github.owlcs.ontapi.jena.model.OntObject OWL Object}.
     */
    Class<? extends EnhNode> getImpl();

    default OntMaker restrict(OntFilter filter) {
        OntJenaException.notNull(filter, "Null restriction filter.");
        return new OntMaker() {
            @Override
            public void make(Node node, EnhGraph eg) {
                OntMaker.this.make(node, eg);
            }

            @Override
            public OntFilter getTester() {
                return OntMaker.this.getTester().and(filter);
            }

            @Override
            public EnhNode instance(Node node, EnhGraph eg) {
                return OntMaker.this.instance(node, eg);
            }

            @Override
            public Class<? extends EnhNode> getImpl() {
                return OntMaker.this.getImpl();
            }
        };
    }

    /**
     * The base maker implementation for our project.
     * <p>
     * Creation in graph is disabled for this maker
     */
    class Default implements OntMaker {
        protected final Class<? extends OntObjectImpl> impl;

        /**
         * Class must be public and have a public constructor with parameters {@link Node} and {@link EnhGraph}.
         *
         * @param impl {@link com.github.owlcs.ontapi.jena.model.OntObject} implementation.
         */
        public Default(Class<? extends OntObjectImpl> impl) {
            this.impl = OntJenaException.notNull(impl, "Null implementation class.");
        }

        @Override
        public void make(Node node, EnhGraph eg) {
            throw new OntJenaException.Unsupported("Creation is not allowed for node " +
                    node + " and class " + impl.getSimpleName());
        }

        @Override
        public EnhNode instance(Node node, EnhGraph eg) {
            try {
                return impl.getDeclaredConstructor(Node.class, EnhGraph.class).newInstance(node, eg);
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException e) {
                throw new OntJenaException("Can't create instance of " + impl, e);
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof JenaException) throw (JenaException) e.getCause();
                throw new OntJenaException("Can't init " + impl, e);
            }
        }

        @Override
        public Class<? extends OntObjectImpl> getImpl() {
            return impl;
        }
    }

    /**
     * to create a triple representing declaration.
     */
    class WithType extends Default {
        protected final Node type;

        public WithType(Class<? extends OntObjectImpl> impl, Resource type) {
            super(impl);
            this.type = OntJenaException.notNull(type, "Null type.").asNode();
        }

        @Override
        public void make(Node node, EnhGraph eg) {
            eg.asGraph().add(Triple.create(node, RDF.type.asNode(), type));
        }

        @Override
        public OntFilter getTester() {
            return OntFilter.TRUE;
        }
    }
}
