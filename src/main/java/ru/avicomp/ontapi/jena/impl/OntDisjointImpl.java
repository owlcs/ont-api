package ru.avicomp.ontapi.jena.impl;

import java.util.stream.Stream;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.impl.RDFListImpl;
import org.apache.jena.util.iterator.UniqueFilter;

import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.impl.configuration.*;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

/**
 * for anonymous owl:AllDisjointProperties,  owl:AllDisjointClasses, owl:AllDifferent
 * <p>
 * Created by @szuev on 15.11.2016.
 */
public abstract class OntDisjointImpl<O extends OntObject> extends OntObjectImpl implements OntDisjoint<O> {

    public static OntObjectFactory disjointClassesFactory = new CommonOntObjectFactory(new OntMaker.Default(ClassesImpl.class),
            new OntFinder.ByType(OWL.AllDisjointClasses), makeFilter(OWL.members, OntCE.class));
    public static OntObjectFactory differentIndividualsFactory = new CommonOntObjectFactory(new OntMaker.Default(IndividualsImpl.class),
            new OntFinder.ByType(OWL.AllDifferent), makeFilter(OWL.distinctMembers, OntIndividual.class));
    public static OntObjectFactory objectPropertiesFactory = new CommonOntObjectFactory(new OntMaker.Default(ObjectPropertiesImpl.class),
            new OntFinder.ByType(OWL.AllDisjointProperties), makeFilter(OWL.members, OntOPE.class));
    public static OntObjectFactory dataPropertiesFactory = new CommonOntObjectFactory(new OntMaker.Default(DataPropertiesImpl.class),
            new OntFinder.ByType(OWL.AllDisjointProperties), makeFilter(OWL.members, OntNDP.class));
    public static OntObjectFactory abstractPropertiesFactory = new MultiOntObjectFactory(new OntFinder.ByType(OWL.AllDisjointProperties),
            objectPropertiesFactory, dataPropertiesFactory);
    public static OntObjectFactory abstractDisjointFactory = new MultiOntObjectFactory(OntFinder.TYPED, disjointClassesFactory,
            differentIndividualsFactory, objectPropertiesFactory, dataPropertiesFactory);


    public OntDisjointImpl(Node n, EnhGraph m) {
        super(n, m);
    }

    protected abstract Property predicate();

    protected abstract Class<O> componentClass();

    public Stream<O> members() {
        return rdfList(predicate(), componentClass());
    }

    private static OntFilter makeFilter(Property predicate, Class<? extends OntObject> view) {
        return OntFilter.BLANK
                .and(new OntFilter.HasPredicate(predicate))
                .and((node, eg) -> !eg.asGraph()
                        .find(node, predicate.asNode(), Node.ANY)
                        .mapWith(Triple::getObject).filterKeep(n -> isListOf(n, eg, view))
                        .filterKeep(new UniqueFilter<>()).toList().isEmpty());
    }

    private static boolean isListOf(Node node, EnhGraph graph, Class<? extends OntObject> view) {
        if (!RDFListImpl.factory.canWrap(node, graph)) return false;
        if (view == null) return true;
        RDFList list = RDFListImpl.factory.wrap(node, graph).as(RDFList.class);
        for (RDFNode n : list.asJavaList()) {
            if (!n.canAs(view)) return false;
        }
        return true;
    }

    public static Classes createDisjointClasses(OntGraphModelImpl model, Stream<OntCE> classes) {
        OntJenaException.notNull(classes, "Null classes stream.");
        Resource res = model.createResource();
        res.addProperty(RDF.type, OWL.AllDisjointClasses);
        res.addProperty(OWL.members, model.createList(classes.iterator()));
        return model.getNodeAs(res.asNode(), Classes.class);
    }

    public static Individuals createDifferentIndividuals(OntGraphModelImpl model, Stream<OntIndividual> individuals) {
        OntJenaException.notNull(individuals, "Null individuals stream.");
        Resource res = model.createResource();
        res.addProperty(RDF.type, OWL.AllDifferent);
        res.addProperty(OWL.distinctMembers, model.createList(individuals.iterator()));
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
        protected Property predicate() {
            return OWL.members;
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
        protected Property predicate() {
            return OWL.distinctMembers;
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
        protected Property predicate() {
            return OWL.members;
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
