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
package com.github.owlcs.owlapi.tests.api;

import org.junit.Assert;
import org.junit.Test;

public class OWLObjectTypeIndexProviderTestCase {

    private final DataBuilder b = new DataBuilder();

    @Test
    public void testAssertions() {
        Assert.assertEquals(b.ann().typeIndex(), 2034);
        Assert.assertEquals(b.asymm().typeIndex(), 2018);
        Assert.assertEquals(b.annDom().typeIndex(), 2037);
        Assert.assertEquals(b.annRange().typeIndex(), 2036);
        Assert.assertEquals(b.ass().typeIndex(), 2005);
        Assert.assertEquals(b.assAnd().typeIndex(), 2005);
        Assert.assertEquals(b.assOr().typeIndex(), 2005);
        Assert.assertEquals(b.dRangeAnd().typeIndex(), 2030);
        Assert.assertEquals(b.dRangeOr().typeIndex(), 2030);
        Assert.assertEquals(b.assNot().typeIndex(), 2005);
        Assert.assertEquals(b.assNotAnon().typeIndex(), 2005);
        Assert.assertEquals(b.assSome().typeIndex(), 2005);
        Assert.assertEquals(b.assAll().typeIndex(), 2005);
        Assert.assertEquals(b.assHas().typeIndex(), 2005);
        Assert.assertEquals(b.assMin().typeIndex(), 2005);
        Assert.assertEquals(b.assMax().typeIndex(), 2005);
        Assert.assertEquals(b.assEq().typeIndex(), 2005);
        Assert.assertEquals(b.assHasSelf().typeIndex(), 2005);
        Assert.assertEquals(b.assOneOf().typeIndex(), 2005);
        Assert.assertEquals(b.assDSome().typeIndex(), 2005);
        Assert.assertEquals(b.assDAll().typeIndex(), 2005);
        Assert.assertEquals(b.assDHas().typeIndex(), 2005);
        Assert.assertEquals(b.assDMin().typeIndex(), 2005);
        Assert.assertEquals(b.assDMax().typeIndex(), 2005);
        Assert.assertEquals(b.assDEq().typeIndex(), 2005);
        Assert.assertEquals(b.dOneOf().typeIndex(), 2030);
        Assert.assertEquals(b.dNot().typeIndex(), 2030);
        Assert.assertEquals(b.dRangeRestrict().typeIndex(), 2030);
        Assert.assertEquals(b.assD().typeIndex(), 2010);
        Assert.assertEquals(b.assDPlain().typeIndex(), 2010);
        Assert.assertEquals(b.dDom().typeIndex(), 2029);
        Assert.assertEquals(b.dRange().typeIndex(), 2030);
        Assert.assertEquals(b.dDef().typeIndex(), 2038);
        Assert.assertEquals(b.decC().typeIndex(), 2000);
        Assert.assertEquals(b.decOp().typeIndex(), 2000);
        Assert.assertEquals(b.decDp().typeIndex(), 2000);
        Assert.assertEquals(b.decDt().typeIndex(), 2000);
        Assert.assertEquals(b.decAp().typeIndex(), 2000);
        Assert.assertEquals(b.decI().typeIndex(), 2000);
        Assert.assertEquals(b.assDi().typeIndex(), 2007);
        Assert.assertEquals(b.dc().typeIndex(), 2003);
        Assert.assertEquals(b.dDp().typeIndex(), 2031);
        Assert.assertEquals(b.dOp().typeIndex(), 2024);
        Assert.assertEquals(b.du().typeIndex(), 2004);
        Assert.assertEquals(b.ec().typeIndex(), 2001);
        Assert.assertEquals(b.eDp().typeIndex(), 2026);
        Assert.assertEquals(b.eOp().typeIndex(), 2012);
        Assert.assertEquals(b.fdp().typeIndex(), 2028);
        Assert.assertEquals(b.fop().typeIndex(), 2015);
        Assert.assertEquals(b.ifp().typeIndex(), 2016);
        Assert.assertEquals(b.iop().typeIndex(), 2014);
        Assert.assertEquals(b.irr().typeIndex(), 2021);
        Assert.assertEquals(b.ndp().typeIndex(), 2011);
        Assert.assertEquals(b.nop().typeIndex(), 2009);
        Assert.assertEquals(b.opa().typeIndex(), 2008);
        Assert.assertEquals(b.opaInv().typeIndex(), 2008);
        Assert.assertEquals(b.opaInvj().typeIndex(), 2008);
        Assert.assertEquals(b.oDom().typeIndex(), 2022);
        Assert.assertEquals(b.oRange().typeIndex(), 2023);
        Assert.assertEquals(b.chain().typeIndex(), 2025);
        Assert.assertEquals(b.ref().typeIndex(), 2020);
        Assert.assertEquals(b.same().typeIndex(), 2006);
        Assert.assertEquals(b.subAnn().typeIndex(), 2035);
        Assert.assertEquals(b.subClass().typeIndex(), 2002);
        Assert.assertEquals(b.subData().typeIndex(), 2027);
        Assert.assertEquals(b.subObject().typeIndex(), 2013);
        Assert.assertEquals(b.rule().typeIndex(), 2033);
        Assert.assertEquals(b.symm().typeIndex(), 2017);
        Assert.assertEquals(b.trans().typeIndex(), 2019);
        Assert.assertEquals(b.hasKey().typeIndex(), 2032);
        Assert.assertEquals(b.bigRule().typeIndex(), 2033);
        Assert.assertEquals(b.onto().typeIndex(), 1);
    }
}
