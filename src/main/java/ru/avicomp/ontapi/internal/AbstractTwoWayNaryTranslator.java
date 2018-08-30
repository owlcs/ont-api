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
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.jena.model.OntDisjoint;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntObject;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.utils.Models;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

import java.util.Collection;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This is a base for the following translators for axioms with two or more than two entities:
 * <ul>
 * <li>DisjointClasses ({@link DisjointClassesTranslator})</li>
 * <li>DisjointObjectProperties ({@link DisjointObjectPropertiesTranslator})</li>
 * <li>DisjointDataProperties ({@link DisjointDataPropertiesTranslator}),</li>
 * <li>DifferentIndividuals ({@link DifferentIndividualsTranslator})</li>
 * </ul>
 * Each of these axioms could be written in two ways: as single triple (or sequence of single triples) or as special anonymous node with rdf:List inside.
 * <p>
 * Created by szuev on 12.10.2016.
 *
 * @param <Axiom> generic type of {@link OWLAxiom}
 * @param <OWL>   generic type of {@link OWLObject}
 * @param <ONT>   generic type of {@link OntObject}
 */
public abstract class AbstractTwoWayNaryTranslator<Axiom extends OWLAxiom & OWLNaryAxiom<OWL>, OWL extends OWLObject & IsAnonymous, ONT extends OntObject> extends AbstractNaryTranslator<Axiom, OWL, ONT> {
    @Override
    public void write(Axiom axiom, OntGraphModel model) {
        Set<OWL> operands = axiom.operands().collect(Collectors.toSet());
        Set<OWLAnnotation> annotations = axiom.annotations().collect(Collectors.toSet());
        if (operands.isEmpty() && annotations.isEmpty()) { // nothing to write, skip
            return;
        }
        if (operands.size() == 2) { // single triple classic way
            write(axiom, annotations, model);
        } else { // OWL2 anonymous node
            Resource root = model.createResource();
            model.add(root, RDF.type, getMembersType());
            model.add(root, getMembersPredicate(), WriteHelper.addRDFList(model, operands.stream()));
            OntDisjoint<ONT> res = root.as(getDisjointView());
            WriteHelper.addAnnotations(res, annotations.stream());
        }
    }

    @Override
    public ExtendedIterator<OntStatement> listStatements(OntGraphModel model, ConfigProvider.Config config) {
        return super.listStatements(model, config)
                .andThen(Models.listOntObjects(model, getDisjointView()).mapWith(OntObject::getRoot));
    }

    @Override
    public boolean testStatement(OntStatement statement, ConfigProvider.Config config) {
        return super.testStatement(statement, config) || statement.getSubject().canAs(getDisjointView());
    }

    abstract Resource getMembersType();

    abstract Property getMembersPredicate();

    abstract Class<? extends OntDisjoint<ONT>> getDisjointView();

    ONTObject<Axiom> makeAxiom(OntStatement statement,
                               Collection<ONTObject<OWLAnnotation>> annotations,
                               Function<ONT, ONTObject<? extends OWL>> membersExtractor,
                               BiFunction<Collection<ONTObject<? extends OWL>>,
                                       Collection<ONTObject<OWLAnnotation>>, Axiom> creator) {
        Collection<ONTObject<? extends OWL>> members;
        Resource subject = statement.getSubject();
        OntDisjoint<ONT> disjoint = null;
        if (subject.canAs(getDisjointView())) {
            disjoint = subject.as(getDisjointView());
            members = disjoint.members().map(membersExtractor).collect(Collectors.toSet());
        } else {
            members = Stream.of(subject, statement.getObject()).map(r -> r.as(getView()))
                    .map(membersExtractor).collect(Collectors.toSet());
        }
        Axiom axiom = creator.apply(members, annotations);
        return (disjoint != null ? ONTObject.create(axiom, disjoint) : ONTObject.create(axiom, statement))
                .append(annotations).appendWildcards(members);

    }
}
