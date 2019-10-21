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

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.util.iterator.ExtendedIterator;
import com.github.owlcs.ontapi.OntApiException;
import com.github.owlcs.ontapi.jena.OntJenaException;
import com.github.owlcs.ontapi.jena.impl.conf.ObjectFactory;
import com.github.owlcs.ontapi.jena.impl.conf.OntFilter;
import com.github.owlcs.ontapi.jena.impl.conf.OntFinder;
import com.github.owlcs.ontapi.jena.model.OntID;
import com.github.owlcs.ontapi.jena.utils.Iter;
import com.github.owlcs.ontapi.jena.vocabulary.OWL;

import java.util.stream.Stream;

/**
 * An Ontology ID Implementation.
 *
 * Created by szuev on 09.11.2016.
 */
@SuppressWarnings("WeakerAccess")
public class OntIDImpl extends OntObjectImpl implements OntID {
    public static ObjectFactory idFactory = Factories.createCommon(OntIDImpl.class,
            new OntFinder.ByType(OWL.Ontology), new OntFilter.HasType(OWL.Ontology));

    public OntIDImpl(Node n, EnhGraph m) {
        super(n, m);
    }

    @Override
    public String getVersionIRI() {
        Statement st = getProperty(OWL.versionIRI);
        if (st == null || !st.getObject().isURIResource()) return null;
        return st.getObject().asResource().getURI();
    }

    @Override
    public OntIDImpl setVersionIRI(String uri) throws OntApiException {
        if (uri != null && isAnon()) {
            throw new OntJenaException.IllegalArgument("Attempt to add version IRI (" + uri +
                    ") to anonymous ontology (" + asNode().toString() + ").");
        }
        removeAll(OWL.versionIRI);
        if (uri != null) {
            addProperty(OWL.versionIRI, getModel().createResource(uri));
        }
        return this;
    }

    @Override
    public OntIDImpl addImport(String uri) throws OntApiException {
        if (OntJenaException.notNull(uri, "Null uri specified.").equals(getURI())) {
            throw new OntJenaException.IllegalArgument("Can't import itself: " + uri);
        }
        addImportResource(getModel().createResource(uri));
        return this;
    }

    @Override
    public OntIDImpl removeImport(String uri) {
        Resource r = getModel().createResource(OntJenaException.notNull(uri, "Null uri specified."));
        removeImportResource(r);
        return this;
    }

    @Override
    public Stream<String> imports() {
        return Iter.asStream(listImportResources().mapWith(Resource::getURI), getCharacteristics());
    }

    public ExtendedIterator<Resource> listImportResources() {
        return listObjects(OWL.imports)
                .filterKeep(RDFNode::isURIResource)
                .mapWith(RDFNode::asResource);
    }

    public void addImportResource(Resource uri) {
        addProperty(OWL.imports, uri);
    }

    public void removeImportResource(Resource uri) {
        getModel().remove(this, OWL.imports, uri);
    }

    @Override
    public String toString() {
        String iri = asNode().toString();
        String ver = getVersionIRI();
        if (ver != null) {
            return iri + "(" + ver + ")";
        }
        return iri;
    }
}
