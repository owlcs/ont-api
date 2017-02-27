package ru.avicomp.ontapi.jena.converters;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.graph.FrontsNode;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDFS;

import ru.avicomp.ontapi.jena.utils.BuiltIn;
import ru.avicomp.ontapi.jena.utils.Models;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

/**
 * TODO: need to rewrite.
 * <p>
 * To perform preliminary fixing: transform the RDFS ontological graph to the OWL ontological graph.
 * After this conversion is completed there would be a valid owl-dl-ontology but maybe with missing declarations and
 * with some RDFS-garbage (rdfs:Class, rdf:Property).
 * It seems it can be considered as an OWL1 (till rdfs:Class, rdf:Property, etc would be removed by {@link OWLtoOWL2DLFixer})
 * <p>
 * This transformer is optional:
 * if ontology graph already contains one of the five main owl-declarations (owl:Class,
 * owl:ObjectProperty, owl:DatatypeProperty, owl:AnnotationProperty, owl:NamedIndividual) then it can't be pure RDFS-ontology
 * and we believe that there is nothing to do.
 * <p>
 * For some additional info see <a href='https://www.w3.org/TR/rdf-schema'>RDFS specification</a> and
 * <a href='https://www.w3.org/TR/2012/REC-owl2-overview-20121211/#Relationship_to_OWL_1'>some words about OWL 1</a>
 */
public class RDFStoOWLFixer extends TransformAction {
    private static final Set<Node> DATATYPES = BuiltIn.DATATYPES.stream().map(FrontsNode::asNode).collect(Collectors.toSet());

    public RDFStoOWLFixer(Graph graph) {
        super(graph);
    }

    @Override
    public void perform() {
        Model m = getBaseModel(); // TODO: change it.
        m.listStatements(null, RDF.type, RDF.Property).mapWith(Statement::getSubject).forEachRemaining(this::declareRDFProperty);
        m.listStatements(null, RDF.type, RDFS.Class).mapWith(Statement::getSubject).forEachRemaining(this::declareRDFSClass);
        /*m.listStatements(null, RDF.type, RDF.Property).andThen(m.listStatements(null, RDF.type, RDFS.Class))
                .mapWith(Statement::getSubject).forEachRemaining(r -> {
            declareRDFProperty(r);
            declareRDFSClass(r);
        });*/
        /*fixProperties();
        fixClasses();*/
    }

    private void declareRDFProperty(Resource resource) {
        Model m = resource.getModel();
        if (OntRules.isObjectPropertyExpression(m, resource)) {
            resource.addProperty(RDF.type, OWL.ObjectProperty);
        }
        if (OntRules.isDataProperty(m, resource)) {
            resource.addProperty(RDF.type, OWL.DatatypeProperty);
        }
        if (OntRules.isAnnotationProperty(m, resource)) {
            resource.addProperty(RDF.type, OWL.AnnotationProperty);
        }
    }

    private void declareRDFSClass(Resource resource) {
        Model m = resource.getModel();
        if (OntRules.isClass(m, resource)) {
            resource.addProperty(RDF.type, OWL.Class);
        }
        if (OntRules.isDatatype(m, resource)) {
            resource.addProperty(RDF.type, RDFS.Datatype);
        }
    }

    @Override
    public boolean test() {
        return isRDFS() && !isOWL();
    }

    private boolean isRDFS() {
        return containsType(RDFS.Class) || containsType(RDF.Property);
    }

    private boolean isOWL() {
        return containsType(OWL.Class) || containsType(OWL.NamedIndividual)
                || containsType(OWL.AnnotationProperty) || containsType(OWL.DatatypeProperty) || containsType(OWL.ObjectProperty);
    }

    private Set<Resource> getPropertyTypes(Node subject) {
        return getPropertyTypes(subject, new HashSet<>());
    }

    private Set<Resource> getPropertyTypes(Node subject, Set<Node> processed) {
        processed.add(subject);
        Set<Resource> res = new TreeSet<>(Models.RDF_NODE_COMPARATOR);
        Set<Node> ranges = getGraph().find(subject, RDFS.range.asNode(), Node.ANY)
                .mapWith(Triple::getObject).filterDrop(processed::contains).toSet();
        Set<Node> domains = getGraph().find(subject, RDFS.domain.asNode(), Node.ANY)
                .mapWith(Triple::getObject).filterDrop(processed::contains).toSet();
        Set<Node> superProperties = getGraph().find(subject, RDFS.subPropertyOf.asNode(), Node.ANY)
                .mapWith(Triple::getObject).filterDrop(processed::contains).toSet();
        if (ranges.isEmpty() && domains.isEmpty() && superProperties.isEmpty()) {
            res.add(OWL.AnnotationProperty);
            return res;
        }
        for (Node r : ranges) {
            res.add(isDatatypeRange(r) ? OWL.DatatypeProperty : OWL.ObjectProperty);
        }
        for (Node s : superProperties) {
            res.addAll(getPropertyTypes(s, processed));
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

    private void fixProperties() { //todo: rewrite
        Set<Node> properties = getBaseGraph().find(Node.ANY, RDF_TYPE, RDF.Property.asNode()).mapWith(Triple::getSubject).toSet();
        // any standalone none-built-in predicates should be treated as rdf:Property also
        // (it would be replaced by one of the owl property type if possible while OWL=>OWL2 parser).
        Set<Node> standalone = getBaseGraph().find(Node.ANY, Node.ANY, Node.ANY)
                .mapWith(Triple::getPredicate)
                //.filterDrop(node -> getGraph().contains(node, Node.ANY, Node.ANY))
                .toSet();
        properties.addAll(standalone);

        // and add all subjects from triple with predicate rdfs:subPropertyOf
        Set<Node> superProperties = getBaseGraph().find(Node.ANY, RDFS.subPropertyOf.asNode(), Node.ANY)
                .mapWith(Triple::getSubject).toSet();
        properties.addAll(superProperties);
        properties.removeAll(BUILT_IN);

        for (Node prop : properties) {
            Set<Resource> types = getPropertyTypes(prop);
            if (types.isEmpty()) { //just ignore
                GraphTransformConfig.LOGGER.warn("Can't determine property type for " + prop);
                continue;
            }
            if (types.contains(OWL.DatatypeProperty) && types.contains(OWL.ObjectProperty)) { // todo: ignore
                GraphTransformConfig.LOGGER.warn("Property " + prop + " can't be data and object at the same time.");
                continue;
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
