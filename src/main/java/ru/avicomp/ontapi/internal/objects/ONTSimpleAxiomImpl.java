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

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * The base for simple axioms (usually for axioms that generate main triple).
 * It has fixed number of operands.
 * Created by @szz on 04.09.2019.
 *
 * @param <X> - {@link OWLAxiom} subtype
 * @since 1.4.3
 */
public abstract class ONTSimpleAxiomImpl<X extends OWLAxiom> extends ONTAxiomImpl<X> {

    protected ONTSimpleAxiomImpl(Object subject, String predicate, Object object, Supplier<OntGraphModel> m) {
        super(subject, predicate, object, m);
    }

    /**
     * Puts all the axiom's operands into the specified {@code List}.
     *
     * @param cache     {@code Array} to modify
     * @param statement {@link OntStatement}, the source
     * @param factory   {@link InternalObjectFactory}, to produce instances
     */
    protected abstract void collectOperands(Object[] cache,
                                            OntStatement statement,
                                            InternalObjectFactory factory);

    /**
     * Collects the cache.
     *
     * @param s {@link OntStatement}, the statement, not {@code null}
     * @param c {@link InternalConfig}, the config, not {@code null}
     * @param f {@link InternalObjectFactory}, the factory, not {@code null}
     * @return Array of {@code Object}s
     */
    protected final Object[] collectContent(OntStatement s, InternalConfig c, InternalObjectFactory f) {

        Object[] res;
        Object[] annotations = collectAnnotations(s, c, f);
        int n = getOperandsNum();
        if (annotations.length > 0) {
            res = new Object[n + 1];
            res[n] = annotations;
        } else {
            res = new Object[n];
        }
        collectOperands(res, s, f);
        return res;
    }

    @Override
    protected final int collectHashCode(Object[] content) {
        return super.collectHashCode(content);
    }

    @Override
    public final Stream<OWLAnnotation> annotations() {
        return super.annotations();
    }

    @Override
    public final List<OWLAnnotation> annotationsAsList() {
        return super.annotationsAsList();
    }

    @Override
    public final Stream<ONTObject<? extends OWLObject>> objects() {
        return super.objects();
    }
}
