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
import com.github.owlcs.ontapi.jena.model.*;
import com.github.owlcs.ontapi.jena.utils.Iter;
import com.github.owlcs.ontapi.jena.utils.OntModels;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLPrimitive;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A base abstraction for any axioms-by-primitive helper (referencing-axioms functionality).
 * Created by @ssz on 19.03.2020.
 *
 * @param <P> - a subtype of {@link OWLPrimitive}:
 *            either {@link org.semanticweb.owlapi.model.OWLEntity},
 *            {@link org.semanticweb.owlapi.model.IRI} or {@link org.semanticweb.owlapi.model.OWLLiteral}
 */
public abstract class ByPrimitive<P extends OWLPrimitive> extends BaseSearcher {

    /**
     * All translators.
     */
    private static final Set<AxiomTranslator<? extends OWLAxiom>> TRANSLATORS = selectTranslators(null);

    protected static Set<AxiomTranslator<? extends OWLAxiom>> selectTranslators(OWLComponentType type) {
        return OWLTopObjectType.axioms().filter(x -> type == null || x.hasComponent(type))
                .map(OWLTopObjectType::getAxiomType)
                .map(AxiomParserProvider::get)
                .collect(Iter.toUnmodifiableSet());
    }

    /**
     * Answers {@code true} if need to check annotations also.
     *
     * @param m {@link OntModel}
     * @return boolean
     */
    protected static boolean includeAnnotations(OntModel m) {
        return !(m instanceof SearchModel) || ((SearchModel) m).hasAnnotations();
    }

    /**
     * Finds the root (top-level) annotation resource
     * for the given sub-annotation (a resource with {@code owl:Annotation} as {@code rdf:type}).
     *
     * @param annotation {@link OntAnnotation} - sub annotation, not {@code null}
     * @return root or the same input annotation - a resource with {@code owl:Axiom} as {@code rdf:type}
     */
    public static OntAnnotation getRoot(OntAnnotation annotation) {
        OntAnnotation parent = annotation.parent().orElse(null);
        return parent == null ? annotation : getRoot(parent);
    }

    final ExtendedIterator<OntStatement> listStatements(OntModel model, Resource resource) {
        return Iter.concat(listBySubject(model, resource),
                Iter.flatMap(listByObject(model, resource), s -> listRootStatements(model, s)));
    }

    /**
     * Lists all related statements (axiom-candidates) for the given {@code primitive}.
     *
     * @param model     {@link OntModel}
     * @param primitive {@link P}
     * @return an {@link ExtendedIterator} of {@link OntStatement}s
     */
    protected abstract ExtendedIterator<OntStatement> listStatements(OntModel model, P primitive);

    /**
     * Lists all axioms that contain the given {@link P}.
     *
     * @param primitive a {@link P}, not {@code null}
     * @param model     a {@link Supplier} to derive nonnull {@link OntModel}, not {@code null}
     * @param factory   an {@link InternalObjectFactory}, not {@code null}
     * @param config    {@link InternalConfig}, not {@code null}
     * @return an {@link ExtendedIterator} of {@link OWLAxiom}s wrapped with {@link ONTObject}
     */
    public ExtendedIterator<ONTObject<? extends OWLAxiom>> listAxioms(P primitive,
                                                                      Supplier<OntModel> model,
                                                                      InternalObjectFactory factory,
                                                                      InternalConfig config) {
        ExtendedIterator<OntStatement> res = listStatements(model.get(), primitive);
        if (config.isSplitAxiomAnnotations()) {
            return Iter.flatMap(res,
                    s -> Iter.flatMap(listTranslators(s, config), t -> split(t, s, model, factory, config)));
        }
        return Iter.flatMap(res, s -> listTranslators(s, config).mapWith(t -> toAxiom(t, s, model, factory, config)));
    }

    /**
     * Lists all {@link AxiomTranslator}-candidates.
     *
     * @return {@link ExtendedIterator}
     */
    protected ExtendedIterator<AxiomTranslator<? extends OWLAxiom>> listTranslators() {
        return Iter.create(TRANSLATORS);
    }

    /**
     * Lists translators.
     *
     * @param statement {@link OntStatement}
     * @param conf      {@link InternalConfig}
     * @return an {@link ExtendedIterator} of {@link AxiomTranslator}s
     */
    protected ExtendedIterator<? extends AxiomTranslator<? extends OWLAxiom>> listTranslators(OntStatement statement,
                                                                                              InternalConfig conf) {
        return listTranslators().filterKeep(t -> t.testStatement(statement, conf));
    }

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

    /**
     * Lists all related statements for the given root, taking in account {@code owl:Axiom}s and {@code owl:Annotations}.
     *
     * @param model {@link OntModel}
     * @param root  {@link OntObject}
     * @return an {@link ExtendedIterator} of {@link OntStatement}s
     */
    protected ExtendedIterator<OntStatement> listPropertiesIncludeAnnotations(OntModel model, OntObject root) {
        if (!includeAnnotations(model)) {
            return listBySubject(model, root);
        }
        OntAnnotation a = root.getAs(OntAnnotation.class);
        if (a == null) {
            return listBySubject(model, root);
        }
        OntStatement base = ByPrimitive.getRoot(a).getBase();
        if (base != null) {
            return Iter.of(base);
        }
        return listBySubject(model, root);
    }

    final ExtendedIterator<OntStatement> listBySubject(OntModel model, Resource subject) {
        return OntModels.listLocalStatements(model, subject, null, null);
    }

    final ExtendedIterator<OntStatement> listByProperty(OntModel m, Property uri) {
        return OntModels.listLocalStatements(m, null, uri, null);
    }

    final ExtendedIterator<OntStatement> listByObject(OntModel model, RDFNode object) {
        return OntModels.listLocalStatements(model, null, null, object);
    }

    final ExtendedIterator<OntStatement> listStatements(OntModel model) {
        return OntModels.listLocalStatements(model, null, null, null);
    }
}
