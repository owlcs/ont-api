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
package com.github.owlcs.owlapi.tests.api.ontology;

import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyID;

import java.util.Optional;

public class OntologyIDTestCase extends TestBase {

    private static final String TEST_ONTOLOGY_IRI_STRING = "http://test.it/check1";

    @Test
    public void testShouldFindSameHashCode() {
        IRI iri1 = IRI.create(TEST_ONTOLOGY_IRI_STRING, "");
        IRI iri2 = IRI.create(TEST_ONTOLOGY_IRI_STRING, "");
        Assertions.assertEquals(iri1.hashCode(), iri2.hashCode());
        Assertions.assertEquals(iri1, iri2);
    }

    @Test
    public void testShouldFindSameHashCodeForIDs() {
        IRI iri1 = IRI.create(TEST_ONTOLOGY_IRI_STRING, "");
        IRI iri2 = IRI.create(TEST_ONTOLOGY_IRI_STRING, "");
        Assertions.assertEquals(iri1.hashCode(), iri2.hashCode());
        OWLOntologyID id1 = new OWLOntologyID(Optional.of(iri1), Optional.empty());
        OWLOntologyID id2 = new OWLOntologyID(Optional.of(iri2), Optional.empty());
        Assertions.assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    public void testShouldFindSameHashCodeForIDs2() {
        IRI iri1 = IRI.create(TEST_ONTOLOGY_IRI_STRING, "");
        IRI iri2 = IRI.create(TEST_ONTOLOGY_IRI_STRING, "");
        Assertions.assertEquals(iri1.hashCode(), iri2.hashCode());
        OWLOntologyID id1 = new OWLOntologyID(Optional.of(iri1), Optional.empty());
        OWLOntologyID id2 = new OWLOntologyID(Optional.of(iri2), Optional.empty());
        Assertions.assertEquals(id1.hashCode(), id2.hashCode());
        Assertions.assertEquals(id1, id2);
    }

    @Test
    public void testUnequalIdsUnequal() {
        OWLOntologyID id1 = new OWLOntologyID(Optional.of(IRI.create("http://www.w3.org/", "foo")), Optional.empty());
        OWLOntologyID id2 = new OWLOntologyID(Optional.of(IRI.create("http://www.w3.org/", "bar")), Optional.empty());
        Assertions.assertNotEquals(id1.hashCode(), id2.hashCode());
        Assertions.assertNotEquals(id1, id2);
    }

    // this is an experiment, if the manager were to keep all versions of an
    // ontology together in a multimap or something
    @Test
    public void testVersionedIDComparisons() {
        IRI iri1 = IRI.create(TEST_ONTOLOGY_IRI_STRING, "");
        IRI iri2 = IRI.create(TEST_ONTOLOGY_IRI_STRING, "");
        Assertions.assertEquals(iri1.hashCode(), iri2.hashCode());
        Assertions.assertEquals(iri1, iri2);
        OWLOntologyID unversionedID = new OWLOntologyID(Optional.of(iri1), Optional.empty());
        IRI version1IRI = IRI.create(TEST_ONTOLOGY_IRI_STRING + "/", "version1");
        OWLOntologyID version1ID = new OWLOntologyID(Optional.of(iri2), Optional.of(version1IRI));
        Assertions.assertEquals(unversionedID.getOntologyIRI(), version1ID.getOntologyIRI());
        Assertions.assertNotEquals(unversionedID.getVersionIRI(), version1ID.getVersionIRI());
        Assertions.assertNotEquals(unversionedID.hashCode(), version1ID.hashCode());
        Assertions.assertNotEquals(unversionedID, version1ID);
        OWLOntologyID duplicateVersion1ID = new OWLOntologyID(Optional.of(IRI.create(TEST_ONTOLOGY_IRI_STRING, "")),
                Optional.of(version1IRI));
        Assertions.assertEquals(version1ID, duplicateVersion1ID);
        OWLOntologyID differentBasedVersion1ID = new OWLOntologyID(Optional.of(IRI.create(TEST_ONTOLOGY_IRI_STRING +
                "-of-doom", "")), Optional.of(version1IRI));
        Assertions.assertNotEquals(version1ID, differentBasedVersion1ID);
        IRI version2IRI = IRI.create(TEST_ONTOLOGY_IRI_STRING + "/", "version2");
        IRI iri3 = IRI.create(TEST_ONTOLOGY_IRI_STRING, "");
        OWLOntologyID version2ID = new OWLOntologyID(Optional.of(iri3), Optional.of(version2IRI));
        Assertions.assertNotEquals(version1ID.hashCode(), version2ID.hashCode());
        Assertions.assertNotEquals(version1ID, version2ID);
    }
}
