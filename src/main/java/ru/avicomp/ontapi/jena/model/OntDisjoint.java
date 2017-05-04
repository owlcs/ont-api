package ru.avicomp.ontapi.jena.model;

import java.util.stream.Stream;

/**
 * for anonymous collections (owl:AllDisjointProperties,  owl:AllDisjointClasses, owl:AllDifferent).
 * Example:
 * _:x rdf:type owl:AllDisjointProperties; _:x owl:members ( R1 … Rn ).
 * <p>
 * Created by @szuev on 15.11.2016.
 */
public interface OntDisjoint<O extends OntObject> extends OntObject {

    /**
     * @return Stream (not distinct) of all members ({@link OntObject}s).
     */
    Stream<O> members();

    interface Classes extends OntDisjoint<OntCE> {
    }

    interface Individuals extends OntDisjoint<OntIndividual> {
    }

    interface ObjectProperties extends Properties<OntOPE> {
    }

    interface DataProperties extends Properties<OntNDP> {
    }

    interface Properties<P extends OntPE> extends OntDisjoint<P> {
    }
}
