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

import org.semanticweb.owlapi.io.OWLOntologyDocumentTarget;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.AxiomAnnotations;
import org.semanticweb.owlapi.model.parameters.ChangeApplied;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.model.parameters.Navigation;
import org.semanticweb.owlapi.util.OWLAxiomSearchFilter;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Matthew Horridge Stanford Center for Biomedical Informatics Research 03/04/15
 * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/concurrent/ConcurrentOWLOntologyImpl.java'>uk.ac.manchester.cs.owl.owlapi.concurrent.ConcurrentOWLOntologyImpl</a>
 */
@SuppressWarnings({"deprecation"})
public class ConcurrentOWLOntologyImpl implements OWLMutableOntology {

    protected final OWLOntology delegate;
    protected final ReadWriteLock lock;

    /**
     * Constructs a ConcurrentOWLOntology that provides concurrent access to a delegate
     * {@link OWLOntology}.
     *
     * @param delegate      The delegate {@link OWLOntology}.
     * @param readWriteLock The {@link java.util.concurrent.locks.ReadWriteLock} that will provide
     *                      the locking.
     * @throws java.lang.NullPointerException if any parameters are {@code null}.
     */
    @Inject
    public ConcurrentOWLOntologyImpl(OWLOntology delegate, ReadWriteLock readWriteLock) {
        this.delegate = Objects.requireNonNull(delegate);
        this.lock = Objects.requireNonNull(readWriteLock);
    }

    @Override
    public int typeIndex() {
        return delegate.typeIndex();
    }

    @Override
    public void accept(OWLNamedObjectVisitor owlNamedObjectVisitor) {
        delegate.accept(owlNamedObjectVisitor);
    }

    @Override
    public <O> O accept(OWLNamedObjectVisitorEx<O> owlNamedObjectVisitorEx) {
        return delegate.accept(owlNamedObjectVisitorEx);
    }

    @Override
    public int hashCode() {
        lock.readLock().lock();
        try {
            return delegate.hashCode();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        lock.readLock().lock();
        try {
            return delegate.equals(obj);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public OWLOntologyManager getOWLOntologyManager() {
        lock.readLock().lock();
        try {
            return delegate.getOWLOntologyManager();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void setOWLOntologyManager(@Nullable OWLOntologyManager owlOntologyManager) {
        lock.writeLock().lock();
        try {
            delegate.setOWLOntologyManager(owlOntologyManager);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public OWLOntologyID getOntologyID() {
        lock.readLock().lock();
        try {
            return delegate.getOntologyID();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean isAnonymous() {
        lock.readLock().lock();
        try {
            return delegate.isAnonymous();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLAnnotation> getAnnotations() {
        lock.readLock().lock();
        try {
            return delegate.getAnnotations();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<IRI> getDirectImportsDocuments() {
        lock.readLock().lock();
        try {
            return delegate.getDirectImportsDocuments();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<IRI> directImportsDocuments() {
        lock.readLock().lock();
        try {
            return delegate.directImportsDocuments();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLOntology> getDirectImports() {
        lock.readLock().lock();
        try {
            return delegate.getDirectImports();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLOntology> directImports() {
        lock.readLock().lock();
        try {
            return delegate.directImports();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLOntology> getImports() {
        lock.readLock().lock();
        try {
            return delegate.getImports();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLOntology> imports() {
        lock.readLock().lock();
        try {
            return delegate.imports();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLOntology> getImportsClosure() {
        lock.readLock().lock();
        try {
            return delegate.getImportsClosure();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLOntology> importsClosure() {
        lock.readLock().lock();
        try {
            return delegate.importsClosure();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLImportsDeclaration> getImportsDeclarations() {
        lock.readLock().lock();
        try {
            return delegate.getImportsDeclarations();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        lock.readLock().lock();
        try {
            return delegate.isEmpty();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLAxiom> getTBoxAxioms(Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.getTBoxAxioms(imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLAxiom> getABoxAxioms(Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.getABoxAxioms(imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLAxiom> getRBoxAxioms(Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.getRBoxAxioms(imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLAxiom> tboxAxioms(Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.tboxAxioms(imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLAxiom> aboxAxioms(Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.aboxAxioms(imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLAxiom> rboxAxioms(Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.rboxAxioms(imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLClassAxiom> getGeneralClassAxioms() {
        lock.readLock().lock();
        try {
            return delegate.getGeneralClassAxioms();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLEntity> getSignature() {
        lock.readLock().lock();
        try {
            return delegate.getSignature();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLEntity> getSignature(Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.getSignature(imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLClassAxiom> generalClassAxioms() {
        lock.readLock().lock();
        try {
            return delegate.generalClassAxioms();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLEntity> signature() {
        lock.readLock().lock();
        try {
            return delegate.signature();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLEntity> signature(Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.signature(imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean isDeclared(OWLEntity owlEntity) {
        lock.readLock().lock();
        try {
            return delegate.isDeclared(owlEntity);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean isDeclared(OWLEntity owlEntity, Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.isDeclared(owlEntity, imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void saveOntology() throws OWLOntologyStorageException {
        lock.readLock().lock();
        try {
            delegate.saveOntology();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void saveOntology(IRI iri) throws OWLOntologyStorageException {
        lock.readLock().lock();
        try {
            delegate.saveOntology(iri);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void saveOntology(OutputStream outputStream) throws OWLOntologyStorageException {
        lock.readLock().lock();
        try {
            delegate.saveOntology(outputStream);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void saveOntology(OWLDocumentFormat owlDocumentFormat)
            throws OWLOntologyStorageException {
        lock.readLock().lock();
        try {
            delegate.saveOntology(owlDocumentFormat);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void saveOntology(OWLDocumentFormat owlDocumentFormat, IRI iri)
            throws OWLOntologyStorageException {
        lock.readLock().lock();
        try {
            delegate.saveOntology(owlDocumentFormat, iri);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void saveOntology(OWLDocumentFormat owlDocumentFormat, OutputStream outputStream)
            throws OWLOntologyStorageException {
        lock.readLock().lock();
        try {
            delegate.saveOntology(owlDocumentFormat, outputStream);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void saveOntology(OWLOntologyDocumentTarget owlOntologyDocumentTarget)
            throws OWLOntologyStorageException {
        lock.readLock().lock();
        try {
            delegate.saveOntology(owlOntologyDocumentTarget);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void saveOntology(OWLDocumentFormat owlDocumentFormat,
                             OWLOntologyDocumentTarget owlOntologyDocumentTarget) throws OWLOntologyStorageException {
        lock.readLock().lock();
        try {
            delegate.saveOntology(owlDocumentFormat, owlOntologyDocumentTarget);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLClassExpression> getNestedClassExpressions() {
        lock.readLock().lock();
        try {
            return delegate.getNestedClassExpressions();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void accept(OWLObjectVisitor owlObjectVisitor) {
        delegate.accept(owlObjectVisitor);
    }

    @Override
    public <O> O accept(OWLObjectVisitorEx<O> owlObjectVisitorEx) {
        return delegate.accept(owlObjectVisitorEx);
    }

    @Override
    public boolean isTopEntity() {
        lock.readLock().lock();
        try {
            return delegate.isTopEntity();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean isBottomEntity() {
        lock.readLock().lock();
        try {
            return delegate.isBottomEntity();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public String toString() {
        lock.readLock().lock();
        try {
            return delegate.toString();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int compareTo(@Nullable OWLObject o) {
        lock.readLock().lock();
        try {
            return delegate.compareTo(o);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean containsEntityInSignature(OWLEntity owlEntity) {
        lock.readLock().lock();
        try {
            return delegate.containsEntityInSignature(owlEntity);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean containsEntitiesOfTypeInSignature(EntityType<?> type) {
        lock.readLock().lock();
        try {
            return delegate.containsEntitiesOfTypeInSignature(type);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean containsEntitiesOfTypeInSignature(EntityType<?> type,
                                                     Imports includeImportsClosure) {
        lock.readLock().lock();
        try {
            return delegate.containsEntitiesOfTypeInSignature(type, includeImportsClosure);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLAnonymousIndividual> getAnonymousIndividuals() {
        lock.readLock().lock();
        try {
            return delegate.getAnonymousIndividuals();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLClass> getClassesInSignature() {
        lock.readLock().lock();
        try {
            return delegate.getClassesInSignature();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLObjectProperty> getObjectPropertiesInSignature() {
        lock.readLock().lock();
        try {
            return delegate.getObjectPropertiesInSignature();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLDataProperty> getDataPropertiesInSignature() {
        lock.readLock().lock();
        try {
            return delegate.getDataPropertiesInSignature();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLNamedIndividual> getIndividualsInSignature() {
        lock.readLock().lock();
        try {
            return delegate.getIndividualsInSignature();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLDatatype> getDatatypesInSignature() {
        lock.readLock().lock();
        try {
            return delegate.getDatatypesInSignature();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLAnnotationProperty> getAnnotationPropertiesInSignature() {
        lock.readLock().lock();
        try {
            return delegate.getAnnotationPropertiesInSignature();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLAxiom> getAxioms(Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.getAxioms(imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int getAxiomCount(Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.getAxiomCount(imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLLogicalAxiom> getLogicalAxioms(Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.getLogicalAxioms(imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int getLogicalAxiomCount(Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.getLogicalAxiomCount(imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public <T extends OWLAxiom> Set<T> getAxioms(AxiomType<T> axiomType, Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.getAxioms(axiomType, imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public <T extends OWLAxiom> Stream<T> axioms(AxiomType<T> axiomType, Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.axioms(axiomType, imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public <T extends OWLAxiom> int getAxiomCount(AxiomType<T> axiomType, Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.getAxiomCount(axiomType, imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean containsAxiom(OWLAxiom owlAxiom, Imports imports,
                                 AxiomAnnotations axiomAnnotations) {
        lock.readLock().lock();
        try {
            return delegate.containsAxiom(owlAxiom, imports, axiomAnnotations);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLAxiom> getAxiomsIgnoreAnnotations(OWLAxiom owlAxiom, Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.getAxiomsIgnoreAnnotations(owlAxiom, imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLAxiom> axiomsIgnoreAnnotations(OWLAxiom owlAxiom, Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.axiomsIgnoreAnnotations(owlAxiom, imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLAxiom> getReferencingAxioms(OWLPrimitive owlPrimitive, Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.getReferencingAxioms(owlPrimitive, imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLAxiom> referencingAxioms(OWLPrimitive owlPrimitive, Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.referencingAxioms(owlPrimitive, imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLClassAxiom> getAxioms(OWLClass owlClass, Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.getAxioms(owlClass, imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLObjectPropertyAxiom> getAxioms(
            OWLObjectPropertyExpression owlObjectPropertyExpression, Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.getAxioms(owlObjectPropertyExpression, imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLDataPropertyAxiom> getAxioms(OWLDataProperty owlDataProperty, Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.getAxioms(owlDataProperty, imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLIndividualAxiom> getAxioms(OWLIndividual owlIndividual, Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.getAxioms(owlIndividual, imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLAnnotationAxiom> getAxioms(OWLAnnotationProperty owlAnnotationProperty,
                                             Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.getAxioms(owlAnnotationProperty, imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLDatatypeDefinitionAxiom> getAxioms(OWLDatatype owlDatatype, Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.getAxioms(owlDatatype, imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLAxiom> getAxioms() {
        lock.readLock().lock();
        try {
            return delegate.getAxioms();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLAxiom> axioms() {
        // XXX investigate locking access to streams
        lock.readLock().lock();
        try {
            return delegate.axioms();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLLogicalAxiom> getLogicalAxioms() {
        lock.readLock().lock();
        try {
            return delegate.getLogicalAxioms();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLLogicalAxiom> logicalAxioms() {
        lock.readLock().lock();
        try {
            return delegate.logicalAxioms();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public <T extends OWLAxiom> Set<T> getAxioms(AxiomType<T> axiomType) {
        lock.readLock().lock();
        try {
            return delegate.getAxioms(axiomType);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public <T extends OWLAxiom> Stream<T> axioms(AxiomType<T> axiomType) {
        lock.readLock().lock();
        try {
            return delegate.axioms(axiomType);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean containsAxiom(OWLAxiom owlAxiom) {
        lock.readLock().lock();
        try {
            return delegate.containsAxiom(owlAxiom);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLAxiom> getAxioms(boolean b) {
        lock.readLock().lock();
        try {
            return delegate.getAxioms(b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public int getAxiomCount(boolean b) {
        lock.readLock().lock();
        try {
            return delegate.getAxiomCount(b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLLogicalAxiom> getLogicalAxioms(boolean b) {
        lock.readLock().lock();
        try {
            return delegate.getLogicalAxioms(b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public int getLogicalAxiomCount(boolean b) {
        lock.readLock().lock();
        try {
            return delegate.getLogicalAxiomCount(b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public <T extends OWLAxiom> Set<T> getAxioms(AxiomType<T> axiomType, boolean b) {
        lock.readLock().lock();
        try {
            return delegate.getAxioms(axiomType, b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public <T extends OWLAxiom> int getAxiomCount(AxiomType<T> axiomType, boolean b) {
        lock.readLock().lock();
        try {
            return delegate.getAxiomCount(axiomType, b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public boolean containsAxiom(OWLAxiom owlAxiom, boolean b) {
        lock.readLock().lock();
        try {
            return delegate.containsAxiom(owlAxiom, b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public boolean containsAxiomIgnoreAnnotations(OWLAxiom owlAxiom, boolean b) {
        lock.readLock().lock();
        try {
            return delegate.containsAxiomIgnoreAnnotations(owlAxiom, b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLAxiom> getAxiomsIgnoreAnnotations(OWLAxiom owlAxiom, boolean b) {
        lock.readLock().lock();
        try {
            return delegate.getAxiomsIgnoreAnnotations(owlAxiom, b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLAxiom> getReferencingAxioms(OWLPrimitive owlPrimitive, boolean b) {
        lock.readLock().lock();
        try {
            return delegate.getReferencingAxioms(owlPrimitive, b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLClassAxiom> getAxioms(OWLClass owlClass, boolean b) {
        lock.readLock().lock();
        try {
            return delegate.getAxioms(owlClass, b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLObjectPropertyAxiom> getAxioms(
            OWLObjectPropertyExpression owlObjectPropertyExpression, boolean b) {
        lock.readLock().lock();
        try {
            return delegate.getAxioms(owlObjectPropertyExpression, b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLDataPropertyAxiom> getAxioms(OWLDataProperty owlDataProperty, boolean b) {
        lock.readLock().lock();
        try {
            return delegate.getAxioms(owlDataProperty, b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLIndividualAxiom> getAxioms(OWLIndividual owlIndividual, boolean b) {
        lock.readLock().lock();
        try {
            return delegate.getAxioms(owlIndividual, b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLAnnotationAxiom> getAxioms(OWLAnnotationProperty owlAnnotationProperty,
                                             boolean b) {
        lock.readLock().lock();
        try {
            return delegate.getAxioms(owlAnnotationProperty, b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLDatatypeDefinitionAxiom> getAxioms(OWLDatatype owlDatatype, boolean b) {
        lock.readLock().lock();
        try {
            return delegate.getAxioms(owlDatatype, b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int getAxiomCount() {
        lock.readLock().lock();
        try {
            return delegate.getAxiomCount();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int getLogicalAxiomCount() {
        lock.readLock().lock();
        try {
            return delegate.getLogicalAxiomCount();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public <T extends OWLAxiom> int getAxiomCount(AxiomType<T> axiomType) {
        lock.readLock().lock();
        try {
            return delegate.getAxiomCount(axiomType);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean containsAxiomIgnoreAnnotations(OWLAxiom owlAxiom) {
        lock.readLock().lock();
        try {
            return delegate.containsAxiomIgnoreAnnotations(owlAxiom);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLAxiom> getAxiomsIgnoreAnnotations(OWLAxiom owlAxiom) {
        lock.readLock().lock();
        try {
            return delegate.getAxiomsIgnoreAnnotations(owlAxiom);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLAxiom> axiomsIgnoreAnnotations(OWLAxiom owlAxiom) {
        lock.readLock().lock();
        try {
            return delegate.axiomsIgnoreAnnotations(owlAxiom);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLAxiom> getReferencingAxioms(OWLPrimitive owlPrimitive) {
        lock.readLock().lock();
        try {
            return delegate.getReferencingAxioms(owlPrimitive);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLAxiom> referencingAxioms(OWLPrimitive owlPrimitive) {
        lock.readLock().lock();
        try {
            return delegate.referencingAxioms(owlPrimitive);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLClassAxiom> getAxioms(OWLClass owlClass) {
        lock.readLock().lock();
        try {
            return delegate.getAxioms(owlClass);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLObjectPropertyAxiom> getAxioms(
            OWLObjectPropertyExpression owlObjectPropertyExpression) {
        lock.readLock().lock();
        try {
            return delegate.getAxioms(owlObjectPropertyExpression);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLDataPropertyAxiom> getAxioms(OWLDataProperty owlDataProperty) {
        lock.readLock().lock();
        try {
            return delegate.getAxioms(owlDataProperty);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLIndividualAxiom> getAxioms(OWLIndividual owlIndividual) {
        lock.readLock().lock();
        try {
            return delegate.getAxioms(owlIndividual);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLAnnotationAxiom> getAxioms(OWLAnnotationProperty owlAnnotationProperty) {
        lock.readLock().lock();
        try {
            return delegate.getAxioms(owlAnnotationProperty);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLDatatypeDefinitionAxiom> getAxioms(OWLDatatype owlDatatype) {
        lock.readLock().lock();
        try {
            return delegate.getAxioms(owlDatatype);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLClassAxiom> axioms(OWLClass owlClass) {
        lock.readLock().lock();
        try {
            return delegate.axioms(owlClass);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLObjectPropertyAxiom> axioms(
            OWLObjectPropertyExpression owlObjectPropertyExpression) {
        lock.readLock().lock();
        try {
            return delegate.axioms(owlObjectPropertyExpression);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLDataPropertyAxiom> axioms(OWLDataProperty owlDataProperty) {
        lock.readLock().lock();
        try {
            return delegate.axioms(owlDataProperty);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLIndividualAxiom> axioms(OWLIndividual owlIndividual) {
        lock.readLock().lock();
        try {
            return delegate.axioms(owlIndividual);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLAnnotationAxiom> axioms(OWLAnnotationProperty owlAnnotationProperty) {
        lock.readLock().lock();
        try {
            return delegate.axioms(owlAnnotationProperty);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLDatatypeDefinitionAxiom> axioms(OWLDatatype owlDatatype) {
        lock.readLock().lock();
        try {
            return delegate.axioms(owlDatatype);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLClass> getClassesInSignature(Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.getClassesInSignature(imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLObjectProperty> getObjectPropertiesInSignature(Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.getObjectPropertiesInSignature(imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLDataProperty> getDataPropertiesInSignature(Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.getDataPropertiesInSignature(imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLNamedIndividual> getIndividualsInSignature(Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.getIndividualsInSignature(imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLAnonymousIndividual> getReferencedAnonymousIndividuals(Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.getReferencedAnonymousIndividuals(imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLAnonymousIndividual> referencedAnonymousIndividuals(Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.referencedAnonymousIndividuals(imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLAnonymousIndividual> referencedAnonymousIndividuals() {
        lock.readLock().lock();
        try {
            return delegate.referencedAnonymousIndividuals();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLDatatype> getDatatypesInSignature(Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.getDatatypesInSignature(imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLAnnotationProperty> getAnnotationPropertiesInSignature(Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.getAnnotationPropertiesInSignature(imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean containsEntityInSignature(OWLEntity owlEntity, Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.containsEntityInSignature(owlEntity, imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean containsEntityInSignature(IRI iri, Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.containsEntityInSignature(iri, imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean containsClassInSignature(IRI iri, Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.containsClassInSignature(iri, imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean containsObjectPropertyInSignature(IRI iri, Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.containsObjectPropertyInSignature(iri, imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean containsDataPropertyInSignature(IRI iri, Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.containsDataPropertyInSignature(iri, imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean containsAnnotationPropertyInSignature(IRI iri, Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.containsAnnotationPropertyInSignature(iri, imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean containsDatatypeInSignature(IRI iri, Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.containsDatatypeInSignature(iri, imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean containsIndividualInSignature(IRI iri, Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.containsIndividualInSignature(iri, imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean containsDatatypeInSignature(IRI iri) {
        lock.readLock().lock();
        try {
            return delegate.containsDatatypeInSignature(iri);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean containsEntityInSignature(IRI iri) {
        lock.readLock().lock();
        try {
            return delegate.containsEntityInSignature(iri);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean containsClassInSignature(IRI iri) {
        lock.readLock().lock();
        try {
            return delegate.containsClassInSignature(iri);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean containsObjectPropertyInSignature(IRI iri) {
        lock.readLock().lock();
        try {
            return delegate.containsObjectPropertyInSignature(iri);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean containsDataPropertyInSignature(IRI iri) {
        lock.readLock().lock();
        try {
            return delegate.containsDataPropertyInSignature(iri);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean containsAnnotationPropertyInSignature(IRI iri) {
        lock.readLock().lock();
        try {
            return delegate.containsAnnotationPropertyInSignature(iri);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean containsIndividualInSignature(IRI iri) {
        lock.readLock().lock();
        try {
            return delegate.containsIndividualInSignature(iri);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLEntity> getEntitiesInSignature(IRI iri, Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.getEntitiesInSignature(iri, imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<IRI> getPunnedIRIs(Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.getPunnedIRIs(imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean containsReference(OWLEntity owlEntity, Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.containsReference(owlEntity, imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean containsReference(OWLEntity owlEntity) {
        lock.readLock().lock();
        try {
            return delegate.containsReference(owlEntity);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLEntity> getEntitiesInSignature(IRI iri) {
        lock.readLock().lock();
        try {
            return delegate.getEntitiesInSignature(iri);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLEntity> entitiesInSignature(IRI iri) {
        lock.readLock().lock();
        try {
            return delegate.entitiesInSignature(iri);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLClass> getClassesInSignature(boolean b) {
        lock.readLock().lock();
        try {
            return delegate.getClassesInSignature(b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLObjectProperty> getObjectPropertiesInSignature(boolean b) {
        lock.readLock().lock();
        try {
            return delegate.getObjectPropertiesInSignature(b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLDataProperty> getDataPropertiesInSignature(boolean b) {
        lock.readLock().lock();
        try {
            return delegate.getDataPropertiesInSignature(b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLNamedIndividual> getIndividualsInSignature(boolean b) {
        lock.readLock().lock();
        try {
            return delegate.getIndividualsInSignature(b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLAnonymousIndividual> getReferencedAnonymousIndividuals(boolean b) {
        lock.readLock().lock();
        try {
            return delegate.getReferencedAnonymousIndividuals(b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLDatatype> getDatatypesInSignature(boolean b) {
        lock.readLock().lock();
        try {
            return delegate.getDatatypesInSignature(b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLAnnotationProperty> getAnnotationPropertiesInSignature(boolean b) {
        lock.readLock().lock();
        try {
            return delegate.getAnnotationPropertiesInSignature(b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public boolean containsEntityInSignature(OWLEntity owlEntity, boolean b) {
        lock.readLock().lock();
        try {
            return delegate.containsEntityInSignature(owlEntity, b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public boolean containsEntityInSignature(IRI iri, boolean b) {
        lock.readLock().lock();
        try {
            return delegate.containsEntityInSignature(iri, b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public boolean containsClassInSignature(IRI iri, boolean b) {
        lock.readLock().lock();
        try {
            return delegate.containsClassInSignature(iri, b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public boolean containsObjectPropertyInSignature(IRI iri, boolean b) {
        lock.readLock().lock();
        try {
            return delegate.containsObjectPropertyInSignature(iri, b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public boolean containsDataPropertyInSignature(IRI iri, boolean b) {
        lock.readLock().lock();
        try {
            return delegate.containsDataPropertyInSignature(iri, b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public boolean containsAnnotationPropertyInSignature(IRI iri, boolean b) {
        lock.readLock().lock();
        try {
            return delegate.containsAnnotationPropertyInSignature(iri, b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public boolean containsDatatypeInSignature(IRI iri, boolean b) {
        lock.readLock().lock();
        try {
            return delegate.containsDatatypeInSignature(iri, b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public boolean containsIndividualInSignature(IRI iri, boolean b) {
        lock.readLock().lock();
        try {
            return delegate.containsIndividualInSignature(iri, b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLEntity> getEntitiesInSignature(IRI iri, boolean b) {
        lock.readLock().lock();
        try {
            return delegate.getEntitiesInSignature(iri, b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public boolean containsReference(OWLEntity owlEntity, boolean b) {
        lock.readLock().lock();
        try {
            return delegate.containsReference(owlEntity, b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public <T extends OWLAxiom> Set<T> getAxioms(Class<T> aClass, OWLObject owlObject,
                                                 Imports imports, Navigation navigation) {
        lock.readLock().lock();
        try {
            return delegate.getAxioms(aClass, owlObject, imports, navigation);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public <T extends OWLAxiom> Stream<T> axioms(Class<T> aClass, OWLObject owlObject,
                                                 Imports imports, Navigation navigation) {
        lock.readLock().lock();
        try {
            return delegate.axioms(aClass, owlObject, imports, navigation);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public <T extends OWLAxiom> Collection<T> filterAxioms(
            OWLAxiomSearchFilter owlAxiomSearchFilter, Object o, Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.filterAxioms(owlAxiomSearchFilter, o, imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean contains(OWLAxiomSearchFilter owlAxiomSearchFilter, Object o, Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.contains(owlAxiomSearchFilter, o, imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean contains(OWLAxiomSearchFilter owlAxiomSearchFilter, Object o) {
        lock.readLock().lock();
        try {
            return delegate.contains(owlAxiomSearchFilter, o);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public <T extends OWLAxiom> Set<T> getAxioms(Class<T> aClass,
                                                 Class<? extends OWLObject> aClass1, OWLObject owlObject, Imports imports,
                                                 Navigation navigation) {
        lock.readLock().lock();
        try {
            return delegate.getAxioms(aClass, aClass1, owlObject, imports, navigation);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public <T extends OWLAxiom> Stream<T> axioms(Class<T> aClass,
                                                 Class<? extends OWLObject> aClass1, OWLObject owlObject, Imports imports,
                                                 Navigation navigation) {
        lock.readLock().lock();
        try {
            return delegate.axioms(aClass, aClass1, owlObject, imports, navigation);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLSubAnnotationPropertyOfAxiom> getSubAnnotationPropertyOfAxioms(
            OWLAnnotationProperty owlAnnotationProperty) {
        lock.readLock().lock();
        try {
            return delegate.getSubAnnotationPropertyOfAxioms(owlAnnotationProperty);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLAnnotationPropertyDomainAxiom> getAnnotationPropertyDomainAxioms(
            OWLAnnotationProperty owlAnnotationProperty) {
        lock.readLock().lock();
        try {
            return delegate.getAnnotationPropertyDomainAxioms(owlAnnotationProperty);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLAnnotationPropertyRangeAxiom> getAnnotationPropertyRangeAxioms(
            OWLAnnotationProperty owlAnnotationProperty) {
        lock.readLock().lock();
        try {
            return delegate.getAnnotationPropertyRangeAxioms(owlAnnotationProperty);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLAnnotationPropertyDomainAxiom> annotationPropertyDomainAxioms(
            OWLAnnotationProperty owlAnnotationProperty) {
        lock.readLock().lock();
        try {
            return delegate.annotationPropertyDomainAxioms(owlAnnotationProperty);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLAnnotationPropertyRangeAxiom> annotationPropertyRangeAxioms(
            OWLAnnotationProperty owlAnnotationProperty) {
        lock.readLock().lock();
        try {
            return delegate.annotationPropertyRangeAxioms(owlAnnotationProperty);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLDeclarationAxiom> getDeclarationAxioms(OWLEntity owlEntity) {
        lock.readLock().lock();
        try {
            return delegate.getDeclarationAxioms(owlEntity);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLAnnotationAssertionAxiom> getAnnotationAssertionAxioms(
            OWLAnnotationSubject owlAnnotationSubject) {
        lock.readLock().lock();
        try {
            return delegate.getAnnotationAssertionAxioms(owlAnnotationSubject);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLSubClassOfAxiom> getSubClassAxiomsForSubClass(OWLClass owlClass) {
        lock.readLock().lock();
        try {
            return delegate.getSubClassAxiomsForSubClass(owlClass);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLSubClassOfAxiom> getSubClassAxiomsForSuperClass(OWLClass owlClass) {
        lock.readLock().lock();
        try {
            return delegate.getSubClassAxiomsForSuperClass(owlClass);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLEquivalentClassesAxiom> getEquivalentClassesAxioms(OWLClass owlClass) {
        lock.readLock().lock();
        try {
            return delegate.getEquivalentClassesAxioms(owlClass);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLDisjointClassesAxiom> getDisjointClassesAxioms(OWLClass owlClass) {
        lock.readLock().lock();
        try {
            return delegate.getDisjointClassesAxioms(owlClass);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLDisjointUnionAxiom> getDisjointUnionAxioms(OWLClass owlClass) {
        lock.readLock().lock();
        try {
            return delegate.getDisjointUnionAxioms(owlClass);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLHasKeyAxiom> getHasKeyAxioms(OWLClass owlClass) {
        lock.readLock().lock();
        try {
            return delegate.getHasKeyAxioms(owlClass);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLSubObjectPropertyOfAxiom> getObjectSubPropertyAxiomsForSubProperty(
            OWLObjectPropertyExpression owlObjectPropertyExpression) {
        lock.readLock().lock();
        try {
            return delegate.getObjectSubPropertyAxiomsForSubProperty(owlObjectPropertyExpression);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLSubObjectPropertyOfAxiom> getObjectSubPropertyAxiomsForSuperProperty(
            OWLObjectPropertyExpression owlObjectPropertyExpression) {
        lock.readLock().lock();
        try {
            return delegate.getObjectSubPropertyAxiomsForSuperProperty(owlObjectPropertyExpression);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLObjectPropertyDomainAxiom> getObjectPropertyDomainAxioms(
            OWLObjectPropertyExpression owlObjectPropertyExpression) {
        lock.readLock().lock();
        try {
            return delegate.getObjectPropertyDomainAxioms(owlObjectPropertyExpression);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLObjectPropertyRangeAxiom> getObjectPropertyRangeAxioms(
            OWLObjectPropertyExpression owlObjectPropertyExpression) {
        lock.readLock().lock();
        try {
            return delegate.getObjectPropertyRangeAxioms(owlObjectPropertyExpression);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLInverseObjectPropertiesAxiom> getInverseObjectPropertyAxioms(
            OWLObjectPropertyExpression owlObjectPropertyExpression) {
        lock.readLock().lock();
        try {
            return delegate.getInverseObjectPropertyAxioms(owlObjectPropertyExpression);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLEquivalentObjectPropertiesAxiom> getEquivalentObjectPropertiesAxioms(
            OWLObjectPropertyExpression owlObjectPropertyExpression) {
        lock.readLock().lock();
        try {
            return delegate.getEquivalentObjectPropertiesAxioms(owlObjectPropertyExpression);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLDisjointObjectPropertiesAxiom> getDisjointObjectPropertiesAxioms(
            OWLObjectPropertyExpression owlObjectPropertyExpression) {
        lock.readLock().lock();
        try {
            return delegate.getDisjointObjectPropertiesAxioms(owlObjectPropertyExpression);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLFunctionalObjectPropertyAxiom> getFunctionalObjectPropertyAxioms(
            OWLObjectPropertyExpression owlObjectPropertyExpression) {
        lock.readLock().lock();
        try {
            return delegate.getFunctionalObjectPropertyAxioms(owlObjectPropertyExpression);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLInverseFunctionalObjectPropertyAxiom> getInverseFunctionalObjectPropertyAxioms(
            OWLObjectPropertyExpression owlObjectPropertyExpression) {
        lock.readLock().lock();
        try {
            return delegate.getInverseFunctionalObjectPropertyAxioms(owlObjectPropertyExpression);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLSymmetricObjectPropertyAxiom> getSymmetricObjectPropertyAxioms(
            OWLObjectPropertyExpression owlObjectPropertyExpression) {
        lock.readLock().lock();
        try {
            return delegate.getSymmetricObjectPropertyAxioms(owlObjectPropertyExpression);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLAsymmetricObjectPropertyAxiom> getAsymmetricObjectPropertyAxioms(
            OWLObjectPropertyExpression owlObjectPropertyExpression) {
        lock.readLock().lock();
        try {
            return delegate.getAsymmetricObjectPropertyAxioms(owlObjectPropertyExpression);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLReflexiveObjectPropertyAxiom> getReflexiveObjectPropertyAxioms(
            OWLObjectPropertyExpression owlObjectPropertyExpression) {
        lock.readLock().lock();
        try {
            return delegate.getReflexiveObjectPropertyAxioms(owlObjectPropertyExpression);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLIrreflexiveObjectPropertyAxiom> getIrreflexiveObjectPropertyAxioms(
            OWLObjectPropertyExpression owlObjectPropertyExpression) {
        lock.readLock().lock();
        try {
            return delegate.getIrreflexiveObjectPropertyAxioms(owlObjectPropertyExpression);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLTransitiveObjectPropertyAxiom> getTransitiveObjectPropertyAxioms(
            OWLObjectPropertyExpression owlObjectPropertyExpression) {
        lock.readLock().lock();
        try {
            return delegate.getTransitiveObjectPropertyAxioms(owlObjectPropertyExpression);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLSubDataPropertyOfAxiom> getDataSubPropertyAxiomsForSubProperty(
            OWLDataProperty owlDataProperty) {
        lock.readLock().lock();
        try {
            return delegate.getDataSubPropertyAxiomsForSubProperty(owlDataProperty);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLSubDataPropertyOfAxiom> getDataSubPropertyAxiomsForSuperProperty(
            OWLDataPropertyExpression owlDataPropertyExpression) {
        lock.readLock().lock();
        try {
            return delegate.getDataSubPropertyAxiomsForSuperProperty(owlDataPropertyExpression);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLDataPropertyDomainAxiom> getDataPropertyDomainAxioms(
            OWLDataProperty owlDataProperty) {
        lock.readLock().lock();
        try {
            return delegate.getDataPropertyDomainAxioms(owlDataProperty);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLDataPropertyRangeAxiom> getDataPropertyRangeAxioms(
            OWLDataProperty owlDataProperty) {
        lock.readLock().lock();
        try {
            return delegate.getDataPropertyRangeAxioms(owlDataProperty);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLEquivalentDataPropertiesAxiom> getEquivalentDataPropertiesAxioms(
            OWLDataProperty owlDataProperty) {
        lock.readLock().lock();
        try {
            return delegate.getEquivalentDataPropertiesAxioms(owlDataProperty);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLDisjointDataPropertiesAxiom> getDisjointDataPropertiesAxioms(
            OWLDataProperty owlDataProperty) {
        lock.readLock().lock();
        try {
            return delegate.getDisjointDataPropertiesAxioms(owlDataProperty);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLFunctionalDataPropertyAxiom> getFunctionalDataPropertyAxioms(
            OWLDataPropertyExpression owlDataPropertyExpression) {
        lock.readLock().lock();
        try {
            return delegate.getFunctionalDataPropertyAxioms(owlDataPropertyExpression);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLClassAssertionAxiom> getClassAssertionAxioms(OWLIndividual owlIndividual) {
        lock.readLock().lock();
        try {
            return delegate.getClassAssertionAxioms(owlIndividual);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLClassAssertionAxiom> getClassAssertionAxioms(
            OWLClassExpression owlClassExpression) {
        lock.readLock().lock();
        try {
            return delegate.getClassAssertionAxioms(owlClassExpression);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLDataPropertyAssertionAxiom> getDataPropertyAssertionAxioms(
            OWLIndividual owlIndividual) {
        lock.readLock().lock();
        try {
            return delegate.getDataPropertyAssertionAxioms(owlIndividual);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLObjectPropertyAssertionAxiom> getObjectPropertyAssertionAxioms(
            OWLIndividual owlIndividual) {
        lock.readLock().lock();
        try {
            return delegate.getObjectPropertyAssertionAxioms(owlIndividual);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLNegativeObjectPropertyAssertionAxiom> getNegativeObjectPropertyAssertionAxioms(
            OWLIndividual owlIndividual) {
        lock.readLock().lock();
        try {
            return delegate.getNegativeObjectPropertyAssertionAxioms(owlIndividual);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLNegativeDataPropertyAssertionAxiom> getNegativeDataPropertyAssertionAxioms(
            OWLIndividual owlIndividual) {
        lock.readLock().lock();
        try {
            return delegate.getNegativeDataPropertyAssertionAxioms(owlIndividual);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLSameIndividualAxiom> getSameIndividualAxioms(OWLIndividual owlIndividual) {
        lock.readLock().lock();
        try {
            return delegate.getSameIndividualAxioms(owlIndividual);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLDifferentIndividualsAxiom> getDifferentIndividualAxioms(
            OWLIndividual owlIndividual) {
        lock.readLock().lock();
        try {
            return delegate.getDifferentIndividualAxioms(owlIndividual);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLDatatypeDefinitionAxiom> getDatatypeDefinitions(OWLDatatype owlDatatype) {
        lock.readLock().lock();
        try {
            return delegate.getDatatypeDefinitions(owlDatatype);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public ChangeApplied applyChange(OWLOntologyChange owlOntologyChange) {
        lock.writeLock().lock();
        try {
            return getMutableOntology().applyChange(owlOntologyChange);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public ChangeDetails applyChangesAndGetDetails(List<? extends OWLOntologyChange> list) {
        lock.writeLock().lock();
        try {
            return getMutableOntology().applyChangesAndGetDetails(list);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public ChangeApplied addAxiom(OWLAxiom owlAxiom) {
        lock.writeLock().lock();
        try {
            return getMutableOntology().addAxiom(owlAxiom);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public ChangeApplied addAxioms(Collection<? extends OWLAxiom> set) {
        lock.writeLock().lock();
        try {
            return getMutableOntology().addAxioms(set);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public ChangeApplied addAxioms(OWLAxiom... set) {
        lock.writeLock().lock();
        try {
            return getMutableOntology().addAxioms(set);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public ChangeApplied add(OWLAxiom owlAxiom) {
        lock.writeLock().lock();
        try {
            return getMutableOntology().add(owlAxiom);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public ChangeApplied add(Collection<? extends OWLAxiom> set) {
        lock.writeLock().lock();
        try {
            return getMutableOntology().add(set);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public ChangeApplied add(OWLAxiom... set) {
        lock.writeLock().lock();
        try {
            return getMutableOntology().add(set);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private OWLMutableOntology getMutableOntology() {
        return (OWLMutableOntology) delegate;
    }

    @Override
    public Stream<OWLImportsDeclaration> importsDeclarations() {
        lock.readLock().lock();
        try {
            return delegate.importsDeclarations();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public <T extends OWLAxiom> Stream<T> axioms(OWLAxiomSearchFilter filter, Object key,
                                                 Imports includeImportsClosure) {
        lock.readLock().lock();
        try {
            return delegate.axioms(filter, key, includeImportsClosure);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public <T extends OWLAxiom> Stream<T> axioms(OWLAxiomSearchFilter filter, Object key) {
        lock.readLock().lock();
        try {
            return delegate.axioms(filter, key);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public <T extends OWLAxiom> Stream<T> axioms(Class<T> type,
                                                 Class<? extends OWLObject> explicitClass, OWLObject entity, Navigation forSubPosition) {
        lock.readLock().lock();
        try {
            return delegate.axioms(type, explicitClass, entity, forSubPosition);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLSubAnnotationPropertyOfAxiom> subAnnotationPropertyOfAxioms(
            OWLAnnotationProperty subProperty) {
        lock.readLock().lock();
        try {
            return delegate.subAnnotationPropertyOfAxioms(subProperty);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLDatatypeDefinitionAxiom> datatypeDefinitions(OWLDatatype datatype) {
        lock.readLock().lock();
        try {
            return delegate.datatypeDefinitions(datatype);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public ChangeApplied removeAxiom(OWLAxiom axiom) {
        lock.writeLock().lock();
        try {
            return delegate.removeAxiom(axiom);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public ChangeApplied removeAxioms(Collection<? extends OWLAxiom> axioms) {
        lock.writeLock().lock();
        try {
            return delegate.removeAxioms(axioms);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public ChangeApplied removeAxioms(OWLAxiom... axioms) {
        lock.writeLock().lock();
        try {
            return delegate.removeAxioms(axioms);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public ChangeApplied remove(OWLAxiom axiom) {
        lock.writeLock().lock();
        try {
            return delegate.remove(axiom);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public ChangeApplied remove(Collection<? extends OWLAxiom> axioms) {
        lock.writeLock().lock();
        try {
            return delegate.remove(axioms);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public ChangeApplied remove(OWLAxiom... axioms) {
        lock.writeLock().lock();
        try {
            return delegate.remove(axioms);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public ChangeApplied applyDirectChange(OWLOntologyChange change) {
        lock.writeLock().lock();
        try {
            return delegate.applyDirectChange(change);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Stream<OWLDisjointObjectPropertiesAxiom> disjointObjectPropertiesAxioms(
            OWLObjectPropertyExpression property) {
        lock.readLock().lock();
        try {
            return delegate.disjointObjectPropertiesAxioms(property);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLObjectProperty> objectPropertiesInSignature() {
        lock.readLock().lock();
        try {
            return delegate.objectPropertiesInSignature();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLAnnotationAssertionAxiom> annotationAssertionAxioms(
            OWLAnnotationSubject entity) {
        lock.readLock().lock();
        try {
            return delegate.annotationAssertionAxioms(entity);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLAnnotationAssertionAxiom> annotationAssertionAxioms(
            OWLAnnotationSubject entity, Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.annotationAssertionAxioms(entity, imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLAnnotationProperty> annotationPropertiesInSignature() {
        lock.readLock().lock();
        try {
            return delegate.annotationPropertiesInSignature();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLAnnotationProperty> annotationPropertiesInSignature(Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.annotationPropertiesInSignature(imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLAnnotation> annotations() {
        lock.readLock().lock();
        try {
            return delegate.annotations();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<OWLAnnotation> annotationsAsList() {
        lock.readLock().lock();
        try {
            return delegate.annotationsAsList();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLAnnotation> annotations(OWLAnnotationProperty p) {
        lock.readLock().lock();
        try {
            return delegate.annotations(p);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLAnnotation> annotations(Predicate<OWLAnnotation> p) {
        lock.readLock().lock();
        try {
            return delegate.annotations(p);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLAnonymousIndividual> anonymousIndividuals() {
        lock.readLock().lock();
        try {
            return delegate.anonymousIndividuals();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLAsymmetricObjectPropertyAxiom> asymmetricObjectPropertyAxioms(
            OWLObjectPropertyExpression property) {
        lock.readLock().lock();
        try {
            return delegate.asymmetricObjectPropertyAxioms(property);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public <T extends OWLAxiom> Stream<T> axioms(Class<T> type, OWLObject entity,
                                                 Navigation forSubPosition) {
        lock.readLock().lock();
        try {
            return delegate.axioms(type, entity, forSubPosition);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLAxiom> axioms(Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.axioms(imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLAnnotationAxiom> axioms(OWLAnnotationProperty property, Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.axioms(property, imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLClassAxiom> axioms(OWLClass cls, Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.axioms(cls, imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLDataPropertyAxiom> axioms(OWLDataProperty property, Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.axioms(property, imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLDatatypeDefinitionAxiom> axioms(OWLDatatype datatype, Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.axioms(datatype, imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLIndividualAxiom> axioms(OWLIndividual individual, Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.axioms(individual, imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLObjectPropertyAxiom> axioms(OWLObjectPropertyExpression property,
                                                 Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.axioms(property, imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLClassAssertionAxiom> classAssertionAxioms(OWLClassExpression ce) {
        lock.readLock().lock();
        try {
            return delegate.classAssertionAxioms(ce);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLClassAssertionAxiom> classAssertionAxioms(OWLIndividual individual) {
        lock.readLock().lock();
        try {
            return delegate.classAssertionAxioms(individual);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLClass> classesInSignature() {
        lock.readLock().lock();
        try {
            return delegate.classesInSignature();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLClass> classesInSignature(Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.classesInSignature(imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLDataProperty> dataPropertiesInSignature() {
        lock.readLock().lock();
        try {
            return delegate.dataPropertiesInSignature();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLDataProperty> dataPropertiesInSignature(Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.dataPropertiesInSignature(imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLDataPropertyAssertionAxiom> dataPropertyAssertionAxioms(
            OWLIndividual individual) {
        lock.readLock().lock();
        try {
            return delegate.dataPropertyAssertionAxioms(individual);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLDataPropertyDomainAxiom> dataPropertyDomainAxioms(OWLDataProperty property) {
        lock.readLock().lock();
        try {
            return delegate.dataPropertyDomainAxioms(property);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLDataPropertyRangeAxiom> dataPropertyRangeAxioms(OWLDataProperty property) {
        lock.readLock().lock();
        try {
            return delegate.dataPropertyRangeAxioms(property);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLSubDataPropertyOfAxiom> dataSubPropertyAxiomsForSubProperty(
            OWLDataProperty subProperty) {
        lock.readLock().lock();
        try {
            return delegate.dataSubPropertyAxiomsForSubProperty(subProperty);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLSubDataPropertyOfAxiom> dataSubPropertyAxiomsForSuperProperty(
            OWLDataPropertyExpression superProperty) {
        lock.readLock().lock();
        try {
            return delegate.dataSubPropertyAxiomsForSuperProperty(superProperty);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLDatatype> datatypesInSignature() {
        lock.readLock().lock();
        try {
            return delegate.datatypesInSignature();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLDatatype> datatypesInSignature(Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.datatypesInSignature(imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLDeclarationAxiom> declarationAxioms(OWLEntity subject) {
        lock.readLock().lock();
        try {
            return delegate.declarationAxioms(subject);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLDifferentIndividualsAxiom> differentIndividualAxioms(
            OWLIndividual individual) {
        lock.readLock().lock();
        try {
            return delegate.differentIndividualAxioms(individual);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLDisjointClassesAxiom> disjointClassesAxioms(OWLClass cls) {
        lock.readLock().lock();
        try {
            return delegate.disjointClassesAxioms(cls);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLDisjointDataPropertiesAxiom> disjointDataPropertiesAxioms(
            OWLDataProperty property) {
        lock.readLock().lock();
        try {
            return delegate.disjointDataPropertiesAxioms(property);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLDisjointUnionAxiom> disjointUnionAxioms(OWLClass owlClass) {
        lock.readLock().lock();
        try {
            return delegate.disjointUnionAxioms(owlClass);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLEntity> entitiesInSignature(IRI iri, Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.entitiesInSignature(iri, imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLEquivalentClassesAxiom> equivalentClassesAxioms(OWLClass cls) {
        lock.readLock().lock();
        try {
            return delegate.equivalentClassesAxioms(cls);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLEquivalentDataPropertiesAxiom> equivalentDataPropertiesAxioms(
            OWLDataProperty property) {
        lock.readLock().lock();
        try {
            return delegate.equivalentDataPropertiesAxioms(property);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLEquivalentObjectPropertiesAxiom> equivalentObjectPropertiesAxioms(
            OWLObjectPropertyExpression property) {
        lock.readLock().lock();
        try {
            return delegate.equivalentObjectPropertiesAxioms(property);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public <T extends OWLAxiom> Collection<T> filterAxioms(OWLAxiomSearchFilter filter,
                                                           Object key) {
        lock.readLock().lock();
        try {
            return delegate.filterAxioms(filter, key);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLFunctionalDataPropertyAxiom> functionalDataPropertyAxioms(
            OWLDataPropertyExpression property) {
        lock.readLock().lock();
        try {
            return delegate.functionalDataPropertyAxioms(property);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLFunctionalObjectPropertyAxiom> functionalObjectPropertyAxioms(
            OWLObjectPropertyExpression property) {
        lock.readLock().lock();
        try {
            return delegate.functionalObjectPropertyAxioms(property);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLAnnotationAssertionAxiom> getAnnotationAssertionAxioms(
            OWLAnnotationSubject entity, Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.getAnnotationAssertionAxioms(entity, imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLAnnotation> getAnnotations(OWLAnnotationProperty annotationProperty) {
        lock.readLock().lock();
        try {
            return delegate.getAnnotations(annotationProperty);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public <T extends OWLAxiom> Set<T> getAxioms(Class<T> type,
                                                 Class<? extends OWLObject> explicitClass, OWLObject entity, Navigation forSubPosition) {
        lock.readLock().lock();
        try {
            return delegate.getAxioms(type, explicitClass, entity, forSubPosition);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public <T extends OWLAxiom> Set<T> getAxioms(Class<T> type, OWLObject entity,
                                                 Navigation forSubPosition) {
        lock.readLock().lock();
        try {
            return delegate.getAxioms(type, entity, forSubPosition);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Nullable
    public OWLDocumentFormat getFormat() {
        lock.readLock().lock();
        try {
            return delegate.getFormat();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Deprecated
    public Set<OWLAnonymousIndividual> getReferencedAnonymousIndividuals() {
        lock.readLock().lock();
        try {
            return delegate.getReferencedAnonymousIndividuals();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLHasKeyAxiom> hasKeyAxioms(OWLClass cls) {
        lock.readLock().lock();
        try {
            return delegate.hasKeyAxioms(cls);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLNamedIndividual> individualsInSignature() {
        lock.readLock().lock();
        try {
            return delegate.individualsInSignature();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLNamedIndividual> individualsInSignature(Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.individualsInSignature(imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLInverseFunctionalObjectPropertyAxiom> inverseFunctionalObjectPropertyAxioms(
            OWLObjectPropertyExpression property) {
        lock.readLock().lock();
        try {
            return delegate.inverseFunctionalObjectPropertyAxioms(property);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLInverseObjectPropertiesAxiom> inverseObjectPropertyAxioms(
            OWLObjectPropertyExpression property) {
        lock.readLock().lock();
        try {
            return delegate.inverseObjectPropertyAxioms(property);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLIrreflexiveObjectPropertyAxiom> irreflexiveObjectPropertyAxioms(
            OWLObjectPropertyExpression property) {
        lock.readLock().lock();
        try {
            return delegate.irreflexiveObjectPropertyAxioms(property);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLLogicalAxiom> logicalAxioms(Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.logicalAxioms(imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLNegativeDataPropertyAssertionAxiom> negativeDataPropertyAssertionAxioms(
            OWLIndividual individual) {
        lock.readLock().lock();
        try {
            return delegate.negativeDataPropertyAssertionAxioms(individual);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLNegativeObjectPropertyAssertionAxiom> negativeObjectPropertyAssertionAxioms(
            OWLIndividual individual) {
        lock.readLock().lock();
        try {
            return delegate.negativeObjectPropertyAssertionAxioms(individual);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLClassExpression> nestedClassExpressions() {
        lock.readLock().lock();
        try {
            return delegate.nestedClassExpressions();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLObjectProperty> objectPropertiesInSignature(Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.objectPropertiesInSignature(imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLObjectPropertyAssertionAxiom> objectPropertyAssertionAxioms(
            OWLIndividual individual) {
        lock.readLock().lock();
        try {
            return delegate.objectPropertyAssertionAxioms(individual);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLObjectPropertyDomainAxiom> objectPropertyDomainAxioms(
            OWLObjectPropertyExpression property) {
        lock.readLock().lock();
        try {
            return delegate.objectPropertyDomainAxioms(property);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLObjectPropertyRangeAxiom> objectPropertyRangeAxioms(
            OWLObjectPropertyExpression property) {
        lock.readLock().lock();
        try {
            return delegate.objectPropertyRangeAxioms(property);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLSubObjectPropertyOfAxiom> objectSubPropertyAxiomsForSubProperty(
            OWLObjectPropertyExpression subProperty) {
        lock.readLock().lock();
        try {
            return delegate.objectSubPropertyAxiomsForSubProperty(subProperty);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLSubObjectPropertyOfAxiom> objectSubPropertyAxiomsForSuperProperty(
            OWLObjectPropertyExpression superProperty) {
        lock.readLock().lock();
        try {
            return delegate.objectSubPropertyAxiomsForSuperProperty(superProperty);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLReflexiveObjectPropertyAxiom> reflexiveObjectPropertyAxioms(
            OWLObjectPropertyExpression property) {
        lock.readLock().lock();
        try {
            return delegate.reflexiveObjectPropertyAxioms(property);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLSameIndividualAxiom> sameIndividualAxioms(OWLIndividual individual) {
        lock.readLock().lock();
        try {
            return delegate.sameIndividualAxioms(individual);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLSubClassOfAxiom> subClassAxiomsForSubClass(OWLClass cls) {
        lock.readLock().lock();
        try {
            return delegate.subClassAxiomsForSubClass(cls);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLSubClassOfAxiom> subClassAxiomsForSuperClass(OWLClass cls) {
        lock.readLock().lock();
        try {
            return delegate.subClassAxiomsForSuperClass(cls);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLSymmetricObjectPropertyAxiom> symmetricObjectPropertyAxioms(
            OWLObjectPropertyExpression property) {
        lock.readLock().lock();
        try {
            return delegate.symmetricObjectPropertyAxioms(property);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLTransitiveObjectPropertyAxiom> transitiveObjectPropertyAxioms(
            OWLObjectPropertyExpression property) {
        lock.readLock().lock();
        try {
            return delegate.transitiveObjectPropertyAxioms(property);
        } finally {
            lock.readLock().unlock();
        }
    }
}
