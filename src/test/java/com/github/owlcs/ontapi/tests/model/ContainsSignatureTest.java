/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2020, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.tests.model;

import com.github.owlcs.ontapi.OntManagers;
import com.github.owlcs.ontapi.jena.OntVocabulary;
import com.github.owlcs.ontapi.jena.vocabulary.XSD;
import com.github.owlcs.ontapi.tests.ModelData;
import com.github.owlcs.ontapi.utils.OWLEntityUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.semanticweb.owlapi.model.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by @ssz on 01.05.2020.
 */
public class ContainsSignatureTest {

    protected static Map<String, Boolean> createTestEntities(ModelData data, Class<? extends OWLEntity> type) {
        return createTestEntities(data.fetch(OntManagers.createOWLAPIImplManager()), type);
    }

    protected static Map<String, Boolean> createTestEntities(OWLOntology o, Class<? extends OWLEntity> type) {
        Map<String, Boolean> res = collectEntities(o, type);
        OntVocabulary.Factory.get().get(OWLEntityUtils.getResourceType(type)).stream().map(Resource::getURI)
                .forEach(s -> res.putIfAbsent(s, Boolean.FALSE));
        res.put("http://" + RandomStringUtils.randomAlphanumeric(4) + ".xxx", Boolean.FALSE);
        return res;
    }

    protected static Map<String, Boolean> collectEntities(OWLOntology o, Class<? extends OWLEntity> type) {
        Map<String, Boolean> res = new HashMap<>();
        OWLEntityUtils.signature(o, type).forEach(x -> res.put(x.getIRI().getIRIString(), Boolean.TRUE));
        return res;
    }

    protected OWLOntologyManager newManager() {
        return OntManagers.createManager();
    }

    @ParameterizedTest
    @EnumSource(value = ModelData.class)
    public void testContainsClasses(ModelData data) {
        testContains(data, OWLClass.class);
    }

    @ParameterizedTest
    @EnumSource(value = ModelData.class)
    public void testContainsDatatypes(ModelData data) {
        if (data == ModelData.FOOD) { // see https://github.com/owlcs/owlapi/issues/928
            Map<String, Boolean> d = new HashMap<>();
            d.put(XSD.xstring.getURI(), Boolean.TRUE);
            d.put(XSD.nonNegativeInteger.getURI(), Boolean.FALSE);
            testContains(data, OWLDatatype.class, d);
            return;
        }
        testContains(data, OWLDatatype.class);
    }

    @ParameterizedTest
    @EnumSource(value = ModelData.class)
    public void testContainsNamedIndividuals(ModelData data) {
        testContains(data, OWLNamedIndividual.class);
    }

    @ParameterizedTest
    @EnumSource(value = ModelData.class)
    public void testContainsObjectProperty(ModelData data) {
        testContains(data, OWLObjectProperty.class);
    }

    @ParameterizedTest
    @EnumSource(value = ModelData.class)
    public void testContainsAnnotationProperty(ModelData data) {
        testContains(data, OWLAnnotationProperty.class);
    }

    @ParameterizedTest
    @EnumSource(value = ModelData.class)
    public void testContainsDataProperty(ModelData data) {
        testContains(data, OWLDataProperty.class);
    }

    protected void testContains(ModelData data, Class<? extends OWLEntity> type) {
        testContains(data, type, createTestEntities(data, type));
    }

    protected void testContains(ModelData data, Class<? extends OWLEntity> type, Map<String, Boolean> entities) {
        OWLOntology o = data.fetch(newManager());
        testContains(o, type, entities);
    }

    protected void testContains(OWLOntology o, Class<? extends OWLEntity> type, Map<String, Boolean> entities) {
        OWLDataFactory df = o.getOWLOntologyManager().getOWLDataFactory();
        entities.forEach((s, v) -> {
            EntityType<?> t = OWLEntityUtils.getEntityType(type);
            OWLEntity e = df.getOWLEntity(t, IRI.create(s));
            Assertions.assertEquals(v, o.containsEntityInSignature(e), "Test " + t + " ::: " + e);
        });
    }
}
