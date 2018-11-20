/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2018, Avicomp Services, AO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package ru.avicomp.ontapi;

import org.apache.jena.graph.BlankNodeId;
import org.apache.jena.graph.impl.LiteralLabel;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;

/**
 * An extended {@link OWLDataFactory OWL-API DataFactory} for creating entities, axioms and axiom components.
 * <p>
 * Created by @szuev on 16.08.2018.
 *
 * @since 1.3.0
 */
public interface DataFactory extends OWLDataFactory {

    /**
     * Creates an {@link OWLAnonymousIndividual OWL-API Anonymous Individual} for the given {@link BlankNodeId Jena Blank Node Id}.
     *
     * @param id {@link BlankNodeId}, not {@code null}
     * @return {@link OWLAnonymousIndividual} instance
     */
    OWLAnonymousIndividual getOWLAnonymousIndividual(BlankNodeId id);

    /**
     * Creates an {@link OWLLiteral OWL-API Literal} for the given {@link LiteralLabel Jena Literal Label}.
     *
     * @param label {@link LiteralLabel}, not {@code null}
     * @return {@link OWLLiteral} instance
     */
    OWLLiteral getOWLLiteral(LiteralLabel label);
}
