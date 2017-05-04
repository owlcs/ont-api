package ru.avicomp.ontapi.jena.model;

import org.apache.jena.rdf.model.Property;

/**
 * (Named) Object property.
 * Created by szuev on 01.11.2016.
 */
public interface OntNOP extends OntOPE, OntEntity, Property {

    /**
     * Makes inverse of this property.
     *
     * @return {@link OntOPE.Inverse} new anonymous OntOPE resource
     */
    Inverse createInverse();

    /**
     * @see Property#isProperty()
     */
    @Override
    default boolean isProperty() {
        return true;
    }

    /**
     * @see Property#getOrdinal()
     */
    @Override
    default int getOrdinal() {
        return as(Property.class).getOrdinal();
    }
}
