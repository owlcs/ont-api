package ru.avicomp.ontapi.jena.model;

import java.util.stream.Stream;

import ru.avicomp.ontapi.jena.vocabulary.OWL;

/**
 * The datatype resource (both anonymous and named).
 * <p>
 * Created by szuev on 01.11.2016.
 */
public interface OntDT extends OntEntity, OntDR {

    /**
     * Returns all equivalent data ranges.
     * The pattern "DN owl:equivalentClass D" to search.
     *
     * @return Stream of {@link OntDR}s.
     * @see OntCE#equivalentClass()
     */
    default Stream<OntDR> equivalentClass() {
        return objects(OWL.equivalentClass, OntDR.class);
    }

    /**
     * Creates an equivalent class statement
     *
     * @param other {@link OntDR}, not null.
     * @return {@link OntStatement}
     * @see OntCE#addEquivalentClass(OntCE)
     */
    default OntStatement addEquivalentClass(OntDR other) {
        return addStatement(OWL.equivalentClass, other);
    }

    /**
     * Removes equivalent data range
     *
     * @param other {@link OntDR}
     * @see OntCE#removeEquivalentClass(OntCE)
     */
    default void removeEquivalentClass(OntDR other) {
        remove(OWL.equivalentClass, other);
    }
}
