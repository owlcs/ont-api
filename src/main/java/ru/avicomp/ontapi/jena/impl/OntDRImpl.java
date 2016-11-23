package ru.avicomp.ontapi.jena.impl;

import java.util.stream.Stream;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.jena.impl.configuration.*;
import ru.avicomp.ontapi.jena.model.OntDR;
import ru.avicomp.ontapi.jena.model.OntDT;
import ru.avicomp.ontapi.jena.model.OntFR;

/**
 * Implementation for Data Range Expressions.
 * <p>
 * Created by @szuev on 16.11.2016.
 */
public class OntDRImpl extends OntObjectImpl implements OntDR {
    private static final OntFinder DR_FINDER = new OntFinder.ByType(RDFS.Datatype);
    private static final OntFilter DR_FILTER = OntFilter.BLANK.and(new OntFilter.HasType(RDFS.Datatype));

    public static OntObjectFactory oneOfDRFactory = new CommonOntObjectFactory(new OntMaker.Default(OneOfImpl.class), DR_FINDER,
            DR_FILTER.and(new OntFilter.HasPredicate(OWL2.oneOf)));
    public static OntObjectFactory restrictionDRFactory = new CommonOntObjectFactory(new OntMaker.Default(RestrictionImpl.class), DR_FINDER,
            DR_FILTER.and(new OntFilter.HasPredicate(OWL2.onDatatype)).and(new OntFilter.HasPredicate(OWL2.withRestrictions)));
    public static OntObjectFactory complementOfDRFactory = new CommonOntObjectFactory(new OntMaker.Default(ComplementOfImpl.class), DR_FINDER,
            DR_FILTER.and(new OntFilter.HasPredicate(OWL2.datatypeComplementOf)));
    public static OntObjectFactory unionOfDRFactory = new CommonOntObjectFactory(new OntMaker.Default(UnionOfImpl.class), DR_FINDER,
            DR_FILTER.and(new OntFilter.HasPredicate(OWL2.unionOf)));
    public static OntObjectFactory intersectionOfDRFactory = new CommonOntObjectFactory(new OntMaker.Default(IntersectionOfImpl.class), DR_FINDER,
            DR_FILTER.and(new OntFilter.HasPredicate(OWL2.intersectionOf)));
    public static OntObjectFactory abstractDRFactory = new MultiOntObjectFactory(DR_FINDER,
            OntEntityImpl.datatypeFactory, oneOfDRFactory, restrictionDRFactory, complementOfDRFactory, unionOfDRFactory, intersectionOfDRFactory);

    public OntDRImpl(Node n, EnhGraph m) {
        super(n, m);
    }

    private static Resource create(OntGraphModelImpl model) {
        Resource res = model.createResource();
        model.add(res, RDF.type, RDFS.Datatype);
        return res;
    }

    public static OneOf createOneOf(OntGraphModelImpl model, Stream<Literal> values) {
        OntApiException.notNull(values, "Null values stream.");
        Resource res = create(model);
        model.add(res, OWL2.oneOf, model.createList(values.iterator()));
        return model.getNodeAs(res.asNode(), OneOf.class);
    }

    public static Restriction createRestriction(OntGraphModelImpl model, OntDR property, Stream<OntFR> values) {
        OntApiException.notNull(property, "Null property.");
        OntApiException.notNull(values, "Null values stream.");
        Resource res = create(model);
        model.add(res, OWL2.onDatatype, property);
        model.add(res, OWL2.withRestrictions, model.createList(values.iterator()));
        return model.getNodeAs(res.asNode(), Restriction.class);
    }

    public static ComplementOf createComplementOf(OntGraphModelImpl model, OntDR other) {
        OntApiException.notNull(other, "Null data range.");
        Resource res = create(model);
        model.add(res, OWL2.datatypeComplementOf, other);
        return model.getNodeAs(res.asNode(), ComplementOf.class);
    }

    public static UnionOf createUnionOf(OntGraphModelImpl model, Stream<OntDR> values) {
        OntApiException.notNull(values, "Null values stream.");
        Resource res = create(model);
        model.add(res, OWL2.unionOf, model.createList(values.iterator()));
        return model.getNodeAs(res.asNode(), UnionOf.class);
    }

    public static IntersectionOf createIntersectionOf(OntGraphModelImpl model, Stream<OntDR> values) {
        OntApiException.notNull(values, "Null values stream.");
        Resource res = create(model);
        model.add(res, OWL2.intersectionOf, model.createList(values.iterator()));
        return model.getNodeAs(res.asNode(), IntersectionOf.class);
    }

    public static class OneOfImpl extends OntDRImpl implements OneOf {
        public OneOfImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        public Stream<Literal> values() {
            return rdfList(OWL2.oneOf, Literal.class);
        }
    }

    public static class RestrictionImpl extends OntDRImpl implements Restriction {
        public RestrictionImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        public Class<Restriction> getActualClass() {
            return Restriction.class;
        }

        @Override
        public OntDT getDatatype() {
            return getRequiredOntProperty(OWL2.onDatatype, OntDT.class);
        }

        @Override
        public Stream<OntFR> facetRestrictions() {
            return rdfList(OWL2.withRestrictions, OntFR.class);
        }
    }

    public static class ComplementOfImpl extends OntDRImpl implements ComplementOf {
        public ComplementOfImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        public OntDR getDataRange() {
            return getRequiredOntProperty(OWL2.datatypeComplementOf, OntDR.class);
        }
    }

    public static class UnionOfImpl extends OntDRImpl implements UnionOf {
        public UnionOfImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        public Stream<OntDR> dataRanges() {
            return rdfList(OWL2.unionOf, OntDR.class);
        }
    }

    public static class IntersectionOfImpl extends OntDRImpl implements IntersectionOf {
        public IntersectionOfImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        public Stream<OntDR> dataRanges() {
            return rdfList(OWL2.intersectionOf, OntDR.class);
        }
    }
}
