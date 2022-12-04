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

import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.jena.model.OntStatement;
import com.github.owlcs.ontapi.jena.utils.Iterators;
import com.github.owlcs.ontapi.jena.vocabulary.OWL;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.iterator.ExtendedIterator;

/**
 * A technical interface that contains functionality common for searchers
 * ({@link com.github.owlcs.ontapi.internal.ObjectsSearcher ObjectsSearcher}
 * and {@link com.github.owlcs.ontapi.internal.ByObjectSearcher ByObjectSearcher})
 * which have generic type {@link org.semanticweb.owlapi.model.OWLClass OWLClass} or
 * {@link org.semanticweb.owlapi.model.OWLDatatype OWLDatatype}.
 * <p>
 * For internal usage only.
 * <p>
 * Created by @ssz on 06.09.2020.
 */
public interface ForTopEntity {

    Resource getTopEntity();

    boolean isCardinalityRestriction(OntStatement s);

    ExtendedIterator<OntStatement> listByPredicate(OntModel m, Property uri);

    default String getTopEntityURI() {
        return getTopEntity().getURI();
    }

    default ExtendedIterator<OntStatement> listImplicitStatements(OntModel m) {
        return Iterators.flatMap(Iterators.of(OWL.cardinality, OWL.maxCardinality, OWL.minCardinality), p -> listByPredicate(m, p))
                .filterKeep(this::isCardinalityRestriction);
    }
}
