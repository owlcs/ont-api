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

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.impl.LiteralLabel;
import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.internal.InternalObjectFactory;
import ru.avicomp.ontapi.internal.ModelObjectFactory;
import ru.avicomp.ontapi.internal.ONTObject;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntSWRL;

import javax.annotation.Nullable;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A {@link SWRLLiteralArgument} implementation that is also {@link ONTObject}.
 * <p>
 * Created by @ssz on 22.08.2019.
 *
 * @see ru.avicomp.ontapi.owlapi.objects.swrl.SWRLLiteralArgumentImpl
 * @see org.semanticweb.owlapi.model.SWRLDArgument
 * @see ru.avicomp.ontapi.jena.model.OntSWRL.DArg
 * @since 1.4.3
 */
@SuppressWarnings("WeakerAccess")
public class ONTSWRLLiteralImpl extends ONTResourceImpl
        implements SWRLLiteralArgument, ONTObject<SWRLLiteralArgument> {

    public ONTSWRLLiteralImpl(LiteralLabel n, Supplier<OntGraphModel> m) {
        super(n, m);
    }

    @Override
    public SWRLLiteralArgument getOWLObject() {
        return this;
    }

    @Override
    public Node asNode() {
        return NodeFactory.createLiteral(getLiteralLabel());
    }

    @Override
    public OntSWRL.DArg asRDFNode() {
        return as(OntSWRL.DArg.class);
    }

    @Override
    protected LiteralLabel getLiteralLabel() {
        return (LiteralLabel) node;
    }

    @Override
    public OWLLiteral getLiteral() {
        return getONTLiteral().getOWLObject();
    }

    public ONTObject<OWLLiteral> getONTLiteral() {
        InternalObjectFactory of = getObjectFactory();
        if (of instanceof ModelObjectFactory) {
            return ((ModelObjectFactory) of).getLiteral(getLiteralLabel());
        }
        return of.getLiteral(model.get().asRDFNode(asNode()).asLiteral());
    }

    private OWLDatatype getDatatype() {
        return getLiteral().getDatatype();
    }

    @Override
    public boolean containsEntityInSignature(@Nullable OWLEntity entity) {
        if (entity == null || !entity.isOWLDatatype()) return false;
        return getDatatype().equals(entity);
    }

    @Override
    public Stream<ONTObject<? extends OWLObject>> objects() {
        return Stream.of(getONTLiteral());
    }

    @Override
    public boolean canContainClassExpressions() {
        return false;
    }

    @Override
    public boolean canContainAnonymousIndividuals() {
        return false;
    }

    @Override
    public boolean canContainNamedClasses() {
        return false;
    }

    @Override
    public boolean canContainNamedIndividuals() {
        return false;
    }

    @Override
    public boolean canContainObjectProperties() {
        return false;
    }

    @Override
    public boolean canContainDataProperties() {
        return false;
    }

    @Override
    public boolean canContainAnnotationProperties() {
        return false;
    }
}
