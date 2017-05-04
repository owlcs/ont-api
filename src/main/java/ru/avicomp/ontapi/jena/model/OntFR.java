package ru.avicomp.ontapi.jena.model;

import org.apache.jena.rdf.model.Literal;

/**
 * The facet restriction.
 * <p>
 * Created by @szuev on 02.11.2016.
 * @see ru.avicomp.ontapi.jena.vocabulary.XSD
 */
public interface OntFR extends OntObject {
    Literal getValue();

    interface Pattern extends OntFR {
    }

    interface Length extends OntFR {
    }

    interface MinLength extends OntFR {
    }

    interface MaxLength extends OntFR {
    }

    interface MinInclusive extends OntFR {
    }

    interface MaxInclusive extends OntFR {
    }

    interface MinExclusive extends OntFR {
    }

    interface MaxExclusive extends OntFR {
    }

    interface TotalDigits extends OntFR {
    }

    interface FractionDigits extends OntFR {
    }

    interface LangRange extends OntFR {
    }

}
