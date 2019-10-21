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
package com.github.owlcs.ontapi.owlapi.objects.dr;

import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLDatatypeRestriction;
import org.semanticweb.owlapi.model.OWLFacetRestriction;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * @author Matthew Horridge, The University Of Manchester, Bio-Health Informatics Group
 * @since 1.2.0
 */
public class OWLDatatypeRestrictionImpl extends OWLAnonymousDataRangeImpl implements OWLDatatypeRestriction {

    protected final OWLDatatype datatype;
    protected final List<OWLFacetRestriction> restrictions;

    /**
     * @param datatype          a {@link OWLDatatype}
     * @param restrictions a {@code Collection} of facet restriction
     */
    public OWLDatatypeRestrictionImpl(OWLDatatype datatype, Collection<OWLFacetRestriction> restrictions) {
        this.datatype = Objects.requireNonNull(datatype, "datatype cannot be null");
        this.restrictions = toContentList(restrictions, "facet restrictions cannot be null");
    }

    @Override
    public OWLDatatype getDatatype() {
        return datatype;
    }

    @Override
    public Stream<OWLFacetRestriction> facetRestrictions() {
        return restrictions.stream();
    }

    @Override
    public List<OWLFacetRestriction> facetRestrictionsAsList() {
        return restrictions;
    }
}
