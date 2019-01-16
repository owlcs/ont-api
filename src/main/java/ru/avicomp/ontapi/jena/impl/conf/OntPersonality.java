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

import org.apache.jena.rdf.model.Resource;
import ru.avicomp.ontapi.jena.model.OntEntity;
import ru.avicomp.ontapi.jena.model.OntObject;

/**
 * A {@link ru.avicomp.ontapi.jena.model.OntGraphModel Ontology RDF Model} configuration object,
 * that serves for the following purposes:
 * <ul>
 * <li>Defines a set of permitted mappings from [interface] Class objects to {@link OntObjectFactory} factories that can generate instances of the facet represented by the Class.</li>
 * <li>Defines a set of builtin {@link OntEntity entities}, that do not require explicit declarations</li>
 * <li>Defines a set of OWL punnings</li>
 * <li>Defines a set of reserved {@link Resource}s and {@link org.apache.jena.rdf.model.Property}s, that cannot be used as OWL Entities</li>
 * </ul>
 * Created by @szz on 15.01.2019.
 */
public interface OntPersonality {

    /**
     * Gets the implementation-factory for the specified type,
     * returning {@code null} if there isn't one available.
     * TODO: change return type form a concrete class to an interface.
     *
     * @param type a class-type of {@link OntObject}
     * @return {@link OntObjectFactory} a factory to create an instance of the given type
     */
    OntObjectFactory getOntImplementation(Class<? extends OntObject> type);

    /**
     * Makes a full copy of this configuration.
     *
     * @return {@link OntPersonality} a new instance identical to this
     */
    OntPersonality copy();

    /**
     * Returns a punnings vocabulary.
     *
     * @return {@link Punnings}
     */
    Punnings getPunnings();

    /**
     * Returns a builtins vocabulary.
     *
     * @return {@link Builtins}
     */
    Builtins getBuiltins();

    /**
     * Returns a reserved vocabulary.
     *
     * @return {@link Reserved}
     */
    Reserved getReserved();

    /**
     * A vocabulary of built-in {@link OntEntity OWL Entities}.
     * A {@link ru.avicomp.ontapi.jena.model.OntGraphModel model}, that holds this configuration,
     * can contain entities without explicit declarations, it they IRIs are determined by this vocabulary.
     */
    interface Builtins extends Vocabulary<OntEntity> {
    }

    /**
     * A punnings vocabulary.
     * For a given {@link OntEntity} type it returns a {@code Set} of forbidden types.
     * A {@link ru.avicomp.ontapi.jena.model.OntGraphModel model}, that holds this configuration,
     * cannot contain entities which have intersection in {@link ru.avicomp.ontapi.jena.vocabulary.RDF#type rdf:type}
     * that are determined by this vocabulary.
     *
     * @see <a href='https://www.w3.org/TR/owl2-new-features/#F12:_Punning'>Punnings</a>
     */
    interface Punnings extends Vocabulary<OntEntity> {
    }

    /**
     * A vocabulary of reserved IRIs.
     * A {@link ru.avicomp.ontapi.jena.model.OntGraphModel model}, that holds this configuration,
     * cannot contain entities with IRIs from this vocabulary.
     */
    interface Reserved extends Vocabulary<Resource> {
    }
}
