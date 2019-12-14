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

package com.github.owlcs.ontapi.jena.impl.conf;

import com.github.owlcs.ontapi.jena.OntJenaException;
import com.github.owlcs.ontapi.jena.model.OntClass;
import com.github.owlcs.ontapi.jena.model.OntEntity;
import com.github.owlcs.ontapi.jena.model.OntObject;
import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.enhanced.Personality;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import java.util.*;
import java.util.stream.Collectors;

/**
 * An {@link OntPersonality} builder.
 * This must be the only place to create various {@code OntPersonality} objects.
 * <p>
 * Created by @szz on 17.01.2019.
 *
 * @since 1.4.0
 */
@SuppressWarnings("WeakerAccess")
public class PersonalityBuilder {
    private final Map<Class<? extends OntObject>, ObjectFactory> map;

    private final Personality<RDFNode> base;
    private OntPersonality.Punnings punnings;
    private OntPersonality.Builtins builtins;
    private OntPersonality.Reserved reserved;

    public PersonalityBuilder() {
        this(new LinkedHashMap<>());
    }

    protected PersonalityBuilder(Map<Class<? extends OntObject>, ObjectFactory> factories) {
        this.map = Objects.requireNonNull(factories);
        this.base = new Personality<>();
    }

    /**
     * Makes a full copy of the given {@link OntPersonality}
     * in the form of modifiable {@link PersonalityBuilder builder}.
     *
     * @param from {@link OntPersonality} to copy settings, not {@code null}
     * @return {@link PersonalityBuilder}
     */
    public static PersonalityBuilder from(OntPersonality from) {
        return new PersonalityBuilder()
                .addPersonality(OntPersonality.asJenaPersonality(from))
                .setPunnings(from.getPunnings())
                .setBuiltins(from.getBuiltins())
                .setReserved(from.getReserved());
    }

    @SuppressWarnings("rawtypes")
    private static <X extends Vocabulary> X require(X obj, Class<X> type) {
        if (obj == null) {
            throw new IllegalStateException("The " + type.getSimpleName() + " Vocabulary must be present in builder.");
        }
        return obj;
    }

    @SuppressWarnings("rawtypes")
    private static <V extends Vocabulary> V hasSpec(V voc, Class... types) {
        Objects.requireNonNull(voc);
        Set<?> errors = Arrays.stream(types).filter(x -> {
            try {
                //noinspection unchecked
                return voc.get(x) == null;
            } catch (OntJenaException e) {
                return true;
            }
        }).collect(Collectors.toSet());
        if (errors.isEmpty()) return voc;
        throw new IllegalArgumentException("The vocabulary " + voc + " has missed required types: " + errors);
    }

    /**
     * Makes a full copy of this builder.
     *
     * @return {@link PersonalityBuilder}, a copy
     */
    public PersonalityBuilder copy() {
        PersonalityBuilder res = new PersonalityBuilder(new LinkedHashMap<>(this.map));
        res.addPersonality(base.copy());
        if (punnings != null) res.setPunnings(punnings);
        if (builtins != null) res.setBuiltins(builtins);
        if (reserved != null) res.setReserved(reserved);
        return res;
    }

    /**
     * Associates the specified {@link ObjectFactory factory} with the specified {@link OntObject object} type.
     * If the builder previously contained a mapping for the object type (which is common situation),
     * the old factory is replaced by the specified factory.
     * <p>
     * Please note: the {@link ObjectFactory factory} must not explicitly refer to another factory,
     * instead it may contain implicit references through
     * {@link com.github.owlcs.ontapi.jena.impl.PersonalityModel#asPersonalityModel(EnhGraph)} method.
     * For example if you need a check, that some {@link Node node} is an OWL-Class inside your factory,
     * you can use {@link com.github.owlcs.ontapi.jena.impl.PersonalityModel#canAs(Class, Node, EnhGraph)}
     * with the type {@link OntClass.Named}.
     *
     * @param type    {@code Class}-type of the concrete {@link OntObject}.
     * @param factory {@link ObjectFactory} the factory to produce the instances of the {@code type},
     * @return this builder
     */
    public PersonalityBuilder add(Class<? extends OntObject> type, ObjectFactory factory) {
        map.put(type, factory);
        return this;
    }

    /**
     * Adds everything from the specified {@link Personality Jena Personality} to the existing internal collection.
     *
     * @param from {@link Personality} with generic type {@link RDFNode}, not {@code null}
     * @return this builder
     * @see Personality#add(Personality)
     */
    public PersonalityBuilder addPersonality(Personality<RDFNode> from) {
        this.base.add(Objects.requireNonNull(from));
        return this;
    }

    /**
     * Sets a new punnings personality vocabulary.
     *
     * @param punnings {@link OntPersonality.Punnings}, not {@code null}
     * @return this builder
     */
    public PersonalityBuilder setPunnings(OntPersonality.Punnings punnings) {
        this.punnings = hasSpec(punnings, OntEntity.entityTypes().toArray(Class[]::new));
        return this;
    }

    /**
     * Sets a new builtins personality vocabulary.
     *
     * @param builtins {@link OntPersonality.Builtins}, not {@code null}
     * @return this builder
     */
    public PersonalityBuilder setBuiltins(OntPersonality.Builtins builtins) {
        this.builtins = hasSpec(builtins, OntEntity.entityTypes().toArray(Class[]::new));
        return this;
    }

    /**
     * Sets a new reserved personality vocabulary.
     *
     * @param reserved {@link OntPersonality.Reserved}, not {@code null}
     * @return this builder
     */
    public PersonalityBuilder setReserved(OntPersonality.Reserved reserved) {
        this.reserved = hasSpec(reserved, Resource.class, Property.class);
        return this;
    }

    /**
     * Builds a new personality configuration.
     *
     * @return {@link OntPersonality}, fresh instance
     * @throws IllegalStateException in case the builder does not contain require components
     */
    public OntPersonality build() throws IllegalStateException {
        OntPersonalityImpl res = new OntPersonalityImpl(base, punnings(), builtins(), reserved());
        map.forEach(res::register);
        return res;
    }

    private OntPersonality.Punnings punnings() {
        return require(punnings, OntPersonality.Punnings.class);
    }

    private OntPersonality.Builtins builtins() {
        return require(builtins, OntPersonality.Builtins.class);
    }

    private OntPersonality.Reserved reserved() {
        return require(reserved, OntPersonality.Reserved.class);
    }

}
