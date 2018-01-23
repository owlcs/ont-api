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

package ru.avicomp.ontapi.tests;

import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.OntManagers;
import ru.avicomp.ontapi.OntologyModel;
import ru.avicomp.ontapi.jena.impl.OntCEImpl;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

/**
 * For testing miscellaneous general functionality.
 * <p>
 * Created by @szuev on 23.01.2018.
 */
public class MiscOntologyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(MiscOntologyTest.class);

    @Test(expected = OntApiException.class) // not a StackOverflowError
    public void testReadRecursiveGraph() throws OWLOntologyCreationException {
        IRI iri = IRI.create(ReadWriteUtils.getResourceURI("recursive-graph.ttl"));
        LOGGER.debug("The file: {}", iri);
        OntologyModel m = OntManagers.createONT().loadOntology(iri);
        m.asGraphModel().write(System.out, "ttl");
        m.axioms().forEach(a -> LOGGER.debug("{}", a));
    }
}
