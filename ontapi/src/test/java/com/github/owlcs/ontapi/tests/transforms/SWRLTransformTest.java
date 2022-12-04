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

package com.github.owlcs.ontapi.tests.transforms;

import com.github.owlcs.ontapi.OntFormat;
import com.github.owlcs.ontapi.OntManagers;
import com.github.owlcs.ontapi.Ontology;
import com.github.owlcs.ontapi.OntologyManager;
import com.github.owlcs.ontapi.testutils.OWLIOUtils;
import com.github.owlcs.ontapi.testutils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.model.AxiomType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by @ssz on 08.03.2019.
 */
public class SWRLTransformTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SWRLTransformTest.class);

    @Test
    public void testWrongSWRLOntologyWithoutTransform() throws Exception {
        OWLOntologyDocumentSource src = OWLIOUtils.getFileDocumentSource("/ontapi/anyURI-premise.rdf", OntFormat.RDF_XML);
        LOGGER.debug("Source: {}", src);
        OntologyManager m = OntManagers.createManager();
        m.getOntologyConfigurator().setPerformTransformation(false);
        Ontology o = m.loadOntologyFromOntologyDocument(src);

        OWLIOUtils.print(o);

        TestUtils.assertAxiom(o, AxiomType.DECLARATION, 6);
        TestUtils.assertAxiom(o, AxiomType.DATA_PROPERTY_ASSERTION, 6);
        TestUtils.assertAxiom(o, AxiomType.CLASS_ASSERTION, 1);
        TestUtils.assertAxiom(o, AxiomType.SWRL_RULE, 0);
        Assertions.assertEquals(13, o.axioms().count());
    }

    @Test
    public void testWrongSWRLOntologyWithTransform() throws Exception {
        OWLOntologyDocumentSource src = OWLIOUtils.getFileDocumentSource("/ontapi/anyURI-premise.rdf", OntFormat.RDF_XML);
        LOGGER.debug("Source: {}", src);
        OntologyManager m = OntManagers.createManager();
        Ontology o = m.loadOntologyFromOntologyDocument(src);

        OWLIOUtils.print(o);

        TestUtils.assertAxiom(o, AxiomType.DECLARATION, 8);
        TestUtils.assertAxiom(o, AxiomType.DATA_PROPERTY_ASSERTION, 6);
        TestUtils.assertAxiom(o, AxiomType.CLASS_ASSERTION, 1);
        TestUtils.assertAxiom(o, AxiomType.SWRL_RULE, 1);
        Assertions.assertEquals(16, o.axioms().count());
    }
}
