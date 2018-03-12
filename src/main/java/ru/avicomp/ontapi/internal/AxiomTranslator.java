/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2018, Avicomp Services, AO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 *
 */

package ru.avicomp.ontapi.internal;

import org.apache.jena.graph.Triple;
import org.apache.jena.shared.JenaException;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntStatement;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The base class to perform Axiom Graph Translator (operator 'T'), both for reading and writing.
 * Specification: <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Mapping_from_the_Structural_Specification_to_RDF_Graphs'>2.1 Translation of Axioms without Annotations</a>
 * How to annotate see <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Axioms_that_are_Translated_to_Multiple_Triples'>2.3.2 Axioms that are Translated to Multiple Triples</a>
 * One more helpful link: <a href='https://www.w3.org/TR/owl2-quick-reference/'>Quick Reference Guide</a>
 * <p>
 * Created by @szuev on 28.09.2016.
 *
 * @param <Axiom> generic type of {@link OWLAxiom}
 */
@SuppressWarnings("WeakerAccess")
public abstract class AxiomTranslator<Axiom extends OWLAxiom> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AxiomTranslator.class);

    /**
     * Writes axiom to model.
     *
     * @param axiom {@link OWLAxiom}
     * @param model {@link OntGraphModel}
     */
    public abstract void write(Axiom axiom, OntGraphModel model);

    /**
     * Reads axioms and triples from model.
     *
     * @param model {@link OntGraphModel}
     * @return Set of {@link InternalObject} with {@link OWLAxiom} as key and Set of {@link Triple} as value
     * @throws OntApiException if something is wrong and 'ont.api.load.conf.ignore.axioms.read.errors' is false.
     */
    public Set<InternalObject<Axiom>> read(OntGraphModel model) throws OntApiException {
        try {
            try {
                return axioms(model).collect(Collectors.toSet());
            } catch (JenaException e) {
                throw new OntApiException(String.format("Can't process reading. Translator <%s>.", getClass()), e);
            }
        } catch (OntApiException e) {
            if (!getConfig(model).loaderConfig().isIgnoreAxiomsReadErrors()) {
                throw e;
            }
            LOGGER.warn("{}: ontology <{}> contains unparsable axioms", getClass().getSimpleName(), model.getID(), e);
            return Collections.emptySet();
        }
    }

    /**
     * Reads all axioms as stream.
     *
     * @param model {@link OntGraphModel jena-model}
     * @return Stream of {@link InternalObject}
     * @throws JenaException unable to read axioms for this type.
     */
    public Stream<InternalObject<Axiom>> axioms(OntGraphModel model) throws JenaException {
        return statements(model).flatMap(OntStatement::split).map(this::toAxiom);
    }

    /**
     * Returns the stream of statements defining the axiom in the base graph of the specified model.
     *
     * @param model {@link OntGraphModel} the model
     * @return Stream of {@link OntStatement}, always local (not from imports)
     */
    public abstract Stream<OntStatement> statements(OntGraphModel model);

    /**
     * Tests if the specified statement answers the axiom's definition.
     *
     * @param statement {@link OntStatement} any statement, not necessarily local.
     * @return true if the statement corresponds axiom type.
     */
    public abstract boolean testStatement(OntStatement statement);

    /**
     * Creates an OWL Axiom from a statement.
     *
     * @param statement {@link OntStatement} the statement which determines the axiom
     * @return {@link InternalObject} around the {@link OWLAxiom}
     */
    public abstract InternalObject<Axiom> toAxiom(OntStatement statement);

    /**
     * Returns the container with set of {@link OWLAnnotation} associated with the specified statement.
     *
     * @param statement {@link OntStatement}
     * @param conf      {@link ConfigProvider}
     * @return Collection of {@link InternalObject wrap}s around {@link OWLAnnotation}
     */
    Collection<InternalObject<OWLAnnotation>> getAnnotations(OntStatement statement, ConfigProvider.Config conf) {
        return ReadHelper.getAnnotations(statement, conf.dataFactory(), conf.loaderConfig());
    }

    /**
     * Gets the config from model's settings or dummy if it is naked Jena model.
     *
     * @param model {@link OntGraphModel}
     * @return {@link ConfigProvider.Config}
     */
    public ConfigProvider.Config getConfig(OntGraphModel model) {
        return model instanceof ConfigProvider ? ((ConfigProvider) model).getConfig() : ConfigProvider.DEFAULT;
    }

    /**
     * Gets the config from statement.
     *
     * @param statement {@link OntStatement}
     * @return {@link ConfigProvider.Config}
     */
    protected ConfigProvider.Config getConfig(OntStatement statement) {
        return getConfig(statement.getModel());
    }
}
