/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2019, The University of Manchester, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.internal.axioms;

import org.apache.jena.rdf.model.Property;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLSameIndividualAxiom;
import com.github.owlcs.ontapi.internal.InternalConfig;
import com.github.owlcs.ontapi.internal.InternalObjectFactory;
import com.github.owlcs.ontapi.internal.ONTObject;
import com.github.owlcs.ontapi.internal.ONTWrapperImpl;
import com.github.owlcs.ontapi.jena.model.OntIndividual;
import com.github.owlcs.ontapi.jena.model.OntStatement;
import com.github.owlcs.ontapi.jena.vocabulary.OWL;

import java.util.Collection;

/**
 * base class {@link AbstractNaryTranslator}
 * example:
 * <pre>{@code
 * :indi1 owl:sameAs :indi2, :indi3 .
 * }</pre>
 * <p>
 * Created by szuev on 13.10.2016.
 *
 * @see OWLSameIndividualAxiom
 */
public class SameIndividualTranslator extends AbstractNaryTranslator<OWLSameIndividualAxiom, OWLIndividual, OntIndividual> {

    @Override
    public Property getPredicate() {
        return OWL.sameAs;
    }

    @Override
    Class<OntIndividual> getView() {
        return OntIndividual.class;
    }

    @Override
    public ONTObject<OWLSameIndividualAxiom> toAxiom(OntStatement statement,
                                                     InternalObjectFactory reader,
                                                     InternalConfig config) {
        ONTObject<? extends OWLIndividual> a = reader.getIndividual(statement.getSubject(getView()));
        ONTObject<? extends OWLIndividual> b = reader.getIndividual(statement.getObject().as(getView()));
        Collection<ONTObject<OWLAnnotation>> annotations = reader.getAnnotations(statement, config);
        OWLSameIndividualAxiom res = reader.getOWLDataFactory()
                .getOWLSameIndividualAxiom(a.getOWLObject(), b.getOWLObject(), ONTObject.toSet(annotations));
        return ONTWrapperImpl.create(res, statement).append(annotations).append(a).append(b);
    }

}
