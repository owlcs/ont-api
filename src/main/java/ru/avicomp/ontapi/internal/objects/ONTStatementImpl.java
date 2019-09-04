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

import org.apache.jena.graph.FrontsTriple;
import org.apache.jena.graph.Triple;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLObject;
import ru.avicomp.ontapi.internal.InternalCache;
import ru.avicomp.ontapi.internal.ONTObject;
import ru.avicomp.ontapi.jena.model.OntAnnotation;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntStatement;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A base triple component that can be annotated.
 * Contains cache for object's components.
 * Created by @szz on 27.08.2019.
 *
 * @see ONTExpressionImpl
 * @since 1.4.3
 */
@SuppressWarnings("WeakerAccess")
public abstract class ONTStatementImpl extends ONTBaseTripleImpl implements ONTComposite {

    protected final InternalCache.Loading<ONTStatementImpl, Object[]> content;

    protected ONTStatementImpl(Object subject, String predicate, Object object, Supplier<OntGraphModel> m) {
        super(subject, predicate, object, m);
        this.content = InternalCache.createSoftSingleton(x -> collectContent());
    }

    /**
     * Lists all {@link OWLAnnotation}s on this object.
     * The stream is {@link Spliterator#ORDERED}, {@link Spliterator#NONNULL} and {@link Spliterator#SORTED}.
     *
     * @return a {@code Stream} of {@link OWLAnnotation}s
     */
    public abstract Stream<OWLAnnotation> annotations();

    /**
     * Collects the cache.
     *
     * @return {@code Array} of {@code Object}s
     * @see ONTExpressionImpl#collectContent()
     */
    protected abstract Object[] collectContent();

    /**
     * Answers {@code true} if this annotation has sub-annotations.
     *
     * @return boolean
     * @see org.semanticweb.owlapi.model.OWLAxiom#isAnnotated()
     */
    public boolean isAnnotated() {
        return annotations().findFirst().isPresent();
    }

    /**
     * Answers a sorted {@code List} of {@link OWLAnnotation}s on this object.
     *
     * @return a {@code List} of {@link OWLAnnotation}s
     * @see org.semanticweb.owlapi.model.HasAnnotations#annotationsAsList()
     */
    public List<OWLAnnotation> annotationsAsList() {
        return annotations().collect(Collectors.toList());
    }

    /**
     * Gets the content from cache.
     *
     * @return {@code Array} of {@code Object}s
     * @see ONTExpressionImpl#getContent()
     */
    protected Object[] getContent() {
        return content.get(this);
    }

    @Override
    public Stream<Triple> triples() {
        OntStatement root = asStatement();
        Stream<Triple> res = Stream.concat(Stream.of(root.asTriple()), objects().flatMap(ONTObject::triples));
        OntAnnotation a = root.getSubject().getAs(OntAnnotation.class);
        if (a != null) {
            res = Stream.concat(res, a.spec().map(FrontsTriple::asTriple));
        }
        return res;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof OWLObject)) {
            return false;
        }
        OWLObject other = (OWLObject) obj;
        if (typeIndex() != other.typeIndex()) {
            return false;
        }
        if (other instanceof ONTStatementImpl) {
            ONTStatementImpl t = (ONTStatementImpl) other;
            if (notSame(t)) {
                return false;
            }
            if (sameAs(t)) {
                return true;
            }
            // assuming all the rest info is keeping in the content only:
            return Arrays.equals(getContent(), t.getContent());
        }
        // then OWL-API instance is given
        if (hashCode() != other.hashCode()) {
            return false;
        }
        return equalIterators(components().iterator(), other.components().iterator());
    }
}
