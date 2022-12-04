/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2022, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.jena.impl;

import com.github.owlcs.ontapi.jena.OntJenaException;
import com.github.owlcs.ontapi.jena.model.*;
import com.github.owlcs.ontapi.jena.vocabulary.OWL;
import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * owl:Class Implementation
 * <p>
 * Created by szuev on 03.11.2016.
 */
@SuppressWarnings("WeakerAccess")
public class OntClassImpl extends OntObjectImpl implements OntClass.Named {

    public OntClassImpl(Node n, EnhGraph eg) {
        super(OntObjectImpl.checkNamed(n), eg);
    }

    @Override
    public Optional<OntStatement> findRootStatement() {
        return getOptionalRootStatement(this, OWL.Class);
    }

    @Override
    public boolean isBuiltIn() {
        return getModel().isBuiltIn(this);
    }

    @Override
    public Class<Named> getActualClass() {
        return Named.class;
    }

    @Override
    public OntIndividual.Anonymous createIndividual() {
        return OntCEImpl.createAnonymousIndividual(getModel(), this);
    }

    @Override
    public OntIndividual.Named createIndividual(String uri) {
        return OntCEImpl.createNamedIndividual(getModel(), this, uri);
    }

    @Override
    public Stream<OntClass> superClasses(boolean direct) {
        return hierarchy(this, OntClass.class, RDFS.subClassOf, false, direct);
    }

    @Override
    public Stream<OntClass> subClasses(boolean direct) {
        return hierarchy(this, OntClass.class, RDFS.subClassOf, true, direct);
    }

    @Override
    public boolean hasDeclaredProperty(OntRealProperty property, boolean direct) {
        return OntCEImpl.testDomain(this, property, direct);
    }

    @Override
    public Stream<OntRealProperty> declaredProperties(boolean direct) {
        return OntCEImpl.declaredProperties(this, direct);
    }

    @Override
    public boolean isHierarchyRoot() {
        return OntCEImpl.isHierarchyRoot(this);
    }

    @Override
    public OntList<OntRealProperty> createHasKey(Collection<OntObjectProperty> ope, Collection<OntDataProperty> dpe) {
        return OntCEImpl.createHasKey(getModel(), this, Stream.of(ope, dpe).flatMap(Collection::stream));
    }

    @Override
    public OntStatement addHasKeyStatement(OntRealProperty... properties) {
        return OntCEImpl.createHasKey(getModel(), this, Arrays.stream(properties)).getMainStatement();
    }

    @Override
    public Stream<OntList<OntRealProperty>> hasKeys() {
        return OntCEImpl.listHasKeys(getModel(), this);
    }

    @Override
    public OntClassImpl removeHasKey(Resource list) throws OntJenaException.IllegalArgument {
        OntCEImpl.removeHasKey(getModel(), this, list);
        return this;
    }

    @Override
    public boolean isDisjoint(Resource candidate) {
        return OntCEImpl.isDisjoint(this, candidate);
    }

    @Override
    public OntList<OntClass> createDisjointUnion(Collection<OntClass> classes) {
        return getModel().createOntList(this, OWL.disjointUnionOf, OntClass.class,
                Objects.requireNonNull(classes).stream().distinct().iterator());
    }

    @Override
    public Stream<OntList<OntClass>> disjointUnions() {
        return OntListImpl.stream(getModel(), this, OWL.disjointUnionOf, OntClass.class);
    }

    @Override
    public OntClassImpl removeDisjointUnion(Resource rdfList) throws OntJenaException.IllegalArgument {
        getModel().deleteOntList(this, OWL.disjointUnionOf, findDisjointUnion(rdfList).orElse(null));
        return this;
    }
}
