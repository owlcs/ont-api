package ru.avicomp.ontapi;

/**
 * Base runtime exception.
 *
 * Created by @szuev on 27.09.2016.
 */
public class OntException extends RuntimeException {
    public OntException(String message, Throwable cause) {
        super(message, cause);
    }

    public OntException(String message) {
        super(message);
    }

    public OntException(Throwable cause) {
        super(cause);
    }

    public OntException() {
        super();
    }

    public static <T> T notNull(T obj, String message) {
        if (obj == null)
            throw message == null ? new OntException() : new OntException(message);
        return obj;
    }

    public static <T> T notNull(T obj) {
        return notNull(obj, null);
    }

    /**
     * for unsupported things
     * Created by @szuev on 29.09.2016.
     */
    public static class Unsupported extends OntException {
        public Unsupported(String message) {
            super(message);
        }

        public Unsupported(Class clazz, String method) {
            this("Unsupported " + clazz + (method == null || method.isEmpty() ? "" : "#" + method));
        }

        public Unsupported(Class clazz) {
            this(clazz, null);
        }
    }
}
