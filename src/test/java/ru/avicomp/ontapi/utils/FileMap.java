package ru.avicomp.ontapi.utils;

import javax.annotation.Nullable;
import java.util.Objects;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;

/**
 * The analogue of {@link org.semanticweb.owlapi.util.SimpleIRIMapper}
 * Created by @szuev on 23.03.2017.
 */
@SuppressWarnings("WeakerAccess")
public class FileMap implements OWLOntologyIRIMapper {
    protected final IRI iri, doc;

    protected FileMap(IRI iri, IRI file) {
        this.iri = iri;
        this.doc = file;
    }

    public static FileMap create(IRI ont, IRI doc) {
        return new FileMap(ont, doc);
    }

    public IRI getIRI() {
        return iri;
    }

    public IRI getDocument() {
        return doc;
    }

    @Override
    public IRI getDocumentIRI(@Nullable IRI ontologyIRI) {
        return Objects.equals(ontologyIRI, iri) ? doc : null;
    }

    @Override
    public String toString() {
        return String.format("%s => %s", iri, doc);
    }
}
