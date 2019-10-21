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

package com.github.owlcs.ontapi.jena.impl.conf;

import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Resource;
import com.github.owlcs.ontapi.jena.OntJenaException;
import com.github.owlcs.ontapi.jena.model.*;

import java.util.Set;

/**
 * This is a resource type mapper.
 * It is a technical interface that is included into the {@link OntPersonality}.
 * Note: all its methods must return a IRIs (as {@code String}s), not literals or blank-nodes.
 * <p>
 * Created by @ssz on 16.01.2019.
 *
 * @param <T> any subtype of {@link Resource}
 * @see com.github.owlcs.ontapi.jena.utils.BuiltIn.Vocabulary
 * @since 1.4.0
 */
@FunctionalInterface
public interface Vocabulary<T extends Resource> {

    /**
     * Returns a {@code Set} of {@link Node Jena Graph Node}s for the given {@code Class}-type.
     *
     * @param type {@link Class}, any subtype of {@link T}
     * @return Set of {@link Node node}s
     * @throws OntJenaException in case the mapping is not possible
     */
    Set<Node> get(Class<? extends T> type) throws OntJenaException;

    /**
     * A technical interface to describe vocabulary for {@link OntEntity OWL Entity} types.
     * <p>
     * Created by @ssz on 18.01.2019.
     *
     * @see OntEntity#types()
     */
    interface Entities extends Vocabulary<OntObject> {

        default Set<Node> getClasses() {
            return get(OntClass.class);
        }

        default Set<Node> getDatatypes() {
            return get(OntDT.class);
        }

        default Set<Node> getObjectProperties() {
            return get(OntNOP.class);
        }

        default Set<Node> getDatatypeProperties() {
            return get(OntNDP.class);
        }

        default Set<Node> getAnnotationProperties() {
            return get(OntNAP.class);
        }

        default Set<Node> getIndividuals() {
            return get(OntIndividual.Named.class);
        }
    }
}
