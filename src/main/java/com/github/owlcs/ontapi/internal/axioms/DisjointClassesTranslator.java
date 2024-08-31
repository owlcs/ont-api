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

import com.github.owlcs.ontapi.OntApiException;
import com.github.owlcs.ontapi.config.AxiomsSettings;
import com.github.owlcs.ontapi.internal.InternalCache;
import com.github.owlcs.ontapi.internal.ModelObjectFactory;
import com.github.owlcs.ontapi.internal.ONTObject;
import com.github.owlcs.ontapi.internal.ONTObjectFactory;
import com.github.owlcs.ontapi.internal.OntModelSupport;
import com.github.owlcs.ontapi.internal.WriteHelper;
import com.github.owlcs.ontapi.internal.objects.FactoryAccessor;
import com.github.owlcs.ontapi.internal.objects.ONTEntityImpl;
import com.github.owlcs.ontapi.internal.objects.ONTStatementImpl;
import org.apache.jena.graph.FrontsTriple;
import org.apache.jena.graph.Triple;
import org.apache.jena.ontapi.OntModelControls;
import org.apache.jena.ontapi.model.OntClass;
import org.apache.jena.ontapi.model.OntDisjoint;
import org.apache.jena.ontapi.model.OntModel;
import org.apache.jena.ontapi.model.OntStatement;
import org.apache.jena.ontapi.utils.OntModels;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.OWL;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A translator that provides {@link OWLDisjointClassesAxiom} implementations.
 * See {@link AbstractTwoWayNaryTranslator}.
 * Example:
 * <ul>
 * <li>{@code :Complex2 owl:disjointWith  :Simple2 , :Simple1 . }</li>
 * <li>OWL2 alternative way:
 * {@code [ a owl:AllDisjointClasses ; owl:members ( :Complex2 :Simple1 :Simple2 ) ] . }</li>
 * </ul>
 * <p>
 * Created by @ssz on 28.09.2016.
 */
public class DisjointClassesTranslator
        extends AbstractTwoWayNaryTranslator<OWLDisjointClassesAxiom, OWLClassExpression, OntClass> {

    private static final Property PREDICATE = OWL.disjointWith;

    @Override
    Property getPredicate() {
        return PREDICATE;
    }

    @Override
    Class<OntClass> getView() {
        return OntClass.class;
    }

    @Override
    List<OWLClassExpression> operandsAsList(OWLDisjointClassesAxiom axiom, OntModel model) {
        List<OWLClassExpression> operands = axiom.getOperandsAsList();
        if (operands.isEmpty()) {
            return operands;
        }
        List<OntClass> classes = operands.stream().map(it -> (OntClass)WriteHelper.addRDFNode(model, it)).toList();
        if (classes.size() < 2 || !classes.stream().allMatch(OntClass::canAsDisjointClass)) {
            throw new OntApiException.Unsupported(
                    axiom + " cannot be added: prohibited by the profile " + OntModelSupport.profileName(model)
            );
        }
        return operands;
    }

    @Override
    OntModelControls control() {
        return OntModelControls.USE_OWL_CLASS_DISJOINT_WITH_FEATURE;
    }

    @Override
    boolean filter(Statement statement) {
        return super.filter(statement) &&
                statement.getSubject().as(OntClass.class).canAsDisjointClass() &&
                statement.getObject().as(OntClass.class).canAsDisjointClass();
    }

    @Override
    Resource getMembersType() {
        return OWL.AllDisjointClasses;
    }

    @Override
    Property getMembersPredicate() {
        return OWL.members;
    }

    @Override
    Class<OntDisjoint.Classes> getDisjointView() {
        return OntDisjoint.Classes.class;
    }

    @Override
    public ONTObject<OWLDisjointClassesAxiom> toAxiomImpl(OntStatement statement,
                                                          ModelObjectFactory factory,
                                                          AxiomsSettings config) {
        return AxiomImpl.create(statement, factory, config);
    }

    @Override
    public ONTObject<OWLDisjointClassesAxiom> toAxiomWrap(OntStatement statement,
                                                          ONTObjectFactory factory,
                                                          AxiomsSettings config) {
        return makeAxiom(statement, factory.getAnnotations(statement, config),
                factory::getClass,
                (members, annotations) -> factory.getOWLDataFactory()
                        .getOWLDisjointClassesAxiom(TranslateHelper.toSet(members), TranslateHelper.toSet(annotations)));
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.axioms.DisjointClassesAxiomImpl
     */
    public abstract static class AxiomImpl extends ClassNaryAxiomImpl<OWLDisjointClassesAxiom>
            implements OWLDisjointClassesAxiom {

        protected AxiomImpl(Triple t, Supplier<OntModel> m) {
            this(strip(t.getSubject()), t.getPredicate().getURI(), strip(t.getObject()), m);
        }

        protected AxiomImpl(Object s, String p, Object o, Supplier<OntModel> m) {
            super(s, p, o, m);
        }

        /**
         * Creates an {@link ONTObject} container, that is also {@link OWLDisjointClassesAxiom}.
         *
         * @param statement {@link OntStatement}, not {@code null}
         * @param factory   {@link ONTObjectFactory}, not {@code null}
         * @param config    {@link AxiomsSettings}, not {@code null}
         * @return {@link AxiomImpl}
         */
        public static AxiomImpl create(OntStatement statement,
                                       ModelObjectFactory factory,
                                       AxiomsSettings config) {
            if (PREDICATE.equals(statement.getPredicate())) {
                return WithManyObjects.create(statement,
                        SimpleImpl.FACTORY, ComplexImpl.FACTORY, SET_HASH_CODE, factory, config);
            }
            return WithManyObjects.create(statement, ComplexImpl.FACTORY, SET_HASH_CODE, factory, config);
        }

        @Override
        public ExtendedIterator<ONTObject<? extends OWLClassExpression>> listONTComponents(OntStatement statement,
                                                                                           ModelObjectFactory factory) {
            if (PREDICATE.equals(statement.getPredicate())) {
                return super.listONTComponents(statement, factory);
            }
            return OntModels.listMembers(statement.getSubject(OntDisjoint.Classes.class).getList())
                    .mapWith(factory::getClass);
        }

        @FactoryAccessor
        @Override
        protected OWLDisjointClassesAxiom createAxiom(Collection<OWLClassExpression> members,
                                                      Collection<OWLAnnotation> annotations) {
            return getDataFactory().getOWLDisjointClassesAxiom(members,
                    annotations == null ? NO_ANNOTATIONS : annotations);
        }

        @FactoryAccessor
        @Override
        protected OWLSubClassOfAxiom createSubClassOf(OWLClassExpression a, OWLClassExpression b) {
            return getDataFactory().getOWLSubClassOfAxiom(a, b.getObjectComplementOf());
        }

        /**
         * An {@link OWLDisjointClassesAxiom} that is based on a single unannotated triple {@code s owl:disjointWith o},
         * where {@code s} and {@code o} are named classes (URI {@link Resource}s).
         */
        protected static class SimpleImpl extends AxiomImpl implements Simple<OWLClassExpression> {

            private static final BiFunction<Triple, Supplier<OntModel>, SimpleImpl> FACTORY = SimpleImpl::new;

            protected SimpleImpl(Triple t, Supplier<OntModel> m) {
                super(t, m);
            }

            protected SimpleImpl(Object s, String p, Object o, Supplier<OntModel> m) {
                super(s, p, o, m);
            }

            @Override
            protected long count() {
                return 2;
            }

            @Override
            protected boolean sameContent(ONTStatementImpl other) {
                return other instanceof SimpleImpl && isReverseTriple((SimpleImpl) other);
            }

            @Override
            protected AxiomImpl makeCopyWith(ONTObject<OWLDisjointClassesAxiom> other) {
                if (other instanceof SimpleImpl) {
                    Triple t = ((SimpleImpl) other).asTriple();
                    return new SimpleImpl(subject, predicate, object, model) {

                        @Override
                        public Stream<Triple> triples() {
                            return Stream.concat(SimpleImpl.this.triples(), Stream.of(t));
                        }
                    };
                }
                return new SimpleImpl(subject, predicate, object, model) {
                    @Override
                    public Stream<Triple> triples() {
                        return Stream.concat(SimpleImpl.this.triples(), other.triples());
                    }
                };
            }

            @SuppressWarnings("unchecked")
            @Override
            public Set<OWLClass> getNamedClassSet() {
                return (Set<OWLClass>) getOWLComponentsAsSet();
            }

            @SuppressWarnings("unchecked")
            @Override
            public Set<OWLClassExpression> getClassExpressionSet() {
                return (Set<OWLClassExpression>) getOWLComponentsAsSet();
            }

            @SuppressWarnings("unchecked")
            @Override
            public Set<OWLEntity> getSignatureSet() {
                return (Set<OWLEntity>) getOWLComponentsAsSet();
            }

            @Override
            public boolean containsNamedClass(OWLClass clazz) {
                return hasURIResource(ONTEntityImpl.getURI(clazz));
            }

            @Override
            public boolean canContainDatatypes() {
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
            public boolean canContainObjectProperties() {
                return false;
            }

            @Override
            public boolean canContainAnonymousIndividuals() {
                return false;
            }
        }

        /**
         * An {@link OWLDisjointClassesAxiom}
         * that either has annotations or anonymous class expressions in subject or object positions,
         * or it is based on {@link OntDisjoint.Classes} resource.
         * It has a public constructor since it is more generic then {@link SimpleImpl}.
         */
        public static class ComplexImpl extends AxiomImpl implements Complex<ComplexImpl, OWLClassExpression> {

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
            protected long count() {
                return isSimple() ? 2 : members().count();
            }

            boolean isSimple() {
                return predicate.equals(PREDICATE.getURI());
            }

            protected OntDisjoint.Classes asResource() {
                return getPersonalityModel().getNodeAs(getSubjectNode(), OntDisjoint.Classes.class);
            }

            @Override
            public Stream<Triple> triples() {
                if (isSimple()) {
                    return super.triples();
                }
                return Stream.concat(asResource().spec().map(FrontsTriple::asTriple),
                        objects().flatMap(ONTObject::triples));
            }

            @Override
            public OntStatement asStatement() {
                if (isSimple()) {
                    return super.asStatement();
                }
                return asResource().getMainStatement();
            }

            @Override
            public InternalCache.Loading<ComplexImpl, Object[]> getContentCache() {
                return content;
            }

            @Override
            protected boolean sameContent(ONTStatementImpl other) {
                return testSameContent(other);
            }

            @Override
            protected ComplexImpl makeCopyWith(ONTObject<OWLDisjointClassesAxiom> other) {
                ComplexImpl res = new ComplexImpl(subject, predicate, object, model) {
                    @Override
                    public Stream<Triple> triples() {
                        return Stream.concat(ComplexImpl.this.triples(), other.triples());
                    }
                };
                if (hasContent()) {
                    res.putContent(getContent());
                }
                return res;
            }
        }
    }
}
