package ru.avicomp.ontapi.jena.impl;

import java.util.Set;
import java.util.stream.Stream;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;

import ru.avicomp.ontapi.jena.impl.configuration.*;
import ru.avicomp.ontapi.jena.model.OntEntity;
import ru.avicomp.ontapi.jena.utils.BuiltIn;
import ru.avicomp.ontapi.jena.vocabulary.OWL;

/**
 * Base class for {@link OntAPropertyImpl}, {@link OntDPropertyImpl}, {@link OntClassImpl}, {@link OntDatatypeImpl}
 * and storage for all entity factories.
 *
 * The following punnings are considered as illegal:
 * - owl:Class <-> rdfs:Datatype
 * - owl:ObjectProperty <-> owl:DatatypeProperty
 * - owl:ObjectProperty <-> owl:AnnotationProperty
 * - owl:AnnotationProperty <-> owl:DatatypeProperty
 * if {@link OntModelConfig#excludeIllegalPunnings()} returns true than these punnings will be excluded from parsing.
 * <p>
 * Created by szuev on 03.11.2016.
 */
public abstract class OntEntityImpl extends OntObjectImpl implements OntEntity {

    public static OntObjectFactory classFactory = new EntityFactory(OntClassImpl.class, OWL.Class, BuiltIn.CLASSES, RDFS.Datatype);
    public static OntObjectFactory annotationPropertyFactory = new EntityFactory(OntAPropertyImpl.class,
            OWL.AnnotationProperty, BuiltIn.ANNOTATION_PROPERTIES, OWL.ObjectProperty, OWL.DatatypeProperty);
    public static OntObjectFactory dataPropertyFactory = new EntityFactory(OntDPropertyImpl.class,
            OWL.DatatypeProperty, BuiltIn.DATA_PROPERTIES, OWL.ObjectProperty, OWL.AnnotationProperty);
    public static OntObjectFactory objectPropertyFactory = new EntityFactory(OntOPEImpl.NamedProperty.class,
            OWL.ObjectProperty, BuiltIn.OBJECT_PROPERTIES, OWL.DatatypeProperty, OWL.AnnotationProperty);
    public static OntObjectFactory datatypeFactory = new EntityFactory(OntDatatypeImpl.class, RDFS.Datatype, BuiltIn.DATATYPES, OWL.Class);
    public static OntObjectFactory individualFactory = new EntityFactory(OntIndividualImpl.NamedImpl.class, OWL.NamedIndividual);

    public static OntObjectFactory abstractEntityFactory =
            new MultiOntObjectFactory(OntFinder.TYPED,
                    classFactory, annotationPropertyFactory, dataPropertyFactory, objectPropertyFactory, datatypeFactory, individualFactory);

    public OntEntityImpl(Node n, EnhGraph g) {
        super(n, g);
    }

    public abstract Class<? extends OntEntity> getActualClass();

    private static class EntityFactory extends CommonOntObjectFactory {

        private EntityFactory(Class<? extends OntObjectImpl> impl, Resource type, Set<Resource> builtInTypes, Resource... bannedTypes) {
            super(createMaker(impl, type, bannedTypes), createFinder(type), createFilter(type, builtInTypes, bannedTypes));
        }

        private EntityFactory(Class<? extends OntObjectImpl> impl, Resource type) {
            super(createMaker(impl, type), createFinder(type), createFilter(type));
        }

        private static OntMaker createMaker(Class<? extends OntObjectImpl> impl, Resource type, Resource... bannedTypes) {
            return new OntMaker.WithType(impl, type).restrict(doesNotHaveTypes(bannedTypes));
        }

        private static OntFinder createFinder(Resource type) {
            return new OntFinder.ByType(type);
        }

        private static OntFilter createFilter(Resource type, Set<Resource> exactMatches, Resource... bannedTypes) {
            return createFilter(type, bannedTypes).or(new OntFilter.OneOf(exactMatches));
        }

        private static OntFilter createFilter(Resource type, Resource... bannedTypes) {
            return OntFilter.URI.and(new OntFilter.HasType(type)).and(doesNotHaveTypes(bannedTypes));
        }

        private static OntFilter doesNotHaveTypes(Resource... types) {
            OntFilter[] filters = OntModelConfig.excludeIllegalPunnings() ?
                    Stream.of(types).map(OntFilter.HasType::new).map(OntFilter::negate).toArray(OntFilter[]::new) :
                    new OntFilter[0];
            return OntFilter.TRUE.accumulate(filters);
        }
    }

}
