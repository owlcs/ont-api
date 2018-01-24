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

import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;

import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntIndividual;
import ru.avicomp.ontapi.jena.model.OntOPE;
import ru.avicomp.ontapi.jena.model.OntStatement;

/**
 * example:
 * <pre>{@code
 * gr:Saturday rdf:type owl:NamedIndividual , gr:hasNext gr:Sunday ;
 * }</pre>
 * Created by @szuev on 01.10.2016.
 */
public class ObjectPropertyAssertionTranslator extends AxiomTranslator<OWLObjectPropertyAssertionAxiom> {

    /**
     * Note: ObjectPropertyAssertion(ObjectInverseOf(P) S O) = ObjectPropertyAssertion(P O S)
     *
     * @param axiom {@link OWLObjectPropertyAssertionAxiom}
     * @param model {@link OntGraphModel}
     */
    @Override
    public void write(OWLObjectPropertyAssertionAxiom axiom, OntGraphModel model) {
        OWLObjectPropertyExpression property = axiom.getProperty().isAnonymous() ? axiom.getProperty().getInverseProperty() : axiom.getProperty();
        OWLIndividual subject = axiom.getProperty().isAnonymous() ? axiom.getObject() : axiom.getSubject();
        OWLIndividual object = axiom.getProperty().isAnonymous() ? axiom.getSubject() : axiom.getObject();
        WriteHelper.writeAssertionTriple(model, subject, property, object, axiom.annotations());
    }

    /**
     * positive object property assertion: "a1 PN a2"
     * see <a href='https://www.w3.org/TR/owl2-quick-reference/'>Assertions</a>
     *
     * @param model {@link OntGraphModel} the model
     * @return Stream of {@link OntStatement}
     */
    @Override
    public Stream<OntStatement> statements(OntGraphModel model) {
        return model.statements()
                .filter(OntStatement::isLocal)
                .filter(OntStatement::isObject)
                .filter(s -> s.getSubject().canAs(OntIndividual.class))
                .filter(s -> s.getObject().canAs(OntIndividual.class));
    }

    @Override
    public boolean testStatement(OntStatement statement) {
        return statement.isObject()
                && statement.getSubject().canAs(OntIndividual.class)
                && statement.getObject().canAs(OntIndividual.class);
    }

    @Override
    public InternalObject<OWLObjectPropertyAssertionAxiom> asAxiom(OntStatement statement) {
        ConfigProvider.Config conf = getConfig(statement);
        InternalObject<? extends OWLIndividual> subject = ReadHelper.fetchIndividual(statement.getSubject().as(OntIndividual.class), conf.dataFactory());
        InternalObject<? extends OWLObjectPropertyExpression> property = ReadHelper.fetchObjectPropertyExpression(statement.getPredicate().as(OntOPE.class), conf.dataFactory());
        InternalObject<? extends OWLIndividual> object = ReadHelper.fetchIndividual(statement.getObject().as(OntIndividual.class), conf.dataFactory());
        InternalObject.Collection<OWLAnnotation> annotations = ReadHelper.getStatementAnnotations(statement, conf.dataFactory(), conf.loaderConfig());
        OWLObjectPropertyAssertionAxiom res = conf.dataFactory().getOWLObjectPropertyAssertionAxiom(property.getObject(), subject.getObject(), object.getObject(),
                annotations.getObjects());
        return InternalObject.create(res, statement).add(annotations.getTriples()).append(subject).append(property).append(object);
    }
}
