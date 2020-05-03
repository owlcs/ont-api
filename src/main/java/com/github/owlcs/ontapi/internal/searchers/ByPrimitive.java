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

import com.github.owlcs.ontapi.config.AxiomsSettings;
import com.github.owlcs.ontapi.internal.*;
import com.github.owlcs.ontapi.jena.impl.PersonalityModel;
import com.github.owlcs.ontapi.jena.impl.conf.OntPersonality;
import com.github.owlcs.ontapi.jena.model.OntAnnotation;
import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.jena.model.OntObject;
import com.github.owlcs.ontapi.jena.model.OntStatement;
import com.github.owlcs.ontapi.jena.utils.Iter;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLPrimitive;

import java.util.Set;

/**
 * A base abstraction for any axioms-by-primitive search helper (referencing-axioms functionality).
 * Created by @ssz on 19.03.2020.
 *
 * @param <P> - a subtype of {@link OWLPrimitive}:
 *            either {@link org.semanticweb.owlapi.model.OWLEntity},
 *            {@link org.semanticweb.owlapi.model.IRI} or {@link org.semanticweb.owlapi.model.OWLLiteral}
 */
public abstract class ByPrimitive<P extends OWLPrimitive> extends BaseByObject<OWLAxiom, P> {

    /**
     * All translators.
     */
    private static final Set<AxiomTranslator<OWLAxiom>> TRANSLATORS = selectTranslators(null);

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

    /**
     * Answers {@code true} if the given {@code node} contains the specified {@code uri}.
     *
     * @param node {@link RDFNode}
     * @param uri  {@code String}
     * @return boolean
     */
    protected static boolean hasURI(RDFNode node, String uri) {
        if (node.isURIResource()) {
            return uri.equals(node.asResource().getURI());
        }
        return node.isLiteral() && uri.equals(node.asLiteral().getDatatypeURI());
    }

    /**
     * Answers {@code true} if the given {@code uri} is reserved.
     *
     * @param model {@link OntModel}, not {@code null}
     * @param uri   {@code String} to test, not {@code null}
     * @return boolean
     */
    protected static boolean isSystem(OntModel model, String uri) {
        if (model instanceof SearchModel) {
            return ((SearchModel) model).getSystemURIs().contains(uri);
        }
        OntPersonality.Reserved voc = PersonalityModel.asPersonalityModel(model).getOntPersonality().getReserved();
        Node node = NodeFactory.createURI(uri);
        return voc.getProperties().contains(node) || voc.getResources().contains(node);
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
     * @param model     a {@link OntModel}, not {@code null}
     * @param factory   an {@link ONTObjectFactory}, not {@code null}
     * @param config    {@link InternalConfig}, not {@code null}
     * @return an {@link ExtendedIterator} of {@link OWLAxiom}s wrapped with {@link ONTObject}
     */
    @Override
    public ExtendedIterator<ONTObject<OWLAxiom>> listAxioms(P primitive,
                                                            OntModel model,
                                                            ONTObjectFactory factory,
                                                            AxiomsSettings config) {
        ExtendedIterator<OntStatement> res = listStatements(model, primitive);
        if (config.isSplitAxiomAnnotations()) {
            return Iter.flatMap(res,
                    s -> Iter.flatMap(listTranslators(s, config), t -> split(t, s, factory, config)));
        }
        return Iter.flatMap(res, s -> listTranslators(s, config).mapWith(t -> toAxiom(t, s, factory, config)));
    }

    /**
     * Lists all {@link AxiomTranslator}-candidates.
     *
     * @return {@link ExtendedIterator}
     */
    protected ExtendedIterator<AxiomTranslator<OWLAxiom>> listTranslators() {
        return Iter.create(TRANSLATORS);
    }

    /**
     * Lists translators.
     *
     * @param statement {@link OntStatement}
     * @param conf      {@link AxiomsSettings}
     * @return an {@link ExtendedIterator} of {@link AxiomTranslator}s
     */
    protected ExtendedIterator<? extends AxiomTranslator<OWLAxiom>> listTranslators(OntStatement statement,
                                                                                    AxiomsSettings conf) {
        return listTranslators().filterKeep(t -> t.testStatement(statement, conf));
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
}
