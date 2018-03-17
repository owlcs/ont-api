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

package ru.avicomp.ontapi.jena.impl;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;

import ru.avicomp.ontapi.jena.impl.configuration.*;
import ru.avicomp.ontapi.jena.vocabulary.OWL;

/**
 * Property Expression base class.
 * <p>
 * Created by @szuev on 08.11.2016.
 */
public abstract class OntPEImpl extends OntObjectImpl {

    public static Configurable<MultiOntObjectFactory> abstractNamedPropertyFactory = createMultiFactory(OntFinder.TYPED,
            Entities.OBJECT_PROPERTY, Entities.DATA_PROPERTY, Entities.ANNOTATION_PROPERTY);
    public static Configurable<OntObjectFactory> inversePropertyFactory = m -> new CommonOntObjectFactory(
            new OntMaker.Default(OntOPEImpl.InversePropertyImpl.class),
            new OntFinder.ByPredicate(OWL.inverseOf),
            OntOPEImpl.InversePropertyImpl.FILTER.get(m));

    public static Configurable<MultiOntObjectFactory> abstractOPEFactory = createMultiFactory(OntFinder.TYPED,
            Entities.OBJECT_PROPERTY, inversePropertyFactory);
    public static Configurable<MultiOntObjectFactory> abstractPEFactory =
            createMultiFactory(OntFinder.ANY_SUBJECT, abstractNamedPropertyFactory, inversePropertyFactory);

    public OntPEImpl(Node n, EnhGraph m) {
        super(n, m);
    }
}
