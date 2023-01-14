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

package com.github.owlcs.ontapi.tests.internal;

import com.github.owlcs.ontapi.DataFactory;
import com.github.owlcs.ontapi.Ontology;
import com.github.owlcs.ontapi.OntologyManager;
import com.github.owlcs.ontapi.TestDataCollection;
import com.github.owlcs.ontapi.TestDataCollection.Data;
import com.github.owlcs.ontapi.TestManagers;
import com.github.owlcs.ontapi.internal.ONTObject;
import com.github.owlcs.ontapi.testutils.OWLIOUtils;
import org.junit.jupiter.api.Assertions;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.SWRLAtom;
import org.semanticweb.owlapi.model.SWRLRule;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by @ssz on 24.08.2019.
 */
public class SWRLAtomTest extends ContentTestBase {

    public static List<Data> getData() {
        return TestDataCollection.getObjects().stream().filter(Data::isSWRLAtom).collect(Collectors.toList());
    }

    @Override
    OWLObject fromModel(Data data) {
        OntologyManager m = TestManagers.createONTManager();
        DataFactory df = m.getOWLDataFactory();

        SWRLAtom ont = (SWRLAtom) data.create(df);

        Ontology o = m.createOntology();
        o.add(df.getSWRLRule(Collections.singletonList(ont), Collections.emptyList()));
        o.clearCache();
        OWLIOUtils.print(o);
        OWLObject res = o.axioms(AxiomType.SWRL_RULE)
                .flatMap(SWRLRule::body)
                .findFirst()
                .orElseThrow(AssertionError::new);
        Assertions.assertTrue(res instanceof ONTObject);
        return res;
    }


}


