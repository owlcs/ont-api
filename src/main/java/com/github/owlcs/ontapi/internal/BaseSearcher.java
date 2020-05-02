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

package com.github.owlcs.ontapi.internal;

import com.github.owlcs.ontapi.DataFactory;
import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.jena.model.OntStatement;
import com.github.owlcs.ontapi.jena.utils.Iter;
import com.github.owlcs.ontapi.jena.utils.OntModels;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.shared.JenaException;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.OWLAxiom;

import java.util.List;
import java.util.Set;

/**
 * A base abstraction for {@link AxiomTranslator}s and searchers.
 * Created by @ssz on 28.03.2020.
 */
public abstract class BaseSearcher {

    /**
     * Maps each {@link OntStatement Ontology Statement} from the given iterator to the {@link A} instance
     * and returns a new iterator containing {@link A}s.
     * <p>
     * Impl notes: any item of the returned iterator can be either {@link ONTWrapperImpl ONTWrapper}
     * with raw {@link A} from the system-wide {@link DataFactory DataFactory}
     * or {@link ONTObject} attached to the given model.
     * If {@link com.github.owlcs.ontapi.config.AxiomsSettings#isSplitAxiomAnnotations()} is {@code true}
     * and a processed statement is splittable, then the method returns {@link ONTWrapperImpl}s only.
     *
     * @param <A>        a subtype of {@link OWLAxiom}
     * @param translator {@link AxiomTranslator} with generic type {@link A}, not {@code null}
     * @param statements an {@link ExtendedIterator} of {@link OntStatement}s, not {@code null}
     * @param factory    a {@link InternalObjectFactory} to produce OWL-API Objects, not {@code null}
     * @param config     a {@link InternalConfig} to control the process, not {@code null}
     * @return {@link ExtendedIterator} of {@link ONTObject}s that wrap {@link A}s
     * @throws JenaException unable to read axioms of this type
     */
    protected static <A extends OWLAxiom> ExtendedIterator<ONTObject<A>> translate(AxiomTranslator<A> translator,
                                                                                   ExtendedIterator<OntStatement> statements,
                                                                                   InternalObjectFactory factory,
                                                                                   InternalConfig config) {
        return config.isSplitAxiomAnnotations() ?
                Iter.flatMap(statements, s -> split(translator, s, factory, config)) :
                statements.mapWith(s -> toAxiom(translator, s, factory, config));
    }

    /**
     * Creates an axiom from the given statement.
     *
     * @param <A>        a subtype of {@link OWLAxiom}
     * @param translator {@link AxiomTranslator} with generic type {@link A}, not {@code null}
     * @param statement  {@link OntStatement} to split, not {@code null}
     * @param factory    an {@link InternalObjectFactory}, not {@code null}
     * @param config     {@link InternalConfig}, not {@code null}
     * @return an {@link ONTObject} with {@link A}
     */
    protected static <A extends OWLAxiom> ONTObject<A> toAxiom(AxiomTranslator<A> translator,
                                                               OntStatement statement,
                                                               InternalObjectFactory factory,
                                                               InternalConfig config) {
        return factory == InternalObjectFactory.DEFAULT || config == InternalConfig.DEFAULT ?
                translator.toAxiomWrap(statement, factory, config) :
                translator.toAxiomImpl(statement, ((ModelObjectFactory) factory).model(), factory, config);
    }

    /**
     * Splits the statement into several axioms if it is possible.
     *
     * @param <A>        a subtype of {@link OWLAxiom}
     * @param translator {@link AxiomTranslator} with generic type {@link A}, not {@code null}
     * @param statement  {@link OntStatement} to split, not {@code null}
     * @param factory    an {@link InternalObjectFactory}, not {@code null}
     * @param config     {@link InternalConfig}, not {@code null}
     * @return a {@link ExtendedIterator} of {@link ONTObject}
     * @see com.github.owlcs.ontapi.config.AxiomsSettings#isSplitAxiomAnnotations()
     */
    protected static <A extends OWLAxiom> ExtendedIterator<ONTObject<A>> split(AxiomTranslator<A> translator,
                                                                               OntStatement statement,
                                                                               InternalObjectFactory factory,
                                                                               InternalConfig config) {
        // When the spit-setting is true, we cannot always provide an ONTStatement based axiom,
        // because a mapping statement to axiom becomes ambiguous:
        // the same triple may correspond different axiom-instances
        // So, currently there is only one solution - need to use wrappers instead of model-impls
        if (factory == InternalObjectFactory.DEFAULT || config == InternalConfig.DEFAULT) {
            return OntModels.listSplitStatements(statement).mapWith(s -> translator.toAxiomWrap(s, factory, config));
        }
        List<OntStatement> statements = OntModels.listSplitStatements(statement).toList();
        if (statements.size() == 1) { // unambiguous mapping
            return Iter.of(translator.toAxiomImpl(statement, ((ModelObjectFactory) factory).model(), factory, config));
        }
        return Iter.create(statements).mapWith(s -> translator.toAxiomWrap(s, factory, config));
    }

    protected static <T extends AxiomTranslator<? extends A>, A extends OWLAxiom> T toTranslator(OWLTopObjectType type) {
        return type.getTranslator();
    }

    protected static Set<AxiomTranslator<OWLAxiom>> selectTranslators(OWLComponentType type) {
        return OWLTopObjectType.axioms().filter(x -> type == null || x.hasComponent(type))
                .map(OWLTopObjectType::getAxiomType)
                .map(AxiomParserProvider::get)
                .collect(Iter.toUnmodifiableSet());
    }

    protected final ExtendedIterator<OntStatement> listBySubject(OntModel model, Resource subject) {
        return listStatements(model, subject, null, null);
    }

    protected final ExtendedIterator<OntStatement> listBySubjectAndPredicate(OntModel m, Resource subject, Property uri) {
        return listStatements(m, subject, uri, null);
    }

    protected final ExtendedIterator<OntStatement> listByPredicate(OntModel m, Property uri) {
        return listStatements(m, null, uri, null);
    }

    protected final ExtendedIterator<OntStatement> listByPredicateAndObject(OntModel model, Property uri, RDFNode object) {
        return listStatements(model, null, uri, object);
    }

    protected final ExtendedIterator<OntStatement> listByObject(OntModel model, RDFNode object) {
        return listStatements(model, null, null, object);
    }

    /**
     * Returns iterator over all local model's statements.
     *
     * @param model {@link OntModel}, not {@code null}
     * @return {@link ExtendedIterator} of {@link OntStatement}s
     */
    protected final ExtendedIterator<OntStatement> listStatements(OntModel model) {
        return listStatements(model, null, null, null);
    }

    private ExtendedIterator<OntStatement> listStatements(OntModel model, Resource s, Property p, RDFNode o) {
        return OntModels.listLocalStatements(model, s, p, o);
    }
}
