/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2018, Avicomp Services, AO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.semanticweb.owlapi.rio;

import org.junit.Test;
import org.semanticweb.owlapi.api.baseclasses.TestBase;
import org.semanticweb.owlapi.functional.parser.OWLFunctionalSyntaxOWLParserFactory;
import org.semanticweb.owlapi.io.OWLParserFactory;
import org.semanticweb.owlapi.krss2.parser.KRSS2OWLParserFactory;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxOntologyParserFactory;
import org.semanticweb.owlapi.oboformat.OBOFormatOWLAPIParserFactory;
import org.semanticweb.owlapi.owlxml.parser.OWLXMLParserFactory;
import org.semanticweb.owlapi.rdf.rdfxml.parser.RDFXMLParserFactory;
import org.semanticweb.owlapi.rdf.turtle.parser.TurtleOntologyParserFactory;
import org.semanticweb.owlapi.util.PriorityCollection;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Ansell p_ansell@yahoo.com
 */
@SuppressWarnings("javadoc")
public class OWLParserFactoryRegistryTestCase {

    @Test
    public void setUp() {
        // this test used to count the parsers. However, the extra parser in the
        // compatibility package will show up here in Eclipse tests, creating
        // confusion
        // Switched to list the expected parsers anc checking they all appear.
        // Any extra ones are welcome.
        Set<Class<? extends OWLParserFactory>> factories = new HashSet<>();
        factories.add(RDFXMLParserFactory.class);
        factories.add(OWLXMLParserFactory.class);
        factories.add(OWLFunctionalSyntaxOWLParserFactory.class);
        factories.add(TurtleOntologyParserFactory.class);
        factories.add(ManchesterOWLSyntaxOntologyParserFactory.class);
        factories.add(OBOFormatOWLAPIParserFactory.class);
        factories.add(KRSS2OWLParserFactory.class);
        factories.add(RioTurtleParserFactory.class);
        factories.add(RioNQuadsParserFactory.class);
        factories.add(RioJsonParserFactory.class);
        factories.add(RioNTriplesParserFactory.class);
        factories.add(RioTrigParserFactory.class);
        factories.add(RioBinaryRdfParserFactory.class);
        factories.add(RioJsonLDParserFactory.class);
        factories.add(RioN3ParserFactory.class);
        factories.add(RioRDFXMLParserFactory.class);
        factories.add(RioTrixParserFactory.class);
        factories.add(RioRDFaParserFactory.class);
        PriorityCollection<OWLParserFactory> ontologyParsers = TestBase.createOWLManager().getOntologyParsers();
        Set<Class<? extends OWLParserFactory>> found = new HashSet<>();
        for (OWLParserFactory p : ontologyParsers) {
            found.add(p.getClass());
        }
        for (Class<? extends OWLParserFactory> p : factories) {
            assertTrue("Expected among parsers: " + p.getSimpleName(), found
                    .contains(p));
        }
    }
}
