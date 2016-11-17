package ru.avicomp.ontapi.jena.model;

import java.util.stream.Stream;

/**
 * OWLClass Entity (named class expression)
 * <p>
 * Created by szuev on 01.11.2016.
 */
public interface OntClass extends OntEntity, OntCE {

    OntStatement addDisjointUnionOf(Stream<OntCE> classes);

    void removeDisjointUnionOf();

    Stream<OntCE> disjointUnionOf();

}
