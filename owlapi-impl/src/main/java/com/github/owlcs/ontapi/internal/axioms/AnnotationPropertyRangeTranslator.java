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
import com.github.owlcs.ontapi.internal.InternalCache;
import com.github.owlcs.ontapi.internal.ModelObjectFactory;
import com.github.owlcs.ontapi.internal.ONTObject;
import com.github.owlcs.ontapi.internal.ONTObjectFactory;
import com.github.owlcs.ontapi.internal.ONTWrapperImpl;
import com.github.owlcs.ontapi.internal.ReadHelper;
import com.github.owlcs.ontapi.internal.objects.FactoryAccessor;
import com.github.owlcs.ontapi.internal.objects.ONTEntityImpl;
import com.github.owlcs.ontapi.internal.objects.ONTStatementImpl;
import com.github.owlcs.ontapi.jena.model.OntAnnotationProperty;
import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.jena.model.OntStatement;
import org.apache.jena.graph.Triple;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.util.iterator.NullIterator;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLEntity;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * A translator that provides {@link OWLAnnotationPropertyRangeAxiom} implementations.
 * The main triple is {@code A rdfs:range U}, where {@code A} is annotation property and {@code U} is IRI.
 * <p>
 * Created by @szuev on 30.09.2016.
 */
public class AnnotationPropertyRangeTranslator
        extends AbstractPropertyRangeTranslator<OWLAnnotationPropertyRangeAxiom, OntAnnotationProperty> {

    @Override
    Class<OntAnnotationProperty> getView() {
        return OntAnnotationProperty.class;
    }

    /**
     * Returns {@link OntStatement}s defining the {@link OWLAnnotationPropertyRangeAxiom} axiom.
     *
     * @param model  {@link OntModel}
     * @param config {@link AxiomsSettings}
     * @return {@link ExtendedIterator} of {@link OntStatement}s
     */
    @Override
    public ExtendedIterator<OntStatement> listStatements(OntModel model, AxiomsSettings config) {
        if (!config.isLoadAnnotationAxioms()) return NullIterator.instance();
        return super.listStatements(model, config);
    }

    @Override
    public boolean filter(OntStatement statement, AxiomsSettings config) {
        return super.filter(statement, config)
                && statement.getObject().isURIResource()
                && ReadHelper.testAnnotationAxiomOverlaps(statement, config,
                AxiomType.OBJECT_PROPERTY_RANGE, AxiomType.DATA_PROPERTY_RANGE);
    }

    @Override
    public boolean testStatement(OntStatement statement, AxiomsSettings config) {
        return config.isLoadAnnotationAxioms() && super.testStatement(statement, config);
    }

    @Override
    public ONTObject<OWLAnnotationPropertyRangeAxiom> toAxiomImpl(OntStatement statement,
                                                                  ModelObjectFactory factory,
                                                                  AxiomsSettings config) {
        return AxiomImpl.create(statement, factory, config);
    }

    @Override
    public ONTObject<OWLAnnotationPropertyRangeAxiom> toAxiomWrap(OntStatement statement,
                                                                  ONTObjectFactory factory,
                                                                  AxiomsSettings config) {
        ONTObject<OWLAnnotationProperty> p = factory.getProperty(statement.getSubject(getView()));
        ONTObject<IRI> d = factory.getIRI(statement.getResource().getURI());
        Collection<ONTObject<OWLAnnotation>> annotations = factory.getAnnotations(statement, config);
        OWLAnnotationPropertyRangeAxiom res = factory.getOWLDataFactory()
                .getOWLAnnotationPropertyRangeAxiom(p.getOWLObject(), d.getOWLObject(), TranslateHelper.toSet(annotations));
        return ONTWrapperImpl.create(res, statement).append(annotations).append(p).append(d);
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.axioms.AnnotationPropertyRangeAxiomImpl
     */
    @SuppressWarnings("WeakerAccess")
    public abstract static class AxiomImpl
            extends RangeAxiomImpl<OWLAnnotationPropertyRangeAxiom, OWLAnnotationProperty, IRI>
            implements OWLAnnotationPropertyRangeAxiom {

        protected AxiomImpl(Triple t, Supplier<OntModel> m) {
            super(t, m);
        }

        /**
         * Creates an {@link ONTObject} container that is also {@link OWLAnnotationPropertyRangeAxiom}.
         *
         * @param statement {@link OntStatement}, not {@code null}
         * @param factory   {@link ONTObjectFactory}, not {@code null}
         * @param config    {@link AxiomsSettings}, not {@code null}
         * @return {@link AxiomImpl}
         */
        public static AxiomImpl create(OntStatement statement,
                                       ModelObjectFactory factory,
                                       AxiomsSettings config) {
            return WithTwoObjects.create(statement,
                    SimpleImpl.FACTORY, WithAnnotationsImpl.FACTORY, SET_HASH_CODE, factory, config);
        }

        @Override
        public ONTObject<? extends OWLAnnotationProperty> getURISubject(ModelObjectFactory factory) {
            return factory.getAnnotationProperty(getSubjectURI());
        }

        @Override
        public ONTObject<? extends IRI> getURIObject(ModelObjectFactory factory) {
            return factory.getIRI(getObjectURI());
        }

        @Override
        public ONTObject<? extends OWLAnnotationProperty> subjectFromStatement(OntStatement statement,
                                                                               ModelObjectFactory factory) {
            return factory.getProperty(statement.getSubject(OntAnnotationProperty.class));
        }

        @Override
        public ONTObject<? extends IRI> objectFromStatement(OntStatement statement, ModelObjectFactory factory) {
            return factory.getIRI(statement.getObject().asNode().getURI());
        }

        @FactoryAccessor
        @Override
        protected OWLAnnotationPropertyRangeAxiom createAnnotatedAxiom(Collection<OWLAnnotation> annotations) {
            return getDataFactory().getOWLAnnotationPropertyRangeAxiom(eraseModel(getProperty()),
                    getRange(), annotations);
        }

        @Override
        public final boolean canContainNamedClasses() {
            return false;
        }

        @Override
        public final boolean canContainNamedIndividuals() {
            return false;
        }

        @Override
        public final boolean canContainObjectProperties() {
            return false;
        }

        @Override
        public final boolean canContainDataProperties() {
            return false;
        }

        @Override
        public final boolean canContainClassExpressions() {
            return false;
        }

        /**
         * An {@link OWLAnnotationPropertyRangeAxiom} without annotations.
         */
        public static class SimpleImpl extends AxiomImpl implements Simple<OWLAnnotationProperty, IRI> {

            private static final BiFunction<Triple, Supplier<OntModel>, SimpleImpl> FACTORY = SimpleImpl::new;

            protected SimpleImpl(Triple t, Supplier<OntModel> m) {
                super(t, m);
            }

            @Override
            public Set<OWLAnnotationProperty> getAnnotationPropertySet() {
                return createSet(getONTSubject().getOWLObject());
            }

            @Override
            public Set<OWLEntity> getSignatureSet() {
                return createSet(getONTSubject().getOWLObject());
            }

            @Override
            public boolean containsAnnotationProperty(OWLAnnotationProperty property) {
                return getSubjectURI().equals(ONTEntityImpl.getURI(property));
            }

            @Override
            protected boolean sameContent(ONTStatementImpl other) {
                return false;
            }

            @Override
            public boolean canContainDatatypes() {
                return false;
            }

            @Override
            public boolean canContainAnonymousIndividuals() {
                return false;
            }
        }

        /**
         * An {@link OWLAnnotationPropertyRangeAxiom} with annotations.
         * It has a public constructor since it is more generic then {@link SimpleImpl}.
         */
        public static class WithAnnotationsImpl extends AxiomImpl
                implements Complex<WithAnnotationsImpl, OWLAnnotationProperty, IRI> {

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

            @Override
            protected boolean sameContent(ONTStatementImpl other) {
                return other instanceof WithAnnotationsImpl
                        && Arrays.equals(getContent(), ((WithAnnotationsImpl) other).getContent());
            }
        }
    }
}
