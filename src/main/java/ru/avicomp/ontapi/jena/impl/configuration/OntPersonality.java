/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2017, Avicomp Services, AO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package ru.avicomp.ontapi.jena.impl.configuration;

import org.apache.jena.enhanced.Implementation;
import org.apache.jena.enhanced.Personality;
import org.apache.jena.rdf.model.RDFNode;

import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.model.OntObject;

/**
 * Personality (mappings from [interface] Class objects of RDFNode to {@link Implementation} factories)
 * <p>
 * Created by @szuev on 10.11.2016.
 */
public class OntPersonality extends Personality<RDFNode> {

    public OntPersonality(Personality<RDFNode> other) {
        super(other);
    }

    /**
     * registers new OntObject if needed
     *
     * @param view    Interface (OntObject)
     * @param factory Factory to crete object
     */
    public OntPersonality register(Class<? extends OntObject> view, OntObjectFactory factory) {
        return (OntPersonality) super.add(OntJenaException.notNull(view, "Null view."), OntJenaException.notNull(factory, "Null factory."));
    }

    /**
     * removes factory.
     *
     * @param view Interface (OntObject)
     */
    public void unregister(Class<? extends OntObject> view) {
        getMap().remove(view);
    }

    /**
     * gets factory for OntObject
     *
     * @param view Interface (OntObject)
     * @return {@link OntObjectFactory} factory.
     */
    public OntObjectFactory getOntImplementation(Class<? extends OntObject> view) {
        return (OntObjectFactory) OntJenaException.notNull(getImplementation(view), "Can't find factory for object " + view);
    }


    @Override
    public OntPersonality copy() {
        return new OntPersonality(this);
    }
}
