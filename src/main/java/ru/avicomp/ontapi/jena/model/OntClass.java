package ru.avicomp.ontapi.jena.model;

import java.util.Collection;
import java.util.stream.Stream;

/**
 * The Class Entity (named class expression)
 * <p>
 * Created by szuev on 01.11.2016.
 */
public interface OntClass extends OntEntity, OntCE {

    /**
     * Creates a disjoint-union section. The pattern: "CN owl:disjointUnionOf (C1 ... CN)"
     *
     * @param classes the collection of {@link OntCE}s
     * @return {@link OntStatement}
     */
    OntStatement addDisjointUnionOf(Collection<OntCE> classes);

    /**
     * Removes all statements with predicate owl:disjointUnionOf including their content.
     */
    void removeDisjointUnionOf();

    /**
     * Returns all class expressions from the right part of owl:disjointUnionOf construction.
     * @return distinct stream of {@link OntCE}s.
     */
    Stream<OntCE> disjointUnionOf();

}
