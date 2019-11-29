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

import com.github.owlcs.ontapi.DataFactory;
import com.github.owlcs.ontapi.internal.*;
import com.github.owlcs.ontapi.internal.objects.FactoryAccessor;
import com.github.owlcs.ontapi.internal.objects.ONTEntityImpl;
import com.github.owlcs.ontapi.internal.objects.ONTObjectPropertyImpl;
import com.github.owlcs.ontapi.jena.model.OntGraphModel;
import com.github.owlcs.ontapi.jena.model.OntIndividual;
import com.github.owlcs.ontapi.jena.model.OntOPE;
import com.github.owlcs.ontapi.jena.model.OntStatement;
import org.apache.jena.graph.BlankNodeId;
import org.apache.jena.graph.Triple;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.*;

import java.util.Collection;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * A translator that provides {@link OWLObjectPropertyAssertionAxiom} implementations.
 * Example:
 * <pre>{@code
 * gr:Saturday rdf:type owl:NamedIndividual , gr:hasNext gr:Sunday ;
 * }</pre>
 * Created by @szuev on 01.10.2016.
 */
public class ObjectPropertyAssertionTranslator
        extends AbstractPropertyAssertionTranslator<OWLObjectPropertyExpression, OWLObjectPropertyAssertionAxiom> {

    /**
     * Note: ObjectPropertyAssertion(ObjectInverseOf(P) S O) = ObjectPropertyAssertion(P O S)
     *
     * @param axiom {@link OWLObjectPropertyAssertionAxiom}
     * @param model {@link OntGraphModel}
     */
    @Override
    public void write(OWLObjectPropertyAssertionAxiom axiom, OntGraphModel model) {
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
     * See <a href='https://www.w3.org/TR/owl2-quick-reference/'>Assertions</a>
     *
     * @param model  {@link OntGraphModel} the model
     * @param config {@link InternalConfig}
     * @return {@link ExtendedIterator} of {@link OntStatement}s
     */
    @Override
    public ExtendedIterator<OntStatement> listStatements(OntGraphModel model, InternalConfig config) {
        return listStatements(model).filterKeep(s -> testStatement(s, config));
    }

    @Override
    public boolean testStatement(OntStatement statement, InternalConfig config) {
        return statement.isObject()
                && statement.getSubject().canAs(OntIndividual.class)
                && statement.getObject().canAs(OntIndividual.class);
    }

    @Override
    public ONTObject<OWLObjectPropertyAssertionAxiom> toAxiomImpl(OntStatement statement,
                                                                  Supplier<OntGraphModel> model,
                                                                  InternalObjectFactory factory,
                                                                  InternalConfig config) {
        return AxiomImpl.create(statement, model, factory, config);
    }

    @Override
    public ONTObject<OWLObjectPropertyAssertionAxiom> toAxiomWrap(OntStatement statement,
                                                                  InternalObjectFactory factory,
                                                                  InternalConfig config) {
        ONTObject<? extends OWLIndividual> subject = factory.getIndividual(statement.getSubject(OntIndividual.class));
        ONTObject<? extends OWLObjectPropertyExpression> property = factory.getProperty(statement.getPredicate()
                .as(OntOPE.class));
        ONTObject<? extends OWLIndividual> object = factory.getIndividual(statement.getObject(OntIndividual.class));
        Collection<ONTObject<OWLAnnotation>> annotations = factory.getAnnotations(statement, config);
        OWLObjectPropertyAssertionAxiom res = factory.getOWLDataFactory()
                .getOWLObjectPropertyAssertionAxiom(property.getOWLObject(), subject.getOWLObject(),
                        object.getOWLObject(), ONTObject.toSet(annotations));
        return ONTWrapperImpl.create(res, statement).append(annotations)
                .append(subject).append(property).append(object);
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.axioms.OWLObjectPropertyAssertionAxiomImpl
     */
    @SuppressWarnings("WeakerAccess")
    public static abstract class AxiomImpl
            extends AssertionImpl<OWLObjectPropertyAssertionAxiom,
            OWLIndividual, OWLObjectPropertyExpression, OWLIndividual>
            implements OWLObjectPropertyAssertionAxiom {

        protected AxiomImpl(Triple t, Supplier<OntGraphModel> m) {
            super(t, m);
        }

        /**
         * Creates an {@link OWLObjectPropertyAssertionAxiom} that is also {@link ONTObject}.
         *
         * @param statement {@link OntStatement}, the source, not {@code null}
         * @param model     {@link OntGraphModel}-provider, not {@code null}
         * @param factory   {@link InternalObjectFactory}, not {@code null}
         * @param config    {@link InternalConfig}, not {@code null}
         * @return {@link AxiomImpl}
         */
        public static AxiomImpl create(OntStatement statement,
                                       Supplier<OntGraphModel> model,
                                       InternalObjectFactory factory,
                                       InternalConfig config) {
            return WithAssertion.create(statement, model,
                    SimpleImpl.FACTORY, WithAnnotationsImpl.FACTORY, SET_HASH_CODE, factory, config);
        }

        @Override
        public boolean isInSimplifiedForm() {
            return true;
        }

        @Override
        public ONTObject<? extends OWLIndividual> findONTSubject(InternalObjectFactory factory) {
            return findByURIOrBlankId(subject, factory);
        }

        @Override
        public ONTObject<? extends OWLIndividual> findONTObject(InternalObjectFactory factory) {
            return findByURIOrBlankId(object, factory);
        }

        @Override
        public ONTObject<? extends OWLObjectPropertyExpression> findONTPredicate(InternalObjectFactory factory) {
            return findONTProperty(factory);
        }

        public ONTObject<OWLObjectProperty> findONTProperty(InternalObjectFactory factory) {
            return ONTObjectPropertyImpl.find(predicate, factory, model);
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
        protected void collectNamedIndividuals(Set res, InternalObjectFactory factory) {
            if (subject instanceof String) {
                res.add(findNamedIndividual((String) subject,
                        factory == null ? factory = getObjectFactory() : factory).getOWLObject());
            }
            if (object instanceof String) {
                res.add(findNamedIndividual((String) object,
                        factory == null ? getObjectFactory() : factory).getOWLObject());
            }
        }

        /**
         * An {@link OWLObjectPropertyAssertionAxiom} that has no sub-annotations.
         */
        public static class SimpleImpl extends AxiomImpl
                implements Simple<OWLIndividual, OWLObjectPropertyExpression, OWLIndividual> {

            private static final BiFunction<Triple, Supplier<OntGraphModel>, SimpleImpl> FACTORY = SimpleImpl::new;

            protected SimpleImpl(Triple t, Supplier<OntGraphModel> m) {
                super(t, m);
            }

            @Override
            public Set<OWLAnonymousIndividual> getAnonymousIndividualSet() {
                Set<OWLAnonymousIndividual> res = createSortedSet();
                InternalObjectFactory factory = null;
                if (subject instanceof BlankNodeId) {
                    res.add(findAnonymousIndividual((BlankNodeId) subject,
                            factory = getObjectFactory()).getOWLObject());
                }
                if (object instanceof BlankNodeId) {
                    res.add(findAnonymousIndividual((BlankNodeId) object,
                            factory == null ? getObjectFactory() : factory).getOWLObject());
                }
                return res;
            }

            @Override
            public Set<OWLEntity> getSignatureSet() {
                Set<OWLEntity> res = createSortedSet();
                InternalObjectFactory factory = getObjectFactory();
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

            private static final BiFunction<Triple, Supplier<OntGraphModel>, WithAnnotationsImpl> FACTORY =
                    WithAnnotationsImpl::new;
            protected final InternalCache.Loading<WithAnnotationsImpl, Object[]> content;

            public WithAnnotationsImpl(Triple t, Supplier<OntGraphModel> m) {
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
