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
     * - it has a class declaration (i.e. there is a triple "_:a rdf:type C", where C is a class expression).
     * - it is a subject or an object in a statement with predicate owl:sameAs or owl:differentFrom.
     * - it is contained in a rdf:List with predicate owl:distinctMembers or owl:members in a blank node with rdf:type owl:AllDifferent
     * - it is contained in a rdf:List with predicate owl:oneOf in a blank node with rdf:type owl:Class.
     * - it is a part of owl:Axiom or owl:Annotation (bulk annotation) with predicate owl:annotatedTarget or owl:annotatedSource.
     * - it is a part of owl:NegativePropertyAssertion section with predicates owl:sourceIndividual, owl:targetIndividual.
     * - it is a subject or an object in a statement where predicate is a uri-resource("A") with type owl:AnnotationProperty (annotation property assertion "s A t"),
     * - it is a subject in a triple which corresponds data property assertions "_:a R v" (where "R" is a data property, "v" is literal).
     * - it is a subject or a object in a triple which corresponds object property assertion "_:a1 PN _:a2" (where PN is a named object property)
     * <p>
     * Created by szuev on 10.11.2016.
     */
    interface Anonymous extends OntIndividual {
    }
}
