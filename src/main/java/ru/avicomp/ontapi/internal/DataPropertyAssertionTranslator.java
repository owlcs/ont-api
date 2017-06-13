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

import java.util.stream.Stream;

import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntIndividual;
import ru.avicomp.ontapi.jena.model.OntNDP;
import ru.avicomp.ontapi.jena.model.OntStatement;

/**
 * property that belongs to individual.
 * individual could be anonymous!
 * <p>
 * Created by @szuev on 28.09.2016.
 */
public class DataPropertyAssertionTranslator extends AxiomTranslator<OWLDataPropertyAssertionAxiom> {
    @Override
    public void write(OWLDataPropertyAssertionAxiom axiom, OntGraphModel model) {
        WriteHelper.writeAssertionTriple(model, axiom.getSubject(), axiom.getProperty(), axiom.getObject(), axiom.annotations());
    }

    /**
     * positive data property assertion: the rule "a R v":
     * see <a href='https://www.w3.org/TR/owl2-quick-reference/'>Assertions</a>
     *
     * @param model {@link OntGraphModel} the model
     * @return Stream of {@link OntStatement}
     */
    @Override
    public Stream<OntStatement> statements(OntGraphModel model) {
        return model.statements()
                .filter(OntStatement::isLocal)
                .filter(OntStatement::isData)
                .filter(s -> s.getObject().isLiteral())
                .filter(s -> s.getSubject().canAs(OntIndividual.class));
    }

    @Override
    public boolean testStatement(OntStatement statement) {
        return statement.isData()
                && statement.getObject().isLiteral()
                && statement.getSubject().canAs(OntIndividual.class);
    }

    @Override
    public InternalObject<OWLDataPropertyAssertionAxiom> asAxiom(OntStatement statement) {
        ConfigProvider.Config conf = getConfig(statement);
        InternalObject<? extends OWLIndividual> i = ReadHelper.fetchIndividual(statement.getSubject().as(OntIndividual.class), conf.dataFactory());
        InternalObject<OWLDataProperty> p = ReadHelper.fetchDataProperty(statement.getPredicate().as(OntNDP.class), conf.dataFactory());
        InternalObject<OWLLiteral> l = ReadHelper.getLiteral(statement.getObject().asLiteral(), conf.dataFactory());
        InternalObject.Collection<OWLAnnotation> annotations = ReadHelper.getStatementAnnotations(statement, conf.dataFactory(), conf.loaderConfig());
        OWLDataPropertyAssertionAxiom res = conf.dataFactory().getOWLDataPropertyAssertionAxiom(p.getObject(), i.getObject(), l.getObject(),
                annotations.getObjects());
        return InternalObject.create(res, statement).add(annotations.getTriples()).append(i).append(p).append(l);
    }
}
