package ru.avicomp.ontapi.jena.utils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.util.NodeUtils;
import org.apache.jena.util.iterator.ExtendedIterator;

import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.impl.OntIndividualImpl;
import ru.avicomp.ontapi.jena.model.OntIndividual;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

/**
 * helper to work with jena-models
 * <p>
 * Created by szuev on 20.10.2016.
 */
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


    /**
     * creates typed list: the anonymous section which is built using the same rules as true rdf:List,
     * i.e. by using rdf:first, rdf:rest and rdf:nil predicates.
     *
     * @param model   Model
     * @param type    Resource
     * @param members List of {@link RDFNode}'s
     * @return Anonymous resource - the header for typed list.
     */
    public static Resource createTypedList(Model model, Resource type, List<? extends RDFNode> members) {
        if (members.isEmpty()) return RDF.nil.inModel(model);
        Resource res = model.createResource();
        res.addProperty(RDF.type, type);
        res.addProperty(RDF.first, members.remove(0));
        res.addProperty(RDF.rest, createTypedList(model, type, members));
        return res;
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
     * Recursively gets the content of rdf:List as Stream of {@link RDFNode}s
     *
     * @param model   Model
     * @param anyList Resource. could be true rdf:List or typed List (which consists of rdf:first, rdf:rest and rdf:nil nodes)
     * @return Stream, could be empty if list is empty or specified resource is not a list.
     */
    public static Stream<RDFNode> rdfListContent(Model model, Resource anyList) {
        RDFNode first = null;
        RDFNode rest = null;
        Statement rdfFirst = model.getProperty(anyList, RDF.first);
        if (rdfFirst != null) first = rdfFirst.getObject();
        if (first == null) return Stream.empty();

        Statement rdfRest = model.getProperty(anyList, RDF.rest);
        if (rdfRest != null && !RDF.nil.equals(rdfRest.getObject())) {
            rest = rdfRest.getObject();
        }
        if (rest == null) {
            return Stream.of(first);
        }
        Stream<RDFNode> a = Stream.of(first);
        Stream<RDFNode> b = rest.isResource() ? rdfListContent(model, rest.asResource()) : Stream.of(rest);
        return Stream.concat(a, b);
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

    public static <T> Stream<T> asStream(ExtendedIterator<T> iterator) {
        return asStream(iterator, true, false);
    }

    public static <T> Stream<T> asStream(Iterator<T> iterator, boolean distinct, boolean parallel) {
        Iterable<T> iterable = () -> iterator;
        Stream<T> res = StreamSupport.stream(iterable.spliterator(), parallel);
        return distinct ? res.distinct() : res;
    }

    /**
     * gets all statements which have the specified resource as subject,
     * and all statements which have as subject the objects from the top level statements
     * and all other related statements recursively.
     * todo: replace it with not recursive method to avoid stack overflow,
     * it may happen in case of rdf:List with a large number of members (1000+).
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
