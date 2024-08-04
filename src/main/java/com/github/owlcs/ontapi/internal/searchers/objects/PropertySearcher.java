/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2023, owl.cs group.
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
import org.apache.jena.ontapi.model.OntModel;
import org.apache.jena.ontapi.model.OntStatement;
import org.apache.jena.ontapi.utils.Iterators;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.OWLProperty;

/**
 * An abstract {@link OWLProperty} searcher.
 * Created by @ssz on 25.07.2020.
 *
 * @param <E> - subtype of {@link OWLProperty}
 */
abstract class PropertySearcher<E extends OWLProperty> extends WithBuiltins<E> {

    @Override
    protected ExtendedIterator<OntStatement> listRootStatements(OntModel m, Resource uri) {
        return Iterators.concat(super.listRootStatements(m, uri), listByPredicate(m, (Property) uri));
    }

    @Override
    protected Property toResource(OntModel m, String uri) {
        return m.getProperty(uri);
    }

    @Override
    protected ExtendedIterator<String> listEntities(OntModel m, AxiomsSettings conf) {
        return listEntities(m, getModelBuiltins(m, conf), conf);
    }

    @Override
    protected boolean containsEntity(String uri, OntModel m, AxiomsSettings conf) {
        Property p = toResource(m, uri);
        return isInBuiltinSpec(m, p) ? containsInOntology(p, m, conf) : containsDeclaration(p, m, conf);
    }
}
