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

package com.github.owlcs.ontapi.internal.objects;

import com.github.owlcs.ontapi.internal.ONTObject;
import com.github.owlcs.ontapi.owlapi.objects.swrl.VariableImpl;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.ontapi.model.OntModel;
import org.apache.jena.ontapi.model.OntSWRL;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.SWRLVariable;

import java.util.Set;
import java.util.function.Supplier;

/**
 * A {@link SWRLVariable} implementation that is also {@link ONTObject}.
 * Created by @ssz on 21.08.2019.
 *
 * @see VariableImpl
 * @see OntSWRL.Variable
 * @since 2.0.0
 */
public class ONTSWRLVariable extends ONTResourceImpl
        implements SWRLVariable, ONTSimple, ModelObject<SWRLVariable> {

    public ONTSWRLVariable(String uri, Supplier<OntModel> m) {
        super(uri, m);
    }

    @Override
    public String getURI() {
        return (String) node;
    }

    @Override
    public IRI getIRI() {
        return getObjectFactory().toIRI(getURI());
    }

    @Override
    public SWRLVariable getOWLObject() {
        return this;
    }

    @Override
    public Node asNode() {
        return NodeFactory.createURI(getURI());
    }

    @Override
    public OntSWRL.Variable asRDFNode() {
        return as(OntSWRL.Variable.class);
    }

    @Override
    public SWRLVariable eraseModel() {
        return getDataFactory().getSWRLVariable(getIRI());
    }

    @Override
    public boolean containsEntity(OWLEntity entity) {
        return false;
    }

    @Override
    public Set<OWLEntity> getSignatureSet() {
        return createSet();
    }

    @Override
    public Set<OWLDatatype> getDatatypeSet() {
        return createSet();
    }

    @Override
    public Set<OWLClass> getNamedClassSet() {
        return createSet();
    }

    @Override
    public Set<OWLNamedIndividual> getNamedIndividualSet() {
        return createSet();
    }

    @Override
    public Set<OWLDataProperty> getDataPropertySet() {
        return createSet();
    }

    @Override
    public Set<OWLObjectProperty> getObjectPropertySet() {
        return createSet();
    }

    @Override
    public Set<OWLAnnotationProperty> getAnnotationPropertySet() {
        return createSet();
    }

    @Override
    public Set<OWLClassExpression> getClassExpressionSet() {
        return createSet();
    }

    @Override
    public Set<OWLAnonymousIndividual> getAnonymousIndividualSet() {
        return createSet();
    }
}
