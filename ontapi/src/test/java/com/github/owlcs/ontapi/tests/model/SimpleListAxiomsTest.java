/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2022, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.tests.model;

import com.github.owlcs.ontapi.OntFormat;
import com.github.owlcs.ontapi.OntManagers;
import com.github.owlcs.ontapi.testutils.OWLIOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.parameters.Imports;

import java.util.stream.Stream;

/**
 * Created by @ssz on 01.08.2019.
 */
public class SimpleListAxiomsTest {

    private static OWLOntology common;
    private static OWLOntology concurrent;

    @BeforeAll
    public static void prepareModel() throws Exception {
        common = createTestOntology(OntManagers.createManager());
        concurrent = createTestOntology(OntManagers.createConcurrentManager());
    }

    private static OWLOntology createTestOntology(OWLOntologyManager m) throws Exception {
        OWLOntologyDocumentSource s1 = OWLIOUtils.getFileDocumentSource("/ontapi/pizza.ttl", OntFormat.TURTLE);
        OWLOntologyDocumentSource s2 = OWLIOUtils.getFileDocumentSource("/ontapi/family.ttl", OntFormat.TURTLE);
        OWLOntology pizza = m.loadOntologyFromOntologyDocument(s1);
        OWLOntology family = m.loadOntologyFromOntologyDocument(s2);
        OWLImportsDeclaration iri = m.getOWLDataFactory()
                .getOWLImportsDeclaration(family.getOntologyID().getOntologyIRI().orElseThrow(AssertionError::new));
        m.applyChange(new AddImport(pizza, iri));
        return pizza;
    }

    @ParameterizedTest
    @EnumSource(value = AxiomsData.class)
    public void testListAxiomsCommon(AxiomsData data) {
        testListAxioms(data, common);
    }

    @ParameterizedTest
    @EnumSource(value = AxiomsData.class)
    public void testListAxiomsConcurrent(AxiomsData data) {
        testListAxioms(data, concurrent);
    }

    private void testListAxioms(AxiomsData data, OWLOntology ont) {
        Assertions.assertEquals(data.inBase, data.select(ont, false).count());
        Assertions.assertEquals(data.withImports, data.select(ont, true).count());
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
