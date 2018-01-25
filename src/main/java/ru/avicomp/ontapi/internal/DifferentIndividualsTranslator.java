/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2017, Avicomp Services, AO
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
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLDifferentIndividualsAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;
import ru.avicomp.ontapi.jena.model.OntDisjoint;
import ru.avicomp.ontapi.jena.model.OntIndividual;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import uk.ac.manchester.cs.owl.owlapi.OWLDifferentIndividualsAxiomImpl;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * see {@link AbstractTwoWayNaryTranslator}
 * Syntax:
 * <ul>
 * <li>{@code a1 owl:differentFrom a2.}</li>
 * <li><pre>{@code _:x rdf:type owl:AllDifferent.
 * _:x owl:members (a1 ... an). }</pre></li>
 * </ul>
 * <p>
 * Created by @szuev on 29.09.2016.
 *
 * @see OWLDifferentIndividualsAxiom
 */
public class DifferentIndividualsTranslator extends AbstractTwoWayNaryTranslator<OWLDifferentIndividualsAxiom, OWLIndividual, OntIndividual> {
    @Override
    Property getPredicate() {
        return OWL.differentFrom;
    }

    @Override
    Class<OntIndividual> getView() {
        return OntIndividual.class;
    }

    @Override
    OWLDifferentIndividualsAxiom create(Stream<OWLIndividual> components, Set<OWLAnnotation> annotations) {
        return new OWLDifferentIndividualsAxiomImpl(components.collect(Collectors.toSet()), annotations);
    }

    @Override
    Resource getMembersType() {
        return OWL.AllDifferent;
    }

    @Override
    Property getMembersPredicate() {
        return OWL.distinctMembers;
    }

    @Override
    Class<OntDisjoint.Individuals> getDisjointView() {
        return OntDisjoint.Individuals.class;
    }

    @Override
    public InternalObject<OWLDifferentIndividualsAxiom> asAxiom(OntStatement statement) {
        ConfigProvider.Config conf = getConfig(statement);
        InternalObject.Collection<? extends OWLIndividual> members;
        Stream<OntStatement> content;
        if (statement.getSubject().canAs(getDisjointView())) {
            OntDisjoint.Individuals disjoint = statement.getSubject().as(getDisjointView());
            content = disjoint.content();
            members = InternalObject.Collection.create(disjoint.members().map(m -> ReadHelper.fetchIndividual(m, conf.dataFactory())));
        } else {
            content = Stream.of(statement);
            members = InternalObject.Collection.create(Stream.of(statement.getSubject(), statement.getObject())
                    .map(r -> r.as(getView())).map(m -> ReadHelper.fetchIndividual(m, conf.dataFactory())));
        }
        InternalObject.Collection<OWLAnnotation> annotations = ReadHelper.getStatementAnnotations(statement, conf.dataFactory(), conf.loaderConfig());
        OWLDifferentIndividualsAxiom res = conf.dataFactory().getOWLDifferentIndividualsAxiom(members.getObjects(), annotations.getObjects());
        return InternalObject.create(res, content).add(annotations.getTriples()).add(members.getTriples());
    }
}
