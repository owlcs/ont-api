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
import com.github.owlcs.ontapi.internal.objects.ONTStatementImpl;
import com.github.owlcs.ontapi.jena.model.OntGraphModel;
import com.github.owlcs.ontapi.jena.model.OntOPE;
import com.github.owlcs.ontapi.jena.model.OntStatement;
import com.github.owlcs.ontapi.jena.vocabulary.OWL;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Resource;
import org.semanticweb.owlapi.model.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A translator that provides {@link OWLSymmetricObjectPropertyAxiom} implementations.
 * Example:
 * <pre>{@code
 * gr:equal rdf:type owl:ObjectProperty ;  owl:inverseOf gr:equal ;  rdf:type owl:SymmetricProperty ,  owl:TransitiveProperty ;
 * }</pre>
 * <p>
 * Created by @szuev on 30.09.2016.
 */
public class SymmetricObjectPropertyTranslator
        extends AbstractPropertyTypeTranslator<OWLSymmetricObjectPropertyAxiom, OntOPE> {

    @Override
    Resource getType() {
        return OWL.SymmetricProperty;
    }

    @Override
    Class<OntOPE> getView() {
        return OntOPE.class;
    }

    @Override
    public ONTObject<OWLSymmetricObjectPropertyAxiom> toAxiom(OntStatement statement,
                                                              Supplier<OntGraphModel> model,
                                                              InternalObjectFactory factory,
                                                              InternalConfig config) {
        return AxiomImpl.create(statement, model, factory, config);
    }

    @Override
    public ONTObject<OWLSymmetricObjectPropertyAxiom> toAxiom(OntStatement statement,
                                                              InternalObjectFactory factory,
                                                              InternalConfig config) {
        ONTObject<? extends OWLObjectPropertyExpression> p = factory.getProperty(getSubject(statement));
        Collection<ONTObject<OWLAnnotation>> annotations = factory.getAnnotations(statement, config);
        OWLSymmetricObjectPropertyAxiom res = factory.getOWLDataFactory()
                .getOWLSymmetricObjectPropertyAxiom(p.getOWLObject(), ONTObject.toSet(annotations));
        return ONTWrapperImpl.create(res, statement).append(annotations).append(p);
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.axioms.OWLSymmetricObjectPropertyAxiomImpl
     */
    public static abstract class AxiomImpl extends ObjectAxiomImpl<OWLSymmetricObjectPropertyAxiom>
            implements OWLSymmetricObjectPropertyAxiom {

        protected AxiomImpl(Triple t, Supplier<OntGraphModel> m) {
            super(t, m);
        }

        /**
         * Creates an {@link ONTObject} container, that is also {@link OWLSymmetricObjectPropertyAxiom}.
         *
         * @param statement {@link OntStatement}, not {@code null}
         * @param model     {@link OntGraphModel} provider, not {@code null}
         * @param factory   {@link InternalObjectFactory}, not {@code null}
         * @param config    {@link InternalConfig}, not {@code null}
         * @return {@link AxiomImpl}
         */
        public static AxiomImpl create(OntStatement statement,
                                       Supplier<OntGraphModel> model,
                                       InternalObjectFactory factory,
                                       InternalConfig config) {
            return WithOneObject.create(statement, model,
                    SimpleImpl.FACTORY, ComplexImpl.FACTORY, SET_HASH_CODE, factory, config);
        }

        @FactoryAccessor
        @Override
        public Set<OWLSubObjectPropertyOfAxiom> asSubPropertyAxioms() {
            DataFactory df = getDataFactory();
            Set<OWLSubObjectPropertyOfAxiom> res = new HashSet<>(2);
            OWLObjectPropertyExpression p = eraseModel(getProperty());
            res.add(df.getOWLSubObjectPropertyOfAxiom(p, p.getInverseProperty(), NO_ANNOTATIONS));
            res.add(df.getOWLSubObjectPropertyOfAxiom(p.getInverseProperty(), p, NO_ANNOTATIONS));
            return res;
        }

        @FactoryAccessor
        @Override
        protected OWLSymmetricObjectPropertyAxiom createAnnotatedAxiom(Collection<OWLAnnotation> annotations) {
            return getDataFactory().getOWLSymmetricObjectPropertyAxiom(eraseModel(getProperty()), annotations);
        }

        /**
         * An {@link OWLSymmetricObjectPropertyAxiom}
         * that has a named object property as subject and has no annotations.
         */
        public static class SimpleImpl extends AxiomImpl implements Simple<OWLObjectPropertyExpression> {

            private static final BiFunction<Triple, Supplier<OntGraphModel>, SimpleImpl> FACTORY = SimpleImpl::new;

            protected SimpleImpl(Triple t, Supplier<OntGraphModel> m) {
                super(t, m);
            }

            @Override
            public Set<OWLObjectProperty> getObjectPropertySet() {
                return getComponentsAsPropertySet();
            }

            @Override
            public Set<OWLEntity> getSignatureSet() {
                return getComponentsAsEntitySet();
            }

            @Override
            public boolean containsObjectProperty(OWLObjectProperty property) {
                return hasSubject(property);
            }
        }

        /**
         * An {@link OWLSymmetricObjectPropertyAxiom}
         * that either has annotations or an anonymous object property expression (inverse object property)
         * in the main triple's subject position.
         * It has a public constructor since this class is more generic then {@link SimpleImpl}.
         */
        public static class ComplexImpl extends AxiomImpl implements Complex<ComplexImpl, OWLObjectPropertyExpression> {

            private static final BiFunction<Triple, Supplier<OntGraphModel>, ComplexImpl> FACTORY = ComplexImpl::new;
            protected final InternalCache.Loading<ComplexImpl, Object[]> content;

            public ComplexImpl(Triple t, Supplier<OntGraphModel> m) {
                super(t, m);
                this.content = createContentCache();
            }

            @Override
            public InternalCache.Loading<ComplexImpl, Object[]> getContentCache() {
                return content;
            }

            @Override
            protected boolean sameContent(ONTStatementImpl other) {
                return other instanceof ComplexImpl && Arrays.equals(getContent(), ((ComplexImpl) other).getContent());
            }

            @Override
            public ONTObject<OWLSymmetricObjectPropertyAxiom> merge(ONTObject<OWLSymmetricObjectPropertyAxiom> other) {
                if (this == other) {
                    return this;
                }
                if (other instanceof AxiomImpl && sameTriple((AxiomImpl) other)) {
                    return this;
                }
                ComplexImpl res = new ComplexImpl(asTriple(), model) {
                    @Override
                    public Stream<Triple> triples() {
                        return Stream.concat(ComplexImpl.this.triples(), other.triples());
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
}
