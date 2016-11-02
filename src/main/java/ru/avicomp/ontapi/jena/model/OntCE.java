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
        @Override
        default OntType getOntType() {
            return Type.OBJECT_SOME_VALUES_FROM;
        }
    }

    interface DataSomeValuesFrom extends OntCE, Component<OntDR>, ONProperty {
        @Override
        default OntType getOntType() {
            return Type.DATA_SOME_VALUES_FROM;
        }
    }

    interface ObjectAllValuesFrom extends OntCE, Component<OntCE>, ONProperty {
        @Override
        default OntType getOntType() {
            return Type.OBJECT_ALL_VALUES_FROM;
        }
    }

    interface DataAllValuesFrom extends OntCE, Component<OntDR>, ONProperty {
        @Override
        default OntType getOntType() {
            return Type.DATA_ALL_VALUES_FROM;
        }
    }

    interface ObjectMinCardinality extends OntCE, CardinalityRestriction<OntCE> {
        @Override
        default OntType getOntType() {
            return Type.OBJECT_MIN_CARDINALITY;
        }
    }

    interface DataMinCardinality extends OntCE, CardinalityRestriction<OntDR> {
        @Override
        default OntType getOntType() {
            return Type.DATA_MIN_CARDINALITY;
        }
    }

    interface ObjectMaxCardinality extends OntCE, CardinalityRestriction<OntCE> {
        @Override
        default OntType getOntType() {
            return Type.OBJECT_MAX_CARDINALITY;
        }
    }

    interface DataMaxCardinality extends OntCE, CardinalityRestriction<OntDR> {
        @Override
        default OntType getOntType() {
            return Type.DATA_MAX_CARDINALITY;
        }
    }

    interface ObjectCardinality extends OntCE, CardinalityRestriction<OntCE> {
        @Override
        default OntType getOntType() {
            return Type.OBJECT_EXACT_CARDINALITY;
        }
    }

    interface DataCardinality extends OntCE, CardinalityRestriction<OntDR> {
        @Override
        default OntType getOntType() {
            return Type.DATA_EXACT_CARDINALITY;
        }
    }

    interface ObjectHasValue extends OntCE, Value<OntIndividual>, ONProperty {
        @Override
        default OntType getOntType() {
            return Type.OBJECT_HAS_VALUE;
        }
    }

    interface DataHasValue extends OntCE, Value<Literal>, ONProperty {
        @Override
        default OntType getOntType() {
            return Type.DATA_HAS_VALUE;
        }
    }

    interface HasSelf extends OntCE, ONProperty {
        @Override
        default OntType getOntType() {
            return Type.HAS_SELF;
        }
    }

    interface UnionOf extends OntCE, Components<OntCE> {
        @Override
        default OntType getOntType() {
            return Type.UNION_OF;
        }
    }

    interface OneOf extends OntCE, Components<OntCE> {
        @Override
        default OntType getOntType() {
            return Type.ONE_OF;
        }
    }

    interface IntersectionOf extends OntCE, Components<OntCE> {
        @Override
        default OntType getOntType() {
            return Type.INTERSECTION_OF;
        }
    }

    interface ComplementOf extends OntCE, Component<OntCE> {
        @Override
        default OntType getOntType() {
            return Type.COMPLEMENT_OF;
        }
    }

    interface NaryDataAllValuesFrom extends OntCE, Component<OntDR>, ONProperties {
        @Override
        default OntType getOntType() {
            return Type.NARY_DATA_ALL_VALUES_FROM;
        }
    }

    interface NaryDataSomeValuesFrom extends OntCE, Component<OntDR>, ONProperties {
        @Override
        default OntType getOntType() {
            return Type.NARY_DATA_SOME_VALUES_FROM;
        }
    }

    enum Type implements OntType {
        OBJECT_SOME_VALUES_FROM, DATA_SOME_VALUES_FROM,
        OBJECT_ALL_VALUES_FROM, DATA_ALL_VALUES_FROM,
        OBJECT_MIN_CARDINALITY, DATA_MIN_CARDINALITY,
        OBJECT_MAX_CARDINALITY, DATA_MAX_CARDINALITY,
        OBJECT_EXACT_CARDINALITY, DATA_EXACT_CARDINALITY,
        OBJECT_HAS_VALUE, DATA_HAS_VALUE,
        // object only CE types:
        HAS_SELF, INTERSECTION_OF, UNION_OF, ONE_OF, COMPLEMENT_OF,
        // data only (n-ary, not used inside OWL-API) types:
        NARY_DATA_ALL_VALUES_FROM, NARY_DATA_SOME_VALUES_FROM,;

        @Override
        public boolean isCE() {
            return true;
        }
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

