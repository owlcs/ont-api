package ru.avicomp.ontapi.jena.utils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.RDFListImpl;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.util.NodeUtils;
import org.apache.jena.vocabulary.RDFS;

import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.impl.OntIndividualImpl;
import ru.avicomp.ontapi.jena.model.OntIndividual;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

/**
 * helper to work with jena-models
 * <p>
 * Created by szuev on 20.10.2016.
 */
@SuppressWarnings("WeakerAccess")
public class Models {
    public static final Comparator<RDFNode> RDF_NODE_COMPARATOR = (r1, r2) -> NodeUtils.compareRDFTerms(r1.asNode(), r2.asNode());
    public static final Comparator<Statement> STATEMENT_COMPARATOR = Comparator
            .comparing(Statement::getSubject, RDF_NODE_COMPARATOR)
            .thenComparing(Statement::getPredicate, RDF_NODE_COMPARATOR)
            .thenComparing(Statement::getObject, RDF_NODE_COMPARATOR);
    public static final RDFNode BLANK = new ResourceImpl();
    public static final Comparator<Statement> STATEMENT_COMPARATOR_IGNORE_BLANK = Comparator
            .comparing((Function<Statement, RDFNode>) s -> s.getSubject().isAnon() ? BLANK : s.getSubject(), RDF_NODE_COMPARATOR)
            .thenComparing(s -> s.getPredicate().isAnon() ? BLANK : s.getPredicate(), RDF_NODE_COMPARATOR)
            .thenComparing(s -> s.getObject().isAnon() ? BLANK : s.getObject(), RDF_NODE_COMPARATOR);

    public static final Literal TRUE = ResourceFactory.createTypedLiteral(Boolean.TRUE);


    /**
     * creates typed list: the anonymous section which is built using the same rules as true rdf:List {@link RDFListImpl},
     * i.e. by using rdf:first, rdf:rest and rdf:nil predicates.
     *
     * @param model   Model
     * @param type    Resource
     * @param members List of {@link RDFNode}'s
     * @return Anonymous resource - the header for typed list.
     */
    public static Resource createTypedList(Model model, Resource type, List<? extends RDFNode> members) {
        /*if (members.isEmpty()) return RDF.nil.inModel(model);
        Resource res = model.createResource();
        res.addProperty(RDF.type, type);
        res.addProperty(RDF.first, members.remove(0));
        res.addProperty(RDF.rest, createTypedList(model, type, members));
        return res;*/
        RDFList tmp = new RDFListImpl(Node.ANY, (EnhGraph) model) {

            @Override
            public Resource listType() {
                return type;
            }

            @Override
            public RDFList copy() {
                return copy(members.iterator());
            }
        };
        return tmp.copy();
    }

    /**
     * Builds typed list from Stream of RDFNode's
     *
     * @param model   Model
     * @param type    type of list to create
     * @param members Stream of members
     * @return the head of created list.
     */
    public static Resource createTypedList(Model model, Resource type, Stream<? extends RDFNode> members) {
        return createTypedList(model, type, members.collect(Collectors.toList()));
    }

    /**
     * @param model     Model
     * @param candidate Resource to test
     * @return true if specified resource is a member of some rdf:List
     */
    public static boolean isInList(Model model, Resource candidate) {
        return model.contains(null, RDF.first, candidate);
    }

    /**
     * converts rdf-node to anonymous individual.
     * The result anonymous individual could be true (instance of some owl class) or fake (any blank node).
     *
     * @param node {@link RDFNode}
     * @return {@link OntIndividual.Anonymous}
     * @throws OntJenaException if node can be present as anonymous individual
     */
    public static OntIndividual.Anonymous asAnonymousIndividual(RDFNode node) {
        if (OntJenaException.notNull(node, "Null node.").canAs(OntIndividual.Anonymous.class))
            return node.as(OntIndividual.Anonymous.class);
        if (node.isAnon()) {
            return new OntIndividualImpl.AnonymousImpl(node.asNode(), (EnhGraph) node.getModel());
        }
        throw new OntJenaException.Conversion(node + " could not be " + OntIndividual.Anonymous.class);
    }

    /**
     * replaces namespaces map with new one.
     *
     * @param mapping  {@link PrefixMapping} object
     * @param prefixes Map of new prefixes to set.
     * @return Map of previous prefixes.
     */
    public static Map<String, String> setNsPrefixes(PrefixMapping mapping, Map<String, String> prefixes) {
        Map<String, String> init = mapping.getNsPrefixMap();
        init.keySet().forEach(mapping::removeNsPrefix);
        prefixes.forEach((p, u) -> mapping.setNsPrefix(p.replaceAll(":$", ""), u));
        return init;
    }

    /**
     * gets 'punnings' for rdf:Property types (owl:AnnotationProperty, owl:DatatypeProperty, owl:ObjectProperty)
     * it works only with explicit declarations.
     *
     * @param m Model
     * @return Set of resources
     */
    public static Set<Resource> getPropertyPunnings(Model m) {
        Set<Resource> annotationProperties = m.listStatements(null, RDF.type, OWL.AnnotationProperty).mapWith(Statement::getSubject).toSet();
        Set<Resource> objectProperties = m.listStatements(null, RDF.type, OWL.ObjectProperty).mapWith(Statement::getSubject).toSet();
        Set<Resource> datatypeProperties = m.listStatements(null, RDF.type, OWL.DatatypeProperty).mapWith(Statement::getSubject).toSet();
        return unionOfIntersections(annotationProperties, objectProperties, datatypeProperties);
    }

    /**
     * gets 'punnings' for rdfs:Class types (owl:Class and rdfs:Datatype)
     *
     * @param m Model
     * @return Set of resources
     */
    public static Set<Resource> getClassPunnings(Model m) {
        Set<Resource> classes = m.listStatements(null, RDF.type, OWL.Class).mapWith(Statement::getSubject).toSet();
        Set<Resource> datatypes = m.listStatements(null, RDF.type, RDFS.Datatype).mapWith(Statement::getSubject).toSet();
        return unionOfIntersections(classes, datatypes);
    }

    /**
     * gets the set of 'illegal punnings' by their explicit declaration.
     * It seems the following punnings are consider by OWL-API as illegal:
     * - owl:Class <-> rdfs:Datatype
     * - owl:ObjectProperty <-> owl:DatatypeProperty
     * - owl:ObjectProperty <-> owl:AnnotationProperty
     * - owl:AnnotationProperty <-> owl:DatatypeProperty
     *
     * @param m Model
     * @return Set of illegal punnings
     */
    public static Set<Resource> getIllegalPunnings(Model m) {
        Set<Resource> res = new HashSet<>(getPropertyPunnings(m));
        res.addAll(getClassPunnings(m));
        return res;
    }

    @SafeVarargs
    private static <T> Set<T> unionOfIntersections(Collection<T>... collections) {
        Stream<T> res = Stream.empty();
        for (int i = 0; i < collections.length; i++) {
            Set<T> intersection = new HashSet<>(collections[i]);
            intersection.retainAll(collections[i < collections.length - 1 ? i + 1 : 0]);
            res = Stream.concat(res, intersection.stream());
        }
        return res.collect(Collectors.toSet());
    }

    /**
     * gets all statements which have the specified resource as subject,
     * then and all statements which have as subject all objects from the top level statements collected in the previous step.
     * then and all other related statements recursively.
     * Note: rdf:List may content a large number of members (1000+).
     *
     * @param inModel Resource with associated model inside.
     * @return the Set of {@link Statement}
     */
    public static Set<Statement> getAssociatedStatements(Resource inModel) {
        Set<Statement> res = new HashSet<>();
        calcAssociatedStatements(inModel, res);
        return res;
    }

    private static void calcAssociatedStatements(Resource root, Set<Statement> res) {
        if (root.canAs(RDFList.class)) {
            RDFListImpl list = (RDFListImpl) root.as(RDFList.class);
            list.collectStatements().forEach(statement -> {
                res.add(statement);
                if (!list.listFirst().equals(statement.getPredicate())) return;
                RDFNode obj = statement.getObject();
                if (obj.isAnon())
                    calcAssociatedStatements(obj.asResource(), res);
            });
            return;
        }
        root.listProperties().forEachRemaining(statement -> {
            RDFNode obj = statement.getObject();
            if (res.stream().anyMatch(s -> obj.equals(s.getSubject()))) // to avoid cycles
                return;
            res.add(statement);
            if (obj.isAnon()) {
                calcAssociatedStatements(obj.asResource(), res);
            }
        });
    }

}
