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

package com.github.owlcs.ontapi;

import com.github.owlcs.ontapi.transforms.GraphStats;
import com.github.owlcs.ontapi.transforms.OWLIDTransform;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.ontapi.utils.Graphs;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.io.OWLOntologyLoaderMetaData;
import org.semanticweb.owlapi.io.RDFOntologyHeaderStatus;
import org.semanticweb.owlapi.io.RDFTriple;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of {@link OWLOntologyLoaderMetaData}.
 * A wrapper for {@link GraphStats Transformation Stats} to satisfy OWL-API.
 * Constructed while loading to provide some additional information about runtime changes in the source.
 * <p>
 * Created by @ssz on 28.06.2018.
 *
 * @see GraphStats
 */
@SuppressWarnings("WeakerAccess")
public class OntologyMetaData implements OWLOntologyLoaderMetaData {
    @Serial
    private static final long serialVersionUID = -1;

    private final transient Graph graph;
    private Integer count;
    private final RDFOntologyHeaderStatus header;
    private final Set<RDFTriple> unparsed;
    private final ArrayListMultimap<IRI, Class<? extends OWLObject>> guessed;

    /**
     * Default constructor.
     *
     * @param graph    {@link Graph Jena Graph}
     * @param header   {@link RDFOntologyHeaderStatus}
     * @param unparsed Set of {@link RDFTriple}s
     * @param guessed  {@link ArrayListMultimap}, IRI as key (even for blank nodes), Class type as value.
     */
    protected OntologyMetaData(Graph graph,
                               RDFOntologyHeaderStatus header,
                               Set<RDFTriple> unparsed,
                               ArrayListMultimap<IRI, Class<? extends OWLObject>> guessed) {
        this.graph = graph;
        this.header = header;
        this.unparsed = unparsed;
        this.guessed = guessed;
    }

    /**
     * A factory method.
     * Creates {@link OWLOntologyLoaderMetaData} from the given {@link Graph Jena Graph}.
     *
     * @param graph not null
     * @return {@link OntologyMetaData} instance, not null
     */
    public static OntologyMetaData createParserMetaData(Graph graph) {
        return new OntologyMetaData(Graphs.getPrimary(Objects.requireNonNull(graph, "Null graph")),
                RDFOntologyHeaderStatus.PARSED_ONE_HEADER,
                Collections.emptySet(),
                ArrayListMultimap.create());
    }

    /**
     * A factory method.
     * Creates {@link OWLOntologyLoaderMetaData} from the given {@link GraphStats Transformation Stats}.
     *
     * @param stats not null
     * @return {@link OntologyMetaData} instance, not null
     */
    public static OntologyMetaData createParserMetaData(GraphStats stats) {
        Graph graph = Graphs.getPrimary(Objects.requireNonNull(stats, "Null graph").getGraph());
        RDFOntologyHeaderStatus header = RDFOntologyHeaderStatus.PARSED_ONE_HEADER;
        String idKey = OWLIDTransform.class.getSimpleName();
        if (stats.hasTriples(GraphStats.Type.ADDED, idKey)) {
            header = RDFOntologyHeaderStatus.PARSED_ZERO_HEADERS;
        }
        if (stats.hasTriples(GraphStats.Type.DELETED, idKey)) {
            header = RDFOntologyHeaderStatus.PARSED_MULTIPLE_HEADERS;
        }
        Set<RDFTriple> unparsed = stats.triples(GraphStats.Type.UNPARSED)
                .map(OntGraphUtils::triple)
                .collect(Collectors.toSet());
        ArrayListMultimap<IRI, Class<? extends OWLObject>> guessedDeclarations = ArrayListMultimap.create();
        stats.triples(GraphStats.Type.ADDED)
                .collect(Collectors.toSet())
                .forEach(t -> {
                    // note: anonymous subject also wrapped as iri
                    IRI iri = IRI.create(t.getSubject().toString());
                    Class<? extends OWLObject> type = guessTripleType(t);
                    guessedDeclarations.put(iri, type);
                });
        return new OntologyMetaData(graph, header, unparsed, guessedDeclarations);
    }

    /**
     * Guesses OWL-API class type by the given {@link Triple Jena Triple}.
     *
     * @param t triple, not null
     * @return {@link OWLObject} class-type, not null
     */
    protected static Class<? extends OWLObject> guessTripleType(Triple t) {
        if (!RDF.type.asNode().equals(t.getPredicate())) {
            return OWLObject.class;
        }
        // declaration (note: anonymous object also wrapped as uri-resource)
        Resource obj = ResourceFactory.createResource(t.getObject().toString());
        if (OWL.Ontology.equals(obj)) {
            return OWLOntology.class;
        }
        if (t.getSubject().isURI() && t.getObject().isURI()) { // named subject and object
            if (OWL.Class.equals(obj)) {
                return OWLClass.class;
            }
            if (RDFS.Datatype.equals(obj)) {
                return OWLDatatype.class;
            }
            if (OWL.NamedIndividual.equals(obj)) {
                return OWLNamedIndividual.class;
            }
            if (OWL.ObjectProperty.equals(obj)) {
                return OWLObjectProperty.class;
            }
            if (OWL.DatatypeProperty.equals(obj)) {
                return OWLDataProperty.class;
            }
            if (OWL.AnnotationProperty.equals(obj)) {
                return OWLAnnotationProperty.class;
            }
        }
        if (OWL.Class.equals(obj) || OWL.Restriction.equals(obj)) {
            return OWLClassExpression.class;
        }
        if (RDFS.Datatype.equals(obj)) {
            return OWLDataRange.class;
        }
        return OWLObject.class;
    }

    /**
     * Loads triple count in lazy manner if the associated graph is {@code GraphMem}.
     * Otherwise, returns -1.
     * Lazy loading is in order to relieve performance on loading.
     * Anyway nobody uses this stupid interface.
     *
     * @return int size of graph
     */
    @Override
    public int getTripleCount() {
        return count == null ? count = calcTripleCount() : count;
    }

    protected int calcTripleCount() {
        return Graphs.isGraphMem(graph) ? graph.size() : -1;
    }

    @Override
    public RDFOntologyHeaderStatus getHeaderState() {
        return header;
    }

    @Override
    public Stream<RDFTriple> getUnparsedTriples() {
        return unparsed.stream();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Multimap<IRI, Class<?>> getGuessedDeclarations() {
        return Multimaps.unmodifiableMultimap((Multimap) guessed);
    }

    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        getTripleCount();
        out.defaultWriteObject();
    }
}
