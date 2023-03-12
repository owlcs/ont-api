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
package com.github.owlcs.owlapi.tests.api;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class OWLObjectTypeIndexProviderTestCase {

    private final DataBuilder b = new DataBuilder();

    @Test
    public void testAssertions() {
        Assertions.assertEquals(b.ann().typeIndex(), 2034);
        Assertions.assertEquals(b.asymm().typeIndex(), 2018);
        Assertions.assertEquals(b.annDom().typeIndex(), 2037);
        Assertions.assertEquals(b.annRange().typeIndex(), 2036);
        Assertions.assertEquals(b.ass().typeIndex(), 2005);
        Assertions.assertEquals(b.assAnd().typeIndex(), 2005);
        Assertions.assertEquals(b.assOr().typeIndex(), 2005);
        Assertions.assertEquals(b.dRangeAnd().typeIndex(), 2030);
        Assertions.assertEquals(b.dRangeOr().typeIndex(), 2030);
        Assertions.assertEquals(b.assNot().typeIndex(), 2005);
        Assertions.assertEquals(b.assNotAnon().typeIndex(), 2005);
        Assertions.assertEquals(b.assSome().typeIndex(), 2005);
        Assertions.assertEquals(b.assAll().typeIndex(), 2005);
        Assertions.assertEquals(b.assHas().typeIndex(), 2005);
        Assertions.assertEquals(b.assMin().typeIndex(), 2005);
        Assertions.assertEquals(b.assMax().typeIndex(), 2005);
        Assertions.assertEquals(b.assEq().typeIndex(), 2005);
        Assertions.assertEquals(b.assHasSelf().typeIndex(), 2005);
        Assertions.assertEquals(b.assOneOf().typeIndex(), 2005);
        Assertions.assertEquals(b.assDSome().typeIndex(), 2005);
        Assertions.assertEquals(b.assDAll().typeIndex(), 2005);
        Assertions.assertEquals(b.assDHas().typeIndex(), 2005);
        Assertions.assertEquals(b.assDMin().typeIndex(), 2005);
        Assertions.assertEquals(b.assDMax().typeIndex(), 2005);
        Assertions.assertEquals(b.assDEq().typeIndex(), 2005);
        Assertions.assertEquals(b.dOneOf().typeIndex(), 2030);
        Assertions.assertEquals(b.dNot().typeIndex(), 2030);
        Assertions.assertEquals(b.dRangeRestrict().typeIndex(), 2030);
        Assertions.assertEquals(b.assD().typeIndex(), 2010);
        Assertions.assertEquals(b.assDPlain().typeIndex(), 2010);
        Assertions.assertEquals(b.dDom().typeIndex(), 2029);
        Assertions.assertEquals(b.dRange().typeIndex(), 2030);
        Assertions.assertEquals(b.dDef().typeIndex(), 2038);
        Assertions.assertEquals(b.decC().typeIndex(), 2000);
        Assertions.assertEquals(b.decOp().typeIndex(), 2000);
        Assertions.assertEquals(b.decDp().typeIndex(), 2000);
        Assertions.assertEquals(b.decDt().typeIndex(), 2000);
        Assertions.assertEquals(b.decAp().typeIndex(), 2000);
        Assertions.assertEquals(b.decI().typeIndex(), 2000);
        Assertions.assertEquals(b.assDi().typeIndex(), 2007);
        Assertions.assertEquals(b.dc().typeIndex(), 2003);
        Assertions.assertEquals(b.dDp().typeIndex(), 2031);
        Assertions.assertEquals(b.dOp().typeIndex(), 2024);
        Assertions.assertEquals(b.du().typeIndex(), 2004);
        Assertions.assertEquals(b.ec().typeIndex(), 2001);
        Assertions.assertEquals(b.eDp().typeIndex(), 2026);
        Assertions.assertEquals(b.eOp().typeIndex(), 2012);
        Assertions.assertEquals(b.fdp().typeIndex(), 2028);
        Assertions.assertEquals(b.fop().typeIndex(), 2015);
        Assertions.assertEquals(b.ifp().typeIndex(), 2016);
        Assertions.assertEquals(b.iop().typeIndex(), 2014);
        Assertions.assertEquals(b.irr().typeIndex(), 2021);
        Assertions.assertEquals(b.ndp().typeIndex(), 2011);
        Assertions.assertEquals(b.nop().typeIndex(), 2009);
        Assertions.assertEquals(b.opa().typeIndex(), 2008);
        Assertions.assertEquals(b.opaInv().typeIndex(), 2008);
        Assertions.assertEquals(b.opaInvj().typeIndex(), 2008);
        Assertions.assertEquals(b.oDom().typeIndex(), 2022);
        Assertions.assertEquals(b.oRange().typeIndex(), 2023);
        Assertions.assertEquals(b.chain().typeIndex(), 2025);
        Assertions.assertEquals(b.ref().typeIndex(), 2020);
        Assertions.assertEquals(b.same().typeIndex(), 2006);
        Assertions.assertEquals(b.subAnn().typeIndex(), 2035);
        Assertions.assertEquals(b.subClass().typeIndex(), 2002);
        Assertions.assertEquals(b.subData().typeIndex(), 2027);
        Assertions.assertEquals(b.subObject().typeIndex(), 2013);
        Assertions.assertEquals(b.rule().typeIndex(), 2033);
        Assertions.assertEquals(b.symm().typeIndex(), 2017);
        Assertions.assertEquals(b.trans().typeIndex(), 2019);
        Assertions.assertEquals(b.hasKey().typeIndex(), 2032);
        Assertions.assertEquals(b.bigRule().typeIndex(), 2033);
        Assertions.assertEquals(b.onto().typeIndex(), 1);
    }
}
