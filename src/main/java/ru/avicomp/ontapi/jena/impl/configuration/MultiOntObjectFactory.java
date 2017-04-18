package ru.avicomp.ontapi.jena.impl.configuration;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.enhanced.EnhNode;
import org.apache.jena.graph.Node;
import org.apache.jena.ontology.ConversionException;

/**
 * Factory to combine several factories.
 * <p>
 * Created by szuev on 07.11.2016.
 */
public class MultiOntObjectFactory extends OntObjectFactory {
    private final List<OntObjectFactory> factories;
    private OntFinder finder;
    private OntFilter fittingFilter;

    protected MultiOntObjectFactory(OntObjectFactory... factories) {
        this(null, null, factories);
    }

    private MultiOntObjectFactory(OntFinder finder, Stream<OntObjectFactory> factories) {
        this(finder, null, factories.toArray(OntObjectFactory[]::new));
    }

    /**
     * The main constructor
     *
     * @param finder        {@link OntFinder}, optional. if null then uses only array of sub-factories to search
     * @param fittingFilter {@link OntFilter}, optional. to trim searching
     * @param factories     the Array of factories, not null, not empty.
     */
    public MultiOntObjectFactory(OntFinder finder, OntFilter fittingFilter, OntObjectFactory... factories) {
        this.finder = finder;
        this.fittingFilter = fittingFilter;
        this.factories = unbend(factories);
    }



    private static List<OntObjectFactory> unbend(OntObjectFactory... factories) {
        return Arrays.stream(factories)
                .map(f -> MultiOntObjectFactory.class.isInstance(f) ? ((MultiOntObjectFactory) f).factories() : Stream.of(f))
                .flatMap(Function.identity()).collect(Collectors.toList());
    }

    @Override
    public EnhNode wrap(Node node, EnhGraph eg) {
        EnhNode res = doWrap(node, eg);
        if (res != null) return res;
        throw new ConversionException("Can't wrap node " + node + ". Use direct factory.");
    }

    @Override
    public boolean canWrap(Node node, EnhGraph eg) {
        return !(fittingFilter != null && !fittingFilter.test(node, eg)) && factories().anyMatch(f -> f.canWrap(node, eg));
    }

    @Override
    protected EnhNode doWrap(Node node, EnhGraph eg) {
        if (fittingFilter != null && !fittingFilter.test(node, eg)) return null;
        return factories().filter(f -> f.canWrap(node, eg)).map(f -> f.doWrap(node, eg)).findFirst().orElse(null);
    }

    @Override
    public Stream<EnhNode> find(EnhGraph eg) {
        if (finder != null) {
            return finder.find(eg).map(n -> doWrap(n, eg)).filter(Objects::nonNull);
        }
        return factories().map(f -> f.find(eg)).flatMap(Function.identity()).distinct();
    }

    public OntFinder getFinder() {
        return finder;
    }

    public OntFilter getFilter() {
        return fittingFilter;
    }

    public Stream<? extends OntObjectFactory> factories() {
        return factories.stream();
    }

    public MultiOntObjectFactory concat(OntObjectFactory... factories) {
        return new MultiOntObjectFactory(finder, Stream.concat(factories(), unbend(factories).stream()));
    }
}
