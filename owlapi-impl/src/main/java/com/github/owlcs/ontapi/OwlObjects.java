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

package com.github.owlcs.ontapi;

import org.semanticweb.owlapi.model.HasAnnotations;
import org.semanticweb.owlapi.model.HasComponents;
import org.semanticweb.owlapi.model.HasIRI;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLObject;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Helper to work with {@link OWLObject} (mostly for retrieving components from {@link OWLAxiom owl-axioms}).
 * Note: its methods are recursive.
 * <p>
 * Created by @szuev on 08.02.2017.
 */
@SuppressWarnings("WeakerAccess")
public class OwlObjects {

    public static <O extends OWLObject> Stream<O> parseComponents(Class<? extends O> type, HasComponents structure) {
        return structure.componentsWithoutAnnotations().flatMap(o -> toStream(type, o));
    }

    public static <O extends OWLObject> Stream<O> parseAnnotations(Class<? extends O> type, HasAnnotations structure) {
        return structure.annotations().flatMap(o -> toStream(type, o));
    }

    public static <O extends OWLObject, A extends HasComponents> Stream<O> objects(Class<? extends O> type, A container) {
        Stream<O> res = parseComponents(type, container);
        if (container instanceof HasAnnotations) {
            res = Stream.concat(res, parseAnnotations(type, (HasAnnotations) container));
        }
        return res;
    }

    public static <A extends HasComponents> Stream<IRI> iris(A container) {
        return Stream.concat(objects(IRI.class, container),
                objects(OWLObject.class, container)
                        .map(o -> o instanceof HasIRI ? ((HasIRI) o).getIRI() : null)
                        .filter(Objects::nonNull));
    }

    private static <O extends OWLObject> Stream<O> toStream(Class<? extends O> type, Object o) {
        if (type.isInstance(o)) {
            return Stream.of(type.cast(o));
        }
        if (o instanceof HasComponents) {
            if (o instanceof HasAnnotations) {
                return objects(type, (HasComponents & HasAnnotations) o);
            }
            return parseComponents(type, (HasComponents) o);
        }
        if (o instanceof HasAnnotations) {
            return parseAnnotations(type, (HasAnnotations) o);
        }
        Stream<?> stream = null;
        if (o instanceof Stream) {
            stream = ((Stream<?>) o);
        } else if (o instanceof Collection) {
            stream = ((Collection<?>) o).stream();
        }
        if (stream != null) {
            return stream.flatMap(x -> toStream(type, x));
        }
        return Stream.empty();
    }

}
