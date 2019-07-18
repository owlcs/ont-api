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

import org.apache.jena.graph.Graph;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.ChangeApplied;
import ru.avicomp.ontapi.internal.InternalModel;
import ru.avicomp.ontapi.internal.InternalModelHolder;
import ru.avicomp.ontapi.jena.UnionGraph;
import ru.avicomp.ontapi.jena.impl.OntGraphModelImpl;
import ru.avicomp.ontapi.jena.impl.conf.OntPersonality;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.utils.Graphs;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Predicate;
import java.util.stream.Stream;

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
public class OntologyModelImpl extends OntBaseModelImpl implements OntologyModel, OWLMutableOntology {

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

    @Override
    public void setLock(ReadWriteLock lock) {
        throw new OntApiException.Unsupported("Model's lock cannot be changed in ONT-API");
    }

    protected class ChangeProcessor implements OWLOntologyChangeVisitorEx<ChangeApplied>, HasAdapter, Serializable {

        private static final long serialVersionUID = 1150135725506037485L;

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
            if (getBase().contains(annotation)) {
                return NO_OPERATION;
            }
            getBase().add(annotation);
            return SUCCESSFULLY;
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
            // to match behaviour of OWL-API add to graph only single IRI -
            // either ontology IRI or specified declaration IRI.
            OntologyModel ont = getOWLOntologyManager().getImportedOntology(declaration);
            if (ont == null) {
                getBase().getID().addImport(declaration.getIRI().getIRIString());
                return;
            }
            getBase().addImport(getAdapter().asBaseHolder(ont).getBase());
        }

        protected void removeImport(OWLImportsDeclaration declaration) {
            // to match behaviour of OWL-API removes both declaration IRI and ontology IRI
            // (could be different in case of renaming)
            OntologyModel ont = getOWLOntologyManager().getImportedOntology(declaration);
            getBase().getID().removeImport(declaration.getIRI().getIRIString());
            if (ont == null) {
                return;
            }
            getBase().removeImport(getAdapter().asBaseHolder(ont).getBase());
        }

        @Override
        public OWLAdapter getAdapter() {
            return OWLAdapter.get();
        }
    }

    /**
     * A concurrent version of {@link OntologyModel}.
     * <p>
     * Created by szuev on 22.12.2016.
     */
    @SuppressWarnings("WeakerAccess")
    public static class Concurrent extends OWLOntologyWrapper implements OntologyModel, InternalModelHolder {

        private static final long serialVersionUID = 5823394836022970162L;

        protected Concurrent(OntologyModelImpl delegate, ReadWriteLock lock) {
            super(delegate, lock);
        }

        public OntologyModelImpl delegate() {
            return (OntologyModelImpl) delegate;
        }

        /**
         * Creates a concurrent version of Ontology Graph Model with R/W Lock inside, backed by the given model.
         * The internal Jena model, which is provided by the method {@link #getBase()}, does not contain any lock.
         * This is due to the danger of deadlock or livelock,
         * which are possible when working with a (caffeine) cache and a locked graph simultaneously.
         *
         * @return {@link OntGraphModel RDF Model} with a R/W lock inside
         */
        @Override
        public OntGraphModel asGraphModel() {
            lock.readLock().lock();
            try {
                OntGraphModelImpl base = getBase();
                return asConcurrent(base.getGraph(), base.getOntPersonality(), lock);
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
        public InternalModel getBase() {
            return delegate().getBase();
        }

        @Override
        public void setBase(InternalModel m) {
            delegate().setBase(m);
        }

        /**
         * Assembles a concurrent version of the {@link OntGraphModel Ontology RDF Model}.
         * Safety of RDF read/write operations is ensured
         * by the {@link ru.avicomp.ontapi.jena.RWLockedGraph R/W-Locked Graph}.
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
            Graph base = graph.getBaseGraph();
            UnionGraph copy = withBase(graph, Graphs.asConcurrent(base, lock));
            return new OntGraphModelImpl(copy, personality) {

                @Override
                protected void addImportModel(Graph g, String u) {
                    lock.writeLock().lock();
                    try {
                        UnionGraph from = asUnionGraph(g);
                        Graph base = Graphs.asNonConcurrent(from.getBaseGraph());
                        UnionGraph res = withBase(from, base);
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
                public Stream<OntGraphModel> imports() {
                    lock.readLock().lock();
                    try {
                        OntPersonality p = getOntPersonality();
                        List<OntGraphModel> res = listImportGraphs()
                                .mapWith(x -> (OntGraphModel) asConcurrent(x, p, lock))
                                .toList();
                        return res.stream();
                    } finally {
                        lock.readLock().unlock();
                    }
                }

                @Override
                protected Optional<OntGraphModelImpl> findImport(Predicate<OntGraphModelImpl> filter) {
                    lock.readLock().lock();
                    try {
                        return super.findImport(filter);
                    } finally {
                        lock.readLock().unlock();
                    }
                }

                @Override
                public String toString() {
                    return String.format("ConcurrentOntGraphModel{%s}", Graphs.getName(base));
                }
            };
        }

        /**
         * Makes a new {@link UnionGraph} with a the specified {@code base} graph
         * and with the inherited hierarchy structure from the given {@code from} graph.
         * The returned union graph is backed by the specified union graph and vice versa.
         *
         * @param from {@link UnionGraph Union Graph} from which the hierarchical structure is taken, not {@code null}
         * @param base {@link Graph}, a new base graph to suppress the existing from the given {@code from} graph.
         * @return {@link UnionGraph} that is backed by the given {@code from},
         * with the same hierarchy structure but with a new {@code base} graph
         */
        public static UnionGraph withBase(UnionGraph from, Graph base) {
            if (Objects.requireNonNull(from).getBaseGraph().equals(Objects.requireNonNull(base))) {
                return from;
            }
            class U extends UnionGraph {
                private final UnionGraph from;

                private U(Graph base, UnionGraph from) {
                    super(base, from.getUnderlying(), from.getEventManager(), from.isDistinct());
                    this.from = from;
                }

                @Override
                public UnionGraph addGraph(Graph graph) {
                    from.addGraph(graph);
                    addParent(graph);
                    resetGraphsCache();
                    return this;
                }

                @Override
                public UnionGraph removeGraph(Graph graph) {
                    from.removeGraph(graph);
                    removeParent(graph);
                    resetGraphsCache();
                    return this;
                }

                @Override
                protected Set<Graph> collectBaseGraphs() {
                    Set<Graph> res = super.collectBaseGraphs();
                    res.remove(from.getBaseGraph());
                    return res;
                }
            }
            if (from instanceof U) {
                return withBase(((U) from).from, base);
            }
            return new U(base, from);
        }
    }
}
