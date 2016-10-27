package ru.avicomp.ontapi.jena;

import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.jena.enhanced.*;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.compose.DisjointUnion;
import org.apache.jena.graph.compose.MultiUnion;
import org.apache.jena.ontology.AnnotationProperty;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.impl.AnnotationPropertyImpl;
import org.apache.jena.ontology.impl.OntClassImpl;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.impl.ModelCom;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.util.iterator.UniqueFilter;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

/**
 * Created by @szuev on 27.10.2016.
 */
public class OWL2ModelImpl extends ModelCom {
    private final Graph base;

    /**
     * see {@link OntClassImpl#factory} and {@link org.apache.jena.ontology.impl.OWLProfile.SupportsCheck} as examples
     */
    public static final Personality<RDFNode> PERSONALITY = BuiltinPersonalities.model.copy().
            add(OntClass.class, impl(OntClassImpl::new)).
            add(AnnotationProperty.class, impl(AnnotationPropertyImpl::new));

    private interface FakeResourceFactory<T extends EnhNode> {
        T create(Node node, EnhGraph eg);
    }

    private static Implementation impl(FakeResourceFactory<? extends EnhNode> f) {
        return new FakeImplementation() {
            @Override
            public EnhNode wrap(Node node, EnhGraph eg) {
                return f.create(node, eg);
            }
        };
    }

    private static abstract class FakeImplementation extends Implementation {
        @Override
        public boolean canWrap(Node node, EnhGraph eg) {
            return true;
        }
    }

    public OWL2ModelImpl(Graph base) {
        super(base, PERSONALITY);
        this.base = base;
    }

    public void addImport(Graph g) {
        MultiUnion imports = graph instanceof DisjointUnion ? (MultiUnion) ((DisjointUnion) graph).getR() : new MultiUnion();
        imports.addGraph(g);
        graph = new DisjointUnion(base, imports);
    }

    public void removeImport(Graph g) {
        MultiUnion imports = graph instanceof DisjointUnion ? (MultiUnion) ((DisjointUnion) graph).getR() : new MultiUnion();
        imports.removeGraph(g);
        graph = imports.isEmpty() ? base : new DisjointUnion(graph, imports);
    }

    public Graph getBaseGraph() {
        return base;
    }

    /**
     * list named classes only.
     *
     * @return Stream of OntClass'es
     */
    public Stream<OntClass> listClasses() { //todo: not correct
        return byTypes(OWL.Class, RDFS.Class).filter(OWL2ModelImpl::isURI).filter(new UniqueFilter<>()).map(t -> getNodeAs(t.getSubject(), OntClass.class));
    }

    public Stream<AnnotationProperty> listAnnotationProperties() { // todo: not correct
        return byTypes(OWL.AnnotationProperty).filter(OWL2ModelImpl::isURI).filter(new UniqueFilter<>()).map(t -> getNodeAs(t.getSubject(), AnnotationProperty.class));
    }

    protected <T extends RDFNode> ExtendedIterator<T> findByTypeAs(Iterator<Resource> types, Class<T> asKey) {
        return findByTypeAs(types.next(), types, asKey);
    }

    protected <T extends RDFNode> ExtendedIterator<T> findByTypeAs(Resource type, Iterator<Resource> types, Class<T> asKey) {
        return findByType(type, types).mapWith(p -> getNodeAs(p.getSubject(), asKey));
    }

    protected ExtendedIterator<Triple> findByType(Resource type, Iterator<Resource> alternates) {
        ExtendedIterator<Triple> i = findByType(type);
        if (alternates != null) {
            while (alternates.hasNext()) {
                i = i.andThen(findByType(alternates.next()));
            }
        }
        return i.filterKeep(new UniqueFilter<>());
    }

    protected ExtendedIterator<Triple> findByType(Resource type) {
        return getGraph().find(null, RDF.type.asNode(), type.asNode());
    }

    protected Stream<Triple> byType(Resource type) {
        return asStream(findByType(type), false);
    }

    protected Stream<Triple> byTypes(Resource... types) {
        Stream<Triple> res = null;
        for (Resource t : types) {
            if (res == null) {
                res = byType(t);
            } else {
                res = Stream.concat(res, byType(t));
            }
        }
        return res;
    }

    public static <T> Stream<T> asStream(Iterator<T> sourceIterator, boolean parallel) {
        Iterable<T> iterable = () -> sourceIterator;
        return StreamSupport.stream(iterable.spliterator(), parallel);
    }

    public static boolean isURI(Triple triple) {
        return triple.getSubject().isURI();
    }

}
