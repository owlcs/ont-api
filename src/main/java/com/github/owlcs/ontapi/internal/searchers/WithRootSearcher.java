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

import com.github.owlcs.ontapi.internal.BaseSearcher;
import com.github.owlcs.ontapi.jena.model.OntIndividual;
import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.jena.model.OntObject;
import com.github.owlcs.ontapi.jena.model.OntStatement;
import com.github.owlcs.ontapi.jena.utils.Iter;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.util.iterator.ExtendedIterator;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by @ssz on 19.04.2020.
 */
public class WithRootSearcher extends BaseSearcher {

    /**
     * Lists all roots for the given statement.
     *
     * @param model     {@link OntModel}, not {@code null}
     * @param statement {@link Statement}, not {@code null}
     * @return an {@code ExtendedIterator} of {@link Statement}s
     */
    protected final ExtendedIterator<OntStatement> listRootStatements(OntModel model, OntStatement statement) {
        if (statement.getSubject().isURIResource()) {
            return Iter.of(statement);
        }
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
    protected Set<OntStatement> getRootStatements(OntModel model, OntStatement statement) {
        Set<OntStatement> roots = new HashSet<>();
        Set<Resource> seen = new HashSet<>();
        Set<OntStatement> candidates = new LinkedHashSet<>();
        candidates.add(statement);
        while (!candidates.isEmpty()) {
            OntStatement st = candidates.iterator().next();
            candidates.remove(st);
            OntObject subject = st.getSubject();
            if (subject.isURIResource() || subject.canAs(OntIndividual.Anonymous.class)) {
                roots.add(st);
                continue;
            }
            int count = candidates.size();
            listByObject(model, subject).filterKeep(s -> s.getSubject().isURIResource() || seen.add(s.getSubject()))
                    .forEachRemaining(candidates::add);
            if (count != candidates.size()) {
                continue;
            }
            // no new candidates is found -> then it is root
            listProperties(model, subject).forEachRemaining(roots::add);
        }
        return roots;
    }

    /**
     * Lists all related statements for the given root, which should be an anonymous resource.
     * It is to find axiom-statement candidates,
     * for example a statement with the predicate {@code owl:distinctMembers} is not an axiom-statement, but
     * a statement with the same subject and {@code rdf:type} = {@code owl:AllDifferent} is an axiom-statement candidate.
     *
     * @param model {@link OntModel}
     * @param root  {@link OntObject} - an anonymous resource
     * @return an {@link ExtendedIterator} of {@link OntStatement}s
     */
    protected ExtendedIterator<OntStatement> listProperties(OntModel model, OntObject root) {
        return listBySubject(model, root);
    }

}
