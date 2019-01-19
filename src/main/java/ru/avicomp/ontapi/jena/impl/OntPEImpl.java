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

package ru.avicomp.ontapi.jena.impl;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.util.iterator.ExtendedIterator;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.impl.conf.ObjectFactory;
import ru.avicomp.ontapi.jena.impl.conf.OntFilter;
import ru.avicomp.ontapi.jena.impl.conf.OntFinder;
import ru.avicomp.ontapi.jena.model.*;
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
                if (PersonalityModel.canAs(OntNOP.class, res.next().getObject(), g)) return true;
            }
        } finally {
            res.close();
        }
        return false;
    };

    public static ObjectFactory inversePropertyFactory = Factories.createCommon(OntOPEImpl.InversePropertyImpl.class,
            new OntFinder.ByPredicate(OWL.inverseOf), INVERSE_OF_FILTER);

    // todo: make one more view for named properties
    public static ObjectFactory abstractNamedPropertyFactory = Factories.createFrom(OntFinder.TYPED,
            OntNOP.class, OntNDP.class, OntNAP.class);

    public static ObjectFactory abstractOPEFactory = Factories.createFrom(OntFinder.TYPED
            , OntNOP.class, OntOPE.Inverse.class);

    public static ObjectFactory abstractDOPFactory = Factories.createFrom(OntFinder.ANY_SUBJECT
            , OntNDP.class, OntOPE.class);

    public static ObjectFactory abstractPEFactory = Factories.createFrom(OntFinder.ANY_SUBJECT,
            OntNOP.class, OntNDP.class, OntNAP.class, OntOPE.Inverse.class);

    public OntPEImpl(Node n, EnhGraph m) {
        super(n, m);
    }

    @Override
    public Property asProperty() {
        if (!isURIResource()) throw new OntJenaException.IllegalState();
        return as(Property.class);
    }

}
