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

import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntNAP;
import ru.avicomp.ontapi.jena.model.OntStatement;

import java.util.Collection;
import java.util.stream.Stream;

/**
 * Examples:
 * <pre>{@code
 *  foaf:LabelProperty vs:term_status "unstable" .
 *  foaf:LabelProperty rdfs:isDefinedBy <http://xmlns.com/foaf/0.1/> .
 *  pizza:UnclosedPizza rdfs:label "PizzaAberta"@pt .
 * }</pre>
 * <p>
 * Created by @szuev on 28.09.2016.
 */
public class AnnotationAssertionTranslator extends AxiomTranslator<OWLAnnotationAssertionAxiom> {
    @Override
    public void write(OWLAnnotationAssertionAxiom axiom, OntGraphModel model) {
        WriteHelper.writeAssertionTriple(model, axiom.getSubject(), axiom.getProperty(), axiom.getValue(), axiom.annotations());
    }

    /**
     * Annotation assertion: the rule "s A t":
     * See <a href='https://www.w3.org/TR/owl2-quick-reference/'>Annotations</a>
     * Currently there is following default behaviour:
     * if the annotation value has its own annotations then the specified statement is skipped from consideration
     * but comes as annotation of some other axiom.
     * Also it is skipped if load annotations is disabled in configuration.
     *
     * @param model {@link OntGraphModel} the model
     * @return Stream of {@link OntStatement}
     */
    @Override
    public Stream<OntStatement> statements(OntGraphModel model) {
        if (!getConfig(model).loaderConfig().isLoadAnnotationAxioms()) return Stream.empty();
        return model.localStatements()
                .filter(this::testStatement);
    }

    @Override
    public boolean testStatement(OntStatement statement) {
        return ReadHelper.isAnnotationAssertionStatement(statement, getConfig(statement.getModel()).loaderConfig()) &&
                ReadHelper.isEntityOrAnonymousIndividual(statement.getSubject());
    }

    @Override
    public InternalObject<OWLAnnotationAssertionAxiom> toAxiom(OntStatement statement) {
        InternalDataFactory reader = getDataFactory(statement.getModel());
        InternalObject<? extends OWLAnnotationSubject> s = reader.get(statement.getSubject());
        InternalObject<OWLAnnotationProperty> p = reader.get(statement.getPredicate().as(OntNAP.class));
        InternalObject<? extends OWLAnnotationValue> v = reader.get(statement.getObject());
        Collection<InternalObject<OWLAnnotation>> annotations = reader.get(statement);
        OWLAnnotationAssertionAxiom res = reader.getOWLDataFactory()
                .getOWLAnnotationAssertionAxiom(p.getObject(), s.getObject(), v.getObject(),
                InternalObject.extract(annotations));
        return InternalObject.create(res, statement).append(annotations).append(s).append(p).append(v);
    }

}
