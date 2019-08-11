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

package ru.avicomp.ontapi.internal.objects;

import org.apache.jena.graph.BlankNodeId;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.NNF;
import ru.avicomp.ontapi.DataFactory;
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.internal.InternalObjectFactory;
import ru.avicomp.ontapi.internal.ONTObject;
import ru.avicomp.ontapi.jena.model.OntCE;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.utils.OntModels;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Created by @ssz on 10.08.2019.
 *
 * @see ru.avicomp.ontapi.owlapi.objects.ce.OWLAnonymousClassExpressionImpl
 * @see ru.avicomp.ontapi.internal.ReadHelper#calcClassExpression(OntCE, InternalObjectFactory, Set)
 * @since 1.4.3
 */
@SuppressWarnings("WeakerAccess")
public abstract class ONTAnonymousClassExpressionImpl<X extends OWLAnonymousClassExpression>
        extends ONTResourceImpl
        implements OWLAnonymousClassExpression, ONTObject<X> {

    protected ONTAnonymousClassExpressionImpl(BlankNodeId n, Supplier<OntGraphModel> m) {
        super(n, m);
    }

    public static ONTAnonymousClassExpressionImpl create(OntCE ce, Supplier<OntGraphModel> model) {
        Class<? extends OntCE> type = OntModels.getOntType(ce);
        BlankNodeId id = ce.asNode().getBlankNodeId();
        return create(id, type, model);
    }

    public static ONTAnonymousClassExpressionImpl create(BlankNodeId id,
                                                         Class<? extends OntCE> type,
                                                         Supplier<OntGraphModel> model) {
        if (OntCE.ObjectSomeValuesFrom.class == type) {
            return new OSVF(id, model);
        }
        if (OntCE.DataSomeValuesFrom.class == type) {
            return new DSVF(id, model);
        }
        if (OntCE.ObjectAllValuesFrom.class == type) {
            return new OAVF(id, model);
        }
        if (OntCE.DataAllValuesFrom.class == type) {
            return new DAVF(id, model);
        }
        if (OntCE.ObjectHasValue.class == type) {
            return new OHV(id, model);
        }
        if (OntCE.DataHasValue.class == type) {
            return new DHV(id, model);
        }
        if (OntCE.HasSelf.class == type) {
            return new HS(id, model);
        }
        if (OntCE.ObjectCardinality.class == type) {
            return new OEC(id, model);
        }
        if (OntCE.DataCardinality.class == type) {
            return new DEC(id, model);
        }

        // TODO:
        if (OntCE.NaryDataSomeValuesFrom.class == type) {
            return new NDSVF(id, model);
        }
        if (OntCE.NaryDataAllValuesFrom.class == type) {
            return new NDAVF(id, model);
        }
        throw new OntApiException.IllegalState();
    }

    @Override
    protected BlankNodeId getBlankNodeId() {
        return (BlankNodeId) node;
    }

    @Override
    public Node asNode() {
        return NodeFactory.createBlankNode(getBlankNodeId());
    }

    @Override
    public abstract OntCE asResource();

    @SuppressWarnings("unchecked")
    @Override
    public X getObject() {
        return (X) this;
    }

    @Override
    public boolean isOWLThing() {
        return false;
    }

    @Override
    public boolean isOWLNothing() {
        return false;
    }

    @Override
    public OWLClassExpression getNNF() {
        return accept(getNNFClassVisitor());
    }

    @Override
    public OWLClassExpression getComplementNNF() {
        return getObjectComplementOf().accept(getNNFClassVisitor());
    }

    protected OWLClassExpressionVisitorEx<OWLClassExpression> getNNFClassVisitor() {
        return new NNF(getDataFactory()).getClassVisitor();
    }

    @Override
    public OWLObjectComplementOf getObjectComplementOf() {
        return getDataFactory().getOWLObjectComplementOf(this);
    }

    @Override
    public Set<OWLClassExpression> asConjunctSet() {
        return createSet(this);
    }

    @Override
    public Stream<OWLClassExpression> conjunctSet() {
        return Stream.of(this);
    }

    @Override
    public boolean containsConjunct(@Nonnull OWLClassExpression ce) {
        return equals(ce);
    }

    @Override
    public Set<OWLClassExpression> asDisjunctSet() {
        return createSet(this);
    }

    @Override
    public Stream<OWLClassExpression> disjunctSet() {
        return Stream.of(this);
    }

    @Override
    protected Set<OWLAnnotationProperty> getAnnotationPropertySet() {
        return createSet();
    }

    @Override
    public boolean containsEntityInSignature(OWLEntity entity) {
        if (entity.isOWLAnnotationProperty()) return false;
        return super.containsEntityInSignature(entity);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof OWLAnonymousClassExpression)) {
            return false;
        }
        OWLAnonymousClassExpression clazz = (OWLAnonymousClassExpression) obj;
        if (typeIndex() != clazz.typeIndex()) {
            return false;
        }
        if (clazz instanceof ONTResourceImpl) {
            ONTResourceImpl other = (ONTResourceImpl) clazz;
            if (notSame(other)) {
                return false;
            }
            if (node.equals(other.node)) {
                return true;
            }
        }
        if (hashCode() != clazz.hashCode()) {
            return false;
        }
        return equalIterators(components().iterator(), clazz.components().iterator());
    }

    /**
     * @see ru.avicomp.ontapi.owlapi.objects.ce.OWLObjectSomeValuesFromImpl
     * @see OntCE.ObjectSomeValuesFrom
     */
    public static class OSVF
            extends ONTAnonymousClassExpressionImpl<OWLObjectSomeValuesFrom>
            implements OWLObjectSomeValuesFrom {
        private static final long serialVersionUID = -7271347077920525051L;

        public OSVF(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        @Override
        public OntCE.ObjectSomeValuesFrom asResource() {
            return as(OntCE.ObjectSomeValuesFrom.class);
        }

        @Override
        public OWLObjectPropertyExpression getProperty() {
            return getObjectFactory().get(asResource().getProperty()).getObject();
        }

        @Override
        public OWLClassExpression getFiller() {
            return getObjectFactory().get(asResource().getValue()).getObject();
        }
    }

    /**
     * @see ru.avicomp.ontapi.owlapi.objects.ce.OWLObjectAllValuesFromImpl
     * @see OntCE.ObjectAllValuesFrom
     */
    public static class OAVF
            extends ONTAnonymousClassExpressionImpl<OWLObjectAllValuesFrom>
            implements OWLObjectAllValuesFrom {
        private static final long serialVersionUID = 7344292630429705478L;

        public OAVF(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        @Override
        public OntCE.ObjectAllValuesFrom asResource() {
            return as(OntCE.ObjectAllValuesFrom.class);
        }

        @Override
        public OWLObjectPropertyExpression getProperty() {
            return getObjectFactory().get(asResource().getProperty()).getObject();
        }

        @Override
        public OWLClassExpression getFiller() {
            return getObjectFactory().get(asResource().getValue()).getObject();
        }
    }

    /**
     * @see ru.avicomp.ontapi.owlapi.objects.ce.OWLDataSomeValuesFromImpl
     * @see OntCE.DataSomeValuesFrom
     */
    public static class DSVF
            extends ONTAnonymousClassExpressionImpl<OWLDataSomeValuesFrom>
            implements OWLDataSomeValuesFrom {
        private static final long serialVersionUID = -8615372515358861729L;

        public DSVF(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        @Override
        public OntCE.DataSomeValuesFrom asResource() {
            return as(OntCE.DataSomeValuesFrom.class);
        }

        @Override
        public OWLDataProperty getProperty() {
            return getObjectFactory().get(asResource().getProperty()).getObject();
        }

        @Override
        public OWLDataRange getFiller() {
            return getObjectFactory().get(asResource().getValue()).getObject();
        }
    }

    /**
     * @see ru.avicomp.ontapi.owlapi.objects.ce.OWLDataAllValuesFromImpl
     * @see OntCE.DataAllValuesFrom
     */
    public static class DAVF
            extends ONTAnonymousClassExpressionImpl<OWLDataAllValuesFrom>
            implements OWLDataAllValuesFrom {
        private static final long serialVersionUID = -2318746695264624081L;

        public DAVF(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        @Override
        public OntCE.DataAllValuesFrom asResource() {
            return as(OntCE.DataAllValuesFrom.class);
        }

        @Override
        public OWLDataProperty getProperty() {
            return getObjectFactory().get(asResource().getProperty()).getObject();
        }

        @Override
        public OWLDataRange getFiller() {
            return getObjectFactory().get(asResource().getValue()).getObject();
        }
    }

    /**
     * @see ru.avicomp.ontapi.owlapi.objects.ce.OWLDataSomeValuesFromImpl
     * @see OntCE.NaryDataSomeValuesFrom
     */
    public static class NDSVF
            extends ONTAnonymousClassExpressionImpl<OWLDataSomeValuesFrom>
            implements OWLDataSomeValuesFrom {

        private static final long serialVersionUID = -5138385410045585956L;

        public NDSVF(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        @Override
        public OntCE.NaryDataSomeValuesFrom asResource() {
            return as(OntCE.NaryDataSomeValuesFrom.class);
        }

        @Override
        public OWLDataProperty getProperty() {
            return getObjectFactory().get(asResource().getProperty()).getObject();
        }

        @Override
        public OWLDataRange getFiller() {
            return getObjectFactory().get(asResource().getValue()).getObject();
        }
    }

    /**
     * @see ru.avicomp.ontapi.owlapi.objects.ce.OWLDataAllValuesFromImpl
     * @see OntCE.NaryDataAllValuesFrom
     */
    public static class NDAVF
            extends ONTAnonymousClassExpressionImpl<OWLDataAllValuesFrom>
            implements OWLDataAllValuesFrom {

        private static final long serialVersionUID = -8590945215118716553L;

        public NDAVF(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        @Override
        public OntCE.NaryDataAllValuesFrom asResource() {
            return as(OntCE.NaryDataAllValuesFrom.class);
        }

        @Override
        public OWLDataProperty getProperty() {
            return getObjectFactory().get(asResource().getProperty()).getObject();
        }

        @Override
        public OWLDataRange getFiller() {
            return getObjectFactory().get(asResource().getValue()).getObject();
        }
    }

    /**
     * @see ru.avicomp.ontapi.owlapi.objects.ce.OWLObjectHasValueImpl
     * @see OntCE.ObjectHasValue
     */
    public static class OHV
            extends ONTAnonymousClassExpressionImpl<OWLObjectHasValue>
            implements OWLObjectHasValue {
        private static final long serialVersionUID = -5124100732332466223L;

        protected OHV(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        @Override
        public OntCE.ObjectHasValue asResource() {
            return as(OntCE.ObjectHasValue.class);
        }

        @Override
        public OWLObjectSomeValuesFrom asSomeValuesFrom() {
            DataFactory df = getDataFactory();
            return df.getOWLObjectSomeValuesFrom(getProperty(), df.getOWLObjectOneOf(getFiller()));
        }

        @Override
        public OWLIndividual getFiller() {
            return getObjectFactory().get(asResource().getValue()).getObject();
        }

        @Override
        public OWLObjectPropertyExpression getProperty() {
            return getObjectFactory().get(asResource().getProperty()).getObject();
        }
    }

    /**
     * @see ru.avicomp.ontapi.owlapi.objects.ce.OWLDataHasValueImpl
     * @see OntCE.DataHasValue
     */
    public static class DHV
            extends ONTAnonymousClassExpressionImpl<OWLDataHasValue>
            implements OWLDataHasValue {

        private static final long serialVersionUID = 3348946819425114289L;

        protected DHV(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        @Override
        public OntCE.DataHasValue asResource() {
            return as(OntCE.DataHasValue.class);
        }

        @Override
        public OWLDataSomeValuesFrom asSomeValuesFrom() {
            DataFactory df = getDataFactory();
            return df.getOWLDataSomeValuesFrom(getProperty(), df.getOWLDataOneOf(getFiller()));
        }

        @Override
        public OWLLiteral getFiller() {
            return getObjectFactory().get(asResource().getValue()).getObject();
        }

        @Override
        public OWLDataProperty getProperty() {
            return getObjectFactory().get(asResource().getProperty()).getObject();
        }
    }

    /**
     * @see ru.avicomp.ontapi.owlapi.objects.ce.OWLObjectHasSelfImpl
     * @see OntCE.HasSelf
     */
    public static class HS
            extends ONTAnonymousClassExpressionImpl<OWLObjectHasSelf>
            implements OWLObjectHasSelf {
        private static final long serialVersionUID = 469627800250482327L;

        protected HS(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        @Override
        public OntCE.HasSelf asResource() {
            return as(OntCE.HasSelf.class);
        }

        @Override
        public OWLObjectPropertyExpression getProperty() {
            return getObjectFactory().get(asResource().getProperty()).getObject();
        }
    }

    /**
     * @see ru.avicomp.ontapi.owlapi.objects.ce.OWLObjectExactCardinalityImpl
     * @see OntCE.ObjectCardinality
     */
    public static class OEC
            extends ONTAnonymousClassExpressionImpl<OWLObjectExactCardinality>
            implements OWLObjectExactCardinality {
        private static final long serialVersionUID = 3113352123576486865L;

        public OEC(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        @Override
        public OntCE.ObjectCardinality asResource() {
            return as(OntCE.ObjectCardinality.class);
        }

        @Override
        public OWLObjectIntersectionOf asIntersectionOfMinMax() {
            DataFactory df = getDataFactory();
            OWLObjectPropertyExpression p = getProperty();
            OWLClassExpression c = getFiller();
            int q = getCardinality();
            return df.getOWLObjectIntersectionOf(df.getOWLObjectMinCardinality(q, p, c),
                    df.getOWLObjectMaxCardinality(q, p, c));
        }

        @Override
        public int getCardinality() {
            return asResource().getCardinality();
        }

        @Override
        public OWLClassExpression getFiller() {
            return getObjectFactory().get(asResource().getValue()).getObject();
        }

        @Override
        public OWLObjectPropertyExpression getProperty() {
            return getObjectFactory().get(asResource().getProperty()).getObject();
        }
    }

    /**
     * @see ru.avicomp.ontapi.owlapi.objects.ce.OWLDataExactCardinalityImpl
     * @see OntCE.DataCardinality
     */
    public static class DEC
            extends ONTAnonymousClassExpressionImpl<OWLDataExactCardinality>
            implements OWLDataExactCardinality {
        private static final long serialVersionUID = 4196959890521876624L;

        public DEC(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        @Override
        public OntCE.DataCardinality asResource() {
            return as(OntCE.DataCardinality.class);
        }

        @Override
        public OWLObjectIntersectionOf asIntersectionOfMinMax() {
            DataFactory df = getDataFactory();
            OWLDataProperty p = getProperty();
            OWLDataRange c = getFiller();
            int q = getCardinality();
            return df.getOWLObjectIntersectionOf(df.getOWLDataMinCardinality(q, p, c),
                    df.getOWLDataMaxCardinality(q, p, c));
        }

        @Override
        public int getCardinality() {
            return asResource().getCardinality();
        }

        @Override
        public OWLDataRange getFiller() {
            return getObjectFactory().get(asResource().getValue()).getObject();
        }

        @Override
        public OWLDataProperty getProperty() {
            return getObjectFactory().get(asResource().getProperty()).getObject();
        }
    }
}
