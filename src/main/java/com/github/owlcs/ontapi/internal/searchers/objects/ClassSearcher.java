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

package com.github.owlcs.ontapi.internal.searchers.objects;

import com.github.owlcs.ontapi.OntApiException;
import com.github.owlcs.ontapi.config.AxiomsSettings;
import com.github.owlcs.ontapi.internal.*;
import com.github.owlcs.ontapi.internal.searchers.WithRootSearcher;
import com.github.owlcs.ontapi.internal.searchers.axioms.ByClass;
import com.github.owlcs.ontapi.jena.impl.PersonalityModel;
import com.github.owlcs.ontapi.jena.impl.conf.OntPersonality;
import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.jena.model.OntStatement;
import com.github.owlcs.ontapi.jena.utils.Iter;
import com.github.owlcs.ontapi.jena.utils.OntModels;
import com.github.owlcs.ontapi.jena.vocabulary.OWL;
import com.github.owlcs.ontapi.jena.vocabulary.RDF;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Created by @ssz on 19.04.2020.
 */
public class ClassSearcher extends WithRootSearcher implements ObjectsSearcher<OWLClass> {

    private static final Set<AxiomTranslator<OWLAxiom>> TRANSLATORS = selectTranslators(OWLComponentType.CLASS);

    protected static OntPersonality.Builtins getBuiltins(OntModel m) {
        return PersonalityModel.asPersonalityModel(m).getOntPersonality().getBuiltins();
    }

    private static Function<String, ONTObject<OWLClass>> factoryMapping(OntModel model, ONTObjectFactory factory) {
        if (factory instanceof ModelObjectFactory) {
            return ((ModelObjectFactory) factory)::getClass;
        }
        return uri -> factory.getClass(OntApiException.mustNotBeNull(model.getOntClass(uri)));
    }

    @Override
    public ExtendedIterator<ONTObject<OWLClass>> listONTObjects(OntModel model,
                                                                ONTObjectFactory factory,
                                                                AxiomsSettings config) {
        return listClasses(model, config).mapWith(factoryMapping(model, factory));
    }

    @Override
    public boolean containsONTObject(OWLClass object, OntModel model, ONTObjectFactory factory, AxiomsSettings config) {
        return containsClass(object.getIRI().getIRIString(), model, config);
    }

    @Override
    public Optional<ONTObject<OWLClass>> findONTObject(OWLClass object,
                                                       OntModel model,
                                                       ONTObjectFactory factory,
                                                       AxiomsSettings config) {
        String uri = object.getIRI().getIRIString();
        if (containsClass(uri, model, config)) {
            return Optional.of(InternalObjectFactory.getONTClass(uri, model, factory));
        }
        return Optional.empty();
    }

    protected boolean containsClass(String uri, OntModel m, AxiomsSettings conf) {
        Resource clazz = m.getResource(uri);
        if (getBuiltins(m).getClasses().contains(clazz.asNode())) {
            if (OWL.Thing.equals(clazz)) {
                if (containAxiom(Iter.flatMap(listImplicitStatements(m), s -> listRootStatements(m, s)), conf)) {
                    return true;
                }
            }
            return containAxiom(listStatements(m, clazz), conf);
        }
        if (m.independent()) {
            return m.getBaseGraph().contains(clazz.asNode(), RDF.type.asNode(), OWL.Class.asNode());
        }
        if (!m.contains(clazz, RDF.type, OWL.Class)) {
            return false;
        }
        return containAxiom(listStatements(m, clazz), conf);
    }

    protected ExtendedIterator<String> listClasses(OntModel m, AxiomsSettings conf) {
        Set<String> builtins = new HashSet<>();
        getBuiltins(m).getClasses()
                .forEach(x -> {
                    if (containAxiom(listStatements(m, m.getResource(x.getURI())), conf)) {
                        builtins.add(x.getURI());
                    }
                });
        if (!builtins.contains(OWL.Thing.getURI())) {
            if (containAxiom(Iter.flatMap(listImplicitStatements(m), s -> listRootStatements(m, s)), conf)) {
                builtins.add(OWL.Thing.getURI());
            }
        }
        ExtendedIterator<String> explicit = listByPredicateAndObject(m, RDF.type, OWL.Class)
                .mapWith(x -> x.getSubject().getURI())
                .filterKeep(x -> x != null && !builtins.contains(x));
        ExtendedIterator<String> res = Iter.concat(explicit, Iter.create(builtins));
        if (!m.independent()) {
            ExtendedIterator<String> shared = listClassesFromImports(m)
                    .filterKeep(x -> containAxiom(listStatements(m, m.getResource(x)), conf));
            res = Iter.concat(res, shared);
        }
        return res;
    }

    protected ExtendedIterator<OntStatement> listStatements(OntModel m, Resource clazz) {
        return Iter.concat(listBySubject(m, clazz), Iter.flatMap(listByObject(m, clazz), s -> listRootStatements(m, s)));
    }

    protected ExtendedIterator<OntStatement> listImplicitStatements(OntModel m) {
        return Iter.flatMap(Iter.of(OWL.cardinality, OWL.maxCardinality, OWL.minCardinality), p -> listByPredicate(m, p))
                .filterKeep(this::isCardinalityRestriction);
    }

    protected ExtendedIterator<String> listClassesFromImports(OntModel m) {
        ExtendedIterator<OntModel> imports = OntModels.listImports(m);
        return Iter.distinct(Iter.flatMap(imports, i -> i.listStatements(null, RDF.type, OWL.Class))
                .mapWith(Statement::getSubject).filterKeep(RDFNode::isURIResource).mapWith(Resource::getURI));
    }

    protected boolean isCardinalityRestriction(OntStatement s) {
        return ByClass.OBJECT_CARDINALITY_TYPES.stream().anyMatch(t -> s.getSubject().canAs(t));
    }

    protected boolean containAxiom(ExtendedIterator<OntStatement> top, AxiomsSettings conf) {
        return Iter.anyMatch(top, s -> Iter.findFirst(listTranslators(s, conf)).isPresent());
    }

    protected ExtendedIterator<? extends AxiomTranslator<OWLAxiom>> listTranslators(OntStatement statement,
                                                                                    AxiomsSettings conf) {
        return Iter.create(TRANSLATORS).filterKeep(t -> t.testStatement(statement, conf));
    }
}
