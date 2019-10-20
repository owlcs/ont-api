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

package ru.avicomp.ontapi.tests;

import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyID;
import ru.avicomp.ontapi.OntologyID;
import ru.avicomp.ontapi.jena.OntModelFactory;
import ru.avicomp.ontapi.jena.model.OntGraphModel;

/**
 * To test {@link OntologyID} and related functionality.
 *
 * Created by @ssz on 13.09.2018.
 */
public class OntologyIDTest {

    @Test
    public void testAnonymousID() {
        OntGraphModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OWLOntologyID id = new OntologyID(m.getID());
        Assert.assertTrue(id.isAnonymous());
        Assert.assertFalse(id.getOntologyIRI().isPresent());
        Assert.assertFalse(id.getVersionIRI().isPresent());
        Assert.assertEquals(17 + 37 * m.getID().asNode().getBlankNodeLabel().hashCode(), id.hashCode());
    }

    @Test
    public void testOntologyIRI() {
        String uri = "http://ex";
        OntGraphModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OWLOntologyID id = new OntologyID(m.setID(uri));
        Assert.assertFalse(id.isAnonymous());
        Assert.assertTrue(id.getOntologyIRI().isPresent());
        Assert.assertFalse(id.getVersionIRI().isPresent());
        Assert.assertEquals(uri, id.getOntologyIRI().map(IRI::getIRIString).orElseThrow(AssertionError::new));
        OWLOntologyID owl = new OWLOntologyID(IRI.create(uri));
        Assert.assertEquals(owl, id);
        Assert.assertEquals(owl.hashCode(), id.hashCode());
        Assert.assertEquals(owl.toString(), id.toString());
    }

    @Test
    public void testVersionIRI() {
        String uri = "http://ex";
        String ver = "http://v";
        OntGraphModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD)
                .setID(uri).setVersionIRI(ver).getModel();
        OWLOntologyID id = new OntologyID(m.getID());
        Assert.assertFalse(id.isAnonymous());
        Assert.assertTrue(id.getOntologyIRI().isPresent());
        Assert.assertTrue(id.getVersionIRI().isPresent());
        Assert.assertEquals(uri, id.getOntologyIRI().map(IRI::getIRIString).orElseThrow(AssertionError::new));
        Assert.assertEquals(ver, id.getVersionIRI().map(IRI::getIRIString).orElseThrow(AssertionError::new));
        OWLOntologyID owl = new OWLOntologyID(IRI.create(uri), IRI.create(ver));
        Assert.assertEquals(owl, id);
        Assert.assertEquals(owl.hashCode(), id.hashCode());
        Assert.assertEquals(owl.toString(), id.toString());
    }

    @Test
    public void testCompareIDs() {
        OWLOntologyID owl1 = new OWLOntologyID(IRI.create("x://A"), IRI.create("x://B"));
        OntologyID ont1 = OntologyID.asONT(owl1);
        Assert.assertFalse(ont1.isAnonymous());
        Assert.assertTrue(ont1.getOntologyIRI().isPresent());
        Assert.assertTrue(ont1.getVersionIRI().isPresent());
        Assert.assertEquals(owl1, ont1);
        Assert.assertEquals(owl1.hashCode(), ont1.hashCode());
        Assert.assertEquals(owl1.toString(), ont1.toString());

        OWLOntologyID owl2 = new OWLOntologyID(IRI.create("x://C"));
        OntologyID ont2 = OntologyID.asONT(owl2);
        Assert.assertFalse(ont2.isAnonymous());
        Assert.assertTrue(ont2.getOntologyIRI().isPresent());
        Assert.assertFalse(ont2.getVersionIRI().isPresent());
        Assert.assertEquals(owl2, ont2);
        Assert.assertEquals(owl2.hashCode(), ont2.hashCode());
        Assert.assertEquals(owl2.toString(), ont2.toString());

        OWLOntologyID owl3 = new OWLOntologyID();
        OntologyID ont3 = OntologyID.asONT(owl3);
        Assert.assertTrue(ont3.isAnonymous());
        Assert.assertFalse(ont3.getOntologyIRI().isPresent());
        Assert.assertFalse(ont3.getVersionIRI().isPresent());
    }
}
