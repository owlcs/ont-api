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

import com.github.owlcs.ontapi.OntApiException;
import com.github.owlcs.ontapi.config.AxiomsSettings;
import com.github.owlcs.ontapi.internal.*;
import com.github.owlcs.ontapi.internal.searchers.ForDatatype;
import com.github.owlcs.ontapi.jena.model.OntClass;
import com.github.owlcs.ontapi.jena.model.OntDataRange;
import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.jena.model.OntStatement;
import com.github.owlcs.ontapi.jena.utils.Iter;
import com.github.owlcs.ontapi.jena.vocabulary.RDF;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDatatype;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An {@link ObjectsSearcher} that retrieves {@link OWLDatatype OWL-API Datatype}s.
 * Created by @ssz on 31.08.2020.
 */
public class DatatypeSearcher extends WithCardinality<OWLDatatype> implements ForDatatype {
    /**
     * All translators, since any axiom can be annotated and therefore contains an literal with datatype inside
     */
    private static final Set<AxiomTranslator<OWLAxiom>> TRANSLATORS = selectTranslators(null);

    private static OntDataRange.Named getDatatype(Statement statement, OntModel model) {
        Resource s = statement.getSubject();
        RDFNode o = statement.getObject();
        if (s.isURIResource() && RDF.type.equals(statement.getPredicate()) && RDFS.Datatype.equals(o)) {
            return model.getDatatype(s.getURI());
        }
        if (o.isURIResource()) {
            return model.getDatatype(o.asResource().getURI());
        }
        if (!o.isLiteral()) {
            return null;
        }
        return getDatatype(o, model);
    }

    private static OntDataRange.Named getDatatype(RDFNode node, OntModel model) {
        if (!node.isLiteral()) return null;
        return model.getDatatype(node.asLiteral());
    }

    @Override
    protected Set<Node> getBuiltinsSpec(OntModel m) {
        return getBuiltinsVocabulary(m).getDatatypes();
    }

    @Override
    protected ExtendedIterator<? extends AxiomTranslator<OWLAxiom>> listTranslators() {
        return Iter.create(TRANSLATORS);
    }

    @Override
    protected Resource getEntityType() {
        return RDFS.Datatype;
    }

    @Override
    protected ONTObject<OWLDatatype> createEntity(String uri, OntModel model, ONTObjectFactory factory) {
        return factory.getDatatype(OntApiException.mustNotBeNull(model.getDatatype(uri)));
    }

    @Override
    protected ONTObject<OWLDatatype> createEntity(String uri, ModelObjectFactory factory) {
        return factory.getDatatype(uri);
    }

    @Override
    protected ExtendedIterator<String> listEntities(OntModel m, AxiomsSettings conf) {
        Set<String> res = new HashSet<>();
        addToSet(res, m, conf);
        return Iter.create(res);
    }

    private void addToSet(Set<String> res, OntModel model, AxiomsSettings conf) {
        Set<OntStatement> header = listHeaderAnnotations(model).toSet();
        // direct:
        listStatements(model).forEachRemaining(s -> addToSet(res, s, header, model, conf));
        // shared from imports:
        if (!model.independent()) {
            Set<String> fromHeader = header.stream().map(s -> getDatatype(s.getObject(), model)).filter(Objects::nonNull)
                    .map(Resource::getURI).collect(Collectors.toSet());
            listSharedFromImports(model)
                    .filterDrop(res::contains)
                    .filterKeep(s -> fromHeader.contains(s) || containsInAxiom(toResource(model, s), model, conf))
                    .forEachRemaining(res::add);
        }
        // special case of RDFS:Literal
        addTopEntity(res, model, conf);
    }

    private void addToSet(Set<String> res,
                          OntStatement statement,
                          Set<? extends Statement> header,
                          OntModel model,
                          AxiomsSettings conf) {
        OntDataRange.Named d = getDatatype(statement, model);
        if (d == null) {
            return;
        }
        String uri = d.getURI();
        if (res.contains(uri)) { // already processed
            return;
        }
        Class<? extends OntClass.RestrictionCE<?>> type = ForDatatype.getSpecialDataRestrictionType(uri);
        if (type != null && statement.getSubject().canAs(type)) {
            return;
        }
        if (isInOntology(statement, header, model, conf)) {
            res.add(uri);
        }
    }

    @Override
    protected boolean containsInOntology(Resource candidate, OntModel model, AxiomsSettings conf) {
        return containsInOntology(candidate.getURI(), model, conf);
    }

    @Override
    protected boolean containsInOntology(String uri, OntModel model, AxiomsSettings conf) {
        Set<OntStatement> header = listHeaderAnnotations(model).toSet();
        Class<? extends OntClass.RestrictionCE<?>> type = ForDatatype.getSpecialDataRestrictionType(uri);
        return Iter.anyMatch(listStatements(model), s -> {
            if (s.getObject().isURIResource() && uri.equals(s.getResource().getURI())) {
                return isInAxiom(s, model, conf);
            }
            if (!s.getObject().isLiteral())
                return false;
            if (!uri.equals(s.getLiteral().getDatatypeURI()))
                return false;
            if (type != null && s.getSubject().canAs(type))
                return false;
            return isInOntology(s, header, model, conf);
        });
    }

    private boolean isInOntology(OntStatement statement,
                                 Set<? extends Statement> header,
                                 OntModel model,
                                 AxiomsSettings conf) {
        // in header
        if (header.contains(statement)) {
            return true;
        }
        // in axiom
        return isInAxiom(statement, model, conf);
    }

    private boolean isInAxiom(OntStatement statement, OntModel model, AxiomsSettings conf) {
        return containsAxiom(Iter.create(getRootStatements(model, statement)), conf);
    }
}
