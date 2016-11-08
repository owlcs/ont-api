package ru.avicomp.ontapi.jena.model;

import java.util.stream.Stream;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;

/**
 * Common interface for Class Expressions.
 * See for example <a href='https://www.w3.org/TR/owl2-quick-reference/'>2.1 Class Expressions</a>
 * <p>
 * Created by szuev on 01.11.2016.
 */
public interface OntCE extends OntObject {

    Stream<OntCE> subClassOf();

    /**
     * ============================
     * all known Class Expressions:
     * ============================
     */

    interface ObjectSomeValuesFrom extends ComponentRestrictionCE<OntCE> {
    }

    interface DataSomeValuesFrom extends ComponentRestrictionCE<OntDR> {
    }

    interface ObjectAllValuesFrom extends ComponentRestrictionCE<OntCE> {
    }

    interface DataAllValuesFrom extends ComponentRestrictionCE<OntDR> {
    }

    interface ObjectHasValue extends ComponentRestrictionCE<OntIndividual> {
    }

    interface DataHasValue extends ComponentRestrictionCE<Literal> {
    }

    interface ObjectMinCardinality extends CardinalityRestrictionCE<OntCE> {
    }

    interface DataMinCardinality extends CardinalityRestrictionCE<OntDR> {
    }

    interface ObjectMaxCardinality extends CardinalityRestrictionCE<OntCE> {
    }

    interface DataMaxCardinality extends CardinalityRestrictionCE<OntDR> {
    }

    interface ObjectCardinality extends CardinalityRestrictionCE<OntCE> {
    }

    interface DataCardinality extends CardinalityRestrictionCE<OntDR> {
    }

    interface HasSelf extends RestrictionCE, ONProperty {
    }

    interface UnionOf extends ComponentsCE<OntCE> {
    }

    interface OneOf extends ComponentsCE<OntIndividual> {
    }

    interface IntersectionOf extends ComponentsCE<OntCE> {
    }

    interface ComplementOf extends OntCE, Value<OntCE> {
    }

    interface NaryDataAllValuesFrom extends NaryRestrictionCE<OntDR> {
    }

    interface NaryDataSomeValuesFrom extends NaryRestrictionCE<OntDR> {
    }

    /**
     * ======================================
     * Technical interfaces for abstract CEs:
     * ======================================
     */

    interface ONProperty {
        OntPE getOnProperty();

        void setOnProperty(OntPE p);
    }

    interface ONProperties {
        Stream<OntPE> onProperties();

        void setOnProperties(Stream<OntPE> properties);
    }

    interface Components<T extends OntObject> {
        Stream<T> components();

        void setComponents(Stream<T> components);
    }


    interface Value<T extends RDFNode> {
        T getValue();

        void setValue(T value);
    }

    interface Cardinality {
        int getCardinality();

        void setCardinality(int cardinality);

        /**
         * Determines if this restriction is qualified. Qualified cardinality
         * restrictions are defined to be cardinality restrictions that have fillers
         * which aren't TOP (owl:Thing or rdfs:Literal). An object restriction is
         * unqualified if it has a filler that is owl:Thing. A data restriction is
         * unqualified if it has a filler which is the top data type (rdfs:Literal).
         *
         * @return {@code true} if this restriction is qualified, or {@code false}
         * if this restriction is unqualified.
         */
        boolean isQualified();
    }

    /**
     * ============================
     * Interfaces for Abstract CEs:
     * ============================
     */

    interface ComponentsCE<T extends OntObject> extends OntCE, Components<T> {
    }

    interface RestrictionCE extends OntCE {
    }

    interface ComponentRestrictionCE<T extends RDFNode> extends RestrictionCE, ONProperty, Value<T> {
    }

    interface CardinalityRestrictionCE<T extends OntObject> extends Cardinality, ComponentRestrictionCE<T> {
    }

    interface NaryRestrictionCE<T extends OntObject> extends RestrictionCE, ONProperties, Value<T> {
    }


}

