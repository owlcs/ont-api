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

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.rdf.model.Literal;
import ru.avicomp.ontapi.jena.vocabulary.OWL;

import java.util.stream.Stream;

/**
 * Interface encapsulating an Ontology <b>D</b>ata <b>T</b>ype {@link OntEntity OWL Entity}
 * (i.e. named {@link OntDR data range} expression).
 * <p>
 * Created by szuev on 01.11.2016.
 */
public interface OntDT extends OntEntity, OntDR {

    /**
     * Lists all equivalent data ranges.
     * The pattern to search is {@code DN owl:equivalentClass D}, where {@code DN} is this {@link OntDT Data Type},
     * and {@code D} is a search object, the {@link OntDR data-range expression}.
     *
     * @return Stream of {@link OntDR}s
     * @see OntCE#equivalentClass()
     */
    default Stream<OntDR> equivalentClass() {
        return objects(OWL.equivalentClass, OntDR.class);
    }

    /**
     * Creates an equivalent class statement with the given {@link OntDR Data Range expression}.
     *
     * @param other {@link OntDR}, not null
     * @return {@link OntStatement}
     * @see OntCE#addEquivalentClass(OntCE)
     */
    default OntStatement addEquivalentClass(OntDR other) {
        return addStatement(OWL.equivalentClass, other);
    }

    /**
     * Removes an equivalent data range,
     * attached to this data-type on predicate {@link OWL#equivalentClass owl:equivalenrClass}
     *
     * @param other {@link OntDR}
     * @see OntCE#removeEquivalentClass(OntCE)
     */
    default void removeEquivalentClass(OntDR other) {
        remove(OWL.equivalentClass, other);
    }

    /**
     * Creates a Jena Datatype.
     *
     * @return {@link RDFDatatype Jena RDF Datatype}
     */
    default RDFDatatype toRDFDatatype() {
        return TypeMapper.getInstance().getSafeTypeByName(getURI());
    }

    /**
     * Builds a typed literal from its value form.
     *
     * @param lex String, lexical form of the result literal
     * @return {@link Literal}
     * @see org.apache.jena.rdf.model.Model#createTypedLiteral(String, RDFDatatype)
     */
    default Literal createLiteral(String lex) {
        return getModel().createTypedLiteral(lex, toRDFDatatype());
    }
}
