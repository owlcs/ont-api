package ru.avicomp.ontapi.jena.model;

import org.apache.jena.rdf.model.Literal;

/**
 * TODO:
 * Facet Restriction.
 * <p>
 * Created by @szuev on 02.11.2016.
 */
public interface OntFR extends OntObject {
    Literal getValue();

    enum Type {
        LENGTH, MIN_LENGTH, MAX_LENGTH, PATTERN, MIN_INCLUSIVE, MIN_EXCLUSIVE, MAX_INCLUSIVE, MAX_EXCLUSIVE, TOTAL_DIGITS, FRACTION_DIGIT, LANG_RANGE
    }
}
