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

package ru.avicomp.ontapi.tests.model;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import ru.avicomp.ontapi.OntFormat;
import ru.avicomp.ontapi.OntManagers;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

import java.util.stream.Stream;

/**
 * Created by @ssz on 01.08.2019.
 */
@RunWith(Parameterized.class)
public class SimpleListAxiomsTest {

    private static OWLOntology common;
    private static OWLOntology concurrent;
    private final AxiomsData data;

    public SimpleListAxiomsTest(AxiomsData data) {
        this.data = data;
    }

    @Parameterized.Parameters(name = "{0}")
    public static AxiomsData[] data() {
        return AxiomsData.values();
    }

    @BeforeClass
    public static void prepareModel() throws Exception {
        common = createTestOntology(OntManagers.createONT());
        concurrent = createTestOntology(OntManagers.createConcurrentONT());
    }

    private static OWLOntology createTestOntology(OWLOntologyManager m) throws Exception {
        OWLOntologyDocumentSource s1 = ReadWriteUtils.getFileDocumentSource("/ontapi/pizza.ttl", OntFormat.TURTLE);
        OWLOntologyDocumentSource s2 = ReadWriteUtils.getFileDocumentSource("/ontapi/family.ttl", OntFormat.TURTLE);
        OWLOntology pizza = m.loadOntologyFromOntologyDocument(s1);
        OWLOntology family = m.loadOntologyFromOntologyDocument(s2);
        OWLImportsDeclaration iri = m.getOWLDataFactory()
                .getOWLImportsDeclaration(family.getOntologyID().getOntologyIRI().orElseThrow(AssertionError::new));
        m.applyChange(new AddImport(pizza, iri));
        return pizza;
    }

    @Test
    public void testListAxiomsCommon() {
        testListAxioms(common);
    }

    @Test
    public void testListAxiomsConcurrent() {
        testListAxioms(concurrent);
    }

    private void testListAxioms(OWLOntology ont) {
        Assert.assertEquals(data.inBase, data.select(ont, false).count());
        Assert.assertEquals(data.withImports, data.select(ont, true).count());
    }

    enum AxiomsData {
        TBOX(885, 692) {
            @Override
            Stream<? extends OWLAxiom> select(OWLOntology ont, Imports imports) {
                return ont.tboxAxioms(imports);
            }
        },
        ABOX(1870, 11) {
            @Override
            Stream<? extends OWLAxiom> select(OWLOntology ont, Imports imports) {
                return ont.aboxAxioms(imports);
            }
        },
        RBOX(143, 9) {
            @Override
            Stream<? extends OWLAxiom> select(OWLOntology ont, Imports imports) {
                return ont.rboxAxioms(imports);
            }
        },
        ;
        final int inBase;
        final int withImports;

        AxiomsData(int withImports, int base) {
            this.inBase = base;
            this.withImports = withImports;
        }

        abstract Stream<? extends OWLAxiom> select(OWLOntology ont, Imports imports);

        Stream<? extends OWLAxiom> select(OWLOntology ont, boolean withImports) {
            return select(ont, Imports.fromBoolean(withImports));
        }
    }
}
