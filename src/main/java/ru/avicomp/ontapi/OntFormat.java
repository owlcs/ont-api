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
    XML_RDF("rdf", Lang.RDFXML, new RDFXMLDocumentFormat()),
    TTL_RDF("ttl", Lang.TURTLE, new TurtleDocumentFormat()),
    JSON_LD_RDF("json", Lang.JSONLD, new RDFJsonLDDocumentFormat()),
    JSON_RDF("json", Lang.RDFJSON, new RDFJsonDocumentFormat()),
    NTRIPLES("nt", Lang.NTRIPLES, new NTriplesDocumentFormat()),
    NQUADS("nq", Lang.NQUADS, new NQuadsDocumentFormat()),
    TRIG("trig", Lang.TRIG, new TrigDocumentFormat()),
    TRIX("trix", Lang.TRIX, new TrixDocumentFormat()),
    // jena only:
    THRIF("trdf", Lang.RDFTHRIFT, null),
    CSV("csv", Lang.CSV, null),
    // owl-api formats only:
    OWL_XML_RDF("owl", "OWL/XML", null, new OWLXMLDocumentFormat()),
    MANCHESTER_SYNTAX("omn", "ManchesterSyntax", null, new ManchesterSyntaxDocumentFormat()),
    FUNCTIONAL_SYNTAX("fss", "FunctionalSyntax", null, new FunctionalSyntaxDocumentFormat()),
    BINARY("brf", "BinaryRDF", null, new BinaryRDFDocumentFormat(), true),
    RDFA("html", "RDFA", null, new RDFaDocumentFormat(), true),
    OBO("obo", "OBO", null, new OBODocumentFormat()),
    KRSS2("krss2", "KRSS2", null, new KRSS2DocumentFormat(), true),
    DL("dl", "DL", null, new DLSyntaxDocumentFormat(), true),;
    private final String id;
    private String ext;
    private Lang jena;
    private OWLDocumentFormat owl;
    private boolean disabled;

    OntFormat(String ext, Lang jena, OWLDocumentFormat owl) {
        this(ext, jena.getLabel(), jena, owl);
    }

    OntFormat(String ext, String id, Lang jena, OWLDocumentFormat owl) {
        this(ext, id, jena, owl, false);
    }

    OntFormat(String ext, String id, Lang jena, OWLDocumentFormat owl, boolean disabled) {
        this.id = id;
        this.jena = jena;
        this.ext = ext;
        this.owl = owl;
        this.disabled = disabled;
    }

    public String getType() {
        return id;
    }

    public String getExt() {
        return ext;
    }

    public OWLDocumentFormat getOwlFormat() {
        return owl;
    }

    public Lang getLang() {
        return jena;
    }

    public boolean isSupported() {
        return !disabled;
    }

    public boolean isJena() {
        return jena != null;
    }

    public boolean isOWL() {
        return owl != null;
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

    public static OntFormat get(OWLDocumentFormat owl) {
        for (OntFormat r : values()) {
            if (Objects.equals(r.owl, owl)) return r;
        }
        return null;
    }
}
