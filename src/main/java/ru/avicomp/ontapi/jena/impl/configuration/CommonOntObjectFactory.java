package ru.avicomp.ontapi.jena.impl.configuration;

import java.util.stream.Stream;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.enhanced.EnhNode;
import org.apache.jena.graph.Node;
import org.apache.jena.ontology.ConversionException;

import ru.avicomp.ontapi.OntApiException;

/**
 * Default implementation of {@link OntObjectFactory}
 * <p>
 * Created by szuev on 07.11.2016.
 */
public class CommonOntObjectFactory extends OntObjectFactory {
    private final OntMaker maker;
    private final OntFinder finder;
    private final OntFilter filter;

    public CommonOntObjectFactory(OntMaker maker, OntFinder finder, OntFilter primary, OntFilter... additional) {
        this.maker = OntApiException.notNull(maker, "Null maker.");
        this.finder = OntApiException.notNull(finder, "Null finder.");
        this.filter = OntApiException.notNull(primary, "Null primary filter.").accumulate(additional);
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
            throw new ConversionException(String.format("Cannot convert node %s to %s", node, maker.getInstanceClass().getSimpleName()));
        return maker.instance(node, eg);
    }

    @Override
    public boolean canWrap(Node node, EnhGraph eg) {
        return filter.test(node, eg);
    }

    @Override
    public EnhNode create(Node node, EnhGraph eg) {
        maker.make(node, eg);
        return maker.instance(node, eg);
    }

    @Override
    public Stream<EnhNode> find(EnhGraph eg) {
        return finder.find(eg).filter(n -> filter.test(n, eg)).distinct().map(n -> maker.instance(n, eg));
    }
}
