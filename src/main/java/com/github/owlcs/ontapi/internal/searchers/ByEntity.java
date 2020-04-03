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

package com.github.owlcs.ontapi.internal.searchers;

import com.github.owlcs.ontapi.internal.*;
import com.github.owlcs.ontapi.jena.model.OntIndividual;
import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.jena.model.OntStatement;
import com.github.owlcs.ontapi.jena.utils.Iter;
import com.github.owlcs.ontapi.jena.utils.OntModels;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A base searcher for {@link OWLEntity}.
 * Created by @ssz on 29.03.2020.
 *
 * @param <E> - {@link OWLEntity}
 */
public abstract class ByEntity<E extends OWLEntity> extends ByPrimitive<E> {

    static Set<AxiomTranslator<? extends OWLAxiom>> selectTranslators(OWLComponentType type) {
        return OWLContentType.all().filter(x -> x.isAxiom() && x.hasComponent(type))
                .map(OWLContentType::getAxiomType)
                .map(AxiomParserProvider::get)
                .collect(Iter.toUnmodifiableSet());
    }

    public ExtendedIterator<OntStatement> listRootStatements(OntModel model, OntStatement statement) {
        return Iter.create(getRootStatements(model, statement));
    }

    /**
     * Returns a {@code Set} of root statements.
     * Any statement has one or more roots or is a root itself.
     * A statement with the predicate {@code rdf:type} is always a root.
     *
     * @param model     {@link OntModel}, not {@code null}
     * @param statement {@link Statement}, not {@code null}
     * @return a {@code Set} of {@link Statement}s
     */
    public static Set<OntStatement> getRootStatements(OntModel model, OntStatement statement) {
        Set<OntStatement> roots = new HashSet<>();
        Set<Resource> seen = new HashSet<>();
        Set<OntStatement> candidates = new LinkedHashSet<>();
        candidates.add(statement);
        while (!candidates.isEmpty()) {
            OntStatement st = candidates.iterator().next();
            candidates.remove(st);
            Resource subject = st.getSubject();
            if (subject.isURIResource() || subject.canAs(OntIndividual.Anonymous.class)) {
                roots.add(st);
                continue;
            }
            int count = candidates.size();
            OntModels.listLocalStatements(model, null, null, subject)
                    .filterKeep(s -> s.getSubject().isURIResource() || seen.add(s.getSubject()))
                    .forEachRemaining(candidates::add);
            if (count != candidates.size()) {
                continue;
            }
            // no new candidates is found -> then it is root
            OntModels.listLocalStatements(model, subject, null, null).forEachRemaining(roots::add);
        }
        return roots;
    }

    @Override
    public final ExtendedIterator<ONTObject<? extends OWLAxiom>> listAxioms(E entity,
                                                                            Supplier<OntModel> model,
                                                                            InternalObjectFactory factory,
                                                                            InternalConfig config) {
        ExtendedIterator<OntStatement> res = listStatements(model.get(), entity.getIRI().getIRIString());
        if (config.isSplitAxiomAnnotations()) {
            return Iter.flatMap(res, s -> Iter.flatMap(listTranslators(s, config), t -> split(t, s, model, factory, config)));
        }
        return Iter.flatMap(res, s -> listTranslators(s, config).mapWith(t -> toAxiom(t, s, model, factory, config)));
    }

    /**
     * Lists all {@link AxiomTranslator}-candidates.
     *
     * @return {@link ExtendedIterator}
     */
    protected abstract ExtendedIterator<AxiomTranslator<? extends OWLAxiom>> listTranslators();

    /**
     * Lists all statements-candidates
     *
     * @param m   {@link OntModel}
     * @param uri {@code String}
     * @return an {@link ExtendedIterator} over {@link OntStatement}s
     */
    protected ExtendedIterator<OntStatement> listStatements(OntModel m, String uri) {
        Resource res = m.getResource(uri);
        return Iter.concat(OntModels.listLocalStatements(m, res, null, null),
                Iter.flatMap(OntModels.listLocalStatements(m, null, null, res), s -> listRootStatements(m, s)));
    }

    protected ExtendedIterator<? extends AxiomTranslator<? extends OWLAxiom>> listTranslators(OntStatement statement,
                                                                                              InternalConfig conf) {
        return listTranslators().filterKeep(t -> t.testStatement(statement, conf));
    }
}
