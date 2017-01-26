package ru.avicomp.ontapi.jena.impl.configuration;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.jena.enhanced.Personality;
import org.apache.jena.rdf.model.RDFNode;

import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.model.OntObject;

/**
 *
 * This is extended {@link Function} which is used as a container for an object (a factory or a part of factory)
 * for simplification the code and to have a possibility to change
 * the default behaviour easily during the initialization of personalities.
 *
 * <p>
 * Currently the are only two modes: {@link Mode#LAX} and {@link Mode#STRICT}.
 * The first one is a lax way. Any owl-entity could have more than one types simultaneously, there is no any restrictions.
 * The second one is to exclude so called 'illegal punnings' (property and class/datatype intersections) from consideration,
 * i.e. the interpretation of such things as owl-entity ({@link ru.avicomp.ontapi.jena.model.OntEntity}) is prohibited,
 * but they still can be treated as any other objects ({@link OntObject})
 * <p>
 * Created by @szuev on 21.01.2017.
 */
public interface Configurable<T> extends Function<Configurable.Mode, T> {

    default T get(Mode m) {
        return OntJenaException.notNull(apply(OntJenaException.notNull(m, "Null mode.")), "Null result for mode " + m + ".");
    }

    enum Mode {
        /**
         * The following punnings are considered as illegal and will be excluded:
         * - owl:Class <-> rdfs:Datatype
         * - owl:ObjectProperty <-> owl:DatatypeProperty
         * - owl:ObjectProperty <-> owl:AnnotationProperty
         * - owl:AnnotationProperty <-> owl:DatatypeProperty
         */
        STRICT,
        /**
         * Don't care about illegal punnings.
         */
        LAX,
    }

    @SafeVarargs
    static Configurable<MultiOntObjectFactory> create(OntFinder finder, Configurable<? extends OntObjectFactory>... factories) {
        return mode -> new MultiOntObjectFactory(finder, Stream.of(factories).map(c -> c.get(mode)).toArray(OntObjectFactory[]::new));
    }

    @SafeVarargs
    static Configurable<MultiOntObjectFactory> append(Configurable<MultiOntObjectFactory> multi, Configurable<? extends OntObjectFactory>... factories) {
        return mode -> multi.get(mode).concat(Stream.of(factories).map(c -> c.get(mode)).toArray(OntObjectFactory[]::new));
    }

    static Configurable<MultiOntObjectFactory> concat(Configurable<? extends OntObjectFactory> first, Configurable<? extends OntObjectFactory> rest) {
        return mode -> new MultiOntObjectFactory(Stream.of(first, rest).map(c -> c.get(mode)).toArray(OntObjectFactory[]::new));
    }

    class PersonalityBuilder {
        private final Map<Class<? extends OntObject>, Configurable<? extends OntObjectFactory>> map = new LinkedHashMap<>();

        public PersonalityBuilder add(Class<? extends OntObject> key, Configurable<? extends OntObjectFactory> value) {
            map.put(key, value);
            return this;
        }

        public OntPersonality build(Personality<RDFNode> init, Mode mode) {
            OntJenaException.notNull(mode, "Null mode.");
            OntPersonality res = new OntPersonality(init == null ? new Personality<>() : init);
            map.forEach((k, v) -> res.register(k, v.get(mode)));
            return res;
        }
    }

}
