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
package com.github.owlcs.owlapi.tests.api.syntax;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.util.QNameShortFormProvider;

import java.util.HashMap;
import java.util.Map;

import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.Class;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.IRI;
import static org.junit.Assert.assertEquals;

public class ShortFormProviderTestCase {

    @Test
    public void shouldFindShortForm() {
        OWLClass c = Class(IRI("http://www.ebi.ac.uk/fgpt/ontologies/test/", "TEST_00001"));
        QNameShortFormProvider shortener = new QNameShortFormProvider();
        String shortform = shortener.getShortForm(c);
        assertEquals("test:TEST_00001", shortform);
    }

    @Test
    public void shouldFindShortFormForWoman() {
        OWLClass c = Class(IRI("http://www.example.org/#", "Woman"));
        QNameShortFormProvider shortener = new QNameShortFormProvider();
        String shortform = shortener.getShortForm(c);
        assertEquals("www:Woman", shortform);
    }

    @Test
    public void shouldFindShortFormForSetPRefix() {
        OWLClass c = Class(IRI("http://www.example.org/#", "Woman"));
        Map<String, String> prefixes = new HashMap<>();
        prefixes.put("test", "http://www.example.org/#");
        QNameShortFormProvider shortener = new QNameShortFormProvider(prefixes);
        String shortform = shortener.getShortForm(c);
        assertEquals("test:Woman", shortform);
    }
}
