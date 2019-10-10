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
import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.DataFactory;
import ru.avicomp.ontapi.internal.*;
import ru.avicomp.ontapi.internal.objects.FactoryAccessor;
import ru.avicomp.ontapi.internal.objects.ONTDataPropertyImpl;
import ru.avicomp.ontapi.internal.objects.ONTEntityImpl;
import ru.avicomp.ontapi.internal.objects.ONTLiteralImpl;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntNPA;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.owlapi.objects.OWLLiteralImpl;

import java.util.Collection;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A translator that provides {@link OWLNegativeDataPropertyAssertionAxiom} implementations.
 * Example:
 * <pre>{@code
 * [ a owl:NegativePropertyAssertion; owl:sourceIndividual :ind1; owl:assertionProperty :dataProp; owl:targetValue "TEST"^^xsd:string ]
 * }</pre>
 * Created by szuev on 12.10.2016.
 */
public class NegativeDataPropertyAssertionTranslator
        extends AbstractNegativePropertyAssertionTranslator<OWLNegativeDataPropertyAssertionAxiom, OntNPA.DataAssertion> {

    @Override
    OntNPA.DataAssertion createNPA(OWLNegativeDataPropertyAssertionAxiom axiom, OntGraphModel model) {
        return WriteHelper.addDataProperty(model, axiom.getProperty())
                .addNegativeAssertion(WriteHelper.addIndividual(model, axiom.getSubject()),
                        WriteHelper.addLiteral(model, axiom.getObject()));
    }

    @Override
    Class<OntNPA.DataAssertion> getView() {
        return OntNPA.DataAssertion.class;
    }

    @Override
    public ONTObject<OWLNegativeDataPropertyAssertionAxiom> toAxiom(OntStatement statement,
                                                                    Supplier<OntGraphModel> model,
                                                                    InternalObjectFactory factory,
                                                                    InternalConfig config) {
        return AxiomImpl.create(statement, model, factory, config);
    }

    @Override
    public ONTObject<OWLNegativeDataPropertyAssertionAxiom> toAxiom(OntStatement statement,
                                                                    InternalObjectFactory factory,
                                                                    InternalConfig config) {
        OntNPA.DataAssertion npa = statement.getSubject(getView());
        ONTObject<? extends OWLIndividual> s = factory.getIndividual(npa.getSource());
        ONTObject<OWLDataProperty> p = factory.getProperty(npa.getProperty());
        ONTObject<OWLLiteral> o = factory.getLiteral(npa.getTarget());
        Collection<ONTObject<OWLAnnotation>> annotations = factory.getAnnotations(statement, config);
        OWLNegativeDataPropertyAssertionAxiom res = factory.getOWLDataFactory()
                .getOWLNegativeDataPropertyAssertionAxiom(p.getOWLObject(),
                        s.getOWLObject(), o.getOWLObject(), ONTObject.toSet(annotations));
        return ONTWrapperImpl.create(res, npa).append(annotations).append(s).append(p).append(o);
    }

    /**
     * @see ru.avicomp.ontapi.owlapi.axioms.OWLNegativeDataPropertyAssertionAxiomImpl
     */
    public static class AxiomImpl
            extends NegativeAssertionImpl<OntNPA.DataAssertion, OWLNegativeDataPropertyAssertionAxiom,
            OWLDataPropertyExpression, OWLLiteral>
            implements OWLNegativeDataPropertyAssertionAxiom {

        private static final BiFunction<Triple, Supplier<OntGraphModel>, AxiomImpl> FACTORY = AxiomImpl::new;

        public AxiomImpl(Triple t, Supplier<OntGraphModel> m) {
            super(t, m);
        }

        /**
         * Creates an {@link OWLNegativeDataPropertyAssertionAxiom} that is also {@link ONTObject}.
         *
         * @param statement {@link OntStatement}, the source, not {@code null}
         * @param model     {@link OntGraphModel}-provider, not {@code null}
         * @param factory   {@link InternalObjectFactory}, not {@code null}
         * @param config    {@link InternalConfig}, not {@code null}
         * @return {@link AxiomImpl}
         */
        public static AxiomImpl create(OntStatement statement,
                                       Supplier<OntGraphModel> model,
                                       InternalObjectFactory factory,
                                       InternalConfig config) {
            return WithAssertion.create(statement, model, FACTORY, SET_HASH_CODE, factory, config);
        }

        protected AxiomImpl(Object s, String p, Object o, Supplier<OntGraphModel> m) {
            super(s, p, o, m);
        }

        @Override
        public Class<OntNPA.DataAssertion> getType() {
            return OntNPA.DataAssertion.class;
        }

        @Override
        public Object fromObject(ONTObject o) {
            return OWLLiteralImpl.asONT((OWLLiteral) o.getOWLObject()).getLiteralLabel();
        }

        @Override
        public ONTObject<? extends OWLLiteral> toObject(Object o, InternalObjectFactory factory) {
            return ONTLiteralImpl.find((LiteralLabel) o, factory, model);
        }

        @Override
        public Object fromPredicate(ONTObject o) {
            OWLDataProperty property = (OWLDataProperty) o.getOWLObject();
            return ONTEntityImpl.getURI(property);
        }

        @Override
        public ONTObject<? extends OWLDataProperty> toPredicate(Object p, InternalObjectFactory factory) {
            return ONTDataPropertyImpl.find((String) p, factory, model);
        }

        @Override
        public ONTObject<? extends OWLDataProperty> fetchONTPredicate(OntStatement statement,
                                                                      InternalObjectFactory factory) {
            return factory.getProperty(getResource(statement).getProperty());
        }

        @Override
        public ONTObject<? extends OWLLiteral> fetchONTObject(OntStatement statement,
                                                              InternalObjectFactory factory) {
            return factory.getLiteral(getResource(statement).getTarget());
        }

        @Override
        public boolean containsAnonymousIndividuals() {
            return getContent()[0] instanceof BlankNodeId;
        }

        @FactoryAccessor
        @Override
        public OWLSubClassOfAxiom asOWLSubClassOfAxiom() {
            DataFactory df = getDataFactory();
            return df.getOWLSubClassOfAxiom(df.getOWLObjectOneOf(getFSubject()),
                    df.getOWLObjectComplementOf(df.getOWLDataHasValue(getFPredicate(), getFObject())));
        }

        @FactoryAccessor
        @Override
        protected OWLNegativeDataPropertyAssertionAxiom createAnnotatedAxiom(Collection<OWLAnnotation> annotations) {
            DataFactory df = getDataFactory();
            return df.getOWLNegativeDataPropertyAssertionAxiom(getFPredicate(), getFSubject(), getFObject(),
                    annotations);
        }

        @Override
        protected AxiomImpl makeCopy(ONTObject<OWLNegativeDataPropertyAssertionAxiom> other) {
            return new AxiomImpl(subject, predicate, object, model) {
                @Override
                public Stream<Triple> triples() {
                    return Stream.concat(super.triples(), other.triples());
                }
            };
        }

        @Override
        public boolean canContainObjectProperties() {
            return false;
        }

        @Override
        public Set<OWLDataProperty> getDataPropertySet() {
            return createSet(getProperty().asOWLDataProperty());
        }

        @Override
        public boolean containsDataProperty(OWLDataProperty property) {
            return getProperty().equals(property);
        }

        @Override
        public Set<OWLNamedIndividual> getNamedIndividualSet() {
            Object[] content = getContent();
            if (content[0] instanceof String) {
                Set<OWLNamedIndividual> res = createSortedSet();
                res.add(toNamedIndividual((String) content[0], getObjectFactory()).getOWLObject());
                return res;
            }
            return createSet();
        }

        @Override
        public boolean containsNamedIndividual(OWLNamedIndividual individual) {
            Object[] content = getContent();
            return content[0] instanceof String && content[0].equals(ONTEntityImpl.getURI(individual));
        }
    }
}
