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

import com.github.owlcs.ontapi.internal.AxiomTranslator;
import com.github.owlcs.ontapi.jena.model.*;
import com.github.owlcs.ontapi.jena.utils.Iter;
import com.github.owlcs.ontapi.jena.utils.OntModels;
import com.github.owlcs.ontapi.jena.vocabulary.OWL;
import com.github.owlcs.ontapi.jena.vocabulary.XSD;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDatatype;

import java.util.Set;
import java.util.stream.Stream;

/**
 * A searcher for {@link OWLDatatype}.
 * Created by @ssz on 06.04.2020.
 */
public class ByDatatype extends ByEntity<OWLDatatype> {
    private static final Set<AxiomTranslator<? extends OWLAxiom>> TRANSLATORS = selectTranslators(null);
    private static final Set<Class<? extends OntClass.CardinalityRestrictionCE<?, ?>>> DATA_CARDINALITY_TYPES =
            Stream.of(OntClass.DataMaxCardinality.class, OntClass.DataMinCardinality.class, OntClass.DataCardinality.class)
                    .collect(Iter.toUnmodifiableSet());

    private static boolean isDataRestriction(OntStatement s) {
        return DATA_CARDINALITY_TYPES.stream().anyMatch(t -> s.getSubject().canAs(t));
    }

    @Override
    public ExtendedIterator<OntStatement> listStatements(OntModel m, String uri) {
        ExtendedIterator<OntStatement> res = super.listStatements(m, uri);
        if (RDFS.Literal.getURI().equals(uri)) {
            res = Iter.concat(res, Iter.flatMap(listForTopEntity(m), s -> listRootStatements(m, s)));
        }
        return Iter.concat(res, Iter.flatMap(listFromLiterals(m, uri), s -> listRootStatements(m, s)));
    }

    protected ExtendedIterator<OntStatement> listForTopEntity(OntModel m) {
        return Iter.flatMap(Iter.of(OWL.cardinality, OWL.maxCardinality, OWL.minCardinality),
                p -> OntModels.listLocalStatements(m, null, p, null)).filterKeep(ByDatatype::isDataRestriction);
    }

    protected ExtendedIterator<OntStatement> listFromLiterals(OntModel m, String uri) {
        ExtendedIterator<OntStatement> res = OntModels.listLocalStatements(m, null, null, null)
                .filterKeep(s -> s.getObject().isLiteral() && uri.equals(s.getLiteral().getDatatypeURI()));
        // https://github.com/owlcs/owlapi/issues/783
        if (XSD.xboolean.getURI().equals(uri)) {
            // HasSelf
            res = res.filterDrop(s -> s.getSubject().canAs(OntClass.HasSelf.class));
        } else if (XSD.nonNegativeInteger.getURI().equals(uri)) {
            // cardinality restrictions
            res = res.filterDrop(s -> s.getSubject().canAs(OntClass.CardinalityRestrictionCE.class));
        }
        return res;
    }

    @Override
    protected ExtendedIterator<OntStatement> listForSubject(OntModel model, OntObject subject) {
        if (!ByPrimitive.includeAnnotations(model)) {
            return super.listForSubject(model, subject);
        }
        OntAnnotation a = subject.getAs(OntAnnotation.class);
        if (a == null) {
            return super.listForSubject(model, subject);
        }
        OntStatement base = ByPrimitive.getRoot(a).getBase();
        if (base != null) {
            return Iter.of(base);
        }
        return super.listForSubject(model, subject);
    }

    @Override
    protected ExtendedIterator<AxiomTranslator<? extends OWLAxiom>> listTranslators() {
        return Iter.create(TRANSLATORS);
    }
}
