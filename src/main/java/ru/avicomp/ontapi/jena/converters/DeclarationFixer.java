package ru.avicomp.ontapi.jena.converters;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDFS;

import ru.avicomp.ontapi.jena.utils.BuiltIn;
import ru.avicomp.ontapi.jena.utils.Models;
import ru.avicomp.ontapi.jena.utils.Streams;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

/**
 * todo: rewrite
 * Class to perform the final tuning of the ontology: mostly for fixing missed owl-declarations where it is possible.
 * Consists of several other converters.
 */
public class DeclarationFixer extends TransformAction {
    private final List<TransformAction> inner;

    public DeclarationFixer(Graph graph) {
        super(graph);
        inner = Stream.of(
                new ObjectTripleDeclarationFixer(graph)
                , new SubjectTripleDeclarationFixer(graph)

                , new StandaloneURIFixer(graph)
                , new HierarchicalPropertyFixer(graph)

                , new ExtraDeclarationFixer(graph)

                , new NamedIndividualFixer(graph)
        ).collect(Collectors.toList());
    }

    @Override
    public void perform() {
        inner.forEach(TransformAction::perform);
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
    private static class ObjectTripleDeclarationFixer extends BaseTripleDeclarationFixer {

        ObjectTripleDeclarationFixer(Graph graph) {
            super(graph);
        }

        @Override
        public void perform() {
            fixByAnnotations();
            fixClassesAndDatatypes();
            fixOwlProperties();
            fixNamedIndividuals();
            // domain is always class for object and datatype properties
            listStatements(null, RDFS.domain, null)
                    .filter(s -> chooseTypeFrom(types(s.getSubject()), OWL.ObjectProperty, OWL.DatatypeProperty) != null)
                    .map(Statement::getObject)
                    .forEach(o -> declare(o, OWL.Class, true));
        }

        private void fixNamedIndividuals() {
            Model m = getModel();
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
                    .mapWith(main -> {
                        Statement res = m.getProperty(main, OWL.members);
                        return res != null ? res : m.getProperty(main, OWL.distinctMembers);
                    })
                    .filterKeep(Objects::nonNull)
                    .forEachRemaining(s -> fixList(s, OWL.NamedIndividual));
        }

        private void fixClassesAndDatatypes() {
            Model m = getModel();
            fixAmbiguousTypes(m, OWL.equivalentClass, OWL.Class, RDFS.Datatype);

            Stream.of(RDFS.subClassOf, OWL.disjointWith, OWL.onClass).forEach(p -> fixExplicitTypes(m, p, OWL.Class));

            fixExplicitTypes(m, OWL.onDataRange, RDFS.Datatype);

            //if (containsType(OWL.disjointUnionOf))
            m.listStatements(null, OWL.disjointUnionOf, (RDFNode) null).forEachRemaining(s -> fixList(s, OWL.Class));
            //if (containsType(OWL.AllDisjointClasses))
            m.listStatements(null, RDF.type, OWL.AllDisjointClasses).mapWith(Statement::getSubject)
                    .mapWith(main -> m.getProperty(main, OWL.members))
                    .filterKeep(Objects::nonNull)
                    .forEachRemaining(s -> fixList(s, OWL.Class));

            fixRestrictionExpressionsByProperty(m);
            fixEnumerationExpressionsByEntity(m);
            fixBooleanExpressionsByClass(m);
        }

        private void fixOwlProperties() {
            Model m = getModel();
            fixExplicitTypes(m, OWL.inverseOf, OWL.ObjectProperty);
            Stream.of(RDFS.subPropertyOf, OWL.equivalentProperty, OWL.propertyDisjointWith)
                    .forEach(predicate -> fixAmbiguousTypes(m, predicate, OWL.ObjectProperty, OWL.DatatypeProperty));
            //if (containsType(OWL.AllDisjointProperties))
            m.listStatements(null, RDF.type, OWL.AllDisjointProperties)
                    .mapWith(Statement::getSubject)
                    .mapWith(main -> m.getProperty(main, OWL.members))
                    .filterKeep(Objects::nonNull)
                    .forEachRemaining(this::fixPropertyList);

            fixHasSelfExpressions(m);
            fixRestrictionExpressionsByEntity(m);
        }

        private void fixByAnnotations() {
            listStatements(null, OWL.annotatedProperty, RDF.type)
                    .map(Statement::getSubject)
                    .filter(RDFNode::isAnon)
                    .forEach(this::fixDeclarationFromAnnotation);
        }

        private void fixDeclarationFromAnnotation(Resource root) {
            if (types(root, false).noneMatch(type -> OWL.Axiom.equals(type) || OWL.Annotation.equals(type))) return;
            try {
                Resource entity = root.getProperty(OWL.annotatedSource).getObject().asResource();
                Resource type = root.getProperty(OWL.annotatedTarget).getObject().asResource();
                if (!entity.isURIResource()) return;
                if (!type.isURIResource()) return;
                addType(entity, type);
            } catch (Exception ignore) {
                // ignore. wrong annotation
            }
        }

        private void fixRestrictionExpressionsByProperty(Model m) {
            // fix allValuesFrom, someValuesFrom by onProperty
            Stream.of(OWL.allValuesFrom, OWL.someValuesFrom)
                    .forEach(predicate -> m.listStatements(null, predicate, (RDFNode) null)
                            .filterKeep(this::isRestriction)
                            .forEachRemaining(s -> {
                                Resource propertyType = choosePropertyType(getOnPropertyFromRestriction(s.getSubject()));
                                if (propertyType == null) return;
                                declare(s.getObject(), OWL.ObjectProperty.equals(propertyType) ? OWL.Class : RDFS.Datatype, true);
                            }));
        }

        private void fixEnumerationExpressionsByEntity(Model m) {
            Stream.of(OWL.intersectionOf, OWL.unionOf)
                    .forEach(predicate -> m.listStatements(null, predicate, (RDFNode) null)
                            .forEachRemaining(s -> {
                                Resource type = chooseExpressionType(s.getSubject());
                                fixList(s, type);
                            }));

        }

        private void fixBooleanExpressionsByClass(Model m) {
            // owl:complementOf  -> always class
            m.listStatements(null, OWL.complementOf, (RDFNode) null)
                    .mapWith(Statement::getObject)
                    .forEachRemaining(n -> declare(n, OWL.Class, true));
        }

        private void fixHasSelfExpressions(Model m) {
            // Restriction 'hasSelf' has always ObjectProperty assigned.
            m.listStatements(null, OWL.onProperty, (RDFNode) null)
                    .filterKeep(this::isRestriction)
                    .filterKeep(s -> m.contains(s.getSubject(), OWL.hasSelf, Models.TRUE)).forEachRemaining(s -> {
                Resource property = getOnPropertyFromRestriction(s.getSubject());
                if (property == null) return;
                declare(property, OWL.ObjectProperty, true);
            });
        }

        private void fixRestrictionExpressionsByEntity(Model m) {
            // fix allValuesFrom, someValuesFrom by entity (datarange or class)
            Stream.of(OWL.allValuesFrom, OWL.someValuesFrom)
                    .forEach(predicate -> m.listStatements(null, predicate, (RDFNode) null)
                            .filterKeep(this::isRestriction)
                            .forEachRemaining(s -> {
                                Resource entityType = chooseExpressionType(s.getObject());
                                if (entityType == null) return;
                                Resource property = getOnPropertyFromRestriction(s.getSubject());
                                if (property == null) return;
                                declare(property, OWL.Class.equals(entityType) ? OWL.ObjectProperty : OWL.DatatypeProperty, true);
                            }));
        }

        private boolean isRestriction(Statement s) {
            return s != null && s.getModel().contains(s.getSubject(), RDF.type, OWL.Restriction);
        }

        private Resource getOnPropertyFromRestriction(RDFNode node) {
            if (node == null || !node.isResource()) return null;
            List<Resource> properties = Streams.asStream(node.getModel().listStatements(node.asResource(), OWL.onProperty, (RDFNode) null))
                    .map(Statement::getObject).filter(RDFNode::isResource).map(RDFNode::asResource).distinct().collect(Collectors.toList());
            return properties.size() == 1 ? properties.get(0) : null;
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


        private void fixList(Statement statement, Resource type) {
            if (statement == null || type == null) return;
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

    }

    private static class SubjectTripleDeclarationFixer extends BaseTripleDeclarationFixer {
        SubjectTripleDeclarationFixer(Graph graph) {
            super(graph);
        }

        @Override
        public void perform() {
            Model m = getModel();
            fixClassesAndDatatypes(m);
            fixObjectProperties(m);
        }

        private void fixClassesAndDatatypes(Model m) {
            // left part of triple for owl:equivalentClass
            m.listStatements(null, OWL.equivalentClass, (RDFNode) null)
                    .forEachRemaining(s -> declare(s.getSubject(), chooseExpressionType(s.getObject()), true));

        }

        private void fixObjectProperties(Model m) {
            //always object properties.
            Stream.of(OWL.InverseFunctionalProperty, OWL.ReflexiveProperty, OWL.IrreflexiveProperty,
                    OWL.SymmetricProperty, OWL.AsymmetricProperty, OWL.TransitiveProperty, OWL.propertyChainAxiom)
                    .forEach(type -> m.listStatements(null, RDF.type, type).mapWith(Statement::getSubject)
                            .forEachRemaining(r -> declare(r, OWL.ObjectProperty, true)));
        }
    }

    /**
     * to remove excessive declarations, such as "_:x rdf:type owl:ObjectProperty" for anonymous owl:inverseOf
     */
    private static class ExtraDeclarationFixer extends TransformAction {

        ExtraDeclarationFixer(Graph graph) {
            super(graph);
        }

        @Override
        public void perform() {
            getBaseGraph().find(Node.ANY, OWL.inverseOf.asNode(), Node.ANY)
                    .mapWith(Triple::getSubject)
                    .filterKeep(Node::isBlank)
                    .forEachRemaining(node -> deleteType(node, OWL.ObjectProperty));
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
    private static class NamedIndividualFixer extends BaseTripleDeclarationFixer {
        NamedIndividualFixer(Graph graph) {
            super(graph);
        }

        @Override
        public void perform() {
            Set<Resource> individuals = listStatements(null, RDF.type, null)
                    .filter(s -> s.getSubject().isURIResource())
                    .filter(s -> isDefinitelyClass(s.getObject()))
                    .map(Statement::getSubject).collect(Collectors.toSet());
            individuals.forEach(r -> declare(r, OWL.NamedIndividual, true));
        }
    }

    /**
     * todo: wrong logic
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
                    .filterKeep(this::isAnnotationProperty) //todo: what is it?
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
            return isTypePropertyOf(node, OWL.AnnotationProperty.asNode(), BUILT_IN_ANNOTATION_PROPERTIES);
        }

        boolean isObjectProperty(Node node) {
            return isTypePropertyOf(node, OWL.ObjectProperty.asNode(), BUILT_IN_OBJECT_PROPERTIES);
        }

        boolean isDataProperty(Node node) {
            return isTypePropertyOf(node, OWL.DatatypeProperty.asNode(), BUILT_IN_DATA_PROPERTIES);
        }

        private boolean isTypePropertyOf(Node candidate, Node type, Set<Node> builtIn) {
            return isTypePropertyOf(candidate, type, builtIn, new HashSet<>());
        }

        private boolean isTypePropertyOf(Node candidate, Node type, Set<Node> builtIn, Set<Node> processed) {
            processed.add(candidate);
            return builtIn.contains(candidate) ||
                    getTypes(candidate).contains(type) ||
                    Streams.asStream(getGraph().find(candidate, RDFS.subPropertyOf.asNode(), Node.ANY)
                            .mapWith(Triple::getObject).filterDrop(processed::contains)).
                            anyMatch(node -> isTypePropertyOf(node, type, builtIn, processed));
        }
    }

    /**
     * Created by szuev on 23.01.2017.
     */
    abstract static class BaseTripleDeclarationFixer extends TransformAction {
        BaseTripleDeclarationFixer(Graph graph) {
            super(graph);
        }

        /**
         * choose unambiguous type of expression (either datarange expression or class expression).
         *
         * @param node RDFNode to test
         * @return owl:Class, rdfs:Datatype or null
         */
        Resource chooseExpressionType(RDFNode node) {
            if (node == null || !node.isResource()) return null;
            List<Resource> types = types(node, false)
                    .map(r -> OWL.Class.equals(r) || OWL.Restriction.equals(r) ? OWL.Class : RDFS.Datatype.equals(r) ? RDFS.Datatype : null)
                    .distinct().collect(Collectors.toList());
            return types.size() == 1 ? types.get(0) : null;
        }

        boolean isDefinitelyClass(RDFNode subject) {
            return OWL.Class.equals(chooseExpressionType(subject));
        }

        /**
         * choose unambiguous type of property (either object property expression or datatype property).
         *
         * @param node RDFNode to test
         * @return owl:ObjectProperty, owl:DatatypeProperty or null
         */
        Resource choosePropertyType(RDFNode node) {
            if (node == null || !node.isResource()) return null;
            List<Resource> types = types(node, true)
                    .map(r -> OWL.ObjectProperty.equals(r) ? OWL.ObjectProperty : OWL.DatatypeProperty)
                    .distinct().collect(Collectors.toList());
            if (types.size() == 1) return types.get(0);
            if (node.getModel().contains(node.asResource(), OWL.inverseOf, (RDFNode) null) || node.getModel().contains(null, OWL.inverseOf, node)) {
                return OWL.ObjectProperty;
            }
            return null;
        }

        Resource chooseTypeFrom(Stream<Resource> stream, Resource... oneOf) {
            Set<Resource> trueTypes = stream.collect(Collectors.toSet());
            List<Resource> res = Stream.of(oneOf).filter(trueTypes::contains).distinct().collect(Collectors.toList());
            return res.size() == 1 ? res.get(0) : null;
        }

        /**
         * adds declaration triple "@object rdf:type @type" into base class iff @object is uri resource,
         * have no other types assigned and is not built-in.
         *
         * @param object the subject of new triple.
         * @param type   the object of new triple (null to do nothing).
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
}
