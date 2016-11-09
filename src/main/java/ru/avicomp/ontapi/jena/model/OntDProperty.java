package ru.avicomp.ontapi.jena.model;

import java.util.stream.Stream;

/**
 * (Named) Datatype Property here.
 * <p>
 * Created by szuev on 01.11.2016.
 */
public interface OntDProperty extends OntPE, OntEntity {
    @Override
    Stream<OntCE> domain();

    @Override
    Stream<OntDR> range();

    void setFunctional(boolean functional);

    boolean isFunctional();

}
