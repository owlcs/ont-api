package ru.avicomp.ontapi.jena.model;

import java.util.stream.Stream;

import org.apache.jena.rdf.model.Property;

/**
 * (Named) Datatype Property here.
 * <p>
 * Created by szuev on 01.11.2016.
 */
public interface OntNDP extends OntPE, OntEntity, Property {
    @Override
    Stream<OntCE> domain();

    @Override
    Stream<OntDR> range();

    void setFunctional(boolean functional);

    boolean isFunctional();

    @Override
    default boolean isProperty() {
        return true;
    }

    @Override
    default int getOrdinal() {
        return as(Property.class).getOrdinal();
    }

}
