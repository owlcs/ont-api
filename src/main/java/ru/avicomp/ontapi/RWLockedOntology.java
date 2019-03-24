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

package ru.avicomp.ontapi;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.AxiomAnnotations;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.model.parameters.Navigation;
import org.semanticweb.owlapi.util.OWLAxiomSearchFilter;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An abstract base {@link OWLMutableOntology mutable ontology} model with {@link ReadWriteLock R/W lock} support.
 * It contains implementations for methods either returning {@code Set} or accepting {@link Imports}.
 * <p>
 * Created by @ssz on 23.03.2019.
 */
@SuppressWarnings({"WeakerAccess", "deprecation"})
@ParametersAreNonnullByDefault
public abstract class RWLockedOntology implements OWLMutableOntology {

    protected final ReadWriteLock lock;

    /**
     * Constructs an {@code RWLockedOntology} instance.
     *
     * @param lock {@link ReadWriteLock} or {@code null}
     */
    public RWLockedOntology(@Nullable ReadWriteLock lock) {
        this.lock = lock == null ? NoOpReadWriteLock.NO_OP_RW_LOCK : lock;
    }

    /**
     * Creates a {@code Set} from the given {@code Stream} preserving the order.
     * It is a {@code Stream}-terminal operation: the input cannot be reused anymore after calling this method.
     *
     * @param s   {@code Stream} of {@link X}, not {@code null}
     * @param <X> anything
     * @return {@code Set} of {@link X}
     */
    private static <X> Set<X> toSet(Stream<X> s) {
        return s.collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Creates a {@code List} from the given {@code Stream}.
     * It is a {@code Stream}-terminal operation.
     *
     * @param s   {@code Stream} of {@link X}, not {@code null}
     * @param <X> anything
     * @return {@code Set} of {@link X}
     */
    private static <X> List<X> toList(Stream<X> s) {
        return s.collect(Collectors.toList());
    }

    /**
     * Gets R/W Lock associated with this ontology instance.
     *
     * @return {@link ReadWriteLock}
     */
    public ReadWriteLock getLock() {
        return lock;
    }

    @Override
    public void setLock(ReadWriteLock lock) {
        throw new OntApiException.Unsupported("Misuse: attempt to change locking.");
    }

    /**
     * Answers {@code true} if the ontology is equipped with a true R/W lock and therefore must be thread-safe.
     *
     * @return boolean
     */
    public boolean isConcurrent() {
        return NoOpReadWriteLock.isConcurrent(lock);
    }

    /**
     * Performs the given operation in the dedicated read-locked section returning its result.
     *
     * @param op  {@link Supplier}
     * @param <X> anything
     * @return {@link X}
     */
    protected <X> X withReadLockToObject(Supplier<X> op) {
        lock.readLock().lock();
        try {
            return op.get();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Performs the given operation in the dedicated read-locked section.
     *
     * @param op  {@link WithThrowable}
     * @param <T> {@link Throwable} subtype
     * @throws T in case something goes wrong while {@code op}
     */
    protected <T extends Throwable> void withReadLock(WithThrowable<T> op) throws T {
        lock.readLock().lock();
        try {
            op.apply();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Performs the given operation in the dedicated write-locked section returning its result.
     *
     * @param op  {@link Supplier}
     * @param <X> anything
     * @return {@link X}
     */
    protected <X> X withWriteLockToObject(Supplier<X> op) {
        lock.writeLock().lock();
        try {
            return op.get();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Performs the given operation in the dedicated write-locked section.
     *
     * @param op  {@link WithThrowable}
     * @param <T> {@link Throwable} subtype
     * @throws T in case something goes wrong while {@code op}
     */
    protected <T extends Throwable> void withWriteLock(WithThrowable<T> op) throws T {
        lock.writeLock().lock();
        try {
            op.apply();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Creates a {@code Set} from the given collection preserving the order.
     * The operation is performed into the dedicated read-locked section.
     *
     * @param sop {@link Supplier} to get {@code Stream} of {@link X}
     * @param <X> anything
     * @return {@code Set} of {@link X}
     */
    protected <X> Set<X> toSet(Supplier<Stream<X>> sop) {
        return withReadLockToObject(() -> toSet(sop.get()));
    }

    /**
     * Performs the given {@code map} operation, that returns {@code Stream},
     * on this ontology and all its {@link #imports() imports} and returns the sum of results.
     * WARNING: although a stream construction happens in the dedicated read-locked section
     * it does not guarantee thread-safety!
     * Thread-safety must be ensured by other internal mechanisms
     * (for example, s stream could be based on immutable snapshot collection).
     *
     * @param imports {@link Imports}
     * @param map     {@link Function}
     * @param <X>     anything
     * @return {@code Stream} of {@link X}
     */
    protected <X> Stream<X> withImportsToStream(Imports imports, Function<OWLOntology, Stream<X>> map) {
        return withReadLockToObject(() -> {
            if (Imports.EXCLUDED.equals(imports)) {
                return map.apply(RWLockedOntology.this);
            }
            return importsClosure().flatMap(map);
        });
    }

    /**
     * Performs the given {@code map} operation, that returns long,
     * on this ontology and all its {@link #imports() imports} and returns the sum of results.
     * The whole process is performed in the dedicated read-locked section.
     *
     * @param imports {@link Imports}
     * @param map     {@link ToLongFunction}
     * @return long
     */
    protected long withImportsToLong(Imports imports, ToLongFunction<OWLOntology> map) {
        return withReadLockToObject(() -> {
            if (Imports.EXCLUDED.equals(imports)) {
                return map.applyAsLong(RWLockedOntology.this);
            }
            return importsClosure().mapToLong(map).sum();
        });
    }

    /**
     * Performs the given {@code test} operation on this ontology and all its {@link #imports() imports}
     * in the dedicated read-locked section.
     *
     * @param imports {@link Imports}
     * @param test    {@link Predicate}
     * @return boolean
     */
    protected boolean withImportsToBoolean(Imports imports, Predicate<OWLOntology> test) {
        return withReadLockToObject(() -> {
            if (Imports.EXCLUDED.equals(imports)) {
                return test.test(RWLockedOntology.this);
            }
            return importsClosure().anyMatch(test);
        });
    }

    @Override
    public Stream<OWLAxiom> axioms(Imports imports) {
        return withImportsToStream(imports, HasAxioms::axioms);
    }

    @Override
    public <R extends OWLAxiom> Stream<R> axioms(AxiomType<R> type, Imports imports) {
        return withImportsToStream(imports, x -> x.axioms(type));
    }

    @Override
    public Stream<OWLLogicalAxiom> logicalAxioms(Imports imports) {
        return withImportsToStream(imports, HasLogicalAxioms::logicalAxioms);
    }

    @Override
    public Stream<OWLAxiom> axiomsIgnoreAnnotations(OWLAxiom axiom, Imports imports) {
        return withImportsToStream(imports, x -> x.axiomsIgnoreAnnotations(axiom));
    }

    @Override
    public Stream<OWLAxiom> referencingAxioms(OWLPrimitive primitive, Imports imports) {
        return withImportsToStream(imports, x -> x.referencingAxioms(primitive));
    }

    @Override
    public Stream<OWLClassAxiom> axioms(OWLClass clazz, Imports imports) {
        return withImportsToStream(imports, x -> x.axioms(clazz));
    }

    @Override
    public Stream<OWLDatatypeDefinitionAxiom> axioms(OWLDatatype datatype, Imports imports) {
        return withImportsToStream(imports, x -> x.axioms(datatype));
    }

    @Override
    public Stream<OWLIndividualAxiom> axioms(OWLIndividual individual, Imports imports) {
        return withImportsToStream(imports, x -> x.axioms(individual));
    }

    @Override
    public Stream<OWLAnnotationAxiom> axioms(OWLAnnotationProperty property, Imports imports) {
        return withImportsToStream(imports, x -> x.axioms(property));
    }

    @Override
    public Stream<OWLObjectPropertyAxiom> axioms(OWLObjectPropertyExpression property, Imports imports) {
        return withImportsToStream(imports, x -> x.axioms(property));
    }

    @Override
    public Stream<OWLDataPropertyAxiom> axioms(OWLDataProperty property, Imports imports) {
        return withImportsToStream(imports, x -> x.axioms(property));
    }

    @Override
    public Stream<OWLAxiom> tboxAxioms(Imports imports) {
        return axioms(imports, AxiomType.TBoxAxiomTypes);
    }

    @Override
    public Stream<OWLAxiom> aboxAxioms(Imports imports) {
        return axioms(imports, AxiomType.ABoxAxiomTypes);
    }

    @Override
    public Stream<OWLAxiom> rboxAxioms(Imports imports) {
        return axioms(imports, AxiomType.RBoxAxiomTypes);
    }

    protected Stream<OWLAxiom> axioms(Imports imports, Set<AxiomType<?>> types) {
        return withImportsToStream(imports, x -> types.stream().flatMap(x::axioms));
    }

    @Override
    public Stream<OWLEntity> signature(Imports imports) {
        return withImportsToStream(imports, HasSignature::signature);
    }

    @Override
    public Stream<OWLClass> classesInSignature(Imports imports) {
        return withImportsToStream(imports, HasClassesInSignature::classesInSignature);
    }

    @Override
    public Stream<OWLDatatype> datatypesInSignature(Imports imports) {
        return withImportsToStream(imports, HasDatatypesInSignature::datatypesInSignature);
    }

    @Override
    public Stream<OWLNamedIndividual> individualsInSignature(Imports imports) {
        return withImportsToStream(imports, HasIndividualsInSignature::individualsInSignature);
    }

    @Override
    public Stream<OWLObjectProperty> objectPropertiesInSignature(Imports imports) {
        return withImportsToStream(imports, HasObjectPropertiesInSignature::objectPropertiesInSignature);
    }

    @Override
    public Stream<OWLDataProperty> dataPropertiesInSignature(Imports imports) {
        return withImportsToStream(imports, HasDataPropertiesInSignature::dataPropertiesInSignature);
    }

    @Override
    public Stream<OWLAnnotationProperty> annotationPropertiesInSignature(Imports imports) {
        return withImportsToStream(imports, HasAnnotationPropertiesInSignature::annotationPropertiesInSignature);
    }

    @Override
    public Stream<OWLAnonymousIndividual> referencedAnonymousIndividuals(Imports imports) {
        return withImportsToStream(imports, OWLSignature::referencedAnonymousIndividuals);
    }

    @Override
    public Stream<OWLEntity> entitiesInSignature(IRI iri, Imports imports) {
        return withImportsToStream(imports, x -> x.entitiesInSignature(iri));
    }

    @Override
    public <T extends OWLAxiom> Stream<T> axioms(Class<T> type,
                                                 OWLObject object,
                                                 Imports imports,
                                                 Navigation navigation) {
        return withImportsToStream(imports, x -> x.axioms(type, object, navigation));
    }

    @Override
    public <T extends OWLAxiom> Stream<T> axioms(OWLAxiomSearchFilter filter, Object key, Imports imports) {
        return withImportsToStream(imports, x -> x.axioms(filter, key));
    }

    @Override
    public <T extends OWLAxiom> Stream<T> axioms(Class<T> type,
                                                 Class<? extends OWLObject> explicitClass,
                                                 OWLObject object,
                                                 Imports imports,
                                                 Navigation navigation) {
        return withImportsToStream(imports, x -> x.axioms(type, explicitClass, object, navigation));
    }

    @Override
    public Stream<OWLAnnotationAssertionAxiom> annotationAssertionAxioms(OWLAnnotationSubject entity, Imports imports) {
        return withImportsToStream(imports, x -> x.axioms(OWLAnnotationAssertionAxiom.class,
                OWLAnnotationSubject.class, entity, Navigation.IN_SUB_POSITION));
    }

    @Override
    public boolean isDeclared(OWLEntity entity, Imports imports) {
        return withImportsToBoolean(imports, x -> x.isDeclared(entity));
    }

    @Override
    public boolean containsAxiom(OWLAxiom axiom,
                                 Imports imports,
                                 AxiomAnnotations annotations) {
        return withImportsToBoolean(imports, x -> annotations.contains(x, axiom));
    }

    @Override
    public boolean containsEntitiesOfTypeInSignature(EntityType<?> type, Imports imports) {
        return withImportsToBoolean(imports, x -> x.containsEntitiesOfTypeInSignature(type));
    }

    @Override
    public int getAxiomCount(Imports imports) {
        return (int) withImportsToLong(imports, OWLAxiomCollection::getAxiomCount);
    }

    @Override
    public <T extends OWLAxiom> int getAxiomCount(AxiomType<T> type, Imports imports) {
        return (int) withImportsToLong(imports, x -> x.getAxiomCount(type));
    }

    @Override
    public int getLogicalAxiomCount(Imports imports) {
        return (int) withImportsToLong(imports, OWLAxiomCollection::getLogicalAxiomCount);
    }

    @Override
    public List<OWLAnnotation> annotationsAsList() {
        return withReadLockToObject(() -> toList(annotations()));
    }

    @Override
    public Set<IRI> getDirectImportsDocuments() {
        return toSet(this::directImportsDocuments);
    }

    @Override
    public Set<OWLOntology> getDirectImports() {
        return toSet(this::directImports);
    }

    @Override
    public Set<OWLOntology> getImports() {
        return toSet(this::imports);
    }

    @Override
    public Set<OWLOntology> getImportsClosure() {
        return toSet(this::importsClosure);
    }

    @Override
    public Set<OWLImportsDeclaration> getImportsDeclarations() {
        return toSet(this::importsDeclarations);
    }

    @Override
    public Set<OWLClassAxiom> getGeneralClassAxioms() {
        return toSet(this::generalClassAxioms);
    }

    @Override
    public Set<OWLEntity> getSignature() {
        return toSet(this::signature);
    }

    @Override
    public Set<OWLClassExpression> getNestedClassExpressions() {
        return toSet(this::nestedClassExpressions);
    }

    @Override
    public Set<OWLAnonymousIndividual> getAnonymousIndividuals() {
        return toSet(this::anonymousIndividuals);
    }

    @Override
    public Set<OWLClass> getClassesInSignature() {
        return toSet(this::classesInSignature);
    }

    @Override
    public Set<OWLObjectProperty> getObjectPropertiesInSignature() {
        return toSet(this::objectPropertiesInSignature);
    }

    @Override
    public Set<OWLDataProperty> getDataPropertiesInSignature() {
        return toSet(this::dataPropertiesInSignature);
    }

    @Override
    public Set<OWLNamedIndividual> getIndividualsInSignature() {
        return toSet(this::individualsInSignature);
    }

    @Override
    public Set<OWLDatatype> getDatatypesInSignature() {
        return toSet(this::datatypesInSignature);
    }

    @Override
    public Set<OWLAnnotationProperty> getAnnotationPropertiesInSignature() {
        return toSet(this::annotationPropertiesInSignature);
    }

    @Override
    public Set<OWLAnnotation> getAnnotations() {
        return toSet(this::annotations);
    }

    @Override
    public Set<OWLAxiom> getAxioms() {
        return toSet(this::axioms);
    }

    @Override
    public Set<OWLLogicalAxiom> getLogicalAxioms() {
        return toSet(this::logicalAxioms);
    }

    @Override
    public Set<OWLAnonymousIndividual> getReferencedAnonymousIndividuals() {
        return toSet(this::referencedAnonymousIndividuals);
    }

    @Override
    public Set<OWLEntity> getEntitiesInSignature(IRI iri) {
        return toSet(() -> entitiesInSignature(iri));
    }

    @Override
    public Set<OWLAxiom> getAxiomsIgnoreAnnotations(OWLAxiom axiom) {
        return toSet(() -> axiomsIgnoreAnnotations(axiom));
    }

    @Override
    public Set<OWLAxiom> getReferencingAxioms(OWLPrimitive primitive) {
        return toSet(() -> referencingAxioms(primitive));
    }

    @Override
    public Set<OWLClassAxiom> getAxioms(OWLClass clazz) {
        return toSet(() -> axioms(clazz));
    }

    @Override
    public Set<OWLObjectPropertyAxiom> getAxioms(OWLObjectPropertyExpression property) {
        return toSet(() -> axioms(property));
    }

    @Override
    public Set<OWLDataPropertyAxiom> getAxioms(OWLDataProperty property) {
        return toSet(() -> axioms(property));
    }

    @Override
    public Set<OWLIndividualAxiom> getAxioms(OWLIndividual individual) {
        return toSet(() -> axioms(individual));
    }

    @Override
    public Set<OWLAnnotationAxiom> getAxioms(OWLAnnotationProperty property) {
        return toSet(() -> axioms(property));
    }

    @Override
    public Set<OWLDatatypeDefinitionAxiom> getAxioms(OWLDatatype datatype) {
        return toSet(() -> axioms(datatype));
    }

    @Override
    public Set<OWLDeclarationAxiom> getDeclarationAxioms(OWLEntity entity) {
        return toSet(() -> declarationAxioms(entity));
    }

    @Override
    public Set<OWLSubAnnotationPropertyOfAxiom> getSubAnnotationPropertyOfAxioms(OWLAnnotationProperty property) {
        return toSet(() -> subAnnotationPropertyOfAxioms(property));
    }

    @Override
    public Set<OWLAnnotationPropertyDomainAxiom> getAnnotationPropertyDomainAxioms(OWLAnnotationProperty property) {
        return toSet(() -> annotationPropertyDomainAxioms(property));
    }

    @Override
    public Set<OWLAnnotationPropertyRangeAxiom> getAnnotationPropertyRangeAxioms(OWLAnnotationProperty property) {
        return toSet(() -> annotationPropertyRangeAxioms(property));
    }

    @Override
    public Set<OWLAnnotationAssertionAxiom> getAnnotationAssertionAxioms(OWLAnnotationSubject subject) {
        return toSet(() -> annotationAssertionAxioms(subject));
    }

    @Override
    public Set<OWLSubClassOfAxiom> getSubClassAxiomsForSubClass(OWLClass clazz) {
        return toSet(() -> subClassAxiomsForSubClass(clazz));
    }

    @Override
    public Set<OWLSubClassOfAxiom> getSubClassAxiomsForSuperClass(OWLClass clazz) {
        return toSet(() -> subClassAxiomsForSuperClass(clazz));
    }

    @Override
    public Set<OWLEquivalentClassesAxiom> getEquivalentClassesAxioms(OWLClass clazz) {
        return toSet(() -> equivalentClassesAxioms(clazz));
    }

    @Override
    public Set<OWLDisjointClassesAxiom> getDisjointClassesAxioms(OWLClass clazz) {
        return toSet(() -> disjointClassesAxioms(clazz));
    }

    @Override
    public Set<OWLDisjointUnionAxiom> getDisjointUnionAxioms(OWLClass clazz) {
        return toSet(() -> disjointUnionAxioms(clazz));
    }

    @Override
    public Set<OWLHasKeyAxiom> getHasKeyAxioms(OWLClass clazz) {
        return toSet(() -> hasKeyAxioms(clazz));
    }

    @Override
    public Set<OWLSubObjectPropertyOfAxiom> getObjectSubPropertyAxiomsForSubProperty(OWLObjectPropertyExpression property) {
        return toSet(() -> objectSubPropertyAxiomsForSubProperty(property));
    }

    @Override
    public Set<OWLSubObjectPropertyOfAxiom> getObjectSubPropertyAxiomsForSuperProperty(OWLObjectPropertyExpression property) {
        return toSet(() -> objectSubPropertyAxiomsForSuperProperty(property));
    }

    @Override
    public Set<OWLObjectPropertyDomainAxiom> getObjectPropertyDomainAxioms(OWLObjectPropertyExpression property) {
        return toSet(() -> objectPropertyDomainAxioms(property));
    }

    @Override
    public Set<OWLObjectPropertyRangeAxiom> getObjectPropertyRangeAxioms(OWLObjectPropertyExpression property) {
        return toSet(() -> objectPropertyRangeAxioms(property));
    }

    @Override
    public Set<OWLInverseObjectPropertiesAxiom> getInverseObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        return toSet(() -> inverseObjectPropertyAxioms(property));
    }

    @Override
    public Set<OWLEquivalentObjectPropertiesAxiom> getEquivalentObjectPropertiesAxioms(OWLObjectPropertyExpression property) {
        return toSet(() -> equivalentObjectPropertiesAxioms(property));
    }

    @Override
    public Set<OWLDisjointObjectPropertiesAxiom> getDisjointObjectPropertiesAxioms(OWLObjectPropertyExpression property) {
        return toSet(() -> disjointObjectPropertiesAxioms(property));
    }

    @Override
    public Set<OWLFunctionalObjectPropertyAxiom> getFunctionalObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        return toSet(() -> functionalObjectPropertyAxioms(property));
    }

    @Override
    public Set<OWLInverseFunctionalObjectPropertyAxiom> getInverseFunctionalObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        return toSet(() -> inverseFunctionalObjectPropertyAxioms(property));
    }

    @Override
    public Set<OWLSymmetricObjectPropertyAxiom> getSymmetricObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        return toSet(() -> symmetricObjectPropertyAxioms(property));
    }

    @Override
    public Set<OWLAsymmetricObjectPropertyAxiom> getAsymmetricObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        return toSet(() -> asymmetricObjectPropertyAxioms(property));
    }

    @Override
    public Set<OWLReflexiveObjectPropertyAxiom> getReflexiveObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        return toSet(() -> reflexiveObjectPropertyAxioms(property));
    }

    @Override
    public Set<OWLIrreflexiveObjectPropertyAxiom> getIrreflexiveObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        return toSet(() -> irreflexiveObjectPropertyAxioms(property));
    }

    @Override
    public Set<OWLTransitiveObjectPropertyAxiom> getTransitiveObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        return toSet(() -> transitiveObjectPropertyAxioms(property));
    }

    @Override
    public Set<OWLSubDataPropertyOfAxiom> getDataSubPropertyAxiomsForSubProperty(OWLDataProperty property) {
        return toSet(() -> dataSubPropertyAxiomsForSubProperty(property));
    }

    @Override
    public Set<OWLSubDataPropertyOfAxiom> getDataSubPropertyAxiomsForSuperProperty(OWLDataPropertyExpression property) {
        return toSet(() -> dataSubPropertyAxiomsForSuperProperty(property));
    }

    @Override
    public Set<OWLDataPropertyDomainAxiom> getDataPropertyDomainAxioms(OWLDataProperty property) {
        return toSet(() -> dataPropertyDomainAxioms(property));
    }

    @Override
    public Set<OWLDataPropertyRangeAxiom> getDataPropertyRangeAxioms(OWLDataProperty property) {
        return toSet(() -> dataPropertyRangeAxioms(property));
    }

    @Override
    public Set<OWLEquivalentDataPropertiesAxiom> getEquivalentDataPropertiesAxioms(OWLDataProperty property) {
        return toSet(() -> equivalentDataPropertiesAxioms(property));
    }

    @Override
    public Set<OWLDisjointDataPropertiesAxiom> getDisjointDataPropertiesAxioms(OWLDataProperty property) {
        return toSet(() -> disjointDataPropertiesAxioms(property));
    }

    @Override
    public Set<OWLFunctionalDataPropertyAxiom> getFunctionalDataPropertyAxioms(OWLDataPropertyExpression property) {
        return toSet(() -> functionalDataPropertyAxioms(property));
    }

    @Override
    public Set<OWLClassAssertionAxiom> getClassAssertionAxioms(OWLIndividual individual) {
        return toSet(() -> classAssertionAxioms(individual));
    }

    @Override
    public Set<OWLClassAssertionAxiom> getClassAssertionAxioms(OWLClassExpression clazz) {
        return toSet(() -> classAssertionAxioms(clazz));
    }

    @Override
    public Set<OWLDataPropertyAssertionAxiom> getDataPropertyAssertionAxioms(OWLIndividual individual) {
        return toSet(() -> dataPropertyAssertionAxioms(individual));
    }

    @Override
    public Set<OWLObjectPropertyAssertionAxiom> getObjectPropertyAssertionAxioms(OWLIndividual individual) {
        return toSet(() -> objectPropertyAssertionAxioms(individual));
    }

    @Override
    public Set<OWLNegativeObjectPropertyAssertionAxiom> getNegativeObjectPropertyAssertionAxioms(OWLIndividual individual) {
        return toSet(() -> negativeObjectPropertyAssertionAxioms(individual));
    }

    @Override
    public Set<OWLNegativeDataPropertyAssertionAxiom> getNegativeDataPropertyAssertionAxioms(OWLIndividual individual) {
        return toSet(() -> negativeDataPropertyAssertionAxioms(individual));
    }

    @Override
    public Set<OWLSameIndividualAxiom> getSameIndividualAxioms(OWLIndividual individual) {
        return toSet(() -> sameIndividualAxioms(individual));
    }

    @Override
    public Set<OWLDifferentIndividualsAxiom> getDifferentIndividualAxioms(OWLIndividual individual) {
        return toSet(() -> differentIndividualAxioms(individual));
    }

    @Override
    public Set<OWLDatatypeDefinitionAxiom> getDatatypeDefinitions(OWLDatatype datatype) {
        return toSet(() -> datatypeDefinitions(datatype));
    }

    @Override
    public Set<OWLAnnotation> getAnnotations(OWLAnnotationProperty property) {
        return toSet(this::annotations);
    }

    @Override
    public <T extends OWLAxiom> Set<T> getAxioms(AxiomType<T> type) {
        return toSet(() -> axioms(type));
    }

    @Override
    public <T extends OWLAxiom> Set<T> getAxioms(Class<T> type,
                                                 Class<? extends OWLObject> classType,
                                                 OWLObject entity,
                                                 Navigation navigation) {
        return toSet(() -> axioms(type, classType, entity, navigation));
    }

    @Override
    public <T extends OWLAxiom> Set<T> getAxioms(Class<T> type, OWLObject entity, Navigation navigation) {
        return toSet(() -> axioms(type, entity, navigation));
    }

    @Override
    public <T extends OWLAxiom> Collection<T> filterAxioms(OWLAxiomSearchFilter filter, Object key) {
        return toSet(() -> axioms(filter, key));
    }

    @Override
    public Set<OWLAxiom> getAxioms(Imports imports) {
        return toSet(() -> axioms(imports));
    }

    @Override
    public Set<OWLLogicalAxiom> getLogicalAxioms(Imports imports) {
        return toSet(() -> logicalAxioms(imports));
    }

    @Override
    public Set<OWLEntity> getSignature(Imports imports) {
        return toSet(() -> signature(imports));
    }

    @Override
    public Set<OWLAxiom> getTBoxAxioms(Imports imports) {
        return toSet(() -> tboxAxioms(imports));
    }

    @Override
    public Set<OWLAxiom> getABoxAxioms(Imports imports) {
        return toSet(() -> aboxAxioms(imports));
    }

    @Override
    public Set<OWLAxiom> getRBoxAxioms(Imports imports) {
        return toSet(() -> rboxAxioms(imports));
    }

    @Override
    public Set<OWLClass> getClassesInSignature(Imports imports) {
        return toSet(() -> classesInSignature(imports));
    }

    @Override
    public Set<OWLDatatype> getDatatypesInSignature(Imports imports) {
        return toSet(() -> datatypesInSignature(imports));
    }

    @Override
    public Set<OWLNamedIndividual> getIndividualsInSignature(Imports imports) {
        return toSet(() -> individualsInSignature(imports));
    }

    @Override
    public Set<OWLObjectProperty> getObjectPropertiesInSignature(Imports imports) {
        return toSet(() -> objectPropertiesInSignature(imports));
    }

    @Override
    public Set<OWLDataProperty> getDataPropertiesInSignature(Imports imports) {
        return toSet(() -> dataPropertiesInSignature(imports));
    }

    @Override
    public Set<OWLAnnotationProperty> getAnnotationPropertiesInSignature(Imports imports) {
        return toSet(() -> annotationPropertiesInSignature(imports));
    }

    @Override
    public Set<OWLAnonymousIndividual> getReferencedAnonymousIndividuals(Imports imports) {
        return toSet(() -> referencedAnonymousIndividuals(imports));
    }

    @Override
    public <T extends OWLAxiom> Set<T> getAxioms(AxiomType<T> type, Imports imports) {
        return toSet(() -> axioms(type, imports));
    }

    @Override
    public Set<OWLAxiom> getAxiomsIgnoreAnnotations(OWLAxiom axiom, Imports imports) {
        return toSet(() -> axiomsIgnoreAnnotations(axiom, imports));
    }

    @Override
    public Set<OWLAxiom> getReferencingAxioms(OWLPrimitive primitive, Imports imports) {
        return toSet(() -> referencingAxioms(primitive, imports));
    }

    @Override
    public Set<OWLClassAxiom> getAxioms(OWLClass clazz, Imports imports) {
        return toSet(() -> axioms(clazz, imports));
    }

    @Override
    public Set<OWLObjectPropertyAxiom> getAxioms(OWLObjectPropertyExpression property, Imports imports) {
        return toSet(() -> axioms(property, imports));
    }

    @Override
    public Set<OWLDataPropertyAxiom> getAxioms(OWLDataProperty property, Imports imports) {
        return toSet(() -> axioms(property, imports));
    }

    @Override
    public Set<OWLIndividualAxiom> getAxioms(OWLIndividual individual, Imports imports) {
        return toSet(() -> axioms(individual, imports));
    }

    @Override
    public Set<OWLAnnotationAxiom> getAxioms(OWLAnnotationProperty property, Imports imports) {
        return toSet(() -> axioms(property, imports));
    }

    @Override
    public Set<OWLDatatypeDefinitionAxiom> getAxioms(OWLDatatype datatype, Imports imports) {
        return toSet(() -> axioms(datatype, imports));
    }

    @Override
    public Set<OWLEntity> getEntitiesInSignature(IRI iri, Imports imports) {
        return toSet(() -> entitiesInSignature(iri, imports));
    }

    @Override
    public Set<OWLAnnotationAssertionAxiom> getAnnotationAssertionAxioms(OWLAnnotationSubject entity, Imports imports) {
        return toSet(() -> annotationAssertionAxioms(entity, imports));
    }

    @Override
    public <T extends OWLAxiom> Set<T> getAxioms(Class<T> type,
                                                 OWLObject object,
                                                 Imports imports,
                                                 Navigation navigation) {
        return toSet(() -> axioms(type, object, imports, navigation));
    }

    @Override
    public <T extends OWLAxiom> Collection<T> filterAxioms(OWLAxiomSearchFilter filter, Object o, Imports imports) {
        return toSet(() -> axioms(filter, o, imports));
    }

    @Override
    public <T extends OWLAxiom> Set<T> getAxioms(Class<T> type,
                                                 Class<? extends OWLObject> explicitClass,
                                                 OWLObject object,
                                                 Imports imports,
                                                 Navigation navigation) {
        return toSet(() -> axioms(type, explicitClass, object, imports, navigation));
    }

    @Override
    public boolean contains(OWLAxiomSearchFilter filter, Object o, Imports imports) {
        return withImportsToBoolean(imports, x -> x.contains(filter, o));
    }

    @Override
    public boolean containsReference(OWLEntity entity, Imports imports) {
        return withImportsToBoolean(imports, x -> x.containsReference(entity));
    }

    @Override
    public boolean containsEntityInSignature(OWLEntity entity, Imports imports) {
        return withImportsToBoolean(imports, x -> x.containsEntityInSignature(entity));
    }

    @Override
    public boolean containsEntityInSignature(IRI iri, Imports imports) {
        return withImportsToBoolean(imports, x -> x.containsEntityInSignature(iri));
    }

    @Override
    public boolean containsClassInSignature(IRI iri, Imports imports) {
        return withImportsToBoolean(imports, x -> x.containsClassInSignature(iri));
    }

    @Override
    public boolean containsObjectPropertyInSignature(IRI iri, Imports imports) {
        return withImportsToBoolean(imports, x -> x.containsObjectPropertyInSignature(iri));
    }

    @Override
    public boolean containsDataPropertyInSignature(IRI iri, Imports imports) {
        return withImportsToBoolean(imports, x -> x.containsDataPropertyInSignature(iri));
    }

    @Override
    public boolean containsAnnotationPropertyInSignature(IRI iri, Imports imports) {
        return withImportsToBoolean(imports, x -> x.containsAnnotationPropertyInSignature(iri));
    }

    @Override
    public boolean containsDatatypeInSignature(IRI iri, Imports imports) {
        return withImportsToBoolean(imports, x -> x.containsDatatypeInSignature(iri));
    }

    @Override
    public boolean containsIndividualInSignature(IRI iri, Imports imports) {
        return withImportsToBoolean(imports, x -> x.containsIndividualInSignature(iri));
    }

    @FunctionalInterface
    protected interface WithThrowable<T extends Throwable> {
        void apply() throws T;
    }

}