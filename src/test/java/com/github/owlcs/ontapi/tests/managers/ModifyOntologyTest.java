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

package com.github.owlcs.ontapi.tests.managers;

import com.github.owlcs.ontapi.DataFactory;
import com.github.owlcs.ontapi.OntManagers;
import com.github.owlcs.ontapi.OntologyManager;
import com.github.owlcs.ontapi.OntologyModel;
import com.github.owlcs.ontapi.jena.model.OntGraphModel;
import com.github.owlcs.ontapi.utils.ReadWriteUtils;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.RemoveAxiom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * To test add/remove through manager.
 * Created by @ssz on 31.10.2019.
 */
public class ModifyOntologyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModifyOntologyTest.class);

    @Test
    public void testBulkRemoveAxioms() {
        String ns = "http://x#";
        OntologyManager m = OntManagers.createONT();

        OntologyModel o = m.createOntology();
        OntGraphModel g = o.asGraphModel();
        g.createOntClass(ns + "X").addSuperClass(g.createOntClass(ns + "Y"));
        ReadWriteUtils.print(g);
        Assert.assertEquals(3, o.axioms().peek(x -> LOGGER.debug("(1): {}", x)).count());

        List<RemoveAxiom> changes = o.axioms().map(x -> new RemoveAxiom(o, x)).collect(Collectors.toList());
        m.applyChanges(changes);
        ReadWriteUtils.print(g);

        Assert.assertEquals(0, o.axioms().peek(x -> LOGGER.debug("(2): {}", x)).count());
        Assert.assertEquals(1, g.size());
    }

    @Test
    public void testBulkAddAxioms() {
        String ns = "http://x#";
        OntologyManager m = OntManagers.createONT();
        DataFactory df = m.getOWLDataFactory();
        List<OWLAxiom> axioms = new ArrayList<>();
        axioms.add(df.getOWLDeclarationAxiom(df.getOWLClass(ns + "X")));
        axioms.add(df.getOWLDeclarationAxiom(df.getOWLClass(ns + "Y")));
        axioms.add(df.getOWLSubClassOfAxiom(df.getOWLClass(ns + "X"), df.getOWLClass(ns + "Y")));
        axioms.add(df.getOWLSubClassOfAxiom(df.getOWLClass(ns + "Z"), df.getOWLClass(ns + "W")));

        OntologyModel o = m.createOntology();
        List<AddAxiom> changes = axioms.stream().map(x -> new AddAxiom(o, x)).collect(Collectors.toList());
        m.applyChanges(changes);
        ReadWriteUtils.print(o);
        Assert.assertEquals(4, o.axioms().peek(x -> LOGGER.debug("(1): {}", x)).count());
        Assert.assertEquals(7, o.asGraphModel().size());
    }
}
