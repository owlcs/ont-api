package ru.avicomp.ontapi.io;

import org.apache.jena.riot.RDFFormat;
import org.semanticweb.owlapi.formats.*;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLDocumentFormatImpl;

/**
 * todo:
 * Created by @szuev on 27.09.2016.
 */
public enum OntFormat {
    XML_RDF("rdf", RDFFormat.RDFXML_ABBREV, new RDFXMLDocumentFormat()),
    TTL_RDF("ttl", RDFFormat.TURTLE_PRETTY, new OWLDocumentFormatImpl() {
        @Override
        public String getKey() {
            return OntTurtleStoreFactory.ONT_TURTLE_SYNTAX_KEY;
        }
    }),
    JSON_LD_RDF("json", RDFFormat.JSONLD, new RDFJsonLDDocumentFormat()),
    JSON_RDF("json", RDFFormat.RDFJSON, new RDFJsonDocumentFormat()),
    NTRIPLES("nt", RDFFormat.NTRIPLES, new NTriplesDocumentFormat()),
    TRIG("trig", RDFFormat.TRIG, new TrigDocumentFormat()),
    // owl-api formats only:
    OWL_XML_RDF("owl", "OWL/XML", null, new OWLXMLDocumentFormat()),
    MANCHESTER_SYNTAX("omn", "ManchesterSyntax", null, new ManchesterSyntaxDocumentFormat()),
    FUNCTIONAL_SYNTAX("fss", "FunctionalSyntax", null, new FunctionalSyntaxDocumentFormat()),;

    private final String id;
    private String ext;
    private RDFFormat jena;
    private OWLDocumentFormat owl;


    OntFormat(String ext, RDFFormat jena, OWLDocumentFormat owl) {
        this(ext, jena.getLang().getLabel(), jena, owl);
    }

    OntFormat(String ext, String id, RDFFormat jena, OWLDocumentFormat owl) {
        this.id = id;
        this.jena = jena;
        this.ext = ext;
        this.owl = owl;
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

    public RDFFormat getJenaFormat() {
        return jena;
    }

    public boolean isJena() {
        return jena != null;
    }

    public boolean isXML() {
        return id.contains("XML");
    }
}
