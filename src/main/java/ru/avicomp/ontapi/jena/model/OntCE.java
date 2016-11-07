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

    interface ObjectHasValue extends ValueRestrictionCE<OntIndividual> {
    }

    interface DataHasValue extends ValueRestrictionCE<Literal> {
    }

    interface HasSelf extends RestrictionCE, ONProperty {
    }

    interface UnionOf extends ComponentsCE<OntCE> {
    }

    interface OneOf extends ComponentsCE<OntIndividual> {
    }

    interface IntersectionOf extends ComponentsCE<OntCE> {
    }

    interface ComplementOf extends OntCE, Component<OntCE> {
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

        void setOntProperty(OntPE p);
    }

    interface ONProperties {
        Stream<OntPE> onProperties();
    }

    interface Component<T extends OntObject> {
        T getComponent();

        void setComponent(T c);
    }

    interface Components<T extends OntObject> {
        Stream<T> components();

        void setComponents(Stream<T> components);

        void clear();
    }

    interface Cardinality {
        Integer getCardinality();

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

    interface Value<T extends RDFNode> {
        T getValue();
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

    interface ComponentRestrictionCE<T extends OntObject> extends RestrictionCE, Component<T>, ONProperty {
    }

    interface ValueRestrictionCE<T extends RDFNode> extends RestrictionCE, Value<T>, ONProperty {
    }

    interface NaryRestrictionCE<T extends OntObject> extends RestrictionCE, ONProperties, Component<T> {
    }

    interface CardinalityRestrictionCE<T extends OntObject> extends ComponentRestrictionCE<T>, Cardinality {
    }

}

