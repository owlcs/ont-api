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

package ru.avicomp.ontapi.tests.jena;

import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDFS;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avicomp.ontapi.jena.OntModelFactory;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.utils.Models;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * To test {@link Models} utility.
 * <p>
 * Created by @szuev on 25.04.2018.
 */
public class ModelUtilsTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModelUtilsTest.class);

    @Test
    public void testDeleteResources() {
        OntGraphModel m = OntModelFactory.createModel();
        m.read(ModelUtilsTest.class.getResourceAsStream("/ontapi/recursive-graph.ttl"), null, "ttl");
        String ns = m.getID().getURI() + "#";
        OntObject d = m.createDisjointClasses(Arrays.asList(
                m.createOntEntity(OntClass.class, ns + "CL1"),
                m.createOntEntity(OntClass.class, ns + "CL2"),
                m.createUnionOf(Arrays.asList(
                        m.createOntEntity(OntClass.class, ns + "CL4"),
                        m.createOntEntity(OntClass.class, ns + "CL5"),
                        m.createOntEntity(OntClass.class, ns + "CL6"))),
                m.createOntEntity(OntClass.class, ns + "CL3")));

        ReadWriteUtils.print(m);
        Assert.assertEquals(40, m.localStatements().count());

        Resource r = m.statements(null, RDFS.subClassOf, null)
                .map(Statement::getObject)
                .filter(RDFNode::isAnon)
                .map(RDFNode::asResource)
                .filter(s -> s.hasProperty(OWL.someValuesFrom))
                .findFirst().orElseThrow(IllegalStateException::new);

        LOGGER.debug("Delete {}", r);
        Models.deleteAll(r);
        LOGGER.debug("Delete {}", d);
        Models.deleteAll(d);
        List<OntCE> classes = m.listClasses()
                .filter(s -> s.getLocalName().contains("CL"))
                .collect(Collectors.toList());
        classes.forEach(c -> {
            LOGGER.debug("Delete {}", c);
            Models.deleteAll(c);
        });

        LOGGER.debug("---------------");
        ReadWriteUtils.print(m);
        Assert.assertEquals(10, m.statements().count());
    }

    @Test
    public void testAddLabels() {
        OntGraphModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntID id = m.getID();
        id.addLabel("lab1");
        id.addLabel("lab1", "e1");
        id.addLabel("lab2", "e2");
        id.addLabel("lab3", "language3");
        id.addLabel("lab4", "e2");
        id.addLabel("lab5", "e2");
        id.addLabel("lab5");
        ReadWriteUtils.print(m);
        Assert.assertEquals(2, Models.langValues(id, RDFS.label, null).count());
        Assert.assertEquals(3, Models.langValues(id, RDFS.label, "e2").count());
        Assert.assertEquals(1, Models.langValues(id, RDFS.label, "language3").count());
        Assert.assertEquals(7, m.listObjectsOfProperty(id, RDFS.label).toSet().size());
    }

    @Test
    public void testInsertModel() {
        OntGraphModel a1 = OntModelFactory.createModel().setID("http://a").getModel();
        OntGraphModel a2 = OntModelFactory.createModel().setID("http://a").getModel();
        OntClass c1 = a1.createOntEntity(OntClass.class, "http://a#Class-a1");
        OntClass c2 = a2.createOntEntity(OntClass.class, "http://a#Class-a2");

        // collection depending on a1
        OntGraphModel m1 = OntModelFactory.createModel().setID("http://m1").getModel().addImport(a1);
        OntGraphModel m2 = OntModelFactory.createModel().setID("http://m2").getModel().addImport(a1);
        Assert.assertTrue(ModelFactory.createModelForGraph(m1.getGraph()).containsResource(c1));
        Assert.assertFalse(ModelFactory.createModelForGraph(m1.getGraph()).containsResource(c2));
        Assert.assertTrue(ModelFactory.createModelForGraph(m2.getGraph()).containsResource(c1));
        Assert.assertFalse(ModelFactory.createModelForGraph(m2.getGraph()).containsResource(c2));

        Models.insert(() -> Stream.of(m1, m2), a2, true);
        Assert.assertTrue(ModelFactory.createModelForGraph(m1.getGraph()).containsResource(c2));
        Assert.assertFalse(ModelFactory.createModelForGraph(m1.getGraph()).containsResource(c1));
        Assert.assertTrue(ModelFactory.createModelForGraph(m2.getGraph()).containsResource(c2));
        Assert.assertFalse(ModelFactory.createModelForGraph(m2.getGraph()).containsResource(c1));
    }

}
