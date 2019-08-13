/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2019, Avicomp Services, AO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package ru.avicomp.ontapi.tests.internal;

import org.junit.Assert;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.util.SimpleRenderer;
import ru.avicomp.ontapi.DataFactory;
import ru.avicomp.ontapi.OntManagers;
import ru.avicomp.ontapi.OntologyManager;
import ru.avicomp.ontapi.OntologyModel;
import ru.avicomp.ontapi.internal.ONTObject;
import ru.avicomp.ontapi.tests.DataFactoryTest;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by @ssz on 13.08.2019.
 */
@RunWith(Parameterized.class)
public class EntityTest extends ObjectFactoryTest {

    public EntityTest(DataFactoryTest.Data data) {
        super(data);
    }

    @Parameterized.Parameters(name = "{0}")
    public static List<DataFactoryTest.Data> getData() {
        return DataFactoryTest.getData().stream()
                .filter(DataFactoryTest.Data::isEntity)
                .collect(Collectors.toList());
    }

    @Override
    OWLObject fromModel() {
        OntologyManager m = OntManagers.createONT();
        DataFactory df = m.getOWLDataFactory();

        OWLEntity ont = (OWLEntity) data.create(df);

        OntologyModel o = m.createOntology();
        o.add(df.getOWLDeclarationAxiom(ont));
        o.clearCache();
        //ReadWriteUtils.print(o);
        OWLEntity res = o.signature().findFirst().orElseThrow(AssertionError::new);
        Assert.assertTrue(res instanceof ONTObject);
        return res;
    }

    @Override
    void compare(OWLObject expected, OWLObject actual) {
        data.assertCheckNotSame(expected, actual);
        data.assertCheckHashCode(expected, actual);
        data.assertCheckEquals(expected, actual);
        // todo: here is some minor bug or misunderstanding in OWL-API and ONT-API (as copy-paste):
        //  OWL2DatatypeImpl do not use renderer - just naked IRI is always returned
        String toString = new SimpleRenderer().render(actual);
        Assert.assertEquals(toString, actual.toString());
    }
}
