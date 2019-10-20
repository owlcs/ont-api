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

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
                WriteHelper.getRDFType(axiom.getEntity()), axiom.annotationsAsList());
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
                                                  InternalObjectFactory factory,
                                                  InternalConfig config) {
        OntEntity e = Entities.find(statement.getResource())
                .map(Entities::getActualType)
                .map(t -> statement.getModel().getOntEntity(t, statement.getSubject()))
                .orElseThrow(() -> new OntJenaException.IllegalArgument("Can't find entity by the statement " + statement));
        ONTObject<? extends OWLEntity> entity = factory.getEntity(e);
        Collection<ONTObject<OWLAnnotation>> annotations = factory.getAnnotations(statement, config);
        OWLDeclarationAxiom res = factory.getOWLDataFactory().getOWLDeclarationAxiom(entity.getOWLObject(),
                ONTObject.toSet(annotations));
        return ONTWrapperImpl.create(res, statement).append(annotations);
    }

    /**
     * @see ru.avicomp.ontapi.owlapi.axioms.OWLDeclarationAxiomImpl
     */
    public abstract static class AxiomImpl extends ONTAxiomImpl<OWLDeclarationAxiom> implements OWLDeclarationAxiom {

        protected AxiomImpl(Triple t, Supplier<OntGraphModel> m) {
            super(t, m);
        }

        /**
         * Creates an {@link OWLDeclarationAxiom} that is also {@link ONTObject}.
         *
         * Impl notes: if there is no annotations associated with the given {@link OntStatement},
         * then a {@link SimpleImpl} instance is returned.
         * Otherwise the method returns a {@link WithAnnotationsImpl} instance with a cache inside.
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
            Collection annotations = ONTAxiomImpl.collectAnnotations(statement, factory, config);
            if (annotations.isEmpty()) {
                SimpleImpl res = new SimpleImpl(statement.asTriple(), model);
                int hash = OWLObject.hashIteration(res.hashIndex(), res.findONTEntity(factory).hashCode());
                res.hashCode = OWLObject.hashIteration(hash, 1);
                return res;
            }
            WithAnnotationsImpl res = new WithAnnotationsImpl(statement.asTriple(), model);
            int hash = OWLObject.hashIteration(res.hashIndex(), res.findONTEntity(factory).hashCode());
            if (annotations.isEmpty()) {
                res.hashCode = OWLObject.hashIteration(hash, 1);
                return res;
            }
            int h = 1;
            int index = 0;
            Object[] content = new Object[annotations.size()];
            for (Object a : annotations) {
                content[index++] = a;
                h = WithContent.hashIteration(h, a.hashCode());
            }
            res.hashCode = OWLObject.hashIteration(hash, h);
            res.putContent(content);
            return res;
        }

        @Override
        public OntStatement asStatement() {
            return OntApiException.mustNotBeNull(getResource().getRoot());
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
            return getDataFactory().getOWLDeclarationAxiom(eraseModel(getEntity()), annotations);
        }

        @Override
        protected boolean sameContent(ONTStatementImpl other) {
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
        public static class SimpleImpl extends AxiomImpl implements WithoutAnnotations {

            protected SimpleImpl(Triple t, Supplier<OntGraphModel> m) {
                super(t, m);
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

            @Override
            public boolean isAnnotated() {
                return false;
            }
        }

        /**
         * An {@link OWLDeclarationAxiom} that has annotations.
         * This class has a public constructor since it is more generic then {@link SimpleImpl}.
         * Impl note: since Java does not allow multiple inheritance, copy-paste cannot be avoided here...
         *
         * @see ONTAnnotationImpl.WithAnnotationsImpl
         */
        public static class WithAnnotationsImpl extends AxiomImpl implements WithContent<WithAnnotationsImpl> {
            protected final InternalCache.Loading<WithAnnotationsImpl, Object[]> content;

            public WithAnnotationsImpl(Triple t, Supplier<OntGraphModel> m) {
                super(t, m);
                this.content = createContentCache();
            }

            @Override
            public Object[] collectContent() {
                return collectAnnotations(asStatement(), getObjectFactory(), getConfig()).toArray();
            }

            @Override
            public InternalCache.Loading<WithAnnotationsImpl, Object[]> getContentCache() {
                return content;
            }

            @Override
            public boolean isAnnotated() {
                return true;
            }

            @Override
            public Stream<OWLAnnotation> annotations() {
                return ONTAnnotationImpl.contentAsStream(getContent());
            }

            @Override
            public List<OWLAnnotation> annotationsAsList() {
                return ONTAnnotationImpl.contentAsList(getContent());
            }

            @SuppressWarnings("unchecked")
            @Override
            public Stream<ONTObject<? extends OWLObject>> objects() {
                Stream res = Stream.concat(super.objects(), annotations());
                return (Stream<ONTObject<? extends OWLObject>>) res;
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
