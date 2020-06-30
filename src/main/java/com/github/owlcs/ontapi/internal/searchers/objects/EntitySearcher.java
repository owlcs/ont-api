/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2020, owl.cs group.
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
import com.github.owlcs.ontapi.internal.*;
import com.github.owlcs.ontapi.internal.searchers.WithRootStatement;
import com.github.owlcs.ontapi.jena.impl.PersonalityModel;
import com.github.owlcs.ontapi.jena.impl.conf.OntPersonality;
import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.jena.model.OntStatement;
import com.github.owlcs.ontapi.jena.utils.Iter;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;

import java.util.Optional;
import java.util.function.Function;

/**
 * Created by @ssz on 30.06.2020.
 *
 * @param <E> - subtype of {@link OWLEntity}
 */
abstract class EntitySearcher<E extends OWLEntity> extends WithRootStatement implements ObjectsSearcher<E> {

    protected static OntPersonality.Builtins getBuiltins(OntModel m) {
        return asPersonalityModel(m).getOntPersonality().getBuiltins();
    }

    static PersonalityModel asPersonalityModel(OntModel m) {
        return PersonalityModel.asPersonalityModel(m);
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

    protected final boolean containsInAxiom(String uri, OntModel m, AxiomsSettings conf) {
        return containsInAxiom(m.getResource(uri), m, conf);
    }

    protected boolean containsInAxiom(Resource uri, OntModel m, AxiomsSettings conf) {
        return containsAxiom(listRootStatements(m, uri), conf);
    }

    protected final boolean containsAxiom(ExtendedIterator<OntStatement> roots, AxiomsSettings conf) {
        return Iter.anyMatch(roots, s -> Iter.findFirst(listTranslators(s, conf)).isPresent());
    }

    private ExtendedIterator<OntStatement> listRootStatements(OntModel m, Resource uri) {
        return Iter.concat(listBySubject(m, uri), Iter.flatMap(listByObject(m, uri), s -> listRootStatements(m, s)));
    }
}
