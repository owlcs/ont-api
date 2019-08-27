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

package ru.avicomp.ontapi;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyID;
import ru.avicomp.ontapi.jena.model.OntID;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.Optional;

/**
 * An enhanced {@link OWLOntologyID} that is based on {@link Node Jena Graph Node}.
 * This is an ontology identifier, any OWL2 ontology model must have one and only one ontology id.
 * There is an important restriction: an anonymous ontology id cannot have version iri.
 * <p>
 * Created by @ssz on 13.09.2018.
 *
 * @see OntID
 * @see <a href='https://www.w3.org/TR/owl-syntax/#Ontology_IRI_and_Version_IRI'>3.1 Ontology IRI and Version IRI</a>
 */
public class OntologyID extends OWLOntologyID implements AsNode {

    protected final Node node;

    /**
     * Constructs an ID instance by the given iri and without any version iri.
     *
     * @param iri String, not {@code null}
     */
    public OntologyID(String iri) {
        this(iri, null);
    }

    /**
     * Constructs an ID instance by the given iri and version iri.
     *
     * @param iri String, not {@code null}
     * @param ver String, can be {@code null}
     */
    public OntologyID(String iri, String ver) {
        this(NodeFactory.createURI(Objects.requireNonNull(iri, "Null iri")), ver);
    }

    /**
     * Constructs an anonymous ontology identifier
     * specifying that the ontology IRI (and hence the version IRI) is not present.
     */
    public OntologyID() {
        this(NodeFactory.createBlankNode(), null);
    }

    /**
     * Constructs an ontology identifier from the given {@link OntID Ontology Graph Model ID}.
     * The difference between these two IDs ( {@link OntID} and {@link OntologyID this classs}) is
     * that the former is used in the RDF representation (i.e. in {@link ru.avicomp.ontapi.jena.model.OntGraphModel}),
     * while the other is in the axiomatic view (i.e. in {@link org.semanticweb.owlapi.model.OWLOntology}).
     *
     * @param id {@link OntID}, not {@code null}
     */
    public OntologyID(OntID id) {
        this(id.asNode(), id.getVersionIRI());
    }

    /**
     * Constructs an ontology identifier from the given {@link Node RDF Graph Node} and {@code version}-iri.
     *
     * @param id      {@link Node}, not {@code null}, ether blank or uri node
     * @param version {@link String}, can be {@code null}
     * @throws OntApiException               if {@code id} is anonymous and {@code version} is not {@code null}
     * @throws IllegalStateException         if the OWL-API guys have changed the basic implementation, and I do not know about it yet
     * @throws UnsupportedOperationException if {@code id} is not uri or blank node
     * @throws NullPointerException          if {@code id} is {@code null}
     */
    protected OntologyID(Node id, String version) throws OntApiException,
            IllegalStateException, UnsupportedOperationException, NullPointerException {
        Optional<String> internalID;
        Optional<IRI> ontologyIRI, versionIRI;
        if (Objects.requireNonNull(id, "Null id node").isBlank()) {
            if (version != null) {
                throw new OntApiException("Anonymous ontology id (" + id +
                        ") can not be accompanied by a version (" + version + ")");
            }
            internalID = Optional.of(id.getBlankNodeLabel());
            ontologyIRI = Optional.empty();
            versionIRI = Optional.empty();
        } else {
            internalID = Optional.empty();
            ontologyIRI = Optional.of(id.getURI()).map(IRI::create);
            versionIRI = Optional.ofNullable(version).map(IRI::create);
        }
        this.node = id;
        int hashCode = 17;
        hashCode += 37 * internalID.hashCode();
        hashCode += 37 * ontologyIRI.hashCode();
        hashCode += 37 * versionIRI.hashCode();
        setField("hashCode", hashCode);
        setField("internalID", internalID);
        setField("ontologyIRI", ontologyIRI);
        setField("versionIRI", versionIRI);
    }

    private void setField(String name, Object value) {
        try {
            Field field = getClass().getSuperclass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(this, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("Can't get access to " + name, e);
        }
    }

    /**
     * Answers the graph node that this ontology identifier wraps.
     *
     * @return {@link Node}, not {@code null}
     */
    @Override
    public Node asNode() {
        return node;
    }

    /**
     * Converts any instance of {@link OWLOntologyID} to the {@link OntologyID ONT-API Ontology Identifier implementation}.
     *
     * @param id {@link OWLOntologyID}, not {@code null}
     * @return {@link OntologyID}, not {@code null}
     * @throws IllegalArgumentException should never happen, actually
     */
    public static OntologyID asONT(OWLOntologyID id) throws IllegalArgumentException {
        if (id instanceof OntologyID) {
            return (OntologyID) id;
        }
        if (id.isAnonymous()) return new OntologyID();
        String iri = id.getOntologyIRI().map(IRI::getIRIString).orElseThrow(IllegalArgumentException::new);
        String ver = id.getVersionIRI().map(IRI::getIRIString).orElse(null);
        return create(iri, ver);
    }

    /**
     * Creates an {@code OntologyID} by the given {@code iri}.
     * Notice that the corresponding constructor does not allow {@code null}s, while this method does.
     *
     * @param iri {@link IRI}, can be {@code null}
     * @return {@link OntologyID}, not {@code null}
     */
    public static OntologyID create(IRI iri) {
        return create(iri == null ? null : iri.getIRIString(), null);
    }

    /**
     * Creates an {@code OntologyID} by the given {@code iri} and {@code ver}.
     * Notice that the corresponding constructor does not allow {@code null}s, while this method does.
     *
     * @param iri String, can be {@code null}
     * @param ver String, can be {@code null}
     * @return {@link OntologyID}, not {@code null}
     */
    public static OntologyID create(String iri, String ver) {
        return iri == null ? new OntologyID() : new OntologyID(iri, ver);
    }

}
