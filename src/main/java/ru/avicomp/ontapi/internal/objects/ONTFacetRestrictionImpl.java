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

package ru.avicomp.ontapi.internal.objects;

import org.apache.jena.graph.BlankNodeId;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWLFacet;
import ru.avicomp.ontapi.internal.InternalObjectFactory;
import ru.avicomp.ontapi.internal.ONTObject;
import ru.avicomp.ontapi.internal.ReadHelper;
import ru.avicomp.ontapi.jena.model.OntFR;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.utils.OntModels;

import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * An {@link OWLFacetRestriction} implementation that is also {@link ONTObject}.
 * Created by @ssz on 20.08.2019.
 *
 * @see ru.avicomp.ontapi.owlapi.objects.OWLFacetRestrictionImpl
 * @see OntFR
 * @since 1.4.3
 */
@SuppressWarnings("WeakerAccess")
public class ONTFacetRestrictionImpl
        extends ONTExpressionImpl<OntFR> implements OWLFacetRestriction, ONTObject<OWLFacetRestriction> {

    public ONTFacetRestrictionImpl(BlankNodeId n, Supplier<OntGraphModel> m) {
        super(n, m);
    }

    /**
     * Wraps the given {@link OntFR} as {@link OWLFacetRestriction} and {@link ONTObject}.
     *
     * @param fr    {@link OntFR}, not {@code null}
     * @param factory {@link InternalObjectFactory}, not {@code null}
     * @param model a provider of non-null {@link OntGraphModel}, cannot be {@code null}
     * @return {@link ONTFacetRestrictionImpl}
     */
    public static ONTFacetRestrictionImpl create(OntFR fr,
                                                 InternalObjectFactory factory,
                                                 Supplier<OntGraphModel> model) {
        ONTFacetRestrictionImpl res = new ONTFacetRestrictionImpl(fr.asNode().getBlankNodeId(), model);
        res.putContent(res.collectContent(fr, factory));
        return res;
    }

    @Override
    public OntFR asRDFNode() {
        return as(OntFR.class);
    }

    @Override
    public OWLFacetRestriction getOWLObject() {
        return this;
    }

    @Override
    protected Object[] collectContent(OntFR fr, InternalObjectFactory of) {
        Class<? extends OntFR> type = OntModels.getOntType(fr);
        return new Object[]{ReadHelper.getFacet(type), of.getLiteral(fr.getValue())};
    }

    @Override
    public OWLFacet getFacet() {
        return (OWLFacet) getContent()[0];
    }

    @Override
    public OWLLiteral getFacetValue() {
        return getONTLiteral().getOWLObject();
    }

    @SuppressWarnings("unchecked")
    public ONTObject<OWLLiteral> getONTLiteral() {
        return (ONTObject<OWLLiteral>) getContent()[1];
    }

    public OWLDatatype getDatatype() {
        return getFacetValue().getDatatype();
    }

    @Override
    public Stream<ONTObject<? extends OWLObject>> objects() {
        return Stream.of(getONTLiteral());
    }

    @Override
    public boolean containsDatatype(OWLDatatype datatype) {
        return getDatatype().equals(datatype);
    }

    @Override
    public Set<OWLEntity> getSignatureSet() {
        return createSet(getDatatype());
    }

    @Override
    public boolean canContainNamedClasses() {
        return false;
    }

    @Override
    public Set<OWLDatatype> getDatatypeSet() {
        return createSet(getDatatype());
    }

    @Override
    public boolean canContainNamedIndividuals() {
        return false;
    }

    @Override
    public boolean canContainDataProperties() {
        return false;
    }

    @Override
    public boolean canContainObjectProperties() {
        return false;
    }

    @Override
    public boolean canContainAnnotationProperties() {
        return false;
    }

    @Override
    public boolean canContainClassExpressions() {
        return false;
    }

    @Override
    public boolean canContainAnonymousIndividuals() {
        return false;
    }
}
