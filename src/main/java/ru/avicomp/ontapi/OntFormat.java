/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2017, Avicomp Services, AO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package ru.avicomp.ontapi;

import javax.annotation.Nullable;
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
 * There are 22 ONT formats (22(19 actual, i.e. without intersection) OWL document formats + 15(11 actual) jena languages),
 * but only 12 of them can be used without any hesitation (see {@link #isSupported()}).
 * For working with the OWL-API interfaces the {@link #createOwlFormat()} method could be used.
 * <p>
 * Created by @szuev on 27.09.2016.
 */
public enum OntFormat {
    TURTLE("Turtle", "ttl", Arrays.asList(Lang.TURTLE, Lang.TTL, Lang.N3), // n3 is treated as turtle
            Arrays.asList(TurtleDocumentFormat.class, RioTurtleDocumentFormat.class, N3DocumentFormat.class)),
    RDF_XML("RDF/XML", "rdf", Collections.singletonList(Lang.RDFXML), Arrays.asList(RDFXMLDocumentFormat.class, RioRDFXMLDocumentFormat.class)),
    // json has more priority since json-ld can't be read as json, but json can be as json-ld
    RDF_JSON("RDF/JSON", "rj", Lang.RDFJSON, RDFJsonDocumentFormat.class),
    JSON_LD("JSON-LD", "jsonld", Lang.JSONLD, RDFJsonLDDocumentFormat.class),

    NTRIPLES("N-Triples", "nt", Arrays.asList(Lang.NTRIPLES, Lang.NT), Collections.singletonList(NTriplesDocumentFormat.class)),
    NQUADS("N-Quads", "nq", Arrays.asList(Lang.NQUADS, Lang.NQ), Collections.singletonList(NQuadsDocumentFormat.class)),
    TRIG("TriG", "trig", Lang.TRIG, TrigDocumentFormat.class),
    TRIX("TriX", "trix", Lang.TRIX, TrixDocumentFormat.class),
    // jena only:
    RDF_THRIFT("RDF-THRIFT", "trdf", Lang.RDFTHRIFT, null),
    CSV("CSV", "csv", Lang.CSV, null),
    TSV("TSV", "tsv", Lang.TSV, null),
    // owl-api formats only:
    OWL_XML("OWL/XML", "owl", null, OWLXMLDocumentFormat.class),
    MANCHESTER_SYNTAX("ManchesterSyntax", "omn", null, ManchesterSyntaxDocumentFormat.class),
    FUNCTIONAL_SYNTAX("FunctionalSyntax", "fss", null, FunctionalSyntaxDocumentFormat.class),
    BINARY_RDF("BinaryRDF", "brf", null, BinaryRDFDocumentFormat.class),
    RDFA("RDFA", "html", null, RDFaDocumentFormat.class),
    OBO("OBO", "obo", null, OBODocumentFormat.class),
    KRSS("KRSS", "krss", null, KRSSDocumentFormat.class),
    KRSS2("KRSS2", "krss2", null, KRSS2DocumentFormat.class),
    DL("DL", "dl", null, DLSyntaxDocumentFormat.class),
    DL_HTML("DL/HTML", "html", null, DLSyntaxHTMLDocumentFormat.class),
    LATEX("LATEX", "tex", null, LatexDocumentFormat.class),;

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
        this.jenaLangs = Collections.unmodifiableList(jena);
        this.owlTypes = Collections.unmodifiableList(owl);
    }

    OntFormat(String id, String ext, Lang jena, Class<? extends OWLDocumentFormat> owl) {
        this(id, ext,
                jena == null ? Collections.emptyList() : Collections.singletonList(jena),
                owl == null ? Collections.emptyList() : Collections.singletonList(owl));
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
    @Nullable
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
     * Returns {@code true} if format is good for using by ONT-API(Jena) mechanisms.
     * Note: even if it is not good, usually it is still possible to use it by the native OWL-API mechanisms (directly
     * or as last attempt to load or save)... but maybe to read only or to write only,
     * or maybe with expectancy of some 'controlled' uri-transformations after reloading,
     * or by additional configuring manager with external storers/parsers (although it is not supported by ONT-API right now)
     *
     * - CSV ({@link Lang#CSV}) is not a valid Jena RDF serialization format (it is only for SPARQL results).
     * But it is possible to use it for reading csv files. Need to add jena-csv to dependencies.
     * For more details see <a href='http://jena.apache.org/documentation/csv/'>jena-csv</a>.
     * - TSV ({@link Lang#TSV}) Used by Jena for result sets, not RDF syntax.
     * - {@link BinaryRDFDocumentFormat} does not support writing to a Writer (see {@link org.eclipse.rdf4j.rio.binary.BinaryRDFWriterFactory}).
     * for the following formats there are no {@link org.semanticweb.owlapi.model.OWLStorerFactory}s in the current OWL-API 5.1.4 dependencies:
     * - {@link RDFaDocumentFormat}
     * - {@link KRSSDocumentFormat}
     * for the following formats there are no {@link org.semanticweb.owlapi.io.OWLParserFactory}s in the current OWL-API 5.1.4 dependencies:
     * - {@link LatexDocumentFormat}
     * - {@link DLSyntaxHTMLDocumentFormat}
     * Incorrect behaviour on reloading (the reloaded test-ontology does not match to the initial):
     * - {@link KRSS2DocumentFormat}
     * - {@link DLSyntaxDocumentFormat}
     * - {@link OBODocumentFormat}
     * The test ontology fot that case:
     * <pre> {@code
     * <http://ex> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#Ontology> .
     * <http://ex#C> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#Class> .
     * <http://ex#I> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#NamedIndividual> , <http://ex#C> .
     * } </pre>
     *
     * @return false if format is broken by some reasons.
     * @see #isReadSupported()
     * @see #isWriteSupported()
     */
    public boolean isSupported() {
        return isWriteSupported() && isReadSupported() && !OBO.equals(this);
    }

    /**
     * @return true if reading is possible through OWL-API or Jena
     * @see #isSupported()
     */
    public boolean isReadSupported() {
        return isNoneOf(TSV, LATEX, DL, DL_HTML, KRSS2);
    }

    /**
     * @return true if writing is possible through OWL-API or Jena
     * @see #isSupported()
     */
    public boolean isWriteSupported() {
        return isNoneOf(CSV, TSV, RDFA, BINARY_RDF, KRSS);
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
