package ru.avicomp.ontapi.jena.model;

import java.util.stream.Stream;

import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

/**
 * for named and anonymous individuals
 *
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
     * Anonymous Individual here.
     * <p>
     * Created by szuev on 10.11.2016.
     */
    interface Anonymous extends OntIndividual {
    }
}
