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

package com.github.owlcs.ontapi.tests.model;

import com.github.owlcs.ontapi.OWLAdapter;
import com.github.owlcs.ontapi.OntFormat;
import com.github.owlcs.ontapi.OntManagers;
import com.github.owlcs.ontapi.OntologyManager;
import com.github.owlcs.ontapi.utils.FileMap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
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
 * Created by @ssz on 08.03.2020.
 */
@RunWith(Parameterized.class)
public class ReferencingAxiomsTest {
    private final TestData data;

    public ReferencingAxiomsTest(TestData data) {
        this.data = data;
    }

    @Parameterized.Parameters(name = "{0}")
    public static TestData[] getData() {
        return TestData.values();
    }

    @Test
    public void testSearchByClass() {
        OWLOntology ont = data.load(OntManagers.createONT());
        long distinctCount = ont.classesInSignature().flatMap(ont::referencingAxioms).distinct().count();
        Assert.assertEquals(data.byClassDistinctCount, distinctCount);
        long nonDistinctCount = ont.classesInSignature().flatMap(ont::referencingAxioms).count();
        Assert.assertEquals(data.byClassCount, nonDistinctCount);
    }

    enum TestData {
        PIZZA("/ontapi/pizza.ttl", 1577, 795),
        FAMILY("/ontapi/family.ttl", 342, 236),
        PEOPLE("/ontapi/people.ttl", 233, 149),
        CAMERA("/ontapi/camera.ttl", 60, 47),
        KOALA("/ontapi/koala.ttl", 82, 59),
        TRAVEL("/ontapi/travel.ttl", 163, 111),
        WINE("/ontapi/wine.ttl", 576, 462) {
            @Override
            String getName() {
                return "http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine";
            }

            @Override
            public OWLOntology load(OWLOntologyManager manager) {
                return load(manager, FOOD, this);
            }
        },
        FOOD("/ontapi/food.ttl", 415, 284) {
            @Override
            String getName() {
                return "http://www.w3.org/TR/2003/PR-owl-guide-20031209/food";
            }

            @Override
            public OWLOntology load(OWLOntologyManager manager) {
                return load(manager, WINE, this);
            }
        };
        private final Path file;
        private final OntFormat format;
        private final long byClassCount;
        private final long byClassDistinctCount;

        TestData(String file, long byClassCount, long byClassDistinctCount) {
            this(file, OntFormat.TURTLE, byClassCount, byClassDistinctCount);
        }

        TestData(String file, OntFormat format, long byClassCount, long byClassDistinctCount) {
            try {
                this.file = Paths.get(TestData.class.getResource(file).toURI()).toRealPath();
            } catch (IOException | URISyntaxException e) {
                throw new ExceptionInInitializerError(e);
            }
            this.byClassCount = byClassCount;
            this.byClassDistinctCount = byClassDistinctCount;
            this.format = format;
        }

        public OWLOntology load(OWLOntologyManager manager) {
            try {
                // no transform
                OWLOntologyLoaderConfiguration conf = OWLAdapter.get()
                        .asONT(manager.getOntologyLoaderConfiguration())
                        .setPerformTransformation(false);
                return manager.loadOntologyFromOntologyDocument(getDocumentSource(), conf);
            } catch (OWLOntologyCreationException e) {
                throw new AssertionError(e);
            }
        }

        static OWLOntology load(OWLOntologyManager manager, TestData... data) {
            OWLOntology res = null;
            OWLOntologyLoaderConfiguration conf = manager.getOntologyLoaderConfiguration();
            if (!(manager instanceof OntologyManager)) { // OWL-API
                conf = conf.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
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
                conf = OWLAdapter.get().asONT(conf).setProcessImports(false).setPerformTransformation(false);
                for (TestData d : data) {
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
}
