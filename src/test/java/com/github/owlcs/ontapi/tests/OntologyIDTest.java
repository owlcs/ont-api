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

package com.github.owlcs.ontapi.tests;

import com.github.owlcs.ontapi.ID;
import org.apache.jena.ontapi.OntModelFactory;
import org.apache.jena.ontapi.model.OntModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyID;

/**
 * To test {@link ID} and related functionality.
 * <p>
 * Created by @ssz on 13.09.2018.
 */
public class OntologyIDTest {

    @Test
    public void testAnonymousID() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OWLOntologyID id = new ID(m.getID());
        Assertions.assertTrue(id.isAnonymous());
        Assertions.assertFalse(id.getOntologyIRI().isPresent());
        Assertions.assertFalse(id.getVersionIRI().isPresent());
        Assertions.assertEquals(17 + 37 * m.getID().asNode().getBlankNodeLabel().hashCode(), id.hashCode());
    }

    @Test
    public void testOntologyIRI() {
        String uri = "http://ex";
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OWLOntologyID id = new ID(m.setID(uri));
        Assertions.assertFalse(id.isAnonymous());
        Assertions.assertTrue(id.getOntologyIRI().isPresent());
        Assertions.assertFalse(id.getVersionIRI().isPresent());
        Assertions.assertEquals(uri, id.getOntologyIRI().map(IRI::getIRIString).orElseThrow(AssertionError::new));
        OWLOntologyID owl = new OWLOntologyID(IRI.create(uri));
        Assertions.assertEquals(owl, id);
        Assertions.assertEquals(owl.hashCode(), id.hashCode());
        Assertions.assertEquals(owl.toString(), id.toString());
    }

    @Test
    public void testVersionIRI() {
        String uri = "http://ex";
        String ver = "http://v";
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD)
                .setID(uri).setVersionIRI(ver).getModel();
        OWLOntologyID id = new ID(m.getID());
        Assertions.assertFalse(id.isAnonymous());
        Assertions.assertTrue(id.getOntologyIRI().isPresent());
        Assertions.assertTrue(id.getVersionIRI().isPresent());
        Assertions.assertEquals(uri, id.getOntologyIRI().map(IRI::getIRIString).orElseThrow(AssertionError::new));
        Assertions.assertEquals(ver, id.getVersionIRI().map(IRI::getIRIString).orElseThrow(AssertionError::new));
        OWLOntologyID owl = new OWLOntologyID(IRI.create(uri), IRI.create(ver));
        Assertions.assertEquals(owl, id);
        Assertions.assertEquals(owl.hashCode(), id.hashCode());
        Assertions.assertEquals(owl.toString(), id.toString());
    }

    @Test
    public void testCompareIDs() {
        OWLOntologyID owl1 = new OWLOntologyID(IRI.create("x://A"), IRI.create("x://B"));
        ID ont1 = ID.asONT(owl1);
        Assertions.assertFalse(ont1.isAnonymous());
        Assertions.assertTrue(ont1.getOntologyIRI().isPresent());
        Assertions.assertTrue(ont1.getVersionIRI().isPresent());
        Assertions.assertEquals(owl1, ont1);
        Assertions.assertEquals(owl1.hashCode(), ont1.hashCode());
        Assertions.assertEquals(owl1.toString(), ont1.toString());

        OWLOntologyID owl2 = new OWLOntologyID(IRI.create("x://C"));
        ID ont2 = ID.asONT(owl2);
        Assertions.assertFalse(ont2.isAnonymous());
        Assertions.assertTrue(ont2.getOntologyIRI().isPresent());
        Assertions.assertFalse(ont2.getVersionIRI().isPresent());
        Assertions.assertEquals(owl2, ont2);
        Assertions.assertEquals(owl2.hashCode(), ont2.hashCode());
        Assertions.assertEquals(owl2.toString(), ont2.toString());

        OWLOntologyID owl3 = new OWLOntologyID();
        ID ont3 = ID.asONT(owl3);
        Assertions.assertTrue(ont3.isAnonymous());
        Assertions.assertFalse(ont3.getOntologyIRI().isPresent());
        Assertions.assertFalse(ont3.getVersionIRI().isPresent());
    }
}
