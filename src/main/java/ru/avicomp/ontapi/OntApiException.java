package ru.avicomp.ontapi;

import java.util.function.Supplier;

/**
 * Base runtime exception.
 * todo: extends {@link org.semanticweb.owlapi.model.OWLRuntimeException}; do we need our own exception in that case?
 * <p>
 * Created by @szuev on 27.09.2016.
 */
public class OntApiException extends RuntimeException {
    public OntApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public OntApiException(String message) {
        super(message);
    }

    public OntApiException(Throwable cause) {
        super(cause);
    }

    public OntApiException() {
        super();
    }

    public static <T> T notNull(T obj, String message) {
        if (obj == null)
            throw message == null ? new OntApiException() : new OntApiException(message);
        return obj;
    }

    public static <T> T notNull(T obj) {
        return notNull(obj, null);
    }


    public static Supplier<OntApiException> supplier(String msg) {
        return () -> new OntApiException(msg);
    }

    /**
     * for unsupported things
     * Created by @szuev on 29.09.2016.
     */
    public static class Unsupported extends OntApiException {
        public Unsupported(String message) {
            super(message);
        }

        public Unsupported(Class clazz, String method) {
            this("Unsupported " + clazz.getName() + (method == null || method.isEmpty() ? "" : "#" + method));
        }

        public Unsupported(Class clazz) {
            this(clazz, null);
        }
    }
}
