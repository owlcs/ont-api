package ru.avicomp.ontapi.jena.model;

import java.util.stream.Stream;

/**
 * Common interface for Class Expressions
 * <p>
 * Created by szuev on 01.11.2016.
 */
public interface OntCE extends OntObject {
    @Override
    default boolean isCE() {
        return true;
    }

    Stream<OntCE> subClassOf();

    interface UnionOf extends OntCE {

    }

    interface OneOf extends OntCE {
    }

    interface IntersectionOf extends OntCE {
    }
}
