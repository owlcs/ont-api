package ru.avicomp.ontapi.jena.model;

import java.util.stream.Stream;

import org.apache.jena.rdf.model.Literal;

/**
 * Data Range.
 * <p>
 * Created by szuev on 01.11.2016.
 */
public interface OntDR extends OntObject {
    interface OneOf extends OntDR {
        Stream<Literal> values();

        @Override
        default OntType getOntType() {
            return Type.ONE_OF;
        }
    }

    interface Restriction extends OntDR {
        OntDatatypeEntity getDatatype();

        Stream<OntFR> facetRestrictions();

        @Override
        default OntType getOntType() {
            return Type.RESTRICTION;
        }
    }

    interface ComplementOf extends OntDR {
        OntDR getDataRange();

        @Override
        default OntType getOntType() {
            return Type.COMPLEMENT_OF;
        }
    }

    interface UnionOf extends OntDR {
        Stream<OntDR> dataRanges();

        @Override
        default OntType getOntType() {
            return Type.UNION_OF;
        }
    }

    interface IntersectionOf extends OntDR {
        Stream<OntDR> dataRanges();

        @Override
        default OntType getOntType() {
            return Type.INTERSECTION_OF;
        }
    }

    enum Type implements OntType {
        ONE_OF, RESTRICTION, COMPLEMENT_OF, UNION_OF, INTERSECTION_OF,;

        @Override
        public boolean isDR() {
            return true;
        }
    }

}
