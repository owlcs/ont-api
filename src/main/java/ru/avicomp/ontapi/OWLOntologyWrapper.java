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
package ru.avicomp.ontapi;

import org.semanticweb.owlapi.io.OWLOntologyDocumentTarget;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.AxiomAnnotations;
import org.semanticweb.owlapi.model.parameters.ChangeApplied;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.model.parameters.Navigation;
import org.semanticweb.owlapi.util.OWLAxiomSearchFilter;

import javax.annotation.Nullable;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A decorator for any {@link OWLOntology} instance.
 * Notices that it does not implement {@link OntologyModel} and therefore cannot be used in ONT-API directly.
 * <p>
 * Matthew Horridge Stanford Center for Biomedical Informatics Research 03/04/15
 *
 * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/concurrent/ConcurrentOWLOntologyImpl.java'>uk.ac.manchester.cs.owl.owlapi.concurrent.ConcurrentOWLOntologyImpl</a>
 */
@SuppressWarnings({"deprecation", "NullableProblems", "WeakerAccess"})
public class OWLOntologyWrapper implements OWLMutableOntology {

    protected final OWLOntology delegate;
    protected final ReadWriteLock lock;

    /**
     * Constructs a new {@code OWLOntologyWrapper} that provides concurrent access to the specified delegate {@link OWLOntology ontology}.
     *
     * @param delegate {@link OWLOntology} the delegate
     * @param lock     {@link ReadWriteLock} the R/W Lock to provide thread-safe or {@code null} for non-concurrent mode
     */
    public OWLOntologyWrapper(OWLOntology delegate, ReadWriteLock lock) {
        this.delegate = Objects.requireNonNull(delegate, "Null delegate");
        this.lock = lock == null ? NoOpReadWriteLock.NO_OP_RW_LOCK : lock;
    }

    /**
     * Gets R/W Lock associated with this ontology instance.
     *
     * @return {@link ReadWriteLock}
     */
    public ReadWriteLock getLock() {
        return lock;
    }

    /**
     * Answers {@code true} if this ontology must be thread-safe.
     *
     * @return boolean
     */
    public boolean isConcurrent() {
        return NoOpReadWriteLock.isConcurrent(lock);
    }

    /**
     * Performs final actions over the axiom stream before release out.
     * <p>
     * Currently, it is only for ensuring safety if it is a multithreaded environment,
     * as indicated by the parameter {@link #isConcurrent()}.
     * If {@link #isConcurrent()} is {@code true} then the collecting must not go beyond this method,
     * otherwise it is allowed to be lazy.
     * This class does not produce parallel streams due to dangerous of livelocks or even deadlocks
     * while interacting with load-cache (see {@link ru.avicomp.ontapi.internal.InternalModel}),
     * which is used {@code ConcurrentMap} inside.
     * On the other hand, OWL-API implementation (and, as a consequence, ONT-API) uses {@code ReadWriteLock} everywhere
     * and therefore without this method there is a dangerous of {@link java.util.ConcurrentModificationException},
     * if some processing are allowed outside the method.
     *
     * @param res Stream of {@link R}s
     * @param <R> anything
     * @return Stream of {@link R}s
     */
    protected <R> Stream<R> reduce(Stream<R> res) {
        // use ArrayList since it is faster in common cases than HashSet,
        // and unique is provided by other mechanisms:
        return isConcurrent() ? res.collect(Collectors.toList()).stream() : res;
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
            if (obj == this) return true;
            if (obj instanceof OWLOntologyWrapper) {
                obj = ((OWLOntologyWrapper) obj).delegate;
            }
            return delegate.equals(obj);
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
    public int compareTo(OWLObject o) {
        lock.readLock().lock();
        try {
            return delegate.compareTo(o);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int typeIndex() {
        return delegate.typeIndex();
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
        lock.readLock().lock();
        try {
            return delegate.getOWLOntologyManager();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void setOWLOntologyManager(@Nullable OWLOntologyManager manager) {
        lock.writeLock().lock();
        try {
            delegate.setOWLOntologyManager(manager);
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
    public Set<OWLAnnotation> getAnnotations() {
        lock.readLock().lock();
        try {
            return delegate.getAnnotations();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
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
            return reduce(delegate.directImportsDocuments());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
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
            return reduce(delegate.directImports());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
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
            return reduce(delegate.imports());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
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
            return reduce(delegate.importsClosure());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
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
    public Set<OWLAxiom> getTBoxAxioms(Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.getTBoxAxioms(imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLAxiom> getABoxAxioms(Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.getABoxAxioms(imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
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
            return reduce(delegate.tboxAxioms(imports));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLAxiom> aboxAxioms(Imports imports) {
        lock.readLock().lock();
        try {
            return reduce(delegate.aboxAxioms(imports));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLAxiom> rboxAxioms(Imports imports) {
        lock.readLock().lock();
        try {
            return reduce(delegate.rboxAxioms(imports));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLClassAxiom> getGeneralClassAxioms() {
        lock.readLock().lock();
        try {
            return delegate.getGeneralClassAxioms();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLEntity> getSignature() {
        lock.readLock().lock();
        try {
            return delegate.getSignature();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
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
            return reduce(delegate.generalClassAxioms());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLEntity> signature() {
        lock.readLock().lock();
        try {
            return reduce(delegate.signature());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLEntity> signature(Imports imports) {
        lock.readLock().lock();
        try {
            return reduce(delegate.signature(imports));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean isDeclared(OWLEntity entity) {
        lock.readLock().lock();
        try {
            return delegate.isDeclared(entity);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean isDeclared(OWLEntity entity, Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.isDeclared(entity, imports);
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
    public void saveOntology(OutputStream stream) throws OWLOntologyStorageException {
        lock.readLock().lock();
        try {
            delegate.saveOntology(stream);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void saveOntology(OWLDocumentFormat format) throws OWLOntologyStorageException {
        lock.readLock().lock();
        try {
            delegate.saveOntology(format);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void saveOntology(OWLDocumentFormat format, IRI iri) throws OWLOntologyStorageException {
        lock.readLock().lock();
        try {
            delegate.saveOntology(format, iri);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void saveOntology(OWLDocumentFormat format, OutputStream stream) throws OWLOntologyStorageException {
        lock.readLock().lock();
        try {
            delegate.saveOntology(format, stream);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void saveOntology(OWLOntologyDocumentTarget target) throws OWLOntologyStorageException {
        lock.readLock().lock();
        try {
            delegate.saveOntology(target);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void saveOntology(OWLDocumentFormat format, OWLOntologyDocumentTarget target) throws OWLOntologyStorageException {
        lock.readLock().lock();
        try {
            delegate.saveOntology(format, target);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLClassExpression> getNestedClassExpressions() {
        lock.readLock().lock();
        try {
            return delegate.getNestedClassExpressions();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean containsEntityInSignature(OWLEntity entity) {
        lock.readLock().lock();
        try {
            return delegate.containsEntityInSignature(entity);
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
    public boolean containsEntitiesOfTypeInSignature(EntityType<?> type, Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.containsEntitiesOfTypeInSignature(type, imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLAnonymousIndividual> getAnonymousIndividuals() {
        lock.readLock().lock();
        try {
            return delegate.getAnonymousIndividuals();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLClass> getClassesInSignature() {
        lock.readLock().lock();
        try {
            return delegate.getClassesInSignature();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLObjectProperty> getObjectPropertiesInSignature() {
        lock.readLock().lock();
        try {
            return delegate.getObjectPropertiesInSignature();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLDataProperty> getDataPropertiesInSignature() {
        lock.readLock().lock();
        try {
            return delegate.getDataPropertiesInSignature();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLNamedIndividual> getIndividualsInSignature() {
        lock.readLock().lock();
        try {
            return delegate.getIndividualsInSignature();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLDatatype> getDatatypesInSignature() {
        lock.readLock().lock();
        try {
            return delegate.getDatatypesInSignature();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLAnnotationProperty> getAnnotationPropertiesInSignature() {
        lock.readLock().lock();
        try {
            return delegate.getAnnotationPropertiesInSignature();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
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
    public <T extends OWLAxiom> Set<T> getAxioms(AxiomType<T> type, Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.getAxioms(type, imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public <T extends OWLAxiom> Stream<T> axioms(AxiomType<T> type, Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.axioms(type, imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public <T extends OWLAxiom> int getAxiomCount(AxiomType<T> type, Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.getAxiomCount(type, imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean containsAxiom(OWLAxiom axiom, Imports imports, AxiomAnnotations annotations) {
        lock.readLock().lock();
        try {
            return delegate.containsAxiom(axiom, imports, annotations);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLAxiom> getAxiomsIgnoreAnnotations(OWLAxiom axiom, Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.getAxiomsIgnoreAnnotations(axiom, imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLAxiom> axiomsIgnoreAnnotations(OWLAxiom axiom, Imports imports) {
        lock.readLock().lock();
        try {
            return reduce(delegate.axiomsIgnoreAnnotations(axiom, imports));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLAxiom> getReferencingAxioms(OWLPrimitive primitive, Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.getReferencingAxioms(primitive, imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLAxiom> referencingAxioms(OWLPrimitive primitive, Imports imports) {
        lock.readLock().lock();
        try {
            return reduce(delegate.referencingAxioms(primitive, imports));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLClassAxiom> getAxioms(OWLClass clazz, Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.getAxioms(clazz, imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLObjectPropertyAxiom> getAxioms(OWLObjectPropertyExpression property, Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.getAxioms(property, imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLDataPropertyAxiom> getAxioms(OWLDataProperty property, Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.getAxioms(property, imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLIndividualAxiom> getAxioms(OWLIndividual individual, Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.getAxioms(individual, imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLAnnotationAxiom> getAxioms(OWLAnnotationProperty property, Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.getAxioms(property, imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLDatatypeDefinitionAxiom> getAxioms(OWLDatatype datatype, Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.getAxioms(datatype, imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
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
        lock.readLock().lock();
        try {
            return reduce(delegate.axioms());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
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
            return reduce(delegate.logicalAxioms());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public <T extends OWLAxiom> Set<T> getAxioms(AxiomType<T> type) {
        lock.readLock().lock();
        try {
            return delegate.getAxioms(type);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public <T extends OWLAxiom> Stream<T> axioms(AxiomType<T> type) {
        lock.readLock().lock();
        try {
            return delegate.axioms(type);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean containsAxiom(OWLAxiom axiom) {
        lock.readLock().lock();
        try {
            return delegate.containsAxiom(axiom);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLAxiom> getAxioms(boolean b) {
        lock.readLock().lock();
        try {
            return delegate.getAxioms(b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int getAxiomCount(boolean b) {
        lock.readLock().lock();
        try {
            return delegate.getAxiomCount(b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLLogicalAxiom> getLogicalAxioms(boolean b) {
        lock.readLock().lock();
        try {
            return delegate.getLogicalAxioms(b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int getLogicalAxiomCount(boolean b) {
        lock.readLock().lock();
        try {
            return delegate.getLogicalAxiomCount(b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public <T extends OWLAxiom> Set<T> getAxioms(AxiomType<T> axiomType, boolean b) {
        lock.readLock().lock();
        try {
            return delegate.getAxioms(axiomType, b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public <T extends OWLAxiom> int getAxiomCount(AxiomType<T> axiomType, boolean b) {
        lock.readLock().lock();
        try {
            return delegate.getAxiomCount(axiomType, b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean containsAxiom(OWLAxiom axiom, boolean b) {
        lock.readLock().lock();
        try {
            return delegate.containsAxiom(axiom, b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean containsAxiomIgnoreAnnotations(OWLAxiom axiom, boolean b) {
        lock.readLock().lock();
        try {
            return delegate.containsAxiomIgnoreAnnotations(axiom, b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLAxiom> getAxiomsIgnoreAnnotations(OWLAxiom axiom, boolean b) {
        lock.readLock().lock();
        try {
            return delegate.getAxiomsIgnoreAnnotations(axiom, b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLAxiom> getReferencingAxioms(OWLPrimitive owlPrimitive, boolean b) {
        lock.readLock().lock();
        try {
            return delegate.getReferencingAxioms(owlPrimitive, b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLClassAxiom> getAxioms(OWLClass owlClass, boolean b) {
        lock.readLock().lock();
        try {
            return delegate.getAxioms(owlClass, b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLObjectPropertyAxiom> getAxioms(OWLObjectPropertyExpression owlObjectPropertyExpression, boolean b) {
        lock.readLock().lock();
        try {
            return delegate.getAxioms(owlObjectPropertyExpression, b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLDataPropertyAxiom> getAxioms(OWLDataProperty owlDataProperty, boolean b) {
        lock.readLock().lock();
        try {
            return delegate.getAxioms(owlDataProperty, b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLIndividualAxiom> getAxioms(OWLIndividual individual, boolean b) {
        lock.readLock().lock();
        try {
            return delegate.getAxioms(individual, b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLAnnotationAxiom> getAxioms(OWLAnnotationProperty property, boolean b) {
        lock.readLock().lock();
        try {
            return delegate.getAxioms(property, b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
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
    public boolean containsAxiomIgnoreAnnotations(OWLAxiom axiom) {
        lock.readLock().lock();
        try {
            return delegate.containsAxiomIgnoreAnnotations(axiom);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLAxiom> getAxiomsIgnoreAnnotations(OWLAxiom axiom) {
        lock.readLock().lock();
        try {
            return delegate.getAxiomsIgnoreAnnotations(axiom);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLAxiom> axiomsIgnoreAnnotations(OWLAxiom axiom) {
        lock.readLock().lock();
        try {
            return reduce(delegate.axiomsIgnoreAnnotations(axiom));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
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
            return reduce(delegate.referencingAxioms(owlPrimitive));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLClassAxiom> getAxioms(OWLClass owlClass) {
        lock.readLock().lock();
        try {
            return delegate.getAxioms(owlClass);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLObjectPropertyAxiom> getAxioms(OWLObjectPropertyExpression owlObjectPropertyExpression) {
        lock.readLock().lock();
        try {
            return delegate.getAxioms(owlObjectPropertyExpression);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLDataPropertyAxiom> getAxioms(OWLDataProperty owlDataProperty) {
        lock.readLock().lock();
        try {
            return delegate.getAxioms(owlDataProperty);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLIndividualAxiom> getAxioms(OWLIndividual individual) {
        lock.readLock().lock();
        try {
            return delegate.getAxioms(individual);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLAnnotationAxiom> getAxioms(OWLAnnotationProperty property) {
        lock.readLock().lock();
        try {
            return delegate.getAxioms(property);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
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
            return reduce(delegate.axioms(owlClass));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLObjectPropertyAxiom> axioms(OWLObjectPropertyExpression owlObjectPropertyExpression) {
        lock.readLock().lock();
        try {
            return reduce(delegate.axioms(owlObjectPropertyExpression));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLDataPropertyAxiom> axioms(OWLDataProperty owlDataProperty) {
        lock.readLock().lock();
        try {
            return reduce(delegate.axioms(owlDataProperty));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLIndividualAxiom> axioms(OWLIndividual individual) {
        lock.readLock().lock();
        try {
            return reduce(delegate.axioms(individual));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLAnnotationAxiom> axioms(OWLAnnotationProperty property) {
        lock.readLock().lock();
        try {
            return reduce(delegate.axioms(property));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLDatatypeDefinitionAxiom> axioms(OWLDatatype owlDatatype) {
        lock.readLock().lock();
        try {
            return reduce(delegate.axioms(owlDatatype));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLClass> getClassesInSignature(Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.getClassesInSignature(imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLObjectProperty> getObjectPropertiesInSignature(Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.getObjectPropertiesInSignature(imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLDataProperty> getDataPropertiesInSignature(Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.getDataPropertiesInSignature(imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLNamedIndividual> getIndividualsInSignature(Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.getIndividualsInSignature(imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
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
            return reduce(delegate.referencedAnonymousIndividuals(imports));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLAnonymousIndividual> referencedAnonymousIndividuals() {
        lock.readLock().lock();
        try {
            return reduce(delegate.referencedAnonymousIndividuals());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLDatatype> getDatatypesInSignature(Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.getDatatypesInSignature(imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLAnnotationProperty> getAnnotationPropertiesInSignature(Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.getAnnotationPropertiesInSignature(imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean containsEntityInSignature(OWLEntity entity, Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.containsEntityInSignature(entity, imports);
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
    public boolean containsReference(OWLEntity entity, Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.containsReference(entity, imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean containsReference(OWLEntity entity) {
        lock.readLock().lock();
        try {
            return delegate.containsReference(entity);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
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
            return reduce(delegate.entitiesInSignature(iri));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLClass> getClassesInSignature(boolean b) {
        lock.readLock().lock();
        try {
            return delegate.getClassesInSignature(b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLObjectProperty> getObjectPropertiesInSignature(boolean b) {
        lock.readLock().lock();
        try {
            return delegate.getObjectPropertiesInSignature(b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLDataProperty> getDataPropertiesInSignature(boolean b) {
        lock.readLock().lock();
        try {
            return delegate.getDataPropertiesInSignature(b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLNamedIndividual> getIndividualsInSignature(boolean b) {
        lock.readLock().lock();
        try {
            return delegate.getIndividualsInSignature(b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLAnonymousIndividual> getReferencedAnonymousIndividuals(boolean b) {
        lock.readLock().lock();
        try {
            return delegate.getReferencedAnonymousIndividuals(b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLDatatype> getDatatypesInSignature(boolean b) {
        lock.readLock().lock();
        try {
            return delegate.getDatatypesInSignature(b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLAnnotationProperty> getAnnotationPropertiesInSignature(boolean b) {
        lock.readLock().lock();
        try {
            return delegate.getAnnotationPropertiesInSignature(b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean containsEntityInSignature(OWLEntity entity, boolean b) {
        lock.readLock().lock();
        try {
            return delegate.containsEntityInSignature(entity, b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean containsEntityInSignature(IRI iri, boolean b) {
        lock.readLock().lock();
        try {
            return delegate.containsEntityInSignature(iri, b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean containsClassInSignature(IRI iri, boolean b) {
        lock.readLock().lock();
        try {
            return delegate.containsClassInSignature(iri, b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean containsObjectPropertyInSignature(IRI iri, boolean b) {
        lock.readLock().lock();
        try {
            return delegate.containsObjectPropertyInSignature(iri, b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean containsDataPropertyInSignature(IRI iri, boolean b) {
        lock.readLock().lock();
        try {
            return delegate.containsDataPropertyInSignature(iri, b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean containsAnnotationPropertyInSignature(IRI iri, boolean b) {
        lock.readLock().lock();
        try {
            return delegate.containsAnnotationPropertyInSignature(iri, b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean containsDatatypeInSignature(IRI iri, boolean b) {
        lock.readLock().lock();
        try {
            return delegate.containsDatatypeInSignature(iri, b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean containsIndividualInSignature(IRI iri, boolean b) {
        lock.readLock().lock();
        try {
            return delegate.containsIndividualInSignature(iri, b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLEntity> getEntitiesInSignature(IRI iri, boolean b) {
        lock.readLock().lock();
        try {
            return delegate.getEntitiesInSignature(iri, b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean containsReference(OWLEntity entity, boolean b) {
        lock.readLock().lock();
        try {
            return delegate.containsReference(entity, b);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public <T extends OWLAxiom> Set<T> getAxioms(Class<T> type, OWLObject object, Imports imports, Navigation navigation) {
        lock.readLock().lock();
        try {
            return delegate.getAxioms(type, object, imports, navigation);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public <T extends OWLAxiom> Stream<T> axioms(Class<T> type, OWLObject object, Imports imports, Navigation navigation) {
        lock.readLock().lock();
        try {
            return delegate.axioms(type, object, imports, navigation);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public <T extends OWLAxiom> Collection<T> filterAxioms(OWLAxiomSearchFilter filter, Object o, Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.filterAxioms(filter, o, imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean contains(OWLAxiomSearchFilter filter, Object o, Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.contains(filter, o, imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean contains(OWLAxiomSearchFilter filter, Object o) {
        lock.readLock().lock();
        try {
            return delegate.contains(filter, o);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public <T extends OWLAxiom> Set<T> getAxioms(Class<T> type,
                                                 Class<? extends OWLObject> explicitClass,
                                                 OWLObject object,
                                                 Imports imports,
                                                 Navigation navigation) {
        lock.readLock().lock();
        try {
            return delegate.getAxioms(type, explicitClass, object, imports, navigation);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public <T extends OWLAxiom> Stream<T> axioms(Class<T> type,
                                                 Class<? extends OWLObject> explicitClass,
                                                 OWLObject object,
                                                 Imports imports,
                                                 Navigation navigation) {
        lock.readLock().lock();
        try {
            return delegate.axioms(type, explicitClass, object, imports, navigation);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLSubAnnotationPropertyOfAxiom> getSubAnnotationPropertyOfAxioms(OWLAnnotationProperty property) {
        lock.readLock().lock();
        try {
            return delegate.getSubAnnotationPropertyOfAxioms(property);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLAnnotationPropertyDomainAxiom> getAnnotationPropertyDomainAxioms(OWLAnnotationProperty property) {
        lock.readLock().lock();
        try {
            return delegate.getAnnotationPropertyDomainAxioms(property);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLAnnotationPropertyRangeAxiom> getAnnotationPropertyRangeAxioms(OWLAnnotationProperty property) {
        lock.readLock().lock();
        try {
            return delegate.getAnnotationPropertyRangeAxioms(property);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLAnnotationPropertyDomainAxiom> annotationPropertyDomainAxioms(OWLAnnotationProperty property) {
        lock.readLock().lock();
        try {
            return reduce(delegate.annotationPropertyDomainAxioms(property));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLAnnotationPropertyRangeAxiom> annotationPropertyRangeAxioms(OWLAnnotationProperty property) {
        lock.readLock().lock();
        try {
            return reduce(delegate.annotationPropertyRangeAxioms(property));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLDeclarationAxiom> getDeclarationAxioms(OWLEntity entity) {
        lock.readLock().lock();
        try {
            return delegate.getDeclarationAxioms(entity);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLAnnotationAssertionAxiom> getAnnotationAssertionAxioms(OWLAnnotationSubject subject) {
        lock.readLock().lock();
        try {
            return delegate.getAnnotationAssertionAxioms(subject);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLSubClassOfAxiom> getSubClassAxiomsForSubClass(OWLClass clazz) {
        lock.readLock().lock();
        try {
            return delegate.getSubClassAxiomsForSubClass(clazz);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLSubClassOfAxiom> getSubClassAxiomsForSuperClass(OWLClass clazz) {
        lock.readLock().lock();
        try {
            return delegate.getSubClassAxiomsForSuperClass(clazz);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLEquivalentClassesAxiom> getEquivalentClassesAxioms(OWLClass clazz) {
        lock.readLock().lock();
        try {
            return delegate.getEquivalentClassesAxioms(clazz);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLDisjointClassesAxiom> getDisjointClassesAxioms(OWLClass clazz) {
        lock.readLock().lock();
        try {
            return delegate.getDisjointClassesAxioms(clazz);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLDisjointUnionAxiom> getDisjointUnionAxioms(OWLClass clazz) {
        lock.readLock().lock();
        try {
            return delegate.getDisjointUnionAxioms(clazz);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLHasKeyAxiom> getHasKeyAxioms(OWLClass clazz) {
        lock.readLock().lock();
        try {
            return delegate.getHasKeyAxioms(clazz);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLSubObjectPropertyOfAxiom> getObjectSubPropertyAxiomsForSubProperty(OWLObjectPropertyExpression property) {
        lock.readLock().lock();
        try {
            return delegate.getObjectSubPropertyAxiomsForSubProperty(property);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLSubObjectPropertyOfAxiom> getObjectSubPropertyAxiomsForSuperProperty(OWLObjectPropertyExpression property) {
        lock.readLock().lock();
        try {
            return delegate.getObjectSubPropertyAxiomsForSuperProperty(property);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLObjectPropertyDomainAxiom> getObjectPropertyDomainAxioms(OWLObjectPropertyExpression property) {
        lock.readLock().lock();
        try {
            return delegate.getObjectPropertyDomainAxioms(property);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLObjectPropertyRangeAxiom> getObjectPropertyRangeAxioms(OWLObjectPropertyExpression property) {
        lock.readLock().lock();
        try {
            return delegate.getObjectPropertyRangeAxioms(property);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLInverseObjectPropertiesAxiom> getInverseObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        lock.readLock().lock();
        try {
            return delegate.getInverseObjectPropertyAxioms(property);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLEquivalentObjectPropertiesAxiom> getEquivalentObjectPropertiesAxioms(OWLObjectPropertyExpression property) {
        lock.readLock().lock();
        try {
            return delegate.getEquivalentObjectPropertiesAxioms(property);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLDisjointObjectPropertiesAxiom> getDisjointObjectPropertiesAxioms(OWLObjectPropertyExpression property) {
        lock.readLock().lock();
        try {
            return delegate.getDisjointObjectPropertiesAxioms(property);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLFunctionalObjectPropertyAxiom> getFunctionalObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        lock.readLock().lock();
        try {
            return delegate.getFunctionalObjectPropertyAxioms(property);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLInverseFunctionalObjectPropertyAxiom> getInverseFunctionalObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        lock.readLock().lock();
        try {
            return delegate.getInverseFunctionalObjectPropertyAxioms(property);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLSymmetricObjectPropertyAxiom> getSymmetricObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        lock.readLock().lock();
        try {
            return delegate.getSymmetricObjectPropertyAxioms(property);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLAsymmetricObjectPropertyAxiom> getAsymmetricObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        lock.readLock().lock();
        try {
            return delegate.getAsymmetricObjectPropertyAxioms(property);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLReflexiveObjectPropertyAxiom> getReflexiveObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        lock.readLock().lock();
        try {
            return delegate.getReflexiveObjectPropertyAxioms(property);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLIrreflexiveObjectPropertyAxiom> getIrreflexiveObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        lock.readLock().lock();
        try {
            return delegate.getIrreflexiveObjectPropertyAxioms(property);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLTransitiveObjectPropertyAxiom> getTransitiveObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        lock.readLock().lock();
        try {
            return delegate.getTransitiveObjectPropertyAxioms(property);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLSubDataPropertyOfAxiom> getDataSubPropertyAxiomsForSubProperty(OWLDataProperty property) {
        lock.readLock().lock();
        try {
            return delegate.getDataSubPropertyAxiomsForSubProperty(property);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLSubDataPropertyOfAxiom> getDataSubPropertyAxiomsForSuperProperty(OWLDataPropertyExpression property) {
        lock.readLock().lock();
        try {
            return delegate.getDataSubPropertyAxiomsForSuperProperty(property);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLDataPropertyDomainAxiom> getDataPropertyDomainAxioms(OWLDataProperty property) {
        lock.readLock().lock();
        try {
            return delegate.getDataPropertyDomainAxioms(property);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLDataPropertyRangeAxiom> getDataPropertyRangeAxioms(OWLDataProperty property) {
        lock.readLock().lock();
        try {
            return delegate.getDataPropertyRangeAxioms(property);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLEquivalentDataPropertiesAxiom> getEquivalentDataPropertiesAxioms(OWLDataProperty property) {
        lock.readLock().lock();
        try {
            return delegate.getEquivalentDataPropertiesAxioms(property);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLDisjointDataPropertiesAxiom> getDisjointDataPropertiesAxioms(OWLDataProperty property) {
        lock.readLock().lock();
        try {
            return delegate.getDisjointDataPropertiesAxioms(property);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLFunctionalDataPropertyAxiom> getFunctionalDataPropertyAxioms(OWLDataPropertyExpression property) {
        lock.readLock().lock();
        try {
            return delegate.getFunctionalDataPropertyAxioms(property);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLClassAssertionAxiom> getClassAssertionAxioms(OWLIndividual individual) {
        lock.readLock().lock();
        try {
            return delegate.getClassAssertionAxioms(individual);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLClassAssertionAxiom> getClassAssertionAxioms(OWLClassExpression clazz) {
        lock.readLock().lock();
        try {
            return delegate.getClassAssertionAxioms(clazz);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLDataPropertyAssertionAxiom> getDataPropertyAssertionAxioms(OWLIndividual individual) {
        lock.readLock().lock();
        try {
            return delegate.getDataPropertyAssertionAxioms(individual);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLObjectPropertyAssertionAxiom> getObjectPropertyAssertionAxioms(OWLIndividual individual) {
        lock.readLock().lock();
        try {
            return delegate.getObjectPropertyAssertionAxioms(individual);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLNegativeObjectPropertyAssertionAxiom> getNegativeObjectPropertyAssertionAxioms(OWLIndividual individual) {
        lock.readLock().lock();
        try {
            return delegate.getNegativeObjectPropertyAssertionAxioms(individual);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLNegativeDataPropertyAssertionAxiom> getNegativeDataPropertyAssertionAxioms(OWLIndividual individual) {
        lock.readLock().lock();
        try {
            return delegate.getNegativeDataPropertyAssertionAxioms(individual);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLSameIndividualAxiom> getSameIndividualAxioms(OWLIndividual individual) {
        lock.readLock().lock();
        try {
            return delegate.getSameIndividualAxioms(individual);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLDifferentIndividualsAxiom> getDifferentIndividualAxioms(OWLIndividual individual) {
        lock.readLock().lock();
        try {
            return delegate.getDifferentIndividualAxioms(individual);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLDatatypeDefinitionAxiom> getDatatypeDefinitions(OWLDatatype datatype) {
        lock.readLock().lock();
        try {
            return delegate.getDatatypeDefinitions(datatype);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public ChangeApplied applyChange(OWLOntologyChange change) {
        lock.writeLock().lock();
        try {
            return getMutableOntology().applyChange(change);
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
    public ChangeApplied addAxiom(OWLAxiom axiom) {
        lock.writeLock().lock();
        try {
            return getMutableOntology().addAxiom(axiom);
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
    public ChangeApplied add(OWLAxiom axiom) {
        lock.writeLock().lock();
        try {
            return getMutableOntology().add(axiom);
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
            return reduce(delegate.importsDeclarations());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public <T extends OWLAxiom> Stream<T> axioms(OWLAxiomSearchFilter filter, Object key, Imports includeImportsClosure) {
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
                                                 Class<? extends OWLObject> explicitClass,
                                                 OWLObject entity,
                                                 Navigation forSubPosition) {
        lock.readLock().lock();
        try {
            return delegate.axioms(type, explicitClass, entity, forSubPosition);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLSubAnnotationPropertyOfAxiom> subAnnotationPropertyOfAxioms(OWLAnnotationProperty property) {
        lock.readLock().lock();
        try {
            return reduce(delegate.subAnnotationPropertyOfAxioms(property));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLDatatypeDefinitionAxiom> datatypeDefinitions(OWLDatatype datatype) {
        lock.readLock().lock();
        try {
            return reduce(delegate.datatypeDefinitions(datatype));
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
    public Stream<OWLDisjointObjectPropertiesAxiom> disjointObjectPropertiesAxioms(OWLObjectPropertyExpression property) {
        lock.readLock().lock();
        try {
            return reduce(delegate.disjointObjectPropertiesAxioms(property));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLObjectProperty> objectPropertiesInSignature() {
        lock.readLock().lock();
        try {
            return reduce(delegate.objectPropertiesInSignature());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLAnnotationAssertionAxiom> annotationAssertionAxioms(OWLAnnotationSubject entity) {
        lock.readLock().lock();
        try {
            return reduce(delegate.annotationAssertionAxioms(entity));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLAnnotationAssertionAxiom> annotationAssertionAxioms(OWLAnnotationSubject entity, Imports imports) {
        lock.readLock().lock();
        try {
            return reduce(delegate.annotationAssertionAxioms(entity, imports));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLAnnotationProperty> annotationPropertiesInSignature() {
        lock.readLock().lock();
        try {
            return reduce(delegate.annotationPropertiesInSignature());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLAnnotationProperty> annotationPropertiesInSignature(Imports imports) {
        lock.readLock().lock();
        try {
            return reduce(delegate.annotationPropertiesInSignature(imports));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLAnnotation> annotations() {
        lock.readLock().lock();
        try {
            return reduce(delegate.annotations());
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
            return reduce(delegate.annotations(p));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLAnnotation> annotations(Predicate<OWLAnnotation> p) {
        lock.readLock().lock();
        try {
            return reduce(delegate.annotations(p));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLAnonymousIndividual> anonymousIndividuals() {
        lock.readLock().lock();
        try {
            return reduce(delegate.anonymousIndividuals());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLAsymmetricObjectPropertyAxiom> asymmetricObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        lock.readLock().lock();
        try {
            return reduce(delegate.asymmetricObjectPropertyAxioms(property));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public <T extends OWLAxiom> Stream<T> axioms(Class<T> type, OWLObject entity, Navigation forSubPosition) {
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
            return reduce(delegate.axioms(imports));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLAnnotationAxiom> axioms(OWLAnnotationProperty property, Imports imports) {
        lock.readLock().lock();
        try {
            return reduce(delegate.axioms(property, imports));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLClassAxiom> axioms(OWLClass clazz, Imports imports) {
        lock.readLock().lock();
        try {
            return reduce(delegate.axioms(clazz, imports));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLDataPropertyAxiom> axioms(OWLDataProperty property, Imports imports) {
        lock.readLock().lock();
        try {
            return reduce(delegate.axioms(property, imports));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLDatatypeDefinitionAxiom> axioms(OWLDatatype datatype, Imports imports) {
        lock.readLock().lock();
        try {
            return reduce(delegate.axioms(datatype, imports));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLIndividualAxiom> axioms(OWLIndividual individual, Imports imports) {
        lock.readLock().lock();
        try {
            return reduce(delegate.axioms(individual, imports));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLObjectPropertyAxiom> axioms(OWLObjectPropertyExpression property, Imports imports) {
        lock.readLock().lock();
        try {
            return reduce(delegate.axioms(property, imports));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLClassAssertionAxiom> classAssertionAxioms(OWLClassExpression ce) {
        lock.readLock().lock();
        try {
            return reduce(delegate.classAssertionAxioms(ce));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLClassAssertionAxiom> classAssertionAxioms(OWLIndividual individual) {
        lock.readLock().lock();
        try {
            return reduce(delegate.classAssertionAxioms(individual));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLClass> classesInSignature() {
        lock.readLock().lock();
        try {
            return reduce(delegate.classesInSignature());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLClass> classesInSignature(Imports imports) {
        lock.readLock().lock();
        try {
            return reduce(delegate.classesInSignature(imports));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLDataProperty> dataPropertiesInSignature() {
        lock.readLock().lock();
        try {
            return reduce(delegate.dataPropertiesInSignature());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLDataProperty> dataPropertiesInSignature(Imports imports) {
        lock.readLock().lock();
        try {
            return reduce(delegate.dataPropertiesInSignature(imports));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLDataPropertyAssertionAxiom> dataPropertyAssertionAxioms(OWLIndividual individual) {
        lock.readLock().lock();
        try {
            return reduce(delegate.dataPropertyAssertionAxioms(individual));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLDataPropertyDomainAxiom> dataPropertyDomainAxioms(OWLDataProperty property) {
        lock.readLock().lock();
        try {
            return reduce(delegate.dataPropertyDomainAxioms(property));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLDataPropertyRangeAxiom> dataPropertyRangeAxioms(OWLDataProperty property) {
        lock.readLock().lock();
        try {
            return reduce(delegate.dataPropertyRangeAxioms(property));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLSubDataPropertyOfAxiom> dataSubPropertyAxiomsForSubProperty(OWLDataProperty property) {
        lock.readLock().lock();
        try {
            return reduce(delegate.dataSubPropertyAxiomsForSubProperty(property));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLSubDataPropertyOfAxiom> dataSubPropertyAxiomsForSuperProperty(OWLDataPropertyExpression property) {
        lock.readLock().lock();
        try {
            return reduce(delegate.dataSubPropertyAxiomsForSuperProperty(property));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLDatatype> datatypesInSignature() {
        lock.readLock().lock();
        try {
            return reduce(delegate.datatypesInSignature());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLDatatype> datatypesInSignature(Imports imports) {
        lock.readLock().lock();
        try {
            return reduce(delegate.datatypesInSignature(imports));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLDeclarationAxiom> declarationAxioms(OWLEntity subject) {
        lock.readLock().lock();
        try {
            return reduce(delegate.declarationAxioms(subject));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLDifferentIndividualsAxiom> differentIndividualAxioms(OWLIndividual individual) {
        lock.readLock().lock();
        try {
            return reduce(delegate.differentIndividualAxioms(individual));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLDisjointClassesAxiom> disjointClassesAxioms(OWLClass clazz) {
        lock.readLock().lock();
        try {
            return reduce(delegate.disjointClassesAxioms(clazz));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLDisjointDataPropertiesAxiom> disjointDataPropertiesAxioms(OWLDataProperty property) {
        lock.readLock().lock();
        try {
            return reduce(delegate.disjointDataPropertiesAxioms(property));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLDisjointUnionAxiom> disjointUnionAxioms(OWLClass owlClass) {
        lock.readLock().lock();
        try {
            return reduce(delegate.disjointUnionAxioms(owlClass));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLEntity> entitiesInSignature(IRI iri, Imports imports) {
        lock.readLock().lock();
        try {
            return reduce(delegate.entitiesInSignature(iri, imports));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLEquivalentClassesAxiom> equivalentClassesAxioms(OWLClass clazz) {
        lock.readLock().lock();
        try {
            return reduce(delegate.equivalentClassesAxioms(clazz));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLEquivalentDataPropertiesAxiom> equivalentDataPropertiesAxioms(OWLDataProperty property) {
        lock.readLock().lock();
        try {
            return reduce(delegate.equivalentDataPropertiesAxioms(property));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLEquivalentObjectPropertiesAxiom> equivalentObjectPropertiesAxioms(OWLObjectPropertyExpression property) {
        lock.readLock().lock();
        try {
            return reduce(delegate.equivalentObjectPropertiesAxioms(property));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public <T extends OWLAxiom> Collection<T> filterAxioms(OWLAxiomSearchFilter filter, Object key) {
        lock.readLock().lock();
        try {
            return delegate.filterAxioms(filter, key);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLFunctionalDataPropertyAxiom> functionalDataPropertyAxioms(OWLDataPropertyExpression property) {
        lock.readLock().lock();
        try {
            return reduce(delegate.functionalDataPropertyAxioms(property));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLFunctionalObjectPropertyAxiom> functionalObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        lock.readLock().lock();
        try {
            return reduce(delegate.functionalObjectPropertyAxioms(property));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLAnnotationAssertionAxiom> getAnnotationAssertionAxioms(OWLAnnotationSubject entity, Imports imports) {
        lock.readLock().lock();
        try {
            return delegate.getAnnotationAssertionAxioms(entity, imports);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<OWLAnnotation> getAnnotations(OWLAnnotationProperty property) {
        lock.readLock().lock();
        try {
            return delegate.getAnnotations(property);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public <T extends OWLAxiom> Set<T> getAxioms(Class<T> type,
                                                 Class<? extends OWLObject> explicitClass,
                                                 OWLObject entity,
                                                 Navigation forSubPosition) {
        lock.readLock().lock();
        try {
            return delegate.getAxioms(type, explicitClass, entity, forSubPosition);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public <T extends OWLAxiom> Set<T> getAxioms(Class<T> type, OWLObject entity, Navigation navigation) {
        lock.readLock().lock();
        try {
            return delegate.getAxioms(type, entity, navigation);
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
    public Set<OWLAnonymousIndividual> getReferencedAnonymousIndividuals() {
        lock.readLock().lock();
        try {
            return delegate.getReferencedAnonymousIndividuals();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLHasKeyAxiom> hasKeyAxioms(OWLClass clazz) {
        lock.readLock().lock();
        try {
            return reduce(delegate.hasKeyAxioms(clazz));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLNamedIndividual> individualsInSignature() {
        lock.readLock().lock();
        try {
            return reduce(delegate.individualsInSignature());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLNamedIndividual> individualsInSignature(Imports imports) {
        lock.readLock().lock();
        try {
            return reduce(delegate.individualsInSignature(imports));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLInverseFunctionalObjectPropertyAxiom> inverseFunctionalObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        lock.readLock().lock();
        try {
            return reduce(delegate.inverseFunctionalObjectPropertyAxioms(property));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLInverseObjectPropertiesAxiom> inverseObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        lock.readLock().lock();
        try {
            return reduce(delegate.inverseObjectPropertyAxioms(property));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLIrreflexiveObjectPropertyAxiom> irreflexiveObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        lock.readLock().lock();
        try {
            return reduce(delegate.irreflexiveObjectPropertyAxioms(property));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLLogicalAxiom> logicalAxioms(Imports imports) {
        lock.readLock().lock();
        try {
            return reduce(delegate.logicalAxioms(imports));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLNegativeDataPropertyAssertionAxiom> negativeDataPropertyAssertionAxioms(OWLIndividual individual) {
        lock.readLock().lock();
        try {
            return reduce(delegate.negativeDataPropertyAssertionAxioms(individual));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLNegativeObjectPropertyAssertionAxiom> negativeObjectPropertyAssertionAxioms(OWLIndividual individual) {
        lock.readLock().lock();
        try {
            return reduce(delegate.negativeObjectPropertyAssertionAxioms(individual));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLClassExpression> nestedClassExpressions() {
        lock.readLock().lock();
        try {
            return reduce(delegate.nestedClassExpressions());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLObjectProperty> objectPropertiesInSignature(Imports imports) {
        lock.readLock().lock();
        try {
            return reduce(delegate.objectPropertiesInSignature(imports));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLObjectPropertyAssertionAxiom> objectPropertyAssertionAxioms(OWLIndividual individual) {
        lock.readLock().lock();
        try {
            return reduce(delegate.objectPropertyAssertionAxioms(individual));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLObjectPropertyDomainAxiom> objectPropertyDomainAxioms(OWLObjectPropertyExpression property) {
        lock.readLock().lock();
        try {
            return reduce(delegate.objectPropertyDomainAxioms(property));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLObjectPropertyRangeAxiom> objectPropertyRangeAxioms(OWLObjectPropertyExpression property) {
        lock.readLock().lock();
        try {
            return reduce(delegate.objectPropertyRangeAxioms(property));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLSubObjectPropertyOfAxiom> objectSubPropertyAxiomsForSubProperty(OWLObjectPropertyExpression property) {
        lock.readLock().lock();
        try {
            return reduce(delegate.objectSubPropertyAxiomsForSubProperty(property));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLSubObjectPropertyOfAxiom> objectSubPropertyAxiomsForSuperProperty(OWLObjectPropertyExpression property) {
        lock.readLock().lock();
        try {
            return reduce(delegate.objectSubPropertyAxiomsForSuperProperty(property));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLReflexiveObjectPropertyAxiom> reflexiveObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        lock.readLock().lock();
        try {
            return reduce(delegate.reflexiveObjectPropertyAxioms(property));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLSameIndividualAxiom> sameIndividualAxioms(OWLIndividual individual) {
        lock.readLock().lock();
        try {
            return reduce(delegate.sameIndividualAxioms(individual));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLSubClassOfAxiom> subClassAxiomsForSubClass(OWLClass clazz) {
        lock.readLock().lock();
        try {
            return reduce(delegate.subClassAxiomsForSubClass(clazz));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLSubClassOfAxiom> subClassAxiomsForSuperClass(OWLClass clazz) {
        lock.readLock().lock();
        try {
            return reduce(delegate.subClassAxiomsForSuperClass(clazz));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLSymmetricObjectPropertyAxiom> symmetricObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        lock.readLock().lock();
        try {
            return reduce(delegate.symmetricObjectPropertyAxioms(property));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<OWLTransitiveObjectPropertyAxiom> transitiveObjectPropertyAxioms(OWLObjectPropertyExpression property) {
        lock.readLock().lock();
        try {
            return reduce(delegate.transitiveObjectPropertyAxioms(property));
        } finally {
            lock.readLock().unlock();
        }
    }
}
