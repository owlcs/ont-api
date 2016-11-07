package ru.avicomp.ontapi.jena.impl;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.enhanced.EnhNode;
import org.apache.jena.enhanced.Implementation;
import org.apache.jena.enhanced.Personality;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.*;
import org.apache.jena.vocabulary.RDF;

import ru.avicomp.ontapi.OntException;
import ru.avicomp.ontapi.jena.model.*;

/**
 * TODO:
 * personalities here
 * <p>
 * Created by @szuev on 04.11.2016.
 */
public class OntConfiguration {

    private static final Personality<RDFNode> PERSONALITY = new Personality<RDFNode>()
            // standard resources:
            .add(Resource.class, ResourceImpl.factory)
            .add(Property.class, PropertyImpl.factory)
            .add(Literal.class, LiteralImpl.factory)
            .add(Container.class, ResourceImpl.factory)
            .add(Alt.class, AltImpl.factory)
            .add(Bag.class, BagImpl.factory)
            .add(Seq.class, SeqImpl.factory)
            .add(ReifiedStatement.class, ReifiedStatementImpl.reifiedStatementFactory)
            .add(RDFList.class, RDFListImpl.factory)
            .add(RDFNode.class, ResourceImpl.rdfNodeFactory)

            .add(OntObject.class, OntObjectImpl.factory)
            .add(OntClassEntity.class, OntEntityImpl.classFactory)
            .add(OntAPEntity.class, OntEntityImpl.aPFactory)
            .add(OntDPEntity.class, OntEntityImpl.dPFactory)
            .add(OntOPEntity.class, OntEntityImpl.oPFactory)
            .add(OntDatatypeEntity.class, OntEntityImpl.datatypeFactory)
            .add(OntIndividualEntity.class, OntEntityImpl.individualFactory)
            .add(OntEntity.class, new OntMultiObjectFactory(OntEntityImpl.classFactory, OntEntityImpl.abstractEntityFactory));
    // todo: replace with out resources. ontology additions
/*            .add(OntResource.class, OntResourceImpl.factory)
            .add(Ontology.class, OntologyImpl.factory)
            .add(OntClass.class, org.apache.jena.ontology.impl.OntClassImpl.factory)
            .add(EnumeratedClass.class, EnumeratedClassImpl.factory)
            .add(IntersectionClass.class, IntersectionClassImpl.factory)
            .add(UnionClass.class, UnionClassImpl.factory)
            .add(ComplementClass.class, ComplementClassImpl.factory)
            .add(DataRange.class, DataRangeImpl.factory)

            .add(Restriction.class, RestrictionImpl.factory)
            .add(HasValueRestriction.class, HasValueRestrictionImpl.factory)
            .add(AllValuesFromRestriction.class, AllValuesFromRestrictionImpl.factory)
            .add(SomeValuesFromRestriction.class, SomeValuesFromRestrictionImpl.factory)
            .add(CardinalityRestriction.class, CardinalityRestrictionImpl.factory)
            .add(MinCardinalityRestriction.class, MinCardinalityRestrictionImpl.factory)
            .add(MaxCardinalityRestriction.class, MaxCardinalityRestrictionImpl.factory)
            .add(QualifiedRestriction.class, QualifiedRestrictionImpl.factory)
            .add(MinCardinalityQRestriction.class, MinCardinalityQRestrictionImpl.factory)
            .add(MaxCardinalityQRestriction.class, MaxCardinalityQRestrictionImpl.factory)
            .add(CardinalityQRestriction.class, CardinalityQRestrictionImpl.factory)

            .add(OntProperty.class, OntPropertyImpl.factory)
            .add(ObjectProperty.class, ObjectPropertyImpl.factory)
            .add(DatatypeProperty.class, DatatypePropertyImpl.factory)
            .add(TransitiveProperty.class, TransitivePropertyImpl.factory)
            .add(SymmetricProperty.class, SymmetricPropertyImpl.factory)
            .add(FunctionalProperty.class, FunctionalPropertyImpl.factory)
            .add(InverseFunctionalProperty.class, InverseFunctionalPropertyImpl.factory)
            .add(AllDifferent.class, AllDifferentImpl.factory)
            .add(Individual.class, IndividualImpl.factory)
            .add(AnnotationProperty.class, AnnotationPropertyImpl.factory);*/

    /**
     * register new OntObject if needed
     *
     * @param view    Interface
     * @param factory Factory to crete object
     */
    public static void register(Class<? extends OntObject> view, OntObjectFactory factory) {
        PERSONALITY.add(view, factory);
    }

    public static Personality<RDFNode> getPersonality() {
        return PERSONALITY;
    }

    /**
     * Created by @szuev on 03.11.2016.
     */
    public static abstract class OntObjectFactory extends Implementation {
        public EnhNode create(Node node, EnhGraph eg) {
            throw new OntException("Creation is not allowed: " + node);
        }

        public Stream<EnhNode> find(EnhGraph eg) {
            return Stream.empty();
        }
    }

    public static abstract class BaseOntObjectFactory extends OntObjectFactory {
        private final Class<? extends OntObjectImpl> impl;

        protected BaseOntObjectFactory(Class<? extends OntObjectImpl> impl) {
            this.impl = impl;
        }

        public EnhNode newInstance(Node node, EnhGraph eg) {
            try {
                return impl.getDeclaredConstructor(Node.class, EnhGraph.class).newInstance(node, eg);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new OntException(e);
            }
        }

        @Override
        public EnhNode wrap(Node node, EnhGraph eg) {
            if (!canWrap(node, eg))
                throw new OntException(String.format("Cannot convert node %s to %s", node, impl.getSimpleName()));
            return newInstance(node, eg);
        }
    }

    public static abstract class OntTypedObjectFactory extends BaseOntObjectFactory {
        protected final Node type;
        private static final Node RDF_TYPE = RDF.type.asNode();

        public OntTypedObjectFactory(Class<? extends OntObjectImpl> view, Resource type) {
            super(view);
            this.type = OntException.notNull(type, "Null type").asNode();
        }

        public EnhNode create(Node node, EnhGraph eg) {
            eg.asGraph().add(Triple.create(node, RDF_TYPE, type));
            return newInstance(node, eg);
        }

        @Override
        public Stream<EnhNode> find(EnhGraph eg) {
            return GraphModelImpl.asStream(eg.asGraph().find(Node.ANY, RDF_TYPE, type).
                    mapWith(Triple::getSubject).filterKeep(node -> canWrap(node, eg)).mapWith(n -> newInstance(n, eg)));
        }

        @Override
        public boolean canWrap(Node node, EnhGraph eg) {
            return eg.asGraph().contains(node, RDF_TYPE, type);
        }
    }

    public static class OntSimpleObjectFactory extends OntTypedObjectFactory {
        private final Boolean named;

        public OntSimpleObjectFactory(Class<? extends OntObjectImpl> view, Resource type, Boolean named) {
            super(view, type);
            this.named = named;
        }

        private boolean test(Node node) {
            return named == null || named && node.isURI() || !named && node.isBlank();
        }

        @Override
        public EnhNode create(Node node, EnhGraph eg) {
            if (!test(node)) {
                throw new OntException(String.format("Not %s node: %s", named ? "named" : "anonymous", node));
            }
            return super.create(node, eg);
        }

        @Override
        public Stream<EnhNode> find(EnhGraph eg) {
            return super.find(eg).filter(n -> test(n.asNode()));
        }

        @Override
        public boolean canWrap(Node node, EnhGraph eg) {
            return test(node) && super.canWrap(node, eg);
        }
    }

    public static class OntMultiObjectFactory extends OntObjectFactory {
        private final OntObjectFactory[] factories;

        public OntMultiObjectFactory(OntObjectFactory... factories) {
            this.factories = factories;
        }

        @Override
        public EnhNode wrap(Node node, EnhGraph eg) {
            for (OntObjectFactory f : factories) {
                if (f.canWrap(node, eg)) return f.wrap(node, eg);
            }
            throw new OntException("Can't wrap node " + node + ". Use direct factory");
        }

        @Override
        public boolean canWrap(Node node, EnhGraph eg) {
            for (OntObjectFactory f : factories) {
                if (f.canWrap(node, eg)) return true;
            }
            return false;
        }

        @Override
        public Stream<EnhNode> find(EnhGraph eg) {
            return Arrays.stream(factories).map(f -> f.find(eg)).flatMap(Function.identity());
        }
    }
}
