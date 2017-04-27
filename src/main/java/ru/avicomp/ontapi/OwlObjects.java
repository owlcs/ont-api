package ru.avicomp.ontapi;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Stream;

import org.semanticweb.owlapi.model.*;

/**
 * Helper to work with {@link OWLObject} (mostly for parsing {@link org.semanticweb.owlapi.model.OWLAxiom})
 * <p>
 * Created by @szuev on 08.02.2017.
 */
@SuppressWarnings("WeakerAccess")
public class OwlObjects {

    public static <O extends OWLObject> Stream<O> parseComponents(Class<O> view, HasComponents structure) {
        return structure.componentsWithoutAnnotations().map(o -> toStream(view, o)).flatMap(Function.identity());
    }

    public static <O extends OWLObject> Stream<O> parseAnnotations(Class<O> view, HasAnnotations structure) {
        return structure.annotations().map(o -> toStream(view, o)).flatMap(Function.identity());
    }

    public static <O extends OWLObject, A extends HasAnnotations & HasComponents> Stream<O> objects(Class<O> view, A container) {
        return Stream.concat(parseComponents(view, container), parseAnnotations(view, container));
    }

    public static <A extends HasAnnotations & HasComponents> Stream<IRI> iris(A container) {
        return Stream.concat(objects(IRI.class, container),
                objects(OWLObject.class, container).filter(HasIRI.class::isInstance).map(HasIRI.class::cast).map(HasIRI::getIRI));
    }

    private static <O extends OWLObject> Stream<O> toStream(Class<O> view, Object o) {
        if (view.isInstance(o)) {
            return Stream.of(view.cast(o));
        }
        if (o instanceof HasComponents) {
            return parseComponents(view, (HasComponents) o);
        }
        if (o instanceof HasAnnotations) {
            return parseAnnotations(view, (HasAnnotations) o);
        }
        Stream<?> stream = null;
        if (o instanceof Stream) {
            stream = ((Stream<?>) o);
        }
        if (o instanceof Collection) {
            stream = ((Collection<?>) o).stream();
        }
        if (stream != null) {
            return stream.map(_o -> toStream(view, _o)).flatMap(Function.identity());
        }
        return Stream.empty();
    }

}
