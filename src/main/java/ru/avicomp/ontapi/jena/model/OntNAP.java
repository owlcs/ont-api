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

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;

import java.util.stream.Stream;

/**
 * Interface encapsulating a <b>N</b>amed <b>A</b>nnotation <b>P</b>roperty.
 * The first word in this abbreviation means that it is an URI-{@link Resource Resource}.
 * This is an extension to the standard jena {@link Property},
 * the {@link OntEntity OWL Entity} and the {@link OntPE abstract property expression} interfaces.
 * In OWL2 an Annotation Property cannot be anonymous.
 * <p>
 * Created by szuev on 01.11.2016.
 */
public interface OntNAP extends OntPE, OntProperty<OntNAP> {

    /**
     * {@inheritDoc}
     *
     * @return <b>distinct</b> {@code Stream} of annotation properties
     * @since 1.4.0
     */
    Stream<OntNAP> superProperties(boolean direct);

    /**
     * {@inheritDoc}
     *
     * @return <b>distinct</b> {@code Stream} of annotation properties
     * @since 1.4.0
     */
    Stream<OntNAP> subProperties(boolean direct);

    /**
     * Lists all valid annotation property domains in the form of java {@code Stream}.
     *
     * @return {@code Stream} of uri-{@link Resource}s
     * @since 1.4.0
     */
    @Override
    Stream<Resource> domains();

    /**
     * Lists all valid annotation property ranges.
     *
     * @return {@code Stream} of uri-{@link Resource}s
     * @since 1.4.0
     */
    @Override
    Stream<Resource> ranges();

    /**
     * Adds domain statement {@code A rdfs:domain U},
     * where {@code A} is this annotation property and {@code U} is any IRI.
     *
     * @param domain uri-{@link Resource}, not {@code null}
     * @return {@link OntStatement} to allow subsequent annotations adding
     * @throws ru.avicomp.ontapi.jena.OntJenaException in case of anonymous resource is specified
     * @see #domains()
     * @see #removeDomain(Resource)
     * @see #addDomain(Resource)
     * @see #addRangeStatement(Resource)
     * @since 1.4.0
     */
    OntStatement addDomainStatement(Resource domain);

    /**
     * Adds range statement {@code A rdfs:range U}, where {@code A} is an annotation property, {@code U} is any IRI.
     *
     * @param range uri-{@link Resource}, not {@code null}
     * @return {@link OntStatement} to allow subsequent annotations adding
     * @throws ru.avicomp.ontapi.jena.OntJenaException in case of input is anonymous resource
     * @see #ranges()
     * @see #removeRange(Resource)
     * @see #addRange(Resource)
     * @see #addDomainStatement(Resource)
     * @since 1.4.0
     */
    OntStatement addRangeStatement(Resource range);

    /**
     * Lists all direct super properties.
     * The pattern is {@code A1 rdfs:subPropertyOf A2},
     * where {@code A1} is this property and {@code A2} is what needs to be returned.
     *
     * @return {@code Stream} of {@link OntNAP}s
     * @see #addSubPropertyOfStatement(OntNAP)
     * @see #addSuperProperty(OntNAP)
     * @see #removeSuperProperty(Resource)
     * @see #superProperties(boolean)
     */
    @Override
    default Stream<OntNAP> superProperties() {
        return objects(RDFS.subPropertyOf, OntNAP.class);
    }

    /**
     * Adds a statement with the {@link RDFS#range} as predicate and the specified {@code uri} as an object.
     *
     * @param uri an URI-{@link Resource}, not {@code null}
     * @return <b>this</b> instance to allow cascading calls
     * @see #addRangeStatement(Resource)
     */
    default OntNAP addRange(Resource uri) {
        addRangeStatement(uri);
        return this;
    }

    /**
     * Adds a statement with the {@link RDFS#domain} as predicate and the specified {@code uri} as an object.
     *
     * @param uri an URI-{@link Resource}, not {@code null}
     * @return <b>this</b> instance to allow cascading calls
     * @see #addDomainStatement(Resource)
     */
    default OntNAP addDomain(Resource uri) {
        addDomainStatement(uri);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    default OntNAP removeDomain(Resource domain) {
        remove(RDFS.domain, domain);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    default OntNAP removeRange(Resource range) {
        remove(RDFS.range, range);
        return this;
    }

    /**
     * Adds the given property as super property returning this property itself.
     *
     * @param property {@link OntNAP}, not {@code null}
     * @return <b>this</b> instance to allow cascading calls
     * @see #removeSuperProperty(Resource)
     * @since 1.4.0
     */
    default OntNAP addSuperProperty(OntNAP property) {
        addSubPropertyOfStatement(property);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default OntNAP removeSuperProperty(Resource property) {
        remove(RDFS.subPropertyOf, property);
        return this;
    }

    /**
     * Adds the given property as super property returning a new statement to annotate.
     * The triple pattern is {@code this rdfs:subPropertyOf property}).
     *
     * @param property {@link OntNAP}, not {@code null}
     * @return {@link OntStatement} to allow subsequent annotations adding
     * @since 1.4.0
     */
    default OntStatement addSubPropertyOfStatement(OntNAP property) {
        return addStatement(RDFS.subPropertyOf, property);
    }

}
