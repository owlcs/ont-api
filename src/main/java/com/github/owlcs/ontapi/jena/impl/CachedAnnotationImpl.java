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

package com.github.owlcs.ontapi.jena.impl;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.iterator.ExtendedIterator;
import com.github.owlcs.ontapi.jena.OntJenaException;
import com.github.owlcs.ontapi.jena.model.OntAnnotation;
import com.github.owlcs.ontapi.jena.model.OntNAP;
import com.github.owlcs.ontapi.jena.model.OntStatement;
import com.github.owlcs.ontapi.jena.utils.Iter;

import java.util.Set;

/**
 * A read-only {@link OntAnnotationImpl Ontology Annotation Implementation} with in-memory caches.
 * <p>
 * Created by @szuev on 25.08.2018.
 *
 * @see CachedStatementImpl
 */
@SuppressWarnings("WeakerAccess")
public class CachedAnnotationImpl extends OntAnnotationImpl {

    private Set<OntStatement> assertions;
    private Set<OntAnnotation> descendants;

    public CachedAnnotationImpl(Node n, EnhGraph m) {
        super(n, m);
    }

    protected <R> R noModify() {
        throw new OntJenaException.Unsupported(this + ": modifying is not allowed.");
    }

    @Override
    public ExtendedIterator<OntStatement> listAssertions() {
        if (assertions == null) {
            assertions = super.listAssertions().mapWith(s -> (OntStatement) new CachedStatementImpl(s)).toSet();
        }
        return Iter.create(assertions);
    }

    @Override
    public ExtendedIterator<OntAnnotation> listDescendants() {
        if (descendants == null) {
            EnhGraph m = getModel();
            descendants = super.listAnnotatedSources()
                    .mapWith(s -> (OntAnnotation) new CachedAnnotationImpl(((OntStatementImpl) s).getSubjectNode(), m)).toSet();
        }
        return Iter.create(descendants);
    }

    @Override
    public OntStatement addAnnotation(OntNAP property, RDFNode value) {
        return noModify();
    }

    @Override
    public Resource addLiteral(Property p, boolean o) {
        return noModify();
    }

    @Override
    public Resource addProperty(Property p, long o) {
        return noModify();
    }

    @Override
    public Resource addLiteral(Property p, long o) {
        return noModify();
    }

    @Override
    public Resource addLiteral(Property p, char o) {
        return noModify();
    }

    @Override
    public Resource addProperty(Property p, float o) {
        return noModify();
    }

    @Override
    public Resource addProperty(Property p, double o) {
        return noModify();
    }

    @Override
    public Resource addLiteral(Property p, double o) {
        return noModify();
    }

    @Override
    public Resource addLiteral(Property p, float o) {
        return noModify();
    }

    @Override
    public Resource addProperty(Property p, String o) {
        return noModify();
    }

    @Override
    public Resource addProperty(Property p, String o, String l) {
        return noModify();
    }

    @Override
    public Resource addProperty(Property p, String lexicalForm, RDFDatatype datatype) {
        return noModify();
    }

    @Override
    public Resource addLiteral(Property p, Object o) {
        return noModify();
    }

    @Override
    public Resource addLiteral(Property p, Literal o) {
        return noModify();
    }

    @Override
    public Resource addProperty(Property p, RDFNode o) {
        return noModify();
    }

    @Override
    public Resource removeProperties() {
        return noModify();
    }

    @Override
    public Resource removeAll(Property p) {
        return noModify();
    }
}
