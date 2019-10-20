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

package ru.avicomp.ontapi.internal.axioms;

import org.apache.jena.graph.FrontsTriple;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.internal.InternalCache;
import ru.avicomp.ontapi.internal.InternalConfig;
import ru.avicomp.ontapi.internal.InternalObjectFactory;
import ru.avicomp.ontapi.internal.ONTObject;
import ru.avicomp.ontapi.internal.objects.FactoryAccessor;
import ru.avicomp.ontapi.internal.objects.ONTEntityImpl;
import ru.avicomp.ontapi.internal.objects.ONTStatementImpl;
import ru.avicomp.ontapi.jena.model.OntCE;
import ru.avicomp.ontapi.jena.model.OntDisjoint;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.utils.OntModels;
import ru.avicomp.ontapi.jena.vocabulary.OWL;

import java.util.Arrays;
import java.util.Collection;
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
 * Created by @szuev on 28.09.2016.
 */
public class DisjointClassesTranslator
        extends AbstractTwoWayNaryTranslator<OWLDisjointClassesAxiom, OWLClassExpression, OntCE> {

    @Override
    Property getPredicate() {
        return OWL.disjointWith;
    }

    @Override
    Class<OntCE> getView() {
        return OntCE.class;
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
    public ONTObject<OWLDisjointClassesAxiom> toAxiom(OntStatement statement,
                                                      Supplier<OntGraphModel> model,
                                                      InternalObjectFactory factory,
                                                      InternalConfig config) {
        return AxiomImpl.create(statement, model, factory, config);
    }

    @Override
    public ONTObject<OWLDisjointClassesAxiom> toAxiom(OntStatement statement,
                                                      InternalObjectFactory factory,
                                                      InternalConfig config) {
        return makeAxiom(statement, factory.getAnnotations(statement, config),
                factory::getClass,
                (members, annotations) -> factory.getOWLDataFactory()
                        .getOWLDisjointClassesAxiom(ONTObject.toSet(members), ONTObject.toSet(annotations)));
    }

    /**
     * @see ru.avicomp.ontapi.owlapi.axioms.OWLDisjointClassesAxiomImpl
     */
    public abstract static class AxiomImpl extends ClassNaryAxiomImpl<OWLDisjointClassesAxiom>
            implements OWLDisjointClassesAxiom {

        protected AxiomImpl(Triple t, Supplier<OntGraphModel> m) {
            this(strip(t.getSubject()), t.getPredicate().getURI(), strip(t.getObject()), m);
        }

        protected AxiomImpl(Object s, String p, Object o, Supplier<OntGraphModel> m) {
            super(s, p, o, m);
        }

        /**
         * Creates an {@link ONTObject} container, that is also {@link OWLDisjointClassesAxiom}.
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
            if (OWL.disjointWith.equals(statement.getPredicate())) {
                return WithManyObjects.create(statement, model,
                        SimpleImpl.FACTORY, ComplexImpl.FACTORY, SET_HASH_CODE, factory, config);
            }
            return WithManyObjects.create(statement, model, ComplexImpl.FACTORY, SET_HASH_CODE, factory, config);
        }

        @Override
        public ExtendedIterator<ONTObject<? extends OWLClassExpression>> listONTComponents(OntStatement statement,
                                                                                           InternalObjectFactory factory) {
            if (OWL.disjointWith.equals(statement.getPredicate())) {
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

            private static final BiFunction<Triple, Supplier<OntGraphModel>, SimpleImpl> FACTORY = SimpleImpl::new;

            protected SimpleImpl(Triple t, Supplier<OntGraphModel> m) {
                super(t, m);
            }

            protected SimpleImpl(Object s, String p, Object o, Supplier<OntGraphModel> m) {
                super(s, p, o, m);
            }

            @Override
            protected long count() {
                return 2;
            }

            @Override
            protected boolean sameContent(ONTStatementImpl other) {
                // triple is checked above in trace
                return other instanceof SimpleImpl
                        && subject.equals(((AxiomImpl) other).getObjectURI())
                        && object.equals(((AxiomImpl) other).getSubjectURI());
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
                String uri = ONTEntityImpl.getURI(clazz);
                return subject.equals(uri) || object.equals(uri);
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
            public boolean canContainAnnotationProperties() {
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
        public static class ComplexImpl extends AxiomImpl
                implements Complex<ComplexImpl, OWLClassExpression> {

            private static final BiFunction<Triple, Supplier<OntGraphModel>, ComplexImpl> FACTORY = ComplexImpl::new;
            protected final InternalCache.Loading<ComplexImpl, Object[]> content;

            public ComplexImpl(Triple t, Supplier<OntGraphModel> m) {
                this(strip(t.getSubject()), t.getPredicate().getURI(), strip(t.getObject()), m);
            }

            protected ComplexImpl(Object s, String p, Object o, Supplier<OntGraphModel> m) {
                super(s, p, o, m);
                this.content = createContentCache();
            }

            @Override
            protected long count() {
                return isSimple() ? 2 : members().count();
            }

            boolean isSimple() {
                return predicate.equals(OWL.disjointWith.getURI());
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
                return asResource().getRoot();
            }

            @Override
            public InternalCache.Loading<ComplexImpl, Object[]> getContentCache() {
                return content;
            }

            @Override
            protected boolean sameContent(ONTStatementImpl other) {
                if (other instanceof ComplexImpl) {
                    return Arrays.equals(getContent(), ((ComplexImpl) other).getContent());
                }
                if (other instanceof SimpleImpl) {
                    InternalObjectFactory factory = getObjectFactory();
                    return equalIterators(objects(factory).iterator(),
                            ((SimpleImpl) other).objects(factory).iterator());
                }
                return false;
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
