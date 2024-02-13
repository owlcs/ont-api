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

package com.github.owlcs.ontapi;

import com.github.owlcs.ontapi.internal.ONTObjectFactory;
import com.github.owlcs.ontapi.owlapi.ImportsDeclarationImpl;
import com.github.owlcs.ontapi.owlapi.InternalizedEntities;
import com.github.owlcs.ontapi.owlapi.axioms.AnnotationAssertionAxiomImpl;
import com.github.owlcs.ontapi.owlapi.axioms.AnnotationPropertyDomainAxiomImpl;
import com.github.owlcs.ontapi.owlapi.axioms.AnnotationPropertyRangeAxiomImpl;
import com.github.owlcs.ontapi.owlapi.axioms.AsymmetricObjectPropertyAxiomImpl;
import com.github.owlcs.ontapi.owlapi.axioms.ClassAssertionAxiomImpl;
import com.github.owlcs.ontapi.owlapi.axioms.DataPropertyAssertionAxiomImpl;
import com.github.owlcs.ontapi.owlapi.axioms.DataPropertyDomainAxiomImpl;
import com.github.owlcs.ontapi.owlapi.axioms.DataPropertyRangeAxiomImpl;
import com.github.owlcs.ontapi.owlapi.axioms.DatatypeDefinitionAxiomImpl;
import com.github.owlcs.ontapi.owlapi.axioms.DeclarationAxiomImpl;
import com.github.owlcs.ontapi.owlapi.axioms.DifferentIndividualsAxiomImpl;
import com.github.owlcs.ontapi.owlapi.axioms.DisjointClassesAxiomImpl;
import com.github.owlcs.ontapi.owlapi.axioms.DisjointDataPropertiesAxiomImpl;
import com.github.owlcs.ontapi.owlapi.axioms.DisjointObjectPropertiesAxiomImpl;
import com.github.owlcs.ontapi.owlapi.axioms.DisjointUnionAxiomImpl;
import com.github.owlcs.ontapi.owlapi.axioms.EquivalentClassesAxiomImpl;
import com.github.owlcs.ontapi.owlapi.axioms.EquivalentDataPropertiesAxiomImpl;
import com.github.owlcs.ontapi.owlapi.axioms.EquivalentObjectPropertiesAxiomImpl;
import com.github.owlcs.ontapi.owlapi.axioms.FunctionalDataPropertyAxiomImpl;
import com.github.owlcs.ontapi.owlapi.axioms.FunctionalObjectPropertyAxiomImpl;
import com.github.owlcs.ontapi.owlapi.axioms.HasKeyAxiomImpl;
import com.github.owlcs.ontapi.owlapi.axioms.InverseFunctionalObjectPropertyAxiomImpl;
import com.github.owlcs.ontapi.owlapi.axioms.InverseObjectPropertiesAxiomImpl;
import com.github.owlcs.ontapi.owlapi.axioms.IrreflexiveObjectPropertyAxiomImpl;
import com.github.owlcs.ontapi.owlapi.axioms.NegativeDataPropertyAssertionAxiomImpl;
import com.github.owlcs.ontapi.owlapi.axioms.NegativeObjectPropertyAssertionAxiomImpl;
import com.github.owlcs.ontapi.owlapi.axioms.ObjectPropertyAssertionAxiomImpl;
import com.github.owlcs.ontapi.owlapi.axioms.ObjectPropertyDomainAxiomImpl;
import com.github.owlcs.ontapi.owlapi.axioms.ObjectPropertyRangeAxiomImpl;
import com.github.owlcs.ontapi.owlapi.axioms.ReflexiveObjectPropertyAxiomImpl;
import com.github.owlcs.ontapi.owlapi.axioms.RuleImpl;
import com.github.owlcs.ontapi.owlapi.axioms.SameIndividualAxiomImpl;
import com.github.owlcs.ontapi.owlapi.axioms.SubAnnotationPropertyOfAxiomImpl;
import com.github.owlcs.ontapi.owlapi.axioms.SubClassOfAxiomImpl;
import com.github.owlcs.ontapi.owlapi.axioms.SubDataPropertyOfAxiomImpl;
import com.github.owlcs.ontapi.owlapi.axioms.SubObjectPropertyOfAxiomImpl;
import com.github.owlcs.ontapi.owlapi.axioms.SubPropertyChainAxiomImpl;
import com.github.owlcs.ontapi.owlapi.axioms.SymmetricObjectPropertyAxiomImpl;
import com.github.owlcs.ontapi.owlapi.axioms.TransitiveObjectPropertyAxiomImpl;
import com.github.owlcs.ontapi.owlapi.objects.AnnotationImpl;
import com.github.owlcs.ontapi.owlapi.objects.AnnotationImplNotAnnotated;
import com.github.owlcs.ontapi.owlapi.objects.AnonymousIndividualImpl;
import com.github.owlcs.ontapi.owlapi.objects.FacetRestrictionImpl;
import com.github.owlcs.ontapi.owlapi.objects.LiteralImpl;
import com.github.owlcs.ontapi.owlapi.objects.ObjectInverseOfImpl;
import com.github.owlcs.ontapi.owlapi.objects.ce.DataAllValuesFromImpl;
import com.github.owlcs.ontapi.owlapi.objects.ce.DataExactCardinalityImpl;
import com.github.owlcs.ontapi.owlapi.objects.ce.DataHasValueImpl;
import com.github.owlcs.ontapi.owlapi.objects.ce.DataMaxCardinalityImpl;
import com.github.owlcs.ontapi.owlapi.objects.ce.DataMinCardinalityImpl;
import com.github.owlcs.ontapi.owlapi.objects.ce.DataSomeValuesFromImpl;
import com.github.owlcs.ontapi.owlapi.objects.ce.ObjectAllValuesFromImpl;
import com.github.owlcs.ontapi.owlapi.objects.ce.ObjectComplementOfImpl;
import com.github.owlcs.ontapi.owlapi.objects.ce.ObjectExactCardinalityImpl;
import com.github.owlcs.ontapi.owlapi.objects.ce.ObjectHasSelfImpl;
import com.github.owlcs.ontapi.owlapi.objects.ce.ObjectHasValueImpl;
import com.github.owlcs.ontapi.owlapi.objects.ce.ObjectIntersectionOfImpl;
import com.github.owlcs.ontapi.owlapi.objects.ce.ObjectMaxCardinalityImpl;
import com.github.owlcs.ontapi.owlapi.objects.ce.ObjectMinCardinalityImpl;
import com.github.owlcs.ontapi.owlapi.objects.ce.ObjectOneOfImpl;
import com.github.owlcs.ontapi.owlapi.objects.ce.ObjectSomeValuesFromImpl;
import com.github.owlcs.ontapi.owlapi.objects.ce.ObjectUnionOfImpl;
import com.github.owlcs.ontapi.owlapi.objects.dr.DataComplementOfImpl;
import com.github.owlcs.ontapi.owlapi.objects.dr.DataIntersectionOfImpl;
import com.github.owlcs.ontapi.owlapi.objects.dr.DataOneOfImpl;
import com.github.owlcs.ontapi.owlapi.objects.dr.DataUnionOfImpl;
import com.github.owlcs.ontapi.owlapi.objects.dr.DatatypeRestrictionImpl;
import com.github.owlcs.ontapi.owlapi.objects.entity.AnnotationPropertyImpl;
import com.github.owlcs.ontapi.owlapi.objects.entity.ClassImpl;
import com.github.owlcs.ontapi.owlapi.objects.entity.DataPropertyImpl;
import com.github.owlcs.ontapi.owlapi.objects.entity.DatatypeImpl;
import com.github.owlcs.ontapi.owlapi.objects.entity.NamedIndividualImpl;
import com.github.owlcs.ontapi.owlapi.objects.entity.ObjectPropertyImpl;
import com.github.owlcs.ontapi.owlapi.objects.swrl.BuiltInAtomImpl;
import com.github.owlcs.ontapi.owlapi.objects.swrl.ClassAtomImpl;
import com.github.owlcs.ontapi.owlapi.objects.swrl.DataPropertyAtomImpl;
import com.github.owlcs.ontapi.owlapi.objects.swrl.DataRangeAtomImpl;
import com.github.owlcs.ontapi.owlapi.objects.swrl.DifferentIndividualsAtomImpl;
import com.github.owlcs.ontapi.owlapi.objects.swrl.IndividualArgumentImpl;
import com.github.owlcs.ontapi.owlapi.objects.swrl.LiteralArgumentImpl;
import com.github.owlcs.ontapi.owlapi.objects.swrl.ObjectPropertyAtomImpl;
import com.github.owlcs.ontapi.owlapi.objects.swrl.SameIndividualAtomImpl;
import com.github.owlcs.ontapi.owlapi.objects.swrl.VariableImpl;
import org.apache.jena.graph.BlankNodeId;
import org.apache.jena.graph.impl.LiteralLabel;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.VersionInfo;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.semanticweb.owlapi.vocab.OWLFacet;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.owlcs.ontapi.OntApiException.IllegalArgument;
import static com.github.owlcs.ontapi.OntApiException.notNull;
import static com.github.owlcs.ontapi.owlapi.InternalizedEntities.FALSE_LITERAL;
import static com.github.owlcs.ontapi.owlapi.InternalizedEntities.OWL_BACKWARD_COMPATIBLE_WITH;
import static com.github.owlcs.ontapi.owlapi.InternalizedEntities.OWL_BOTTOM_DATA_PROPERTY;
import static com.github.owlcs.ontapi.owlapi.InternalizedEntities.OWL_BOTTOM_OBJECT_PROPERTY;
import static com.github.owlcs.ontapi.owlapi.InternalizedEntities.OWL_DEPRECATED;
import static com.github.owlcs.ontapi.owlapi.InternalizedEntities.OWL_INCOMPATIBLE_WITH;
import static com.github.owlcs.ontapi.owlapi.InternalizedEntities.OWL_NOTHING;
import static com.github.owlcs.ontapi.owlapi.InternalizedEntities.OWL_THING;
import static com.github.owlcs.ontapi.owlapi.InternalizedEntities.OWL_TOP_DATA_PROPERTY;
import static com.github.owlcs.ontapi.owlapi.InternalizedEntities.OWL_TOP_OBJECT_PROPERTY;
import static com.github.owlcs.ontapi.owlapi.InternalizedEntities.OWL_VERSION_INFO;
import static com.github.owlcs.ontapi.owlapi.InternalizedEntities.RDFS_COMMENT;
import static com.github.owlcs.ontapi.owlapi.InternalizedEntities.RDFS_IS_DEFINED_BY;
import static com.github.owlcs.ontapi.owlapi.InternalizedEntities.RDFS_LABEL;
import static com.github.owlcs.ontapi.owlapi.InternalizedEntities.RDFS_SEE_ALSO;
import static com.github.owlcs.ontapi.owlapi.InternalizedEntities.TRUE_LITERAL;

/**
 * The facility to create {@link OWLObject OWL-API Object}s including {@link OWLAxiom OWL Axiom}s.
 * All things produced by this factory are immutable objects and can be used as input parameters to build an ontology.
 * <p>
 * Impl notes:
 * It is a modified copy-paste from {@code uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl}.
 * There are two main differences with the original (OWL-API) implementation: no compression and no cache.
 * <ul>
 * <li>Jena RDF-Graph is a primary essence in ONT-API, all information should be kept and remain in the very graph,
 *  and any literal string compression should be implemented on the graph-level (or even JVM level - it java 9 is used),
 *  if it is required, but not in this OWLDataFactory.</li>
 * <li>The cache is present in ONT-API model implementation (see {@link com.github.owlcs.ontapi.internal.InternalGraphModel}),
 * the original global caches from the OWL-API-impl seems to be superfluous here.</li>
 * </ul>
 * <p>
 * Also, this implementation is capable to produce
 * {@link OWLLiteral}s and {@link OWLAnonymousIndividual} based on Jena RDF terms:
 * {@link LiteralLabel} and {@link BlankNodeId} respectively.
 * <p>
 *
 * @author Matthew Horridge, The University Of Manchester, Bio-Health Informatics Group
 * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLDataFactoryImpl.java'>uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl</a>
 * @see ONTObjectFactory
 */
@SuppressWarnings("NullableProblems")
public class DataFactoryImpl implements DataFactory {
    private static final long serialVersionUID = -4032031232398404873L;

    private static final String LEXICAL_VALUE_CANNOT_BE_NULL = "lexicalValue cannot be null";
    private static final String LITERAL_CANNOT_BE_NULL = "literal cannot be null";
    private static final String LITERALS_CANNOT_BE_NULL = "literals cannot be null";
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
    private static final String OWL_CLASS_CANNOT_BE_NULL = "class cannot be null";
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
    private static final String FACET_CANNOT_BE_NULL = "facet cannot be null";
    private static final String DATATYPE_CANNOT_BE_NULL = "datatype cannot be null";
    private static final String FACET_RESTRICTIONS_CANNOT_BE_NULL = "facet restrictions cannot be null";
    private static final String DATA_RANGE_CANNOT_BE_NULL = "dataRange cannot be null";
    private static final String DATA_RANGES_CANNOT_BE_NULL = "data ranges cannot be null";
    private static final String ID_CANNOT_BE_NULL = "id cannot be null";
    private static final String IRI_CANNOT_BE_NULL = "iri cannot be null";
    private static final String ENTITY_TYPE_CANNOT_BE_NULL = "entityType cannot be null";
    private static final String ANNOTATIONS_CANNOT_BE_NULL = "annotations cannot be null";

    private static int nonNegativeCardinality(int value) {
        if (value < 0) {
            throw new IllegalArgument(CARDINALITY_CANNOT_BE_NEGATIVE);
        }
        return value;
    }

    private static Collection<OWLAnnotation> nonNullAnnotations(Collection<OWLAnnotation> annotations) {
        return notNull(annotations, ANNOTATIONS_CANNOT_BE_NULL);
    }

    @Override
    public void purge() {
        // nothing
    }

    @Override
    public <E extends OWLEntity> E getOWLEntity(EntityType<E> type, IRI iri) {
        return notNull(type, ENTITY_TYPE_CANNOT_BE_NULL).buildEntity(notNull(iri, IRI_CANNOT_BE_NULL), this);
    }

    @Override
    public OWLClass getOWLClass(IRI iri) {
        return new ClassImpl(notNull(iri, IRI_CANNOT_BE_NULL));
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
        return new ObjectPropertyImpl(notNull(iri, IRI_CANNOT_BE_NULL));
    }

    @Override
    public OWLObjectInverseOf getOWLObjectInverseOf(OWLObjectProperty property) {
        return new ObjectInverseOfImpl(notNull(property, PROPERTY_CANNOT_BE_NULL));
    }

    @Override
    public OWLDataProperty getOWLDataProperty(IRI iri) {
        return new DataPropertyImpl(notNull(iri, IRI_CANNOT_BE_NULL));
    }

    @Override
    public OWLAnnotationProperty getOWLAnnotationProperty(IRI iri) {
        return new AnnotationPropertyImpl(notNull(iri, IRI_CANNOT_BE_NULL));
    }

    @Override
    public OWLNamedIndividual getOWLNamedIndividual(IRI iri) {
        return new NamedIndividualImpl(notNull(iri, IRI_CANNOT_BE_NULL));
    }

    @Override
    public OWLAnonymousIndividual getOWLAnonymousIndividual(String id) {
        return getOWLAnonymousIndividual(BlankNodeId.create(notNull(id, ID_CANNOT_BE_NULL)));
    }

    @Override
    public OWLAnonymousIndividual getOWLAnonymousIndividual() {
        return getOWLAnonymousIndividual(BlankNodeId.create());
    }

    /**
     * {@inheritDoc}
     *
     * @param id {@link BlankNodeId}, not {@code null}
     * @return {@link AnonymousIndividualImpl}
     * @since 1.3.0
     */
    @Override
    public OWLAnonymousIndividual getOWLAnonymousIndividual(BlankNodeId id) {
        return new AnonymousIndividualImpl(notNull(id, ID_CANNOT_BE_NULL));
    }

    @Override
    public OWLDatatype getOWLDatatype(IRI iri) {
        return new DatatypeImpl(notNull(iri, IRI_CANNOT_BE_NULL));
    }

    @Override
    public OWLAnnotation getOWLAnnotation(OWLAnnotationProperty property, OWLAnnotationValue value) {
        return new AnnotationImplNotAnnotated(property, value);
    }

    @Override
    public OWLAnnotation getOWLAnnotation(OWLAnnotationProperty property,
                                          OWLAnnotationValue value,
                                          Stream<OWLAnnotation> annotations) {
        return getOWLAnnotation(property, value,
                notNull(annotations, ANNOTATIONS_CANNOT_BE_NULL).collect(Collectors.toList()));
    }

    @Override
    public OWLAnnotation getOWLAnnotation(OWLAnnotationProperty property,
                                          OWLAnnotationValue value,
                                          Collection<OWLAnnotation> annotations) {
        return new AnnotationImpl(notNull(property, PROPERTY_CANNOT_BE_NULL),
                notNull(value, VALUE_CANNOT_BE_NULL), nonNullAnnotations(annotations));
    }

    @Override
    public OWLDataOneOf getOWLDataOneOf(Stream<? extends OWLLiteral> values) {
        return getOWLDataOneOf(notNull(values, LITERALS_CANNOT_BE_NULL).collect(Collectors.toList()));
    }

    @Override
    public OWLDataOneOf getOWLDataOneOf(Collection<? extends OWLLiteral> values) {
        return new DataOneOfImpl(notNull(values, LITERALS_CANNOT_BE_NULL));
    }

    @Override
    public OWLDataComplementOf getOWLDataComplementOf(OWLDataRange range) {
        return new DataComplementOfImpl(notNull(range, DATA_RANGE_CANNOT_BE_NULL));
    }

    @Override
    public OWLDataComplementOf getOWLDataComplementOf(OWL2Datatype range) {
        return getOWLDataComplementOf(notNull(range, DATA_RANGE_CANNOT_BE_NULL).getDatatype(this));
    }

    @Override
    public OWLDataIntersectionOf getOWLDataIntersectionOf(Stream<? extends OWLDataRange> ranges) {
        return new DataIntersectionOfImpl(notNull(ranges, DATA_RANGES_CANNOT_BE_NULL).collect(Collectors.toList()));
    }

    @Override
    public OWLDataIntersectionOf getOWLDataIntersectionOf(Collection<? extends OWLDataRange> ranges) {
        return new DataIntersectionOfImpl(notNull(ranges, DATA_RANGES_CANNOT_BE_NULL));
    }

    @Override
    public OWLDataUnionOf getOWLDataUnionOf(Stream<? extends OWLDataRange> ranges) {
        return getOWLDataUnionOf(notNull(ranges, DATA_RANGES_CANNOT_BE_NULL).collect(Collectors.toList()));
    }

    @Override
    public OWLDataUnionOf getOWLDataUnionOf(Collection<? extends OWLDataRange> ranges) {
        return new DataUnionOfImpl(notNull(ranges, DATA_RANGES_CANNOT_BE_NULL));
    }

    @Override
    public OWLDatatypeRestriction getOWLDatatypeRestriction(OWLDatatype datatype,
                                                            Collection<OWLFacetRestriction> restrictions) {
        return new DatatypeRestrictionImpl(notNull(datatype, DATATYPE_CANNOT_BE_NULL),
                notNull(restrictions, FACET_RESTRICTIONS_CANNOT_BE_NULL));
    }

    @Override
    public OWLDatatypeRestriction getOWLDatatypeRestriction(OWLDatatype datatype,
                                                            OWLFacet facet,
                                                            OWLLiteral value) {
        return getOWLDatatypeRestriction(datatype, Collections.singletonList(getOWLFacetRestriction(facet, value)));
    }

    @Override
    public OWLFacetRestriction getOWLFacetRestriction(OWLFacet facet, OWLLiteral value) {
        return new FacetRestrictionImpl(notNull(facet, FACET_CANNOT_BE_NULL),
                notNull(value, FACET_VALUE_CANNOT_BE_NULL));
    }

    @Override
    public OWLObjectIntersectionOf getOWLObjectIntersectionOf(Stream<? extends OWLClassExpression> operands) {
        return getOWLObjectIntersectionOf(notNull(operands, CLASS_EXPRESSIONS_CANNOT_BE_NULL)
                .collect(Collectors.toList()));
    }

    @Override
    public OWLObjectIntersectionOf getOWLObjectIntersectionOf(Collection<? extends OWLClassExpression> operands) {
        return new ObjectIntersectionOfImpl(notNull(operands, CLASS_EXPRESSIONS_CANNOT_BE_NULL));
    }

    @Override
    public OWLDataAllValuesFrom getOWLDataAllValuesFrom(OWLDataPropertyExpression property, OWLDataRange range) {
        return new DataAllValuesFromImpl(notNull(property, PROPERTY_CANNOT_BE_NULL),
                notNull(range, DATA_RANGE_CANNOT_BE_NULL));
    }

    @Override
    public OWLDataAllValuesFrom getOWLDataAllValuesFrom(OWLDataPropertyExpression property, OWL2Datatype range) {
        return getOWLDataAllValuesFrom(property, notNull(range, DATA_RANGE_CANNOT_BE_NULL).getDatatype(this));
    }

    @Override
    public OWLDataExactCardinality getOWLDataExactCardinality(int cardinality, OWLDataPropertyExpression property) {
        return getOWLDataExactCardinality(cardinality, property, getTopDatatype());
    }

    @Override
    public OWLDataExactCardinality getOWLDataExactCardinality(int cardinality,
                                                              OWLDataPropertyExpression property,
                                                              OWLDataRange range) {
        return new DataExactCardinalityImpl(notNull(property, PROPERTY_CANNOT_BE_NULL),
                nonNegativeCardinality(cardinality), notNull(range, DATA_RANGE_CANNOT_BE_NULL));
    }

    @Override
    public OWLDataExactCardinality getOWLDataExactCardinality(int cardinality,
                                                              OWLDataPropertyExpression property,
                                                              OWL2Datatype range) {
        return getOWLDataExactCardinality(cardinality, property,
                notNull(range, DATA_RANGE_CANNOT_BE_NULL).getDatatype(this));
    }

    @Override
    public OWLDataMaxCardinality getOWLDataMaxCardinality(int cardinality, OWLDataPropertyExpression property) {
        return getOWLDataMaxCardinality(cardinality, property, getTopDatatype());
    }

    @Override
    public OWLDataMaxCardinality getOWLDataMaxCardinality(int cardinality,
                                                          OWLDataPropertyExpression property,
                                                          OWLDataRange range) {
        return new DataMaxCardinalityImpl(notNull(property, PROPERTY_CANNOT_BE_NULL),
                nonNegativeCardinality(cardinality), notNull(range, DATA_RANGE_CANNOT_BE_NULL));
    }

    @Override
    public OWLDataMaxCardinality getOWLDataMaxCardinality(int cardinality,
                                                          OWLDataPropertyExpression property,
                                                          OWL2Datatype range) {
        return getOWLDataMaxCardinality(cardinality, property,
                notNull(range, DATA_RANGE_CANNOT_BE_NULL).getDatatype(this));
    }

    @Override
    public OWLDataMinCardinality getOWLDataMinCardinality(int cardinality, OWLDataPropertyExpression property) {
        return getOWLDataMinCardinality(cardinality, property, getTopDatatype());
    }

    @Override
    public OWLDataMinCardinality getOWLDataMinCardinality(int cardinality,
                                                          OWLDataPropertyExpression property,
                                                          OWLDataRange range) {
        return new DataMinCardinalityImpl(notNull(property, PROPERTY_CANNOT_BE_NULL),
                nonNegativeCardinality(cardinality), notNull(range, DATA_RANGE_CANNOT_BE_NULL));
    }

    @Override
    public OWLDataMinCardinality getOWLDataMinCardinality(int cardinality,
                                                          OWLDataPropertyExpression property,
                                                          OWL2Datatype range) {
        return getOWLDataMinCardinality(cardinality, property,
                notNull(range, DATA_RANGE_CANNOT_BE_NULL).getDatatype(this));
    }

    @Override
    public OWLDataSomeValuesFrom getOWLDataSomeValuesFrom(OWLDataPropertyExpression property, OWLDataRange range) {
        return new DataSomeValuesFromImpl(notNull(property, PROPERTY_CANNOT_BE_NULL),
                notNull(range, DATA_RANGE_CANNOT_BE_NULL));
    }

    @Override
    public OWLDataSomeValuesFrom getOWLDataSomeValuesFrom(OWLDataPropertyExpression property, OWL2Datatype range) {
        return getOWLDataSomeValuesFrom(notNull(property, PROPERTY_CANNOT_BE_NULL),
                notNull(range, DATA_RANGE_CANNOT_BE_NULL).getDatatype(this));
    }

    @Override
    public OWLDataHasValue getOWLDataHasValue(OWLDataPropertyExpression property, OWLLiteral value) {
        return new DataHasValueImpl(notNull(property, PROPERTY_CANNOT_BE_NULL),
                notNull(value, VALUE_CANNOT_BE_NULL));
    }

    @Override
    public OWLObjectComplementOf getOWLObjectComplementOf(OWLClassExpression operand) {
        return new ObjectComplementOfImpl(notNull(operand, CLASS_EXPRESSION_CANNOT_BE_NULL));
    }

    @Override
    public OWLObjectAllValuesFrom getOWLObjectAllValuesFrom(OWLObjectPropertyExpression property,
                                                            OWLClassExpression classExpression) {
        return new ObjectAllValuesFromImpl(notNull(property, PROPERTY_CANNOT_BE_NULL),
                notNull(classExpression, CLASS_EXPRESSION_CANNOT_BE_NULL));
    }

    @Override
    public OWLObjectOneOf getOWLObjectOneOf(Stream<? extends OWLIndividual> values) {
        return getOWLObjectOneOf(notNull(values, CLASS_EXPRESSIONS_CANNOT_BE_NULL).collect(Collectors.toList()));
    }

    @Override
    public OWLObjectOneOf getOWLObjectOneOf(Collection<? extends OWLIndividual> values) {
        return new ObjectOneOfImpl(notNull(values, CLASS_EXPRESSIONS_CANNOT_BE_NULL));
    }

    @Override
    public OWLObjectExactCardinality getOWLObjectExactCardinality(int cardinality,
                                                                  OWLObjectPropertyExpression property) {
        return getOWLObjectExactCardinality(cardinality, property, getOWLThing());
    }

    @Override
    public OWLObjectExactCardinality getOWLObjectExactCardinality(int cardinality,
                                                                  OWLObjectPropertyExpression property,
                                                                  OWLClassExpression clazz) {
        return new ObjectExactCardinalityImpl(notNull(property, PROPERTY_CANNOT_BE_NULL),
                nonNegativeCardinality(cardinality), notNull(clazz, CLASS_EXPRESSION_CANNOT_BE_NULL));
    }

    @Override
    public OWLObjectMinCardinality getOWLObjectMinCardinality(int cardinality, OWLObjectPropertyExpression property) {
        return getOWLObjectMinCardinality(cardinality, property, getOWLThing());
    }

    @Override
    public OWLObjectMinCardinality getOWLObjectMinCardinality(int cardinality,
                                                              OWLObjectPropertyExpression property,
                                                              OWLClassExpression clazz) {
        return new ObjectMinCardinalityImpl(notNull(property, PROPERTY_CANNOT_BE_NULL),
                nonNegativeCardinality(cardinality), notNull(clazz, CLASS_EXPRESSION_CANNOT_BE_NULL));
    }

    @Override
    public OWLObjectMaxCardinality getOWLObjectMaxCardinality(int cardinality, OWLObjectPropertyExpression property) {
        return getOWLObjectMaxCardinality(cardinality, property, getOWLThing());
    }

    @Override
    public OWLObjectMaxCardinality getOWLObjectMaxCardinality(int cardinality,
                                                              OWLObjectPropertyExpression property,
                                                              OWLClassExpression clazz) {
        return new ObjectMaxCardinalityImpl(notNull(property, PROPERTY_CANNOT_BE_NULL),
                nonNegativeCardinality(cardinality), notNull(clazz, CLASS_EXPRESSION_CANNOT_BE_NULL));
    }

    @Override
    public OWLObjectHasSelf getOWLObjectHasSelf(OWLObjectPropertyExpression property) {
        return new ObjectHasSelfImpl(notNull(property, PROPERTY_CANNOT_BE_NULL));
    }

    @Override
    public OWLObjectSomeValuesFrom getOWLObjectSomeValuesFrom(OWLObjectPropertyExpression property,
                                                              OWLClassExpression clazz) {
        return new ObjectSomeValuesFromImpl(notNull(property, PROPERTY_CANNOT_BE_NULL),
                notNull(clazz, CLASS_EXPRESSION_CANNOT_BE_NULL));
    }

    @Override
    public OWLObjectHasValue getOWLObjectHasValue(OWLObjectPropertyExpression property, OWLIndividual individual) {
        return new ObjectHasValueImpl(notNull(property, PROPERTY_CANNOT_BE_NULL),
                notNull(individual, INDIVIDUAL_CANNOT_BE_NULL));
    }

    @Override
    public OWLObjectUnionOf getOWLObjectUnionOf(Stream<? extends OWLClassExpression> operands) {
        return getOWLObjectUnionOf(notNull(operands, CLASS_EXPRESSIONS_CANNOT_BE_NULL)
                .collect(Collectors.toList()));
    }

    @Override
    public OWLObjectUnionOf getOWLObjectUnionOf(Collection<? extends OWLClassExpression> operands) {
        return new ObjectUnionOfImpl(notNull(operands, CLASS_EXPRESSIONS_CANNOT_BE_NULL));
    }

    @Override
    public SWRLVariable getSWRLVariable(IRI var) {
        return new VariableImpl(notNull(var, VAR_CANNOT_BE_NULL));
    }

    @Override
    public SWRLIndividualArgument getSWRLIndividualArgument(OWLIndividual individual) {
        return new IndividualArgumentImpl(notNull(individual, INDIVIDUAL_CANNOT_BE_NULL));
    }

    @Override
    public SWRLLiteralArgument getSWRLLiteralArgument(OWLLiteral literal) {
        return new LiteralArgumentImpl(notNull(literal, LITERAL_CANNOT_BE_NULL));
    }

    @Override
    public SWRLClassAtom getSWRLClassAtom(OWLClassExpression predicate, SWRLIArgument arg) {
        return new ClassAtomImpl(notNull(predicate, PREDICATE_CANNOT_BE_NULL), notNull(arg, ARG_CANNOT_BE_NULL));
    }

    @Override
    public SWRLDataRangeAtom getSWRLDataRangeAtom(OWLDataRange predicate, SWRLDArgument arg) {
        return new DataRangeAtomImpl(notNull(predicate, PREDICATE_CANNOT_BE_NULL), notNull(arg, ARG_CANNOT_BE_NULL));
    }

    @Override
    public SWRLDataRangeAtom getSWRLDataRangeAtom(OWL2Datatype range, SWRLDArgument arg) {
        return getSWRLDataRangeAtom(notNull(range, DATA_RANGE_CANNOT_BE_NULL).getDatatype(this), arg);
    }

    @Override
    public SWRLObjectPropertyAtom getSWRLObjectPropertyAtom(OWLObjectPropertyExpression property,
                                                            SWRLIArgument arg0, SWRLIArgument arg1) {
        return new ObjectPropertyAtomImpl(notNull(property, PROPERTY_CANNOT_BE_NULL),
                notNull(arg0, ARG0_CANNOT_BE_NULL), notNull(arg1, ARG1_CANNOT_BE_NULL));
    }

    @Override
    public SWRLDataPropertyAtom getSWRLDataPropertyAtom(OWLDataPropertyExpression property,
                                                        SWRLIArgument arg0,
                                                        SWRLDArgument arg1) {
        return new DataPropertyAtomImpl(notNull(property, PROPERTY_CANNOT_BE_NULL),
                notNull(arg0, ARG0_CANNOT_BE_NULL), notNull(arg1, ARG1_CANNOT_BE_NULL));
    }

    @Override
    public SWRLBuiltInAtom getSWRLBuiltInAtom(IRI builtInIRI, List<SWRLDArgument> args) {
        return new BuiltInAtomImpl(notNull(builtInIRI, BUILT_IN_IRI_CANNOT_BE_NULL),
                notNull(args, ARGS_CANNOT_BE_NULL));
    }

    @Override
    public SWRLDifferentIndividualsAtom getSWRLDifferentIndividualsAtom(SWRLIArgument arg0, SWRLIArgument arg1) {
        return new DifferentIndividualsAtomImpl(getOWLObjectProperty(OWLRDFVocabulary.OWL_DIFFERENT_FROM),
                notNull(arg0, ARG0_CANNOT_BE_NULL), notNull(arg1, ARG1_CANNOT_BE_NULL));
    }

    @Override
    public SWRLSameIndividualAtom getSWRLSameIndividualAtom(SWRLIArgument arg0, SWRLIArgument arg1) {
        return new SameIndividualAtomImpl(getOWLObjectProperty(OWLRDFVocabulary.OWL_SAME_AS),
                notNull(arg0, ARG0_CANNOT_BE_NULL), notNull(arg1, ARG1_CANNOT_BE_NULL));
    }

    @Override
    public OWLLiteral getOWLLiteral(boolean b) {
        return b ? TRUE_LITERAL : FALSE_LITERAL;
    }

    @Override
    public OWLLiteral getOWLLiteral(int i) {
        return LiteralImpl.createLiteral(i);
    }

    @Override
    public OWLLiteral getOWLLiteral(double d) {
        return LiteralImpl.createLiteral(d);
    }

    @Override
    public OWLLiteral getOWLLiteral(float f) {
        return LiteralImpl.createLiteral(f);
    }

    @Override
    public OWLLiteral getOWLLiteral(String txt) {
        return LiteralImpl.createLiteral(notNull(txt, VALUE_CANNOT_BE_NULL));
    }

    @Override
    public OWLLiteral getOWLLiteral(String txt, String lang) {
        return LiteralImpl.createLiteral(notNull(txt, LITERAL_CANNOT_BE_NULL), lang);
    }

    @Override
    public OWLLiteral getOWLLiteral(String txt, OWLDatatype dt) {
        return LiteralImpl.createLiteral(notNull(txt, LEXICAL_VALUE_CANNOT_BE_NULL),
                notNull(dt, DATATYPE_CANNOT_BE_NULL));
    }

    /**
     * {@inheritDoc}
     *
     * @param label {@link LiteralLabel}, not {@code null}
     * @return {@link LiteralImpl}
     * @since 1.3.0
     */
    @Override
    public OWLLiteral getOWLLiteral(LiteralLabel label) {
        return LiteralImpl.newLiteral(notNull(label, VALUE_CANNOT_BE_NULL));
    }

    @Override
    public OWLDatatype getBooleanOWLDatatype() {
        return InternalizedEntities.XSD_BOOLEAN;
    }

    @Override
    public OWLDatatype getStringOWLDatatype() {
        return InternalizedEntities.XSD_STRING;
    }

    @Override
    public OWLDatatype getDoubleOWLDatatype() {
        return InternalizedEntities.XSD_DOUBLE;
    }

    @Override
    public OWLDatatype getFloatOWLDatatype() {
        return InternalizedEntities.XSD_FLOAT;
    }

    @Override
    public OWLDatatype getIntegerOWLDatatype() {
        return InternalizedEntities.XSD_INTEGER;
    }

    @Override
    public OWLDatatype getTopDatatype() {
        return InternalizedEntities.RDFS_LITERAL;
    }

    @Override
    public OWLDatatype getRDFPlainLiteral() {
        return InternalizedEntities.RDF_PLAIN_LITERAL;
    }

    @Override
    public OWLAsymmetricObjectPropertyAxiom getOWLAsymmetricObjectPropertyAxiom(OWLObjectPropertyExpression properties,
                                                                                Collection<OWLAnnotation> annotations) {
        return new AsymmetricObjectPropertyAxiomImpl(notNull(properties, PROPERTY_CANNOT_BE_NULL),
                nonNullAnnotations(annotations));
    }

    @Override
    public OWLDataPropertyDomainAxiom getOWLDataPropertyDomainAxiom(OWLDataPropertyExpression property,
                                                                    OWLClassExpression domain,
                                                                    Collection<OWLAnnotation> annotations) {
        return new DataPropertyDomainAxiomImpl(notNull(property, PROPERTY_CANNOT_BE_NULL),
                notNull(domain, DOMAIN_CANNOT_BE_NULL), nonNullAnnotations(annotations));
    }

    @Override
    public OWLDataPropertyRangeAxiom getOWLDataPropertyRangeAxiom(OWLDataPropertyExpression property,
                                                                  OWLDataRange range,
                                                                  Collection<OWLAnnotation> annotations) {
        return new DataPropertyRangeAxiomImpl(notNull(property, PROPERTY_CANNOT_BE_NULL),
                notNull(range, OWL_DATA_RANGE_CANNOT_BE_NULL), nonNullAnnotations(annotations));
    }

    @Override
    public OWLDataPropertyRangeAxiom getOWLDataPropertyRangeAxiom(OWLDataPropertyExpression property,
                                                                  OWL2Datatype range,
                                                                  Collection<OWLAnnotation> annotations) {
        return getOWLDataPropertyRangeAxiom(property,
                notNull(range, DATA_RANGE_CANNOT_BE_NULL).getDatatype(this), annotations);
    }

    @Override
    public OWLSubDataPropertyOfAxiom getOWLSubDataPropertyOfAxiom(OWLDataPropertyExpression subProperty,
                                                                  OWLDataPropertyExpression superProperty,
                                                                  Collection<OWLAnnotation> annotations) {
        return new SubDataPropertyOfAxiomImpl(notNull(subProperty, SUB_PROPERTY_CANNOT_BE_NULL),
                notNull(superProperty, SUPER_PROPERTY_CANNOT_BE_NULL), nonNullAnnotations(annotations));
    }

    @Override
    public OWLDeclarationAxiom getOWLDeclarationAxiom(OWLEntity entity, Collection<OWLAnnotation> annotations) {
        return new DeclarationAxiomImpl(notNull(entity, OWL_ENTITY_CANNOT_BE_NULL), nonNullAnnotations(annotations));
    }

    @Override
    public OWLDifferentIndividualsAxiom getOWLDifferentIndividualsAxiom(Collection<? extends OWLIndividual> individuals,
                                                                        Collection<OWLAnnotation> annotations) {
        return new DifferentIndividualsAxiomImpl(notNull(individuals, INDIVIDUALS_CANNOT_BE_NULL),
                nonNullAnnotations(annotations));
    }

    /**
     * Makes {@code DisjointClasses} axiom.
     *
     * @param classes     Collection of {@link OWLClassExpression}
     * @param annotations Collection of {@link OWLAnnotation}
     * @return {@link OWLDisjointClassesAxiom}
     * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLDataFactoryImpl.java#L686'>uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl#getOWLDisjointClassesAxiom(Collection, Collection)</a>
     */
    @Override
    public OWLDisjointClassesAxiom getOWLDisjointClassesAxiom(Collection<? extends OWLClassExpression> classes,
                                                              Collection<OWLAnnotation> annotations) {
        notNull(classes, CLASS_EXPRESSIONS_CANNOT_BE_NULL_OR_CONTAIN_NULL);
        nonNullAnnotations(annotations);
        // OWLDisjointClassesAxiomImpl internally makes a sorted distinct list,
        OWLDisjointClassesAxiom res = new DisjointClassesAxiomImpl(classes, annotations);
        // Hack to handle the case where classes has only a single member
        // which will usually be the result of :x owl:disjointWith :x .
        classes = res.getOperandsAsList();
        if (classes.size() == 1) {
            OWLClassExpression clazz = classes.iterator().next();
            if (clazz.isOWLThing() || clazz.isOWLNothing()) {
                throw new IllegalArgument(String.format("DisjointClasses(%s) cannot be created: " +
                        "it is not a syntactically valid OWL 2 axiom. " +
                        "Please consider the possibility of adding " +
                        "the axiom SubClassOf(%s, owl:Nothing) instead.", clazz, clazz));
            }
            return getOWLDisjointClassesAxiom(Arrays.asList(OWL_THING, clazz),
                    createDisjointWithThingAnnotations(annotations, clazz));
        }
        return res;
    }

    private Set<OWLAnnotation> createDisjointWithThingAnnotations(Collection<OWLAnnotation> annotations,
                                                                  OWLClassExpression clazz) {
        Set<OWLAnnotation> modifiedAnnotations = new HashSet<>(annotations.size() + 1);
        modifiedAnnotations.addAll(annotations);
        String provenanceComment = String.format("%s on %s",
                VersionInfo.getVersionInfo().getGeneratedByMessage(), Instant.now());
        OWLAnnotation provenanceAnnotation = getOWLAnnotation(RDFS_COMMENT, getOWLLiteral(provenanceComment));
        Set<OWLAnnotation> metaAnnotations = Collections.singleton(provenanceAnnotation);
        String changeComment = String.format("DisjointClasses(%s) replaced by DisjointClasses(%s %s)",
                clazz, clazz, InternalizedEntities.OWL_THING);
        modifiedAnnotations.add(getOWLAnnotation(RDFS_COMMENT, getOWLLiteral(changeComment), metaAnnotations));
        return modifiedAnnotations;
    }

    @Override
    public OWLDisjointDataPropertiesAxiom getOWLDisjointDataPropertiesAxiom(Collection<? extends OWLDataPropertyExpression> properties,
                                                                            Collection<OWLAnnotation> annotations) {
        return new DisjointDataPropertiesAxiomImpl(notNull(properties, PROPERTIES_CANNOT_BE_NULL),
                nonNullAnnotations(annotations));
    }

    @Override
    public OWLDisjointObjectPropertiesAxiom getOWLDisjointObjectPropertiesAxiom(Collection<? extends OWLObjectPropertyExpression> properties,
                                                                                Collection<OWLAnnotation> annotations) {
        return new DisjointObjectPropertiesAxiomImpl(notNull(properties, PROPERTIES_CANNOT_BE_NULL),
                nonNullAnnotations(annotations));
    }

    @Override
    public OWLEquivalentClassesAxiom getOWLEquivalentClassesAxiom(Collection<? extends OWLClassExpression> classes,
                                                                  Collection<OWLAnnotation> annotations) {
        return new EquivalentClassesAxiomImpl(notNull(classes, CLASS_EXPRESSIONS_CANNOT_BE_NULL),
                nonNullAnnotations(annotations));
    }

    @Override
    public OWLEquivalentDataPropertiesAxiom getOWLEquivalentDataPropertiesAxiom(Collection<? extends OWLDataPropertyExpression> properties,
                                                                                Collection<OWLAnnotation> annotations) {
        return new EquivalentDataPropertiesAxiomImpl(notNull(properties, PROPERTIES_CANNOT_BE_NULL),
                nonNullAnnotations(annotations));
    }

    @Override
    public OWLFunctionalDataPropertyAxiom getOWLFunctionalDataPropertyAxiom(OWLDataPropertyExpression property,
                                                                            Collection<OWLAnnotation> annotations) {
        return new FunctionalDataPropertyAxiomImpl(notNull(property, PROPERTY_CANNOT_BE_NULL),
                nonNullAnnotations(annotations));
    }

    @Override
    public OWLFunctionalObjectPropertyAxiom getOWLFunctionalObjectPropertyAxiom(OWLObjectPropertyExpression property,
                                                                                Collection<OWLAnnotation> annotations) {
        return new FunctionalObjectPropertyAxiomImpl(notNull(property, PROPERTY_CANNOT_BE_NULL),
                nonNullAnnotations(annotations));
    }

    @Override
    public OWLImportsDeclaration getOWLImportsDeclaration(IRI importedOntologyIRI) {
        return new ImportsDeclarationImpl(notNull(importedOntologyIRI, IMPORTED_ONTOLOGY_IRI_CANNOT_BE_NULL));
    }

    @Override
    public OWLDataPropertyAssertionAxiom getOWLDataPropertyAssertionAxiom(OWLDataPropertyExpression property,
                                                                          OWLIndividual subject,
                                                                          OWLLiteral object,
                                                                          Collection<OWLAnnotation> annotations) {
        return new DataPropertyAssertionAxiomImpl(notNull(subject, SUBJECT_CANNOT_BE_NULL),
                notNull(property, PROPERTY_CANNOT_BE_NULL),
                notNull(object, OBJECT_CANNOT_BE_NULL),
                nonNullAnnotations(annotations));
    }

    @Override
    public OWLNegativeDataPropertyAssertionAxiom getOWLNegativeDataPropertyAssertionAxiom(OWLDataPropertyExpression property,
                                                                                          OWLIndividual subject,
                                                                                          OWLLiteral object,
                                                                                          Collection<OWLAnnotation> annotations) {
        return new NegativeDataPropertyAssertionAxiomImpl(notNull(subject, SUBJECT_CANNOT_BE_NULL),
                notNull(property, PROPERTY_CANNOT_BE_NULL),
                notNull(object, OBJECT_CANNOT_BE_NULL),
                nonNullAnnotations(annotations));
    }

    @Override
    public OWLNegativeObjectPropertyAssertionAxiom getOWLNegativeObjectPropertyAssertionAxiom(OWLObjectPropertyExpression property,
                                                                                              OWLIndividual subject,
                                                                                              OWLIndividual object,
                                                                                              Collection<OWLAnnotation> annotations) {
        return new NegativeObjectPropertyAssertionAxiomImpl(notNull(subject, SUBJECT_CANNOT_BE_NULL),
                notNull(property, PROPERTY_CANNOT_BE_NULL),
                notNull(object, OBJECT_CANNOT_BE_NULL),
                nonNullAnnotations(annotations));
    }

    @Override
    public OWLClassAssertionAxiom getOWLClassAssertionAxiom(OWLClassExpression ces,
                                                            OWLIndividual individual,
                                                            Collection<OWLAnnotation> annotations) {
        return new ClassAssertionAxiomImpl(notNull(individual, INDIVIDUAL_CANNOT_BE_NULL),
                notNull(ces, CLASS_EXPRESSION_CANNOT_BE_NULL), nonNullAnnotations(annotations));
    }

    @Override
    public OWLInverseFunctionalObjectPropertyAxiom getOWLInverseFunctionalObjectPropertyAxiom(OWLObjectPropertyExpression property,
                                                                                              Collection<OWLAnnotation> annotations) {
        return new InverseFunctionalObjectPropertyAxiomImpl(notNull(property, PROPERTY_CANNOT_BE_NULL),
                nonNullAnnotations(annotations));
    }

    @Override
    public OWLIrreflexiveObjectPropertyAxiom getOWLIrreflexiveObjectPropertyAxiom(OWLObjectPropertyExpression property,
                                                                                  Collection<OWLAnnotation> annotations) {
        return new IrreflexiveObjectPropertyAxiomImpl(notNull(property, PROPERTY_CANNOT_BE_NULL),
                nonNullAnnotations(annotations));
    }

    @Override
    public OWLObjectPropertyDomainAxiom getOWLObjectPropertyDomainAxiom(OWLObjectPropertyExpression property,
                                                                        OWLClassExpression ces,
                                                                        Collection<OWLAnnotation> annotations) {
        return new ObjectPropertyDomainAxiomImpl(notNull(property, PROPERTY_CANNOT_BE_NULL),
                notNull(ces, CLASS_EXPRESSION_CANNOT_BE_NULL), nonNullAnnotations(annotations));
    }

    @Override
    public OWLObjectPropertyRangeAxiom getOWLObjectPropertyRangeAxiom(OWLObjectPropertyExpression property,
                                                                      OWLClassExpression range,
                                                                      Collection<OWLAnnotation> annotations) {
        return new ObjectPropertyRangeAxiomImpl(notNull(property, PROPERTY_CANNOT_BE_NULL),
                notNull(range, RANGE_CANNOT_BE_NULL), nonNullAnnotations(annotations));
    }

    @Override
    public OWLSubObjectPropertyOfAxiom getOWLSubObjectPropertyOfAxiom(OWLObjectPropertyExpression subProperty,
                                                                      OWLObjectPropertyExpression superProperty,
                                                                      Collection<OWLAnnotation> annotations) {
        return new SubObjectPropertyOfAxiomImpl(notNull(subProperty, SUB_PROPERTY_CANNOT_BE_NULL),
                notNull(superProperty, SUPER_PROPERTY_CANNOT_BE_NULL),
                nonNullAnnotations(annotations));
    }

    @Override
    public OWLReflexiveObjectPropertyAxiom getOWLReflexiveObjectPropertyAxiom(OWLObjectPropertyExpression property,
                                                                              Collection<OWLAnnotation> annotations) {
        return new ReflexiveObjectPropertyAxiomImpl(notNull(property, PROPERTY_CANNOT_BE_NULL),
                nonNullAnnotations(annotations));
    }

    @Override
    public OWLSameIndividualAxiom getOWLSameIndividualAxiom(Collection<? extends OWLIndividual> individuals,
                                                            Collection<OWLAnnotation> annotations) {
        return new SameIndividualAxiomImpl(notNull(individuals, INDIVIDUALS_CANNOT_BE_NULL),
                nonNullAnnotations(annotations));
    }

    @Override
    public OWLSubClassOfAxiom getOWLSubClassOfAxiom(OWLClassExpression subClass,
                                                    OWLClassExpression superClass,
                                                    Collection<OWLAnnotation> annotations) {
        return new SubClassOfAxiomImpl(notNull(subClass, SUBCLASS_CANNOT_BE_NULL),
                notNull(superClass, SUPERCLASS_CANNOT_BE_NULL), nonNullAnnotations(annotations));
    }

    @Override
    public OWLSymmetricObjectPropertyAxiom getOWLSymmetricObjectPropertyAxiom(OWLObjectPropertyExpression property,
                                                                              Collection<OWLAnnotation> annotations) {
        return new SymmetricObjectPropertyAxiomImpl(notNull(property, PROPERTY_CANNOT_BE_NULL),
                nonNullAnnotations(annotations));
    }

    @Override
    public OWLTransitiveObjectPropertyAxiom getOWLTransitiveObjectPropertyAxiom(OWLObjectPropertyExpression property,
                                                                                Collection<OWLAnnotation> annotations) {
        return new TransitiveObjectPropertyAxiomImpl(notNull(property, PROPERTY_CANNOT_BE_NULL),
                nonNullAnnotations(annotations));
    }

    @Override
    public OWLInverseObjectPropertiesAxiom getOWLInverseObjectPropertiesAxiom(OWLObjectPropertyExpression forwardProperty,
                                                                              OWLObjectPropertyExpression inverseProperty,
                                                                              Collection<OWLAnnotation> annotations) {
        return new InverseObjectPropertiesAxiomImpl(notNull(forwardProperty, FORWARD_PROPERTY_CANNOT_BE_NULL),
                notNull(inverseProperty, INVERSE_PROPERTY_CANNOT_BE_NULL), nonNullAnnotations(annotations));
    }

    @Override
    public OWLSubPropertyChainOfAxiom getOWLSubPropertyChainOfAxiom(List<? extends OWLObjectPropertyExpression> chain,
                                                                    OWLObjectPropertyExpression superProperty,
                                                                    Collection<OWLAnnotation> annotations) {
        return new SubPropertyChainAxiomImpl(notNull(chain, CHAIN_CANNOT_BE_NULL),
                notNull(superProperty, SUPER_PROPERTY_CANNOT_BE_NULL), nonNullAnnotations(annotations));
    }

    @Override
    public OWLHasKeyAxiom getOWLHasKeyAxiom(OWLClassExpression ce,
                                            Collection<? extends OWLPropertyExpression> properties,
                                            Collection<OWLAnnotation> annotations) {
        return new HasKeyAxiomImpl(notNull(ce, CLASS_EXPRESSION_CANNOT_BE_NULL),
                notNull(properties, PROPERTIES_CANNOT_BE_NULL), nonNullAnnotations(annotations));
    }

    @Override
    public OWLDisjointUnionAxiom getOWLDisjointUnionAxiom(OWLClass clazz,
                                                          Stream<? extends OWLClassExpression> ces,
                                                          Collection<OWLAnnotation> annotations) {
        return getOWLDisjointUnionAxiom(clazz,
                notNull(ces, CLASS_EXPRESSIONS_CANNOT_BE_NULL).collect(Collectors.toList()), annotations);
    }

    @Override
    public OWLDisjointUnionAxiom getOWLDisjointUnionAxiom(OWLClass clazz,
                                                          Collection<? extends OWLClassExpression> ces,
                                                          Collection<OWLAnnotation> annotations) {
        return new DisjointUnionAxiomImpl(notNull(clazz, OWL_CLASS_CANNOT_BE_NULL),
                notNull(ces, CLASS_EXPRESSIONS_CANNOT_BE_NULL), notNull(annotations, ANNOTATIONS_CANNOT_BE_NULL));
    }

    @Override
    public OWLEquivalentObjectPropertiesAxiom getOWLEquivalentObjectPropertiesAxiom(Collection<? extends OWLObjectPropertyExpression> properties,
                                                                                    Collection<OWLAnnotation> annotations) {
        return new EquivalentObjectPropertiesAxiomImpl(notNull(properties, PROPERTIES_CANNOT_BE_NULL),
                nonNullAnnotations(annotations));
    }

    @Override
    public OWLObjectPropertyAssertionAxiom getOWLObjectPropertyAssertionAxiom(OWLObjectPropertyExpression property,
                                                                              OWLIndividual individual,
                                                                              OWLIndividual object,
                                                                              Collection<OWLAnnotation> annotations) {
        return new ObjectPropertyAssertionAxiomImpl(notNull(individual, INDIVIDUAL_CANNOT_BE_NULL),
                notNull(property, PROPERTY_CANNOT_BE_NULL),
                notNull(object, OBJECT_CANNOT_BE_NULL), nonNullAnnotations(annotations));
    }

    @Override
    public OWLSubAnnotationPropertyOfAxiom getOWLSubAnnotationPropertyOfAxiom(OWLAnnotationProperty sub,
                                                                              OWLAnnotationProperty sup,
                                                                              Collection<OWLAnnotation> annotations) {
        return new SubAnnotationPropertyOfAxiomImpl(notNull(sub, SUB_PROPERTY_CANNOT_BE_NULL),
                notNull(sup, SUPER_PROPERTY_CANNOT_BE_NULL), nonNullAnnotations(annotations));
    }

    @Override
    public OWLAnnotationAssertionAxiom getOWLAnnotationAssertionAxiom(OWLAnnotationSubject subject,
                                                                      OWLAnnotation annotation) {
        notNull(annotation, ANNOTATION_CANNOT_BE_NULL);
        return getOWLAnnotationAssertionAxiom(annotation.getProperty(),
                subject, annotation.getValue(), annotation.annotations().collect(Collectors.toList()));
    }

    @Override
    public OWLAnnotationAssertionAxiom getOWLAnnotationAssertionAxiom(OWLAnnotationSubject subject,
                                                                      OWLAnnotation annotation,
                                                                      Collection<OWLAnnotation> annotations) {
        notNull(annotation, ANNOTATION_CANNOT_BE_NULL);
        return getOWLAnnotationAssertionAxiom(annotation.getProperty(), subject, annotation.getValue(), annotations);
    }

    @Override
    public OWLAnnotationAssertionAxiom getOWLAnnotationAssertionAxiom(OWLAnnotationProperty property,
                                                                      OWLAnnotationSubject subject,
                                                                      OWLAnnotationValue value,
                                                                      Collection<OWLAnnotation> annotations) {
        return new AnnotationAssertionAxiomImpl(notNull(subject, SUBJECT_CANNOT_BE_NULL),
                notNull(property, PROPERTY_CANNOT_BE_NULL),
                notNull(value, VALUE_CANNOT_BE_NULL),
                nonNullAnnotations(annotations));
    }

    @Override
    public OWLAnnotationAssertionAxiom getDeprecatedOWLAnnotationAssertionAxiom(IRI subject) {
        return getOWLAnnotationAssertionAxiom(getOWLDeprecated(),
                notNull(subject, SUBJECT_CANNOT_BE_NULL), getOWLLiteral(true));
    }

    @Override
    public OWLAnnotationPropertyDomainAxiom getOWLAnnotationPropertyDomainAxiom(OWLAnnotationProperty property,
                                                                                IRI domain,
                                                                                Collection<OWLAnnotation> annotations) {
        return new AnnotationPropertyDomainAxiomImpl(notNull(property, PROPERTY_CANNOT_BE_NULL),
                notNull(domain, DOMAIN_CANNOT_BE_NULL), nonNullAnnotations(annotations));
    }

    @Override
    public OWLAnnotationPropertyRangeAxiom getOWLAnnotationPropertyRangeAxiom(OWLAnnotationProperty property,
                                                                              IRI range,
                                                                              Collection<OWLAnnotation> annotations) {
        return new AnnotationPropertyRangeAxiomImpl(notNull(property, PROPERTY_CANNOT_BE_NULL),
                notNull(range, RANGE_CANNOT_BE_NULL), nonNullAnnotations(annotations));
    }

    @Override
    public OWLDatatypeDefinitionAxiom getOWLDatatypeDefinitionAxiom(OWLDatatype datatype,
                                                                    OWLDataRange range,
                                                                    Collection<OWLAnnotation> annotations) {
        return new DatatypeDefinitionAxiomImpl(notNull(datatype, DATATYPE_CANNOT_BE_NULL),
                notNull(range, DATA_RANGE_CANNOT_BE_NULL), nonNullAnnotations(annotations));
    }

    @Override
    public OWLDatatypeDefinitionAxiom getOWLDatatypeDefinitionAxiom(OWLDatatype datatype,
                                                                    OWL2Datatype range,
                                                                    Collection<OWLAnnotation> annotations) {
        return getOWLDatatypeDefinitionAxiom(datatype,
                notNull(range, DATA_RANGE_CANNOT_BE_NULL).getDatatype(this), annotations);
    }

    @Override
    public SWRLRule getSWRLRule(Collection<? extends SWRLAtom> body, Collection<? extends SWRLAtom> head) {
        return getSWRLRule(body, head, Collections.emptyList());
    }

    @Override
    public SWRLRule getSWRLRule(Collection<? extends SWRLAtom> body,
                                Collection<? extends SWRLAtom> head,
                                Collection<OWLAnnotation> annotations) {
        return new RuleImpl(notNull(body, BODY_CANNOT_BE_NULL),
                notNull(head, HEAD_CANNOT_BE_NULL), nonNullAnnotations(annotations));
    }

}
