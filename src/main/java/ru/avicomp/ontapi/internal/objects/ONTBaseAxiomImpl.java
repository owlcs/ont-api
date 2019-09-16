/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2019, Avicomp Services, AO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package ru.avicomp.ontapi.internal.objects;

import org.apache.jena.graph.Triple;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.util.NNF;
import ru.avicomp.ontapi.internal.*;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntStatement;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A base axiom.
 * TODO: need to remove {@link ONTAxiomImpl} and rename this class to same name (ONTAxiomImpl)
 * Created by @ssz on 15.09.2019.
 *
 * @param <X> - the {@link OWLAxiom} subtype, must be the same that this class implements
 * @see ru.avicomp.ontapi.owlapi.axioms.OWLAxiomImpl
 * @since 1.4.3
 */
public abstract class ONTBaseAxiomImpl<X extends OWLAxiom>
        extends ONTBaseTripleImpl implements OWLAxiom, HasConfig {

    protected ONTBaseAxiomImpl(Object subject, String predicate, Object object, Supplier<OntGraphModel> m) {
        super(subject, predicate, object, m);
    }

    /**
     * Collects all annotations for the given axiom's main {@link OntStatement}.
     *
     * @param axiom   {@link OntStatement} the root axiom's statement, not {@code null}
     * @param factory {@link InternalObjectFactory} to retrieve {@link ONTObject}s, not {@code null}
     * @param config  {@link InternalConfig} to control reading, not {@code null}
     * @return a sorted nonnull distinct {@code Collection}
     * of {@link ONTObject}s with {@link OWLAnnotation}s (can be empty if no annotations)
     * @see ONTAnnotationImpl#collectAnnotations(OntStatement, InternalObjectFactory)
     */
    protected static Collection<ONTObject<OWLAnnotation>> collectAnnotations(OntStatement axiom,
                                                                             InternalObjectFactory factory,
                                                                             InternalConfig config) {
        Map<OWLAnnotation, ONTObject<OWLAnnotation>> res = new TreeMap<>();
        ReadHelper.listAnnotations(axiom, config, factory).forEachRemaining(x -> WithMerge.add(res, x));
        return res.values();
    }

    /**
     * Answers {@code true} if the given array contains {@link OWLAnnotation} at the end position.
     *
     * @param content an {@code Array}, not {@code null}
     * @return boolean
     */
    protected static boolean hasAnnotations(Object[] content) {
        return content[content.length - 1] instanceof OWLAnnotation;
    }

    @Override
    public InternalConfig getConfig() {
        return HasConfig.getConfig(model.get());
    }

    @Override
    public Stream<Triple> triples() {
        return Stream.concat(super.triples(), objects().flatMap(ONTObject::triples));
    }

    @SuppressWarnings("unchecked")
    @FactoryAccessor
    @Override
    public final X getAxiomWithoutAnnotations() {
        return createAnnotatedAxiom(createSet());
    }

    @SuppressWarnings("unchecked")
    @FactoryAccessor
    @Override
    public final <T extends OWLAxiom> T getAnnotatedAxiom(@Nonnull Stream<OWLAnnotation> annotations) {
        return (T) createAnnotatedAxiom(appendAnnotations(annotations.iterator()));
    }

    /**
     * Creates a fresh system-wide {@link X axiom}.
     *
     * @param annotations a {@code Collection} of {@link OWLAnnotation}s to append to the axiom
     * @return {@link X}
     */
    @FactoryAccessor
    protected abstract X createAnnotatedAxiom(Collection<OWLAnnotation> annotations);

    @FactoryAccessor
    @Override
    public OWLAxiom getNNF() {
        return accept(new NNF(getDataFactory()));
    }
}
