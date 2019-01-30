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
import org.apache.jena.enhanced.EnhNode;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.util.iterator.WrappedIterator;
import org.apache.jena.vocabulary.RDF;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.impl.conf.BaseFactoryImpl;
import ru.avicomp.ontapi.jena.impl.conf.ObjectFactory;
import ru.avicomp.ontapi.jena.impl.conf.OntFilter;
import ru.avicomp.ontapi.jena.impl.conf.OntFinder;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.utils.Iter;
import ru.avicomp.ontapi.jena.vocabulary.OWL;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Property Expression base impl-class.
 * No functionality, just a collection of factories related to all OWL property-expressions.
 * <p>
 * Created by @szuev on 08.11.2016.
 */
@SuppressWarnings("WeakerAccess")
public abstract class OntPEImpl extends OntObjectImpl implements OntPE {

    private static final Node OWL_INVERSE_OF = OWL.inverseOf.asNode();
    private static final OntFilter INVERSE_OF_FILTER = (n, g) -> {
        if (!n.isBlank()) return false;
        ExtendedIterator<Triple> res = g.asGraph().find(n, OWL_INVERSE_OF, Node.ANY);
        try {
            while (res.hasNext()) {
                if (PersonalityModel.canAs(OntNOP.class, res.next().getObject(), g)) return true;
            }
        } finally {
            res.close();
        }
        return false;
    };
    private static final OntFinder NAMED_PROPERTY_FINDER = Factories.createFinder(OWL.AnnotationProperty
            , OWL.ObjectProperty, OWL.DatatypeProperty);

    public static ObjectFactory inversePropertyFactory = Factories.createCommon(OntOPEImpl.InversePropertyImpl.class,
            new OntFinder.ByPredicate(OWL.inverseOf), INVERSE_OF_FILTER);
    public static ObjectFactory abstractNamedPropertyFactory = Factories.createFrom(NAMED_PROPERTY_FINDER
            , OntNOP.class, OntNDP.class, OntNAP.class);

    public static ObjectFactory abstractOPEFactory = createObjectPropertyExpressionFactory();
    //Factories.createFrom(OBJECT_PROPERTY_FINDER, OntNOP.class, OntOPE.Inverse.class);

    public static ObjectFactory abstractDOPFactory = createDataOrObjectPropertyFactory();
    //Factories.createFrom(OntFinder.ANY_SUBJECT, OntNDP.class, OntOPE.class);

    public static ObjectFactory abstractPEFactory = createPropertyExpressionFactory();
    //Factories.createFrom(OntFinder.ANY_SUBJECT, OntNOP.class, OntNDP.class, OntNAP.class, OntOPE.Inverse.class);

    public OntPEImpl(Node n, EnhGraph m) {
        super(n, m);
    }

    private static ObjectFactory createObjectPropertyExpressionFactory() {
        return new WithInverseObjectPropertyFactory() {
            final ObjectFactory namedObjectProperty = new WrappedFactoryImpl(OntNOP.class);

            @Override
            public ExtendedIterator<EnhNode> iterator(EnhGraph eg) {
                ExtendedIterator<EnhNode> res = eg.asGraph().find(Node.ANY, RDF.Nodes.type, OWL.ObjectProperty.asNode())
                        .mapWith(t -> wrap(t.getSubject(), eg, namedObjectProperty));
                return Iter.concat(res, super.iterator(eg)).filterDrop(Objects::isNull);
            }

            @Override
            public boolean canWrap(Node node, EnhGraph eg) {
                if (node.isURI()) {
                    return namedObjectProperty.canWrap(node, eg);
                }
                return super.canWrap(node, eg);
            }

            @Override
            public EnhNode createInstance(Node node, EnhGraph eg) {
                if (node.isURI()) {
                    return wrap(node, eg, namedObjectProperty);
                }
                return super.createInstance(node, eg);
            }
        };
    }

    private static ObjectFactory createDataOrObjectPropertyFactory() {
        return new PropertiesFactory()
                .add(OWL.ObjectProperty, OntNOP.class)
                .add(OWL.DatatypeProperty, OntNDP.class);
    }

    private static ObjectFactory createPropertyExpressionFactory() {
        return new PropertiesFactory()
                .add(OWL.ObjectProperty, OntNOP.class)
                .add(OWL.DatatypeProperty, OntNDP.class)
                .add(OWL.AnnotationProperty, OntNAP.class);
    }

    @Override
    public Property asProperty() {
        if (!isURIResource()) throw new OntJenaException.IllegalState();
        return as(Property.class);
    }

    private static class PropertiesFactory extends WithInverseObjectPropertyFactory {
        final List<Factory> factories = new ArrayList<>();

        PropertiesFactory add(Resource declaration, Class<? extends OntPE> type) {
            factories.add(new Factory(declaration.asNode(), new WrappedFactoryImpl(type)));
            return this;
        }

        @Override
        public ExtendedIterator<EnhNode> iterator(EnhGraph eg) {
            Graph g = eg.asGraph();
            ExtendedIterator<EnhNode> res = Iter.distinct(Iter.flatMap(WrappedIterator.create(factories.iterator()),
                    f -> g.find(Node.ANY, RDF.Nodes.type, f.t).mapWith(t -> wrap(t.getSubject(), eg, f.f))));
            return Iter.concat(res, super.iterator(eg)).filterDrop(Objects::isNull);
        }

        @Override
        public boolean canWrap(Node node, EnhGraph eg) {
            if (!node.isURI()) {
                return super.canWrap(node, eg);
            }
            for (Factory f : factories) {
                if (f.f.canWrap(node, eg)) return true;
            }
            return false;
        }

        @Override
        public EnhNode createInstance(Node node, EnhGraph eg) {
            if (!node.isURI()) {
                return super.createInstance(node, eg);
            }
            for (Factory f : factories) {
                EnhNode res = wrap(node, eg, f.f);
                if (res != null) return res;
            }
            return null;
        }

        private class Factory {
            private final Node t;
            private final ObjectFactory f;

            private Factory(Node type, ObjectFactory factory) {
                this.t = Objects.requireNonNull(type);
                this.f = Objects.requireNonNull(factory);
            }
        }
    }

    private static abstract class WithInverseObjectPropertyFactory extends BaseFactoryImpl {
        final ObjectFactory anonymous = new WrappedFactoryImpl(OntOPE.Inverse.class);

        @Override
        public ExtendedIterator<EnhNode> iterator(EnhGraph eg) {
            return Iter.distinct(eg.asGraph().find(Node.ANY, OWL_INVERSE_OF, Node.ANY)
                    .mapWith(t -> wrap(t.getSubject(), eg, anonymous)));
        }

        @Override
        public boolean canWrap(Node node, EnhGraph eg) {
            if (node.isBlank()) {
                return anonymous.canWrap(node, eg);
            }
            return false;
        }

        @Override
        public EnhNode createInstance(Node node, EnhGraph eg) {
            if (node.isBlank()) {
                return wrap(node, eg, anonymous);
            }
            return null;
        }
    }
}
