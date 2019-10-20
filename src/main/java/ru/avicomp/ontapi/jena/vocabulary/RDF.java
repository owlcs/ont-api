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

package ru.avicomp.ontapi.jena.vocabulary;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

/**
 * Vocabulary definition for the standard RDF.
 * See <a href='http://www.w3.org/1999/02/22-rdf-syntax-ns#'>schema</a>.
 * <p>
 * Created by @szuev on 21.12.2016.
 */
public class RDF extends org.apache.jena.vocabulary.RDF {

    /**
     * This property is used explicitly in facet restrictions.
     * Also, it can be used as literal type (e.g. 'test'^^rdf:PlainLiteral) in old ontologies based on RDF-1.0
     *
     * @see <a href='https://www.w3.org/TR/rdf-plain-literal'>rdf:PlainLiteral: A Datatype for RDF Plain Literals (Second Edition)</a>
     */
    public final static Resource PlainLiteral = resource("PlainLiteral");

    /**
     * This property is used in facet restrictions.
     * The facet {@code rdf:langRange} can be used to refer to a subset of strings containing the language tag.
     *
     * @see <a href='https://www.w3.org/TR/rdf-plain-literal/#langRange'>rdf:langRange</a>
     */
    public static final Property langRange = property("langRange");

}
