package ru.avicomp.ontapi;

import java.util.Objects;
import java.util.stream.Stream;

import org.apache.jena.riot.Lang;
import org.semanticweb.owlapi.formats.*;
import org.semanticweb.owlapi.model.OWLDocumentFormat;

/**
 * Map between jena and OWL-API languages.
 * Currently there are 18 formats, but only 13 are fully supported.
 *
 * Created by @szuev on 27.09.2016.
 */
public enum OntFormat {
    TURTLE("ttl", Lang.TURTLE, TurtleDocumentFormat.class),
    RDF_XML("rdf", Lang.RDFXML, RDFXMLDocumentFormat.class),

    // json has more priority since json-ld can't be read as json, but json can be as json-ld
    RDF_JSON("rj", Lang.RDFJSON, RDFJsonDocumentFormat.class),
    JSON_LD("jsonld", Lang.JSONLD, RDFJsonLDDocumentFormat.class),

    NTRIPLES("nt", Lang.NTRIPLES, NTriplesDocumentFormat.class),
    NQUADS("nq", Lang.NQUADS, NQuadsDocumentFormat.class),
    TRIG("trig", Lang.TRIG, TrigDocumentFormat.class),
    TRIX("trix", Lang.TRIX, TrixDocumentFormat.class),
    // jena only:
    THRIFT("trdf", Lang.RDFTHRIFT, null),
    CSV("csv", "CSV", Lang.CSV, null, true),
    // owl-api formats only:
    OWL_XML("owl", "OWL/XML", null, OWLXMLDocumentFormat.class),
    MANCHESTER_SYNTAX("omn", "ManchesterSyntax", null, ManchesterSyntaxDocumentFormat.class),
    FUNCTIONAL_SYNTAX("fss", "FunctionalSyntax", null, FunctionalSyntaxDocumentFormat.class),
    BINARY("brf", "BinaryRDF", null, BinaryRDFDocumentFormat.class, true),
    RDFA("html", "RDFA", null, RDFaDocumentFormat.class, true),
    OBO("obo", "OBO", null, OBODocumentFormat.class),
    KRSS2("krss2", "KRSS2", null, KRSS2DocumentFormat.class, true),
    DL("dl", "DL", null, DLSyntaxDocumentFormat.class, true),;


    private final String id;                            // the "id"
    private String ext;                                 // primary extension
    private Lang jenaType;                              // jena "language" syntax
    private Class<? extends OWLDocumentFormat> owlType; // OWLDocumentFormat type
    private boolean disabled;                           // true to skip

    OntFormat(String ext, Lang jena, Class<? extends OWLDocumentFormat> owl) {
        this(ext, jena.getLabel(), jena, owl);
    }

    OntFormat(String ext, String id, Lang jena, Class<? extends OWLDocumentFormat> owl) {
        this(ext, id, jena, owl, false);
    }

    /**
     * The main constructor.
     *
     * @param ext      String, primary extension.
     * @param id       String, "short name", could be used inside jena {@link org.apache.jena.rdf.model.Model} model to read and write.
     * @param jena     {@link Lang} language object. Nullable. Could be used inside jena {@link org.apache.jena.riot.RDFDataMgr} manager to read and write.
     * @param owl      {@link OWLDocumentFormat} Class. Nullable. Objects of this type are used to read and write in OWL-API (... and also to store prefixes).
     * @param disabled true if this format is not supported at the moment by some reasons.
     */
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
            return owlType == null ? null: owlType.newInstance();
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

    public boolean isXML() { // there are two xml formats: RDF/XML and OWL/XML
        return equals(RDF_XML) || equals(OWL_XML);
    }

    public boolean isJSON() { // there are two json formats: RDF/JSON and JSONLD
        return equals(RDF_JSON) || equals(JSON_LD);
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
        return get(OntApiException.notNull(documentFormat, "Null owl-document-format specified.").getClass());
    }

    public static OntFormat get(Class<? extends OWLDocumentFormat> type) {
        OntApiException.notNull(type, "Null owl-document-format class specified.");
        for (OntFormat r : values()) {
            if (Objects.equals(r.owlType, type)) return r;
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
