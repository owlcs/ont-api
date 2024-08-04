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

package com.github.owlcs.ontapi.internal.objects;

import com.github.owlcs.ontapi.internal.AsRDFNode;
import com.github.owlcs.ontapi.internal.HasObjectFactory;
import com.github.owlcs.ontapi.internal.ModelObjectFactory;
import com.github.owlcs.ontapi.internal.ONTObject;
import com.github.owlcs.ontapi.owlapi.objects.AnonymousIndividualImpl;
import javax.annotation.Nonnull;
import org.apache.jena.graph.Triple;
import org.apache.jena.ontapi.common.OntEnhGraph;
import org.apache.jena.ontapi.model.OntIndividual;
import org.apache.jena.ontapi.model.OntModel;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * An {@link OWLAnonymousIndividual} implementation that is also an instance of {@link ONTObject}.
 * Created by @ssz on 07.08.2019.
 *
 * @see AnonymousIndividualImpl
 * @since 2.0.0
 */
public class ONTAnonymousIndividualImpl extends AnonymousIndividualImpl
        implements OWLAnonymousIndividual, HasObjectFactory, ONTSimple, ModelObject<OWLAnonymousIndividual>, AsRDFNode {

    protected final Supplier<OntModel> model;

    public ONTAnonymousIndividualImpl(String n, Supplier<OntModel> m) {
        super(n);
        this.model = Objects.requireNonNull(m);
    }

    @Override
    public OntIndividual.Anonymous asRDFNode() {
        return OntEnhGraph.asPersonalityModel(getModel()).getNodeAs(asNode(), OntIndividual.Anonymous.class);
    }

    @Override
    public OWLAnonymousIndividual getOWLObject() {
        return this;
    }

    @Override
    public OWLAnonymousIndividual eraseModel() {
        return getDataFactory().getOWLAnonymousIndividual(id);
    }

    @Override
    public OntModel getModel() {
        return model.get();
    }

    @Override
    @Nonnull
    public ModelObjectFactory getObjectFactory() {
        return HasObjectFactory.getObjectFactory(getModel());
    }

    @Override
    public Stream<Triple> triples() {
        return Stream.empty();
    }

    @Override
    public boolean isAnonymousIndividual() {
        return true;
    }

    @Override
    public Set<OWLAnonymousIndividual> getAnonymousIndividualSet() {
        return createSet(this);
    }

    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        throw new NotSerializableException("Suspicious method call. " +
                "Serialization is unsupported for ONTAnonymousIndividual.");
    }

    @Serial
    private void readObject(ObjectInputStream in) throws Exception {
        throw new NotSerializableException("Suspicious method call. " +
                "Deserialization is unsupported for ONTAnonymousIndividual.");
    }
}
