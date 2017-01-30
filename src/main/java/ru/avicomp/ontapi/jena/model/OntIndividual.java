package ru.avicomp.ontapi.jena.model;

import java.util.stream.Stream;

import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

/**
 * for named and anonymous individuals
 * <p>
 * Created by @szuev on 02.11.2016.
 */
public interface OntIndividual extends OntObject {

    OntStatement attachClass(OntCE clazz);

    /**
     * removes class assertion for the specified class expression
     *
     * @param clazz {@link OntCE}
     * @throws ru.avicomp.ontapi.jena.OntJenaException in case it is anonymous individual and there is no more class-assertions.
     */
    void detachClass(OntCE clazz);

    default Stream<OntCE> classes() {
        return objects(RDF.type, OntCE.class);
    }

    default Stream<OntIndividual> sameAs() {
        return objects(OWL.sameAs, OntIndividual.class);
    }

    default OntStatement addSameAs(OntIndividual other) {
        return addStatement(OWL.sameAs, other);
    }

    default void removeSameAs(OntIndividual other) {
        remove(OWL.sameAs, other);
    }

    default Stream<OntIndividual> differentFrom() {
        return objects(OWL.differentFrom, OntIndividual.class);
    }

    default OntStatement addDifferentFrom(OntIndividual other) {
        return addStatement(OWL.differentFrom, other);
    }

    default void removeDifferentFrom(OntIndividual other) {
        remove(OWL.differentFrom, other);
    }

    /**
     * Named Individual here.
     * <p>
     * Created by szuev on 01.11.2016.
     */
    interface Named extends OntIndividual, OntEntity {
    }

    /**
     * Class for Anonymous Individuals.
     * The anonymous individual is a blank node ("_:a") which satisfies one of the following conditions:
     * 1) it has a class declaration (i.e. there is a triple "_:a rdf:type C", where C is a class expression)
     * 2) it is a subject or an object in a statement with predicate owl:sameAs or owl:differentFrom
     * 3) it is contained in a rdf:List with predicate owl:distinctMembers or owl:members in a blank node with rdf:type owl:AllDifferent
     * 4) it is contained in a rdf:List with predicate owl:oneOf in a blank node with rdf:type owl:Class
     * 5) it is a part of owl:NegativePropertyAssertion section with predicates owl:sourceIndividual or owl:targetIndividual
     * 6) it is an object with predicate owl:hasValue inside "_:x rdf:type owl:Restriction" (Object Property Restriction)
     * 7) it is a subject or an object in a statement where predicate is an uri-resource with rdf:type owl:AnnotationProperty (i.e. annotation property assertion "s A t")
     * 8) it is a subject in a triple which corresponds data property assertion "_:a R v" (where "R" is a datatype property, "v" is a literal)
     * 9) it is a subject or an object in a triple which corresponds object property assertion "_:a1 PN _:a2" (where PN is a named object property)
     *
     * <p>
     * Created by szuev on 10.11.2016.
     */
    interface Anonymous extends OntIndividual {
    }
}
