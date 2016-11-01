package ru.avicomp.ontapi.jena.model;

/**
 * Data Range.
 * <p>
 * Created by szuev on 01.11.2016.
 */
public interface OntDR extends OntObject {
    @Override
    default boolean isDR() {
        return true;
    }

    default boolean isDatatype() {
        return false;
    }
}
