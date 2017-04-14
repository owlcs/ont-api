package org.semanticweb.owlapi.api.test;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.semanticweb.owlapi.api.test.baseclasses.TestBase;
import org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat;
import org.semanticweb.owlapi.formats.OWLXMLDocumentFormat;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.model.*;


@ru.avicomp.ontapi.utils.ModifiedForONTApi
@SuppressWarnings("javadoc")
@RunWith(Parameterized.class)
public class PunnedDeclarationsNotAddedTestCase extends TestBase {

    @Parameters(name = "{0}")
    public static Collection<OWLDocumentFormat> data() {
        return Arrays.asList(new FunctionalSyntaxDocumentFormat(), new OWLXMLDocumentFormat(), new RDFXMLDocumentFormat(), new TurtleDocumentFormat());
    }

    private final OWLDocumentFormat format;

    public PunnedDeclarationsNotAddedTestCase(OWLDocumentFormat format) {
        this.format = format;
    }

    private OWLOntology getOntologyWithPunnedInvalidDeclarations() {
        OWLOntology o = getOWLOntology();
        OWLObjectProperty op = df.getOWLObjectProperty(iri("testProperty"));
        OWLAnnotationProperty ap = df.getOWLAnnotationProperty(iri("testProperty"));
        o.add(df.getOWLDeclarationAxiom(op));
        o.add(df.getOWLTransitiveObjectPropertyAxiom(op));
        OWLAnnotationAssertionAxiom assertion = df.getOWLAnnotationAssertionAxiom(iri("test"), df.getOWLAnnotation(ap, iri("otherTest")));
        o.add(assertion);
        return o;
    }

    private OWLOntology getOntologyWithMissingDeclarations() {
        OWLOntology o = getOWLOntology();
        OWLObjectProperty op = df.getOWLObjectProperty(iri("testObjectProperty"));
        OWLAnnotationProperty ap = df.getOWLAnnotationProperty(iri("testAnnotationProperty"));
        o.add(df.getOWLTransitiveObjectPropertyAxiom(op));
        OWLAnnotationAssertionAxiom assertion = df.getOWLAnnotationAssertionAxiom(iri("test"), df.getOWLAnnotation(ap, iri("otherTest")));
        o.add(assertion);
        return o;
    }

    @Test
    public void shouldDeclareMissingEntities() throws OWLOntologyCreationException, OWLOntologyStorageException {
        OWLOntology o = getOntologyWithMissingDeclarations();
        OWLOntology reloaded = roundTrip(o, format);
        OWLObjectProperty op = df.getOWLObjectProperty(iri("testObjectProperty"));
        OWLAnnotationProperty ap = df.getOWLAnnotationProperty(iri("testAnnotationProperty"));
        Assert.assertTrue(reloaded.containsAxiom(df.getOWLDeclarationAxiom(ap)));
        Assert.assertTrue(reloaded.containsAxiom(df.getOWLDeclarationAxiom(op)));
    }

    @Test
    public void shouldNotAddDeclarationsForIllegalPunnings() throws Exception {
        if (DEBUG_USE_OWL) {
            testOWLAPI();
        } else {
            testONTAPI();
        }
    }

    private void testOWLAPI() throws Exception {
        OWLOntology o = getOntologyWithPunnedInvalidDeclarations();
        OWLOntology reloaded = roundTrip(o, format);
        OWLAnnotationProperty ap = df.getOWLAnnotationProperty(iri("testProperty"));
        OWLDeclarationAxiom ax = df.getOWLDeclarationAxiom(ap);
        Assert.assertFalse("ap testProperty should not have been declared", reloaded.containsAxiom(ax));
    }

    private void testONTAPI() throws Exception {
        OWLOntologyManager m = setupManager();
        OWLOntologyLoaderConfiguration conf = ((ru.avicomp.ontapi.config.OntLoaderConfiguration) m
                .getOntologyLoaderConfiguration())
                .setAllowReadDeclarations(false)
                .setPersonality(ru.avicomp.ontapi.jena.impl.configuration.OntModelConfig.ONT_PERSONALITY_STRICT);
        m.setOntologyLoaderConfiguration(conf);

        OWLOntology o = m.createOntology(IRI.getNextDocumentIRI(uriBase));

        OWLObjectProperty op = df.getOWLObjectProperty(iri("testProperty"));
        OWLAnnotationProperty ap = df.getOWLAnnotationProperty(iri("testProperty"));
        OWLAnnotationAssertionAxiom assertion = df.getOWLAnnotationAssertionAxiom(iri("test"), df.getOWLAnnotation(ap, iri("otherTest")));

        o.add(df.getOWLDeclarationAxiom(op));
        o.add(df.getOWLTransitiveObjectPropertyAxiom(op));

        // ONT-API HACK: not even able to add illegal axioms:
        try {
            o.add(assertion);
            Assert.fail("The assetrtion succesfully added: " + assertion);
        } catch (ru.avicomp.ontapi.OntApiException e) {
            LOGGER.info("Exception: {}", e);
            Throwable cause = e.getCause();
            if (cause instanceof ru.avicomp.ontapi.jena.OntJenaException) {
                LOGGER.info("Cause: {}", cause);
                return;
            }
            throw e;
        }
    }

}
