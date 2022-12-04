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

package com.github.owlcs.ontapi.owlapi.objects.entity;

import org.apache.jena.rdf.model.Resource;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.semanticweb.owlapi.vocab.OWL2Datatype.*;

/**
 * An optimised implementation of {@link OWLDatatype},
 * that is actually a wrapper for {@link OWL2Datatype}.
 * See {@code uk.ac.manchester.cs.owl.owlapi.OWL2DatatypeImpl}.
 * <p>
 * Created by @szz on 14.08.2019.
 *
 * @author Matthew Horridge, Stanford University, Bio-Medical Informatics Research Group
 * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWL2DatatypeImpl.java'>uk.ac.manchester.cs.owl.owlapi.OWL2DatatypeImpl</a>
 * @since 2.0.0
 */
@SuppressWarnings("WeakerAccess")
public class BuiltinDatatypeImpl extends DatatypeImpl {

    /**
     * A {@code Map} of all known builtin datatypes.
     */
    public static final Map<String, OWLDatatype> BUILTIN_OWL_DATATYPES = createBuiltinsMap(BuiltinDatatypeImpl::new);

    private final OWL2Datatype from;

    /**
     * @param dt {@link OWL2Datatype} - a datatype enum constant
     */
    public BuiltinDatatypeImpl(OWL2Datatype dt) {
        super(dt.getIRI());
        this.from = dt;
    }

    /**
     * Creates a map of builtins known by OWL-API.
     *
     * @param map a facility {@code Function}
     *            to derive an {@link OWLDatatype} instance from {@link OWL2Datatype} constant
     * @return an unmodifiable {@code Map} with URIs ({@code String}) as keys and {@link OWLDatatype} as values
     */
    public static Map<String, OWLDatatype> createBuiltinsMap(Function<OWL2Datatype, OWLDatatype> map) {
        return Collections.unmodifiableMap(Arrays.stream(OWL2Datatype.values())
                .collect(Collectors.toMap(x -> x.getIRI().getIRIString(), map)));
    }

    /**
     * Finds and returns an {@link OWLDatatype} instance using the {@link Resource} reference.
     *
     * @param r {@link Resource}, not {@code null}
     * @return {@link OWLDatatype}
     * @throws NullPointerException if incorrect input or nothing found
     */
    public static OWLDatatype fromResource(Resource r) {
        return Objects.requireNonNull(BUILTIN_OWL_DATATYPES.get(Objects.requireNonNull(r.getURI(), "Not URI: " + r)),
                "Can't find builtin datatype for " + r);
    }

    @Override
    public OWL2Datatype getBuiltInDatatype() {
        return from;
    }

    @Override
    public boolean isString() {
        return from == XSD_STRING;
    }

    @Override
    public boolean isInteger() {
        return from == XSD_INTEGER;
    }

    @Override
    public boolean isFloat() {
        return from == XSD_FLOAT;
    }

    @Override
    public boolean isDouble() {
        return from == XSD_DOUBLE;
    }

    @Override
    public boolean isBoolean() {
        return from == XSD_BOOLEAN;
    }

    @Override
    public boolean isRDFPlainLiteral() {
        return from == RDF_PLAIN_LITERAL;
    }

    @Override
    public boolean isTopDatatype() {
        return from == RDFS_LITERAL;
    }

    @Override
    public boolean isTopEntity() {
        return from == RDFS_LITERAL;
    }

    @Override
    public boolean isBuiltIn() {
        return true;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof OWLDatatype)) {
            return false;
        }
        OWLDatatype other = (OWLDatatype) obj;
        return from.getIRI().equals(other.getIRI());
    }

    @Override
    public int compareTo(@Nullable OWLObject o) {
        if (o == null) {
            throw new NullPointerException("Object cannot be null in a #compareTo() call.");
        }
        int res = Integer.compare(typeIndex(), o.typeIndex());
        if (res != 0) {
            return res;
        }
        if (o instanceof OWLDatatype) {
            res = getIRI().compareTo(((OWLDatatype) o).getIRI());
        }
        return res;
    }

    @Override
    public String toString() { // to match OWL-API behavior
        return toStringID();
    }

}
