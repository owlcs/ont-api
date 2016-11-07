package ru.avicomp.ontapi.jena.model;

import java.util.stream.Stream;

import org.apache.jena.rdf.model.Literal;

/**
 * Common interface for Data Ranges.
 * See for example <a href='https://www.w3.org/TR/owl2-quick-reference/'>2.4 Data Ranges</a>
 * <p>
 * Created by szuev on 01.11.2016.
 */
public interface OntDR extends OntObject {
    interface OneOf extends OntDR {
        Stream<Literal> values();
    }

    interface Restriction extends OntDR {
        OntDatatypeEntity getDatatype();

        Stream<OntFR> facetRestrictions();
    }

    interface ComplementOf extends OntDR {
        OntDR getDataRange();
    }

    interface UnionOf extends OntDR {
        Stream<OntDR> dataRanges();
    }

    interface IntersectionOf extends OntDR {
        Stream<OntDR> dataRanges();
    }

}
