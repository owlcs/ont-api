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

package ru.avicomp.ontapi.jena.impl.configuration;

import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.model.OntObject;

/**
 * This is our analogue of {@link java.util.function.Function} which is used as an objects container with possibility to choose one of them depending on the input parameter.
 * For simplification code and to be able to change easily the default behaviour during the initialization of personalities.
 * <p>
 * Currently the are three modes: {@link Mode#LAX}, {@link Mode#STRICT} and {@link Mode#MEDIUM}
 * The first one is a lax way. Any owl-entity could have more than one types simultaneously, there is no any restrictions.
 * The second one is to exclude so called 'illegal punnings' (property and class/datatype intersections) from consideration,
 * i.e. the interpretation of such things as owl-entity ({@link ru.avicomp.ontapi.jena.model.OntEntity}) is prohibited,
 * but they still can be treated as any other objects ({@link OntObject})
 * The third one is a week variant of {@link Mode#STRICT}.
 * <p>
 * Created by @szuev on 21.01.2017.
 */
@FunctionalInterface
public interface Configurable<T> {

    /**
     * Choose object by the given argument.
     *
     * @param t Mode
     * @return the wrapped object.
     */
    T select(Configurable.Mode t);

    default T get(Mode m) {
        return OntJenaException.notNull(select(OntJenaException.notNull(m, "Null mode.")), "Null result for mode " + m + ".");
    }

    enum Mode {
        /**
         * The following punnings are considered as illegal and are excluded:
         * - owl:Class &lt;-&gt; rdfs:Datatype
         * - owl:ObjectProperty &lt;-&gt; owl:DatatypeProperty
         * - owl:ObjectProperty &lt;-&gt; owl:AnnotationProperty
         * - owl:AnnotationProperty &lt;-&gt; owl:DatatypeProperty
         */
        STRICT,
        /**
         * Forbidden intersections of declarations:
         * - Class &lt;-&gt; Datatype
         * - ObjectProperty &lt;-&gt; DataProperty
         */
        MEDIUM,
        /**
         * Allow everything.
         */
        LAX,
    }

}
