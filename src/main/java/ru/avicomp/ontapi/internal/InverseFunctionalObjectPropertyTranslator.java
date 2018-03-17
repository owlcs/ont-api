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

import org.apache.jena.rdf.model.Resource;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLInverseFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import ru.avicomp.ontapi.jena.model.OntOPE;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.vocabulary.OWL;

import java.util.Collection;

/**
 * example:
 * <pre>{@code
 * pizza:hasBase rdf:type owl:InverseFunctionalProperty
 * }</pre>
 * <p>
 * Created by @szuev on 28.09.2016.
 */
public class InverseFunctionalObjectPropertyTranslator extends AbstractPropertyTypeTranslator<OWLInverseFunctionalObjectPropertyAxiom, OntOPE> {
    @Override
    Resource getType() {
        return OWL.InverseFunctionalProperty;
    }

    @Override
    Class<OntOPE> getView() {
        return OntOPE.class;
    }

    @Override
    public InternalObject<OWLInverseFunctionalObjectPropertyAxiom> toAxiom(OntStatement statement) {
        InternalDataFactory reader = getDataFactory(statement.getModel());
        InternalObject<? extends OWLObjectPropertyExpression> p = reader.get(getSubject(statement));
        Collection<InternalObject<OWLAnnotation>> annotations = reader.get(statement);
        OWLInverseFunctionalObjectPropertyAxiom res = reader.getOWLDataFactory()
                .getOWLInverseFunctionalObjectPropertyAxiom(p.getObject(), InternalObject.extract(annotations));
        return InternalObject.create(res, statement).append(annotations).append(p);
    }
}
