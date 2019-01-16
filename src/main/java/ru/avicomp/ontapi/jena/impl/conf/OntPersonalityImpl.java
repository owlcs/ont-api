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

package ru.avicomp.ontapi.jena.impl.conf;

import org.apache.jena.enhanced.Implementation;
import org.apache.jena.enhanced.Personality;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.utils.BuiltIn;
import ru.avicomp.ontapi.jena.utils.Iter;
import ru.avicomp.ontapi.jena.vocabulary.OWL;

import java.util.*;

/**
 * Personality (mappings from [interface] Class objects of RDFNode to {@link Implementation} factories)
 * <p>
 * Created by @szuev on 10.11.2016.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
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

    private OntPersonalityImpl(OntPersonalityImpl other) {
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
    public void register(Class<? extends OntObject> type, OntObjectFactory factory) {
        super.add(Objects.requireNonNull(type, "Null type."), Objects.requireNonNull(factory, "Null factory."));
    }

    /**
     * Removes factory.
     *
     * @param view Interface (OntObject)
     */
    public void unregister(Class<? extends OntObject> view) {
        getMap().remove(view);
    }

    /**
     * Gets factory for {@link OntObject}.
     *
     * @param type Interface (OntObject type)
     * @return {@link OntObjectFactory} factory
     */
    @Override
    public OntObjectFactory getOntImplementation(Class<? extends OntObject> type) {
        return (OntObjectFactory) OntJenaException.notNull(getImplementation(type),
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

    public static Builtins createBuiltinsVocabulary(BuiltIn.Vocabulary voc) {
        Objects.requireNonNull(voc);
        Map<Class<? extends OntEntity>, Set<? extends Resource>> res = new HashMap<>();
        res.put(OntNAP.class, voc.annotationProperties());
        res.put(OntNDP.class, voc.datatypeProperties());
        res.put(OntNOP.class, voc.objectProperties());
        res.put(OntDT.class, voc.datatypeProperties());
        res.put(OntClass.class, voc.objectProperties());
        res.put(OntIndividual.Named.class, Collections.emptySet());
        res.put(OntEntity.class, res.values().stream().flatMap(Collection::stream).collect(Iter.toUnmodifiableSet()));
        return type -> get(res, type);
    }

    public static Reserved createReservedVocabulary(BuiltIn.Vocabulary voc) {
        Objects.requireNonNull(voc);
        Map<Class<? extends Resource>, Set<? extends Resource>> res = new HashMap<>();
        res.put(Resource.class, voc.reservedResources());
        res.put(Property.class, voc.reservedProperties());
        return type -> get(res, type);
    }

    public static Punnings createPunningsVocabulary(OntModelConfig.StdMode mode) {
        Objects.requireNonNull(mode);
        Map<Class<? extends OntEntity>, Set<? extends Resource>> res = new HashMap<>();
        if (!OntModelConfig.StdMode.LAX.equals(mode)) {
            put(res, OntClass.class, RDFS.Datatype);
            put(res, OntDT.class, OWL.Class);
        }
        if (OntModelConfig.StdMode.STRICT.equals(mode)) {
            put(res, OntNAP.class, OWL.ObjectProperty, OWL.DatatypeProperty);
            put(res, OntNDP.class, OWL.ObjectProperty, OWL.AnnotationProperty);
            put(res, OntNOP.class, OWL.DatatypeProperty, OWL.AnnotationProperty);
        }
        if (OntModelConfig.StdMode.MEDIUM.equals(mode)) {
            put(res, OntNDP.class, OWL.ObjectProperty);
            put(res, OntNOP.class, OWL.DatatypeProperty);
        }
        OntEntity.entityTypes().forEach(t -> res.computeIfAbsent(t, k -> Collections.emptySet()));
        return type -> get(res, type);
    }

    @SafeVarargs
    private static <K, V> void put(Map<K, Set<? extends V>> map, K key, V... values) {
        map.put(key, Arrays.stream(values).collect(Iter.toUnmodifiableSet()));
    }

    private static <K, V> Set<? extends V> get(Map<K, Set<? extends V>> map, K key) {
        Set<? extends V> res = map.get(OntJenaException.notNull(key, "Null key"));
        if (res == null) {
            throw new OntJenaException.Unsupported("Unsupported class-type " + key);
        }
        return res;
    }

}
