package ru.avicomp.ontapi.jena.impl.configuration;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.enhanced.EnhNode;
import org.apache.jena.graph.Node;

import ru.avicomp.ontapi.OntException;

/**
 * Factory to combine several factories.
 * <p>
 * Created by szuev on 07.11.2016.
 */
public class MultiOntObjectFactory extends OntObjectFactory {
    private final OntObjectFactory[] factories;

    public MultiOntObjectFactory(OntObjectFactory... factories) {
        this.factories = factories;
    }

    @Override
    public EnhNode wrap(Node node, EnhGraph eg) {
        for (OntObjectFactory f : factories) {
            if (f.canWrap(node, eg)) return f.wrap(node, eg);
        }
        throw new OntException("Can't wrap node " + node + ". Use direct factory");
    }

    @Override
    public boolean canWrap(Node node, EnhGraph eg) {
        for (OntObjectFactory f : factories) {
            if (f.canWrap(node, eg)) return true;
        }
        return false;
    }

    @Override
    public Stream<EnhNode> find(EnhGraph eg) {
        return Arrays.stream(factories).map(f -> f.find(eg)).flatMap(Function.identity()).distinct();
    }

    public OntObjectFactory[] getFactories() {
        return factories;
    }
}
