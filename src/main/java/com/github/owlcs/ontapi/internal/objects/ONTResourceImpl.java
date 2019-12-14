/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2019, The University of Manchester, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.internal.objects;

import com.github.owlcs.ontapi.AsNode;
import com.github.owlcs.ontapi.OntApiException;
import com.github.owlcs.ontapi.internal.AsRDFNode;
import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.jena.model.OntObject;
import org.apache.jena.graph.BlankNodeId;
import org.apache.jena.graph.FrontsTriple;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.impl.LiteralLabel;
import org.semanticweb.owlapi.model.OWLObject;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A base RDF-object component, which encapsulates both node and model references.
 * Unlike the {@link org.apache.jena.rdf.model.Resource Jena Resource} it can wrap a literal value,
 * since an {@link OntObject}s is allowed to be literal (in the single case of SWRL-Data-argument).
 * Created by @ssz on 07.08.2019.
 *
 * @see ONTStatementImpl
 * @see OntObject
 * @since 2.0.0
 */
@SuppressWarnings("WeakerAccess")
public abstract class ONTResourceImpl extends ONTObjectImpl implements OWLObject, AsNode, AsRDFNode {
    // URI (String), BlankNodeId, or LiteralLabel
    protected final Object node;

    /**
     * Constructs the base object.
     *
     * @param n - either {@code String} (URI) or {@link BlankNodeId} or {@link LiteralLabel}, not {@code null}
     * @param m - a facility (as {@link Supplier}) to provide nonnull {@link OntModel}, not {@code null}
     */
    protected ONTResourceImpl(Object n, Supplier<OntModel> m) {
        super(m);
        this.node = Objects.requireNonNull(n);
    }

    protected BlankNodeId getBlankNodeId() {
        throw new OntApiException.IllegalState();
    }

    protected String getURI() {
        throw new OntApiException.IllegalState();
    }

    protected LiteralLabel getLiteralLabel() {
        throw new OntApiException.IllegalState();
    }

    @Override
    public abstract Node asNode();

    @Override
    public abstract OntObject asRDFNode();

    protected <X extends OntObject> X as(Class<X> type) {
        return getPersonalityModel().getNodeAs(asNode(), type);
    }

    public Stream<Triple> triples() {
        return asRDFNode().spec().map(FrontsTriple::asTriple);
    }

    protected boolean sameAs(ONTResourceImpl other) {
        if (notSame(other)) {
            return false;
        }
        return node.equals(other.node);
    }
}
