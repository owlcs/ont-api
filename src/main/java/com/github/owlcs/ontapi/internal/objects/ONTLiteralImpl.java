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

package com.github.owlcs.ontapi.internal.objects;

import org.apache.jena.graph.FrontsTriple;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.impl.LiteralLabel;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObject;
import com.github.owlcs.ontapi.internal.*;
import com.github.owlcs.ontapi.jena.impl.PersonalityModel;
import com.github.owlcs.ontapi.jena.model.OntDT;
import com.github.owlcs.ontapi.jena.model.OntGraphModel;
import com.github.owlcs.ontapi.owlapi.objects.OWLLiteralImpl;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * An implementation of {@link OWLLiteral} that is also an {@link ONTObject}.
 * Created by @ssz on 07.08.2019.
 *
 * @see OWLLiteralImpl
 * @since 1.4.3
 */
@SuppressWarnings("WeakerAccess")
public class ONTLiteralImpl extends OWLLiteralImpl
        implements OWLLiteral, HasObjectFactory, ONTComposite, ModelObject<OWLLiteral>, AsRDFNode {

    protected final Supplier<OntGraphModel> model;

    public ONTLiteralImpl(LiteralLabel n, Supplier<OntGraphModel> m) {
        super(n);
        this.model = Objects.requireNonNull(m);
    }

    /**
     * Using the {@code factory} finds or creates an {@link OWLLiteral} instance.
     *
     * @param label   {@link LiteralLabel}, not {@code null}
     * @param factory {@link InternalObjectFactory}, not {@code null}
     * @param model   a {@code Supplier} with a {@link OntGraphModel},
     *                which is only used in case the {@code factory} has no reference to a model
     * @return an {@link ONTObject} which is {@link OWLLiteral}
     */
    public static ONTObject<OWLLiteral> find(LiteralLabel label,
                                             InternalObjectFactory factory,
                                             Supplier<OntGraphModel> model) {
        if (factory instanceof ModelObjectFactory) {
            return ((ModelObjectFactory) factory).getLiteral(label);
        }
        RDFNode res = model.get().asRDFNode(NodeFactory.createLiteral(label));
        return factory.getLiteral(res.asLiteral());
    }

    @Override
    public Literal asRDFNode() {
        return getModel().asRDFNode(asNode()).asLiteral();
    }

    @Override
    public OWLLiteral getOWLObject() {
        return this;
    }

    @Override
    public OntGraphModel getModel() {
        return model.get();
    }

    @Override
    public OWLLiteral eraseModel() {
        return getDataFactory().getOWLLiteral(label);
    }

    @Override
    public InternalObjectFactory getObjectFactory() {
        return HasObjectFactory.getObjectFactory(getModel());
    }

    /**
     * Returns a {@link OntDT} instance that corresponds datatype.
     *
     * @return {@link OntDT}
     * @see OWLLiteralImpl#getDatatype()
     * @see OntGraphModel#getDatatype(Literal)
     */
    public OntDT getDatatypeResource() {
        return PersonalityModel.asPersonalityModel(getModel())
                .getNodeAs(NodeFactory.createURI(getDatatypeURI()), OntDT.class);
    }

    @Override
    public OWLDatatype getDatatype() {
        return getONTDatatype().getOWLObject();
    }

    public ONTObject<? extends OWLDatatype> getONTDatatype() {
        InternalObjectFactory of = getObjectFactory();
        if (of instanceof ModelObjectFactory) {
            return ((ModelObjectFactory) of).getDatatype(getDatatypeURI());
        }
        return of.getDatatype(getDatatypeResource());
    }

    @Override
    public Stream<Triple> triples() {
        OntDT res = getDatatypeResource();
        return res.isBuiltIn() ? Stream.empty() : res.spec().map(FrontsTriple::asTriple);
    }

    @Override
    public Stream<ONTObject<? extends OWLObject>> objects() {
        return Stream.of(getONTDatatype());
    }

    @Override
    public boolean canContainClassExpressions() {
        return false;
    }

    @Override
    public boolean canContainAnonymousIndividuals() {
        return false;
    }

    @Override
    public boolean canContainNamedClasses() {
        return false;
    }

    @Override
    public boolean canContainNamedIndividuals() {
        return false;
    }

    @Override
    public boolean canContainObjectProperties() {
        return false;
    }

    @Override
    public boolean canContainDataProperties() {
        return false;
    }

    @Override
    public boolean canContainAnnotationProperties() {
        return false;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        throw new NotSerializableException("Suspicious method call. Serialization is unsupported for ONTLiteral.");
    }

    private void readObject(ObjectInputStream in) throws Exception {
        throw new NotSerializableException("Suspicious method call. Deserialization is unsupported for ONTLiteral.");
    }
}
