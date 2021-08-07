/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2021, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.internal.objects;

import com.github.owlcs.ontapi.internal.ModelObjectFactory;
import com.github.owlcs.ontapi.internal.ONTObject;
import com.github.owlcs.ontapi.internal.ONTObjectFactory;
import com.github.owlcs.ontapi.internal.ReadHelper;
import com.github.owlcs.ontapi.jena.model.OntFacetRestriction;
import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.jena.utils.OntModels;
import com.github.owlcs.ontapi.owlapi.objects.FacetRestrictionImpl;
import org.apache.jena.graph.BlankNodeId;
import org.apache.jena.graph.impl.LiteralLabel;
import org.apache.jena.rdf.model.Literal;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWLFacet;

import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * An {@link OWLFacetRestriction} implementation that is also {@link ONTObject}.
 * Created by @ssz on 20.08.2019.
 *
 * @see FacetRestrictionImpl
 * @see OntFacetRestriction
 * @since 2.0.0
 */
@SuppressWarnings("WeakerAccess")
public class ONTFacetRestrictionImpl
        extends ONTExpressionImpl<OntFacetRestriction> implements OWLFacetRestriction, ModelObject<OWLFacetRestriction> {

    public ONTFacetRestrictionImpl(BlankNodeId n, Supplier<OntModel> m) {
        super(n, m);
    }

    /**
     * Wraps the given {@link OntFacetRestriction} as {@link OWLFacetRestriction} and {@link ONTObject}.
     *
     * @param fr      {@link OntFacetRestriction}, not {@code null}
     * @param factory {@link ONTObjectFactory}, not {@code null}
     * @param model   a provider of non-null {@link OntModel}, cannot be {@code null}
     * @return {@link ONTFacetRestrictionImpl}
     */
    public static ONTFacetRestrictionImpl create(OntFacetRestriction fr,
                                                 ONTObjectFactory factory,
                                                 Supplier<OntModel> model) {
        ONTFacetRestrictionImpl res = new ONTFacetRestrictionImpl(fr.asNode().getBlankNodeId(), model);
        res.putContent(res.initContent(fr, factory));
        return res;
    }

    @Override
    public OntFacetRestriction asRDFNode() {
        return as(OntFacetRestriction.class);
    }

    @Override
    public OWLFacetRestriction getOWLObject() {
        return this;
    }

    @Override
    protected Object[] collectContent(OntFacetRestriction fr, ONTObjectFactory factory) {
        Class<? extends OntFacetRestriction> type = OntModels.getOntType(fr);
        return new Object[]{ReadHelper.getFacet(type), fr.getValue().asNode().getLiteral()};
    }

    @Override
    protected Object[] initContent(OntFacetRestriction fr, ONTObjectFactory factory) {
        OWLFacet facet = ReadHelper.getFacet(OntModels.getOntType(fr));
        Literal value = fr.getValue();
        int hash = OWLObject.hashIteration(hashIndex(), facet.hashCode());
        this.hashCode = OWLObject.hashIteration(hash, factory.getLiteral(value).hashCode());
        return new Object[]{facet, value.asNode().getLiteral()};
    }

    @Override
    public OWLFacet getFacet() {
        return (OWLFacet) getContent()[0];
    }

    @Override
    public OWLLiteral getFacetValue() {
        return getONTLiteral().getOWLObject();
    }

    public ONTObject<OWLLiteral> getONTLiteral() {
        return findONTLiteral(getObjectFactory());
    }

    protected ONTObject<OWLLiteral> findONTLiteral(ModelObjectFactory factory) {
        return factory.getLiteral((LiteralLabel) getContent()[1]);
    }

    @Override
    public OWLFacetRestriction eraseModel() {
        return getDataFactory().getOWLFacetRestriction(getFacet(), eraseModel(getFacetValue()));
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
