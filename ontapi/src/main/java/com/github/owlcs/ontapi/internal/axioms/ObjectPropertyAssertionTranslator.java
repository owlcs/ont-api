/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2022, owl.cs group.
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

import com.github.owlcs.ontapi.DataFactory;
import com.github.owlcs.ontapi.config.AxiomsSettings;
import com.github.owlcs.ontapi.internal.*;
import com.github.owlcs.ontapi.internal.objects.FactoryAccessor;
import com.github.owlcs.ontapi.internal.objects.ONTEntityImpl;
import com.github.owlcs.ontapi.jena.model.OntIndividual;
import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.jena.model.OntObjectProperty;
import com.github.owlcs.ontapi.jena.model.OntStatement;
import org.apache.jena.graph.BlankNodeId;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.*;

import java.util.Collection;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * A translator that provides {@link OWLObjectPropertyAssertionAxiom} implementations.
 * <p>
 * The pattern is {@code a1 PN a2}, where:
 * <ul>
 * <li>{@code a1} and {@code a2} - individuals (named or anonymous)</li>
 * <li>{@code PN} - named object property expression</li>
 * </ul>
 * Example:
 * <pre>{@code
 * gr:Saturday rdf:type owl:NamedIndividual , gr:hasNext gr:Sunday ;
 * }</pre>
 * <p>
 * Created by @szuev on 01.10.2016.
 */
public class ObjectPropertyAssertionTranslator
        extends AbstractPropertyAssertionTranslator<OWLObjectPropertyExpression, OWLObjectPropertyAssertionAxiom> {

    /**
     * Note: ObjectPropertyAssertion(ObjectInverseOf(P) S O) = ObjectPropertyAssertion(P O S)
     *
     * @param axiom {@link OWLObjectPropertyAssertionAxiom}
     * @param model {@link OntModel}
     */
    @Override
    public void write(OWLObjectPropertyAssertionAxiom axiom, OntModel model) {
        OWLIndividual s = axiom.getSubject();
        OWLObjectPropertyExpression p = axiom.getProperty();
        OWLIndividual o = axiom.getObject();
        WriteHelper.writeAssertionTriple(model,
                p.isAnonymous() ? o : s,
                p.isAnonymous() ? p.getInverseProperty() : p,
                p.isAnonymous() ? s : o,
                axiom.annotationsAsList());
    }

    /**
     * Lists positive object property assertion: {@code a1 PN a2}.
     * See <a href="https://www.w3.org/TR/owl2-quick-reference/">Assertions</a>
     *
     * @param model  {@link OntModel} the model
     * @param config {@link AxiomsSettings}
     * @return {@link ExtendedIterator} of {@link OntStatement}s
     */
    @Override
    public ExtendedIterator<OntStatement> listStatements(OntModel model, AxiomsSettings config) {
        return listStatements(model).filterKeep(s -> testStatement(s, config));
    }

    @Override
    public boolean testStatement(OntStatement statement, AxiomsSettings config) {
        return statement.getPredicate().canAs(OntObjectProperty.Named.class)
                && statement.getSubject().canAs(OntIndividual.class)
                && statement.getObject().canAs(OntIndividual.class);
    }

    @Override
    public ONTObject<OWLObjectPropertyAssertionAxiom> toAxiomImpl(OntStatement statement,
                                                                  ModelObjectFactory factory,
                                                                  AxiomsSettings config) {
        return AxiomImpl.create(statement, factory, config);
    }

    @Override
    public ONTObject<OWLObjectPropertyAssertionAxiom> toAxiomWrap(OntStatement statement,
                                                                  ONTObjectFactory factory,
                                                                  AxiomsSettings config) {
        ONTObject<? extends OWLIndividual> subject = factory.getIndividual(statement.getSubject(OntIndividual.class));
        ONTObject<? extends OWLObjectPropertyExpression> property = factory.getProperty(statement.getPredicate()
                .as(OntObjectProperty.class));
        ONTObject<? extends OWLIndividual> object = factory.getIndividual(statement.getObject(OntIndividual.class));
        Collection<ONTObject<OWLAnnotation>> annotations = factory.getAnnotations(statement, config);
        OWLObjectPropertyAssertionAxiom res = factory.getOWLDataFactory()
                .getOWLObjectPropertyAssertionAxiom(property.getOWLObject(), subject.getOWLObject(),
                        object.getOWLObject(), TranslateHelper.toSet(annotations));
        return ONTWrapperImpl.create(res, statement).append(annotations)
                .append(subject).append(property).append(object);
    }

    @Override
    Triple createSearchTriple(OWLObjectPropertyAssertionAxiom axiom) {
        Node subject = TranslateHelper.getSearchNode(axiom.getSubject());
        if (subject == null) return null;
        Node object = TranslateHelper.getSearchNode(axiom.getObject());
        if (object == null) return null;
        Node property = WriteHelper.toNode(axiom.getProperty().asOWLObjectProperty());
        return Triple.create(subject, property, object);
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.axioms.ObjectPropertyAssertionAxiomImpl
     */
    @SuppressWarnings("WeakerAccess")
    public static abstract class AxiomImpl
            extends AssertionImpl<OWLObjectPropertyAssertionAxiom,
            OWLIndividual, OWLObjectPropertyExpression, OWLIndividual>
            implements OWLObjectPropertyAssertionAxiom {

        protected AxiomImpl(Triple t, Supplier<OntModel> m) {
            super(t, m);
        }

        /**
         * Creates an {@link OWLObjectPropertyAssertionAxiom} that is also {@link ONTObject}.
         *
         * @param statement {@link OntStatement}, the source, not {@code null}
         * @param factory   {@link ONTObjectFactory}, not {@code null}
         * @param config    {@link AxiomsSettings}, not {@code null}
         * @return {@link AxiomImpl}
         */
        public static AxiomImpl create(OntStatement statement,
                                       ModelObjectFactory factory,
                                       AxiomsSettings config) {
            return WithAssertion.create(statement,
                    SimpleImpl.FACTORY, WithAnnotationsImpl.FACTORY, SET_HASH_CODE, factory, config);
        }

        @Override
        public boolean isInSimplifiedForm() {
            return true;
        }

        @Override
        public ONTObject<? extends OWLIndividual> findONTSubject(ModelObjectFactory factory) {
            return findByURIOrBlankId(subject, factory);
        }

        @Override
        public ONTObject<? extends OWLIndividual> findONTObject(ModelObjectFactory factory) {
            return findByURIOrBlankId(object, factory);
        }

        @Override
        public ONTObject<? extends OWLObjectPropertyExpression> findONTPredicate(ModelObjectFactory factory) {
            return findONTProperty(factory);
        }

        public ONTObject<OWLObjectProperty> findONTProperty(ModelObjectFactory factory) {
            return factory.getObjectProperty(predicate);
        }

        @FactoryAccessor
        @Override
        public OWLObjectPropertyAssertionAxiom getSimplified() {
            return eraseModel();
        }

        @FactoryAccessor
        @Override
        protected OWLObjectPropertyAssertionAxiom createAnnotatedAxiom(Collection<OWLAnnotation> annotations) {
            return getDataFactory().getOWLObjectPropertyAssertionAxiom(getFPredicate(), getFSubject(),
                    getFObject(), annotations);
        }

        @FactoryAccessor
        @Override
        public OWLSubClassOfAxiom asOWLSubClassOfAxiom() {
            DataFactory df = getDataFactory();
            return df.getOWLSubClassOfAxiom(df.getOWLObjectOneOf(getFSubject()),
                    df.getOWLObjectHasValue(getFPredicate(), getFObject()));
        }

        @Override
        public boolean canContainAnnotationProperties() {
            return isAnnotated();
        }

        @Override
        public boolean canContainDatatypes() {
            return isAnnotated();
        }

        @Override
        public boolean canContainAnonymousIndividuals() {
            return isAnnotated();
        }

        @Override
        public boolean canContainDataProperties() {
            return false;
        }

        @Override
        public Set<OWLObjectProperty> getObjectPropertySet() {
            return createSet(getProperty().asOWLObjectProperty());
        }

        @Override
        public boolean containsObjectProperty(OWLObjectProperty property) {
            return predicate.equals(ONTEntityImpl.getURI(property));
        }

        @Override
        public Set<OWLNamedIndividual> getNamedIndividualSet() {
            Set<OWLNamedIndividual> res = createSortedSet();
            collectNamedIndividuals(res, null);
            return res;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        protected void collectNamedIndividuals(Set res, ModelObjectFactory factory) {
            if (subject instanceof String) {
                res.add((factory == null ? factory = getObjectFactory() : factory).getNamedIndividual((String) subject).getOWLObject());
            }
            if (object instanceof String) {
                res.add((factory == null ? getObjectFactory() : factory).getNamedIndividual((String) object).getOWLObject());
            }
        }

        /**
         * An {@link OWLObjectPropertyAssertionAxiom} that has no sub-annotations.
         */
        public static class SimpleImpl extends AxiomImpl
                implements Simple<OWLIndividual, OWLObjectPropertyExpression, OWLIndividual> {

            private static final BiFunction<Triple, Supplier<OntModel>, SimpleImpl> FACTORY = SimpleImpl::new;

            protected SimpleImpl(Triple t, Supplier<OntModel> m) {
                super(t, m);
            }

            @Override
            public Set<OWLAnonymousIndividual> getAnonymousIndividualSet() {
                Set<OWLAnonymousIndividual> res = createSortedSet();
                ModelObjectFactory f = null;
                if (subject instanceof BlankNodeId) {
                    res.add((f = getObjectFactory()).getAnonymousIndividual((BlankNodeId) subject).getOWLObject());
                }
                if (object instanceof BlankNodeId) {
                    res.add((f == null ? getObjectFactory() : f).getAnonymousIndividual((BlankNodeId) object).getOWLObject());
                }
                return res;
            }

            @Override
            public Set<OWLEntity> getSignatureSet() {
                Set<OWLEntity> res = createSortedSet();
                ModelObjectFactory factory = getObjectFactory();
                res.add(findONTProperty(factory).getOWLObject());
                collectNamedIndividuals(res, factory);
                return res;
            }

            @Override
            public boolean containsNamedIndividual(OWLNamedIndividual individual) {
                String uri = null;
                return hasURISubject() && getSubjectURI().equals(uri = ONTEntityImpl.getURI(individual)) ||
                        hasURIObject() && getObjectURI().equals(uri == null ? ONTEntityImpl.getURI(individual) : uri);
            }
        }

        /**
         * An {@link OWLObjectPropertyAssertionAxiom} that has sub-annotations.
         * This class has a public constructor since it is more generic then {@link SimpleImpl}.
         */
        public static class WithAnnotationsImpl extends AxiomImpl
                implements WithAnnotations<WithAnnotationsImpl, OWLIndividual, OWLObjectPropertyExpression, OWLIndividual> {

            private static final BiFunction<Triple, Supplier<OntModel>, WithAnnotationsImpl> FACTORY =
                    WithAnnotationsImpl::new;
            protected final InternalCache.Loading<WithAnnotationsImpl, Object[]> content;

            public WithAnnotationsImpl(Triple t, Supplier<OntModel> m) {
                super(t, m);
                this.content = createContentCache();
            }

            @Override
            public InternalCache.Loading<WithAnnotationsImpl, Object[]> getContentCache() {
                return content;
            }
        }
    }
}
