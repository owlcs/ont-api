package ru.avicomp.ontapi.jena.model;

import java.util.stream.Stream;

/**
 * Data Property here.
 * <p>
 * Created by szuev on 01.11.2016.
 */
public interface OntDPEntity extends OntPropertyEntity {
    @Override
    Stream<OntCE> domain();

    @Override
    Stream<OntDR> range();

    @Override
    default Type getOntType() {
        return Type.DATA_PROPERTY;
    }
}
