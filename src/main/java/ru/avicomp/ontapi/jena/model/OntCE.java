package ru.avicomp.ontapi.jena.model;

import java.util.stream.Stream;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDFS;

/**
 * Common interface for Class Expressions.
 * See for example <a href='https://www.w3.org/TR/owl2-quick-reference/'>2.1 Class Expressions</a>
 * <p>
 * Created by szuev on 01.11.2016.
 */
public interface OntCE extends OntObject {

    Stream<OntCE> subClassOf();

    default OntStatement addSubClassOf(OntCE superClass) {
        return addStatement(RDFS.subClassOf, superClass);
    }

    default void removeSubClassOf(OntCE superClass) {
        remove(RDFS.subClassOf, superClass);
    }

    default OntStatement addDisjointWith(OntCE other) {
        return addStatement(OWL2.disjointWith, other);
    }

    default void removeDisjointWith(OntCE other) {
        remove(OWL2.disjointWith, other);
    }

    /**
     * ============================
     * all known Class Expressions:
     * ============================
     */

    interface ObjectSomeValuesFrom extends ComponentRestrictionCE<OntCE, OntOPE> {
    }

    interface DataSomeValuesFrom extends ComponentRestrictionCE<OntDR, OntNDP> {
    }

    interface ObjectAllValuesFrom extends ComponentRestrictionCE<OntCE, OntOPE> {
    }

    interface DataAllValuesFrom extends ComponentRestrictionCE<OntDR, OntNDP> {
    }

    interface ObjectHasValue extends ComponentRestrictionCE<OntIndividual, OntOPE> {
    }

    interface DataHasValue extends ComponentRestrictionCE<Literal, OntNDP> {
    }

    interface ObjectMinCardinality extends CardinalityRestrictionCE<OntCE, OntOPE> {
    }

    interface DataMinCardinality extends CardinalityRestrictionCE<OntDR, OntNDP> {
    }

    interface ObjectMaxCardinality extends CardinalityRestrictionCE<OntCE, OntOPE> {
    }

    interface DataMaxCardinality extends CardinalityRestrictionCE<OntDR, OntNDP> {
    }

    interface ObjectCardinality extends CardinalityRestrictionCE<OntCE, OntOPE> {
    }

    interface DataCardinality extends CardinalityRestrictionCE<OntDR, OntNDP> {
    }

    interface HasSelf extends RestrictionCE, ONProperty<OntOPE> {
    }

    interface UnionOf extends ComponentsCE<OntCE> {
    }

    interface OneOf extends ComponentsCE<OntIndividual> {
    }

    interface IntersectionOf extends ComponentsCE<OntCE> {
    }

    interface ComplementOf extends OntCE, Value<OntCE> {
    }

    interface NaryDataAllValuesFrom extends NaryRestrictionCE<OntDR, OntNDP> {
    }

    interface NaryDataSomeValuesFrom extends NaryRestrictionCE<OntDR, OntNDP> {
    }

    /**
     * ======================================
     * Technical interfaces for abstract CEs:
     * ======================================
     */

    interface ONProperty<P extends OntPE> {
        P getOnProperty();

        void setOnProperty(P p);
    }

    interface ONProperties<P extends OntPE> {
        Stream<P> onProperties();

        void setOnProperties(Stream<P> properties);
    }

    interface Components<O extends OntObject> {
        Stream<O> components();

        void setComponents(Stream<O> components);
    }


    interface Value<O extends RDFNode> {
        O getValue();

        void setValue(O value);
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

    interface ComponentsCE<O extends OntObject> extends OntCE, Components<O> {
    }

    interface RestrictionCE extends OntCE {
    }

    interface ComponentRestrictionCE<O extends RDFNode, P extends OntPE> extends RestrictionCE, ONProperty<P>, Value<O> {
    }

    interface CardinalityRestrictionCE<O extends OntObject, P extends OntPE> extends Cardinality, ComponentRestrictionCE<O, P> {
    }

    interface NaryRestrictionCE<O extends OntObject, P extends OntPE> extends RestrictionCE, ONProperties<P>, Value<O> {
    }


}

