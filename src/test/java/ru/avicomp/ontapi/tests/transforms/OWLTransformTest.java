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

package ru.avicomp.ontapi.tests.transforms;

import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avicomp.ontapi.OntManagers;
import ru.avicomp.ontapi.OntologyManager;
import ru.avicomp.ontapi.OntologyModel;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

import java.nio.file.Paths;

/**
 * Created by @szuev on 01.04.2018.
 */
public class OWLTransformTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(OWLTransformTest.class);

    @Test
    public void testCardinalityRestrictions() throws Exception {
        IRI iri = IRI.create(Paths.get(OWLTransformTest.class.getResource("/owlapi/owl11/family/family.owl").toURI()).toRealPath().toUri());
        LOGGER.debug("IRI {}", iri);
        OntologyManager m = OntManagers.createONT();
        OntologyModel o = m.loadOntology(iri);
        ReadWriteUtils.print(o.asGraphModel());
        o.axioms().forEach(x -> LOGGER.debug("{}", x));
        Assert.assertEquals(136, o.getAxiomCount());
    }
}
