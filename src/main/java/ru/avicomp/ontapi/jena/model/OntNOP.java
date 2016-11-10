package ru.avicomp.ontapi.jena.model;

/**
 * (Named) Object property
 * Created by szuev on 01.11.2016.
 */
public interface OntNOP extends OntOPE, OntEntity {

    /**
     * inverse this property
     *
     * @return new anonymous OntOPE resource
     */
    Inverse createInverse();
}
