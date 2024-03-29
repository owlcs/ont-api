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

package com.github.owlcs.owlapi.tests.api;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.functional.renderer.FunctionalSyntaxStorerFactory;
import org.semanticweb.owlapi.krss2.renderer.KRSS2OWLSyntaxStorerFactory;
import org.semanticweb.owlapi.latex.renderer.LatexStorerFactory;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterSyntaxStorerFactory;
import org.semanticweb.owlapi.model.OWLStorer;
import org.semanticweb.owlapi.model.PriorityCollectionSorting;
import org.semanticweb.owlapi.oboformat.OBOFormatStorerFactory;
import org.semanticweb.owlapi.owlxml.renderer.OWLXMLStorerFactory;
import org.semanticweb.owlapi.rdf.rdfxml.renderer.RDFXMLStorerFactory;
import org.semanticweb.owlapi.rdf.turtle.renderer.TurtleStorerFactory;
import org.semanticweb.owlapi.rio.RioBinaryRdfStorerFactory;
import org.semanticweb.owlapi.rio.RioJsonLDStorerFactory;
import org.semanticweb.owlapi.rio.RioJsonStorerFactory;
import org.semanticweb.owlapi.rio.RioN3StorerFactory;
import org.semanticweb.owlapi.rio.RioNQuadsStorerFactory;
import org.semanticweb.owlapi.rio.RioNTriplesStorerFactory;
import org.semanticweb.owlapi.rio.RioRDFXMLStorerFactory;
import org.semanticweb.owlapi.rio.RioTrigStorerFactory;
import org.semanticweb.owlapi.rio.RioTrixStorerFactory;
import org.semanticweb.owlapi.rio.RioTurtleStorerFactory;
import org.semanticweb.owlapi.util.PriorityCollection;

import java.util.Arrays;
import java.util.List;

public class PriorityCollectionTestCase {

    @Test
    public void testShouldStoreStorers() {
        List<OWLStorer> storers = Arrays.asList(new RioBinaryRdfStorerFactory().get(),
                new RioJsonLDStorerFactory().get(),
                new RioJsonStorerFactory().get(),
                new RioN3StorerFactory().get(),
                new RioNQuadsStorerFactory().get(),
                new RioNTriplesStorerFactory().get(),
                new RioRDFXMLStorerFactory().get(),
                new RioTrigStorerFactory().get(),
                new RioTrixStorerFactory().get(),
                new RioTurtleStorerFactory().get(),
                new OBOFormatStorerFactory().get(),
                new RDFXMLStorerFactory().get(),
                new OWLXMLStorerFactory().get(),
                new FunctionalSyntaxStorerFactory().get(),
                new ManchesterSyntaxStorerFactory().get(),
                new KRSS2OWLSyntaxStorerFactory().get(),
                new TurtleStorerFactory().get(),
                new LatexStorerFactory().get());
        PriorityCollection<OWLStorer> pc = new PriorityCollection<>(PriorityCollectionSorting.ON_SET_INJECTION_ONLY);
        pc.set(storers);
        Assertions.assertEquals(storers.size(), pc.size(), pc.toString());
    }
}
