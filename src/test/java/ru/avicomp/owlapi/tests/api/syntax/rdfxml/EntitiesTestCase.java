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

package ru.avicomp.owlapi.tests.api.syntax.rdfxml;

import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.rdf.rdfxml.renderer.RDFXMLStorerFactory;
import ru.avicomp.owlapi.tests.api.baseclasses.TestBase;


@SuppressWarnings("javadoc")
public class EntitiesTestCase extends TestBase {

    /**
     * ONT-API comment:
     * The test was modified to make it passed in ONT-API also.
     * The config option 'useNamespaceEntities' is only for RDF/XML format.
     * As for me it is incorrect to store this option in the global settings since it takes affect on a single format only, but whatever.
     * In the ONT-API there is a substitution of formats and instead OWL-Storer there is Jena mechanism,
     * so this option won't take affect and turns out to be unnecessary.
     * But we always can use direct way with pure OWL format...
     *
     * @throws Exception
     */
    @Test
    public void shouldRoundtripEntities() throws Exception {
        String input = "<?xml version=\"1.0\"?>\n"
                + "<!DOCTYPE rdf:RDF [<!ENTITY vin  \"http://www.w3.org/TR/2004/REC-owl-guide-20040210/wine#\" > ]>\n"
                + "<rdf:RDF"
                + " xmlns:owl =\"http://www.w3.org/2002/07/owl#\""
                + " xmlns:rdf =\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\""
                + " xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\""
                + " xmlns:xsd =\"http://www.w3.org/2001/XMLSchema#\"> \n"
                + "<owl:Ontology rdf:about=\"\">"
                + "<owl:priorVersion rdf:resource=\"&vin;test\"/>"
                + "</owl:Ontology>"
                + "</rdf:RDF>";
        String base = "urn:test";
        IRI iri = IRI.create(base + "#", "test");

        OWLOntologyManager m = setupManager();

        StringDocumentSource source = new StringDocumentSource(input, iri, new RDFXMLDocumentFormat(), null);
        OWLOntology o = m.loadOntologyFromOntologyDocument(source);
        Assert.assertEquals("Wrong ontology IRI", base, o.getOntologyID().getOntologyIRI().map(IRI::getIRIString).orElse(null));
        OWLDocumentFormat format = o.getFormat();
        Assert.assertNotNull("No format", format);

        m.getOntologyConfigurator().withUseNamespaceEntities(true);
        Assert.assertTrue(m.getOntologyWriterConfiguration().isUseNamespaceEntities());
        //m.setOntologyWriterConfiguration(m.getOntologyWriterConfiguration().withUseNamespaceEntities(true));

        StringDocumentTarget target = new StringDocumentTarget();

        OWLStorerFactory store = new RDFXMLStorerFactory();
        store.createStorer().storeOntology(o, target, format);
        //o.getOWLOntologyManager().saveOntology(o, format, target);
        LOGGER.debug("As string:\n{}", target);
        Assert.assertTrue(target.toString().contains("<owl:priorVersion rdf:resource=\"&vin;test\"/>"));
    }
}
