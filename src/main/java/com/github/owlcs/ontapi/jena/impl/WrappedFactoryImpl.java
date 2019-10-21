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

package com.github.owlcs.ontapi.jena.impl;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.enhanced.EnhNode;
import org.apache.jena.graph.Node;
import org.apache.jena.util.iterator.ExtendedIterator;
import com.github.owlcs.ontapi.jena.OntJenaException;
import com.github.owlcs.ontapi.jena.impl.conf.BaseFactoryImpl;
import com.github.owlcs.ontapi.jena.impl.conf.ObjectFactory;
import com.github.owlcs.ontapi.jena.model.OntObject;

import java.util.Objects;

/**
 * A factory wrapper.
 * It is a facility to provide implicit links between {@link ObjectFactory object factories}.
 * For more details see the {@link PersonalityModel} description and also the description for
 * the method {@link com.github.owlcs.ontapi.jena.impl.conf.PersonalityBuilder#add(Class, ObjectFactory)}.
 * <p>
 * Created by @ssz on 18.01.2019.
 *
 * @see Factories
 * @since 1.4.0
 */
@SuppressWarnings("WeakerAccess")
public class WrappedFactoryImpl extends BaseFactoryImpl {
    private final Class<? extends OntObject> type;

    public WrappedFactoryImpl(Class<? extends OntObject> type) {
        this.type = Objects.requireNonNull(type);
    }

    static WrappedFactoryImpl of(Class<? extends OntObject> type) {
        return new WrappedFactoryImpl(type);
    }

    /**
     * Finds and returns the {@link ObjectFactory} instance for the encapsulated {@link OntObject object} type.
     * This factory and the returned one are synonymous: both have the same behaviour.
     *
     * @param g {@link EnhGraph}, the model. Must be instance of {@link PersonalityModel}
     * @return {@link ObjectFactory}, not {@code null}
     * @throws OntJenaException in case nothing is found
     */
    public ObjectFactory getDelegate(EnhGraph g) throws OntJenaException {
        return getFactory(g);
    }

    protected ObjectFactory getFactory(EnhGraph g) throws OntJenaException {
        ObjectFactory res = PersonalityModel.asPersonalityModel(g).getOntPersonality().getObjectFactory(type);
        if (res == null) {
            throw new OntJenaException.IllegalState("Unable to find factory for " + type);
        }
        return res;
    }

    @Override
    public ExtendedIterator<EnhNode> iterator(EnhGraph eg) {
        return getDelegate(eg).iterator(eg);
    }

    @Override
    public boolean canWrap(Node node, EnhGraph eg) {
        return getDelegate(eg).canWrap(node, eg);
    }

    @Override
    public EnhNode createInstance(Node node, EnhGraph eg) {
        return getDelegate(eg).createInstance(node, eg);
    }

    @Override
    public EnhNode wrap(Node node, EnhGraph eg) {
        return getDelegate(eg).wrap(node, eg);
    }

    @Override
    public String toString() {
        return String.format("Factory[%s]", OntObjectImpl.viewAsString(type));
    }
}
