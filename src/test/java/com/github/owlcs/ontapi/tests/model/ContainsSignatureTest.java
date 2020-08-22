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
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.semanticweb.owlapi.model.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by @ssz on 01.05.2020.
 */
@RunWith(Parameterized.class)
public class ContainsSignatureTest {

    protected final ModelData data;

    public ContainsSignatureTest(ModelData data) {
        this.data = data;
    }

    @Parameterized.Parameters(name = "{0}")
    public static ModelData[] getData() {
        return ModelData.values();
    }

    @Test
    public void testContainsClasses() {
        testContains(OWLClass.class);
    }

    @Test
    public void testContainsDatatypes() {
        if (data == ModelData.FOOD) { // see https://github.com/owlcs/owlapi/issues/928
            Map<String, Boolean> data = new HashMap<>();
            data.put(XSD.xstring.getURI(), Boolean.TRUE);
            data.put(XSD.nonNegativeInteger.getURI(), Boolean.FALSE);
            testContains(OWLDatatype.class, data);
            return;
        }
        testContains(OWLDatatype.class);
    }

    @Test
    public void testContainsNamedIndividuals() {
        testContains(OWLNamedIndividual.class);
    }

    @Test
    public void testContainsObjectProperty() {
        testContains(OWLObjectProperty.class);
    }

    @Test
    public void testContainsAnnotationProperty() {
        testContains(OWLAnnotationProperty.class);
    }

    @Test
    public void testContainsDataProperty() {
        testContains(OWLDataProperty.class);
    }

    protected OWLOntologyManager newManager() {
        return OntManagers.createManager();
    }

    protected void testContains(Class<? extends OWLEntity> type) {
        testContains(type, createTestEntities(data, type));
    }

    protected void testContains(Class<? extends OWLEntity> type, Map<String, Boolean> entities) {
        OWLOntology o = data.fetch(newManager());
        testContains(o, type, entities);
    }

    protected void testContains(OWLOntology o, Class<? extends OWLEntity> type, Map<String, Boolean> entities) {
        OWLDataFactory df = o.getOWLOntologyManager().getOWLDataFactory();
        entities.forEach((s, v) -> {
            EntityType<?> t = OWLEntityUtils.getEntityType(type);
            OWLEntity e = df.getOWLEntity(t, IRI.create(s));
            Assert.assertEquals("Test " + t + " ::: " + e, v, o.containsEntityInSignature(e));
        });
    }

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
}
