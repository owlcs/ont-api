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
 *
 */

package ru.avicomp.ontapi.internal;

import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLIndividual;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

import java.util.Collection;
import java.util.stream.Stream;

/**
 * Creating individual (both named and anonymous):
 * <pre>{@code pizza:France rdf:type owl:NamedIndividual, pizza:Country, owl:Thing.}</pre>
 * <p>
 * Created by @szuev on 28.09.2016.
 */
public class ClassAssertionTranslator extends AxiomTranslator<OWLClassAssertionAxiom> {
    @Override
    public void write(OWLClassAssertionAxiom axiom, OntGraphModel model) {
        OntCE ce = WriteHelper.addClassExpression(model, axiom.getClassExpression());
        OWLIndividual individual = axiom.getIndividual();
        OntObject subject = individual.isAnonymous() ?
                WriteHelper.toResource(individual).inModel(model).as(OntObject.class) :
                WriteHelper.addIndividual(model, individual);
        OntStatement statement = subject.addStatement(RDF.type, ce);
        WriteHelper.addAnnotations(statement, axiom.annotations());
    }

    @Override
    public Stream<OntStatement> statements(OntGraphModel model) {
        return model.statements(null, RDF.type, null)
                .filter(OntStatement::isLocal)
                .filter(s -> s.getObject().canAs(OntCE.class))
                .filter(s -> s.getSubject().canAs(OntIndividual.class));
    }

    @Override
    public boolean testStatement(OntStatement statement) {
        return statement.getPredicate().equals(RDF.type)
                && statement.getObject().canAs(OntCE.class)
                && statement.getSubject().canAs(OntIndividual.class);
    }

    @Override
    public InternalObject<OWLClassAssertionAxiom> toAxiom(OntStatement statement) {
        ConfigProvider.Config conf = getConfig(statement);
        InternalObject<? extends OWLIndividual> i = ReadHelper.fetchIndividual(statement.getSubject().as(OntIndividual.class), conf.dataFactory());
        InternalObject<? extends OWLClassExpression> ce = ReadHelper.fetchClassExpression(statement.getObject().as(OntCE.class), conf.dataFactory());
        Collection<InternalObject<OWLAnnotation>> annotations = getAnnotations(statement, conf);
        OWLClassAssertionAxiom res = conf.dataFactory().getOWLClassAssertionAxiom(ce.getObject(), i.getObject(), InternalObject.extract(annotations));
        return InternalObject.create(res, statement).append(annotations).append(i).append(ce);
    }
}
