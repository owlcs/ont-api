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

package ru.avicomp.ontapi.jena.impl.conf;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.enhanced.EnhNode;
import org.apache.jena.enhanced.Implementation;
import org.apache.jena.graph.Node;
import ru.avicomp.ontapi.jena.OntJenaException;

/**
 * An extended {@link Implementation} factory,
 * the base class for any {@link ObjectFactory factories} to produce
 * {@link ru.avicomp.ontapi.jena.model.OntObject Ontology Object}s.
 * Used to bind implementation (node) and interface.
 * Also, in addition to the standard jena methods,
 * this implementation includes nodes search and graph transformation functionality.
 * <p>
 * Created by @szuev on 03.11.2016.
 */
public abstract class BaseFactoryImpl extends Implementation implements ObjectFactory {

    protected static EnhNode safeWrap(Node n, EnhGraph g, Iterable<ObjectFactory> factories) {
        for (ObjectFactory f : factories) {
            EnhNode r = safeWrap(n, g, f);
            if (r != null) return r;
        }
        return null;
    }

    protected static EnhNode safeWrap(Node n, EnhGraph g, ObjectFactory... factories) {
        for (ObjectFactory f : factories) {
            EnhNode r = safeWrap(n, g, f);
            if (r != null) return r;
        }
        return null;
    }

    protected static EnhNode safeWrap(Node n, EnhGraph g, ObjectFactory f) {
        try {
            return f.wrap(n, g);
        } catch (OntJenaException.Conversion c) {
            return null;
        }
    }

    protected static boolean canWrap(Node node, EnhGraph eg, ObjectFactory... factories) {
        for (ObjectFactory f : factories) {
            if (f.canWrap(node, eg)) return true;
        }
        return false;
    }

    protected static boolean canWrap(Node node, EnhGraph eg, Iterable<ObjectFactory> factories) {
        for (ObjectFactory f : factories) {
            if (f.canWrap(node, eg)) return true;
        }
        return false;
    }

    protected static EnhNode wrap(Node node, EnhGraph eg, OntJenaException.Conversion ex, ObjectFactory... factories) {
        for (ObjectFactory f : factories) {
            try {
                return f.wrap(node, eg);
            } catch (OntJenaException.Conversion c) {
                ex.addSuppressed(c);
            }
        }
        throw ex;
    }

    protected static EnhNode wrap(Node node, EnhGraph eg, OntJenaException.Conversion ex, Iterable<ObjectFactory> factories) {
        for (ObjectFactory f : factories) {
            try {
                return f.wrap(node, eg);
            } catch (OntJenaException.Conversion c) {
                ex.addSuppressed(c);
            }
        }
        throw ex;
    }

    /**
     * Creates a new {@link EnhNode} wrapping the given {@link Node} node in the context of the graph {@link EnhGraph}.
     *
     * @param node the node to be wrapped
     * @param eg   the graph containing the node
     * @return A new enhanced node which wraps node but presents the interface(s) that this factory encapsulates.
     * @throws OntJenaException.Conversion in case wrapping is impossible
     */
    @Override
    public EnhNode wrap(Node node, EnhGraph eg) {
        if (!canWrap(node, eg))
            throw new OntJenaException.Conversion("Can't wrap node " + node + ". Use direct factory.");
        return createInstance(node, eg);
    }
}
