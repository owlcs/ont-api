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

import com.github.owlcs.ontapi.config.AxiomsSettings;
import com.github.owlcs.ontapi.internal.ModelObjectFactory;
import com.github.owlcs.ontapi.internal.ONTObject;
import com.github.owlcs.ontapi.internal.WriteHelper;
import com.github.owlcs.ontapi.internal.objects.ONTAxiomImpl;
import com.github.owlcs.ontapi.internal.objects.ONTEntityImpl;
import com.github.owlcs.ontapi.internal.objects.ONTStatementImpl;
import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.jena.model.OntObjectProperty;
import com.github.owlcs.ontapi.jena.model.OntProperty;
import com.github.owlcs.ontapi.jena.model.OntStatement;
import com.github.owlcs.ontapi.jena.vocabulary.RDF;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.HasProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyCharacteristicAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLPropertyExpression;
import org.semanticweb.owlapi.model.OWLUnaryPropertyAxiom;

import java.util.Set;
import java.util.function.Supplier;

/**
 * The base class to read and write axiom which is related to simple typed triple associated with object or data property.
 * <p>
 * List of sub-classes:
 * {@link FunctionalDataPropertyTranslator},
 * {@link FunctionalObjectPropertyTranslator},
 * {@link ReflexiveObjectPropertyTranslator},
 * {@link IrreflexiveObjectPropertyTranslator},
 * {@link AsymmetricObjectPropertyTranslator},
 * {@link SymmetricObjectPropertyTranslator},
 * {@link TransitiveObjectPropertyTranslator},
 * {@link InverseFunctionalObjectPropertyTranslator},
 * <p>
 * Created by @ssz on 28.09.2016.
 */
public abstract class AbstractPropertyTypeTranslator<Axiom extends OWLAxiom & HasProperty<? extends OWLObject>,
        P extends OntProperty> extends AbstractSimpleTranslator<Axiom> {

    abstract Resource getType();

    abstract Class<P> getView();

    P getSubject(OntStatement s) {
        return s.getSubject(getView());
    }

    @Override
    public ExtendedIterator<OntStatement> listStatements(OntModel model, AxiomsSettings config) {
        return listByPredicateAndObject(model, RDF.type, getType())
                .filterKeep(s -> s.getSubject().canAs(getView()));
    }

    @Override
    public boolean testStatement(OntStatement statement, AxiomsSettings config) {
        return statement.getObject().equals(getType())
                && statement.isDeclaration()
                && statement.getSubject().canAs(getView());
    }

    @Override
    public void write(Axiom axiom, OntModel model) {
        WriteHelper.writeTriple(model, axiom.getProperty(), RDF.type, getType(), axiom.annotationsAsList());
    }

    @Override
    boolean testSearchTriple(Triple t) {
        return t.getSubject().isURI();
    }

    @Override
    Triple createSearchTriple(Axiom axiom) {
        Node n = TranslateHelper.getSearchNode(axiom.getProperty());
        if (n == null) return null;
        return Triple.create(n, RDF.type.asNode(), getType().asNode());
    }

    /**
     * The base for {@code ONTAxiom} with one object property.
     *
     * @param <A> a subtype of {@link OWLObjectPropertyCharacteristicAxiom}
     */
    @SuppressWarnings("WeakerAccess")
    protected static abstract class ObjectAxiomImpl<A extends OWLObjectPropertyCharacteristicAxiom>
            extends UnaryAxiomImpl<A, OWLObjectPropertyExpression> {

        protected ObjectAxiomImpl(Triple t, Supplier<OntModel> m) {
            super(t, m);
        }

        @Override
        public ONTObject<? extends OWLObjectPropertyExpression> findURISubject(ModelObjectFactory factory) {
            return factory.getObjectProperty((String) subject);
        }

        @Override
        public ONTObject<? extends OWLObjectPropertyExpression> fetchONTSubject(OntStatement statement,
                                                                                ModelObjectFactory factory) {
            return factory.getProperty(statement.getSubject(OntObjectProperty.class));
        }

        @Override
        public boolean canContainDataProperties() {
            return false;
        }

        @Override
        protected boolean sameContent(ONTStatementImpl other) {
            return false;
        }

        @SuppressWarnings("unchecked")
        Set<OWLObjectProperty> getComponentsAsPropertySet() {
            return (Set<OWLObjectProperty>) getOWLComponentsAsSet();
        }

        @SuppressWarnings("unchecked")
        Set<OWLEntity> getComponentsAsEntitySet() {
            return (Set<OWLEntity>) getOWLComponentsAsSet();
        }

        boolean hasSubject(OWLObjectProperty property) {
            return subject.equals(ONTEntityImpl.getURI(property));
        }
    }


    /**
     * The base for {@code ONTAxiom} with one object or data property.
     *
     * @param <A> a subtype of {@link OWLUnaryPropertyAxiom}
     * @param <P> a subtype of {@link OWLPropertyExpression}
     */
    @SuppressWarnings("WeakerAccess")
    protected static abstract class UnaryAxiomImpl<A extends OWLUnaryPropertyAxiom<P>,
            P extends OWLPropertyExpression> extends ONTAxiomImpl<A>
            implements WithOneObject<P> {

        protected UnaryAxiomImpl(Triple t, Supplier<OntModel> m) {
            super(t, m);
        }

        public final P getProperty() {
            return getONTValue().getOWLObject();
        }

        @Override
        public final boolean canContainAnnotationProperties() {
            return isAnnotated();
        }

        @Override
        public final boolean canContainDatatypes() {
            return isAnnotated();
        }

        @Override
        public final boolean canContainAnonymousIndividuals() {
            return isAnnotated();
        }

        @Override
        public final boolean canContainNamedClasses() {
            return false;
        }

        @Override
        public final boolean canContainNamedIndividuals() {
            return false;
        }

        @Override
        public final boolean canContainClassExpressions() {
            return false;
        }
    }
}
