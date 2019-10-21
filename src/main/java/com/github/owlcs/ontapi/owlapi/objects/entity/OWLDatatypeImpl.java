/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2019, The University of Manchester, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.github.owlcs.ontapi.owlapi.objects.entity;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLRuntimeException;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import com.github.owlcs.ontapi.owlapi.OWLObjectImpl;

import java.util.Objects;
import java.util.Set;

/**
 * @author Matthew Horridge, The University Of Manchester, Bio-Health Informatics Group
 * @since 1.2.0
 */
public class OWLDatatypeImpl extends OWLObjectImpl implements OWLDatatype {

    private final IRI iri;
    private final boolean top;
    private final boolean builtin;

    /**
     * @param iri datatype iri
     */
    public OWLDatatypeImpl(IRI iri) {
        this.iri = Objects.requireNonNull(iri, "iri cannot be null");
        top = iri.equals(OWLRDFVocabulary.RDFS_LITERAL.getIRI());
        builtin = top || OWL2Datatype.isBuiltIn(iri) || iri
                .equals(OWLRDFVocabulary.RDF_PLAIN_LITERAL.getIRI());
    }

    @Override
    public boolean isTopEntity() {
        return top;
    }

    @Override
    public boolean isRDFPlainLiteral() {
        return iri.isPlainLiteral();
    }

    @Override
    public String toStringID() {
        return iri.toString();
    }

    @Override
    public IRI getIRI() {
        return iri;
    }

    @Override
    public boolean isBuiltIn() {
        return builtin;
    }

    @Override
    protected Set<OWLEntity> getSignatureSet() {
        return createSet(this);
    }

    @Override
    public Set<OWLDatatype> getDatatypeSet() {
        return createSet(this);
    }

    @Override
    public OWL2Datatype getBuiltInDatatype() {
        if (!builtin) {
            throw new OWLRuntimeException(iri + " is not a built-in datatype. " +
                    "The #getBuiltInDatatype() method should only be called on built-in datatypes.");
        }
        return OWL2Datatype.getDatatype(iri);
    }

    @Override
    public boolean isDouble() {
        return iri.equals(OWL2Datatype.XSD_DOUBLE.getIRI());
    }

    @Override
    public boolean isFloat() {
        return iri.equals(OWL2Datatype.XSD_FLOAT.getIRI());
    }

    @Override
    public boolean isInteger() {
        return iri.equals(OWL2Datatype.XSD_INTEGER.getIRI());
    }

    @Override
    public boolean isString() {
        return iri.equals(OWL2Datatype.XSD_STRING.getIRI());
    }

    @Override
    public boolean isBoolean() {
        return iri.equals(OWL2Datatype.XSD_BOOLEAN.getIRI());
    }

    @Override
    public boolean isTopDatatype() {
        return top;
    }

    @Override
    public boolean isOWLDatatype() {
        return true;
    }
}
