package ru.avicomp.ontapi.jena.impl;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.impl.RDFListImpl;

import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.impl.configuration.*;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

/**
 * for anonymous owl:AllDisjointProperties,  owl:AllDisjointClasses, owl:AllDifferent sections.
 * <p>
 * Created by @szuev on 15.11.2016.
 */
public abstract class OntDisjointImpl<O extends OntObject> extends OntObjectImpl implements OntDisjoint<O> {
    public static final OntFinder PROPERTIES_FINDER = new OntFinder.ByType(OWL.AllDisjointProperties);

    public static Configurable<OntObjectFactory> disjointClassesFactory =
            createFactory(ClassesImpl.class, OWL.AllDisjointClasses, OntCEImpl.abstractCEFactory, true, OWL.members);

    public static Configurable<OntObjectFactory> differentIndividualsFactory =
            createFactory(IndividualsImpl.class, OWL.AllDifferent, OntIndividualImpl.abstractIndividualFactory, true, OWL.members, OWL.distinctMembers);

    public static Configurable<OntObjectFactory> objectPropertiesFactory =
            createFactory(ObjectPropertiesImpl.class, OWL.AllDisjointProperties, OntPEImpl.abstractOPEFactory, false, OWL.members);

    public static Configurable<OntObjectFactory> dataPropertiesFactory =
            createFactory(DataPropertiesImpl.class, OWL.AllDisjointProperties, OntEntityImpl.dataPropertyFactory, false, OWL.members);

    public static Configurable<MultiOntObjectFactory> abstractPropertiesFactory =
            createMultiFactory(PROPERTIES_FINDER, objectPropertiesFactory, dataPropertiesFactory);
    public static Configurable<MultiOntObjectFactory> abstractDisjointFactory =
            createMultiFactory(OntFinder.TYPED, abstractPropertiesFactory, disjointClassesFactory, differentIndividualsFactory);


    public OntDisjointImpl(Node n, EnhGraph m) {
        super(n, m);
    }

    protected abstract Stream<Property> predicates();

    protected abstract Class<O> componentClass();

    public Stream<O> members() {
        return predicates().map(p -> rdfList(p, componentClass())).flatMap(Function.identity());
    }

    private static Configurable<OntObjectFactory> createFactory(Class<? extends OntDisjointImpl> impl,
                                                                Resource type,
                                                                Configurable<? extends OntObjectFactory> memberFactory,
                                                                boolean allowEmptyList,
                                                                Property... predicates) {
        OntMaker maker = new OntMaker.WithType(impl, type);
        OntFinder finder = new OntFinder.ByType(type);
        OntFilter filter = OntFilter.BLANK.and(new OntFilter.HasType(type));
        return m -> new CommonOntObjectFactory(maker, finder, filter
                .and(getHasPredicatesFilter(predicates))
                .and(getHasMembersOfFilter(memberFactory.get(m), allowEmptyList, predicates)));
    }

    private static OntFilter getHasPredicatesFilter(Property... predicates) {
        OntFilter res = OntFilter.FALSE;
        for (Property p : predicates) {
            res = res.or(new OntFilter.HasPredicate(p));
        }
        return res;
    }

    private static OntFilter getHasMembersOfFilter(OntObjectFactory memberFactory, boolean allowEmptyList, Property... predicates) {
        return (node, eg) -> listRoots(node, eg.asGraph(), predicates).anyMatch(n -> testList(n, eg, memberFactory, allowEmptyList));
    }

    private static Stream<Node> listRoots(Node node, Graph graph, Property... predicates) {
        return Stream.of(predicates)
                .map(predicate -> Iter.asStream(graph.find(node, predicate.asNode(), Node.ANY).mapWith(Triple::getObject)))
                .flatMap(Function.identity());
    }

    private static boolean testList(Node node, EnhGraph graph, OntObjectFactory factory, boolean allowEmptyList) {
        if (!RDFListImpl.factory.canWrap(node, graph)) return false;
        if (factory == null) return true;
        List<RDFNode> list = RDFListImpl.factory.wrap(node, graph).as(RDFList.class).asJavaList();
        return (list.isEmpty() && allowEmptyList) || list.stream().map(RDFNode::asNode).anyMatch(n -> factory.canWrap(n, graph));
    }

    public static Classes createDisjointClasses(OntGraphModelImpl model, Stream<OntCE> classes) {
        OntJenaException.notNull(classes, "Null classes stream.");
        Resource res = model.createResource();
        res.addProperty(RDF.type, OWL.AllDisjointClasses);
        res.addProperty(OWL.members, model.createList(classes.iterator()));
        return model.getNodeAs(res.asNode(), Classes.class);
    }

    /**
     * Creates blank node "_:x rdf:type owl:AllDifferent. _:x owl:members (a1 … an)."
     * <p>
     * Note: the predicate is "owl:members", not "owl:distinctMembers" (but the last one is correct also)
     * see <a href='https://www.w3.org/TR/owl2-quick-reference/#Additional_Vocabulary_in_OWL_2_RDF_Syntax'>4.2 Additional Vocabulary in OWL 2 RDF Syntax</a>
     *
     * @param model       {@link OntGraphModelImpl}
     * @param individuals stream of {@link OntIndividual}
     * @return {@link ru.avicomp.ontapi.jena.model.OntDisjoint.Individuals}
     */
    public static Individuals createDifferentIndividuals(OntGraphModelImpl model, Stream<OntIndividual> individuals) {
        OntJenaException.notNull(individuals, "Null individuals stream.");
        Resource res = model.createResource();
        res.addProperty(RDF.type, OWL.AllDifferent);
        res.addProperty(OWL.members, model.createList(individuals.iterator()));
        return model.getNodeAs(res.asNode(), Individuals.class);
    }

    public static ObjectProperties createDisjointObjectProperties(OntGraphModelImpl model, Stream<OntOPE> properties) {
        OntJenaException.notNull(properties, "Null properties stream.");
        Resource res = model.createResource();
        res.addProperty(RDF.type, OWL.AllDisjointProperties);
        res.addProperty(OWL.members, model.createList(properties.iterator()));
        return model.getNodeAs(res.asNode(), ObjectProperties.class);
    }

    public static DataProperties createDisjointDataProperties(OntGraphModelImpl model, Stream<OntNDP> properties) {
        OntJenaException.notNull(properties, "Null properties stream.");
        Resource res = model.createResource();
        res.addProperty(RDF.type, OWL.AllDisjointProperties);
        res.addProperty(OWL.members, model.createList(properties.iterator()));
        return model.getNodeAs(res.asNode(), DataProperties.class);
    }

    public static class ClassesImpl extends OntDisjointImpl<OntCE> implements Classes {
        public ClassesImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        protected Stream<Property> predicates() {
            return Stream.of(OWL.members);
        }

        @Override
        protected Class<OntCE> componentClass() {
            return OntCE.class;
        }
    }

    public static class IndividualsImpl extends OntDisjointImpl<OntIndividual> implements Individuals {
        public IndividualsImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        protected Stream<Property> predicates() {
            return Stream.of(OWL.members, OWL.distinctMembers);
        }

        @Override
        protected Class<OntIndividual> componentClass() {
            return OntIndividual.class;
        }
    }

    public abstract static class PropertiesImpl<P extends OntPE> extends OntDisjointImpl<P> implements Properties<P> {

        public PropertiesImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        protected Stream<Property> predicates() {
            return Stream.of(OWL.members);
        }
    }

    public static class ObjectPropertiesImpl extends PropertiesImpl<OntOPE> implements ObjectProperties {
        public ObjectPropertiesImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        protected Class<OntOPE> componentClass() {
            return OntOPE.class;
        }
    }

    public static class DataPropertiesImpl extends PropertiesImpl<OntNDP> implements DataProperties {
        public DataPropertiesImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        protected Class<OntNDP> componentClass() {
            return OntNDP.class;
        }
    }
}
