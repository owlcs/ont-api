package ru.avicomp.ontapi.utils;


import javax.annotation.Nullable;
import java.net.URI;
import java.util.Optional;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyID;

import com.google.common.base.Strings;
import ru.avicomp.ontapi.OntException;

/**
 * TODO
 * <p>
 * Created by @szuev on 27.09.2016.
 */
public class OntIRI extends IRI {
    private static final String URI_RESOURCE_SEPARATOR = "#";

    protected OntIRI(String prefix, @Nullable String suffix) {
        super(prefix, suffix);
    }

    protected OntIRI(String s) {
        super(s);
    }

    protected OntIRI(URI uri) {
        super(uri);
    }

    private String base;

    public OntIRI addFragment(String fragment) {
        if (Strings.isNullOrEmpty(fragment))
            throw new OntIRIException("Incorrect fragment specified: '" + fragment + "'");
        return new OntIRI(String.format("%s%s%s", getBase(), URI_RESOURCE_SEPARATOR, fragment));
    }

    public OntIRI addPath(String path) {
        String base = getIRIString();
        if (base.contains(URI_RESOURCE_SEPARATOR)) {
            base = getBase();
        }
        return new OntIRI(String.format("%s/%s", base, OntIRIException.notNull(path, "Null path specified.")));
    }


    public String getBase() {
        if (base != null) return base;
        String iri = getIRIString();
        return base = iri.replaceFirst("#.*$", "").replaceFirst("/$", "");
    }

    public OWLOntologyID toOwlOntologyID() {
        return toOwlOntologyID(null);
    }

    public OWLOntologyID toOwlOntologyID(IRI versionIRI) {
        return versionIRI == null ? new OWLOntologyID(this) : new OWLOntologyID(this, versionIRI);
    }

    public Resource toResource() {
        return ResourceFactory.createResource(getIRIString());
    }

    public Property toProperty() {
        return ResourceFactory.createProperty(getIRIString());
    }

    public static OntIRI create(URI uri) {
        return new OntIRI(OntIRIException.notNull(uri, "Null URI"));
    }

    public static OntIRI create(Resource resource) {
        // should we allow anonymous resources also?
        if (!OntIRIException.notNull(resource, "Null resource specified").isURIResource()) {
            throw new OntIRIException("Not uri-resource: " + resource);
        }
        return new OntIRI(resource.getURI());
    }

    public static OntIRI create(String string) {
        return new OntIRI(string);
    }

    public static OntIRI create(IRI iri) {
        return new OntIRI(OntIRIException.notNull(iri, "Null owl-IRI specified.").getIRIString());
    }

    public static OntIRI create(OWLOntology o) {
        Optional<IRI> opt = OntIRIException.notNull(o, "Null owl-ontology specified.").getOntologyID().getOntologyIRI();
        return opt.isPresent() ? create(opt.get()) : null;
    }

    public static String toStringIRI(OWLOntologyID id) {
        return getString(id.getOntologyIRI());
    }

    public static String toStringVersionIRI(OWLOntologyID id) {
        return getString(id.getVersionIRI());
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static String getString(Optional<IRI> optional) {
        return optional.map(IRI::getIRIString).orElse(null);
    }

    private static class OntIRIException extends OntException {
        OntIRIException(String s) {
            super(s);
        }

        OntIRIException(String message, Throwable cause) {
            super(message, cause);
        }

        public OntIRIException() {
            super();
        }

        public static <T> T notNull(T obj, String message) {
            if (obj == null)
                throw message == null ? new OntIRIException() : new OntIRIException(message);
            return obj;
        }
    }

}
