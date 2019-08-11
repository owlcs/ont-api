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

import org.apache.jena.graph.*;
import org.semanticweb.owlapi.model.OWLObject;
import ru.avicomp.ontapi.DataFactory;
import ru.avicomp.ontapi.internal.HasObjectFactory;
import ru.avicomp.ontapi.internal.InternalObjectFactory;
import ru.avicomp.ontapi.jena.impl.PersonalityModel;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntObject;
import ru.avicomp.ontapi.owlapi.OWLObjectImpl;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A base-resource component.
 * Created by @ssz on 07.08.2019.
 *
 * @since 1.4.3
 */
@SuppressWarnings("WeakerAccess")
public abstract class ONTResourceImpl extends OWLObjectImpl implements OWLObject, HasObjectFactory, FrontsNode {
    private static final long serialVersionUID = 1142247905809986910L;

    protected final Object node;
    protected final Supplier<OntGraphModel> model;

    /**
     * Constructs the base object.
     *
     * @param n - either {@code String} (URI) or {@link BlankNodeId}, not {@code null}
     * @param m - a facility (as {@link Supplier}) to provide nonnull {@link OntGraphModel}, not {@code null}
     */
    protected ONTResourceImpl(Object n, Supplier<OntGraphModel> m) {
        this.node = Objects.requireNonNull(n);
        this.model = Objects.requireNonNull(m);
    }

    protected BlankNodeId getBlankNodeId() {
        throw new IllegalStateException();
    }

    protected String getURI() {
        throw new IllegalStateException();
    }

    @Override
    public InternalObjectFactory getObjectFactory() {
        return HasObjectFactory.getObjectFactory(model.get());
    }

    protected DataFactory getDataFactory() {
        return getObjectFactory().getOWLDataFactory();
    }

    protected PersonalityModel getPersonalityModel() {
        return PersonalityModel.asPersonalityModel(model.get());
    }

    @Override
    public abstract Node asNode();

    public abstract OntObject asResource();

    protected <X extends OntObject> X as(Class<X> type) {
        return getPersonalityModel().getNodeAs(asNode(), type);
    }

    public Stream<Triple> triples() {
        return asResource().spec().map(FrontsTriple::asTriple);
    }

    protected boolean sameAs(ONTResourceImpl other) {
        if (notSame(other)) {
            return false;
        }
        return node.equals(other.node);
    }

}
