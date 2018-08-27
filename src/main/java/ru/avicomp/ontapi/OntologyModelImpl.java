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
import ru.avicomp.ontapi.jena.RWLockedGraph;
import ru.avicomp.ontapi.jena.UnionGraph;
import ru.avicomp.ontapi.jena.impl.OntGraphModelImpl;
import ru.avicomp.ontapi.jena.impl.conf.OntPersonality;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.utils.Graphs;

import javax.annotation.Nonnull;
import java.util.concurrent.locks.ReadWriteLock;

import static org.semanticweb.owlapi.model.parameters.ChangeApplied.NO_OPERATION;
import static org.semanticweb.owlapi.model.parameters.ChangeApplied.SUCCESSFULLY;

/**
 * The main ontology model implementation. Not concurrent. Editable.
 * Provides access to {@link OntGraphModel}.
 * <p>
 * Created by @szuev on 27.09.2016.
 *
 * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLOntologyImpl.java'>uk.ac.manchester.cs.owl.owlapi.OWLOntologyImpl</a>
 * @see OntBaseModelImpl
 * @see OntologyModel
 */
@SuppressWarnings("WeakerAccess")
public class OntologyModelImpl extends OntBaseModelImpl implements OntologyModel {

    protected transient ChangeProcessor changer;

    /**
     * To construct an ontology based on the given graph.
     *
     * @param graph  {@link Graph}
     * @param config {@link OntologyManagerImpl.ModelConfig}
     */
    protected OntologyModelImpl(Graph graph, OntologyManagerImpl.ModelConfig config) {
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
     * A concurrent version of {@link OntologyModel}.
     * <p>
     * Created by szuev on 22.12.2016.
     */
    @SuppressWarnings("WeakerAccess")
    public static class Concurrent extends OWLOntologyWrapper implements OntologyModel, ConfigProvider, InternalModelHolder {

        protected Concurrent(OntologyModelImpl delegate, ReadWriteLock lock) {
            super(delegate, lock);
        }

        public OntologyModelImpl delegate() {
            return (OntologyModelImpl) delegate;
        }

        /**
         * Creates a concurrent version of Ontology Graph Model with R/W Lock inside, backed by the given model.
         * The internal Jena model, which is provided by the method {@link #getBase()}, does not contain any lock.
         * This is due to the danger of the occurrence of deadlock or livelock,
         * which are possible when work with the (caffeine) cache and the locked graph simultaneously.
         *
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

        /**
         * Makes a concurrent version of the base graph model.
         *
         * @return {@link OntGraphModel} with {@link ReadWriteLock R/W Lock} inside
         */
        protected OntGraphModel makeGraphModel() {
            InternalModel base = getBase();
            OntPersonality p = base.getPersonality();
            UnionGraph orig = base.getGraph();
            UnionGraph copy = new UnionGraph(new RWLockedGraph(orig.getBaseGraph(), lock), orig.getUnderlying(), orig.getEventManager());
            return new OntGraphModelImpl(copy, p) {

                @Override
                public OntGraphModelImpl addImport(OntGraphModel m) {
                    return super.addImport(asNonConcurrent(m));
                }

                @Override
                public OntGraphModelImpl removeImport(OntGraphModel m) {
                    return super.removeImport(asNonConcurrent(m));
                }

                private OntGraphModel asNonConcurrent(OntGraphModel m) {
                    return new OntGraphModelImpl(Graphs.asNonConcurrent(m.getGraph()), p);
                }
            };
        }

        /**
         * Clears cache.
         * It does not change the object state so uses read lock here.
         */
        @Override
        public void clearCache() {
            lock.readLock().lock();
            try {
                delegate().clearCache();
            } finally {
                lock.readLock().unlock();
            }
        }

        @Override
        public ConfigProvider.Config getConfig() {
            lock.readLock().lock();
            try {
                return delegate().getConfig();
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
            return delegate().getBase();
        }

        @Override
        public void setBase(InternalModel m) {
            delegate().setBase(m);
        }

    }
}
