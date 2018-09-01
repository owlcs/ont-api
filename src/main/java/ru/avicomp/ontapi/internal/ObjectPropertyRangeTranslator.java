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

import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import ru.avicomp.ontapi.jena.model.OntCE;
import ru.avicomp.ontapi.jena.model.OntOPE;
import ru.avicomp.ontapi.jena.model.OntStatement;

import java.util.Collection;

/**
 * See {@link AbstractPropertyRangeTranslator}.
 * <p>
 * Created by @szuev on 28.09.2016.
 */
public class ObjectPropertyRangeTranslator extends AbstractPropertyRangeTranslator<OWLObjectPropertyRangeAxiom, OntOPE> {
    @Override
    Class<OntOPE> getView() {
        return OntOPE.class;
    }

    @Override
    protected boolean filter(OntStatement statement, InternalConfig config) {
        return super.filter(statement, config) && statement.getObject().canAs(OntCE.class);
    }

    @Override
    public ONTObject<OWLObjectPropertyRangeAxiom> toAxiom(OntStatement statement, InternalDataFactory reader, InternalConfig config) {
        ONTObject<? extends OWLObjectPropertyExpression> p = reader.get(statement.getSubject(getView()));
        ONTObject<? extends OWLClassExpression> ce = reader.get(statement.getObject().as(OntCE.class));
        Collection<ONTObject<OWLAnnotation>> annotations = reader.get(statement, config);
        OWLObjectPropertyRangeAxiom res = reader.getOWLDataFactory()
                .getOWLObjectPropertyRangeAxiom(p.getObject(), ce.getObject(), ONTObject.extract(annotations));
        return ONTObject.create(res, statement).append(annotations).append(p).append(ce);
    }
}
