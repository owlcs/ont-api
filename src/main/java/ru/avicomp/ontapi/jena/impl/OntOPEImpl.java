/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2018, Avicomp Services, AO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package ru.avicomp.ontapi.jena.impl;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.*;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

import java.util.Collection;
import java.util.stream.Stream;

/**
 * owl:ObjectProperty (could be also Annotation, InverseFunctional, Transitive, SymmetricProperty, etc)
 * <p>
 * Created by szuev on 03.11.2016.
 */
@SuppressWarnings("WeakerAccess")
public abstract class OntOPEImpl extends OntPEImpl implements OntOPE {

    public OntOPEImpl(Node n, EnhGraph g) {
        super(n, g);
    }

    public static class NamedPropertyImpl extends OntOPEImpl implements OntNOP {

        public NamedPropertyImpl(Node n, EnhGraph g) {
            super(n, g);
        }

        @Override
        public Inverse createInverse() {
            Resource res = getModel().createResource();
            getModel().add(res, OWL.inverseOf, this);
            return new InversePropertyImpl(res.asNode(), getModel());
        }

        @Override
        public boolean isBuiltIn() {
            return Entities.OBJECT_PROPERTY.builtInURIs().contains(this);
        }

        @Override
        public Class<OntNOP> getActualClass() {
            return OntNOP.class;
        }

        @Override
        public Property inModel(Model m) {
            return getModel() == m ? this : m.createProperty(getURI());
        }

        @Override
        public OntStatement getRoot() {
            return getRoot(RDF.type, OWL.ObjectProperty);
        }

        @Override
        public Property asProperty() {
            return as(Property.class);
        }
    }

    public static class InversePropertyImpl extends OntOPEImpl implements OntOPE.Inverse {

        public InversePropertyImpl(Node n, EnhGraph g) {
            super(n, g);
        }

        @Override
        public OntStatement getRoot() {
            return getModel().createOntStatement(true, this, OWL.inverseOf, getRequiredDirectProperty());
        }

        protected Resource getRequiredDirectProperty() {
            try (Stream<OntStatement> statements = getModel().statements(this, OWL.inverseOf, null)) {
                return statements.findFirst()
                        .map(Statement::getObject).map(RDFNode::asResource)
                        .orElseThrow(() -> new OntJenaException("Can't find owl:inverseOf object prop."));
            }
        }

        @Override
        public OntOPE getDirect() {
            Resource res = getRequiredDirectProperty();
            return res.as(OntOPE.class);
        }

        @Override
        public Property asProperty() {
            return getRequiredDirectProperty().as(Property.class);
        }
    }

    @Override
    public OntNPA.ObjectAssertion addNegativeAssertion(OntIndividual source, OntIndividual target) {
        return OntNPAImpl.create(getModel(), source, this, target);
    }

    @Override
    public OntStatement addSuperPropertyOf(Collection<OntOPE> chain) {
        OntJenaException.notNull(chain, "Null properties chain");
        return addStatement(OWL.propertyChainAxiom, getModel().createList(chain.iterator()));
    }

    @Override
    public void removeSuperPropertyOf() {
        clearAll(OWL.propertyChainAxiom);
    }

    @Override
    public Stream<OntOPE> superPropertyOf() {
        return getRequiredProperty(OWL.propertyChainAxiom).getObject().as(RDFList.class).asJavaList().stream().map(r -> r.as(OntOPE.class));
    }

    @Override
    public Stream<RDFList> propertyChains() {
        return statements(OWL.propertyChainAxiom).map(Statement::getObject).map(r -> r.as(RDFList.class));
    }

    @Override
    public void setFunctional(boolean functional) {
        changeType(OWL.FunctionalProperty, functional);
    }

    @Override
    public void setInverseFunctional(boolean inverseFunctional) {
        changeType(OWL.InverseFunctionalProperty, inverseFunctional);
    }

    @Override
    public void setAsymmetric(boolean asymmetric) {
        changeType(OWL.AsymmetricProperty, asymmetric);
    }

    @Override
    public void setTransitive(boolean transitive) {
        changeType(OWL.TransitiveProperty, transitive);
    }

    @Override
    public void setReflexive(boolean reflexive) {
        changeType(OWL.ReflexiveProperty, reflexive);
    }

    @Override
    public void setIrreflexive(boolean irreflexive) {
        changeType(OWL.IrreflexiveProperty, irreflexive);
    }

    @Override
    public void setSymmetric(boolean symmetric) {
        changeType(OWL.SymmetricProperty, symmetric);
    }

    @Override
    public OntOPE getInverseOf() {
        return getObject(OWL.inverseOf, OntOPE.class);
    }
}

