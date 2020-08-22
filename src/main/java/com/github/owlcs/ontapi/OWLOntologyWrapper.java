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
package com.github.owlcs.ontapi;

import org.semanticweb.owlapi.io.OWLOntologyDocumentTarget;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.ChangeApplied;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.model.parameters.Navigation;
import org.semanticweb.owlapi.util.OWLAxiomSearchFilter;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Predicate;
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
public class OWLOntologyWrapper extends RWLockedOntology {

    protected final OWLOntology delegate;

    /**
     * Constructs a new {@code OWLOntologyWrapper}
     * that provides concurrent access to the specified delegate {@link OWLOntology ontology}.
     *
     * @param delegate {@link OWLOntology} the delegate
     * @param lock     {@link ReadWriteLock} the R/W Lock to provide thread-safe or {@code null} for non-concurrent mode
     */
    public OWLOntologyWrapper(OWLOntology delegate, ReadWriteLock lock) {
        super(lock);
        this.delegate = Objects.requireNonNull(delegate, "Null delegate");
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
        return withReadLockToObject(delegate::directImportsDocuments);
    }

    @Override
    public Stream<OWLOntology> directImports() {
        return withReadLockToObject(delegate::directImports);
    }

    @Override
    public Stream<OWLOntology> imports() {
        return withReadLockToObject(() -> getOWLOntologyManager().imports(this));
    }

    @Override
    public Stream<OWLOntology> importsClosure() {
        return withReadLockToObject(() -> getOWLOntologyManager().importsClosure(this));
    }

    @Override
    public Stream<OWLClassAxiom> generalClassAxioms() {
        return withReadLockToObject(delegate::generalClassAxioms);
    }

    @Override
    public Stream<OWLEntity> signature() {
        return withReadLockToObject(delegate::signature);
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
        return withReadLockToObject(delegate::axioms);
    }

    @Override
    public Stream<OWLLogicalAxiom> logicalAxioms() {
        return withReadLockToObject(delegate::logicalAxioms);
    }

    @Override
    public <T extends OWLAxiom> Stream<T> axioms(AxiomType<T> type) {
        return withReadLockToObject(() -> delegate.axioms(type));
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
        return withReadLockToObject(() -> delegate.axiomsIgnoreAnnotations(axiom));
    }

    @Override
    public Stream<OWLAxiom> referencingAxioms(OWLPrimitive primitive) {
        return withReadLockToObject(() -> delegate.referencingAxioms(primitive));
    }

    @Override
    public Stream<OWLClassAxiom> axioms(OWLClass clazz) {
        return withReadLockToObject(() -> delegate.axioms(clazz));
    }

    @Override
    public Stream<OWLObjectPropertyAxiom> axioms(OWLObjectPropertyExpression property) {
        return withReadLockToObject(() -> delegate.axioms(property));
    }

    @Override
    public Stream<OWLDataPropertyAxiom> axioms(OWLDataProperty property) {
        return withReadLockToObject(() -> delegate.axioms(property));
    }

    @Override
    public Stream<OWLIndividualAxiom> axioms(OWLIndividual individual) {
        return withReadLockToObject(() -> delegate.axioms(individual));
    }

    @Override
    public Stream<OWLAnnotationAxiom> axioms(OWLAnnotationProperty property) {
        return withReadLockToObject(() -> delegate.axioms(property));
    }

    @Override
    public Stream<OWLDatatypeDefinitionAxiom> axioms(OWLDatatype datatype) {
        return withReadLockToObject(() -> delegate.axioms(datatype));
    }

    @Override
    public Stream<OWLAnonymousIndividual> referencedAnonymousIndividuals() {
        return withReadLockToObject(delegate::referencedAnonymousIndividuals);
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
        return withReadLockToObject(() -> delegate.entitiesInSignature(iri));
    }

    @Override
    public boolean contains(OWLAxiomSearchFilter filter, Object o) {
        return withReadLockToObject(() -> delegate.contains(filter, o));
    }

    @Override
    public Stream<OWLAnnotationPropertyDomainAxiom> annotationPropertyDomainAxioms(OWLAnnotationProperty property) {
        return withReadLockToObject(() -> delegate.annotationPropertyDomainAxioms(property));
    }

    @Override
    public Stream<OWLAnnotationPropertyRangeAxiom> annotationPropertyRangeAxioms(OWLAnnotationProperty property) {
        return withReadLockToObject(() -> delegate.annotationPropertyRangeAxioms(property));
    }

    @Override
    public Stream<OWLImportsDeclaration> importsDeclarations() {
        return withReadLockToObject(delegate::importsDeclarations);
    }

    @Override
    public <T extends OWLAxiom> Stream<T> axioms(OWLAxiomSearchFilter filter, Object key) {
        return withReadLockToObject(() -> delegate.axioms(filter, key));
    }

    @Override
    public <T extends OWLAxiom> Stream<T> axioms(Class<T> type,
                                                 Class<? extends OWLObject> explicitType,
                                                 OWLObject entity,
                                                 Navigation navigation) {
        return withReadLockToObject(() -> delegate.axioms(type, explicitType, entity, navigation));
    }

    @Override
    public Stream<OWLSubAnnotationPropertyOfAxiom> subAnnotationPropertyOfAxioms(OWLAnnotationProperty property) {
        return withReadLockToObject(() -> delegate.subAnnotationPropertyOfAxioms(property));
    }

    @Override
    public Stream<OWLDatatypeDefinitionAxiom> datatypeDefinitions(OWLDatatype datatype) {
        return withReadLockToObject(() -> delegate.datatypeDefinitions(datatype));
    }

    @Override
    public Stream<OWLDisjointObjectPropertiesAxiom> disjointObjectPropertiesAxioms(OWLObjectPropertyExpression property) {
        return withReadLockToObject(() -> delegate.disjointObjectPropertiesAxioms(property));
    }

    @Override
    public Stream<OWLObjectProperty> objectPropertiesInSignature() {
        return withReadLockToObject(delegate::objectPropertiesInSignature);
    }

    @Override
    public Stream<OWLAnnotationAssertionAxiom> annotationAssertionAxioms(OWLAnnotationSubject entity) {
        return withReadLockToObject(() -> delegate.annotationAssertionAxioms(entity));
    }

    @Override
    public Stream<OWLAnnotationProperty> annotationPropertiesInSignature() {
        return withReadLockToObject(delegate::annotationPropertiesInSignature);
    }

    @Override
    public Stream<OWLAnnotation> annotations() {
        return withReadLockToObject(delegate::annotations);
    }

    @Override
    public Stream<OWLAnnotation> annotations(OWLAnnotationProperty p) {
        return withReadLockToObject(() -> delegate.annotations(p));
    }

    @Override
    public Stream<OWLAnnotation> annotations(Predicate<OWLAnnotation> p) {
        return withReadLockToObject(() -> delegate.annotations(p));
    }

    @Override
    public Stream<OWLAnonymousIndividual> anonymousIndividuals() {
        return withReadLockToObject(delegate::anonymousIndividuals);
    }

    @Override
    public Stream<OWLAsymmetricObjectPropertyAxiom> asymmetricObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        return withReadLockToObject(() -> delegate.asymmetricObjectPropertyAxioms(property));
    }

    @Override
    public <T extends OWLAxiom> Stream<T> axioms(Class<T> type, OWLObject entity, Navigation navigation) {
        return withReadLockToObject(() -> delegate.axioms(type, entity, navigation));
    }

    @Override
    public Stream<OWLClassAssertionAxiom> classAssertionAxioms(OWLClassExpression ce) {
        return withReadLockToObject(() -> delegate.classAssertionAxioms(ce));
    }

    @Override
    public Stream<OWLClassAssertionAxiom> classAssertionAxioms(OWLIndividual individual) {
        return withReadLockToObject(() -> delegate.classAssertionAxioms(individual));
    }

    @Override
    public Stream<OWLClass> classesInSignature() {
        return withReadLockToObject(delegate::classesInSignature);
    }

    @Override
    public Stream<OWLDataProperty> dataPropertiesInSignature() {
        return withReadLockToObject(delegate::dataPropertiesInSignature);
    }

    @Override
    public Stream<OWLDataPropertyAssertionAxiom> dataPropertyAssertionAxioms(OWLIndividual individual) {
        return withReadLockToObject(() -> delegate.dataPropertyAssertionAxioms(individual));
    }

    @Override
    public Stream<OWLDataPropertyDomainAxiom> dataPropertyDomainAxioms(OWLDataProperty property) {
        return withReadLockToObject(() -> delegate.dataPropertyDomainAxioms(property));
    }

    @Override
    public Stream<OWLDataPropertyRangeAxiom> dataPropertyRangeAxioms(OWLDataProperty property) {
        return withReadLockToObject(() -> delegate.dataPropertyRangeAxioms(property));
    }

    @Override
    public Stream<OWLSubDataPropertyOfAxiom> dataSubPropertyAxiomsForSubProperty(OWLDataProperty property) {
        return withReadLockToObject(() -> delegate.dataSubPropertyAxiomsForSubProperty(property));
    }

    @Override
    public Stream<OWLSubDataPropertyOfAxiom> dataSubPropertyAxiomsForSuperProperty(OWLDataPropertyExpression property) {
        return withReadLockToObject(() -> delegate.dataSubPropertyAxiomsForSuperProperty(property));
    }

    @Override
    public Stream<OWLDatatype> datatypesInSignature() {
        return withReadLockToObject(delegate::datatypesInSignature);
    }

    @Override
    public Stream<OWLDeclarationAxiom> declarationAxioms(OWLEntity subject) {
        return withReadLockToObject(() -> delegate.declarationAxioms(subject));
    }

    @Override
    public Stream<OWLDifferentIndividualsAxiom> differentIndividualAxioms(OWLIndividual individual) {
        return withReadLockToObject(() -> delegate.differentIndividualAxioms(individual));
    }

    @Override
    public Stream<OWLDisjointClassesAxiom> disjointClassesAxioms(OWLClass clazz) {
        return withReadLockToObject(() -> delegate.disjointClassesAxioms(clazz));
    }

    @Override
    public Stream<OWLDisjointDataPropertiesAxiom> disjointDataPropertiesAxioms(OWLDataProperty property) {
        return withReadLockToObject(() -> delegate.disjointDataPropertiesAxioms(property));
    }

    @Override
    public Stream<OWLDisjointUnionAxiom> disjointUnionAxioms(OWLClass clazz) {
        return withReadLockToObject(() -> delegate.disjointUnionAxioms(clazz));
    }

    @Override
    public Stream<OWLEquivalentClassesAxiom> equivalentClassesAxioms(OWLClass clazz) {
        return withReadLockToObject(() -> delegate.equivalentClassesAxioms(clazz));
    }

    @Override
    public Stream<OWLEquivalentDataPropertiesAxiom> equivalentDataPropertiesAxioms(OWLDataProperty property) {
        return withReadLockToObject(() -> delegate.equivalentDataPropertiesAxioms(property));
    }

    @Override
    public Stream<OWLEquivalentObjectPropertiesAxiom> equivalentObjectPropertiesAxioms(OWLObjectPropertyExpression property) {
        return withReadLockToObject(() -> delegate.equivalentObjectPropertiesAxioms(property));
    }

    @Override
    public Stream<OWLFunctionalDataPropertyAxiom> functionalDataPropertyAxioms(OWLDataPropertyExpression property) {
        return withReadLockToObject(() -> delegate.functionalDataPropertyAxioms(property));
    }

    @Override
    public Stream<OWLFunctionalObjectPropertyAxiom> functionalObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        return withReadLockToObject(() -> delegate.functionalObjectPropertyAxioms(property));
    }

    @Override
    public Stream<OWLHasKeyAxiom> hasKeyAxioms(OWLClass clazz) {
        return withReadLockToObject(() -> delegate.hasKeyAxioms(clazz));
    }

    @Override
    public Stream<OWLNamedIndividual> individualsInSignature() {
        return withReadLockToObject(delegate::individualsInSignature);
    }

    @Override
    public Stream<OWLInverseFunctionalObjectPropertyAxiom> inverseFunctionalObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        return withReadLockToObject(() -> delegate.inverseFunctionalObjectPropertyAxioms(property));
    }

    @Override
    public Stream<OWLInverseObjectPropertiesAxiom> inverseObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        return withReadLockToObject(() -> delegate.inverseObjectPropertyAxioms(property));
    }

    @Override
    public Stream<OWLIrreflexiveObjectPropertyAxiom> irreflexiveObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        return withReadLockToObject(() -> delegate.irreflexiveObjectPropertyAxioms(property));
    }

    @Override
    public Stream<OWLNegativeDataPropertyAssertionAxiom> negativeDataPropertyAssertionAxioms(OWLIndividual individual) {
        return withReadLockToObject(() -> delegate.negativeDataPropertyAssertionAxioms(individual));
    }

    @Override
    public Stream<OWLNegativeObjectPropertyAssertionAxiom> negativeObjectPropertyAssertionAxioms(OWLIndividual individual) {
        return withReadLockToObject(() -> delegate.negativeObjectPropertyAssertionAxioms(individual));
    }

    @Override
    public Stream<OWLClassExpression> nestedClassExpressions() {
        return withReadLockToObject(delegate::nestedClassExpressions);
    }

    @Override
    public Stream<OWLObjectPropertyAssertionAxiom> objectPropertyAssertionAxioms(OWLIndividual individual) {
        return withReadLockToObject(() -> delegate.objectPropertyAssertionAxioms(individual));
    }

    @Override
    public Stream<OWLObjectPropertyDomainAxiom> objectPropertyDomainAxioms(OWLObjectPropertyExpression property) {
        return withReadLockToObject(() -> delegate.objectPropertyDomainAxioms(property));
    }

    @Override
    public Stream<OWLObjectPropertyRangeAxiom> objectPropertyRangeAxioms(OWLObjectPropertyExpression property) {
        return withReadLockToObject(() -> delegate.objectPropertyRangeAxioms(property));
    }

    @Override
    public Stream<OWLSubObjectPropertyOfAxiom> objectSubPropertyAxiomsForSubProperty(OWLObjectPropertyExpression property) {
        return withReadLockToObject(() -> delegate.objectSubPropertyAxiomsForSubProperty(property));
    }

    @Override
    public Stream<OWLSubObjectPropertyOfAxiom> objectSubPropertyAxiomsForSuperProperty(OWLObjectPropertyExpression property) {
        return withReadLockToObject(() -> delegate.objectSubPropertyAxiomsForSuperProperty(property));
    }

    @Override
    public Stream<OWLReflexiveObjectPropertyAxiom> reflexiveObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        return withReadLockToObject(() -> delegate.reflexiveObjectPropertyAxioms(property));
    }

    @Override
    public Stream<OWLSameIndividualAxiom> sameIndividualAxioms(OWLIndividual individual) {
        return withReadLockToObject(() -> delegate.sameIndividualAxioms(individual));
    }

    @Override
    public Stream<OWLSubClassOfAxiom> subClassAxiomsForSubClass(OWLClass clazz) {
        return withReadLockToObject(() -> delegate.subClassAxiomsForSubClass(clazz));
    }

    @Override
    public Stream<OWLSubClassOfAxiom> subClassAxiomsForSuperClass(OWLClass clazz) {
        return withReadLockToObject(() -> delegate.subClassAxiomsForSuperClass(clazz));
    }

    @Override
    public Stream<OWLSymmetricObjectPropertyAxiom> symmetricObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        return withReadLockToObject(() -> delegate.symmetricObjectPropertyAxioms(property));
    }

    @Override
    public Stream<OWLTransitiveObjectPropertyAxiom> transitiveObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        return withReadLockToObject(() -> delegate.transitiveObjectPropertyAxioms(property));
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

}