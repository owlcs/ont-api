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
import org.apache.jena.rdf.model.Property;
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

    public static Graph convert(Graph graph) {
        CONVERTERS.stream(graph).forEach(TransformAction::perform);
        return graph;
    }

    @FunctionalInterface
    public interface Factory<GC extends TransformAction> {
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

        public Stream<TransformAction> stream(Graph graph) {
            return set.stream().map(factory -> factory.create(graph));
        }
    }

    public static abstract class TransformAction {
        public static final Node RDF_TYPE = RDF.type.asNode();
        protected Graph graph;

        protected TransformAction(Graph graph) {
            this.graph = graph;
        }

        public abstract void perform();

        public Graph getGraph() {
            return graph;
        }

        public Graph getBaseGraph() {
            return graph instanceof UnionGraph ? ((UnionGraph) graph).getBaseGraph() : graph;
        }

        protected void addType(Node subject, Resource type) {
            getGraph().add(Triple.create(subject, RDF_TYPE, type.asNode()));
        }

        protected void deleteType(Node subject, Resource type) {
            getGraph().delete(Triple.create(subject, RDF_TYPE, type.asNode()));
        }
    }

    /**
     * OWL 1 DL -> OWL 2 DL
     * see <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Parsing_of_the_Ontology_Header_and_Declarations'>Chapter 3, Table 5 and Table 6</a>
     */
    private static class OWL1toOWL2DL extends TransformAction {
        private OWL1toOWL2DL(Graph graph) {
            super(graph);
        }

        @Override
        public void perform() {
            findByType(OWL.OntologyProperty).forEachRemaining(node -> addType(node, OWL.AnnotationProperty));
            findByType(OWL.AnnotationProperty).forEachRemaining(node -> deleteType(node, OWL.OntologyProperty));
            Stream.of(OWL.DataRange, RDFS.Datatype, OWL.Restriction, OWL.Class).
                    forEach(type -> findByType(type).forEachRemaining(node -> deleteType(node, RDFS.Class)));
            Stream.of(OWL.ObjectProperty, OWL.FunctionalProperty, OWL.InverseFunctionalProperty, OWL.TransitiveProperty, OWL.DatatypeProperty, OWL.AnnotationProperty).
                    forEach(type -> findByType(type).forEachRemaining(node -> deleteType(node, RDF.Property)));
            Stream.of(OWL.InverseFunctionalProperty, OWL.TransitiveProperty, OWL.SymmetricProperty).
                    forEach(type -> findByType(type).forEachRemaining(node -> addType(node, OWL.ObjectProperty)));
        }

        private ExtendedIterator<Node> findByType(Resource type) {
            return getBaseGraph().find(Node.ANY, RDF_TYPE, type.asNode()).mapWith(Triple::getSubject);
        }
    }

    /**
     * Class to convert the RDFS ontological graph to the OWL2 ontological graph.
     * <p>
     * see <a href='https://www.w3.org/TR/rdf-schema'>RDFS specification</a>
     */
    private static class RDFStoOWL extends TransformAction {
        private static final Set<Node> DATATYPES = JenaUtils.BUILT_IN_DATATYPES.stream().map(t -> NodeFactory.createURI(t.getURI())).collect(Collectors.toSet());
        private static final Set<Node> NOT_INDIVIDUAL_TYPES = Stream.of(RDFS.Class, OWL.Class, RDFS.Datatype, OWL.DataRange, RDF.Property, OWL.DatatypeProperty,
                OWL.AnnotationProperty, OWL.ObjectProperty, OWL.Ontology, OWL2.NamedIndividual).
                map(Resource::asNode).collect(Collectors.toSet());
        private static final Set<Node> SYSTEM_CLASSES = Stream.of(OWL.Nothing, OWL.Thing,
                RDFS.Class, OWL.Class, RDF.List, RDFS.Resource, RDF.Property, OWL.DeprecatedClass, OWL.Ontology).
                map(Resource::asNode).collect(Collectors.toSet());

        private ClassConverter classConverter;
        private PropertyConverter propertyConverter;
        private IndividualConverter individualConverter;

        private RDFStoOWL(Graph graph) {
            super(graph);
            classConverter = new ClassConverter(graph);
            propertyConverter = new PropertyConverter(graph);
            individualConverter = new IndividualConverter(graph);
        }

        @Override
        public void perform() {
            propertyConverter.perform();
            classConverter.perform(); // todo: class-converter is dependent on property-converter
            individualConverter.perform();
        }

        private Set<Resource> getPropertyTypes(Node subject) {
            Set<Resource> res = new HashSet<>();
            Set<Node> ranges = getGraph().find(subject, RDFS.range.asNode(), Node.ANY).mapWith(Triple::getObject).toSet();
            Set<Node> domains = getGraph().find(subject, RDFS.domain.asNode(), Node.ANY).mapWith(Triple::getObject).toSet();
            Set<Node> superProperties = getGraph().find(subject, RDFS.subPropertyOf.asNode(), Node.ANY).mapWith(Triple::getObject).toSet();
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
            Set<Node> types = getGraph().find(node, RDF.type.asNode(), Node.ANY).mapWith(Triple::getObject).toSet();
            for (Node t : types) {
                if (NOT_INDIVIDUAL_TYPES.contains(t)) return false;
            }
            return true;
        }

        private boolean isDatatypeRange(Node range) {
            return DATATYPES.contains(range) ||
                    graph.contains(range, RDF_TYPE, RDFS.Datatype.asNode()) || graph.contains(range, RDF_TYPE, OWL.DataRange.asNode());
        }

        private boolean isDatatype(Node subject) {
            return graph.contains(subject, RDF_TYPE, RDFS.Datatype.asNode());
        }

        private class ClassConverter extends TransformAction {
            ClassConverter(Graph graph) {
                super(graph);
            }

            @Override
            public void perform() {
                Set<Node> declarations = getBaseGraph().find(Node.ANY, RDF_TYPE, RDFS.Class.asNode()).andThen(findByPredicate(RDFS.subClassOf)).mapWith(Triple::getSubject).toSet();
                Set<Node> classes = new HashSet<>(declarations);
                // consider objects from @Class,rdfs:subClassOf,@Any and @Class,rdf:type,@Any triplets as owl:Classes also:
                for (Node subject : declarations) {
                    Stream.of(RDF_TYPE, RDFS.subClassOf.asNode()).forEach(predicate -> classes.addAll(getBaseGraph().find(subject, predicate, Node.ANY).mapWith(Triple::getObject).toSet()));
                }
                // left side of ObjectProperties is individual, anonymous individuals should be assigned to class
                Set<Node> anonIndividuals = new HashSet<>();
                getGraph().find(Node.ANY, RDF_TYPE, OWL.ObjectProperty.asNode()).mapWith(Triple::getSubject).
                        forEachRemaining(objectProperty -> anonIndividuals.addAll(getBaseGraph().
                                find(Node.ANY, objectProperty, Node.ANY).mapWith(Triple::getObject).filterKeep(Node::isBlank).toSet()));
                for (Node i : anonIndividuals) {
                    classes.addAll(getBaseGraph().find(i, RDF_TYPE, Node.ANY).mapWith(Triple::getObject).toSet());
                }
                // not datatype and not system class:
                classes.stream().filter(node -> !isDatatype(node)).filter(node -> !SYSTEM_CLASSES.contains(node)).forEach(node -> addType(node, OWL.Class));
                // could such OWL-structures be here (inside RDFS Ontology)?
                findClassExpressions().mapWith(Triple::getObject).filterDrop(RDFStoOWL.this::isDatatype).forEachRemaining(n -> addType(n, OWL.Class));
            }

            private ExtendedIterator<Triple> findByPredicate(Property predicate) {
                return getBaseGraph().find(Node.ANY, predicate.asNode(), Node.ANY);
            }

            private ExtendedIterator<Triple> findClassExpressions() {
                return findByPredicate(OWL.intersectionOf).andThen(findByPredicate(OWL.oneOf)).andThen(findByPredicate(OWL.unionOf)).andThen(findByPredicate(OWL.complementOf));
            }

        }

        private class PropertyConverter extends TransformAction {
            PropertyConverter(Graph graph) {
                super(graph);
            }

            @Override
            public void perform() {
                Set<Triple> properties = getBaseGraph().find(Node.ANY, RDF_TYPE, RDF.Property.asNode()).toSet();
                for (Triple triple : properties) {
                    Node prop = triple.getSubject();
                    Set<Resource> types = getPropertyTypes(prop);
                    if (types.isEmpty()) throw new OntException("Can't determine property type for " + triple);
                    if (types.contains(OWL.DatatypeProperty) && types.contains(OWL.ObjectProperty)) {
                        throw new OntException("Property " + triple + " can't be data and object at the same time.");
                    }
                    types.stream().sorted(JenaUtils.RDF_NODE_COMPARATOR).forEach(type -> addType(prop, type));
                }
            }
        }

        private class IndividualConverter extends TransformAction {
            IndividualConverter(Graph graph) {
                super(graph);
            }

            @Override
            public void perform() {
                Set<Node> individuals = getBaseGraph().find(Node.ANY, RDF_TYPE, Node.ANY).mapWith(Triple::getSubject).
                        filterKeep(Node::isURI).filterKeep(RDFStoOWL.this::isIndividual).toSet();
                individuals.forEach(node -> addType(node, OWL2.NamedIndividual));
            }
        }
    }

}
