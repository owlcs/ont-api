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

package ru.avicomp.ontapi.utils;

import org.apache.jena.graph.*;
import org.apache.jena.shared.AccessDeniedException;
import org.apache.jena.shared.AddDeniedException;
import org.apache.jena.shared.DeleteDeniedException;
import org.apache.jena.shared.PrefixMapping;

import java.util.Objects;

/**
 * Created by @ssz on 21.10.2018.
 */
public class UnmodifiableGraph extends org.apache.jena.sparql.graph.UnmodifiableGraph {

    private static final Capabilities READ_ONLY_CAPABILITIES = new Capabilities() {
        @Override
        public boolean sizeAccurate() {
            return true;
        }

        @Override
        public boolean addAllowed() {
            return addAllowed(false);
        }

        @Override
        public boolean addAllowed(boolean every) {
            return false;
        }

        @Override
        public boolean deleteAllowed() {
            return deleteAllowed(false);
        }

        @Override
        public boolean deleteAllowed(boolean every) {
            return false;
        }

        @Override
        public boolean canBeEmpty() {
            return true;
        }

        @Override
        public boolean iteratorRemoveAllowed() {
            return false;
        }

        @Override
        public boolean findContractSafe() {
            return true;
        }

        @Override
        public boolean handlesLiteralTyping() {
            return true;
        }
    };

    private PrefixMapping pm;

    public UnmodifiableGraph(Graph base) {
        super(Objects.requireNonNull(base));
        pm = base.getPrefixMapping();
    }

    @Override
    public void add(Triple t) {
        throw new AddDeniedException("Read only graph: can't add triple " + t);
    }

    @Override
    public void remove(Node s, Node p, Node o) {
        GraphUtil.remove(this, s, p, o);
    }

    @Override
    public void delete(Triple t) {
        throw new DeleteDeniedException("Read only graph: can't delete triple " + t);
    }

    @Override
    public void clear() {
        throw new AccessDeniedException("Read only graph: can't clear");
    }

    @Override
    public Capabilities getCapabilities() {
        return READ_ONLY_CAPABILITIES;
    }

    @Override
    public PrefixMapping getPrefixMapping() {
        return PrefixMapping.Factory.create().setNsPrefixes(pm).lock();
    }

}
