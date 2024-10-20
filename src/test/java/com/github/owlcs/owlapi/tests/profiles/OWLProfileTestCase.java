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
package com.github.owlcs.owlapi.tests.profiles;

import com.github.owlcs.ontapi.OntologyManager;
import com.github.owlcs.ontapi.TestOntSpecifications;
import com.github.owlcs.ontapi.testutils.OWLIOUtils;
import com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory;
import com.github.owlcs.owlapi.OWLManager;
import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.profiles.OWLProfile;
import org.semanticweb.owlapi.profiles.OWLProfileViolation;
import org.semanticweb.owlapi.profiles.OWLProfileViolationVisitor;
import org.semanticweb.owlapi.profiles.OWLProfileViolationVisitorEx;
import org.semanticweb.owlapi.profiles.Profiles;
import org.semanticweb.owlapi.profiles.violations.CycleInDatatypeDefinition;
import org.semanticweb.owlapi.profiles.violations.DatatypeIRIAlsoUsedAsClassIRI;
import org.semanticweb.owlapi.profiles.violations.EmptyOneOfAxiom;
import org.semanticweb.owlapi.profiles.violations.IllegalPunning;
import org.semanticweb.owlapi.profiles.violations.InsufficientIndividuals;
import org.semanticweb.owlapi.profiles.violations.InsufficientOperands;
import org.semanticweb.owlapi.profiles.violations.InsufficientPropertyExpressions;
import org.semanticweb.owlapi.profiles.violations.LastPropertyInChainNotInImposedRange;
import org.semanticweb.owlapi.profiles.violations.LexicalNotInLexicalSpace;
import org.semanticweb.owlapi.profiles.violations.OntologyIRINotAbsolute;
import org.semanticweb.owlapi.profiles.violations.OntologyVersionIRINotAbsolute;
import org.semanticweb.owlapi.profiles.violations.UseOfAnonymousIndividual;
import org.semanticweb.owlapi.profiles.violations.UseOfBuiltInDatatypeInDatatypeDefinition;
import org.semanticweb.owlapi.profiles.violations.UseOfDataOneOfWithMultipleLiterals;
import org.semanticweb.owlapi.profiles.violations.UseOfDefinedDatatypeInDatatypeRestriction;
import org.semanticweb.owlapi.profiles.violations.UseOfIllegalAxiom;
import org.semanticweb.owlapi.profiles.violations.UseOfIllegalClassExpression;
import org.semanticweb.owlapi.profiles.violations.UseOfIllegalDataRange;
import org.semanticweb.owlapi.profiles.violations.UseOfIllegalFacetRestriction;
import org.semanticweb.owlapi.profiles.violations.UseOfNonAbsoluteIRI;
import org.semanticweb.owlapi.profiles.violations.UseOfNonAtomicClassExpression;
import org.semanticweb.owlapi.profiles.violations.UseOfNonEquivalentClassExpression;
import org.semanticweb.owlapi.profiles.violations.UseOfNonSimplePropertyInAsymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.profiles.violations.UseOfNonSimplePropertyInCardinalityRestriction;
import org.semanticweb.owlapi.profiles.violations.UseOfNonSimplePropertyInDisjointPropertiesAxiom;
import org.semanticweb.owlapi.profiles.violations.UseOfNonSimplePropertyInFunctionalPropertyAxiom;
import org.semanticweb.owlapi.profiles.violations.UseOfNonSimplePropertyInInverseFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.profiles.violations.UseOfNonSimplePropertyInIrreflexivePropertyAxiom;
import org.semanticweb.owlapi.profiles.violations.UseOfNonSimplePropertyInObjectHasSelf;
import org.semanticweb.owlapi.profiles.violations.UseOfNonSubClassExpression;
import org.semanticweb.owlapi.profiles.violations.UseOfNonSuperClassExpression;
import org.semanticweb.owlapi.profiles.violations.UseOfObjectOneOfWithMultipleIndividuals;
import org.semanticweb.owlapi.profiles.violations.UseOfObjectPropertyInverse;
import org.semanticweb.owlapi.profiles.violations.UseOfPropertyInChainCausesCycle;
import org.semanticweb.owlapi.profiles.violations.UseOfReservedVocabularyForAnnotationPropertyIRI;
import org.semanticweb.owlapi.profiles.violations.UseOfReservedVocabularyForClassIRI;
import org.semanticweb.owlapi.profiles.violations.UseOfReservedVocabularyForDataPropertyIRI;
import org.semanticweb.owlapi.profiles.violations.UseOfReservedVocabularyForIndividualIRI;
import org.semanticweb.owlapi.profiles.violations.UseOfReservedVocabularyForObjectPropertyIRI;
import org.semanticweb.owlapi.profiles.violations.UseOfReservedVocabularyForOntologyIRI;
import org.semanticweb.owlapi.profiles.violations.UseOfReservedVocabularyForVersionIRI;
import org.semanticweb.owlapi.profiles.violations.UseOfTopDataPropertyAsSubPropertyInSubPropertyAxiom;
import org.semanticweb.owlapi.profiles.violations.UseOfUndeclaredAnnotationProperty;
import org.semanticweb.owlapi.profiles.violations.UseOfUndeclaredClass;
import org.semanticweb.owlapi.profiles.violations.UseOfUndeclaredDataProperty;
import org.semanticweb.owlapi.profiles.violations.UseOfUndeclaredDatatype;
import org.semanticweb.owlapi.profiles.violations.UseOfUndeclaredObjectProperty;
import org.semanticweb.owlapi.profiles.violations.UseOfUnknownDatatype;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.semanticweb.owlapi.vocab.OWLFacet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.AnnotationProperty;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.AnonymousIndividual;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.AsymmetricObjectProperty;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.Boolean;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.Class;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.ClassAssertion;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.DataAllValuesFrom;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.DataComplementOf;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.DataExactCardinality;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.DataIntersectionOf;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.DataMaxCardinality;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.DataMinCardinality;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.DataOneOf;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.DataProperty;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.DataPropertyAssertion;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.DataPropertyDomain;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.DataPropertyRange;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.DataUnionOf;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.Datatype;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.DatatypeDefinition;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.DatatypeRestriction;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.DifferentIndividuals;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.DisjointClasses;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.DisjointDataProperties;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.DisjointObjectProperties;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.DisjointUnion;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.Double;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.EquivalentClasses;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.EquivalentDataProperties;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.EquivalentObjectProperties;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.FacetRestriction;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.FunctionalDataProperty;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.FunctionalObjectProperty;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.HasKey;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.IRI;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.Integer;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.InverseFunctionalObjectProperty;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.InverseObjectProperties;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.IrreflexiveObjectProperty;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.Literal;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.NamedIndividual;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.NegativeDataPropertyAssertion;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.NegativeObjectPropertyAssertion;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.OWLNothing;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.OWLThing;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.ObjectAllValuesFrom;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.ObjectComplementOf;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.ObjectExactCardinality;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.ObjectHasSelf;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.ObjectIntersectionOf;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.ObjectInverseOf;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.ObjectMaxCardinality;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.ObjectMinCardinality;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.ObjectOneOf;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.ObjectProperty;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.ObjectPropertyDomain;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.ObjectPropertyRange;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.ObjectSomeValuesFrom;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.ObjectUnionOf;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.SameIndividual;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.SubAnnotationPropertyOf;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.SubClassOf;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.SubDataPropertyOf;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.SubObjectPropertyOf;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.SubPropertyChainOf;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.SymmetricObjectProperty;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.TransitiveObjectProperty;

@SuppressWarnings({"rawtypes"})
public class OWLProfileTestCase extends TestBase {

    private static final String START = OWLThing().getIRI().getNamespace();
    private static final OWLClass CL = Class(IRI("urn:test#", "fakeclass"));
    private static final OWLDataProperty DATAP = DataProperty(IRI("urn:datatype#", "fakedatatypeproperty"));
    private static final OWLDataPropertyRangeAxiom DATA_PROPERTY_RANGE2 = DataPropertyRange(DATAP,
            DatatypeRestriction(Integer(), FacetRestriction(OWLFacet.LANG_RANGE, Literal(1))));
    private static final OWLDataPropertyRangeAxiom DATA_PROPERTY_RANGE = DataPropertyRange(DATAP,
            DatatypeRestriction(Integer(), FacetRestriction(OWLFacet.MAX_EXCLUSIVE, Literal(1))));
    private static final OWLObjectProperty OP = ObjectProperty(IRI("urn:datatype#", "fakeobjectproperty"));
    private static final OWLDatatype UNKNOWNFAKEDATATYPE = Datatype(IRI(START, "unknownfakedatatype"));
    private static final OWLDatatype FAKEUNDECLAREDDATATYPE = Datatype(IRI("urn:datatype#", "fakeundeclareddatatype"));
    private static final OWLDatatype FAKEDATATYPE = Datatype(IRI("urn:datatype#", "fakedatatype"));
    private static final IRI onto = IRI.create("urn:test#", "ontology");
    private static final OWLDataFactory DF = OWLManager.getOWLDataFactory();
    private static final OWLObjectProperty P = ObjectProperty(IRI("urn:test#", "objectproperty"));
    private OWLOntology o;

    @BeforeEach
    @Override
    public void setupManagersClean() {
        super.setupManagersClean();
        if (!OWLManager.DEBUG_USE_OWL) {
            // allow illegal punnings
            com.github.owlcs.ontapi.config.OntLoaderConfiguration conf = ((OntologyManager) m).getOntologyLoaderConfiguration()
                    .setSpecification(TestOntSpecifications.OWL2_FULL_NO_INF);
            m.setOntologyLoaderConfiguration(conf);
        }
        try {
            o = getOWLOntology(onto);
        } catch (OWLOntologyCreationException e) {
            throw new AssertionError("Exception", e);
        }
    }

    private void declare(OWLOntology ont, OWLEntity... entities) {
        Stream.of(entities).map(OWLFunctionalSyntaxFactory::Declaration).forEach(ont::add);
    }

    private static final Comparator<Class> comp = Comparator.comparing(Class::getSimpleName);

    private void checkInCollection(List<OWLProfileViolation> violations, Class[] inputList) {
        List<Class> list = new ArrayList<>(Arrays.asList(inputList));
        List<Class> list1 = new ArrayList<>();
        for (OWLProfileViolation v : violations) {
            list1.add(v.getClass());
        }
        list.sort(comp);
        list1.sort(comp);
        Assertions.assertEquals(list, list1);
    }

    private void runAssert(OWLOntology ontology, OWLProfile profile, Class<?>... expectedViolations) {
        OWLIOUtils.print(ontology);
        List<OWLProfileViolation> violations = profile.checkOntology(ontology).getViolations();
        violations.forEach(v -> LOGGER.debug("Violation: [{}]", v));
        Assertions.assertEquals(expectedViolations.length, violations.size());
        checkInCollection(violations, expectedViolations);
        for (OWLProfileViolation violation : violations) {
            ontology.getOWLOntologyManager().applyChanges(violation.repair());
            violation.accept(new OWLProfileViolationVisitor() {
            });
            violation.accept(new OWLProfileViolationVisitorEx<String>() {

                @SuppressWarnings("NullableProblems")
                @Override
                public Optional<String> doDefault(OWLProfileViolation object) {
                    return Optional.of(object.toString());
                }
            });
        }
        violations = profile.checkOntology(ontology).getViolations();
        Assertions.assertEquals(0, violations.size());
    }

    @Test
    @Tests(method = "public Object visit(OWLDatatype datatype)")
    public void testShouldCreateViolationForOWLDatatypeInOWL2DLProfile() {
        declare(o, UNKNOWNFAKEDATATYPE, FAKEDATATYPE, Class(FAKEDATATYPE.getIRI()), DATAP);
        o.addAxiom(DataPropertyRange(DATAP, FAKEUNDECLAREDDATATYPE));
        runAssert(o, Profiles.OWL2_DL, UseOfUndeclaredDatatype.class,
                DatatypeIRIAlsoUsedAsClassIRI.class, UseOfUnknownDatatype.class,
                DatatypeIRIAlsoUsedAsClassIRI.class);
    }

    @Test
    @Tests(method = "public Object visit(OWLDatatypeDefinitionAxiom axiom)")
    public void testShouldCreateViolationForOWLDatatypeDefinitionAxiomInOWL2DLProfile() {
        declare(o, Integer(), Boolean(), FAKEDATATYPE);
        o.add(DatatypeDefinition(Boolean(), Integer()), DatatypeDefinition(FAKEDATATYPE, Integer()), DatatypeDefinition(
                Integer(), FAKEDATATYPE));
        Class[] expectedViolations = {CycleInDatatypeDefinition.class, CycleInDatatypeDefinition.class,
                UseOfBuiltInDatatypeInDatatypeDefinition.class, UseOfBuiltInDatatypeInDatatypeDefinition.class};
        runAssert(o, Profiles.OWL2_DL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDatatypeDefinitionAxiom axiom)")
    public void testShouldCreateViolationForOWLDatatypeDefinitionAxiomInOWL2DLProfileCycles() {
        OWLDatatype d = Datatype(IRI(START, "test"));
        declare(o, d, Integer(), Boolean(), FAKEDATATYPE);
        o.add(DatatypeDefinition(d, Boolean()), DatatypeDefinition(Boolean(), d), DatatypeDefinition(FAKEDATATYPE,
                Integer()), DatatypeDefinition(Integer(), FAKEDATATYPE));
        runAssert(o, Profiles.OWL2_DL, CycleInDatatypeDefinition.class,
                CycleInDatatypeDefinition.class, CycleInDatatypeDefinition.class,
                CycleInDatatypeDefinition.class, UseOfBuiltInDatatypeInDatatypeDefinition.class,
                UseOfBuiltInDatatypeInDatatypeDefinition.class,
                UseOfBuiltInDatatypeInDatatypeDefinition.class, UseOfUnknownDatatype.class,
                UseOfUnknownDatatype.class, UseOfUnknownDatatype.class);
    }

    @Test
    @Tests(method = "public Object visit(OWLObjectProperty property)")
    public void testShouldCreateViolationForOWLObjectPropertyInOWL2DLProfile() {
        IRI iri = IRI(START, "test");
        declare(o, ObjectProperty(iri), DataProperty(iri), AnnotationProperty(iri));
        o.addAxiom(SubObjectPropertyOf(OP, ObjectProperty(iri)));
        runAssert(o, Profiles.OWL2_DL, UseOfReservedVocabularyForObjectPropertyIRI.class,
                UseOfReservedVocabularyForObjectPropertyIRI.class,
                UseOfReservedVocabularyForDataPropertyIRI.class,
                UseOfReservedVocabularyForAnnotationPropertyIRI.class,
                UseOfUndeclaredObjectProperty.class, IllegalPunning.class, IllegalPunning.class,
                IllegalPunning.class, IllegalPunning.class, IllegalPunning.class, IllegalPunning.class,
                IllegalPunning.class, IllegalPunning.class);
    }

    @Test
    @Tests(method = "public Object visit(OWLDataProperty property)")
    public void testShouldCreateViolationForOWLDataPropertyInOWL2DLProfile1() {
        declare(o, DataProperty(IRI(START, "fail")));
        runAssert(o, Profiles.OWL2_DL, UseOfReservedVocabularyForDataPropertyIRI.class);
    }

    @Test
    @Tests(method = "public Object visit(OWLDataProperty property)")
    public void testShouldCreateViolationForOWLDataPropertyInOWL2DLProfile2() {
        o.addAxiom(FunctionalDataProperty(DATAP));
        Class[] expectedViolations = {UseOfUndeclaredDataProperty.class};
        runAssert(o, Profiles.OWL2_DL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDataProperty property)")
    public void testShouldCreateViolationForOWLDataPropertyInOWL2DLProfile3() {
        declare(o, DATAP, AnnotationProperty(DATAP.getIRI()));
        runAssert(o, Profiles.OWL2_DL, IllegalPunning.class, IllegalPunning.class);
    }

    @Test
    @Tests(method = "public Object visit(OWLDataProperty property)")
    public void testShouldCreateViolationForOWLDataPropertyInOWL2DLProfile4() {
        declare(o, DATAP, ObjectProperty(DATAP.getIRI()));
        runAssert(o, Profiles.OWL2_DL, IllegalPunning.class, IllegalPunning.class);
    }

    @Test
    @Tests(method = "public Object visit(OWLAnnotationProperty property)")
    public void testShouldCreateViolationForOWLAnnotationPropertyInOWL2DLProfile() {
        IRI iri = IRI(START, "test");
        declare(o, ObjectProperty(iri), DataProperty(iri), AnnotationProperty(iri));
        o.add(SubAnnotationPropertyOf(AnnotationProperty(IRI("urn:test#", "t")), AnnotationProperty(iri)));
        runAssert(o, Profiles.OWL2_DL,
                UseOfReservedVocabularyForAnnotationPropertyIRI.class,
                UseOfReservedVocabularyForAnnotationPropertyIRI.class,
                UseOfReservedVocabularyForDataPropertyIRI.class,
                UseOfReservedVocabularyForObjectPropertyIRI.class,
                UseOfUndeclaredAnnotationProperty.class,
                IllegalPunning.class, IllegalPunning.class, IllegalPunning.class, IllegalPunning.class,
                IllegalPunning.class, IllegalPunning.class, IllegalPunning.class, IllegalPunning.class);
    }

    @Test
    @Tests(method = "public Object visit(OWLOntology ontology)")
    public void testShouldCreateViolationForOWLOntologyInOWL2DLProfile() throws OWLOntologyCreationException {
        o = m.createOntology(new OWLOntologyID(Optional.of(IRI(START, "test")), Optional.of(IRI(START, "test1"))));
        Class[] expectedViolations = {UseOfReservedVocabularyForOntologyIRI.class,
                UseOfReservedVocabularyForVersionIRI.class};
        runAssert(o, Profiles.OWL2_DL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLClass desc)")
    public void testShouldCreateViolationForOWLClassInOWL2DLProfile() {
        declare(o, Class(IRI(START, "test")), FAKEDATATYPE);
        o.add(ClassAssertion(Class(FAKEDATATYPE.getIRI()), AnonymousIndividual()));
        runAssert(o, Profiles.OWL2_DL, UseOfUndeclaredClass.class, DatatypeIRIAlsoUsedAsClassIRI.class,
                UseOfReservedVocabularyForClassIRI.class, DatatypeIRIAlsoUsedAsClassIRI.class);
    }

    @Test
    @Tests(method = "public Object visit(OWLDataOneOf node)")
    public void testShouldCreateViolationForOWLDataOneOfInOWL2DLProfile() {
        declare(o, DATAP);
        o.add(DataPropertyRange(DATAP, DataOneOf()));
        Class[] expectedViolations = {EmptyOneOfAxiom.class};
        runAssert(o, Profiles.OWL2_DL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDataUnionOf node)")
    public void testShouldCreateViolationForOWLDataUnionOfInOWL2DLProfile() {
        declare(o, DATAP);
        o.add(DataPropertyRange(DATAP, DataUnionOf()));
        Class[] expectedViolations = {InsufficientOperands.class};
        runAssert(o, Profiles.OWL2_DL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDataIntersectionOf node)")
    public void testShouldCreateViolationForOWLDataIntersectionOfInOWL2DLProfile() {
        declare(o, DATAP);
        o.add(DataPropertyRange(DATAP, DataIntersectionOf()));
        Class[] expectedViolations = {InsufficientOperands.class};
        runAssert(o, Profiles.OWL2_DL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLObjectIntersectionOf node)")
    public void testShouldCreateViolationForOWLObjectIntersectionOfInOWL2DLProfile() {
        declare(o, OP);
        o.add(ObjectPropertyRange(OP, ObjectIntersectionOf()));
        Class[] expectedViolations = {InsufficientOperands.class};
        runAssert(o, Profiles.OWL2_DL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLObjectOneOf node)")
    public void testShouldCreateViolationForOWLObjectOneOfInOWL2DLProfile() {
        declare(o, OP);
        o.add(ObjectPropertyRange(OP, ObjectOneOf()));
        Class[] expectedViolations = {EmptyOneOfAxiom.class};
        runAssert(o, Profiles.OWL2_DL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLObjectUnionOf node)")
    public void testShouldCreateViolationForOWLObjectUnionOfInOWL2DLProfile() {
        declare(o, OP);
        o.add(ObjectPropertyRange(OP, ObjectUnionOf()));
        Class[] expectedViolations = {InsufficientOperands.class};
        runAssert(o, Profiles.OWL2_DL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLEquivalentClassesAxiom node)")
    public void testShouldCreateViolationForOWLEquivalentClassesAxiomInOWL2DLProfile() {
        declare(o, OP);
        o.add(EquivalentClasses());
        if (OWLManager.DEBUG_USE_OWL) {
            runAssert(o, Profiles.OWL2_DL, Stream.of(InsufficientOperands.class).toArray(Class[]::new));
        } else {
            // ONT-API: no possibility to add axiom which has no any triples.
            Assertions.assertTrue(Profiles.OWL2_DL.checkOntology(o).isInProfile());
        }
    }

    @Test
    @Tests(method = "public Object visit(OWLDisjointClassesAxiom node)")
    public void testShouldCreateViolationForOWLDisjointClassesAxiomInOWL2DLProfile() {
        declare(o, OP);
        o.add(DisjointClasses());
        if (OWLManager.DEBUG_USE_OWL) {
            Class[] expectedViolations = {InsufficientOperands.class};
            runAssert(o, Profiles.OWL2_DL, expectedViolations);
        } else {
            // In ONT-API there is no possibility to add or read empty owl:disjointWith axiom.
            // Although it is possible to create section "_:x rdf:type owl:AllDisjointClasses. _:x owl:members rdf:nil.",
            // but it doesn't make sense and disabled in API. So nothing to check
            Assertions.assertTrue(Profiles.OWL2_DL.checkOntology(o).isInProfile());
        }
    }

    @Test
    @Tests(method = "public Object visit(OWLDisjointUnionAxiom node)")
    public void testShouldCreateViolationForOWLDisjointUnionAxiomInOWL2DLProfile() {
        declare(o, OP);
        OWLClass otherfakeclass = Class(IRI("urn:test#", "otherfakeclass"));
        declare(o, CL);
        declare(o, otherfakeclass);
        o.add(DisjointUnion(CL, otherfakeclass));
        Class[] expectedViolations = {InsufficientOperands.class};
        runAssert(o, Profiles.OWL2_DL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLEquivalentObjectPropertiesAxiom node)")
    public void testShouldCreateViolationForOWLEquivalentObjectPropertiesAxiomInOWL2DLProfile() {
        o.add(EquivalentObjectProperties());
        if (OWLManager.DEBUG_USE_OWL) {
            Class[] expectedViolations = {InsufficientPropertyExpressions.class};
            runAssert(o, Profiles.OWL2_DL, expectedViolations);
        } else {
            // in ONT-API there is no possibility to add or read empty owl:equivalentProperty axiom
            Assertions.assertTrue(Profiles.OWL2_DL.checkOntology(o).isInProfile());
        }
    }

    @Test
    @Tests(method = "public Object visit(OWLDisjointDataPropertiesAxiom node)")
    public void testShouldCreateViolationForOWLDisjointDataPropertiesAxiomInOWL2DLProfile() {
        o.add(DisjointDataProperties());
        if (OWLManager.DEBUG_USE_OWL) {
            Class[] expectedViolations = {InsufficientPropertyExpressions.class};
            runAssert(o, Profiles.OWL2_DL, expectedViolations);
        } else {
            // In ONT-API there is no possibility to add or read empty owl:propertyDisjointWith axiom.
            // Although it is possible to create section "_:x rdf:type owl:AllDisjointProperties. _:x owl:members rdf:nil.",
            // but it doesn't make sense and disabled in API. So nothing to check
            Assertions.assertTrue(Profiles.OWL2_DL.checkOntology(o).isInProfile());
        }
    }

    @Test
    @Tests(method = "public Object visit(OWLEquivalentDataPropertiesAxiom node)")
    public void testShouldCreateViolationForOWLEquivalentDataPropertiesAxiomInOWL2DLProfile() {
        o.add(EquivalentDataProperties());
        if (OWLManager.DEBUG_USE_OWL) {
            runAssert(o, Profiles.OWL2_DL, Stream.of(InsufficientPropertyExpressions.class).toArray(Class[]::new));
        } else {
            // in ONT-API there is no possibility to add or read empty owl:equivalentProperty axiom
            Assertions.assertTrue(Profiles.OWL2_DL.checkOntology(o).isInProfile());
        }
    }

    @Test
    @Tests(method = "public Object visit(OWLHasKeyAxiom node)")
    public void testShouldCreateViolationForOWLHasKeyAxiomInOWL2DLProfile() {
        declare(o, CL);
        o.add(HasKey(CL));
        Class[] expectedViolations = {InsufficientPropertyExpressions.class};
        runAssert(o, Profiles.OWL2_DL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLSameIndividualAxiom node)")
    public void testShouldCreateViolationForOWLSameIndividualAxiomInOWL2DLProfile() {
        o.add(SameIndividual());
        if (OWLManager.DEBUG_USE_OWL) {
            Class[] expectedViolations = {InsufficientIndividuals.class};
            runAssert(o, Profiles.OWL2_DL, expectedViolations);
        } else {
            // In ONT-API there is no possibility to add(or read) empty owl:sameAs axiom.
            Assertions.assertTrue(Profiles.OWL2_DL.checkOntology(o).isInProfile());
        }
    }

    @Test
    @Tests(method = "public Object visit(OWLDifferentIndividualsAxiom node)")
    public void testShouldCreateViolationForOWLDifferentIndividualsAxiomInOWL2DLProfile() {
        o.add(DifferentIndividuals());
        if (OWLManager.DEBUG_USE_OWL) {
            Class[] expectedViolations = {InsufficientIndividuals.class};
            runAssert(o, Profiles.OWL2_DL, expectedViolations);
        } else {
            // In ONT-API there is no possibility to add or read empty owl:differentFrom axiom.
            // Although it is possible to create section "_:x rdf:type owl:AllDifferent. _:x owl:members rdf:nil.",
            // it doesn't make sense and disabled in API. So nothing to check
            Assertions.assertTrue(Profiles.OWL2_DL.checkOntology(o).isInProfile());
        }
    }

    @Test
    @Tests(method = "public Object visit(OWLNamedIndividual individual)")
    public void testShouldCreateViolationForOWLNamedIndividualInOWL2DLProfile() {
        o.add(ClassAssertion(OWLThing(), NamedIndividual(IRI(START, "i"))));
        Class[] expectedViolations = {UseOfReservedVocabularyForIndividualIRI.class};
        runAssert(o, Profiles.OWL2_DL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLSubDataPropertyOfAxiom axiom)")
    public void testShouldCreateViolationForOWLSubDataPropertyOfAxiomInOWL2DLProfile() {
        o.add(SubDataPropertyOf(DF.getOWLTopDataProperty(), DF.getOWLTopDataProperty()));
        Class[] expectedViolations = {UseOfTopDataPropertyAsSubPropertyInSubPropertyAxiom.class};
        runAssert(o, Profiles.OWL2_DL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLObjectMinCardinality desc)")
    public void testShouldCreateViolationForOWLObjectMinCardinalityInOWL2DLProfile() {
        declare(o, OP, CL);
        o.add(TransitiveObjectProperty(OP), SubClassOf(CL, ObjectMinCardinality(1, OP, OWLThing())));
        Class[] expectedViolations = {UseOfNonSimplePropertyInCardinalityRestriction.class};
        runAssert(o, Profiles.OWL2_DL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLObjectMaxCardinality desc)")
    public void testShouldCreateViolationForOWLObjectMaxCardinalityInOWL2DLProfile() {
        declare(o, OP, CL);
        o.add(TransitiveObjectProperty(OP), SubClassOf(CL, ObjectMaxCardinality(1, OP, OWLThing())));
        Class[] expectedViolations = {UseOfNonSimplePropertyInCardinalityRestriction.class};
        runAssert(o, Profiles.OWL2_DL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLObjectExactCardinality desc)")
    public void testShouldCreateViolationForOWLObjectExactCardinalityInOWL2DLProfile() {
        declare(o, OP, CL);
        o.add(TransitiveObjectProperty(OP), SubClassOf(CL, ObjectExactCardinality(1, OP, OWLThing())));
        Class[] expectedViolations = {UseOfNonSimplePropertyInCardinalityRestriction.class};
        runAssert(o, Profiles.OWL2_DL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLObjectHasSelf desc)")
    public void testShouldCreateViolationForOWLObjectHasSelfInOWL2DLProfile() {
        declare(o, OP);
        o.add(TransitiveObjectProperty(OP), ObjectPropertyRange(OP, ObjectHasSelf(OP)));
        Class[] expectedViolations = {UseOfNonSimplePropertyInObjectHasSelf.class};
        runAssert(o, Profiles.OWL2_DL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLFunctionalObjectPropertyAxiom axiom)")
    public void testShouldCreateViolationForOWLFunctionalObjectPropertyAxiomInOWL2DLProfile() {
        declare(o, OP);
        o.add(TransitiveObjectProperty(OP), FunctionalObjectProperty(OP));
        Class[] expectedViolations = {UseOfNonSimplePropertyInFunctionalPropertyAxiom.class};
        runAssert(o, Profiles.OWL2_DL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLInverseFunctionalObjectPropertyAxiom axiom)")
    public void testShouldCreateViolationForOWLInverseFunctionalObjectPropertyAxiomInOWL2DLProfile() {
        declare(o, OP);
        o.add(TransitiveObjectProperty(OP), InverseFunctionalObjectProperty(OP));
        Class[] expectedViolations = {UseOfNonSimplePropertyInInverseFunctionalObjectPropertyAxiom.class};
        runAssert(o, Profiles.OWL2_DL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLIrreflexiveObjectPropertyAxiom axiom)")
    public void testShouldCreateViolationForOWLIrreflexiveObjectPropertyAxiomInOWL2DLProfile() {
        declare(o, OP);
        o.add(TransitiveObjectProperty(OP), IrreflexiveObjectProperty(OP));
        Class[] expectedViolations = {UseOfNonSimplePropertyInIrreflexivePropertyAxiom.class};
        runAssert(o, Profiles.OWL2_DL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLAsymmetricObjectPropertyAxiom axiom)")
    public void testShouldCreateViolationForOWLAsymmetricObjectPropertyAxiomInOWL2DLProfile() {
        declare(o, OP);
        o.add(TransitiveObjectProperty(OP), AsymmetricObjectProperty(OP));
        Class[] expectedViolations = {UseOfNonSimplePropertyInAsymmetricObjectPropertyAxiom.class};
        runAssert(o, Profiles.OWL2_DL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDisjointObjectPropertiesAxiom axiom)")
    public void testShouldCreateViolationForOWLDisjointObjectPropertiesAxiomInOWL2DLProfile() {
        declare(o, OP);
        o.add(TransitiveObjectProperty(OP), DisjointObjectProperties(OP));
        Class[] expectedViolations = {InsufficientPropertyExpressions.class,
                UseOfNonSimplePropertyInDisjointPropertiesAxiom.class};
        runAssert(o, Profiles.OWL2_DL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLSubPropertyChainOfAxiom axiom)")
    public void testShouldCreateViolationForOWLSubPropertyChainOfAxiomInOWL2DLProfile() {
        OWLObjectProperty op1 = ObjectProperty(IRI("urn:test#", "op"));
        declare(o, OP, op1);
        o.add(SubPropertyChainOf(Collections.singletonList(op1), OP), SubPropertyChainOf(Arrays.asList(OP, op1, OP), OP),
                SubPropertyChainOf(Arrays.asList(OP, op1), OP), SubPropertyChainOf(Arrays.asList(op1, OP, op1, OP), OP));
        Class[] expectedViolations = {InsufficientPropertyExpressions.class, UseOfPropertyInChainCausesCycle.class,
                UseOfPropertyInChainCausesCycle.class, UseOfPropertyInChainCausesCycle.class};
        runAssert(o, Profiles.OWL2_DL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLOntology ont)")
    public void testShouldCreateViolationForOWLOntologyInOWL2Profile() throws OWLOntologyCreationException {
        o = m.createOntology(new OWLOntologyID(Optional.of(IRI("test", "")), Optional.of(IRI("test1", ""))));
        Class[] expectedViolations = {OntologyIRINotAbsolute.class, OntologyVersionIRINotAbsolute.class};
        runAssert(o, Profiles.OWL2_FULL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(IRI iri)")
    public void testShouldCreateViolationForIRIInOWL2Profile() {
        declare(o, Class(IRI("test", "")));
        Class[] expectedViolations = {UseOfNonAbsoluteIRI.class};
        runAssert(o, Profiles.OWL2_FULL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLLiteral node)")
    public void testShouldCreateViolationForOWLLiteralInOWL2Profile() {
        declare(o, DATAP);
        o.add(DataPropertyAssertion(DATAP, AnonymousIndividual(), Literal("wrong", OWL2Datatype.XSD_INTEGER)));
        Class[] expectedViolations = {LexicalNotInLexicalSpace.class};
        runAssert(o, Profiles.OWL2_FULL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDatatypeRestriction node)")
    public void testShouldCreateViolationForOWLDatatypeRestrictionInOWL2Profile() {
        declare(o, DATAP);
        o.add(DatatypeDefinition(Integer(), Boolean()),
                DatatypeDefinition(df.getOWLDatatype("urn:test:undeclaredDatatype"), Boolean()),
                DATA_PROPERTY_RANGE2);
        Class[] expectedViolations = {UseOfDefinedDatatypeInDatatypeRestriction.class,
                UseOfIllegalFacetRestriction.class, UseOfUndeclaredDatatype.class};
        runAssert(o, Profiles.OWL2_FULL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDatatypeDefinitionAxiom axiom)")
    public void testShouldCreateViolationForOWLDatatypeDefinitionAxiomInOWL2Profile() {
        o.add(DatatypeDefinition(FAKEDATATYPE, Boolean()));
        Class[] expectedViolations = {UseOfUndeclaredDatatype.class};
        runAssert(o, Profiles.OWL2_FULL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDatatype node)")
    public void testShouldCreateViolationForOWLDatatypeInOWL2ELProfile() {
        declare(o, Boolean());
        runAssert(o, Profiles.OWL2_EL, UseOfIllegalDataRange.class);
    }

    @Test
    @Tests(method = "public Object visit(OWLAnonymousIndividual individual)")
    public void testShouldCreateViolationForOWLAnonymousIndividualInOWL2ELProfile() {
        o.add(ClassAssertion(OWLThing(), DF.getOWLAnonymousIndividual()));
        Class[] expectedViolations = {UseOfAnonymousIndividual.class};
        runAssert(o, Profiles.OWL2_EL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLObjectInverseOf property)")
    public void testShouldCreateViolationForOWLObjectInverseOfInOWL2ELProfile() {
        declare(o, OP);
        o.add(SubObjectPropertyOf(OP, ObjectInverseOf(OP)));
        Class[] expectedViolations = {UseOfObjectPropertyInverse.class};
        runAssert(o, Profiles.OWL2_EL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDataAllValuesFrom desc)")
    public void testShouldCreateViolationForOWLDataAllValuesFromInOWL2ELProfile() {
        declare(o, DATAP, CL);
        o.add(SubClassOf(CL, DataAllValuesFrom(DATAP, Integer())));
        Class[] expectedViolations = {UseOfIllegalClassExpression.class};
        runAssert(o, Profiles.OWL2_EL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDataExactCardinality desc)")
    public void testShouldCreateViolationForOWLDataExactCardinalityInOWL2ELProfile() {
        declare(o, DATAP, CL, Integer());
        o.add(SubClassOf(CL, DataExactCardinality(1, DATAP, Integer())));
        Class[] expectedViolations = {UseOfIllegalClassExpression.class};
        runAssert(o, Profiles.OWL2_EL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDataMaxCardinality desc)")
    public void testShouldCreateViolationForOWLDataMaxCardinalityInOWL2ELProfile() {
        declare(o, DATAP, CL, Integer());
        o.add(SubClassOf(CL, DataMaxCardinality(1, DATAP, Integer())));
        Class[] expectedViolations = {UseOfIllegalClassExpression.class};
        runAssert(o, Profiles.OWL2_EL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDataMinCardinality desc)")
    public void testShouldCreateViolationForOWLDataMinCardinalityInOWL2ELProfile() {
        declare(o, DATAP, CL, Integer());
        o.add(SubClassOf(CL, DataMinCardinality(1, DATAP, Integer())));
        Class[] expectedViolations = {UseOfIllegalClassExpression.class};
        runAssert(o, Profiles.OWL2_EL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLObjectAllValuesFrom desc)")
    public void testShouldCreateViolationForOWLObjectAllValuesFromInOWL2ELProfile() {
        declare(o, OP, CL);
        o.add(SubClassOf(CL, ObjectAllValuesFrom(OP, OWLThing())));
        Class[] expectedViolations = {UseOfIllegalClassExpression.class};
        runAssert(o, Profiles.OWL2_EL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLObjectComplementOf desc)")
    public void testShouldCreateViolationForOWLObjectComplementOfInOWL2ELProfile() {
        declare(o, OP);
        o.add(ObjectPropertyRange(OP, ObjectComplementOf(OWLNothing())));
        Class[] expectedViolations = {UseOfIllegalClassExpression.class};
        runAssert(o, Profiles.OWL2_EL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLObjectExactCardinality desc)")
    public void testShouldCreateViolationForOWLObjectExactCardinalityInOWL2ELProfile() {
        declare(o, OP, CL);
        o.add(SubClassOf(CL, ObjectExactCardinality(1, OP, OWLThing())));
        Class[] expectedViolations = {UseOfIllegalClassExpression.class};
        runAssert(o, Profiles.OWL2_EL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLObjectMaxCardinality desc)")
    public void testShouldCreateViolationForOWLObjectMaxCardinalityInOWL2ELProfile() {
        declare(o, OP, CL);
        o.add(SubClassOf(CL, ObjectMaxCardinality(1, OP, OWLThing())));
        Class[] expectedViolations = {UseOfIllegalClassExpression.class};
        runAssert(o, Profiles.OWL2_EL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLObjectMinCardinality desc)")
    public void testShouldCreateViolationForOWLObjectMinCardinalityInOWL2ELProfile() {
        declare(o, OP, CL);
        o.add(SubClassOf(CL, ObjectMinCardinality(1, OP, OWLThing())));
        Class[] expectedViolations = {UseOfIllegalClassExpression.class};
        runAssert(o, Profiles.OWL2_EL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLObjectOneOf desc)")
    public void testShouldCreateViolationForOWLObjectOneOfInOWL2ELProfile() {
        declare(o, OP);
        o.add(ObjectPropertyRange(OP, ObjectOneOf(NamedIndividual(IRI("urn:test#", "i1")), NamedIndividual(IRI(
                "urn:test#", "i2")))));
        Class[] expectedViolations = {UseOfObjectOneOfWithMultipleIndividuals.class};
        runAssert(o, Profiles.OWL2_EL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLObjectUnionOf desc)")
    public void testShouldCreateViolationForOWLObjectUnionOfInOWL2ELProfile() {
        declare(o, OP);
        o.add(ObjectPropertyRange(OP, ObjectUnionOf(OWLThing(), OWLNothing())));
        Class[] expectedViolations = {UseOfIllegalClassExpression.class};
        runAssert(o, Profiles.OWL2_EL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDataComplementOf node)")
    public void testShouldCreateViolationForOWLDataComplementOfInOWL2ELProfile() {
        declare(o, DATAP);
        o.add(DataPropertyRange(DATAP, DataComplementOf(Double())));
        Class[] expectedViolations = {UseOfIllegalDataRange.class, UseOfIllegalDataRange.class};
        runAssert(o, Profiles.OWL2_EL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDataOneOf node)")
    public void testShouldCreateViolationForOWLDataOneOfInOWL2ELProfile() {
        declare(o, DATAP);
        o.add(DataPropertyRange(DATAP, DataOneOf(Literal(1), Literal(2))));
        Class[] expectedViolations = {UseOfDataOneOfWithMultipleLiterals.class};
        runAssert(o, Profiles.OWL2_EL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDatatypeRestriction node)")
    public void testShouldCreateViolationForOWLDatatypeRestrictionInOWL2ELProfile() {
        declare(o, DATAP);
        o.add(DATA_PROPERTY_RANGE);
        Class[] expectedViolations = {UseOfIllegalDataRange.class};
        runAssert(o, Profiles.OWL2_EL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDataUnionOf node)")
    public void testShouldCreateViolationForOWLDataUnionOfInOWL2ELProfile() {
        declare(o, DATAP);
        o.add(DataPropertyRange(DATAP, DataUnionOf(Double(), Integer())));
        Class[] expectedViolations = {UseOfIllegalDataRange.class, UseOfIllegalDataRange.class};
        runAssert(o, Profiles.OWL2_EL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLAsymmetricObjectPropertyAxiom axiom)")
    public void testShouldCreateViolationForOWLAsymmetricObjectPropertyAxiomInOWL2ELProfile() {
        declare(o, OP);
        o.add(AsymmetricObjectProperty(OP));
        Class[] expectedViolations = {UseOfIllegalAxiom.class};
        runAssert(o, Profiles.OWL2_EL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDisjointDataPropertiesAxiom axiom)")
    public void testShouldCreateViolationForOWLDisjointDataPropertiesAxiomInOWL2ELProfile() {
        OWLDataProperty dp = DataProperty(IRI("urn:test#", "other"));
        declare(o, DATAP, dp);
        o.add(DisjointDataProperties(DATAP, dp));
        Class[] expectedViolations = {UseOfIllegalAxiom.class};
        runAssert(o, Profiles.OWL2_EL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDisjointObjectPropertiesAxiom axiom)")
    public void testShouldCreateViolationForOWLDisjointObjectPropertiesAxiomInOWL2ELProfile() {
        OWLObjectProperty op1 = ObjectProperty(IRI("urn:test#", "test"));
        declare(o, OP, op1);
        o.add(DisjointObjectProperties(op1, OP));
        Class[] expectedViolations = {UseOfIllegalAxiom.class};
        runAssert(o, Profiles.OWL2_EL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDisjointUnionAxiom axiom)")
    public void testShouldCreateViolationForOWLDisjointUnionAxiomInOWL2ELProfile() {
        declare(o, CL);
        o.add(DisjointUnion(CL, OWLThing(), OWLNothing()));
        Class[] expectedViolations = {UseOfIllegalAxiom.class};
        runAssert(o, Profiles.OWL2_EL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLFunctionalObjectPropertyAxiom axiom)")
    public void testShouldCreateViolationForOWLFunctionalObjectPropertyAxiomInOWL2ELProfile() {
        declare(o, OP);
        o.add(FunctionalObjectProperty(OP));
        Class[] expectedViolations = {UseOfIllegalAxiom.class};
        runAssert(o, Profiles.OWL2_EL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLHasKeyAxiom axiom)")
    public void testShouldCreateViolationForOWLHasKeyAxiomInOWL2ELProfile() {
        declare(o, CL, OP);
        o.add(HasKey(CL, OP));
        Class[] expectedViolations = {};
        runAssert(o, Profiles.OWL2_EL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLInverseFunctionalObjectPropertyAxiom axiom)")
    public void testShouldCreateViolationForOWLInverseFunctionalObjectPropertyAxiomInOWL2ELProfile() {
        declare(o, P);
        o.add(InverseFunctionalObjectProperty(P));
        Class[] expectedViolations = {UseOfIllegalAxiom.class};
        runAssert(o, Profiles.OWL2_EL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLInverseObjectPropertiesAxiom axiom)")
    public void testShouldCreateViolationForOWLInverseObjectPropertiesAxiomInOWL2ELProfile() {
        declare(o, P);
        OWLObjectProperty p1 = ObjectProperty(IRI("urn:test#", "objectproperty"));
        declare(o, p1);
        o.add(InverseObjectProperties(P, p1));
        Class[] expectedViolations = {UseOfIllegalAxiom.class};
        runAssert(o, Profiles.OWL2_EL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLIrreflexiveObjectPropertyAxiom axiom)")
    public void testShouldCreateViolationForOWLIrreflexiveObjectPropertyAxiomInOWL2ELProfile() {
        declare(o, P);
        o.add(IrreflexiveObjectProperty(P));
        Class[] expectedViolations = {UseOfIllegalAxiom.class};
        runAssert(o, Profiles.OWL2_EL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLSymmetricObjectPropertyAxiom axiom)")
    public void testShouldCreateViolationForOWLSymmetricObjectPropertyAxiomInOWL2ELProfile() {
        declare(o, P);
        o.add(SymmetricObjectProperty(P));
        Class[] expectedViolations = {UseOfIllegalAxiom.class};
        runAssert(o, Profiles.OWL2_EL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(SWRLRule rule)")
    public void testShouldCreateViolationForSWRLRuleInOWL2ELProfile() {
        o.add(DF.getSWRLRule(new HashSet<>(), new HashSet<>()));
        Class[] expectedViolations = {UseOfIllegalAxiom.class};
        runAssert(o, Profiles.OWL2_EL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLSubPropertyChainOfAxiom axiom)")
    public void testShouldCreateViolationForOWLSubPropertyChainOfAxiomInOWL2ELProfile() {
        OWLObjectProperty op1 = ObjectProperty(IRI("urn:test#", "op1"));
        OWLObjectProperty op2 = ObjectProperty(IRI("urn:test#", "op"));
        declare(o, op1, OP, op2, CL);
        o.add(ObjectPropertyRange(OP, CL));
        List<OWLObjectProperty> asList = Arrays.asList(op2, op1);
        o.add(SubPropertyChainOf(asList, OP));
        Class[] expectedViolations = {LastPropertyInChainNotInImposedRange.class};
        runAssert(o, Profiles.OWL2_EL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDatatype node)")
    public void testShouldCreateViolationForOWLDatatypeInOWL2QLProfile() {
        declare(o, FAKEDATATYPE);
        runAssert(o, Profiles.OWL2_QL, UseOfIllegalDataRange.class);
    }

    @Test
    @Tests(method = "public Object visit(OWLAnonymousIndividual individual)")
    public void testShouldCreateViolationForOWLAnonymousIndividualInOWL2QLProfile() {
        o.add(ClassAssertion(OWLThing(), DF.getOWLAnonymousIndividual()));
        Class[] expectedViolations = {UseOfAnonymousIndividual.class};
        runAssert(o, Profiles.OWL2_QL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLHasKeyAxiom axiom)")
    public void testShouldCreateViolationForOWLHasKeyAxiomInOWL2QLProfile() {
        declare(o, CL, OP);
        o.add(HasKey(CL, OP));
        Class[] expectedViolations = {UseOfIllegalAxiom.class};
        runAssert(o, Profiles.OWL2_QL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLSubClassOfAxiom axiom)")
    public void testShouldCreateViolationForOWLSubClassOfAxiomInOWL2QLProfile() {
        declare(o, OP);
        o.add(SubClassOf(ObjectComplementOf(OWLNothing()), ObjectUnionOf(OWLThing(), OWLNothing())));
        Class[] expectedViolations = {UseOfNonSubClassExpression.class, UseOfNonSuperClassExpression.class};
        runAssert(o, Profiles.OWL2_QL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLEquivalentClassesAxiom axiom)")
    public void testShouldCreateViolationForOWLEquivalentClassesAxiomInOWL2QLProfile() {
        o.add(EquivalentClasses(ObjectUnionOf(OWLNothing(), OWLThing()), OWLNothing()));
        Class[] expectedViolations = {UseOfNonSubClassExpression.class};
        runAssert(o, Profiles.OWL2_QL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDisjointClassesAxiom axiom)")
    public void testShouldCreateViolationForOWLDisjointClassesAxiomInOWL2QLProfile() {
        o.add(DisjointClasses(ObjectComplementOf(OWLThing()), OWLThing()));
        Class[] expectedViolations = {UseOfNonSubClassExpression.class};
        runAssert(o, Profiles.OWL2_QL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLObjectPropertyDomainAxiom axiom)")
    public void testShouldCreateViolationForOWLObjectPropertyDomainAxiomInOWL2QLProfile() {
        declare(o, OP);
        o.add(ObjectPropertyDomain(OP, ObjectUnionOf(OWLNothing(), OWLThing())));
        Class[] expectedViolations = {UseOfNonSuperClassExpression.class};
        runAssert(o, Profiles.OWL2_QL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLObjectPropertyRangeAxiom axiom)")
    public void testShouldCreateViolationForOWLObjectPropertyRangeAxiomInOWL2QLProfile() {
        declare(o, OP);
        o.add(ObjectPropertyRange(OP, ObjectUnionOf(OWLNothing(), OWLThing())));
        Class[] expectedViolations = {UseOfNonSuperClassExpression.class};
        runAssert(o, Profiles.OWL2_QL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLSubPropertyChainOfAxiom axiom)")
    public void testShouldCreateViolationForOWLSubPropertyChainOfAxiomInOWL2QLProfile() {
        OWLObjectProperty op1 = ObjectProperty(IRI("urn:test#", "op"));
        declare(o, OP, op1);
        o.add(SubPropertyChainOf(Arrays.asList(OP, op1), OP));
        Class[] expectedViolations = {UseOfIllegalAxiom.class};
        runAssert(o, Profiles.OWL2_QL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLFunctionalObjectPropertyAxiom axiom)")
    public void testShouldCreateViolationForOWLFunctionalObjectPropertyAxiomInOWL2QLProfile() {
        declare(o, OP);
        o.add(FunctionalObjectProperty(OP));
        Class[] expectedViolations = {UseOfIllegalAxiom.class};
        runAssert(o, Profiles.OWL2_QL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLInverseFunctionalObjectPropertyAxiom axiom)")
    public void testShouldCreateViolationForOWLInverseFunctionalObjectPropertyAxiomInOWL2QLProfile() {
        declare(o, OP);
        o.add(InverseFunctionalObjectProperty(OP));
        Class[] expectedViolations = {UseOfIllegalAxiom.class};
        runAssert(o, Profiles.OWL2_QL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLTransitiveObjectPropertyAxiom axiom)")
    public void testShouldCreateViolationForOWLTransitiveObjectPropertyAxiomInOWL2QLProfile() {
        declare(o, OP);
        o.add(TransitiveObjectProperty(OP));
        Class[] expectedViolations = {UseOfIllegalAxiom.class};
        runAssert(o, Profiles.OWL2_QL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLFunctionalDataPropertyAxiom axiom)")
    public void testShouldCreateViolationForOWLFunctionalDataPropertyAxiomInOWL2QLProfile() {
        declare(o, DATAP);
        o.add(FunctionalDataProperty(DATAP));
        Class[] expectedViolations = {UseOfIllegalAxiom.class};
        runAssert(o, Profiles.OWL2_QL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDataPropertyDomainAxiom axiom)")
    public void testShouldCreateViolationForOWLDataPropertyDomainAxiomInOWL2QLProfile() {
        declare(o, DATAP, OP);
        o.add(DataPropertyDomain(DATAP, ObjectMaxCardinality(1, OP, OWLNothing())));
        Class[] expectedViolations = {UseOfNonSuperClassExpression.class};
        runAssert(o, Profiles.OWL2_QL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLClassAssertionAxiom axiom)")
    public void testShouldCreateViolationForOWLClassAssertionAxiomInOWL2QLProfile() {
        OWLNamedIndividual i = NamedIndividual(IRI("urn:test#", "i"));
        declare(o, OP, i);
        o.add(ClassAssertion(ObjectSomeValuesFrom(OP, OWLThing()), i));
        Class[] expectedViolations = {UseOfNonAtomicClassExpression.class};
        runAssert(o, Profiles.OWL2_QL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLSameIndividualAxiom axiom)")
    public void testShouldCreateViolationForOWLSameIndividualAxiomInOWL2QLProfile() {
        o.add(SameIndividual(NamedIndividual(IRI("urn:test#", "individual1")), NamedIndividual(IRI("urn:test#",
                "individual2"))));
        Class[] expectedViolations = {UseOfIllegalAxiom.class};
        runAssert(o, Profiles.OWL2_QL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLNegativeObjectPropertyAssertionAxiom axiom)")
    public void testShouldCreateViolationForOWLNegativeObjectPropertyAssertionAxiomInOWL2QLProfile() {
        declare(o, OP);
        OWLNamedIndividual i = NamedIndividual(IRI("urn:test#", "i"));
        OWLNamedIndividual i1 = NamedIndividual(IRI("urn:test#", "i"));
        declare(o, i, i1);
        o.add(NegativeObjectPropertyAssertion(OP, i, i1));
        Class[] expectedViolations = {UseOfIllegalAxiom.class};
        runAssert(o, Profiles.OWL2_QL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLNegativeDataPropertyAssertionAxiom axiom)")
    public void testShouldCreateViolationForOWLNegativeDataPropertyAssertionAxiomInOWL2QLProfile() {
        declare(o, DATAP);
        OWLNamedIndividual i = NamedIndividual(IRI("urn:test#", "i"));
        declare(o, i);
        o.add(NegativeDataPropertyAssertion(DATAP, i, Literal(1)));
        Class[] expectedViolations = {UseOfIllegalAxiom.class};
        runAssert(o, Profiles.OWL2_QL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDisjointUnionAxiom axiom)")
    public void testShouldCreateViolationForOWLDisjointUnionAxiomInOWL2QLProfile() {
        declare(o, CL);
        o.add(DisjointUnion(CL, OWLThing(), OWLNothing()));
        Class[] expectedViolations = {UseOfIllegalAxiom.class};
        runAssert(o, Profiles.OWL2_QL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLIrreflexiveObjectPropertyAxiom axiom)")
    public void testShouldCreateViolationForOWLIrreflexiveObjectPropertyAxiomInOWL2QLProfile() {
        declare(o, OP);
        o.add(IrreflexiveObjectProperty(OP));
        Class[] expectedViolations = {};
        runAssert(o, Profiles.OWL2_QL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(SWRLRule rule)")
    public void testShouldCreateViolationForSWRLRuleInOWL2QLProfile() {
        o.add(DF.getSWRLRule(new HashSet<>(), new HashSet<>()));
        Class[] expectedViolations = {UseOfIllegalAxiom.class};
        runAssert(o, Profiles.OWL2_QL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDataComplementOf node)")
    public void testShouldCreateViolationForOWLDataComplementOfInOWL2QLProfile() {
        declare(o, DATAP);
        o.add(DataPropertyRange(DATAP, DataComplementOf(Integer())));
        Class[] expectedViolations = {UseOfIllegalDataRange.class};
        runAssert(o, Profiles.OWL2_QL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDataOneOf node)")
    public void testShouldCreateViolationForOWLDataOneOfInOWL2QLProfile() {
        declare(o, DATAP);
        o.add(DataPropertyRange(DATAP, DataOneOf(Literal(1), Literal(2))));
        Class[] expectedViolations = {UseOfIllegalDataRange.class};
        runAssert(o, Profiles.OWL2_QL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDatatypeRestriction node)")
    public void testShouldCreateViolationForOWLDatatypeRestrictionInOWL2QLProfile() {
        declare(o, DATAP);
        o.add(DATA_PROPERTY_RANGE);
        Class[] expectedViolations = {UseOfIllegalDataRange.class};
        runAssert(o, Profiles.OWL2_QL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDataUnionOf node)")
    public void testShouldCreateViolationForOWLDataUnionOfInOWL2QLProfile() {
        declare(o, DATAP);
        o.add(DataPropertyRange(DATAP, DataUnionOf(Integer(), Boolean())));
        Class[] expectedViolations = {UseOfIllegalDataRange.class, UseOfIllegalDataRange.class};
        runAssert(o, Profiles.OWL2_QL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLClassAssertionAxiom axiom)")
    public void testShouldCreateViolationForOWLClassAssertionAxiomInOWL2RLProfile() {
        declare(o, OP);
        o.add(ClassAssertion(ObjectMinCardinality(1, OP, OWLThing()), NamedIndividual(IRI("urn:test#", "i"))));
        Class[] expectedViolations = {UseOfNonSuperClassExpression.class};
        runAssert(o, Profiles.OWL2_RL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDataPropertyDomainAxiom axiom)")
    public void testShouldCreateViolationForOWLDataPropertyDomainAxiomInOWL2RLProfile() {
        declare(o, DATAP, OP);
        o.add(DataPropertyDomain(DATAP, ObjectMinCardinality(1, OP, OWLThing())));
        Class[] expectedViolations = {UseOfNonSuperClassExpression.class};
        runAssert(o, Profiles.OWL2_RL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDisjointClassesAxiom axiom)")
    public void testShouldCreateViolationForOWLDisjointClassesAxiomInOWL2RLProfile() {
        o.add(DisjointClasses(ObjectComplementOf(OWLThing()), OWLThing()));
        Class[] expectedViolations = {UseOfNonSubClassExpression.class, UseOfNonSubClassExpression.class};
        runAssert(o, Profiles.OWL2_RL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDisjointDataPropertiesAxiom axiom)")
    public void testShouldCreateViolationForOWLDisjointDataPropertiesAxiomInOWL2RLProfile() {
        OWLDataProperty dp = DataProperty(IRI("urn:test#", "dproperty"));
        declare(o, DATAP, dp);
        o.add(DisjointDataProperties(DATAP, dp));
        Class[] expectedViolations = {};
        runAssert(o, Profiles.OWL2_RL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDisjointUnionAxiom axiom)")
    public void testShouldCreateViolationForOWLDisjointUnionAxiomInOWL2RLProfile() {
        declare(o, CL);
        o.add(DisjointUnion(CL, OWLThing(), OWLNothing()));
        Class[] expectedViolations = {UseOfIllegalAxiom.class};
        runAssert(o, Profiles.OWL2_RL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLEquivalentClassesAxiom axiom)")
    public void testShouldCreateViolationForOWLEquivalentClassesAxiomInOWL2RLProfile() {
        o.add(EquivalentClasses(ObjectComplementOf(OWLThing()), OWLNothing()));
        Class[] expectedViolations = {UseOfNonEquivalentClassExpression.class};
        runAssert(o, Profiles.OWL2_RL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLEquivalentDataPropertiesAxiom axiom)")
    public void testShouldCreateViolationForOWLEquivalentDataPropertiesAxiomInOWL2RLProfile() {
        OWLDataProperty dp = DataProperty(IRI("urn:test#", "test"));
        declare(o, DATAP, dp);
        o.add(EquivalentDataProperties(DATAP, dp));
        Class[] expectedViolations = {};
        runAssert(o, Profiles.OWL2_RL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLFunctionalDataPropertyAxiom axiom)")
    public void testShouldCreateViolationForOWLFunctionalDataPropertyAxiomInOWL2RLProfile() {
        declare(o, DATAP);
        o.add(FunctionalDataProperty(DATAP));
        Class[] expectedViolations = {};
        runAssert(o, Profiles.OWL2_RL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLHasKeyAxiom axiom)")
    public void testShouldCreateViolationForOWLHasKeyAxiomInOWL2RLProfile() {
        declare(o, CL, OP);
        o.add(HasKey(ObjectComplementOf(CL), OP));
        Class[] expectedViolations = {UseOfNonSubClassExpression.class};
        runAssert(o, Profiles.OWL2_RL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLObjectPropertyDomainAxiom axiom)")
    public void testShouldCreateViolationForOWLObjectPropertyDomainAxiomInOWL2RLProfile() {
        declare(o, OP, OP);
        o.add(ObjectPropertyDomain(OP, ObjectMinCardinality(1, OP, OWLThing())));
        Class[] expectedViolations = {UseOfNonSuperClassExpression.class};
        runAssert(o, Profiles.OWL2_RL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLObjectPropertyRangeAxiom axiom)")
    public void testShouldCreateViolationForOWLObjectPropertyRangeAxiomInOWL2RLProfile() {
        declare(o, OP);
        o.add(ObjectPropertyRange(OP, ObjectMinCardinality(1, OP, OWLThing())));
        Class[] expectedViolations = {UseOfNonSuperClassExpression.class};
        runAssert(o, Profiles.OWL2_RL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLSubClassOfAxiom axiom)")
    public void testShouldCreateViolationForOWLSubClassOfAxiomInOWL2RLProfile() {
        o.add(SubClassOf(ObjectComplementOf(OWLThing()), ObjectOneOf(NamedIndividual(IRI("urn:test#", "test")))));
        Class[] expectedViolations = {UseOfNonSubClassExpression.class, UseOfNonSuperClassExpression.class};
        runAssert(o, Profiles.OWL2_RL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(SWRLRule rule)")
    public void testShouldCreateViolationForSWRLRuleInOWL2RLProfile() {
        o.add(DF.getSWRLRule(new HashSet<>(), new HashSet<>()));
        Class[] expectedViolations = {UseOfIllegalAxiom.class};
        runAssert(o, Profiles.OWL2_RL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDataComplementOf node)")
    public void testShouldCreateViolationForOWLDataComplementOfInOWL2RLProfile() {
        declare(o, DATAP);
        o.add(DataPropertyRange(DATAP, DataComplementOf(Integer())));
        Class[] expectedViolations = {UseOfIllegalDataRange.class};
        runAssert(o, Profiles.OWL2_RL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDataIntersectionOf node)")
    public void testShouldCreateViolationForOWLDataIntersectionOfInOWL2RLProfile() {
        declare(o, DATAP);
        o.add(DataPropertyRange(DATAP, DataIntersectionOf(Integer(), Boolean())));
        Class[] expectedViolations = {};
        runAssert(o, Profiles.OWL2_RL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDataOneOf node)")
    public void testShouldCreateViolationForOWLDataOneOfInOWL2RLProfile() {
        declare(o, DATAP);
        o.add(DataPropertyRange(DATAP, DataOneOf(Literal(1), Literal(2))));
        Class[] expectedViolations = {UseOfIllegalDataRange.class};
        runAssert(o, Profiles.OWL2_RL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDatatype node)")
    public void testShouldCreateViolationForOWLDatatypeInOWL2RLProfile() {
        declare(o, Datatype(IRI("urn:test#", "test")));
        runAssert(o, Profiles.OWL2_RL, UseOfIllegalDataRange.class);
    }

    @Test
    @Tests(method = "public Object visit(OWLDatatypeRestriction node)")
    public void testShouldCreateViolationForOWLDatatypeRestrictionInOWL2RLProfile() {
        declare(o, DATAP);
        o.add(DATA_PROPERTY_RANGE);
        Class[] expectedViolations = {UseOfIllegalDataRange.class};
        runAssert(o, Profiles.OWL2_RL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDataUnionOf node)")
    public void testShouldCreateViolationForOWLDataUnionOfInOWL2RLProfile() {
        declare(o, DATAP);
        o.add(DataPropertyRange(DATAP, DataUnionOf(Double(), Integer())));
        Class[] expectedViolations = {UseOfIllegalDataRange.class};
        runAssert(o, Profiles.OWL2_RL, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDatatypeDefinitionAxiom axiom)")
    public void testShouldCreateViolationForOWLDatatypeDefinitionAxiomInOWL2RLProfile() {
        OWLDatatype datatype = Datatype(IRI("urn:test#", "datatype"));
        declare(o, datatype);
        o.add(DatatypeDefinition(datatype, Boolean()));
        runAssert(o, Profiles.OWL2_RL, UseOfIllegalAxiom.class, UseOfIllegalDataRange.class,
                UseOfIllegalDataRange.class);
    }
}
