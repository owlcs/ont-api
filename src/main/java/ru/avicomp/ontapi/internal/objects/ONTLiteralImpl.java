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

import org.apache.jena.graph.FrontsTriple;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.impl.LiteralLabel;
import org.apache.jena.rdf.model.Literal;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLLiteral;
import ru.avicomp.ontapi.internal.HasObjectFactory;
import ru.avicomp.ontapi.internal.InternalObjectFactory;
import ru.avicomp.ontapi.internal.ONTObject;
import ru.avicomp.ontapi.jena.impl.PersonalityModel;
import ru.avicomp.ontapi.jena.model.OntDT;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.owlapi.objects.OWLLiteralImpl;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * An implementation of {@link OWLLiteral} that is also an {@link ONTObject}.
 * Created by @ssz on 07.08.2019.
 *
 * @see OWLLiteralImpl
 * @since 1.4.3
 */
@SuppressWarnings("WeakerAccess")
public class ONTLiteralImpl extends OWLLiteralImpl implements OWLLiteral, ONTObject<OWLLiteral>, HasObjectFactory {
    private static final long serialVersionUID = 3145008815239693328L;

    protected final OntGraphModel model;

    public ONTLiteralImpl(LiteralLabel n, OntGraphModel m) {
        super(n);
        this.model = Objects.requireNonNull(m);
    }

    @Override
    public InternalObjectFactory getObjectFactory() {
        return HasObjectFactory.getObjectFactory(model);
    }

    /**
     * Returns a {@link OntDT} instance that corresponds datatype.
     *
     * @return {@link OntDT}
     * @see OWLLiteralImpl#calcOWLDatatype()
     * @see OntGraphModel#getDatatype(Literal)
     */
    public OntDT getDatatypeResource() {
        return PersonalityModel.asPersonalityModel(model)
                .getNodeAs(NodeFactory.createURI(getDatatypeURI(label)), OntDT.class);
    }

    @Override
    public OWLDatatype getDatatype() {
        return getObjectFactory().get(getDatatypeResource()).getObject();
    }

    @Override
    public OWLLiteral getObject() {
        return this;
    }

    @Override
    public Stream<Triple> triples() {
        OntDT res = getDatatypeResource();
        return res.isBuiltIn() ? Stream.empty() : res.spec().map(FrontsTriple::asTriple);
    }
}
