package com.github.owlcs.ontapi.tests.transforms;

import com.github.owlcs.ontapi.OntFormat;
import com.github.owlcs.ontapi.OntManagers;
import com.github.owlcs.ontapi.testutils.OWLIOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.semanticweb.owlapi.model.AxiomType;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @see <a href="https://github.com/owlcs/ont-api/issues/60">issue #60</a>
 */
public class Issue60Test {
    private static final String PREFIXES = """
            @prefix owl: <http://www.w3.org/2002/07/owl#> .
            @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
            @prefix ex: <https://example.org/dummy-ontology/> .
            @prefix vs: <http://www.w3.org/2003/06/sw-vocab-status/ns#> .
            ex: a owl:Ontology .
            """;

    private static Stream<Arguments> matchesOwlApiArguments() {
        String annotation = "ex:C a owl:Class ; vs:term_status \"stable\" .\n";
        return Stream.of(
                Arguments.of("undeclared VS", annotation, 0),
                Arguments.of("rdf:Property", annotation + "vs:term_status a rdf:Property .", 0),
                Arguments.of("annotation property", annotation + "vs:term_status a owl:AnnotationProperty .", 0),
                Arguments.of("data property", annotation + "vs:term_status a owl:DatatypeProperty .", 1),
                Arguments.of("explicit individual", annotation + "ex:C a owl:NamedIndividual .", 1),
                Arguments.of("class assertion", annotation + "ex:C a ex:D . ex:D a owl:Class .", 1),
                Arguments.of("object assertion", annotation + "ex:p a owl:ObjectProperty . ex:C ex:p ex:i .", 2),
                Arguments.of("custom vocabulary", "ex:C a owl:Class ; ex:status \"stable\" .", 0),
                Arguments.of("two classes", annotation + "ex:D a owl:Class ; vs:term_status \"unstable\" .", 0),
                Arguments.of("NCBITAXON pattern", """
                        ex:C a owl:Class ; ex:cui "C2768480" ; ex:rank "species" .
                        ex:rank a owl:DatatypeProperty .
                        """, 1)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("matchesOwlApiArguments")
    public void testMatchesOwlApi(String name, String body, int individuals) throws Exception {
        var ontapi = OntManagers.createManager().loadOntologyFromOntologyDocument(
                OWLIOUtils.getStringDocumentSource(PREFIXES + body, OntFormat.TURTLE));
        var owlapi = OntManagers.createOWLAPIImplManager().loadOntologyFromOntologyDocument(
                OWLIOUtils.getStringDocumentSource(PREFIXES + body, OntFormat.TURTLE));
        Assertions.assertEquals(individuals, owlapi.getIndividualsInSignature().size(), name);
        Assertions.assertEquals(owlapi.getIndividualsInSignature(), ontapi.getIndividualsInSignature(), name);
        Assertions.assertEquals(owlapi.axioms().filter(a -> !a.isOfType(AxiomType.DECLARATION)).collect(Collectors.toSet()),
                ontapi.axioms().filter(a -> !a.isOfType(AxiomType.DECLARATION)).collect(Collectors.toSet()), name);
    }
}
