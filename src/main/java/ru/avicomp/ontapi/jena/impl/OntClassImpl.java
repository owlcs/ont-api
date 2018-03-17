/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2017, Avicomp Services, AO
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

import java.util.Collection;
import java.util.stream.Stream;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;

import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

/**
 * owl:Class Implementation
 *
 * Created by szuev on 03.11.2016.
 */
public class OntClassImpl extends OntObjectImpl implements OntClass {

    public OntClassImpl(Node n, EnhGraph eg) {
        super(OntObjectImpl.checkNamed(n), eg);
    }

    @Override
    public boolean isBuiltIn() {
        return Entities.CLASS.builtInURIs().contains(this);
    }

    @Override
    public Class<OntClass> getActualClass() {
        return OntClass.class;
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
    public OntStatement addHasKey(Collection<OntOPE> objectProperties, Collection<OntNDP> dataProperties) {
        return OntCEImpl.addHasKey(this, objectProperties, dataProperties);
    }

    @Override
    public void removeHasKey() {
        clearAll(OWL.hasKey);
    }

    @Override
    public Stream<OntPE> hasKey() {
        return rdfListMembers(OWL.hasKey, OntPE.class);
    }

    @Override
    public OntStatement addDisjointUnionOf(Collection<OntCE> classes) {
        return addStatement(OWL.disjointUnionOf, getModel().createList(OntJenaException.notNull(classes, "Null classes collection.").iterator()));
    }

    @Override
    public void removeDisjointUnionOf() {
        clearAll(OWL.disjointUnionOf);
    }

    @Override
    public Stream<OntCE> disjointUnionOf() {
        return rdfListMembers(OWL.disjointUnionOf, OntCE.class);
    }

    @Override
    public OntStatement getRoot() {
        return getRoot(RDF.type, OWL.Class);
    }
}
