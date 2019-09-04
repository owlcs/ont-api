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

import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLObject;
import ru.avicomp.ontapi.internal.InternalConfig;
import ru.avicomp.ontapi.internal.InternalObjectFactory;
import ru.avicomp.ontapi.internal.ONTObject;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntStatement;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The base for simple axioms (usually for axioms that generate main triple).
 * Created by @szz on 04.09.2019.
 *
 * @param <X> - {@link OWLAxiom} subtype
 * @since 1.4.3
 */
public abstract class ONTSimpleAxiomImpl<X extends OWLAxiom> extends ONTAxiomImpl {

    protected ONTSimpleAxiomImpl(Object subject, String predicate, Object object, Supplier<OntGraphModel> m) {
        super(subject, predicate, object, m);
    }

    /**
     * Collects the cache for the given {@code axiom}, and returns the same object.
     *
     * @param axiom     {@link X}
     * @param statement {@link OntStatement}
     * @param factory   {@link InternalObjectFactory}
     * @param config    {@link InternalConfig}
     * @param <X>       subtype of {@link ONTSimpleAxiomImpl}
     * @return the same {@code axiom}
     */
    protected static <X extends ONTSimpleAxiomImpl> X collect(X axiom,
                                                              OntStatement statement,
                                                              InternalObjectFactory factory,
                                                              InternalConfig config) {
        axiom.content.put(axiom, axiom.collectContent(statement, config, factory));
        return axiom;
    }

    /**
     * Gets the number of semantic operands.
     * For axioms that generate main triple usually it equal {@code 2}.
     *
     * @return int
     */
    protected abstract int getOperandsNum();

    /**
     * Creates a fresh {@link X axiom}, that may not be from ONT-API model cache, but be rather system-wide.
     *
     * @param annotations a {@code Collection} of {@link OWLAnnotation}s to append to the axiom
     * @return {@link X}
     */
    protected abstract X createAnnotatedAxiom(Collection<OWLAnnotation> annotations);

    /**
     * Puts all the axiom's operands into the specified {@code List}.
     *
     * @param cache {@code List} to modify (add)
     * @param s     {@link OntStatement}, the source
     * @param f     {@link InternalObjectFactory}, to produce instances
     */
    protected abstract void collectOperands(List cache, OntStatement s, InternalObjectFactory f);

    @SuppressWarnings("unchecked")
    @Override
    protected final Object[] collectContent(OntStatement s, InternalConfig c, InternalObjectFactory f) {
        Set<ONTObject<OWLAnnotation>> annotations = collectAnnotations(s, c, f);
        List res = new ArrayList(annotations.isEmpty() ? getOperandsNum() : (getOperandsNum() + annotations.size()));
        collectOperands(res, s, f);
        if (!annotations.isEmpty()) {
            res.addAll(annotations);
        }
        return res.toArray();
    }

    @SuppressWarnings("unchecked")
    @Override
    public final X getAxiomWithoutAnnotations() {
        return createAnnotatedAxiom(Collections.emptySet());
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <T extends OWLAxiom> T getAnnotatedAxiom(@Nonnull Stream<OWLAnnotation> annotations) {
        return (T) createAnnotatedAxiom(appendAnnotations(annotations));
    }

    @SuppressWarnings("unchecked")
    @Override
    public final Stream<ONTObject<? extends OWLObject>> objects() {
        List res = Arrays.asList(getContent());
        return (Stream<ONTObject<? extends OWLObject>>) res.stream();
    }

    @SuppressWarnings("unchecked")
    public final Stream<OWLAnnotation> annotations() {
        List it = Arrays.asList(getContent());
        return (Stream<OWLAnnotation>) it.stream().skip(getOperandsNum());
    }

    @Override
    public final List<OWLAnnotation> annotationsAsList() {
        return annotations().collect(Collectors.toList());
    }

    @Override
    public final boolean isAnnotated() {
        return getContent().length > getOperandsNum();
    }
}
