package ru.avicomp.ontapi.jena.model;

import java.util.stream.Stream;

/**
 * Data Property here.
 * <p>
 * Created by szuev on 01.11.2016.
 */
public interface OntDPEntity extends OntPropertyEntity {
    @Override
    default boolean isDataProperty() {
        return true;
    }

    @Override
    Stream<OntCE> getDomain();

    @Override
    Stream<OntDR> getRange();
}
