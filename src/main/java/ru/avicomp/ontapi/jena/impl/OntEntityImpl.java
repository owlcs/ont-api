package ru.avicomp.ontapi.jena.impl;

import java.util.Set;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDFS;

import ru.avicomp.ontapi.jena.JenaUtils;
import ru.avicomp.ontapi.jena.impl.configuration.*;
import ru.avicomp.ontapi.jena.model.OntEntity;

/**
 * Entity.
 * Created by szuev on 03.11.2016.
 */
public abstract class OntEntityImpl extends OntObjectImpl implements OntEntity {

    public static OntObjectFactory classFactory = new EntityFactory(OntClassImpl.class, OWL2.Class, JenaUtils.BUILT_IN_CLASSES);
    public static OntObjectFactory annotationPropertyFactory = new EntityFactory(OntAPropertyImpl.class, OWL2.AnnotationProperty, JenaUtils.BUILT_IN_ANNOTATION_PROPERTIES);
    public static OntObjectFactory dataPropertyFactory = new EntityFactory(OntDPropertyImpl.class, OWL2.DatatypeProperty, JenaUtils.BUILT_IN_DATA_PROPERTIES);
    public static OntObjectFactory objectPropertyFactory = new EntityFactory(OntOPEImpl.NamedProperty.class, OWL2.ObjectProperty, JenaUtils.BUILT_IN_OBJECT_PROPERTIES);
    public static OntObjectFactory datatypeFactory = new EntityFactory(OntDatatypeImpl.class, RDFS.Datatype, JenaUtils.BUILT_IN_DATATYPES);
    public static OntObjectFactory individualFactory = new EntityFactory(OntIndividualImpl.NamedImpl.class, OWL2.NamedIndividual);

    public static OntObjectFactory abstractEntityFactory =
            new MultiOntObjectFactory(OntFinder.TYPED,
                    classFactory, annotationPropertyFactory, dataPropertyFactory, objectPropertyFactory, datatypeFactory, individualFactory);

    public OntEntityImpl(Node n, EnhGraph g) {
        super(n, g);
    }

    public abstract Class<? extends OntEntity> getActualClass();

    private static class EntityFactory extends CommonOntObjectFactory {
        private EntityFactory(Class<? extends OntObjectImpl> impl, Resource type, Set<Resource> builtInTypes) {
            super(makeEntityMaker(impl, type), makeEntityFinder(type), makeEntityFilter(type, builtInTypes));
        }

        private EntityFactory(Class<? extends OntObjectImpl> impl, Resource type) {
            super(makeEntityMaker(impl, type), makeEntityFinder(type), makeEntityFilter(type));
        }

        private static OntMaker makeEntityMaker(Class<? extends OntObjectImpl> impl, Resource type) {
            return new OntMaker.WithType(impl, type);
        }

        private static OntFinder makeEntityFinder(Resource type) {
            return new OntFinder.ByType(type);
        }

        private static OntFilter makeEntityFilter(Resource type, Set<Resource> exactMatches) {
            return makeEntityFilter(type).or(new OntFilter.OneOf(exactMatches));
        }

        private static OntFilter makeEntityFilter(Resource type) {
            return OntFilter.URI.and(new OntFilter.HasType(type));
        }

    }

}
