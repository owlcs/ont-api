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
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;

import ru.avicomp.ontapi.jena.impl.configuration.*;
import ru.avicomp.ontapi.jena.model.*;

/**
 * for anonymous owl:AllDisjointProperties,  owl:AllDisjointClasses, owl:AllDifferent
 * <p>
 * Created by @szuev on 15.11.2016.
 */
public abstract class OntDisjointImpl<O extends OntObject> extends OntObjectImpl implements OntDisjoint<O> {

    public static OntObjectFactory disjointClassesFactory = new CommonOntObjectFactory(new OntMaker.Default(ClassesImpl.class),
            new OntFinder.ByType(OWL2.AllDisjointClasses), makeFilter(OWL2.members, OntCE.class));
    public static OntObjectFactory differentIndividualsFactory = new CommonOntObjectFactory(new OntMaker.Default(IndividualsImpl.class),
            new OntFinder.ByType(OWL2.AllDifferent), makeFilter(OWL2.distinctMembers, OntIndividual.class));
    public static OntObjectFactory objectPropertiesFactory = new CommonOntObjectFactory(new OntMaker.Default(ObjectPropertiesImpl.class),
            new OntFinder.ByType(OWL2.AllDisjointProperties), makeFilter(OWL2.members, OntOPE.class));
    public static OntObjectFactory dataPropertiesFactory = new CommonOntObjectFactory(new OntMaker.Default(DataPropertiesImpl.class),
            new OntFinder.ByType(OWL2.AllDisjointProperties), makeFilter(OWL2.members, OntNDP.class));
    public static OntObjectFactory abstractPropertiesFactory = new MultiOntObjectFactory(new OntFinder.ByType(OWL2.AllDisjointProperties),
            objectPropertiesFactory, dataPropertiesFactory);
    public static OntObjectFactory abstractDisjointFactory = new MultiOntObjectFactory(OntFinder.TYPED, disjointClassesFactory,
            differentIndividualsFactory, objectPropertiesFactory, dataPropertiesFactory);


    public OntDisjointImpl(Node n, EnhGraph m) {
        super(n, m);
    }

    protected abstract Property predicate();

    protected abstract Class<O> componentClass();

    public Stream<O> members() {
        return listOf(predicate(), componentClass());
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
        Resource res = model.createResource();
        res.addProperty(RDF.type, OWL2.AllDisjointClasses);
        res.addProperty(OWL2.members, model.createList(classes.iterator()));
        return model.getNodeAs(res.asNode(), Classes.class);
    }

    public static Individuals createDifferentIndividuals(OntGraphModelImpl model, Stream<OntIndividual> individuals) {
        Resource res = model.createResource();
        res.addProperty(RDF.type, OWL2.AllDifferent);
        res.addProperty(OWL2.distinctMembers, model.createList(individuals.iterator()));
        return model.getNodeAs(res.asNode(), Individuals.class);
    }

    public static ObjectProperties createDisjointObjectProperties(OntGraphModelImpl model, Stream<OntOPE> properties) {
        Resource res = model.createResource();
        res.addProperty(RDF.type, OWL2.AllDisjointProperties);
        res.addProperty(OWL2.members, model.createList(properties.iterator()));
        return model.getNodeAs(res.asNode(), ObjectProperties.class);
    }

    public static DataProperties createDisjointDataProperties(OntGraphModelImpl model, Stream<OntNDP> properties) {
        Resource res = model.createResource();
        res.addProperty(RDF.type, OWL2.AllDisjointProperties);
        res.addProperty(OWL2.members, model.createList(properties.iterator()));
        return model.getNodeAs(res.asNode(), DataProperties.class);
    }

    public static class ClassesImpl extends OntDisjointImpl<OntCE> implements Classes {
        public ClassesImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        protected Property predicate() {
            return OWL2.members;
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
            return OWL2.distinctMembers;
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
            return OWL2.members;
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
