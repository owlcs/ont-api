/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2021, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.tests.internal;

import com.github.owlcs.ontapi.internal.ONTObject;
import com.github.owlcs.ontapi.internal.ONTWrapperImpl;
import com.github.owlcs.ontapi.jena.OntModelFactory;
import com.github.owlcs.ontapi.jena.model.OntClass;
import com.github.owlcs.ontapi.jena.model.OntDisjoint;
import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.owlapi.axioms.DisjointClassesAxiomImpl;
import com.github.owlcs.ontapi.utils.ReadWriteUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;

/**
 * Created by @ssz on 06.03.2019.
 */
public class ONTWrappersTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ONTWrappersTest.class);

    @Test
    public void testDisjoint() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntClass c1 = m.createOntClass("C1");
        OntDisjoint.Classes c = m.createDisjointClasses(Arrays.asList(c1,
                m.createOntClass("C2"),
                m.createObjectUnionOf(Arrays.asList(m.createOntClass("C3"), m.getOWLThing()))));

        ReadWriteUtils.print(m);
        ONTObject<OWLDisjointClassesAxiom> ax =
                ONTWrapperImpl.create(new DisjointClassesAxiomImpl(Collections.emptySet(), Collections.emptySet()), c);

        Assertions.assertEquals(8, ax.triples().peek(x -> LOGGER.debug("{}", x)).count());
        LOGGER.debug("---");
        c.getList().addFirst(m.createOntClass("C3"));
        Assertions.assertEquals(10, ax.triples().peek(x -> LOGGER.debug("{}", x)).count());
    }
}
