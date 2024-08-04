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
import com.github.owlcs.ontapi.internal.WriteHelper;
import com.github.owlcs.ontapi.internal.objects.ONTAxiomImpl;
import com.github.owlcs.ontapi.internal.objects.ONTStatementImpl;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.ontapi.model.OntModel;
import org.apache.jena.ontapi.model.OntProperty;
import org.apache.jena.ontapi.model.OntStatement;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLPropertyExpression;

import java.util.function.Supplier;

/**
 * The base class for {@link SubObjectPropertyOfTranslator}, {@link SubDataPropertyOfTranslator}
 * and {@link SubAnnotationPropertyOfTranslator}.
 * Example:
 * {@code foaf:msnChatID rdfs:subPropertyOf foaf:nick .}
 * <p>
 * Created by @ssz on 30.09.2016.
 */
public abstract class AbstractSubPropertyTranslator<Axiom extends OWLAxiom, P extends OntProperty>
        extends AbstractSimpleTranslator<Axiom> {

    abstract OWLPropertyExpression getSubProperty(Axiom axiom);

    abstract OWLPropertyExpression getSuperProperty(Axiom axiom);

    abstract Class<P> getView();

    @Override
    public ExtendedIterator<OntStatement> listStatements(OntModel model, AxiomsSettings config) {
        return listByPredicate(model, RDFS.subPropertyOf).filterKeep(s -> filter(s, config));
    }

    protected boolean filter(OntStatement statement, AxiomsSettings config) {
        return statement.getSubject().canAs(getView()) && statement.getObject().canAs(getView());
    }

    @Override
    public boolean testStatement(OntStatement statement, AxiomsSettings config) {
        return RDFS.subPropertyOf.equals(statement.getPredicate()) && filter(statement, config);
    }

    @Override
    public void write(Axiom axiom, OntModel model) {
        WriteHelper.writeTriple(model, getSubProperty(axiom), RDFS.subPropertyOf, getSuperProperty(axiom),
                axiom.annotationsAsList());
    }

    @Override
    Triple createSearchTriple(Axiom axiom) {
        Node subject = TranslateHelper.getSearchNode(getSubProperty(axiom));
        if (subject == null) return null;
        Node object = TranslateHelper.getSearchNode(getSuperProperty(axiom));
        if (object == null) return null;
        return Triple.create(subject, RDFS.subPropertyOf.asNode(), object);
    }

    /**
     * A base {@code rdfs:subPropertyOf} axiom.
     *
     * @param <A> - subtype of {@link OWLAxiom},
     *            either {@link org.semanticweb.owlapi.model.OWLSubAnnotationPropertyOfAxiom}
     *            or {@link org.semanticweb.owlapi.model.OWLSubPropertyAxiom}
     * @param <P> - subtype of {@link OWLPropertyExpression}
     */
    @SuppressWarnings("WeakerAccess")
    public abstract static class SubPropertyAxiomImpl<A extends OWLAxiom, P extends OWLPropertyExpression>
            extends ONTAxiomImpl<A> implements WithTwoObjects.Unary<P> {

        protected SubPropertyAxiomImpl(Triple t, Supplier<OntModel> m) {
            super(t, m);
        }

        protected SubPropertyAxiomImpl(Object subject, String predicate, Object object, Supplier<OntModel> m) {
            super(subject, predicate, object, m);
        }

        public P getSubProperty() {
            return getONTSubject().getOWLObject();
        }

        public P getSuperProperty() {
            return getONTObject().getOWLObject();
        }

        @Override
        protected boolean sameContent(ONTStatementImpl other) {
            return false;
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
        public final boolean canContainClassExpressions() {
            return false;
        }

        @Override
        public final boolean canContainNamedClasses() {
            return false;
        }

        @Override
        public final boolean canContainNamedIndividuals() {
            return false;
        }
    }
}
