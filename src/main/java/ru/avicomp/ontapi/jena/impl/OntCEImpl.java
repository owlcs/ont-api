package ru.avicomp.ontapi.jena.impl;

import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDFS;

import ru.avicomp.ontapi.jena.model.OntCE;

/**
 * base class for any class-expression.
 * <p>
 * Created by szuev on 03.11.2016.
 */
abstract class OntCEImpl extends OntObjectImpl implements OntCE {
    protected final Property predicate;

    OntCEImpl(Resource resource, Property predicate) {
        super(resource);
        this.predicate = predicate;
    }

    @Override
    public Stream<OntCE> subClassOf() {
        return getModel().classExpressions(this, RDFS.subClassOf);
    }


    abstract static class ComponentsCEImpl extends OntCEImpl implements OntCE.Components<OntCE> {

        ComponentsCEImpl(Resource r, Property predicate) {
            super(r, predicate);
        }

        @Override
        public Stream<OntCE> components() {
            // ignore anything but Class Expressions
            return GraphModelImpl.asStream(getModel().listObjectsOfProperty(this, predicate)
                    .mapWith(n -> n.as(RDFList.class))
                    .mapWith(list -> getModel().asStream(list).filter(o -> getModel().isCE(o)).map(OntCE.class::cast)))
                    .flatMap(Function.identity()).distinct();
        }
    }

    static class UnionOfImpl extends ComponentsCEImpl implements OntCE.UnionOf {
        UnionOfImpl(Resource r) {
            super(r, OWL.unionOf);
        }
    }

    static class IntersectionOfImpl extends ComponentsCEImpl implements OntCE.IntersectionOf {
        IntersectionOfImpl(Resource r) {
            super(r, OWL.intersectionOf);
        }
    }

    static class OneOfImpl extends ComponentsCEImpl implements OntCE.OneOf {
        OneOfImpl(Resource r) {
            super(r, OWL.oneOf);
        }
    }

    enum Type {
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

    }
}
