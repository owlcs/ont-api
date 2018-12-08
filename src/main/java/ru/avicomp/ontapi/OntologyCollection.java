/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2018, Avicomp Services, AO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package ru.avicomp.ontapi;

import org.semanticweb.owlapi.model.HasOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.util.CollectionFactory;

import java.io.Serializable;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.stream.Stream;

/**
 * A collection to store anything that has {@link OWLOntologyID Ontology ID}.
 * Implementation notes:
 * It was introduced to be sure that all members are in the consistent state.
 * Currently it is not possible to use directly different {@code Map}s with {@link OWLOntologyID Ontology ID} as keys
 * like in the original OWL-API implementation,
 * since anything, including that ID, can be changed externally (e.g. directly from the jena graph
 * using shadow {@link ru.avicomp.ontapi.jena.model.OntGraphModel} interface or something else).
 * On the other hand, it is not expected that this collection will hold a large number of elements,
 * so using {@code Set} as internal storage collection is OK.
 * <p>
 * Created by @ssz on 08.12.2018.
 */
@SuppressWarnings("WeakerAccess")
public class OntologyCollection<O extends HasOntologyID> implements Serializable {
    private static final long serialVersionUID = 3693502109998760296L;
    // TODO: switch to real map
    protected final Collection<O> map;

    public OntologyCollection(ReadWriteLock lock) {
        // TODO: must use only RW-Lock for synchronization instead of concurrent collection
        this.map = lock != NoOpReadWriteLock.NO_OP_RW_LOCK ?
                CollectionFactory.createSyncSet() : CollectionFactory.createSet();
    }

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public void clear() {
        map.clear();
    }

    public Stream<O> values() {
        return map.stream();
    }

    public Stream<OWLOntologyID> keys() {
        return values().map(HasOntologyID::getOntologyID);
    }

    public Optional<O> get(OWLOntologyID key) {
        return values()
                .filter(o -> o.getOntologyID().hashCode() == key.hashCode() && key.equals(o.getOntologyID()))
                .findFirst();
    }

    public boolean contains(OWLOntologyID key) {
        return values()
                .filter(o -> o.getOntologyID().hashCode() == key.hashCode())
                .anyMatch(o -> key.equals(o.getOntologyID()));
    }

    public void add(O o) {
        map.add(o);
    }

    public Optional<O> remove(OWLOntologyID id) {
        Optional<O> res = get(id);
        res.ifPresent(map::remove);
        return res;
    }
}
