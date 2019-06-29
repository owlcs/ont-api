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
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.impl.conf.BaseFactoryImpl;
import ru.avicomp.ontapi.jena.impl.conf.ObjectFactory;
import ru.avicomp.ontapi.jena.impl.conf.OntFinder;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.utils.Iter;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static ru.avicomp.ontapi.jena.impl.WrappedFactoryImpl.of;

/**
 * Property Expression base impl-class.
 * No functionality, just a collection of factories related to all OWL property-expressions.
 * <p>
 * Created by @szuev on 08.11.2016.
 */
@SuppressWarnings("WeakerAccess")
public abstract class OntPEImpl extends OntObjectImpl implements OntPE {

    public static final OntFinder NAMED_PROPERTY_FINDER = Factories.createFinder(OWL.AnnotationProperty
            , OWL.ObjectProperty, OWL.DatatypeProperty);

    public static ObjectFactory inversePropertyFactory = createAnonymousObjectPropertyFactory();
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

    public static ObjectFactory createObjectPropertyExpressionFactory() {
        return new HasAnonymous() {
            private final ObjectFactory named = of(OntNOP.class);

            @Override
            public ExtendedIterator<EnhNode> iterator(EnhGraph eg) {
                ExtendedIterator<EnhNode> res = eg.asGraph().find(Node.ANY, RDF.Nodes.type, OWL.ObjectProperty.asNode())
                        .filterKeep(t -> t.getSubject().isURI())
                        .mapWith(t -> named.createInstance(t.getSubject(), eg));
                return Iter.concat(res, anonymous.iterator(eg));
            }

            @Override
            public boolean canWrap(Node node, EnhGraph eg) {
                if (node.isURI()) {
                    return named.canWrap(node, eg);
                }
                return anonymous.canWrap(node, eg);
            }

            @Override
            public EnhNode createInstance(Node node, EnhGraph eg) {
                if (node.isURI()) {
                    return named.createInstance(node, eg);
                }
                return anonymous.createInstance(node, eg);
            }

            @Override
            public EnhNode wrap(Node node, EnhGraph eg) {
                if (node.isURI())
                    return named.wrap(node, eg);
                if (node.isBlank())
                    return anonymous.wrap(node, eg);
                throw new OntJenaException.Conversion("Can't convert node " + node + " to Object Property Expression.");
            }
        };
    }

    public static ObjectFactory createDataOrObjectPropertyFactory() {
        return new PropertiesFactory()
                .add(OWL.ObjectProperty, OntNOP.class)
                .add(OWL.DatatypeProperty, OntNDP.class);
    }

    public static ObjectFactory createPropertyExpressionFactory() {
        return new PropertiesFactory()
                .add(OWL.ObjectProperty, OntNOP.class)
                .add(OWL.DatatypeProperty, OntNDP.class)
                .add(OWL.AnnotationProperty, OntNAP.class);
    }

    public static ObjectFactory createAnonymousObjectPropertyFactory() {
        return new AnonymousObjectPropertyFactory();
    }

    @Override
    public Property asProperty() {
        if (!isURIResource()) throw new OntJenaException.IllegalState();
        return as(Property.class);
    }

    protected static class PropertiesFactory extends HasAnonymous {
        final List<Factory> factories = new ArrayList<>();

        PropertiesFactory add(Resource declaration, Class<? extends OntPE> type) {
            factories.add(new Factory(declaration.asNode(), type));
            return this;
        }

        @Override
        public ExtendedIterator<EnhNode> iterator(EnhGraph eg) {
            Graph g = eg.asGraph();
            ExtendedIterator<EnhNode> res = Iter.distinct(Iter.flatMap(Iter.create(factories),
                    f -> g.find(Node.ANY, RDF.Nodes.type, f.nt)
                            .mapWith(t -> safeWrap(t.getSubject(), eg, f.f)).filterDrop(Objects::isNull)));
            return Iter.concat(res, anonymous.iterator(eg));
        }

        @Override
        public boolean canWrap(Node node, EnhGraph eg) {
            if (!node.isURI()) {
                return anonymous.canWrap(node, eg);
            }
            for (Factory f : factories) {
                if (f.f.canWrap(node, eg)) return true;
            }
            return false;
        }

        @Override
        public EnhNode createInstance(Node node, EnhGraph eg) {
            if (!node.isURI()) {
                return anonymous.createInstance(node, eg);
            }
            for (Factory f : factories) {
                EnhNode res = safeWrap(node, eg, f.f);
                if (res != null) return res;
            }
            return null;
        }

        @Override
        public EnhNode wrap(Node node, EnhGraph eg) {
            if (node.isBlank())
                return anonymous.wrap(node, eg);
            OntJenaException.Conversion ex = new OntJenaException.Conversion("Can't convert node " +
                    node + " to Property Expression");
            if (!node.isURI())
                throw ex;
            for (Factory f : factories) {
                try {
                    return f.f.wrap(node, eg);
                } catch (OntJenaException.Conversion c) {
                    ex.addSuppressed(c);
                }
            }
            throw ex;
        }

        private class Factory {
            private final Node nt;
            private final ObjectFactory f;

            private Factory(Node nodeType, Class<? extends OntPE> classType) {
                this.nt = Objects.requireNonNull(nodeType);
                this.f = of(classType);
            }
        }
    }

    protected static abstract class HasAnonymous extends BaseFactoryImpl {
        protected final ObjectFactory anonymous = of(OntOPE.Inverse.class);
    }

    public static class AnonymousObjectPropertyFactory extends BaseFactoryImpl {
        private static final Node OWL_INVERSE_OF = OWL.inverseOf.asNode();
        protected final ObjectFactory named = of(OntNOP.class);

        @Override
        public ExtendedIterator<EnhNode> iterator(EnhGraph eg) {
            return triples(Node.ANY, eg)
                    .filterKeep(x -> x.getSubject().isBlank())
                    .mapWith(x -> createInstance(x.getSubject(), eg));
        }

        @Override
        public boolean canWrap(Node node, EnhGraph eg) {
            return node.isBlank() && Iter.findFirst(triples(node, eg)).isPresent();
        }

        @Override
        public EnhNode createInstance(Node node, EnhGraph eg) {
            return new OntOPEImpl.InversePropertyImpl(node, eg);
        }

        private ExtendedIterator<Triple> triples(Node node, EnhGraph eg) {
            // "_:x owl:inverseOf PN":
            return eg.asGraph().find(node, OWL_INVERSE_OF, Node.ANY).filterKeep(x -> named.canWrap(x.getObject(), eg));
        }
    }
}
