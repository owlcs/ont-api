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
package ru.avicomp.ontapi.owlapi.axioms;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.SWRLVariableExtractor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Matthew Horridge, The University Of Manchester, Bio-Health Informatics Group
 * @since 1.2.0
 */
public class SWRLRuleImpl extends OWLLogicalAxiomImpl implements SWRLRule {

    private static final AtomSimplifier ATOM_SIMPLIFIER = new AtomSimplifier();
    private final LinkedHashSet<SWRLAtom> head;
    private final LinkedHashSet<SWRLAtom> body;
    private final boolean containsAnonymousClassExpressions;

    /**
     * @param body        rule body
     * @param head        rule head
     * @param annotations annotations on the axiom
     */
    public SWRLRuleImpl(Collection<? extends SWRLAtom> body, Collection<? extends SWRLAtom> head,
                        Collection<OWLAnnotation> annotations) {
        super(annotations);
        this.head = new LinkedHashSet<>(Objects.requireNonNull(head, "head cannot be null"));
        this.body = new LinkedHashSet<>(Objects.requireNonNull(body, "body cannot be null"));
        containsAnonymousClassExpressions = hasAnon();
    }

    /**
     * @param body rule body
     * @param head rule head
     */
    public SWRLRuleImpl(Collection<? extends SWRLAtom> body, Collection<? extends SWRLAtom> head) {
        this(body, head, NO_ANNOTATIONS);
    }

    @SuppressWarnings("unchecked")
    @Override
    public SWRLRuleImpl getAxiomWithoutAnnotations() {
        if (!isAnnotated()) {
            return this;
        }
        return new SWRLRuleImpl(body, head, NO_ANNOTATIONS);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends OWLAxiom> T getAnnotatedAxiom(@Nonnull Stream<OWLAnnotation> anns) {
        return (T) new SWRLRuleImpl(body, head, mergeAnnos(anns));
    }

    @Override
    public Stream<SWRLVariable> variables() {
        return accept(new SWRLVariableExtractor()).stream();
    }

    private boolean hasAnon() {
        return classAtomPredicates().anyMatch(OWLClassExpression::isAnonymous);
    }

    @Override
    public boolean containsAnonymousClassExpressions() {
        return containsAnonymousClassExpressions;
    }

    @Override
    public Stream<OWLClassExpression> classAtomPredicates() {
        return Stream.concat(head(), body()).filter(c -> c instanceof SWRLClassAtom)
                .map(c -> ((SWRLClassAtom) c).getPredicate()).distinct().sorted();
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
        return new ArrayList<>(body);
    }

    @Override
    public List<SWRLAtom> headList() {
        return new ArrayList<>(head);
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
        // For same implementation instances, no need to create or sort sets
        if (obj instanceof SWRLRuleImpl) {
            SWRLRuleImpl other = (SWRLRuleImpl) obj;
            return body.equals(other.body) && head.equals(other.head)
                    && annotations.equals(other.annotationsAsList());
        }
        // For different implementations, just use sets, do not sort
        SWRLRule other = (SWRLRule) obj;
        return body.equals(other.body().collect(Collectors.toCollection(LinkedHashSet::new)))
                && head.equals(other.head().collect(Collectors.toCollection(LinkedHashSet::new)))
                && annotations.equals(other.annotationsAsList());
    }

    protected static class AtomSimplifier implements SWRLObjectVisitorEx<SWRLObject> {

        @Override
        public SWRLObject doDefault(Object o) {
            return (SWRLObject) o;
        }

        @Override
        public SWRLRule visit(@Nonnull SWRLRule node) {
            List<SWRLAtom> nodebody = node.body().map(a -> (SWRLAtom) a.accept(this)).collect(Collectors.toList());
            List<SWRLAtom> nodehead = node.head().map(a -> (SWRLAtom) a.accept(this)).collect(Collectors.toList());
            return new SWRLRuleImpl(nodebody, nodehead, NO_ANNOTATIONS);
        }

        @Override
        public SWRLObjectPropertyAtom visit(@Nonnull SWRLObjectPropertyAtom node) {
            return node.getSimplified();
        }
    }
}
