package ru.avicomp.ontapi.jena.impl.configuration;

import java.util.stream.Stream;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.enhanced.EnhNode;
import org.apache.jena.graph.Node;
import org.apache.jena.ontology.ConversionException;

import ru.avicomp.ontapi.jena.OntJenaException;

/**
 * Default implementation of {@link OntObjectFactory}.
 * This is a designer that consists of three modules:
 * - {@link OntMaker} for initialization and physical creation a node {@link EnhNode} in the graph {@link EnhGraph}.
 * - {@link OntFilter} to test the presence of a node in the graph
 * - {@link OntFinder} to search for nodes in the graph.
 * <p>
 * Created by szuev on 07.11.2016.
 */
public class CommonOntObjectFactory extends OntObjectFactory {
    private final OntMaker maker;
    private final OntFinder finder;
    private final OntFilter filter;

    public CommonOntObjectFactory(OntMaker maker, OntFinder finder, OntFilter primary, OntFilter... additional) {
        this.maker = OntJenaException.notNull(maker, "Null maker.");
        this.finder = OntJenaException.notNull(finder, "Null finder.");
        this.filter = OntJenaException.notNull(primary, "Null primary filter.").accumulate(additional);
    }

    public OntMaker getMaker() {
        return maker;
    }

    public OntFinder getFinder() {
        return finder;
    }

    public OntFilter getFilter() {
        return filter;
    }

    @Override
    public EnhNode wrap(Node node, EnhGraph eg) {
        if (!canWrap(node, eg))
            throw new ConversionException(String.format("Can't wrap node %s to %s", node, maker.getTargetView()));
        return maker.instance(node, eg);
    }

    @Override
    public boolean canWrap(Node node, EnhGraph eg) {
        return filter.test(node, eg);
    }

    @Override
    public EnhNode create(Node node, EnhGraph eg) {
        if (!canCreate(node, eg))
            throw new OntJenaException.Creation(String.format("Can't modify graph for %s (%s)", node, maker.getTargetView()));
        maker.make(node, eg);
        return maker.instance(node, eg);
    }

    @Override
    public boolean canCreate(Node node, EnhGraph eg) {
        return maker.getTester().test(node, eg);
    }

    @Override
    public Stream<EnhNode> find(EnhGraph eg) {
        return finder.restrict(filter).find(eg).map(n -> maker.instance(n, eg));
    }
}
