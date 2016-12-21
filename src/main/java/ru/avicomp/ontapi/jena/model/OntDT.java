package ru.avicomp.ontapi.jena.model;

import java.util.stream.Stream;

import ru.avicomp.ontapi.jena.vocabulary.OWL2;

/**
 * Datatype Resource.
 * <p>
 * Created by szuev on 01.11.2016.
 */
public interface OntDT extends OntEntity, OntDR {

    default Stream<OntDR> equivalentClass() {
        return objects(OWL2.equivalentClass, OntDR.class);
    }

    default OntStatement addEquivalentClass(OntDR other) {
        return addStatement(OWL2.equivalentClass, other);
    }

    default void removeEquivalentClass(OntDR other) {
        remove(OWL2.equivalentClass, other);
    }
}
