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

import com.github.owlcs.ontapi.OwlObjects;
import com.github.owlcs.ontapi.config.AxiomsSettings;
import com.github.owlcs.ontapi.internal.ONTObject;
import com.github.owlcs.ontapi.internal.ONTObjectFactory;
import com.github.sszuev.jena.ontapi.model.OntModel;
import com.github.sszuev.jena.ontapi.model.OntObject;
import com.github.sszuev.jena.ontapi.model.OntStatement;
import com.github.sszuev.jena.ontapi.utils.Iterators;
import com.github.sszuev.jena.ontapi.utils.Models;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.EntityType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;

/**
 * Created by @ssz on 12.04.2020.
 */
public class ByIRI extends ByPrimitive<IRI> {
    private static final ByClass BY_CLASS = new ByClass();
    private static final ByDatatype BY_DATATYPE = new ByDatatype();

    @Override
    public final ExtendedIterator<ONTObject<OWLAxiom>> listONTAxioms(IRI iri,
                                                                     OntModel model,
                                                                     ONTObjectFactory factory,
                                                                     AxiomsSettings config) {
        ExtendedIterator<ONTObject<OWLAxiom>> res = super.listONTAxioms(iri, model, factory, config);
        if (isSystem(model, iri.getIRIString())) {
            return res.filterKeep(x -> containsURI(factory.getOWLDataFactory(), x.getOWLObject(), iri));
        }
        return res;
    }

    @Override
    protected ExtendedIterator<OntStatement> listStatements(OntModel model, IRI iri) {
        String uri = iri.getIRIString();
        ExtendedIterator<OntStatement> res = listCandidates(model, uri);
        return BY_DATATYPE.includeImplicit(BY_CLASS.includeImplicit(res, model, uri), model, uri);
    }

    protected ExtendedIterator<OntStatement> listCandidates(OntModel model, String iri) {
        return Iterators.flatMap(listStatements(model).filterKeep(x -> Models.containsURI(x, iri)), s -> listRootStatements(model, s));
    }

    /**
     * Answers {@code true} if the given {@code iri} is a part of the given {@code axiom}.
     *
     * @param factory {@link OWLDataFactory}
     * @param axiom   {@link OWLAxiom}
     * @param iri     {@link IRI}
     * @return boolean
     */
    protected boolean containsURI(OWLDataFactory factory, OWLAxiom axiom, IRI iri) {
        if (axiom.isAnnotated() || AxiomType.ANNOTATION_ASSERTION.equals(axiom.getAxiomType())) {
            return OwlObjects.iris(axiom).anyMatch(iri::equals);
        }
        return EntityType.values().stream().map(t -> factory.getOWLEntity(t, iri)).anyMatch(axiom::containsEntityInSignature);
    }

    @Override
    protected ExtendedIterator<OntStatement> listProperties(OntModel model, OntObject root) {
        return listPropertiesIncludeAnnotations(model, root);
    }
}
