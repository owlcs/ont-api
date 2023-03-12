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

package com.github.owlcs.ontapi.jena.impl.conf;

import com.github.owlcs.ontapi.jena.OntJenaException;
import com.github.owlcs.ontapi.jena.model.OntObject;
import org.apache.jena.enhanced.Implementation;
import org.apache.jena.enhanced.Personality;
import org.apache.jena.rdf.model.RDFNode;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * A default implementation of {@link OntPersonality}.
 * Mappings from [interface] Class objects of RDFNode to {@link Implementation} factories.
 * <p>
 * Created by @szuev on 10.11.2016.
 */
@SuppressWarnings({"WeakerAccess"})
public class OntPersonalityImpl extends Personality<RDFNode> implements OntPersonality {
    private final Punnings punnings;
    private final Builtins builtins;
    private final Reserved reserved;

    public OntPersonalityImpl(Personality<RDFNode> other, Punnings punnings, Builtins builtins, Reserved reserved) {
        super(Objects.requireNonNull(other, "Null personalities"));
        this.builtins = Objects.requireNonNull(builtins, "Null builtins vocabulary");
        this.punnings = Objects.requireNonNull(punnings, "Null punnings vocabulary");
        this.reserved = Objects.requireNonNull(reserved, "Null reserved vocabulary");
    }

    protected OntPersonalityImpl(OntPersonalityImpl other) {
        this(other, other.getPunnings(), other.getBuiltins(), other.getReserved());
    }

    @Override
    public Builtins getBuiltins() {
        return builtins;
    }

    @Override
    public Punnings getPunnings() {
        return punnings;
    }

    @Override
    public Reserved getReserved() {
        return reserved;
    }

    /**
     * Registers new OntObject if needed
     *
     * @param type    Interface (OntObject)
     * @param factory Factory to crete object
     */
    public void register(Class<? extends OntObject> type, ObjectFactory factory) {
        super.add(Objects.requireNonNull(type, "Null type."), ObjectFactory.asJenaImplementation(factory));
    }

    /**
     * Removes the factory.
     *
     * @param view Interface (OntObject)
     */
    public void unregister(Class<? extends OntObject> view) {
        getMap().remove(view);
    }

    @Override
    public Stream<Class<? extends RDFNode>> types() {
        return getMap().keySet().stream();
    }

    /**
     * Gets factory for {@link OntObject}.
     *
     * @param type Interface (OntObject type)
     * @return {@link ObjectFactory} factory
     */
    @Override
    public ObjectFactory getObjectFactory(Class<? extends OntObject> type) {
        return (ObjectFactory) OntJenaException.notNull(getImplementation(type),
                "Can't find factory for the object type " + type);
    }

    @Override
    public OntPersonalityImpl add(Personality<RDFNode> other) {
        super.add(other);
        return this;
    }

    @Override
    public OntPersonalityImpl copy() {
        return new OntPersonalityImpl(this);
    }

}
