package ru.avicomp.ontapi;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.jena.riot.Lang;
import org.semanticweb.owlapi.formats.*;
import org.semanticweb.owlapi.model.OWLDocumentFormat;

/**
 * The map between jena languages ({@link Lang}) and OWL-API formats ({@link OWLDocumentFormat}).
 * There are 21 ONT formats (22 OWL formats + 14 jena languages)
 * <p>
 * Created by @szuev on 27.09.2016.
 */
public enum OntFormat {
    TURTLE("Turtle", "ttl", Arrays.asList(Lang.TURTLE, Lang.TTL, Lang.N3), // n3 is treated as turtle
            Arrays.asList(TurtleDocumentFormat.class, RioTurtleDocumentFormat.class, N3DocumentFormat.class)),
    RDF_XML("RDF/XML", "rdf", Collections.singletonList(Lang.RDFXML), Arrays.asList(RDFXMLDocumentFormat.class, RioRDFXMLDocumentFormat.class)),
    // json has more priority since json-ld can't be read as json, but json can be as json-ld
    RDF_JSON("RDF/JSON", "rj", Collections.singletonList(Lang.RDFJSON), Collections.singletonList(RDFJsonDocumentFormat.class)),
    JSON_LD("JSON-LD", "jsonld", Collections.singletonList(Lang.JSONLD), Collections.singletonList(RDFJsonLDDocumentFormat.class)),

    NTRIPLES("N-Triples", "nt", Arrays.asList(Lang.NTRIPLES, Lang.NT), Collections.singletonList(NTriplesDocumentFormat.class)),
    NQUADS("N-Quads", "nq", Arrays.asList(Lang.NQUADS, Lang.NQ), Collections.singletonList(NQuadsDocumentFormat.class)),
    TRIG("TriG", "trig", Collections.singletonList(Lang.TRIG), Collections.singletonList(TrigDocumentFormat.class)),
    TRIX("TriX", "trix", Collections.singletonList(Lang.TRIX), Collections.singletonList(TrixDocumentFormat.class)),
    // jena only:
    RDF_THRIFT("RDF-THRIFT", "trdf", Collections.singletonList(Lang.RDFTHRIFT), null),
    CSV("CSV", "csv", Collections.singletonList(Lang.CSV), null),
    // owl-api formats only:
    OWL_XML("OWL/XML", "owl", null, Collections.singletonList(OWLXMLDocumentFormat.class)),
    MANCHESTER_SYNTAX("ManchesterSyntax", "omn", null, Collections.singletonList(ManchesterSyntaxDocumentFormat.class)),
    FUNCTIONAL_SYNTAX("FunctionalSyntax", "fss", null, Collections.singletonList(FunctionalSyntaxDocumentFormat.class)),
    BINARY("BinaryRDF", "brf", null, Collections.singletonList(BinaryRDFDocumentFormat.class)),
    RDFA("RDFA", "html", null, Collections.singletonList(RDFaDocumentFormat.class)),
    OBO("OBO", "obo", null, Collections.singletonList(OBODocumentFormat.class)),
    KRSS("KRSS", "krss", null, Collections.singletonList(KRSSDocumentFormat.class)),
    KRSS2("KRSS2", "krss2", null, Collections.singletonList(KRSS2DocumentFormat.class)),
    DL("DL", "dl", null, Collections.singletonList(DLSyntaxDocumentFormat.class)),
    DL_HTML("DL/HTML", "html", null, Collections.singletonList(DLSyntaxHTMLDocumentFormat.class)),
    LATEXT("LATEX", "tex", null, Collections.singletonList(LatexDocumentFormat.class)),;

    private final String id;
    private String ext;
    private final List<Lang> jenaLangs;
    private final List<Class<? extends OWLDocumentFormat>> owlTypes;

    /**
     * @param id   String, "short name", could be used inside jena {@link org.apache.jena.rdf.model.Model} model to read and write.
     * @param ext  String, primary extension.
     * @param jena List of language objects ({@link Lang}). Could be used inside jena {@link org.apache.jena.riot.RDFDataMgr} manager to read and write.
     * @param owl  List of {@link OWLDocumentFormat} classes. OWLDocumentFormat is used to read and write in OWL-API (... and also to store prefixes sometimes).
     */
    OntFormat(String id, String ext, List<Lang> jena, List<Class<? extends OWLDocumentFormat>> owl) {
        this.id = OntApiException.notNull(id, "Id is required.");
        this.ext = ext;
        this.jenaLangs = jena == null ? Collections.emptyList() : Collections.unmodifiableList(jena);
        this.owlTypes = owl == null ? Collections.emptyList() : Collections.unmodifiableList(owl);
    }

    public String getID() {
        return id;
    }

    /**
     * The primary file extension.
     * There is no usage in the API.
     * It's for convenience only.
     *
     * @return String.
     */
    public String getExt() {
        return ext;
    }

    /**
     * The primary jena Language.
     *
     * @return {@link Lang}, could be null.
     */
    public Lang getLang() {
        return jenaLangs.isEmpty() ? null : jenaLangs.get(0);
    }

    /**
     * Gets all jena Languages associated with this type in the form of Stream.
     *
     * @return Stream of {@link Lang}, could be empty.
     */
    public Stream<Lang> jenaLangs() {
        return jenaLangs.stream();
    }

    /**
     * Gets instances of all OWL-API formats associated with this type in the form of Stream
     *
     * @return Stream of {@link OWLDocumentFormat}, could be empty.
     */
    public Stream<OWLDocumentFormat> owlFormats() {
        return owlTypes.stream().map(OntFormat::owlFormatInstance);
    }

    /**
     * Creates new instance of {@link OWLDocumentFormat} by type setting.
     * if there is no any owl-document format in this map then return instance of fake format ({@link SimpleDocumentFormat})
     * with reference to this instance inside.
     * So there is a support OWL-API-style even for pure jena formats.
     *
     * @return {@link OWLDocumentFormat}, no null.
     * @throws OntApiException if something wrong.
     */
    public OWLDocumentFormat createOwlFormat() {
        return owlTypes.isEmpty() ? new SimpleDocumentFormat() : owlFormatInstance(owlTypes.get(0));
    }

    protected static OWLDocumentFormat owlFormatInstance(Class<? extends OWLDocumentFormat> type) {
        try {
            return type.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new OntApiException("Can't create " + type.getSimpleName(), e);
        }
    }

    /**
     * Checks if format is good for using.
     * todo: need to retest this list.
     *
     * @return false if format is broken by some reasons.
     */
    public boolean isSupported() {
        return isNoneOf(CSV, BINARY, RDFA, KRSS2, DL);
    }

    public boolean isJena() {
        return !jenaLangs.isEmpty();
    }

    public boolean isOWL() {
        return !owlTypes.isEmpty();
    }

    public boolean isJenaOnly() {
        return isJena() && !isOWL();
    }

    public boolean isOWLOnly() {
        return isOWL() && !isJena();
    }

    /**
     * Currently there are only two xml formats: RDF/XML and OWL/XML
     *
     * @return true if one of them.
     */
    public boolean isXML() {
        return isOneOf(RDF_XML, OWL_XML);
    }

    /**
     * Currently there are only two json formats: RDF/JSON and JSONLD
     *
     * @return true if one of them.
     */
    public boolean isJSON() {
        return isOneOf(RDF_JSON, JSON_LD);
    }

    private boolean isOneOf(OntFormat... formats) {
        return Stream.of(formats).anyMatch(this::equals);
    }

    private boolean isNoneOf(OntFormat... formats) {
        return Stream.of(formats).noneMatch(this::equals);
    }

    public static Stream<OntFormat> formats() {
        return Stream.of(values());
    }

    public static Stream<OntFormat> supported() {
        return formats().filter(OntFormat::isSupported);
    }

    public static Stream<OntFormat> owlOnly() {
        return supported().filter(OntFormat::isOWLOnly);
    }

    public static Stream<OntFormat> jenaOnly() {
        return supported().filter(OntFormat::isJenaOnly);
    }

    public static OntFormat get(OWLDocumentFormat owlFormat) {
        Class<? extends OWLDocumentFormat> type = OntApiException.notNull(owlFormat, "Null owl-document-format specified.").getClass();
        return SimpleDocumentFormat.class.equals(type) ? get(owlFormat.getKey()) : get(type);
    }

    public static OntFormat get(Class<? extends OWLDocumentFormat> type) {
        OntApiException.notNull(type, "Null owl-document-format class specified.");
        for (OntFormat r : values()) {
            if (r.owlTypes.stream().anyMatch(type::equals)) return r;
        }
        return null;
    }

    public static OntFormat get(Lang lang) {
        OntApiException.notNull(lang, "Null jena-language specified.");
        for (OntFormat r : values()) {
            if (r.jenaLangs.stream().anyMatch(lang::equals)) return r;
        }
        return null;
    }

    public static OntFormat get(String id) {
        OntApiException.notNull(id, "Null ont-format id specified.");
        for (OntFormat r : values()) {
            if (Objects.equals(r.id, id)) return r;
        }
        return null;
    }

    public class SimpleDocumentFormat extends AbstractRDFPrefixDocumentFormat {
        @Override
        public String getKey() {
            return id;
        }
    }
}
