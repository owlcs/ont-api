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

import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.util.iterator.NullIterator;
import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.internal.*;
import ru.avicomp.ontapi.internal.objects.ONTAnnotationImpl;
import ru.avicomp.ontapi.internal.objects.ONTSimpleAxiomImpl;
import ru.avicomp.ontapi.jena.model.*;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

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
                        ONTObject.extract(annotations));
        return ONTObjectImpl.create(res, statement).append(annotations).append(s).append(p).append(v);
    }

    /**
     * @see ru.avicomp.ontapi.owlapi.axioms.OWLAnnotationAssertionAxiomImpl
     */
    public static class AxiomImpl extends ONTSimpleAxiomImpl<OWLAnnotationAssertionAxiom>
            implements ONTObject<OWLAnnotationAssertionAxiom>, OWLAnnotationAssertionAxiom {

        protected AxiomImpl(Object subject, String predicate, Object object, Supplier<OntGraphModel> m) {
            super(subject, predicate, object, m);
        }

        public static AxiomImpl create(OntStatement s,
                                       Supplier<OntGraphModel> m,
                                       InternalObjectFactory of,
                                       InternalConfig c) {
            return collect(new AxiomImpl(fromNode(s.getSubject()),
                    s.getPredicate().getURI(), fromNode(s.getObject()), m), s, of, c);
        }

        @Override
        public OWLAnnotationAssertionAxiom getOWLObject() {
            return this;
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

        @SuppressWarnings("unchecked")
        public ONTObject<? extends OWLAnnotationSubject> getONTSubject() {
            return (ONTObject<? extends OWLAnnotationSubject>) getContent()[0];
        }

        @SuppressWarnings("unchecked")
        public ONTObject<OWLAnnotationProperty> getONTProperty() {
            return (ONTObject<OWLAnnotationProperty>) getContent()[1];
        }

        @SuppressWarnings("unchecked")
        public ONTObject<? extends OWLAnnotationValue> getONTValue() {
            return (ONTObject<? extends OWLAnnotationValue>) getContent()[2];
        }

        @Override
        public OWLAnnotation getAnnotation() {
            return getDataFactory().getOWLAnnotation(getProperty(), getValue());
        }

        @Override
        public boolean isDeprecatedIRIAssertion() {
            return ONTAnnotationImpl.isDeprecated(predicate, object);
        }

        @Override
        protected OWLAnnotationAssertionAxiom createAnnotatedAxiom(Collection<OWLAnnotation> annotations) {
            return getDataFactory().getOWLAnnotationAssertionAxiom(getProperty(), getSubject(), getValue(), annotations);
        }

        @Override
        protected int getOperandsNum() {
            return 3;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void collectOperands(List cache, OntStatement s, InternalObjectFactory f) {
            cache.add(f.getSubject(s.getSubject(OntObject.class)));
            cache.add(f.getProperty(s.getPredicate().as(OntNAP.class)));
            cache.add(f.getValue(s.getObject()));
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
    }

}
