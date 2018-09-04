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

package ru.avicomp.ontapi.jena.impl;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.util.iterator.ExtendedIterator;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.impl.conf.*;
import ru.avicomp.ontapi.jena.model.OntNOP;
import ru.avicomp.ontapi.jena.model.OntPE;
import ru.avicomp.ontapi.jena.vocabulary.OWL;

/**
 * Property Expression base impl-class.
 * No functionality, just a collection of factories related to all OWL property-expressions.
 * <p>
 * Created by @szuev on 08.11.2016.
 */
@SuppressWarnings("WeakerAccess")
public abstract class OntPEImpl extends OntObjectImpl implements OntPE {

    public static final OntFilter INVERSE_OF_FILTER = (n, g) -> {
        if (!n.isBlank()) return false;
        ExtendedIterator<Triple> res = g.asGraph().find(n, OWL.inverseOf.asNode(), Node.ANY);
        try {
            while (res.hasNext()) {
                if (OntObjectImpl.canAs(OntNOP.class, res.next().getObject(), g)) return true;
            }
        } finally {
            res.close();
        }
        return false;
    };

    public static OntObjectFactory inversePropertyFactory = new CommonOntObjectFactory(new OntMaker.Default(OntOPEImpl.InversePropertyImpl.class),
            new OntFinder.ByPredicate(OWL.inverseOf), INVERSE_OF_FILTER);

    public static Configurable<OntObjectFactory> abstractNamedPropertyFactory = concatFactories(OntFinder.TYPED,
            Entities.OBJECT_PROPERTY, Entities.DATA_PROPERTY, Entities.ANNOTATION_PROPERTY);

    public static Configurable<OntObjectFactory> abstractOPEFactory = buildMultiFactory(OntFinder.TYPED, null,
            Entities.OBJECT_PROPERTY, inversePropertyFactory);

    @SuppressWarnings("unchecked")
    public static Configurable<OntObjectFactory> abstractDOPFactory =
            concatFactories(OntFinder.ANY_SUBJECT, Entities.DATA_PROPERTY, abstractOPEFactory);

    public static Configurable<OntObjectFactory> abstractPEFactory =
            buildMultiFactory(OntFinder.ANY_SUBJECT, null, abstractNamedPropertyFactory, inversePropertyFactory);

    public OntPEImpl(Node n, EnhGraph m) {
        super(n, m);
    }

    @Override
    public Property asProperty() {
        if (!isURIResource()) throw new OntJenaException.IllegalState();
        return as(Property.class);
    }

}
