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
package ru.avicomp.owlapi.tests.api;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.util.DLExpressivityChecker;
import ru.avicomp.ontapi.OntManagers;
import ru.avicomp.owlapi.OWLManager;
import ru.avicomp.owlapi.tests.api.baseclasses.TestBase;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * From now on, this test is no longer suitable for OWL-API comparison mode (see {@link OWLManager#DEBUG_USE_OWL})
 * due to changes in OWL-API-api:5.1.8.
 * And now it is just to test that the OWL-API and ONT-API behaviours with {@link DLExpressivityChecker} match.
 * TODO: move to ru.avicomp.ontapi.tests
 */
@SuppressWarnings({"javadoc"})
@RunWith(Parameterized.class)
public class DLExpressivityCheckerTestCase extends TestBase {
    private final OWLAxiom axiom;

    public DLExpressivityCheckerTestCase(OWLAxiom object) {
        this.axiom = object;
    }

    @Parameterized.Parameters
    public static List<OWLAxiom> getData() {
        Builder b = new Builder();
        return Arrays.asList(
                b.dRange()
                , b.dDef()
                , b.decC()
                , b.decOp()
                , b.decDp()
                , b.decDt()
                , b.decAp()
                , b.decI()
                , b.assDi()
                , b.dc()
                , b.dDp()
                , b.dOp()
                , b.du()
                , b.ec()
                , b.eDp()
                , b.eOp()
                , b.fdp()
                , b.fop()
                , b.ifp()
                , b.iop()
                , b.irr()
                , b.ndp()
                , b.nop()
                , b.opa()
                , b.opaInv()
                , b.opaInvj()
                , b.oDom()
                , b.oRange()
                , b.chain()
                , b.ref()
                , b.same()
                , b.subAnn()
                , b.subClass()
                , b.subData()
                , b.subObject()
                , b.rule()
                , b.symm()
                , b.trans()
                , b.hasKey()
                , b.bigRule()
                , b.ann()
                , b.asymm()
                , b.annDom()
                , b.annRange()
                , b.ass()
                , b.assAnd()
                , b.assOr()
                , b.dRangeAnd()
                , b.dRangeOr()
                , b.assNot()
                , b.assNotAnon()
                , b.assSome()
                , b.assAll()
                , b.assHas()
                , b.assMin()
                , b.assMax()
                , b.assEq()
                , b.assHasSelf()
                , b.assOneOf()
                , b.assDSome()
                , b.assDAll()
                , b.assDHas()
                , b.assDMin()
                , b.assDMax()
                , b.assDEq()
                , b.dOneOf()
                , b.dNot()
                , b.dRangeRestrict()
                , b.assD()
                , b.assDPlain()
                , b.dDom());
    }

    @BeforeClass
    public static void before() { // only for ONT-API
        Assume.assumeTrue(!OWLManager.DEBUG_USE_OWL);
    }

    @Test
    public void testCompare() throws Exception {
        OWLOntology ont = OntManagers.createONT().createOntology();
        OWLOntology owl = OntManagers.createOWL().createOntology();
        owl.add(axiom);
        ont.add(axiom);
        String expected = new DLExpressivityChecker(Collections.singleton(owl)).getDescriptionLogicName();
        String actual = new DLExpressivityChecker(Collections.singleton(ont)).getDescriptionLogicName();
        Assert.assertEquals(expected, actual);
    }
}
