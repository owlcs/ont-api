/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2020, The University of Manchester, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.internal.searchers;

import com.github.owlcs.ontapi.internal.*;
import com.github.owlcs.ontapi.internal.axioms.SubClassOfTranslator;
import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.jena.model.OntStatement;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

/**
 * Created by @ssz on 18.04.2020.
 */
public class SubClassOfBySubject extends BaseByObject<OWLSubClassOfAxiom, OWLClass> {
    private static final SubClassOfTranslator TRANSLATOR = toTranslator(OWLTopObjectType.SUBCLASS_OF);

    @Override
    public ExtendedIterator<ONTObject<OWLSubClassOfAxiom>> listAxioms(OWLClass clazz,
                                                                      OntModel model,
                                                                      InternalObjectFactory factory,
                                                                      InternalConfig config) {
        Resource s = WriteHelper.toResource(clazz.getIRI());
        ExtendedIterator<OntStatement> res = listBySubjectAndPredicate(model, s, RDFS.subClassOf)
                .filterKeep(TRANSLATOR::filter);
        return translate(TRANSLATOR, res, factory, config);
    }
}
