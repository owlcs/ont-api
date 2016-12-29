package ru.avicomp.ontapi.jena;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.graph.FrontsNode;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.*;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDFS;
import org.apache.log4j.Logger;

import ru.avicomp.ontapi.OntBuildingFactoryImpl;
import ru.avicomp.ontapi.jena.utils.BuiltIn;
import ru.avicomp.ontapi.jena.utils.Models;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

/**
 * Class to perform some transformation action on the specified graph.
 * Currently it is to convert the OWL-1/RDFS ontological graph to the OWL-2-DL graph.
 * Use it after loading inside {@link OntBuildingFactoryImpl}
 * <p>
 * Created by szuev on 28.10.2016.
 */
public abstract class GraphConverter {
    private static final Logger LOGGER = Logger.getLogger(GraphConverter.class);
    public static final FactoryStore CONVERTERS = new FactoryStore()
            .add(RDFStoOWL::new)
            .add(OWLtoOWL2DL::new)
            .add(ObjectReferenceFixer::new)
            .add(OwlClassFixer::new)
            .add(NamedIndividualFixer::new)
            .add(PropertyTypeFixer::new);

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
        static final Set<Node> BUILT_IN = BuiltIn.ALL.stream().map(FrontsNode::asNode).collect(Collectors.toSet());

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
            addType(subject, type.asNode());
        }

        protected void addType(Node subject, Node type) {
            getGraph().add(Triple.create(subject, RDF_TYPE, type));
        }

        protected void deleteType(Node subject, Resource type) {
            getGraph().delete(Triple.create(subject, RDF_TYPE, type.asNode()));
        }

        protected boolean containsType(Resource type) {
            return getBaseGraph().contains(Node.ANY, RDF_TYPE, type.asNode());
        }

        protected Set<Node> getTypes(Node subject) {
            return getGraph().find(subject, RDF_TYPE, Node.ANY).mapWith(Triple::getObject).toSet();
        }
    }

    /**
     * OWL 1 DL -> OWL 2 DL
     * see <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Parsing_of_the_Ontology_Header_and_Declarations'>Chapter 3, Table 5 and Table 6</a>
     */
    private static class OWLtoOWL2DL extends TransformAction {
        private OWLtoOWL2DL(Graph graph) {
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

        @Override
        public boolean test() {
            return containsType(RDFS.Class) || containsType(RDF.Property) || containsType(OWL.OntologyProperty);
        }

        private ExtendedIterator<Node> findByType(Resource type) {
            return getBaseGraph().find(Node.ANY, RDF_TYPE, type.asNode()).mapWith(Triple::getSubject);
        }
    }

    /**
     * TODO: rewrite
     * Class to convert the RDFS ontological graph to the OWL2 ontological graph.
     * <p>
     * see <a href='https://www.w3.org/TR/rdf-schema'>RDFS specification</a>
     */
    private static class RDFStoOWL extends TransformAction {
        private static final Set<Node> DATATYPES = BuiltIn.DATATYPES.stream().map(FrontsNode::asNode).collect(Collectors.toSet());
        private static final Set<Node> NOT_INDIVIDUAL_TYPES = Stream.of(RDFS.Class, OWL.Class,
                RDFS.Datatype, OWL.DataRange,
                RDF.Property, OWL.DatatypeProperty, OWL.AnnotationProperty, OWL.ObjectProperty,
                OWL.Ontology, OWL.NamedIndividual).map(Resource::asNode).collect(Collectors.toSet());

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
            return !containsType(OWL.Class) && !containsType(OWL.AnnotationProperty) && !containsType(OWL.DatatypeProperty) && !containsType(OWL.AnnotationProperty) && !containsType(OWL.NamedIndividual);
        }

        private Set<Resource> getPropertyTypes(Node subject) {
            Set<Resource> res = new TreeSet<>(Models.RDF_NODE_COMPARATOR);
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
                    graph.contains(range, RDF_TYPE, RDFS.Datatype.asNode()) || graph.contains(range, RDF_TYPE, OWL.DataRange.asNode());
        }

        private boolean isDatatype(Node subject) {
            return graph.contains(subject, RDF_TYPE, RDFS.Datatype.asNode());
        }

        private ExtendedIterator<Triple> findByPredicate(Property predicate) {
            return getBaseGraph().find(Node.ANY, predicate.asNode(), Node.ANY);
        }

        private ExtendedIterator<Triple> findClassExpressions() {
            return findByPredicate(OWL.intersectionOf).andThen(findByPredicate(OWL.oneOf)).andThen(findByPredicate(OWL.unionOf)).andThen(findByPredicate(OWL.complementOf));
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
            getGraph().find(Node.ANY, RDF_TYPE, OWL.ObjectProperty.asNode()).mapWith(Triple::getSubject).
                    forEachRemaining(objectProperty -> anonIndividuals.addAll(getBaseGraph().
                            find(Node.ANY, objectProperty, Node.ANY).mapWith(Triple::getObject).filterKeep(Node::isBlank).toSet()));
            for (Node i : anonIndividuals) {
                classes.addAll(getBaseGraph().find(i, RDF_TYPE, Node.ANY).mapWith(Triple::getObject).toSet());
            }
            classes.removeAll(BUILT_IN);
            // not datatype and not system class:
            classes.stream().filter(node -> !isDatatype(node)).forEach(node -> addType(node, OWL.Class));
            // could such OWL-structures be here (inside RDFS Ontology)?
            findClassExpressions().mapWith(Triple::getObject).filterDrop(RDFStoOWL.this::isDatatype).forEachRemaining(n -> addType(n, OWL.Class));
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
                if (types.isEmpty()) { //todo: ignore?
                    throw new OntJenaException("Can't determine property type for " + prop);
                }
                if (types.contains(OWL.DatatypeProperty) && types.contains(OWL.ObjectProperty)) { // todo: ignore?
                    throw new OntJenaException("Property " + prop + " can't be data and object at the same time.");
                }
                types.forEach(type -> addType(prop, type));
            }
        }

        private void fixIndividuals() {
            Set<Node> individuals = getBaseGraph().find(Node.ANY, RDF_TYPE, Node.ANY).mapWith(Triple::getSubject).
                    filterKeep(Node::isURI).filterKeep(RDFStoOWL.this::isIndividual).toSet();
            individuals.forEach(node -> addType(node, OWL.NamedIndividual));
        }
    }

    /**
     * To fix missed declarations in the right side of triple.
     * For owl:Class, owl:Datatype, owl:ObjectProperties and owl:DatatypeProperty
     * example:
     * If we have triple "C1 owl:equivalentClass C2" where C1 has type owl:Class and C2 is untyped uri-resource,
     * then we add the same type (owl:Class) to graph for C2 (i.e. declare C2 as separated class in owl-model).
     */
    private static class ObjectReferenceFixer extends TransformAction {

        private ObjectReferenceFixer(Graph graph) {
            super(graph);
        }

        @Override
        public void perform() {
            Model m = ModelFactory.createModelForGraph(getGraph());
            fixClasses(m);
            fixProperties(m);
            // domain is always class for object and data properties
            m.listStatements(null, RDFS.domain, (RDFNode) null)
                    .filterKeep(s -> chooseType(types(s.getSubject()).collect(Collectors.toSet()), OWL.ObjectProperty, OWL.DatatypeProperty) != null).
                    mapWith(Statement::getObject).forEachRemaining(o -> fixType(o, OWL.Class));
        }

        private void fixClasses(Model m) {
            fixSimpleTriples(m, OWL.equivalentClass, OWL.Class, RDFS.Datatype);

            Stream.of(RDFS.subClassOf, OWL.disjointWith)
                    .forEach(predicate -> fixSimpleTriples(m, predicate, OWL.Class));

            // owl:disjointUnionOf:
            if (getBaseGraph().contains(Node.ANY, OWL.disjointUnionOf.asNode(), Node.ANY)) {
                m.listStatements(null, OWL.disjointUnionOf, (RDFNode) null).forEachRemaining(this::fixClassList);
            }
            // owl:AllDisjointClasses:
            if (getBaseGraph().contains(Node.ANY, RDF_TYPE, OWL.AllDisjointClasses.asNode())) {
                m.listStatements(null, RDF.type, OWL.AllDisjointClasses).mapWith(Statement::getSubject)
                        .mapWith(main -> m.getProperty(main, OWL.members))
                        .forEachRemaining(this::fixClassList);
            }
        }

        private void fixProperties(Model m) {
            fixSimpleTriples(m, OWL.inverseOf, OWL.ObjectProperty);
            Stream.of(RDFS.subPropertyOf, OWL.equivalentProperty, OWL.propertyDisjointWith)
                    .forEach(predicate -> fixSimpleTriples(m, predicate, OWL.ObjectProperty, OWL.DatatypeProperty));

            // owl:AllDisjointProperties (object or data properties)
            if (getBaseGraph().contains(Node.ANY, RDF_TYPE, OWL.AllDisjointProperties.asNode())) {
                m.listStatements(null, RDF.type, OWL.AllDisjointProperties).mapWith(Statement::getSubject)
                        .mapWith(main -> m.getProperty(main, OWL.members))
                        .forEachRemaining(this::fixPropertyList);
            }
        }

        private void fixSimpleTriples(Model m, Property predicate, Resource... allowedTypes) {
            m.listStatements(null, predicate, (RDFNode) null)
                    .forEachRemaining(s -> {
                        Resource type = chooseType(types(s.getSubject()).collect(Collectors.toSet()), allowedTypes);
                        if (type == null) return;
                        fixType(s.getObject(), type);
                    });
        }

        private Resource chooseType(Set<Resource> candidates, Resource... oneOf) {
            Set<Resource> res = Stream.of(oneOf).collect(Collectors.toSet());
            res.retainAll(candidates);
            return res.size() == 1 ? (Resource) res.toArray()[0] : null;
        }

        private void fixClassList(Statement statement) {
            if (statement == null) return;
            if (!statement.getObject().canAs(RDFList.class)) return;
            RDFList list = statement.getObject().as(RDFList.class);
            list.asJavaList().forEach(r -> fixType(r, OWL.Class));
        }

        private void fixPropertyList(Statement statement) {
            if (statement == null) return;
            if (!statement.getObject().canAs(RDFList.class)) return;
            RDFList list = statement.getObject().as(RDFList.class);
            List<RDFNode> members = list.asJavaList();
            Set<Resource> types = members.stream().map(this::types).flatMap(Function.identity()).collect(Collectors.toSet());
            Resource type = chooseType(types, OWL.ObjectProperty, OWL.DatatypeProperty);
            if (type == null) return;
            members.forEach(r -> fixType(r, type));
        }

        private void fixType(RDFNode object, Resource type) {
            if (type == null) return;
            if (!object.isURIResource()) { // if it is not uri then nothing has been missed.
                return;
            }
            if (types(object).count() != 0) { // don't touch if it has already some types (even they wrong)
                return;
            }
            if (BuiltIn.ALL.contains(object.asResource())) { // example : sp:ElementList rdfs:subClassOf rdf:List
                return;
            }
            addType(object.asNode(), type);
        }

        protected Stream<Resource> types(RDFNode inModel) {
            return !inModel.isURIResource() ? Stream.empty() :
                    Models.asStream(inModel.getModel().listStatements(inModel.asResource(), RDF.type, (RDFNode) null)
                            .mapWith(Statement::getObject)
                            .filterKeep(RDFNode::isURIResource).mapWith(RDFNode::asResource));
        }
    }

    /**
     * any uri-resources from right part should be declared as owl:Class if there is no other declarations.
     */
    private static class OwlClassFixer extends TransformAction {

        protected OwlClassFixer(Graph graph) {
            super(graph);
        }

        @Override
        public void perform() {
            Set<Node> nodes = getBaseGraph().find(Node.ANY, RDF_TYPE, Node.ANY)
                    .mapWith(Triple::getObject)
                    .filterKeep(Node::isURI)
                    .filterDrop(BUILT_IN::contains)
                    .filterDrop(node -> getGraph().contains(node, Node.ANY, Node.ANY))
                    .toSet();
            nodes.forEach(node -> addType(node, OWL.Class));
        }
    }
    /**
     * To fix missed owl:NamedIndividual declaration
     */
    private static class NamedIndividualFixer extends TransformAction {
        private NamedIndividualFixer(Graph graph) {
            super(graph);
        }

        @Override
        public void perform() {
            Set<Node> individuals = getBaseGraph().find(Node.ANY, RDF_TYPE, Node.ANY)
                    .filterKeep(triple -> triple.getSubject().isURI())
                    .filterKeep(triple -> getTypes(triple.getObject()).contains(OWL.Class.asNode())).mapWith(Triple::getSubject).toSet();
            individuals.forEach(node -> addType(node, OWL.NamedIndividual));
        }
    }

    /**
     * To fix missed property owl type declarations
     */
    private static class PropertyTypeFixer extends TransformAction {
        private static final Set<Node> BUILT_IN_ANNOTATION_PROPERTIES = BuiltIn.ANNOTATION_PROPERTIES.stream().map(Resource::asNode).collect(Collectors.toSet());
        private static final Set<Node> BUILT_IN_OBJECT_PROPERTIES = BuiltIn.OBJECT_PROPERTIES.stream().map(Resource::asNode).collect(Collectors.toSet());
        private static final Set<Node> BUILT_IN_DATA_PROPERTIES = BuiltIn.DATA_PROPERTIES.stream().map(Resource::asNode).collect(Collectors.toSet());

        private PropertyTypeFixer(Graph graph) {
            super(graph);
        }

        @Override
        public void perform() {
            getBaseGraph().find(Node.ANY, RDF_TYPE, OWL.DatatypeProperty.asNode()).mapWith(Triple::getSubject).filterKeep(this::isAnnotationProperty)
                    .forEachRemaining(node -> addType(node, OWL.AnnotationProperty));
            getBaseGraph().find(Node.ANY, RDF_TYPE, OWL.ObjectProperty.asNode()).mapWith(Triple::getSubject).filterKeep(this::isAnnotationProperty)
                    .forEachRemaining(node -> addType(node, OWL.ObjectProperty));

            getBaseGraph().find(Node.ANY, RDF_TYPE, OWL.AnnotationProperty.asNode()).mapWith(Triple::getSubject)
                    .forEachRemaining(node -> {
                        if (isObjectProperty(node)) {
                            addType(node, OWL.ObjectProperty);
                        } else if (isDataProperty(node)) {
                            addType(node, OWL.DatatypeProperty);
                        }
                    });
        }

        boolean isAnnotationProperty(Node node) {
            return BUILT_IN_ANNOTATION_PROPERTIES.contains(node) ||
                    getTypes(node).contains(OWL.AnnotationProperty.asNode()) ||
                    Models.asStream(getGraph().find(node, RDFS.subPropertyOf.asNode(), Node.ANY).mapWith(Triple::getObject)).
                            anyMatch(this::isAnnotationProperty);
        }

        boolean isObjectProperty(Node node) {
            return BUILT_IN_OBJECT_PROPERTIES.contains(node) ||
                    getTypes(node).contains(OWL.ObjectProperty.asNode()) ||
                    Models.asStream(getGraph().find(node, RDFS.subPropertyOf.asNode(), Node.ANY).mapWith(Triple::getObject)).
                            anyMatch(this::isObjectProperty);
        }

        boolean isDataProperty(Node node) {
            return BUILT_IN_DATA_PROPERTIES.contains(node) ||
                    getTypes(node).contains(OWL.DatatypeProperty.asNode()) ||
                    Models.asStream(getGraph().find(node, RDFS.subPropertyOf.asNode(), Node.ANY).mapWith(Triple::getObject)).
                            anyMatch(this::isDataProperty);
        }
    }

}
