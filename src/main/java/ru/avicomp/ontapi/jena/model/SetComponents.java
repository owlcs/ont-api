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

package ru.avicomp.ontapi.jena.model;

import org.apache.jena.rdf.model.RDFNode;
import ru.avicomp.ontapi.jena.OntJenaException;

import java.util.Arrays;
import java.util.Collection;

/**
 * A technical interface to provide working with {@link OntList Ontology []-list}.
 * <p>
 * Created by @ssz on 08.05.2019.
 *
 * @param <V> - {@link RDFNode}, a list's item type
 * @param <R> - {@link OntObject}, a return type
 * @see WithOntList
 * @see HasRDFNodeList
 * @since 1.4.0
 */
interface SetComponents<V extends RDFNode, R extends OntObject> extends WithOntList<V> {

    /**
     * Replaces the existing []-list content with the specified one, that is given in the form of vararg array.
     *
     * @param values an {@code Array} of the type {@link V}
     * @return <b>this</b> instance to allow cascading calls
     * @since 1.4.0
     */
    @SuppressWarnings("unchecked")
    default R setComponents(V... values) {
        return setComponents(Arrays.asList(values));
    }

    /**
     * Replaces the existing []-list content with the specified one, that is given in the form of {@link Collection}.
     * Nulls and self references are not allowed.
     *
     * @param components a {@code Collection} of the type {@link V}
     * @return <b>this</b> instance to allow cascading calls
     * @throws OntJenaException in case of wrong input
     */
    @SuppressWarnings("unchecked")
    default R setComponents(Collection<V> components) {
        if (components.stream().peek(OntJenaException::notNull).anyMatch(SetComponents.this::equals)) {
            throw new OntJenaException.IllegalArgument();
        }
        getList().clear().addAll(components);
        return (R) this;
    }
}
