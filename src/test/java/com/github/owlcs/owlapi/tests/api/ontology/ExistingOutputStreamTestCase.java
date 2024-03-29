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
package com.github.owlcs.owlapi.tests.api.ontology;

import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat;
import org.semanticweb.owlapi.formats.ManchesterSyntaxDocumentFormat;
import org.semanticweb.owlapi.formats.OWLXMLDocumentFormat;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLOntology;

import javax.annotation.Nonnull;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;

/**
 * API writers/storers/renderers should not close streams if they didn't open
 * them.
 *
 * @author Matthew Horridge, The University of Manchester, Bio-Health Informatics Group
 */
public class ExistingOutputStreamTestCase extends TestBase {

    @Test
    public void testOutputStreamRemainsOpen() {
        OWLOntology ontology = getOWLOntology();
        saveOntology(ontology, new RDFXMLDocumentFormat());
        saveOntology(ontology, new OWLXMLDocumentFormat());
        saveOntology(ontology, new TurtleDocumentFormat());
        saveOntology(ontology, new FunctionalSyntaxDocumentFormat());
        saveOntology(ontology, new ManchesterSyntaxDocumentFormat());
    }

    // test that the stream is not closed by adding a comment at the end
    @Override
    protected StringDocumentTarget saveOntology(@Nonnull OWLOntology o, OWLDocumentFormat format) {
        try (BufferedOutputStream os = new BufferedOutputStream(new ByteArrayOutputStream());
             OutputStreamWriter w = new OutputStreamWriter(os)) {
            o.getOWLOntologyManager().saveOntology(o, format, os);
            os.flush();
            w.write("<!-- Comment -->");
        } catch (Exception e) {
            return Assertions.fail(e);
        }
        return new StringDocumentTarget();
    }
}
