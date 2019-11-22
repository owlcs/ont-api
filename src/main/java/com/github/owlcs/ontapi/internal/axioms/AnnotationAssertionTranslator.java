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
import com.github.owlcs.ontapi.internal.objects.FactoryAccessor;
import com.github.owlcs.ontapi.internal.objects.ONTAnnotationImpl;
import com.github.owlcs.ontapi.internal.objects.ONTLiteralImpl;
import com.github.owlcs.ontapi.jena.model.*;
import org.apache.jena.graph.BlankNodeId;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.impl.LiteralLabel;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.util.iterator.NullIterator;
import org.semanticweb.owlapi.model.*;

import java.util.Collection;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * A translator that provides {@link OWLAnnotationAssertionAxiom} implementations.
 * Examples:
 * <pre>{@code
 *  foaf:LabelProperty vs:term_status "unstable" .
 *  foaf:LabelProperty rdfs:isDefinedBy <http://xmlns.com/foaf/0.1/> .
 *  pizza:UnclosedPizza rdfs:label "PizzaAberta"@pt .
 * }</pre>
 * <p>
 * Created by @szuev on 28.09.2016.
 */
@SuppressWarnings("WeakerAccess")
public class AnnotationAssertionTranslator
        extends AbstractPropertyAssertionTranslator<OWLAnnotationProperty, OWLAnnotationAssertionAxiom> {

    @Override
    public void write(OWLAnnotationAssertionAxiom axiom, OntGraphModel model) {
        WriteHelper.writeAssertionTriple(model, axiom.getSubject(),
                axiom.getProperty(), axiom.getValue(), axiom.annotationsAsList());
    }

    /**
     * Answers the annotation assertion statements.
     * The rule {@code s A t}, where {@code s} is an IRI or anonymous individual,
     * {@code t} is an IRI, anonymous individual, or literal, and {@code A} is an annotation property.
     * Currently there is following default behaviour:
     * if the annotation value has its own annotations then the specified statement is skipped from consideration
     * but comes as annotation of some other axiom.
     * Also it is skipped if load annotations is disabled in the configuration.
     *
     * @param model  {@link OntGraphModel} the model
     * @param config {@link InternalConfig}
     * @return {@link ExtendedIterator} of {@link OntStatement}s
     * @see <a href='https://www.w3.org/TR/owl2-quick-reference/'>Annotations</a>
     */
    @Override
    public ExtendedIterator<OntStatement> listStatements(OntGraphModel model, InternalConfig config) {
        if (!config.isLoadAnnotationAxioms()) return NullIterator.instance();
        OntID id = model.getID();
        return listStatements(model).filterKeep(s -> !id.equals(s.getSubject()) && filter(s, config));
    }

    @Override
    public boolean testStatement(OntStatement statement, InternalConfig config) {
        if (!config.isLoadAnnotationAxioms()) return false;
        if (statement.getSubject().canAs(OntID.class)) return false;
        return filter(statement, config);
    }

    public boolean filter(OntStatement s, InternalConfig c) {
        return ReadHelper.isAnnotationAssertionStatement(s, c)
                && ReadHelper.isEntityOrAnonymousIndividual(s.getSubject());
    }

    @Override
    public ONTObject<OWLAnnotationAssertionAxiom> toAxiomImpl(OntStatement statement,
                                                              Supplier<OntGraphModel> model,
                                                              InternalObjectFactory factory,
                                                              InternalConfig config) {
        return AxiomImpl.create(statement, model, factory, config);
    }

    @Override
    public ONTObject<OWLAnnotationAssertionAxiom> toAxiomWrap(OntStatement statement,
                                                              InternalObjectFactory factory,
                                                              InternalConfig config) {
        ONTObject<? extends OWLAnnotationSubject> s = factory.getSubject(statement.getSubject(OntObject.class));
        ONTObject<OWLAnnotationProperty> p = factory.getProperty(statement.getPredicate().as(OntNAP.class));
        ONTObject<? extends OWLAnnotationValue> v = factory.getValue(statement.getObject());
        Collection<ONTObject<OWLAnnotation>> annotations = factory.getAnnotations(statement, config);
        OWLAnnotationAssertionAxiom res = factory.getOWLDataFactory()
                .getOWLAnnotationAssertionAxiom(p.getOWLObject(), s.getOWLObject(), v.getOWLObject(),
                        ONTObject.toSet(annotations));
        return ONTWrapperImpl.create(res, statement).append(annotations).append(s).append(p).append(v);
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.axioms.OWLAnnotationAssertionAxiomImpl
     * @see ONTAnnotationImpl
     */
    public static abstract class AxiomImpl
            extends AssertionImpl<OWLAnnotationAssertionAxiom,
            OWLAnnotationSubject, OWLAnnotationProperty, OWLAnnotationValue>
            implements OWLAnnotationAssertionAxiom {

        protected AxiomImpl(Triple t, Supplier<OntGraphModel> m) {
            super(t, m);
        }

        /**
         * Creates an {@link OWLAnnotationAssertionAxiom} that is also {@link ONTObject}.
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
                    SimpleImpl.FACTORY, AxiomImpl.WithAnnotationsImpl.FACTORY, SET_HASH_CODE, factory, config);
        }

        @Override
        public ONTObject<? extends OWLAnnotationSubject> findONTSubject(InternalObjectFactory factory) {
            return ONTAnnotationImpl.findONTSubject(this, factory);
        }

        @Override
        public ONTObject<? extends OWLAnnotationValue> findONTObject(InternalObjectFactory factory) {
            return ONTAnnotationImpl.findONTObject(this, factory);
        }

        @Override
        public ONTObject<? extends OWLAnnotationProperty> findONTPredicate(InternalObjectFactory factory) {
            return ONTAnnotationImpl.findONTPredicate(this, factory);
        }

        @FactoryAccessor
        @Override
        public OWLAnnotation getAnnotation() {
            return getDataFactory().getOWLAnnotation(getFPredicate(), getFObject());
        }

        @Override
        public boolean isDeprecatedIRIAssertion() {
            return ONTAnnotationImpl.isDeprecated(predicate, object);
        }

        @FactoryAccessor
        @Override
        protected OWLAnnotationAssertionAxiom createAnnotatedAxiom(Collection<OWLAnnotation> annotations) {
            return getDataFactory().getOWLAnnotationAssertionAxiom(getFPredicate(), getFSubject(),
                    getFObject(), annotations);
        }

        @Override
        public boolean canContainNamedIndividuals() {
            return false;
        }

        @Override
        public boolean canContainDataProperties() {
            return false;
        }

        @Override
        public boolean canContainObjectProperties() {
            return false;
        }

        /**
         * An {@link OWLAnnotationAssertionAxiom} that has no sub-annotations.
         */
        public static class SimpleImpl extends AxiomImpl
                implements Simple<OWLAnnotationSubject, OWLAnnotationProperty, OWLAnnotationValue> {

            private static final BiFunction<Triple, Supplier<OntGraphModel>, SimpleImpl> FACTORY = SimpleImpl::new;

            protected SimpleImpl(Triple t, Supplier<OntGraphModel> m) {
                super(t, m);
            }

            @Override
            public boolean containsDatatype(OWLDatatype datatype) {
                return object instanceof LiteralLabel
                        && findLiteral(getObjectFactory()).containsEntityInSignature(datatype);
            }

            @Override
            public boolean containsAnnotationProperty(OWLAnnotationProperty property) {
                return getONTPredicate().getOWLObject().equals(property);
            }

            @Override
            public Set<OWLAnonymousIndividual> getAnonymousIndividualSet() {
                Set<OWLAnonymousIndividual> res = createSortedSet();
                InternalObjectFactory factory = null;
                if (subject instanceof BlankNodeId) {
                    res.add(findAnonymousSubject(factory = getObjectFactory()));
                }
                if (object instanceof BlankNodeId) {
                    res.add(findAnonymousObject(factory == null ? getObjectFactory() : factory));
                }
                return res;
            }

            @Override
            public Set<OWLAnnotationProperty> getAnnotationPropertySet() {
                return createSet(getProperty());
            }

            @Override
            public Set<OWLDatatype> getDatatypeSet() {
                return object instanceof LiteralLabel ?
                        createSet(findLiteral(getObjectFactory()).getDatatype()) : createSet();
            }

            @Override
            public Set<OWLEntity> getSignatureSet() {
                Set<OWLEntity> res = createSortedSet();
                InternalObjectFactory factory = getObjectFactory();
                res.add(findONTPredicate(factory).getOWLObject());
                if (object instanceof LiteralLabel) {
                    res.add(findLiteral(factory).getDatatype());
                }
                return res;
            }

            protected OWLLiteral findLiteral(InternalObjectFactory factory) {
                return ONTLiteralImpl.find((LiteralLabel) object, factory, model).getOWLObject();
            }

            private OWLAnonymousIndividual findAnonymousSubject(InternalObjectFactory factory) {
                return findAnonymousIndividual((BlankNodeId) subject, factory).getOWLObject();
            }

            private OWLAnonymousIndividual findAnonymousObject(InternalObjectFactory factory) {
                return findAnonymousIndividual((BlankNodeId) object, factory).getOWLObject();
            }
        }

        /**
         * An {@link OWLAnnotationAssertionAxiom} that has sub-annotations.
         * This class has a public constructor since it is more generic then {@link SimpleImpl}.
         *
         * @see ONTAnnotationImpl.WithAnnotationsImpl
         */
        public static class WithAnnotationsImpl extends AxiomImpl
                implements WithAnnotations<WithAnnotationsImpl,
                OWLAnnotationSubject, OWLAnnotationProperty, OWLAnnotationValue> {

            private static final BiFunction<Triple, Supplier<OntGraphModel>, WithAnnotationsImpl> FACTORY =
                    AxiomImpl.WithAnnotationsImpl::new;
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
