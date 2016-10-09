package ru.avicomp.ontapi.io;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.PrintWriter;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.AbstractOWLStorer;
import org.semanticweb.owlapi.util.OWLDocumentFormatFactoryImpl;
import org.semanticweb.owlapi.util.OWLStorerFactoryImpl;

/**
 * todo: might be removed from project later.
 * ONT-API Turtle Store Factory
 * <p>
 * Created by szuev on 12.05.2016.
 */

public class OntTurtleStoreFactory extends OWLStorerFactoryImpl {

    static final String ONT_TURTLE_SYNTAX_KEY = "ONT-API Turtle Syntax";

    private static final OntTurtleStoreFactory INSTANCE = new OntTurtleStoreFactory();

    private static final long serialVersionUID = -1L;

    /**
     * public to use injection mechanism, see META-INF/services.
     */
    public OntTurtleStoreFactory() {
        super(new DocumentFormatFactory());
    }

    public static OntTurtleStoreFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public OWLStorer createStorer() {
        return new Store();
    }

    /**
     * OWL-API Factory for Ont Turtle Document OntFormat.
     * <p>
     * Created by szuev on 12.05.2016.
     */
    private static class DocumentFormatFactory extends OWLDocumentFormatFactoryImpl {

        private static final long serialVersionUID = -1L;

        @Nonnull
        @Override
        public String getKey() {
            return ONT_TURTLE_SYNTAX_KEY;
        }

        @Override
        public OWLDocumentFormat createFormat() {
            return OntFormat.TTL_RDF.getOwlFormat();
        }
    }

    /**
     * OWL-API Store
     * <p>
     * Created by szuev on 12.05.2016.
     */
    private class Store extends AbstractOWLStorer {
        private static final long serialVersionUID = -1;

        @Override
        protected void storeOntology(@Nonnull OWLOntology ontology, @Nonnull PrintWriter writer, @Nonnull OWLDocumentFormat format) throws OWLOntologyStorageException {
            try {
                new OntTurtleRenderer(ontology, writer).render();
            } catch (OWLRuntimeException e) {
                throw new OWLOntologyStorageException(e);
            }
        }

        @Override
        public boolean canStoreOntology(@Nullable OWLDocumentFormat format) {
            return OntFormat.TTL_RDF.getOwlFormat().equals(format);
        }
    }
}

