/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2018, Avicomp Services, AO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package ru.avicomp.ontapi.owlapi.objects;

import org.apache.jena.datatypes.BaseDatatype;
import org.apache.jena.datatypes.DatatypeFormatException;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.datatypes.xsd.impl.RDFLangString;
import org.apache.jena.graph.impl.LiteralLabel;
import org.apache.jena.graph.impl.LiteralLabelFactory;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObject;
import ru.avicomp.ontapi.owlapi.InternalizedEntities;
import ru.avicomp.ontapi.owlapi.OWLObjectImpl;
import ru.avicomp.ontapi.owlapi.objects.entity.OWLDatatypeImpl;

import javax.annotation.Nullable;
import java.lang.ref.SoftReference;
import java.util.*;

/**
 * An ON-API implementation of {@link OWLLiteral},
 * encapsulated {@link LiteralLabel Jena Literal Label}.
 * <p>
 * Created by @szz on 06.09.2018.
 */
@SuppressWarnings("WeakerAccess")
public class OWLLiteralImpl extends OWLObjectImpl implements OWLLiteral {

    protected static TypeMapper typeMapper = TypeMapper.getInstance();
    public static final Map<String, OWLDatatype> BUILTIN_OWL_DATATYPES = Collections.unmodifiableMap(new HashMap<String, OWLDatatype>() {
        {
            put(InternalizedEntities.LANGSTRING);
            put(InternalizedEntities.RDFSLITERAL);
            put(InternalizedEntities.PLAIN);
            put(InternalizedEntities.XSDBOOLEAN);
            put(InternalizedEntities.XSDDOUBLE);
            put(InternalizedEntities.XSDFLOAT);
            put(InternalizedEntities.XSDINTEGER);
            put(InternalizedEntities.XSDSTRING);
        }

        private void put(OWLDatatype d) {
            put(d.getIRI().getIRIString(), d);
        }
    });

    protected final LiteralLabel label;
    private transient SoftReference<OWLDatatype> owlDatatypeRef;

    protected OWLLiteralImpl(LiteralLabel label) {
        this.label = Objects.requireNonNull(label);
    }

    public static OWLLiteralImpl createLiteral(String s) {
        return newLiteral(LiteralLabelFactory.createTypedLiteral(s));
    }

    public static OWLLiteralImpl createLiteral(String val, String lang) {
        // original logic is saved:
        return newLiteral(LiteralLabelFactory.create(val, lang == null ? "" : lang.trim().toLowerCase(Locale.ENGLISH)));
    }

    public static OWLLiteralImpl createLiteral(int i) {
        return newLiteral(i, null, XSDDatatype.XSDinteger);
    }

    public static OWLLiteralImpl createLiteral(boolean b) {
        return newLiteral(LiteralLabelFactory.createTypedLiteral(b));
    }

    public static OWLLiteralImpl createLiteral(double d) {
        return newLiteral(LiteralLabelFactory.createTypedLiteral(d));
    }

    public static OWLLiteralImpl createLiteral(float f) {
        return newLiteral(LiteralLabelFactory.createTypedLiteral(f));
    }

    public static OWLLiteralImpl createLiteral(String txt, OWLDatatype owl) {
        if (owl.isRDFPlainLiteral() || InternalizedEntities.LANGSTRING.equals(owl)) {
            // original logic is saved:
            int sep = txt.lastIndexOf('@');
            if (sep != -1) {
                String lex = txt.substring(0, sep);
                String lang = txt.substring(sep + 1);
                return newLiteral(lex, lang, RDFLangString.rdfLangString);
            }
            return newLiteral(txt, null, XSDDatatype.XSDstring);
        }
        if (owl.isString()) {
            return createLiteral(txt);
        }
        if (owl.isBoolean()) {
            return parseBoolean(txt, owl);
        }
        if (owl.isFloat()) {
            return parseFloat(txt, owl);
        }
        if (owl.isDouble()) {
            return parseDouble(txt, owl);
        }
        if (owl.isInteger()) {
            return parseInteger(txt, owl);
        }
        return createDefaultLiteral(txt, owl);
    }

    /**
     * Creates an {@link OWLLiteralImpl} instance by string value and {@link OWLDatatype OWL Datatype}.
     * This method works with {@link TypeMapper Jena Type Mapper}, but it does not change it.
     *
     * @param txt String, not {@code null}
     * @param owl {@link OWLDatatype}, not {@code null}
     * @return {@link OWLLiteralImpl}
     */
    public static OWLLiteralImpl createDefaultLiteral(String txt, OWLDatatype owl) {
        String uri = owl.getIRI().getIRIString();
        RDFDatatype dt = typeMapper.getTypeByName(uri);
        if (dt == null) { // do not litter the global manager:
            dt = new BaseDatatype(uri);
        }
        OWLLiteralImpl res = newLiteral(txt, null, dt);
        res.owlDatatypeRef = new SoftReference<>(owl);
        return res;
    }

    /**
     * Creates an {@link OWLLiteralImpl} instance with the given lexical form, language tag and {@link RDFDatatype RDF Datatype}.
     *
     * @param lex  String, not {@code null}
     * @param lang String or {@code null} for plain or typed literals
     * @param dt   {@link RDFDatatype}, may be {@code null} in some circumstances
     * @return {@link OWLLiteralImpl}
     */
    public static OWLLiteralImpl newLiteral(Object lex, String lang, RDFDatatype dt) {
        return newLiteral(LiteralLabelFactory.createByValue(lex, lang, dt));
    }

    /**
     * Creates an {@link OWLLiteralImpl} instance for the specifies {@link LiteralLabel}.
     *
     * @param label {@link LiteralLabel}, not {@code null}
     * @return {@link OWLLiteralImpl}
     */
    public static OWLLiteralImpl newLiteral(LiteralLabel label) {
        return new OWLLiteralImpl(label);
    }

    @SuppressWarnings("unused")
    public static OWLLiteralImpl parseBoolean(String txt, OWLDatatype owl) {
        return createLiteral(parseBoolean(txt.trim()));
    }

    public static boolean parseBoolean(String str) {
        return Boolean.parseBoolean(str) || "1".equals(str.trim());
    }

    public static OWLLiteralImpl parseDouble(String txt, OWLDatatype dt) {
        try {
            return createLiteral(Double.parseDouble(txt));
        } catch (NumberFormatException e) {
            return createDefaultLiteral(txt, dt);
        }
    }

    public static OWLLiteralImpl parseFloat(String txt, OWLDatatype dt) {
        if ("-0.0".equals(txt.trim())) {
            // original comment: according to some W3C test, this needs to be different from 0.0; Java floats disagree
            return createDefaultLiteral(txt, dt);
        }
        try {
            return createLiteral(Float.parseFloat(txt));
        } catch (NumberFormatException e) {
            return createDefaultLiteral(txt, dt);
        }
    }

    public static OWLLiteralImpl parseInteger(String txt, OWLDatatype dt) {
        // original comment: again, some W3C tests require padding zeroes to make literals different
        if ('0' == txt.trim().charAt(0)) {
            return createDefaultLiteral(txt, dt);
        }
        try {
            // original comment: this is fine for values that can be parsed as ints - not all values are
            return createLiteral(Integer.parseInt(txt));
        } catch (NumberFormatException ex) {
            // original comment: try as a big decimal
            return createDefaultLiteral(txt, dt);
        }
    }

    public static boolean isDatatypeRegistered(RDFDatatype dt) {
        return BaseDatatype.class != dt.getClass();
    }

    /**
     * Returns {@code true} if the literals are equal to each other.
     * The method {@link LiteralLabel#equals(Object)} won't work for custom datatypes
     * since them are not cached in the {@link TypeMapper}.
     *
     * @param left  {@link LiteralLabel}, can be {@code null}
     * @param right {@link LiteralLabel}, can be {@code null}
     * @return {@code true} if the arguments are equal to each other and {@code false} otherwise
     * @see #createDefaultLiteral(String, OWLDatatype)
     */
    public static boolean equals(LiteralLabel left, LiteralLabel right) {
        if (left == right) return true;
        if (left == null || right == null) {
            return false;
        }
        return Objects.equals(left.getLexicalForm(), right.getLexicalForm())
                && Objects.equals(left.language(), right.language())
                && Objects.equals(left.getDatatypeURI(), right.getDatatypeURI());
    }

    public LiteralLabel getLiteralLabel() {
        return label;
    }

    @Override
    public String getLiteral() {
        return label.getLexicalForm();
    }

    public Object getValue() throws DatatypeFormatException {
        return label.getValue();
    }

    @Override
    public String getLang() {
        return label.language();
    }

    @Override
    public OWLDatatype getDatatype() {
        // Although, OWLDatatype is a lightweight object, but ontology usually has a lot of literals
        // and usually this object is not really needed. Besides, it is really cheap to make it cached:
        OWLDatatype res;
        if (owlDatatypeRef != null && (res = owlDatatypeRef.get()) != null)
            return res;
        owlDatatypeRef = new SoftReference<>(res = calcDatatype());
        return res;
    }

    public OWLDatatype calcDatatype() {
        RDFDatatype dt = label.getDatatype();
        if (dt != null) {
            String uri = dt.getURI();
            OWLDatatype owl = BUILTIN_OWL_DATATYPES.get(uri);
            if (owl != null && isDatatypeRegistered(dt)) {
                return owl;
            }
            return new OWLDatatypeImpl(IRI.create(uri));
        }
        return label.language().isEmpty() ? InternalizedEntities.PLAIN : InternalizedEntities.LANGSTRING;
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

    protected int getLiteralHashCode() {
        if (label.isWellFormedRaw()) {
            Object value = label.getValue();
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            if (value instanceof Boolean) {
                return (Boolean) value ? 1 : 0;
            }
        }
        return label.getLexicalForm().hashCode();
    }

    @Override
    public int initHashCode() {
        int hash = hashIndex();
        hash = OWLObject.hashIteration(hash, getDatatype().hashCode());
        hash = OWLObject.hashIteration(hash, getLiteralHashCode() * 65536);
        return OWLObject.hashIteration(hash, getLang().hashCode());
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof OWLLiteralImpl) {
            LiteralLabel other = ((OWLLiteralImpl) obj).label;
            return equals(label, other);
        }
        return super.equals(obj);
    }
}
