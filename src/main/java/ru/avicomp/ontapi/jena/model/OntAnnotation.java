package ru.avicomp.ontapi.jena.model;

import java.util.stream.Stream;

/**
 * An annotation ont-object.
 * It's the anonymous jena-resource ({@link OntObject}) with one of the two types:
 * - owl:Axiom ({@link ru.avicomp.ontapi.jena.vocabulary.OWL#Axiom}) for root annotations, it is usually owned by axiomatic statements.
 * - owl:Annotation ({@link ru.avicomp.ontapi.jena.vocabulary.OWL#Annotation}) for sub-annotations,
 * and also for annotation of several specific axioms with main-statement '_:x rdf:type @type' where @type is
 * owl:AllDisjointClasses, owl:AllDisjointProperties, owl:AllDifferent or owl:NegativePropertyAssertion.
 * Example:
 * <pre>
 * [ a                      owl:Axiom ;
 *   rdfs:comment           "some comment 1", "some comment 2"@fr ;
 *   owl:annotatedProperty  rdf:type ;
 *   owl:annotatedSource    <http://example.test.org#SomeClassN1> ;
 *   owl:annotatedTarget    owl:Class
 * ] .
 * </pre>
 * <p>
 * Created by @szuev on 26.03.2017.
 */
public interface OntAnnotation extends OntObject {

    /**
     * Returns the annotations assertions attached to this object.
     * The example above contains two such statements: '_:x rdfs:comment "comment1";' and '_:x rdfs:comment "comment2"@fr'.
     *
     * @return Stream of annotation statements {@link OntStatement}s,
     */
    Stream<OntStatement> assertions();

}
