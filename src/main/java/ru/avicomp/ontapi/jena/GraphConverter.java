package ru.avicomp.ontapi.jena;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import ru.avicomp.ontapi.OntException;

/**
 * Class to perform some transformation action on the specified graph.
 * Currently it is to convert the OWL-1/RDFS ontological graph to the OWL-2-DL graph.
 * <p>
 * Created by szuev on 28.10.2016.
 */
public abstract class GraphConverter {
    public static final FactoryStore CONVERTERS = new FactoryStore().add(RDFStoOWL::new).add(OWL1toOWL2DL::new);

    protected Graph graph;

    public GraphConverter(Graph graph) {
        this.graph = graph;
    }

    public static void convert(Graph graph) {
        CONVERTERS.stream(graph).forEach(GraphConverter::perform);
    }

    public Graph getGraph() {
        return graph;
    }

    public abstract void perform();

    protected ExtendedIterator<Triple> findByType(Resource type) {
        return findByType(type.asNode());
    }

    protected ExtendedIterator<Triple> findByType(Node type) {
        return find(RDF.type, type);
    }

    protected ExtendedIterator<Triple> find(Resource predicate, Node object) {
        return find(Node.ANY, predicate, object);
    }

    protected ExtendedIterator<Triple> find(Node subject, Resource predicate, Node object) {
        return graph.find(subject, predicate.asNode(), object);
    }


    protected void deleteType(Resource typeToFind, Resource typeToRemove) {
        findByType(typeToFind).mapWith(t -> Triple.create(t.getSubject(), RDF.type.asNode(), typeToRemove.asNode()))
                .forEachRemaining(graph::delete);
    }

    protected void addType(Resource typeToFind, Resource typeToAdd) {
        findByType(typeToFind).mapWith(Triple::getSubject).forEachRemaining(node -> addType(node, typeToAdd));
    }

    protected void addType(Node subject, Resource type) {
        graph.add(Triple.create(subject, RDF.type.asNode(), type.asNode()));
    }

    protected void replaceType(Resource typeToFind, Resource typeToReplace) {
        findByType(typeToFind).toSet().forEach(t -> {
            graph.delete(t);
            graph.add(Triple.create(t.getSubject(), RDF.type.asNode(), typeToReplace.asNode()));
        });
    }

    @FunctionalInterface
    interface Factory<GC extends GraphConverter> {
        GC create(Graph graph);
    }

    public static class FactoryStore {
        private Set<Factory> set = new LinkedHashSet<>();

        public FactoryStore add(Factory f) {
            set.add(f);
            return this;
        }

        public FactoryStore remove(Factory f) {
            set.remove(f);
            return this;
        }

        public Stream<GraphConverter> stream(Graph graph) {
            return set.stream().map(f -> f.create(graph));
        }
    }

    /**
     * OWL 1 DL -> OWL 2 DL
     * see <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Parsing_of_the_Ontology_Header_and_Declarations'>Chapter 3, Table 5 and Table 6</a>
     */
    private static class OWL1toOWL2DL extends GraphConverter {
        OWL1toOWL2DL(Graph graph) {
            super(graph);
        }

        @Override
        public void perform() {
            replaceType(OWL.OntologyProperty, OWL.AnnotationProperty);
            Stream.of(OWL.DataRange, RDFS.Datatype, OWL.Restriction, OWL.Class).forEach(r -> deleteType(r, RDFS.Class));
            Stream.of(OWL.ObjectProperty, OWL.FunctionalProperty, OWL.InverseFunctionalProperty, OWL.TransitiveProperty,
                    OWL.DatatypeProperty, OWL.AnnotationProperty).forEach(r -> deleteType(r, RDF.Property));
            Stream.of(OWL.InverseFunctionalProperty, OWL.TransitiveProperty, OWL.SymmetricProperty).forEach(r -> addType(r, OWL.ObjectProperty));
        }
    }

    /**
     * see <a href='https://www.w3.org/TR/rdf-schema'>RDFS specification</a>
     */
    private static class RDFStoOWL extends GraphConverter {
        private static final Set<Node> DATATYPES = JenaUtils.BUILT_IN_DATATYPES.stream().map(t -> NodeFactory.createURI(t.getURI())).collect(Collectors.toSet());
        private static final Set<Node> NOT_INDIVIDUAL_TYPES = Stream.of(RDFS.Class, RDFS.Datatype, RDF.Property, OWL.DataRange, OWL.Ontology, OWL2.NamedIndividual).
                map(Resource::asNode).collect(Collectors.toSet());

        RDFStoOWL(Graph graph) {
            super(graph);
        }

        @Override
        public void perform() {
            processClasses();
            processProperties();
            processIndividuals();
        }

        private void processClasses() {
            findByType(RDFS.Class).mapWith(Triple::getSubject).filterDrop(this::isDatatype).
                    forEachRemaining(n -> addType(n, OWL.Class));
            // could such OWL-structures be here (inside RDFS Ontology)?
            findByType(OWL.intersectionOf).andThen(findByType(OWL.oneOf)).andThen(findByType(OWL.unionOf)).andThen(findByType(OWL.complementOf)).
                    mapWith(Triple::getObject).filterDrop(this::isDatatype).forEachRemaining(n -> addType(n, OWL.Class));
        }

        private void processProperties() {
            Set<Triple> properties = findByType(RDF.Property).toSet();
            for (Triple triple : properties) {
                Node prop = triple.getSubject();
                Set<Resource> types = getPropertyTypes(prop);
                if (types.isEmpty()) throw new OntException("Can't determine property type for " + triple);
                if (types.contains(OWL.DatatypeProperty) && types.contains(OWL.ObjectProperty)) {
                    throw new OntException("Property " + triple + " can't be data and object at the same time.");
                }
                types.forEach(type -> addType(prop, type));
            }
        }

        private void processIndividuals() {
            Set<Node> individuals = find(Node.ANY, RDF.type, Node.ANY).mapWith(Triple::getSubject).
                    filterKeep(Node::isURI).filterKeep(this::isIndividual).toSet();
            individuals.forEach(node -> addType(node, OWL2.NamedIndividual));
        }

        private Set<Resource> getPropertyTypes(Node subject) {
            Set<Resource> res = new HashSet<>();
            Set<Node> ranges = find(subject, RDFS.range, Node.ANY).mapWith(Triple::getObject).toSet();
            Set<Node> domains = find(subject, RDFS.domain, Node.ANY).mapWith(Triple::getObject).toSet();
            Set<Node> superProperties = find(subject, RDFS.subPropertyOf, Node.ANY).mapWith(Triple::getObject).toSet();
            if (ranges.isEmpty() && domains.isEmpty() && superProperties.isEmpty()) {
                res.add(OWL.AnnotationProperty);
                return res;
            }
            for (Node r : ranges) {
                res.add(isDatatypeRange(r) ? OWL.DatatypeProperty : OWL.ObjectProperty);
            }
            for (Node s : superProperties) {
                res.addAll(getPropertyTypes(s));
            }
            return res;
        }

        private boolean isIndividual(Node node) {
            //todo: only local
            Set<Node> types = find(node, RDF.type, Node.ANY).mapWith(Triple::getObject).toSet();
            for (Node t : types) {
                if (NOT_INDIVIDUAL_TYPES.contains(t)) return false;
            }
            return true;
        }

        private boolean isDatatypeRange(Node range) {
            return DATATYPES.contains(range) ||
                    graph.contains(range, RDF.type.asNode(), RDFS.Datatype.asNode()) || graph.contains(range, RDF.type.asNode(), OWL.DataRange.asNode());
        }

        private boolean isDatatype(Node subject) {
            return graph.contains(subject, RDF.type.asNode(), RDFS.Datatype.asNode());
        }
    }

}
