/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2020, The University of Manchester, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.internal;

import com.github.owlcs.ontapi.config.AxiomsSettings;
import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.jena.utils.Iter;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.OWLObject;

import java.util.Optional;

/**
 * An {@link OWLObject OWL Object}s searcher.
 * Created by @ssz on 19.04.2020.
 *
 * @param <K> - {@link OWLObject}
 * @see ONTObject
 * @see ByObjectSearcher
 */
public interface ObjectsSearcher<K extends OWLObject> {

    /**
     * Lists all objects from the specified {@code model}.
     *
     * @param model   {@link OntModel}, not {@code null}
     * @param factory {@link ONTObjectFactory}, not {@code null}
     * @param config  {@link AxiomsSettings}, not {@code null}
     * @return an {@link ExtendedIterator} over {@link K} wrapped with {@link ONTObject}
     */
    ExtendedIterator<ONTObject<K>> listONTObjects(OntModel model, ONTObjectFactory factory, AxiomsSettings config);

    /**
     * Answers {@code true} if the specified {@code model} contains the given {@code object}.
     *
     * @param object  {@link K} - an object to search, not {@code null}
     * @param model   {@link OntModel} - a model to search for, not {@code null}
     * @param factory {@link ONTObjectFactory - to produce ONT-API Objects, not {@code null}}
     * @param config  {@link AxiomsSettings} - to configure the process, not {@code null}
     * @return boolean
     */
    default boolean containsONTObject(K object, OntModel model, ONTObjectFactory factory, AxiomsSettings config) {
        return findONTObject(object, model, factory, config).isPresent();
    }

    /**
     * Finds a model-{@code object} from the specified {@code model}.
     *
     * @param object  {@link K} - an object to search, not {@code null}
     * @param model   {@link OntModel} - a model to search for, not {@code null}
     * @param factory {@link ONTObjectFactory - to produce ONT-API Objects, not {@code null}}
     * @param config  {@link AxiomsSettings} - to configure the process, not {@code null}
     * @return an {@code Optional} that wraps an {@code ONTObject}-container with a desired {@link K}-instance
     */
    default Optional<ONTObject<K>> findONTObject(K object, OntModel model, ONTObjectFactory factory, AxiomsSettings config) {
        return Iter.findFirst(listONTObjects(model, factory, config).filterKeep(x -> x.getOWLObject().equals(object)));
    }

}
