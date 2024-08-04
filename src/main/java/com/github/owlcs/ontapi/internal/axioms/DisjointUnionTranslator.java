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

import com.github.owlcs.ontapi.DataFactory;
import com.github.owlcs.ontapi.OntApiException;
import com.github.owlcs.ontapi.config.AxiomsSettings;
import com.github.owlcs.ontapi.internal.ModelObjectFactory;
import com.github.owlcs.ontapi.internal.ONTObject;
import com.github.owlcs.ontapi.internal.ONTObjectFactory;
import com.github.owlcs.ontapi.internal.objects.FactoryAccessor;
import org.apache.jena.graph.Triple;
import org.apache.jena.ontapi.model.OntClass;
import org.apache.jena.ontapi.model.OntList;
import org.apache.jena.ontapi.model.OntModel;
import org.apache.jena.ontapi.model.OntStatement;
import org.apache.jena.ontapi.utils.OntModels;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.OWL;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointUnionAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLObject;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A translator that provides {@link OWLDisjointUnionAxiom} implementations.
 * Example in turtle:
 * <pre>{@code
 * :MyClass1 owl:disjointUnionOf ( :MyClass2 [ a owl:Class ; owl:unionOf ( :MyClass3 :MyClass4  ) ] ) ;
 * }</pre>
 * <p>
 * Created by @ssz on 17.10.2016.
 *
 * @see <a href='https://www.w3.org/TR/owl2-syntax/#Disjoint_Union_of_Class_Expressions'>9.1.4 Disjoint Union of Class Expressions</a>
 */
public class DisjointUnionTranslator extends AbstractListBasedTranslator<OWLDisjointUnionAxiom, OntClass.Named,
        OWLClassExpression, OntClass, OWLClassExpression> {
    @Override
    public OWLObject getSubject(OWLDisjointUnionAxiom axiom) {
        return axiom.getOWLClass();
    }

    @Override
    public Property getPredicate() {
        return OWL.disjointUnionOf;
    }

    @Override
    public Collection<? extends OWLObject> getObjects(OWLDisjointUnionAxiom axiom) {
        return axiom.getOperandsAsList();
    }

    @Override
    Class<OntClass.Named> getView() {
        return OntClass.Named.class;
    }

    @Override
    public ONTObject<OWLDisjointUnionAxiom> toAxiomImpl(OntStatement statement,
                                                        ModelObjectFactory factory,
                                                        AxiomsSettings config) {
        return AxiomImpl.create(statement, factory, config);
    }

    @Override
    public ONTObject<OWLDisjointUnionAxiom> toAxiomWrap(OntStatement statement,
                                                        ONTObjectFactory factory,
                                                        AxiomsSettings config) {
        return makeAxiom(statement, factory::getClass, OntClass.Named::findDisjointUnion, factory::getClass, Collectors.toSet(),
                (s, m) -> factory.getOWLDataFactory().getOWLDisjointUnionAxiom(s.getOWLObject().asOWLClass(),
                        TranslateHelper.toSet(m),
                        TranslateHelper.toSet(factory.getAnnotations(statement, config))));
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.axioms.DisjointUnionAxiomImpl
     */
    @SuppressWarnings("WeakerAccess")
    public static class AxiomImpl
            extends WithListImpl<OWLDisjointUnionAxiom, OntClass>
            implements WithList.Sorted<OWLDisjointUnionAxiom, OWLClass, OWLClassExpression>, OWLDisjointUnionAxiom {

        private static final BiFunction<Triple, Supplier<OntModel>, AxiomImpl> FACTORY = AxiomImpl::new;

        protected AxiomImpl(Triple t, Supplier<OntModel> m) {
            super(t, m);
        }

        protected AxiomImpl(Object subject, String predicate, Object object, Supplier<OntModel> m) {
            super(subject, predicate, object, m);
        }

        /**
         * Creates an {@link ONTObject} container that is also {@link  OWLDisjointUnionAxiom}.
         *
         * @param statement {@link OntStatement}, not {@code null}
         * @param factory   {@link ONTObjectFactory}, not {@code null}
         * @param config    {@link AxiomsSettings}, not {@code null}
         * @return {@link AxiomImpl}
         */
        public static AxiomImpl create(OntStatement statement,
                                       ModelObjectFactory factory,
                                       AxiomsSettings config) {
            return Sorted.create(statement, FACTORY, SET_HASH_CODE, factory, config);
        }

        @Override
        protected OntList<OntClass> findList(OntStatement statement) {
            return statement.getSubject(OntClass.Named.class).findDisjointUnion(statement.getObject(RDFList.class))
                    .orElseThrow(() -> new OntApiException.IllegalState("Can't find []-list in " + statement));
        }

        @Override
        public ExtendedIterator<ONTObject<? extends OWLClassExpression>> listONTComponents(OntStatement statement,
                                                                                           ModelObjectFactory factory) {
            return OntModels.listMembers(findList(statement)).mapWith(factory::getClass);
        }

        @Override
        public OWLClass getOWLClass() {
            return getONTSubject().getOWLObject();
        }

        @Override
        public Stream<OWLClassExpression> classExpressions() {
            return operands();
        }

        @Override
        public Stream<OWLClassExpression> operands() {
            return members().map(ONTObject::getOWLObject);
        }

        @SuppressWarnings("rawtypes")
        @Override
        public ONTObject fromContentItem(Object x, ModelObjectFactory factory) {
            return x instanceof String ? findSubjectByURI((String) x, factory) : (ONTObject) x;
        }

        @Override
        public ONTObject<OWLClass> findSubjectByURI(String uri, ModelObjectFactory factory) {
            return factory.getClass(uri);
        }

        @Override
        public ONTObject<OWLClass> fetchONTSubject(OntStatement statement, ModelObjectFactory factory) {
            return findSubjectByURI(statement.getSubject().getURI(), factory);
        }

        @Override
        protected AxiomImpl makeCopy(ONTObject<OWLDisjointUnionAxiom> other) {
            return new AxiomImpl(subject, predicate, object, model) {
                @Override
                public Stream<Triple> triples() {
                    return Stream.concat(AxiomImpl.this.triples(), other.triples());
                }
            };
        }

        @FactoryAccessor
        @Override
        protected OWLDisjointUnionAxiom createAnnotatedAxiom(Object[] content,
                                                             ModelObjectFactory factory,
                                                             Collection<OWLAnnotation> annotations) {
            return getDataFactory().getOWLDisjointUnionAxiom(getFactoryClass(content, factory),
                    getFactoryMembers(content, factory), annotations);
        }

        @FactoryAccessor
        @Override
        public OWLEquivalentClassesAxiom getOWLEquivalentClassesAxiom() {
            DataFactory df = getDataFactory();
            ModelObjectFactory factory = getObjectFactory();
            Object[] content = getContent();
            return df.getOWLEquivalentClassesAxiom(getFactoryClass(content, factory),
                    df.getOWLObjectUnionOf(getFactoryMembers(content, factory)));
        }

        @FactoryAccessor
        @Override
        public OWLDisjointClassesAxiom getOWLDisjointClassesAxiom() {
            return getDataFactory().getOWLDisjointClassesAxiom(getFactoryMembers(getContent(), getObjectFactory()));
        }

        @FactoryAccessor
        protected List<OWLClassExpression> getFactoryMembers(Object[] content, ModelObjectFactory factory) {
            return members(content, factory).map(x -> eraseModel(x.getOWLObject())).collect(Collectors.toList());
        }

        @FactoryAccessor
        protected OWLClass getFactoryClass(Object[] content, ModelObjectFactory factory) {
            return eraseModel(findONTSubject(content[0], factory).getOWLObject());
        }
    }
}
