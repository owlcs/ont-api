package ru.avicomp.ontapi;

import java.util.Objects;
import java.util.stream.Stream;

import org.apache.jena.riot.Lang;
import org.semanticweb.owlapi.formats.*;
import org.semanticweb.owlapi.model.OWLDocumentFormat;

/**
 * Map between jena and OWL-API languages.
 *
 * Created by @szuev on 27.09.2016.
 */
public enum OntFormat {
    XML_RDF("rdf", Lang.RDFXML, RDFXMLDocumentFormat.class),
    TTL_RDF("ttl", Lang.TURTLE, TurtleDocumentFormat.class),
    JSON_LD_RDF("jsonld", Lang.JSONLD, RDFJsonLDDocumentFormat.class),
    JSON_RDF("rj", Lang.RDFJSON, RDFJsonDocumentFormat.class),
    NTRIPLES("nt", Lang.NTRIPLES, NTriplesDocumentFormat.class),
    NQUADS("nq", Lang.NQUADS, NQuadsDocumentFormat.class),
    TRIG("trig", Lang.TRIG, TrigDocumentFormat.class),
    TRIX("trix", Lang.TRIX, TrixDocumentFormat.class),
    // jena only:
    THRIF("trdf", Lang.RDFTHRIFT, null),
    CSV("csv", "CSV", Lang.CSV, null, true),
    // owl-api formats only:
    OWL_XML_RDF("owl", "OWL/XML", null, OWLXMLDocumentFormat.class),
    MANCHESTER_SYNTAX("omn", "ManchesterSyntax", null, ManchesterSyntaxDocumentFormat.class),
    FUNCTIONAL_SYNTAX("fss", "FunctionalSyntax", null, FunctionalSyntaxDocumentFormat.class),
    BINARY("brf", "BinaryRDF", null, BinaryRDFDocumentFormat.class, true),
    RDFA("html", "RDFA", null, RDFaDocumentFormat.class, true),
    OBO("obo", "OBO", null, OBODocumentFormat.class),
    KRSS2("krss2", "KRSS2", null, KRSS2DocumentFormat.class, true),
    DL("dl", "DL", null, DLSyntaxDocumentFormat.class, true),;


    private final String id;
    private String ext; // primary extension.
    private Lang jenaType;
    private Class<? extends OWLDocumentFormat> owlType;
    private boolean disabled;

    OntFormat(String ext, Lang jena, Class<? extends OWLDocumentFormat> owl) {
        this(ext, jena.getLabel(), jena, owl);
    }

    OntFormat(String ext, String id, Lang jena, Class<? extends OWLDocumentFormat> owl) {
        this(ext, id, jena, owl, false);
    }

    OntFormat(String ext, String id, Lang jena, Class<? extends OWLDocumentFormat> owl, boolean disabled) {
        this.id = OntApiException.notNull(id, "Id is required.");
        this.jenaType = jena;
        this.ext = ext;
        this.owlType = owl;
        this.disabled = disabled;
    }

    public String getID() {
        return id;
    }

    public String getExt() {
        return ext;
    }

    public OWLDocumentFormat createOwlFormat() {
        try {
            return owlType.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new OntApiException(e);
        }
    }

    public Lang getLang() {
        return jenaType;
    }

    public boolean isSupported() {
        return !disabled;
    }

    public boolean isJena() {
        return jenaType != null;
    }

    public boolean isOWL() {
        return owlType != null;
    }

    public boolean isXML() {
        return id.contains("XML");
    }

    public static Stream<OntFormat> all() {
        return Stream.of(values()).filter(OntFormat::isSupported);
    }

    public static Stream<OntFormat> owlOnly() {
        return all().filter(OntFormat::isOWL).filter(f -> !f.isJena());
    }

    public static Stream<OntFormat> jenaOnly() {
        return all().filter(OntFormat::isJena).filter(f -> !f.isOWL());
    }

    public static OntFormat get(OWLDocumentFormat documentFormat) {
        OntApiException.notNull(documentFormat, "Null owl-document-format specified.");
        for (OntFormat r : values()) {
            if (Objects.equals(r.owlType, documentFormat.getClass())) return r;
        }
        return null;
    }

    public static OntFormat get(Lang lang) {
        OntApiException.notNull(lang, "Null jena-language specified.");
        for (OntFormat r : values()) {
            if (Objects.equals(r.jenaType, lang)) return r;
        }
        return null;
    }
}
