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

import org.apache.jena.rdf.model.Resource;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLFunctionalDataPropertyAxiom;
import ru.avicomp.ontapi.internal.InternalConfig;
import ru.avicomp.ontapi.internal.InternalObjectFactory;
import ru.avicomp.ontapi.internal.ONTObject;
import ru.avicomp.ontapi.internal.ONTObjectImpl;
import ru.avicomp.ontapi.jena.model.OntNDP;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.vocabulary.OWL;

import java.util.Collection;

/**
 * example:
 * <pre>{@code
 * foaf:gender rdf:type owl:DatatypeProperty , owl:FunctionalProperty ;
 * }</pre>
 * <p>
 * Created by @szuev on 30.09.2016.
 */
public class FunctionalDataPropertyTranslator extends AbstractPropertyTypeTranslator<OWLFunctionalDataPropertyAxiom, OntNDP> {

    @Override
    Resource getType() {
        return OWL.FunctionalProperty;
    }

    @Override
    Class<OntNDP> getView() {
        return OntNDP.class;
    }

    @Override
    public ONTObject<OWLFunctionalDataPropertyAxiom> toAxiom(OntStatement statement,
                                                             InternalObjectFactory reader,
                                                             InternalConfig config) {
        ONTObject<OWLDataProperty> p = reader.get(getSubject(statement));
        Collection<ONTObject<OWLAnnotation>> annotations = reader.get(statement, config);
        OWLFunctionalDataPropertyAxiom res = reader.getOWLDataFactory()
                .getOWLFunctionalDataPropertyAxiom(p.getObject(), ONTObject.extract(annotations));
        return ONTObjectImpl.create(res, statement).append(annotations).append(p);
    }

}
