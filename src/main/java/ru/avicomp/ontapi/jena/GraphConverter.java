package ru.avicomp.ontapi.jena;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.log4j.Logger;

/**
 * Class to perform some transformation action on the specified graph.
 * Currently it is to convert the OWL-1/RDFS ontological graph to the OWL-2-DL graph.
 * Use it after loading inside {@link ru.avicomp.ontapi.OntologyFactoryImpl}
 * <p>
 * Created by szuev on 28.10.2016.
 */
public abstract class GraphConverter {
    private static final Logger LOGGER = Logger.getLogger(GraphConverter.class);
    public static final FactoryStore CONVERTERS = new FactoryStore().add(RDFStoOWL::new).add(OWL1toOWL2DL::new);

    public static Graph convert(Graph graph) {
        CONVERTERS.stream(graph).forEach(TransformAction::process);
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

    /**
     * The base class for any graph-convert-transformation
     */
    public static abstract class TransformAction {
        static final Node RDF_TYPE = RDF.type.asNode();
        protected Graph graph;

        protected TransformAction(Graph graph) {
            this.graph = graph;
        }

        /**
         * performs the graph transformation.
         */
        public abstract void perform();

        /**
         * decides is the transformation needed or not.
         *
         * @return true to process, false to skip
         */
        public boolean test() {
            return true;
        }

        public void process() {
            if (test()) {
                if (LOGGER.isDebugEnabled()) LOGGER.debug("Process " + getClass().getSimpleName());
                perform();
            }
        }

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

        protected boolean containsType(Resource type) {
            return getBaseGraph().contains(Node.ANY, RDF_TYPE, type.asNode());
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
            findByType(OWL2.OntologyProperty).forEachRemaining(node -> addType(node, OWL2.AnnotationProperty));
            findByType(OWL2.AnnotationProperty).forEachRemaining(node -> deleteType(node, OWL2.OntologyProperty));
            Stream.of(OWL2.DataRange, RDFS.Datatype, OWL2.Restriction, OWL2.Class).
                    forEach(type -> findByType(type).forEachRemaining(node -> deleteType(node, RDFS.Class)));
            Stream.of(OWL2.ObjectProperty, OWL2.FunctionalProperty, OWL2.InverseFunctionalProperty, OWL2.TransitiveProperty, OWL2.DatatypeProperty, OWL2.AnnotationProperty).
                    forEach(type -> findByType(type).forEachRemaining(node -> deleteType(node, RDF.Property)));
            Stream.of(OWL2.InverseFunctionalProperty, OWL2.TransitiveProperty, OWL2.SymmetricProperty).
                    forEach(type -> findByType(type).forEachRemaining(node -> addType(node, OWL2.ObjectProperty)));
        }

        @Override
        public boolean test() {
            return containsType(RDFS.Class) || containsType(RDF.Property) || containsType(OWL2.OntologyProperty);
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
        private static final Set<Node> DATATYPES = JenaUtils.BUILT_IN_DATATYPES.stream().
                map(t -> NodeFactory.createURI(t.getURI())).collect(Collectors.toSet());
        private static final Set<Node> BUILT_IN = Stream.concat(JenaUtils.BUILT_IN_PROPERTIES.stream(), JenaUtils.BUILT_IN_RESOURCES.stream()).map(Function.identity()).
                map(t -> NodeFactory.createURI(t.getURI())).collect(Collectors.toSet());
        private static final Set<Node> NOT_INDIVIDUAL_TYPES = Stream.of(RDFS.Class, OWL2.Class,
                RDFS.Datatype, OWL2.DataRange,
                RDF.Property, OWL2.DatatypeProperty, OWL2.AnnotationProperty, OWL2.ObjectProperty,
                OWL2.Ontology, OWL2.NamedIndividual).
                map(Resource::asNode).collect(Collectors.toSet());

        private RDFStoOWL(Graph graph) {
            super(graph);
        }

        @Override
        public void perform() {
            // the order is important
            fixProperties();
            fixClasses();
            fixIndividuals();
        }

        @Override
        public boolean test() {
            return !containsType(OWL2.Class) && !containsType(OWL2.AnnotationProperty) && !containsType(OWL2.DatatypeProperty) && !containsType(OWL2.AnnotationProperty) && !containsType(OWL2.NamedIndividual);
        }

        private Set<Resource> getPropertyTypes(Node subject) {
            Set<Resource> res = new TreeSet<>(JenaUtils.RDF_NODE_COMPARATOR);
            Set<Node> ranges = getGraph().find(subject, RDFS.range.asNode(), Node.ANY).mapWith(Triple::getObject).toSet();
            Set<Node> domains = getGraph().find(subject, RDFS.domain.asNode(), Node.ANY).mapWith(Triple::getObject).toSet();
            Set<Node> superProperties = getGraph().find(subject, RDFS.subPropertyOf.asNode(), Node.ANY).mapWith(Triple::getObject).toSet();
            if (ranges.isEmpty() && domains.isEmpty() && superProperties.isEmpty()) {
                res.add(OWL2.AnnotationProperty);
                return res;
            }
            for (Node r : ranges) {
                res.add(isDatatypeRange(r) ? OWL2.DatatypeProperty : OWL2.ObjectProperty);
            }
            for (Node s : superProperties) {
                res.addAll(getPropertyTypes(s));
            }
            return res;
        }

        private boolean isIndividual(Node uri) {
            // everything that are not Class(owl:Class), Property (owl:AnnotationProperty, owl:DataProperty and owl:ObjectProperty), datatype(rdfs:Datatype) and not owl:Ontology
            Set<Node> types = getGraph().find(uri, RDF_TYPE, Node.ANY).mapWith(Triple::getObject).toSet();
            for (Node type : types) {
                //if (getGraph().contains(type, RDF_TYPE, OWL2.Class.asNode())) return true;
                if (NOT_INDIVIDUAL_TYPES.contains(type)) return false;
            }
            return true;
        }

        private boolean isDatatypeRange(Node range) {
            return DATATYPES.contains(range) ||
                    graph.contains(range, RDF_TYPE, RDFS.Datatype.asNode()) || graph.contains(range, RDF_TYPE, OWL2.DataRange.asNode());
        }

        private boolean isDatatype(Node subject) {
            return graph.contains(subject, RDF_TYPE, RDFS.Datatype.asNode());
        }

        private ExtendedIterator<Triple> findByPredicate(Property predicate) {
            return getBaseGraph().find(Node.ANY, predicate.asNode(), Node.ANY);
        }

        private ExtendedIterator<Triple> findClassExpressions() {
            return findByPredicate(OWL2.intersectionOf).andThen(findByPredicate(OWL2.oneOf)).andThen(findByPredicate(OWL2.unionOf)).andThen(findByPredicate(OWL2.complementOf));
        }

        private void fixClasses() {
            Set<Node> declarations = getBaseGraph().find(Node.ANY, RDF_TYPE, RDFS.Class.asNode()).andThen(findByPredicate(RDFS.subClassOf)).mapWith(Triple::getSubject).toSet();
            Set<Node> classes = new HashSet<>(declarations);
            // consider objects from @Class,rdfs:subClassOf,@Any and @Class,rdf:type,@Any triplets as owl:Class also:
            for (Node subject : declarations) {
                Stream.of(RDF_TYPE, RDFS.subClassOf.asNode()).forEach(predicate -> classes.addAll(getBaseGraph().find(subject, predicate, Node.ANY).mapWith(Triple::getObject).toSet()));
            }
            // left side of ObjectProperties is individual; anonymous individuals should be assigned to class
            Set<Node> anonIndividuals = new HashSet<>();
            getGraph().find(Node.ANY, RDF_TYPE, OWL2.ObjectProperty.asNode()).mapWith(Triple::getSubject).
                    forEachRemaining(objectProperty -> anonIndividuals.addAll(getBaseGraph().
                            find(Node.ANY, objectProperty, Node.ANY).mapWith(Triple::getObject).filterKeep(Node::isBlank).toSet()));
            for (Node i : anonIndividuals) {
                classes.addAll(getBaseGraph().find(i, RDF_TYPE, Node.ANY).mapWith(Triple::getObject).toSet());
            }
            classes.removeAll(BUILT_IN);
            // not datatype and not system class:
            classes.stream().filter(node -> !isDatatype(node)).forEach(node -> addType(node, OWL2.Class));
            // could such OWL-structures be here (inside RDFS Ontology)?
            findClassExpressions().mapWith(Triple::getObject).filterDrop(RDFStoOWL.this::isDatatype).forEachRemaining(n -> addType(n, OWL2.Class));
        }

        private void fixProperties() {
            Set<Node> properties = getBaseGraph().find(Node.ANY, RDF_TYPE, RDF.Property.asNode()).mapWith(Triple::getSubject).toSet();
            // any standalone none-built-in predicates should be treated as rdf:Property also
            Set<Node> standalone = getBaseGraph().find(Node.ANY, Node.ANY, Node.ANY).
                    mapWith(Triple::getPredicate).filterDrop(node -> getGraph().contains(node, Node.ANY, Node.ANY)).toSet();
            properties.addAll(standalone);
            // all rdfs:subPropertyOf
            Set<Node> superProperties = getBaseGraph().find(Node.ANY, RDFS.subPropertyOf.asNode(), Node.ANY).mapWith(Triple::getSubject).toSet();
            properties.addAll(superProperties);
            properties.removeAll(BUILT_IN);
            for (Node prop : properties) {
                Set<Resource> types = getPropertyTypes(prop);
                if (types.isEmpty()) throw new OntJenaException("Can't determine property type for " + prop);
                if (types.contains(OWL2.DatatypeProperty) && types.contains(OWL2.ObjectProperty)) {
                    throw new OntJenaException("Property " + prop + " can't be data and object at the same time.");
                }
                types.forEach(type -> addType(prop, type));
            }
        }

        private void fixIndividuals() {
            Set<Node> individuals = getBaseGraph().find(Node.ANY, RDF_TYPE, Node.ANY).mapWith(Triple::getSubject).
                    filterKeep(Node::isURI).filterKeep(RDFStoOWL.this::isIndividual).toSet();
            individuals.forEach(node -> addType(node, OWL2.NamedIndividual));
        }
    }

}
