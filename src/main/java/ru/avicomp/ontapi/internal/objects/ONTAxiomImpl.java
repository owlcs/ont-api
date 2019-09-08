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
import org.semanticweb.owlapi.util.NNF;
import ru.avicomp.ontapi.internal.InternalConfig;
import ru.avicomp.ontapi.internal.InternalObjectFactory;
import ru.avicomp.ontapi.internal.ONTObject;
import ru.avicomp.ontapi.internal.ReadHelper;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntObject;
import ru.avicomp.ontapi.jena.model.OntStatement;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A base axiom.
 * Created by @ssz on 01.09.2019.
 *
 * @param <X> - the {@link OWLAxiom} subtype, must be the same that this class implements
 * @see ru.avicomp.ontapi.owlapi.axioms.OWLAxiomImpl
 * @since 1.4.3
 */
@SuppressWarnings("WeakerAccess")
public abstract class ONTAxiomImpl<X extends OWLAxiom> extends ONTStatementImpl implements OWLAxiom {

    protected ONTAxiomImpl(Object subject, String predicate, Object object, Supplier<OntGraphModel> m) {
        super(subject, predicate, object, m);
    }

    /**
     * Collects the cache for the given {@code axiom}, and returns the same object.
     * This method is an optimization hack:
     * we know the statement, so we can get all info from it, before leave it forgotten and available for GC.
     *
     * @param axiom     {@link X}
     * @param statement {@link OntStatement}
     * @param factory   {@link InternalObjectFactory}
     * @param config    {@link InternalConfig}
     * @param <X>       subtype of {@link ONTAxiomImpl}
     * @return the same {@code axiom}
     */
    protected static <X extends ONTAxiomImpl> X init(X axiom,
                                                     OntStatement statement,
                                                     InternalObjectFactory factory,
                                                     InternalConfig config) {
        Object[] content = axiom.collectContent(statement, config, factory);
        axiom.content.put(axiom, content);
        axiom.hashCode = axiom.collectHashCode(content);
        return axiom;
    }

    @Override
    protected final Object[] collectContent() {
        return collectContent(asStatement(), getConfig(), getObjectFactory());
    }

    /**
     * Collects the cache.
     *
     * @param s {@link OntStatement}, the statement, not {@code null}
     * @param c {@link InternalConfig}, the config, not {@code null}
     * @param f {@link InternalObjectFactory}, the factory, not {@code null}
     * @return Array of {@code Object}s
     * @see ONTExpressionImpl#collectContent(OntObject, InternalObjectFactory)
     * @see ONTAnnotationImpl#collectContent(OntStatement, InternalObjectFactory)
     */
    protected abstract Object[] collectContent(OntStatement s, InternalConfig c, InternalObjectFactory f);

    /**
     * Collects all annotations as Array.
     *
     * @param statement {@link OntStatement}, not {@code null}
     * @param config    {@link InternalConfig}, not {@code null}
     * @param factory   {@link InternalObjectFactory}, not {@code null}
     * @return an {@code Array} with all annotations
     */
    protected Object[] collectAnnotations(OntStatement statement,
                                          InternalConfig config,
                                          InternalObjectFactory factory) {
        Set<ONTObject<OWLAnnotation>> res = createObjectSet();
        ReadHelper.listAnnotations(statement, config, factory).forEachRemaining(res::add);
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
        return (T) createAnnotatedAxiom(appendAnnotations(annotations.iterator()));
    }

    /**
     * Creates a fresh {@link X axiom}, that may not be from ONT-API model cache, but be rather system-wide.
     *
     * @param annotations a {@code Collection} of {@link OWLAnnotation}s to append to the axiom
     * @return {@link X}
     */
    protected abstract X createAnnotatedAxiom(Collection<OWLAnnotation> annotations);

    @Override
    public OWLAxiom getNNF() {
        return accept(new NNF(getDataFactory()));
    }

}
