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

package com.github.owlcs.ontapi.internal.axioms;

import com.github.owlcs.ontapi.config.AxiomsSettings;
import com.github.owlcs.ontapi.internal.ONTObject;
import com.github.owlcs.ontapi.internal.ONTWrapperImpl;
import com.github.owlcs.ontapi.internal.WriteHelper;
import com.github.owlcs.ontapi.jena.model.OntDisjoint;
import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.jena.model.OntObject;
import com.github.owlcs.ontapi.jena.model.OntStatement;
import com.github.owlcs.ontapi.jena.utils.OntModels;
import com.github.owlcs.ontapi.jena.vocabulary.RDF;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.*;

import java.util.Collection;
import java.util.List;
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
public abstract class AbstractTwoWayNaryTranslator<Axiom extends OWLAxiom & OWLNaryAxiom<OWL>,
        OWL extends OWLObject & IsAnonymous, ONT extends OntObject> extends AbstractNaryTranslator<Axiom, OWL, ONT> {

    @Override
    public void write(Axiom axiom, OntModel model) {
        List<OWL> operands = axiom.getOperandsAsList();
        List<OWLAnnotation> annotations = axiom.annotationsAsList();
        if (operands.isEmpty() && annotations.isEmpty()) { // nothing to write, skip
            LOGGER.warn("Nothing to write, wrong axiom is given: {}", axiom);
            return;
        }
        if (operands.size() == 2) { // single triple - use classic way
            write(axiom, annotations, model);
        } else { // OWL2 anonymous node
            Resource root = model.createResource();
            model.add(root, RDF.type, getMembersType());
            model.add(root, getMembersPredicate(), WriteHelper.addRDFList(model, operands));
            OntDisjoint<ONT> res = root.as(getDisjointView());
            WriteHelper.addAnnotations(res, annotations);
        }
    }

    @Override
    public ExtendedIterator<OntStatement> listStatements(OntModel model, AxiomsSettings config) {
        return super.listStatements(model, config).andThen(listDisjointStatements(model));
    }

    private ExtendedIterator<OntStatement> listDisjointStatements(OntModel model) {
        return OntModels.listLocalObjects(model, getDisjointView()).mapWith(OntObject::getMainStatement);
    }

    @Override
    public boolean testStatement(OntStatement statement, AxiomsSettings config) {
        return super.testStatement(statement, config)
                || (RDF.type.equals(statement.getPredicate()) && statement.getSubject().canAs(getDisjointView()));
    }

    abstract Resource getMembersType();

    abstract Property getMembersPredicate();

    abstract Class<? extends OntDisjoint<ONT>> getDisjointView();

    @Override
    protected ExtendedIterator<OntStatement> listSearchStatements(Axiom key, OntModel model, AxiomsSettings config) {
        Collection<Triple> search = getSearchTriples(key);
        if (search.isEmpty()) {
            return listStatements(model, config);
        }
        return listSearchStatements(model, search).andThen(listDisjointStatements(model));
    }

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
        return (disjoint != null ? ONTWrapperImpl.create(axiom, disjoint) : ONTWrapperImpl.create(axiom, statement))
                .append(annotations).append(members);
    }

}
