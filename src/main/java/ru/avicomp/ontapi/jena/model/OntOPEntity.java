package ru.avicomp.ontapi.jena.model;

import java.util.stream.Stream;

/**
 * Object property
 * Created by szuev on 01.11.2016.
 */
public interface OntOPEntity extends OntPropertyEntity, OntPE {
    @Override
    Stream<OntCE> domain();

    @Override
    Stream<OntCE> range();

}
