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

import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import ru.avicomp.ontapi.jena.model.OntCE;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntStatement;

import java.util.Collection;
import java.util.stream.Stream;

/**
 * Examples:
 * <pre>{@code
 * pizza:JalapenoPepperTopping rdfs:subClassOf pizza:PepperTopping.
 * pizza:JalapenoPepperTopping rdfs:subClassOf [ a owl:Restriction; owl:onProperty pizza:hasSpiciness; owl:someValuesFrom pizza:Hot].
 * }</pre>
 * <p>
 * Created by @szuev on 28.09.2016.
 */
public class SubClassOfTranslator extends AxiomTranslator<OWLSubClassOfAxiom> {
    @Override
    public void write(OWLSubClassOfAxiom axiom, OntGraphModel model) {
        WriteHelper.writeTriple(model, axiom.getSubClass(), RDFS.subClassOf, axiom.getSuperClass(), axiom.annotations());
    }

    @Override
    public Stream<OntStatement> statements(OntGraphModel model) {
        return model.localStatements(null, RDFS.subClassOf, null)
                .filter(s -> s.getSubject().canAs(OntCE.class))
                .filter(s -> s.getObject().canAs(OntCE.class));
    }

    @Override
    public boolean testStatement(OntStatement statement) {
        return statement.getPredicate().equals(RDFS.subClassOf)
                && statement.getSubject().canAs(OntCE.class)
                && statement.getObject().canAs(OntCE.class);
    }

    @Override
    public InternalObject<OWLSubClassOfAxiom> toAxiom(OntStatement statement) {
        InternalDataFactory reader = getDataFactory(statement.getModel());
        InternalObject<? extends OWLClassExpression> sub = reader.get(statement.getSubject().as(OntCE.class));
        InternalObject<? extends OWLClassExpression> sup = reader.get(statement.getObject().as(OntCE.class));
        Collection<InternalObject<OWLAnnotation>> annotations = reader.get(statement);
        OWLSubClassOfAxiom res = reader.getOWLDataFactory()
                .getOWLSubClassOfAxiom(sub.getObject(), sup.getObject(), InternalObject.extract(annotations));
        return InternalObject.create(res, statement).append(annotations).append(sub).append(sup);
    }
}
