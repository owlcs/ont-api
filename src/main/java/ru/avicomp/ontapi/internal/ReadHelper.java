/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2019, Avicomp Services, AO
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
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWLFacet;
import ru.avicomp.ontapi.DataFactory;
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.utils.Iter;
import ru.avicomp.ontapi.jena.utils.OntModels;

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
     * @param statement {@link OntStatement} to test
     * @param conf      {@link InternalConfig}
     * @param o         {@link AxiomType#SUB_OBJECT_PROPERTY}
     *                                                       or {@link AxiomType#OBJECT_PROPERTY_DOMAIN}
     *                                                       or {@link AxiomType#OBJECT_PROPERTY_RANGE}
     * @param d         {@link AxiomType#SUB_DATA_PROPERTY}
     *                                                     or {@link AxiomType#DATA_PROPERTY_DOMAIN}
     *                                                     or {@link AxiomType#DATA_PROPERTY_RANGE}
     * @return {@code true} if the statement is good to be represented in the form of annotation axiom
     */
    public static boolean testAnnotationAxiomOverlaps(OntStatement statement,
                                                      InternalConfig conf,
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
     * @param config {@link InternalConfig}, not {@code null}
     * @return {@code true} if the specified statement is annotation property assertion
     */
    public static boolean isAnnotationAssertionStatement(OntStatement s, InternalConfig config) {
        return s.isAnnotation()
                && !s.isBulkAnnotation()
                && (config.isAllowBulkAnnotationAssertions() || !s.hasAnnotations());
    }

    /**
     * Returns the container with set of {@link OWLAnnotation} associated with the specified statement.
     *
     * @param statement {@link OntStatement}, axiom root statement, not {@code null}
     * @param conf      {@link InternalConfig}
     * @param of        {@link InternalObjectFactory}
     * @return a set of wraps {@link ONTObject} around {@link OWLAnnotation}
     */
    public static Set<ONTObject<OWLAnnotation>> getAnnotations(OntStatement statement,
                                                               InternalConfig conf,
                                                               InternalObjectFactory of) {
        ExtendedIterator<OntStatement> res = OntModels.listAnnotations(statement);
        if (conf.isLoadAnnotationAxioms() && isDeclarationStatement(statement)) {
            // for compatibility with OWL-API skip all plain annotations attached to an entity (or anonymous individual)
            // they would go separately as annotation-assertions.
            res = res.filterDrop(s -> isAnnotationAssertionStatement(s, conf));
        }
        return res.mapWith(of::get).toSet();
    }

    /**
     * Lists all annotations related to the object (including assertions).
     *
     * @param obj {@link OntObject}
     * @param of  {@link InternalObjectFactory}
     * @return {@link ExtendedIterator} of {@link ONTObject}s of {@link OWLAnnotation}
     */
    public static ExtendedIterator<ONTObject<OWLAnnotation>> listOWLAnnotations(OntObject obj,
                                                                                InternalObjectFactory of) {
        return OntModels.listAnnotations(obj).mapWith(of::get);
    }

    /**
     * Translates {@link OntStatement} to {@link ONTObject} encapsulated {@link OWLAnnotation}.
     *
     * @param ann {@link OntStatement}
     * @param of  {@link InternalObjectFactory}
     * @return {@link ONTObject} around {@link OWLAnnotation}
     */
    public static ONTObject<OWLAnnotation> getAnnotation(OntStatement ann, InternalObjectFactory of) {
        return ann.getSubject().getAs(OntAnnotation.class) != null ||
                ann.hasAnnotations() ?
                getHierarchicalAnnotations(ann, of) : getPlainAnnotation(ann, of);
    }

    private static ONTObject<OWLAnnotation> getPlainAnnotation(OntStatement ann, InternalObjectFactory of) {
        ONTObject<OWLAnnotationProperty> p = of.get(ann.getPredicate().as(OntNAP.class));
        ONTObject<? extends OWLAnnotationValue> v = of.getValue(ann.getObject());
        OWLAnnotation res = of.getOWLDataFactory().getOWLAnnotation(p.getOWLObject(), v.getOWLObject(), Stream.empty());
        return ONTObjectImpl.create(res, ann).append(p).append(v);
    }

    private static ONTObject<OWLAnnotation> getHierarchicalAnnotations(OntStatement root, InternalObjectFactory of) {
        OntObject subject = root.getSubject();
        ONTObject<OWLAnnotationProperty> p = of.get(root.getPredicate().as(OntNAP.class));
        ONTObject<? extends OWLAnnotationValue> v = of.getValue(root.getObject());
        Set<? extends ONTObject<OWLAnnotation>> children = OntModels.listAnnotations(root)
                .mapWith(a -> getHierarchicalAnnotations(a, of)).toSet();
        OWLAnnotation object = of.getOWLDataFactory()
                .getOWLAnnotation(p.getOWLObject(), v.getOWLObject(), children.stream().map(ONTObject::getOWLObject));
        ONTObjectImpl<OWLAnnotation> res = ONTObjectImpl.create(object, root);
        OntAnnotation a;
        if ((a = subject.getAs(OntAnnotation.class)) != null) {
            res = res.append(a);
        }
        return res.append(p).append(v).append(children);
    }

    /**
     * Maps {@link OntFR} =&gt; {@link OWLFacetRestriction}.
     *
     * @param fr {@link OntFR}, not {@code null}
     * @param of {@link InternalObjectFactory}, not {@code null}
     * @return {@link ONTObject} around {@link OWLFacetRestriction}
     */
    public static ONTObject<OWLFacetRestriction> getFacetRestriction(OntFR fr, InternalObjectFactory of) {
        OWLFacetRestriction res = calcOWLFacetRestriction(fr, of);
        return ONTObjectImpl.create(res, fr);
    }

    /**
     * Creates an {@link OWLFacetRestriction} instance.
     *
     * @param fr {@link OntFR}, not {@code null}
     * @param of {@link InternalObjectFactory}, not {@code null}
     * @return {@link OWLFacetRestriction}
     */
    public static OWLFacetRestriction calcOWLFacetRestriction(OntFR fr, InternalObjectFactory of) {
        OWLLiteral literal = of.get(OntApiException.notNull(fr, "Null facet restriction.").getValue()).getOWLObject();
        Class<? extends OntFR> type = OntModels.getOntType(fr);
        return of.getOWLDataFactory().getOWLFacetRestriction(getFacet(type), literal);
    }

    /**
     * Gets the facet by ONT-API type.
     *
     * @param type {@code Class}-type of {@link OntFR}
     * @return {@link OWLFacet}
     */
    public static OWLFacet getFacet(Class<? extends OntFR> type) {
        if (OntFR.Length.class == type)
            return OWLFacet.LENGTH;
        if (OntFR.MinLength.class == type)
            return OWLFacet.MIN_LENGTH;
        if (OntFR.MaxLength.class == type)
            return OWLFacet.MAX_LENGTH;
        if (OntFR.MinInclusive.class == type)
            return OWLFacet.MIN_INCLUSIVE;
        if (OntFR.MaxInclusive.class == type)
            return OWLFacet.MAX_INCLUSIVE;
        if (OntFR.MinExclusive.class == type)
            return OWLFacet.MIN_EXCLUSIVE;
        if (OntFR.MaxExclusive.class == type)
            return OWLFacet.MAX_EXCLUSIVE;
        if (OntFR.Pattern.class == type)
            return OWLFacet.PATTERN;
        if (OntFR.FractionDigits.class == type)
            return OWLFacet.FRACTION_DIGITS;
        if (OntFR.TotalDigits.class == type)
            return OWLFacet.TOTAL_DIGITS;
        if (OntFR.LangRange.class == type)
            return OWLFacet.LANG_RANGE;
        throw new OntApiException.IllegalArgument("Unsupported facet restriction " + type);
    }

    /**
     * Calculates an {@link OWLDataRange} wrapped by {@link ONTObject}.
     * Note: this method is recursive.
     *
     * @param dr   {@link OntDR Ontology Data Range} to map
     * @param of   {@link InternalObjectFactory}
     * @param seen Set of {@link Resource}
     * @return {@link ONTObject} around {@link OWLDataRange}
     * @throws OntApiException if something is wrong.
     */
    @SuppressWarnings("unchecked")
    public static ONTObject<? extends OWLDataRange> calcDataRange(OntDR dr,
                                                                  InternalObjectFactory of,
                                                                  Set<Resource> seen) {
        if (OntApiException.notNull(dr, "Null data range").isURIResource()) {
            return of.get(dr.as(OntDT.class));
        }
        if (seen.contains(dr)) {
            throw new OntApiException("Recursive loop on data range " + dr);
        }
        seen.add(dr);
        DataFactory df = of.getOWLDataFactory();
        if (dr instanceof OntDR.Restriction) {
            OntDR.Restriction _dr = (OntDR.Restriction) dr;
            ONTObject<OWLDatatype> d = of.get(_dr.getValue());
            Set<ONTObject<OWLFacetRestriction>> restrictions = OntModels.listMembers(_dr.getList())
                    .mapWith(of::get).toSet();
            OWLDataRange res = df.getOWLDatatypeRestriction(d.getOWLObject(),
                    restrictions.stream().map(ONTObject::getOWLObject).collect(Collectors.toList()));
            return ONTObjectImpl.create(res, dr).append(restrictions);
        }
        if (dr instanceof OntDR.ComplementOf) {
            OntDR.ComplementOf _dr = (OntDR.ComplementOf) dr;
            ONTObject<? extends OWLDataRange> d = calcDataRange(_dr.getValue(), of, seen);
            return ONTObjectImpl.create(df.getOWLDataComplementOf(d.getOWLObject()), _dr).append(d);
        }
        if (dr instanceof OntDR.UnionOf || dr instanceof OntDR.IntersectionOf) {
            OntDR.ComponentsDR<OntDR> _dr = (OntDR.ComponentsDR<OntDR>) dr;
            Set<ONTObject<OWLDataRange>> dataRanges = OntModels.listMembers(_dr.getList())
                    .mapWith(d -> (ONTObject<OWLDataRange>) calcDataRange(d, of, seen)).toSet();
            OWLDataRange res = dr instanceof OntDR.UnionOf ?
                    df.getOWLDataUnionOf(dataRanges.stream().map(ONTObject::getOWLObject)) :
                    df.getOWLDataIntersectionOf(dataRanges.stream().map(ONTObject::getOWLObject));
            return ONTObjectImpl.create(res, dr).append(dataRanges);
        }
        if (dr instanceof OntDR.OneOf) {
            OntDR.OneOf _dr = (OntDR.OneOf) dr;
            Set<ONTObject<OWLLiteral>> literals = _dr.getList().members().map(of::get)
                    .collect(Collectors.toSet());
            OWLDataRange res = df.getOWLDataOneOf(literals.stream().map(ONTObject::getOWLObject));
            return ONTObjectImpl.create(res, _dr);
        }
        throw new OntApiException("Unsupported data range expression " + dr);
    }

    /**
     * Calculates an {@link OWLClassExpression} wrapped by {@link ONTObject}.
     * Note: this method is recursive.
     *
     * @param ce   {@link OntCE Ontology Class Expression} to map
     * @param of   {@link InternalObjectFactory}
     * @param seen Set of {@link Resource},
     *             a subsidiary collection to prevent possible graph recursions
     *             (e.g. {@code _:b0 owl:complementOf _:b0})
     * @return {@link ONTObject} around {@link OWLClassExpression}
     * @throws OntApiException if something is wrong.
     */
    @SuppressWarnings("unchecked")
    public static ONTObject<? extends OWLClassExpression> calcClassExpression(OntCE ce,
                                                                              InternalObjectFactory of,
                                                                              Set<Resource> seen) {
        if (OntApiException.notNull(ce, "Null class expression").isURIResource()) {
            return of.get(ce.as(OntClass.class));
        }
        if (!seen.add(ce)) {
            throw new OntApiException("Recursive loop on class expression " + ce);
        }
        DataFactory df = of.getOWLDataFactory();
        Class<? extends OntObject> type = OntModels.getOntType(ce);
        if (OntCE.ObjectSomeValuesFrom.class.equals(type) || OntCE.ObjectAllValuesFrom.class.equals(type)) {
            OntCE.ComponentRestrictionCE<OntCE, OntOPE> _ce = (OntCE.ComponentRestrictionCE<OntCE, OntOPE>) ce;
            ONTObject<? extends OWLObjectPropertyExpression> p = of.get(_ce.getProperty());
            ONTObject<? extends OWLClassExpression> c = calcClassExpression(_ce.getValue(), of, seen);
            OWLClassExpression owl;
            if (OntCE.ObjectSomeValuesFrom.class.equals(type)) {
                owl = df.getOWLObjectSomeValuesFrom(p.getOWLObject(), c.getOWLObject());
            } else {
                owl = df.getOWLObjectAllValuesFrom(p.getOWLObject(), c.getOWLObject());
            }
            return ONTObjectImpl.create(owl, _ce).append(p).append(c);
        }
        if (OntCE.DataSomeValuesFrom.class.equals(type) || OntCE.DataAllValuesFrom.class.equals(type)) {
            OntCE.ComponentRestrictionCE<OntDR, OntNDP> _ce = (OntCE.ComponentRestrictionCE<OntDR, OntNDP>) ce;
            ONTObject<OWLDataProperty> p = of.get(_ce.getProperty());
            ONTObject<? extends OWLDataRange> d = of.get(_ce.getValue());
            OWLClassExpression owl;
            if (OntCE.DataSomeValuesFrom.class.equals(type)) {
                owl = df.getOWLDataSomeValuesFrom(p.getOWLObject(), d.getOWLObject());
            } else {
                owl = df.getOWLDataAllValuesFrom(p.getOWLObject(), d.getOWLObject());
            }
            return ONTObjectImpl.create(owl, _ce).append(p).append(d);
        }
        if (OntCE.ObjectHasValue.class.equals(type)) {
            OntCE.ObjectHasValue _ce = (OntCE.ObjectHasValue) ce;
            ONTObject<? extends OWLObjectPropertyExpression> p = of.get(_ce.getProperty());
            ONTObject<? extends OWLIndividual> i = of.get(_ce.getValue());
            return ONTObjectImpl.create(df.getOWLObjectHasValue(p.getOWLObject(), i.getOWLObject()), _ce).append(p).append(i);
        }
        if (OntCE.DataHasValue.class.equals(type)) {
            OntCE.DataHasValue _ce = (OntCE.DataHasValue) ce;
            ONTObject<OWLDataProperty> p = of.get(_ce.getProperty());
            ONTObject<OWLLiteral> l = of.get(_ce.getValue());
            return ONTObjectImpl.create(df.getOWLDataHasValue(p.getOWLObject(), l.getOWLObject()), _ce).append(p);
        }
        if (OntCE.ObjectMinCardinality.class.equals(type)
                || OntCE.ObjectMaxCardinality.class.equals(type)
                || OntCE.ObjectCardinality.class.equals(type)) {
            OntCE.CardinalityRestrictionCE<OntCE, OntOPE> _ce = (OntCE.CardinalityRestrictionCE<OntCE, OntOPE>) ce;
            ONTObject<? extends OWLObjectPropertyExpression> p = of.get(_ce.getProperty());
            ONTObject<? extends OWLClassExpression> c = calcClassExpression(_ce.getValue() == null ?
                    _ce.getModel().getOWLThing() : _ce.getValue(), of, seen);
            OWLObjectCardinalityRestriction owl;
            if (OntCE.ObjectMinCardinality.class.equals(type)) {
                owl = df.getOWLObjectMinCardinality(_ce.getCardinality(), p.getOWLObject(), c.getOWLObject());
            } else if (OntCE.ObjectMaxCardinality.class.equals(type)) {
                owl = df.getOWLObjectMaxCardinality(_ce.getCardinality(), p.getOWLObject(), c.getOWLObject());
            } else {
                owl = df.getOWLObjectExactCardinality(_ce.getCardinality(), p.getOWLObject(), c.getOWLObject());
            }
            return ONTObjectImpl.create(owl, _ce).append(p).append(c);
        }
        if (OntCE.DataMinCardinality.class.equals(type)
                || OntCE.DataMaxCardinality.class.equals(type)
                || OntCE.DataCardinality.class.equals(type)) {
            OntCE.CardinalityRestrictionCE<OntDR, OntNDP> _ce = (OntCE.CardinalityRestrictionCE<OntDR, OntNDP>) ce;
            ONTObject<OWLDataProperty> p = of.get(_ce.getProperty());
            ONTObject<? extends OWLDataRange> d = of.get(_ce.getValue() == null ?
                    _ce.getModel().getOntEntity(OntDT.class, RDFS.Literal) : _ce.getValue());
            OWLDataCardinalityRestriction owl;
            if (OntCE.DataMinCardinality.class.equals(type)) {
                owl = df.getOWLDataMinCardinality(_ce.getCardinality(), p.getOWLObject(), d.getOWLObject());
            } else if (OntCE.DataMaxCardinality.class.equals(type)) {
                owl = df.getOWLDataMaxCardinality(_ce.getCardinality(), p.getOWLObject(), d.getOWLObject());
            } else {
                owl = df.getOWLDataExactCardinality(_ce.getCardinality(), p.getOWLObject(), d.getOWLObject());
            }
            return ONTObjectImpl.create(owl, _ce).append(p).append(d);
        }
        if (OntCE.HasSelf.class.equals(type)) {
            OntCE.HasSelf _ce = (OntCE.HasSelf) ce;
            ONTObject<? extends OWLObjectPropertyExpression> p = of.get(_ce.getProperty());
            return ONTObjectImpl.create(df.getOWLObjectHasSelf(p.getOWLObject()), _ce).append(p);
        }
        if (OntCE.UnionOf.class.equals(type) || OntCE.IntersectionOf.class.equals(type)) {
            OntCE.ComponentsCE<OntCE> _ce = (OntCE.ComponentsCE<OntCE>) ce;
            Set<ONTObject<OWLClassExpression>> components = OntModels.listMembers(_ce.getList())
                    .mapWith(c -> (ONTObject<OWLClassExpression>) calcClassExpression(c, of, seen))
                    .toSet();
            OWLClassExpression owl;
            if (OntCE.UnionOf.class.equals(type)) {
                owl = df.getOWLObjectUnionOf(components.stream().map(ONTObject::getOWLObject));
            } else {
                owl = df.getOWLObjectIntersectionOf(components.stream().map(ONTObject::getOWLObject));
            }
            return ONTObjectImpl.create(owl, _ce).append(components);
        }
        if (OntCE.OneOf.class.equals(type)) {
            OntCE.OneOf _ce = (OntCE.OneOf) ce;
            Set<ONTObject<OWLIndividual>> components = OntModels.listMembers(_ce.getList())
                    .mapWith(i -> (ONTObject<OWLIndividual>) of.get(i)).toSet();
            OWLClassExpression owl = df.getOWLObjectOneOf(components.stream().map(ONTObject::getOWLObject));
            return ONTObjectImpl.create(owl, _ce).append(components);
        }
        if (ce instanceof OntCE.ComplementOf) {
            OntCE.ComplementOf _ce = (OntCE.ComplementOf) ce;
            ONTObject<? extends OWLClassExpression> c = calcClassExpression(_ce.getValue(), of, seen);
            return ONTObjectImpl.create(df.getOWLObjectComplementOf(c.getOWLObject()), _ce).append(c);
        }
        if (ce instanceof OntCE.NaryRestrictionCE) {
            OntCE.NaryRestrictionCE<OntDR, OntNDP> _ce = (OntCE.NaryRestrictionCE<OntDR, OntNDP>) ce;
            ONTObject<OWLDataProperty> p = of.get(_ce.getProperty());
            ONTObject<? extends OWLDataRange> d = of.get(_ce.getValue());
            OWLClassExpression owl;
            if (OntCE.NaryDataSomeValuesFrom.class.equals(type)) {
                owl = df.getOWLDataSomeValuesFrom(p.getOWLObject(), d.getOWLObject());
            } else {
                owl = df.getOWLDataAllValuesFrom(p.getOWLObject(), d.getOWLObject());
            }
            return ONTObjectImpl.create(owl, _ce).append(p).append(d);

        }
        throw new OntApiException("Unsupported class expression " + ce);
    }

    /**
     * @param var {@link OntSWRL.Variable}
     * @param of  {@link InternalObjectFactory}
     * @return {@link ONTObject} around {@link SWRLVariable}
     */
    public static ONTObject<SWRLVariable> getSWRLVariable(OntSWRL.Variable var, InternalObjectFactory of) {
        if (!OntApiException.notNull(var, "Null swrl var").isURIResource()) {
            throw new OntApiException("Anonymous swrl var " + var);
        }
        return ONTObjectImpl.create(of.getOWLDataFactory().getSWRLVariable(of.toIRI(var.getURI())), var);
    }

    /**
     * @param arg {@link OntSWRL.DArg}
     * @param of  {@link InternalObjectFactory}
     * @return {@link ONTObject} around {@link SWRLDArgument}
     */
    public static ONTObject<? extends SWRLDArgument> getSWRLLiteralArg(OntSWRL.DArg arg, InternalObjectFactory of) {
        if (OntApiException.notNull(arg, "Null SWRL-D arg").isLiteral()) {
            return ONTObjectImpl.create(of.getOWLDataFactory()
                    .getSWRLLiteralArgument(of.get(arg.asLiteral()).getOWLObject()), arg);
        }
        if (arg.canAs(OntSWRL.Variable.class)) {
            return of.get(arg.as(OntSWRL.Variable.class));
        }
        throw new OntApiException("Unsupported SWRL-D arg " + arg);
    }

    /**
     * @param arg {@link OntSWRL.IArg}
     * @param of  {@link InternalObjectFactory}
     * @return {@link ONTObject} around {@link SWRLIArgument}
     */
    public static ONTObject<? extends SWRLIArgument> getSWRLIndividualArg(OntSWRL.IArg arg, InternalObjectFactory of) {
        if (OntApiException.notNull(arg, "Null SWRL-I arg").canAs(OntIndividual.class)) {
            return ONTObjectImpl.create(of.getOWLDataFactory()
                    .getSWRLIndividualArgument(of.get(arg.as(OntIndividual.class)).getOWLObject()), arg);
        }
        if (arg.canAs(OntSWRL.Variable.class)) {
            return of.get(arg.as(OntSWRL.Variable.class));
        }
        throw new OntApiException("Unsupported SWRL-I arg " + arg);
    }

    /**
     * @param atom {@link OntSWRL.Atom}
     * @param of   {@link InternalObjectFactory}
     * @return {@link ONTObject} around {@link SWRLAtom}
     */
    public static ONTObject<? extends SWRLAtom> calcSWRLAtom(OntSWRL.Atom atom, InternalObjectFactory of) {
        if (atom instanceof OntSWRL.Atom.BuiltIn) {
            OntSWRL.Atom.BuiltIn _atom = (OntSWRL.Atom.BuiltIn) atom;
            IRI iri = of.toIRI(_atom.getPredicate().getURI());
            List<ONTObject<? extends SWRLDArgument>> arguments = _atom.arguments().map(of::get)
                    .collect(Collectors.toList());
            SWRLAtom res = of.getOWLDataFactory().getSWRLBuiltInAtom(iri, arguments.stream().map(ONTObject::getOWLObject)
                    .collect(Collectors.toList()));
            return ONTObjectImpl.create(res, _atom).appendWildcards(arguments);
        }
        if (atom instanceof OntSWRL.Atom.OntClass) {
            OntSWRL.Atom.OntClass _atom = (OntSWRL.Atom.OntClass) atom;
            ONTObject<? extends OWLClassExpression> c = of.get(_atom.getPredicate());
            ONTObject<? extends SWRLIArgument> a = of.get(_atom.getArg());
            return ONTObjectImpl.create(of.getOWLDataFactory().getSWRLClassAtom(c.getOWLObject(), a.getOWLObject()), _atom)
                    .append(c).append(a);
        }
        if (atom instanceof OntSWRL.Atom.DataProperty) {
            OntSWRL.Atom.DataProperty _atom = (OntSWRL.Atom.DataProperty) atom;
            ONTObject<OWLDataProperty> p = of.get(_atom.getPredicate());
            ONTObject<? extends SWRLIArgument> f = of.get(_atom.getFirstArg());
            ONTObject<? extends SWRLDArgument> s = of.get(_atom.getSecondArg());
            return ONTObjectImpl.create(of.getOWLDataFactory()
                    .getSWRLDataPropertyAtom(p.getOWLObject(), f.getOWLObject(), s.getOWLObject()), _atom)
                    .append(p).append(f).append(s);
        }
        if (atom instanceof OntSWRL.Atom.ObjectProperty) {
            OntSWRL.Atom.ObjectProperty _atom = (OntSWRL.Atom.ObjectProperty) atom;
            ONTObject<? extends OWLObjectPropertyExpression> p = of.get(_atom.getPredicate());
            ONTObject<? extends SWRLIArgument> f = of.get(_atom.getFirstArg());
            ONTObject<? extends SWRLIArgument> s = of.get(_atom.getSecondArg());
            return ONTObjectImpl.create(of.getOWLDataFactory()
                    .getSWRLObjectPropertyAtom(p.getOWLObject(), f.getOWLObject(), s.getOWLObject()), _atom)
                    .append(p).append(f).append(s);
        }
        if (atom instanceof OntSWRL.Atom.DataRange) {
            OntSWRL.Atom.DataRange _atom = (OntSWRL.Atom.DataRange) atom;
            ONTObject<? extends OWLDataRange> d = of.get(_atom.getPredicate());
            ONTObject<? extends SWRLDArgument> a = of.get(_atom.getArg());
            return ONTObjectImpl.create(of.getOWLDataFactory()
                    .getSWRLDataRangeAtom(d.getOWLObject(), a.getOWLObject()), _atom).append(d).append(a);
        }
        if (atom instanceof OntSWRL.Atom.DifferentIndividuals) {
            OntSWRL.Atom.DifferentIndividuals _atom = (OntSWRL.Atom.DifferentIndividuals) atom;
            ONTObject<? extends SWRLIArgument> f = of.get(_atom.getFirstArg());
            ONTObject<? extends SWRLIArgument> s = of.get(_atom.getSecondArg());
            return ONTObjectImpl.create(of.getOWLDataFactory()
                    .getSWRLDifferentIndividualsAtom(f.getOWLObject(), s.getOWLObject()), _atom).append(f).append(s);
        }
        if (atom instanceof OntSWRL.Atom.SameIndividuals) {
            OntSWRL.Atom.SameIndividuals _atom = (OntSWRL.Atom.SameIndividuals) atom;
            ONTObject<? extends SWRLIArgument> f = of.get(_atom.getFirstArg());
            ONTObject<? extends SWRLIArgument> s = of.get(_atom.getSecondArg());
            return ONTObjectImpl.create(of.getOWLDataFactory()
                    .getSWRLSameIndividualAtom(f.getOWLObject(), s.getOWLObject()), _atom).append(f).append(s);
        }
        throw new OntApiException("Unsupported SWRL atom " + atom);
    }

}
