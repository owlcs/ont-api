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

import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.internal.ONTObject;
import ru.avicomp.ontapi.jena.model.OntDT;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.vocabulary.RDF;
import ru.avicomp.ontapi.jena.vocabulary.XSD;

import java.util.Set;
import java.util.function.Supplier;

/**
 * An {@link OWLDatatype} implementation that is also {@link ONTObject}.
 * Created by @ssz on 09.08.2019.
 *
 * @see ru.avicomp.ontapi.owlapi.objects.entity.OWLDatatypeImpl
 * @see ru.avicomp.ontapi.owlapi.objects.entity.OWLBuiltinDatatypeImpl
 * @since 1.4.3
 */
public class ONTDatatypeImpl extends ONTEntityImpl implements OWLDatatype, ONTObject<OWLDatatype> {

    public ONTDatatypeImpl(String uri, Supplier<OntGraphModel> m) {
        super(uri, m);
    }

    @Override
    public OntDT asRDFNode() {
        return as(OntDT.class);
    }

    @Override
    public OWLDatatype getOWLObject() {
        return this;
    }

    @Override
    protected Set<OWLDatatype> getDatatypeSet() {
        return createSet(this);
    }

    @Override
    public OWL2Datatype getBuiltInDatatype() {
        if (!isBuiltIn()) {
            throw new OntApiException(this + " is not a built in datatype.");
        }
        return OWL2Datatype.getDatatype(getIRI());
    }

    @Override
    public boolean isString() {
        return equals(XSD.xstring);
    }

    @Override
    public boolean isInteger() {
        return equals(XSD.integer);
    }

    @Override
    public boolean isFloat() {
        return equals(XSD.xfloat);
    }

    @Override
    public boolean isDouble() {
        return equals(XSD.xdouble);
    }

    @Override
    public boolean isBoolean() {
        return equals(XSD.xboolean);
    }

    @Override
    public boolean isRDFPlainLiteral() {
        return equals(RDF.PlainLiteral);
    }

    @Override
    public boolean isTopEntity() {
        return isTopDatatype();
    }

    @Override
    public boolean isTopDatatype() {
        return equals(RDFS.Literal);
    }

    @Override
    public boolean isOWLDatatype() {
        return true;
    }

}
