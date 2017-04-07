package ru.avicomp.ontapi.jena;

import java.util.function.Supplier;

import org.apache.jena.shared.JenaException;

/**
 * To use inside ont-jena subsystem (package {@link ru.avicomp.ontapi.jena}).
 * <p>
 * Created by @szuev on 24.11.2016.
 */
public class OntJenaException extends JenaException {
    public OntJenaException() {
        super();
    }

    public OntJenaException(String message) {
        super(message);
    }

    public OntJenaException(Throwable cause) {
        super(cause);
    }

    public OntJenaException(String message, Throwable cause) {
        super(message, cause);
    }

    public static <T> T notNull(T obj, String message) {
        if (obj == null)
            throw message == null ? new OntJenaException() : new OntJenaException(message);
        return obj;
    }

    public static Supplier<OntJenaException> supplier(String msg) {
        return () -> new OntJenaException(msg);
    }

    /**
     * this is an analogue of {@link org.apache.jena.ontology.ConversionException},
     * used inside top level api ({@link ru.avicomp.ontapi.jena.model.OntGraphModel} and maybe {@link ru.avicomp.ontapi.jena.model.OntObject}).
     */
    public static class Conversion extends OntJenaException {
        public Conversion(String message, Throwable cause) {
            super(message, cause);
        }

        public Conversion(String message) {
            super(message);
        }
    }

    public static class Creation extends OntJenaException {
        public Creation(String message, Throwable cause) {
            super(message, cause);
        }

        public Creation(String message) {
            super(message);
        }
    }

    public static class Unsupported extends OntJenaException {
    }
}
