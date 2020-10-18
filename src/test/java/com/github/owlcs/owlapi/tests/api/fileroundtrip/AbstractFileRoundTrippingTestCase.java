/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2020, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.owlapi.tests.api.fileroundtrip;

import com.github.owlcs.owlapi.OWLManager;
import com.github.owlcs.owlapi.tests.api.baseclasses.ParametrizedRoundTrippingTestCase;
import org.junit.jupiter.api.Assertions;
import org.semanticweb.owlapi.io.StreamDocumentSource;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

/**
 * @author Matthew Horridge, The University Of Manchester, Information  Management Group
 * <p>
 * Created by @ssz on 11.10.2020.
 */
public abstract class AbstractFileRoundTrippingTestCase extends ParametrizedRoundTrippingTestCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFileRoundTrippingTestCase.class);

    protected static OWLOntology createOntology(String fileName) {
        return createOntology(fileName, OWLManager.createOWLOntologyManager(), new OWLOntologyLoaderConfiguration());
    }

    protected static OWLOntology createOntology(String fileName, OWLOntologyManager m, OWLOntologyLoaderConfiguration conf) {
        LOGGER.debug("Load ontology from file <{}>", fileName);
        OWLOntology o = ontologyFromClasspathFile(m, conf, fileName);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("ontology as parsed from input file:");
            o.axioms().forEach(ax -> LOGGER.debug(ax.toString()));
        }
        return o;
    }

    protected static OWLOntology ontologyFromClasspathFile(OWLOntologyManager m,
                                                           OWLOntologyLoaderConfiguration conf,
                                                           String fileName) {
        try {
            InputStream in = AbstractFileRoundTrippingTestCase.class.getResourceAsStream("/owlapi/" + fileName);
            return m.loadOntologyFromOntologyDocument(new StreamDocumentSource(in), conf);
        } catch (OWLOntologyCreationException e) {
            return Assertions.fail(e);
        }
    }
}
