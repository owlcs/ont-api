/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2022, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.github.owlcs.ontapi.owlapi.objects.entity;

import com.github.owlcs.ontapi.owlapi.OWLObjectImpl;
import org.apache.jena.rdf.model.Resource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import java.util.Objects;


public class DataPropertyImpl extends OWLObjectImpl implements OWLDataProperty {

    private final IRI iri;
    private final boolean builtin;

    /**
     * @param iri property iri
     */
    public DataPropertyImpl(IRI iri) {
        this.iri = Objects.requireNonNull(iri, "iri cannot be null");
        builtin = iri.equals(OWLRDFVocabulary.OWL_TOP_DATA_PROPERTY.getIRI()) || iri.equals(
                OWLRDFVocabulary.OWL_BOTTOM_DATA_PROPERTY.getIRI());
    }

    /**
     * Creates an {@link OWLDataProperty} instance using the {@link Resource} reference.
     *
     * @param r {@link Resource}, not {@code null}
     * @return {@link OWLDataProperty}
     * @throws NullPointerException if incorrect input
     */
    public static OWLDataProperty fromResource(Resource r) {
        return new DataPropertyImpl(IRI.create(Objects.requireNonNull(r.getURI(), "Not URI: " + r)));
    }

    @Override
    public String toStringID() {
        return iri.toString();
    }

    @Override
    public IRI getIRI() {
        return iri;
    }

    @Override
    public boolean isBuiltIn() {
        return builtin;
    }
}
