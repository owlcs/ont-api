package ru.avicomp.ontapi.jena.impl;

import java.util.stream.Stream;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;

import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.impl.configuration.*;
import ru.avicomp.ontapi.jena.model.OntDR;
import ru.avicomp.ontapi.jena.model.OntDT;
import ru.avicomp.ontapi.jena.model.OntFR;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

/**
 * Implementation for Data Range Expressions.
 * <p>
 * Created by @szuev on 16.11.2016.
 */
public class OntDRImpl extends OntObjectImpl implements OntDR {
    private static final OntFinder DR_FINDER = new OntFinder.ByType(RDFS.Datatype);
    private static final OntFilter DR_FILTER = OntFilter.BLANK.and(new OntFilter.HasType(RDFS.Datatype));

    public static Configurable<OntObjectFactory> oneOfDRFactory = m ->
            new CommonOntObjectFactory(new OntMaker.Default(OneOfImpl.class), DR_FINDER, DR_FILTER.and(new OntFilter.HasPredicate(OWL.oneOf)));
    public static Configurable<OntObjectFactory> restrictionDRFactory = m ->
            new CommonOntObjectFactory(new OntMaker.Default(RestrictionImpl.class), DR_FINDER, DR_FILTER.and(new OntFilter.HasPredicate(OWL.onDatatype)).and(new OntFilter.HasPredicate(OWL.withRestrictions)));
    public static Configurable<OntObjectFactory> complementOfDRFactory = m ->
            new CommonOntObjectFactory(new OntMaker.Default(ComplementOfImpl.class), DR_FINDER, DR_FILTER.and(new OntFilter.HasPredicate(OWL.datatypeComplementOf)));
    public static Configurable<OntObjectFactory> unionOfDRFactory = m ->
            new CommonOntObjectFactory(new OntMaker.Default(UnionOfImpl.class), DR_FINDER, DR_FILTER.and(new OntFilter.HasPredicate(OWL.unionOf)));
    public static Configurable<OntObjectFactory> intersectionOfDRFactory = m ->
            new CommonOntObjectFactory(new OntMaker.Default(IntersectionOfImpl.class), DR_FINDER, DR_FILTER.and(new OntFilter.HasPredicate(OWL.intersectionOf)));

    public static Configurable<MultiOntObjectFactory> abstractAnonDRFactory = createMultiFactory(DR_FINDER,
            oneOfDRFactory, restrictionDRFactory, complementOfDRFactory, unionOfDRFactory, intersectionOfDRFactory);

    public static Configurable<MultiOntObjectFactory> abstractDRFactory = createMultiFactory(DR_FINDER, OntEntityImpl.datatypeFactory, abstractAnonDRFactory);

    public OntDRImpl(Node n, EnhGraph m) {
        super(n, m);
    }

    private static Resource create(OntGraphModelImpl model) {
        Resource res = model.createResource();
        model.add(res, RDF.type, RDFS.Datatype);
        return res;
    }

    public static OneOf createOneOf(OntGraphModelImpl model, Stream<Literal> values) {
        OntJenaException.notNull(values, "Null values stream.");
        Resource res = create(model);
        model.add(res, OWL.oneOf, model.createList(values.iterator()));
        return model.getNodeAs(res.asNode(), OneOf.class);
    }

    public static Restriction createRestriction(OntGraphModelImpl model, OntDR property, Stream<OntFR> values) {
        OntJenaException.notNull(property, "Null property.");
        OntJenaException.notNull(values, "Null values stream.");
        Resource res = create(model);
        model.add(res, OWL.onDatatype, property);
        model.add(res, OWL.withRestrictions, model.createList(values.iterator()));
        return model.getNodeAs(res.asNode(), Restriction.class);
    }

    public static ComplementOf createComplementOf(OntGraphModelImpl model, OntDR other) {
        OntJenaException.notNull(other, "Null data range.");
        Resource res = create(model);
        model.add(res, OWL.datatypeComplementOf, other);
        return model.getNodeAs(res.asNode(), ComplementOf.class);
    }

    public static UnionOf createUnionOf(OntGraphModelImpl model, Stream<OntDR> values) {
        OntJenaException.notNull(values, "Null values stream.");
        Resource res = create(model);
        model.add(res, OWL.unionOf, model.createList(values.iterator()));
        return model.getNodeAs(res.asNode(), UnionOf.class);
    }

    public static IntersectionOf createIntersectionOf(OntGraphModelImpl model, Stream<OntDR> values) {
        OntJenaException.notNull(values, "Null values stream.");
        Resource res = create(model);
        model.add(res, OWL.intersectionOf, model.createList(values.iterator()));
        return model.getNodeAs(res.asNode(), IntersectionOf.class);
    }

    public static class OneOfImpl extends OntDRImpl implements OneOf {
        public OneOfImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        public Stream<Literal> values() {
            return rdfListMembers(OWL.oneOf, Literal.class);
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
            return getRequiredObject(OWL.onDatatype, OntDT.class);
        }

        @Override
        public Stream<OntFR> facetRestrictions() {
            return rdfListMembers(OWL.withRestrictions, OntFR.class);
        }
    }

    public static class ComplementOfImpl extends OntDRImpl implements ComplementOf {
        public ComplementOfImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        public OntDR getDataRange() {
            return getRequiredObject(OWL.datatypeComplementOf, OntDR.class);
        }
    }

    public static class UnionOfImpl extends OntDRImpl implements UnionOf {
        public UnionOfImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        public Stream<OntDR> dataRanges() {
            return rdfListMembers(OWL.unionOf, OntDR.class);
        }
    }

    public static class IntersectionOfImpl extends OntDRImpl implements IntersectionOf {
        public IntersectionOfImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        public Stream<OntDR> dataRanges() {
            return rdfListMembers(OWL.intersectionOf, OntDR.class);
        }
    }
}
