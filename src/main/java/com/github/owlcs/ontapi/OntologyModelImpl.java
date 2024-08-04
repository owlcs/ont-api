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

import com.github.owlcs.ontapi.internal.InternalGraphModel;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.jena.graph.Graph;
import org.apache.jena.ontapi.UnionGraph;
import org.apache.jena.ontapi.common.OntPersonality;
import org.apache.jena.ontapi.impl.OntGraphModelImpl;
import org.apache.jena.ontapi.model.OntModel;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.AddOntologyAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLMutableOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyChangeVisitorEx;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.RemoveAxiom;
import org.semanticweb.owlapi.model.RemoveImport;
import org.semanticweb.owlapi.model.RemoveOntologyAnnotation;
import org.semanticweb.owlapi.model.SetOntologyID;
import org.semanticweb.owlapi.model.parameters.ChangeApplied;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * The main ontology model implementation. Not concurrent. Editable.
 * Provides access to {@link OntModel}.
 * <p>
 * Created by @ssz on 27.09.2016.
 *
 * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLOntologyImpl.java'>uk.ac.manchester.cs.owl.owlapi.OWLOntologyImpl</a>
 * @see OntBaseModelImpl
 * @see Ontology
 */
@SuppressWarnings("WeakerAccess")
public class OntologyModelImpl extends OntBaseModelImpl implements Ontology, OWLMutableOntology {

    @Serial
    private static final long serialVersionUID = -2882895355499914294L;
    protected final OWLOntologyChangeVisitorEx<ChangeApplied> changer;

    /**
     * To construct an ontology based on the given graph.
     *
     * @param graph  {@link Graph}
     * @param config {@link ModelConfig}
     */
    protected OntologyModelImpl(Graph graph, ModelConfig config) {
        super(graph, config);
        this.changer = createChangeProcessor();
    }

    @Override
    public ChangeApplied applyDirectChange(OWLOntologyChange change) {
        return change.accept(changer);
    }

    @Override
    public OntologyManagerImpl getOWLOntologyManager() {
        return (OntologyManagerImpl) super.getOWLOntologyManager();
    }

    protected OWLOntologyChangeVisitorEx<ChangeApplied> createChangeProcessor() {
        return new ChangeProcessor();
    }

    @Override
    public void clearCache() {
        getGraphModel().clearCache();
    }

    /**
     * Returns the jena model shadow.
     *
     * @return {@link OntModel}
     */
    @Override
    public OntModel asGraphModel() {
        return getGraphModel();
    }

    @Override
    public void setLock(@Nullable ReadWriteLock lock) {
        throw new OntApiException.Unsupported("Model's lock cannot be changed in ONT-API");
    }

    /**
     * Auxiliary class, which controls any changes that occur to the ontology through the OWL-API interface.
     */
    protected class ChangeProcessor implements OWLOntologyChangeVisitorEx<ChangeApplied>, HasAdapter, Serializable {

        @Serial
        private static final long serialVersionUID = 1150135725506037485L;

        @Override
        public ChangeApplied visit(@Nonnull AddAxiom change) {
            beforeChange();
            OWLAxiom axiom = change.getAxiom();
            if (containsAxiom(axiom)) {
                return ChangeApplied.NO_OPERATION;
            }
            getGraphModel().add(axiom);
            return ChangeApplied.SUCCESSFULLY;
        }

        @Override
        public ChangeApplied visit(@Nonnull RemoveAxiom change) {
            beforeChange();
            OWLAxiom axiom = change.getAxiom();
            if (containsAxiom(axiom)) {
                getGraphModel().remove(axiom);
                return ChangeApplied.SUCCESSFULLY;
            }
            return ChangeApplied.NO_OPERATION;
        }

        @Override
        public ChangeApplied visit(@Nonnull AddImport change) {
            OWLImportsDeclaration importDeclaration = change.getImportDeclaration();
            if (importsDeclarations().noneMatch(importDeclaration::equals)) {
                addImport(importDeclaration);
                return ChangeApplied.SUCCESSFULLY;
            }
            return ChangeApplied.NO_OPERATION;
        }

        @Override
        public ChangeApplied visit(@Nonnull RemoveImport change) {
            OWLImportsDeclaration importDeclaration = change.getImportDeclaration();
            if (importsDeclarations().anyMatch(importDeclaration::equals)) {
                removeImport(importDeclaration);
                return ChangeApplied.SUCCESSFULLY;
            }
            return ChangeApplied.NO_OPERATION;
        }

        @Override
        public ChangeApplied visit(@Nonnull AddOntologyAnnotation change) {
            beforeChange();
            OWLAnnotation annotation = change.getAnnotation();
            if (getGraphModel().contains(annotation)) {
                return ChangeApplied.NO_OPERATION;
            }
            getGraphModel().add(annotation);
            return ChangeApplied.SUCCESSFULLY;
        }

        @Override
        public ChangeApplied visit(@Nonnull RemoveOntologyAnnotation change) {
            beforeChange();
            OWLAnnotation annotation = change.getAnnotation();
            if (annotations().anyMatch(annotation::equals)) {
                getGraphModel().remove(annotation);
                return ChangeApplied.SUCCESSFULLY;
            }
            return ChangeApplied.NO_OPERATION;
        }

        @Override
        public ChangeApplied visit(@Nonnull SetOntologyID change) {
            OWLOntologyID id = change.getNewOntologyID();
            if (getOntologyID().equals(id)) {
                return ChangeApplied.NO_OPERATION;
            }
            setOntologyID(id);
            return ChangeApplied.SUCCESSFULLY;
        }

        /**
         * Performs preliminary actions before change the ontological data.
         * <p>
         * Currently, such action is forced loading of the whole container cache.
         * This is necessary in order to get exactly the same objects that have been added.
         * Without force loading, a cache will be assembled automatically on demand,
         * and will contain everything from the graph in a strictly defined form.
         * This may confuse when manual editing.
         * For example, adding {@code SubClassOf} will also add all class declaration triples,
         * and the axioms count will increment by more than one.
         * This will not happen if the cache is already loaded before the operation.
         * <p>
         * Also, an exception is thrown in case the content cache is disabled.
         * For more details about this,
         * see the method {@link com.github.owlcs.ontapi.config.CacheSettings#useContentCache()} description.
         */
        protected void beforeChange() {
            if (!getConfig().useContentCache()) {
                throw new OntApiException.ModificationDenied("Direct mutations through OWL-API interface are not allowed");
            }
            getGraphModel().forceLoad();
        }

        /**
         * Adds the import declaration.
         * If the declaration corresponds some graph found in manager,
         * the reference to that graph will also be added into the {@link UnionGraph} hierarchy structure.
         *
         * @param declaration {@link OWLImportsDeclaration}, not {@code null}
         */
        protected void addImport(OWLImportsDeclaration declaration) {
            // to match behaviour of OWL-API add to graph only single IRI -
            // either ontology IRI or specified declaration IRI.
            Ontology ont = getOWLOntologyManager().getImportedOntology(declaration);
            if (ont == null) {
                getGraphModel().getID().addImport(declaration.getIRI().getIRIString());
                return;
            }
            getGraphModel().addImport(getAdapter().asBaseModel(ont).getGraphModel());
        }

        /**
         * Removes the import declaration.
         * If the declaration corresponds some sub-graph,
         * the reference to that graph will be also removed from the {@link UnionGraph} hierarchy structure.
         *
         * @param declaration {@link OWLImportsDeclaration}, not {@code null}
         */
        protected void removeImport(OWLImportsDeclaration declaration) {
            // to match behaviour of OWL-API removes both declaration IRI and ontology IRI
            // (could be different in case of renaming)
            Ontology ont = getOWLOntologyManager().getImportedOntology(declaration);
            if (ont != null) {
                getGraphModel().removeImport(getAdapter().asBaseModel(ont).getGraphModel());
            }
            getGraphModel().getID().removeImport(declaration.getIRI().getIRIString());
        }

        @Override
        public OWLAdapter getAdapter() {
            return OWLAdapter.get();
        }
    }

    /**
     * A concurrent version of {@link Ontology}.
     * <p>
     * Created @ssz on 22.12.2016.
     */
    @SuppressWarnings("WeakerAccess")
    public static class Concurrent extends OWLOntologyWrapper implements Ontology, OntBaseModel {

        @Serial
        private static final long serialVersionUID = 5823394836022970162L;

        protected Concurrent(OntologyModelImpl delegate, ReadWriteLock lock) {
            super(delegate, lock);
        }

        public OntologyModelImpl delegate() {
            return (OntologyModelImpl) delegate;
        }

        /**
         * Creates a concurrent version of Ontology Graph Model with R/W Lock inside, backed by the given model.
         * The internal Jena model, which is provided by the method {@link #getGraphModel()}, does not contain any lock.
         * This is due to the danger of deadlock or livelock,
         * which are possible when working with a (caffeine) cache and a locked graph simultaneously.
         *
         * @return {@link OntModel RDF Model} with an R/W lock inside
         */
        @Override
        public OntModel asGraphModel() {
            lock.readLock().lock();
            try {
                InternalGraphModel base = getGraphModel();
                return asConcurrent(base.getUnionGraph(), base.getOntPersonality(), lock);
            } finally {
                lock.readLock().unlock();
            }
        }

        /**
         * Clears the cache.
         * It does not change the object state so the method uses read lock.
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
        public OntologyManager getOWLOntologyManager() {
            return (OntologyManager) super.getOWLOntologyManager();
        }

        @Override
        public InternalGraphModel getGraphModel() {
            return delegate().getGraphModel();
        }

        @Override
        public void setGraphModel(InternalGraphModel m) {
            delegate().setGraphModel(m);
        }

        @Override
        public ModelConfig getConfig() {
            return delegate().getConfig();
        }

        @Override
        public void setConfig(ModelConfig conf) {
            delegate().setConfig(conf);
        }

        /**
         * Assembles a concurrent version of the {@link OntModel Ontology RDF Model}.
         * Safety of RDF read/write operations is ensured
         * by the {@link com.github.sszuev.graphs.ReadWriteLockingGraph R/W-Locked Graph}.
         * Safety of changes in hierarchy is ensured in the model level, by the returned instance itself.
         * Note: currently, the assembly and modification of complex ontology objects are not safe (todo?).
         *
         * @param graph       {@link UnionGraph}, not {@code null}
         * @param personality {@link OntPersonality}, not {@code null}
         * @param lock        {@link ReadWriteLock}, not {@code null}
         * @return {@link OntGraphModelImpl} completed with the give R/W lock
         */
        public static OntGraphModelImpl asConcurrent(UnionGraph graph,
                                                     OntPersonality personality,
                                                     ReadWriteLock lock) {
            UnionGraph copy = UnionGraphConnector.withBase(graph, OntGraphUtils.asConcurrent(graph.getBaseGraph(), lock));
            return new OntGraphModelImpl(copy, personality) {

                @Override
                protected void addImportModel(Graph g, String u) {
                    lock.writeLock().lock();
                    try {
                        UnionGraph from = OntGraphUtils.asUnionGraph(g);
                        UnionGraph res = UnionGraphConnector.withBase(from, OntGraphUtils.asNonConcurrent(from.getBaseGraph()));
                        super.addImportModel(res, u);
                    } finally {
                        lock.writeLock().unlock();
                    }
                }

                @Override
                protected void removeImportModel(Graph g, String u) {
                    lock.writeLock().lock();
                    try {
                        super.removeImportModel(g, u);
                    } finally {
                        lock.writeLock().unlock();
                    }
                }

                @Override
                public Stream<OntModel> imports() {
                    lock.readLock().lock();
                    try {
                        OntPersonality p = getOntPersonality();
                        List<OntModel> res = listImportGraphs()
                                .mapWith(x -> (OntModel) asConcurrent(x, p, lock))
                                .toList();
                        return res.stream();
                    } finally {
                        lock.readLock().unlock();
                    }
                }

                @Override
                protected Optional<OntGraphModelImpl> findImportAsRawModel(Predicate<OntGraphModelImpl> filter) {
                    lock.readLock().lock();
                    try {
                        return super.findImportAsRawModel(filter);
                    } finally {
                        lock.readLock().unlock();
                    }
                }

                @Override
                public String toString() {
                    return String.format("ConcurrentOntGraphModel{%s}", OntGraphUtils.getOntologyGraphPrintName(graph));
                }
            };
        }
    }

}
