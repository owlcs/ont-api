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

package ru.avicomp.owlapi;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.CollectionFactory;
import org.semanticweb.owlapi.util.VersionInfo;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.semanticweb.owlapi.vocab.OWLFacet;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import ru.avicomp.owlapi.axioms.*;
import ru.avicomp.owlapi.objects.OWLObjectInverseOfImpl;
import ru.avicomp.owlapi.objects.ce.*;
import ru.avicomp.owlapi.objects.dr.OWLDataIntersectionOfImpl;
import ru.avicomp.owlapi.objects.dr.OWLDataOneOfImpl;
import ru.avicomp.owlapi.objects.dr.OWLDataUnionOfImpl;
import ru.avicomp.owlapi.objects.dr.OWLDatatypeRestrictionImpl;
import ru.avicomp.owlapi.objects.entity.*;
import ru.avicomp.owlapi.objects.literal.OWLLiteralImplString;
import ru.avicomp.owlapi.objects.swrl.SWRLVariableImpl;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ru.avicomp.owlapi.InternalizedEntities.*;

/**
 * It is a modified copy-paste from uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl.
 * There are two main differences with the original (OWL-API) implementation: no compression and no cache.
 * Jena RDF-Graph is a primary essence in ONT-API, all information should be kept and remain in that graph,
 * and, it seems any literal string compression should be implemented on the graph-level, if needed, not in this DataFactory.
 * The cache is present in ONT-API model implementation (see {@link ru.avicomp.ontapi.internal.InternalModel}),
 * the original global caches from OWL-API-impl seems to be superfluous here.
 * <p>
 *
 * @author Matthew Horridge, The University Of Manchester, Bio-Health Informatics Group
 * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLDataFactoryImpl.java'>uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl</a>
 * @see ru.avicomp.ontapi.internal.InternalDataFactory
 */
@SuppressWarnings("NullableProblems")
public class OWLDataFactoryImpl implements OWLDataFactory {

    private static final String LEXICAL_VALUE_CANNOT_BE_NULL = "lexicalValue cannot be null";
    private static final String LITERAL_CANNOT_BE_NULL = "literal cannot be null";
    private static final String VAR_CANNOT_BE_NULL = "var cannot be null";
    private static final String BUILT_IN_IRI_CANNOT_BE_NULL = "builtInIRI cannot be null";
    private static final String ARGS_CANNOT_BE_NULL = "args cannot be null";
    private static final String ARG1_CANNOT_BE_NULL = "arg1 cannot be null";
    private static final String ARG0_CANNOT_BE_NULL = "arg0 cannot be null";
    private static final String ARG_CANNOT_BE_NULL = "arg cannot be null";
    private static final String PREDICATE_CANNOT_BE_NULL = "predicate cannot be null";
    private static final String HEAD_CANNOT_BE_NULL = "head cannot be null";
    private static final String BODY_CANNOT_BE_NULL = "body cannot be null";
    private static final String ANNOTATION_CANNOT_BE_NULL = "annotation cannot be null";
    private static final String OWL_CLASS_CANNOT_BE_NULL = "owlClass cannot be null";
    private static final String CHAIN_CANNOT_BE_NULL = "chain cannot be null";
    private static final String INVERSE_PROPERTY_CANNOT_BE_NULL = "inverseProperty cannot be null";
    private static final String FORWARD_PROPERTY_CANNOT_BE_NULL = "forwardProperty cannot be null";
    private static final String SUPERCLASS_CANNOT_BE_NULL = "superclass cannot be null";
    private static final String SUBCLASS_CANNOT_BE_NULL = "subclass cannot be null";
    private static final String RANGE_CANNOT_BE_NULL = "range cannot be null";
    private static final String SUBJECT_CANNOT_BE_NULL = "subject cannot be null";
    private static final String OBJECT_CANNOT_BE_NULL = "object cannot be null";
    private static final String IMPORTED_ONTOLOGY_IRI_CANNOT_BE_NULL = "importedOntologyIRI cannot be null";
    private static final String CLASS_EXPRESSIONS_CANNOT_BE_NULL = "classExpressions cannot be null";
    private static final String PROPERTIES_CANNOT_BE_NULL = "properties cannot be null";
    private static final String CLASS_EXPRESSIONS_CANNOT_BE_NULL_OR_CONTAIN_NULL = "classExpressions cannot be null or contain null";
    private static final String INDIVIDUALS_CANNOT_BE_NULL = "individuals cannot be null";
    private static final String OWL_ENTITY_CANNOT_BE_NULL = "owlEntity cannot be null";
    private static final String SUPER_PROPERTY_CANNOT_BE_NULL = "superProperty cannot be null";
    private static final String SUB_PROPERTY_CANNOT_BE_NULL = "subProperty cannot be null";
    private static final String OWL_DATA_RANGE_CANNOT_BE_NULL = "owlDataRange cannot be null";
    private static final String DOMAIN_CANNOT_BE_NULL = "domain cannot be null";
    private static final String INDIVIDUAL_CANNOT_BE_NULL = "individual cannot be null";
    private static final String CLASS_EXPRESSION_CANNOT_BE_NULL = "classExpression cannot be null";
    private static final String VALUE_CANNOT_BE_NULL = "value cannot be null";
    private static final String CARDINALITY_CANNOT_BE_NEGATIVE = "cardinality cannot be negative";
    private static final String FACET_VALUE_CANNOT_BE_NULL = "facetValue cannot be null";
    private static final String PROPERTY_CANNOT_BE_NULL = "property cannot be null";
    private static final String TYPED_CONSTANT_CANNOT_BE_NULL = "typedConstant cannot be null";
    private static final String FACET_CANNOT_BE_NULL = "facet cannot be null";
    private static final String DATATYPE_CANNOT_BE_NULL = "datatype cannot be null";
    private static final String DATA_RANGE_CANNOT_BE_NULL = "dataRange cannot be null";
    private static final String ID_CANNOT_BE_NULL = "id cannot be null";
    private static final String IRI_CANNOT_BE_NULL = "iri cannot be null";
    private static final String ENTITY_TYPE_CANNOT_BE_NULL = "entityType cannot be null";
    private static final String ANNOTATIONS_CANNOT_BE_NULL = "annotations cannot be null";

    private static final OWLLiteral NEGATIVE_FLOAT_ZERO = getBasicLiteral("-0.0", XSDFLOAT);

    private static void checkNotNegative(long value, String message) {
        if (value < 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void checkAnnotations(Collection<OWLAnnotation> o) {
        Objects.requireNonNull(o, ANNOTATIONS_CANNOT_BE_NULL);
    }

    public static boolean asBoolean(String str) {
        return Boolean.parseBoolean(str) || "1".equals(str.trim());
    }

    @Override
    public void purge() {
        // nothing
    }

    @Override
    public <E extends OWLEntity> E getOWLEntity(EntityType<E> entityType, IRI iri) {
        Objects.requireNonNull(entityType, ENTITY_TYPE_CANNOT_BE_NULL);
        Objects.requireNonNull(iri, IRI_CANNOT_BE_NULL);
        return entityType.buildEntity(iri, this);
    }

    @Override
    public OWLClass getOWLClass(IRI iri) {
        Objects.requireNonNull(iri, IRI_CANNOT_BE_NULL);
        return new OWLClassImpl(iri);
    }

    @Override
    public OWLAnnotationProperty getRDFSLabel() {
        return RDFS_LABEL;
    }

    @Override
    public OWLAnnotationProperty getRDFSComment() {
        return RDFS_COMMENT;
    }

    @Override
    public OWLAnnotationProperty getRDFSSeeAlso() {
        return RDFS_SEE_ALSO;
    }

    @Override
    public OWLAnnotationProperty getRDFSIsDefinedBy() {
        return RDFS_IS_DEFINED_BY;
    }

    @Override
    public OWLAnnotationProperty getOWLVersionInfo() {
        return OWL_VERSION_INFO;
    }

    @Override
    public OWLAnnotationProperty getOWLBackwardCompatibleWith() {
        return OWL_BACKWARD_COMPATIBLE_WITH;
    }

    @Override
    public OWLAnnotationProperty getOWLIncompatibleWith() {
        return OWL_INCOMPATIBLE_WITH;
    }

    @Override
    public OWLAnnotationProperty getOWLDeprecated() {
        return OWL_DEPRECATED;
    }

    @Override
    public OWLClass getOWLThing() {
        return OWL_THING;
    }

    @Override
    public OWLClass getOWLNothing() {
        return OWL_NOTHING;
    }

    @Override
    public OWLDataProperty getOWLBottomDataProperty() {
        return OWL_BOTTOM_DATA_PROPERTY;
    }

    @Override
    public OWLObjectProperty getOWLBottomObjectProperty() {
        return OWL_BOTTOM_OBJECT_PROPERTY;
    }

    @Override
    public OWLDataProperty getOWLTopDataProperty() {
        return OWL_TOP_DATA_PROPERTY;
    }

    @Override
    public OWLObjectProperty getOWLTopObjectProperty() {
        return OWL_TOP_OBJECT_PROPERTY;
    }

    @Override
    public OWLObjectProperty getOWLObjectProperty(IRI iri) {
        Objects.requireNonNull(iri, IRI_CANNOT_BE_NULL);
        return new OWLObjectPropertyImpl(iri);
    }

    @Override
    public OWLDataProperty getOWLDataProperty(IRI iri) {
        Objects.requireNonNull(iri, IRI_CANNOT_BE_NULL);
        return new OWLDataPropertyImpl(iri);
    }

    @Override
    public OWLNamedIndividual getOWLNamedIndividual(IRI iri) {
        Objects.requireNonNull(iri, IRI_CANNOT_BE_NULL);
        return new OWLNamedIndividualImpl(iri);
    }

    @Override
    public OWLAnonymousIndividual getOWLAnonymousIndividual(String nodeId) {
        Objects.requireNonNull(nodeId, ID_CANNOT_BE_NULL);
        return new ru.avicomp.owlapi.objects.OWLAnonymousIndividualImpl(NodeID.getNodeID(nodeId));
    }

    @Override
    public OWLAnonymousIndividual getOWLAnonymousIndividual() {
        return new ru.avicomp.owlapi.objects.OWLAnonymousIndividualImpl(NodeID.getNodeID(null));
    }

    @Override
    public OWLDatatype getOWLDatatype(IRI iri) {
        Objects.requireNonNull(iri, IRI_CANNOT_BE_NULL);
        return new OWLDatatypeImpl(iri);
    }

    @Override
    public OWLLiteral getOWLLiteral(boolean value) {
        return value ? TRUELITERAL : FALSELITERAL;
    }

    @Override
    public OWLDataOneOf getOWLDataOneOf(Stream<? extends OWLLiteral> values) {
        return new OWLDataOneOfImpl(values);
    }

    @Override
    public OWLDataComplementOf getOWLDataComplementOf(OWLDataRange dataRange) {
        Objects.requireNonNull(dataRange, DATA_RANGE_CANNOT_BE_NULL);
        return new ru.avicomp.owlapi.objects.dr.OWLDataComplementOfImpl(dataRange);
    }

    @Override
    public OWLDataComplementOf getOWLDataComplementOf(OWL2Datatype dataRange) {
        return getOWLDataComplementOf(dataRange.getDatatype(this));
    }

    @Override
    public OWLDataIntersectionOf getOWLDataIntersectionOf(Stream<? extends OWLDataRange> dataRanges) {
        return new OWLDataIntersectionOfImpl(dataRanges.map(x -> x));
    }

    @Override
    public OWLDataUnionOf getOWLDataUnionOf(Stream<? extends OWLDataRange> dataRanges) {
        return new OWLDataUnionOfImpl(dataRanges.map(x -> x));
    }

    @Override
    public OWLDatatypeRestriction getOWLDatatypeRestriction(OWLDatatype dataType, Collection<OWLFacetRestriction> facetRestrictions) {
        Objects.requireNonNull(dataType, DATATYPE_CANNOT_BE_NULL);
        Objects.requireNonNull(facetRestrictions, "facets");
        return new ru.avicomp.owlapi.objects.dr.OWLDatatypeRestrictionImpl(dataType, facetRestrictions);
    }

    @Override
    public OWLDatatypeRestriction getOWLDatatypeRestriction(OWLDatatype dataType, OWLFacet facet, OWLLiteral typedLiteral) {
        Objects.requireNonNull(dataType, DATATYPE_CANNOT_BE_NULL);
        Objects.requireNonNull(facet, FACET_CANNOT_BE_NULL);
        Objects.requireNonNull(typedLiteral, TYPED_CONSTANT_CANNOT_BE_NULL);
        return new OWLDatatypeRestrictionImpl(dataType, CollectionFactory.createSet(getOWLFacetRestriction(facet, typedLiteral)));
    }

    @Override
    public OWLFacetRestriction getOWLFacetRestriction(OWLFacet facet, OWLLiteral facetValue) {
        Objects.requireNonNull(facet, FACET_CANNOT_BE_NULL);
        Objects.requireNonNull(facetValue, FACET_VALUE_CANNOT_BE_NULL);
        return new ru.avicomp.owlapi.objects.OWLFacetRestrictionImpl(facet, facetValue);
    }

    @Override
    public OWLObjectIntersectionOf getOWLObjectIntersectionOf(Stream<? extends OWLClassExpression> operands) {
        return new ru.avicomp.owlapi.objects.ce.OWLObjectIntersectionOfImpl(operands.map(x -> x));
    }

    @Override
    public OWLObjectIntersectionOf getOWLObjectIntersectionOf(
            Collection<? extends OWLClassExpression> operands) {
        return new OWLObjectIntersectionOfImpl(operands);
    }

    @Override
    public OWLDataAllValuesFrom getOWLDataAllValuesFrom(OWLDataPropertyExpression property, OWLDataRange dataRange) {
        Objects.requireNonNull(dataRange, DATA_RANGE_CANNOT_BE_NULL);
        Objects.requireNonNull(property, PROPERTY_CANNOT_BE_NULL);
        return new OWLDataAllValuesFromImpl(property, dataRange);
    }

    @Override
    public OWLDataAllValuesFrom getOWLDataAllValuesFrom(OWLDataPropertyExpression property, OWL2Datatype dataRange) {
        return getOWLDataAllValuesFrom(property, dataRange.getDatatype(this));
    }

    @Override
    public OWLDataExactCardinality getOWLDataExactCardinality(int cardinality, OWLDataPropertyExpression property) {
        checkNotNegative(cardinality, CARDINALITY_CANNOT_BE_NEGATIVE);
        Objects.requireNonNull(property, PROPERTY_CANNOT_BE_NULL);
        return new ru.avicomp.owlapi.objects.ce.OWLDataExactCardinalityImpl(property, cardinality, getTopDatatype());
    }

    @Override
    public OWLDataExactCardinality getOWLDataExactCardinality(int cardinality, OWLDataPropertyExpression property, OWLDataRange dataRange) {
        Objects.requireNonNull(dataRange, DATA_RANGE_CANNOT_BE_NULL);
        Objects.requireNonNull(property, PROPERTY_CANNOT_BE_NULL);
        checkNotNegative(cardinality, CARDINALITY_CANNOT_BE_NEGATIVE);
        return new OWLDataExactCardinalityImpl(property, cardinality, dataRange);
    }

    @Override
    public OWLDataExactCardinality getOWLDataExactCardinality(int cardinality, OWLDataPropertyExpression property, OWL2Datatype dataRange) {
        return getOWLDataExactCardinality(cardinality, property, dataRange.getDatatype(this));
    }

    @Override
    public OWLDataMaxCardinality getOWLDataMaxCardinality(int cardinality, OWLDataPropertyExpression property) {
        checkNotNegative(cardinality, CARDINALITY_CANNOT_BE_NEGATIVE);
        Objects.requireNonNull(property, PROPERTY_CANNOT_BE_NULL);
        return new ru.avicomp.owlapi.objects.ce.OWLDataMaxCardinalityImpl(property, cardinality, getTopDatatype());
    }

    @Override
    public OWLDataMaxCardinality getOWLDataMaxCardinality(int cardinality, OWLDataPropertyExpression property, OWLDataRange dataRange) {
        checkNotNegative(cardinality, CARDINALITY_CANNOT_BE_NEGATIVE);
        Objects.requireNonNull(property, PROPERTY_CANNOT_BE_NULL);
        Objects.requireNonNull(dataRange, DATA_RANGE_CANNOT_BE_NULL);
        return new ru.avicomp.owlapi.objects.ce.OWLDataMaxCardinalityImpl(property, cardinality, dataRange);
    }

    @Override
    public OWLDataMaxCardinality getOWLDataMaxCardinality(int cardinality, OWLDataPropertyExpression property, OWL2Datatype dataRange) {
        return getOWLDataMaxCardinality(cardinality, property, dataRange.getDatatype(this));
    }

    @Override
    public OWLDataMinCardinality getOWLDataMinCardinality(int cardinality, OWLDataPropertyExpression property) {
        checkNotNegative(cardinality, CARDINALITY_CANNOT_BE_NEGATIVE);
        Objects.requireNonNull(property, PROPERTY_CANNOT_BE_NULL);
        return new ru.avicomp.owlapi.objects.ce.OWLDataMinCardinalityImpl(property, cardinality, getTopDatatype());
    }

    @Override
    public OWLDataMinCardinality getOWLDataMinCardinality(int cardinality, OWLDataPropertyExpression property, OWLDataRange dataRange) {
        Objects.requireNonNull(dataRange, DATA_RANGE_CANNOT_BE_NULL);
        checkNotNegative(cardinality, CARDINALITY_CANNOT_BE_NEGATIVE);
        Objects.requireNonNull(property, PROPERTY_CANNOT_BE_NULL);
        return new OWLDataMinCardinalityImpl(property, cardinality, dataRange);
    }

    @Override
    public OWLDataMinCardinality getOWLDataMinCardinality(int cardinality, OWLDataPropertyExpression property, OWL2Datatype dataRange) {
        return getOWLDataMinCardinality(cardinality, property, dataRange.getDatatype(this));
    }

    @Override
    public OWLDataSomeValuesFrom getOWLDataSomeValuesFrom(OWLDataPropertyExpression property, OWLDataRange dataRange) {
        Objects.requireNonNull(dataRange, DATA_RANGE_CANNOT_BE_NULL);
        Objects.requireNonNull(property, PROPERTY_CANNOT_BE_NULL);
        return new OWLDataSomeValuesFromImpl(property, dataRange);
    }

    @Override
    public OWLDataSomeValuesFrom getOWLDataSomeValuesFrom(OWLDataPropertyExpression property, OWL2Datatype dataRange) {
        return getOWLDataSomeValuesFrom(property, dataRange.getDatatype(this));
    }

    @Override
    public OWLDataHasValue getOWLDataHasValue(OWLDataPropertyExpression property, OWLLiteral value) {
        Objects.requireNonNull(property, PROPERTY_CANNOT_BE_NULL);
        Objects.requireNonNull(value, VALUE_CANNOT_BE_NULL);
        return new OWLDataHasValueImpl(property, value);
    }

    @Override
    public OWLObjectComplementOf getOWLObjectComplementOf(OWLClassExpression operand) {
        Objects.requireNonNull(operand, "operand");
        return new OWLObjectComplementOfImpl(operand);
    }

    @Override
    public OWLObjectAllValuesFrom getOWLObjectAllValuesFrom(OWLObjectPropertyExpression property, OWLClassExpression classExpression) {
        Objects.requireNonNull(classExpression, CLASS_EXPRESSION_CANNOT_BE_NULL);
        Objects.requireNonNull(property, PROPERTY_CANNOT_BE_NULL);
        return new OWLObjectAllValuesFromImpl(property, classExpression);
    }

    @Override
    public OWLObjectOneOf getOWLObjectOneOf(Stream<? extends OWLIndividual> values) {
        return new OWLObjectOneOfImpl(values.map(x -> x));
    }

    @Override
    public OWLObjectExactCardinality getOWLObjectExactCardinality(int cardinality, OWLObjectPropertyExpression property) {
        checkNotNegative(cardinality, CARDINALITY_CANNOT_BE_NEGATIVE);
        Objects.requireNonNull(property, PROPERTY_CANNOT_BE_NULL);
        return new ru.avicomp.owlapi.objects.ce.OWLObjectExactCardinalityImpl(property, cardinality, OWL_THING);
    }

    @Override
    public OWLObjectExactCardinality getOWLObjectExactCardinality(int cardinality, OWLObjectPropertyExpression property, OWLClassExpression classExpression) {
        Objects.requireNonNull(classExpression, CLASS_EXPRESSION_CANNOT_BE_NULL);
        Objects.requireNonNull(property, PROPERTY_CANNOT_BE_NULL);
        checkNotNegative(cardinality, CARDINALITY_CANNOT_BE_NEGATIVE);
        return new ru.avicomp.owlapi.objects.ce.OWLObjectExactCardinalityImpl(property, cardinality, classExpression);
    }

    @Override
    public OWLObjectMinCardinality getOWLObjectMinCardinality(int cardinality, OWLObjectPropertyExpression property) {
        checkNotNegative(cardinality, CARDINALITY_CANNOT_BE_NEGATIVE);
        Objects.requireNonNull(property, PROPERTY_CANNOT_BE_NULL);
        return new ru.avicomp.owlapi.objects.ce.OWLObjectMinCardinalityImpl(property, cardinality, OWL_THING);
    }

    @Override
    public OWLObjectMinCardinality getOWLObjectMinCardinality(int cardinality, OWLObjectPropertyExpression property, OWLClassExpression classExpression) {
        Objects.requireNonNull(classExpression, CLASS_EXPRESSION_CANNOT_BE_NULL);
        Objects.requireNonNull(property, PROPERTY_CANNOT_BE_NULL);
        checkNotNegative(cardinality, CARDINALITY_CANNOT_BE_NEGATIVE);
        return new ru.avicomp.owlapi.objects.ce.OWLObjectMinCardinalityImpl(property, cardinality, classExpression);
    }

    @Override
    public OWLObjectMaxCardinality getOWLObjectMaxCardinality(int cardinality, OWLObjectPropertyExpression property) {
        checkNotNegative(cardinality, CARDINALITY_CANNOT_BE_NEGATIVE);
        Objects.requireNonNull(property, PROPERTY_CANNOT_BE_NULL);
        return new ru.avicomp.owlapi.objects.ce.OWLObjectMaxCardinalityImpl(property, cardinality, OWL_THING);
    }

    @Override
    public OWLObjectMaxCardinality getOWLObjectMaxCardinality(int cardinality, OWLObjectPropertyExpression property, OWLClassExpression classExpression) {
        checkNotNegative(cardinality, CARDINALITY_CANNOT_BE_NEGATIVE);
        Objects.requireNonNull(classExpression, CLASS_EXPRESSION_CANNOT_BE_NULL);
        Objects.requireNonNull(property, PROPERTY_CANNOT_BE_NULL);
        return new ru.avicomp.owlapi.objects.ce.OWLObjectMaxCardinalityImpl(property, cardinality, classExpression);
    }

    @Override
    public OWLObjectHasSelf getOWLObjectHasSelf(OWLObjectPropertyExpression property) {
        Objects.requireNonNull(property, PROPERTY_CANNOT_BE_NULL);
        return new OWLObjectHasSelfImpl(property);
    }

    @Override
    public OWLObjectSomeValuesFrom getOWLObjectSomeValuesFrom(OWLObjectPropertyExpression property, OWLClassExpression classExpression) {
        Objects.requireNonNull(classExpression, CLASS_EXPRESSION_CANNOT_BE_NULL);
        Objects.requireNonNull(property, PROPERTY_CANNOT_BE_NULL);
        return new OWLObjectSomeValuesFromImpl(property, classExpression);
    }

    @Override
    public OWLObjectHasValue getOWLObjectHasValue(OWLObjectPropertyExpression property, OWLIndividual individual) {
        Objects.requireNonNull(property, PROPERTY_CANNOT_BE_NULL);
        Objects.requireNonNull(individual, INDIVIDUAL_CANNOT_BE_NULL);
        return new ru.avicomp.owlapi.objects.ce.OWLObjectHasValueImpl(property, individual);
    }

    @Override
    public OWLObjectUnionOf getOWLObjectUnionOf(Stream<? extends OWLClassExpression> operands) {
        return new ru.avicomp.owlapi.objects.ce.OWLObjectUnionOfImpl(operands.map(x -> x));
    }

    @Override
    public OWLObjectUnionOf getOWLObjectUnionOf(Collection<? extends OWLClassExpression> operands) {
        return new OWLObjectUnionOfImpl(operands);
    }

    @Override
    public OWLAsymmetricObjectPropertyAxiom getOWLAsymmetricObjectPropertyAxiom(OWLObjectPropertyExpression propertyExpression, Collection<OWLAnnotation> annotations) {
        Objects.requireNonNull(propertyExpression, PROPERTY_CANNOT_BE_NULL);
        checkAnnotations(annotations);
        return new OWLAsymmetricObjectPropertyAxiomImpl(propertyExpression, annotations);
    }

    @Override
    public OWLDataPropertyDomainAxiom getOWLDataPropertyDomainAxiom(OWLDataPropertyExpression property, OWLClassExpression domain, Collection<OWLAnnotation> annotations) {
        Objects.requireNonNull(property, PROPERTY_CANNOT_BE_NULL);
        Objects.requireNonNull(domain, DOMAIN_CANNOT_BE_NULL);
        checkAnnotations(annotations);
        return new OWLDataPropertyDomainAxiomImpl(property, domain, annotations);
    }

    @Override
    public OWLDataPropertyRangeAxiom getOWLDataPropertyRangeAxiom(OWLDataPropertyExpression property, OWLDataRange owlDataRange, Collection<OWLAnnotation> annotations) {
        Objects.requireNonNull(property, PROPERTY_CANNOT_BE_NULL);
        Objects.requireNonNull(owlDataRange, OWL_DATA_RANGE_CANNOT_BE_NULL);
        checkAnnotations(annotations);
        return new OWLDataPropertyRangeAxiomImpl(property, owlDataRange, annotations);
    }

    @Override
    public OWLDataPropertyRangeAxiom getOWLDataPropertyRangeAxiom(OWLDataPropertyExpression property, OWL2Datatype owlDataRange, Collection<OWLAnnotation> annotations) {
        return getOWLDataPropertyRangeAxiom(property, owlDataRange.getDatatype(this), annotations);
    }

    @Override
    public OWLSubDataPropertyOfAxiom getOWLSubDataPropertyOfAxiom(OWLDataPropertyExpression subProperty, OWLDataPropertyExpression superProperty, Collection<OWLAnnotation> annotations) {
        Objects.requireNonNull(subProperty, SUB_PROPERTY_CANNOT_BE_NULL);
        Objects.requireNonNull(superProperty, SUPER_PROPERTY_CANNOT_BE_NULL);
        checkAnnotations(annotations);
        return new ru.avicomp.owlapi.axioms.OWLSubDataPropertyOfAxiomImpl(subProperty, superProperty, annotations);
    }

    @Override
    public OWLDeclarationAxiom getOWLDeclarationAxiom(OWLEntity owlEntity, Collection<OWLAnnotation> annotations) {
        Objects.requireNonNull(owlEntity, OWL_ENTITY_CANNOT_BE_NULL);
        checkAnnotations(annotations);
        return new OWLDeclarationAxiomImpl(owlEntity, annotations);
    }

    @Override
    public OWLDifferentIndividualsAxiom getOWLDifferentIndividualsAxiom(Collection<? extends OWLIndividual> individuals, Collection<OWLAnnotation> annotations) {
        Objects.requireNonNull(individuals, INDIVIDUALS_CANNOT_BE_NULL);
        checkAnnotations(annotations);
        return new ru.avicomp.owlapi.axioms.OWLDifferentIndividualsAxiomImpl(individuals, annotations);
    }

    /**
     * TODO: does that workaround with single class-expression make sense in our case?
     *
     * @param classes     Collection of {@link OWLClassExpression}
     * @param annotations Collection of {@link OWLAnnotation}
     * @return {@link OWLDisjointClassesAxiom}
     * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLDataFactoryImpl.java#L700'>uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl#getOWLDisjointClassesAxiom(Collection, Collection)</a>
     */
    @Override
    public OWLDisjointClassesAxiom getOWLDisjointClassesAxiom(Collection<? extends OWLClassExpression> classes, Collection<OWLAnnotation> annotations) {
        Objects.requireNonNull(classes, CLASS_EXPRESSIONS_CANNOT_BE_NULL_OR_CONTAIN_NULL);
        checkAnnotations(annotations);
        // Hack to handle the case where classExpressions has only a single member which will usually be the result of :x owl:disjointWith :x .
        if (classes.size() == 1) {
            Set<OWLClassExpression> modifiedClassExpressions = new HashSet<>(2);
            OWLClassExpression clazz = classes.iterator().next();
            OWLClass addedClass = clazz.isOWLThing() ? OWL_NOTHING : OWL_THING;
            modifiedClassExpressions.add(addedClass);
            modifiedClassExpressions.add(clazz);
            return getOWLDisjointClassesAxiom(modifiedClassExpressions, makeSingletonDisjointClassWarningAnnotation(annotations, clazz, addedClass));
        }
        return new ru.avicomp.owlapi.axioms.OWLDisjointClassesAxiomImpl(classes, annotations);
    }

    private Set<OWLAnnotation> makeSingletonDisjointClassWarningAnnotation(Collection<OWLAnnotation> annotations, OWLClassExpression classExpression, OWLClassExpression addedClass) {
        Set<OWLAnnotation> modifiedAnnotations = new HashSet<>(annotations.size() + 1);
        modifiedAnnotations.addAll(annotations);
        String provenanceComment = String.format("%s on %s", VersionInfo.getVersionInfo().getGeneratedByMessage(), Instant.now());
        OWLAnnotation provenanceAnnotation = getOWLAnnotation(RDFS_COMMENT, getOWLLiteral(provenanceComment));
        Set<OWLAnnotation> metaAnnotations = Collections.singleton(provenanceAnnotation);
        String changeComment = String.format("DisjointClasses(%s) replaced by DisjointClasses(%s %s)", classExpression, classExpression, addedClass);
        modifiedAnnotations.add(getOWLAnnotation(RDFS_COMMENT, getOWLLiteral(changeComment), metaAnnotations));
        return modifiedAnnotations;
    }

    @Override
    public OWLDisjointDataPropertiesAxiom getOWLDisjointDataPropertiesAxiom(Collection<? extends OWLDataPropertyExpression> properties, Collection<OWLAnnotation> annotations) {
        Objects.requireNonNull(properties, PROPERTIES_CANNOT_BE_NULL);
        checkAnnotations(annotations);
        return new ru.avicomp.owlapi.axioms.OWLDisjointDataPropertiesAxiomImpl(properties, annotations);
    }

    @Override
    public OWLDisjointObjectPropertiesAxiom getOWLDisjointObjectPropertiesAxiom(Collection<? extends OWLObjectPropertyExpression> properties, Collection<OWLAnnotation> annotations) {
        Objects.requireNonNull(properties, PROPERTIES_CANNOT_BE_NULL);
        checkAnnotations(annotations);
        return new ru.avicomp.owlapi.axioms.OWLDisjointObjectPropertiesAxiomImpl(properties, annotations);
    }

    @Override
    public OWLEquivalentClassesAxiom getOWLEquivalentClassesAxiom(Collection<? extends OWLClassExpression> classExpressions, Collection<OWLAnnotation> annotations) {
        Objects.requireNonNull(classExpressions, CLASS_EXPRESSIONS_CANNOT_BE_NULL);
        checkAnnotations(annotations);
        return new ru.avicomp.owlapi.axioms.OWLEquivalentClassesAxiomImpl(classExpressions, annotations);
    }

    @Override
    public OWLEquivalentDataPropertiesAxiom getOWLEquivalentDataPropertiesAxiom(Collection<? extends OWLDataPropertyExpression> properties, Collection<OWLAnnotation> annotations) {
        Objects.requireNonNull(properties, PROPERTIES_CANNOT_BE_NULL);
        checkAnnotations(annotations);
        return new ru.avicomp.owlapi.axioms.OWLEquivalentDataPropertiesAxiomImpl(properties, annotations);
    }

    @Override
    public OWLFunctionalDataPropertyAxiom getOWLFunctionalDataPropertyAxiom(OWLDataPropertyExpression property, Collection<OWLAnnotation> annotations) {
        Objects.requireNonNull(property, PROPERTY_CANNOT_BE_NULL);
        checkAnnotations(annotations);
        return new ru.avicomp.owlapi.axioms.OWLFunctionalDataPropertyAxiomImpl(property, annotations);
    }

    @Override
    public OWLFunctionalObjectPropertyAxiom getOWLFunctionalObjectPropertyAxiom(OWLObjectPropertyExpression property, Collection<OWLAnnotation> annotations) {
        Objects.requireNonNull(property, PROPERTY_CANNOT_BE_NULL);
        checkAnnotations(annotations);
        return new ru.avicomp.owlapi.axioms.OWLFunctionalObjectPropertyAxiomImpl(property, annotations);
    }

    @Override
    public OWLImportsDeclaration getOWLImportsDeclaration(IRI importedOntologyIRI) {
        Objects.requireNonNull(importedOntologyIRI, IMPORTED_ONTOLOGY_IRI_CANNOT_BE_NULL);
        return new OWLImportsDeclarationImpl(importedOntologyIRI);
    }

    @Override
    public OWLDataPropertyAssertionAxiom getOWLDataPropertyAssertionAxiom(OWLDataPropertyExpression property, OWLIndividual subject, OWLLiteral object, Collection<OWLAnnotation> annotations) {
        Objects.requireNonNull(property, PROPERTY_CANNOT_BE_NULL);
        Objects.requireNonNull(object, OBJECT_CANNOT_BE_NULL);
        Objects.requireNonNull(subject, SUBJECT_CANNOT_BE_NULL);
        checkAnnotations(annotations);
        return new ru.avicomp.owlapi.axioms.OWLDataPropertyAssertionAxiomImpl(subject, property, object, annotations);
    }

    @Override
    public OWLNegativeDataPropertyAssertionAxiom getOWLNegativeDataPropertyAssertionAxiom(OWLDataPropertyExpression property, OWLIndividual subject, OWLLiteral object, Collection<OWLAnnotation> annotations) {
        Objects.requireNonNull(property, PROPERTY_CANNOT_BE_NULL);
        Objects.requireNonNull(object, OBJECT_CANNOT_BE_NULL);
        Objects.requireNonNull(subject, SUBJECT_CANNOT_BE_NULL);
        checkAnnotations(annotations);
        return new ru.avicomp.owlapi.axioms.OWLNegativeDataPropertyAssertionAxiomImpl(subject, property, object, annotations);
    }

    @Override
    public OWLNegativeObjectPropertyAssertionAxiom getOWLNegativeObjectPropertyAssertionAxiom(OWLObjectPropertyExpression property, OWLIndividual subject, OWLIndividual object, Collection<OWLAnnotation> annotations) {
        Objects.requireNonNull(property, PROPERTY_CANNOT_BE_NULL);
        Objects.requireNonNull(object, OBJECT_CANNOT_BE_NULL);
        Objects.requireNonNull(subject, SUBJECT_CANNOT_BE_NULL);
        checkAnnotations(annotations);
        return new OWLNegativeObjectPropertyAssertionAxiomImpl(subject, property, object, annotations);
    }

    @Override
    public OWLClassAssertionAxiom getOWLClassAssertionAxiom(OWLClassExpression classExpression, OWLIndividual individual, Collection<OWLAnnotation> annotations) {
        Objects.requireNonNull(classExpression, CLASS_EXPRESSION_CANNOT_BE_NULL);
        Objects.requireNonNull(individual, INDIVIDUAL_CANNOT_BE_NULL);
        checkAnnotations(annotations);
        return new OWLClassAssertionAxiomImpl(individual, classExpression, annotations);
    }

    @Override
    public OWLInverseFunctionalObjectPropertyAxiom getOWLInverseFunctionalObjectPropertyAxiom(OWLObjectPropertyExpression property, Collection<OWLAnnotation> annotations) {
        Objects.requireNonNull(property, PROPERTY_CANNOT_BE_NULL);
        checkAnnotations(annotations);
        return new ru.avicomp.owlapi.axioms.OWLInverseFunctionalObjectPropertyAxiomImpl(property, annotations);
    }

    @Override
    public OWLIrreflexiveObjectPropertyAxiom getOWLIrreflexiveObjectPropertyAxiom(OWLObjectPropertyExpression property, Collection<OWLAnnotation> annotations) {
        Objects.requireNonNull(property, PROPERTY_CANNOT_BE_NULL);
        checkAnnotations(annotations);
        return new OWLIrreflexiveObjectPropertyAxiomImpl(property, annotations);
    }

    @Override
    public OWLObjectPropertyDomainAxiom getOWLObjectPropertyDomainAxiom(OWLObjectPropertyExpression property, OWLClassExpression classExpression, Collection<OWLAnnotation> annotations) {
        Objects.requireNonNull(property, PROPERTY_CANNOT_BE_NULL);
        Objects.requireNonNull(classExpression, CLASS_EXPRESSION_CANNOT_BE_NULL);
        checkAnnotations(annotations);
        return new OWLObjectPropertyDomainAxiomImpl(property, classExpression, annotations);
    }

    @Override
    public OWLObjectPropertyRangeAxiom getOWLObjectPropertyRangeAxiom(OWLObjectPropertyExpression property, OWLClassExpression range, Collection<OWLAnnotation> annotations) {
        Objects.requireNonNull(property, PROPERTY_CANNOT_BE_NULL);
        Objects.requireNonNull(range, RANGE_CANNOT_BE_NULL);
        checkAnnotations(annotations);
        return new OWLObjectPropertyRangeAxiomImpl(property, range, annotations);
    }

    @Override
    public OWLSubObjectPropertyOfAxiom getOWLSubObjectPropertyOfAxiom(OWLObjectPropertyExpression subProperty, OWLObjectPropertyExpression superProperty, Collection<OWLAnnotation> annotations) {
        Objects.requireNonNull(subProperty, SUB_PROPERTY_CANNOT_BE_NULL);
        Objects.requireNonNull(superProperty, SUPER_PROPERTY_CANNOT_BE_NULL);
        checkAnnotations(annotations);
        return new OWLSubObjectPropertyOfAxiomImpl(subProperty, superProperty, annotations);
    }

    @Override
    public OWLReflexiveObjectPropertyAxiom getOWLReflexiveObjectPropertyAxiom(OWLObjectPropertyExpression property, Collection<OWLAnnotation> annotations) {
        Objects.requireNonNull(property, PROPERTY_CANNOT_BE_NULL);
        checkAnnotations(annotations);
        return new ru.avicomp.owlapi.axioms.OWLReflexiveObjectPropertyAxiomImpl(property, annotations);
    }

    @Override
    public OWLSameIndividualAxiom getOWLSameIndividualAxiom(Collection<? extends OWLIndividual> individuals, Collection<OWLAnnotation> annotations) {
        Objects.requireNonNull(individuals, INDIVIDUALS_CANNOT_BE_NULL);
        checkAnnotations(annotations);
        return new ru.avicomp.owlapi.axioms.OWLSameIndividualAxiomImpl(individuals, annotations);
    }

    @Override
    public OWLSubClassOfAxiom getOWLSubClassOfAxiom(OWLClassExpression subClass, OWLClassExpression superClass, Collection<OWLAnnotation> annotations) {
        Objects.requireNonNull(subClass, SUBCLASS_CANNOT_BE_NULL);
        Objects.requireNonNull(superClass, SUPERCLASS_CANNOT_BE_NULL);
        checkAnnotations(annotations);
        return new ru.avicomp.owlapi.axioms.OWLSubClassOfAxiomImpl(subClass, superClass, annotations);
    }

    @Override
    public OWLSymmetricObjectPropertyAxiom getOWLSymmetricObjectPropertyAxiom(OWLObjectPropertyExpression property, Collection<OWLAnnotation> annotations) {
        Objects.requireNonNull(property, PROPERTY_CANNOT_BE_NULL);
        checkAnnotations(annotations);
        return new ru.avicomp.owlapi.axioms.OWLSymmetricObjectPropertyAxiomImpl(property, annotations);
    }

    @Override
    public OWLTransitiveObjectPropertyAxiom getOWLTransitiveObjectPropertyAxiom(OWLObjectPropertyExpression property, Collection<OWLAnnotation> annotations) {
        Objects.requireNonNull(property, PROPERTY_CANNOT_BE_NULL);
        checkAnnotations(annotations);
        return new OWLTransitiveObjectPropertyAxiomImpl(property, annotations);
    }

    @Override
    public OWLObjectInverseOf getOWLObjectInverseOf(OWLObjectProperty property) {
        Objects.requireNonNull(property, PROPERTY_CANNOT_BE_NULL);
        return new OWLObjectInverseOfImpl(property);
    }

    @Override
    public OWLInverseObjectPropertiesAxiom getOWLInverseObjectPropertiesAxiom(OWLObjectPropertyExpression forwardProperty, OWLObjectPropertyExpression inverseProperty, Collection<OWLAnnotation> annotations) {
        Objects.requireNonNull(forwardProperty, FORWARD_PROPERTY_CANNOT_BE_NULL);
        Objects.requireNonNull(inverseProperty, INVERSE_PROPERTY_CANNOT_BE_NULL);
        checkAnnotations(annotations);
        return new ru.avicomp.owlapi.axioms.OWLInverseObjectPropertiesAxiomImpl(forwardProperty, inverseProperty, annotations);
    }

    @Override
    public OWLSubPropertyChainOfAxiom getOWLSubPropertyChainOfAxiom(List<? extends OWLObjectPropertyExpression> chain, OWLObjectPropertyExpression superProperty, Collection<OWLAnnotation> annotations) {
        Objects.requireNonNull(superProperty, SUPER_PROPERTY_CANNOT_BE_NULL);
        Objects.requireNonNull(chain, CHAIN_CANNOT_BE_NULL);
        checkAnnotations(annotations);
        return new ru.avicomp.owlapi.axioms.OWLSubPropertyChainAxiomImpl(chain, superProperty, annotations);
    }

    @Override
    public OWLHasKeyAxiom getOWLHasKeyAxiom(OWLClassExpression ce, Collection<? extends OWLPropertyExpression> objectProperties, Collection<OWLAnnotation> annotations) {
        Objects.requireNonNull(ce, CLASS_EXPRESSION_CANNOT_BE_NULL);
        Objects.requireNonNull(objectProperties, PROPERTIES_CANNOT_BE_NULL);
        checkAnnotations(annotations);
        return new OWLHasKeyAxiomImpl(ce, objectProperties, annotations);
    }

    @Override
    public OWLDisjointUnionAxiom getOWLDisjointUnionAxiom(OWLClass owlClass, Stream<? extends OWLClassExpression> classExpressions, Collection<OWLAnnotation> annotations) {
        Objects.requireNonNull(owlClass, OWL_CLASS_CANNOT_BE_NULL);
        checkAnnotations(annotations);
        return new ru.avicomp.owlapi.axioms.OWLDisjointUnionAxiomImpl(owlClass, classExpressions.map(x -> x), annotations);
    }

    @Override
    public OWLEquivalentObjectPropertiesAxiom getOWLEquivalentObjectPropertiesAxiom(Collection<? extends OWLObjectPropertyExpression> properties, Collection<OWLAnnotation> annotations) {
        Objects.requireNonNull(properties, PROPERTIES_CANNOT_BE_NULL);
        checkAnnotations(annotations);
        return new OWLEquivalentObjectPropertiesAxiomImpl(properties, annotations);
    }

    @Override
    public OWLObjectPropertyAssertionAxiom getOWLObjectPropertyAssertionAxiom(OWLObjectPropertyExpression property, OWLIndividual individual, OWLIndividual object, Collection<OWLAnnotation> annotations) {
        Objects.requireNonNull(property, PROPERTY_CANNOT_BE_NULL);
        Objects.requireNonNull(individual, INDIVIDUAL_CANNOT_BE_NULL);
        Objects.requireNonNull(object, OBJECT_CANNOT_BE_NULL);
        checkAnnotations(annotations);
        return new ru.avicomp.owlapi.axioms.OWLObjectPropertyAssertionAxiomImpl(individual, property, object, annotations);
    }

    @Override
    public OWLSubAnnotationPropertyOfAxiom getOWLSubAnnotationPropertyOfAxiom(OWLAnnotationProperty sub, OWLAnnotationProperty sup, Collection<OWLAnnotation> annotations) {
        Objects.requireNonNull(sub, SUB_PROPERTY_CANNOT_BE_NULL);
        Objects.requireNonNull(sup, SUPER_PROPERTY_CANNOT_BE_NULL);
        checkAnnotations(annotations);
        return new ru.avicomp.owlapi.axioms.OWLSubAnnotationPropertyOfAxiomImpl(sub, sup, annotations);
    }

    // Annotations
    @Override
    public OWLAnnotationProperty getOWLAnnotationProperty(IRI iri) {
        Objects.requireNonNull(iri, IRI_CANNOT_BE_NULL);
        return new OWLAnnotationPropertyImpl(iri);
    }

    @Override
    public OWLAnnotation getOWLAnnotation(OWLAnnotationProperty property, OWLAnnotationValue value) {
        return new ru.avicomp.owlapi.objects.OWLAnnotationImplNotAnnotated(property, value);
    }

    @Override
    public OWLAnnotation getOWLAnnotation(OWLAnnotationProperty property, OWLAnnotationValue value, Stream<OWLAnnotation> annotations) {
        Objects.requireNonNull(property, PROPERTY_CANNOT_BE_NULL);
        Objects.requireNonNull(value, VALUE_CANNOT_BE_NULL);
        Objects.requireNonNull(annotations, ANNOTATIONS_CANNOT_BE_NULL);
        return new ru.avicomp.owlapi.objects.OWLAnnotationImpl(property, value, annotations);
    }

    @Override
    public OWLAnnotationAssertionAxiom getOWLAnnotationAssertionAxiom(OWLAnnotationSubject subject, OWLAnnotation annotation) {
        Objects.requireNonNull(annotation, ANNOTATION_CANNOT_BE_NULL);
        return getOWLAnnotationAssertionAxiom(annotation.getProperty(), subject, annotation.getValue(), annotation.annotations().collect(Collectors.toList()));
    }

    @Override
    public OWLAnnotationAssertionAxiom getOWLAnnotationAssertionAxiom(OWLAnnotationSubject subject, OWLAnnotation annotation, Collection<OWLAnnotation> annotations) {
        Objects.requireNonNull(annotation, ANNOTATION_CANNOT_BE_NULL);
        return getOWLAnnotationAssertionAxiom(annotation.getProperty(), subject, annotation.getValue(), annotations);
    }

    @Override
    public OWLAnnotationAssertionAxiom getOWLAnnotationAssertionAxiom(OWLAnnotationProperty property, OWLAnnotationSubject subject, OWLAnnotationValue value,
                                                                      Collection<OWLAnnotation> annotations) {
        Objects.requireNonNull(subject, SUBJECT_CANNOT_BE_NULL);
        Objects.requireNonNull(property, PROPERTY_CANNOT_BE_NULL);
        Objects.requireNonNull(value, VALUE_CANNOT_BE_NULL);
        checkAnnotations(annotations);
        return new OWLAnnotationAssertionAxiomImpl(subject, property, value, annotations);
    }

    @Override
    public OWLAnnotationAssertionAxiom getDeprecatedOWLAnnotationAssertionAxiom(IRI subject) {
        Objects.requireNonNull(subject, SUBJECT_CANNOT_BE_NULL);
        return getOWLAnnotationAssertionAxiom(getOWLDeprecated(), subject, getOWLLiteral(true));
    }

    @Override
    public OWLAnnotationPropertyDomainAxiom getOWLAnnotationPropertyDomainAxiom(OWLAnnotationProperty prop, IRI domain, Collection<OWLAnnotation> annotations) {
        Objects.requireNonNull(prop, PROPERTY_CANNOT_BE_NULL);
        Objects.requireNonNull(domain, DOMAIN_CANNOT_BE_NULL);
        checkAnnotations(annotations);
        return new OWLAnnotationPropertyDomainAxiomImpl(prop, domain, annotations);
    }

    @Override
    public OWLAnnotationPropertyRangeAxiom getOWLAnnotationPropertyRangeAxiom(OWLAnnotationProperty prop, IRI range, Collection<OWLAnnotation> annotations) {
        Objects.requireNonNull(prop, PROPERTY_CANNOT_BE_NULL);
        Objects.requireNonNull(range, RANGE_CANNOT_BE_NULL);
        checkAnnotations(annotations);
        return new OWLAnnotationPropertyRangeAxiomImpl(prop, range, annotations);
    }

    // SWRL
    @Override
    public SWRLRule getSWRLRule(Collection<? extends SWRLAtom> body, Collection<? extends SWRLAtom> head, Collection<OWLAnnotation> annotations) {
        Objects.requireNonNull(body, BODY_CANNOT_BE_NULL);
        Objects.requireNonNull(head, HEAD_CANNOT_BE_NULL);
        checkAnnotations(annotations);
        return new ru.avicomp.owlapi.axioms.SWRLRuleImpl(body, head, annotations);
    }

    @Override
    public SWRLRule getSWRLRule(Collection<? extends SWRLAtom> body, Collection<? extends SWRLAtom> head) {
        Objects.requireNonNull(body, BODY_CANNOT_BE_NULL);
        Objects.requireNonNull(head, HEAD_CANNOT_BE_NULL);
        return new SWRLRuleImpl(body, head);
    }

    @Override
    public SWRLClassAtom getSWRLClassAtom(OWLClassExpression predicate, SWRLIArgument arg) {
        Objects.requireNonNull(predicate, PREDICATE_CANNOT_BE_NULL);
        Objects.requireNonNull(arg, ARG_CANNOT_BE_NULL);
        return new ru.avicomp.owlapi.objects.swrl.SWRLClassAtomImpl(predicate, arg);
    }

    @Override
    public SWRLDataRangeAtom getSWRLDataRangeAtom(OWLDataRange predicate, SWRLDArgument arg) {
        Objects.requireNonNull(predicate, PREDICATE_CANNOT_BE_NULL);
        Objects.requireNonNull(arg, ARG_CANNOT_BE_NULL);
        return new ru.avicomp.owlapi.objects.swrl.SWRLDataRangeAtomImpl(predicate, arg);
    }

    @Override
    public SWRLDataRangeAtom getSWRLDataRangeAtom(OWL2Datatype predicate, SWRLDArgument arg) {
        return getSWRLDataRangeAtom(predicate.getDatatype(this), arg);
    }

    @Override
    public SWRLObjectPropertyAtom getSWRLObjectPropertyAtom(OWLObjectPropertyExpression property,
                                                            SWRLIArgument arg0, SWRLIArgument arg1) {
        Objects.requireNonNull(property, PROPERTY_CANNOT_BE_NULL);
        Objects.requireNonNull(arg0, ARG0_CANNOT_BE_NULL);
        Objects.requireNonNull(arg1, ARG1_CANNOT_BE_NULL);
        return new ru.avicomp.owlapi.objects.swrl.SWRLObjectPropertyAtomImpl(property, arg0, arg1);
    }

    @Override
    public SWRLDataPropertyAtom getSWRLDataPropertyAtom(OWLDataPropertyExpression property, SWRLIArgument arg0, SWRLDArgument arg1) {
        Objects.requireNonNull(property, PROPERTY_CANNOT_BE_NULL);
        Objects.requireNonNull(arg0, ARG0_CANNOT_BE_NULL);
        Objects.requireNonNull(arg1, ARG1_CANNOT_BE_NULL);
        return new ru.avicomp.owlapi.objects.swrl.SWRLDataPropertyAtomImpl(property, arg0, arg1);
    }

    @Override
    public SWRLBuiltInAtom getSWRLBuiltInAtom(IRI builtInIRI, List<SWRLDArgument> args) {
        Objects.requireNonNull(builtInIRI, BUILT_IN_IRI_CANNOT_BE_NULL);
        Objects.requireNonNull(args, ARGS_CANNOT_BE_NULL);
        return new ru.avicomp.owlapi.objects.swrl.SWRLBuiltInAtomImpl(builtInIRI, args);
    }

    @Override
    public SWRLVariable getSWRLVariable(IRI var) {
        Objects.requireNonNull(var, VAR_CANNOT_BE_NULL);
        try {
            Constructor<ru.avicomp.owlapi.objects.swrl.SWRLVariableImpl> res = SWRLVariableImpl.class.getDeclaredConstructor(IRI.class);
            res.setAccessible(true);
            return res.newInstance(var);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new IllegalStateException("Can't init SWRLVariable", e);
        }
    }

    @Override
    public SWRLIndividualArgument getSWRLIndividualArgument(OWLIndividual individual) {
        Objects.requireNonNull(individual, INDIVIDUAL_CANNOT_BE_NULL);
        return new ru.avicomp.owlapi.objects.swrl.SWRLIndividualArgumentImpl(individual);
    }

    @Override
    public SWRLLiteralArgument getSWRLLiteralArgument(OWLLiteral literal) {
        Objects.requireNonNull(literal, LITERAL_CANNOT_BE_NULL);
        return new ru.avicomp.owlapi.objects.swrl.SWRLLiteralArgumentImpl(literal);
    }

    @Override
    public SWRLDifferentIndividualsAtom getSWRLDifferentIndividualsAtom(SWRLIArgument arg0, SWRLIArgument arg1) {
        Objects.requireNonNull(arg0, ARG0_CANNOT_BE_NULL);
        Objects.requireNonNull(arg1, ARG1_CANNOT_BE_NULL);
        return new ru.avicomp.owlapi.objects.swrl.SWRLDifferentIndividualsAtomImpl(getOWLObjectProperty(OWLRDFVocabulary.OWL_DIFFERENT_FROM), arg0, arg1);
    }

    @Override
    public SWRLSameIndividualAtom getSWRLSameIndividualAtom(SWRLIArgument arg0, SWRLIArgument arg1) {
        Objects.requireNonNull(arg0, ARG0_CANNOT_BE_NULL);
        Objects.requireNonNull(arg1, ARG1_CANNOT_BE_NULL);
        return new ru.avicomp.owlapi.objects.swrl.SWRLSameIndividualAtomImpl(getOWLObjectProperty(OWLRDFVocabulary.OWL_SAME_AS), arg0, arg1);
    }

    @Override
    public OWLDatatypeDefinitionAxiom getOWLDatatypeDefinitionAxiom(OWLDatatype datatype, OWLDataRange dataRange, Collection<OWLAnnotation> annotations) {
        Objects.requireNonNull(datatype, DATATYPE_CANNOT_BE_NULL);
        Objects.requireNonNull(dataRange, DATA_RANGE_CANNOT_BE_NULL);
        checkAnnotations(annotations);
        return new OWLDatatypeDefinitionAxiomImpl(datatype, dataRange, annotations);
    }

    @Override
    public OWLDatatypeDefinitionAxiom getOWLDatatypeDefinitionAxiom(OWLDatatype datatype, OWL2Datatype dataRange, Collection<OWLAnnotation> annotations) {
        return getOWLDatatypeDefinitionAxiom(datatype, dataRange.getDatatype(this), annotations);
    }

    @Override
    public OWLLiteral getOWLLiteral(String lexicalValue, OWLDatatype datatype) {
        Objects.requireNonNull(lexicalValue, LEXICAL_VALUE_CANNOT_BE_NULL);
        Objects.requireNonNull(datatype, DATATYPE_CANNOT_BE_NULL);
        if (datatype.isRDFPlainLiteral() || datatype.equals(LANGSTRING)) {
            int sep = lexicalValue.lastIndexOf('@');
            if (sep != -1) {
                String lex = lexicalValue.substring(0, sep);
                String lang = lexicalValue.substring(sep + 1);
                return getBasicLiteral(lex, lang, LANGSTRING);
            } else {
                return getBasicLiteral(lexicalValue, XSDSTRING);
            }
        }
        // check the special cases
        return parseSpecialCases(lexicalValue, datatype);
    }

    private static OWLLiteral getBasicLiteral(String lexicalValue, String lang, OWLDatatype datatype) {
        return new ru.avicomp.owlapi.objects.literal.OWLLiteralImplNoCompression(lexicalValue, lang, datatype);
    }

    private static OWLLiteral getBasicLiteral(String lexicalValue, OWLDatatype datatype) {
        return getBasicLiteral(lexicalValue, "", datatype);
    }

    private OWLLiteral parseSpecialCases(String lexicalValue, OWLDatatype datatype) {
        OWLLiteral literal;
        try {
            if (datatype.isString()) {
                literal = getOWLLiteral(lexicalValue);
            } else if (datatype.isBoolean()) {
                literal = getOWLLiteral(asBoolean(lexicalValue.trim()));
            } else if (datatype.isFloat()) {
                literal = parseFloat(lexicalValue, datatype);
            } else if (datatype.isDouble()) {
                literal = getOWLLiteral(Double.parseDouble(lexicalValue));
            } else if (datatype.isInteger()) {
                literal = parseInteger(lexicalValue, datatype);
            } else {
                literal = getBasicLiteral(lexicalValue, datatype);
            }
        } catch (NumberFormatException e) {
            // some literal is malformed, i.e., wrong format
            literal = getBasicLiteral(lexicalValue, datatype);
        }
        return literal;
    }

    private static OWLLiteral parseFloat(String lexicalValue, OWLDatatype datatype) {
        if ("-0.0".equals(lexicalValue.trim())) {
            // according to some W3C test, this needs to be
            // different from 0.0; Java floats disagree
            return NEGATIVE_FLOAT_ZERO;
        }
        try {
            return new ru.avicomp.owlapi.objects.literal.OWLLiteralImplFloat(Float.parseFloat(lexicalValue));
        } catch (NumberFormatException e) {
            return getBasicLiteral(lexicalValue, datatype);
        }
    }

    private static OWLLiteral parseInteger(String lexicalValue, OWLDatatype datatype) {
        OWLLiteral literal;
        // again, some W3C tests require padding zeroes to make
        // literals different
        if (lexicalValue.trim().charAt(0) == '0') {
            literal = getBasicLiteral(lexicalValue, XSDINTEGER);
        } else {
            try {
                // this is fine for values that can be parsed as
                // ints - not all values are
                literal = new ru.avicomp.owlapi.objects.literal.OWLLiteralImplInteger(Integer.parseInt(lexicalValue));
            } catch (NumberFormatException ex) {
                // try as a big decimal
                literal = getBasicLiteral(lexicalValue, datatype);
            }
        }
        return literal;
    }

    @Override
    public OWLLiteral getOWLLiteral(int value) {
        return new ru.avicomp.owlapi.objects.literal.OWLLiteralImplInteger(value);
    }

    @Override
    public OWLLiteral getOWLLiteral(double value) {
        return new ru.avicomp.owlapi.objects.literal.OWLLiteralImplDouble(value);
    }

    @Override
    public OWLLiteral getOWLLiteral(float value) {
        return new ru.avicomp.owlapi.objects.literal.OWLLiteralImplFloat(value);
    }

    @Override
    public OWLLiteral getOWLLiteral(String value) {
        Objects.requireNonNull(value, VALUE_CANNOT_BE_NULL);
        return new ru.avicomp.owlapi.objects.literal.OWLLiteralImplString(value);
    }

    @Override
    public OWLLiteral getOWLLiteral(String literal, String lang) {
        Objects.requireNonNull(literal, LITERAL_CANNOT_BE_NULL);
        String normalisedLang;
        if (lang == null) {
            normalisedLang = "";
        } else {
            normalisedLang = lang.trim().toLowerCase(Locale.ENGLISH);
        }
        if (normalisedLang.isEmpty()) {
            return new OWLLiteralImplString(literal);
        } else {
            return new ru.avicomp.owlapi.objects.literal.OWLLiteralImplPlain(literal, normalisedLang);
        }
    }

    @Override
    public OWLDatatype getBooleanOWLDatatype() {
        return InternalizedEntities.XSDBOOLEAN;
    }

    @Override
    public OWLDatatype getStringOWLDatatype() {
        return InternalizedEntities.XSDSTRING;
    }

    @Override
    public OWLDatatype getDoubleOWLDatatype() {
        return InternalizedEntities.XSDDOUBLE;
    }

    @Override
    public OWLDatatype getFloatOWLDatatype() {
        return InternalizedEntities.XSDFLOAT;
    }

    @Override
    public OWLDatatype getIntegerOWLDatatype() {
        return InternalizedEntities.XSDINTEGER;
    }

    @Override
    public OWLDatatype getTopDatatype() {
        return InternalizedEntities.RDFSLITERAL;
    }

    @Override
    public OWLDatatype getRDFPlainLiteral() {
        return InternalizedEntities.PLAIN;
    }

}
