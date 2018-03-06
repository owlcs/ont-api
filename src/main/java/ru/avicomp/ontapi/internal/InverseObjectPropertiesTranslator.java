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
import org.semanticweb.owlapi.model.OWLInverseObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntOPE;
import ru.avicomp.ontapi.jena.model.OntObject;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.vocabulary.OWL;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * example:
 * <pre>{@code
 * pizza:hasBase owl:inverseOf pizza:isBaseOf ;
 * }</pre>
 * <p>
 * Created by @szuev on 30.09.2016.
 */
class InverseObjectPropertiesTranslator extends AxiomTranslator<OWLInverseObjectPropertiesAxiom> {
    @Override
    public void write(OWLInverseObjectPropertiesAxiom axiom, OntGraphModel model) {
        WriteHelper.writeTriple(model, axiom.getFirstProperty(), OWL.inverseOf, axiom.getSecondProperty(), axiom.annotations());
    }

    @Override
    public Stream<OntStatement> statements(OntGraphModel model) {
        // NOTE as a precaution: the first (commented) way is not correct
        // since it includes anonymous object property expressions (based on owl:inverseOf),
        // which could be treat as separated axioms, but OWL-API doesn't think so.
        /*return model.statements(null, OWL.inverseOf, null)
                .filter(OntStatement::isLocal)
                .filter(s -> s.getSubject().canAs(OntOPE.class))
                .filter(s -> s.getObject().canAs(OntOPE.class));*/
        return model.ontObjects(OntOPE.class)
                .map(subj -> subj.inverseOf().map(obj -> subj.statement(OWL.inverseOf, obj)))
                .flatMap(Function.identity())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(OntStatement::isLocal);
    }

    @Override
    public boolean testStatement(OntStatement statement) {
        if (!statement.getPredicate().equals(OWL.inverseOf) || !statement.getObject().isResource()) return false;
        OntObject subject = statement.getSubject();
        OntObject object = statement.getObject().as(OntObject.class);
        // to not take into account the object property expressions:
        return (subject.isURIResource() || subject.hasType(OWL.ObjectProperty))
                && (object.isURIResource() || object.hasType(OWL.ObjectProperty));
    }

    @Override
    public InternalObject<OWLInverseObjectPropertiesAxiom> toAxiom(OntStatement statement) {
        ConfigProvider.Config conf = getConfig(statement);
        InternalObject<? extends OWLObjectPropertyExpression> f = ReadHelper.fetchObjectPropertyExpression(statement.getSubject().as(OntOPE.class), conf.dataFactory());
        InternalObject<? extends OWLObjectPropertyExpression> s = ReadHelper.fetchObjectPropertyExpression(statement.getObject().as(OntOPE.class), conf.dataFactory());
        Collection<InternalObject<OWLAnnotation>> annotations = getAnnotations(statement, conf);
        OWLInverseObjectPropertiesAxiom res = conf.dataFactory().getOWLInverseObjectPropertiesAxiom(f.getObject(), s.getObject(), InternalObject.extract(annotations));
        return InternalObject.create(res, statement).append(annotations).append(f).append(s);
    }
}
