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

package com.github.owlcs.ontapi.internal.searchers.objects;

import com.github.owlcs.ontapi.OntApiException;
import com.github.owlcs.ontapi.config.AxiomsSettings;
import com.github.owlcs.ontapi.internal.AxiomTranslator;
import com.github.owlcs.ontapi.internal.ModelObjectFactory;
import com.github.owlcs.ontapi.internal.ONTObject;
import com.github.owlcs.ontapi.internal.ONTObjectFactory;
import com.github.owlcs.ontapi.internal.OWLComponentType;
import com.github.owlcs.ontapi.internal.ObjectsSearcher;
import com.github.sszuev.jena.ontapi.impl.OntGraphModelImpl;
import com.github.sszuev.jena.ontapi.impl.PersonalityModel;
import com.github.sszuev.jena.ontapi.model.OntClass;
import com.github.sszuev.jena.ontapi.model.OntIndividual;
import com.github.sszuev.jena.ontapi.model.OntModel;
import com.github.sszuev.jena.ontapi.utils.Iterators;
import com.github.sszuev.jena.ontapi.utils.OntModels;
import com.github.sszuev.jena.ontapi.vocabulary.OWL;
import com.github.sszuev.jena.ontapi.vocabulary.RDF;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.util.HashSet;
import java.util.Set;

/**
 * An {@link ObjectsSearcher} that retrieves {@link OWLNamedIndividual OWL-API Named Individual}s.
 * Created by @ssz on 30.06.2020.
 */
public class NamedIndividualSearcher extends EntitySearcher<OWLNamedIndividual> {
    private static final Set<AxiomTranslator<OWLAxiom>> TRANSLATORS = selectTranslators(OWLComponentType.NAMED_INDIVIDUAL);

    @Override
    protected ExtendedIterator<? extends AxiomTranslator<OWLAxiom>> listTranslators() {
        return Iterators.create(TRANSLATORS);
    }

    @Override
    protected ONTObject<OWLNamedIndividual> createEntity(String uri, OntModel model, ONTObjectFactory factory) {
        return factory.getIndividual(OntApiException.mustNotBeNull(model.getIndividual(uri)));
    }

    @Override
    protected ONTObject<OWLNamedIndividual> createEntity(String uri, ModelObjectFactory factory) {
        return factory.getNamedIndividual(uri);
    }

    @Override
    protected Resource getEntityType() {
        return OWL.NamedIndividual;
    }

    @Override
    protected ExtendedIterator<String> listEntities(OntModel m, AxiomsSettings conf) {
        ExtendedIterator<String> res = listIndividuals(m);
        if (!m.independent()) {
            res = Iterators.concat(res, listSharedFromImports(m).filterKeep(x -> containsInOntology(x, m, conf)));
        }
        return res;
    }

    @Override
    protected ExtendedIterator<String> listSharedFromImports(OntModel m) {
        return Iterators.distinct(Iterators.flatMap(OntModels.listImports(m), this::listIndividuals));
    }

    @Override
    protected boolean containsEntity(String uri, OntModel m, AxiomsSettings conf) {
        OntIndividual i = m.getIndividual(uri);
        if (i == null) {
            return false;
        }
        if (m.independent()) {
            return true;
        }
        return containsInOntology(i, m, conf);
    }

    /**
     * Answers an iterator over all model's base named individuals.
     *
     * @param model {@link OntModel}, not {@code null}
     * @return a {@code ExtendedIterator} of URIs
     * @see com.github.owlcs.ontapi.internal.axioms.ClassAssertionTranslator
     * @see OntGraphModelImpl#listIndividuals(OntModel, Set, ExtendedIterator)
     */
    protected ExtendedIterator<String> listIndividuals(OntModel model) {
        Set<Triple> seen = new HashSet<>();
        PersonalityModel p = asPersonalityModel(model);
        Set<Node> system = getSystemResources(model);
        return model.getBaseGraph().find(Node.ANY, RDF.Nodes.type, Node.ANY).mapWith(t -> {
            if (!t.getSubject().isURI()) {
                return null;
            }
            Node type = t.getObject();
            if (OWL.NamedIndividual.asNode().equals(type)) {
                return seen.remove(t) ? null : model.asStatement(t);
            }
            if (system.contains(type)) {
                return null;
            }
            if (seen.remove(t)) {
                return null;
            }
            if (p.findNodeAs(type, OntClass.class) == null) {
                return null;
            }
            return model.asStatement(t);
        }).filterKeep(s -> {
            if (s == null) return false;
            OntIndividual i = s.getSubject().getAs(OntIndividual.class);
            if (i == null) return false;
            Iterators.concat(Iterators.of(OWL.NamedIndividual), OntModels.listClasses(i))
                    .forEachRemaining(x -> {
                        if (s.getObject().equals(x)) {
                            return;
                        }
                        seen.add(Triple.create(i.asNode(), RDF.Nodes.type, x.asNode()));
                    });
            return true;
        }).mapWith(s -> s.getSubject().getURI());
    }
}
