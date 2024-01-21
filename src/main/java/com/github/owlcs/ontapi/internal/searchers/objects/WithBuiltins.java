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

package com.github.owlcs.ontapi.internal.searchers.objects;

import com.github.owlcs.ontapi.config.AxiomsSettings;
import com.github.sszuev.jena.ontapi.impl.conf.OntPersonality;
import com.github.sszuev.jena.ontapi.model.OntModel;
import com.github.sszuev.jena.ontapi.utils.Iterators;
import com.github.sszuev.jena.ontapi.vocabulary.RDF;
import org.apache.jena.graph.FrontsNode;
import org.apache.jena.graph.Node;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.OWLEntity;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by @ssz on 25.07.2020.
 *
 * @param <E> - subtype of {@link OWLEntity}
 */
abstract class WithBuiltins<E extends OWLEntity> extends EntitySearcher<E> {

    static OntPersonality.Builtins getBuiltinsVocabulary(OntModel m) {
        return asPersonalityModel(m).getOntPersonality().getBuiltins();
    }

    /**
     * Answers a {@code Set} of all builtins that allowed by the specification.
     *
     * @param m {@link OntModel}
     * @return a {@code Set} of uris ({@link Node}s)
     */
    protected abstract Set<Node> getBuiltinsSpec(OntModel m);

    /**
     * Answers a {@code Set} of all builtins that present somewhere in the ontology.
     *
     * @param m    {@link OntModel}
     * @param conf {@link AxiomsSettings}
     * @return a {@code Set} of uris ({@code String}s)
     */
    protected Set<String> getModelBuiltins(OntModel m, AxiomsSettings conf) {
        Set<String> res = new HashSet<>();
        getBuiltinsSpec(m).forEach(x -> {
            if (containsInOntology(x.getURI(), m, conf)) {
                res.add(x.getURI());
            }
        });
        return res;
    }

    /**
     * Answers {@code true} if the given {@code node} is builtin.
     *
     * @param m    {@link OntModel}
     * @param node {@link FrontsNode}
     * @return boolean
     */
    protected boolean isInBuiltinSpec(OntModel m, FrontsNode node) {
        return getBuiltinsSpec(m).contains(node.asNode());
    }

    final ExtendedIterator<String> listEntities(OntModel m, Set<String> builtins, AxiomsSettings conf) {
        ExtendedIterator<String> explicit = listByPredicateAndObject(m, RDF.type, getEntityType())
                .mapWith(x -> x.getSubject().getURI())
                .filterKeep(x -> x != null && !builtins.contains(x));
        ExtendedIterator<String> res = Iterators.concat(explicit, Iterators.create(builtins));
        if (!m.independent()) {
            res = Iterators.concat(res, listSharedFromImports(m).filterKeep(x -> containsInAxiom(toResource(m, x), m, conf)));
        }
        return res;
    }
}
