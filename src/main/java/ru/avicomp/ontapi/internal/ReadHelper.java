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

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.util.iterator.WrappedIterator;
import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWLFacet;
import ru.avicomp.ontapi.DataFactory;
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.jena.impl.OntListImpl;
import ru.avicomp.ontapi.jena.impl.OntObjectImpl;
import ru.avicomp.ontapi.jena.impl.OntStatementImpl;
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
     * @param conf      {@link InternalConfig}
     * @param o         {@link AxiomType#SUB_OBJECT_PROPERTY} or {@link AxiomType#OBJECT_PROPERTY_DOMAIN} or {@link AxiomType#OBJECT_PROPERTY_RANGE}
     * @param d         {@link AxiomType#SUB_DATA_PROPERTY} or {@link AxiomType#DATA_PROPERTY_DOMAIN} or {@link AxiomType#DATA_PROPERTY_RANGE}
     * @return true if the statement is good to be represented in the form of annotation axiom.
     */
    protected static boolean testAnnotationAxiomOverlaps(OntStatement statement,
                                                         InternalConfig conf,
                                                         AxiomType<? extends OWLObjectPropertyAxiom> o,
                                                         AxiomType<? extends OWLDataPropertyAxiom> d) {
        return !conf.isIgnoreAnnotationAxiomOverlaps() ||
                Stream.of(d, o).map(AxiomParserProvider::get).noneMatch(a -> a.testStatement(statement, conf));
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
     * @param statement {@link OntStatement}
     * @param conf      {@link InternalConfig}
     * @param df        {@link InternalDataFactory}
     * @return a set of wraps {@link ONTObject} around {@link OWLAnnotation}
     */
    public static Set<ONTObject<OWLAnnotation>> getAnnotations(OntStatement statement,
                                                               InternalConfig conf,
                                                               InternalDataFactory df) {
        ExtendedIterator<OntStatement> res = listAnnotations(statement);
        if (conf.isLoadAnnotationAxioms() && isDeclarationStatement(statement)) {
            // for compatibility with OWL-API skip all plain annotations attached to an entity (or anonymous individual)
            // they would go separately as annotation-assertions.
            res = res.filterDrop(s -> isAnnotationAssertionStatement(s, conf));
        }
        return res.mapWith(a -> getAnnotation(a, df)).toSet();
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
        Set<ONTObject<OWLAnnotation>> children = listAnnotations(root)
                .mapWith(a -> getHierarchicalAnnotations(a, df)).toSet();
        OWLAnnotation object = df.getOWLDataFactory()
                .getOWLAnnotation(p.getObject(), v.getObject(), children.stream().map(ONTObject::getObject));
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
     * @param dr                  {@link OntDR Ontology Data Range} to map
     * @param internalDataFactory {@link InternalDataFactory}
     * @param seen                Set of {@link Resource}
     * @return {@link ONTObject} around {@link OWLDataRange}
     * @throws OntApiException if something is wrong.
     */
    @SuppressWarnings("unchecked")
    public static ONTObject<? extends OWLDataRange> calcDataRange(OntDR dr,
                                                                  InternalDataFactory internalDataFactory,
                                                                  Set<Resource> seen) {
        if (OntApiException.notNull(dr, "Null data range").isURIResource()) {
            return internalDataFactory.get(dr.as(OntDT.class));
        }
        if (seen.contains(dr)) {
            throw new OntApiException("Recursive loop on data range " + dr);
        }
        seen.add(dr);
        DataFactory df = internalDataFactory.getOWLDataFactory();
        if (dr instanceof OntDR.Restriction) {
            OntDR.Restriction _dr = (OntDR.Restriction) dr;
            ONTObject<OWLDatatype> d = internalDataFactory.get(_dr.getDatatype());
            Set<ONTObject<OWLFacetRestriction>> restrictions = listMembers(_dr.getList())
                    .mapWith(f -> getFacetRestriction(f, internalDataFactory)).toSet();
            OWLDataRange res = df.getOWLDatatypeRestriction(d.getObject(),
                    restrictions.stream().map(ONTObject::getObject).collect(Collectors.toList()));
            return ONTObject.create(res, dr).append(restrictions);
        }
        if (dr instanceof OntDR.ComplementOf) {
            OntDR.ComplementOf _dr = (OntDR.ComplementOf) dr;
            ONTObject<? extends OWLDataRange> d = calcDataRange(_dr.getDataRange(), internalDataFactory, seen);
            return ONTObject.create(df.getOWLDataComplementOf(d.getObject()), _dr).append(d);
        }
        if (dr instanceof OntDR.UnionOf || dr instanceof OntDR.IntersectionOf) {
            OntDR.ComponentsDR<OntDR> _dr = (OntDR.ComponentsDR<OntDR>) dr;
            Set<ONTObject<OWLDataRange>> dataRanges = listMembers(_dr.getList())
                    .mapWith(d -> (ONTObject<OWLDataRange>) calcDataRange(d, internalDataFactory, seen)).toSet();
            OWLDataRange res = dr instanceof OntDR.UnionOf ?
                    df.getOWLDataUnionOf(dataRanges.stream().map(ONTObject::getObject)) :
                    df.getOWLDataIntersectionOf(dataRanges.stream().map(ONTObject::getObject));
            return ONTObject.create(res, dr).append(dataRanges);
        }
        if (dr instanceof OntDR.OneOf) {
            OntDR.OneOf _dr = (OntDR.OneOf) dr;
            Set<ONTObject<OWLLiteral>> literals = _dr.values().map(internalDataFactory::get).collect(Collectors.toSet());
            OWLDataRange res = df.getOWLDataOneOf(literals.stream().map(ONTObject::getObject));
            return ONTObject.create(res, _dr);
        }
        throw new OntApiException("Unsupported data range expression " + dr);
    }

    /**
     * Calculates an {@link OWLClassExpression} wrapped by {@link ONTObject}.
     * Note: this method is recursive.
     *
     * @param ce                  {@link OntCE Ontology Class Expression} to map
     * @param internalDataFactory {@link InternalDataFactory}
     * @param seen                Set of {@link Resource},
     *                            a subsidiary collection to prevent possible graph recursions (e.g. {@code _:b0 owl:complementOf _:b0})
     * @return {@link ONTObject} around {@link OWLClassExpression}
     * @throws OntApiException if something is wrong.
     */
    @SuppressWarnings("unchecked")
    public static ONTObject<? extends OWLClassExpression> calcClassExpression(OntCE ce,
                                                                              InternalDataFactory internalDataFactory,
                                                                              Set<Resource> seen) {
        if (OntApiException.notNull(ce, "Null class expression").isURIResource()) {
            return internalDataFactory.get(ce.as(OntClass.class));
        }
        if (seen.contains(ce)) {
            throw new OntApiException("Recursive loop on class expression " + ce);
        }
        seen.add(ce);
        DataFactory df = internalDataFactory.getOWLDataFactory();
        Class<? extends OntObject> type = getType(ce);
        if (OntCE.ObjectSomeValuesFrom.class.equals(type) || OntCE.ObjectAllValuesFrom.class.equals(type)) {
            OntCE.ComponentRestrictionCE<OntCE, OntOPE> _ce = (OntCE.ComponentRestrictionCE<OntCE, OntOPE>) ce;
            ONTObject<? extends OWLObjectPropertyExpression> p = internalDataFactory.get(_ce.getOnProperty());
            ONTObject<? extends OWLClassExpression> c = calcClassExpression(_ce.getValue(), internalDataFactory, seen);
            OWLClassExpression owl;
            if (OntCE.ObjectSomeValuesFrom.class.equals(type)) {
                owl = df.getOWLObjectSomeValuesFrom(p.getObject(), c.getObject());
            } else {
                owl = df.getOWLObjectAllValuesFrom(p.getObject(), c.getObject());
            }
            return ONTObject.create(owl, _ce).append(p).append(c);
        }
        if (OntCE.DataSomeValuesFrom.class.equals(type) || OntCE.DataAllValuesFrom.class.equals(type)) {
            OntCE.ComponentRestrictionCE<OntDR, OntNDP> _ce = (OntCE.ComponentRestrictionCE<OntDR, OntNDP>) ce;
            ONTObject<OWLDataProperty> p = internalDataFactory.get(_ce.getOnProperty());
            ONTObject<? extends OWLDataRange> d = internalDataFactory.get(_ce.getValue());
            OWLClassExpression owl;
            if (OntCE.DataSomeValuesFrom.class.equals(type)) {
                owl = df.getOWLDataSomeValuesFrom(p.getObject(), d.getObject());
            } else {
                owl = df.getOWLDataAllValuesFrom(p.getObject(), d.getObject());
            }
            return ONTObject.create(owl, _ce).append(p).append(d);
        }
        if (OntCE.ObjectHasValue.class.equals(type)) {
            OntCE.ObjectHasValue _ce = (OntCE.ObjectHasValue) ce;
            ONTObject<? extends OWLObjectPropertyExpression> p = internalDataFactory.get(_ce.getOnProperty());
            ONTObject<? extends OWLIndividual> i = internalDataFactory.get(_ce.getValue());
            return ONTObject.create(df.getOWLObjectHasValue(p.getObject(), i.getObject()), _ce).append(p).append(i);
        }
        if (OntCE.DataHasValue.class.equals(type)) {
            OntCE.DataHasValue _ce = (OntCE.DataHasValue) ce;
            ONTObject<OWLDataProperty> p = internalDataFactory.get(_ce.getOnProperty());
            ONTObject<OWLLiteral> l = internalDataFactory.get(_ce.getValue());
            return ONTObject.create(df.getOWLDataHasValue(p.getObject(), l.getObject()), _ce).append(p);
        }
        if (OntCE.ObjectMinCardinality.class.equals(type)
                || OntCE.ObjectMaxCardinality.class.equals(type)
                || OntCE.ObjectCardinality.class.equals(type)) {
            OntCE.CardinalityRestrictionCE<OntCE, OntOPE> _ce = (OntCE.CardinalityRestrictionCE<OntCE, OntOPE>) ce;
            ONTObject<? extends OWLObjectPropertyExpression> p = internalDataFactory.get(_ce.getOnProperty());
            ONTObject<? extends OWLClassExpression> c = calcClassExpression(_ce.getValue() == null ?
                    _ce.getModel().getOWLThing() : _ce.getValue(), internalDataFactory, seen);
            OWLObjectCardinalityRestriction owl;
            if (OntCE.ObjectMinCardinality.class.equals(type)) {
                owl = df.getOWLObjectMinCardinality(_ce.getCardinality(), p.getObject(), c.getObject());
            } else if (OntCE.ObjectMaxCardinality.class.equals(type)) {
                owl = df.getOWLObjectMaxCardinality(_ce.getCardinality(), p.getObject(), c.getObject());
            } else {
                owl = df.getOWLObjectExactCardinality(_ce.getCardinality(), p.getObject(), c.getObject());
            }
            return ONTObject.create(owl, _ce).append(p).append(c);
        }
        if (OntCE.DataMinCardinality.class.equals(type)
                || OntCE.DataMaxCardinality.class.equals(type)
                || OntCE.DataCardinality.class.equals(type)) {
            OntCE.CardinalityRestrictionCE<OntDR, OntNDP> _ce = (OntCE.CardinalityRestrictionCE<OntDR, OntNDP>) ce;
            ONTObject<OWLDataProperty> p = internalDataFactory.get(_ce.getOnProperty());
            ONTObject<? extends OWLDataRange> d = internalDataFactory.get(_ce.getValue() == null ?
                    _ce.getModel().getOntEntity(OntDT.class, RDFS.Literal) : _ce.getValue());
            OWLDataCardinalityRestriction owl;
            if (OntCE.DataMinCardinality.class.equals(type)) {
                owl = df.getOWLDataMinCardinality(_ce.getCardinality(), p.getObject(), d.getObject());
            } else if (OntCE.DataMaxCardinality.class.equals(type)) {
                owl = df.getOWLDataMaxCardinality(_ce.getCardinality(), p.getObject(), d.getObject());
            } else {
                owl = df.getOWLDataExactCardinality(_ce.getCardinality(), p.getObject(), d.getObject());
            }
            return ONTObject.create(owl, _ce).append(p).append(d);
        }
        if (OntCE.HasSelf.class.equals(type)) {
            OntCE.HasSelf _ce = (OntCE.HasSelf) ce;
            ONTObject<? extends OWLObjectPropertyExpression> p = internalDataFactory.get(_ce.getOnProperty());
            return ONTObject.create(df.getOWLObjectHasSelf(p.getObject()), _ce).append(p);
        }
        if (OntCE.UnionOf.class.equals(type) || OntCE.IntersectionOf.class.equals(type)) {
            OntCE.ComponentsCE<OntCE> _ce = (OntCE.ComponentsCE<OntCE>) ce;
            Set<ONTObject<OWLClassExpression>> components = listMembers(_ce.getList())
                    .mapWith(c -> (ONTObject<OWLClassExpression>) calcClassExpression(c, internalDataFactory, seen))
                    .toSet();
            OWLClassExpression owl;
            if (OntCE.UnionOf.class.equals(type)) {
                owl = df.getOWLObjectUnionOf(components.stream().map(ONTObject::getObject));
            } else {
                owl = df.getOWLObjectIntersectionOf(components.stream().map(ONTObject::getObject));
            }
            return ONTObject.create(owl, _ce).append(components);
        }
        if (OntCE.OneOf.class.equals(type)) {
            OntCE.OneOf _ce = (OntCE.OneOf) ce;
            Set<ONTObject<OWLIndividual>> components = listMembers(_ce.getList())
                    .mapWith(i -> (ONTObject<OWLIndividual>) internalDataFactory.get(i)).toSet();
            OWLClassExpression owl = df.getOWLObjectOneOf(components.stream().map(ONTObject::getObject));
            return ONTObject.create(owl, _ce).append(components);
        }
        if (ce instanceof OntCE.ComplementOf) {
            OntCE.ComplementOf _ce = (OntCE.ComplementOf) ce;
            ONTObject<? extends OWLClassExpression> c = calcClassExpression(_ce.getValue(), internalDataFactory, seen);
            return ONTObject.create(df.getOWLObjectComplementOf(c.getObject()), _ce).append(c);
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
            return ONTObject.create(df.getOWLDataFactory()
                    .getSWRLLiteralArgument(df.get(arg.asLiteral()).getObject()), arg);
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
            return ONTObject.create(df.getOWLDataFactory()
                    .getSWRLIndividualArgument(df.get(arg.as(OntIndividual.class)).getObject()), arg);
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
    public static ONTObject<? extends SWRLAtom> calcSWRLAtom(OntSWRL.Atom atom, InternalDataFactory df) {
        if (atom instanceof OntSWRL.Atom.BuiltIn) {
            OntSWRL.Atom.BuiltIn _atom = (OntSWRL.Atom.BuiltIn) atom;
            IRI iri = df.toIRI(_atom.getPredicate().getURI());
            List<ONTObject<? extends SWRLDArgument>> arguments = _atom.arguments().map(a -> getSWRLLiteralArg(a, df))
                    .collect(Collectors.toList());
            SWRLAtom res = df.getOWLDataFactory().getSWRLBuiltInAtom(iri, arguments.stream().map(ONTObject::getObject)
                    .collect(Collectors.toList()));
            return ONTObject.create(res, _atom).appendWildcards(arguments);
        }
        if (atom instanceof OntSWRL.Atom.OntClass) {
            OntSWRL.Atom.OntClass _atom = (OntSWRL.Atom.OntClass) atom;
            ONTObject<? extends OWLClassExpression> c = df.get(_atom.getPredicate());
            ONTObject<? extends SWRLIArgument> a = getSWRLIndividualArg(_atom.getArg(), df);
            return ONTObject.create(df.getOWLDataFactory().getSWRLClassAtom(c.getObject(), a.getObject()), _atom)
                    .append(c).append(a);
        }
        if (atom instanceof OntSWRL.Atom.DataProperty) {
            OntSWRL.Atom.DataProperty _atom = (OntSWRL.Atom.DataProperty) atom;
            ONTObject<OWLDataProperty> p = df.get(_atom.getPredicate());
            ONTObject<? extends SWRLIArgument> f = getSWRLIndividualArg(_atom.getFirstArg(), df);
            ONTObject<? extends SWRLDArgument> s = getSWRLLiteralArg(_atom.getSecondArg(), df);
            return ONTObject.create(df.getOWLDataFactory()
                    .getSWRLDataPropertyAtom(p.getObject(), f.getObject(), s.getObject()), _atom)
                    .append(p).append(f).append(s);
        }
        if (atom instanceof OntSWRL.Atom.ObjectProperty) {
            OntSWRL.Atom.ObjectProperty _atom = (OntSWRL.Atom.ObjectProperty) atom;
            ONTObject<? extends OWLObjectPropertyExpression> p = df.get(_atom.getPredicate());
            ONTObject<? extends SWRLIArgument> f = getSWRLIndividualArg(_atom.getFirstArg(), df);
            ONTObject<? extends SWRLIArgument> s = getSWRLIndividualArg(_atom.getSecondArg(), df);
            return ONTObject.create(df.getOWLDataFactory()
                    .getSWRLObjectPropertyAtom(p.getObject(), f.getObject(), s.getObject()), _atom)
                    .append(p).append(f).append(s);
        }
        if (atom instanceof OntSWRL.Atom.DataRange) {
            OntSWRL.Atom.DataRange _atom = (OntSWRL.Atom.DataRange) atom;
            ONTObject<? extends OWLDataRange> d = df.get(_atom.getPredicate());
            ONTObject<? extends SWRLDArgument> a = getSWRLLiteralArg(_atom.getArg(), df);
            return ONTObject.create(df.getOWLDataFactory()
                    .getSWRLDataRangeAtom(d.getObject(), a.getObject()), _atom).append(d).append(a);
        }
        if (atom instanceof OntSWRL.Atom.DifferentIndividuals) {
            OntSWRL.Atom.DifferentIndividuals _atom = (OntSWRL.Atom.DifferentIndividuals) atom;
            ONTObject<? extends SWRLIArgument> f = getSWRLIndividualArg(_atom.getFirstArg(), df);
            ONTObject<? extends SWRLIArgument> s = getSWRLIndividualArg(_atom.getSecondArg(), df);
            return ONTObject.create(df.getOWLDataFactory()
                    .getSWRLDifferentIndividualsAtom(f.getObject(), s.getObject()), _atom).append(f).append(s);
        }
        if (atom instanceof OntSWRL.Atom.SameIndividuals) {
            OntSWRL.Atom.SameIndividuals _atom = (OntSWRL.Atom.SameIndividuals) atom;
            ONTObject<? extends SWRLIArgument> f = getSWRLIndividualArg(_atom.getFirstArg(), df);
            ONTObject<? extends SWRLIArgument> s = getSWRLIndividualArg(_atom.getSecondArg(), df);
            return ONTObject.create(df.getOWLDataFactory()
                    .getSWRLSameIndividualAtom(f.getObject(), s.getObject()), _atom).append(f).append(s);
        }
        throw new OntApiException("Unsupported SWRL atom " + atom);
    }

    public static Class<? extends OntObject> getType(OntCE ce) {
        if (ce instanceof OntObjectImpl) {
            return OntApiException.notNull(((OntObjectImpl) ce).getActualClass(),
                    "Can't determine type of class expression " + ce);
        }
        return OntObjectImpl.findActualClass(ce);
    }

    /**
     * Returns an iterator over all annotations of the given statement.
     *
     * @param s {@link OntStatement}
     * @return {@link ExtendedIterator}
     */
    public static ExtendedIterator<OntStatement> listAnnotations(OntStatement s) {
        if (s instanceof OntStatementImpl) {
            return ((OntStatementImpl) s).listAnnotations();
        }
        return WrappedIterator.create(s.annotations().iterator());
    }

    /**
     * Lists all members from {@link OntList Ontology List}.
     *
     * @param list {@link OntList}
     * @param <R>  {@link RDFNode}, a type of list members
     * @return {@link ExtendedIterator} of {@link R}
     */
    public static <R extends RDFNode> ExtendedIterator<R> listMembers(OntList<R> list) {
        if (list instanceof OntListImpl) {
            return ((OntListImpl<R>) list).listMembers();
        }
        return WrappedIterator.create(list.members().iterator());
    }

}
