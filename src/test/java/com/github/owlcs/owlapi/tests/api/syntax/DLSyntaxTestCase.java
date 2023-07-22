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

package com.github.owlcs.owlapi.tests.api.syntax;

import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.dlsyntax.renderer.DLSyntaxObjectRenderer;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;

public class DLSyntaxTestCase extends TestBase {

    @Test
    public void testCommasOnDisjointThree() {
        OWLClass a = df.getOWLClass("urn:test#", "A");
        OWLClass b = df.getOWLClass("urn:test#", "B");
        OWLClass c = df.getOWLClass("urn:test#", "C");
        OWLAxiom ax = df.getOWLDisjointClassesAxiom(a, b, c);
        DLSyntaxObjectRenderer visitor = new DLSyntaxObjectRenderer();
        String render = visitor.render(ax);
        Assertions.assertEquals("A ⊑ ¬ B, A ⊑ ¬ C, B ⊑ ¬ C", render);
    }

    @Test
    public void testCommasOnDisjointTwo() {
        OWLClass a = df.getOWLClass("urn:test#", "A");
        OWLClass b = df.getOWLClass("urn:test#", "B");
        OWLAxiom ax = df.getOWLDisjointClassesAxiom(a, b);
        DLSyntaxObjectRenderer visitor = new DLSyntaxObjectRenderer();
        String render = visitor.render(ax);
        Assertions.assertEquals("A ⊑ ¬ B", render);
    }

    @Test
    public void testCommasOnDisjointFour() {
        OWLClass a = df.getOWLClass("urn:test#", "A");
        OWLClass b = df.getOWLClass("urn:test#", "B");
        OWLClass c = df.getOWLClass("urn:test#", "C");
        OWLClass d = df.getOWLClass("urn:test#", "D");
        OWLAxiom ax = df.getOWLDisjointClassesAxiom(a, b, c, d);
        DLSyntaxObjectRenderer visitor = new DLSyntaxObjectRenderer();
        String render = visitor.render(ax);
        Assertions.assertEquals("A ⊑ ¬ B, A ⊑ ¬ C, A ⊑ ¬ D, B ⊑ ¬ C, B ⊑ ¬ D, C ⊑ ¬ D", render);
    }
}
