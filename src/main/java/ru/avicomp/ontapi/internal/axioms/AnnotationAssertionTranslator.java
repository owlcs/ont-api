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

import org.apache.jena.graph.BlankNodeId;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.impl.LiteralLabel;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.util.iterator.NullIterator;
import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.internal.*;
import ru.avicomp.ontapi.internal.objects.*;
import ru.avicomp.ontapi.jena.model.*;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

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
                axiom.getProperty(), axiom.getValue(), axiom.annotations());
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
    public ONTObject<OWLAnnotationAssertionAxiom> toAxiom(OntStatement statement,
                                                          Supplier<OntGraphModel> model,
                                                          InternalObjectFactory factory,
                                                          InternalConfig config) {
        return AxiomImpl.create(statement, model, factory, config);
    }

    @Override
    public ONTObject<OWLAnnotationAssertionAxiom> toAxiom(OntStatement statement,
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
     * @see ru.avicomp.ontapi.owlapi.axioms.OWLAnnotationAssertionAxiomImpl
     * @see ONTAnnotationImpl
     */
    public static abstract class AxiomImpl extends ONTAxiomImpl<OWLAnnotationAssertionAxiom>
            implements OWLAnnotationAssertionAxiom {

        protected AxiomImpl(Triple t, Supplier<OntGraphModel> m) {
            super(t, m);
        }

        /**
         * Creates an {@link OWLAnnotationAssertionAxiom} that is also {@link ONTObject}.
         * <p>
         * Impl notes: if there is no annotations associated with the given {@link OntStatement},
         * then a {@link Simple} instance is returned.
         * Otherwise the method returns a {@link WithAnnotations} instance with a cache inside.
         *
         * @param statement {@link OntStatement}, the source
         * @param model     {@link OntGraphModel}-provider
         * @param factory   {@link InternalObjectFactory}
         * @param config    {@link InternalConfig}
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
            hash = OWLObject.hashIteration(hash, res.findONTSubject(factory).hashCode());
            hash = OWLObject.hashIteration(hash, res.findONTProperty(factory).hashCode());
            hash = OWLObject.hashIteration(hash, res.findONTValue(factory).hashCode());
            return OWLObject.hashIteration(hash, hashCode(content, 0));
        }

        @Override
        public Stream<ONTObject<? extends OWLObject>> objects() {
            InternalObjectFactory factory = getObjectFactory();
            return Stream.of(findONTSubject(factory), findONTProperty(factory), findONTValue(factory));
        }

        @Override
        public OWLAnnotationSubject getSubject() {
            return getONTSubject().getOWLObject();
        }

        @Override
        public OWLAnnotationProperty getProperty() {
            return getONTProperty().getOWLObject();
        }

        @Override
        public OWLAnnotationValue getValue() {
            return getONTValue().getOWLObject();
        }

        public ONTObject<? extends OWLAnnotationSubject> getONTSubject() {
            return findONTSubject(getObjectFactory());
        }

        public ONTObject<OWLAnnotationProperty> getONTProperty() {
            return findONTProperty(getObjectFactory());
        }

        public ONTObject<? extends OWLAnnotationValue> getONTValue() {
            return findONTValue(getObjectFactory());
        }

        protected ONTObject<? extends OWLAnnotationSubject> findONTSubject(InternalObjectFactory factory) {
            return ONTAnnotationImpl.findONTSubject(this, factory);
        }

        protected ONTObject<? extends OWLAnnotationValue> findONTValue(InternalObjectFactory factory) {
            return ONTAnnotationImpl.findONTObject(this, factory);
        }

        protected ONTObject<OWLAnnotationProperty> findONTProperty(InternalObjectFactory factory) {
            return ONTAnnotationImpl.findONTPredicate(this, factory);
        }

        @FactoryAccessor
        protected OWLAnnotationProperty getFactoryProperty() {
            return eraseModel(getProperty());
        }

        @FactoryAccessor
        protected OWLAnnotationSubject getFactorySubject() {
            return eraseModel(getSubject());
        }

        @FactoryAccessor
        protected OWLAnnotationValue getFactoryValue() {
            return eraseModel(getValue());
        }

        @Override
        protected boolean sameContent(ONTStatementImpl other) {
            return false;
        }

        @FactoryAccessor
        @Override
        public OWLAnnotation getAnnotation() {
            return getDataFactory().getOWLAnnotation(getFactoryProperty(), getFactoryValue());
        }

        @Override
        public boolean isDeprecatedIRIAssertion() {
            return ONTAnnotationImpl.isDeprecated(predicate, object);
        }

        @FactoryAccessor
        @Override
        protected OWLAnnotationAssertionAxiom createAnnotatedAxiom(Collection<OWLAnnotation> annotations) {
            return getDataFactory().getOWLAnnotationAssertionAxiom(getFactoryProperty(), getFactorySubject(),
                    getFactoryValue(), annotations);
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
        public boolean canContainObjectProperties() {
            return false;
        }

        @Override
        public boolean canContainClassExpressions() {
            return false;
        }

        /**
         * An {@link OWLAnnotationAssertionAxiom} that has no sub-annotations.
         */
        public static class Simple extends AxiomImpl {

            protected Simple(Triple t, Supplier<OntGraphModel> m) {
                super(t, m);
            }

            @Override
            public boolean containsDatatype(OWLDatatype datatype) {
                return object instanceof LiteralLabel
                        && getONTValue().getOWLObject().containsEntityInSignature(datatype);
            }

            @Override
            public boolean containsAnnotationProperty(OWLAnnotationProperty property) {
                return getONTProperty().getOWLObject().equals(property);
            }

            @Override
            public Set<OWLAnonymousIndividual> getAnonymousIndividualSet() {
                Set<OWLAnonymousIndividual> res = createSortedSet();
                InternalObjectFactory factory = null;
                if (subject instanceof BlankNodeId) {
                    res.add(findAnonymousIndividual(findONTSubject(factory = getObjectFactory())));
                }
                if (object instanceof BlankNodeId) {
                    res.add(findAnonymousIndividual(findONTValue(factory == null ? getObjectFactory() : factory)));
                }
                return res;
            }

            @Override
            public Set<OWLAnnotationProperty> getAnnotationPropertySet() {
                return createSet(getProperty());
            }

            @Override
            public Set<OWLDatatype> getDatatypeSet() {
                return object instanceof LiteralLabel ? createSet(findDatatype()) : createSet();
            }

            @Override
            public Set<OWLEntity> getSignatureSet() {
                Set<OWLEntity> res = createSortedSet();
                res.add(getProperty());
                if (object instanceof LiteralLabel) {
                    res.add(findDatatype());
                }
                return res;
            }

            protected OWLDatatype findDatatype() {
                return getValue().asLiteral().orElseThrow(OntApiException.IllegalState::new).getDatatype();
            }

            protected OWLAnonymousIndividual findAnonymousIndividual(ONTObject<? extends OWLAnnotationObject> value) {
                return value.getOWLObject().asAnonymousIndividual().orElseThrow(OntApiException.IllegalState::new);
            }
        }

        /**
         * An {@link OWLAnnotationAssertionAxiom} that has sub-annotations.
         * This class has a public constructor since it is more generic then {@link Simple}.
         * Impl note: since Java does not allow multiple inheritance, copy-paste cannot be avoided here...
         *
         * @see ONTAnnotationImpl.WithAnnotations
         */
        public static class WithAnnotations extends AxiomImpl implements WithContent<WithAnnotations> {
            protected final InternalCache.Loading<WithAnnotations, Object[]> content;

            public WithAnnotations(Triple t, Supplier<OntGraphModel> m) {
                super(t, m);
                this.content = createContentCache();
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
            public InternalCache.Loading<WithAnnotations, Object[]> getContentCache() {
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
        }
    }

}
