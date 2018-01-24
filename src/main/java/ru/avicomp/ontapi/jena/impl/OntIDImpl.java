/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2017, Avicomp Services, AO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package ru.avicomp.ontapi.jena.impl;

import java.util.stream.Stream;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.impl.configuration.*;
import ru.avicomp.ontapi.jena.model.OntID;
import ru.avicomp.ontapi.jena.utils.Iter;
import ru.avicomp.ontapi.jena.vocabulary.OWL;

/**
 * Ontology ID
 * Created by szuev on 09.11.2016.
 */
public class OntIDImpl extends OntObjectImpl implements OntID {
    public static Configurable<OntObjectFactory> idFactory = m ->
            new CommonOntObjectFactory(new OntMaker.Default(OntIDImpl.class), new OntFinder.ByType(OWL.Ontology), new OntFilter.HasType(OWL.Ontology));

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
    public void setVersionIRI(String uri) {
        removeAll(OWL.versionIRI);
        if (uri != null) {
            addProperty(OWL.versionIRI, getModel().createResource(uri));
        }
    }

    @Override
    public void addImport(String uri) {
        if (OntJenaException.notNull(uri, "Null uri specified.").equals(getURI())) {
            throw new OntJenaException("Can't import itself: " + uri);
        }
        addImportResource(getModel().createResource(uri));
    }

    @Override
    public void removeImport(String uri) {
        removeImportResource(getModel().createResource(uri));
    }

    @Override
    public Stream<String> imports() {
        return importResources().map(Resource::getURI);
    }

    public Stream<Resource> importResources() {
        return Iter.asStream(listProperties(OWL.imports)
                .mapWith(Statement::getObject)
                .filterKeep(RDFNode::isURIResource)
                .mapWith(RDFNode::asResource));
    }

    public void addImportResource(Resource uri) {
        addProperty(OWL.imports, uri);
    }

    public void removeImportResource(Resource uri) {
        remove(OWL.imports, uri);
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
