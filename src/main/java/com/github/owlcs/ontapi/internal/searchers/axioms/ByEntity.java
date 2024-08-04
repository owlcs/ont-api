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

package com.github.owlcs.ontapi.internal.searchers.axioms;

import com.github.owlcs.ontapi.config.AxiomsSettings;
import com.github.owlcs.ontapi.internal.ONTObject;
import com.github.owlcs.ontapi.internal.ONTObjectFactory;
import org.apache.jena.ontapi.model.OntModel;
import org.apache.jena.ontapi.model.OntStatement;
import org.apache.jena.ontapi.utils.Iterators;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;

/**
 * A base searcher for {@link OWLEntity}.
 * Created by @ssz on 29.03.2020.
 *
 * @param <E> - {@link OWLEntity}
 */
public abstract class ByEntity<E extends OWLEntity> extends ByPrimitive<E> {

    @Override
    public final ExtendedIterator<ONTObject<OWLAxiom>> listONTAxioms(E entity,
                                                                     OntModel model,
                                                                     ONTObjectFactory factory,
                                                                     AxiomsSettings config) {
        ExtendedIterator<OntStatement> res = listStatements(model, entity);
        if (config.isSplitAxiomAnnotations()) {
            return Iterators.flatMap(res, s -> Iterators.flatMap(listTranslators(s, config),
                    t -> split(t, s, factory, config)).filterKeep(x -> filter(x.getOWLObject(), entity)));
        }
        return Iterators.flatMap(res, s -> listTranslators(s, config).mapWith(t -> toAxiom(t, s, factory, config))
                .filterKeep(x -> filter(x.getOWLObject(), entity)));
    }

    @Override
    public final ExtendedIterator<OntStatement> listStatements(OntModel model, E entity) {
        return listStatements(model, entity.getIRI().getIRIString());
    }

    /**
     * Answers {@code true} if the axiom contains the entity.
     * It is for a case of punning. In normal circumstances no need to filter out.
     *
     * @param axiom  {@link A}, not {@code null}
     * @param entity {@link E}, not {@code null}
     * @param <A>    subtype of {@link OWLAxiom}
     * @return boolean
     */
    protected <A extends OWLAxiom> boolean filter(A axiom, E entity) {
        return axiom.containsEntityInSignature(entity);
    }

    /**
     * Lists all statements-candidates
     *
     * @param m   {@link OntModel}
     * @param uri {@code String}
     * @return an {@link ExtendedIterator} over {@link OntStatement}s
     */
    protected ExtendedIterator<OntStatement> listStatements(OntModel m, String uri) {
        return super.listStatements(m, m.getResource(uri));
    }

}
