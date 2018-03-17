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

import org.apache.jena.graph.Graph;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.ChangeApplied;
import ru.avicomp.ontapi.internal.ConfigProvider;
import ru.avicomp.ontapi.internal.InternalModel;
import ru.avicomp.ontapi.internal.InternalModelHolder;
import ru.avicomp.ontapi.jena.ConcurrentGraph;
import ru.avicomp.ontapi.jena.OntModelFactory;
import ru.avicomp.ontapi.jena.UnionGraph;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.owlapi.ConcurrentOWLOntologyImpl;

import javax.annotation.Nonnull;
import java.util.concurrent.locks.ReadWriteLock;

import static org.semanticweb.owlapi.model.parameters.ChangeApplied.NO_OPERATION;
import static org.semanticweb.owlapi.model.parameters.ChangeApplied.SUCCESSFULLY;

/**
 * The main ontology model implementation. Not concurrent. Editable.
 * Provides access to {@link OntGraphModel}.
 *
 * Created by @szuev on 27.09.2016.
 * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLOntologyImpl.java'>uk.ac.manchester.cs.owl.owlapi.OWLOntologyImpl</a>
 * @see OntBaseModelImpl
 * @see OntologyModel
 */
@SuppressWarnings("WeakerAccess")
public class OntologyModelImpl extends OntBaseModelImpl implements OntologyModel {

    protected transient ChangeProcessor changer;

    /**
     * To construct fresh (empty) ontology.
     * @param manager ontology manager
     * @param id      the id
     */
    public OntologyModelImpl(OntologyManagerImpl manager, OWLOntologyID id) {
        super(manager, id);
    }

    /**
     * To construct an ontology based on graph.
     *
     * @param graph  {@link Graph}
     * @param config {@link OntologyManagerImpl.ModelConfig}
     */
    public OntologyModelImpl(Graph graph, OntologyManagerImpl.ModelConfig config) {
        super(graph, config);
    }

    @Override
    public ChangeApplied applyDirectChange(OWLOntologyChange change) {
        return change.accept(getChangeProcessor());
    }

    @Override
    public OntologyManagerImpl getOWLOntologyManager() {
        return (OntologyManagerImpl) super.getOWLOntologyManager();
    }

    protected ChangeProcessor getChangeProcessor() {
        return changer == null ? changer = new ChangeProcessor() : changer;
    }

    @Override
    public void clearCache() {
        getBase().clearCache();
    }

    /**
     * Returns the jena model shadow.
     *
     * @return {@link OntGraphModel}
     */
    @Override
    public OntGraphModel asGraphModel() {
        return getBase();
    }

    /**
     * Returns concurrent representation of model if it is allowed by manager.
     * For internal usage only
     *
     * @return {@link OntologyModel}
     */
    public OntologyModel asConcurrent() {
        OntologyManagerImpl manager = getOWLOntologyManager();
        if (!manager.isConcurrent()) {
            throw new OntApiException.Unsupported("Concurrency is not allowed.");
        }
        return new Concurrent(this, manager.getLock());
    }

    protected class ChangeProcessor implements OWLOntologyChangeVisitorEx<ChangeApplied> {

        @Override
        public ChangeApplied visit(@Nonnull AddAxiom change) {
            OWLAxiom axiom = change.getAxiom();
            if (containsAxiom(axiom)) {
                return NO_OPERATION;
            }
            getBase().add(axiom);
            return SUCCESSFULLY;
        }

        @Override
        public ChangeApplied visit(@Nonnull RemoveAxiom change) {
            OWLAxiom axiom = change.getAxiom();
            if (containsAxiom(axiom)) {
                getBase().remove(axiom);
                return SUCCESSFULLY;
            }
            return NO_OPERATION;
        }

        @Override
        public ChangeApplied visit(@Nonnull AddImport change) {
            OWLImportsDeclaration importDeclaration = change.getImportDeclaration();
            if (importsDeclarations().noneMatch(importDeclaration::equals)) {
                addImport(importDeclaration);
                return SUCCESSFULLY;
            }
            return NO_OPERATION;
        }

        @Override
        public ChangeApplied visit(@Nonnull RemoveImport change) {
            OWLImportsDeclaration importDeclaration = change.getImportDeclaration();
            if (importsDeclarations().anyMatch(importDeclaration::equals)) {
                removeImport(importDeclaration);
                return SUCCESSFULLY;
            }
            return NO_OPERATION;
        }

        @Override
        public ChangeApplied visit(@Nonnull AddOntologyAnnotation change) {
            OWLAnnotation annotation = change.getAnnotation();
            if (annotations().noneMatch(annotation::equals)) {
                getBase().add(annotation);
                return SUCCESSFULLY;
            }
            return NO_OPERATION;
        }

        @Override
        public ChangeApplied visit(@Nonnull RemoveOntologyAnnotation change) {
            OWLAnnotation annotation = change.getAnnotation();
            if (annotations().anyMatch(annotation::equals)) {
                getBase().remove(annotation);
                return SUCCESSFULLY;
            }
            return NO_OPERATION;
        }

        @Override
        public ChangeApplied visit(@Nonnull SetOntologyID change) {
            OWLOntologyID id = change.getNewOntologyID();
            if (getOntologyID().equals(id)) {
                return NO_OPERATION;
            }
            setOntologyID(id);
            return SUCCESSFULLY;
        }

        protected void addImport(OWLImportsDeclaration declaration) {
            // to match behaviour of OWL-API add to graph only single IRI - either ontology IRI or specified declaration IRI.
            OntologyModel ont = getOWLOntologyManager().getImportedOntology(declaration);
            if (ont == null) {
                getBase().getID().addImport(declaration.getIRI().getIRIString());
                return;
            }
            getBase().addImport(((InternalModelHolder) ont).getBase());
        }

        protected void removeImport(OWLImportsDeclaration declaration) {
            // to match behaviour of OWL-API removes both declaration IRI and ontology IRI (could be different in case of renaming)
            OntologyModel ont = getOWLOntologyManager().getImportedOntology(declaration);
            getBase().getID().removeImport(declaration.getIRI().getIRIString());
            if (ont == null) {
                return;
            }
            getBase().removeImport(((InternalModelHolder) ont).getBase());
        }
    }

    /**
     * The analogue of <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/concurrent/ConcurrentOWLOntologyImpl.java'>uk.ac.manchester.cs.owl.owlapi.concurrent.ConcurrentOWLOntologyImpl</a>.
     * <p>
     * Created by szuev on 22.12.2016.
     */
    @SuppressWarnings("WeakerAccess")
    public static class Concurrent extends ConcurrentOWLOntologyImpl implements OntologyModel, ConfigProvider, InternalModelHolder {
        protected OntologyModelImpl delegate;
        protected ReadWriteLock lock;

        protected Concurrent(OntologyModelImpl delegate, ReadWriteLock lock) {
            super(delegate, lock);
            this.delegate = delegate;
            this.lock = lock;
        }

        public ReadWriteLock getLock() {
            return lock;
        }

        /**
         * @return {@link OntGraphModel}
         */
        @Override
        public OntGraphModel asGraphModel() {
            lock.readLock().lock();
            try {
                return makeGraphModel();
            } finally {
                lock.readLock().unlock();
            }
        }

        protected OntGraphModel makeGraphModel() {
            UnionGraph thisGraph = getBase().getGraph();
            UnionGraph newGraph = new UnionGraph(new ConcurrentGraph(thisGraph.getBaseGraph(), lock), thisGraph.getEventManager());
            thisGraph.getUnderlying().graphs().forEach(newGraph::addGraph);
            return OntModelFactory.createModel(newGraph, getConfig().loaderConfig().getPersonality());
        }

        /**
         * it does not change object state so read lock here
         */
        @Override
        public void clearCache() {
            lock.readLock().lock();
            try {
                delegate.clearCache();
            } finally {
                lock.readLock().unlock();
            }
        }

        @Override
        public ConfigProvider.Config getConfig() {
            lock.readLock().lock();
            try {
                return delegate.getConfig();
            } finally {
                lock.readLock().unlock();
            }
        }

        @Override
        public OntologyManager getOWLOntologyManager() {
            return (OntologyManager) super.getOWLOntologyManager();
        }

        @Override
        public InternalModel getBase() {
            return delegate.getBase();
        }

        @Override
        public void setBase(InternalModel m) {
            delegate.setBase(m);
        }

    }
}
