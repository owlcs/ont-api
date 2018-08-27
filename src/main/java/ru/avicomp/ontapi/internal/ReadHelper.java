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

package ru.avicomp.ontapi.internal;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWLFacet;
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.config.OntLoaderConfiguration;
import ru.avicomp.ontapi.jena.impl.OntObjectImpl;
import ru.avicomp.ontapi.jena.model.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Helper to translate rdf-graph to the owl-objects form.
 * <p>
 * Created by @szuev on 25.11.2016.
 */
@SuppressWarnings("WeakerAccess")
public class ReadHelper {

    /**
     * Auxiliary method for simplification code.
     * Used in Annotation Translators.
     * If the specified statement also belongs to the another type of axiom and it is prohibited in the config then returns false.
     * This is for three kinds of statements:
     * <ul>
     * <li>{@code A1 rdfs:subPropertyOf A2}</li>
     * <li>{@code A rdfs:domain U}</li>
     * <li>{@code A rdfs:range U}</li>
     * </ul>
     * Each of them is wider than the analogous statement for object or data property,
     * e.g. "P rdfs:range C" could be treated as "A rdfs:range U", but not vice versa.
     *
     * @param statement {@link OntStatement} to test
     * @param conf      {@link OntLoaderConfiguration}
     * @param o         {@link AxiomType#SUB_OBJECT_PROPERTY} or {@link AxiomType#OBJECT_PROPERTY_DOMAIN} or {@link AxiomType#OBJECT_PROPERTY_RANGE}
     * @param d         {@link AxiomType#SUB_DATA_PROPERTY} or {@link AxiomType#DATA_PROPERTY_DOMAIN} or {@link AxiomType#DATA_PROPERTY_RANGE}
     * @return true if the statement is good to be represented in the form of annotation axiom.
     */
    protected static boolean testAnnotationAxiomOverlaps(OntStatement statement,
                                                         OntLoaderConfiguration conf,
                                                         AxiomType<? extends OWLObjectPropertyAxiom> o,
                                                         AxiomType<? extends OWLDataPropertyAxiom> d) {
        return conf == null || !(conf.isIgnoreAnnotationAxiomOverlaps() &&
                Stream.of(d, o).map(AxiomParserProvider::get).anyMatch(a -> a.testStatement(statement)));
    }

    /**
     * Answers {@code true} if the given {@link OntStatement} is a declaration (predicate = {@code rdf:type})
     * of some OWL entity or anonymous individual.
     *
     * @param s {@link OntStatement} to test
     * @return boolean
     */
    public static boolean isDeclarationStatement(OntStatement s) {
        return s.isDeclaration() && isEntityOrAnonymousIndividual(s.getSubject());
    }

    /**
     * Answers {@code true} if the given {@link Resource} is an OWL-Entity or Anonymous Individual
     *
     * @param o {@link Resource}
     * @return boolean
     */
    public static boolean isEntityOrAnonymousIndividual(Resource o) {
        return o.isURIResource() || o.canAs(OntIndividual.Anonymous.class);
    }

    /**
     * Answers if the given {@link OntStatement} can be considered as annotation property assertion.
     *
     * @param s        {@link OntStatement}, not null
     * @param withBulk {@code true} if bulk annotations are allowed by the config
     * @return {@code true} if the specified statement is annotation property assertion
     */
    public static boolean isAnnotationAssertionStatement(OntStatement s, boolean withBulk) {
        return s.isAnnotation()
                && !s.isBulkAnnotation()
                && (withBulk || !s.hasAnnotations());
    }

    /**
     * Returns the container with set of {@link OWLAnnotation} associated with the specified statement.
     *
     * @param statement {@link OntStatement}
     * @param df        {@link InternalDataFactory}
     * @return a set of wraps {@link ONTObject} around {@link OWLAnnotation}
     */
    public static Set<ONTObject<OWLAnnotation>> getAnnotations(OntStatement statement, NoCacheDataFactory df) {
        Stream<OntStatement> res = statement.annotations();
        OntLoaderConfiguration conf = df.config.loaderConfig();
        if (conf.isLoadAnnotationAxioms() && isDeclarationStatement(statement)) {
            boolean withBulk = conf.isAllowBulkAnnotationAssertions();
            // for compatibility with OWL-API skip all plain annotations attached to an entity (or anonymous individual)
            // they would go separately as annotation-assertions.
            res = res.filter(s -> !isAnnotationAssertionStatement(s, withBulk));
        }
        return res.map(a -> getAnnotation(a, df)).collect(Collectors.toSet());
    }

    /**
     * Lists all annotations related to the object (including assertions).
     *
     * @param obj {@link OntObject}
     * @param df  {@link InternalDataFactory}
     * @return Stream of {@link ONTObject}s of {@link OWLAnnotation}
     */
    public static Stream<ONTObject<OWLAnnotation>> objectAnnotations(OntObject obj, InternalDataFactory df) {
        return obj.annotations().map(a -> getAnnotation(a, df));
    }

    /**
     * Translates {@link OntStatement} to {@link ONTObject} encapsulated {@link OWLAnnotation}.
     *
     * @param ann {@link OntStatement}
     * @param df  {@link InternalDataFactory}
     * @return {@link ONTObject} around {@link OWLAnnotation}
     */
    public static ONTObject<OWLAnnotation> getAnnotation(OntStatement ann, InternalDataFactory df) {
        return ann.hasAnnotations() ? getHierarchicalAnnotations(ann, df) : getPlainAnnotation(ann, df);
    }

    private static ONTObject<OWLAnnotation> getPlainAnnotation(OntStatement ann, InternalDataFactory df) {
        ONTObject<OWLAnnotationProperty> p = df.get(ann.getPredicate().as(OntNAP.class));
        ONTObject<? extends OWLAnnotationValue> v = df.get(ann.getObject());
        OWLAnnotation res = df.getOWLDataFactory().getOWLAnnotation(p.getObject(), v.getObject(), Stream.empty());
        return ONTObject.create(res, ann).append(p).append(v);
    }

    private static ONTObject<OWLAnnotation> getHierarchicalAnnotations(OntStatement root, InternalDataFactory df) {
        Resource subject = root.getSubject();
        ONTObject<OWLAnnotationProperty> p = df.get(root.getPredicate().as(OntNAP.class));
        ONTObject<? extends OWLAnnotationValue> v = df.get(root.getObject());
        Set<ONTObject<OWLAnnotation>> children = root.annotations().map(a -> getHierarchicalAnnotations(a, df)).collect(Collectors.toSet());
        OWLAnnotation object = df.getOWLDataFactory().getOWLAnnotation(p.getObject(), v.getObject(), children.stream().map(ONTObject::getObject));
        ONTObject<OWLAnnotation> res = ONTObject.create(object, root);
        if (subject.canAs(OntAnnotation.class)) {
            res = res.append(subject.as(OntAnnotation.class));
        }
        return res.append(p).append(v).append(children);
    }

    /**
     * Maps {@link OntFR} =&gt; {@link OWLFacetRestriction}.
     *
     * @param fr {@link OntFR}
     * @param df {@link InternalDataFactory}
     * @return {@link ONTObject} around {@link OWLFacetRestriction}
     */
    public static ONTObject<OWLFacetRestriction> getFacetRestriction(OntFR fr, InternalDataFactory df) {
        OWLFacetRestriction res = calcOWLFacetRestriction(fr, df);
        return ONTObject.create(res, fr);
    }

    public static OWLFacetRestriction calcOWLFacetRestriction(OntFR fr, InternalDataFactory df) {
        OWLLiteral literal = df.get(OntApiException.notNull(fr, "Null facet restriction.").getValue()).getObject();
        Class<? extends OntObject> view = OntApiException.notNull(((OntObjectImpl) fr).getActualClass(),
                "Can't determine view of facet restriction " + fr);
        if (OntFR.Length.class.equals(view))
            return df.getOWLDataFactory().getOWLFacetRestriction(OWLFacet.LENGTH, literal);
        if (OntFR.MinLength.class.equals(view))
            return df.getOWLDataFactory().getOWLFacetRestriction(OWLFacet.MIN_LENGTH, literal);
        if (OntFR.MaxLength.class.equals(view))
            return df.getOWLDataFactory().getOWLFacetRestriction(OWLFacet.MAX_LENGTH, literal);
        if (OntFR.MinInclusive.class.equals(view))
            return df.getOWLDataFactory().getOWLFacetRestriction(OWLFacet.MIN_INCLUSIVE, literal);
        if (OntFR.MaxInclusive.class.equals(view))
            return df.getOWLDataFactory().getOWLFacetRestriction(OWLFacet.MAX_INCLUSIVE, literal);
        if (OntFR.MinExclusive.class.equals(view))
            return df.getOWLDataFactory().getOWLFacetRestriction(OWLFacet.MIN_EXCLUSIVE, literal);
        if (OntFR.MaxExclusive.class.equals(view))
            return df.getOWLDataFactory().getOWLFacetRestriction(OWLFacet.MAX_EXCLUSIVE, literal);
        if (OntFR.Pattern.class.equals(view))
            return df.getOWLDataFactory().getOWLFacetRestriction(OWLFacet.PATTERN, literal);
        if (OntFR.FractionDigits.class.equals(view))
            return df.getOWLDataFactory().getOWLFacetRestriction(OWLFacet.FRACTION_DIGITS, literal);
        if (OntFR.TotalDigits.class.equals(view))
            return df.getOWLDataFactory().getOWLFacetRestriction(OWLFacet.TOTAL_DIGITS, literal);
        if (OntFR.LangRange.class.equals(view))
            return df.getOWLDataFactory().getOWLFacetRestriction(OWLFacet.LANG_RANGE, literal);
        throw new OntApiException("Unsupported facet restriction " + fr);
    }


    /**
     * Calculates an {@link OWLDataRange} wrapped by {@link ONTObject}.
     * Note: this method is recursive.
     *
     * @param dr   {@link OntDR}
     * @param df   {@link NoCacheDataFactory}
     * @param seen Set of {@link Resource}
     * @return {@link ONTObject} around {@link OWLDataRange}
     * @throws OntApiException if something is wrong.
     */
    @SuppressWarnings("unchecked")
    public static ONTObject<? extends OWLDataRange> calcDataRange(OntDR dr,
                                                                  NoCacheDataFactory df,
                                                                  Set<Resource> seen) {
        if (OntApiException.notNull(dr, "Null data range.").isAnon() && seen.contains(dr)) {
            throw new OntApiException("Recursive loop on data range " + dr);
        }
        NoCacheDataFactory.SimpleMap<OntDR, ONTObject<? extends OWLDataRange>> found = df.dataRangeStore();
        ONTObject<? extends OWLDataRange> r = found.get(dr);
        if (r != null) return r;
        seen.add(dr);
        if (dr.isURIResource()) {
            return ONTObject.create(df.getOWLDataFactory().getOWLDatatype(df.toIRI(dr.getURI())), dr);
        }
        Class<? extends OntObject> view = OntApiException.notNull(((OntObjectImpl) dr).getActualClass(),
                "Can't determine view of data range " + dr);
        if (OntDR.Restriction.class.equals(view)) {
            OntDR.Restriction _dr = (OntDR.Restriction) dr;
            ONTObject<OWLDatatype> d = df.get(_dr.getDatatype());
            Set<ONTObject<OWLFacetRestriction>> restrictions = _dr.facetRestrictions().map(f -> getFacetRestriction(f, df))
                    .collect(Collectors.toSet());
            OWLDataRange res = df.getOWLDataFactory().getOWLDatatypeRestriction(d.getObject(), restrictions.stream().map(ONTObject::getObject).collect(Collectors.toList()));
            return ONTObject.create(res, dr).append(restrictions);
        }
        if (OntDR.ComplementOf.class.equals(view)) {
            OntDR.ComplementOf _dr = (OntDR.ComplementOf) dr;
            ONTObject<? extends OWLDataRange> d = found.get(_dr.getDataRange(), v -> calcDataRange(v, df, seen));
            return ONTObject.create(df.getOWLDataFactory().getOWLDataComplementOf(d.getObject()), _dr).append(d);
        }
        if (OntDR.UnionOf.class.equals(view) ||
                OntDR.IntersectionOf.class.equals(view)) {
            Set<ONTObject<? extends OWLDataRange>> dataRanges =
                    (OntDR.UnionOf.class.equals(view) ? ((OntDR.UnionOf) dr).dataRanges() : ((OntDR.IntersectionOf) dr).dataRanges())
                            .map(d -> found.get(d, v -> calcDataRange(v, df, seen)))
                            .collect(Collectors.toSet());
            OWLDataRange res = OntDR.UnionOf.class.equals(view) ?
                    df.getOWLDataFactory().getOWLDataUnionOf(dataRanges.stream().map(ONTObject::getObject)) :
                    df.getOWLDataFactory().getOWLDataIntersectionOf(dataRanges.stream().map(ONTObject::getObject));
            return ONTObject.create(res, dr).appendWildcards(dataRanges);
        }
        if (OntDR.OneOf.class.equals(view)) {
            OntDR.OneOf _dr = (OntDR.OneOf) dr;
            Set<ONTObject<OWLLiteral>> literals = _dr.values().map(df::get).collect(Collectors.toSet());
            OWLDataRange res = df.getOWLDataFactory().getOWLDataOneOf(literals.stream().map(ONTObject::getObject));
            return ONTObject.create(res, _dr);
        }
        throw new OntApiException("Unsupported data range expression " + dr);
    }

    /**
     * @param ce {@link OntCE}
     * @return {@link ONTObject} around {@link OWLClassExpression}
     */
    public static ONTObject<? extends OWLClassExpression> fetchClassExpression(OntCE ce) {
        return AxiomTranslator.getDataFactory(ce.getModel()).get(ce);
    }

    /**
     * Calculates an {@link OWLClassExpression} wrapped by {@link ONTObject}.
     * Note: this method is recursive.
     *
     * @param ce   {@link OntCE}
     * @param df   {@link NoCacheDataFactory}
     * @param seen Set of {@link Resource}
     * @return {@link ONTObject} around {@link OWLClassExpression}
     * @throws OntApiException if something is wrong.
     */
    @SuppressWarnings("unchecked")
    public static ONTObject<? extends OWLClassExpression> calcClassExpression(OntCE ce,
                                                                              NoCacheDataFactory df,
                                                                              Set<Resource> seen) {
        if (ce.isAnon() && seen.contains(ce)) {
            throw new OntApiException("Recursive loop on class expression " + ce);
        }
        NoCacheDataFactory.SimpleMap<OntCE, ONTObject<? extends OWLClassExpression>> found = df.classExpressionStore();
        ONTObject<? extends OWLClassExpression> res = found.get(ce);
        if (res != null) return res;
        seen.add(ce);
        if (ce.isURIResource()) {
            return ONTObject.create(df.getOWLDataFactory().getOWLClass(df.toIRI(ce.getURI())), ce);
        }
        Class<? extends OntObject> view = OntApiException.notNull(((OntObjectImpl) ce).getActualClass(),
                "Can't determine type of class expression " + ce);
        if (OntCE.ObjectSomeValuesFrom.class.equals(view) ||
                OntCE.ObjectAllValuesFrom.class.equals(view)) {
            OntCE.ComponentRestrictionCE<OntCE, OntOPE> _ce = (OntCE.ComponentRestrictionCE<OntCE, OntOPE>) ce;
            ONTObject<? extends OWLObjectPropertyExpression> p = df.get(_ce.getOnProperty());
            ONTObject<? extends OWLClassExpression> c = found.get(_ce.getValue(), v -> calcClassExpression(v, df, seen));
            OWLClassExpression owl;
            if (OntCE.ObjectSomeValuesFrom.class.equals(view))
                owl = df.getOWLDataFactory().getOWLObjectSomeValuesFrom(p.getObject(), c.getObject());
            else if (OntCE.ObjectAllValuesFrom.class.equals(view))
                owl = df.getOWLDataFactory().getOWLObjectAllValuesFrom(p.getObject(), c.getObject());
            else
                throw new OntApiException("Should never happen");
            return ONTObject.create(owl, _ce).append(p).append(c);
        }
        if (OntCE.DataSomeValuesFrom.class.equals(view) ||
                OntCE.DataAllValuesFrom.class.equals(view)) {
            OntCE.ComponentRestrictionCE<OntDR, OntNDP> _ce = (OntCE.ComponentRestrictionCE<OntDR, OntNDP>) ce;
            ONTObject<OWLDataProperty> p = df.get(_ce.getOnProperty());
            ONTObject<? extends OWLDataRange> d = df.get(_ce.getValue());
            OWLClassExpression owl;
            if (OntCE.DataSomeValuesFrom.class.equals(view))
                owl = df.getOWLDataFactory().getOWLDataSomeValuesFrom(p.getObject(), d.getObject());
            else if (OntCE.DataAllValuesFrom.class.equals(view))
                owl = df.getOWLDataFactory().getOWLDataAllValuesFrom(p.getObject(), d.getObject());
            else
                throw new OntApiException("Should never happen");
            return ONTObject.create(owl, _ce).append(p).append(d);
        }
        if (OntCE.ObjectHasValue.class.equals(view)) {
            OntCE.ObjectHasValue _ce = (OntCE.ObjectHasValue) ce;
            ONTObject<? extends OWLObjectPropertyExpression> p = df.get(_ce.getOnProperty());
            ONTObject<? extends OWLIndividual> i = df.get(_ce.getValue());
            return ONTObject.create(df.getOWLDataFactory().getOWLObjectHasValue(p.getObject(), i.getObject()), _ce).append(p).append(i);
        }
        if (OntCE.DataHasValue.class.equals(view)) {
            OntCE.DataHasValue _ce = (OntCE.DataHasValue) ce;
            ONTObject<OWLDataProperty> p = df.get(_ce.getOnProperty());
            ONTObject<OWLLiteral> l = df.get(_ce.getValue());
            return ONTObject.create(df.getOWLDataFactory().getOWLDataHasValue(p.getObject(), l.getObject()), _ce).append(p);
        }
        if (OntCE.ObjectMinCardinality.class.equals(view) ||
                OntCE.ObjectMaxCardinality.class.equals(view) ||
                OntCE.ObjectCardinality.class.equals(view)) {
            OntCE.CardinalityRestrictionCE<OntCE, OntOPE> _ce = (OntCE.CardinalityRestrictionCE<OntCE, OntOPE>) ce;
            ONTObject<? extends OWLObjectPropertyExpression> p = df.get(_ce.getOnProperty());
            ONTObject<? extends OWLClassExpression> c =
                    found.get(_ce.getValue() == null ? _ce.getModel().getOWLThing() : _ce.getValue(),
                            v -> calcClassExpression(v, df, seen));
            OWLObjectCardinalityRestriction owl;
            if (OntCE.ObjectMinCardinality.class.equals(view))
                owl = df.getOWLDataFactory().getOWLObjectMinCardinality(_ce.getCardinality(), p.getObject(), c.getObject());
            else if (OntCE.ObjectMaxCardinality.class.equals(view))
                owl = df.getOWLDataFactory().getOWLObjectMaxCardinality(_ce.getCardinality(), p.getObject(), c.getObject());
            else if (OntCE.ObjectCardinality.class.equals(view))
                owl = df.getOWLDataFactory().getOWLObjectExactCardinality(_ce.getCardinality(), p.getObject(), c.getObject());
            else
                throw new OntApiException("Should never happen");
            return ONTObject.create(owl, _ce).append(p).append(c);
        }
        if (OntCE.DataMinCardinality.class.equals(view) ||
                OntCE.DataMaxCardinality.class.equals(view) ||
                OntCE.DataCardinality.class.equals(view)) {
            OntCE.CardinalityRestrictionCE<OntDR, OntNDP> _ce = (OntCE.CardinalityRestrictionCE<OntDR, OntNDP>) ce;
            ONTObject<OWLDataProperty> p = df.get(_ce.getOnProperty());
            ONTObject<? extends OWLDataRange> d = df.get(_ce.getValue() == null ? _ce.getModel().getOntEntity(OntDT.class, RDFS.Literal) : _ce.getValue());
            OWLDataCardinalityRestriction owl;
            if (OntCE.DataMinCardinality.class.equals(view))
                owl = df.getOWLDataFactory().getOWLDataMinCardinality(_ce.getCardinality(), p.getObject(), d.getObject());
            else if (OntCE.DataMaxCardinality.class.equals(view))
                owl = df.getOWLDataFactory().getOWLDataMaxCardinality(_ce.getCardinality(), p.getObject(), d.getObject());
            else if (OntCE.DataCardinality.class.equals(view))
                owl = df.getOWLDataFactory().getOWLDataExactCardinality(_ce.getCardinality(), p.getObject(), d.getObject());
            else
                throw new OntApiException("Should never happen");
            return ONTObject.create(owl, _ce).append(p).append(d);
        }
        if (OntCE.HasSelf.class.equals(view)) {
            OntCE.HasSelf _ce = (OntCE.HasSelf) ce;
            ONTObject<? extends OWLObjectPropertyExpression> p = df.get(_ce.getOnProperty());
            return ONTObject.create(df.getOWLDataFactory().getOWLObjectHasSelf(p.getObject()), _ce).append(p);
        }
        if (OntCE.UnionOf.class.equals(view) ||
                OntCE.IntersectionOf.class.equals(view)) {
            OntCE.ComponentsCE<OntCE> _ce = (OntCE.ComponentsCE<OntCE>) ce;
            Set<ONTObject<? extends OWLClassExpression>> components = _ce.components()
                    .map(c -> found.get(c, v -> calcClassExpression(v, df, seen))).collect(Collectors.toSet());
            OWLClassExpression owl;
            if (OntCE.UnionOf.class.equals(view))
                owl = df.getOWLDataFactory().getOWLObjectUnionOf(components.stream().map(ONTObject::getObject));
            else if (OntCE.IntersectionOf.class.equals(view))
                owl = df.getOWLDataFactory().getOWLObjectIntersectionOf(components.stream().map(ONTObject::getObject));
            else
                throw new OntApiException("Should never happen");
            return ONTObject.create(owl, _ce).appendWildcards(components);
        }
        if (OntCE.OneOf.class.equals(view)) {
            OntCE.OneOf _ce = (OntCE.OneOf) ce;
            Set<ONTObject<? extends OWLIndividual>> components = _ce.components()
                    .map(df::get).collect(Collectors.toSet());
            OWLClassExpression owl = df.getOWLDataFactory().getOWLObjectOneOf(components.stream().map(ONTObject::getObject));
            return ONTObject.create(owl, _ce).appendWildcards(components);
        }
        if (ce instanceof OntCE.ComplementOf) {
            OntCE.ComplementOf _ce = (OntCE.ComplementOf) ce;
            ONTObject<? extends OWLClassExpression> c = found.get(_ce.getValue(), v -> calcClassExpression(v, df, seen));
            return ONTObject.create(df.getOWLDataFactory().getOWLObjectComplementOf(c.getObject()), _ce).append(c);
        }
        throw new OntApiException("Unsupported class expression " + ce);
    }

    /**
     * @param var {@link OntSWRL.Variable}
     * @param df  {@link InternalDataFactory}
     * @return {@link ONTObject} around {@link SWRLVariable}
     */
    public static ONTObject<SWRLVariable> getSWRLVariable(OntSWRL.Variable var, InternalDataFactory df) {
        if (!OntApiException.notNull(var, "Null swrl var").isURIResource()) {
            throw new OntApiException("Anonymous swrl var " + var);
        }
        return ONTObject.create(df.getOWLDataFactory().getSWRLVariable(df.toIRI(var.getURI())), var);
    }

    /**
     * @param arg {@link OntSWRL.DArg}
     * @param df  {@link InternalDataFactory}
     * @return {@link ONTObject} around {@link SWRLDArgument}
     */
    public static ONTObject<? extends SWRLDArgument> getSWRLLiteralArg(OntSWRL.DArg arg, InternalDataFactory df) {
        if (OntApiException.notNull(arg, "Null SWRL-D arg").isLiteral()) {
            return ONTObject.create(df.getOWLDataFactory().getSWRLLiteralArgument(df.get(arg.asLiteral()).getObject()), arg);
        }
        if (arg.canAs(OntSWRL.Variable.class)) {
            return getSWRLVariable(arg.as(OntSWRL.Variable.class), df);
        }
        throw new OntApiException("Unsupported SWRL-D arg " + arg);
    }

    /**
     * @param arg {@link OntSWRL.IArg}
     * @param df  {@link InternalDataFactory}
     * @return {@link ONTObject} around {@link SWRLIArgument}
     */
    public static ONTObject<? extends SWRLIArgument> getSWRLIndividualArg(OntSWRL.IArg arg, InternalDataFactory df) {
        if (OntApiException.notNull(arg, "Null SWRL-I arg").canAs(OntIndividual.class)) {
            return ONTObject.create(df.getOWLDataFactory().getSWRLIndividualArgument(df.get(arg.as(OntIndividual.class)).getObject()), arg);
        }
        if (arg.canAs(OntSWRL.Variable.class)) {
            return getSWRLVariable(arg.as(OntSWRL.Variable.class), df);
        }
        throw new OntApiException("Unsupported SWRL-I arg " + arg);
    }

    /**
     * @param atom {@link OntSWRL.Atom}
     * @param df   {@link InternalDataFactory}
     * @return {@link ONTObject} around {@link SWRLAtom}
     */
    @SuppressWarnings("unchecked")
    public static ONTObject<? extends SWRLAtom> calcSWRLAtom(OntSWRL.Atom atom, InternalDataFactory df) {
        Class<? extends OntObject> view = OntApiException.notNull(((OntObjectImpl) OntApiException.notNull(atom, "Null SWRL atom.")).getActualClass(),
                "Can't determine view of SWRL atom " + atom);
        if (OntSWRL.Atom.BuiltIn.class.equals(view)) {
            OntSWRL.Atom.BuiltIn _atom = (OntSWRL.Atom.BuiltIn) atom;
            IRI iri = df.toIRI(_atom.getPredicate().getURI());
            List<ONTObject<? extends SWRLDArgument>> arguments = _atom.arguments().map(a -> getSWRLLiteralArg(a, df)).collect(Collectors.toList());
            SWRLAtom res = df.getOWLDataFactory().getSWRLBuiltInAtom(iri, arguments.stream().map(ONTObject::getObject).collect(Collectors.toList()));
            return ONTObject.create(res, _atom).appendWildcards(arguments);
        }
        if (OntSWRL.Atom.OntClass.class.equals(view)) {
            OntSWRL.Atom.OntClass _atom = (OntSWRL.Atom.OntClass) atom;
            ONTObject<? extends OWLClassExpression> c = df.get(_atom.getPredicate());
            ONTObject<? extends SWRLIArgument> a = getSWRLIndividualArg(_atom.getArg(), df);
            return ONTObject.create(df.getOWLDataFactory().getSWRLClassAtom(c.getObject(), a.getObject()), _atom).append(c).append(a);
        }
        if (OntSWRL.Atom.DataProperty.class.equals(view)) {
            OntSWRL.Atom.DataProperty _atom = (OntSWRL.Atom.DataProperty) atom;
            ONTObject<OWLDataProperty> p = df.get(_atom.getPredicate());
            ONTObject<? extends SWRLIArgument> f = getSWRLIndividualArg(_atom.getFirstArg(), df);
            ONTObject<? extends SWRLDArgument> s = getSWRLLiteralArg(_atom.getSecondArg(), df);
            return ONTObject.create(df.getOWLDataFactory().getSWRLDataPropertyAtom(p.getObject(), f.getObject(), s.getObject()), _atom).append(p).append(f).append(s);
        }
        if (OntSWRL.Atom.ObjectProperty.class.equals(view)) {
            OntSWRL.Atom.ObjectProperty _atom = (OntSWRL.Atom.ObjectProperty) atom;
            ONTObject<? extends OWLObjectPropertyExpression> p = df.get(_atom.getPredicate());
            ONTObject<? extends SWRLIArgument> f = getSWRLIndividualArg(_atom.getFirstArg(), df);
            ONTObject<? extends SWRLIArgument> s = getSWRLIndividualArg(_atom.getSecondArg(), df);
            return ONTObject.create(df.getOWLDataFactory().getSWRLObjectPropertyAtom(p.getObject(), f.getObject(), s.getObject()), _atom).append(p).append(f).append(s);
        }
        if (OntSWRL.Atom.DataRange.class.equals(view)) {
            OntSWRL.Atom.DataRange _atom = (OntSWRL.Atom.DataRange) atom;
            ONTObject<? extends OWLDataRange> d = df.get(_atom.getPredicate());
            ONTObject<? extends SWRLDArgument> a = getSWRLLiteralArg(_atom.getArg(), df);
            return ONTObject.create(df.getOWLDataFactory().getSWRLDataRangeAtom(d.getObject(), a.getObject()), _atom).append(d).append(a);
        }
        if (OntSWRL.Atom.DifferentIndividuals.class.equals(view)) {
            OntSWRL.Atom.DifferentIndividuals _atom = (OntSWRL.Atom.DifferentIndividuals) atom;
            ONTObject<? extends SWRLIArgument> f = getSWRLIndividualArg(_atom.getFirstArg(), df);
            ONTObject<? extends SWRLIArgument> s = getSWRLIndividualArg(_atom.getSecondArg(), df);
            return ONTObject.create(df.getOWLDataFactory().getSWRLDifferentIndividualsAtom(f.getObject(), s.getObject()), _atom).append(f).append(s);
        }
        if (OntSWRL.Atom.SameIndividuals.class.equals(view)) {
            OntSWRL.Atom.SameIndividuals _atom = (OntSWRL.Atom.SameIndividuals) atom;
            ONTObject<? extends SWRLIArgument> f = getSWRLIndividualArg(_atom.getFirstArg(), df);
            ONTObject<? extends SWRLIArgument> s = getSWRLIndividualArg(_atom.getSecondArg(), df);
            return ONTObject.create(df.getOWLDataFactory().getSWRLSameIndividualAtom(f.getObject(), s.getObject()), _atom).append(f).append(s);
        }
        throw new OntApiException("Unsupported SWRL atom " + atom);
    }

}
