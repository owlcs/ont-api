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

import com.github.owlcs.ontapi.config.AxiomsSettings;
import com.github.owlcs.ontapi.internal.*;
import com.github.owlcs.ontapi.internal.objects.FactoryAccessor;
import com.github.owlcs.ontapi.internal.objects.ONTAxiomImpl;
import com.github.owlcs.ontapi.internal.objects.ONTEntityImpl;
import com.github.owlcs.ontapi.internal.objects.ONTStatementImpl;
import com.github.owlcs.ontapi.jena.model.OntDataRange;
import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.jena.model.OntStatement;
import com.github.owlcs.ontapi.jena.vocabulary.OWL;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A translator that provides {@link OWLDatatypeDefinitionAxiom} implementations.
 * <p>
 * The main statement is {@code DN owl:equivalentClass D},
 * where {@code DN} - an OWL datatype (named data range) and {@code D} - any data range.
 * <p>
 * Example:
 * <pre>{@code
 * :data-type-3 rdf:type rdfs:Datatype ;
 *              owl:equivalentClass [
 *                       rdf:type rdfs:Datatype ;
 *                       owl:unionOf ( :data-type-1  :data-type-2 )
 *              ] .
 * }</pre>
 * <p>
 * Created by @szuev on 18.10.2016.
 */
public class DatatypeDefinitionTranslator extends AbstractSimpleTranslator<OWLDatatypeDefinitionAxiom> {

    @Override
    public void write(OWLDatatypeDefinitionAxiom axiom, OntModel model) {
        WriteHelper.writeTriple(model, axiom.getDatatype(), OWL.equivalentClass, axiom.getDataRange(),
                axiom.annotationsAsList());
    }

    @Override
    public ExtendedIterator<OntStatement> listStatements(OntModel model, AxiomsSettings config) {
        return listByPredicate(model, OWL.equivalentClass)
                .filterKeep(s -> s.getSubject().canAs(OntDataRange.Named.class) && s.getObject().canAs(OntDataRange.class));
    }

    @Override
    public boolean testStatement(OntStatement statement, AxiomsSettings config) {
        return statement.getPredicate().equals(OWL.equivalentClass)
                && statement.getSubject().canAs(OntDataRange.Named.class)
                && statement.getObject().canAs(OntDataRange.class);
    }

    @Override
    public ONTObject<OWLDatatypeDefinitionAxiom> toAxiomImpl(OntStatement statement,
                                                             ModelObjectFactory factory,
                                                             AxiomsSettings config) {
        return AxiomImpl.create(statement, factory, config);
    }

    @Override
    public ONTObject<OWLDatatypeDefinitionAxiom> toAxiomWrap(OntStatement statement,
                                                             ONTObjectFactory factory,
                                                             AxiomsSettings config) {
        ONTObject<OWLDatatype> dt = factory.getDatatype(statement.getSubject(OntDataRange.Named.class));
        ONTObject<? extends OWLDataRange> dr = factory.getDatatype(statement.getObject(OntDataRange.class));
        Collection<ONTObject<OWLAnnotation>> annotations = factory.getAnnotations(statement, config);
        OWLDatatypeDefinitionAxiom res = factory.getOWLDataFactory()
                .getOWLDatatypeDefinitionAxiom(dt.getOWLObject(), dr.getOWLObject(), TranslateHelper.toSet(annotations));
        return ONTWrapperImpl.create(res, statement).append(annotations).append(dt).append(dr);
    }

    @Override
    Triple createSearchTriple(OWLDatatypeDefinitionAxiom axiom) {
        Node object = TranslateHelper.getSearchNode(axiom.getDataRange());
        if (object == null) return null;
        Node subject = WriteHelper.toNode(axiom.getDatatype());
        return Triple.create(subject, OWL.equivalentClass.asNode(), object);
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.axioms.DatatypeDefinitionAxiomImpl
     */
    public abstract static class AxiomImpl extends ONTAxiomImpl<OWLDatatypeDefinitionAxiom>
            implements WithTwoObjects<OWLDatatype, OWLDataRange>, OWLDatatypeDefinitionAxiom {

        protected AxiomImpl(Triple t, Supplier<OntModel> m) {
            super(t, m);
        }

        protected AxiomImpl(Object subject, String predicate, Object object, Supplier<OntModel> m) {
            super(subject, predicate, object, m);
        }

        /**
         * Creates an {@link ONTObject} container that is also {@link OWLDatatypeDefinitionAxiom}.
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
                    SimpleImpl.FACTORY, ComplexImpl.FACTORY, SET_HASH_CODE, factory, config);
        }

        @Override
        public OWLDatatype getDatatype() {
            return getONTSubject().getOWLObject();
        }

        @Override
        public OWLDataRange getDataRange() {
            return getONTObject().getOWLObject();
        }

        @Override
        public ONTObject<? extends OWLDatatype> getURISubject(ModelObjectFactory factory) {
            return factory.getDatatype(getSubjectURI());
        }

        @Override
        public ONTObject<? extends OWLDatatype> subjectFromStatement(OntStatement statement,
                                                                     ModelObjectFactory factory) {
            return factory.getDatatype(statement.getSubject(OntDataRange.Named.class));
        }

        @Override
        public ONTObject<? extends OWLDataRange> getURIObject(ModelObjectFactory factory) {
            return factory.getDatatype(getObjectURI());
        }

        @Override
        public ONTObject<? extends OWLDataRange> objectFromStatement(OntStatement statement,
                                                                     ModelObjectFactory factory) {
            return factory.getDatatype(statement.getObject(OntDataRange.class));
        }

        @FactoryAccessor
        @Override
        protected OWLDatatypeDefinitionAxiom createAnnotatedAxiom(Collection<OWLAnnotation> annotations) {
            return getDataFactory().getOWLDatatypeDefinitionAxiom(eraseModel(getDatatype()),
                    eraseModel(getDataRange()), annotations);
        }

        @Override
        public final boolean canContainNamedClasses() {
            return false;
        }

        @Override
        public final boolean canContainClassExpressions() {
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
        public final boolean canContainAnonymousIndividuals() {
            return isAnnotated();
        }

        @Override
        public final boolean canContainAnnotationProperties() {
            return isAnnotated();
        }

        /**
         * An {@link OWLDatatypeDefinitionAxiom}
         * that has named data range expressions (i.e. {@link OWLDatatype OWL datatype}s) both as subject and object
         * and has no annotations.
         */
        public static class SimpleImpl extends AxiomImpl implements Simple<OWLDatatype, OWLDataRange> {

            private static final BiFunction<Triple, Supplier<OntModel>, SimpleImpl> FACTORY = SimpleImpl::new;

            protected SimpleImpl(Triple t, Supplier<OntModel> m) {
                super(t, m);
            }

            @SuppressWarnings("unchecked")
            @Override
            public Set<OWLDatatype> getDatatypeSet() {
                return (Set<OWLDatatype>) getOWLComponentsAsSet();
            }

            @SuppressWarnings("unchecked")
            @Override
            public Set<OWLEntity> getSignatureSet() {
                return (Set<OWLEntity>) getOWLComponentsAsSet();
            }

            @Override
            public boolean containsDatatype(OWLDatatype datatype) {
                return hasURIResource(ONTEntityImpl.getURI(datatype));
            }

            @Override
            protected boolean sameContent(ONTStatementImpl other) {
                return false;
            }
        }

        /**
         * An {@link OWLDatatypeDefinitionAxiom}
         * that either has annotations or anonymous data range expression in main triple's object position.
         * It has a public constructor since it is more generic then {@link SimpleImpl}.
         */
        public static class ComplexImpl extends AxiomImpl
                implements Complex<ComplexImpl, OWLDatatype, OWLDataRange> {

            private static final BiFunction<Triple, Supplier<OntModel>, ComplexImpl> FACTORY = ComplexImpl::new;

            protected final InternalCache.Loading<ComplexImpl, Object[]> content;

            public ComplexImpl(Triple t, Supplier<OntModel> m) {
                this(strip(t.getSubject()), t.getPredicate().getURI(), strip(t.getObject()), m);
            }

            protected ComplexImpl(Object s, String p, Object o, Supplier<OntModel> m) {
                super(s, p, o, m);
                this.content = createContentCache();
            }

            @Override
            public InternalCache.Loading<ComplexImpl, Object[]> getContentCache() {
                return content;
            }

            @Override
            protected boolean sameAs(ONTStatementImpl other) {
                if (notSame(other)) {
                    return false;
                }
                if (!sameSubject(other)) {
                    return false;
                }
                return sameContent(other);
            }

            @Override
            protected boolean sameContent(ONTStatementImpl other) {
                return other instanceof ComplexImpl && Arrays.equals(getContent(), ((ComplexImpl) other).getContent());
            }

            @Override
            public ONTObject<OWLDatatypeDefinitionAxiom> merge(ONTObject<OWLDatatypeDefinitionAxiom> other) {
                if (this == other) {
                    return this;
                }
                if (other instanceof AxiomImpl && sameTriple((AxiomImpl) other)) {
                    return this;
                }
                ComplexImpl res = new ComplexImpl(subject, predicate, object, model) {
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
