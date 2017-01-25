package ru.avicomp.ontapi.jena.impl.configuration;

/**
 * Possibility to change default behaviour.
 * <p>
 * Currently the are two modes: {@link Mode#LAX} and {@link Mode#STRICT}.
 * The last one is to exclude illegal punnings from consideration.
 * <p>
 * Created by @szuev on 21.01.2017.
 */
public interface Configurable<T> {

    /**
     * returns new object with specified mode assigned.
     *
     * @param m Mode
     * @return new object of type <T>
     */
    T select(Mode m);

    enum Mode {
        /**
         * The following punnings are considered as illegal and excluded by this mode:
         * - owl:Class <-> rdfs:Datatype
         * - owl:ObjectProperty <-> owl:DatatypeProperty
         * - owl:ObjectProperty <-> owl:AnnotationProperty
         * - owl:AnnotationProperty <-> owl:DatatypeProperty
         */
        STRICT,
        /**
         * Don't care about illegal punnings.
         */
        LAX,
    }

}
