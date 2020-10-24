/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2020, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.internal;

import com.github.owlcs.ontapi.DataFactory;
import com.github.owlcs.ontapi.OntApiException;
import com.github.owlcs.ontapi.config.AxiomsSettings;
import com.github.owlcs.ontapi.jena.model.*;
import com.github.owlcs.ontapi.jena.utils.Iter;
import com.github.owlcs.ontapi.jena.utils.OntModels;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWLFacet;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A collection of helper methods to perform translation of RDF-Graph to the {@code OWLObject}s (i.e. reading from a {@code Graph}).
 * <p>
 * Created by @szuev on 25.11.2016.
 *
 * @see WriteHelper
 */
@SuppressWarnings("WeakerAccess")
public class ReadHelper {

    /**
     * Auxiliary method for simplification code.
     * Used in Annotation Translators.
     * If the specified statement also belongs to the another type of axiom
     * and such situation is prohibited in the config then returns {@code false}.
     * This is for three kinds of statements:
     * <ul>
     * <li>{@code A1 rdfs:subPropertyOf A2}</li>
     * <li>{@code A rdfs:domain U}</li>
     * <li>{@code A rdfs:range U}</li>
     * </ul>
     * Each of them is wider than the analogous statement for object or data property,
     * e.g. {@code P rdfs:range C} could be treated as {@code A rdfs:range U}, but not vice versa.
     *
     * @param statement {@link OntStatement} to test, not {@code null}
     * @param conf      {@link AxiomsSettings}, not {@code null}
     * @param o         {@link AxiomType#SUB_OBJECT_PROPERTY}
     *                                                       or {@link AxiomType#OBJECT_PROPERTY_DOMAIN}
     *                                                       or {@link AxiomType#OBJECT_PROPERTY_RANGE}
     * @param d         {@link AxiomType#SUB_DATA_PROPERTY}
     *                                                     or {@link AxiomType#DATA_PROPERTY_DOMAIN}
     *                                                     or {@link AxiomType#DATA_PROPERTY_RANGE}
     * @return {@code true} if the statement is good to be represented in the form of annotation axiom
     */
    public static boolean testAnnotationAxiomOverlaps(OntStatement statement,
                                                      AxiomsSettings conf,
                                                      AxiomType<? extends OWLObjectPropertyAxiom> o,
                                                      AxiomType<? extends OWLDataPropertyAxiom> d) {
        return !conf.isIgnoreAnnotationAxiomOverlaps() ||
                Iter.noneMatch(Iter.of(o, d).mapWith(AxiomParserProvider::get), a -> a.testStatement(statement, conf));
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
     * @param s      {@link OntStatement}, not {@code null}
     * @param config {@link AxiomsSettings}, not {@code null}
     * @return {@code true} if the specified statement is annotation property assertion
     */
    public static boolean isAnnotationAssertionStatement(OntStatement s, AxiomsSettings config) {
        return s.isAnnotationAssertion()
                && !s.belongsToAnnotation()
                && (config.isAllowBulkAnnotationAssertions() || !s.hasAnnotations());
    }

    /**
     * Lists all {@link OWLAnnotation}s which are associated with the specified statement.
     * All {@code OWLAnnotation}s are provided in the form of {@link ONTObject}-wrappers.
     *
     * @param statement {@link OntStatement}, axiom root statement, not {@code null}
     * @param conf      {@link AxiomsSettings}, not {@code null}
     * @param factory   {@link ONTObjectFactory}
     * @return a set of wraps {@link ONTObject} around {@link OWLAnnotation}
     */
    public static ExtendedIterator<ONTObject<OWLAnnotation>> listAnnotations(OntStatement statement,
                                                                             AxiomsSettings conf,
                                                                             ONTObjectFactory factory) {
        ExtendedIterator<OntStatement> res = OntModels.listAnnotations(statement);
        if (conf.isLoadAnnotationAxioms() && isDeclarationStatement(statement)) {
            // for compatibility with OWL-API skip all plain annotations attached to an entity (or anonymous individual)
            // they would go separately as annotation-assertions.
            res = res.filterDrop(s -> isAnnotationAssertionStatement(s, conf));
        }
        return res.mapWith(factory::getAnnotation);
    }

    /**
     * Lists all annotations related to the object (including assertions).
     *
     * @param obj {@link OntObject}
     * @param of  {@link ONTObjectFactory}
     * @return {@link ExtendedIterator} of {@link ONTObject}s of {@link OWLAnnotation}
     */
    public static ExtendedIterator<ONTObject<OWLAnnotation>> listOWLAnnotations(OntObject obj,
                                                                                ONTObjectFactory of) {
        return OntModels.listAnnotations(obj).mapWith(of::getAnnotation);
    }

    /**
     * Translates {@link OntStatement} to {@link ONTObject} encapsulated {@link OWLAnnotation}.
     *
     * @param ann {@link OntStatement}
     * @param of  {@link ONTObjectFactory}
     * @return {@link ONTObject} around {@link OWLAnnotation}
     */
    public static ONTObject<OWLAnnotation> getAnnotation(OntStatement ann, ONTObjectFactory of) {
        return ann.getSubject().getAs(OntAnnotation.class) != null ||
                ann.hasAnnotations() ?
                getHierarchicalAnnotations(ann, of) : getPlainAnnotation(ann, of);
    }

    private static ONTObject<OWLAnnotation> getPlainAnnotation(OntStatement ann, ONTObjectFactory of) {
        ONTObject<OWLAnnotationProperty> p = of.getProperty(ann.getPredicate().as(OntAnnotationProperty.class));
        ONTObject<? extends OWLAnnotationValue> v = of.getValue(ann.getObject());
        OWLAnnotation res = of.getOWLDataFactory().getOWLAnnotation(p.getOWLObject(), v.getOWLObject(), Stream.empty());
        return ONTWrapperImpl.create(res, ann).append(p).append(v);
    }

    private static ONTObject<OWLAnnotation> getHierarchicalAnnotations(OntStatement root, ONTObjectFactory of) {
        OntObject subject = root.getSubject();
        ONTObject<OWLAnnotationProperty> p = of.getProperty(root.getPredicate().as(OntAnnotationProperty.class));
        ONTObject<? extends OWLAnnotationValue> v = of.getValue(root.getObject());
        Set<? extends ONTObject<OWLAnnotation>> children = OntModels.listAnnotations(root)
                .mapWith(a -> getHierarchicalAnnotations(a, of)).toSet();
        OWLAnnotation object = of.getOWLDataFactory()
                .getOWLAnnotation(p.getOWLObject(), v.getOWLObject(), children.stream().map(ONTObject::getOWLObject));
        ONTWrapperImpl<OWLAnnotation> res = ONTWrapperImpl.create(object, root);
        OntAnnotation a;
        if ((a = subject.getAs(OntAnnotation.class)) != null) {
            res = res.append(a);
        }
        return res.append(p).append(v).append(children);
    }

    /**
     * Maps {@link OntFacetRestriction} =&gt; {@link OWLFacetRestriction}.
     *
     * @param fr {@link OntFacetRestriction}, not {@code null}
     * @param of {@link ONTObjectFactory}, not {@code null}
     * @return {@link ONTObject} around {@link OWLFacetRestriction}
     */
    public static ONTObject<OWLFacetRestriction> getFacetRestriction(OntFacetRestriction fr, ONTObjectFactory of) {
        OWLFacetRestriction res = calcOWLFacetRestriction(fr, of);
        return ONTWrapperImpl.create(res, fr);
    }

    /**
     * Creates an {@link OWLFacetRestriction} instance.
     *
     * @param fr {@link OntFacetRestriction}, not {@code null}
     * @param of {@link ONTObjectFactory}, not {@code null}
     * @return {@link OWLFacetRestriction}
     */
    public static OWLFacetRestriction calcOWLFacetRestriction(OntFacetRestriction fr, ONTObjectFactory of) {
        OWLLiteral literal = of.getLiteral(OntApiException.notNull(fr, "Null facet restriction.").getValue()).getOWLObject();
        Class<? extends OntFacetRestriction> type = OntModels.getOntType(fr);
        return of.getOWLDataFactory().getOWLFacetRestriction(getFacet(type), literal);
    }

    /**
     * Gets the facet by ONT-API type.
     *
     * @param type {@code Class}-type of {@link OntFacetRestriction}
     * @return {@link OWLFacet}
     * @see WriteHelper#getFRType(OWLFacet)
     */
    public static OWLFacet getFacet(Class<? extends OntFacetRestriction> type) {
        if (OntFacetRestriction.Length.class == type)
            return OWLFacet.LENGTH;
        if (OntFacetRestriction.MinLength.class == type)
            return OWLFacet.MIN_LENGTH;
        if (OntFacetRestriction.MaxLength.class == type)
            return OWLFacet.MAX_LENGTH;
        if (OntFacetRestriction.MinInclusive.class == type)
            return OWLFacet.MIN_INCLUSIVE;
        if (OntFacetRestriction.MaxInclusive.class == type)
            return OWLFacet.MAX_INCLUSIVE;
        if (OntFacetRestriction.MinExclusive.class == type)
            return OWLFacet.MIN_EXCLUSIVE;
        if (OntFacetRestriction.MaxExclusive.class == type)
            return OWLFacet.MAX_EXCLUSIVE;
        if (OntFacetRestriction.Pattern.class == type)
            return OWLFacet.PATTERN;
        if (OntFacetRestriction.FractionDigits.class == type)
            return OWLFacet.FRACTION_DIGITS;
        if (OntFacetRestriction.TotalDigits.class == type)
            return OWLFacet.TOTAL_DIGITS;
        if (OntFacetRestriction.LangRange.class == type)
            return OWLFacet.LANG_RANGE;
        throw new OntApiException.IllegalArgument("Unsupported facet restriction " + type);
    }

    /**
     * Calculates an {@link OWLDataRange} wrapped by {@link ONTObject}.
     * Note: this method is recursive.
     *
     * @param dr   {@link OntDataRange Ontology Data Range} to map
     * @param of   {@link ONTObjectFactory}
     * @param seen Set of {@link Resource}
     * @return {@link ONTObject} around {@link OWLDataRange}
     * @throws OntApiException if something is wrong.
     */
    @SuppressWarnings("unchecked")
    public static ONTObject<? extends OWLDataRange> calcDataRange(OntDataRange dr,
                                                                  ONTObjectFactory of,
                                                                  Set<Resource> seen) {
        if (OntApiException.notNull(dr, "Null data range").isURIResource()) {
            return of.getDatatype(dr.as(OntDataRange.Named.class));
        }
        if (seen.contains(dr)) {
            throw new OntApiException("Recursive loop on data range " + dr);
        }
        seen.add(dr);
        DataFactory df = of.getOWLDataFactory();
        if (dr instanceof OntDataRange.Restriction) {
            OntDataRange.Restriction _dr = (OntDataRange.Restriction) dr;
            ONTObject<OWLDatatype> d = of.getDatatype(_dr.getValue());
            Set<ONTObject<OWLFacetRestriction>> restrictions = OntModels.listMembers(_dr.getList())
                    .mapWith(of::getFacetRestriction).toSet();
            OWLDataRange res = df.getOWLDatatypeRestriction(d.getOWLObject(),
                    restrictions.stream().map(ONTObject::getOWLObject).collect(Collectors.toList()));
            return ONTWrapperImpl.create(res, dr).append(restrictions);
        }
        if (dr instanceof OntDataRange.ComplementOf) {
            OntDataRange.ComplementOf _dr = (OntDataRange.ComplementOf) dr;
            ONTObject<? extends OWLDataRange> d = calcDataRange(_dr.getValue(), of, seen);
            return ONTWrapperImpl.create(df.getOWLDataComplementOf(d.getOWLObject()), _dr).append(d);
        }
        if (dr instanceof OntDataRange.UnionOf || dr instanceof OntDataRange.IntersectionOf) {
            OntDataRange.ComponentsDR<OntDataRange> _dr = (OntDataRange.ComponentsDR<OntDataRange>) dr;
            Set<ONTObject<OWLDataRange>> dataRanges = OntModels.listMembers(_dr.getList())
                    .mapWith(d -> (ONTObject<OWLDataRange>) calcDataRange(d, of, seen)).toSet();
            OWLDataRange res = dr instanceof OntDataRange.UnionOf ?
                    df.getOWLDataUnionOf(dataRanges.stream().map(ONTObject::getOWLObject)) :
                    df.getOWLDataIntersectionOf(dataRanges.stream().map(ONTObject::getOWLObject));
            return ONTWrapperImpl.create(res, dr).append(dataRanges);
        }
        if (dr instanceof OntDataRange.OneOf) {
            OntDataRange.OneOf _dr = (OntDataRange.OneOf) dr;
            Set<ONTObject<OWLLiteral>> literals = _dr.getList().members().map(of::getLiteral)
                    .collect(Collectors.toSet());
            OWLDataRange res = df.getOWLDataOneOf(literals.stream().map(ONTObject::getOWLObject));
            return ONTWrapperImpl.create(res, _dr);
        }
        throw new OntApiException("Unsupported data range expression " + dr);
    }

    /**
     * Calculates an {@link OWLClassExpression} wrapped by {@link ONTObject}.
     * Note: this method is recursive.
     *
     * @param ce   {@link OntClass Ontology Class Expression} to map
     * @param of   {@link ONTObjectFactory}
     * @param seen Set of {@link Resource},
     *             a subsidiary collection to prevent possible graph recursions
     *             (e.g. {@code _:b0 owl:complementOf _:b0})
     * @return {@link ONTObject} around {@link OWLClassExpression}
     * @throws OntApiException if something is wrong.
     */
    @SuppressWarnings("unchecked")
    public static ONTObject<? extends OWLClassExpression> calcClassExpression(OntClass ce,
                                                                              ONTObjectFactory of,
                                                                              Set<Resource> seen) {
        if (OntApiException.notNull(ce, "Null class expression").isURIResource()) {
            return of.getClass(ce.as(OntClass.Named.class));
        }
        if (!seen.add(ce)) {
            throw new OntApiException("Recursive loop on class expression " + ce);
        }
        DataFactory df = of.getOWLDataFactory();
        Class<? extends OntObject> type = OntModels.getOntType(ce);
        if (OntClass.ObjectSomeValuesFrom.class.equals(type) || OntClass.ObjectAllValuesFrom.class.equals(type)) {
            OntClass.ComponentRestrictionCE<OntClass, OntObjectProperty> _ce = (OntClass.ComponentRestrictionCE<OntClass, OntObjectProperty>) ce;
            ONTObject<? extends OWLObjectPropertyExpression> p = of.getProperty(_ce.getProperty());
            ONTObject<? extends OWLClassExpression> c = calcClassExpression(_ce.getValue(), of, seen);
            OWLClassExpression owl;
            if (OntClass.ObjectSomeValuesFrom.class.equals(type)) {
                owl = df.getOWLObjectSomeValuesFrom(p.getOWLObject(), c.getOWLObject());
            } else {
                owl = df.getOWLObjectAllValuesFrom(p.getOWLObject(), c.getOWLObject());
            }
            return ONTWrapperImpl.create(owl, _ce).append(p).append(c);
        }
        if (OntClass.DataSomeValuesFrom.class.equals(type) || OntClass.DataAllValuesFrom.class.equals(type)) {
            OntClass.ComponentRestrictionCE<OntDataRange, OntDataProperty> _ce = (OntClass.ComponentRestrictionCE<OntDataRange, OntDataProperty>) ce;
            ONTObject<OWLDataProperty> p = of.getProperty(_ce.getProperty());
            ONTObject<? extends OWLDataRange> d = of.getDatatype(_ce.getValue());
            OWLClassExpression owl;
            if (OntClass.DataSomeValuesFrom.class.equals(type)) {
                owl = df.getOWLDataSomeValuesFrom(p.getOWLObject(), d.getOWLObject());
            } else {
                owl = df.getOWLDataAllValuesFrom(p.getOWLObject(), d.getOWLObject());
            }
            return ONTWrapperImpl.create(owl, _ce).append(p).append(d);
        }
        if (OntClass.ObjectHasValue.class.equals(type)) {
            OntClass.ObjectHasValue _ce = (OntClass.ObjectHasValue) ce;
            ONTObject<? extends OWLObjectPropertyExpression> p = of.getProperty(_ce.getProperty());
            ONTObject<? extends OWLIndividual> i = of.getIndividual(_ce.getValue());
            return ONTWrapperImpl.create(df.getOWLObjectHasValue(p.getOWLObject(), i.getOWLObject()), _ce).append(p).append(i);
        }
        if (OntClass.DataHasValue.class.equals(type)) {
            OntClass.DataHasValue _ce = (OntClass.DataHasValue) ce;
            ONTObject<OWLDataProperty> p = of.getProperty(_ce.getProperty());
            ONTObject<OWLLiteral> l = of.getLiteral(_ce.getValue());
            return ONTWrapperImpl.create(df.getOWLDataHasValue(p.getOWLObject(), l.getOWLObject()), _ce).append(p);
        }
        if (OntClass.ObjectMinCardinality.class.equals(type)
                || OntClass.ObjectMaxCardinality.class.equals(type)
                || OntClass.ObjectCardinality.class.equals(type)) {
            OntClass.CardinalityRestrictionCE<OntClass, OntObjectProperty> _ce = (OntClass.CardinalityRestrictionCE<OntClass, OntObjectProperty>) ce;
            ONTObject<? extends OWLObjectPropertyExpression> p = of.getProperty(_ce.getProperty());
            ONTObject<? extends OWLClassExpression> c = calcClassExpression(_ce.getValue() == null ?
                    _ce.getModel().getOWLThing() : _ce.getValue(), of, seen);
            OWLObjectCardinalityRestriction owl;
            if (OntClass.ObjectMinCardinality.class.equals(type)) {
                owl = df.getOWLObjectMinCardinality(_ce.getCardinality(), p.getOWLObject(), c.getOWLObject());
            } else if (OntClass.ObjectMaxCardinality.class.equals(type)) {
                owl = df.getOWLObjectMaxCardinality(_ce.getCardinality(), p.getOWLObject(), c.getOWLObject());
            } else {
                owl = df.getOWLObjectExactCardinality(_ce.getCardinality(), p.getOWLObject(), c.getOWLObject());
            }
            return ONTWrapperImpl.create(owl, _ce).append(p).append(c);
        }
        if (OntClass.DataMinCardinality.class.equals(type)
                || OntClass.DataMaxCardinality.class.equals(type)
                || OntClass.DataCardinality.class.equals(type)) {
            OntClass.CardinalityRestrictionCE<OntDataRange, OntDataProperty> _ce = (OntClass.CardinalityRestrictionCE<OntDataRange, OntDataProperty>) ce;
            ONTObject<OWLDataProperty> p = of.getProperty(_ce.getProperty());
            ONTObject<? extends OWLDataRange> d = of.getDatatype(_ce.getValue() == null ?
                    _ce.getModel().getOntEntity(OntDataRange.Named.class, RDFS.Literal) : _ce.getValue());
            OWLDataCardinalityRestriction owl;
            if (OntClass.DataMinCardinality.class.equals(type)) {
                owl = df.getOWLDataMinCardinality(_ce.getCardinality(), p.getOWLObject(), d.getOWLObject());
            } else if (OntClass.DataMaxCardinality.class.equals(type)) {
                owl = df.getOWLDataMaxCardinality(_ce.getCardinality(), p.getOWLObject(), d.getOWLObject());
            } else {
                owl = df.getOWLDataExactCardinality(_ce.getCardinality(), p.getOWLObject(), d.getOWLObject());
            }
            return ONTWrapperImpl.create(owl, _ce).append(p).append(d);
        }
        if (OntClass.HasSelf.class.equals(type)) {
            OntClass.HasSelf _ce = (OntClass.HasSelf) ce;
            ONTObject<? extends OWLObjectPropertyExpression> p = of.getProperty(_ce.getProperty());
            return ONTWrapperImpl.create(df.getOWLObjectHasSelf(p.getOWLObject()), _ce).append(p);
        }
        if (OntClass.UnionOf.class.equals(type) || OntClass.IntersectionOf.class.equals(type)) {
            OntClass.ComponentsCE<OntClass> _ce = (OntClass.ComponentsCE<OntClass>) ce;
            Set<ONTObject<OWLClassExpression>> components = OntModels.listMembers(_ce.getList())
                    .mapWith(c -> (ONTObject<OWLClassExpression>) calcClassExpression(c, of, seen))
                    .toSet();
            OWLClassExpression owl;
            if (OntClass.UnionOf.class.equals(type)) {
                owl = df.getOWLObjectUnionOf(components.stream().map(ONTObject::getOWLObject));
            } else {
                owl = df.getOWLObjectIntersectionOf(components.stream().map(ONTObject::getOWLObject));
            }
            return ONTWrapperImpl.create(owl, _ce).append(components);
        }
        if (OntClass.OneOf.class.equals(type)) {
            OntClass.OneOf _ce = (OntClass.OneOf) ce;
            Set<ONTObject<OWLIndividual>> components = OntModels.listMembers(_ce.getList())
                    .mapWith(i -> (ONTObject<OWLIndividual>) of.getIndividual(i)).toSet();
            OWLClassExpression owl = df.getOWLObjectOneOf(components.stream().map(ONTObject::getOWLObject));
            return ONTWrapperImpl.create(owl, _ce).append(components);
        }
        if (ce instanceof OntClass.ComplementOf) {
            OntClass.ComplementOf _ce = (OntClass.ComplementOf) ce;
            ONTObject<? extends OWLClassExpression> c = calcClassExpression(_ce.getValue(), of, seen);
            return ONTWrapperImpl.create(df.getOWLObjectComplementOf(c.getOWLObject()), _ce).append(c);
        }
        if (ce instanceof OntClass.NaryRestrictionCE) {
            OntClass.NaryRestrictionCE<OntDataRange, OntDataProperty> _ce = (OntClass.NaryRestrictionCE<OntDataRange, OntDataProperty>) ce;
            ONTObject<OWLDataProperty> p = of.getProperty(_ce.getProperty());
            ONTObject<? extends OWLDataRange> d = of.getDatatype(_ce.getValue());
            OWLClassExpression owl;
            if (OntClass.NaryDataSomeValuesFrom.class.equals(type)) {
                owl = df.getOWLDataSomeValuesFrom(p.getOWLObject(), d.getOWLObject());
            } else {
                owl = df.getOWLDataAllValuesFrom(p.getOWLObject(), d.getOWLObject());
            }
            return ONTWrapperImpl.create(owl, _ce).append(p).append(d);

        }
        throw new OntApiException("Unsupported class expression " + ce);
    }

    /**
     * @param var {@link OntSWRL.Variable}
     * @param of  {@link ONTObjectFactory}
     * @return {@link ONTObject} around {@link SWRLVariable}
     */
    public static ONTObject<SWRLVariable> getSWRLVariable(OntSWRL.Variable var, ONTObjectFactory of) {
        if (!OntApiException.notNull(var, "Null swrl var").isURIResource()) {
            throw new OntApiException("Anonymous swrl var " + var);
        }
        return ONTWrapperImpl.create(of.getOWLDataFactory().getSWRLVariable(of.toIRI(var.getURI())), var);
    }

    /**
     * @param arg {@link OntSWRL.DArg}
     * @param of  {@link ONTObjectFactory}
     * @return {@link ONTObject} around {@link SWRLDArgument}
     */
    public static ONTObject<? extends SWRLDArgument> getSWRLLiteralArg(OntSWRL.DArg arg, ONTObjectFactory of) {
        if (OntApiException.notNull(arg, "Null SWRL-D arg").isLiteral()) {
            return ONTWrapperImpl.create(of.getOWLDataFactory()
                    .getSWRLLiteralArgument(of.getLiteral(arg.asLiteral()).getOWLObject()), arg);
        }
        if (arg.canAs(OntSWRL.Variable.class)) {
            return of.getSWRLVariable(arg.as(OntSWRL.Variable.class));
        }
        throw new OntApiException("Unsupported SWRL-D arg " + arg);
    }

    /**
     * @param arg {@link OntSWRL.IArg}
     * @param of  {@link ONTObjectFactory}
     * @return {@link ONTObject} around {@link SWRLIArgument}
     */
    public static ONTObject<? extends SWRLIArgument> getSWRLIndividualArg(OntSWRL.IArg arg, ONTObjectFactory of) {
        if (OntApiException.notNull(arg, "Null SWRL-I arg").canAs(OntIndividual.class)) {
            return ONTWrapperImpl.create(of.getOWLDataFactory()
                    .getSWRLIndividualArgument(of.getIndividual(arg.as(OntIndividual.class)).getOWLObject()), arg);
        }
        if (arg.canAs(OntSWRL.Variable.class)) {
            return of.getSWRLVariable(arg.as(OntSWRL.Variable.class));
        }
        throw new OntApiException("Unsupported SWRL-I arg " + arg);
    }

    /**
     * @param atom {@link OntSWRL.Atom}
     * @param of   {@link ONTObjectFactory}
     * @return {@link ONTObject} around {@link SWRLAtom}
     */
    public static ONTObject<? extends SWRLAtom> calcSWRLAtom(OntSWRL.Atom<?> atom, ONTObjectFactory of) {
        if (atom instanceof OntSWRL.Atom.WithBuiltin) {
            OntSWRL.Atom.WithBuiltin _atom = (OntSWRL.Atom.WithBuiltin) atom;
            IRI iri = of.toIRI(_atom.getPredicate().getURI());
            List<ONTObject<? extends SWRLDArgument>> arguments = _atom.arguments().map(of::getSWRLArgument)
                    .collect(Collectors.toList());
            SWRLAtom res = of.getOWLDataFactory().getSWRLBuiltInAtom(iri, arguments.stream().map(ONTObject::getOWLObject)
                    .collect(Collectors.toList()));
            return ONTWrapperImpl.create(res, _atom).append(arguments);
        }
        if (atom instanceof OntSWRL.Atom.WithClass) {
            OntSWRL.Atom.WithClass _atom = (OntSWRL.Atom.WithClass) atom;
            ONTObject<? extends OWLClassExpression> c = of.getClass(_atom.getPredicate());
            ONTObject<? extends SWRLIArgument> a = of.getSWRLArgument(_atom.getArg());
            return ONTWrapperImpl.create(of.getOWLDataFactory().getSWRLClassAtom(c.getOWLObject(), a.getOWLObject()), _atom)
                    .append(c).append(a);
        }
        if (atom instanceof OntSWRL.Atom.WithDataProperty) {
            OntSWRL.Atom.WithDataProperty _atom = (OntSWRL.Atom.WithDataProperty) atom;
            ONTObject<OWLDataProperty> p = of.getProperty(_atom.getPredicate());
            ONTObject<? extends SWRLIArgument> f = of.getSWRLArgument(_atom.getFirstArg());
            ONTObject<? extends SWRLDArgument> s = of.getSWRLArgument(_atom.getSecondArg());
            return ONTWrapperImpl.create(of.getOWLDataFactory()
                    .getSWRLDataPropertyAtom(p.getOWLObject(), f.getOWLObject(), s.getOWLObject()), _atom)
                    .append(p).append(f).append(s);
        }
        if (atom instanceof OntSWRL.Atom.WithObjectProperty) {
            OntSWRL.Atom.WithObjectProperty _atom = (OntSWRL.Atom.WithObjectProperty) atom;
            ONTObject<? extends OWLObjectPropertyExpression> p = of.getProperty(_atom.getPredicate());
            ONTObject<? extends SWRLIArgument> f = of.getSWRLArgument(_atom.getFirstArg());
            ONTObject<? extends SWRLIArgument> s = of.getSWRLArgument(_atom.getSecondArg());
            return ONTWrapperImpl.create(of.getOWLDataFactory()
                    .getSWRLObjectPropertyAtom(p.getOWLObject(), f.getOWLObject(), s.getOWLObject()), _atom)
                    .append(p).append(f).append(s);
        }
        if (atom instanceof OntSWRL.Atom.WithDataRange) {
            OntSWRL.Atom.WithDataRange _atom = (OntSWRL.Atom.WithDataRange) atom;
            ONTObject<? extends OWLDataRange> d = of.getDatatype(_atom.getPredicate());
            ONTObject<? extends SWRLDArgument> a = of.getSWRLArgument(_atom.getArg());
            return ONTWrapperImpl.create(of.getOWLDataFactory()
                    .getSWRLDataRangeAtom(d.getOWLObject(), a.getOWLObject()), _atom).append(d).append(a);
        }
        if (atom instanceof OntSWRL.Atom.WithDifferentIndividuals) {
            OntSWRL.Atom.WithDifferentIndividuals _atom = (OntSWRL.Atom.WithDifferentIndividuals) atom;
            ONTObject<? extends SWRLIArgument> f = of.getSWRLArgument(_atom.getFirstArg());
            ONTObject<? extends SWRLIArgument> s = of.getSWRLArgument(_atom.getSecondArg());
            return ONTWrapperImpl.create(of.getOWLDataFactory()
                    .getSWRLDifferentIndividualsAtom(f.getOWLObject(), s.getOWLObject()), _atom).append(f).append(s);
        }
        if (atom instanceof OntSWRL.Atom.WithSameIndividuals) {
            OntSWRL.Atom.WithSameIndividuals _atom = (OntSWRL.Atom.WithSameIndividuals) atom;
            ONTObject<? extends SWRLIArgument> f = of.getSWRLArgument(_atom.getFirstArg());
            ONTObject<? extends SWRLIArgument> s = of.getSWRLArgument(_atom.getSecondArg());
            return ONTWrapperImpl.create(of.getOWLDataFactory()
                    .getSWRLSameIndividualAtom(f.getOWLObject(), s.getOWLObject()), _atom).append(f).append(s);
        }
        throw new OntApiException("Unsupported SWRL atom " + atom);
    }

}
