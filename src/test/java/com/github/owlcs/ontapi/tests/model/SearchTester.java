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

package com.github.owlcs.ontapi.tests.model;

import com.github.owlcs.ontapi.internal.PrefixMappingRenderer;
import org.apache.jena.ontapi.OntModelFactory;
import org.semanticweb.owlapi.model.OWLObject;

import java.util.Objects;

/**
 * Created by @ssz on 30.05.2020.
 */
abstract class SearchTester {

    final long count;
    final String type;

    SearchTester(String type, long count) {
        this.type = Objects.requireNonNull(type);
        this.count = count;
    }

    static long toLong(OWLObject obj) {
        return obj.anonymousIndividuals().findFirst().isPresent() ? toString(obj).hashCode() : obj.hashCode();
    }

    static String toString(OWLObject obj) {
        return new PrefixMappingRenderer(OntModelFactory.STANDARD).render(obj).replaceAll("\\s_:[a-z\\d\\-]+", " _:x");
    }

}
