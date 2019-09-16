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
import ru.avicomp.ontapi.internal.HasConfig;
import ru.avicomp.ontapi.internal.InternalCache;
import ru.avicomp.ontapi.internal.InternalConfig;
import ru.avicomp.ontapi.internal.ONTObject;
import ru.avicomp.ontapi.jena.model.OntAnnotation;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntStatement;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A base triple component that can be annotated.
 * Contains cache for object's components.
 * Created by @szz on 27.08.2019.
 *
 * @see ONTExpressionImpl
 * @see OntStatement
 * @since 1.4.3
 */
@SuppressWarnings("WeakerAccess")
public abstract class ONTStatementImpl extends ONTBaseTripleImpl
        implements ONTComposite, HasConfig, WithContent<ONTStatementImpl> {
    /**
     * A cache, an {@code Array} of content, the last element is reserved for annotations,
     * which are also presented as {@code Object[]}.
     */
    protected final InternalCache.Loading<ONTStatementImpl, Object[]> content;

    protected ONTStatementImpl(Object subject, String predicate, Object object, Supplier<OntGraphModel> m) {
        super(subject, predicate, object, m);
        this.content = createContent();
    }

    @Override
    public InternalConfig getConfig() {
        return HasConfig.getConfig(model.get());
    }

    @Override
    public abstract Object[] collectContent();

    /**
     * Gets the number of semantic operands.
     * For axioms that generate main triple usually it equal {@code 2}.
     * For n-ary axioms with unknown predefined number of operands the result is {@code -1}.
     *
     * @return a positive int or {@code -1}
     */
    protected abstract int getOperandsNum();

    @Override
    public final int initHashCode() {
        return collectHashCode(getContent());
    }

    /**
     * Calculates the {@code hashCode}.
     * Note: must be overridden for n-ary axioms.
     *
     * @param content an {@code Array} with the content
     * @return hash code for this axiom
     */
    protected int collectHashCode(Object[] content) {
        int res = hashIndex();
        int n = getOperandsNum();
        for (int i = 0; i < n; i++) {
            res = OWLObject.hashIteration(res, content[i].hashCode());
        }
        return OWLObject.hashIteration(res, n == content.length ? 1 : Arrays.hashCode((Object[]) content[n]));
    }

    @Override
    public Object[] getContent() {
        return content.get(this);
    }

    @Override
    public void putContent(Object[] content) {
        this.content.put(this, content);
    }

    @Override
    public boolean hasContent() {
        return !content.isEmpty();
    }

    @Override
    public void clearContent() {
        content.clear();
    }

    @Override
    public boolean isAnnotated() {
        return getContent().length > getOperandsNum();
    }

    @SuppressWarnings("unchecked")
    public Stream<OWLAnnotation> annotations() {
        Object[] content = getContent();
        int n = getOperandsNum();
        if (content.length == n) {
            return Stream.empty();
        }
        List res = Arrays.asList((Object[]) content[n]);
        return (Stream<OWLAnnotation>) res.stream();
    }

    @SuppressWarnings("unchecked")
    public List<OWLAnnotation> annotationsAsList() {
        Object[] content = getContent();
        int n = getOperandsNum();
        if (content.length == n) {
            return Collections.emptyList();
        }
        List res = Arrays.asList((Object[]) content[n]);
        return (List<OWLAnnotation>) Collections.unmodifiableList(res);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Stream<ONTObject<? extends OWLObject>> objects() {
        Object[] content = getContent();
        Stream res = Arrays.stream(content);
        int n = getOperandsNum();
        if (content.length == n) {
            return (Stream<ONTObject<? extends OWLObject>>) res;
        }
        return (Stream<ONTObject<? extends OWLObject>>) Stream.concat(res.limit(n),
                Arrays.stream(((Object[]) content[n])));
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
    protected boolean sameContent(ONTBaseTripleImpl other) {
        // assuming all the rest info is keeping in the content only:
        return other instanceof ONTStatementImpl && Arrays.equals(getContent(), ((ONTStatementImpl) other).getContent());
    }

}
