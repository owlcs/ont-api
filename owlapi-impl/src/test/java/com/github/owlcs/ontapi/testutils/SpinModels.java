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

import com.github.owlcs.ontapi.OntologyManager;
import org.apache.jena.util.FileManager;
import org.semanticweb.owlapi.model.IRI;


/**
 * Collection of all spin models (located in resources)
 * Please note: it is for tests purposes only!
 * <p>
 * Created by szuev on 21.04.2017.
 */
public enum SpinModels {
    SP("/etc/sp.ttl", "http://spinrdf.org/sp"),
    SPIN("/etc/spin.ttl", "http://spinrdf.org/spin"),
    SPL("/etc/spl.spin.ttl", "http://spinrdf.org/spl"),
    SPIF("/etc/spif.ttl", "http://spinrdf.org/spif"),
    SPINMAP("/etc/spinmap.spin.ttl", "http://spinrdf.org/spinmap"),
    SMF("/etc/functions-smf.ttl", "http://topbraid.org/functions-smf"),
    FN("/etc/functions-fn.ttl", "http://topbraid.org/functions-fn"),
    AFN("/etc/functions-afn.ttl", "http://topbraid.org/functions-afn"),
    SMF_BASE("/etc/sparqlmotionfunctions.ttl", "http://topbraid.org/sparqlmotionfunctions"),
    SPINMAPL("/etc/spinmapl.spin.ttl", "http://topbraid.org/spin/spinmapl");

    private final String resource, uri;

    SpinModels(String file, String uri) {
        this.resource = file;
        this.uri = uri;
    }

    public static void addMappings(OntologyManager m) {
        for (SpinModels spin : values()) {
            m.getIRIMappers().add(FileMap.create(spin.getIRI(), spin.getFile()));
        }
    }

    public static void addMappings(FileManager fileManager) {
        for (SpinModels spin : values()) {
            fileManager.getLocationMapper().addAltEntry(spin.getIRI().getIRIString(), spin.getFile().toURI().toString());
        }
    }

    public String uri() {
        return uri;
    }

    public String file() {
        return resource;
    }

    public IRI getIRI() {
        return IRI.create(uri);
    }

    public IRI getFile() {
        return IRI.create(OWLIOUtils.getResourceURI(resource));
    }
}
