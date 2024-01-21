/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2023, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.sszuev.jena.ontapi.impl.objects;

import com.github.sszuev.jena.ontapi.OntJenaException;
import com.github.sszuev.jena.ontapi.common.BaseFactoryImpl;
import com.github.sszuev.jena.ontapi.common.Factories;
import com.github.sszuev.jena.ontapi.common.ObjectFactory;
import com.github.sszuev.jena.ontapi.common.OntFinder;
import com.github.sszuev.jena.ontapi.common.WrappedFactoryImpl;
import com.github.sszuev.jena.ontapi.model.OntAnnotationProperty;
import com.github.sszuev.jena.ontapi.model.OntClass;
import com.github.sszuev.jena.ontapi.model.OntDataProperty;
import com.github.sszuev.jena.ontapi.model.OntObjectProperty;
import com.github.sszuev.jena.ontapi.model.OntProperty;
import com.github.sszuev.jena.ontapi.model.OntRelationalProperty;
import com.github.sszuev.jena.ontapi.utils.Iterators;
import com.github.sszuev.jena.ontapi.vocabulary.OWL;
import com.github.sszuev.jena.ontapi.vocabulary.RDF;
import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.enhanced.EnhNode;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.iterator.ExtendedIterator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Property Expression base impl-class.
 * No functionality, just a collection of factories related to all OWL property-expressions.
 * <p>
 * Created by @ssz on 08.11.2016.
 */
@SuppressWarnings("WeakerAccess")
public abstract class OntPEImpl extends OntObjectImpl implements OntProperty {

    public static final OntFinder NAMED_PROPERTY_FINDER = Factories.createFinder(OWL.AnnotationProperty
            , OWL.ObjectProperty, OWL.DatatypeProperty);

    public static ObjectFactory inversePropertyFactory = createAnonymousObjectPropertyFactory();
    public static ObjectFactory abstractNamedPropertyFactory = Factories.createFrom(NAMED_PROPERTY_FINDER
            , OntObjectProperty.Named.class, OntDataProperty.class, OntAnnotationProperty.class);

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
            private final ObjectFactory named = WrappedFactoryImpl.of(OntObjectProperty.Named.class);

            @Override
            public ExtendedIterator<EnhNode> iterator(EnhGraph eg) {
                ExtendedIterator<EnhNode> res = eg.asGraph().find(Node.ANY, RDF.Nodes.type, OWL.ObjectProperty.asNode())
                        .filterKeep(t -> t.getSubject().isURI())
                        .mapWith(t -> named.createInstance(t.getSubject(), eg));
                return Iterators.concat(res, anonymous.iterator(eg));
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
                .add(OWL.ObjectProperty, OntObjectProperty.Named.class)
                .add(OWL.DatatypeProperty, OntDataProperty.class);
    }

    public static ObjectFactory createPropertyExpressionFactory() {
        return new PropertiesFactory()
                .add(OWL.ObjectProperty, OntObjectProperty.Named.class)
                .add(OWL.DatatypeProperty, OntDataProperty.class)
                .add(OWL.AnnotationProperty, OntAnnotationProperty.class);
    }

    public static ObjectFactory createAnonymousObjectPropertyFactory() {
        return new AnonymousObjectPropertyFactory();
    }

    public static Stream<OntClass> declaringClasses(OntRelationalProperty property, boolean direct) {
        Set<OntClass> domains = property.domains()
                .flatMap(clazz -> Stream.concat(Stream.of(clazz), clazz.subClasses(false)))
                .filter(OntCEImpl::isNotBuiltin)
                .collect(Collectors.toSet());
        if (domains.isEmpty()) {
            Stream<OntClass> res = property.getModel().ontObjects(OntClass.class).filter(OntCEImpl::isNotBuiltin);
            if (!direct) {
                return res;
            } else {
                return res.filter(OntClass::isHierarchyRoot);
            }
        }
        return domains.stream().filter(clazz -> clazz.hasDeclaredProperty(property, direct));
    }

    @Override
    public Property asProperty() {
        if (!isURIResource()) throw new OntJenaException.IllegalState();
        return as(Property.class);
    }

    protected static class PropertiesFactory extends HasAnonymous {
        final List<Factory> factories = new ArrayList<>();

        PropertiesFactory add(Resource declaration, Class<? extends OntProperty> type) {
            factories.add(new Factory(declaration.asNode(), type));
            return this;
        }

        @Override
        public ExtendedIterator<EnhNode> iterator(EnhGraph eg) {
            Graph g = eg.asGraph();
            ExtendedIterator<EnhNode> res = Iterators.distinct(Iterators.flatMap(Iterators.create(factories),
                    f -> g.find(Node.ANY, RDF.Nodes.type, f.nt)
                            .mapWith(t -> safeWrap(t.getSubject(), eg, f.f)).filterDrop(Objects::isNull)));
            return Iterators.concat(res, anonymous.iterator(eg));
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

        private static class Factory {
            private final Node nt;
            private final ObjectFactory f;

            private Factory(Node nodeType, Class<? extends OntProperty> classType) {
                this.nt = Objects.requireNonNull(nodeType);
                this.f = WrappedFactoryImpl.of(classType);
            }
        }
    }

    protected static abstract class HasAnonymous extends BaseFactoryImpl {
        protected final ObjectFactory anonymous = WrappedFactoryImpl.of(OntObjectProperty.Inverse.class);
    }

    public static class AnonymousObjectPropertyFactory extends BaseFactoryImpl {
        private static final Node OWL_INVERSE_OF = OWL.inverseOf.asNode();
        protected final ObjectFactory named = WrappedFactoryImpl.of(OntObjectProperty.Named.class);

        @Override
        public ExtendedIterator<EnhNode> iterator(EnhGraph eg) {
            return listTriples(Node.ANY, eg)
                    .filterKeep(x -> x.getSubject().isBlank())
                    .mapWith(x -> createInstance(x.getSubject(), eg));
        }

        @Override
        public boolean canWrap(Node node, EnhGraph eg) {
            return node.isBlank() && Iterators.findFirst(listTriples(node, eg)).isPresent();
        }

        @Override
        public EnhNode createInstance(Node node, EnhGraph eg) {
            return new OntOPEImpl.InversePropertyImpl(node, eg);
        }

        private ExtendedIterator<Triple> listTriples(Node node, EnhGraph eg) {
            // "_:x owl:inverseOf PN":
            return eg.asGraph().find(node, OWL_INVERSE_OF, Node.ANY).filterKeep(x -> named.canWrap(x.getObject(), eg));
        }
    }
}
