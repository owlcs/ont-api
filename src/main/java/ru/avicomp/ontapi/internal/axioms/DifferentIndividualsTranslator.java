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

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.semanticweb.owlapi.model.OWLDifferentIndividualsAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;
import ru.avicomp.ontapi.internal.InternalConfig;
import ru.avicomp.ontapi.internal.InternalObjectFactory;
import ru.avicomp.ontapi.internal.ONTObject;
import ru.avicomp.ontapi.jena.model.OntDisjoint;
import ru.avicomp.ontapi.jena.model.OntIndividual;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.vocabulary.OWL;

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
    public ONTObject<OWLDifferentIndividualsAxiom> toAxiom(OntStatement statement,
                                                           InternalObjectFactory reader,
                                                           InternalConfig config) {
        return makeAxiom(statement, reader.getAnnotations(statement, config),
                reader::getIndividual,
                (members, annotations) -> reader.getOWLDataFactory()
                        .getOWLDifferentIndividualsAxiom(ONTObject.toSet(members), ONTObject.toSet(annotations)));
    }
}
