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

import ru.avicomp.ontapi.jena.utils.BuiltIn;
import ru.avicomp.ontapi.jena.utils.Models;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

/**
 * Class to perform some transformation action on the specified graph.
 * Currently it is to convert the OWL1/RDFS ontological graph to the OWL2DL graph and to fix missed declarations.
 * Use it to fix "mistakes" in graph after loading from io-stream according OWL2 specification and before using common API.
 * <p>
 * Created by szuev on 28.10.2016.
 */
public abstract class GraphConverter {
    private static final Logger LOGGER = Logger.getLogger(GraphConverter.class);
    public static final FactoryStore CONVERTERS = new FactoryStore()
            .add(RDFStoOWLFixer::new)
            .add(OWLtoOWL2DLFixer::new)
            .add(OwlDeclarationFixer::new);

    /**
     * the main method to perform conversion one {@link Graph} to another.
     * Note: currently it returns the same graph, not a fixed copy.
     *
     * @param graph input graph
     * @return output graph
     */
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
     * The base class for any graph-converter.
     */
    @SuppressWarnings("WeakerAccess")
    public static abstract class TransformAction {
        protected static final Node RDF_TYPE = RDF.type.asNode();
        protected static final Set<Node> BUILT_IN = BuiltIn.ALL.stream().map(FrontsNode::asNode).collect(Collectors.toSet());

        private final Graph graph;

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

        public Model getModel() {
            return ModelFactory.createModelForGraph(getGraph());
        }

        /**
         * returns Stream of types for specified {@link RDFNode}, or empty stream if the input is not uri-resource.
         *
         * @param node node, attached to model.
         * @return Stream of {@link Resource}s
         */
        protected Stream<Resource> types(RDFNode node) {
            return types(node, true);
        }

        protected Stream<Resource> types(RDFNode node, boolean requireURI) {
            return requireURI && !node.isURIResource() ? Stream.empty() :
                    Models.asStream(node.asResource().listProperties(RDF.type)
                            .mapWith(Statement::getObject)
                            .filterKeep(RDFNode::isURIResource)
                            .mapWith(RDFNode::asResource));
        }
    }

    /**
     * OWL 1 DL -> OWL 2 DL
     * See <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Parsing_of_the_Ontology_Header_and_Declarations'>Chapter 3, Table 5 and Table 6</a>
     */
    public static class OWLtoOWL2DLFixer extends TransformAction {
        public OWLtoOWL2DLFixer(Graph graph) {
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
            // TODO: merge several owl:Ontology to single one.
            // TODO: if there is no any owl:Ontology -> add new anonymous owl:Ontology
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
     * TODO:
     * To perform preliminary fixing: transform the RDFS ontological graph to the OWL ontological graph.
     * After this conversion is completed there would be a valid owl-ontology but maybe with missing declarations and
     * with some RDFS-garbage (rdfs:Class, rdf:Property).
     * It seems it can be considered as an OWL1.
     * <p>
     * This transformer is optional: if ontology graph already contains one of the five main owl-declarations (owl:Class,
     * owl:ObjectProperty, owl:DatatypeProperty, owl:AnnotationProperty, owl:NamedIndividual) then it can't be pure RDFS-ontology
     * and we believe that there is nothing to do.
     * <p>
     * For some additional info see <a href='https://www.w3.org/TR/rdf-schema'>RDFS specification</a> and
     * <a href='https://www.w3.org/TR/2012/REC-owl2-overview-20121211/#Relationship_to_OWL_1'>some words about OWL 1</a>
     */
    public static class RDFStoOWLFixer extends TransformAction {
        private static final Set<Node> DATATYPES = BuiltIn.DATATYPES.stream().map(FrontsNode::asNode).collect(Collectors.toSet());

        public RDFStoOWLFixer(Graph graph) {
            super(graph);
        }

        @Override
        public void perform() {
            // the order is important
            fixProperties();
            fixClasses();
        }

        @Override
        public boolean test() {
            return !containsType(OWL.Class)
                    && !containsType(OWL.AnnotationProperty)
                    && !containsType(OWL.DatatypeProperty)
                    && !containsType(OWL.ObjectProperty)
                    && !containsType(OWL.NamedIndividual);
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

        private boolean isDatatypeRange(Node range) {
            return DATATYPES.contains(range) ||
                    getGraph().contains(range, RDF_TYPE, RDFS.Datatype.asNode()) ||
                    getGraph().contains(range, RDF_TYPE, OWL.DataRange.asNode());
        }

        private boolean isDatatype(Node subject) {
            return getGraph().contains(subject, RDF_TYPE, RDFS.Datatype.asNode());
        }

        private ExtendedIterator<Triple> findByPredicate(Property predicate) {
            return getBaseGraph().find(Node.ANY, predicate.asNode(), Node.ANY);
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

        private ExtendedIterator<Triple> findClassExpressions() {
            return findByPredicate(OWL.intersectionOf).andThen(findByPredicate(OWL.oneOf)).andThen(findByPredicate(OWL.unionOf)).andThen(findByPredicate(OWL.complementOf));
        }

        private void fixClasses() {
            Set<Node> declarations = getBaseGraph()
                    .find(Node.ANY, RDF_TYPE, RDFS.Class.asNode())
                    .andThen(findByPredicate(RDFS.subClassOf))
                    .mapWith(Triple::getSubject).toSet();
            Set<Node> classes = new HashSet<>(declarations);
            // consider uri-objects from {@Class,rdfs:subClassOf,@Any} and {@Class,rdf:type,@Any} triplets as owl:Class also:
            for (Node subject : declarations) {
                Stream.of(RDF_TYPE, RDFS.subClassOf.asNode())
                        .forEach(predicate -> classes.addAll(getBaseGraph()
                                .find(subject, predicate, Node.ANY)
                                .mapWith(Triple::getObject).filterKeep(Node::isURI)
                                .toSet()));
            }
            // left side of ObjectProperties is an individual; anonymous individuals should be assigned to class
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
            findClassExpressions().mapWith(Triple::getObject).filterDrop(RDFStoOWLFixer.this::isDatatype).forEachRemaining(n -> addType(n, OWL.Class));
        }
    }


    /**
     * Class to perform the final tuning of the ontology: mostly for fixing missed owl-declarations where it is possible.
     * Consists of several other converters.
     */
    public static class OwlDeclarationFixer extends TransformAction {
        private final List<TransformAction> inner;

        public OwlDeclarationFixer(Graph graph) {
            super(graph);
            inner = Stream.of(
                    new StandaloneURIFixer(graph)

                    , new ObjectTripleDeclarationFixer(graph)
                    , new SubjectTripleDeclarationFixer(graph)
                    , new HierarchicalPropertyFixer(graph)

                    , new NamedIndividualFixer(graph)
            ).collect(Collectors.toList());
        }

        @Override
        public void perform() {
            inner.forEach(TransformAction::perform);
        }
    }

    /**
     * TODO:
     * To fix missed declarations for the object of the triple in clear cases (when we know exactly what type expected).
     * <p>
     * For owl:Class, owl:Datatype, owl:ObjectProperties, owl:DatatypeProperty, owl:NameIndividual
     * <p>
     * Example:
     * If we have triple "C1 owl:equivalentClass C2" where C1 has type owl:Class and C2 is untyped uri-resource,
     * then we know exactly that C2 is a class also and so we can add the same type (owl:Class) to graph for C2 (i.e.
     * declare C2 as separated class in owl-model).
     */
    private static class ObjectTripleDeclarationFixer extends TransformAction {

        ObjectTripleDeclarationFixer(Graph graph) {
            super(graph);
        }

        @Override
        public void perform() {
            Model m = getModel();
            fixClassesAndDatatypes(m);
            fixOwlProperties(m);
            fixNamedIndividuals(m);
            // domain is always class for object and datatype properties
            m.listStatements(null, RDFS.domain, (RDFNode) null)
                    .filterKeep(s -> chooseTypeFrom(types(s.getSubject()), OWL.ObjectProperty, OWL.DatatypeProperty) != null).
                    mapWith(Statement::getObject).forEachRemaining(o -> declare(o, OWL.Class, true));
        }


        private void fixNamedIndividuals(Model m) {
            Stream.of(OWL.sameAs, OWL.differentFrom)
                    .forEach(p -> fixExplicitTypes(m, p, OWL.NamedIndividual));

            // triple with owl:hasValue has always individual or literal as object
            // owl:targetIndividual, owl:sourceIndividual has always individual in the right part of triple
            Stream.of(OWL.hasValue, OWL.targetIndividual, OWL.sourceIndividual).forEach(p ->
                    m.listStatements(null, p, (RDFNode) null)
                            .mapWith(Statement::getObject)
                            .filterKeep(RDFNode::isURIResource)
                            .mapWith(RDFNode::asResource)
                            .forEachRemaining(r -> declare(r, OWL.NamedIndividual, true)));
            //if (containsType(OWL.oneOf))
            m.listStatements(null, OWL.oneOf, (RDFNode) null).forEachRemaining(s -> fixList(s, OWL.NamedIndividual));
            //if (containsType(OWL.AllDifferent))
            m.listStatements(null, RDF.type, OWL.AllDifferent).mapWith(Statement::getSubject)
                    .mapWith(main -> m.getProperty(main, OWL.members))
                    .forEachRemaining(s -> fixList(s, OWL.NamedIndividual));
        }

        private void fixClassesAndDatatypes(Model m) {
            fixAmbiguousTypes(m, OWL.equivalentClass, OWL.Class, RDFS.Datatype);

            Stream.of(RDFS.subClassOf, OWL.disjointWith, OWL.onClass).forEach(p -> fixExplicitTypes(m, p, OWL.Class));
            //if (containsType(OWL.disjointUnionOf))
            m.listStatements(null, OWL.disjointUnionOf, (RDFNode) null).forEachRemaining(s -> fixList(s, OWL.Class));
            //if (containsType(OWL.AllDisjointClasses))
            m.listStatements(null, RDF.type, OWL.AllDisjointClasses).mapWith(Statement::getSubject)
                    .mapWith(main -> m.getProperty(main, OWL.members))
                    .forEachRemaining(s -> fixList(s, OWL.Class));

            fixRestrictions(m);
        }

        private void fixRestrictions(Model m) {
            // fix allValuesFrom, someValuesFrom by onProperty
            Stream.of(OWL.allValuesFrom, OWL.someValuesFrom)
                    .forEach(predicate -> m.listStatements(null, predicate, (RDFNode) null)
                            .forEachRemaining(s -> {
                                Set<Resource> propertyTypes = Models.asStream(m.listStatements(s.getSubject(), OWL.onProperty, (RDFNode) null)
                                        .mapWith(Statement::getObject))
                                        .map(n -> types(n, false))
                                        .flatMap(Function.identity())
                                        .collect(Collectors.toSet());
                                Resource type = propertyTypes.contains(OWL.ObjectProperty) ? OWL.Class :
                                        propertyTypes.contains(OWL.DatatypeProperty) ? RDFS.Datatype : null;
                                declare(s.getObject(), type, false);
                            }));
        }

        private void fixOwlProperties(Model m) {
            fixExplicitTypes(m, OWL.inverseOf, OWL.ObjectProperty);
            Stream.of(RDFS.subPropertyOf, OWL.equivalentProperty, OWL.propertyDisjointWith)
                    .forEach(predicate -> fixAmbiguousTypes(m, predicate, OWL.ObjectProperty, OWL.DatatypeProperty));
            //if (containsType(OWL.AllDisjointProperties))
            m.listStatements(null, RDF.type, OWL.AllDisjointProperties).mapWith(Statement::getSubject)
                    .mapWith(main -> m.getProperty(main, OWL.members))
                    .forEachRemaining(this::fixPropertyList);

            // Restriction 'hasSelf' has always ObjectProperty assigned.
            Literal _true = ResourceFactory.createTypedLiteral(true);
            m.listStatements(null, OWL.onProperty, (RDFNode) null)
                    .filterKeep(s -> m.contains(s.getSubject(), OWL.hasSelf, _true))
                    .mapWith(Statement::getObject)
                    .forEachRemaining(n -> declare(n, OWL.ObjectProperty, true));
        }

        /**
         * fix for ambiguous situations when the predicate is used for different types.
         * e.g. owl:equivalentClass can be used to define Datatype and Class both,
         * so we have to choose the correct type from the specified array.
         *
         * @param m              Model
         * @param predicate      Property
         * @param candidateTypes Array of types to choose.
         */
        private void fixAmbiguousTypes(Model m, Property predicate, Resource... candidateTypes) {
            m.listStatements(null, predicate, (RDFNode) null)
                    .forEachRemaining(s -> declare(s.getObject(), chooseTypeFrom(types(s.getSubject()), candidateTypes), false));
        }

        private void fixExplicitTypes(Model m, Property predicate, Resource type) {
            m.listStatements(null, predicate, (RDFNode) null)
                    .forEachRemaining(s -> declare(s.getObject(), type, true));
        }

        private Resource chooseTypeFrom(Stream<Resource> stream, Resource... oneOf) {
            Set<Resource> trueTypes = stream.collect(Collectors.toSet());
            return Stream.of(oneOf).filter(trueTypes::contains).findFirst().orElse(null);
        }

        private void fixList(Statement statement, Resource type) {
            if (statement == null) return;
            if (!statement.getObject().canAs(RDFList.class)) return;
            RDFList list = statement.getObject().as(RDFList.class);
            list.asJavaList().forEach(r -> declare(r, type, true));
        }

        private void fixPropertyList(Statement statement) {
            if (statement == null) return;
            if (!statement.getObject().canAs(RDFList.class)) return;
            RDFList list = statement.getObject().as(RDFList.class);
            List<RDFNode> members = list.asJavaList();
            Stream<Resource> types = members.stream().map(this::types).flatMap(Function.identity());
            Resource type = chooseTypeFrom(types, OWL.ObjectProperty, OWL.DatatypeProperty);
            if (type == null) return;
            members.forEach(r -> declare(r, type, false));
        }

        /**
         * adds declaration triple "@object rdf:type @type" into base class iff @object is uri resource,
         * have no other types assigned and is not built-in.
         *
         * @param object the subject of new triple.
         * @param type   the object of new triple.
         * @param force  if true don't check previous assigned types.
         */
        void declare(RDFNode object, Resource type, boolean force) {
            if (type == null) return;
            if (!object.isURIResource()) { // if it is not uri then nothing has been missed.
                return;
            }
            if (!force && types(object).count() != 0) { // don't touch if it has already some types (even they wrong)
                return;
            }
            if (BuiltIn.ALL.contains(object.asResource())) { // example : sp:ElementList rdfs:subClassOf rdf:List
                return;
            }
            addType(object.asNode(), type);
        }
    }

    private static class SubjectTripleDeclarationFixer extends TransformAction {
        SubjectTripleDeclarationFixer(Graph graph) {
            super(graph);
        }

        @Override
        public void perform() {
            fixObjectProperties();
        }

        private void fixObjectProperties() {
            Stream.of(OWL.InverseFunctionalProperty, OWL.ReflexiveProperty, OWL.IrreflexiveProperty,
                    OWL.SymmetricProperty, OWL.AsymmetricProperty, OWL.TransitiveProperty)
                    .forEach(type -> getGraph().find(Node.ANY, RDF_TYPE, type.asNode())
                            .mapWith(Triple::getSubject)
                            .filterKeep(Node::isURI)
                            .forEachRemaining(r -> addType(r, OWL.ObjectProperty)));
        }
    }

    /**
     * Any uri-resources from the right part of the declaration-triple or from rdf:List should be treated as owl:Class
     * if it is not built-in and there are no other occurrences (declarations) in the union graph.
     */
    private static class StandaloneURIFixer extends TransformAction {

        StandaloneURIFixer(Graph graph) {
            super(graph);
        }

        @Override
        public void perform() {
            Set<Node> nodes = getBaseGraph()
                    .find(Node.ANY, RDF_TYPE, Node.ANY)
                    .andThen(getBaseGraph().find(Node.ANY, RDF.first.asNode(), Node.ANY))
                    .mapWith(Triple::getObject)
                    .filterKeep(Node::isURI)
                    .filterDrop(BUILT_IN::contains)
                    .filterDrop(node -> getGraph().contains(node, Node.ANY, Node.ANY))
                    .toSet();
            nodes.forEach(node -> addType(node, OWL.Class));
        }
    }

    /**
     * To fix missed owl:NamedIndividual declarations.
     */
    private static class NamedIndividualFixer extends TransformAction {
        NamedIndividualFixer(Graph graph) {
            super(graph);
        }

        @Override
        public void perform() {
            Set<Node> individuals = getBaseGraph().find(Node.ANY, RDF_TYPE, Node.ANY)
                    .filterKeep(triple -> triple.getSubject().isURI())
                    .filterKeep(triple -> getTypes(triple.getObject()).contains(OWL.Class.asNode()))
                    .mapWith(Triple::getSubject).toSet();
            individuals.forEach(node -> addType(node, OWL.NamedIndividual));
        }
    }

    /**
     * To fix missed owl-property type declarations.
     */
    private static class HierarchicalPropertyFixer extends TransformAction {
        private static final Set<Node> BUILT_IN_ANNOTATION_PROPERTIES = BuiltIn.ANNOTATION_PROPERTIES.stream().map(Resource::asNode).collect(Collectors.toSet());
        private static final Set<Node> BUILT_IN_OBJECT_PROPERTIES = BuiltIn.OBJECT_PROPERTIES.stream().map(Resource::asNode).collect(Collectors.toSet());
        private static final Set<Node> BUILT_IN_DATA_PROPERTIES = BuiltIn.DATA_PROPERTIES.stream().map(Resource::asNode).collect(Collectors.toSet());

        HierarchicalPropertyFixer(Graph graph) {
            super(graph);
        }

        @Override
        public void perform() {
            getBaseGraph()
                    .find(Node.ANY, RDF_TYPE, OWL.DatatypeProperty.asNode())
                    .mapWith(Triple::getSubject)
                    .filterKeep(this::isAnnotationProperty)
                    .forEachRemaining(node -> addType(node, OWL.AnnotationProperty));
            getBaseGraph()
                    .find(Node.ANY, RDF_TYPE, OWL.ObjectProperty.asNode())
                    .mapWith(Triple::getSubject)
                    .filterKeep(this::isAnnotationProperty)
                    .forEachRemaining(node -> addType(node, OWL.ObjectProperty));
            getBaseGraph()
                    .find(Node.ANY, RDF_TYPE, OWL.AnnotationProperty.asNode())
                    .mapWith(Triple::getSubject)
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
