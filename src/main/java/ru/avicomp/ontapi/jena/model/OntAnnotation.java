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

package ru.avicomp.ontapi.jena.model;

import java.util.stream.Stream;

/**
 * An annotation ont-object.
 * It's an anonymous jena-resource ({@link OntObject}) with one of the two types:
 * <ul>
 * <li>owl:Axiom ({@link ru.avicomp.ontapi.jena.vocabulary.OWL#Axiom}) for root annotations, it is usually owned by axiomatic statements.</li>
 * <li>owl:Annotation (see {@link ru.avicomp.ontapi.jena.vocabulary.OWL#Annotation}) for sub-annotations,
 * and also for annotation of several specific axioms with main-statement {@code _:x rdf:type @type} where @type is
 * owl:AllDisjointClasses, owl:AllDisjointProperties, owl:AllDifferent or owl:NegativePropertyAssertion.</li>
 * </ul>
 * Example:
 * <pre>
 * {@code
 * [ a                      owl:Axiom ;
 *   rdfs:comment           "some comment 1", "some comment 2"@fr ;
 *   owl:annotatedProperty  rdf:type ;
 *   owl:annotatedSource    <http://example.test.org#SomeClassN1> ;
 *   owl:annotatedTarget    owl:Class
 * ] .
 * }
 * </pre>
 * Created by @szuev on 26.03.2017.
 * @see OntStatement
 */
public interface OntAnnotation extends OntObject {

    /**
     * Returns the annotations assertions attached to this object.
     * The annotation assertion is a statements with annotation property ({@link OntNAP}) as predicate.
     * The example above contains two such statements: '_:x rdfs:comment "comment1";' and '_:x rdfs:comment "comment2"@fr'.
     *
     * @return Stream of annotation statements {@link OntStatement}s,
     * @see OntObject#annotations()
     */
    Stream<OntStatement> assertions();

}
