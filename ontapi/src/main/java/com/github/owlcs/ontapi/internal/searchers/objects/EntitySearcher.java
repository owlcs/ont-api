/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2022, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.internal.searchers.objects;

import com.github.owlcs.ontapi.config.AxiomsSettings;
import com.github.owlcs.ontapi.internal.AxiomTranslator;
import com.github.owlcs.ontapi.internal.ModelObjectFactory;
import com.github.owlcs.ontapi.internal.ONTObject;
import com.github.owlcs.ontapi.internal.ONTObjectFactory;
import com.github.owlcs.ontapi.internal.ObjectsSearcher;
import com.github.owlcs.ontapi.internal.searchers.WithRootStatement;
import com.github.owlcs.ontapi.jena.impl.PersonalityModel;
import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.jena.model.OntObject;
import com.github.owlcs.ontapi.jena.model.OntStatement;
import com.github.owlcs.ontapi.jena.utils.Iterators;
import com.github.owlcs.ontapi.jena.utils.OntModels;
import com.github.owlcs.ontapi.jena.vocabulary.RDF;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;

import java.util.Optional;
import java.util.function.Function;

/**
 * An abstract {@link OWLEntity} searcher.
 * Created by @ssz on 30.06.2020.
 *
 * @param <E> - subtype of {@link OWLEntity}
 */
abstract class EntitySearcher<E extends OWLEntity> extends WithRootStatement implements ObjectsSearcher<E> {

    static PersonalityModel asPersonalityModel(OntModel m) {
        return PersonalityModel.asPersonalityModel(m);
    }

    /**
     * Recursively lists all annotations for the ontology header in the form of a flat stream.
     *
     * @param m {@link OntModel}, not {@code null}
     * @return an {@code ExtendedIterator} of all header annotations including sub-annotations
     */
    public static ExtendedIterator<OntStatement> listHeaderAnnotations(OntModel m) {
        return m.id().map(OntObject::getMainStatement).map(OntModels::listAllAnnotations).orElse(Iterators.of());
    }

    @Override
    public final ExtendedIterator<ONTObject<E>> listONTObjects(OntModel model,
                                                               ONTObjectFactory factory,
                                                               AxiomsSettings config) {
        return listEntities(model, config).mapWith(createMapping(model, factory));
    }

    @Override
    public final boolean containsONTObject(E object, OntModel model, ONTObjectFactory factory, AxiomsSettings config) {
        return containsEntity(object.getIRI().getIRIString(), model, config);
    }

    @Override
    public final Optional<ONTObject<E>> findONTObject(E object,
                                                      OntModel model,
                                                      ONTObjectFactory factory,
                                                      AxiomsSettings config) {
        String uri = object.getIRI().getIRIString();
        if (containsEntity(uri, model, config)) {
            return Optional.of(createONTEntity(uri, model, factory));
        }
        return Optional.empty();
    }

    protected abstract ExtendedIterator<String> listEntities(OntModel m, AxiomsSettings conf);

    protected abstract boolean containsEntity(String uri, OntModel m, AxiomsSettings conf);

    protected abstract ExtendedIterator<? extends AxiomTranslator<OWLAxiom>> listTranslators();

    protected abstract ONTObject<E> createEntity(String uri, OntModel model, ONTObjectFactory factory);

    protected abstract ONTObject<E> createEntity(String uri, ModelObjectFactory factory);

    protected abstract Resource getEntityType();

    final ONTObject<E> createONTEntity(String uri, OntModel model, ONTObjectFactory factory) {
        if (factory instanceof ModelObjectFactory) {
            return createEntity(uri, (ModelObjectFactory) factory);
        }
        return createEntity(uri, model, factory);
    }

    final Function<String, ONTObject<E>> createMapping(OntModel model, ONTObjectFactory factory) {
        if (factory instanceof ModelObjectFactory) {
            ModelObjectFactory f = (ModelObjectFactory) factory;
            return uri -> createEntity(uri, f);
        }
        return uri -> createEntity(uri, model, factory);
    }

    protected final ExtendedIterator<? extends AxiomTranslator<OWLAxiom>> listTranslators(OntStatement statement,
                                                                                          AxiomsSettings conf) {
        return listTranslators().filterKeep(t -> t.testStatement(statement, conf));
    }

    /**
     * Lists all uris (entities) that are declared somewhere in imports,
     * but are also present in the base graph in some axiom definition.
     *
     * @param m {@link OntModel}
     * @return {@link ExtendedIterator} of uris ({@code String}s)
     */
    protected ExtendedIterator<String> listSharedFromImports(OntModel m) {
        return listFromImports(m).mapWith(Resource::getURI);
    }

    private ExtendedIterator<Resource> listFromImports(OntModel m) {
        ExtendedIterator<OntModel> imports = OntModels.listImports(m);
        return Iterators.distinct(Iterators.flatMap(imports, i -> i.listStatements(null, RDF.type, getEntityType()))
                .mapWith(Statement::getSubject).filterKeep(RDFNode::isURIResource));
    }

    protected boolean containsInOntology(String uri, OntModel m, AxiomsSettings conf) {
        return containsInOntology(toResource(m, uri), m, conf);
    }

    /**
     * Answers {@code true} if the given uri-resource (OWLEntity) contains somewhere
     * in the given OWL-ontology deeps (in some axiom or in the header).
     *
     * @param uri  {@link Resource}, entity of {@link #getEntityType() this type}, not {@code null}
     * @param m    {@link OntModel}, not {@code null}
     * @param conf {@link AxiomsSettings}, not {@code null}
     * @return boolean
     */
    protected boolean containsInOntology(Resource uri, OntModel m, AxiomsSettings conf) {
        return containsInAxiom(uri, m, conf);
    }

    final boolean containsInAxiom(Resource uri, OntModel m, AxiomsSettings conf) {
        return containsAxiom(listRootStatements(m, uri), conf);
    }

    protected final boolean containsAxiom(ExtendedIterator<OntStatement> roots, AxiomsSettings conf) {
        return Iterators.anyMatch(roots, s -> Iterators.findFirst(listTranslators(s, conf)).isPresent());
    }

    protected ExtendedIterator<OntStatement> listRootStatements(OntModel m, Resource uri) {
        return Iterators.concat(listBySubject(m, uri), Iterators.flatMap(listByObject(m, uri), s -> listRootStatements(m, s)));
    }

    protected Resource toResource(OntModel m, String uri) {
        return m.getResource(uri);
    }

    final boolean containsDeclaration(Resource uri, OntModel m, AxiomsSettings conf) {
        Resource type = getEntityType();
        if (m.independent()) {
            return m.getBaseGraph().contains(uri.asNode(), RDF.type.asNode(), type.asNode());
        }
        if (!m.contains(uri, RDF.type, type)) {
            return false;
        }
        return containsInAxiom(uri, m, conf);
    }
}
