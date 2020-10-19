/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2020, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.tests.jena;

import com.github.owlcs.ontapi.jena.OntJenaException;
import com.github.owlcs.ontapi.jena.OntModelFactory;
import com.github.owlcs.ontapi.jena.model.*;
import com.github.owlcs.ontapi.jena.vocabulary.OWL;
import com.github.owlcs.ontapi.jena.vocabulary.RDF;
import com.github.owlcs.ontapi.utils.ReadWriteUtils;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * To test {@link OntIndividual}.
 * <p>
 * Created by @ssz on 11.05.2019.
 */
public class OntIndividualTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(OntIndividualTest.class);

    @Test
    public void testPositiveAssertions() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntIndividual i1 = m.createIndividual("I1");
        OntIndividual i2 = m.createIndividual("I2");
        OntDataProperty d = m.createDataProperty("D");
        OntObjectProperty p = m.createObjectProperty("P");
        OntAnnotationProperty a = m.getRDFSComment();

        Assertions.assertSame(i1, i1.addAssertion(d, m.createLiteral("1"))
                .addAssertion(d, m.createLiteral("2"))
                .addAssertion(p.asNamed(), i2)
                .addAssertion(a, m.createLiteral("3")));
        Assertions.assertEquals(4, i1.positiveAssertions().count());
        Assertions.assertEquals(2, i1.positiveAssertions(d).count());
        Assertions.assertEquals(8, m.size());

        Assertions.assertSame(i1, i1.removeAssertion(d, null).removeAssertion(p.asNamed(), i2));
        Assertions.assertEquals(1, i1.positiveAssertions().count());
        Assertions.assertSame(i1, i1.removeAssertion(null, null));
        Assertions.assertEquals(0, i1.positiveAssertions().count());
        Assertions.assertEquals(4, m.size());
    }

    @Test
    public void testDifferentIndividuals() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntIndividual a = m.createIndividual("A");
        OntIndividual b = m.createOntClass("C1").createIndividual("B");
        OntIndividual c = m.createOntClass("C2").createIndividual();
        OntIndividual d = m.createIndividual("D");

        Assertions.assertNotNull(a.addDifferentFromStatement(b));
        Assertions.assertSame(a, a.addDifferentIndividual(c).addDifferentIndividual(d).removeDifferentIndividual(b));
        Assertions.assertEquals(2, a.differentIndividuals().count());
        Assertions.assertSame(a, a.removeDifferentIndividual(null));
        Assertions.assertEquals(7, m.size());
    }

    @Test
    public void testSameIndividuals() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntIndividual a = m.createIndividual("A");
        OntIndividual b = m.createOntClass("C1").createIndividual("B");
        OntIndividual c = m.createOntClass("C2").createIndividual();
        OntIndividual d = m.createIndividual("D");

        Assertions.assertNotNull(a.addSameAsStatement(b));
        Assertions.assertSame(a, a.addSameIndividual(c).addSameIndividual(d).removeSameIndividual(b));
        Assertions.assertEquals(2, a.sameIndividuals().count());
        Assertions.assertSame(a, a.removeSameIndividual(null));
        Assertions.assertEquals(7, m.size());
    }

    @Test
    public void testNegativeAssertions() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntIndividual i1 = m.createIndividual("I1");
        OntIndividual i2 = m.createIndividual("I2");
        OntIndividual i3 = m.createIndividual("I3");
        OntDataProperty d = m.createDataProperty("D");
        OntObjectProperty p = m.createObjectProperty("P");

        Assertions.assertSame(i1, i1.addNegativeAssertion(d, m.createLiteral("1")));
        Assertions.assertEquals(1, i1.negativeAssertions().count());
        Assertions.assertEquals(0, i1.positiveAssertions().count());
        Assertions.assertSame(i1, i1.addNegativeAssertion(d, m.createLiteral("2"))
                .addNegativeAssertion(p, i2).addNegativeAssertion(p, i3));
        Assertions.assertEquals(4, i1.negativeAssertions().count());
        Assertions.assertEquals(0, i1.positiveAssertions().count());

        Assertions.assertEquals(2, m.statements(null, OWL.targetIndividual, null).count());
        Assertions.assertEquals(2, m.statements(null, OWL.targetValue, null).count());
        Assertions.assertEquals(21, m.size());

        Assertions.assertSame(i1, i1.removeNegativeAssertion(d, null).removeNegativeAssertion(p.asNamed(), i3));
        Assertions.assertEquals(1, i1.negativeAssertions().count());
        Assertions.assertEquals(1, m.statements(null, OWL.targetIndividual, null).count());
        Assertions.assertEquals(0, m.statements(null, OWL.targetValue, null).count());

        Assertions.assertSame(i1, i1.removeNegativeAssertion(null, null));
        Assertions.assertEquals(5, m.size());
    }

    @Test
    public void testRemoveIndividual() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntIndividual i1 = m.createIndividual("I1");
        OntIndividual i2 = m.createIndividual("I2");
        OntIndividual i3 = m.createIndividual("I3");
        OntDataProperty d = m.createDataProperty("D");
        OntObjectProperty p = m.createObjectProperty("P");

        i1.addNegativeAssertion(p, i2)
                .addAssertion(p.asNamed(), i3)
                .addNegativeAssertion(d, m.createLiteral("1"))
                .addAssertion(d, m.createLiteral("2")).addComment("The individual to test");
        Assertions.assertEquals(16, m.size());

        Assertions.assertEquals(4, m.removeOntObject(i1).size());
    }

    @Test
    public void testClassAssertions() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntIndividual i1 = m.createIndividual("I1");
        OntClass c1 = m.createOntClass("C1");
        OntClass c2 = m.createOntClass("C2");
        OntIndividual i2 = c2.createIndividual();
        long size = 4;

        Assertions.assertSame(i1, i1.detachClass(c1).detachClass(m.getOWLThing()));
        Assertions.assertEquals(size, m.size());

        Assertions.assertNotNull(i1.addClassAssertion(c2));
        Assertions.assertSame(i1, i1.attachClass(c2).attachClass(c1).attachClass(m.getOWLThing()));
        Assertions.assertEquals(3, i1.classes().count());
        Assertions.assertEquals(size + 3, m.size());

        Assertions.assertSame(i1, i1.detachClass(c2));
        Assertions.assertEquals(size + 2, m.size());
        Assertions.assertEquals(2, i1.classes().count());
        Assertions.assertSame(i1, i1.detachClass(null));
        Assertions.assertEquals(size, m.size());
        Assertions.assertEquals(0, i1.classes().count());

        Assertions.assertSame(i2, i2.attachClass(m.getOWLThing()).attachClass(c1).attachClass(c2));
        Assertions.assertEquals(size + 2, m.size());
        Assertions.assertEquals(3, i2.classes().count());

        // not possible to delete all class assertions:
        try {
            i2.detachClass(null);
            Assertions.fail("Possible to delete all class expressions");
        } catch (OntJenaException j) {
            LOGGER.debug("Expected: {}", j.getMessage());
        }
        Assertions.assertEquals(size + 2, m.size());
        Assertions.assertSame(i2, i2.detachClass(c2).detachClass(m.getOWLThing()));
        Assertions.assertEquals(1, i2.classes().count());
        Assertions.assertEquals(size, m.size());

        // not possible to delete the last class assertions:
        try {
            i2.detachClass(c1);
            Assertions.fail("Possible to delete the last class expressions");
        } catch (OntJenaException j) {
            LOGGER.debug("Expected: {}", j.getMessage());
        }
        Assertions.assertEquals(size, m.size());
    }

    @Test
    public void testListIndividuals() {
        String ns = "http://xx#";
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD).setNsPrefix("x", ns);
        m.getOWLThing().createIndividual(ns + "I1").addSameAsStatement(m.createIndividual(ns + "I2"));
        OntClass.Named c1 = m.createOntClass(ns + "C1");
        OntClass.Named c2 = m.createOntClass(ns + "C2");
        OntClass.Named c3 = m.createOntClass(ns + "C3");
        m.createIndividual(ns + "I3")
                .addDifferentIndividual(m.createIndividual(ns + "I4"))
                .addDifferentIndividual(c3.createIndividual());
        c1.createIndividual(ns + "I5");
        c2.createIndividual(ns + "I5");
        c2.createIndividual(ns + "I3");
        c3.createIndividual(ns + "I3");
        ReadWriteUtils.print(m);

        // class-assertions:
        Assertions.assertEquals(6, m.statements(null, RDF.type, null)
                .filter(x -> x.getObject().canAs(OntClass.class)).count());
        // all individuals:
        Assertions.assertEquals(6, m.ontObjects(OntIndividual.class).count());
        // distinct class asserted individuals:
        Assertions.assertEquals(4, m.individuals().count());
        // named individuals:
        Assertions.assertEquals(5, m.namedIndividuals().peek(x -> Assertions.assertTrue(x.isURIResource())).count());
    }
}
