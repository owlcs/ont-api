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

package ru.avicomp.ontapi.internal.axioms;

import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.util.iterator.NullIterator;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import ru.avicomp.ontapi.internal.*;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.impl.Entities;
import ru.avicomp.ontapi.jena.model.OntEntity;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntObject;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.utils.Models;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

import java.util.Collection;
import java.util.Objects;

/**
 * It is a translator for axioms of the {@link org.semanticweb.owlapi.model.AxiomType#DECLARATION} type.
 * Each non-builtin {@link OWLEntity entity} must have a declaration.
 * The entity declaration is a simple triplet with {@code rdf:type} predicate,
 * in OWL2 the subject and object of that triple are IRIs.
 * <p>
 * Created by @szuev on 28.09.2016.
 */
public class DeclarationTranslator extends AxiomTranslator<OWLDeclarationAxiom> {
    @Override
    public void write(OWLDeclarationAxiom axiom, OntGraphModel model) {
        WriteHelper.writeDeclarationTriple(model, axiom.getEntity(), RDF.type,
                WriteHelper.getRDFType(axiom.getEntity()), axiom.annotations());
    }

    @Override
    public ExtendedIterator<OntStatement> listStatements(OntGraphModel model, InternalConfig config) {
        if (!config.isAllowReadDeclarations()) return NullIterator.instance();
        // this way is used for two reasons:
        // 1) performance (union of several find operation for the pattern [ANY,rdf:type,Resource] is faster
        // then single find operation [ANY,rdf:type,ANY] and subsequent filter)
        // 2) to filter out punnings using standard entity factories
        return Models.listEntities(model).mapWith(OntObject::getRoot).filterDrop(Objects::isNull);
    }

    @Override
    public boolean testStatement(OntStatement statement, InternalConfig config) {
        if (!statement.getSubject().isURIResource()) return false;
        if (!statement.getObject().isURIResource()) return false;
        if (!statement.isDeclaration()) return false;
        // again. this way is used to restrict illegal punnings
        return Entities.find(statement.getResource())
                .map(Entities::getActualType)
                .map(t -> statement.getModel().getOntEntity(t, statement.getSubject()))
                .isPresent();
    }

    @Override
    public ONTObject<OWLDeclarationAxiom> toAxiom(OntStatement statement, InternalObjectFactory reader, InternalConfig config) {
        OntEntity e = Entities.find(statement.getResource())
                .map(Entities::getActualType)
                .map(t -> statement.getModel().getOntEntity(t, statement.getSubject()))
                .orElseThrow(() -> new OntJenaException.IllegalArgument("Can't find entity by the statement " + statement));
        ONTObject<? extends OWLEntity> entity = reader.get(e);
        Collection<ONTObject<OWLAnnotation>> annotations = reader.get(statement, config);
        OWLDeclarationAxiom res = reader.getOWLDataFactory().getOWLDeclarationAxiom(entity.getObject(), ONTObject.extract(annotations));
        return ONTObject.create(res, statement).append(annotations);
    }
}
