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

package com.github.owlcs.ontapi.internal.searchers;

import com.github.owlcs.ontapi.jena.model.OntClass;
import com.github.owlcs.ontapi.jena.model.OntStatement;
import com.github.owlcs.ontapi.jena.vocabulary.XSD;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;

import java.util.Set;

/**
 * Created by @ssz on 06.09.2020.
 */
public interface ForDatatype extends ForTopEntity {

    Set<Class<? extends OntClass.CardinalityRestrictionCE<?, ?>>> DATA_CARDINALITY_TYPES =
            Set.of(OntClass.DataMaxCardinality.class, OntClass.DataMinCardinality.class, OntClass.DataCardinality.class);

    /**
     * Answers a class-type of a {@link OntClass.RestrictionCE},
     * whose instances have the specified {@code OWLDatatype} in their composition,
     * which, at the same time, is excluded from the signature according to OWLAPI design.
     *
     * @param uri - {@code String}
     * @return - {@code Class}-type or {@code null}
     * @see <a href='https://github.com/owlcs/owlapi/issues/783'>OWLAPI Issue 783</a>
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static Class<? extends OntClass.RestrictionCE<?>> getSpecialDataRestrictionType(String uri) {
        Class res = null;
        if (XSD.xboolean.getURI().equals(uri)) {
            res = OntClass.HasSelf.class;
        }
        if (XSD.nonNegativeInteger.getURI().equals(uri)) {
            res = OntClass.CardinalityRestrictionCE.class;
        }
        return res;
    }

    @Override
    default Resource getTopEntity() {
        return RDFS.Literal;
    }

    @Override
    default boolean isCardinalityRestriction(OntStatement s) {
        return DATA_CARDINALITY_TYPES.stream().anyMatch(t -> s.getSubject().canAs(t));
    }
}
