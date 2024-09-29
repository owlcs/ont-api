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

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.semanticweb.owlapi.io.OWLOntologyDocumentTarget;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.ChangeDetails;
import org.semanticweb.owlapi.model.EntityType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationSubject;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLAsymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyExpression;
import org.semanticweb.owlapi.model.OWLDataPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLDatatypeDefinitionAxiom;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLDifferentIndividualsAxiom;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointUnionAxiom;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalDataPropertyAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLHasKeyAxiom;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLIndividualAxiom;
import org.semanticweb.owlapi.model.OWLInverseFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLInverseObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLIrreflexiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLMutableOntology;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLNamedObjectVisitor;
import org.semanticweb.owlapi.model.OWLNamedObjectVisitorEx;
import org.semanticweb.owlapi.model.OWLNegativeDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLNegativeObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLObjectVisitor;
import org.semanticweb.owlapi.model.OWLObjectVisitorEx;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLPrimitive;
import org.semanticweb.owlapi.model.OWLReflexiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLSameIndividualAxiom;
import org.semanticweb.owlapi.model.OWLSubAnnotationPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubDataPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLTransitiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.parameters.AxiomAnnotations;
import org.semanticweb.owlapi.model.parameters.ChangeApplied;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.model.parameters.Navigation;
import org.semanticweb.owlapi.util.OWLAxiomSearchFilter;

import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A decorator for any {@link OWLOntology} instance.
 * Notices that it does not implement {@link Ontology} and therefore cannot be used in ONT-API directly.
 * <p>
 * Matthew Horridge Stanford Center for Biomedical Informatics Research 03/04/15
 *
 * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/concurrent/ConcurrentOWLOntologyImpl.java'>uk.ac.manchester.cs.owl.owlapi.concurrent.ConcurrentOWLOntologyImpl</a>
 */
@SuppressWarnings("WeakerAccess")
@ParametersAreNonnullByDefault
public class OWLOntologyWrapper implements OWLMutableOntology {

    protected final OWLOntology delegate;
    protected final ReadWriteLock lock;

    /**
     * Constructs a new {@code OWLOntologyWrapper}
     * that provides concurrent access to the specified delegate {@link OWLOntology ontology}.
     *
     * @param delegate {@link OWLOntology} the delegate
     * @param lock     {@link ReadWriteLock} the R/W Lock to provide thread-safe or {@code null} for non-concurrent mode
     */
    public OWLOntologyWrapper(OWLOntology delegate, @Nullable ReadWriteLock lock) {
        this.lock = NoOpReadWriteLock.nonNull(lock);
        this.delegate = Objects.requireNonNull(delegate, "Null delegate");
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
        throw new OntApiException.Unsupported("Misuse: attempt to change lock.");
    }

    /**
     * Answers {@code true} if the ontology is equipped with a true R/W lock and therefore must be thread-safe.
     *
     * @return boolean
     */
    public final boolean isConcurrent() {
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

    protected <X> Stream<X> withReadLockToStream(Supplier<Stream<X>> op) {
        if (!isConcurrent()) {
            return op.get();
        }
        lock.readLock().lock();
        try {
            return op.get().toList().stream();
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

    @Override
    public int hashCode() {
        return withReadLockToObject(delegate::hashCode);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return withReadLockToObject(() -> {
            if (obj == OWLOntologyWrapper.this) return true;
            return delegate.equals(obj instanceof OWLOntologyWrapper ? ((OWLOntologyWrapper) obj).delegate : obj);
        });
    }

    @Override
    public String toString() {
        return withReadLockToObject(delegate::toString);
    }

    @Override
    public int compareTo(OWLObject o) {
        return withReadLockToObject(() -> delegate.compareTo(o));
    }

    @Override
    public void accept(OWLNamedObjectVisitor visitor) {
        delegate.accept(visitor);
    }

    @Override
    public <O> O accept(OWLNamedObjectVisitorEx<O> visitor) {
        return delegate.accept(visitor);
    }

    @Override
    public void accept(OWLObjectVisitor visitor) {
        delegate.accept(visitor);
    }

    @Override
    public <O> O accept(OWLObjectVisitorEx<O> visitor) {
        return delegate.accept(visitor);
    }

    @Override
    public OWLOntologyManager getOWLOntologyManager() {
        return withReadLockToObject(delegate::getOWLOntologyManager);
    }

    @Override
    public void setOWLOntologyManager(@Nullable OWLOntologyManager manager) {
        withWriteLock(() -> delegate.setOWLOntologyManager(manager));
    }

    @Override
    public OWLOntologyID getOntologyID() {
        return withReadLockToObject(delegate::getOntologyID);
    }

    @Override
    public boolean isAnonymous() {
        return withReadLockToObject(delegate::isAnonymous);
    }

    @Override
    public boolean isEmpty() {
        return withReadLockToObject(delegate::isEmpty);
    }

    @Override
    public Stream<IRI> directImportsDocuments() {
        return withReadLockToStream(delegate::directImportsDocuments);
    }

    @Override
    public Stream<OWLOntology> directImports() {
        return withReadLockToStream(delegate::directImports);
    }

    @Override
    public Stream<OWLOntology> imports() {
        return withReadLockToStream(() -> getOWLOntologyManager().imports(this));
    }

    @Override
    public Stream<OWLOntology> importsClosure() {
        return withReadLockToStream(() -> getOWLOntologyManager().importsClosure(this));
    }

    @Override
    public Stream<OWLClassAxiom> generalClassAxioms() {
        return withReadLockToStream(delegate::generalClassAxioms);
    }

    @Override
    public Stream<OWLEntity> signature() {
        return withReadLockToStream(delegate::signature);
    }

    @Override
    public boolean isDeclared(OWLEntity entity) {
        return withReadLockToObject(() -> delegate.isDeclared(entity));
    }

    @Override
    public boolean containsEntityInSignature(OWLEntity entity) {
        return withReadLockToObject(() -> delegate.containsEntityInSignature(entity));
    }

    @Override
    public boolean containsEntitiesOfTypeInSignature(EntityType<?> type) {
        return withReadLockToObject(() -> delegate.containsEntitiesOfTypeInSignature(type));
    }

    @Override
    public Stream<OWLAxiom> axioms() {
        return withReadLockToStream(delegate::axioms);
    }

    @Override
    public Stream<OWLLogicalAxiom> logicalAxioms() {
        return withReadLockToStream(delegate::logicalAxioms);
    }

    @Override
    public <T extends OWLAxiom> Stream<T> axioms(AxiomType<T> type) {
        return withReadLockToStream(() -> delegate.axioms(type));
    }

    @Override
    public boolean containsAxiom(OWLAxiom axiom) {
        return withReadLockToObject(() -> delegate.containsAxiom(axiom));
    }

    @Override
    public int getAxiomCount() {
        return withReadLockToObject(delegate::getAxiomCount);
    }

    @Override
    public int getLogicalAxiomCount() {
        return withReadLockToObject(delegate::getLogicalAxiomCount);
    }

    @Override
    public <T extends OWLAxiom> int getAxiomCount(AxiomType<T> type) {
        return withReadLockToObject(() -> delegate.getAxiomCount(type));
    }

    @Override
    public boolean containsAxiomIgnoreAnnotations(OWLAxiom axiom) {
        return withReadLockToObject(() -> delegate.containsAxiomIgnoreAnnotations(axiom));
    }

    @Override
    public Stream<OWLAxiom> axiomsIgnoreAnnotations(OWLAxiom axiom) {
        return withReadLockToStream(() -> delegate.axiomsIgnoreAnnotations(axiom));
    }

    @Override
    public Stream<OWLAxiom> referencingAxioms(OWLPrimitive primitive) {
        return withReadLockToStream(() -> delegate.referencingAxioms(primitive));
    }

    @Override
    public Stream<OWLClassAxiom> axioms(OWLClass clazz) {
        return withReadLockToStream(() -> delegate.axioms(clazz));
    }

    @Override
    public Stream<OWLObjectPropertyAxiom> axioms(OWLObjectPropertyExpression property) {
        return withReadLockToStream(() -> delegate.axioms(property));
    }

    @Override
    public Stream<OWLDataPropertyAxiom> axioms(OWLDataProperty property) {
        return withReadLockToStream(() -> delegate.axioms(property));
    }

    @Override
    public Stream<OWLIndividualAxiom> axioms(OWLIndividual individual) {
        return withReadLockToStream(() -> delegate.axioms(individual));
    }

    @Override
    public Stream<OWLAnnotationAxiom> axioms(OWLAnnotationProperty property) {
        return withReadLockToStream(() -> delegate.axioms(property));
    }

    @Override
    public Stream<OWLDatatypeDefinitionAxiom> axioms(OWLDatatype datatype) {
        return withReadLockToStream(() -> delegate.axioms(datatype));
    }

    @Override
    public Stream<OWLAnonymousIndividual> referencedAnonymousIndividuals() {
        return withReadLockToStream(delegate::referencedAnonymousIndividuals);
    }

    @Override
    public boolean containsDatatypeInSignature(IRI iri) {
        return withReadLockToObject(() -> delegate.containsDatatypeInSignature(iri));
    }

    @Override
    public boolean containsEntityInSignature(IRI iri) {
        return withReadLockToObject(() -> delegate.containsEntityInSignature(iri));
    }

    @Override
    public boolean containsClassInSignature(IRI iri) {
        return withReadLockToObject(() -> delegate.containsClassInSignature(iri));
    }

    @Override
    public boolean containsObjectPropertyInSignature(IRI iri) {
        return withReadLockToObject(() -> delegate.containsObjectPropertyInSignature(iri));
    }

    @Override
    public boolean containsDataPropertyInSignature(IRI iri) {
        return withReadLockToObject(() -> delegate.containsDataPropertyInSignature(iri));
    }

    @Override
    public boolean containsAnnotationPropertyInSignature(IRI iri) {
        return withReadLockToObject(() -> delegate.containsAnnotationPropertyInSignature(iri));
    }

    @Override
    public boolean containsIndividualInSignature(IRI iri) {
        return withReadLockToObject(() -> delegate.containsIndividualInSignature(iri));
    }

    @Override
    public Set<IRI> getPunnedIRIs(Imports imports) {
        return withReadLockToObject(() -> delegate.getPunnedIRIs(imports));
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean containsReference(OWLEntity entity) {
        return withReadLockToObject(() -> delegate.containsReference(entity));
    }

    @Override
    public Stream<OWLEntity> entitiesInSignature(IRI iri) {
        return withReadLockToStream(() -> delegate.entitiesInSignature(iri));
    }

    @Override
    public boolean contains(OWLAxiomSearchFilter filter, Object o) {
        return withReadLockToObject(() -> delegate.contains(filter, o));
    }

    @Override
    public Stream<OWLAnnotationPropertyDomainAxiom> annotationPropertyDomainAxioms(OWLAnnotationProperty property) {
        return withReadLockToStream(() -> delegate.annotationPropertyDomainAxioms(property));
    }

    @Override
    public Stream<OWLAnnotationPropertyRangeAxiom> annotationPropertyRangeAxioms(OWLAnnotationProperty property) {
        return withReadLockToStream(() -> delegate.annotationPropertyRangeAxioms(property));
    }

    @Override
    public Stream<OWLImportsDeclaration> importsDeclarations() {
        return withReadLockToStream(delegate::importsDeclarations);
    }

    @Override
    public <T extends OWLAxiom> Stream<T> axioms(OWLAxiomSearchFilter filter, Object key) {
        return withReadLockToStream(() -> delegate.axioms(filter, key));
    }

    @Override
    public <T extends OWLAxiom> Stream<T> axioms(Class<T> type,
                                                 Class<? extends OWLObject> explicitType,
                                                 OWLObject entity,
                                                 Navigation navigation) {
        return withReadLockToStream(() -> delegate.axioms(type, explicitType, entity, navigation));
    }

    @Override
    public Stream<OWLSubAnnotationPropertyOfAxiom> subAnnotationPropertyOfAxioms(OWLAnnotationProperty property) {
        return withReadLockToStream(() -> delegate.subAnnotationPropertyOfAxioms(property));
    }

    @Override
    public Stream<OWLDatatypeDefinitionAxiom> datatypeDefinitions(OWLDatatype datatype) {
        return withReadLockToStream(() -> delegate.datatypeDefinitions(datatype));
    }

    @Override
    public Stream<OWLDisjointObjectPropertiesAxiom> disjointObjectPropertiesAxioms(OWLObjectPropertyExpression property) {
        return withReadLockToStream(() -> delegate.disjointObjectPropertiesAxioms(property));
    }

    @Override
    public Stream<OWLObjectProperty> objectPropertiesInSignature() {
        return withReadLockToStream(delegate::objectPropertiesInSignature);
    }

    @Override
    public Stream<OWLAnnotationAssertionAxiom> annotationAssertionAxioms(OWLAnnotationSubject entity) {
        return withReadLockToStream(() -> delegate.annotationAssertionAxioms(entity));
    }

    @Override
    public Stream<OWLAnnotationProperty> annotationPropertiesInSignature() {
        return withReadLockToStream(delegate::annotationPropertiesInSignature);
    }

    @Override
    public Stream<OWLAnnotation> annotations() {
        return withReadLockToStream(delegate::annotations);
    }

    @Override
    public Stream<OWLAnnotation> annotations(OWLAnnotationProperty p) {
        return withReadLockToStream(() -> delegate.annotations(p));
    }

    @Override
    public Stream<OWLAnnotation> annotations(Predicate<OWLAnnotation> p) {
        return withReadLockToStream(() -> delegate.annotations(p));
    }

    @Override
    public Stream<OWLAnonymousIndividual> anonymousIndividuals() {
        return withReadLockToStream(delegate::anonymousIndividuals);
    }

    @Override
    public Stream<OWLAsymmetricObjectPropertyAxiom> asymmetricObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        return withReadLockToStream(() -> delegate.asymmetricObjectPropertyAxioms(property));
    }

    @Override
    public <T extends OWLAxiom> Stream<T> axioms(Class<T> type, OWLObject entity, Navigation navigation) {
        return withReadLockToStream(() -> delegate.axioms(type, entity, navigation));
    }

    @Override
    public Stream<OWLClassAssertionAxiom> classAssertionAxioms(OWLClassExpression ce) {
        return withReadLockToStream(() -> delegate.classAssertionAxioms(ce));
    }

    @Override
    public Stream<OWLClassAssertionAxiom> classAssertionAxioms(OWLIndividual individual) {
        return withReadLockToStream(() -> delegate.classAssertionAxioms(individual));
    }

    @Override
    public Stream<OWLClass> classesInSignature() {
        return withReadLockToStream(delegate::classesInSignature);
    }

    @Override
    public Stream<OWLDataProperty> dataPropertiesInSignature() {
        return withReadLockToStream(delegate::dataPropertiesInSignature);
    }

    @Override
    public Stream<OWLDataPropertyAssertionAxiom> dataPropertyAssertionAxioms(OWLIndividual individual) {
        return withReadLockToStream(() -> delegate.dataPropertyAssertionAxioms(individual));
    }

    @Override
    public Stream<OWLDataPropertyDomainAxiom> dataPropertyDomainAxioms(OWLDataProperty property) {
        return withReadLockToStream(() -> delegate.dataPropertyDomainAxioms(property));
    }

    @Override
    public Stream<OWLDataPropertyRangeAxiom> dataPropertyRangeAxioms(OWLDataProperty property) {
        return withReadLockToStream(() -> delegate.dataPropertyRangeAxioms(property));
    }

    @Override
    public Stream<OWLSubDataPropertyOfAxiom> dataSubPropertyAxiomsForSubProperty(OWLDataProperty property) {
        return withReadLockToStream(() -> delegate.dataSubPropertyAxiomsForSubProperty(property));
    }

    @Override
    public Stream<OWLSubDataPropertyOfAxiom> dataSubPropertyAxiomsForSuperProperty(OWLDataPropertyExpression property) {
        return withReadLockToStream(() -> delegate.dataSubPropertyAxiomsForSuperProperty(property));
    }

    @Override
    public Stream<OWLDatatype> datatypesInSignature() {
        return withReadLockToStream(delegate::datatypesInSignature);
    }

    @Override
    public Stream<OWLDeclarationAxiom> declarationAxioms(OWLEntity subject) {
        return withReadLockToStream(() -> delegate.declarationAxioms(subject));
    }

    @Override
    public Stream<OWLDifferentIndividualsAxiom> differentIndividualAxioms(OWLIndividual individual) {
        return withReadLockToStream(() -> delegate.differentIndividualAxioms(individual));
    }

    @Override
    public Stream<OWLDisjointClassesAxiom> disjointClassesAxioms(OWLClass clazz) {
        return withReadLockToStream(() -> delegate.disjointClassesAxioms(clazz));
    }

    @Override
    public Stream<OWLDisjointDataPropertiesAxiom> disjointDataPropertiesAxioms(OWLDataProperty property) {
        return withReadLockToStream(() -> delegate.disjointDataPropertiesAxioms(property));
    }

    @Override
    public Stream<OWLDisjointUnionAxiom> disjointUnionAxioms(OWLClass clazz) {
        return withReadLockToStream(() -> delegate.disjointUnionAxioms(clazz));
    }

    @Override
    public Stream<OWLEquivalentClassesAxiom> equivalentClassesAxioms(OWLClass clazz) {
        return withReadLockToStream(() -> delegate.equivalentClassesAxioms(clazz));
    }

    @Override
    public Stream<OWLEquivalentDataPropertiesAxiom> equivalentDataPropertiesAxioms(OWLDataProperty property) {
        return withReadLockToStream(() -> delegate.equivalentDataPropertiesAxioms(property));
    }

    @Override
    public Stream<OWLEquivalentObjectPropertiesAxiom> equivalentObjectPropertiesAxioms(OWLObjectPropertyExpression property) {
        return withReadLockToStream(() -> delegate.equivalentObjectPropertiesAxioms(property));
    }

    @Override
    public Stream<OWLFunctionalDataPropertyAxiom> functionalDataPropertyAxioms(OWLDataPropertyExpression property) {
        return withReadLockToStream(() -> delegate.functionalDataPropertyAxioms(property));
    }

    @Override
    public Stream<OWLFunctionalObjectPropertyAxiom> functionalObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        return withReadLockToStream(() -> delegate.functionalObjectPropertyAxioms(property));
    }

    @Override
    public Stream<OWLHasKeyAxiom> hasKeyAxioms(OWLClass clazz) {
        return withReadLockToStream(() -> delegate.hasKeyAxioms(clazz));
    }

    @Override
    public Stream<OWLNamedIndividual> individualsInSignature() {
        return withReadLockToStream(delegate::individualsInSignature);
    }

    @Override
    public Stream<OWLInverseFunctionalObjectPropertyAxiom> inverseFunctionalObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        return withReadLockToStream(() -> delegate.inverseFunctionalObjectPropertyAxioms(property));
    }

    @Override
    public Stream<OWLInverseObjectPropertiesAxiom> inverseObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        return withReadLockToStream(() -> delegate.inverseObjectPropertyAxioms(property));
    }

    @Override
    public Stream<OWLIrreflexiveObjectPropertyAxiom> irreflexiveObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        return withReadLockToStream(() -> delegate.irreflexiveObjectPropertyAxioms(property));
    }

    @Override
    public Stream<OWLNegativeDataPropertyAssertionAxiom> negativeDataPropertyAssertionAxioms(OWLIndividual individual) {
        return withReadLockToStream(() -> delegate.negativeDataPropertyAssertionAxioms(individual));
    }

    @Override
    public Stream<OWLNegativeObjectPropertyAssertionAxiom> negativeObjectPropertyAssertionAxioms(OWLIndividual individual) {
        return withReadLockToStream(() -> delegate.negativeObjectPropertyAssertionAxioms(individual));
    }

    @Override
    public Stream<OWLClassExpression> nestedClassExpressions() {
        return withReadLockToStream(delegate::nestedClassExpressions);
    }

    @Override
    public Stream<OWLObjectPropertyAssertionAxiom> objectPropertyAssertionAxioms(OWLIndividual individual) {
        return withReadLockToStream(() -> delegate.objectPropertyAssertionAxioms(individual));
    }

    @Override
    public Stream<OWLObjectPropertyDomainAxiom> objectPropertyDomainAxioms(OWLObjectPropertyExpression property) {
        return withReadLockToStream(() -> delegate.objectPropertyDomainAxioms(property));
    }

    @Override
    public Stream<OWLObjectPropertyRangeAxiom> objectPropertyRangeAxioms(OWLObjectPropertyExpression property) {
        return withReadLockToStream(() -> delegate.objectPropertyRangeAxioms(property));
    }

    @Override
    public Stream<OWLSubObjectPropertyOfAxiom> objectSubPropertyAxiomsForSubProperty(OWLObjectPropertyExpression property) {
        return withReadLockToStream(() -> delegate.objectSubPropertyAxiomsForSubProperty(property));
    }

    @Override
    public Stream<OWLSubObjectPropertyOfAxiom> objectSubPropertyAxiomsForSuperProperty(OWLObjectPropertyExpression property) {
        return withReadLockToStream(() -> delegate.objectSubPropertyAxiomsForSuperProperty(property));
    }

    @Override
    public Stream<OWLReflexiveObjectPropertyAxiom> reflexiveObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        return withReadLockToStream(() -> delegate.reflexiveObjectPropertyAxioms(property));
    }

    @Override
    public Stream<OWLSameIndividualAxiom> sameIndividualAxioms(OWLIndividual individual) {
        return withReadLockToStream(() -> delegate.sameIndividualAxioms(individual));
    }

    @Override
    public Stream<OWLSubClassOfAxiom> subClassAxiomsForSubClass(OWLClass clazz) {
        return withReadLockToStream(() -> delegate.subClassAxiomsForSubClass(clazz));
    }

    @Override
    public Stream<OWLSubClassOfAxiom> subClassAxiomsForSuperClass(OWLClass clazz) {
        return withReadLockToStream(() -> delegate.subClassAxiomsForSuperClass(clazz));
    }

    @Override
    public Stream<OWLSymmetricObjectPropertyAxiom> symmetricObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        return withReadLockToStream(() -> delegate.symmetricObjectPropertyAxioms(property));
    }

    @Override
    public Stream<OWLTransitiveObjectPropertyAxiom> transitiveObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        return withReadLockToStream(() -> delegate.transitiveObjectPropertyAxioms(property));
    }

    @Override
    public void saveOntology() throws OWLOntologyStorageException {
        withReadLock(delegate::saveOntology);
    }

    @Override
    public void saveOntology(IRI iri) throws OWLOntologyStorageException {
        withReadLock(() -> delegate.saveOntology(iri));
    }

    @Override
    public void saveOntology(OutputStream stream) throws OWLOntologyStorageException {
        withReadLock(() -> delegate.saveOntology(stream));
    }

    @Override
    public void saveOntology(OWLDocumentFormat format) throws OWLOntologyStorageException {
        withReadLock(() -> delegate.saveOntology(format));
    }

    @Override
    public void saveOntology(OWLDocumentFormat format, IRI iri) throws OWLOntologyStorageException {
        withReadLock(() -> delegate.saveOntology(format, iri));
    }

    @Override
    public void saveOntology(OWLDocumentFormat format, OutputStream stream) throws OWLOntologyStorageException {
        withReadLock(() -> delegate.saveOntology(format, stream));
    }

    @Override
    public void saveOntology(OWLOntologyDocumentTarget target) throws OWLOntologyStorageException {
        withReadLock(() -> delegate.saveOntology(target));
    }

    @Override
    public void saveOntology(OWLDocumentFormat format,
                             OWLOntologyDocumentTarget target) throws OWLOntologyStorageException {
        withReadLock(() -> delegate.saveOntology(format, target));
    }

    @Override
    @Nullable
    public OWLDocumentFormat getFormat() {
        return withReadLockToObject(delegate::getFormat);
    }

    @Override
    public ChangeApplied applyDirectChange(OWLOntologyChange change) {
        return withWriteLockToObject(() -> delegate.applyDirectChange(change));
    }

    @Override
    public ChangeApplied applyChange(OWLOntologyChange change) {
        return withWriteLockToObject(() -> delegate.applyChange(change));
    }

    @Override
    public ChangeDetails applyChangesAndGetDetails(List<? extends OWLOntologyChange> changes) {
        return withWriteLockToObject(() -> delegate.applyChangesAndGetDetails(changes));
    }

    @Override
    public ChangeApplied addAxiom(OWLAxiom axiom) {
        return withWriteLockToObject(() -> delegate.addAxiom(axiom));
    }

    @Override
    public ChangeApplied addAxioms(Collection<? extends OWLAxiom> axioms) {
        return withWriteLockToObject(() -> delegate.addAxioms(axioms));
    }

    @Override
    public ChangeApplied addAxioms(OWLAxiom... axioms) {
        return withWriteLockToObject(() -> delegate.addAxioms(axioms));
    }

    @Override
    public ChangeApplied add(OWLAxiom axiom) {
        return withWriteLockToObject(() -> delegate.add(axiom));
    }

    @Override
    public ChangeApplied add(Collection<? extends OWLAxiom> axioms) {
        return withWriteLockToObject(() -> delegate.add(axioms));
    }

    @Override
    public ChangeApplied add(OWLAxiom... axioms) {
        return withWriteLockToObject(() -> delegate.add(axioms));
    }

    @Override
    public ChangeApplied removeAxiom(OWLAxiom axiom) {
        return withWriteLockToObject(() -> delegate.removeAxiom(axiom));
    }

    @Override
    public ChangeApplied removeAxioms(Collection<? extends OWLAxiom> axioms) {
        return withWriteLockToObject(() -> delegate.removeAxioms(axioms));
    }

    @Override
    public ChangeApplied removeAxioms(OWLAxiom... axioms) {
        return withWriteLockToObject(() -> delegate.removeAxioms(axioms));
    }

    @Override
    public ChangeApplied remove(OWLAxiom axiom) {
        return withWriteLockToObject(() -> delegate.remove(axiom));
    }

    @Override
    public ChangeApplied remove(Collection<? extends OWLAxiom> axioms) {
        return withWriteLockToObject(() -> delegate.remove(axioms));
    }

    @Override
    public ChangeApplied remove(OWLAxiom... axioms) {
        return withWriteLockToObject(() -> delegate.remove(axioms));
    }

    @Override
    public Stream<OWLAxiom> axioms(Imports imports) {
        return withReadLockToStream(() -> delegate.axioms(imports));
    }

    @Override
    public <R extends OWLAxiom> Stream<R> axioms(AxiomType<R> type, Imports imports) {
        return withReadLockToStream(() -> delegate.axioms(type, imports));
    }

    @Override
    public Stream<OWLLogicalAxiom> logicalAxioms(Imports imports) {
        return withReadLockToStream(() -> delegate.logicalAxioms(imports));
    }

    @Override
    public Stream<OWLAxiom> axiomsIgnoreAnnotations(OWLAxiom axiom, Imports imports) {
        return withReadLockToStream(() -> delegate.axiomsIgnoreAnnotations(axiom, imports));
    }

    @Override
    public Stream<OWLAxiom> referencingAxioms(OWLPrimitive primitive, Imports imports) {
        return withReadLockToStream(() -> delegate.referencingAxioms(primitive, imports));
    }

    @Override
    public Stream<OWLClassAxiom> axioms(OWLClass clazz, Imports imports) {
        return withReadLockToStream(() -> delegate.axioms(clazz, imports));
    }

    @Override
    public Stream<OWLDatatypeDefinitionAxiom> axioms(OWLDatatype datatype, Imports imports) {
        return withReadLockToStream(() -> delegate.axioms(datatype, imports));
    }

    @Override
    public Stream<OWLIndividualAxiom> axioms(OWLIndividual individual, Imports imports) {
        return withReadLockToStream(() -> delegate.axioms(individual, imports));
    }

    @Override
    public Stream<OWLAnnotationAxiom> axioms(OWLAnnotationProperty property, Imports imports) {
        return withReadLockToStream(() -> delegate.axioms(property, imports));
    }

    @Override
    public Stream<OWLObjectPropertyAxiom> axioms(OWLObjectPropertyExpression property, Imports imports) {
        return withReadLockToStream(() -> delegate.axioms(property, imports));
    }

    @Override
    public Stream<OWLDataPropertyAxiom> axioms(OWLDataProperty property, Imports imports) {
        return withReadLockToStream(() -> delegate.axioms(property, imports));
    }

    @Override
    public Stream<OWLAxiom> tboxAxioms(Imports imports) {
        return withReadLockToStream(() -> delegate.tboxAxioms(imports));
    }

    @Override
    public Stream<OWLAxiom> aboxAxioms(Imports imports) {
        return withReadLockToStream(() -> delegate.aboxAxioms(imports));
    }

    @Override
    public Stream<OWLAxiom> rboxAxioms(Imports imports) {
        return withReadLockToStream(() -> delegate.rboxAxioms(imports));
    }

    @Override
    public Stream<OWLEntity> signature(Imports imports) {
        return withReadLockToStream(() -> delegate.signature(imports));
    }

    @Override
    public Stream<OWLClass> classesInSignature(Imports imports) {
        return withReadLockToStream(() -> delegate.classesInSignature(imports));
    }

    @Override
    public Stream<OWLDatatype> datatypesInSignature(Imports imports) {
        return withReadLockToStream(() -> delegate.datatypesInSignature(imports));
    }

    @Override
    public Stream<OWLNamedIndividual> individualsInSignature(Imports imports) {
        return withReadLockToStream(() -> delegate.individualsInSignature(imports));
    }

    @Override
    public Stream<OWLObjectProperty> objectPropertiesInSignature(Imports imports) {
        return withReadLockToStream(() -> delegate.objectPropertiesInSignature(imports));
    }

    @Override
    public Stream<OWLDataProperty> dataPropertiesInSignature(Imports imports) {
        return withReadLockToStream(() -> delegate.dataPropertiesInSignature(imports));
    }

    @Override
    public Stream<OWLAnnotationProperty> annotationPropertiesInSignature(Imports imports) {
        return withReadLockToStream(() -> delegate.annotationPropertiesInSignature(imports));
    }

    @Override
    public Stream<OWLAnonymousIndividual> referencedAnonymousIndividuals(Imports imports) {
        return withReadLockToStream(() -> delegate.referencedAnonymousIndividuals(imports));
    }

    @Override
    public Stream<OWLEntity> entitiesInSignature(IRI iri, Imports imports) {
        return withReadLockToStream(() -> delegate.entitiesInSignature(iri, imports));
    }

    @Override
    public <T extends OWLAxiom> Stream<T> axioms(Class<T> type,
                                                 OWLObject object,
                                                 Imports imports,
                                                 Navigation navigation) {
        return withReadLockToStream(() -> delegate.axioms(type, object, imports, navigation));
    }

    @Override
    public <T extends OWLAxiom> Stream<T> axioms(OWLAxiomSearchFilter filter, Object key, Imports imports) {
        return withReadLockToStream(() -> delegate.axioms(filter, key, imports));
    }

    @Override
    public <T extends OWLAxiom> Stream<T> axioms(Class<T> type,
                                                 Class<? extends OWLObject> explicitClass,
                                                 OWLObject object,
                                                 Imports imports,
                                                 Navigation navigation) {
        return withReadLockToStream(() -> delegate.axioms(type, explicitClass, object, imports, navigation));
    }

    @Override
    public Stream<OWLAnnotationAssertionAxiom> annotationAssertionAxioms(OWLAnnotationSubject entity, Imports imports) {
        return withReadLockToStream(() -> delegate.annotationAssertionAxioms(entity, imports));
    }

    @Override
    public boolean isDeclared(OWLEntity entity, Imports imports) {
        return withReadLockToObject(() -> delegate.isDeclared(entity, imports));
    }

    @Override
    public boolean containsAxiom(OWLAxiom axiom,
                                 Imports imports,
                                 AxiomAnnotations annotations) {
        return withReadLockToObject(() -> delegate.containsAxiom(axiom, imports, annotations));
    }

    @Override
    public boolean containsEntitiesOfTypeInSignature(EntityType<?> type, Imports imports) {
        return withReadLockToObject(() -> delegate.containsEntitiesOfTypeInSignature(type, imports));
    }

    @Override
    public int getAxiomCount(Imports imports) {
        return withReadLockToObject(() -> delegate.getAxiomCount(imports));
    }

    @Override
    public <T extends OWLAxiom> int getAxiomCount(AxiomType<T> type, Imports imports) {
        return withReadLockToObject(() -> delegate.getAxiomCount(type, imports));
    }

    @Override
    public int getLogicalAxiomCount(Imports imports) {
        return withReadLockToObject(() -> delegate.getLogicalAxiomCount(imports));
    }

    @Override
    public List<OWLAnnotation> annotationsAsList() {
        return withReadLockToObject(delegate::annotationsAsList);
    }

    @Override
    public Set<IRI> getDirectImportsDocuments() {
        return withReadLockToObject(delegate::getDirectImportsDocuments);
    }

    @Override
    public Set<OWLOntology> getDirectImports() {
        return withReadLockToObject(delegate::getDirectImports);
    }

    @Override
    public Set<OWLOntology> getImports() {
        return withReadLockToObject(delegate::getImports);
    }

    @Override
    public Set<OWLOntology> getImportsClosure() {
        return withReadLockToObject(delegate::getImportsClosure);
    }

    @Override
    public Set<OWLImportsDeclaration> getImportsDeclarations() {
        return withReadLockToObject(delegate::getImportsDeclarations);
    }

    @Override
    public Set<OWLClassAxiom> getGeneralClassAxioms() {
        return withReadLockToObject(delegate::getGeneralClassAxioms);
    }

    @Override
    public Set<OWLEntity> getSignature() {
        return withReadLockToObject(delegate::getSignature);
    }

    @Override
    public Set<OWLClassExpression> getNestedClassExpressions() {
        return withReadLockToObject(delegate::getNestedClassExpressions);
    }

    @Override
    public Set<OWLAnonymousIndividual> getAnonymousIndividuals() {
        return withReadLockToObject(delegate::getAnonymousIndividuals);
    }

    @Override
    public Set<OWLClass> getClassesInSignature() {
        return withReadLockToObject(delegate::getClassesInSignature);
    }

    @Override
    public Set<OWLObjectProperty> getObjectPropertiesInSignature() {
        return withReadLockToObject(delegate::getObjectPropertiesInSignature);
    }

    @Override
    public Set<OWLDataProperty> getDataPropertiesInSignature() {
        return withReadLockToObject(delegate::getDataPropertiesInSignature);
    }

    @Override
    public Set<OWLNamedIndividual> getIndividualsInSignature() {
        return withReadLockToObject(delegate::getIndividualsInSignature);
    }

    @Override
    public Set<OWLDatatype> getDatatypesInSignature() {
        return withReadLockToObject(delegate::getDatatypesInSignature);
    }

    @Override
    public Set<OWLAnnotationProperty> getAnnotationPropertiesInSignature() {
        return withReadLockToObject(delegate::getAnnotationPropertiesInSignature);
    }

    @Override
    public Set<OWLAnnotation> getAnnotations() {
        return withReadLockToObject(delegate::getAnnotations);
    }

    @Override
    public Set<OWLAxiom> getAxioms() {
        return withReadLockToObject(delegate::getAxioms);
    }

    @Override
    public Set<OWLLogicalAxiom> getLogicalAxioms() {
        return withReadLockToObject(delegate::getLogicalAxioms);
    }

    @Override
    public Set<OWLAnonymousIndividual> getReferencedAnonymousIndividuals() {
        return withReadLockToObject(delegate::getReferencedAnonymousIndividuals);
    }

    @Override
    public Set<OWLEntity> getEntitiesInSignature(IRI iri) {
        return withReadLockToObject(() -> delegate.getEntitiesInSignature(iri));
    }

    @Override
    public Set<OWLAxiom> getAxiomsIgnoreAnnotations(OWLAxiom axiom) {
        return withReadLockToObject(() -> delegate.getAxiomsIgnoreAnnotations(axiom));
    }

    @Override
    public Set<OWLAxiom> getReferencingAxioms(OWLPrimitive primitive) {
        return withReadLockToObject(() -> delegate.getReferencingAxioms(primitive));
    }

    @Override
    public Set<OWLClassAxiom> getAxioms(OWLClass clazz) {
        return withReadLockToObject(() -> delegate.getAxioms(clazz));
    }

    @Override
    public Set<OWLObjectPropertyAxiom> getAxioms(OWLObjectPropertyExpression property) {
        return withReadLockToObject(() -> delegate.getAxioms(property));
    }

    @Override
    public Set<OWLDataPropertyAxiom> getAxioms(OWLDataProperty property) {
        return withReadLockToObject(() -> delegate.getAxioms(property));
    }

    @Override
    public Set<OWLIndividualAxiom> getAxioms(OWLIndividual individual) {
        return withReadLockToObject(() -> delegate.getAxioms(individual));
    }

    @Override
    public Set<OWLAnnotationAxiom> getAxioms(OWLAnnotationProperty property) {
        return withReadLockToObject(() -> delegate.getAxioms(property));
    }

    @Override
    public Set<OWLDatatypeDefinitionAxiom> getAxioms(OWLDatatype datatype) {
        return withReadLockToObject(() -> delegate.getAxioms(datatype));
    }

    @Override
    public Set<OWLDeclarationAxiom> getDeclarationAxioms(OWLEntity entity) {
        return withReadLockToObject(() -> delegate.getDeclarationAxioms(entity));
    }

    @Override
    public Set<OWLSubAnnotationPropertyOfAxiom> getSubAnnotationPropertyOfAxioms(OWLAnnotationProperty property) {
        return withReadLockToObject(() -> delegate.getSubAnnotationPropertyOfAxioms(property));
    }

    @Override
    public Set<OWLAnnotationPropertyDomainAxiom> getAnnotationPropertyDomainAxioms(OWLAnnotationProperty property) {
        return withReadLockToObject(() -> delegate.getAnnotationPropertyDomainAxioms(property));
    }

    @Override
    public Set<OWLAnnotationPropertyRangeAxiom> getAnnotationPropertyRangeAxioms(OWLAnnotationProperty property) {
        return withReadLockToObject(() -> delegate.getAnnotationPropertyRangeAxioms(property));
    }

    @Override
    public Set<OWLAnnotationAssertionAxiom> getAnnotationAssertionAxioms(OWLAnnotationSubject subject) {
        return withReadLockToObject(() -> delegate.getAnnotationAssertionAxioms(subject));
    }

    @Override
    public Set<OWLSubClassOfAxiom> getSubClassAxiomsForSubClass(OWLClass clazz) {
        return withReadLockToObject(() -> delegate.getSubClassAxiomsForSubClass(clazz));
    }

    @Override
    public Set<OWLSubClassOfAxiom> getSubClassAxiomsForSuperClass(OWLClass clazz) {
        return withReadLockToObject(() -> delegate.getSubClassAxiomsForSuperClass(clazz));
    }

    @Override
    public Set<OWLEquivalentClassesAxiom> getEquivalentClassesAxioms(OWLClass clazz) {
        return withReadLockToObject(() -> delegate.getEquivalentClassesAxioms(clazz));
    }

    @Override
    public Set<OWLDisjointClassesAxiom> getDisjointClassesAxioms(OWLClass clazz) {
        return withReadLockToObject(() -> delegate.getDisjointClassesAxioms(clazz));
    }

    @Override
    public Set<OWLDisjointUnionAxiom> getDisjointUnionAxioms(OWLClass clazz) {
        return withReadLockToObject(() -> delegate.getDisjointUnionAxioms(clazz));
    }

    @Override
    public Set<OWLHasKeyAxiom> getHasKeyAxioms(OWLClass clazz) {
        return withReadLockToObject(() -> delegate.getHasKeyAxioms(clazz));
    }

    @Override
    public Set<OWLSubObjectPropertyOfAxiom> getObjectSubPropertyAxiomsForSubProperty(OWLObjectPropertyExpression property) {
        return withReadLockToObject(() -> delegate.getObjectSubPropertyAxiomsForSubProperty(property));
    }

    @Override
    public Set<OWLSubObjectPropertyOfAxiom> getObjectSubPropertyAxiomsForSuperProperty(OWLObjectPropertyExpression property) {
        return withReadLockToObject(() -> delegate.getObjectSubPropertyAxiomsForSuperProperty(property));
    }

    @Override
    public Set<OWLObjectPropertyDomainAxiom> getObjectPropertyDomainAxioms(OWLObjectPropertyExpression property) {
        return withReadLockToObject(() -> delegate.getObjectPropertyDomainAxioms(property));
    }

    @Override
    public Set<OWLObjectPropertyRangeAxiom> getObjectPropertyRangeAxioms(OWLObjectPropertyExpression property) {
        return withReadLockToObject(() -> delegate.getObjectPropertyRangeAxioms(property));
    }

    @Override
    public Set<OWLInverseObjectPropertiesAxiom> getInverseObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        return withReadLockToObject(() -> delegate.getInverseObjectPropertyAxioms(property));
    }

    @Override
    public Set<OWLEquivalentObjectPropertiesAxiom> getEquivalentObjectPropertiesAxioms(OWLObjectPropertyExpression property) {
        return withReadLockToObject(() -> delegate.getEquivalentObjectPropertiesAxioms(property));
    }

    @Override
    public Set<OWLDisjointObjectPropertiesAxiom> getDisjointObjectPropertiesAxioms(OWLObjectPropertyExpression property) {
        return withReadLockToObject(() -> delegate.getDisjointObjectPropertiesAxioms(property));
    }

    @Override
    public Set<OWLFunctionalObjectPropertyAxiom> getFunctionalObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        return withReadLockToObject(() -> delegate.getFunctionalObjectPropertyAxioms(property));
    }

    @Override
    public Set<OWLInverseFunctionalObjectPropertyAxiom> getInverseFunctionalObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        return withReadLockToObject(() -> delegate.getInverseFunctionalObjectPropertyAxioms(property));
    }

    @Override
    public Set<OWLSymmetricObjectPropertyAxiom> getSymmetricObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        return withReadLockToObject(() -> delegate.getSymmetricObjectPropertyAxioms(property));
    }

    @Override
    public Set<OWLAsymmetricObjectPropertyAxiom> getAsymmetricObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        return withReadLockToObject(() -> delegate.getAsymmetricObjectPropertyAxioms(property));
    }

    @Override
    public Set<OWLReflexiveObjectPropertyAxiom> getReflexiveObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        return withReadLockToObject(() -> delegate.getReflexiveObjectPropertyAxioms(property));
    }

    @Override
    public Set<OWLIrreflexiveObjectPropertyAxiom> getIrreflexiveObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        return withReadLockToObject(() -> delegate.getIrreflexiveObjectPropertyAxioms(property));
    }

    @Override
    public Set<OWLTransitiveObjectPropertyAxiom> getTransitiveObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        return withReadLockToObject(() -> delegate.getTransitiveObjectPropertyAxioms(property));
    }

    @Override
    public Set<OWLSubDataPropertyOfAxiom> getDataSubPropertyAxiomsForSubProperty(OWLDataProperty property) {
        return withReadLockToObject(() -> delegate.getDataSubPropertyAxiomsForSubProperty(property));
    }

    @Override
    public Set<OWLSubDataPropertyOfAxiom> getDataSubPropertyAxiomsForSuperProperty(OWLDataPropertyExpression property) {
        return withReadLockToObject(() -> delegate.getDataSubPropertyAxiomsForSuperProperty(property));
    }

    @Override
    public Set<OWLDataPropertyDomainAxiom> getDataPropertyDomainAxioms(OWLDataProperty property) {
        return withReadLockToObject(() -> delegate.getDataPropertyDomainAxioms(property));
    }

    @Override
    public Set<OWLDataPropertyRangeAxiom> getDataPropertyRangeAxioms(OWLDataProperty property) {
        return withReadLockToObject(() -> delegate.getDataPropertyRangeAxioms(property));
    }

    @Override
    public Set<OWLEquivalentDataPropertiesAxiom> getEquivalentDataPropertiesAxioms(OWLDataProperty property) {
        return withReadLockToObject(() -> delegate.getEquivalentDataPropertiesAxioms(property));
    }

    @Override
    public Set<OWLDisjointDataPropertiesAxiom> getDisjointDataPropertiesAxioms(OWLDataProperty property) {
        return withReadLockToObject(() -> delegate.getDisjointDataPropertiesAxioms(property));
    }

    @Override
    public Set<OWLFunctionalDataPropertyAxiom> getFunctionalDataPropertyAxioms(OWLDataPropertyExpression property) {
        return withReadLockToObject(() -> delegate.getFunctionalDataPropertyAxioms(property));
    }

    @Override
    public Set<OWLClassAssertionAxiom> getClassAssertionAxioms(OWLIndividual individual) {
        return withReadLockToObject(() -> delegate.getClassAssertionAxioms(individual));
    }

    @Override
    public Set<OWLClassAssertionAxiom> getClassAssertionAxioms(OWLClassExpression clazz) {
        return withReadLockToObject(() -> delegate.getClassAssertionAxioms(clazz));
    }

    @Override
    public Set<OWLDataPropertyAssertionAxiom> getDataPropertyAssertionAxioms(OWLIndividual individual) {
        return withReadLockToObject(() -> delegate.getDataPropertyAssertionAxioms(individual));
    }

    @Override
    public Set<OWLObjectPropertyAssertionAxiom> getObjectPropertyAssertionAxioms(OWLIndividual individual) {
        return withReadLockToObject(() -> delegate.getObjectPropertyAssertionAxioms(individual));
    }

    @Override
    public Set<OWLNegativeObjectPropertyAssertionAxiom> getNegativeObjectPropertyAssertionAxioms(OWLIndividual individual) {
        return withReadLockToObject(() -> delegate.getNegativeObjectPropertyAssertionAxioms(individual));
    }

    @Override
    public Set<OWLNegativeDataPropertyAssertionAxiom> getNegativeDataPropertyAssertionAxioms(OWLIndividual individual) {
        return withReadLockToObject(() -> delegate.getNegativeDataPropertyAssertionAxioms(individual));
    }

    @Override
    public Set<OWLSameIndividualAxiom> getSameIndividualAxioms(OWLIndividual individual) {
        return withReadLockToObject(() -> delegate.getSameIndividualAxioms(individual));
    }

    @Override
    public Set<OWLDifferentIndividualsAxiom> getDifferentIndividualAxioms(OWLIndividual individual) {
        return withReadLockToObject(() -> delegate.getDifferentIndividualAxioms(individual));
    }

    @Override
    public Set<OWLDatatypeDefinitionAxiom> getDatatypeDefinitions(OWLDatatype datatype) {
        return withReadLockToObject(() -> delegate.getDatatypeDefinitions(datatype));
    }

    @Override
    public Set<OWLAnnotation> getAnnotations(OWLAnnotationProperty property) {
        return withReadLockToObject(() -> delegate.getAnnotations(property));
    }

    @Override
    public <T extends OWLAxiom> Set<T> getAxioms(AxiomType<T> type) {
        return withReadLockToObject(() -> delegate.getAxioms(type));
    }

    @Override
    public <T extends OWLAxiom> Set<T> getAxioms(Class<T> type,
                                                 Class<? extends OWLObject> classType,
                                                 OWLObject entity,
                                                 Navigation navigation) {
        return withReadLockToObject(() -> delegate.getAxioms(type, classType, entity, navigation));
    }

    @Override
    public <T extends OWLAxiom> Set<T> getAxioms(Class<T> type, OWLObject entity, Navigation navigation) {
        return withReadLockToObject(() -> delegate.getAxioms(type, entity, navigation));
    }

    @Override
    public <T extends OWLAxiom> Collection<T> filterAxioms(OWLAxiomSearchFilter filter, Object key) {
        return withReadLockToObject(() -> delegate.filterAxioms(filter, key));
    }

    @Override
    public Set<OWLAxiom> getAxioms(Imports imports) {
        return withReadLockToObject(() -> delegate.getAxioms(imports));
    }

    @Override
    public Set<OWLLogicalAxiom> getLogicalAxioms(Imports imports) {
        return withReadLockToObject(() -> delegate.getLogicalAxioms(imports));
    }

    @Override
    public Set<OWLEntity> getSignature(Imports imports) {
        return withReadLockToObject(() -> delegate.getSignature(imports));
    }

    @Override
    public Set<OWLAxiom> getTBoxAxioms(Imports imports) {
        return withReadLockToObject(() -> delegate.getTBoxAxioms(imports));
    }

    @Override
    public Set<OWLAxiom> getABoxAxioms(Imports imports) {
        return withReadLockToObject(() -> delegate.getABoxAxioms(imports));
    }

    @Override
    public Set<OWLAxiom> getRBoxAxioms(Imports imports) {
        return withReadLockToObject(() -> delegate.getRBoxAxioms(imports));
    }

    @Override
    public Set<OWLClass> getClassesInSignature(Imports imports) {
        return withReadLockToObject(() -> delegate.getClassesInSignature(imports));
    }

    @Override
    public Set<OWLDatatype> getDatatypesInSignature(Imports imports) {
        return withReadLockToObject(() -> delegate.getDatatypesInSignature(imports));
    }

    @Override
    public Set<OWLNamedIndividual> getIndividualsInSignature(Imports imports) {
        return withReadLockToObject(() -> delegate.getIndividualsInSignature(imports));
    }

    @Override
    public Set<OWLObjectProperty> getObjectPropertiesInSignature(Imports imports) {
        return withReadLockToObject(() -> delegate.getObjectPropertiesInSignature(imports));
    }

    @Override
    public Set<OWLDataProperty> getDataPropertiesInSignature(Imports imports) {
        return withReadLockToObject(() -> delegate.getDataPropertiesInSignature(imports));
    }

    @Override
    public Set<OWLAnnotationProperty> getAnnotationPropertiesInSignature(Imports imports) {
        return withReadLockToObject(() -> delegate.getAnnotationPropertiesInSignature(imports));
    }

    @Override
    public Set<OWLAnonymousIndividual> getReferencedAnonymousIndividuals(Imports imports) {
        return withReadLockToObject(() -> delegate.getReferencedAnonymousIndividuals(imports));
    }

    @Override
    public <T extends OWLAxiom> Set<T> getAxioms(AxiomType<T> type, Imports imports) {
        return withReadLockToObject(() -> delegate.getAxioms(type, imports));
    }

    @Override
    public Set<OWLAxiom> getAxiomsIgnoreAnnotations(OWLAxiom axiom, Imports imports) {
        return withReadLockToObject(() -> delegate.getAxiomsIgnoreAnnotations(axiom, imports));
    }

    @Override
    public Set<OWLAxiom> getReferencingAxioms(OWLPrimitive primitive, Imports imports) {
        return withReadLockToObject(() -> delegate.getReferencingAxioms(primitive, imports));
    }

    @Override
    public Set<OWLClassAxiom> getAxioms(OWLClass clazz, Imports imports) {
        return withReadLockToObject(() -> delegate.getAxioms(clazz, imports));
    }

    @Override
    public Set<OWLObjectPropertyAxiom> getAxioms(OWLObjectPropertyExpression property, Imports imports) {
        return withReadLockToObject(() -> delegate.getAxioms(property, imports));
    }

    @Override
    public Set<OWLDataPropertyAxiom> getAxioms(OWLDataProperty property, Imports imports) {
        return withReadLockToObject(() -> delegate.getAxioms(property, imports));
    }

    @Override
    public Set<OWLIndividualAxiom> getAxioms(OWLIndividual individual, Imports imports) {
        return withReadLockToObject(() -> delegate.getAxioms(individual, imports));
    }

    @Override
    public Set<OWLAnnotationAxiom> getAxioms(OWLAnnotationProperty property, Imports imports) {
        return withReadLockToObject(() -> delegate.getAxioms(property, imports));
    }

    @Override
    public Set<OWLDatatypeDefinitionAxiom> getAxioms(OWLDatatype datatype, Imports imports) {
        return withReadLockToObject(() -> delegate.getAxioms(datatype, imports));
    }

    @Override
    public Set<OWLEntity> getEntitiesInSignature(IRI iri, Imports imports) {
        return withReadLockToObject(() -> delegate.getEntitiesInSignature(iri, imports));
    }

    @Override
    public Set<OWLAnnotationAssertionAxiom> getAnnotationAssertionAxioms(OWLAnnotationSubject entity, Imports imports) {
        return withReadLockToObject(() -> delegate.getAnnotationAssertionAxioms(entity, imports));
    }

    @Override
    public <T extends OWLAxiom> Set<T> getAxioms(Class<T> type,
                                                 OWLObject object,
                                                 Imports imports,
                                                 Navigation navigation) {
        return withReadLockToObject(() -> delegate.getAxioms(type, object, imports, navigation));
    }

    @Override
    public <T extends OWLAxiom> Collection<T> filterAxioms(OWLAxiomSearchFilter filter, Object o, Imports imports) {
        return withReadLockToObject(() -> delegate.filterAxioms(filter, o, imports));
    }

    @Override
    public <T extends OWLAxiom> Set<T> getAxioms(Class<T> type,
                                                 Class<? extends OWLObject> explicitClass,
                                                 OWLObject object,
                                                 Imports imports,
                                                 Navigation navigation) {
        return withReadLockToObject(() -> delegate.getAxioms(type, explicitClass, object, imports, navigation));
    }

    @Override
    public boolean contains(OWLAxiomSearchFilter filter, Object o, Imports imports) {
        return withReadLockToObject(() -> delegate.contains(filter, o, imports));
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean containsReference(OWLEntity entity, Imports imports) {
        return withReadLockToObject(() -> delegate.containsReference(entity, imports));
    }

    @Override
    public boolean containsEntityInSignature(OWLEntity entity, Imports imports) {
        return withReadLockToObject(() -> delegate.containsEntityInSignature(entity, imports));
    }

    @Override
    public boolean containsEntityInSignature(IRI iri, Imports imports) {
        return withReadLockToObject(() -> delegate.containsEntityInSignature(iri, imports));
    }

    @Override
    public boolean containsClassInSignature(IRI iri, Imports imports) {
        return withReadLockToObject(() -> delegate.containsClassInSignature(iri, imports));
    }

    @Override
    public boolean containsObjectPropertyInSignature(IRI iri, Imports imports) {
        return withReadLockToObject(() -> delegate.containsObjectPropertyInSignature(iri, imports));
    }

    @Override
    public boolean containsDataPropertyInSignature(IRI iri, Imports imports) {
        return withReadLockToObject(() -> delegate.containsDataPropertyInSignature(iri, imports));
    }

    @Override
    public boolean containsAnnotationPropertyInSignature(IRI iri, Imports imports) {
        return withReadLockToObject(() -> delegate.containsAnnotationPropertyInSignature(iri, imports));
    }

    @Override
    public boolean containsDatatypeInSignature(IRI iri, Imports imports) {
        return withReadLockToObject(() -> delegate.containsDatatypeInSignature(iri, imports));
    }

    @Override
    public boolean containsIndividualInSignature(IRI iri, Imports imports) {
        return withReadLockToObject(() -> delegate.containsIndividualInSignature(iri, imports));
    }

    @FunctionalInterface
    protected interface WithThrowable<T extends Throwable> {
        void apply() throws T;
    }

}