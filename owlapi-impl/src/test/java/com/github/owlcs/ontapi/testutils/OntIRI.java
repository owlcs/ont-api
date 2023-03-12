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

package com.github.owlcs.ontapi.testutils;


import com.github.owlcs.ontapi.OntApiException;
import com.google.common.base.Strings;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyID;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.Optional;

/**
 * An extended {@link IRI} with several new methods.
 * <p>
 * Created by @szuev on 27.09.2016.
 */
public class OntIRI extends IRI {
    private static final String URI_RESOURCE_SEPARATOR = "#";

    protected OntIRI(String prefix, @Nullable String suffix) {
        super(prefix, suffix);
    }

    protected OntIRI(String s) {
        super(s);
    }

    protected OntIRI(URI uri) {
        super(uri);
    }

    private String base;

    public OntIRI addFragment(String fragment) {
        if (Strings.isNullOrEmpty(fragment))
            throw new OntIRIException("Incorrect fragment specified: '" + fragment + "'");
        return new OntIRI(String.format("%s%s%s", getBase(), URI_RESOURCE_SEPARATOR, fragment));
    }

    public OntIRI addPath(String path) {
        String base = getIRIString();
        if (base.contains(URI_RESOURCE_SEPARATOR)) {
            base = getBase();
        }
        return new OntIRI(String.format("%s/%s", base, OntIRIException.notNull(path, "Null path specified.")));
    }


    public String getBase() {
        if (base != null) return base;
        String iri = getIRIString();
        return base = iri.replaceFirst("#.*$", "").replaceFirst("/$", "");
    }

    public OWLOntologyID toOwlOntologyID() {
        return toOwlOntologyID(null);
    }

    public OWLOntologyID toOwlOntologyID(IRI versionIRI) {
        return versionIRI == null ? new OWLOntologyID(this) : new OWLOntologyID(this, versionIRI);
    }

    public Resource toResource() {
        return ResourceFactory.createResource(getIRIString());
    }

    public static OntIRI create(URI uri) {
        return new OntIRI(OntIRIException.notNull(uri, "Null URI"));
    }

    public static OntIRI create(Resource resource) {
        // should we allow anonymous resources also?
        if (!OntIRIException.notNull(resource, "Null resource specified").isURIResource()) {
            throw new OntIRIException("Not uri-resource: " + resource);
        }
        return new OntIRI(resource.getURI());
    }

    public static OntIRI create(String string) {
        return new OntIRI(string);
    }

    public static OntIRI create(IRI iri) {
        return new OntIRI(OntIRIException.notNull(iri, "Null owl-IRI specified.").getIRIString());
    }

    public static OntIRI create(OWLOntology o) {
        Optional<IRI> opt = OntIRIException.notNull(o, "Null owl-ontology specified.").getOntologyID().getOntologyIRI();
        return opt.map(OntIRI::create).orElse(null);
    }

    public static String toStringIRI(OWLOntologyID id) {
        return getString(id.getOntologyIRI());
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static String getString(Optional<IRI> optional) {
        return optional.map(IRI::getIRIString).orElse(null);
    }

    private static class OntIRIException extends OntApiException {
        OntIRIException(String s) {
            super(s);
        }

        public OntIRIException() {
            super();
        }

        public static <T> T notNull(T obj, String message) {
            if (obj == null)
                throw message == null ? new OntIRIException() : new OntIRIException(message);
            return obj;
        }
    }

}
