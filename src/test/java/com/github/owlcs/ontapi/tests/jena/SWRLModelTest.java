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

package com.github.owlcs.ontapi.tests.jena;

import com.github.owlcs.ontapi.OntFormat;
import com.github.owlcs.ontapi.jena.OntModelFactory;
import com.github.owlcs.ontapi.jena.model.*;
import com.github.owlcs.ontapi.jena.vocabulary.RDF;
import com.github.owlcs.ontapi.jena.vocabulary.SWRL;
import com.github.owlcs.ontapi.jena.vocabulary.SWRLB;
import com.github.owlcs.ontapi.utils.ReadWriteUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.XSD;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;

/**
 * Created by @ssz on 09.03.2019.
 */
public class SWRLModelTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SWRLModelTest.class);

    private static OntSWRL.Variable getVariable(OntModel m, String localName) {
        return m.ontObjects(OntSWRL.Variable.class)
                .filter(r -> localName.equals(r.getLocalName())).findFirst().orElseThrow(AssertionError::new);
    }

    @Test
    public void testSWRLObjectsOnFreshOntology() {
        String uri = "http://test.com/swrl-1";
        String ns = uri + "#";

        OntModel m = OntModelFactory.createModel()
                .setID(uri).getModel()
                .setNsPrefix("test", ns)
                .setNsPrefix("SWRL", SWRL.NS)
                .setNsPrefixes(OntModelFactory.STANDARD);

        OntClass.Named cl1 = m.createOntClass(ns + "Class1");
        OntClass.Named cl2 = m.createOntClass(ns + "Class2");
        OntDataProperty d = m.createDataProperty(ns + "DP");
        OntObjectProperty.Named p = m.createObjectProperty(ns + "OP");
        OntIndividual i1 = cl1.createIndividual(ns + "Individual1");

        OntClass.OneOf cl3 = m.createOneOf(i1);
        OntIndividual i2 = cl3.createIndividual();

        OntSWRL.Variable var1 = m.createSWRLVariable(ns + "Variable1");
        OntSWRL.DArg dArg1 = m.createTypedLiteral(12).inModel(m).as(OntSWRL.DArg.class);
        OntSWRL.DArg dArg2 = var1.as(OntSWRL.DArg.class);

        OntSWRL.Atom.WithBuiltin atom1 = m.createBuiltInSWRLAtom(m.createResource(ns + "AtomPredicate1"),
                Arrays.asList(dArg1, dArg2));
        OntSWRL.Atom.WithClass atom2 = m.createClassSWRLAtom(cl2, i2.as(OntSWRL.IArg.class));
        OntSWRL.Atom.WithSameIndividuals atom3 = m.createSameIndividualsSWRLAtom(i1.as(OntSWRL.IArg.class),
                var1.as(OntSWRL.IArg.class));
        OntSWRL.Atom.WithDataProperty atom4 = m.createDataPropertySWRLAtom(d, i2.as(OntSWRL.IArg.class), dArg2);
        OntSWRL.Atom.WithObjectProperty atom5 = m.createObjectPropertySWRLAtom(p, var1, i1.as(OntSWRL.IArg.class));

        OntSWRL.Imp imp = m.createSWRLImp(Collections.singletonList(atom1), Arrays.asList(atom2, atom3, atom4, atom5));
        imp.addAnnotation(m.getRDFSComment(), "This is SWRL Imp").annotate(m.getRDFSLabel(), cl1.createIndividual());

        ReadWriteUtils.print(m);

        Assert.assertEquals(2, atom1.arguments().count());
        Assert.assertEquals(1, atom2.arguments().count());
        Assert.assertEquals(2, atom3.arguments().count());
        Assert.assertEquals(2, atom4.arguments().count());
        Assert.assertEquals(2, atom5.arguments().count());

        Assert.assertEquals(18, imp.spec().peek(x -> LOGGER.debug("Imp Spec: {}", x)).count());
        Assert.assertEquals(8, atom1.spec().peek(x -> LOGGER.debug("BuiltIn Spec: {}", x)).count());
        Assert.assertEquals(3, atom2.spec().peek(x -> LOGGER.debug("Classes Spec: {}", x)).count());
        Assert.assertEquals(4, atom3.spec().peek(x -> LOGGER.debug("Individuals Spec: {}", x)).count());
        Assert.assertEquals(4, atom4.spec().peek(x -> LOGGER.debug("DataProperies Spec: {}", x)).count());
        Assert.assertEquals(4, atom5.spec().peek(x -> LOGGER.debug("ObjectProperies Spec: {}", x)).count());

        // literals(2) and variables(1):
        LOGGER.debug("All D-Args:");
        Assert.assertEquals("Incorrect count of SWRL D-Arg", 3,
                m.ontObjects(OntSWRL.DArg.class).map(String::valueOf).peek(LOGGER::debug).count());
        // individuals(2 anonymous, 1 named) and variables(1):
        LOGGER.debug("All I-Args:");
        Assert.assertEquals("Incorrect count of SWRL I-Arg", 4,
                m.ontObjects(OntSWRL.IArg.class).map(String::valueOf).peek(LOGGER::debug).count());

        Assert.assertEquals(1, m.ontObjects(OntSWRL.Builtin.class).peek(x -> LOGGER.debug("Builtin: {}", x)).count());

        Assert.assertEquals("Incorrect count of atoms", 5, m.ontObjects(OntSWRL.Atom.class).count());
        Assert.assertEquals("Incorrect count of unary atoms", 1, m.ontObjects(OntSWRL.Atom.Unary.class).count());
        Assert.assertEquals("Incorrect count of binary atoms", 3, m.ontObjects(OntSWRL.Atom.Binary.class).count());
        Assert.assertEquals("Incorrect count of variables", 1, m.ontObjects(OntSWRL.Variable.class).count());
        Assert.assertEquals("Incorrect count of SWRL:Imp", 1, m.ontObjects(OntSWRL.Imp.class).count());
        Assert.assertEquals("Incorrect count of SWRL Objects", 8,
                m.ontObjects(OntSWRL.class).peek(x -> LOGGER.debug("SWRL Obj: {}", x)).count());

        Assert.assertEquals(5, m.statements(null, RDF.type, SWRL.AtomList)
                .map(OntStatement::getSubject)
                .map(s -> s.as(RDFList.class))
                .peek(s -> LOGGER.debug("SWRL-List: {}", s.asJavaList()))
                .count());
    }

    @Test
    public void testSWRLObjectsOnLoadOntology() {
        Graph g = ReadWriteUtils.loadResourceAsModel("/owlapi/owlapi/SWRLTest.owl", OntFormat.RDF_XML).getGraph();
        OntModel m = OntModelFactory.createModel(g);

        ReadWriteUtils.print(m);

        Assert.assertEquals(1, m.ontObjects(OntSWRL.Imp.class).count());
        Assert.assertEquals(3, m.ontObjects(OntSWRL.Atom.WithObjectProperty.class).count());
        Assert.assertEquals(0, m.ontObjects(OntSWRL.Atom.WithDifferentIndividuals.class).count());
        Assert.assertEquals(0, m.ontObjects(OntSWRL.Atom.WithDataRange.class).count());
        Assert.assertEquals(3, m.ontObjects(OntSWRL.Atom.class).count());
        Assert.assertEquals(0, m.ontObjects(OntSWRL.Atom.WithBuiltin.class).count());
        Assert.assertEquals(0, m.ontObjects(OntSWRL.Atom.Unary.class).count());
        Assert.assertEquals(3, m.ontObjects(OntSWRL.Atom.Binary.class).count());
        Assert.assertEquals(3, m.ontObjects(OntSWRL.Variable.class).count());
        Assert.assertEquals(3, m.ontObjects(OntSWRL.IArg.class).count());
        Assert.assertEquals(3, m.ontObjects(OntSWRL.DArg.class).count());
        Assert.assertEquals(3, m.ontObjects(OntSWRL.Arg.class).count());
        Assert.assertEquals(0, m.ontObjects(OntSWRL.Builtin.class).count());
        Assert.assertEquals(7, m.ontObjects(OntSWRL.class).count());

        OntSWRL.Imp imp = m.ontObjects(OntSWRL.Imp.class).findFirst().orElseThrow(AssertionError::new);
        Assert.assertEquals(2, imp.getBodyList().members().count());
        Assert.assertEquals(1, imp.getHeadList().members().count());

        OntSWRL.Variable x = getVariable(m, "x");
        OntSWRL.Variable y = getVariable(m, "y");
        OntSWRL.Variable z = getVariable(m, "z");

        // modify:
        imp.getBodyList().addFirst(m.createDifferentIndividualsSWRLAtom(x, y));
        m.createSWRLImp(Collections.emptyList(),
                Collections.singletonList(m.createDataRangeSWRLAtom(XSD.xdouble.inModel(m).as(OntDataRange.Named.class), z)));
        ReadWriteUtils.print(m);

        Assert.assertEquals(2, m.ontObjects(OntSWRL.Imp.class).count());
        Assert.assertEquals(3, m.ontObjects(OntSWRL.Atom.WithObjectProperty.class).count());
        Assert.assertEquals(1, m.ontObjects(OntSWRL.Atom.WithDifferentIndividuals.class).count());
        Assert.assertEquals(1, m.ontObjects(OntSWRL.Atom.WithDataRange.class).count());
        Assert.assertEquals(5, m.ontObjects(OntSWRL.Atom.class).count());
        Assert.assertEquals(0, m.ontObjects(OntSWRL.Atom.WithBuiltin.class).count());
        Assert.assertEquals(1, m.ontObjects(OntSWRL.Atom.Unary.class).count());
        Assert.assertEquals(4, m.ontObjects(OntSWRL.Atom.Binary.class).count());
        Assert.assertEquals(3, m.ontObjects(OntSWRL.Variable.class).count());
        Assert.assertEquals(3, m.ontObjects(OntSWRL.IArg.class).count());
        Assert.assertEquals(3, m.ontObjects(OntSWRL.DArg.class).count());
        Assert.assertEquals(3, m.ontObjects(OntSWRL.Arg.class).count());
        Assert.assertEquals(0, m.ontObjects(OntSWRL.Builtin.class).count());
        Assert.assertEquals(10, m.ontObjects(OntSWRL.class).count());
    }

    @Test
    public void testCoreSWRLBuiltins() {
        String uri = "http://test.com/swrl-2";
        String ns = uri + "#";

        OntModel m = OntModelFactory.createModel()
                .setID(uri).getModel()
                .setNsPrefix("test", ns)
                .setNsPrefix("swrl", SWRL.NS)
                .setNsPrefix("swrlb", SWRLB.NS)
                .setNsPrefixes(OntModelFactory.STANDARD);

        OntSWRL.Variable var1 = m.createSWRLVariable("v1");
        OntSWRL.Variable var2 = m.createSWRLVariable("v2");
        OntSWRL.Atom<?> a = m.createBuiltInSWRLAtom(SWRLB.equal,
                Arrays.asList(m.createTypedLiteral(1d).as(OntSWRL.DArg.class), var1));
        OntSWRL.Atom<?> b = m.createBuiltInSWRLAtom(SWRLB.add,
                Arrays.asList(var1, m.createTypedLiteral(2d).as(OntSWRL.DArg.class), var2));
        OntSWRL.Atom<?> c = m.createBuiltInSWRLAtom(m.getResource(ns + "del"),
                Arrays.asList(var2, m.createTypedLiteral(2d).as(OntSWRL.DArg.class)));

        ReadWriteUtils.print(m);

        Assert.assertEquals(3, m.ontObjects(OntSWRL.Atom.WithBuiltin.class).count());
        Assert.assertEquals(1, m.ontObjects(OntSWRL.Builtin.class).count());

        Assert.assertEquals(7, a.spec().peek(x -> LOGGER.debug("1)Spec: {}", x)).count());
        Assert.assertEquals(9, b.spec().peek(x -> LOGGER.debug("2)Spec: {}", x)).count());
        Assert.assertEquals(8, c.spec().peek(x -> LOGGER.debug("3)Spec: {}", x)).count());

        Assert.assertEquals(0, a.getPredicate().spec().count());
        Assert.assertEquals(0, b.getPredicate().spec().count());
        Assert.assertEquals(1, c.getPredicate().spec().count());
    }

    @Test
    public void testAssembleSWRLAtomsWithAnonymousIndividuals() {
        OntModel m = OntModelFactory.createModel()
                .setNsPrefix("swrl", SWRL.NS)
                .setNsPrefixes(OntModelFactory.STANDARD);

        OntClass.Named c = m.createOntClass("C");
        OntObjectProperty.Named p1 = m.createObjectProperty("P1");
        OntDataProperty p2 = m.createDataProperty("P2");
        Resource i1 = m.createResource();
        Resource i2 = m.createResource();
        Resource i3 = m.createResource();
        Literal v = m.createLiteral("v");

        OntSWRL.Atom.WithClass a1 = m.createResource(SWRL.ClassAtom)
                .addProperty(SWRL.argument1, i1)
                .addProperty(SWRL.classPredicate, c)
                .as(OntSWRL.Atom.WithClass.class);

        Assert.assertEquals(c, a1.getPredicate());
        Assert.assertEquals(i1, a1.getArg());

        OntSWRL.Atom.WithSameIndividuals a2 = m.createResource(SWRL.SameIndividualAtom)
                .addProperty(SWRL.argument1, i1)
                .addProperty(SWRL.argument2, i2)
                .as(OntSWRL.Atom.WithSameIndividuals.class);
        Assert.assertEquals(i1, a2.getFirstArg());
        Assert.assertEquals(i2, a2.getSecondArg());
        Assert.assertEquals(OWL.sameAs, a2.getPredicate());

        OntSWRL.Atom.WithObjectProperty a3 = m.createResource(SWRL.IndividualPropertyAtom)
                .addProperty(SWRL.argument1, i1)
                .addProperty(SWRL.argument2, i3)
                .addProperty(SWRL.propertyPredicate, p1)
                .as(OntSWRL.Atom.WithObjectProperty.class);
        Assert.assertEquals(i1, a3.getFirstArg());
        Assert.assertEquals(i3, a3.getSecondArg());
        Assert.assertEquals(p1, a3.getPredicate());

        OntSWRL.Atom.WithDataProperty a4 = m.createResource(SWRL.DatavaluedPropertyAtom)
                .addProperty(SWRL.argument1, i1)
                .addProperty(SWRL.argument2, v)
                .addProperty(SWRL.propertyPredicate, p2)
                .as(OntSWRL.Atom.WithDataProperty.class);
        Assert.assertEquals(i1, a4.getFirstArg());
        //noinspection AssertEqualsBetweenInconvertibleTypes
        Assert.assertEquals(v, a4.getSecondArg());
        Assert.assertEquals(p2, a4.getPredicate());

        ReadWriteUtils.print(m);
        Assert.assertEquals(3, m.ontObjects(OntSWRL.IArg.class).count());
        Assert.assertEquals(1, m.ontObjects(OntSWRL.DArg.class).count());
        Assert.assertEquals(4, m.ontObjects(OntSWRL.Atom.class).count());
    }
}
