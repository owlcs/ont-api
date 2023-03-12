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

package com.github.owlcs.ontapi.transforms.vocabulary;

import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

import java.util.UUID;

/**
 * Vocabulary with (possible, temporary) resources used by ONT-API only.
 * Might be moved to {@link com.github.owlcs.ontapi.jena.vocabulary} if necessary.
 */
public class AVC {
    public final static String URI = "https://github.com/owlcs/ont-api";
    public final static String NS = URI + "#";

    public static final Resource AnonymousIndividual = resource("AnonymousIndividual");

    public static Resource error(int n) {
        if (n < 0) throw new IllegalArgumentException();
        return error(String.valueOf(n));
    }

    public static Resource randomIRI() {
        return resource("auto-" + UUID.randomUUID());
    }

    public static Resource error(Node node) {
        return error(node.toString());
    }

    public static Resource error(String suffix) {
        return resource("error-" + suffix);
    }

    protected static Resource resource(String local) {
        return ResourceFactory.createResource(NS + local);
    }

    protected static Property property(String local) {
        return ResourceFactory.createProperty(NS + local);
    }
}
