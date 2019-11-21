/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2019, The University of Manchester, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.internal.axioms;

import com.github.owlcs.ontapi.internal.*;
import com.github.owlcs.ontapi.internal.objects.*;
import com.github.owlcs.ontapi.jena.model.*;
import com.github.owlcs.ontapi.jena.utils.Iter;
import com.github.owlcs.ontapi.jena.utils.OntModels;
import com.github.owlcs.ontapi.owlapi.axioms.SWRLRuleImpl;
import org.apache.jena.graph.FrontsTriple;
import org.apache.jena.graph.Triple;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.*;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A translator that provides {@link SWRLRule} implementations.
 * All of them have type {@link org.semanticweb.owlapi.model.AxiomType#SWRL_RULE}.
 * Specification: <a href='https://www.w3.org/Submission/SWRL/'>SWRL: A Semantic Web Rule Language Combining OWL and RuleML</a>.
 * <p>
 * Created by szuev on 20.10.2016.
 */
public class SWRLRuleTranslator extends AxiomTranslator<SWRLRule> {

    @Override
    public void write(SWRLRule axiom, OntGraphModel model) {
        Stream<OntSWRL.Atom> head = axiom.head().map(atom -> WriteHelper.addSWRLAtom(model, atom));
        Stream<OntSWRL.Atom> body = axiom.body().map(atom -> WriteHelper.addSWRLAtom(model, atom));
        WriteHelper.addAnnotations(model.createSWRLImp(head.collect(Collectors.toList()),
                body.collect(Collectors.toList())), axiom.annotationsAsList());
    }

    @Override
    public ExtendedIterator<OntStatement> listStatements(OntGraphModel model, InternalConfig config) {
        return OntModels.listLocalObjects(model, OntSWRL.Imp.class).mapWith(OntObject::getRoot);
    }

    @Override
    public boolean testStatement(OntStatement statement, InternalConfig config) {
        return statement.getSubject().canAs(OntSWRL.Imp.class);
    }

    @Override
    public ONTObject<SWRLRule> toAxiom(OntStatement statement,
                                       Supplier<OntGraphModel> model,
                                       InternalObjectFactory factory,
                                       InternalConfig config) {
        return AxiomImpl.create(statement, model, factory, config);
    }

    @Override
    public ONTObject<SWRLRule> toAxiom(OntStatement statement, InternalObjectFactory factory, InternalConfig config) {
        OntSWRL.Imp imp = statement.getSubject(OntSWRL.Imp.class);

        Collection<ONTObject<? extends SWRLAtom>> head = imp.head()
                .map(factory::getSWRLAtom).collect(Collectors.toList());
        Collection<ONTObject<? extends SWRLAtom>> body = imp.body()
                .map(factory::getSWRLAtom).collect(Collectors.toList());

        Collection<ONTObject<OWLAnnotation>> annotations = factory.getAnnotations(statement, config);
        SWRLRule res = factory.getOWLDataFactory()
                .getSWRLRule(body.stream().map(ONTObject::getOWLObject).collect(Collectors.toList()),
                        head.stream().map(ONTObject::getOWLObject)
                                .collect(Collectors.toList()), ONTObject.toSet(annotations));
        return ONTWrapperImpl.create(res, imp).append(annotations).append(body).append(head);
    }

    /**
     * @see SWRLRuleImpl
     */
    public static class AxiomImpl extends ONTAxiomImpl<SWRLRule> implements WithContent<AxiomImpl>, SWRLRule {
        protected final InternalCache.Loading<AxiomImpl, Object[]> content;

        public AxiomImpl(Triple t, Supplier<OntGraphModel> m) {
            super(t, m);
            this.content = createContentCache();
        }

        /**
         * Creates an {@link ONTObject} container that is also {@link SWRLRule}.
         *
         * @param statement {@link OntStatement}, not {@code null}
         * @param model     {@link OntGraphModel} provider, not {@code null}
         * @param factory   {@link InternalObjectFactory}, not {@code null}
         * @param config    {@link InternalConfig}, not {@code null}
         * @return {@link AxiomImpl}
         */
        @SuppressWarnings("unused")
        public static AxiomImpl create(OntStatement statement,
                                       Supplier<OntGraphModel> model,
                                       InternalObjectFactory factory,
                                       InternalConfig config) {
            return new AxiomImpl(statement.asTriple(), model);
        }

        private static Collection<ONTObject<? extends SWRLAtom>> collectAtoms(OntList<OntSWRL.Atom> list,
                                                                              InternalObjectFactory factory) {
            return Iter.addAll(OntModels.listMembers(list).mapWith(factory::getSWRLAtom), new ArrayList<>());
        }

        @SuppressWarnings("unchecked")
        private static <X extends OWLObject> Stream<X> content(Class<X> type, Object[] content, int index) {
            Object[] arr = (Object[]) content[index];
            return Arrays.stream(arr).map(x -> type.isInstance(x) ? (X) x : ((ONTObject<? extends X>) x).getOWLObject());
        }

        @SuppressWarnings({"unchecked", "SameParameterValue"})
        private static <X extends OWLObject> List<X> getContentAsList(Class<X> type, Object[] content, int index) {
            Object[] arr = (Object[]) content[index];
            if (arr.length == 0) {
                return Collections.emptyList();
            }
            if (type.isInstance(arr[0])) {
                List res = Arrays.asList(arr);
                return Collections.unmodifiableList(res);
            }
            return Arrays.stream(arr).map(x -> ((ONTObject<X>) x).getOWLObject()).collect(Collectors.toList());
        }

        @Override
        public InternalCache.Loading<AxiomImpl, Object[]> getContentCache() {
            return content;
        }

        public OntSWRL.Imp asResource() {
            return getPersonalityModel().getNodeAs(getSubjectNode(), OntSWRL.Imp.class);
        }

        @Override
        public OntStatement asStatement() {
            return asResource().getRoot();
        }

        @Override
        public Object[] collectContent() {
            OntSWRL.Imp imp = asResource();
            InternalObjectFactory factory = getObjectFactory();
            Collection<ONTObject<OWLAnnotation>> annotations = collectAnnotations(imp.getRoot(), factory, getConfig());
            Object[] res;
            if (annotations.isEmpty()) {
                res = new Object[2];
            } else {
                res = new Object[3];
                res[2] = annotations.toArray();
            }
            // may contain duplicates:
            res[0] = collectAtoms(imp.getHeadList(), factory).toArray();
            res[1] = collectAtoms(imp.getBodyList(), factory).toArray();
            return res;
        }

        @Override
        public Stream<Triple> triples() {
            return Stream.concat(asResource().spec().map(FrontsTriple::asTriple),
                    objects().flatMap(ONTObject::triples));
        }

        @SuppressWarnings("unchecked")
        @Override
        public Stream<ONTObject<? extends OWLObject>> objects() {
            Stream res = Arrays.stream(getContent()).flatMap(x -> Arrays.stream((Object[]) x));
            return (Stream<ONTObject<? extends OWLObject>>) res;
        }

        @Override
        public Stream<SWRLAtom> body() {
            return content(SWRLAtom.class, getContent(), 1).distinct();
        }

        @Override
        public Stream<SWRLAtom> head() {
            return content(SWRLAtom.class, getContent(), 0).distinct();
        }

        @Override
        public Stream<SWRLVariable> variables() {
            return SWRLRuleImpl.variables(this);
        }

        @Override
        public boolean containsAnonymousClassExpressions() {
            return classAtomPredicates().anyMatch(OWLClassExpression::isAnonymous);
        }

        @Override
        public Stream<OWLClassExpression> classAtomPredicates() {
            return SWRLRuleImpl.classAtomPredicates(this);
        }

        @Override
        public boolean isAnnotated() {
            return getContent().length == 3;
        }

        @Override
        public Stream<OWLAnnotation> annotations() {
            Object[] content = getContent();
            if (content.length == 3) {
                return content(OWLAnnotation.class, content, 2);
            }
            return Stream.empty();
        }

        @Override
        public List<OWLAnnotation> annotationsAsList() {
            Object[] content = getContent();
            if (content.length != 3) {
                return Collections.emptyList();
            }
            return getContentAsList(OWLAnnotation.class, content, 2);
        }

        @Override
        public boolean canContainAnnotationProperties() {
            return isAnnotated();
        }

        @FactoryAccessor
        @Override
        public SWRLRule getSimplified() {
            return eraseModel().getSimplified();
        }

        @FactoryAccessor
        @Override
        protected SWRLRule createAnnotatedAxiom(Collection<OWLAnnotation> annotations) {
            return getDataFactory().getSWRLRule(body().map(ONTObjectImpl::eraseModel).collect(Collectors.toList()),
                    head().map(ONTObjectImpl::eraseModel).collect(Collectors.toList()), annotations);
        }

        @Override
        protected boolean sameContent(ONTStatementImpl other) {
            return other instanceof AxiomImpl && Arrays.deepEquals(getContent(), ((AxiomImpl) other).getContent());
        }

        @Override
        protected boolean sameComponents(HasComponents other) {
            SWRLRule rule = (SWRLRule) other;
            return sameAnnotations(rule) && equalStreams(body(), rule.body()) && equalStreams(head(), rule.head());
        }

        private boolean sameAnnotations(OWLAxiom other) {
            if (isAnnotated()) {
                return other.isAnnotated() && annotationsAsList().equals(other.annotationsAsList());
            }
            return !other.isAnnotated();
        }

        @Override
        public ONTObject<SWRLRule> merge(ONTObject<SWRLRule> other) {
            if (this == other) {
                return this;
            }
            if (other instanceof AxiomImpl && sameTriple((AxiomImpl) other)) {
                return this;
            }
            AxiomImpl res = new AxiomImpl(asTriple(), model) {
                @Override
                public Stream<Triple> triples() {
                    return Stream.concat(AxiomImpl.this.triples(), other.triples());
                }
            };
            if (hasContent()) {
                res.putContent(getContent());
            }
            res.hashCode = hashCode;
            return res;
        }
    }
}
