/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2023, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.internal.searchers.axioms;

import com.github.owlcs.ontapi.owlapi.objects.LiteralImpl;
import com.github.sszuev.jena.ontapi.model.OntClass;
import com.github.sszuev.jena.ontapi.model.OntModel;
import com.github.sszuev.jena.ontapi.model.OntObject;
import com.github.sszuev.jena.ontapi.model.OntStatement;
import com.github.sszuev.jena.ontapi.utils.Iterators;
import com.github.sszuev.jena.ontapi.utils.StdModels;
import com.github.sszuev.jena.ontapi.vocabulary.XSD;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.OWLLiteral;

/**
 * A searcher for {@link OWLLiteral}
 * Created by @ssz on 11.04.2020.
 */
public class ByLiteral extends ByPrimitive<OWLLiteral> {

    @Override
    public ExtendedIterator<OntStatement> listStatements(OntModel model, OWLLiteral literal) {
        Literal object = model.asRDFNode(LiteralImpl.asONT(literal).asNode()).asLiteral();
        ExtendedIterator<OntStatement> res = listByObject(model, object);
        // https://github.com/owlcs/owlapi/issues/783
        if (XSD.nonNegativeInteger.getURI().equals(object.getDatatypeURI())) {
            // cardinality restrictions
            res = res.filterDrop(s -> s.getSubject().canAs(OntClass.CardinalityRestrictionCE.class));
        } else if (StdModels.TRUE.equals(object)) {
            // HasSelf
            res = res.filterDrop(s -> s.getSubject().canAs(OntClass.HasSelf.class));
        }
        return Iterators.flatMap(res, s -> listRootStatements(model, s));
    }

    @Override
    protected ExtendedIterator<OntStatement> listProperties(OntModel model, OntObject root) {
        return listPropertiesIncludeAnnotations(model, root);
    }
}
