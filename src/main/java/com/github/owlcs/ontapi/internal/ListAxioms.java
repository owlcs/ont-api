/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2020, The University of Manchester, owl.cs group.
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

import org.semanticweb.owlapi.model.*;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * Created by @ssz on 11.05.2020.
 */
interface ListAxioms {

    /**
     * Lists axioms of the given class-type.
     *
     * @param type {@code Class}
     * @param <A>  subtype of {@link OWLAxiom}
     * @return a {@code Stream} of {@link OWLAxiom}s
     * @see AxiomType
     */
    <A extends OWLAxiom> Stream<A> listOWLAxioms(Class<A> type);

    /**
     * Lists {@link OWLDeclarationAxiom Declaration Axiom}s for the specified {@link OWLEntity entity}.
     * Note: method may return non-cached axioms.
     *
     * @param entity {@link OWLEntity}, not {@code null}
     * @return a {@code Stream} of {@link OWLDeclarationAxiom}s
     */
    default Stream<OWLDeclarationAxiom> listOWLDeclarationAxioms(OWLEntity entity) {
        return listOWLAxioms(OWLDeclarationAxiom.class).filter(a -> Objects.equals(entity, a.getEntity()));
    }

    /**
     * Lists {@link OWLAnnotationAssertionAxiom Annotation Assertion Axiom}s
     * with the given {@link OWLAnnotationSubject subject}.
     * Note: method returns non-cached axioms.
     *
     * @param subject {@link OWLAnnotationSubject}, not {@code null}
     * @return a {@code Stream} of {@link OWLAnnotationAssertionAxiom}s
     */
    default Stream<OWLAnnotationAssertionAxiom> listOWLAnnotationAssertionAxioms(OWLAnnotationSubject subject) {
        return listOWLAxioms(OWLAnnotationAssertionAxiom.class).filter(a -> Objects.equals(subject, a.getSubject()));
    }

    /**
     * Lists {@link OWLSubClassOfAxiom SubClassOf Axiom}s by the given sub {@link OWLClass class}.
     *
     * @param subject {@link OWLClass}, not {@code null}
     * @return a {@code Stream} of {@link OWLSubClassOfAxiom}s
     */
    default Stream<OWLSubClassOfAxiom> listOWLSubClassOfAxiomsBySubject(OWLClass subject) {
        return listOWLAxioms(OWLSubClassOfAxiom.class).filter(a -> Objects.equals(subject, a.getSubClass()));
    }

    /**
     * Lists {@link OWLSubClassOfAxiom SubClassOf Axiom}s by the given super {@link OWLClass class}.
     *
     * @param object {@link OWLClass}, not {@code null}
     * @return {@code Stream} of {@link OWLSubClassOfAxiom}s
     */
    default Stream<OWLSubClassOfAxiom> listOWLSubClassOfAxiomsByObject(OWLClass object) {
        return listOWLAxioms(OWLSubClassOfAxiom.class).filter(a -> Objects.equals(object, a.getSuperClass()));
    }

    /**
     * Lists {@link OWLEquivalentClassesAxiom EquivalentClasses Axiom}s by the given {@link OWLClass class}-component.
     *
     * @param clazz {@link OWLClass}, not {@code null}
     * @return a {@code Stream} of {@link OWLEquivalentClassesAxiom}s
     */
    default Stream<OWLEquivalentClassesAxiom> listOWLEquivalentClassesAxioms(OWLClass clazz) {
        return listOWLNaryAxiomAxiomsByOperand(OWLEquivalentClassesAxiom.class, clazz);
    }

    /**
     * Lists {@link OWLDisjointClassesAxiom DisjointClasses Axiom}s by the given {@link OWLClass class}-component.
     *
     * @param clazz {@link OWLClass}, not {@code null}
     * @return a {@code Stream} of {@link OWLDisjointClassesAxiom}s
     */
    default Stream<OWLDisjointClassesAxiom> listOWLDisjointClassesAxioms(OWLClass clazz) {
        return listOWLNaryAxiomAxiomsByOperand(OWLDisjointClassesAxiom.class, clazz);
    }

    default Stream<OWLDisjointUnionAxiom> listOWLDisjointUnionAxioms(OWLClass subject) {
        return listOWLAxioms(OWLDisjointUnionAxiom.class).filter(x -> Objects.equals(subject, x.getOWLClass()));
    }

    default Stream<OWLHasKeyAxiom> listOWLHasKeyAxioms(OWLClass subject) {
        return listOWLAxioms(OWLHasKeyAxiom.class).filter(x -> Objects.equals(subject, x.getClassExpression()));
    }

    default Stream<OWLClassAssertionAxiom> listOWLClassAssertionAxioms(OWLClassExpression object) {
        return listOWLAxioms(OWLClassAssertionAxiom.class).filter(x -> Objects.equals(object, x.getClassExpression()));
    }

    default Stream<OWLDatatypeDefinitionAxiom> listOWLDatatypeDefinitionAxioms(OWLDatatype datatype) {
        return listOWLAxioms(OWLDatatypeDefinitionAxiom.class).filter(x -> Objects.equals(datatype, x.getDatatype()));
    }

    /**
     * Lists {@link OWLClassAssertionAxiom ClassAssertion Axiom}s by the given {@link OWLIndividual individual}-subject.
     *
     * @param subject {@link OWLIndividual}, not {@code null}
     * @return a {@code Stream} of {@link OWLClassAssertionAxiom}s
     */
    default Stream<OWLClassAssertionAxiom> listOWLClassAssertionAxioms(OWLIndividual subject) {
        return listOWLAxioms(OWLClassAssertionAxiom.class).filter(x -> Objects.equals(subject, x.getIndividual()));
    }

    default Stream<OWLSameIndividualAxiom> listOWLSameIndividualAxioms(OWLIndividual operand) {
        return listOWLNaryAxiomAxiomsByOperand(OWLSameIndividualAxiom.class, operand);
    }

    default Stream<OWLDifferentIndividualsAxiom> listOWLDifferentIndividualsAxioms(OWLIndividual operand) {
        return listOWLNaryAxiomAxiomsByOperand(OWLDifferentIndividualsAxiom.class, operand);
    }

    /**
     * Lists {@link OWLObjectPropertyAssertionAxiom ObjectPropertyAssertion Axiom}s
     * by the given {@link OWLIndividual individual}-subject.
     *
     * @param subject {@link OWLIndividual}, not {@code null}
     * @return a {@code Stream} of {@link OWLObjectPropertyAssertionAxiom}s
     */
    default Stream<OWLObjectPropertyAssertionAxiom> listOWLObjectPropertyAssertionAxioms(OWLIndividual subject) {
        return listOWLAxioms(OWLObjectPropertyAssertionAxiom.class).filter(x -> Objects.equals(subject, x.getSubject()));
    }

    default Stream<OWLNegativeObjectPropertyAssertionAxiom> listOWLNegativeObjectPropertyAssertionAxioms(OWLIndividual subject) {
        return listOWLAxioms(OWLNegativeObjectPropertyAssertionAxiom.class).filter(x -> Objects.equals(subject, x.getSubject()));
    }

    /**
     * Lists {@link OWLDataPropertyAssertionAxiom DataPropertyAssertion Axiom}s by the given {@link OWLIndividual individual}-subject.
     *
     * @param subject {@link OWLIndividual}, not {@code null}
     * @return a {@code Stream} of {@link OWLDataPropertyAssertionAxiom}s
     */
    default Stream<OWLDataPropertyAssertionAxiom> listOWLDataPropertyAssertionAxioms(OWLIndividual subject) {
        return listOWLAxioms(OWLDataPropertyAssertionAxiom.class).filter(x -> Objects.equals(subject, x.getSubject()));
    }

    default Stream<OWLNegativeDataPropertyAssertionAxiom> listOWLNegativeDataPropertyAssertionAxioms(OWLIndividual subject) {
        return listOWLAxioms(OWLNegativeDataPropertyAssertionAxiom.class).filter(x -> Objects.equals(subject, x.getSubject()));
    }

    default Stream<OWLEquivalentObjectPropertiesAxiom> listOWLEquivalentObjectPropertiesAxioms(OWLObjectPropertyExpression operand) {
        return listOWLNaryAxiomAxiomsByOperand(OWLEquivalentObjectPropertiesAxiom.class, operand);
    }

    default Stream<OWLDisjointObjectPropertiesAxiom> listOWLDisjointObjectPropertiesAxioms(OWLObjectPropertyExpression operand) {
        return listOWLNaryAxiomAxiomsByOperand(OWLDisjointObjectPropertiesAxiom.class, operand);
    }

    default Stream<OWLSubObjectPropertyOfAxiom> listOWLSubObjectPropertyOfAxiomsBySubject(OWLObjectPropertyExpression subject) {
        return listOWLSubPropertyOfAxiomsBySubject(OWLSubObjectPropertyOfAxiom.class, subject);
    }

    default Stream<OWLSubObjectPropertyOfAxiom> listOWLSubObjectPropertyOfAxiomsByObject(OWLObjectPropertyExpression object) {
        return listOWLSubPropertyOfAxiomsByObject(OWLSubObjectPropertyOfAxiom.class, object);
    }

    default Stream<OWLObjectPropertyDomainAxiom> listOWLObjectPropertyDomainAxioms(OWLObjectPropertyExpression subject) {
        return listOWLPropertyAxioms(OWLObjectPropertyDomainAxiom.class, subject);
    }

    default Stream<OWLObjectPropertyRangeAxiom> listOWLObjectPropertyRangeAxioms(OWLObjectPropertyExpression subject) {
        return listOWLPropertyAxioms(OWLObjectPropertyRangeAxiom.class, subject);
    }

    default Stream<OWLInverseObjectPropertiesAxiom> listOWLInverseObjectPropertiesAxioms(OWLObjectPropertyExpression operand) {
        return listOWLNaryAxiomAxiomsByOperand(OWLInverseObjectPropertiesAxiom.class, operand);
    }

    default Stream<OWLFunctionalObjectPropertyAxiom> listOWLFunctionalObjectPropertyAxioms(OWLObjectPropertyExpression subject) {
        return listOWLPropertyAxioms(OWLFunctionalObjectPropertyAxiom.class, subject);
    }

    default Stream<OWLInverseFunctionalObjectPropertyAxiom> listOWLInverseFunctionalObjectPropertyAxioms(OWLObjectPropertyExpression subject) {
        return listOWLPropertyAxioms(OWLInverseFunctionalObjectPropertyAxiom.class, subject);
    }

    default Stream<OWLSymmetricObjectPropertyAxiom> listOWLSymmetricObjectPropertyAxioms(OWLObjectPropertyExpression subject) {
        return listOWLPropertyAxioms(OWLSymmetricObjectPropertyAxiom.class, subject);
    }

    default Stream<OWLAsymmetricObjectPropertyAxiom> listOWLAsymmetricObjectPropertyAxioms(OWLObjectPropertyExpression subject) {
        return listOWLPropertyAxioms(OWLAsymmetricObjectPropertyAxiom.class, subject);
    }

    default Stream<OWLReflexiveObjectPropertyAxiom> listOWLReflexiveObjectPropertyAxioms(OWLObjectPropertyExpression subject) {
        return listOWLPropertyAxioms(OWLReflexiveObjectPropertyAxiom.class, subject);
    }

    default Stream<OWLIrreflexiveObjectPropertyAxiom> listOWLIrreflexiveObjectPropertyAxioms(OWLObjectPropertyExpression subject) {
        return listOWLPropertyAxioms(OWLIrreflexiveObjectPropertyAxiom.class, subject);
    }

    default Stream<OWLTransitiveObjectPropertyAxiom> listOWLTransitiveObjectPropertyAxioms(OWLObjectPropertyExpression subject) {
        return listOWLPropertyAxioms(OWLTransitiveObjectPropertyAxiom.class, subject);
    }

    default Stream<OWLEquivalentDataPropertiesAxiom> listOWLEquivalentDataPropertiesAxioms(OWLDataProperty operand) {
        return listOWLNaryAxiomAxiomsByOperand(OWLEquivalentDataPropertiesAxiom.class, operand);
    }

    default Stream<OWLDisjointDataPropertiesAxiom> listOWLDisjointDataPropertiesAxioms(OWLDataProperty operand) {
        return listOWLNaryAxiomAxiomsByOperand(OWLDisjointDataPropertiesAxiom.class, operand);
    }

    default Stream<OWLSubDataPropertyOfAxiom> listOWLSubDataPropertyOfAxiomsBySubject(OWLDataProperty subject) {
        return listOWLSubPropertyOfAxiomsBySubject(OWLSubDataPropertyOfAxiom.class, subject);
    }

    default Stream<OWLSubDataPropertyOfAxiom> listOWLSubDataPropertyOfAxiomsByObject(OWLDataProperty object) {
        return listOWLSubPropertyOfAxiomsByObject(OWLSubDataPropertyOfAxiom.class, object);
    }

    default Stream<OWLDataPropertyDomainAxiom> listOWLDataPropertyDomainAxioms(OWLDataProperty subject) {
        return listOWLPropertyAxioms(OWLDataPropertyDomainAxiom.class, subject);
    }

    default Stream<OWLDataPropertyRangeAxiom> listOWLDataPropertyRangeAxioms(OWLDataProperty subject) {
        return listOWLPropertyAxioms(OWLDataPropertyRangeAxiom.class, subject);
    }

    default Stream<OWLFunctionalDataPropertyAxiom> listOWLFunctionalDataPropertyAxioms(OWLDataProperty subject) {
        return listOWLPropertyAxioms(OWLFunctionalDataPropertyAxiom.class, subject);
    }

    default Stream<OWLSubAnnotationPropertyOfAxiom> listOWLSubAnnotationPropertyOfAxiomsBySubject(OWLAnnotationProperty subject) {
        return listOWLAxioms(OWLSubAnnotationPropertyOfAxiom.class).filter(x -> Objects.equals(subject, x.getSubProperty()));
    }

    default Stream<OWLSubAnnotationPropertyOfAxiom> listOWLSubAnnotationPropertyOfAxiomsByObject(OWLAnnotationProperty object) {
        return listOWLAxioms(OWLSubAnnotationPropertyOfAxiom.class).filter(x -> Objects.equals(object, x.getSuperProperty()));
    }

    default Stream<OWLAnnotationPropertyDomainAxiom> listOWLAnnotationPropertyDomainAxioms(OWLAnnotationProperty subject) {
        return listOWLPropertyAxioms(OWLAnnotationPropertyDomainAxiom.class, subject);
    }

    default Stream<OWLAnnotationPropertyRangeAxiom> listOWLAnnotationPropertyRangeAxioms(OWLAnnotationProperty subject) {
        return listOWLPropertyAxioms(OWLAnnotationPropertyRangeAxiom.class, subject);
    }

    default <A extends OWLNaryAxiom<? super K>,
            K extends OWLObject> Stream<A> listOWLNaryAxiomAxiomsByOperand(Class<A> type, K operand) {
        return listOWLAxioms(type).filter(a -> a.operands().anyMatch(operand::equals));
    }

    default <A extends OWLAxiom & HasProperty<P>,
            P extends OWLPropertyExpression> Stream<A> listOWLPropertyAxioms(Class<A> type, P property) {
        return listOWLAxioms(type).filter(x -> Objects.equals(property, x.getProperty()));
    }

    default <A extends OWLSubPropertyAxiom<P>,
            P extends OWLPropertyExpression> Stream<A> listOWLSubPropertyOfAxiomsBySubject(Class<A> type, P subject) {
        return listOWLAxioms(type).filter(x -> Objects.equals(subject, x.getSubProperty()));
    }

    default <A extends OWLSubPropertyAxiom<P>,
            P extends OWLPropertyExpression> Stream<A> listOWLSubPropertyOfAxiomsByObject(Class<A> type, P object) {
        return listOWLAxioms(type).filter(x -> Objects.equals(object, x.getSuperProperty()));
    }
}
