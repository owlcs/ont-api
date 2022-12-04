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

package com.github.owlcs.ontapi.internal.searchers.objects;

import com.github.owlcs.ontapi.config.AxiomsSettings;
import com.github.owlcs.ontapi.internal.searchers.ForTopEntity;
import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.jena.utils.Iterators;
import org.apache.jena.rdf.model.Resource;
import org.semanticweb.owlapi.model.OWLEntity;

import java.util.Set;

/**
 * Created by @ssz on 06.09.2020.
 *
 * @param <E> either {@link org.semanticweb.owlapi.model.OWLClass} or {@link org.semanticweb.owlapi.model.OWLDatatype}
 */
abstract class WithCardinality<E extends OWLEntity> extends WithBuiltins<E> implements ForTopEntity {

    private boolean containsCardinalityAxiom(OntModel model, AxiomsSettings conf) {
        return containsAxiom(Iterators.flatMap(listImplicitStatements(model), s -> listRootStatements(model, s)), conf);
    }

    protected void addTopEntity(Set<String> res, OntModel model, AxiomsSettings conf) {
        String uri = getTopEntityURI();
        if (res.contains(uri)) {
            return;
        }
        if (containsCardinalityAxiom(model, conf)) {
            res.add(uri);
        }
    }

    @Override
    protected final boolean containsEntity(String uri, OntModel model, AxiomsSettings conf) {
        Resource res = toResource(model, uri);
        if (!isInBuiltinSpec(model, res)) {
            return containsDeclaration(res, model, conf);
        }
        if (getTopEntity().equals(res)) {
            if (containsCardinalityAxiom(model, conf)) {
                return true;
            }
        }
        return containsInOntology(res, model, conf);
    }
}
