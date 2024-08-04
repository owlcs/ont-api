/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2023, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.owlapi.objects;

import com.github.owlcs.ontapi.AsNode;
import com.github.owlcs.ontapi.owlapi.InternalizedEntities;
import com.github.owlcs.ontapi.owlapi.OWLObjectImpl;
import com.github.owlcs.ontapi.owlapi.objects.entity.BuiltinDatatypeImpl;
import com.github.owlcs.ontapi.owlapi.objects.entity.DatatypeImpl;
import javax.annotation.Nullable;
import org.apache.jena.datatypes.BaseDatatype;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.datatypes.xsd.impl.RDFLangString;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.impl.LiteralLabel;
import org.apache.jena.graph.impl.LiteralLabelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.XSD;
import org.semanticweb.owlapi.model.HasHashIndex;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An ONT-API implementation of {@link OWLLiteral}, encapsulated {@link LiteralLabel Jena Literal Label}.
 * <p>
 * Created by @szz on 06.09.2018.
 */
@SuppressWarnings("WeakerAccess")
public class LiteralImpl extends OWLObjectImpl implements OWLLiteral, AsNode {

    private static final Set<String> NUMBER_DATATYPES = Stream.of(XSD.integer, XSD.xfloat, XSD.xdouble)
            .map(Resource::getURI).collect(Collectors.toUnmodifiableSet());

    protected static final TypeMapper typeMapper = TypeMapper.getInstance();

    protected transient final LiteralLabel label;

    protected LiteralImpl(LiteralLabel label) {
        this.label = Objects.requireNonNull(label);
    }

    public static LiteralImpl createLiteral(String s) {
        return newLiteral(LiteralLabelFactory.createTypedLiteral(s));
    }

    /**
     * Creates a literal impl by the given lexical from and language tag.
     * The method normalises language tag to the trimmed lower-case form.
     * Notice that language tag in general case may contain upper-case letters,
     * and two similar but different strings are not equal.
     * To create a literal with the retention of the syntax of a language-tag,
     * use methods {@link #newLiteral(LiteralLabel)} or {@link #newLiteral(String, String, OWLDatatype)}
     *
     * @param val  String, lexical form, not {@code null}
     * @param lang String, or {@code null} for Plain Literals
     * @return {@link LiteralImpl}
     * @see #normalizeLanguageTag(String)
     * @see #equals(Object)
     */
    public static LiteralImpl createLiteral(String val, String lang) {
        return newLiteral(newLiteralLabel(val, normalizeLanguageTag(lang), null));
    }

    /**
     * Creates a literal impl wrapping the given int number.
     *
     * @param i int
     * @return {@link LiteralImpl}
     */
    public static LiteralImpl createLiteral(int i) {
        return newLiteral(i, null, XSDDatatype.XSDinteger);
    }

    /**
     * Creates a literal impl wrapping the given boolean primitive.
     *
     * @param b boolean
     * @return {@link LiteralImpl}
     */
    public static LiteralImpl createLiteral(boolean b) {
        return newLiteral(LiteralLabelFactory.createTypedLiteral(b));
    }

    /**
     * Creates a literal impl wrapping the given double number.
     *
     * @param d double
     * @return {@link LiteralImpl}
     */
    public static LiteralImpl createLiteral(double d) {
        return newLiteral(LiteralLabelFactory.createTypedLiteral(d));
    }

    /**
     * Creates a literal impl wrapping the given float number.
     *
     * @param f float
     * @return {@link LiteralImpl}
     */
    public static LiteralImpl createLiteral(float f) {
        return newLiteral(LiteralLabelFactory.createTypedLiteral(f));
    }

    /**
     * Creates a literal impl by the given string and {@link OWLDatatype OWL Datatype}.
     * If the datatype is {@link InternalizedEntities#RDF_PLAIN_LITERAL rdf:PlainLiteral} or
     * {@link InternalizedEntities#RDF_LANG_STRING rdf:langString}
     * the method will parse and normalize datatype from the first (String) argument,
     * see {@link #createLiteral(String, String)} description.
     * If the datatype is {@link InternalizedEntities#XSD_STRING xsd:string},
     * then no parsing is performed and the returned literal will have the same lexical form as specified.
     * In other cases the string is parsed, and the lexical form may differ from what was specified.
     * For example, if the input are {@code "1e-07"} and {@link InternalizedEntities#XSD_DOUBLE xsd:double},
     * the output will be {@code "1.OE7"^^xsd:double}.
     *
     * @param txt String, not {@code null}
     * @param owl {@link OWLDatatype}, not {@code null}
     * @return {@link LiteralImpl}
     * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLDataFactoryInternalsImplNoCache.java#L139'>uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryInternalsImpl#getOWLLiteral(String, OWLDatatype)</a>
     * @see #normalizeLanguageTag(String)
     * @see #equals(Object)
     */
    public static LiteralImpl createLiteral(String txt, OWLDatatype owl) {
        if (owl.isRDFPlainLiteral() || InternalizedEntities.RDF_LANG_STRING.equals(owl)) {
            // original logic is saved:
            String lex, lang;
            RDFDatatype dt;
            int sep = txt.lastIndexOf('@');
            if (sep != -1) {
                lex = txt.substring(0, sep);
                lang = normalizeLanguageTag(txt.substring(sep + 1));
                dt = RDFLangString.rdfLangString;
            } else {
                lex = txt;
                lang = null;
                dt = XSDDatatype.XSDstring;
            }
            return newLiteral(lex, lang, dt);
        }
        if (owl.isString()) {
            return createLiteral(txt);
        }
        if (owl.isBoolean()) {
            return parseBooleanLiteral(txt);
        }
        if (owl.isFloat()) {
            return parseFloatLiteral(txt, owl);
        }
        if (owl.isDouble()) {
            return parseDoubleLiteral(txt, owl);
        }
        if (owl.isInteger()) {
            return parseIntegerLiteral(txt, owl);
        }
        return newLiteral(txt, null, owl);
    }

    /**
     * Normalises the language tag
     *
     * @param lang String, possible {@code null}
     * @return String
     * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLDataFactoryInternalsImplNoCache.java#L103'>uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryInternalsImpl#getOWLLiteral(String, String)</a>
     */
    public static String normalizeLanguageTag(String lang) {
        return lang == null ? "" : lang.trim().toLowerCase(Locale.ENGLISH);
    }

    public static LiteralImpl parseBooleanLiteral(String txt) {
        return createLiteral(parseBoolean(txt.trim()));
    }

    public static boolean parseBoolean(String txt) {
        return Boolean.parseBoolean(txt = txt.trim()) || "1".equals(txt);
    }

    public static LiteralImpl parseDoubleLiteral(String txt, OWLDatatype dt) {
        try {
            return createLiteral(Double.parseDouble(txt));
        } catch (NumberFormatException e) {
            return newLiteral(txt, null, dt);
        }
    }

    public static LiteralImpl parseFloatLiteral(String txt, OWLDatatype dt) {
        if ("-0.0".equals(txt.trim())) {
            // original comment: according to some W3C test, this needs to be different from 0.0; Java floats disagree
            return newLiteral(txt, null, dt);
        }
        try {
            return createLiteral(Float.parseFloat(txt));
        } catch (NumberFormatException e) {
            return newLiteral(txt, null, dt);
        }
    }

    public static LiteralImpl parseIntegerLiteral(String txt, OWLDatatype dt) {
        // original comment: again, some W3C tests require padding zeroes to make literals different
        if ('0' == txt.trim().charAt(0)) {
            return newLiteral(txt, null, dt);
        }
        try {
            // original comment: this is fine for values that can be parsed as ints - not all values are
            return createLiteral(Integer.parseInt(txt));
        } catch (NumberFormatException ex) {
            // original comment: try as a big decimal
            return newLiteral(txt, null, dt);
        }
    }

    /**
     * Converts any instance of {@link OWLLiteral} to the {@link LiteralImpl ONT-API Literal implementation}.
     *
     * @param literal {@link OWLLiteral}
     * @return {@link LiteralImpl}
     */
    public static LiteralImpl asONT(OWLLiteral literal) {
        if (literal instanceof LiteralImpl) {
            return (LiteralImpl) literal;
        }
        return newLiteral(literal.getLiteral(), literal.getLang(), literal.getDatatype());
    }

    /**
     * Creates an {@link LiteralImpl} instance by string value and {@link OWLDatatype OWL Datatype}.
     * This method works with {@link TypeMapper Jena Type Mapper}, but it does not change it.
     *
     * @param txt  String, not {@code null}
     * @param lang String, can be {@code null}
     * @param owl  {@link OWLDatatype}, not {@code null}
     * @return {@link LiteralImpl}
     */
    public static LiteralImpl newLiteral(String txt, String lang, OWLDatatype owl) {
        return newLiteral(txt, lang, getRDFDatatype(owl.getIRI().getIRIString()));
    }

    /**
     * Gets the {@link RDFDatatype} for the given {@code uri}.
     *
     * @param uri String, not {@code null}
     * @return {@link RDFDatatype}
     */
    public static RDFDatatype getRDFDatatype(String uri) {
        RDFDatatype res = typeMapper.getTypeByName(uri);
        if (res == null) { // do not litter the global manager:
            res = new BaseDatatype(uri);
        }
        return res;
    }

    /**
     * Creates an {@link LiteralImpl} instance with the given lexical form, language tag and {@link RDFDatatype RDF Datatype}.
     *
     * @param lex  String, not {@code null}
     * @param lang String or {@code null} for plain or typed literals
     * @param dt   {@link RDFDatatype}, may be {@code null} in some circumstances
     * @return {@link LiteralImpl}
     */
    public static LiteralImpl newLiteral(Object lex, String lang, RDFDatatype dt) {
        return newLiteral(newLiteralLabel(lex, lang, dt));
    }

    private static LiteralLabel newLiteralLabel(Object lex, String lang, RDFDatatype dt) {
        if (dt != null && (lang == null || lang.isEmpty())) {
            return LiteralLabelFactory.createByValue(lex, dt);
        }
        if (lang != null && !lang.isEmpty()) {
            if (lex instanceof String) {
                return LiteralLabelFactory.createLang(lex.toString(), lang);
            }
        } else if (lex instanceof String) {
            return LiteralLabelFactory.createString(lex.toString());
        } else {
            return LiteralLabelFactory.createTypedLiteral(lex);
        }
        throw new IllegalArgumentException("Can't construct literal ['%s', %s, %s]".formatted(lex, lang, dt));
    }

    /**
     * Creates an {@link LiteralImpl} instance for the specifies {@link LiteralLabel}.
     *
     * @param label {@link LiteralLabel}, not {@code null}
     * @return {@link LiteralImpl}
     */
    public static LiteralImpl newLiteral(LiteralLabel label) {
        return new LiteralImpl(label);
    }

    /**
     * Calculates a hash-code for lexical form.
     * The original (OWL-API) logic is preserved.
     *
     * @param label {@link LiteralLabel}, not {@code null}
     * @return int
     */
    protected static int calcLiteralLabelHashCode(LiteralLabel label) {
        if (label.isWellFormed()) {
            Object value = label.getValue();
            String dtURI = label.getDatatypeURI();
            if (value instanceof Number && NUMBER_DATATYPES.contains(dtURI)) {
                return ((Number) value).intValue();
            } else if (value instanceof Boolean && XSD.xboolean.getURI().equals(dtURI)) {
                return (Boolean) value ? 1 : 0;
            }
        }
        return label.getLexicalForm().hashCode();
    }

    /**
     * Calculates a hash-code for {@link LiteralImpl} given as its components.
     *
     * @param index    int, constant, {@link HasHashIndex#hashIndex()}
     * @param label    {@link LiteralLabel}, not {@code null}
     * @param datatype {@link OWLDatatype}, not {@code null}
     * @param lang     String, not {@code null}
     * @return int
     */
    protected static int calcHashCode(int index, LiteralLabel label, OWLDatatype datatype, String lang) {
        index = OWLObject.hashIteration(index, datatype.hashCode());
        index = OWLObject.hashIteration(index, calcLiteralLabelHashCode(label) * 65536);
        return OWLObject.hashIteration(index, lang.hashCode());
    }

    /**
     * Answers {@code true} if the argument-literals are equal to each other and {@code false} otherwise.
     *
     * @param left  {@link LiteralLabel}, can be {@code null}
     * @param right {@link LiteralLabel}, can be {@code null}
     * @return boolean
     */
    public static boolean equals(LiteralLabel left, LiteralLabel right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        if (!Objects.equals(left.getLexicalForm(), right.getLexicalForm()))
            return false;
        if (!Objects.equals(left.language(), right.language()))
            return false;
        if (Objects.equals(left.getDatatype(), right.getDatatype())) {
            return true;
        }
        return Objects.equals(getDatatypeURI(left), getDatatypeURI(right));
    }

    /**
     * Gets the valid (in OWL-API sense) datatype URI from the given {@code label}.
     *
     * @param label {@link LiteralLabel}, not {@code null}
     * @return String
     */
    public static String getDatatypeURI(LiteralLabel label) {
        String uri = label.getDatatypeURI();
        String lang = label.language();
        if (uri != null) {
            if (RDF.PlainLiteral.getURI().equals(uri) && lang.isEmpty()) {
                // a special case of ".."^^rdf:PlainLiteral:
                return XSD.xstring.getURI();
            }
            return uri;
        }
        return lang.isEmpty() ? XSD.xstring.getURI() : RDF.langString.getURI();
    }

    @Override
    public Node asNode() {
        //noinspection deprecation
        return NodeFactory.createLiteral(label);
    }

    /**
     * Returns a {@link LiteralLabel Jena Literal Label}, that is encapsulated by this object.
     *
     * @return {@link LiteralLabel}
     */
    public LiteralLabel getLiteralLabel() {
        return label;
    }

    @Override
    public String getLiteral() {
        return label.getLexicalForm();
    }

    @Override
    public String getLang() {
        return label.language();
    }

    /**
     * Returns the datatype URI.
     * For a lang-literal the datatype URI is {@link RDF#langString rdf:langString}.
     * For a plain-literal the datatype URI is {@link XSD#xstring xsd:string}.
     *
     * @return String, cannot be {@code null}
     */
    public String getDatatypeURI() {
        return getDatatypeURI(label);
    }

    /**
     * Returns the {@link OWLDatatype OWL-API datatype} parsed from the encapsulated {@link LiteralLabel}.
     * Please note: in the special case of no-lang PlainLiteral (e.g. {@code '...'^^rdf:PlainLiteral})
     * the method returns {@link InternalizedEntities#XSD_STRING},
     * although the encapsulated label may contain {@link RDF#PlainLiteral} type.
     *
     * @return {@link OWLDatatype}
     */
    @Override
    public OWLDatatype getDatatype() {
        String uri = getDatatypeURI();
        OWLDatatype res = BuiltinDatatypeImpl.BUILTIN_OWL_DATATYPES.get(uri);
        return res != null ? res : new DatatypeImpl(IRI.create(uri));
    }

    @Override
    public boolean hasLang() {
        return !label.language().isEmpty();
    }

    @Override
    public boolean hasLang(@Nullable String lang) {
        if (lang == null) {
            return !hasLang();
        }
        return getLang().equalsIgnoreCase(lang.trim());
    }

    @Override
    public boolean isRDFPlainLiteral() {
        return getDatatype().isRDFPlainLiteral();
    }

    @Override
    public boolean isInteger() {
        return getDatatype().isInteger();
    }

    @Override
    public boolean isBoolean() {
        return getDatatype().isBoolean();
    }

    @Override
    public boolean isDouble() {
        return getDatatype().isDouble();
    }

    @Override
    public boolean isFloat() {
        return getDatatype().isFloat();
    }

    @Override
    public int parseInteger() {
        return Integer.parseInt(getLiteral());
    }

    @Override
    public boolean parseBoolean() {
        return parseBoolean(getLiteral());
    }

    @Override
    public double parseDouble() {
        return Double.parseDouble(getLiteral());
    }

    @Override
    public float parseFloat() {
        String res = getLiteral();
        if ("inf".equalsIgnoreCase(res)) {
            return Float.POSITIVE_INFINITY;
        }
        if ("-inf".equalsIgnoreCase(res)) {
            return Float.NEGATIVE_INFINITY;
        }
        return Float.parseFloat(res);
    }

    @Override
    protected Set<OWLEntity> getSignatureSet() {
        return createSet(getDatatype());
    }

    @Override
    protected Set<OWLDatatype> getDatatypeSet() {
        return createSet(getDatatype());
    }

    @Override
    public boolean containsEntityInSignature(@Nullable OWLEntity entity) {
        if (entity == null || !entity.isOWLDatatype()) return false;
        return getDatatype().equals(entity);
    }

    @Override
    protected Set<OWLClass> getNamedClassSet() {
        return createSet();
    }

    @Override
    protected Set<OWLNamedIndividual> getNamedIndividualSet() {
        return createSet();
    }

    @Override
    protected Set<OWLDataProperty> getDataPropertySet() {
        return createSet();
    }

    @Override
    protected Set<OWLObjectProperty> getObjectPropertySet() {
        return createSet();
    }

    @Override
    protected Set<OWLAnnotationProperty> getAnnotationPropertySet() {
        return createSet();
    }

    @Override
    protected Set<OWLClassExpression> getClassExpressionSet() {
        return createSet();
    }

    @Override
    protected Set<OWLAnonymousIndividual> getAnonymousIndividualSet() {
        return createSet();
    }

    @Override
    public int initHashCode() {
        return calcHashCode(hashIndex(), label, getDatatype(), getLang());
    }

    /**
     * Answers whether some other object is "equal to" this one.
     * <p>
     * The comparison is performed by the lexical form, language tag and {@link OWLDatatype OWL Datatype}.
     * So, {@code "01"^^xsd:integer} and {@code "1"^^xsd:integer} are two different literals.
     * Also, {@code "1e-07"^^xsd:double} and {@code "1.OE7"^^xsd:double} are not equal,
     * although the expression {@code literal.getLiteralLabel().getValue()}
     * will return equal objects for both literals from the mentioned pairs.
     * Also note: the language tag is a {@code String}, and therefore the comparison is case-sensitive, e.g.
     * the tags "SU-su" and "su-su" are not equal.
     *
     * @param obj {@link Object} anything
     * @return {@code true} if this object is the same as the obj argument; {@code false} otherwise
     */
    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof OWLLiteral)) {
            return false;
        }
        if (obj instanceof LiteralImpl other) {
            if (notSame(other)) {
                return false;
            }
            return equals(label, other.getLiteralLabel());
        }
        if (obj instanceof AsNode) {
            return asNode().equals(((AsNode) obj).asNode());
        }
        return super.equals(obj);
    }

    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeObject(label.getDatatypeURI());
        out.writeObject(label.language());
        out.writeObject(label.getLexicalForm());
    }

    @Serial
    private void readObject(ObjectInputStream in) throws Exception {
        in.defaultReadObject();
        String uri = (String) in.readObject();
        String lang = (String) in.readObject();
        String value = (String) in.readObject();
        LiteralLabel label = newLiteralLabel(value, lang, getRDFDatatype(uri));
        Field field = getClass().getDeclaredField("label");
        field.setAccessible(true);
        field.set(this, label);
        field.setAccessible(false);
    }
}
