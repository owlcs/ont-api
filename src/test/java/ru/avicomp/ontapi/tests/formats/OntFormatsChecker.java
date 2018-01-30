package ru.avicomp.ontapi.tests.formats;

import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.IRIDocumentSource;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.io.UnparsableOntologyException;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avicomp.ontapi.OntFormat;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * As a test.
 * Created by @szuev on 10.01.2018.
 */
public class OntFormatsChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger(OntFormatsChecker.class);

    @Test
    public void test() {
        OWLOntology ontology;
        try {
            ontology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(ReadWriteUtils.getResourceFile("pizza.ttl"));
        } catch (OWLOntologyCreationException e) {
            throw new AssertionError(e);
        }
        LOGGER.info("{}", ontology);
        Set<OntFormat> writeNotSupported = new HashSet<>();
        Set<OntFormat> readNotSupported = new HashSet<>();
        for (OntFormat f : OntFormat.values()) {
            Path p = ReadWriteUtils.save(ontology, "pizza", f);
            LOGGER.debug("Format: {}, File: {}", f, p);
            if (p == null) {
                if (!f.isJenaOnly())
                    writeNotSupported.add(f);
                continue;
            }
            try {
                OWLOntologyDocumentSource source = new IRIDocumentSource(IRI.create(p.toUri()), f.createOwlFormat(), null);
                OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(source);
            } catch (UnparsableOntologyException e) {
                LOGGER.error("Can't read " + p, e);
                readNotSupported.add(f);
            } catch (OWLOntologyCreationException e) {
                throw new AssertionError(e);
            }
        }
        LOGGER.debug("Write not supported: {}", writeNotSupported);
        LOGGER.debug("Read not supported: {}", readNotSupported);
        writeNotSupported.forEach(f -> Assert.assertFalse(f.toString(), f.isWriteSupported()));
        readNotSupported.forEach(f -> Assert.assertFalse(f.toString(), f.isReadSupported()));
    }
}
