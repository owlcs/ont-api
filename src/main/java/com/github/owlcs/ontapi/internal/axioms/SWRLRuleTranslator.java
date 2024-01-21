/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2023, owl.cs group.
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

import com.github.owlcs.ontapi.config.AxiomsSettings;
import com.github.owlcs.ontapi.internal.AxiomTranslator;
import com.github.owlcs.ontapi.internal.InternalCache;
import com.github.owlcs.ontapi.internal.ModelObjectFactory;
import com.github.owlcs.ontapi.internal.ONTObject;
import com.github.owlcs.ontapi.internal.ONTObjectFactory;
import com.github.owlcs.ontapi.internal.ONTWrapperImpl;
import com.github.owlcs.ontapi.internal.WriteHelper;
import com.github.owlcs.ontapi.internal.objects.FactoryAccessor;
import com.github.owlcs.ontapi.internal.objects.ONTAxiomImpl;
import com.github.owlcs.ontapi.internal.objects.ONTObjectImpl;
import com.github.owlcs.ontapi.internal.objects.ONTStatementImpl;
import com.github.owlcs.ontapi.internal.objects.WithContent;
import com.github.owlcs.ontapi.owlapi.axioms.RuleImpl;
import com.github.sszuev.jena.ontapi.model.OntList;
import com.github.sszuev.jena.ontapi.model.OntModel;
import com.github.sszuev.jena.ontapi.model.OntObject;
import com.github.sszuev.jena.ontapi.model.OntSWRL;
import com.github.sszuev.jena.ontapi.model.OntStatement;
import com.github.sszuev.jena.ontapi.utils.Iterators;
import com.github.sszuev.jena.ontapi.utils.OntModels;
import org.apache.jena.graph.FrontsTriple;
import org.apache.jena.graph.Triple;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.HasComponents;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.SWRLAtom;
import org.semanticweb.owlapi.model.SWRLRule;
import org.semanticweb.owlapi.model.SWRLVariable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A translator that provides {@link SWRLRule} implementations.
 * All of them have type {@link AxiomType#SWRL_RULE}.
 * Specification: <a href="https://www.w3.org/Submission/SWRL/">SWRL: A Semantic Web Rule Language Combining OWL and RuleML</a>.
 * <p>
 * Created @ssz on 20.10.2016.
 */
public class SWRLRuleTranslator extends AxiomTranslator<SWRLRule> {

    @Override
    public void write(SWRLRule axiom, OntModel model) {
        Stream<OntSWRL.Atom<?>> head = axiom.head().map(atom -> WriteHelper.addSWRLAtom(model, atom));
        Stream<OntSWRL.Atom<?>> body = axiom.body().map(atom -> WriteHelper.addSWRLAtom(model, atom));
        WriteHelper.addAnnotations(model.createSWRLImp(head.collect(Collectors.toList()),
                body.collect(Collectors.toList())), axiom.annotationsAsList());
    }

    @Override
    public ExtendedIterator<OntStatement> listStatements(OntModel model, AxiomsSettings config) {
        return OntModels.listLocalObjects(model, OntSWRL.Imp.class).mapWith(OntObject::getMainStatement);
    }

    @Override
    public boolean testStatement(OntStatement statement, AxiomsSettings config) {
        return statement.getSubject().canAs(OntSWRL.Imp.class);
    }

    @Override
    public ONTObject<SWRLRule> toAxiomImpl(OntStatement statement,
                                           ModelObjectFactory factory,
                                           AxiomsSettings config) {
        return AxiomImpl.create(statement, factory, config);
    }

    @Override
    public ONTObject<SWRLRule> toAxiomWrap(OntStatement statement, ONTObjectFactory factory, AxiomsSettings config) {
        OntSWRL.Imp imp = statement.getSubject(OntSWRL.Imp.class);

        Collection<ONTObject<? extends SWRLAtom>> head = imp.head()
                .map(factory::getSWRLAtom).collect(Collectors.toList());
        Collection<ONTObject<? extends SWRLAtom>> body = imp.body()
                .map(factory::getSWRLAtom).collect(Collectors.toList());

        Collection<ONTObject<OWLAnnotation>> annotations = factory.getAnnotations(statement, config);
        SWRLRule res = factory.getOWLDataFactory()
                .getSWRLRule(body.stream().map(ONTObject::getOWLObject).collect(Collectors.toList()),
                        head.stream().map(ONTObject::getOWLObject)
                                .collect(Collectors.toList()), TranslateHelper.toSet(annotations));
        return ONTWrapperImpl.create(res, imp).append(annotations).append(body).append(head);
    }

    /**
     * @see RuleImpl
     */
    public static class AxiomImpl extends ONTAxiomImpl<SWRLRule> implements WithContent<AxiomImpl>, SWRLRule {
        protected final InternalCache.Loading<AxiomImpl, Object[]> content;

        public AxiomImpl(Triple t, Supplier<OntModel> m) {
            super(t, m);
            this.content = createContentCache();
        }

        /**
         * Creates an {@link ONTObject} container that is also {@link SWRLRule}.
         *
         * @param statement {@link OntStatement}, not {@code null}
         * @param factory   {@link ONTObjectFactory}, not {@code null}
         * @param config    {@link AxiomsSettings}, not {@code null}
         * @return {@link AxiomImpl}
         */
        @SuppressWarnings("unused")
        public static AxiomImpl create(OntStatement statement,
                                       ModelObjectFactory factory,
                                       AxiomsSettings config) {
            return new AxiomImpl(statement.asTriple(), factory.model());
        }

        @SuppressWarnings("rawtypes")
        private static Collection<ONTObject<? extends SWRLAtom>> collectAtoms(OntList<OntSWRL.Atom> list,
                                                                              ONTObjectFactory factory) {
            return Iterators.addAll(OntModels.listMembers(list).mapWith(factory::getSWRLAtom), new ArrayList<>());
        }

        @SuppressWarnings("unchecked")
        private static <X extends OWLObject> Stream<X> content(Class<X> type, Object[] content, int index) {
            Object[] arr = (Object[]) content[index];
            return Arrays.stream(arr).map(x -> type.isInstance(x) ? (X) x : ((ONTObject<? extends X>) x).getOWLObject());
        }

        @SuppressWarnings({"unchecked", "SameParameterValue", "rawtypes"})
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
            return asResource().getMainStatement();
        }

        @Override
        public Object[] collectContent() {
            OntSWRL.Imp imp = asResource();
            ONTObjectFactory factory = getObjectFactory();
            Collection<ONTObject<OWLAnnotation>> annotations = collectAnnotations(imp.getMainStatement(), factory, getConfig());
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

        @SuppressWarnings({"unchecked", "rawtypes"})
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
            return RuleImpl.variables(this);
        }

        @Override
        public boolean containsAnonymousClassExpressions() {
            return classAtomPredicates().anyMatch(OWLClassExpression::isAnonymous);
        }

        @Override
        public Stream<OWLClassExpression> classAtomPredicates() {
            return RuleImpl.classAtomPredicates(this);
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
