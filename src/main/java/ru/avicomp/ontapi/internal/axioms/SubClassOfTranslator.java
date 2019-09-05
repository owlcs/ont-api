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

import org.apache.jena.rdf.model.Statement;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import ru.avicomp.ontapi.internal.*;
import ru.avicomp.ontapi.internal.objects.ONTSimpleAxiomImpl;
import ru.avicomp.ontapi.jena.model.OntCE;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.utils.OntModels;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/**
 * A translator that provides {@link OWLSubClassOfAxiom} implementations.
 * Examples:
 * <pre>{@code
 * pizza:JalapenoPepperTopping
 *         rdfs:subClassOf   pizza:PepperTopping ;
 *         rdfs:subClassOf   [ a                   owl:Restriction ;
 *                             owl:onProperty      pizza:hasSpiciness ;
 *                             owl:someValuesFrom  pizza:Hot
 *                           ] .
 * }</pre>
 * <p>
 * Created by @szuev on 28.09.2016.
 *
 * @see <a href='https://www.w3.org/TR/owl-syntax/#Subclass_Axioms'>9.1.1 Subclass Axioms</a>
 */
@SuppressWarnings("WeakerAccess")
public class SubClassOfTranslator extends AxiomTranslator<OWLSubClassOfAxiom> {

    @Override
    public void write(OWLSubClassOfAxiom axiom, OntGraphModel model) {
        WriteHelper.writeTriple(model, axiom.getSubClass(), RDFS.subClassOf, axiom.getSuperClass(), axiom.annotations());
    }

    @Override
    public ExtendedIterator<OntStatement> listStatements(OntGraphModel model, InternalConfig config) {
        return OntModels.listLocalStatements(model, null, RDFS.subClassOf, null).filterKeep(this::filter);
    }

    @Override
    public boolean testStatement(OntStatement statement, InternalConfig config) {
        return statement.getPredicate().equals(RDFS.subClassOf) && filter(statement);
    }

    public boolean filter(Statement s) {
        return s.getSubject().canAs(OntCE.class) && s.getObject().canAs(OntCE.class);
    }

    @Override
    public ONTObject<OWLSubClassOfAxiom> toAxiom(OntStatement statement,
                                                 Supplier<OntGraphModel> model,
                                                 InternalObjectFactory factory,
                                                 InternalConfig config) {
        return AxiomImpl.create(statement, model, factory, config);
    }

    @Override
    public ONTObject<OWLSubClassOfAxiom> toAxiom(OntStatement statement,
                                                 InternalObjectFactory factory,
                                                 InternalConfig config) {
        ONTObject<? extends OWLClassExpression> sub = factory.getClass(statement.getSubject(OntCE.class));
        ONTObject<? extends OWLClassExpression> sup = factory.getClass(statement.getObject().as(OntCE.class));
        Collection<ONTObject<OWLAnnotation>> annotations = factory.getAnnotations(statement, config);
        OWLSubClassOfAxiom res = factory.getOWLDataFactory()
                .getOWLSubClassOfAxiom(sub.getOWLObject(), sup.getOWLObject(), ONTObject.extract(annotations));
        return ONTWrapperImpl.create(res, statement).append(annotations).append(sub).append(sup);
    }

    /**
     * @see ru.avicomp.ontapi.owlapi.axioms.OWLSubClassOfAxiomImpl
     */
    public static class AxiomImpl extends ONTSimpleAxiomImpl<OWLSubClassOfAxiom>
            implements ONTObject<OWLSubClassOfAxiom>, OWLSubClassOfAxiom {

        protected AxiomImpl(Object subject, String predicate, Object object, Supplier<OntGraphModel> m) {
            super(subject, predicate, object, m);
        }

        /**
         * Wraps the given {@link OntStatement} as {@link OWLSubClassOfAxiom} and {@link ONTObject}.
         *
         * @param s  {@link OntStatement}, not {@code null}
         * @param m  {@link OntGraphModel} provider, not {@code null}
         * @param of {@link InternalObjectFactory}, not {@code null}
         * @param c  {@link InternalConfig}, not {@code null}
         * @return {@link AxiomImpl}
         */
        public static AxiomImpl create(OntStatement s,
                                       Supplier<OntGraphModel> m,
                                       InternalObjectFactory of,
                                       InternalConfig c) {
            return collect(new AxiomImpl(fromNode(s.getSubject()), s.getPredicate().getURI(),
                    fromNode(s.getObject()), m), s, of, c);
        }

        @Override
        protected OWLSubClassOfAxiom createAnnotatedAxiom(Collection<OWLAnnotation> annotations) {
            return getDataFactory().getOWLSubClassOfAxiom(getSubClass(), getSuperClass(), annotations);
        }

        @Override
        public OWLClassExpression getSubClass() {
            return getONTSubClass().getOWLObject();
        }

        @Override
        public OWLClassExpression getSuperClass() {
            return getONTSuperClass().getOWLObject();
        }

        @SuppressWarnings("unchecked")
        public ONTObject<? extends OWLClassExpression> getONTSubClass() {
            return (ONTObject<? extends OWLClassExpression>) getContent()[0];
        }

        @SuppressWarnings("unchecked")
        public ONTObject<? extends OWLClassExpression> getONTSuperClass() {
            return (ONTObject<? extends OWLClassExpression>) getContent()[1];
        }

        @Override
        public boolean isGCI() {
            return getSubClass().isAnonymous();
        }

        @Override
        public boolean canContainAnnotationProperties() {
            return isAnnotated();
        }

        @Override
        public OWLSubClassOfAxiom getOWLObject() {
            return this;
        }

        @Override
        protected int getOperandsNum() {
            return 2;
        }

        @Override
        protected void collectOperands(List<ONTObject<? extends OWLObject>> cache,
                                       OntStatement s,
                                       InternalObjectFactory f) {
            cache.add(f.getClass(s.getSubject(OntCE.class)));
            cache.add(f.getClass(s.getObject(OntCE.class)));
        }
    }

}
