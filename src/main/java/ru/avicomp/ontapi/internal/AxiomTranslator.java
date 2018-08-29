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
 */

package ru.avicomp.ontapi.internal;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.shared.JenaException;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.util.iterator.WrappedIterator;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import ru.avicomp.ontapi.jena.impl.OntGraphModelImpl;
import ru.avicomp.ontapi.jena.model.OntEntity;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntObject;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.utils.Iter;
import ru.avicomp.ontapi.jena.utils.Models;

import java.util.stream.Stream;

/**
 * The base abstract class to perform Axiom Graph Translator (operator 'T'), both for reading and writing.
 * Specification: <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Mapping_from_the_Structural_Specification_to_RDF_Graphs'>2.1 Translation of Axioms without Annotations</a>.
 * Additional info about annotations translation <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Axioms_that_are_Translated_to_Multiple_Triples'>2.3.2 Axioms that are Translated to Multiple Triples</a>.
 * One more (and most useful) link: <a href='https://www.w3.org/TR/owl2-quick-reference/'>Quick Reference Guide</a>.
 * To get particular instance of this class the method {@link AxiomParserProvider#get(AxiomType)} can be used.
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
     * Reads all model axioms in form of stream.
     *
     * @param model {@link OntGraphModel ONT-API Jena Model}
     * @return Stream of {@link ONTObject} around {@link OWLAxiom}
     * @throws JenaException unable to read axioms of this type.
     */
    public Stream<ONTObject<Axiom>> axioms(OntGraphModel model) throws JenaException {
        ConfigProvider.Config conf = getConfig(model);
        return adjust(conf, statements(model)).map(this::toAxiom);
    }

    /**
     * Creates an OWL Axiom from a statement.
     *
     * @param statement {@link OntStatement} the statement which determines the axiom
     * @return {@link ONTObject} around {@link OWLAxiom}
     */
    public abstract ONTObject<Axiom> toAxiom(OntStatement statement);

    /**
     * Performs common operations on the stream of {@link OntStatement Ontology Statement}s
     * before converting them into axioms.
     *
     * @param conf       {@link ConfigProvider.Config} configuration
     * @param statements Stream of {@link OntStatement}s
     * @return Stream of {@link OntStatement}s
     */
    protected Stream<OntStatement> adjust(ConfigProvider.Config conf, Stream<OntStatement> statements) {
        Stream<OntStatement> res = statements.map(Models::createCachedStatement);
        if (conf.isSplitAxiomAnnotations()) {
            return res.flatMap(Models::split);
        }
        return res;
    }

    /**
     * Returns a stream of statements defining the axiom in the base graph of the specified model.
     *
     * @param model {@link OntGraphModel ONT-API Jena Model}
     * @return Stream of {@link OntStatement}, always local (not from imports)
     */
    public final Stream<OntStatement> statements(OntGraphModel model) {
        return Iter.asStream(listStatements(model));
    }

    /**
     * Lists all statements for the base graph of the given model that match this axiom definition.
     *
     * @param model {@link OntGraphModel Ontology Jena Model}
     * @return {@link ExtendedIterator} of {@link OntStatement}s
     */
    protected abstract ExtendedIterator<OntStatement> listStatements(OntGraphModel model);

    /**
     * Tests if the specified statement answers the axiom definition.
     *
     * @param statement {@link OntStatement} any statement, not necessarily local.
     * @return true if the statement corresponds axiom type.
     */
    public abstract boolean testStatement(OntStatement statement);

    /**
     * Lists all model statements, which belong to the base graph, using the given SPO.
     * Placed here for more control.
     * Not sure that the methods to work with {@code ExtendedIterator}
     * (like {@link OntGraphModelImpl#listLocalStatements(Resource, Property, RDFNode)})
     * should be placed in public interface: {@code Stream}-based analogues are almost the same but more functional.
     * But they are needed to make the axioms listing a bit faster.
     *
     * @param model {@link OntGraphModel}
     * @param s     {@link Resource}, can be {@code null}
     * @param p     {@link Property}, can be {@code null}
     * @param o     {@link RDFNode}, can be {@code null}
     * @return {@link ExtendedIterator} of {@link OntStatement}s local to the base model graph
     * @see OntGraphModel#localStatements(Resource, Property, RDFNode)
     * @since 1.3.0
     */
    public static ExtendedIterator<OntStatement> listStatements(OntGraphModel model, Resource s, Property p, RDFNode o) {
        if (model instanceof OntGraphModelImpl) {
            return ((OntGraphModelImpl) model).listLocalStatements(s, p, o);
        }
        return asIterator(model.localStatements(s, p, o));
    }

    /**
     * Lists all ontology objects with the given {@code type}, which are defined in the base graph.
     *
     * @param model {@link OntGraphModel}
     * @param type  {@link Class}-type
     * @param <O>   subclass of {@link OntObject}
     * @return {@link ExtendedIterator} of ontology objects of the type {@link O}
     * @see OntGraphModel#ontObjects(Class)
     * @since 1.3.0
     */
    public static <O extends OntObject> ExtendedIterator<O> listOntObjects(OntGraphModel model, Class<O> type) {
        ExtendedIterator<O> res;
        if (model instanceof OntGraphModelImpl) {
            res = ((OntGraphModelImpl) model).listOntObjects(type);
        } else {
            res = asIterator(model.ontObjects(type));
        }
        return res.filterKeep(OntObject::isLocal);
    }

    /**
     * Lists all OWL entities that are defined in the base graph.
     *
     * @param model {@link OntGraphModel}
     * @return {@link ExtendedIterator} of {@link OntEntity}s
     * @see OntGraphModel#ontEntities()
     * @since 1.3.0
     */
    public static ExtendedIterator<OntEntity> listEntities(OntGraphModel model) {
        ExtendedIterator<OntEntity> res;
        if (model instanceof OntGraphModelImpl) {
            res = ((OntGraphModelImpl) model).listOntEntities();
        } else {
            res = asIterator(model.ontEntities());
        }
        return res.filterKeep(OntObject::isLocal);
    }

    private static <R> ExtendedIterator<R> asIterator(Stream<R> stream) {
        return WrappedIterator.create(stream.iterator());
    }

    /**
     * Gets the config from model's settings or dummy if it is naked Jena model.
     *
     * @param model {@link OntGraphModel}
     * @return {@link ConfigProvider.Config}
     */
    public static ConfigProvider.Config getConfig(OntGraphModel model) {
        return model instanceof ConfigProvider ? ((ConfigProvider) model).getConfig() : ConfigProvider.DEFAULT_CONFIG;
    }

    /**
     * Gets the ONT-API Data-Factory from model's settings.
     *
     * @param model {@link OntGraphModel}
     * @return {@link InternalDataFactory}
     */
    public static InternalDataFactory getDataFactory(OntGraphModel model) {
        return model instanceof InternalModel ? ((InternalModel) model).getDataFactory() : ConfigProvider.DEFAULT_DATA_FACTORY;
    }
}
