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

package ru.avicomp.ontapi.internal;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.shared.JenaException;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import ru.avicomp.ontapi.jena.impl.OntGraphModelImpl;
import ru.avicomp.ontapi.jena.model.OntClass;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.utils.Iter;
import ru.avicomp.ontapi.jena.utils.Models;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

/**
 * The base abstract class that intended to perform Axiom Graph Translator (operator {@code T}) in both directions:
 * for reading and writing.
 * It is designed to work with any {@link OntGraphModel}, but it is optimized to use {@link InternalModel}.
 * See the specification about operator {@code T} and about annotations translation (operator 'TANN'):
 * <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Mapping_from_the_Structural_Specification_to_RDF_Graphs'>2.1 Translation of Axioms without Annotations</a>,
 * <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Axioms_that_are_Translated_to_Multiple_Triples'>2.3.2 Axioms that are Translated to Multiple Triples</a>.
 * And one more (and most useful) link: <a href='https://www.w3.org/TR/owl2-quick-reference/'>Quick Reference Guide</a>.
 * To get a particular instance of this class the method {@link AxiomParserProvider#get(AxiomType)} can be used.
 * <p>
 * Created by @szuev on 28.09.2016.
 *
 * @param <Axiom> generic type of {@link OWLAxiom}
 */
@SuppressWarnings("WeakerAccess")
public abstract class AxiomTranslator<Axiom extends OWLAxiom> {

    /**
     * Writes the given axiom to the model.
     *
     * @param axiom {@link OWLAxiom OWL-API axiom object}
     * @param model {@link OntGraphModel ONT-API Jena Model}
     */
    public abstract void write(Axiom axiom, OntGraphModel model);

    /**
     * Reads all model axioms in the form of stream.
     *
     * @param model {@link OntGraphModel ONT-API Jena Model}
     * @return Stream of {@link ONTObject}s that wrap {@link Axiom}s
     * @throws JenaException unable to read axioms of this type
     */
    public final Stream<ONTObject<Axiom>> axioms(OntGraphModel model) throws JenaException {
        InternalConfig conf = getConfig(model).snapshot();
        InternalObjectFactory factory = getObjectFactory(model);
        return Iter.asStream(listAxioms(model, factory, conf));
    }

    /**
     * Returns an {@link ExtendedIterator Extended Iterator} of all model {@link Axiom}s.
     *
     * @param model   {@link OntGraphModel ONT-API Jena Model}
     * @param factory {@link InternalObjectFactory} to produce OWL-API Objects
     * @param config  {@link InternalConfig} to control process
     * @return {@link ExtendedIterator} of {@link ONTObject}s that wrap {@link Axiom}s
     * @throws JenaException unable to read axioms of this type
     */
    public ExtendedIterator<ONTObject<Axiom>> listAxioms(OntGraphModel model,
                                                         InternalObjectFactory factory,
                                                         InternalConfig config) throws JenaException {
        return translate(listStatements(model, config), factory, config);
    }

    /**
     * Maps each {@link OntStatement Ontology Statement} from the given iterator to the {@link Axiom} instance
     * and returns a new iterator containing {@link OWLAxiom}s.
     *
     * @param statements {@link ExtendedIterator} of {@link OntStatement}s
     * @param factory    {@link InternalObjectFactory} to produce OWL-API Objects
     * @param config     {@link InternalConfig} to control process
     * @return {@link ExtendedIterator} of {@link ONTObject}s that wrap {@link Axiom}s
     * @throws JenaException unable to read axioms of this type
     */
    protected ExtendedIterator<ONTObject<Axiom>> translate(ExtendedIterator<OntStatement> statements,
                                                           InternalObjectFactory factory,
                                                           InternalConfig config) {
        if (!config.isSplitAxiomAnnotations()) {
            return statements.mapWith(s -> toAxiom(s, factory, config));
        }
        return Iter.flatMap(statements, Models::listSplitStatements).mapWith(s -> toAxiom(s, factory, config));
    }

    /**
     * Returns a stream of statements defining the axiom in the base graph of the specified model.
     *
     * @param model {@link OntGraphModel ONT-API Jena Model}
     * @return Stream of {@link OntStatement}, always local (not from imports)
     */
    public final Stream<OntStatement> statements(OntGraphModel model) {
        return Iter.asStream(listStatements(model, getConfig(model).snapshot()));
    }

    /**
     * Tests that the given statement defines the axiom.
     *
     * @param statement {@link OntStatement}
     * @return {@code true} if the statement defines the axiom
     */
    public final boolean testStatement(OntStatement statement) {
        return testStatement(statement, getConfig(statement.getModel()).snapshot());
    }

    /**
     * Performs translation {@code OntStatement -> OWLAxiom} using default settings.
     *
     * @param statement {@link OntStatement}
     * @return {@link ONTObject} around {@link OWLAxiom}
     * @throws JenaException if no possible to translate statement to axiom
     */
    public final ONTObject<Axiom> toAxiom(OntStatement statement) throws JenaException {
        return toAxiom(statement, getObjectFactory(statement.getModel()), getConfig(statement.getModel()).snapshot());
    }

    /**
     * Lists all statements from the base graph of the given model that match this axiom definition.
     *
     * @param model  {@link OntGraphModel Ontology Jena Model}
     * @param config {@link InternalConfig} control settings
     * @return {@link ExtendedIterator} of {@link OntStatement}s
     */
    public abstract ExtendedIterator<OntStatement> listStatements(OntGraphModel model, InternalConfig config);

    /**
     * Tests if the specified statement answers the axiom definition.
     *
     * @param statement {@link OntStatement} any statement, not necessarily local.
     * @param config    {@link InternalConfig} control settings
     * @return true if the statement corresponds axiom type.
     */
    public abstract boolean testStatement(OntStatement statement, InternalConfig config);

    /**
     * Creates an OWL Axiom from a statement.
     *
     * @param statement {@link OntStatement} the statement which determines the axiom
     * @param factory   {@link InternalObjectFactory} the data factory to create OWL-API objects
     * @param config    {@link InternalConfig} to control process
     * @return {@link ONTObject} around {@link OWLAxiom}
     * @throws JenaException if no possible to get axiom from the statement
     */
    public abstract ONTObject<Axiom> toAxiom(OntStatement statement,
                                             InternalObjectFactory factory,
                                             InternalConfig config) throws JenaException;

    /**
     * Gets the config from model's settings or dummy if it is naked Jena model.
     *
     * @param model {@link OntGraphModel}
     * @return {@link InternalConfig}
     */
    public static InternalConfig getConfig(OntGraphModel model) {
        return model instanceof InternalModel ? ((InternalModel) model).getSnapshotConfig() : InternalConfig.DEFAULT;
    }

    /**
     * Gets the ONT-API Data-Factory from model's settings.
     *
     * @param model {@link OntGraphModel}
     * @return {@link InternalObjectFactory}
     */
    public static InternalObjectFactory getObjectFactory(OntGraphModel model) {
        return model instanceof InternalModel ? ((InternalModel) model).getObjectFactory() : InternalObjectFactory.DEFAULT;
    }

    /**
     * Gets all {@link Resource}s that are reserved for a model and cannot represent a {@link OntClass}.
     *
     * @param model {@link OntGraphModel}, not {@code null}
     * @return a {@code Set} of {@link Resource}s
     */
    protected static Set<Resource> getSystemClasses(OntGraphModel model) {
        if (model instanceof OntGraphModelImpl) {
            return ((OntGraphModelImpl) model).getSystemResources(OntClass.class);
        }
        return Collections.emptySet();
    }
}
