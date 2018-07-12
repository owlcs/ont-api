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

package ru.avicomp.ontapi.jena;

import org.apache.jena.graph.Graph;
import org.apache.jena.mem.GraphMem;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.impl.ModelCom;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.system.JenaSystem;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import ru.avicomp.ontapi.jena.impl.OntGraphModelImpl;
import ru.avicomp.ontapi.jena.impl.conf.OntModelConfig;
import ru.avicomp.ontapi.jena.impl.conf.OntPersonality;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

/**
 * A factory to produce different kinds of {@link OntGraphModel OWL2 model}s, {@link Model Common model}s and {@link Graph graph}s.
 * It is an ONT-API analogue of {@link org.apache.jena.rdf.model.ModelFactory}.
 * <p>
 * Created by szuev on 14.02.2017.
 */
public class OntModelFactory {

    static {
        init();
    }

    /**
     * A standard prefixes map for any OWL2 ontology. Just for convenience.
     */
    public static final PrefixMapping STANDARD = PrefixMapping.Factory.create()
            .setNsPrefix("owl", OWL.NS)
            .setNsPrefix("rdfs", RDFS.uri)
            .setNsPrefix("rdf", RDF.uri)
            .setNsPrefix("xsd", XSD.NS)
            .lock();

    /**
     * Initializes Jena System.
     */
    public static void init() {
        JenaSystem.init();
    }

    /**
     * Creates default (in-memory) graph implementation.
     *
     * @return {@link GraphMem in-memory Jena Graph}
     * @see org.apache.jena.graph.Factory#createGraphMem()
     */
    public static Graph createDefaultGraph() {
        return new GraphMem();
    }

    /**
     * Creates default (in-memory) RDF Model implementation.
     *
     * @return {@link Model}
     * @see org.apache.jena.rdf.model.ModelFactory#createDefaultModel()
     */
    public static Model createDefaultModel() {
        return new ModelCom(createDefaultGraph());
    }

    /**
     * Creates a fresh in-memory Ontology RDF Model with default personalities.
     *
     * @return {@link OntGraphModel}
     * @see OntModelConfig#getPersonality()
     */
    public static OntGraphModel createModel() {
        return createModel(createDefaultGraph());
    }

    /**
     * Creates an Ontology RDF Model wrapper around the given graph with default personalities.
     *
     * @param graph {@link Graph}
     * @return {@link OntGraphModel}
     */
    public static OntGraphModel createModel(Graph graph) {
        return createModel(graph, OntModelConfig.getPersonality());
    }

    /**
     * Creates an Ontology RDF Model wrapper around the given graph with given personalities.
     *
     * @param graph       {@link Graph}
     * @param personality {@link OntPersonality}
     * @return {@link OntGraphModel}
     */
    public static OntGraphModel createModel(Graph graph, OntPersonality personality) {
        return new OntGraphModelImpl(graph, personality);
    }

}
