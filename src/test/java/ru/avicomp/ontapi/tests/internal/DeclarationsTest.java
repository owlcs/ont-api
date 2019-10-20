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

package ru.avicomp.ontapi.tests.internal;

import org.junit.runners.Parameterized;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLObject;
import ru.avicomp.ontapi.DataFactory;
import ru.avicomp.ontapi.OntManagers;
import ru.avicomp.ontapi.OntologyManager;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by @ssz on 03.10.2019.
 */
public class DeclarationsTest extends CommonAxiomsTest {
    public DeclarationsTest(Data data) {
        super(data);
    }

    @Parameterized.Parameters(name = "{0}")
    public static List<Data> getData() {
        return getObjects().stream().filter(Data::isAxiom)
                .map(x -> (AxiomData) x)
                .filter(x -> AxiomType.DECLARATION.equals(x.getType()))
                .collect(Collectors.toList());
    }

    @Override
    OWLObject fromModel() {
        OntologyManager m = OntManagers.createONT();
        // to allow annotations for declaration:
        m.getOntologyConfigurator().setLoadAnnotationAxioms(false);
        DataFactory df = m.getOWLDataFactory();
        OWLAxiom ont = (OWLAxiom) data.create(df);

        return createONTObject(m, ont);
    }
}
