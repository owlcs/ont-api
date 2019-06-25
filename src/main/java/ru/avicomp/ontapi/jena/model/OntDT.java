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

package ru.avicomp.ontapi.jena.model;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;
import ru.avicomp.ontapi.jena.vocabulary.OWL;

import java.util.Objects;
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
     * @return {@code Stream} of {@link OntDR}s
     * @see OntCE#equivalentClasses()
     * @since 1.4.2
     */
    default Stream<OntDR> equivalentClasses() {
        return objects(OWL.equivalentClass, OntDR.class);
    }

    /**
     * Creates an equivalent class statement with the given {@link OntDR Data Range expression}.
     *
     * @param other {@link OntDR}, not {@code null}
     * @return {@link OntStatement} to allow the subsequent annotations addition
     * @see #addEquivalentClass(OntDR)
     * @see #removeEquivalentClass(Resource)
     * @see OntCE#addEquivalentClassStatement(OntCE)
     * @since 1.4.0
     */
    default OntStatement addEquivalentClassStatement(OntDR other) {
        return addStatement(OWL.equivalentClass, other);
    }

    /**
     * Creates an equivalent class statement with the given {@link OntDR Data Range expression}.
     *
     * @param other {@link OntDR}, not {@code null}
     * @return <b>this</b> instance to allow cascading calls
     * @see #addEquivalentClassStatement(OntDR)
     * @see #removeEquivalentClass(Resource)
     * @see OntCE#addEquivalentClass(OntCE)
     */
    default OntDT addEquivalentClass(OntDR other) {
        addEquivalentClassStatement(other);
        return this;
    }

    /**
     * Removes the given equivalent data range,
     * that is attached to this data-type on predicate {@link OWL#equivalentClass owl:equivalenrClass},
     * including all the statement's related annotations.
     * No-op in case nothing is found.
     * The {@code null} input means removing all {@link OWL#equivalentClass owl:equivalentClass} statements
     * with all their annotations.
     *
     * @param other {@link Resource}, or {@code null} to remove all equivalent data ranges
     * @return <b>this</b> instance to allow cascading calls
     * @see #addEquivalentClass(OntDR)
     * @see #addEquivalentClassStatement(OntDR)
     * @see OntCE#removeEquivalentClass(Resource)
     */
    default OntDT removeEquivalentClass(Resource other) {
        remove(OWL.equivalentClass, other);
        return this;
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
     * Builds a typed literal from its value form given as an object.
     * Note: there is no validation for lexical form.
     *
     * @param obj anything, not {@code null}
     * @return {@link Literal}
     * @since 1.4.1
     */
    default Literal createLiteral(Object obj) {
        return createLiteral(String.valueOf(Objects.requireNonNull(obj)));
    }

    /**
     * Builds a typed literal from its value form.
     * Note: there is no validation for lexical form,
     * so it is possible to create an illegal literal, e.g. {@code "wrong"^^xsd:int}.
     *
     * @param lex String, lexical form of the result literal, not {@code null}
     * @return {@link Literal}
     * @see org.apache.jena.rdf.model.Model#createTypedLiteral(String, RDFDatatype)
     */
    default Literal createLiteral(String lex) {
        return getModel().createTypedLiteral(Objects.requireNonNull(lex), toRDFDatatype());
    }

    /**
     * Lists all equivalent classes.
     *
     * @return {@code Stream} of equivalent {@link OntDR}s
     * @deprecated since 1.4.2: use the method {@link #equivalentClasses()} instead
     */
    @Deprecated
    default Stream<OntDR> equivalentClass() {
        return equivalentClasses();
    }

}
