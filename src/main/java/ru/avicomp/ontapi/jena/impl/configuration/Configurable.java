package ru.avicomp.ontapi.jena.impl.configuration;

import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.model.OntObject;

/**
 *
 * This is our analogue of {@link java.util.function.Function} which is used as an objects container with possibility to choose one depending on the parameter.
 * For simplification the code and to be able to change the default behaviour easily during the initialization of personalities.
 *
 * <p>
 * Currently the are only two modes: {@link Mode#LAX} and {@link Mode#STRICT}.
 * The first one is a lax way. Any owl-entity could have more than one types simultaneously, there is no any restrictions.
 * The second one is to exclude so called 'illegal punnings' (property and class/datatype intersections) from consideration,
 * i.e. the interpretation of such things as owl-entity ({@link ru.avicomp.ontapi.jena.model.OntEntity}) is prohibited,
 * but they still can be treated as any other objects ({@link OntObject})
 * <p>
 * Created by @szuev on 21.01.2017.
 */
@FunctionalInterface
public interface Configurable<T> {

    /**
     * Choose object by the given argument.
     *
     * @param t Mode
     * @return the wrapped object.
     */
    T select(Configurable.Mode t);

    default T get(Mode m) {
        return OntJenaException.notNull(select(OntJenaException.notNull(m, "Null mode.")), "Null result for mode " + m + ".");
    }

    enum Mode {
        /**
         * The following punnings are considered as illegal and will be excluded:
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
