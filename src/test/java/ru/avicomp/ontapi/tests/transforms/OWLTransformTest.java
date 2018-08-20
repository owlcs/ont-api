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

package ru.avicomp.ontapi.tests.transforms;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.io.OWLOntologyLoaderMetaData;
import org.semanticweb.owlapi.io.RDFOntologyHeaderStatus;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avicomp.ontapi.OntFormat;
import ru.avicomp.ontapi.OntManagers;
import ru.avicomp.ontapi.OntologyManager;
import ru.avicomp.ontapi.OntologyModel;
import ru.avicomp.ontapi.jena.OntModelFactory;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.utils.ReadWriteUtils;
import ru.avicomp.ontapi.utils.StringInputStreamDocumentSource;

import java.nio.file.Paths;

/**
 * Created by @szuev on 01.04.2018.
 */
public class OWLTransformTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(OWLTransformTest.class);

    @Test
    public void testCardinalityRestrictions() throws Exception {
        IRI iri = IRI.create(Paths.get(OWLTransformTest.class.getResource("/owlapi/owl11/family/family.owl").toURI()).toRealPath().toUri());
        LOGGER.debug("IRI {}", iri);
        OntologyManager m = OntManagers.createONT();
        OntologyModel o = m.loadOntology(iri);
        ReadWriteUtils.print(o.asGraphModel());
        o.axioms().forEach(x -> LOGGER.debug("{}", x));
        Assert.assertEquals(136, o.getAxiomCount());
    }

    @Test
    public void testParseZeroHeader() throws OWLOntologyCreationException {
        Model m = ModelFactory.createDefaultModel().setNsPrefixes(OntModelFactory.STANDARD);
        m.createResource("http://class").addProperty(RDF.type, OWL.Class);
        String txt = ReadWriteUtils.toString(m, OntFormat.TURTLE);

        OntologyManager manager = OntManagers.createONT();
        OntologyModel o = manager.loadOntologyFromOntologyDocument(new StringInputStreamDocumentSource(txt, OntFormat.TURTLE));
        OWLOntologyLoaderMetaData meta = manager.getNonnullOntologyFormat(o)
                .getOntologyLoaderMetaData().orElseThrow(AssertionError::new);
        print(meta);
        Assert.assertEquals(RDFOntologyHeaderStatus.PARSED_ZERO_HEADERS, meta.getHeaderState());
        Assert.assertEquals(0, meta.getUnparsedTriples().count());
        Assert.assertEquals(1, meta.getGuessedDeclarations().size());
        Assert.assertEquals(2, meta.getTripleCount());
    }

    @Test
    public void testParseMultipleHeader() throws OWLOntologyCreationException {
        String ontIRI = "http://o";
        String verIRI = "http://v";
        Model m = ModelFactory.createDefaultModel().setNsPrefixes(OntModelFactory.STANDARD);
        m.createResource("http://class").addProperty(RDF.type, OWL.Class);
        m.createResource().addProperty(RDF.type, OWL.Ontology);
        m.createResource("http://ont1").addProperty(RDF.type, OWL.Ontology);
        m.createResource(ontIRI).addProperty(RDF.type, OWL.Ontology)
                .addProperty(OWL.versionIRI, m.createResource(verIRI));
        String txt = ReadWriteUtils.toString(m, OntFormat.TURTLE);

        LOGGER.debug("Ontology:\n{}", txt);

        OntologyManager manager = OntManagers.createONT();
        OntologyModel o = manager.loadOntologyFromOntologyDocument(new StringInputStreamDocumentSource(txt, OntFormat.TURTLE));
        ReadWriteUtils.print(o);
        Assert.assertEquals(ontIRI, o.getOntologyID().getOntologyIRI().map(IRI::getIRIString).orElseThrow(AssertionError::new));
        Assert.assertEquals(verIRI, o.getOntologyID().getVersionIRI().map(IRI::getIRIString).orElseThrow(AssertionError::new));

        OWLOntologyLoaderMetaData meta = manager.getNonnullOntologyFormat(o)
                .getOntologyLoaderMetaData().orElseThrow(AssertionError::new);
        print(meta);

        Assert.assertEquals(RDFOntologyHeaderStatus.PARSED_MULTIPLE_HEADERS, meta.getHeaderState());
        Assert.assertEquals(0, meta.getUnparsedTriples().count());
        Assert.assertEquals(1, meta.getGuessedDeclarations().size());
        Assert.assertEquals(o.asGraphModel().size(), meta.getTripleCount());
    }

    @Test
    public void testUnparsableTriples() throws OWLOntologyCreationException {
        Model m = ModelFactory.createDefaultModel().setNsPrefixes(OntModelFactory.STANDARD);
        m.createResource("http://ont").addProperty(RDF.type, OWL.Ontology);

        Resource clazz = m.createResource("http://class").addProperty(RDF.type, RDFS.Class);
        Resource prop = m.createResource("http://prop").addProperty(RDF.type, RDF.Property);
        // restriction, either data or object
        m.createResource()
                .addProperty(RDF.type, OWL.Restriction)
                .addProperty(OWL.onProperty, prop)
                .addProperty(OWL.allValuesFrom, clazz);
        String txt = ReadWriteUtils.toString(m, OntFormat.TURTLE);

        LOGGER.debug("Ontology:\n{}", txt);

        OntologyManager manager = OntManagers.createONT();
        OntologyModel o = manager.loadOntologyFromOntologyDocument(new StringInputStreamDocumentSource(txt, OntFormat.TURTLE));
        ReadWriteUtils.print(o);

        OWLOntologyLoaderMetaData meta = manager.getNonnullOntologyFormat(o)
                .getOntologyLoaderMetaData().orElseThrow(AssertionError::new);
        print(meta);

        Assert.assertEquals(RDFOntologyHeaderStatus.PARSED_ONE_HEADER, meta.getHeaderState());
        Assert.assertEquals(1, meta.getUnparsedTriples().count());
        Assert.assertEquals(0, meta.getGuessedDeclarations().size());
        Assert.assertEquals(m.size(), meta.getTripleCount());

    }

    private static void print(OWLOntologyLoaderMetaData meta) {
        meta.getGuessedDeclarations().asMap().forEach((x, y) -> LOGGER.debug("Guessed: {} => {}", x, y));
        meta.getUnparsedTriples().forEach(t -> LOGGER.debug("Unparsed: {}", t));
    }
}
