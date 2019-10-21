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

package com.github.owlcs.ontapi.tests.internal;

import org.junit.Assert;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.semanticweb.owlapi.model.*;
import com.github.owlcs.ontapi.DataFactory;
import com.github.owlcs.ontapi.OntManagers;
import com.github.owlcs.ontapi.OntologyManager;
import com.github.owlcs.ontapi.OntologyModel;
import com.github.owlcs.ontapi.internal.ONTObject;
import com.github.owlcs.ontapi.utils.ReadWriteUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by @ssz on 21.08.2019.
 */
@RunWith(Parameterized.class)
public class SWRLArgTest extends ObjectFactoryTestBase {

    public SWRLArgTest(Data data) {
        super(data);
    }

    @Parameterized.Parameters(name = "{0}")
    public static List<Data> getData() {
        return getObjects().stream()
                .filter(x -> x.isSWRLVariable() || x.isSWRLIndividual() || x.isSWRLLiteral())
                .collect(Collectors.toList());
    }

    @Override
    OWLObject fromModel() {
        OntologyManager m = OntManagers.createONT();
        DataFactory df = m.getOWLDataFactory();

        SWRLArgument ont = (SWRLArgument) data.create(df);

        OntologyModel o = m.createOntology();
        SWRLVariable var = df.getSWRLVariable("X");
        SWRLIArgument arg1 = ont instanceof SWRLIArgument ? (SWRLIArgument) ont : var;
        SWRLDArgument arg2 = ont instanceof SWRLDArgument ? (SWRLDArgument) ont : var;
        SWRLAtom a = df.getSWRLDataPropertyAtom(df.getOWLTopDataProperty(), arg1, arg2);
        o.add(df.getSWRLRule(Collections.singletonList(a), Collections.emptyList()));
        o.clearCache();
        ReadWriteUtils.print(o);
        OWLObject res = o.axioms(AxiomType.SWRL_RULE)
                .flatMap(SWRLRule::body)
                .flatMap(SWRLAtom::allArguments)
                .filter(x -> !var.equals(x))
                .findFirst()
                .orElseThrow(AssertionError::new);
        Assert.assertTrue(res instanceof ONTObject);
        return res;
    }

}

