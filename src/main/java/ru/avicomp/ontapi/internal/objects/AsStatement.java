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

package ru.avicomp.ontapi.internal.objects;

import org.apache.jena.graph.FrontsTriple;
import org.apache.jena.graph.Triple;
import ru.avicomp.ontapi.jena.model.OntStatement;

/**
 * The generic interface for things that can be seen as wrappers round {@link OntStatement Ontology Statement},
 * which is an extended {@link org.apache.jena.rdf.model.Statement Jena Statement},
 * which, in turn, is a {@link Triple} within {@link org.apache.jena.rdf.model.Model Jena Model}
 * (in our case {@link ru.avicomp.ontapi.jena.model.OntGraphModel Ontology Model}).
 * <p>
 * Created by @szz on 01.10.2019.
 *
 * @see ru.avicomp.ontapi.internal.AsRDFNode
 * @see ru.avicomp.ontapi.AsNode
 * @since 1.4.3
 */
public interface AsStatement extends FrontsTriple {

    /**
     * Answers the root statement of this object.
     *
     * @return {@link OntStatement}, never {@code null}
     */
    OntStatement asStatement();

    /**
     * Answers the root triple of this object.
     *
     * @return {@link Triple}, never {@code null}
     */
    @Override
    default Triple asTriple() {
        return asStatement().asTriple();
    }

}
