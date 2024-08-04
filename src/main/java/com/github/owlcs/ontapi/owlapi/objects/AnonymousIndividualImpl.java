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
package com.github.owlcs.ontapi.owlapi.objects;

import com.github.owlcs.ontapi.AsNode;
import com.github.owlcs.ontapi.BlankNodeId;
import com.github.owlcs.ontapi.owlapi.OWLObjectImpl;
import javax.annotation.Nullable;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.semanticweb.owlapi.model.NodeID;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * An ON-API implementation of {@link OWLAnonymousIndividual},
 * encapsulated {@link BlankNodeId Jena Bkank Node Id}.
 */
public class AnonymousIndividualImpl extends OWLObjectImpl implements OWLAnonymousIndividual, AsNode {

    protected final String id;

    /**
     * @param id node id, not {@code null}
     */
    public AnonymousIndividualImpl(String id) {
        this.id = Objects.requireNonNull(id, "nodeID cannot be null");
    }

    /**
     * Converts any instance of {@link OWLAnonymousIndividual} to the
     * {@link AnonymousIndividualImpl ONT-API Anonymous Individual implementation}.
     *
     * @param individual {@link OWLAnonymousIndividual}
     * @return {@link AnonymousIndividualImpl}
     */
    public static AnonymousIndividualImpl asONT(OWLAnonymousIndividual individual) {
        if (individual instanceof AnonymousIndividualImpl) {
            return (AnonymousIndividualImpl) individual;
        }
        String id;
        if (individual instanceof AsNode) {
            id = ((AsNode) individual).asNode().getBlankNodeLabel();
        } else {
            id = individual.toStringID();
        }
        return new AnonymousIndividualImpl(id);
    }

    public BlankNodeId getBlankNodeId() {
        return BlankNodeId.of(id);
    }

    @Override
    public Node asNode() {
        return NodeFactory.createBlankNode(id);
    }

    @Override
    public NodeID getID() {
        return NodeID.getNodeID(id);
    }

    @Override
    public String toStringID() {
        return getID().getID();
    }

    @Override
    public OWLAnonymousIndividual asOWLAnonymousIndividual() {
        return this;
    }

    @Override
    public Optional<OWLAnonymousIndividual> asAnonymousIndividual() {
        return Optional.of(this);
    }

    @Override
    protected Set<OWLAnonymousIndividual> getAnonymousIndividualSet() {
        return createSet();
    }

    @Override
    public boolean containsEntityInSignature(@Nullable OWLEntity entity) {
        return false;
    }

    @Override
    protected Set<OWLEntity> getSignatureSet() {
        return createSet();
    }

    @Override
    protected Set<OWLClass> getNamedClassSet() {
        return createSet();
    }

    @Override
    protected Set<OWLDatatype> getDatatypeSet() {
        return createSet();
    }

    @Override
    protected Set<OWLNamedIndividual> getNamedIndividualSet() {
        return createSet();
    }

    @Override
    protected Set<OWLDataProperty> getDataPropertySet() {
        return createSet();
    }

    @Override
    protected Set<OWLObjectProperty> getObjectPropertySet() {
        return createSet();
    }

    @Override
    protected Set<OWLAnnotationProperty> getAnnotationPropertySet() {
        return createSet();
    }

    @Override
    protected Set<OWLClassExpression> getClassExpressionSet() {
        return createSet();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof OWLAnonymousIndividual)) {
            return false;
        }
        if (obj instanceof AnonymousIndividualImpl other) {
            if (notSame(other)) {
                return false;
            }
            return id.equals(other.id);
        }
        if (obj instanceof AsNode) {
            return asNode().equals(((AsNode) obj).asNode());
        }
        return super.equals(obj);
    }
}
