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

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.util.iterator.NullIterator;
import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.internal.*;
import ru.avicomp.ontapi.internal.objects.*;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.impl.Entities;
import ru.avicomp.ontapi.jena.model.OntEntity;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntObject;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.utils.OntModels;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * It is a translator for axioms of the {@link AxiomType#DECLARATION} type.
 * Each non-builtin {@link OWLEntity entity} must have a declaration.
 * The entity declaration is a simple triplet with {@code rdf:type} predicate,
 * in OWL2 the subject and object of that triple are IRIs.
 * <p>
 * Created by @szuev on 28.09.2016.
 */
@SuppressWarnings("WeakerAccess")
public class DeclarationTranslator extends AxiomTranslator<OWLDeclarationAxiom> {

    @Override
    public void write(OWLDeclarationAxiom axiom, OntGraphModel model) {
        WriteHelper.writeDeclarationTriple(model, axiom.getEntity(), RDF.type,
                WriteHelper.getRDFType(axiom.getEntity()), axiom.annotations());
    }

    @Override
    public ExtendedIterator<OntStatement> listStatements(OntGraphModel model, InternalConfig config) {
        if (!config.isAllowReadDeclarations()) return NullIterator.instance();
        // this way is used for two reasons:
        // 1) performance (union of several find operation for the pattern [ANY,rdf:type,Resource] is faster
        // then single find operation [ANY,rdf:type,ANY] and subsequent filter)
        // 2) to filter out punnings using standard entity factories
        return OntModels.listLocalEntities(model).mapWith(OntObject::getRoot).filterDrop(Objects::isNull);
    }

    @Override
    public boolean testStatement(OntStatement statement, InternalConfig config) {
        if (!statement.getSubject().isURIResource()) return false;
        if (!statement.getObject().isURIResource()) return false;
        if (!statement.isDeclaration()) return false;
        // again. this way is used to restrict illegal punnings
        return Entities.find(statement.getResource())
                .map(Entities::getActualType)
                .map(t -> statement.getModel().getOntEntity(t, statement.getSubject()))
                .isPresent();
    }

    @Override
    public ONTObject<OWLDeclarationAxiom> toAxiom(OntStatement statement,
                                                  Supplier<OntGraphModel> model,
                                                  InternalObjectFactory factory,
                                                  InternalConfig config) {
        return AxiomImpl.create(statement, model, factory, config);
    }

    @Override
    public ONTObject<OWLDeclarationAxiom> toAxiom(OntStatement statement,
                                                  InternalObjectFactory reader,
                                                  InternalConfig config) {
        OntEntity e = Entities.find(statement.getResource())
                .map(Entities::getActualType)
                .map(t -> statement.getModel().getOntEntity(t, statement.getSubject()))
                .orElseThrow(() -> new OntJenaException.IllegalArgument("Can't find entity by the statement " + statement));
        ONTObject<? extends OWLEntity> entity = reader.getEntity(e);
        Collection<ONTObject<OWLAnnotation>> annotations = reader.getAnnotations(statement, config);
        OWLDeclarationAxiom res = reader.getOWLDataFactory().getOWLDeclarationAxiom(entity.getOWLObject(),
                ONTObject.toSet(annotations));
        return ONTWrapperImpl.create(res, statement).append(annotations);
    }

    /**
     * @see ru.avicomp.ontapi.owlapi.axioms.OWLDeclarationAxiomImpl
     */
    public abstract static class AxiomImpl extends ONTBaseAxiomImpl<OWLDeclarationAxiom>
            implements ONTObject<OWLDeclarationAxiom>, OWLDeclarationAxiom {

        protected AxiomImpl(Object subject, String predicate, Object object, Supplier<OntGraphModel> m) {
            super(subject, predicate, object, m);
        }

        /**
         * Creates an {@link OWLDeclarationAxiom} that is also {@link ONTObject}.
         *
         * Impl notes: if there is no annotations associated with the given {@link OntStatement},
         * then a {@link Simple} instance is returned.
         * Otherwise the method returns a {@link WithAnnotations} instance with a cache inside.
         *
         * @param statement  {@link OntStatement}, the source
         * @param model  {@link OntGraphModel}-provider
         * @param factory {@link InternalObjectFactory}
         * @param config  {@link InternalConfig}
         * @return {@link AxiomImpl}
         */
        public static AxiomImpl create(OntStatement statement,
                                       Supplier<OntGraphModel> model,
                                       InternalObjectFactory factory,
                                       InternalConfig config) {
            Object[] content = WithAnnotations.collectContent(statement, factory, config);
            AxiomImpl res;
            if (content == EMPTY) {
                res = new Simple(statement.asTriple(), model);
            } else {
                res = WithContent.addContent(new WithAnnotations(statement.asTriple(), model), content);
            }
            res.hashCode = collectHashCode(res, factory, content);
            return res;
        }

        private static int collectHashCode(AxiomImpl res,
                                           InternalObjectFactory factory,
                                           Object[] content) {
            int hash = res.hashIndex();
            hash = OWLObject.hashIteration(hash, res.findONTEntity(factory).hashCode());
            return OWLObject.hashIteration(hash, hashCode(content, 0));
        }

        @Override
        public OntStatement asStatement() {
            return OntApiException.mustNotBeNull(getResource().getRoot());
        }

        @Override
        public OWLDeclarationAxiom getOWLObject() {
            return this;
        }

        @Override
        public Stream<ONTObject<? extends OWLObject>> objects() {
            return Stream.of(getONTEntity());
        }

        @Override
        public OWLEntity getEntity() {
            return getONTEntity().getOWLObject();
        }

        public ONTObject<? extends OWLEntity> getONTEntity() {
            return findONTEntity(getObjectFactory());
        }

        protected ONTObject<? extends OWLEntity> findONTEntity(InternalObjectFactory factory) {
            if (factory instanceof ModelObjectFactory) {
                return ((ModelObjectFactory) factory).getEntity((String) subject, getResourceType());
            }
            return factory.getEntity(getResource());
        }

        /**
         * Returns an {@link OntEntity}, which is a {@link org.apache.jena.rdf.model.Resource Jena Resource}.
         *
         * @return {@link OntEntity}
         */
        public OntEntity getResource() {
            Node s = getSubjectNode();
            Class<? extends OntEntity> t = getResourceType().getActualType();
            return OntApiException.mustNotBeNull(getPersonalityModel()
                    .findNodeAs(s, t), "Can't find entity " + subject);
        }

        /**
         * Returns an entity type from jena subsystem.
         *
         * @return {@link Entities}
         */
        public Entities getResourceType() {
            Node type = getObjectNode();
            return Entities.find(type)
                    .orElseThrow(() -> new OntApiException.IllegalState("Can't find type for " + subject));
        }

        @FactoryAccessor
        @Override
        protected OWLDeclarationAxiom createAnnotatedAxiom(Collection<OWLAnnotation> annotations) {
            return getDataFactory().getOWLDeclarationAxiom(getEntity(), annotations);
        }

        @Override
        protected boolean sameContent(ONTBaseTripleImpl other) {
            return false;
        }

        @Override
        public Set<OWLClass> getNamedClassSet() {
            OWLEntity res = getEntity();
            return res.isOWLClass() ? createSet(res.asOWLClass()) : createSet();
        }

        @Override
        public Set<OWLClassExpression> getClassExpressionSet() {
            OWLEntity res = getEntity();
            return res.isOWLClass() ? createSet(res.asOWLClass()) : createSet();
        }

        @Override
        public Set<OWLNamedIndividual> getNamedIndividualSet() {
            OWLEntity res = getEntity();
            return res.isOWLNamedIndividual() ? createSet(res.asOWLNamedIndividual()) : createSet();
        }

        @Override
        public Set<OWLDataProperty> getDataPropertySet() {
            OWLEntity res = getEntity();
            return res.isOWLDataProperty() ? createSet(res.asOWLDataProperty()) : createSet();
        }

        @Override
        public Set<OWLObjectProperty> getObjectPropertySet() {
            OWLEntity res = getEntity();
            return res.isOWLObjectProperty() ? createSet(res.asOWLObjectProperty()) : createSet();
        }

        /**
         * An {@link OWLDeclarationAxiom} that has no annotations.
         */
        public static class Simple extends AxiomImpl {

            protected Simple(Triple t, Supplier<OntGraphModel> m) {
                this(strip(t.getSubject()), t.getPredicate().getURI(), strip(t.getObject()), m);
            }

            protected Simple(Object subject, String predicate, Object object, Supplier<OntGraphModel> m) {
                super(subject, predicate, object, m);
            }

            @Override
            public boolean containsEntity(OWLEntity entity) {
                return getEntity().equals(entity);
            }

            @Override
            public Set<OWLEntity> getSignatureSet() {
                return createSet(getEntity());
            }

            @Override
            public Set<OWLDatatype> getDatatypeSet() {
                OWLEntity res = getEntity();
                return res.isOWLDatatype() ? createSet(res.asOWLDatatype()) : createSet();
            }

            @Override
            public Set<OWLAnnotationProperty> getAnnotationPropertySet() {
                OWLEntity res = getEntity();
                return res.isOWLAnnotationProperty() ? createSet(res.asOWLAnnotationProperty()) : createSet();
            }

            @Override
            public boolean canContainAnonymousIndividuals() {
                return false;
            }
        }

        /**
         * An {@link OWLDeclarationAxiom} that has annotations.
         * This class has a public constructor since it is more generic then {@link Simple}.
         * TODO: Can't avoid copy-paste...
         *
         * @see ONTAnnotationImpl.WithAnnotations
         */
        public static class WithAnnotations extends AxiomImpl implements WithContent<WithAnnotations> {
            protected final InternalCache.Loading<WithAnnotations, Object[]> content;

            public WithAnnotations(Triple t, Supplier<OntGraphModel> m) {
                this(strip(t.getSubject()), t.getPredicate().getURI(), strip(t.getObject()), m);
            }

            protected WithAnnotations(Object subject, String predicate, Object object, Supplier<OntGraphModel> m) {
                super(subject, predicate, object, m);
                this.content = createContent();
            }

            protected static Object[] collectContent(OntStatement statement,
                                                     InternalObjectFactory factory,
                                                     InternalConfig config) {
                return toArray(collectAnnotations(statement, factory, config));
            }

            @Override
            public Object[] collectContent() {
                return collectContent(asStatement(), getObjectFactory(), getConfig());
            }

            @Override
            public Object[] getContent() {
                return content.get(this);
            }

            @Override
            public boolean isAnnotated() {
                return true;
            }

            @SuppressWarnings("unchecked")
            @Override
            public Stream<OWLAnnotation> annotations() {
                Stream res = Arrays.stream(getContent());
                return (Stream<OWLAnnotation>) res;
            }

            @SuppressWarnings("unchecked")
            @Override
            public List<OWLAnnotation> annotationsAsList() {
                List res = Arrays.asList(getContent());
                return (List<OWLAnnotation>) Collections.unmodifiableList(res);
            }

            @SuppressWarnings("unchecked")
            @Override
            public Stream<ONTObject<? extends OWLObject>> objects() {
                Stream res = Stream.concat(super.objects(), annotations());
                return (Stream<ONTObject<? extends OWLObject>>) res;
            }

            @Override
            public void putContent(Object[] content) {
                this.content.put(this, content);
            }

            @Override
            public boolean hasContent() {
                return !content.isEmpty();
            }

            @Override
            public void clearContent() {
                content.clear();
            }

            @Override
            public boolean containsEntity(OWLEntity entity) {
                if (entity.isOWLAnnotationProperty() || entity.isOWLDatatype()) {
                    return super.containsEntity(entity);
                }
                return getEntity().equals(entity);
            }
        }
    }
}
