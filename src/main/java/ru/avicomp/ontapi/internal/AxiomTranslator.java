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

import org.apache.jena.graph.Node;
import org.apache.jena.shared.JenaException;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import ru.avicomp.ontapi.jena.impl.PersonalityModel;
import ru.avicomp.ontapi.jena.model.OntClass;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.utils.Iter;
import ru.avicomp.ontapi.jena.utils.OntModels;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
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
     * If possible, gets the {@code model}'s {@link InternalConfig Config},
     * otherwise returns the default one.
     *
     * @param model {@link OntGraphModel}
     * @return {@link InternalConfig}
     */
    public static InternalConfig getConfig(OntGraphModel model) {
        return model instanceof HasConfig ? ((HasConfig) model).getConfig().snapshot() : InternalConfig.DEFAULT;
    }

    /**
     * If possible, gets the {@code model}'s {@link InternalObjectFactory Object Factory},
     * otherwise returns the default one.
     *
     * @param model {@link OntGraphModel}
     * @return {@link InternalObjectFactory}
     */
    public static InternalObjectFactory getObjectFactory(OntGraphModel model) {
        return model instanceof HasObjectFactory ?
                ((HasObjectFactory) model).getObjectFactory() : InternalObjectFactory.DEFAULT;
    }

    /**
     * Gets all uri-{@link Node}s that are reserved for a model and cannot represent a {@link OntClass}.
     *
     * @param model {@link OntGraphModel}, not {@code null}
     * @return a {@code Set} of {@link Node}s
     */
    protected static Set<Node> getSystemResources(OntGraphModel model) {
        if (model instanceof PersonalityModel) {
            return ((PersonalityModel) model).getSystemResources(OntClass.class);
        }
        return Collections.emptySet();
    }

    /**
     * Writes the given axiom to the model.
     *
     * @param axiom {@link OWLAxiom OWL-API axiom object}
     * @param model {@link OntGraphModel ONT-API Jena Model}
     */
    public abstract void write(Axiom axiom, OntGraphModel model);

    /**
     * Reads all model's axioms in the form of stream.
     *
     * @param model {@link OntGraphModel ONT-API Jena Model}, not {@code null}
     * @return a {@code Stream} of {@link ONTObject}s that wrap {@link Axiom}s
     * @throws JenaException if unable to read axioms of this type
     */
    public final Stream<ONTObject<Axiom>> axioms(OntGraphModel model) throws JenaException {
        return Iter.asStream(listAxioms(model));
    }

    /**
     * Reads all model's axioms in the form of iterator.
     *
     * @param model {@link OntGraphModel ONT-API Jena Model}, not {@code null}
     * @return an {@code ExtendedIterator} of {@link ONTObject}s that wrap {@link Axiom}s
     * @throws JenaException if unable to read axioms of this type
     * @since 1.4.3
     */
    public final ExtendedIterator<ONTObject<Axiom>> listAxioms(OntGraphModel model) throws JenaException {
        Objects.requireNonNull(model, "Null model.");
        InternalObjectFactory factory = getObjectFactory(model);
        InternalConfig config = getConfig(model);
        return translate(listStatements(model, config), () -> model, factory, config);
    }

    /**
     * Returns an {@link ExtendedIterator Extended Iterator} of all model {@link Axiom}s.
     *
     * @param model   a facility (as {@link Supplier}) to provide nonnull {@link OntGraphModel}
     * @param factory {@link InternalObjectFactory} to produce OWL-API Objects, not {@code null}
     * @param config  {@link InternalConfig} to control process, not {@code null}
     * @return {@link ExtendedIterator} of {@link ONTObject}s that wrap {@link Axiom}s
     * @throws JenaException unable to read axioms of this type
     */
    protected ExtendedIterator<ONTObject<Axiom>> listAxioms(Supplier<OntGraphModel> model,
                                                            InternalObjectFactory factory,
                                                            InternalConfig config) throws JenaException {
        return translate(listStatements(model.get(), config), model, factory, config);
    }

    /**
     * Maps each {@link OntStatement Ontology Statement} from the given iterator to the {@link Axiom} instance
     * and returns a new iterator containing {@link Axiom}s.
     *
     * Impl notes: any item of the returned iterator can be either {@link ONTWrapperImpl ONTWrapper}
     * with raw {@link Axiom} from the system-wide {@link ru.avicomp.ontapi.DataFactory DataFactory}
     * or {@link ONTObject} attached to the given model.
     * If {@link ru.avicomp.ontapi.config.AxiomsSettings#isSplitAxiomAnnotations()} is {@code true},
     * then the method returns {@link ONTWrapperImpl}s only.
     *
     * This method is for internal usage only,
     *
     * @param statements an {@link ExtendedIterator} of {@link OntStatement}s, not {@code null}
     * @param model a facility (as {@link Supplier}) to provide nonnull {@link OntGraphModel}, not {@code null}
     * @param factory    a {@link InternalObjectFactory} to produce OWL-API Objects, not {@code null}
     * @param config     a {@link InternalConfig} to control the process, not {@code null}
     * @return {@link ExtendedIterator} of {@link ONTObject}s that wrap {@link Axiom}s
     * @throws JenaException unable to read axioms of this type
     */
    protected ExtendedIterator<ONTObject<Axiom>> translate(ExtendedIterator<OntStatement> statements,
                                                           Supplier<OntGraphModel> model,
                                                           InternalObjectFactory factory,
                                                           InternalConfig config) {
        Function<OntStatement, ONTObject<Axiom>> toAxiom;
        // When the spit-setting is true, we cannot provide an ONTStatement based axioms,
        // just because in this case a mapping statement to axiom is ambiguous.
        // So, currently there is only one solution - need to use wrappers
        if (factory == InternalObjectFactory.DEFAULT || config == InternalConfig.DEFAULT || config.isSplitAxiomAnnotations()) {
            // use ONTWrapper
            toAxiom = s -> toAxiom(s, factory, config);
        } else {
            // use ONTObject-impl
            toAxiom = s -> toAxiom(s, model, factory, config);
        }
        return translate(statements, toAxiom, config.isSplitAxiomAnnotations());
    }

    /**
     * Maps each {@link OntStatement Ontology Statement} from the given iterator to the {@link Axiom} instance
     * and returns a new iterator containing {@link Axiom}s.
     *
     * @param statements an {@link ExtendedIterator} of the source {@link OntStatement Statement}s
     * @param toAxiom    a mapping function,
     *                   either {@link AxiomTranslator#toAxiom(OntStatement, InternalObjectFactory, InternalConfig)}
     *                   or {@link AxiomTranslator#toAxiom(OntStatement, Supplier, InternalObjectFactory, InternalConfig)}
     * @param split      if {@code true} splits all given statements if that possible
     * @return a new {@link ExtendedIterator} of {@link Axiom}s
     */
    private ExtendedIterator<ONTObject<Axiom>> translate(ExtendedIterator<OntStatement> statements,
                                                         Function<OntStatement, ONTObject<Axiom>> toAxiom,
                                                         boolean split) {
        return split ?
                Iter.flatMap(statements, OntModels::listSplitStatements).mapWith(toAxiom) :
                statements.mapWith(toAxiom);
    }

    /**
     * Returns a stream of statements defining the axiom in the base graph of the specified model.
     *
     * @param model {@link OntGraphModel ONT-API Jena Model}
     * @return a {@code Stream} of {@link OntStatement}s, always local (not from imports)
     */
    public final Stream<OntStatement> statements(OntGraphModel model) {
        return Iter.asStream(listStatements(model, getConfig(model)));
    }

    /**
     * Tests if the given statement defines an {@link Axiom axiom}.
     *
     * @param statement {@link OntStatement}, not {@code null}
     * @return {@code true} if the statement defines the axiom of the type {@link Axiom}
     */
    public final boolean testStatement(OntStatement statement) {
        return testStatement(statement, getConfig(statement.getModel()));
    }

    /**
     * Performs translation {@code OntStatement -> OWLAxiom} using either the default settings
     * or extracted from the statement.
     *
     * @param statement {@link OntStatement}
     * @return {@link ONTObject} around {@link OWLAxiom}
     * @throws JenaException if no possible to translate statement to axiom
     */
    public final ONTObject<Axiom> toAxiom(OntStatement statement) throws JenaException {
        OntGraphModel m = statement.getModel();
        if (m instanceof HasConfig && m instanceof HasObjectFactory) {
            // use ONTObject implementations:
            return toAxiom(statement, () -> m,
                    ((HasObjectFactory) m).getObjectFactory(), ((HasConfig) m).getConfig());
        }
        // use ONTWrapper:
        return toAxiom(statement, getObjectFactory(statement.getModel()), getConfig(statement.getModel()));
    }

    /**
     * Lists all statements from the base graph of the given model that match this axiom definition.
     *
     * @param model  {@link OntGraphModel Ontology Jena Model}, not {@code null}
     * @param config {@link InternalConfig} control settings, not {@code null}
     * @return {@link ExtendedIterator} of {@link OntStatement}s
     */
    public abstract ExtendedIterator<OntStatement> listStatements(OntGraphModel model, InternalConfig config);

    /**
     * Tests if the specified statement answers the axiom definition.
     *
     * @param statement {@link OntStatement} any statement, not necessarily local
     * @param config    {@link InternalConfig} control settings
     * @return {@code true} if the statement corresponds axiom type
     */
    public abstract boolean testStatement(OntStatement statement, InternalConfig config);

    /**
     * Creates an OWL Axiom from a statement.
     * Impl note: the method returns a simple {@link ONTWrapperImpl ONT Wrapper} with an {@link Axiom} inside,
     * an axiom is created from the system-wide {@link ru.avicomp.ontapi.DataFactory Data Factory}.
     * TODO: will be replaced with {@link #toAxiom(OntStatement, Supplier, InternalObjectFactory, InternalConfig)},
     *  see <a href='https://github.com/avicomp/ont-api/issues/87'>#87</a>
     *
     * @param statement {@link OntStatement} the statement which determines the axiom
     * @param factory   {@link InternalObjectFactory} the data factory to create OWL-API objects
     * @param config    {@link InternalConfig} to control process
     * @return {@link ONTObject} around {@link OWLAxiom}
     * @throws JenaException if no possible to get an axiom from the statement
     */
    protected abstract ONTObject<Axiom> toAxiom(OntStatement statement,
                                                InternalObjectFactory factory,
                                                InternalConfig config) throws JenaException;

    /**
     * Creates an OWL Axiom from a statement.
     * Impl note: the method returns {@link ONTObject ONT Object} which is attached to the model.
     *
     * @param statement {@link OntStatement} the statement which determines the axiom
     * @param model     a facility (as {@link Supplier}) to provide nonnull {@link OntGraphModel}
     * @param factory   {@link InternalObjectFactory} the data factory to create OWL-API objects
     * @param config    {@link InternalConfig} to control process
     * @return {@link ONTObject} around {@link OWLAxiom}
     * @throws JenaException if no possible to get axiom from the statement
     */
    protected ONTObject<Axiom> toAxiom(OntStatement statement,
                                       Supplier<OntGraphModel> model,
                                       InternalObjectFactory factory,
                                       InternalConfig config) throws JenaException {
        return toAxiom(statement, factory, config);
    }
}
