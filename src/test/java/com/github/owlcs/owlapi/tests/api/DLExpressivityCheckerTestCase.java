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

import com.github.owlcs.owlapi.OWLManager;
import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.util.DLExpressivityChecker;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DLExpressivityCheckerTestCase extends TestBase {
    public static List<Data> getData() {
        DataBuilder b = new DataBuilder();
        return Arrays.asList(
                new Data("UNIVRESTR", b.assAll())
                , new Data("", b.dDef())
                , new Data("", b.decC())
                , new Data("", b.decOp())
                , new Data("", b.decDp())
                , new Data("", b.decDt())
                , new Data("", b.decAp())
                , new Data("", b.decI())
                , new Data("", b.ec())
                , new Data("", b.nop())
                , new Data("", b.opa())
                , new Data("", b.subAnn())
                , new Data("", b.subClass())
                , new Data("", b.rule())
                , new Data("", b.hasKey())
                , new Data("", b.bigRule())
                , new Data("", b.ann())
                , new Data("", b.annDom())
                , new Data("", b.annRange())
                , new Data("", b.ass())
                , new Data("CUO", b.assDi())
                , new Data("C", b.dc())
                , new Data("C", b.assNot())
                , new Data("C", b.assNotAnon())
                , new Data("R", b.dOp())
                , new Data("R", b.irr())
                , new Data("R", b.asymm())
                , new Data("R", b.assHasSelf())
                , new Data("RRESTR(D)", b.dRange())
                , new Data("RRESTR(D)", b.dRangeAnd())
                , new Data("RRESTR(D)", b.dRangeOr())
                , new Data("RRESTR(D)", b.dOneOf())
                , new Data("RRESTR(D)", b.dNot())
                , new Data("RRESTR(D)", b.dRangeRestrict())
                , new Data("RRESTR(D)", b.dDom())
                , new Data("CU", b.du())
                , new Data("H(D)", b.eDp())
                , new Data("H(D)", b.subData())
                , new Data("H", b.eOp())
                , new Data("H", b.subObject())
                , new Data("F(D)", b.fdp())
                , new Data("F", b.fop())
                , new Data("IF", b.ifp())
                , new Data("I", b.iop())
                , new Data("I", b.opaInv())
                , new Data("I", b.opaInvj())
                , new Data("I", b.symm())
                , new Data("(D)", b.dDp())
                , new Data("(D)", b.ndp())
                , new Data("(D)", b.assDAll())
                , new Data("(D)", b.assDHas())
                , new Data("(D)", b.assD())
                , new Data("(D)", b.assDPlain())
                , new Data("O", b.same())
                , new Data("+", b.trans())
                , new Data("CINT", b.assAnd())
                , new Data("U", b.assOr())
                , new Data("RRESTR", b.oDom())
                , new Data("RRESTR", b.oRange())
                , new Data("E", b.assSome())
                , new Data("EO", b.assHas())
                , new Data("Q", b.assMin())
                , new Data("Q", b.assMax())
                , new Data("Q", b.assEq())
                , new Data("UO", b.assOneOf())
                , new Data("E(D)", b.assDSome())
                , new Data("Q(D)", b.assDMin())
                , new Data("Q(D)", b.assDMax())
                , new Data("Q(D)", b.assDEq())
                , new Data("Rr", b.chain())
                , new Data("Rr", b.ref())
                , new Data("RIQ", b.ref(), b.trans(), b.symm(), b.subObject(), b.fop(), b.assMinTop(), b.assMin())
                , new Data("N", b.assMinTop())
        );
    }

    @ParameterizedTest
    @MethodSource("getData")
    public void testAssertion(Data data) throws Exception {
        OWLOntology o = OWLManager.createOWLOntologyManager().createOntology();
        data.axioms.forEach(o::add);

        String actual = new DLExpressivityChecker(Collections.singleton(o)).getDescriptionLogicName();
        Assertions.assertEquals(data.expected, actual);
    }

    public static class Data {
        private final String expected;
        private final List<OWLAxiom> axioms;

        public Data(String expected, OWLAxiom... axioms) {
            this(expected, Arrays.stream(axioms).collect(Collectors.toList()));
        }

        private Data(String expected, List<OWLAxiom> axioms) {
            this.expected = expected;
            this.axioms = axioms;
        }

        @Override
        public String toString() {
            return String.valueOf(axioms);
        }
    }
}
