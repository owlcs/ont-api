package org.semanticweb.owlapi.api.test.syntax;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.api.test.baseclasses.TestBase;
import org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory;
import org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.Namespaces;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

/**
 * Created by ses on 6/23/14.
 */
@ru.avicomp.ontapi.utils.ModifiedForONTApi
@SuppressWarnings("javadoc")
public class IRIShorteningTestCase extends TestBase {

    @Test
    public void testIriEqualToPrefixNotShortenedInFSS() throws Exception {
        OWLOntology o = createTestOntology();
        String output = saveOntology(o, new FunctionalSyntaxDocumentFormat()).toString();
        testMatchExact(output, "NamedIndividual(rdf:)", false);
        testMatchExact(output, "NamedIndividual(rdf:type)", true);
    }

    private void testMatchExact(String output, String text, boolean expected) {
        String message = "should " + (expected ? "" : "not ") + "contain" + text + " - " + output;
        Assert.assertTrue(message, expected == output.contains(text));
    }

    @Test
    public void testIriEqualToPrefixShortenedInTurtle() throws Exception {
        OWLOntology o = createTestOntology();
        String output = saveOntology(o, new TurtleDocumentFormat()).toString();
        LOGGER.debug(output);
        Set<String> patterns = Stream.of("%s\\s+rdf:type\\s+%s", "%s\\s+a\\s+%s").collect(Collectors.toSet());
        Stream.of("rdf:", "rdf:type").forEach(test ->
                Assert.assertTrue("Can't find <" + test + "> named individual",
                        patterns.stream().anyMatch(p -> matchRegex(output, String.format(p, test, "owl:NamedIndividual")))));
    }

    public boolean matchRegex(String output, String regex) {
        Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(output);
        return matcher.find();
    }

    private OWLOntology createTestOntology() {
        OWLOntology o = getOWLOntology();
        OWLNamedIndividual i = df.getOWLNamedIndividual(OWLFunctionalSyntaxFactory.IRI(Namespaces.RDF.getPrefixIRI(), ""));
        o.add(df.getOWLDeclarationAxiom(i));
        i = df.getOWLNamedIndividual(OWLRDFVocabulary.RDF_TYPE);
        o.add(df.getOWLDeclarationAxiom(i));
        return o;
    }

    @Test
    public void shouldOutputURNsCorrectly() throws OWLOntologyCreationException, OWLOntologyStorageException {
        OWLOntology o = m.createOntology(IRI.create("urn:ontology:", "test"));
        o.add(df.getOWLObjectPropertyAssertionAxiom(
                df.getOWLObjectProperty("urn:test#", "p"),
                df.getOWLNamedIndividual("urn:test#", "test"), df.getOWLNamedIndividual("urn:other:", "test")));
        equal(o, roundTrip(o));
    }
}
