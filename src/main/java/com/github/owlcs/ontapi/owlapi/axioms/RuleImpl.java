/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2021, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.github.owlcs.ontapi.owlapi.axioms;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.SWRLVariableExtractor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@SuppressWarnings("WeakerAccess")
public class RuleImpl extends LogicalAxiomImpl implements SWRLRule {

    private static final AtomSimplifier ATOM_SIMPLIFIER = new AtomSimplifier();
    private final List<SWRLAtom> head;
    private final List<SWRLAtom> body;
    private final boolean containsAnonymousClassExpressions;

    /**
     * @param body        a {@code Collection} of {@link SWRLAtom}s, the rule body
     * @param head        a {@code Collection} of {@link SWRLAtom}s, the rule head
     * @param annotations a {@code Collection} of {@link OWLAnnotation}s on the axiom
     */
    public RuleImpl(Collection<? extends SWRLAtom> body,
                    Collection<? extends SWRLAtom> head,
                    Collection<OWLAnnotation> annotations) {
        super(annotations);
        this.head = toDistinctList(Objects.requireNonNull(head, "head cannot be null"));
        this.body = toDistinctList(Objects.requireNonNull(body, "body cannot be null"));
        this.containsAnonymousClassExpressions = classAtomPredicates().anyMatch(OWLClassExpression::isAnonymous);
    }

    /**
     * Makes a {@code List} without duplicates (and nulls) from the given collection.
     * Both rule's head and body have the same order as specified, but must be distinct at the same time.
     *
     * @param collection {@code Collection}, not {@code null}
     * @param <X>        anything
     * @return a {@code List} of {@link X}s
     */
    private static <X> List<X> toDistinctList(Collection<? extends X> collection) {
        return toDistinctList(collection.stream());
    }

    /**
     * Collects the given {@code stream} into a {@code List} without duplicates (and nulls).
     *
     * @param stream {@code Stream}, not {@code null}
     * @param <X>    anything
     * @return a {@code List} of {@link X}s
     */
    private static <X> List<X> toDistinctList(Stream<? extends X> stream) {
        return stream.map(Objects::requireNonNull).distinct().collect(Collectors.toUnmodifiableList());
    }

    /**
     * Lists class expressions that are predicates of class atoms.
     *
     * @param rule {@link SWRLRule}, not {@code null}
     * @return a {@code Stream} of {@link OWLClassExpression}s
     */
    public static Stream<OWLClassExpression> classAtomPredicates(SWRLRule rule) {
        return forOutput(Stream.concat(rule.head(), rule.body())
                .filter(x -> x instanceof SWRLClassAtom)
                .map(c -> ((SWRLClassAtom) c).getPredicate()));
    }

    /**
     * Gets rule's body as {@code List}.
     *
     * @param rule {@link SWRLRule}, not {@code null}
     * @return a {@link List} of {@link SWRLAtom}s
     */
    public static List<SWRLAtom> getBodyList(SWRLRule rule) {
        if (rule instanceof RuleImpl) {
            return rule.bodyList();
        }
        return toDistinctList(rule.body());
    }

    /**
     * Gets rule's head as {@code List}.
     *
     * @param rule {@link SWRLRule}, not {@code null}
     * @return a {@link List} of {@link SWRLAtom}s
     */
    public static List<SWRLAtom> getHeadList(SWRLRule rule) {
        if (rule instanceof RuleImpl) {
            return rule.headList();
        }
        return toDistinctList(rule.head().distinct());
    }

    /**
     * Lists all variables that appear in the given rule.
     *
     * @param rule {@link SWRLRule}, not {@code null}
     * @return a {@code Stream} of {@link SWRLVariable}s
     */
    public static Stream<SWRLVariable> variables(SWRLRule rule) {
        return rule.accept(new SWRLVariableExtractor()).stream();
    }

    @SuppressWarnings("unchecked")
    @Override
    public RuleImpl getAxiomWithoutAnnotations() {
        if (!isAnnotated()) {
            return this;
        }
        return new RuleImpl(body, head, NO_ANNOTATIONS);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends OWLAxiom> T getAnnotatedAxiom(@Nonnull Stream<OWLAnnotation> annotations) {
        return (T) new RuleImpl(body, head, mergeAnnotations(this, annotations));
    }

    @Override
    public Stream<SWRLVariable> variables() {
        return variables(this);
    }

    @Override
    public boolean containsAnonymousClassExpressions() {
        return containsAnonymousClassExpressions;
    }

    @Override
    public Stream<OWLClassExpression> classAtomPredicates() {
        return classAtomPredicates(this);
    }

    @Override
    public Stream<SWRLAtom> body() {
        return body.stream();
    }

    @Override
    public Stream<SWRLAtom> head() {
        return head.stream();
    }

    @Override
    public List<SWRLAtom> bodyList() {
        return body;
    }

    @Override
    public List<SWRLAtom> headList() {
        return head;
    }

    @Override
    public SWRLRule getSimplified() {
        return (SWRLRule) accept(ATOM_SIMPLIFIER);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof SWRLRule)) {
            return false;
        }
        SWRLRule other = (SWRLRule) obj;
        return annotations.equals(other.annotationsAsList())
                && body.equals(getBodyList(other)) && head.equals(getHeadList(other));
    }

    protected static class AtomSimplifier implements SWRLObjectVisitorEx<SWRLObject> {

        @Override
        public SWRLObject doDefault(@Nonnull Object o) {
            return (SWRLObject) o;
        }

        @Override
        public SWRLRule visit(@Nonnull SWRLRule node) {
            List<SWRLAtom> body = node.body().map(a -> (SWRLAtom) a.accept(this)).collect(Collectors.toList());
            List<SWRLAtom> head = node.head().map(a -> (SWRLAtom) a.accept(this)).collect(Collectors.toList());
            return new RuleImpl(body, head, NO_ANNOTATIONS);
        }

        @Override
        public SWRLObjectPropertyAtom visit(@Nonnull SWRLObjectPropertyAtom node) {
            return node.getSimplified();
        }
    }
}
