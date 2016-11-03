package ru.avicomp.ontapi.jena.model;

import java.util.stream.Stream;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;

/**
 * Common interface for Class Expressions
 * see, for example <a href='https://www.w3.org/TR/owl2-quick-reference/'>2.1 Class Expressions</a>
 * <p>
 * Created by szuev on 01.11.2016.
 */
public interface OntCE extends OntObject {

    Stream<OntCE> subClassOf();

    interface ObjectSomeValuesFrom extends OntCE, Component<OntCE>, ONProperty {
    }

    interface DataSomeValuesFrom extends OntCE, Component<OntDR>, ONProperty {
    }

    interface ObjectAllValuesFrom extends OntCE, Component<OntCE>, ONProperty {
    }

    interface DataAllValuesFrom extends OntCE, Component<OntDR>, ONProperty {
    }

    interface ObjectMinCardinality extends OntCE, CardinalityRestriction<OntCE> {
    }

    interface DataMinCardinality extends OntCE, CardinalityRestriction<OntDR> {
    }

    interface ObjectMaxCardinality extends OntCE, CardinalityRestriction<OntCE> {
    }

    interface DataMaxCardinality extends OntCE, CardinalityRestriction<OntDR> {
    }

    interface ObjectCardinality extends OntCE, CardinalityRestriction<OntCE> {
    }

    interface DataCardinality extends OntCE, CardinalityRestriction<OntDR> {
    }

    interface ObjectHasValue extends OntCE, Value<OntIndividual>, ONProperty {
    }

    interface DataHasValue extends OntCE, Value<Literal>, ONProperty {
    }

    interface HasSelf extends OntCE, ONProperty {
    }

    interface UnionOf extends OntCE, Components<OntCE> {
    }

    interface OneOf extends OntCE, Components<OntCE> {
    }

    interface IntersectionOf extends OntCE, Components<OntCE> {
    }

    interface ComplementOf extends OntCE, Component<OntCE> {
    }

    interface NaryDataAllValuesFrom extends OntCE, Component<OntDR>, ONProperties {
    }

    interface NaryDataSomeValuesFrom extends OntCE, Component<OntDR>, ONProperties {
    }

    /**
     * *
     * Technical interfaces
     * *
     */

    interface ONProperty {
        OntPE onProperty();
    }

    interface ONProperties {
        Stream<OntPE> onProperties();
    }

    interface Component<T extends OntObject> {
        T getComponent();
    }

    interface Components<T extends OntObject> {
        Stream<T> components();
    }

    interface Cardinality {
        Integer getCardinality();

        boolean isQualified();
    }

    interface Value<T extends RDFNode> {
        T getValue();
    }

    interface CardinalityRestriction<T extends OntObject> extends Component<T>, ONProperty, Cardinality {
    }

}

