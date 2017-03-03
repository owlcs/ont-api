package ru.avicomp.ontapi.jena.impl;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;

import ru.avicomp.ontapi.jena.impl.configuration.*;
import ru.avicomp.ontapi.jena.utils.BuiltIn;
import ru.avicomp.ontapi.jena.vocabulary.OWL;

/**
 * This is an enumeration of all entity (configurable-)factories.
 * <p>
 * Created by szuev on 03.11.2016.
 */
public enum Entities implements Configurable<OntObjectFactory> {
    CLASS(OntClassImpl.class, OWL.Class) {
        @Override
        Stream<Resource> bannedTypes(Configurable.Mode mode) {
            switch (mode) {
                case STRICT:
                    return Stream.of(RDFS.Datatype);
                default:
                    return Stream.empty();
            }
        }

        @Override
        Set<Resource> builtInURIs() {
            return BuiltIn.CLASSES;
        }
    },
    DATATYPE(OntDatatypeImpl.class, RDFS.Datatype) {
        @Override
        Stream<Resource> bannedTypes(Configurable.Mode mode) {
            switch (mode) {
                case STRICT:
                    return Stream.of(OWL.Class);
                default:
                    return Stream.empty();
            }
        }

        @Override
        Set<Resource> builtInURIs() {
            return BuiltIn.DATATYPES;
        }
    },
    ANNOTATION_PROPERTY(OntAPropertyImpl.class, OWL.AnnotationProperty) {
        @Override
        Stream<Resource> bannedTypes(Configurable.Mode mode) {
            switch (mode) {
                case STRICT:
                    return Stream.of(OWL.ObjectProperty, OWL.DatatypeProperty);
                default:
                    return Stream.empty();
            }
        }

        @Override
        Set<Resource> builtInURIs() {
            return BuiltIn.ANNOTATION_PROPERTIES;
        }
    },
    DATA_PROPERTY(OntDPropertyImpl.class, OWL.DatatypeProperty) {
        @Override
        Stream<Resource> bannedTypes(Configurable.Mode mode) {
            switch (mode) {
                case STRICT:
                    return Stream.of(OWL.ObjectProperty, OWL.AnnotationProperty);
                default:
                    return Stream.empty();
            }
        }

        @Override
        Set<Resource> builtInURIs() {
            return BuiltIn.DATA_PROPERTIES;
        }
    },
    OBJECT_PROPERTY(OntOPEImpl.NamedPropertyImpl.class, OWL.ObjectProperty) {
        @Override
        Stream<Resource> bannedTypes(Configurable.Mode mode) {
            switch (mode) {
                default:
                    return Stream.empty();
                case STRICT:
                    return Stream.of(OWL.DatatypeProperty, OWL.AnnotationProperty);
            }
        }

        @Override
        Set<Resource> builtInURIs() {
            return BuiltIn.OBJECT_PROPERTIES;
        }
    },
    INDIVIDUAL(OntIndividualImpl.NamedImpl.class, OWL.NamedIndividual);

    public static final Configurable<MultiOntObjectFactory> ALL = m -> new MultiOntObjectFactory(OntFinder.TYPED,
            Stream.of(values()).map(c -> c.get(m)).toArray(OntObjectFactory[]::new));

    private final Class<? extends OntObjectImpl> impl;
    private final Resource type;

    Entities(Class<? extends OntObjectImpl> impl, Resource type) {
        this.impl = impl;
        this.type = type;
    }

    Stream<Resource> bannedTypes(Configurable.Mode mode) {
        return Stream.empty();
    }

    Set<Resource> builtInURIs() {
        return Collections.emptySet();
    }

    @Override
    public OntObjectFactory select(Mode m) {
        OntMaker maker = new OntMaker.WithType(impl, type);
        OntFinder finder = new OntFinder.ByType(type);
        OntFilter filter = OntFilter.URI.and(new OntFilter.HasType(type)).or(new OntFilter.OneOf(builtInURIs()));

        OntFilter illegalPunningsFilter = OntFilter.TRUE.accumulate(bannedTypes(m)
                .map(OntFilter.HasType::new).map(OntFilter::negate).toArray(OntFilter[]::new));

        return new CommonOntObjectFactory(maker.restrict(illegalPunningsFilter), finder, filter.and(illegalPunningsFilter));
    }
}
