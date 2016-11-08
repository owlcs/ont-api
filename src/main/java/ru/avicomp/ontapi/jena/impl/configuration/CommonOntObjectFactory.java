package ru.avicomp.ontapi.jena.impl.configuration;

import java.lang.reflect.InvocationTargetException;
import java.util.stream.Stream;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.enhanced.EnhNode;
import org.apache.jena.graph.Node;

import ru.avicomp.ontapi.OntException;
import ru.avicomp.ontapi.jena.impl.OntObjectImpl;

/**
 * Default implementation of {@link OntObjectFactory}
 * <p>
 * Created by szuev on 07.11.2016.
 */
public class CommonOntObjectFactory extends OntObjectFactory {
    private final Class<? extends OntObjectImpl> impl;
    private final OntMaker maker;
    private final OntFinder finder;
    private final OntFilter filter;

    public CommonOntObjectFactory(Class<? extends OntObjectImpl> impl, OntMaker maker, OntFinder finder, OntFilter primaryFilter, OntFilter... additionalFilters) {
        this.impl = OntException.notNull(impl, "Null implementation class.");
        this.maker = OntException.notNull(maker, "Null maker.");
        this.finder = OntException.notNull(finder, "Null finder.");
        this.filter = OntException.notNull(primaryFilter, "Null primary filter.").accumulate(additionalFilters);
    }

    protected EnhNode newInstance(Node node, EnhGraph eg) {
        try {
            return impl.getDeclaredConstructor(Node.class, EnhGraph.class).newInstance(node, eg);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new OntException(e);
        }
    }

    @Override
    public EnhNode wrap(Node node, EnhGraph eg) {
        if (!canWrap(node, eg))
            throw new OntException(String.format("Cannot convert node %s to %s", node, impl.getSimpleName()));
        return newInstance(node, eg);
    }

    @Override
    public boolean canWrap(Node node, EnhGraph eg) {
        return filter.test(node, eg);
    }

    @Override
    public EnhNode create(Node node, EnhGraph eg) {
        maker.prepare(node, eg);
        return newInstance(node, eg);
    }

    @Override
    public Stream<EnhNode> find(EnhGraph eg) {
        return finder.find(eg).filter(n -> canWrap(n, eg)).distinct().map(n -> newInstance(n, eg));
    }
}
