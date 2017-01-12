package ru.avicomp.ontapi.jena;

import org.apache.jena.shared.JenaException;

/**
 * To use inside ont-jena subsystem (package {@link ru.avicomp.ontapi.jena}).
 * <p>
 * Created by @szuev on 24.11.2016.
 */
public class OntJenaException extends JenaException {
    public OntJenaException() {
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

    /**
     * this is an analogue of {@link org.apache.jena.ontology.ConversionException},
     * used inside top level api ({@link ru.avicomp.ontapi.jena.model.OntGraphModel}, {@link ru.avicomp.ontapi.jena.model.OntObject}).
     */
    public static class Conversion extends OntJenaException {
        public Conversion(String message, Throwable cause) {
            super(message, cause);
        }

        public Conversion(String message) {
            super(message);
        }
    }
}
