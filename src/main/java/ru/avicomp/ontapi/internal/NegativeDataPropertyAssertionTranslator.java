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

import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntNPA;
import ru.avicomp.ontapi.jena.model.OntStatement;

import java.util.Collection;

/**
 * example:
 * <pre>{@code
 * [ a owl:NegativePropertyAssertion; owl:sourceIndividual :ind1; owl:assertionProperty :dataProp; owl:targetValue "TEST"^^xsd:string ]
 * }</pre>
 * Created by szuev on 12.10.2016.
 */
public class NegativeDataPropertyAssertionTranslator extends AbstractNegativePropertyAssertionTranslator<OWLNegativeDataPropertyAssertionAxiom, OntNPA.DataAssertion> {
    @Override
    OntNPA.DataAssertion createNPA(OWLNegativeDataPropertyAssertionAxiom axiom, OntGraphModel model) {
        return WriteHelper.addDataProperty(model, axiom.getProperty())
                .addNegativeAssertion(WriteHelper.addIndividual(model, axiom.getSubject()), WriteHelper.addLiteral(model, axiom.getObject()));
    }

    @Override
    Class<OntNPA.DataAssertion> getView() {
        return OntNPA.DataAssertion.class;
    }

    @Override
    public InternalObject<OWLNegativeDataPropertyAssertionAxiom> toAxiom(OntStatement statement) {
        ConfigProvider.Config conf = getConfig(statement);
        OntNPA.DataAssertion npa = statement.getSubject().as(getView());
        InternalObject<? extends OWLIndividual> s = ReadHelper.fetchIndividual(npa.getSource(), conf.dataFactory());
        InternalObject<OWLDataProperty> p = ReadHelper.fetchDataProperty(npa.getProperty(), conf.dataFactory());
        InternalObject<OWLLiteral> o = ReadHelper.getLiteral(npa.getTarget(), conf.dataFactory());
        Collection<InternalObject<OWLAnnotation>> annotations = getAnnotations(statement, conf);
        OWLNegativeDataPropertyAssertionAxiom res = conf.dataFactory().getOWLNegativeDataPropertyAssertionAxiom(p.getObject(),
                s.getObject(), o.getObject(), InternalObject.extract(annotations));
        return InternalObject.create(res, npa).append(annotations).append(s).append(p).append(o);
    }
}
