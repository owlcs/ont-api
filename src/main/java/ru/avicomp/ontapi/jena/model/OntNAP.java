package ru.avicomp.ontapi.jena.model;

import java.util.stream.Stream;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

/**
 * (Named) Annotation Property resource.
 * <p>
 * Created by szuev on 01.11.2016.
 */
public interface OntNAP extends OntPE, OntEntity, Property {

    OntStatement addDomain(Resource domain);

    OntStatement addRange(Resource range);

    @Override
    Stream<Resource> domain();

    @Override
    Stream<Resource> range();

    @Override
    default boolean isProperty() {
        return true;
    }

    @Override
    default int getOrdinal() {
        return as(Property.class).getOrdinal();
    }
}
