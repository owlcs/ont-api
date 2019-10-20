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

package ru.avicomp.ontapi.jena.impl;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.FrontsNode;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.utils.Iter;
import ru.avicomp.ontapi.jena.vocabulary.OWL;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of the Object Property Expression abstraction.
 * Named instances should have a {@link OWL#OntologyProperty owl:ObjectProperty} type declarations.
 * Anonymous instances should have {@link OWL#inverseOf owl:inverseOf} predicate.
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
            OntGraphModelImpl m = getModel();
            List<Node> nodes = m.localStatements(null, OWL.inverseOf, this)
                    .map(OntStatement::getSubject)
                    .filter(RDFNode::isAnon)
                    .map(FrontsNode::asNode)
                    .distinct()
                    .collect(Collectors.toList());
            if (nodes.size() > 1) {
                throw new OntJenaException.IllegalState("More than one inverse-of object properties found: [" +
                        nodes + " owl:inverseOf " + this + "]");
            }
            Node n = nodes.isEmpty() ?
                    m.createResource().addProperty(OWL.inverseOf, NamedPropertyImpl.this).asNode() :
                    nodes.get(0);
            return m.getNodeAs(n, Inverse.class);
        }

        @Override
        public boolean isBuiltIn() {
            return getModel().isBuiltIn(this);
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
        public Optional<OntStatement> findRootStatement() {
            return getOptionalRootStatement(this, OWL.ObjectProperty);
        }

        @Override
        public int getOrdinal() {
            return OntStatementImpl.createProperty(node, enhGraph).getOrdinal();
        }
    }

    public static class InversePropertyImpl extends OntOPEImpl implements OntOPE.Inverse {

        public InversePropertyImpl(Node n, EnhGraph g) {
            super(n, g);
        }

        @Override
        public Optional<OntStatement> findRootStatement() {
            return Optional.of(getModel().createStatement(this, OWL.inverseOf, getDirect()).asRootStatement());
        }

        @Override
        public Class<? extends OntObject> getActualClass() {
            return OntOPE.Inverse.class;
        }

        @Override
        public OntNOP getDirect() {
            OntGraphModelImpl m = getModel();
            List<Resource> res = Iter.distinct(listObjects(OWL.inverseOf, Resource.class)
                    .filterKeep(RDFNode::isURIResource)).toList();
            if (res.size() != 1)
                throw new OntJenaException.IllegalState("Expected one and only one owl:inverseOf statement, but found: [" +
                        this + " owl:inverseOf " + res + "]");
            return m.getNodeAs(res.get(0).asNode(), OntNOP.class);
        }

        @Override
        public Property asProperty() {
            return getDirect().asProperty();
        }
    }

    @Override
    public Stream<OntOPE> superProperties(boolean direct) {
        return hierarchy(this, OntOPE.class, RDFS.subPropertyOf, false, direct);
    }

    @Override
    public Stream<OntOPE> subProperties(boolean direct) {
        return hierarchy(this, OntOPE.class, RDFS.subPropertyOf, true, direct);
    }

    @Override
    public OntNPA.ObjectAssertion addNegativeAssertion(OntIndividual source, OntIndividual target) {
        return OntNPAImpl.create(getModel(), source, this, target);
    }

    @Override
    public OntList<OntOPE> createPropertyChain(Collection<OntOPE> properties) {
        return OntListImpl.create(getModel(), this, OWL.propertyChainAxiom, OntOPE.class, properties.iterator());
    }

    @Override
    public Stream<OntList<OntOPE>> propertyChains() {
        return OntListImpl.stream(getModel(), this, OWL.propertyChainAxiom, OntOPE.class);
    }

    @Override
    public OntOPEImpl removePropertyChain(Resource rdfList) throws OntJenaException.IllegalArgument {
        getModel().deleteOntList(this, OWL.propertyChainAxiom, findPropertyChain(rdfList).orElse(null));
        return this;
    }

    @Override
    protected OntOPEImpl changeRDFType(Resource type, boolean add) {
        super.changeRDFType(type, add);
        return this;
    }

    @Override
    public OntOPEImpl setFunctional(boolean functional) {
        return changeRDFType(OWL.FunctionalProperty, functional);
    }

    @Override
    public OntOPEImpl setInverseFunctional(boolean inverseFunctional) {
        return changeRDFType(OWL.InverseFunctionalProperty, inverseFunctional);
    }

    @Override
    public OntOPEImpl setSymmetric(boolean symmetric) {
        return changeRDFType(OWL.SymmetricProperty, symmetric);
    }

    @Override
    public OntOPEImpl setAsymmetric(boolean asymmetric) {
        return changeRDFType(OWL.AsymmetricProperty, asymmetric);
    }

    @Override
    public OntOPEImpl setTransitive(boolean transitive) {
        return changeRDFType(OWL.TransitiveProperty, transitive);
    }

    @Override
    public OntOPEImpl setReflexive(boolean reflexive) {
        return changeRDFType(OWL.ReflexiveProperty, reflexive);
    }

    @Override
    public OntOPEImpl setIrreflexive(boolean irreflexive) {
        return changeRDFType(OWL.IrreflexiveProperty, irreflexive);
    }

}

