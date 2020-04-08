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

package com.github.owlcs.ontapi.internal.searchers;

import com.github.owlcs.ontapi.internal.*;
import com.github.owlcs.ontapi.jena.model.OntAnnotation;
import com.github.owlcs.ontapi.jena.model.OntModel;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLPrimitive;

import java.util.function.Supplier;

/**
 * A base abstraction for any axioms-by-primitive helper (referencing-axioms functionality).
 * Created by @ssz on 19.03.2020.
 *
 * @param <P> - a subtype of {@link OWLPrimitive}:
 *            either {@link org.semanticweb.owlapi.model.OWLEntity},
 *            {@link org.semanticweb.owlapi.model.IRI} or {@link org.semanticweb.owlapi.model.OWLLiteral}
 */
public abstract class ByPrimitive<P extends OWLPrimitive> extends BaseSearcher {

    /**
     * Answers {@code true} if need to check annotations also.
     *
     * @param m {@link OntModel}
     * @return boolean
     */
    protected static boolean includeAnnotations(OntModel m) {
        return !(m instanceof SearchModel) || ((SearchModel) m).hasAnnotations();
    }

    /**
     * Finds the root (top-level) annotation resource
     * for the given sub-annotation (a resource with {@code owl:Annotation} as {@code rdf:type}).
     *
     * @param annotation {@link OntAnnotation} - sub annotation, not {@code null}
     * @return root or the same input annotation - a resource with {@code owl:Axiom} as {@code rdf:type}
     */
    public static OntAnnotation getRoot(OntAnnotation annotation) {
        OntAnnotation parent = annotation.parent().orElse(null);
        return parent == null ? annotation : getRoot(parent);
    }

    /**
     * Lists all axioms that contain the given {@link P}.
     *
     * @param primitive a {@link P}, not {@code null}
     * @param model     a {@link Supplier} to derive nonnull {@link OntModel}, not {@code null}
     * @param factory   an {@link InternalObjectFactory}, not {@code null}
     * @param config    {@link InternalConfig}, not {@code null}
     * @return an {@link ExtendedIterator} of {@link OWLAxiom}s wrapped with {@link ONTObject}
     */
    public abstract ExtendedIterator<ONTObject<? extends OWLAxiom>> listAxioms(P primitive,
                                                                               Supplier<OntModel> model,
                                                                               InternalObjectFactory factory,
                                                                               InternalConfig config);

}
