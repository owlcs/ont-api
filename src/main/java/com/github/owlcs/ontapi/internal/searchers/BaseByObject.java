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

import com.github.owlcs.ontapi.internal.BaseSearcher;
import com.github.owlcs.ontapi.internal.ByObject;
import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.jena.model.OntStatement;
import com.github.owlcs.ontapi.jena.utils.OntModels;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLObject;

/**
 * Created by @ssz on 18.04.2020.
 */
@SuppressWarnings("SameParameterValue")
abstract class BaseByObject<A extends OWLAxiom, O extends OWLObject> extends BaseSearcher implements ByObject<A, O> {

    final ExtendedIterator<OntStatement> listBySubject(OntModel model, Resource subject) {
        return listStatements(model, subject, null, null);
    }

    final ExtendedIterator<OntStatement> listBySubjectAndProperty(OntModel m, Resource subject, Property uri) {
        return listStatements(m, subject, uri, null);
    }

    final ExtendedIterator<OntStatement> listByProperty(OntModel m, Property uri) {
        return listStatements(m, null, uri, null);
    }

    final ExtendedIterator<OntStatement> listByPropertyAndObject(OntModel model, Property uri, RDFNode object) {
        return listStatements(model, null, uri, object);
    }

    final ExtendedIterator<OntStatement> listByObject(OntModel model, RDFNode object) {
        return listStatements(model, null, null, object);
    }

    final ExtendedIterator<OntStatement> listStatements(OntModel model) {
        return listStatements(model, null, null, null);
    }

    private ExtendedIterator<OntStatement> listStatements(OntModel model, Resource s, Property p, RDFNode o) {
        return OntModels.listLocalStatements(model, s, p, o);
    }
}
