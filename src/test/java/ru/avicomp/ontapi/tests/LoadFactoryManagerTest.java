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

package ru.avicomp.ontapi.tests;

import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.io.UnparsableOntologyException;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avicomp.ontapi.OntManagers;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

/**
 * To test loading mechanisms from {@link ru.avicomp.ontapi.OntFactoryImpl}
 * <p>
 * Created by @szuev on 16.01.2018.
 */
public class LoadFactoryManagerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoadFactoryManagerTest.class);

    @Test
    public void testLoadWrongDuplicate() throws OWLOntologyCreationException {
        IRI a = IRI.create(ReadWriteUtils.getResourceURI("load-test-a.owl"));
        IRI b = IRI.create(ReadWriteUtils.getResourceURI("load-test-b.ttl"));

        OWLOntologyManager m = OntManagers.createONT();
        OWLOntology o = m.loadOntologyFromOntologyDocument(a);
        Assert.assertEquals(1, m.ontologies().count());
        Assert.assertNotNull(o.getOWLOntologyManager());
        String comment = getComment(o);
        LOGGER.debug("Ontology comment '{}'", comment);

        try {
            m.loadOntologyFromOntologyDocument(b);
        } catch (UnparsableOntologyException e) {
            LOGGER.info("Exception: {}", e.getMessage().substring(0, Math.min(100, e.getMessage().length())));
        }
        // Note: the different with OWL-API (5.1.4) : no ontologies inside manager. Believe it is a bug of OWL-API.
        Assert.assertEquals("Wrong count", 1, m.ontologies().count());
        Assert.assertNotNull("No manager", o.getOWLOntologyManager());
        Assert.assertSame(o, m.ontologies().findFirst().orElseThrow(AssertionError::new));
        Assert.assertEquals(comment, getComment(o));
    }

    private static String getComment(OWLOntology o) {
        return o.annotations().map(OWLAnnotation::getValue)
                .map(OWLAnnotationValue::asLiteral)
                .map(x -> x.orElseThrow(() -> new AssertionError("Empty comment")))
                .map(OWLLiteral::getLiteral)
                .findFirst().orElseThrow(() -> new AssertionError("No comment."));
    }

}
