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

package ru.avicomp.ontapi.jena.model;

import ru.avicomp.ontapi.OntApiException;

import java.util.stream.Stream;

/**
 * An Ontology ID.
 * Each {@link OntGraphModel OWL2 Obtology} must have the only one {@link OntID id} inside.
 * <p>
 * Created by szuev on 09.11.2016.
 */
public interface OntID extends OntObject {

    /**
     * Returns an iri from {@code @this owl:versionIRI @iri} statement.
     *
     * @return String, iri or null
     */
    String getVersionIRI();

    /**
     * Assigns a new version iri to this ontology id object.
     * Null argument means that current version iri should be deleted.
     * @param uri String, can be null.
     */
    void setVersionIRI(String uri);

    /**
     * Adds a triple {@code @this owl:import @uri}.
     *
     * @param uri String, not null
     * @throws OntApiException if input is wrong
     */
    void addImport(String uri) throws OntApiException;

    /**
     * Removes a triple {@code this @owl:import @uri} from this resource.
     * @param uri String, not null
     */
    void removeImport(String uri);

    /**
     * Lists all {@code owl:import}s.
     * @return Stream of Strings
     */
    Stream<String> imports();

}
