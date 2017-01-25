package ru.avicomp.ontapi.jena.impl;

import java.util.Collections;
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
 * Base class for {@link OntAPropertyImpl}, {@link OntDPropertyImpl}, {@link OntClassImpl}, {@link OntDatatypeImpl}.
 * It is a storage for all entity factories.
 * The are two sets of factories: common and strict. The last one is to exclude illegal punnings from consideration.
 * <p>
 * The following punnings are considered as illegal:
 * - owl:Class <-> rdfs:Datatype
 * - owl:ObjectProperty <-> owl:DatatypeProperty
 * - owl:ObjectProperty <-> owl:AnnotationProperty
 * - owl:AnnotationProperty <-> owl:DatatypeProperty
 * <p>
 * Created by szuev on 03.11.2016.
 */
public abstract class OntEntityImpl extends OntObjectImpl implements OntEntity {

    public static EntityFactory classFactory = new EntityFactory(OntClassImpl.class, OWL.Class, BuiltIn.CLASSES);
    public static OntObjectFactory classFactoryStrict = classFactory.toStrict(RDFS.Datatype);

    public static EntityFactory datatypeFactory = new EntityFactory(OntDatatypeImpl.class, RDFS.Datatype, BuiltIn.DATATYPES);
    public static OntObjectFactory datatypeFactoryStrict = datatypeFactory.toStrict(OWL.Class);

    public static EntityFactory annotationPropertyFactory = new EntityFactory(OntAPropertyImpl.class,
            OWL.AnnotationProperty, BuiltIn.ANNOTATION_PROPERTIES);
    public static OntObjectFactory annotationPropertyFactoryStrict = annotationPropertyFactory.toStrict(OWL.ObjectProperty, OWL.DatatypeProperty);

    public static EntityFactory dataPropertyFactory = new EntityFactory(OntDPropertyImpl.class,
            OWL.DatatypeProperty, BuiltIn.DATA_PROPERTIES);
    public static OntObjectFactory dataPropertyFactoryStrict = dataPropertyFactory.toStrict(OWL.ObjectProperty, OWL.AnnotationProperty);

    public static EntityFactory objectPropertyFactory = new EntityFactory(OntOPEImpl.NamedProperty.class,
            OWL.ObjectProperty, BuiltIn.OBJECT_PROPERTIES);
    public static OntObjectFactory objectPropertyFactoryStrict = objectPropertyFactory.toStrict(OWL.DatatypeProperty, OWL.AnnotationProperty);

    public static OntObjectFactory individualFactory = new EntityFactory(OntIndividualImpl.NamedImpl.class, OWL.NamedIndividual);

    public static OntObjectFactory abstractEntityFactory = new MultiOntObjectFactory(OntFinder.TYPED,
            classFactory, datatypeFactory, annotationPropertyFactory, dataPropertyFactory, objectPropertyFactory, individualFactory);
    public static OntObjectFactory abstractEntityFactoryStrict = new MultiOntObjectFactory(OntFinder.TYPED,
            classFactoryStrict, datatypeFactoryStrict,
            annotationPropertyFactoryStrict, dataPropertyFactoryStrict, objectPropertyFactoryStrict, individualFactory);


    public OntEntityImpl(Node n, EnhGraph g) {
        super(n, g);
    }

    public abstract Class<? extends OntEntity> getActualClass();

    public static class EntityFactory extends CommonOntObjectFactory {

        public EntityFactory(Class<? extends OntObjectImpl> impl, Resource type, Set<Resource> builtInTypes) {
            super(createMaker(impl, type), createFinder(type), createFilter(type, builtInTypes));
        }

        private EntityFactory(Class<? extends OntObjectImpl> impl, Resource type) {
            this(impl, type, Collections.emptySet());
        }

        private static OntMaker createMaker(Class<? extends OntObjectImpl> impl, Resource type) {
            return new OntMaker.WithType(impl, type);
        }

        private static OntFinder createFinder(Resource type) {
            return new OntFinder.ByType(type);
        }

        private static OntFilter createFilter(Resource type, Set<Resource> exactMatches) {
            return createFilter(type).or(new OntFilter.OneOf(exactMatches));
        }

        private static OntFilter createFilter(Resource type) {
            return OntFilter.URI.and(new OntFilter.HasType(type));
        }

        private static OntFilter doesNotHaveTypes(Resource... types) {
            return OntFilter.TRUE.accumulate(Stream.of(types).map(OntFilter.HasType::new).map(OntFilter::negate).toArray(OntFilter[]::new));
        }

        public CommonOntObjectFactory toStrict(Resource... bannedTypes) {
            OntFilter illegalPunnings = doesNotHaveTypes(bannedTypes);
            return new CommonOntObjectFactory(
                    getMaker().restrict(illegalPunnings), getFinder(), getFilter().and(illegalPunnings));
        }

    }

}
