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
 * This is a base class for implementations {@link OntAPropertyImpl}, {@link OntDPropertyImpl}, {@link OntClassImpl}, {@link OntDatatypeImpl}.
 * and a storage for all entity factories.

 * Created by szuev on 03.11.2016.
 */
public abstract class OntEntityImpl extends OntObjectImpl implements OntEntity {

    public static Configurable<OntObjectFactory> classFactory = createFactory(OntClassImpl.class, OWL.Class, BuiltIn.CLASSES, RDFS.Datatype);
    public static Configurable<OntObjectFactory> datatypeFactory = createFactory(OntDatatypeImpl.class, RDFS.Datatype, BuiltIn.DATATYPES, OWL.Class);

    public static Configurable<OntObjectFactory> annotationPropertyFactory = createFactory(OntAPropertyImpl.class,
            OWL.AnnotationProperty, BuiltIn.ANNOTATION_PROPERTIES, OWL.ObjectProperty, OWL.DatatypeProperty);
    public static Configurable<OntObjectFactory> dataPropertyFactory = createFactory(OntDPropertyImpl.class,
            OWL.DatatypeProperty, BuiltIn.DATA_PROPERTIES, OWL.ObjectProperty, OWL.AnnotationProperty);
    public static Configurable<OntObjectFactory> objectPropertyFactory = createFactory(OntOPEImpl.NamedPropertyImpl.class,
            OWL.ObjectProperty, BuiltIn.OBJECT_PROPERTIES, OWL.DatatypeProperty, OWL.AnnotationProperty);

    public static Configurable<OntObjectFactory> individualFactory = createFactory(OntIndividualImpl.NamedImpl.class, OWL.NamedIndividual, null);

    public static Configurable<MultiOntObjectFactory> abstractEntityFactory = createMultiFactory(OntFinder.TYPED,
            classFactory, datatypeFactory, annotationPropertyFactory, dataPropertyFactory, objectPropertyFactory, individualFactory);

    public OntEntityImpl(Node n, EnhGraph g) {
        super(n, g);
    }

    public abstract Class<? extends OntEntity> getActualClass();

    private static Configurable<OntObjectFactory> createFactory(Class<? extends OntObjectImpl> impl, Resource type, Set<Resource> builtInURISet, Resource... bannedTypes) {
        OntMaker maker = new OntMaker.WithType(impl, type);
        OntFinder finder = new OntFinder.ByType(type);
        OntFilter filter = OntFilter.URI.and(new OntFilter.HasType(type)).or(new OntFilter.OneOf(builtInURISet));
        OntFilter illegalPunningsFilter = OntFilter.TRUE.accumulate(Stream.of(bannedTypes).map(OntFilter.HasType::new).map(OntFilter::negate).toArray(OntFilter[]::new));

        Configurable<OntMaker> optMaker = m -> Configurable.Mode.LAX.equals(m) ? maker : maker.restrict(illegalPunningsFilter);
        Configurable<OntFilter> optFilter = m -> Configurable.Mode.LAX.equals(m) ? filter : filter.and(illegalPunningsFilter);
        return mode -> new CommonOntObjectFactory(optMaker.get(mode), finder, optFilter.get(mode));
    }
}
