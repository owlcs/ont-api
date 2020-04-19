/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2020, The University of Manchester, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.tests;

import com.github.owlcs.ontapi.OWLAdapter;
import com.github.owlcs.ontapi.OntFormat;
import com.github.owlcs.ontapi.OntologyManager;
import com.github.owlcs.ontapi.utils.FileMap;
import org.junit.Assert;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.PriorityCollection;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * Collection of ontology resources for tests.
 * Created by @ssz on 19.04.2020.
 */
public enum ModelData {
    PIZZA("/ontapi/pizza.ttl"),
    FAMILY("/ontapi/family.ttl"),
    PEOPLE("/ontapi/people.ttl"),
    CAMERA("/ontapi/camera.ttl"),
    KOALA("/ontapi/koala.ttl"),
    TRAVEL("/ontapi/travel.ttl"),
    WINE("/ontapi/wine.ttl") {
        @Override
        String getName() {
            return "http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine";
        }

        @Override
        public OWLOntology load(OWLOntologyManager manager) {
            return load(manager, FOOD, this);
        }
    },
    FOOD("/ontapi/food.ttl") {
        @Override
        String getName() {
            return "http://www.w3.org/TR/2003/PR-owl-guide-20031209/food";
        }

        @Override
        public OWLOntology load(OWLOntologyManager manager) {
            return load(manager, WINE, this);
        }
    },
    NCBITAXON_CUT("/ontapi/ncbitaxon2.ttl"),
    HP_CUT("/ontapi/hp-cut.ttl"),
    ;
    private final Path file;
    private final OntFormat format;

    ModelData(String file) {
        this(file, OntFormat.TURTLE);
    }

    ModelData(String file, OntFormat format) {
        try {
            this.file = Paths.get(ModelData.class.getResource(file).toURI()).toRealPath();
        } catch (IOException | URISyntaxException e) {
            throw new ExceptionInInitializerError(e);
        }
        this.format = format;
    }

    static OWLOntology load(OWLOntologyManager manager, ModelData... data) {
        OWLOntology res = null;
        OWLOntologyLoaderConfiguration conf = createConfig(manager);
        if (!(manager instanceof OntologyManager)) { // OWL-API
            manager.setOntologyLoaderConfiguration(conf);
            PriorityCollection<OWLOntologyIRIMapper> maps = manager.getIRIMappers();
            Arrays.stream(data)
                    .map(d -> FileMap.create(IRI.create(d.getName()), d.getDocumentSource().getDocumentIRI()))
                    .forEach(maps::add);
            try {
                res = manager.loadOntology(IRI.create(data[data.length - 1].getName()));
            } catch (OWLOntologyCreationException e) {
                throw new AssertionError(e);
            }
        } else { // ONT-API
            for (ModelData d : data) {
                try {
                    res = manager.loadOntologyFromOntologyDocument(d.getDocumentSource(), conf);
                } catch (OWLOntologyCreationException e) {
                    throw new AssertionError(e);
                }
            }
        }
        Assert.assertEquals(data.length, manager.ontologies().count());
        return res;
    }

    public OWLOntology load(OWLOntologyManager manager) {
        try {
            return manager.loadOntologyFromOntologyDocument(getDocumentSource(), createConfig(manager));
        } catch (OWLOntologyCreationException e) {
            throw new AssertionError(e);
        }
    }

    static OWLOntologyLoaderConfiguration createConfig(OWLOntologyManager manager) {
        OWLOntologyLoaderConfiguration conf = manager.getOntologyLoaderConfiguration();
        if (manager instanceof OntologyManager) {
            conf = OWLAdapter.get().asONT(conf).setProcessImports(false).setPerformTransformation(false);
        } else {
            conf = conf.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
        }
        return conf;
    }

    public OWLOntologyDocumentSource getDocumentSource() {
        return new FileDocumentSource(file.toFile(), getDocumentFormat());
    }

    public OWLDocumentFormat getDocumentFormat() {
        return format.createOwlFormat();
    }

    String getName() {
        return name();
    }
}
