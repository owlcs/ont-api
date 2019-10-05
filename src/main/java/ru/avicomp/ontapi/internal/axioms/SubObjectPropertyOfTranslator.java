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

package ru.avicomp.ontapi.internal.axioms;

import org.apache.jena.graph.Triple;
import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.internal.*;
import ru.avicomp.ontapi.internal.objects.*;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntOPE;
import ru.avicomp.ontapi.jena.model.OntStatement;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A translator that provides {@link OWLSubObjectPropertyOfAxiom} implementations.
 * See also the base - {@link AbstractSubPropertyTranslator}.
 * <p>
 * Created by @szuev on 29.09.2016.
 */
public class SubObjectPropertyOfTranslator extends AbstractSubPropertyTranslator<OWLSubObjectPropertyOfAxiom, OntOPE> {

    @Override
    OWLPropertyExpression getSubProperty(OWLSubObjectPropertyOfAxiom axiom) {
        return axiom.getSubProperty();
    }

    @Override
    OWLPropertyExpression getSuperProperty(OWLSubObjectPropertyOfAxiom axiom) {
        return axiom.getSuperProperty();
    }

    @Override
    Class<OntOPE> getView() {
        return OntOPE.class;
    }

    @Override
    public ONTObject<OWLSubObjectPropertyOfAxiom> toAxiom(OntStatement statement,
                                                          Supplier<OntGraphModel> model,
                                                          InternalObjectFactory factory,
                                                          InternalConfig config) {
        return AxiomImpl.create(statement, model, factory, config);
    }

    @Override
    public ONTObject<OWLSubObjectPropertyOfAxiom> toAxiom(OntStatement statement,
                                                          InternalObjectFactory factory,
                                                          InternalConfig config) {
        ONTObject<? extends OWLObjectPropertyExpression> sub = factory.getProperty(statement.getSubject(OntOPE.class));
        ONTObject<? extends OWLObjectPropertyExpression> sup = factory.getProperty(statement.getObject().as(OntOPE.class));
        Collection<ONTObject<OWLAnnotation>> annotations = factory.getAnnotations(statement, config);
        OWLSubObjectPropertyOfAxiom res = factory.getOWLDataFactory()
                .getOWLSubObjectPropertyOfAxiom(sub.getOWLObject(), sup.getOWLObject(), ONTObject.toSet(annotations));
        return ONTWrapperImpl.create(res, statement).append(annotations).append(sub).append(sup);
    }

    /**
     * @see ru.avicomp.ontapi.owlapi.axioms.OWLSubObjectPropertyOfAxiomImpl
     */
    @SuppressWarnings("WeakerAccess")
    public abstract static class AxiomImpl extends ONTAxiomImpl<OWLSubObjectPropertyOfAxiom>
            implements WithTwoObjects.Unary<OWLObjectPropertyExpression>, OWLSubObjectPropertyOfAxiom {

        protected AxiomImpl(Triple t, Supplier<OntGraphModel> m) {
            super(t, m);
        }

        /**
         * Creates an {@link ONTObject} container for the given {@link OntStatement};
         * the returned object is also {@link OWLSubObjectPropertyOfAxiom}.
         * <p>
         * Impl notes:
         * If there is no sub-annotations and subject and object are URI-{@link org.apache.jena.rdf.model.Resource}s,
         * then a simplified instance of {@link SimpleImpl} is returned.
         * Otherwise the instance is {@link ComplexImpl} and has a cache inside.
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
            SimpleImpl s = new SimpleImpl(statement.asTriple(), model);
            Object[] content = WithPartialContent.initContent(s, statement, SET_HASH_CODE, factory, config);
            if (content == EMPTY) {
                return s;
            }
            ComplexImpl c = new ComplexImpl(statement.asTriple(), model);
            c.setHashCode(s.hashCode);
            c.putContent(content);
            return c;
        }

        @Override
        public OWLObjectPropertyExpression getSubProperty() {
            return getONTSubject().getOWLObject();
        }

        @Override
        public OWLObjectPropertyExpression getSuperProperty() {
            return getONTObject().getOWLObject();
        }

        @Override
        public ONTObject<? extends OWLObjectPropertyExpression> fetchONTSubject(OntStatement statement, InternalObjectFactory factory) {
            return factory.getProperty(statement.getSubject(OntOPE.class));
        }

        @Override
        public ONTObject<? extends OWLObjectPropertyExpression> fetchONTObject(OntStatement statement, InternalObjectFactory factory) {
            return factory.getProperty(statement.getObject(OntOPE.class));
        }

        @Override
        public ONTObject<? extends OWLObjectPropertyExpression> findByURI(String uri, InternalObjectFactory factory) {
            return ONTObjectPropertyImpl.find(uri, factory, model);
        }

        @FactoryAccessor
        @Override
        protected OWLSubObjectPropertyOfAxiom createAnnotatedAxiom(Collection<OWLAnnotation> annotations) {
            return getDataFactory().getOWLSubObjectPropertyOfAxiom(eraseModel(getSubProperty()),
                    eraseModel(getSuperProperty()), annotations);
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
        public boolean canContainNamedClasses() {
            return false;
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
        public boolean canContainClassExpressions() {
            return false;
        }

        /**
         * An {@link OWLSubObjectPropertyOfAxiom}
         * that has named object properties as subject and object and has no annotations.
         */
        protected static class SimpleImpl extends AxiomImpl implements UnarySimple<OWLObjectPropertyExpression> {

            protected SimpleImpl(Triple t, Supplier<OntGraphModel> m) {
                super(t, m);
            }

            @SuppressWarnings("unchecked")
            @Override
            public Set<OWLObjectProperty> getObjectPropertySet() {
                return (Set<OWLObjectProperty>) getOWLComponentsAsSet();
            }

            @SuppressWarnings("unchecked")
            @Override
            public Set<OWLEntity> getSignatureSet() {
                return (Set<OWLEntity>) getOWLComponentsAsSet();
            }

            @Override
            public boolean containsObjectProperty(OWLObjectProperty property) {
                String uri = ONTEntityImpl.getURI(property);
                return subject.equals(uri) || object.equals(uri);
            }

            @Override
            protected boolean sameContent(ONTStatementImpl other) {
                return false;
            }
        }

        /**
         * An {@link OWLSubObjectPropertyOfAxiom}
         * that either has annotations or anonymous object property expressions (inverse object properties)
         * in its subject or object positions.
         * It has a public constructor since it is more generic then {@link SimpleImpl}.
         */
        public static class ComplexImpl extends AxiomImpl
                implements UnaryWithContent<ComplexImpl, OWLObjectPropertyExpression> {

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
                if (hasURISubject() && !sameSubject(other)) {
                    return false;
                }
                if (hasURIObject() && !sameObject(other)) {
                    return false;
                }
                return other instanceof ComplexImpl && Arrays.equals(getContent(), ((ComplexImpl) other).getContent());
            }

            @Override
            protected boolean sameAs(ONTStatementImpl other) {
                if (notSame(other)) {
                    return false;
                }
                // no #sameTriple(), since it can contain b-nodes
                return sameContent(other);
            }
        }
    }

}
