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

package com.github.owlcs.ontapi.tests.model;

import com.github.owlcs.ontapi.CommonOntologies;
import com.github.owlcs.ontapi.OntManagers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import java.util.Collections;

/**
 * Created by @ssz on 12.02.2021.
 */
public class ContainsAxiomsIgnoreAnnotationsTest {

    protected OWLOntologyManager newManager() {
        return OntManagers.createManager();
    }

    @ParameterizedTest
    @EnumSource(value = CommonOntologies.class)
    public void testContainsAxioms(CommonOntologies data) {
        testContainsIgnoreAnnotations(data.fetch(newManager()));
    }

    private void testContainsIgnoreAnnotations(OWLOntology ont) {
        OWLDataFactory df = ont.getOWLOntologyManager().getOWLDataFactory();
        ont.axioms().forEach(a -> {
            Assertions.assertTrue(ont.containsAxiomIgnoreAnnotations(a));
            OWLAxiom b = a.isAnnotated() ? a.getAxiomWithoutAnnotations() :
                    a.getAnnotatedAxiom(Collections.singleton(df.getRDFSComment("X")));
            Assertions.assertTrue(ont.containsAxiomIgnoreAnnotations(b));
        });
    }
}
